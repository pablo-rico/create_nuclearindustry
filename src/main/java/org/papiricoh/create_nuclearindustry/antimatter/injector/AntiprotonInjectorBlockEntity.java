package org.papiricoh.create_nuclearindustry.antimatter.injector;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
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
import org.papiricoh.create_nuclearindustry.accelerator.energy.FeBuffer;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationBlastModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.InjectorModel;
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
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakAuxiliary;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

import java.util.List;
import java.util.function.Consumer;

/**
 * The tokamak's antiproton injector: a small Penning trap on a shell port with a beam nozzle into
 * the chamber. It keeps a few doses ({@code antimatter.ignitionDose} each) topped up from the holders
 * beside it and from a vial in its port; while its tokamak is {@link TokamakPhase#HEATING} a plasma
 * that is held well enough and dense enough, it fires one dose, which annihilates in the plasma and
 * lifts it past ignition ({@link PlasmaModel#antiprotonIgnite()}). {@link InjectorModel} decides when;
 * each shot takes {@code antimatter.injector.shotFe} from its buffer and starts a cooldown.
 *
 * <p>Its chamber is a trap: without Forge Energy it quenches and annihilates what it holds, and what
 * goes in while its field is down annihilates at once. Armed automatically, by redstone, or safe
 * (the wrench steps through them).
 */
