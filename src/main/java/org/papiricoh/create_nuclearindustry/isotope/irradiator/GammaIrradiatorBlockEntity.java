package org.papiricoh.create_nuclearindustry.isotope.irradiator;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeConfig;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.Isotopes;
import org.papiricoh.create_nuclearindustry.isotope.SterileFood;
import org.papiricoh.create_nuclearindustry.isotope.physics.GammaIrradiation;
import org.papiricoh.create_nuclearindustry.isotope.physics.Isotope;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;

import java.util.List;
import java.util.Locale;

/**
 * The Gamma Irradiator: up to four cobalt-60 sources in a lead head, two blocks above a belt or a
 * depot like a press. Harmful food stopping under it is held while the shutter is open, until it
 * has taken the sterilising dose; then it moves on sterile ({@link SterileFood}). The time grows as
 * the sources decay, and a head too weak to sterilise lets everything pass. Closed, the lead keeps
 * most of the radiation in; open, it shines on whoever stands by the belt.
 */
public class GammaIrradiatorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, RadiationEmitter {
    public static final int SOURCES = 4;
    private static final int REFRESH_INTERVAL = 20;
    /** Share of the radiation the closed lead head lets out. */
    private static final double CLOSED_LEAK = 0.05;

    private final ItemStackHandler sources = new ItemStackHandler(SOURCES) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(IsotopeRegistry.COBALT_60.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            refreshActivity();
            setChanged();
            sendData();
        }
    };
    private BeltProcessingBehaviour processing;
    /** Sum of the sources' activities, as of the last refresh. */
    private double activity;
    private int timer;
    private int required;
    private boolean open;
    /** Game time the belt last asked about a held item: the shutter closes once it stops asking. */
    private long lastHeld = Long.MIN_VALUE;
    /** Client: how far the shutter leaves stand open, eased over half a second. */
    public final LerpedFloat shutter = LerpedFloat.linear().startWithValue(0);

    public GammaIrradiatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        processing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemEnters).whileItemHeld(this::whileItemHeld);
        behaviours.add(processing);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            refreshActivity();
            RadiationEmitters.register(this);
        }
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            RadiationEmitters.unregister(this);
        }
        super.invalidate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            shutter.chase(open ? 1 : 0, 0.1, LerpedFloat.Chaser.LINEAR);
            shutter.tickChaser();
            return;
        }
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            double before = activity;
            refreshActivity();
            if (Math.abs(before - activity) > 0.005) {
                sendData();
            }
        }
        // The belt calls whileItemHeld every tick it holds an item; anything else closes the shutter.
        if (open && level.getGameTime() - lastHeld > 2) {
            setOpen(false);
        }
    }

    private ProcessingResult onItemEnters(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (level == null || !SterileFood.sterilisable(transported.stack)) {
            return ProcessingResult.PASS;
        }
        int ticks = GammaIrradiation.processingTicks(IsotopeConfig.irradiator(), activity);
        if (ticks == GammaIrradiation.TOO_WEAK) {
            return ProcessingResult.PASS;
        }
        required = ticks;
        timer = 0;
        if (!level.isClientSide) {
            lastHeld = level.getGameTime();
            setOpen(true);
        }
        return ProcessingResult.HOLD;
    }

    private ProcessingResult whileItemHeld(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        if (level == null || !SterileFood.sterilisable(transported.stack)) {
            return ProcessingResult.PASS;
        }
        if (level.isClientSide) {
            return ProcessingResult.HOLD;
        }
        lastHeld = level.getGameTime();
        if (!open) {
            int ticks = GammaIrradiation.processingTicks(IsotopeConfig.irradiator(), activity);
            if (ticks == GammaIrradiation.TOO_WEAK) {
                return ProcessingResult.PASS;
            }
            required = ticks;
            timer = 0;
            setOpen(true);
        }
        if (++timer < required) {
            return ProcessingResult.HOLD;
        }
        TransportedItemStack done = transported.copy();
        done.stack = SterileFood.sterilise(transported.stack);
        handler.handleProcessingOnItem(transported, TransportedResult.convertTo(done));
        timer = 0;
        setOpen(false);
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.3f, 2.0f);
        NuclearAdvancements.awardNearby(level, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.FOOD_STERILISED);
        return ProcessingResult.PASS;
    }

    private void setOpen(boolean open) {
        if (this.open != open) {
            this.open = open;
            setChanged();
            sendData();
        }
    }

    /** Brings the sources' clocks up to date and adds their activities. */
    private void refreshActivity() {
        long now = Isotopes.time(level);
        double sum = 0;
        for (int slot = 0; slot < SOURCES; slot++) {
            ItemStack stack = sources.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Isotopes.refresh(stack, now);
                sum += Isotopes.activity(stack, now);
            }
        }
        activity = sum;
    }

    // ------------------------------------------------------------------ sources

    public IItemHandler sources() {
        return sources;
    }

    /** Puts the source in {@code hand} into a free slot. */
    public boolean insertSource(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        for (int slot = 0; slot < SOURCES; slot++) {
            if (sources.getStackInSlot(slot).isEmpty()) {
                ItemStack rest = sources.insertItem(slot, held.copyWithCount(1), false);
                if (rest.isEmpty()) {
                    held.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    /** Hands the weakest source back. */
    public boolean takeSource(Player player) {
        long now = Isotopes.time(level);
        int weakest = -1;
        double lowest = Double.MAX_VALUE;
        for (int slot = 0; slot < SOURCES; slot++) {
            ItemStack stack = sources.getStackInSlot(slot);
            if (!stack.isEmpty() && Isotopes.activity(stack, now) < lowest) {
                lowest = Isotopes.activity(stack, now);
                weakest = slot;
            }
        }
        if (weakest < 0) {
            return false;
        }
        ItemStack taken = sources.extractItem(weakest, 1, false);
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
        return true;
    }

    public int sourceCount() {
        int count = 0;
        for (int slot = 0; slot < SOURCES; slot++) {
            if (!sources.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** Total cobalt-60 activity in the head (1 = one fresh source). */
    public double activity() {
        return activity;
    }

    /** Strength of the sources against one fresh source, 0..1 for the renderer's glow. */
    public double sourceStrength() {
        int count = sourceCount();
        return count == 0 ? 0 : Math.min(1.0, activity / count);
    }

    /** Whether the shutter is open: an item is being sterilised. */
    public boolean isOpen() {
        return open;
    }

    /** Ticks one item takes now, or {@link GammaIrradiation#TOO_WEAK}. */
    public int processingTicks() {
        return GammaIrradiation.processingTicks(IsotopeConfig.irradiator(), activity);
    }

    public int comparatorSignal() {
        return GammaIrradiation.comparator(IsotopeConfig.irradiator(), activity);
    }

    /**
     * Shows an irradiator in a Ponder scene: {@code count} fresh sources and the shutter as given. Its
     * world never runs the server side, so what is set here stays until the next call.
     */
    public void preview(int count, boolean open) {
        for (int slot = 0; slot < SOURCES; slot++) {
            sources.setStackInSlot(slot, slot < count ? new ItemStack(IsotopeRegistry.COBALT_60.get()) : ItemStack.EMPTY);
        }
        activity = Math.min(count, SOURCES);
        this.open = open;
    }

    public void dropSources() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < SOURCES; slot++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    sources.getStackInSlot(slot));
        }
    }

    // ------------------------------------------------------------------ radiation

    @Override
    public Vec3 radiationOrigin() {
        return Vec3.atCenterOf(worldPosition).subtract(0, 1.0, 0);
    }

    @Override
    public double radiationStrength() {
        return GammaIrradiation.emission(IsotopeConfig.irradiator(), activity) * (open ? 1.0 : CLOSED_LEAK);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.gamma_irradiator.goggles.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.gamma_irradiator"))
                .withStyle(ChatFormatting.GOLD));
        int count = sourceCount();
        tooltip.add(line(Component.translatable(k + "sources", count, SOURCES, Math.round(activity * 100)),
                count > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
        int ticks = processingTicks();
        if (count == 0) {
            tooltip.add(line(Component.translatable(k + "empty"), ChatFormatting.DARK_GRAY));
        } else if (ticks == GammaIrradiation.TOO_WEAK) {
            tooltip.add(line(Component.translatable(k + "too_weak"), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(k + "time", String.format(Locale.ROOT, "%.1f", ticks / 20.0)), ChatFormatting.GREEN));
            double days = GammaIrradiation.daysOfService(IsotopeConfig.irradiator(), activity,
                    IsotopeConfig.halfLifeDays(Isotope.COBALT_60)) / IsotopeConfig.timeScale;
            tooltip.add(line(Component.translatable(k + "service", Math.round(days)), ChatFormatting.DARK_GRAY));
        }
        tooltip.add(line(Component.translatable(open ? k + "open" : k + "closed"), open ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Sources", sources.serializeNBT(registries));
        tag.putInt("Timer", timer);
        tag.putInt("Required", required);
        tag.putBoolean("Open", open);
        if (clientPacket) {
            tag.putDouble("Activity", activity);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        sources.deserializeNBT(registries, tag.getCompound("Sources"));
        timer = tag.getInt("Timer");
        required = tag.getInt("Required");
        open = tag.getBoolean("Open");
        if (clientPacket) {
            activity = tag.getDouble("Activity");
        }
    }
}
