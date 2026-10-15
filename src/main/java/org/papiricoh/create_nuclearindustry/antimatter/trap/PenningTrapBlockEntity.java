package org.papiricoh.create_nuclearindustry.antimatter.trap;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolder;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.PlantReadable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.List;
import java.util.function.Consumer;

/**
 * A Penning trap's master: the antimatter it holds, its field and its Forge Energy buffer, worked
 * out each tick by {@link PenningTrapModel}. Safety, as the model has it: a loaded trap that runs out
 * of energy quenches ({@code antimatter.trap.quenchTicks}, with its alarm) and then annihilates;
 * anything put in while its field is down annihilates at once; breaking it loaded annihilates what it
 * holds. Its vial port fills or empties a vial and recharges the vial's battery while the field holds.
 *
 * <p>Clients get the charge, field, state, quench countdown and port contents for the renderer
 * and goggles; the control room reads it as a {@link TargetKind#ANTIMATTER_TRAP}.
 */
public class PenningTrapBlockEntity extends SmartBlockEntity implements AntimatterHolder, PlantReadable, IHaveGoggleInformation {
    /** Ticks between two syncs while only the charge or the field drift. */
    private static final int SYNC_INTERVAL = 10;
    /** Charge that earns the "First Nanogram" advancement, ng. */
    public static final double FIRST_NANOGRAM = 1.0 - 1.0e-9;
    /**
     * Client work each loaded trap hands off every tick (its hum, quench sparks, the gauge's
     * needle), set by the client setup so this class never touches client code.
     */
    public static Consumer<PenningTrapBlockEntity> clientTick = trap -> {};

