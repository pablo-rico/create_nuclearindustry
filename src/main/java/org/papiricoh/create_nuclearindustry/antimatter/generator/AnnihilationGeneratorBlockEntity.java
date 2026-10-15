package org.papiricoh.create_nuclearindustry.antimatter.generator;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationBlastModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolder;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterHolders;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.PlantReadable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.energy.FeSource;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The annihilation generator, worked out each tick by {@link AnnihilationModel#step}: a 1 ng chamber
 * it keeps topped up from the antimatter holders beside it (traps, the conduit, target stations) and
 * from a vial in its port, burnt at the redstone throttle (0 is the standby burn that only holds the
 * field) into 256 RPM of stress capacity, Forge Energy or both. A cold chamber holds antimatter only
 * with FE in its field buffer, like a trap; once lit the burn holds the field itself, and in an
 * electric mode the generator keeps that buffer full from its own output. Its Forge Energy is
 * pushed fairly into every neighbour that takes it, so it can power its own traps.
 *
 * <p>Clients get the chamber, the field, the throttle, the delivered share and the mode, for the
 * renderer, the drone and goggles; the control room reads it as a
 * {@link TargetKind#ANNIHILATION_GENERATOR}.
 */
public class AnnihilationGeneratorBlockEntity extends GeneratingKineticBlockEntity implements AntimatterHolder, PlantReadable {
    /** Client work each generator hands off every tick (its drone, sparks), set by the client setup. */
    public static Consumer<AnnihilationGeneratorBlockEntity> clientTick = generator -> {};
    private static final int SYNC_INTERVAL = 10;
    private static final int AWARD_INTERVAL = 100;
    private static final String K = "create_nuclearindustry.annihilation_generator.";

    private ScrollOptionBehaviour<GeneratorMode> modeOption;
    private double stored;
    /**
     * Ticks the chamber still holds its own field: set to {@link AnnihilationModel#LIT_GRACE_TICKS}
     * by every burn, so a short gap in the supply does not let it go cold; see {@link AnnihilationModel.Step#litTicks()}.
     */
    private int litTicks;
    private int quenchLeft = -1;
    private float field;
    private TrapState chamberState = TrapState.OFFLINE;
    private double throttle;
    private AnnihilationModel.Output output = AnnihilationModel.Output.NONE;
    private int feMade;
    private float generatedSpeed;
    private float generatedCapacity;
    private final FeBuffer fieldBuffer;
    private final FeSource source;
    private final IEnergyStorage terminals = new Terminals();
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
            // Automation only takes a vial out once it is empty.
            return AntimatterVialItem.loaded(getStackInSlot(slot)) ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            portChanged = true;
            setChanged();
        }
    };
    /** The chamber has gone off: the blast is queued and the block goes with it. */
    private boolean annihilating;
    private boolean portChanged;
    private int lastComparator = -1;
    private long lastSync;
    private double syncedStored;
    private double syncedFraction;
    private boolean syncedLit;
    private boolean syncedBurning;
    private TrapState syncedState = TrapState.OFFLINE;
    private AnnihilationModel.Mode syncedMode = AnnihilationModel.Mode.ROTATION;
    // What the last update said, on clients.
    private boolean clientBurning;
    private AnnihilationModel.Mode clientMode = AnnihilationModel.Mode.ROTATION;
    private int clientBufferMax = 1;
    private int clientSourceMax = 1;

    public AnnihilationGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.fieldBuffer = new FeBuffer(PenningTrapModel.bufferCapacity(AnnihilationModel.CHAMBER), this::setChanged);
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        int size = AnnihilationModel.sourceCapacity(p);
        this.source = new FeSource(size, size, this::setChanged);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        modeOption = new ScrollOptionBehaviour<>(GeneratorMode.class, Component.translatable(K + "mode"), this, new GeneratorPanelBox());
        behaviours.add(modeOption);
    }

    // ------------------------------------------------------------------ what the renderer and goggles read

    /** The mode set on the panel or with the wrench. */
    public AnnihilationModel.Mode mode() {
        return modeOption == null ? AnnihilationModel.Mode.ROTATION : modeOption.get().model();
    }

    /** The mode it runs in: {@link AnnihilationModel.Mode#ROTATION} when Forge Energy is disabled. */
    public AnnihilationModel.Mode effectiveMode() {
        return level != null && level.isClientSide ? clientMode : mode().effective(AcceleratorConfig.generator());
    }

    @Override
    public double stored() {
        return stored;
    }

    @Override
    public double capacity() {
        return AnnihilationModel.CHAMBER_NG;
    }

    /** The chamber holds its own field with the burn (it burnt last tick). */
    public boolean lit() {
        return litTicks > 0;
    }

    /** Anything burnt on the last tick, the standby burn included. */
    public boolean burning() {
        return level != null && level.isClientSide ? clientBurning : output.burning();
    }

    /** Share of the rated output delivered on the last tick, 0..1 (the throttle less any shortfall). */
    public double outputFraction() {
        return output.fraction();
    }

    /** The throttle from the redstone signal, 0..1; 0 is the standby burn. */
    public double throttle() {
        return throttle;
    }

    /** Burning on the standby share only: fuelled, but the throttle is at zero. */
    public boolean standby() {
        return burning() && throttle <= 0;
    }

    /** Burning, but short of antimatter for the throttle asked: the supply cannot keep up. */
    public boolean shortSupply() {
        return burning() && throttle > 0 && output.fraction() < throttle - 0.01;
    }

    /** A cold chamber holding antimatter without the energy for its field: it is quenching or about to. */
    public boolean fieldFailing() {
        return litTicks <= 0 && PenningTrapModel.loaded(stored) && (chamberState == TrapState.QUENCHING || field < 0.999f);
    }

    /** Share of its demand the chamber's field got last tick, 0..1. */
    public float fieldStrength() {
        return field;
    }

    public TrapState chamberState() {
        return chamberState;
    }

    public int quenchTicksLeft() {
        return quenchLeft < 0 ? AnnihilationModel.CHAMBER.quenchTicks() : quenchLeft;
    }

    /** Forge Energy made on the last tick, FE. */
    public int feMade() {
        return feMade;
    }

    /** Stress capacity offered now, SU (per RPM times the speed). */
    public double stressCapacity() {
        return output.capacity() * Math.abs(generatedSpeed);
    }

    /** Antiprotons burnt per minute at the last tick's rate, ng. */
    public double burnPerMinute() {
        return output.burnt() * 1200.0;
    }

    /** The chamber's field buffer (cables push into it; the generator's own output tops it up). */
    public FeBuffer fieldBuffer() {
        return fieldBuffer;
    }

    /** Where its Forge Energy waits to be pushed out or pulled by cables. */
    public FeSource source() {
        return source;
    }

    public int fieldBufferMax() {
        return level != null && level.isClientSide ? clientBufferMax : fieldBuffer.getMaxEnergyStored();
    }

    public int sourceMax() {
        return level != null && level.isClientSide ? clientSourceMax : source.getMaxEnergyStored();
    }

    /** What cables see on every side: they push into the field buffer and pull from the source. */
    public IEnergyStorage energy() {
        return terminals;
    }

    public IItemHandler port() {
        return port;
    }

    /** The vial in the port, or empty. */
    public ItemStack vial() {
        return port.getStackInSlot(0);
    }

    public int comparatorSignal() {
        return AnnihilationModel.comparator(output);
    }

    /** The middle of the chamber, where it annihilates. */
    public Vec3 centre() {
        return Vec3.atCenterOf(worldPosition);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            clientTick.accept(this);
            return;
        }
        if (annihilating) {
            return;
        }
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        PenningTrapModel.Params chamber = AnnihilationModel.CHAMBER;
        resizeStores(p, chamber);
        if (quenchLeft < 0) {
            quenchLeft = chamber.quenchTicks();
        }
        throttle = AnnihilationModel.throttle(level.getBestNeighborSignal(worldPosition));
        tickPort();
        if (annihilating) {
            return;
        }
        if (powered()) {
            refuel(chamber);
        }
        AnnihilationModel.Step step = AnnihilationModel.step(p, chamber, throttle, stored, mode(), fieldBuffer.getEnergyStored(),
                quenchLeft, litTicks);
        fieldBuffer.take(step.feUsed());
        output = step.output();
        stored = Math.max(0, stored - output.burnt());
        if (!PenningTrapModel.loaded(stored)) {
            stored = 0;
        }
        TrapState before = chamberState;
        quenchLeft = step.quenchTicksLeft();
        litTicks = step.litTicks();
        field = step.field();
        chamberState = step.state();
        if (chamberState == TrapState.QUENCHING && before != TrapState.QUENCHING) {
            level.playSound(null, worldPosition, AllNuclearSounds.TRAP_QUENCH.get(), SoundSource.BLOCKS, 2.0f, 1.2f);
        }
        if (step.annihilate()) {
            annihilate(0, true);
            return;
        }
        feMade = output.energyFe();
        int self = AnnihilationModel.selfFeed(feMade, fieldBuffer.getMaxEnergyStored() - fieldBuffer.getEnergyStored());
        if (self > 0) {
            fieldBuffer.set(fieldBuffer.getEnergyStored() + self);
        }
        source.generate(feMade - self);
        source.pushFairly(level, worldPosition);
        refreshGeneration(p);
        updateComparator();
        if (output.fraction() > 0 && level.getGameTime() % AWARD_INTERVAL == 0) {
            NuclearAdvancements.awardNearby(level, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.ANNIHILATION_POWER);
        }
        sync(before);
    }

    private void resizeStores(AnnihilationModel.Params p, PenningTrapModel.Params chamber) {
        int buffer = PenningTrapModel.bufferCapacity(chamber);
        if (buffer != fieldBuffer.getMaxEnergyStored()) {
            fieldBuffer.resize(buffer);
        }
        int size = AnnihilationModel.sourceCapacity(p);
        if (size != source.getMaxEnergyStored()) {
            source.resize(size);
        }
    }

    /** Tops the chamber up from the holders beside it that give antimatter (traps, target stations). */
    private void refuel(PenningTrapModel.Params chamber) {
        double want = AnnihilationModel.pull(chamber, stored, AnnihilationModel.PULL_PER_TICK);
        for (Direction side : Direction.values()) {
            if (want <= PenningTrapModel.EMPTY) {
                return;
            }
            BlockPos next = worldPosition.relative(side);
            if (!level.isLoaded(next)) {
                continue;
            }
            AntimatterHolder holder = AntimatterHolders.at(level, next);
            if (holder == null || holder == this || !holder.givesAntimatter()) {
                continue;
            }
            double got = holder.extract(want, false);
            if (got > 0) {
                stored += got;
                want -= got;
                setChanged();
            }
        }
    }

    /** Empties the vial in the port into the chamber while its field holds, and keeps the vial's battery up. */
    private void tickPort() {
        ItemStack vial = port.getStackInSlot(0);
        if (!(vial.getItem() instanceof AntimatterVialItem)) {
            return;
        }
        VialBattery.Params vp = AntimatterConfig.vial();
        boolean holds = powered();
        double inVial = AntimatterVialItem.antimatter(vial);
        if (holds && VialBattery.loaded(inVial)) {
            double moved = VialBattery.drainStep(vp, inVial, space());
            if (moved > 0) {
                inVial -= moved;
                stored += moved;
            }
        }
        int charge = VialBattery.tick(vp, AntimatterVialItem.charge(vial), inVial, holds);
        if (VialBattery.annihilates(charge, inVial)) {
            port.setStackInSlot(0, ItemStack.EMPTY);
            AntimatterAnnihilation.schedule((ServerLevel) level, centre(), inVial);
            return;
        }
        ItemStack updated = vial.copy();
        AntimatterVialItem.set(updated, inVial, charge);
        if (!ItemStack.isSameItemSameComponents(updated, vial)) {
            port.setStackInSlot(0, updated);
        }
    }

    /** Turns the shaft at the rated speed while it offers capacity; tells Create's network only when it matters. */
    private void refreshGeneration(AnnihilationModel.Params p) {
        float speed = AnnihilationModel.shaftSpeed(p, output);
        if (speed != 0 && getBlockState().hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)
                && getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING).getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            speed = -speed;
        }
        float capacity = (float) output.capacity();
        if (speed != generatedSpeed) {
            generatedSpeed = speed;
            generatedCapacity = capacity;
            updateGeneratedRotation();
        } else if (AnnihilationModel.capacityChanged(generatedCapacity, capacity)) {
            generatedCapacity = capacity;
            if (hasNetwork()) {
                notifyStressCapacityChange(generatedCapacity);
            }
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = generatedCapacity;
        return generatedCapacity;
    }

    private void updateComparator() {
        int signal = comparatorSignal();
        if (signal != lastComparator) {
            lastComparator = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void sync(TrapState before) {
        boolean burning = output.burning();
        AnnihilationModel.Mode mode = effectiveMode();
        boolean changed = lit() != syncedLit || burning != syncedBurning || chamberState != syncedState || before != chamberState
                || portChanged || mode != syncedMode || PenningTrapModel.loaded(stored) != PenningTrapModel.loaded(syncedStored);
        boolean drifted = Math.abs(stored - syncedStored) > 0.01 || Math.abs(output.fraction() - syncedFraction) > 0.02
                || chamberState == TrapState.QUENCHING;
        long now = level.getGameTime();
        if (changed || drifted && now - lastSync >= (chamberState == TrapState.QUENCHING ? 2 : SYNC_INTERVAL)) {
            lastSync = now;
            syncedStored = stored;
            syncedFraction = output.fraction();
            syncedLit = lit();
            syncedBurning = burning;
            syncedState = chamberState;
            syncedMode = mode;
            portChanged = false;
            sendData();
        }
    }

    // ------------------------------------------------------------------ antimatter in

    /** Antimatter put in now is held: a lit chamber, or a cold one whose buffer can pay its field. */
    @Override
    public boolean powered() {
        return !annihilating && AnnihilationModel.powered(AnnihilationModel.CHAMBER, stored, fieldBuffer.getEnergyStored(), litTicks);
    }

    /**
     * Takes up to {@code ng} into the chamber. Without its field it takes it all the same and
     * annihilates at once, as a trap does.
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
            annihilate(taken, true);
            return taken;
        }
        stored += taken;
        setChanged();
        return taken;
    }

    /** It only burns antimatter: nothing comes back out. */
    @Override
    public double extract(double ng, boolean simulate) {
        return 0;
    }

    @Override
    public boolean givesAntimatter() {
        return false;
    }

    /**
     * It draws antimatter only while its chamber can hold it: conduits and target stations deliver
     * nothing to a cold chamber without energy, which would only annihilate it. Without this, a
     * generator that went out (a supply gap, its buffer then drained by the idle field) blew up on
     * the next delivery. {@link #insert} itself still annihilates what is forced in.
     */
    @Override
    public boolean acceptsAntimatter() {
        return powered();
    }

    /**
     * Everything in the chamber, plus {@code extra} just put in and a loaded vial in the port,
     * annihilates at the end of the tick; a real blast takes the generator with it.
     *
     * @param clearBlock remove the generator's block with the blast (not when it is being broken anyway)
     */
    public void annihilate(double extra, boolean clearBlock) {
        if (annihilating || !(level instanceof ServerLevel server)) {
            return;
        }
        double total = stored + Math.max(0, extra) + AntimatterVialItem.antimatter(port.getStackInSlot(0));
        boolean explodes = AnnihilationBlastModel.explodes(AntimatterConfig.blast(), total);
        annihilating = explodes;
        stored = 0;
        litTicks = 0;
        chamberState = TrapState.OFFLINE;
        if (AntimatterVialItem.loaded(port.getStackInSlot(0))) {
            port.setStackInSlot(0, ItemStack.EMPTY);
        }
        AntimatterAnnihilation.schedule(server, centre(), total, explodes && clearBlock ? List.of(worldPosition) : List.of());
        setChanged();
    }

    /** Sets the chamber's charge outright without any check (game tests). */
    public void setStored(double ng) {
        stored = Math.max(0, Math.min(capacity(), ng));
        setChanged();
        sendData();
    }

    /** Fills the field buffer as if a cable had (game tests that start a generator on its first tick). */
    public void primeField() {
        fieldBuffer.set(fieldBuffer.getMaxEnergyStored());
    }

    /** Sets the mode outright (game tests, Ponder). */
    public void setMode(AnnihilationModel.Mode mode) {
        if (modeOption != null) {
            modeOption.setValue(GeneratorMode.of(mode).ordinal());
        }
        setChanged();
    }

    /**
     * Shows a running generator in a Ponder scene: its world never runs the physics, so what is set
     * here stays until the next call. Not for a real world.
     */
    public void preview(double ng, double fraction, boolean burning, AnnihilationModel.Mode mode) {
        stored = Math.max(0, Math.min(capacity(), ng));
        litTicks = burning ? AnnihilationModel.LIT_GRACE_TICKS : 0;
        clientBurning = burning;
        clientMode = mode;
        double share = Math.max(0, Math.min(1, fraction));
        output = new AnnihilationModel.Output(burning ? 1.0e-4 : 0, 0, 0, share);
        throttle = share;
        field = 1f;
        chamberState = burning ? TrapState.STABLE : TrapState.OFFLINE;
        setMode(mode);
    }

    @Override
    public void destroy() {
        if (org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly.isMoving()) {
            // Carried to or from a ship: the copy at the new place keeps everything.
            super.destroy();
            return;
        }
        super.destroy();
        if (level == null || level.isClientSide || annihilating) {
            return;
        }
        ItemStack vial = port.getStackInSlot(0);
        if (PenningTrapModel.loaded(stored) || AntimatterVialItem.loaded(vial)) {
            annihilate(0, false);
        } else if (!vial.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, vial);
        }
    }

    // ------------------------------------------------------------------ by hand

    /** Puts one vial from the player's hand into an empty port. */
    public void insertVial(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!port.getStackInSlot(0).isEmpty()) {
            player.displayClientMessage(Component.translatable("create_nuclearindustry.penning_trap.port_full"), true);
            return;
        }
        port.setStackInSlot(0, held.copyWithCount(1));
        if (!player.isCreative()) {
            held.shrink(1);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    /** Hands the vial in the port to the player, or tells them how the generator is doing. */
    public void takeVial(Player player) {
        ItemStack vial = port.getStackInSlot(0);
        if (vial.isEmpty()) {
            player.displayClientMessage(Component.translatable(K + "status_line", fmt(stored, 3),
                    Component.translatable(statusKey()), fmt(output.fraction() * 100, 0)), true);
            return;
        }
        port.setStackInSlot(0, ItemStack.EMPTY);
        if (!player.getInventory().add(vial)) {
            player.drop(vial, false);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    /** The wrench on the top panel: next mode (only rotation while Forge Energy is disabled). */
    public void cycleMode(@Nullable Player player) {
        AnnihilationModel.Mode next = mode().next(AcceleratorConfig.generator());
        setMode(next);
        sendData();
        if (player != null) {
            player.displayClientMessage(Component.translatable(K + "mode_set", Component.translatable(GeneratorMode.langKey(next))), true);
        }
    }

    /** One word for what the generator is doing: running, standby, cold, no_field or quenching. */
    public String statusId() {
        if (chamberState == TrapState.QUENCHING) {
            return "quenching";
        }
        if (fieldFailing()) {
            return "no_field";
        }
        if (burning()) {
            return throttle <= 0 || output.fraction() <= 0 ? "standby" : shortSupply() ? "short" : "running";
        }
        return "cold";
    }

    public String statusKey() {
        return K + "status." + statusId();
    }

    private static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    // ------------------------------------------------------------------ control room

    @Override
    public TargetKind plantKind() {
        return TargetKind.ANNIHILATION_GENERATOR;
    }

    @Override
    public boolean plantFormed() {
        return true;
    }

    @Override
    public MonitorReading plantSample() {
        float[] v = new float[MonitorReading.GENERATOR_VALUES];
        AnnihilationModel.Params p = AcceleratorConfig.generator();
        v[MonitorReading.G_STORED] = (float) stored;
        v[MonitorReading.G_CHAMBER_FILL] = (float) (stored / AnnihilationModel.CHAMBER_NG);
        v[MonitorReading.G_THROTTLE] = (float) throttle;
        v[MonitorReading.G_OUTPUT] = (float) output.fraction();
        v[MonitorReading.G_MODE] = effectiveMode().ordinal();
        v[MonitorReading.G_FE] = feMade;
        v[MonitorReading.G_STRESS] = (float) stressCapacity();
        v[MonitorReading.G_BURN] = (float) burnPerMinute();
        v[MonitorReading.G_LIT] = lit() ? 1 : 0;
        v[MonitorReading.G_BUFFER] = fieldBuffer.getEnergyStored() / (float) Math.max(1, fieldBuffer.getMaxEnergyStored());
        v[MonitorReading.G_STATE] = chamberState.ordinal();
        v[MonitorReading.G_RATED_FE] = (float) p.ratedEnergy();
        return new MonitorReading(TargetKind.ANNIHILATION_GENERATOR, statusKey(), v, v[MonitorReading.G_OUTPUT],
                v[MonitorReading.G_CHAMBER_FILL]);
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AnnihilationGeneratorGoggles.add(this, tooltip);
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Stored", stored);
        tag.putInt("LitTicks", litTicks);
        tag.putInt("QuenchLeft", quenchLeft);
        tag.putFloat("Field", field);
        tag.putInt("ChamberState", chamberState.ordinal());
        tag.putInt("FieldEnergy", fieldBuffer.getEnergyStored());
        tag.putInt("SourceEnergy", source.getEnergyStored());
        tag.put("Port", port.serializeNBT(registries));
        tag.putString("Mode", mode().name());
        tag.putFloat("GenSpeed", generatedSpeed);
        tag.putFloat("GenCapacity", generatedCapacity);
        if (clientPacket) {
            tag.putDouble("Throttle", throttle);
            tag.putDouble("Fraction", output.fraction());
            tag.putBoolean("Burning", output.burning());
            tag.putInt("FeMade", feMade);
            tag.putInt("EffectiveMode", effectiveMode().ordinal());
            tag.putInt("FieldMax", fieldBuffer.getMaxEnergyStored());
            tag.putInt("SourceMax", source.getMaxEnergyStored());
            tag.putDouble("Capacity", output.capacity());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = Math.max(0, tag.getDouble("Stored"));
        litTicks = tag.contains("LitTicks") ? tag.getInt("LitTicks") : AnnihilationModel.litTicksFromLegacy(tag.getBoolean("Lit"));
        quenchLeft = tag.contains("QuenchLeft") ? tag.getInt("QuenchLeft") : -1;
        field = tag.getFloat("Field");
        chamberState = TrapState.byOrdinal(tag.getInt("ChamberState"));
        if (clientPacket && tag.getInt("FieldMax") > 0) {
            fieldBuffer.resize(tag.getInt("FieldMax"));
            source.resize(Math.max(1, tag.getInt("SourceMax")));
        }
        fieldBuffer.set(tag.getInt("FieldEnergy"));
        source.set(tag.getInt("SourceEnergy"));
        port.deserializeNBT(registries, tag.getCompound("Port"));
        if (tag.contains("Mode") && modeOption != null && !clientPacket) {
            modeOption.setValue(GeneratorMode.of(AnnihilationModel.Mode.byName(tag.getString("Mode"))).ordinal());
        }
        generatedSpeed = tag.getFloat("GenSpeed");
        generatedCapacity = tag.getFloat("GenCapacity");
        if (clientPacket) {
            throttle = tag.getDouble("Throttle");
            double fraction = tag.getDouble("Fraction");
            clientBurning = tag.getBoolean("Burning");
            output = new AnnihilationModel.Output(clientBurning ? 1.0e-9 : 0, tag.getDouble("Capacity"), 0, fraction);
            feMade = tag.getInt("FeMade");
            AnnihilationModel.Mode[] modes = AnnihilationModel.Mode.values();
            int effective = tag.getInt("EffectiveMode");
            clientMode = effective >= 0 && effective < modes.length ? modes[effective] : AnnihilationModel.Mode.ROTATION;
            clientBufferMax = Math.max(1, tag.getInt("FieldMax"));
            clientSourceMax = Math.max(1, tag.getInt("SourceMax"));
        }
    }

    /** Cables push into the chamber's field buffer and pull from the generator's source. */
    private final class Terminals implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return fieldBuffer.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return source.extractEnergy(toExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, (long) source.getEnergyStored() + fieldBuffer.getEnergyStored());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, (long) source.getMaxEnergyStored() + fieldBuffer.getMaxEnergyStored());
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
