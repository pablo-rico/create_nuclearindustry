package org.papiricoh.create_nuclearindustry.accelerator.interlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorHooks;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorAttachment;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AcceleratorStructure;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The accelerator's personnel protection. A beam only runs while every {@link BeamInterlockBlockEntity}
 * beside the beamline is secured ("search and secure": the operator checked the vault and closed
 * it). With {@code accelerator.interlock.requirePlayersOutside} nobody may be inside the machine's
 * bounds either: the start is vetoed and a running beam trips. With
 * {@code accelerator.interlock.locksDoors} the doors next to an interlock refuse to open by hand
 * while the beam is on, and a door opened anyway (by redstone) breaks the search and trips it.
 * Hooks into the controller through {@link AcceleratorHooks}.
 */
public final class PersonnelInterlock {
    /** Ticks a look that found nobody inside a running machine is trusted. */
    public static final int OCCUPANCY_INTERVAL = 10;

    /** When each controller last looked for players inside, and whether it found any. */
    private static final Map<AcceleratorControllerBlockEntity, Occupancy> OCCUPANCY = new WeakHashMap<>();

    private record Occupancy(long checkedAt, boolean occupied) {}

    private PersonnelInterlock() {}

    /** Chains this area's hooks onto the controller's; called once at common setup. */
    public static void init() {
        AcceleratorHooks.startVeto = AcceleratorHooks.startVeto.or(PersonnelInterlock::vetoesStart);
        AcceleratorHooks.onTrip = AcceleratorHooks.onTrip.andThen(PersonnelInterlock::onTrip);
        AcceleratorHooks.onStructureChanged = AcceleratorHooks.onStructureChanged.andThen(PersonnelInterlock::forget);
        AcceleratorHooks.onRemoved = AcceleratorHooks.onRemoved.andThen(PersonnelInterlock::forget);
        NeoForge.EVENT_BUS.addListener(PersonnelInterlock::onUseBlock);
    }

    /** Every interlock among a controller's attachments is secured, and nobody is inside if the config asks. */
    public static boolean clear(AcceleratorControllerBlockEntity controller, List<AcceleratorAttachment> attachments) {
        for (AcceleratorAttachment attachment : attachments) {
            if (attachment instanceof BeamInterlockBlockEntity interlock && !interlock.isRemoved() && !interlock.secured()) {
                return false;
            }
        }
        return !AcceleratorConfig.interlockRequiresPlayersOutside || !occupiedCached(controller);
    }

    /** Interlocks among a controller's attachments. */
    public static int count(List<AcceleratorAttachment> attachments) {
        int count = 0;
        for (AcceleratorAttachment attachment : attachments) {
            if (attachment instanceof BeamInterlockBlockEntity) {
                count++;
            }
        }
        return count;
    }

    /** Whether any player who is not a spectator stands within {@code bounds}. */
    public static boolean occupied(Level level, BoundingBox bounds) {
        AABB box = AABB.of(bounds);
        for (Player player : level.players()) {
            if (!player.isSpectator() && player.isAlive() && player.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    private static boolean occupiedCached(AcceleratorControllerBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || controller.getStructure().isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        Occupancy last = OCCUPANCY.get(controller);
        // Only a clear machine is trusted for a while: once somebody was inside, look again every tick,
        // so the beam may start the moment they are out.
        if (last == null || last.occupied() || now - last.checkedAt() >= OCCUPANCY_INTERVAL || now < last.checkedAt()) {
            last = new Occupancy(now, occupied(level, controller.getStructure().get().bounds()));
            OCCUPANCY.put(controller, last);
        }
        return last.occupied();
    }

    /** A start with someone inside the machine is refused, whatever the panels say. */
    private static boolean vetoesStart(AcceleratorControllerBlockEntity controller) {
        if (!AcceleratorConfig.interlockRequiresPlayersOutside || controller.getLevel() == null) {
            return false;
        }
        boolean occupied = controller.getStructure().map(AcceleratorStructure::bounds)
                .map(bounds -> occupied(controller.getLevel(), bounds)).orElse(false);
        OCCUPANCY.put(controller, new Occupancy(controller.getLevel().getGameTime(), occupied));
        return occupied;
    }

    /** An interlock trip sounds the buzzer at every panel and at the controller. */
    private static void onTrip(AcceleratorControllerBlockEntity controller, BeamTripCause cause) {
        Level level = controller.getLevel();
        if (cause != BeamTripCause.INTERLOCK || level == null) {
            return;
        }
        level.playSound(null, controller.getBlockPos(), AllNuclearSounds.INTERLOCK_BUZZER.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        for (AcceleratorAttachment attachment : controller.attachments()) {
            if (attachment instanceof BeamInterlockBlockEntity interlock) {
                level.playSound(null, interlock.getBlockPos(), AllNuclearSounds.INTERLOCK_BUZZER.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    private static void forget(AcceleratorControllerBlockEntity controller) {
        OCCUPANCY.remove(controller);
    }

    // ------------------------------------------------------------------ doors

    /** A door, trapdoor or gate a player opens by hand. */
    public static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock;
    }

    public static boolean isOpenDoor(BlockState state) {
        return isDoor(state) && state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
    }

    /** The interlock beside this door (either half of a two-block door), if any. */
    @Nullable
    public static BeamInterlockBlockEntity interlockBeside(BlockGetter level, BlockPos door, BlockState state) {
        BeamInterlockBlockEntity found = interlockAround(level, door);
        if (found == null && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos other = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? door.above() : door.below();
            found = interlockAround(level, other);
        }
        return found;
    }

    @Nullable
    private static BeamInterlockBlockEntity interlockAround(BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof BeamInterlockBlockEntity interlock) {
                return interlock;
            }
        }
        return null;
    }

    /** Doors next to an interlock stay shut by hand while its beam is on. */
    private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!AcceleratorConfig.interlockLocksDoors) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (!isDoor(state) || isOpenDoor(state)) {
            return;
        }
        BeamInterlockBlockEntity interlock = interlockBeside(level, event.getPos(), state);
        if (interlock == null || !interlock.beamOn()) {
            return;
        }
        event.setCanceled(true);
        if (!level.isClientSide) {
            event.getEntity().displayClientMessage(Component.translatable("create_nuclearindustry.accelerator.interlock.door_locked")
                    .withStyle(ChatFormatting.RED), true);
            level.playSound(null, event.getPos(), AllNuclearSounds.INTERLOCK_BUZZER.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
        }
    }
}