    private double stored;
    private float field;
    private TrapState state = TrapState.OFFLINE;
    /** Countdown to annihilation; {@code quenchTicks} on a healthy trap, -1 until the first tick sets it. */
    private int quenchLeft = -1;
    private VialPortMode mode = VialPortMode.FILL;
    private final FeBuffer energy;
    private final ItemStackHandler port = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof AntimatterVialItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Automation only takes a vial out once the port is done with it.
            return portDone() ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            portChanged = true;
            setChanged();
        }
    };
    /** The trap has gone off: the blast is queued, nothing it held is left to lose again. */
    private boolean annihilating;
    private boolean portChanged;
    private int lastComparator = -1;
    private long lastSync;
    private double syncedStored;
    private float syncedField;
    private TrapState syncedState = TrapState.OFFLINE;
    /** FE that arrived in the buffer since last tick, for the goggles' time to quench. */
    private int lastInput;
    /** What it held at the end of the last tick, for the first-nanogram advancement; NaN before the first. */
    private double storedLastTick = Double.NaN;
    private int bufferBefore;

    public PenningTrapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.energy = new FeBuffer(PenningTrapModel.bufferCapacity(AntimatterConfig.trap(tierOf(state))), this::setChanged);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    private static TrapTier tierOf(BlockState state) {
        return state.getBlock() instanceof PenningTrapBlock trap ? trap.tier() : TrapTier.SMALL;
    }

    // ------------------------------------------------------------------ what the renderer and goggles read

    public TrapTier tier() {
        return tierOf(getBlockState());
    }

    public PenningTrapModel.Params params() {
        return AntimatterConfig.trap(tier());
    }

    @Override
    public double stored() {
        return stored;
    }

    /** The server's capacity, as the last update said: clients may run another config. */
    private double syncedCapacity;

    @Override
    public double capacity() {
        return level != null && level.isClientSide && syncedCapacity > 0 ? syncedCapacity : params().capacity();
    }

    public TrapState state() {
        return state;
    }

    /** Share of its demand the field got last tick, 0..1. */
    public float fieldStrength() {
        return field;
    }

    public int quenchTicksLeft() {
        return quenchLeft < 0 ? params().quenchTicks() : quenchLeft;
    }

    /** Always true: only the master has this block entity. */
    public boolean isMaster() {
        return true;
    }

    public VialPortMode mode() {
        return mode;
    }

    /** The vial in the port, or empty. */
    public ItemStack vial() {
        return port.getStackInSlot(0);
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public FeBuffer buffer() {
        return energy;
    }

    public IItemHandler port() {
        return port;
    }

    /** FE per tick the trap needs now. */
    public double demand() {
        return PenningTrapModel.demand(params(), stored);
    }

    /** Ticks until it annihilates if no more energy comes, or {@link Integer#MAX_VALUE}. */
    public int ticksToAnnihilation() {
        return PenningTrapModel.ticksToAnnihilation(params(), stored, energy.getEnergyStored(), quenchTicksLeft(), lastInput);
    }

    public int comparatorSignal() {
        return PenningTrapModel.comparator(params(), stored);
    }

    /** The middle of the cloud, where it annihilates. */
    public Vec3 centre() {
        BlockPos pos = getBlockPos();
        return switch (tier()) {
            case SMALL -> Vec3.atCenterOf(pos);
            case MEDIUM -> new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            case LARGE -> new Vec3(pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
        };
    }

    /** Every block of this trap. */
    public List<BlockPos> parts() {
        BlockState blockState = getBlockState();
        return blockState.getBlock() instanceof PenningTrapBlock trap ? trap.parts(blockState, getBlockPos()) : List.of(getBlockPos());
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level != null && level.isClientSide) {
            clientTick.accept(this);
            return;
        }
        if (level == null || annihilating) {
            return;
        }
        PenningTrapModel.Params p = params();
        int capacity = PenningTrapModel.bufferCapacity(p);
        if (capacity != energy.getMaxEnergyStored()) {
            energy.resize(capacity);
        }
        if (quenchLeft < 0) {
            quenchLeft = p.quenchTicks();
        }
        int buffered = energy.getEnergyStored();
        lastInput = Math.max(0, buffered - bufferBefore);
        PenningTrapModel.Step step = PenningTrapModel.tick(p, stored, buffered, quenchLeft);
        energy.take(step.feUsed());
        bufferBefore = energy.getEnergyStored();
        TrapState before = state;
        field = step.field();
        state = step.state();
        quenchLeft = step.quenchTicksLeft();
        if (state == TrapState.QUENCHING && before != TrapState.QUENCHING) {
            level.playSound(null, getBlockPos(), AllNuclearSounds.TRAP_QUENCH.get(), SoundSource.BLOCKS, 2.0f, 1.0f);
        }
        if (step.annihilate()) {
            annihilate(0);
            return;
        }
        tickPort();
        if (annihilating) {
            return;
        }
        updateComparators();
        awardFirstNanogram();
        sync(before);
    }

    /** Players nearby earn "First Nanogram" when a trap's charge first reaches a whole nanogram. */
    private void awardFirstNanogram() {
        double before = storedLastTick;
        storedLastTick = stored;
        if (!Double.isNaN(before) && before < FIRST_NANOGRAM && stored >= FIRST_NANOGRAM) {
            NuclearAdvancements.awardNearby(level, getBlockPos(), NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.FIRST_NANOGRAM);
        }
    }

    /** Fills or empties the vial in the port and keeps its battery up while the field holds. */
    private void tickPort() {
        ItemStack vial = port.getStackInSlot(0);
        if (!(vial.getItem() instanceof AntimatterVialItem)) {
            return;
        }
        VialBattery.Params vp = AntimatterConfig.vial();
        boolean holds = powered();
        double inVial = AntimatterVialItem.antimatter(vial);
        if (mode == VialPortMode.FILL && holds) {
            double moved = VialBattery.fillStep(vp, inVial, stored);
            if (moved > 0) {
                stored -= moved;
                inVial += moved;
            }
        } else if (mode == VialPortMode.DRAIN && VialBattery.loaded(inVial)) {
            double moved = VialBattery.drainStep(vp, inVial, space());
            if (moved > 0) {
                if (!holds) {
                    // Emptying a vial into a trap without its field: both annihilate.
                    annihilate(0);
                    return;
                }
                inVial -= moved;
                stored += moved;
            }
        }
        int charge = VialBattery.tick(vp, AntimatterVialItem.charge(vial), inVial, holds);
        if (VialBattery.annihilates(charge, inVial)) {
            port.setStackInSlot(0, ItemStack.EMPTY);
            AntimatterAnnihilation.schedule((ServerLevel) level, portPosition(), inVial);
            return;
        }
        ItemStack updated = vial.copy();
        AntimatterVialItem.set(updated, inVial, charge);
        if (!ItemStack.isSameItemSameComponents(updated, vial)) {
            port.setStackInSlot(0, updated);
        }
    }

    private Vec3 portPosition() {
        return centre();
    }

    /** Whether automation may take the vial: filled up, emptied out, or with nothing left to move. */
    private boolean portDone() {
        ItemStack vial = port.getStackInSlot(0);
        double inVial = AntimatterVialItem.antimatter(vial);
        VialBattery.Params vp = AntimatterConfig.vial();
        return mode == VialPortMode.FILL
                ? inVial >= vp.capacity() - 1.0e-6 || !PenningTrapModel.loaded(stored)
                : !VialBattery.loaded(inVial) || space() <= PenningTrapModel.EMPTY;
    }

    private void updateComparators() {
        int signal = comparatorSignal();
        if (signal != lastComparator) {
            lastComparator = signal;
            for (BlockPos part : parts()) {
                level.updateNeighbourForOutputSignal(part, getBlockState().getBlock());
            }
        }
    }

    private void sync(TrapState before) {
        double capacity = Math.max(1.0e-9, capacity());
        boolean changed = state != syncedState || before != state || portChanged
                || PenningTrapModel.loaded(stored) != PenningTrapModel.loaded(syncedStored);
        boolean drifted = Math.abs(stored - syncedStored) > 0.005 * capacity || Math.abs(field - syncedField) > 0.02f
                || state == TrapState.QUENCHING;
        long now = level.getGameTime();
        if (changed || drifted && now - lastSync >= (state == TrapState.QUENCHING ? 2 : SYNC_INTERVAL)) {
            lastSync = now;
            syncedStored = stored;
            syncedField = field;
            syncedState = state;
            portChanged = false;
            sendData();
        }
    }

    // ------------------------------------------------------------------ antimatter in and out

    @Override
    public boolean powered() {
        return !annihilating && PenningTrapModel.holds(field);
    }

    /**
     * Takes up to {@code ng}. A trap whose field is down takes it all the same and annihilates at
     * once, together with what it held.
     */
    @Override
    public double insert(double ng, boolean simulate) {
        if (annihilating || !(ng > 0)) {
            return 0;
        }
        double taken = Math.min(ng, space());
        if (simulate || taken <= 0) {
            return Math.max(0, taken);
        }
        if (!powered()) {
            annihilate(taken);
            return taken;
        }
        stored += taken;
        setChanged();
        return taken;
    }

    @Override
    public double extract(double ng, boolean simulate) {
        if (annihilating || !(ng > 0)) {
            return 0;
        }
        double given = Math.min(ng, stored);
        if (!simulate && given > 0) {
            stored -= given;
            if (!PenningTrapModel.loaded(stored)) {
                stored = 0;
            }
            setChanged();
        }
        return Math.max(0, given);
    }

    /**
     * The cloud touches the wall: everything held, plus {@code extra} just put in and a loaded vial
     * in the port, annihilates at the end of the tick, and the trap's blocks go with it.
     */
    public void annihilate(double extra) {
        if (annihilating || !(level instanceof ServerLevel server)) {
            return;
        }
        annihilating = true;
        double total = stored + Math.max(0, extra) + AntimatterVialItem.antimatter(port.getStackInSlot(0));
        stored = 0;
        state = TrapState.OFFLINE;
        port.setStackInSlot(0, ItemStack.EMPTY);
        AntimatterAnnihilation.schedule(server, centre(), total, parts());
        // Everyone who sees (or caused) a trap lose its antimatter learns the word for it.
        NuclearAdvancements.awardNearby(server, getBlockPos(), NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.QUENCH);
        setChanged();
    }

    /** Puts antimatter in directly, field or not (game tests, the creative tab): goes through {@link #insert}. */
    public double fill(double ng) {
        return insert(ng, false);
    }

    /** Sets the charge outright without any check (game tests). */
    public void setStored(double ng) {
        stored = Math.max(0, Math.min(capacity(), ng));
        setChanged();
        sendData();
    }

    /** Raises the field as if the trap had been fed this tick (game tests that insert on the first tick). */
    public void primeField() {
        field = 1f;
        state = TrapState.STABLE;
        quenchLeft = params().quenchTicks();
    }

    /**
     * Shows a charge and a field state in a Ponder scene: its world never runs the trap's physics,
     * so what is set here stays until the next call. Not for a real world.
     */
    public void preview(double ng, TrapState shown, float fieldShare, int quenchTicksLeft) {
        stored = Math.max(0, Math.min(params().capacity(), ng));
        state = shown;
        field = Math.max(0, Math.min(1, fieldShare));
        quenchLeft = Math.max(0, quenchTicksLeft);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null || level.isClientSide || annihilating) {
            return;
        }
        ItemStack vial = port.getStackInSlot(0);
        if (PenningTrapModel.loaded(stored) || AntimatterVialItem.loaded(vial)) {
            annihilate(0);
        } else if (!vial.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5,
                    getBlockPos().getZ() + 0.5, vial);
        }
    }

    // ------------------------------------------------------------------ the port, by hand

    /** Puts one vial from the player's hand into an empty port. */
    public void insertVial(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!port.getStackInSlot(0).isEmpty()) {
            player.displayClientMessage(Component.translatable("create_nuclearindustry.penning_trap.port_full"), true);
            return;
        }
        ItemStack one = held.copyWithCount(1);
        port.setStackInSlot(0, one);
        if (!player.isCreative()) {
            held.shrink(1);
        }
        level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
        player.displayClientMessage(Component.translatable("create_nuclearindustry.penning_trap.vial_in",
                Component.translatable(mode.langKey())), true);
    }

    /** Hands the vial in the port to the player, or tells them what the trap holds. */
    public void takeVial(Player player) {
        ItemStack vial = port.getStackInSlot(0);
        if (vial.isEmpty()) {
            player.displayClientMessage(Component.translatable("create_nuclearindustry.penning_trap.status",
                    String.format(java.util.Locale.ROOT, "%.2f", stored), String.format(java.util.Locale.ROOT, "%.0f", capacity()),
                    Component.translatable(stateKey(state))), true);
            return;
        }
        port.setStackInSlot(0, ItemStack.EMPTY);
        if (!player.getInventory().add(vial)) {
            player.drop(vial, false);
        }
        level.playSound(null, getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    public void cycleMode(@org.jetbrains.annotations.Nullable Player player) {
        mode = mode.next();
        portChanged = true;
        setChanged();
        sendData();
        if (player != null) {
            player.displayClientMessage(Component.translatable("create_nuclearindustry.penning_trap.mode",
                    Component.translatable(mode.langKey())), true);
        }
    }

    public void setMode(VialPortMode mode) {
        this.mode = mode;
        portChanged = true;
        setChanged();
    }

    public static String stateKey(TrapState state) {
        return "create_nuclearindustry.penning_trap.state." + state.name().toLowerCase(java.util.Locale.ROOT);
    }

    // ------------------------------------------------------------------ control room

    @Override
    public TargetKind plantKind() {
        return TargetKind.ANTIMATTER_TRAP;
    }

    @Override
    public boolean plantFormed() {
        return true;
    }

    @Override
    public MonitorReading plantSample() {
        float[] v = new float[MonitorReading.TRAP_VALUES];
        PenningTrapModel.Params p = params();
        v[MonitorReading.P_STORED] = (float) stored;
        v[MonitorReading.P_CAPACITY] = (float) p.capacity();
        v[MonitorReading.P_FILL] = (float) PenningTrapModel.fill(p, stored);
        v[MonitorReading.P_FIELD] = field;
        v[MonitorReading.P_BUFFER] = energy.getMaxEnergyStored() <= 0 ? 0 : energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
        v[MonitorReading.P_STATE] = state.ordinal();
        v[MonitorReading.P_DEMAND] = (float) demand();
        v[MonitorReading.P_QUENCH] = quenchTicksLeft();
        return new MonitorReading(TargetKind.ANTIMATTER_TRAP, stateKey(state), v, v[MonitorReading.P_FILL], v[MonitorReading.P_BUFFER]);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        PenningTrapGoggles.add(this, tooltip);
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Stored", stored);
        tag.putFloat("Field", field);
        tag.putInt("State", state.ordinal());
        tag.putInt("QuenchLeft", quenchLeft);
        tag.putString("Mode", mode.name());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Port", port.serializeNBT(registries));
        if (clientPacket) {
            tag.putDouble("Capacity", params().capacity());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = Math.max(0, tag.getDouble("Stored"));
        field = tag.getFloat("Field");
        state = TrapState.byOrdinal(tag.getInt("State"));
        quenchLeft = tag.contains("QuenchLeft") ? tag.getInt("QuenchLeft") : -1;
        mode = VialPortMode.byName(tag.getString("Mode"));
        energy.set(tag.getInt("Energy"));
        port.deserializeNBT(registries, tag.getCompound("Port"));
        if (clientPacket) {
            syncedCapacity = tag.getDouble("Capacity");
        } else {
            bufferBefore = energy.getEnergyStored();
        }
    }
}
