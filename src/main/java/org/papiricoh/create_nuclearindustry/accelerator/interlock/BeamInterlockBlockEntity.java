package org.papiricoh.create_nuclearindustry.accelerator.interlock;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorAttachment;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorSupply;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;

import java.util.List;

/**
 * A Beam Interlock panel. It serves the accelerator by being secured: the controller reads
 * {@link #secured} each tick and refuses or trips the beam while any panel is open. It also hears
 * what the beam is doing, to lock the doors beside it while the beam is on, and breaks its search
 * (opens) when one of those doors is opened anyway.
 */
public class BeamInterlockBlockEntity extends SmartBlockEntity implements AcceleratorAttachment, IHaveGoggleInformation {
    /** Without news from a controller for this long, the panel counts as outside any accelerator. */
    static final int OBSERVE_TIMEOUT = 60;
    /** Ticks between two looks at the doors beside the panel. */
    static final int DOOR_INTERVAL = 5;
    private static final String K = "create_nuclearindustry.accelerator.interlock.";

    private boolean linked;
    private BeamPhase phase = BeamPhase.OFFLINE;
    private long lastObserved = Long.MIN_VALUE;
    @Nullable
    private BlockPos controller;

    public BeamInterlockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    // ------------------------------------------------------------------ the panel

    public boolean secured() {
        BlockState state = getBlockState();
        return state.hasProperty(BeamInterlockBlock.SECURED) && state.getValue(BeamInterlockBlock.SECURED);
    }

    /** Search and secure: refused while a door beside the panel stands open. */
    public void secure(@Nullable Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (secured()) {
            tell(player, Component.translatable(K + "already_secured").withStyle(ChatFormatting.GREEN));
            return;
        }
        if (openDoorBeside()) {
            tell(player, Component.translatable(K + "close_doors").withStyle(ChatFormatting.RED));
            return;
        }
        setSecured(true);
        level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8f, 1.2f);
        tell(player, Component.translatable(K + "secured").withStyle(ChatFormatting.GREEN));
    }

    /** Opens the panel: a running beam trips on the controller's next tick. */
    public void open(@Nullable Player player) {
        if (level == null || level.isClientSide || !secured()) {
            return;
        }
        setSecured(false);
        level.playSound(null, worldPosition, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.8f, 0.9f);
        tell(player, Component.translatable(K + "opened").withStyle(ChatFormatting.YELLOW));
    }

    /** Sets the panel's state directly (game tests and the code above). */
    public void setSecured(boolean secured) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BeamInterlockBlock.SECURED) && state.getValue(BeamInterlockBlock.SECURED) != secured) {
            level.setBlock(worldPosition, state.setValue(BeamInterlockBlock.SECURED, secured), Block.UPDATE_ALL);
        }
    }

    private static void tell(@Nullable Player player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    private boolean openDoorBeside() {
        for (Direction side : Direction.values()) {
            BlockPos next = worldPosition.relative(side);
            if (PersonnelInterlock.isOpenDoor(level.getBlockState(next))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (linked && level.getGameTime() - lastObserved > OBSERVE_TIMEOUT) {
            observe(BeamReadings.NONE);
        }
        if (level.getGameTime() % DOOR_INTERVAL == 0 && secured() && openDoorBeside()) {
            // Somebody opened the door into the vault: the search no longer holds.
            setSecured(false);
            if (beamOn()) {
                level.playSound(null, worldPosition, AllNuclearSounds.INTERLOCK_BUZZER.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    // ------------------------------------------------------------------ the beam

    /** The accelerator this panel serves has beam in the machine (and so locks the doors beside it). */
    public boolean beamOn() {
        return linked && (phase.hasBeam() || phase == BeamPhase.RF_ON);
    }

    public boolean linked() {
        return linked;
    }

    public BeamPhase phase() {
        return phase;
    }

    /** The controller this panel serves, or null. */
    @Nullable
    public BlockPos controller() {
        return controller;
    }

    @Override
    public void contribute(AcceleratorSupply.Builder supply) {}

    @Override
    public void bind(@Nullable BlockPos controller) {
        this.controller = controller == null ? null : controller.immutable();
    }

    @Override
    public void observe(BeamReadings readings) {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean formed = readings.formed();
        lastObserved = formed ? level.getGameTime() : Long.MIN_VALUE;
        BeamPhase next = formed ? readings.phase() : BeamPhase.OFFLINE;
        if (formed != linked || next != phase) {
            linked = formed;
            phase = next;
            sendData();
        }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        boolean secured = secured();
        tooltip.add(line(Component.translatable(secured ? K + "goggles.secured" : K + "goggles.open"),
                secured ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(line(Component.translatable(linked ? K + "goggles.linked" : K + "goggles.not_linked",
                Component.translatable(phase.langKey())), linked ? ChatFormatting.GRAY : ChatFormatting.RED));
        if (beamOn() && AcceleratorConfig.interlockLocksDoors) {
            tooltip.add(line(Component.translatable(K + "goggles.doors_locked"), ChatFormatting.YELLOW));
        }
        tooltip.add(line(Component.translatable(K + "goggles.hint"), ChatFormatting.DARK_GRAY));
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putBoolean("Linked", linked);
            tag.putString("Phase", phase.name());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            linked = tag.getBoolean("Linked");
            phase = BeamReadings.phase(tag.getString("Phase"));
        }
    }
}
