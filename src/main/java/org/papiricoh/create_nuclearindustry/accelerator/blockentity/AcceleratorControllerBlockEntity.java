package org.papiricoh.create_nuclearindustry.accelerator.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorHooks;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorAttachment;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorSupply;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTerminal;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.block.AcceleratorControllerBlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AcceleratorStructure;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AcceleratorValidator;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.RingSection;
import org.papiricoh.create_nuclearindustry.accelerator.physics.BeamBudget;
import org.papiricoh.create_nuclearindustry.accelerator.physics.BeamModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.BeamSequence;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.FusionConfig;
import org.papiricoh.create_nuclearindustry.fusion.physics.MagnetModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.VacuumModel;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Particle accelerator controller. Owns the beamline found by {@link AcceleratorValidator}, the
 * startup sequence, the beam pipe's vacuum, the magnets (and the superconducting parts' helium
 * loop) and the operator's setpoints. Attachments offer their share through
 * {@link AcceleratorAttachment}; the ion source gives the hydrogen; the terminal at the end of the
 * line takes the beam.
 *
 * <p>Each tick: the pumps set the pipe's pressure, the magnets follow their supplies, the
 * {@link BeamSequence} moves through its phases, and while the source makes beam the
 * {@link BeamBudget} works out what reaches the terminal and what is lost on the way.
 */
public class AcceleratorControllerBlockEntity extends BlockEntity implements IHaveGoggleInformation, MultiblockController {
    public static final int HELIUM_TANK_CAPACITY = 8_000;
    /** The pipe's vacuum: base pressure, beam limit, pump-down and outgassing (log10 Pa, decades per tick). */
    public static final VacuumModel.Params VACUUM = new VacuumModel.Params(-7.0, -5.0, 0.04, 1.0 / 60.0, 10.0, 0.0);
    /** Field change per tick of normal-conducting magnets, fraction of rated. */
    public static final double FIELD_RAMP = 0.02;
    /** Superconducting parts per cooling sector of the helium loop. */
    public static final int SC_PER_SECTOR = 8;
    private static final int SUPPLY_INTERVAL = 10;
    private static final int OBSERVE_INTERVAL = 10;
    private static final int SYNC_INTERVAL = 10;
    /** Above this the accelerator counts as a GeV machine for its advancement. */
    private static final double GEV = 1000.0;

    public enum Status { UNFORMED, FORMED, SUSPENDED }