public class AntiprotonInjectorBlockEntity extends SmartBlockEntity implements TokamakAuxiliary, AntimatterHolder,
        IHaveGoggleInformation {
    /** Client work each injector hands off every tick (its nozzle flash, the dose cloud), set by the client setup. */
    public static Consumer<AntiprotonInjectorBlockEntity> clientTick = injector -> {};
    /** Antimatter it pulls into its chamber per tick at most, ng. */
    public static final double PULL_PER_TICK = 0.1;
    private static final int LOOKUP_INTERVAL = 20;
    private static final int SYNC_INTERVAL = 10;
    static final String K = "create_nuclearindustry.antiproton_injector.";

    private double stored;
    private float field;
    private TrapState state = TrapState.OFFLINE;
    private int quenchLeft = -1;
    private InjectorModel.Arming arming = InjectorModel.Arming.AUTOMATIC;
    private boolean redstone;
    private int cooldown;
    private InjectorModel.Verdict verdict = InjectorModel.Verdict.WAITING;
    private long lastShot = Long.MIN_VALUE;
    private int shots;
    private int ignitions;
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
            return AntimatterVialItem.loaded(getStackInSlot(slot)) ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            portChanged = true;
            setChanged();
        }
    };
    @Nullable
    private BlockPos controllerPos;
    private long nextLookup;
    private boolean inTokamak;
    private boolean annihilating;
    private boolean portChanged;
    private int lastComparator = -1;
    private long lastSync;
    private double syncedStored;
    private TrapState syncedState = TrapState.OFFLINE;
    private InjectorModel.Verdict syncedVerdict = InjectorModel.Verdict.WAITING;
    private boolean syncedArmed;
    // What the last update said, on clients.
    private double clientCapacity = InjectorModel.CHAMBER.capacity();
    private double clientDose = InjectorModel.Params.DEFAULT.dose();
    private int clientShotFe = InjectorModel.Params.DEFAULT.shotFe();
    private boolean clientArmed;
    private int clientEnergyMax = 1;

    public AntiprotonInjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        InjectorModel.Params p = AntimatterConfig.injector();
        this.energy = new FeBuffer(InjectorModel.bufferCapacity(p, InjectorModel.chamber(p)), this::setChanged);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    // ------------------------------------------------------------------ what the renderer and goggles read

    public InjectorModel.Params params() {
        return AntimatterConfig.injector();
    }

    @Override
    public double stored() {
        return stored;
    }

    @Override
    public double capacity() {
        return level != null && level.isClientSide ? clientCapacity : InjectorModel.chamber(params()).capacity();
    }

    /** Antiprotons one shot spends, ng (the server's, on clients). */
    public double dose() {
        return level != null && level.isClientSide ? clientDose : params().dose();
    }

    /** Forge Energy one shot takes, FE. */
    public int shotFe() {
        return level != null && level.isClientSide ? clientShotFe : params().shotFe();
    }

    /** Whole doses in the chamber. */
    public int doses() {
        double dose = dose();
        return dose <= 0 ? Integer.MAX_VALUE : (int) Math.floor(Math.max(0, stored) / dose + 1.0e-9);
    }

    public InjectorModel.Arming arming() {
        return arming;
    }

    /** Armed now: automatic, or redstone with a signal. */
    public boolean armed() {
        return level != null && level.isClientSide ? clientArmed : InjectorModel.armed(arming, redstone);
    }

    /** Why it fired or waited on the last tick. */
    public InjectorModel.Verdict verdict() {
        return verdict;
    }

    /** Armed, powered, with a dose and off cooldown: it fires as soon as the plasma takes it. */
    public boolean ready() {
        return verdict.armedAndLoaded();
    }

    public int cooldownTicks() {
        return cooldown;
    }

    public boolean coolingDown() {
        return cooldown > 0;
    }

    /** Game ticks since the last shot (the nozzle flash), or {@link Long#MAX_VALUE} before the first. */
    public long ticksSinceShot() {
        return level == null || lastShot == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0, level.getGameTime() - lastShot);
    }

    /** Shots fired since it was placed (game tests). */
    public int shots() {
        return shots;
    }

    /** Shots the plasma took (game tests). */
    public int ignitions() {
        return ignitions;
    }

    /** It is a part of a formed tokamak, as the controller last said. */
    public boolean inTokamak() {
        return inTokamak;
    }

    public TrapState state() {
        return state;
    }

    public float fieldStrength() {
        return field;
    }

    public int quenchTicksLeft() {
        return quenchLeft < 0 ? InjectorModel.CHAMBER.quenchTicks() : quenchLeft;
    }

    public FeBuffer buffer() {
        return energy;
    }

    public int bufferMax() {
        return level != null && level.isClientSide ? clientEnergyMax : energy.getMaxEnergyStored();
    }

    public IEnergyStorage energy() {
        return energy;
    }

    public IItemHandler port() {
        return port;
    }

    public ItemStack vial() {
        return port.getStackInSlot(0);
    }

    public int comparatorSignal() {
        return InjectorModel.comparator(params(), stored);
    }

    /** The outer face; the nozzle looks the other way, into the plasma. */
    public Direction facing() {
        BlockState blockState = getBlockState();
        return blockState.hasProperty(AntiprotonInjectorBlock.FACING) ? blockState.getValue(AntiprotonInjectorBlock.FACING) : Direction.NORTH;
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
        InjectorModel.Params p = params();
        PenningTrapModel.Params chamber = InjectorModel.chamber(p);
        int capacity = InjectorModel.bufferCapacity(p, chamber);
        if (capacity != energy.getMaxEnergyStored()) {
            energy.resize(capacity);
        }
        if (quenchLeft < 0) {
            quenchLeft = chamber.quenchTicks();
        }
        tickPort(chamber);
        if (annihilating) {
            return;
        }
        if (powered()) {
            refuel(chamber);
        }
        PenningTrapModel.Step step = PenningTrapModel.tick(chamber, stored, energy.getEnergyStored(), quenchLeft);
        energy.take(step.feUsed());
        TrapState before = state;
        field = step.field();
        state = step.state();
        quenchLeft = step.quenchTicksLeft();
        if (state == TrapState.QUENCHING && before != TrapState.QUENCHING) {
            level.playSound(null, worldPosition, AllNuclearSounds.TRAP_QUENCH.get(), SoundSource.BLOCKS, 2.0f, 1.1f);
        }
        if (step.annihilate()) {
            annihilate(0, true);
            return;
        }
        redstone = level.hasNeighborSignal(worldPosition);
        FusionReactorBlockEntity tokamak = controller();
        InjectorModel.Plasma plasma = plasmaOf(tokamak);
        verdict = InjectorModel.verdict(p, InjectorModel.armed(arming, redstone), stored, energy.getEnergyStored(),
                PenningTrapModel.holds(field), cooldown, plasma);
        if (verdict.fires() && tokamak != null) {
            fire(p, tokamak);
        } else {
            cooldown = InjectorModel.cooldownTick(cooldown);
        }
        updateComparator();
        sync(before);
    }

    /** One dose into the plasma. */
    private void fire(InjectorModel.Params p, FusionReactorBlockEntity tokamak) {
        stored = Math.max(0, stored - InjectorModel.spend(p, stored));
        if (!PenningTrapModel.loaded(stored)) {
            stored = 0;
        }
        energy.take(p.shotFe());
        cooldown = InjectorModel.cooldownAfterShot(p);
        lastShot = level.getGameTime();
        shots++;
        boolean took = tokamak.getPhysics().model().antiprotonIgnite();
        level.playSound(null, worldPosition, AllNuclearSounds.ANTIPROTON_SHOT.get(), SoundSource.BLOCKS, 2.0f, 1.0f);
        beam();
        if (took) {
            ignitions++;
            NuclearAdvancements.awardNearby(level, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.ANNIHILATION_IGNITION);
            NuclearAdvancements.awardNearby(level, tokamak.getBlockPos(), NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.ANNIHILATION_IGNITION);
        }
        setChanged();
        sendData();
    }

    /** Sparks along the beam, from the nozzle into the chamber. */
    private void beam() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Direction into = facing().getOpposite();
        Vec3 start = Vec3.atCenterOf(worldPosition);
        for (int i = 1; i <= 4; i++) {
            Vec3 at = start.add(into.getStepX() * i * 0.6, into.getStepY() * i * 0.6, into.getStepZ() * i * 0.6);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.2);
            server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.05);
        }
    }

    /** The tokamak whose shell this injector is a part of: the last one found, looked up again now and then. */
    @Nullable
    private FusionReactorBlockEntity controller() {
        if (controllerPos != null && level.isLoaded(controllerPos)
                && level.getBlockEntity(controllerPos) instanceof FusionReactorBlockEntity tokamak && holdsThis(tokamak)) {
            return tokamak;
        }
        long now = level.getGameTime();
        if (now < nextLookup) {
            return null;
        }
        nextLookup = now + LOOKUP_INTERVAL;
        controllerPos = null;
        for (MultiblockController candidate : MultiblockRegistry.affecting(level, worldPosition)) {
            if (candidate instanceof FusionReactorBlockEntity tokamak && holdsThis(tokamak)) {
                controllerPos = tokamak.getBlockPos().immutable();
                return tokamak;
            }
        }
        return null;
    }

    private boolean holdsThis(FusionReactorBlockEntity tokamak) {
        return !tokamak.isRemoved() && tokamak.getStructure().map(structure -> structure.containsPart(worldPosition)).orElse(false);
    }

    /** What the injector sees of its tokamak's plasma. */
    private static InjectorModel.Plasma plasmaOf(@Nullable FusionReactorBlockEntity tokamak) {
        if (tokamak == null || !tokamak.isFormed() || tokamak.getStatus() != FusionReactorBlockEntity.Status.FORMED) {
            return InjectorModel.Plasma.NONE;
        }
        PlasmaModel model = tokamak.getPhysics().model();
        boolean heating = tokamak.phase() == TokamakPhase.HEATING && model.hasPlasma();
        return new InjectorModel.Plasma(heating, model.getConfinement(), model.getDensity(), model.getTemperature());
    }

    /** Tops the chamber up from the holders beside it that give antimatter. */
    private void refuel(PenningTrapModel.Params chamber) {
        double want = InjectorModel.pull(chamber, stored, PULL_PER_TICK);
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

    /** Empties the vial in the port into the chamber while its field holds, and keeps its battery up. */
    private void tickPort(PenningTrapModel.Params chamber) {
        ItemStack vial = port.getStackInSlot(0);
        if (!(vial.getItem() instanceof AntimatterVialItem)) {
            return;
        }
        VialBattery.Params vp = AntimatterConfig.vial();
        boolean holds = powered();
        double inVial = AntimatterVialItem.antimatter(vial);
        if (holds && VialBattery.loaded(inVial)) {
            double moved = VialBattery.drainStep(vp, inVial, PenningTrapModel.space(chamber, stored));
            if (moved > 0) {
                inVial -= moved;
                stored += moved;
            }
        }
        int charge = VialBattery.tick(vp, AntimatterVialItem.charge(vial), inVial, holds);
        if (VialBattery.annihilates(charge, inVial)) {
            port.setStackInSlot(0, ItemStack.EMPTY);
            AntimatterAnnihilation.schedule((ServerLevel) level, Vec3.atCenterOf(worldPosition), inVial);
            return;
        }
        ItemStack updated = vial.copy();
        AntimatterVialItem.set(updated, inVial, charge);
        if (!ItemStack.isSameItemSameComponents(updated, vial)) {
            port.setStackInSlot(0, updated);
        }
    }

    private void updateComparator() {
        int signal = comparatorSignal();
        if (signal != lastComparator) {
            lastComparator = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void sync(TrapState before) {
        boolean armedNow = InjectorModel.armed(arming, redstone);
        boolean changed = state != syncedState || before != state || verdict != syncedVerdict || armedNow != syncedArmed
                || portChanged || PenningTrapModel.loaded(stored) != PenningTrapModel.loaded(syncedStored);
        boolean drifted = Math.abs(stored - syncedStored) > 0.01 || state == TrapState.QUENCHING;
        long now = level.getGameTime();
        if (changed || drifted && now - lastSync >= (state == TrapState.QUENCHING ? 2 : SYNC_INTERVAL)) {
            lastSync = now;
            syncedStored = stored;
            syncedState = state;
            syncedVerdict = verdict;
            syncedArmed = armedNow;
            portChanged = false;
            sendData();
        }
    }

    // ------------------------------------------------------------------ the tokamak

    /** It adds no heating of its own: it acts on the plasma directly when it fires. */
    @Override
    public void contribute(TokamakSupply.Builder supply) {}

    @Override
    public void observe(TokamakReadings readings) {
        boolean formed = readings.formed();
        if (formed && controllerPos == null) {
            nextLookup = 0;
        }
        if (formed != inTokamak) {
            inTokamak = formed;
            sendData();
        }
    }

    // ------------------------------------------------------------------ antimatter in

    @Override
    public boolean powered() {
        return !annihilating && PenningTrapModel.holds(field);
    }

    /** Takes up to {@code ng}; with its field down it takes it all the same and annihilates at once. */
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

    /** What goes in is for the plasma: nothing comes back out. */
    @Override
    public double extract(double ng, boolean simulate) {
        return 0;
    }

    @Override
    public boolean givesAntimatter() {
        return false;
    }

    /**
     * It draws antimatter only while its field holds: conduits and target stations deliver nothing to
     * an injector without energy (say, one just built into a shell beside a live conduit), which would
     * annihilate it in the tokamak's wall. {@link #insert} itself still annihilates what is forced in.
     */
    @Override
    public boolean acceptsAntimatter() {
        return powered();
    }

    /**
     * Everything held, plus {@code extra} and a loaded vial, annihilates at the end of the tick; a
     * real blast takes the injector with it.
     */
    public void annihilate(double extra, boolean clearBlock) {
        if (annihilating || !(level instanceof ServerLevel server)) {
            return;
        }
        double total = stored + Math.max(0, extra) + AntimatterVialItem.antimatter(port.getStackInSlot(0));
        boolean explodes = AnnihilationBlastModel.explodes(AntimatterConfig.blast(), total);
        annihilating = explodes;
        stored = 0;
        state = TrapState.OFFLINE;
        if (AntimatterVialItem.loaded(port.getStackInSlot(0))) {
            port.setStackInSlot(0, ItemStack.EMPTY);
        }
        AntimatterAnnihilation.schedule(server, Vec3.atCenterOf(worldPosition), total,
                explodes && clearBlock ? List.of(worldPosition) : List.of());
        setChanged();
    }

    @Override
    public void destroy() {
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

    // ------------------------------------------------------------------ test hooks

    /** Sets the chamber's charge outright without any check (game tests). */
    public void setStored(double ng) {
        stored = Math.max(0, ng);
        setChanged();
        sendData();
    }

    /** Fills the buffer and raises the field as if it had been fed (game tests that insert on the first tick). */
    public void primeField() {
        energy.set(energy.getMaxEnergyStored());
        field = 1f;
        state = TrapState.STABLE;
        quenchLeft = InjectorModel.CHAMBER.quenchTicks();
    }

    public void setArming(InjectorModel.Arming arming) {
        this.arming = arming;
        setChanged();
        sendData();
    }

    /** Ends the cooldown at once (game tests). */
    public void clearCooldown() {
        cooldown = 0;
    }

    /**
     * Shows an injector's lamps, dose and last shot in a Ponder scene: its world never runs the
     * physics, so what is set here stays. Not for a real world.
     */
    public void preview(double ng, boolean armedShown, InjectorModel.Verdict shown, boolean justFired) {
        stored = Math.max(0, ng);
        clientArmed = armedShown;
        verdict = shown;
        field = 1f;
        state = TrapState.STABLE;
        if (justFired && level != null) {
            lastShot = level.getGameTime();
        }
    }

    // ------------------------------------------------------------------ by hand

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

    /** Hands the vial in the port to the player, or tells them what the injector is waiting for. */
    public void takeVial(Player player) {
        ItemStack vial = port.getStackInSlot(0);
        if (vial.isEmpty()) {
            player.displayClientMessage(Component.translatable(K + "status_line", doses(),
                    Component.translatable(verdictKey(verdict)), Component.translatable(armingKey(arming))), true);
            return;
        }
        port.setStackInSlot(0, ItemStack.EMPTY);
        if (!player.getInventory().add(vial)) {
            player.drop(vial, false);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    /** The wrench: automatic, redstone, safe. */
    public void cycleArming(@Nullable Player player) {
        arming = arming.next();
        setChanged();
        sendData();
        if (player != null) {
            player.displayClientMessage(Component.translatable(K + "arming_set", Component.translatable(armingKey(arming))), true);
        }
    }

    public static String verdictKey(InjectorModel.Verdict verdict) {
        return K + "verdict." + verdict.id();
    }

    public static String armingKey(InjectorModel.Arming arming) {
        return K + "arming." + arming.id();
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AntiprotonInjectorGoggles.add(this, tooltip);
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
        tag.putString("Arming", arming.name());
        tag.putInt("Cooldown", cooldown);
        tag.putInt("Verdict", verdict.ordinal());
        tag.putLong("LastShot", lastShot);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Port", port.serializeNBT(registries));
        tag.putBoolean("InTokamak", inTokamak);
        if (clientPacket) {
            InjectorModel.Params p = params();
            tag.putDouble("Capacity", InjectorModel.chamber(p).capacity());
            tag.putDouble("Dose", p.dose());
            tag.putInt("ShotFe", p.shotFe());
            tag.putBoolean("Armed", InjectorModel.armed(arming, redstone));
            tag.putInt("EnergyMax", energy.getMaxEnergyStored());
        } else {
            tag.putInt("Shots", shots);
            tag.putInt("Ignitions", ignitions);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = Math.max(0, tag.getDouble("Stored"));
        field = tag.getFloat("Field");
        state = TrapState.byOrdinal(tag.getInt("State"));
        quenchLeft = tag.contains("QuenchLeft") ? tag.getInt("QuenchLeft") : -1;
        arming = InjectorModel.Arming.byName(tag.getString("Arming"));
        cooldown = Math.max(0, tag.getInt("Cooldown"));
        verdict = InjectorModel.Verdict.byOrdinal(tag.getInt("Verdict"));
        lastShot = tag.contains("LastShot") ? tag.getLong("LastShot") : Long.MIN_VALUE;
        if (clientPacket && tag.getInt("EnergyMax") > 0) {
            energy.resize(tag.getInt("EnergyMax"));
        }
        energy.set(tag.getInt("Energy"));
        port.deserializeNBT(registries, tag.getCompound("Port"));
        inTokamak = tag.getBoolean("InTokamak");
        if (clientPacket) {
            clientCapacity = tag.getDouble("Capacity");
            clientDose = tag.getDouble("Dose");
            clientShotFe = tag.getInt("ShotFe");
            clientArmed = tag.getBoolean("Armed");
            clientEnergyMax = Math.max(1, tag.getInt("EnergyMax"));
        } else {
            shots = tag.getInt("Shots");
            ignitions = tag.getInt("Ignitions");
        }
    }
}