    private Status status = Status.UNFORMED;
    private Optional<AcceleratorStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<AcceleratorStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of(AcceleratorValidator.NOT_FORMED), List.of());
    private final BeamSequence sequence = new BeamSequence();
    private final VacuumModel vacuum = new VacuumModel();
    private final MagnetModel magnets = new MagnetModel();
    private final MultiblockMessenger messenger = new MultiblockMessenger("Accelerator");
    private boolean pendingRevalidation = true;
    private int syncCounter;
    private double field;
    private double ringEnergy;
    private double energySetpoint = 1.0;
    private double currentSetpoint = 1.0;
    private boolean redstonePowered;
    private boolean firstBeamAwarded;
    private AcceleratorSupply supply = AcceleratorSupply.NONE;
    /** Replaces what the attachments offer; only game tests set it. */
    private AcceleratorSupply supplyOverride;
    private BeamBudget.Result budget = BeamBudget.Result.NONE;
    private double liquidHeliumBalance;
    private double warmHeliumBalance;

    private final FluidTank liquidHeliumTank = tank(HELIUM_TANK_CAPACITY, NuclearFluidHelper::isLiquidHelium);
    private final FluidTank warmHeliumTank = tank(HELIUM_TANK_CAPACITY, NuclearFluidHelper::isHelium);

    // Synced to clients for goggles, the screen and the renderer.
    private BeamReadings clientReadings = BeamReadings.NONE;
    private String clientValidation = AcceleratorValidator.NOT_FORMED;
    private List<BlockPos> clientInjector = List.of();
    private List<BlockPos> clientExtraction = List.of();
    private BlockPos clientRingCenter = BlockPos.ZERO;
    private int clientRingRadius;
    /** Set by a Ponder scene: the readings and paths are a preview, whatever side the Ponder world claims to be. */
    private boolean ponderPreview;

    public AcceleratorControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private FluidTank tank(int capacity, java.util.function.Predicate<FluidStack> validator) {
        return new FluidTank(capacity, validator) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(Level level, BlockPos pos, BlockState state, AcceleratorControllerBlockEntity entity) {
        if (entity.ponderPreview) {
            return;
        }
        if (level.isClientSide) {
            AcceleratorHooks.clientTick.accept(entity);
            return;
        }
        if (entity.pendingRevalidation) {
            entity.revalidate(false, null);
        }
        entity.serverTick();
        entity.updateBlockState();
        if (++entity.syncCounter >= SYNC_INTERVAL) {
            entity.syncCounter = 0;
            entity.markDirtyAndSync();
        }
    }

    private void serverTick() {
        if (structure.isEmpty()) {
            status = Status.UNFORMED;
            runUnformed();
            return;
        }
        AcceleratorStructure s = structure.get();
        if (!level.hasChunksAt(s.bounds().minX(), s.bounds().minZ(), s.bounds().maxX(), s.bounds().maxZ())) {
            status = Status.SUSPENDED;
            return;
        }
        status = Status.FORMED;
        if (supplyOverride != null) {
            supply = supplyOverride;
        } else if (level.getGameTime() % SUPPLY_INTERVAL == 0 || supply == AcceleratorSupply.NONE) {
            supply = collectSupply(s);
        }
        BeamModel.Params beam = AcceleratorConfig.beam();
        vacuum.tick(VACUUM, pumping(s), false);
        runMagnets(s);

        IonSourceBlockEntity source = source(s);
        double sourceDrive = supply.sourceDrive() * currentSetpoint;
        boolean gas = source != null && source.hasGas(AcceleratorConfig.hydrogenPerTick);
        boolean sourceOn = sequence.sourceOn();
        if (sequence.phase() == BeamPhase.INJECTION) {
            ringEnergy = BeamBudget.linacEnergy(beam, inputs(s, sourceDrive, gas ? 1 : 0, 0));
        } else if (sequence.phase() == BeamPhase.RAMP || sequence.phase() == BeamPhase.BEAM_ON) {
            ringEnergy = BeamModel.ramp(beam, ringEnergy, ringTarget(s), s.ring == null ? 1 : s.ring.cavities(), rfDrive(s));
        } else if (!sequence.phase().hasBeam()) {
            ringEnergy = 0;
        }
        budget = BeamBudget.compute(beam, inputs(s, sourceDrive, gas ? 1 : 0, ringEnergy), sourceOn);

        handle(sequence.tick(AcceleratorConfig.sequence(), conditions(s, gas)));

        if (sequence.sourceOn() && source != null) {
            source.draw(AcceleratorConfig.hydrogenPerTick * currentSetpoint);
        }
        if (sequence.delivering() && budget.current() > 0) {
            if (level.getBlockEntity(s.terminal()) instanceof BeamTerminal terminal) {
                terminal.receiveBeam(budget.energy(), budget.current(), budget.power());
            }
            if (!firstBeamAwarded) {
                firstBeamAwarded = true;
                NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), NuclearEvent.FIRST_BEAM);
            }
        }
        if (level.getGameTime() % OBSERVE_INTERVAL == 0) {
            BeamReadings readings = readings();
            forEachAttachment(s, attachment -> attachment.observe(readings));
        }
    }

    /** No structure: the pipe is open, the magnets lose their field and any sequence winds down at once. */
    private void runUnformed() {
        vacuum.vent();
        field = 0;
        ringEnergy = 0;
        magnets.dropField();
        magnets.tick(FusionConfig.magnetParams(), new MagnetModel.Inputs(1, 0, false, false, false, 0));
        budget = BeamBudget.Result.NONE;
        supply = AcceleratorSupply.NONE;
        if (sequence.phase() != BeamPhase.OFFLINE) {
            handle(sequence.trip(BeamTripCause.STRUCTURE));
            sequence.restore(BeamPhase.OFFLINE, BeamTripCause.STRUCTURE, 0, false);
        }
    }

    private BeamBudget.Inputs inputs(AcceleratorStructure s, double sourceDrive, double gas, double ringEnergy) {
        RingSection ring = s.ring;
        return new BeamBudget.Inputs(s.copperCavities(), activeSuperconductingCavities(s), s.injector.elements(),
                s.injector.count(BeamElementRole.QUADRUPOLE), rfDrive(s), energySetpoint, sourceDrive, gas,
                vacuum.getPressure(), effectiveMagnets(s), ring == null ? 0 : ring.radius(), ring == null ? 0 : ring.cells(),
                ring == null ? 0 : ring.count(BeamElementRole.QUADRUPOLE), ringEnergy,
                s.extraction == null ? 0 : s.extraction.elements(),
                s.extraction == null ? 0 : s.extraction.count(BeamElementRole.QUADRUPOLE));
    }

    private BeamSequence.Conditions conditions(AcceleratorStructure s, boolean gas) {
        BeamModel.Params beam = AcceleratorConfig.beam();
        boolean sc = s.superconducting() > 0;
        MagnetModel.Params magnetParams = FusionConfig.magnetParams();
        double fullLinac = BeamModel.linacEnergy(beam, s.copperCavities(), activeSuperconductingCavities(s), rfDrive(s));
        boolean injectionOk = s.ring == null || BeamModel.injectionOk(beam, fullLinac, ringMaxEnergy(s) / 1000.0);
        boolean atEnergy = s.ring == null || ringEnergy >= ringTarget(s) - 1.0e-6;
        return new BeamSequence.Conditions(true, VacuumModel.canReachPlasmaVacuum(VACUUM, pumping(s)),
                vacuum.atPlasmaVacuum(VACUUM), sc, liquidHeliumTank.getFluidAmount(), !sc || magnets.ready(),
                sc && magnets.quenching(magnetParams), fieldFraction(s), magnetPower(s), rfDrive(s), gas,
                supply.sourceDrive() > 0, interlocksClear(), s.ring != null, injectionOk, atEnergy, budget.lossFraction());
    }

    /** The magnets follow their supplies; superconducting ones go through the helium loop. */
    private void runMagnets(AcceleratorStructure s) {
        boolean energize = sequence.energize() && magnetPower(s) >= AcceleratorConfig.sequence().minimumMagnetPower();
        double target = energize ? 1 : 0;
        field = field < target ? Math.min(target, field + FIELD_RAMP) : Math.max(target, field - FIELD_RAMP);
        if (s.superconducting() == 0) {
            return;
        }
        int sectors = Math.max(1, (int) Math.ceil(s.superconducting() / (double) SC_PER_SECTOR));
        double used = magnets.tick(FusionConfig.magnetParams(), new MagnetModel.Inputs(sectors, target, sequence.cooling(),
                sequence.energize(), sequence.phase().hasBeam(), liquidHeliumTank.getFluidAmount() + liquidHeliumBalance));
        liquidHeliumBalance -= used;
        warmHeliumBalance += used;
        liquidHeliumBalance = settle(liquidHeliumTank, liquidHeliumBalance, AllNuclearFluids.LIQUID_HELIUM.get());
        warmHeliumBalance = settle(warmHeliumTank, warmHeliumBalance, AllNuclearFluids.HELIUM.get());
    }

    private static double settle(FluidTank tank, double balance, Fluid fluid) {
        if (balance <= -1) {
            int owed = (int) Math.floor(-balance);
            balance += tank.drain(owed, IFluidHandler.FluidAction.EXECUTE).getAmount();
            balance = Math.max(balance, -1);
        } else if (balance >= 1) {
            int held = (int) Math.floor(balance);
            tank.fill(new FluidStack(fluid, held), IFluidHandler.FluidAction.EXECUTE);
            balance -= held;
        }
        return balance;
    }

    private double fieldFraction(AcceleratorStructure s) {
        return s.superconducting() > 0 ? Math.min(field, magnets.getField()) : field;
    }

    private double effectiveMagnets(AcceleratorStructure s) {
        return Math.min(magnetPower(s), fieldFraction(s));
    }

    private double pumping(AcceleratorStructure s) {
        int needed = Math.max(1, (int) Math.ceil(s.cells() / (double) Math.max(1, AcceleratorConfig.cellsPerPump)));
        return supply.pumpSpeed() / needed;
    }

    private double magnetPower(AcceleratorStructure s) {
        double load = s.magnetLoad();
        return load <= 0 ? 1 : Math.min(1, supply.magnetCapacity() / load);
    }

    private double rfDemand(AcceleratorStructure s) {
        int copper = s.copperCavities() + (s.ring == null ? 0 : s.ring.count(BeamElementRole.RF_CAVITY));
        int sc = s.superconductingCavities() + (s.ring == null ? 0 : s.ring.count(BeamElementRole.SC_RF_CAVITY));
        return BeamModel.rfDemand(AcceleratorConfig.beam(), copper, sc);
    }

    private double rfDrive(AcceleratorStructure s) {
        return BeamModel.rfDrive(supply.rfPower(), rfDemand(s));
    }

    /** Superconducting cavities only accelerate while cold. */
    private int activeSuperconductingCavities(AcceleratorStructure s) {
        return s.superconducting() > 0 && !magnets.superconducting(FusionConfig.magnetParams()) ? 0 : s.superconductingCavities();
    }

    /** Top energy of the ring, MeV; 0 without one. */
    private double ringMaxEnergy(AcceleratorStructure s) {
        if (s.ring == null) {
            return 0;
        }
        BeamModel.Params beam = AcceleratorConfig.beam();
        double fieldT = BeamModel.dipoleField(beam, s.ring.count(BeamElementRole.DIPOLE), s.ring.count(BeamElementRole.SC_DIPOLE));
        return 1000.0 * BeamModel.ringMaxEnergy(beam, fieldT, s.ring.radius(), s.ring.dipoleFraction());
    }

    private double ringTarget(AcceleratorStructure s) {
        return ringMaxEnergy(s) * energySetpoint;
    }

    /** Every personnel interlock is closed. */
    protected boolean interlocksClear() {
        return true;
    }

    private void handle(BeamSequence.Transition transition) {
        if (transition == null || level == null) {
            return;
        }
        if (transition.to() == BeamPhase.BEAM_ON) {
            messenger.log(Component.translatable("create_nuclearindustry.accelerator.message.beam_on", energyText(budget.energy())));
            if (budget.energy() >= GEV || structure.map(this::ringTarget).orElse(0.0) >= GEV) {
                NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), NuclearEvent.GEV_BEAM);
            }
        }
        if (transition.to() == BeamPhase.BEAM_DUMP) {
            BlockPos at = structure.map(AcceleratorStructure::terminal).orElse(getBlockPos());
            level.playSound(null, at, AllNuclearSounds.BEAM_DUMP.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
        }
        BeamTripCause cause = transition.cause();
        if (cause != null && cause != BeamTripCause.NONE) {
            MutableComponent message = cause == BeamTripCause.OPERATOR
                    ? Component.translatable("create_nuclearindustry.accelerator.message.stopped").withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("create_nuclearindustry.accelerator.message.trip", Component.translatable(cause.langKey()))
                    .withStyle(ChatFormatting.RED);
            if (cause == BeamTripCause.OPERATOR) {
                messenger.status(level, messenger.owner(level), message);
            } else {
                messenger.alert(level, getBlockPos(), message);
            }
            AcceleratorHooks.onTrip.accept(this, cause);
        }
        if (transition.to() == BeamPhase.OFFLINE) {
            ringEnergy = 0;
        }
        markDirtyAndSync();
    }

    private AcceleratorSupply collectSupply(AcceleratorStructure s) {
        AcceleratorSupply.Builder builder = AcceleratorSupply.builder();
        forEachAttachment(s, attachment -> attachment.contribute(builder));
        return builder.build();
    }

    private void forEachAttachment(AcceleratorStructure s, java.util.function.Consumer<AcceleratorAttachment> action) {
        if (level == null) {
            return;
        }
        for (BlockPos pos : s.functionalParts()) {
            if (level.getBlockEntity(pos) instanceof AcceleratorAttachment attachment) {
                action.accept(attachment);
            }
        }
    }

    @Nullable
    private IonSourceBlockEntity source(AcceleratorStructure s) {
        return level != null && level.getBlockEntity(s.source) instanceof IonSourceBlockEntity source ? source : null;
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        if (!state.hasProperty(AcceleratorControllerBlock.ON)) {
            return;
        }
        boolean on = status == Status.FORMED && sequence.phase() != BeamPhase.OFFLINE;
        if (state.getValue(AcceleratorControllerBlock.ON) != on) {
            level.setBlock(getBlockPos(), state.setValue(AcceleratorControllerBlock.ON, on), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ operator

    /** A start from the screen, a desk or redstone; returns what is missing, or NONE. */
    public BeamTripCause requestStart() {
        if (level == null || level.isClientSide) {
            return BeamTripCause.NONE;
        }
        if (pendingRevalidation) {
            revalidate(false, null);
        }
        BeamTripCause missing;
        if (structure.isEmpty()) {
            missing = BeamTripCause.STRUCTURE;
            sequence.trip(missing);
        } else {
            AcceleratorStructure s = structure.get();
            if (supplyOverride == null) {
                supply = collectSupply(s);
            }
            IonSourceBlockEntity source = source(s);
            missing = sequence.requestStart(AcceleratorConfig.sequence(),
                    conditions(s, source != null && source.hasGas(AcceleratorConfig.hydrogenPerTick)));
            if (missing == BeamTripCause.NONE && AcceleratorHooks.startVeto.test(this)) {
                missing = BeamTripCause.INTERLOCK;
                sequence.trip(BeamTripCause.INTERLOCK);
                sequence.restore(BeamPhase.OFFLINE, BeamTripCause.INTERLOCK, 0, false);
            }
        }
        if (missing != BeamTripCause.NONE) {
            messenger.status(level, messenger.owner(level), Component.translatable("create_nuclearindustry.accelerator.message.cannot_start",
                    Component.translatable(missing.langKey())).withStyle(ChatFormatting.RED));
        } else {
            messenger.status(level, messenger.owner(level), Component.translatable("create_nuclearindustry.accelerator.message.starting")
                    .withStyle(ChatFormatting.AQUA));
        }
        markDirtyAndSync();
        return missing;
    }

    public void requestStop() {
        if (level != null && !level.isClientSide) {
            handle(sequence.requestStop());
            markDirtyAndSync();
        }
    }

    /** Trips the beam for {@code cause} (interlocks, quenches). */
    public void trip(BeamTripCause cause) {
        if (level != null && !level.isClientSide) {
            handle(sequence.trip(cause));
        }
    }

    public void setEnergySetpoint(double value) {
        energySetpoint = clamp(value);
        markDirtyAndSync();
    }

    public void setCurrentSetpoint(double value) {
        currentSetpoint = clamp(value);
        markDirtyAndSync();
    }

    public void setOperator(Player player) {
        messenger.setOwner(player);
    }

    /** A rising redstone edge starts the beam, a falling one stops it. */
    public void onRedstoneSignal(boolean powered) {
        if (powered && !redstonePowered) {
            requestStart();
        } else if (!powered && redstonePowered) {
            requestStop();
        }
        if (powered != redstonePowered) {
            redstonePowered = powered;
            setChanged();
        }
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 1;
    }

    // ------------------------------------------------------------------ structure

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.register(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            structure.ifPresent(s -> forEachAttachment(s, attachment -> attachment.observe(BeamReadings.NONE)));
            MultiblockRegistry.unregister(this);
            AcceleratorHooks.onRemoved.accept(this);
        }
        super.setRemoved();
    }

    public void attemptFormation(Player player) {
        revalidate(true, player);
    }

    @Override
    public void requestStructureRevalidation(Player player) {
        messenger.setOwner(player);
        pendingRevalidation = true;
    }

    @Override
    public BoundingBox interestBounds() {
        if (structure.isPresent()) {
            return structure.get().bounds();
        }
        int reach = Math.max(8, AcceleratorConfig.maxLineLength);
        BlockPos pos = getBlockPos();
        return new BoundingBox(pos.getX() - reach, pos.getY() - 2, pos.getZ() - reach, pos.getX() + reach, pos.getY() + 2,
                pos.getZ() + reach);
    }

    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (structure.isPresent()) {
            return snapshot != null && snapshot.isAffectedBy(change);
        }
        return change.removed() || change.isMultiblockPart();
    }

    private void revalidate(boolean playerInitiated, @Nullable Player player) {
        messenger.setOwner(player);
        pendingRevalidation = false;
        if (level == null) {
            return;
        }
        boolean wasFormed = structure.isPresent();
        MultiblockValidationResult<AcceleratorStructure> result = AcceleratorValidator.validate(level, getBlockPos());
        lastValidation = result;
        if (result.formed() && result.structure().isPresent()) {
            AcceleratorStructure next = result.structure().get();
            setStructure(Optional.of(next));
            if (playerInitiated || !wasFormed) {
                NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), NuclearEvent.ACCELERATOR_FORMED);
                if (next.ring != null) {
                    NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), NuclearEvent.RING_FORMED);
                }
                MutableComponent message = Component.translatable("create_nuclearindustry.accelerator.message.formed",
                        next.cells(), energyText(maxEnergy(next))).withStyle(ChatFormatting.AQUA);
                if (!result.warnings().isEmpty()) {
                    message.append(" ").append(MultiblockText.text(result.warnings().get(0)).copy().withStyle(ChatFormatting.YELLOW));
                }
                messenger.status(level, playerInitiated ? player : null, message);
            }
        } else {
            structure.ifPresent(s -> forEachAttachment(s, attachment -> attachment.observe(BeamReadings.NONE)));
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                messenger.status(level, playerInitiated ? player : null, Component.translatable(wasFormed ? "create_nuclearindustry.accelerator.message.broken"
                                : "create_nuclearindustry.accelerator.message.incomplete", validationText())
                        .withStyle(ChatFormatting.RED));
            }
        }
        AcceleratorHooks.onStructureChanged.accept(this);
        markDirtyAndSync();
    }

    private void setStructure(Optional<AcceleratorStructure> next) {
        boolean boundsChanged = !structure.map(AcceleratorStructure::bounds).equals(next.map(AcceleratorStructure::bounds));
        structure = next;
        snapshot = next.isPresent() && level != null && !level.isClientSide
                ? StructureSnapshot.capture(level, next.get().allPositions())
                : null;
        status = next.isPresent() ? Status.FORMED : Status.UNFORMED;
        supply = AcceleratorSupply.NONE;
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    /** Energy at full drive: the linac's, or the ring's top energy. */
    private double maxEnergy(AcceleratorStructure s) {
        return s.ring != null ? ringMaxEnergy(s)
                : BeamModel.linacEnergy(AcceleratorConfig.beam(), s.copperCavities(), s.superconductingCavities(), 1);
    }

    // ------------------------------------------------------------------ readings

    public BeamReadings readings() {
        if (client()) {
            return clientReadings;
        }
        if (structure.isEmpty()) {
            return new BeamReadings(false, false, sequence.phase(), sequence.tripCause(), 0, 0, 0, 0, 0, 0, 0,
                    vacuum.getPressure(), 0, 0, 0, 0, 0, 0, energySetpoint, currentSetpoint, 0,
                    liquidHeliumTank.getFluidAmount(), magnets.getTemperature(), interlocksClear(), terminalActivity(), 0);
        }
        AcceleratorStructure s = structure.get();
        IonSourceBlockEntity source = source(s);
        return new BeamReadings(true, status == Status.SUSPENDED, sequence.phase(), sequence.tripCause(), budget.energy(),
                maxEnergy(s), budget.current(), budget.sourceCurrent(), budget.power(), budget.totalLoss(), budget.lossFraction(),
                vacuum.getPressure(), fieldFraction(s), magnetPower(s), rfDrive(s), supply.rfPower(), rfDemand(s),
                source == null ? 0 : source.tank().getFluidAmount(), energySetpoint, currentSetpoint,
                s.ring == null ? 0 : s.ring.radius(), liquidHeliumTank.getFluidAmount(),
                s.superconducting() > 0 ? magnets.getTemperature() : MagnetModel.AMBIENT_TEMPERATURE, interlocksClear(),
                terminalActivity(), s.cells());
    }

    private double terminalActivity() {
        if (level == null || structure.isEmpty()) {
            return 0;
        }
        return level.getBlockEntity(structure.get().terminal()) instanceof BeamTerminal terminal ? terminal.activity() : 0;
    }

    public Optional<AcceleratorStructure> getStructure() {
        return structure;
    }

    public boolean isFormed() {
        return client() ? clientReadings.formed() : structure.isPresent();
    }

    public BeamPhase phase() {
        return client() ? clientReadings.phase() : sequence.phase();
    }

    public BeamTripCause tripCause() {
        return client() ? clientReadings.tripCause() : sequence.tripCause();
    }

    public BeamBudget.Result budget() {
        return budget;
    }

    public AcceleratorSupply getSupply() {
        return supply;
    }

    /** Replaces what the attachments offer (game tests). */
    public void setSupplyOverride(@Nullable AcceleratorSupply override) {
        supplyOverride = override;
        supply = override == null ? AcceleratorSupply.NONE : override;
    }

    /** Puts the pipe at beam vacuum at once (game tests). */
    public void primeVacuum() {
        vacuum.restore(VACUUM.basePressure());
    }

    public VacuumModel vacuum() {
        return vacuum;
    }

    public MagnetModel magnets() {
        return magnets;
    }

    public BeamSequence sequence() {
        return sequence;
    }

    public FluidTank liquidHeliumTank() {
        return liquidHeliumTank;
    }

    public FluidTank warmHeliumTank() {
        return warmHeliumTank;
    }

    public double ringEnergy() {
        return ringEnergy;
    }

    public String getValidationKey() {
        String message = client() ? clientValidation : lastValidation.primaryMessage();
        return MultiblockText.key(message);
    }

    public Component validationText() {
        return MultiblockText.text(client() ? clientValidation : lastValidation.primaryMessage());
    }

    /** The injector line's turning points, for the renderer: the source, every bend, the terminal. */
    public List<BlockPos> injectorPath() {
        if (client()) {
            return clientInjector;
        }
        return structure.map(AcceleratorStructure::injectorWaypoints).orElse(List.of());
    }

    public List<BlockPos> extractionPath() {
        if (client()) {
            return clientExtraction;
        }
        return structure.map(s -> s.extraction == null ? List.<BlockPos>of() : s.extraction.waypoints()).orElse(List.of());
    }

    public BlockPos ringCenter() {
        if (client()) {
            return clientRingCenter;
        }
        return structure.map(s -> s.ring == null ? BlockPos.ZERO : s.ring.center()).orElse(BlockPos.ZERO);
    }

    public int ringRadius() {
        if (client()) {
            return clientRingRadius;
        }
        return structure.map(s -> s.ring == null ? 0 : s.ring.radius()).orElse(0);
    }

    private boolean client() {
        return ponderPreview || level != null && level.isClientSide;
    }

    /**
     * Shows a beam in a Ponder scene: finds the structure in the scene's world and makes the renderer
     * draw a beam of this energy and current along it.
     */
    public void previewBeam(double energyMev, double current) {
        if (level == null) {
            return;
        }
        ponderPreview = true;
        MultiblockValidationResult<AcceleratorStructure> result = AcceleratorValidator.validate(level, getBlockPos());
        AcceleratorStructure s = result.structure().orElse(null);
        clientInjector = s == null ? List.of() : s.injectorWaypoints();
        clientExtraction = s == null || s.extraction == null ? List.of() : s.extraction.waypoints();
        clientRingCenter = s == null || s.ring == null ? BlockPos.ZERO : s.ring.center();
        clientRingRadius = s == null || s.ring == null ? 0 : s.ring.radius();
        double power = BeamModel.beamPower(energyMev, current);
        clientReadings = new BeamReadings(s != null, false, current > 0 ? BeamPhase.BEAM_ON : BeamPhase.OFFLINE, BeamTripCause.NONE,
                energyMev, energyMev, current, current, power, 0, 0, VACUUM.basePressure(), 1, 1, 1, 0, 0, 0, 1, 1,
                clientRingRadius, 0, MagnetModel.AMBIENT_TEMPERATURE, true, 0, s == null ? 0 : s.cells());
    }

    public MultiblockMessenger messenger() {
        return messenger;
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("create_nuclearindustry.accelerator.goggles.title"))
                .withStyle(ChatFormatting.AQUA));
        BeamReadings r = readings();
        if (!r.formed()) {
            tooltip.add(Component.literal("  ").append(validationText()).withStyle(ChatFormatting.RED));
            return true;
        }
        tooltip.add(line("structure", r.cells(), energyText(r.maxEnergy())).withStyle(ChatFormatting.GRAY));
        if (r.ringRadius() > 0) {
            tooltip.add(line("ring", r.ringRadius()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(line("phase", Component.translatable(r.phase().langKey()))
                .withStyle(r.phase() == BeamPhase.BEAM_ON ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        if (r.tripCause() != BeamTripCause.NONE) {
            tooltip.add(line("trip", Component.translatable(r.tripCause().langKey())).withStyle(ChatFormatting.RED));
        }
        if (r.suspended()) {
            tooltip.add(line("suspended").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(line("beam", energyText(r.energy()), fmt1(r.current()), powerText(r.power()))
                .withStyle(r.beamOn() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(line("losses", fmt(r.lossFraction() * 100), powerText(r.lossPower()))
                .withStyle(r.lossFraction() > 0.25 ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(line("systems", vacuumText(r.vacuum()), fmt(r.magnetPower() * 100), fmt(r.rfDrive() * 100))
                .withStyle(ChatFormatting.GRAY));
        if (r.magnetTemperature() < MagnetModel.AMBIENT_TEMPERATURE - 1 || r.liquidHelium() > 0) {
            tooltip.add(line("cryo", fmt1(r.magnetTemperature()), r.liquidHelium()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(line("hydrogen", r.hydrogen()).withStyle(r.hydrogen() > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
        return true;
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.literal("  ").append(Component.translatable("create_nuclearindustry.accelerator.goggles." + key, args));
    }

    /** An energy as text, in MeV or GeV. */
    public static String energyText(double mev) {
        if (mev >= 1000) {
            return String.format(Locale.ROOT, "%.2f GeV", mev / 1000.0);
        }
        return String.format(Locale.ROOT, "%.1f MeV", mev);
    }

    /** A power as text, in kW or MW. */
    public static String powerText(double kw) {
        if (kw >= 1000) {
            return String.format(Locale.ROOT, "%.2f MW", kw / 1000.0);
        }
        return String.format(Locale.ROOT, "%.1f kW", kw);
    }

    /** A pressure as text, in pascals. */
    public static String vacuumText(double log10Pascals) {
        return String.format(Locale.ROOT, "%.0e Pa", Math.pow(10, log10Pascals)).replace("e+0", "e").replace("e-0", "e-");
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String fmt1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("phase", sequence.phase().name());
        tag.putString("tripCause", sequence.tripCause().name());
        tag.putInt("phaseTicks", sequence.phaseTicks());
        tag.putBoolean("pendingStart", sequence.pendingStart());
        tag.putDouble("vacuum", vacuum.getPressure());
        tag.putDouble("field", field);
        tag.putDouble("magnetField", magnets.getField());
        tag.putDouble("magnetTemperature", magnets.getTemperature());
        tag.putDouble("ringEnergy", ringEnergy);
        tag.putDouble("energySetpoint", energySetpoint);
        tag.putDouble("currentSetpoint", currentSetpoint);
        tag.putBoolean("redstonePowered", redstonePowered);
        tag.putBoolean("firstBeam", firstBeamAwarded);
        tag.putDouble("liquidHeliumBalance", liquidHeliumBalance);
        tag.putDouble("warmHeliumBalance", warmHeliumBalance);
        tag.put("liquidHelium", liquidHeliumTank.writeToNBT(provider, new CompoundTag()));
        tag.put("warmHelium", warmHeliumTank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        sequence.restore(BeamReadings.phase(tag.getString("phase")), BeamReadings.trip(tag.getString("tripCause")),
                tag.getInt("phaseTicks"), tag.getBoolean("pendingStart"));
        vacuum.restore(tag.contains("vacuum") ? tag.getDouble("vacuum") : VacuumModel.ATMOSPHERE);
        field = tag.getDouble("field");
        magnets.restore(tag.getDouble("magnetField"),
                tag.contains("magnetTemperature") ? tag.getDouble("magnetTemperature") : MagnetModel.AMBIENT_TEMPERATURE);
        ringEnergy = tag.getDouble("ringEnergy");
        energySetpoint = tag.contains("energySetpoint") ? clamp(tag.getDouble("energySetpoint")) : 1;
        currentSetpoint = tag.contains("currentSetpoint") ? clamp(tag.getDouble("currentSetpoint")) : 1;
        redstonePowered = tag.getBoolean("redstonePowered");
        firstBeamAwarded = tag.getBoolean("firstBeam");
        liquidHeliumBalance = tag.getDouble("liquidHeliumBalance");
        warmHeliumBalance = tag.getDouble("warmHeliumBalance");
        liquidHeliumTank.readFromNBT(provider, tag.getCompound("liquidHelium"));
        warmHeliumTank.readFromNBT(provider, tag.getCompound("warmHelium"));
        pendingRevalidation = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("readings", readings().write());
        tag.putString("validation", lastValidation.primaryMessage());
        tag.putLongArray("injector", injectorPath().stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("extraction", extractionPath().stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLong("ringCenter", ringCenter().asLong());
        tag.putInt("ringRadius", ringRadius());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        clientReadings = BeamReadings.read(tag.getCompound("readings"));
        clientValidation = tag.getString("validation");
        clientInjector = positions(tag.getLongArray("injector"));
        clientExtraction = positions(tag.getLongArray("extraction"));
        clientRingCenter = BlockPos.of(tag.getLong("ringCenter"));
        clientRingRadius = tag.getInt("ringRadius");
    }

    private static List<BlockPos> positions(long[] packed) {
        List<BlockPos> out = new ArrayList<>(packed.length);
        for (long p : packed) {
            out.add(BlockPos.of(p));
        }
        return out;
    }

    /** Direction the beam leaves the controller. */
    public net.minecraft.core.Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING)
                : net.minecraft.core.Direction.NORTH;
    }
}
