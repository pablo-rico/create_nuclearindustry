package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.FusionConfig;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakAuxiliary;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakClientHooks;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakControl;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructureValidator;
import org.papiricoh.create_nuclearindustry.fusion.physics.MagnetModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.StartupSequence;
import org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.fusion.physics.VacuumModel;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tokamak controller. Owns the plasma, the coils, the vacuum and the startup sequence, the fuel,
 * coolant, plasma steam and helium tanks, and the structure found by {@link TokamakStructureValidator}.
 * Ports and injectors are thin proxies onto these tanks; heaters and pumps in the shell offer their
 * share through {@link TokamakAuxiliary}.
 *
 * <p>Each tick: the pumps set the chamber pressure, liquid helium cools the coils and the magnet
 * drives set the field, the {@link StartupSequence} moves through its phases, and while there is a
 * plasma the {@link PlasmaModel} burns what the fuelling and heating give it. Tank amounts are whole
 * millibuckets, so fractional draws and returns are carried over in balances.
 */
public class FusionReactorBlockEntity extends BlockEntity implements IHaveGoggleInformation, MultiblockController, TokamakControl {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int FUEL_TANK_CAPACITY = 8_000;
    public static final int TANK_CAPACITY = 16_000;
    public static final int HELIUM_TANK_CAPACITY = 8_000;
    /** Injectors in OUTPUT mode never drain the tritium tank below this. */
    public static final int TRITIUM_RESERVE = 1_000;
    /**
     * Plasma steam a matched pair of coolant and steam ports can take out per tick: about 310 MW of
     * fusion power, so one pair carries an R=4 tokamak at its default density with room to spare.
     */
    public static final int PORT_THROUGHPUT = 400;
    /** Highest fuelling setpoint, as the Greenwald fraction it would hold at full field. */
    public static final double MAX_FUEL_DENSITY = 1.5;
    private static final int LEGACY_PELLET_FLUID = 250;
    /** Released tritium contaminates the hall for a couple of in-game hours. */
    private static final int DISRUPTION_CONTAMINATION_TICKS = 6_000;
    private static final double DISRUPTION_RADIATION = 8.0;
    /** How often auxiliaries hear the plasma state, in ticks. */
    private static final int OBSERVE_INTERVAL = 10;
    /** How long the tritium overflow warning stays up after bred tritium found no room. */
    private static final int OVERFLOW_WARNING_TICKS = 100;
    private static final int MESSAGE_COOLDOWN_TICKS = 100;

    private Status status = Status.UNFORMED;
    private Optional<TokamakStructure> structure = Optional.empty();
    private StructureSnapshot snapshot;
    private MultiblockValidationResult<TokamakStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of(TokamakStructureValidator.NOT_FORMED), List.of());
    private final TokamakPhysicsSimulator physics = new TokamakPhysicsSimulator();
    private final MagnetModel magnets = new MagnetModel();
    private final VacuumModel vacuum = new VacuumModel();
    private final StartupSequence sequence = new StartupSequence();
    private final MultiblockMessenger messenger = new MultiblockMessenger("Tokamak");
    private String lastMessageKey = "";
    private long lastMessageTime = Long.MIN_VALUE;
    private boolean pendingRevalidation = true;
    private int syncCounter;
    // Fractional mB owed to (negative) or by (positive) the tanks.
    private double deuteriumBalance;
    private double tritiumBalance;
    private double tritiumCredit;
    private double liquidHeliumBalance;
    private double warmHeliumBalance;
    private double coolantBalance;
    private double fieldTarget;
    private int tritiumOverflowTicks;
    private boolean redstonePowered;
    private boolean upgradeNotice;
    private final List<ItemStack> legacyDrops = new ArrayList<>();

    // Operator settings (TokamakControl).
    private double fuelRate;
    private boolean fuelRateSet;
    private double dtRatio = 0.5;
    private boolean heatingEnabled = true;
    private double heatingLevel = 1.0;
    private TokamakSupply supply = TokamakSupply.NONE;
    /** Replaces what the shell's auxiliaries offer; only game tests set it. */
    private TokamakSupply supplyOverride;

    // Structure summary and readings synced to clients for goggles, the GUI and the renderer.
    private int dispRadius;
    private int dispCoils;
    private int dispBlankets;
    private double dispCoverage;
    private String dispValidationMessage = TokamakStructureValidator.NOT_FORMED;
    private BlockPos dispCenter = BlockPos.ZERO;
    private TokamakReadings clientReadings = TokamakReadings.NONE;

    private final FluidTank deuteriumTank = tank(FUEL_TANK_CAPACITY, NuclearFluidHelper::isDeuterium);
    private final FluidTank tritiumTank = tank(FUEL_TANK_CAPACITY, NuclearFluidHelper::isTritium);
    private final FluidTank coolantTank = tank(TANK_CAPACITY, NuclearFluidHelper::isTokamakCoolant);
    private final FluidTank plasmaSteamTank = tank(TANK_CAPACITY, NuclearFluidHelper::isPlasmaSteam);
    private final FluidTank liquidHeliumTank = tank(HELIUM_TANK_CAPACITY, NuclearFluidHelper::isLiquidHelium);
    private final FluidTank warmHeliumTank = tank(HELIUM_TANK_CAPACITY, NuclearFluidHelper::isHelium);

    /** The coils' helium loop: cryogenic ports fill the liquid tank and drain the warm one. */
    public record CryoTanks(FluidTank liquidHelium, FluidTank warmHelium) {}

    private final CryoTanks cryoTanks = new CryoTanks(liquidHeliumTank, warmHeliumTank);

    public enum Status { UNFORMED, FORMED, SUSPENDED }

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_CONTROLLER.get(), pos, state);
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

    public static void tick(Level level, BlockPos pos, BlockState state, FusionReactorBlockEntity entity) {
        if (level.isClientSide) {
            TokamakClientHooks.clientTick.accept(entity);
            return;
        }
        entity.dropLegacyItems();
        if (entity.pendingRevalidation) {
            entity.revalidate(false, null);
        }
        entity.serverTick();
        entity.updateBlockStateFlags();
        if (++entity.syncCounter >= 10) {
            entity.syncCounter = 0;
            entity.markDirtyAndSync();
        }
    }

    private void serverTick() {
        if (upgradeNotice) {
            upgradeNotice = false;
            notifyOwner(Component.translatable("create_nuclearindustry.tokamak.message.upgraded").withStyle(ChatFormatting.YELLOW), true);
        }
        if (structure.isEmpty()) {
            status = Status.UNFORMED;
            runUnformed();
            return;
        }
        TokamakStructure tokamak = structure.get();
        if (!isLoaded(level, tokamak.bounds())) {
            // Part of the torus is unloaded: everything waits.
            status = Status.SUSPENDED;
            return;
        }
        status = Status.FORMED;
        supply = collectSupply(tokamak);
        fieldTarget = readMagnetDrive(tokamak);

        PlasmaModel plasma = physics.model();
        VacuumModel.Params vacuumParams = FusionConfig.vacuumParams();
        MagnetModel.Params magnetParams = FusionConfig.magnetParams();
        StartupSequence.Params startupParams = FusionConfig.startupParams();

        vacuum.tick(vacuumParams, pumping(tokamak), plasma.hasPlasma());
        runMagnets(tokamak, magnetParams, plasma.hasPlasma());
        handle(sequence.tick(startupParams, conditions(tokamak, vacuumParams, magnetParams)));

        if (plasma.hasPlasma()) {
            runPlasma(tokamak);
        } else if (sequence.phase() == TokamakPhase.OFFLINE) {
            plasma.rechargeSolenoid(FusionConfig.plasmaParams());
        }
        if (tritiumOverflowTicks > 0) {
            tritiumOverflowTicks--;
        }
        if (level.getGameTime() % OBSERVE_INTERVAL == 0) {
            notifyAuxiliaries(tokamak, readings());
        }
    }

    /** No structure: the chamber is open, the coils warm and any sequence winds down at once. */
    private void runUnformed() {
        vacuum.vent();
        if (physics.model().hasPlasma() || physics.isBurning()) {
            physics.quench();
        }
        magnets.dropField();
        magnets.tick(FusionConfig.magnetParams(), new MagnetModel.Inputs(1, 0, false, false, false, 0));
        if (sequence.phase() != TokamakPhase.OFFLINE) {
            handle(sequence.trip(TripCause.STRUCTURE));
            handle(sequence.tick(FusionConfig.startupParams(), new StartupSequence.Conditions(false, false, false, 0,
                    false, false, 0, 0, 0, false, PlasmaModel.AMBIENT_TEMP, 0, 0)));
        }
        physics.model().rechargeSolenoid(FusionConfig.plasmaParams());
        fieldTarget = 0;
        supply = TokamakSupply.NONE;
    }

    private static boolean isLoaded(Level level, BoundingBox box) {
        return level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ());
    }

    private double pumping(TokamakStructure tokamak) {
        return supply.pumpSpeed() / VacuumModel.pumpsNeeded(tokamak.majorRadius());
    }

    private StartupSequence.Conditions conditions(TokamakStructure tokamak, VacuumModel.Params vacuumParams,
                                                  MagnetModel.Params magnetParams) {
        PlasmaModel plasma = physics.model();
        return new StartupSequence.Conditions(true, VacuumModel.canReachPlasmaVacuum(vacuumParams, pumping(tokamak)),
                vacuum.atPlasmaVacuum(vacuumParams), liquidHeliumTank.getFluidAmount(), magnets.ready(),
                magnets.quenching(magnetParams), magnets.getField(), fieldTarget, deuteriumTank.getFluidAmount(),
                plasma.hasPlasma(), plasma.getTemperature(), plasma.getDensity(), plasma.getFusionPower());
    }

    private void runMagnets(TokamakStructure tokamak, MagnetModel.Params params, boolean plasmaPresent) {
        double used = magnets.tick(params, new MagnetModel.Inputs(tokamak.geometry.sectorCount(), fieldTarget,
                sequence.cooling(), sequence.energize(plasmaPresent), plasmaPresent,
                liquidHeliumTank.getFluidAmount() + liquidHeliumBalance));
        liquidHeliumBalance -= used;
        // Boiled-off helium goes back to the plant as warm gas; a full return tank vents it.
        warmHeliumBalance += used;
        liquidHeliumBalance = settle(liquidHeliumTank, liquidHeliumBalance, AllNuclearFluids.LIQUID_HELIUM.get(), false);
        warmHeliumBalance = settle(warmHeliumTank, warmHeliumBalance, AllNuclearFluids.HELIUM.get(), false);
    }

    private void runPlasma(TokamakStructure tokamak) {
        PlasmaModel plasma = physics.model();
        boolean fuelling = sequence.fuelling();
        double heatShare = heatingEnabled ? heatingLevel : 0;

        // Fuelling at the setpoint and D:T mix; whichever isotope runs short is made up with the other.
        double wantD = fuelling ? fuelRate * dtRatio : 0;
        double wantT = fuelling ? fuelRate * (1 - dtRatio) : 0;
        double availableD = Math.max(0, deuteriumTank.getFluidAmount() + deuteriumBalance);
        double availableT = Math.max(0, tritiumTank.getFluidAmount() + tritiumBalance);
        double injectD = Math.min(wantD, availableD);
        double injectT = Math.min(wantT, availableT);
        double short_ = wantD + wantT - injectD - injectT;
        if (short_ > 0) {
            double moreD = Math.min(short_, availableD - injectD);
            injectD += moreD;
            injectT += Math.min(short_ - moreD, availableT - injectT);
        }

        // The beams take their deuterium after the fuelling; short of it, they inject less power.
        double beamDemand = fuelling ? supply.nbiDeuteriumDemand() * heatShare : 0;
        double beamDeuterium = Math.min(beamDemand, Math.max(0, availableD - injectD));
        double beamShare = beamDemand > 0 ? beamDeuterium / beamDemand : 1;
        double beamPower = fuelling ? supply.nbiPower() * heatShare * beamShare : 0;
        double rfPower = fuelling ? supply.rfPower() * heatShare : 0;

        int steamCapacity = Math.min(Math.min(tokamak.coolantPortCount, tokamak.steamPortCount) * PORT_THROUGHPUT,
                plasmaSteamTank.getSpace());
        PlasmaModel.Outputs out = plasma.tick(FusionConfig.plasmaParams(), new PlasmaModel.Inputs(
                tokamak.plasmaVolume(), tokamak.majorRadius(), tokamak.coilCoverage(FusionConfig.coilsPerSector),
                magnets.getField(), tokamak.blanketFraction(), injectD, injectT, sequence.ohmic(), beamPower,
                beamDeuterium, rfPower, coolantTank.getFluidAmount() + coolantBalance, steamCapacity,
                sequence.terminating()));

        deuteriumBalance += out.deuteriumRecycled() - injectD - beamDeuterium;
        tritiumBalance += out.tritiumRecycled() - injectT;
        deuteriumBalance = settle(deuteriumTank, deuteriumBalance, AllNuclearFluids.DEUTERIUM.get(), true);
        tritiumBalance = settle(tritiumTank, tritiumBalance, AllNuclearFluids.TRITIUM.get(), true);

        // Bred tritium goes to the tank; injectors in OUTPUT mode hand out what is above the reserve.
        // With no room left the blankets stop breeding and the operator is warned; nothing is vented.
        tritiumCredit += out.tritiumBred();
        int bred = (int) Math.floor(tritiumCredit);
        if (bred > 0) {
            int filled = tritiumTank.fill(new FluidStack(AllNuclearFluids.TRITIUM.get(), bred), IFluidHandler.FluidAction.EXECUTE);
            if (filled < bred) {
                tritiumOverflowTicks = OVERFLOW_WARNING_TICKS;
            }
            tritiumCredit -= bred;
        }

        // Helium ash pumped out of the plasma tops up the coils' helium loop.
        warmHeliumBalance += out.heliumRemoved();
        warmHeliumBalance = settle(warmHeliumTank, warmHeliumBalance, AllNuclearFluids.HELIUM.get(), false);

        // Coolant boils to plasma steam. Heavy water left in the tank from before is used up the same way.
        coolantBalance -= out.steamProduced();
        int steam = (int) Math.floor(-coolantBalance);
        if (steam > 0) {
            FluidStack coolant = coolantTank.drain(steam, IFluidHandler.FluidAction.EXECUTE);
            plasmaSteamTank.fill(new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), coolant.getAmount()), IFluidHandler.FluidAction.EXECUTE);
            coolantBalance += steam;
        }

        if (!plasma.hasPlasma()) {
            physics.quench();
        }
        if (plasma.isDisrupting()) {
            disrupt(tokamak);
        }
    }

    /**
     * Moves whole millibuckets between a tank and its fractional balance: draws what the balance owes,
     * fills what it holds. A fill that does not fit stays in the balance when {@code keep}, so fuel
     * pumped out of the plasma is never lost; otherwise the excess is vented.
     */
    private static double settle(FluidTank tank, double balance, net.minecraft.world.level.material.Fluid fluid, boolean keep) {
        if (balance <= -1) {
            int owed = (int) Math.floor(-balance);
            balance += tank.drain(owed, IFluidHandler.FluidAction.EXECUTE).getAmount();
            // Never owe more than a tick's worth beyond what the tank had.
            balance = Math.max(balance, -1);
        } else if (balance >= 1) {
            int held = (int) Math.floor(balance);
            int filled = tank.fill(new FluidStack(fluid, held), IFluidHandler.FluidAction.EXECUTE);
            balance -= keep ? filled : held;
        }
        return balance;
    }

    private void handle(StartupSequence.Transition transition) {
        if (transition == null) {
            return;
        }
        if (transition.to() == TokamakPhase.BREAKDOWN) {
            double puff = FusionConfig.breakdownDeuterium;
            deuteriumBalance -= puff;
            deuteriumBalance = settle(deuteriumTank, deuteriumBalance, AllNuclearFluids.DEUTERIUM.get(), true);
            physics.model().breakdown(puff);
        }
        if (transition.to() == TokamakPhase.BURN && level != null) {
            NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), NuclearEvent.TOKAMAK_IGNITION);
            notifyOwner(Component.translatable("create_nuclearindustry.tokamak.message.burning").withStyle(ChatFormatting.GOLD), false);
        }
        TripCause cause = transition.cause();
        if (cause != null && cause != TripCause.NONE && cause != TripCause.DISRUPTION) {
            MutableComponent message = cause == TripCause.OPERATOR
                    ? Component.translatable("create_nuclearindustry.tokamak.message.shutdown").withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("create_nuclearindustry.tokamak.message.trip", Component.translatable(cause.langKey()))
                    .withStyle(ChatFormatting.RED);
            notifyOwner(message, true);
        }
        if (transition.to() == TokamakPhase.OFFLINE) {
            physics.quench();
            notifyOwner(Component.translatable("create_nuclearindustry.tokamak.message.offline").withStyle(ChatFormatting.GRAY), false);
        }
        markDirtyAndSync();
    }

    /** Sums what the heaters and pumps in the shell offer this tick. */
    private TokamakSupply collectSupply(TokamakStructure tokamak) {
        if (supplyOverride != null) {
            return supplyOverride;
        }
        TokamakSupply.Builder builder = TokamakSupply.builder();
        forEachAuxiliary(tokamak, auxiliary -> auxiliary.contribute(builder));
        return builder.build();
    }

    private void notifyAuxiliaries(TokamakStructure tokamak, TokamakReadings readings) {
        forEachAuxiliary(tokamak, auxiliary -> auxiliary.observe(readings));
    }

    private void forEachAuxiliary(TokamakStructure tokamak, java.util.function.Consumer<TokamakAuxiliary> action) {
        if (level == null) {
            return;
        }
        for (BlockPos pos : tokamak.functionalParts()) {
            if (level.getBlockEntity(pos) instanceof TokamakAuxiliary auxiliary) {
                action.accept(auxiliary);
            }
        }
    }

    /** Field the magnet drives ask for: their summed contribution over the drives a tokamak this size needs. */
    private double readMagnetDrive(TokamakStructure tokamak) {
        if (level == null) {
            return 0;
        }
        double drive = 0;
        for (BlockPos pos : tokamak.magnetInputs) {
            if (level.getBlockEntity(pos) instanceof FusionMagnetInputBlockEntity magnet) {
                drive += magnet.getConfinementContribution();
            }
        }
        int needed = Math.max(1, (int) Math.ceil(tokamak.majorRadius() / 4.0));
        return Math.min(1.0, drive / needed);
    }

    /** A disruption dumps the plasma energy into the vessel wall. */
    private void disrupt(TokamakStructure tokamak) {
        BlockPos pos = getBlockPos();
        LOGGER.warn("Tokamak disruption at {}", pos);
        physics.quench();
        handle(sequence.trip(TripCause.DISRUPTION));
        // The tritium inventory escapes into the hall.
        if (!tritiumTank.isEmpty()) {
            FalloutData.contaminate(level, pos, DISRUPTION_RADIATION, DISRUPTION_CONTAMINATION_TICKS);
        }
        tritiumTank.setFluid(FluidStack.EMPTY);
        tritiumBalance = 0;
        tritiumCredit = 0;
        notifyOwner(Component.translatable("create_nuclearindustry.tokamak.message.disruption", pos.toShortString())
                .withStyle(ChatFormatting.DARK_RED), true);
        if (!FusionConfig.disruptionExplosions || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        TokamakGeometry geometry = tokamak.geometry;
        float power = (float) Math.min(12.0, 2.0 + 0.5 * tokamak.majorRadius());
        for (int i = 0; i < 3; i++) {
            int index = serverLevel.random.nextInt(geometry.shellCount());
            BlockPos at = tokamak.center.offset(geometry.shellX(index), geometry.shellY(index), geometry.shellZ(index));
            serverLevel.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
        }
    }

    private void updateBlockStateFlags() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(FusionReactorControllerBlock.ON)) {
            return;
        }
        boolean on = status == Status.FORMED && sequence.phase() != TokamakPhase.OFFLINE;
        boolean ignited = sequence.phase() == TokamakPhase.BURN;
        if (state.getValue(FusionReactorControllerBlock.ON) != on || state.getValue(FusionReactorControllerBlock.IGNITED) != ignited) {
            level.setBlock(getBlockPos(), state.setValue(FusionReactorControllerBlock.ON, on)
                    .setValue(FusionReactorControllerBlock.IGNITED, ignited), Block.UPDATE_CLIENTS);
        }
    }

    /** Tells the player who last worked on the tokamak; the same message is not repeated within a few seconds. */
    private void notifyOwner(Component message, boolean force) {
        if (level == null) {
            return;
        }
        String key = message.getString();
        long now = level.getGameTime();
        if (!force && key.equals(lastMessageKey) && now - lastMessageTime < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        lastMessageKey = key;
        lastMessageTime = now;
        Player owner = messenger.owner(level);
        if (owner != null) {
            owner.displayClientMessage(message, false);
        } else {
            LOGGER.debug("[Tokamak {}] {}", getBlockPos().toShortString(), key);
        }
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
            MultiblockRegistry.unregister(this);
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
        // The axis can be up to (maxRadius + 2) blocks away in any horizontal direction.
        int reach = 2 * (Math.max(TokamakGeometry.MIN_RADIUS, FusionConfig.maxMajorRadius) + 2);
        BlockPos pos = getBlockPos();
        return new BoundingBox(pos.getX() - reach, pos.getY() - TokamakGeometry.HALF_HEIGHT, pos.getZ() - reach,
                pos.getX() + reach, pos.getY() + TokamakGeometry.HALF_HEIGHT, pos.getZ() + reach);
    }

    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (structure.isPresent()) {
            return snapshot != null && snapshot.isAffectedBy(change);
        }
        return change.removed() || change.isMultiblockPart();
    }

    private void revalidate(boolean playerInitiated, Player player) {
        messenger.setOwner(player);
        pendingRevalidation = false;
        if (level == null) {
            return;
        }
        boolean wasFormed = structure.isPresent();
        MultiblockValidationResult<TokamakStructure> result = TokamakStructureValidator.validate(level, getBlockPos());
        lastValidation = result;
        if (result.formed() && result.structure().isPresent()) {
            TokamakStructure next = result.structure().get();
            structure.ifPresent(this::unlinkParts);
            setStructure(Optional.of(next));
            linkParts(next);
            if (!fuelRateSet) {
                fuelRate = defaultFuelRate();
            }
            fuelRate = Math.min(fuelRate, maxFuelRate());
            if (playerInitiated || !wasFormed) {
                MutableComponent message = Component.translatable("create_nuclearindustry.tokamak.message.formed",
                        next.dimensionsText()).withStyle(ChatFormatting.AQUA);
                if (!result.warnings().isEmpty()) {
                    message.append(" ").append(TokamakStructureValidator.text(result.warnings().get(0)).copy().withStyle(ChatFormatting.YELLOW));
                }
                notifyOwner(message, playerInitiated);
            }
        } else {
            structure.ifPresent(this::unlinkParts);
            setStructure(Optional.empty());
            if (playerInitiated || wasFormed) {
                notifyOwner(Component.translatable(wasFormed ? "create_nuclearindustry.tokamak.message.broken"
                                : "create_nuclearindustry.tokamak.message.incomplete", getValidationText())
                        .withStyle(ChatFormatting.RED), playerInitiated);
            }
        }
        markDirtyAndSync();
    }

    private void setStructure(Optional<TokamakStructure> next) {
        boolean boundsChanged = !structure.map(TokamakStructure::bounds).equals(next.map(TokamakStructure::bounds));
        structure = next;
        snapshot = next.isPresent() && level != null && !level.isClientSide
                ? StructureSnapshot.capture(level, next.get().allPositions())
                : null;
        status = next.isPresent() ? Status.FORMED : Status.UNFORMED;
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    private void linkParts(TokamakStructure tokamak) {
        forEachPart(tokamak, part -> part.link(getBlockPos()));
    }

    private void unlinkParts(TokamakStructure tokamak) {
        forEachPart(tokamak, part -> part.unlink(getBlockPos()));
        notifyAuxiliaries(tokamak, TokamakReadings.NONE);
    }

    private void forEachPart(TokamakStructure tokamak, java.util.function.Consumer<TokamakPart> action) {
        if (level == null) {
            return;
        }
        for (BlockPos pos : tokamak.functionalParts()) {
            if (level.getBlockEntity(pos) instanceof TokamakPart part) {
                action.accept(part);
            }
        }
    }

    // ------------------------------------------------------------------ controls

    /** Old fuel switch, kept for the desk lever: "on" asks for a start, "off" for a shutdown. */
    public boolean isFuelEnabled() {
        TokamakPhase phase = phase();
        return phase != TokamakPhase.OFFLINE && phase != TokamakPhase.RAMP_DOWN;
    }

    public void setFuelEnabled(boolean enabled) {
        if (enabled) {
            requestStart();
        } else {
            requestShutdown();
        }
    }

    /** A redstone signal on the controller: a rising edge asks for a start. */
    public void onRedstoneSignal(boolean powered) {
        if (powered && !redstonePowered) {
            requestStart();
        }
        if (powered != redstonePowered) {
            redstonePowered = powered;
            setChanged();
        }
    }

    @Override
    public TripCause requestStart() {
        if (level == null || level.isClientSide) {
            return TripCause.NONE;
        }
        TripCause result;
        TokamakPhase before = sequence.phase();
        if (structure.isEmpty()) {
            result = TripCause.STRUCTURE;
            sequence.trip(TripCause.STRUCTURE);
        } else {
            TokamakStructure tokamak = structure.get();
            if (supply == TokamakSupply.NONE || supplyOverride != null) {
                supply = collectSupply(tokamak);
            }
            fieldTarget = readMagnetDrive(tokamak);
            result = sequence.requestStart(FusionConfig.startupParams(),
                    conditions(tokamak, FusionConfig.vacuumParams(), FusionConfig.magnetParams()));
        }
        if (before == TokamakPhase.OFFLINE) {
            if (result == TripCause.NONE) {
                notifyOwner(Component.translatable("create_nuclearindustry.tokamak.message.starting").withStyle(ChatFormatting.AQUA), true);
            } else {
                notifyOwner(Component.translatable("create_nuclearindustry.tokamak.message.start_blocked",
                        Component.translatable(result.langKey())).withStyle(ChatFormatting.RED), true);
            }
        }
        markDirtyAndSync();
        return result;
    }

    @Override
    public void requestShutdown() {
        handle(sequence.requestShutdown());
        markDirtyAndSync();
    }

    @Override
    public void setFuelRate(double millibucketsPerTick) {
        double clamped = Double.isFinite(millibucketsPerTick) ? Math.max(0, Math.min(maxFuelRate(), millibucketsPerTick)) : fuelRate;
        fuelRateSet = true;
        if (clamped != fuelRate) {
            fuelRate = clamped;
            markDirtyAndSync();
        }
    }

    @Override
    public void setDtRatio(double deuteriumShare) {
        double clamped = Double.isFinite(deuteriumShare) ? Math.max(0, Math.min(1, deuteriumShare)) : dtRatio;
        if (clamped != dtRatio) {
            dtRatio = clamped;
            markDirtyAndSync();
        }
    }

    @Override
    public void setHeating(boolean enabled, double level) {
        double clamped = Double.isFinite(level) ? Math.max(0, Math.min(1, level)) : heatingLevel;
        if (enabled != heatingEnabled || clamped != heatingLevel) {
            heatingEnabled = enabled;
            heatingLevel = clamped;
            markDirtyAndSync();
        }
    }

    @Override
    public double maxFuelRate() {
        int volume = structure.map(TokamakStructure::plasmaVolume).orElse(0);
        return Math.max(1.0, PlasmaModel.fuelRateFor(FusionConfig.plasmaParams(), volume, MAX_FUEL_DENSITY));
    }

    /** The player working the controls gets the tokamak's messages. */
    public void setOperator(Player player) {
        messenger.setOwner(player);
    }

    public boolean isHeatingEnabled() {
        return heatingEnabled;
    }

    public double getHeatingLevel() {
        return heatingLevel;
    }

    public double getFuelRate() {
        return fuelRate;
    }

    public double getDtRatio() {
        return dtRatio;
    }

    /** Fuelling that holds this tokamak at the configured share of its density limit. */
    public double defaultFuelRate() {
        int volume = structure.map(TokamakStructure::plasmaVolume).orElse(0);
        return PlasmaModel.fuelRateFor(FusionConfig.plasmaParams(), volume, FusionConfig.defaultDensity);
    }

    @Override
    public TokamakPhase phase() {
        return level != null && level.isClientSide ? clientReadings.phase() : sequence.phase();
    }

    @Override
    public TripCause tripCause() {
        return level != null && level.isClientSide ? clientReadings.tripCause() : sequence.tripCause();
    }

    @Override
    public TokamakReadings readings() {
        if (level != null && level.isClientSide) {
            return clientReadings;
        }
        if (structure.isEmpty()) {
            return TokamakReadings.NONE;
        }
        PlasmaModel plasma = physics.model();
        return new TokamakReadings(true, status == Status.SUSPENDED, getMajorRadius(), sequence.phase(), sequence.tripCause(),
                plasma.getTemperature(), plasma.getInstability(), plasma.getConfinement(), plasma.getDensity(),
                plasma.ashFraction(), plasma.getFusionPower(), plasma.getHeatingPower(), plasma.gain(),
                plasma.getBurnRate(), plasma.getTritiumBreedingRatio(), plasma.getMode() == PlasmaModel.Mode.DT,
                magnets.getField(), fieldTarget, magnets.getTemperature(), vacuum.getPressure(),
                fuelRate, dtRatio, heatingEnabled, heatingLevel,
                deuteriumTank.getFluidAmount(), tritiumTank.getFluidAmount(), coolantTank.getFluidAmount(),
                plasmaSteamTank.getFluidAmount(), liquidHeliumTank.getFluidAmount(), warmHeliumTank.getFluidAmount(),
                tritiumOverflowTicks > 0);
    }

    /** What the heaters and pumps offered on the last tick. */
    public TokamakSupply getSupply() {
        return supply;
    }

    /** Game tests only: stand in for the shell's heaters and pumps; null goes back to the real ones. */
    public void setSupplyOverride(TokamakSupply override) {
        supplyOverride = override;
    }

    /**
     * Game tests only: skips the slow parts of a start, as if the chamber had been pumped down and the
     * coils cooled and energised already. The sequence still walks every phase.
     */
    public void primeForTest() {
        vacuum.restore(FusionConfig.vacuumParams().basePressure());
        magnets.restore(1.0, MagnetModel.OPERATING_TEMPERATURE);
    }

    public CryoTanks cryoTanks() {
        return cryoTanks;
    }

    /**
     * Ponder only: makes this schematic controller show a plasma of the given state around
     * {@code center}, with no structure or physics behind it.
     */
    public void previewPlasma(BlockPos center, TokamakReadings readings) {
        dispCenter = center.immutable();
        dispRadius = readings.majorRadius();
        clientReadings = readings;
    }

    /** The point on the axis at the plasma's mid-plane; synced to clients for the renderer. */
    public BlockPos getCenter() {
        return level != null && level.isClientSide ? dispCenter : structure.map(s -> s.center).orElse(BlockPos.ZERO);
    }

    public Status getStatus() {
        return status;
    }

    public Optional<TokamakStructure> getStructure() {
        return structure;
    }

    public TokamakPhysicsSimulator getPhysics() {
        return physics;
    }

    /** Field the magnet drives ask for, 0..1. */
    public double getMagnetDrive() {
        return level != null && level.isClientSide ? clientReadings.fieldTarget() : fieldTarget;
    }

    public boolean isFormed() {
        return level != null && level.isClientSide ? dispRadius > 0 : structure.isPresent();
    }

    public int getMajorRadius() {
        return level != null && level.isClientSide ? dispRadius : structure.map(TokamakStructure::majorRadius).orElse(0);
    }

    public int getCoilCount() {
        return level != null && level.isClientSide ? dispCoils : structure.map(s -> s.coilCount).orElse(0);
    }

    public int getBlanketCount() {
        return level != null && level.isClientSide ? dispBlankets : structure.map(s -> s.blanketCount).orElse(0);
    }

    public double getCoilCoverage() {
        return level != null && level.isClientSide ? dispCoverage
                : structure.map(s -> s.coilCoverage(FusionConfig.coilsPerSector)).orElse(0.0);
    }

    /** Why the tokamak is not formed (or its first warning), as a lang key with arguments; see {@link #getValidationText()}. */
    public String getValidationKey() {
        return level != null && level.isClientSide ? dispValidationMessage : lastValidation.primaryMessage();
    }

    /** Why the tokamak is not formed, or its first warning, in the reader's language. */
    public Component getValidationText() {
        return TokamakStructureValidator.text(getValidationKey());
    }

    /** {@link #getValidationText()} resolved in the server's language (English), for plain-text readers. */
    public String getValidationMessage() {
        return getValidationText().getString();
    }

    public FluidTank getDeuteriumTank() {
        return deuteriumTank;
    }

    public FluidTank getTritiumTank() {
        return tritiumTank;
    }

    public FluidTank getCoolantTank() {
        return coolantTank;
    }

    public FluidTank getPlasmaSteamTank() {
        return plasmaSteamTank;
    }

    /** Routes deuterium and tritium into their own tanks. */
    public int fillFuel(FluidStack resource, IFluidHandler.FluidAction action) {
        if (NuclearFluidHelper.isDeuterium(resource)) {
            return deuteriumTank.fill(resource, action);
        }
        if (NuclearFluidHelper.isTritium(resource)) {
            return tritiumTank.fill(resource, action);
        }
        return 0;
    }

    public FluidStack drainSurplusTritium(int maxDrain, IFluidHandler.FluidAction action) {
        int surplus = Math.max(0, tritiumTank.getFluidAmount() - TRITIUM_RESERVE);
        return surplus <= 0 ? FluidStack.EMPTY : tritiumTank.drain(Math.min(maxDrain, surplus), action);
    }

    /** Coil temperature, kelvin. */
    public double getMagnetTemperature() {
        return level != null && level.isClientSide ? clientReadings.magnetTemperature() : magnets.getTemperature();
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
        tooltip.add(Component.translatable("create_nuclearindustry.tokamak.goggles.title").withStyle(ChatFormatting.AQUA));
        if (!isFormed()) {
            tooltip.add(Component.literal("  ").append(getValidationText()).withStyle(ChatFormatting.RED));
            return true;
        }
        TokamakReadings r = readings();
        tooltip.add(line("structure", getMajorRadius(), getCoilCount(), Math.round(getCoilCoverage() * 100), getBlanketCount())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(line("phase", Component.translatable(r.phase().langKey()))
                .withStyle(r.phase() == TokamakPhase.BURN ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        if (r.tripCause() != TripCause.NONE) {
            tooltip.add(line("trip", Component.translatable(r.tripCause().langKey())).withStyle(ChatFormatting.RED));
        }
        if (r.suspended()) {
            tooltip.add(line("suspended").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(line("plasma", Math.round(r.temperature()), Math.round(r.density() * 100), Math.round(r.ashFraction() * 100))
                .withStyle(r.temperature() >= PlasmaModel.DANGER_TEMP || r.density() > PlasmaModel.DENSITY_LIMIT
                        ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(line("power", Math.round(r.fusionPower()), Math.round(r.heatingPower()), gainText(r.gain()))
                .withStyle(r.fusionPower() > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(line("stability", Math.round(r.confinement() * 100), Math.round(r.instability()))
                .withStyle(r.instability() > PlasmaModel.INSTABILITY_WARNING ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(line("magnets", Math.round(r.field() * 100), String.format("%.1f", r.magnetTemperature()), vacuumText(r.vacuum()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(line("fuel", r.deuterium(), r.tritium(), String.format("%.2f", r.tritiumBreeding())).withStyle(ChatFormatting.GRAY));
        tooltip.add(line("coolant", r.coolant(), r.plasmaSteam()).withStyle(ChatFormatting.GRAY));
        tooltip.add(line("helium", r.liquidHelium(), r.warmHelium()).withStyle(ChatFormatting.GRAY));
        if (r.tritiumOverflow()) {
            tooltip.add(line("tritium_overflow").withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.literal("  ").append(Component.translatable("create_nuclearindustry.tokamak.goggles." + key, args));
    }

    /** Q as text: a number, or the infinity sign once the plasma is ignited. */
    public static String gainText(double gain) {
        return Double.isInfinite(gain) ? "∞" : String.format("%.1f", gain);
    }

    /** Chamber pressure as text, in pascals. */
    public static String vacuumText(double log10Pascals) {
        return String.format("%.0e Pa", Math.pow(10, log10Pascals)).replace("e+0", "e").replace("e-0", "e-");
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeCommon(tag, provider);
        tag.putDouble("deuteriumBalance", deuteriumBalance);
        tag.putDouble("tritiumBalance", tritiumBalance);
        tag.putDouble("tritiumCredit", tritiumCredit);
        tag.putDouble("liquidHeliumBalance", liquidHeliumBalance);
        tag.putDouble("warmHeliumBalance", warmHeliumBalance);
        tag.putDouble("coolantBalance", coolantBalance);
        tag.putDouble("magnetField", magnets.getField());
        tag.putDouble("magnetTemperature", magnets.getTemperature());
        tag.putDouble("vacuum", vacuum.getPressure());
        tag.putInt("phaseTicks", sequence.phaseTicks());
        tag.putBoolean("pendingStart", sequence.pendingStart());
        tag.putBoolean("redstonePowered", redstonePowered);
        tag.putBoolean("fuelRateSet", fuelRateSet);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readCommon(tag, provider);
        deuteriumBalance = tag.getDouble("deuteriumBalance");
        tritiumBalance = tag.getDouble("tritiumBalance");
        tritiumCredit = tag.getDouble("tritiumCredit");
        liquidHeliumBalance = tag.getDouble("liquidHeliumBalance");
        warmHeliumBalance = tag.getDouble("warmHeliumBalance");
        coolantBalance = tag.getDouble("coolantBalance");
        magnets.restore(tag.getDouble("magnetField"),
                tag.contains("magnetTemperature") ? tag.getDouble("magnetTemperature") : MagnetModel.AMBIENT_TEMPERATURE);
        vacuum.restore(tag.contains("vacuum") ? tag.getDouble("vacuum") : VacuumModel.ATMOSPHERE);
        redstonePowered = tag.getBoolean("redstonePowered");
        fuelRateSet = tag.getBoolean("fuelRateSet");
        boolean legacy = !tag.contains("phase");
        if (legacy) {
            // Saved before the startup sequence: the plasma cannot carry on without vacuum and cold
            // coils, so it is shut down cleanly and the owner is told what the tokamak now needs.
            boolean wasBurning = physics.wasLegacyBurning();
            physics.clearLegacy();
            physics.quench();
            sequence.restore(TokamakPhase.OFFLINE, wasBurning ? TripCause.STRUCTURE : TripCause.NONE, 0, false);
            upgradeNotice = wasBurning;
            if (wasBurning) {
                LOGGER.info("Tokamak at {} was burning in an older version; it is now offline and needs a start", getBlockPos());
            }
        } else {
            sequence.restore(TokamakReadingsNbt.phase(tag.getString("phase")), TokamakReadingsNbt.trip(tag.getString("tripCause")),
                    tag.getInt("phaseTicks"), tag.getBoolean("pendingStart"));
        }
        if (tag.contains("fuelInventory")) {
            migrateLegacyPellets(tag.getCompound("fuelInventory"), provider);
        }
        pendingRevalidation = true;
    }

    private void writeCommon(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("status", status.name());
        CompoundTag physicsTag = new CompoundTag();
        physics.serializeNBT(physicsTag);
        tag.put("physics", physicsTag);
        tag.put("deuterium", deuteriumTank.writeToNBT(provider, new CompoundTag()));
        tag.put("tritium", tritiumTank.writeToNBT(provider, new CompoundTag()));
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("plasmaSteam", plasmaSteamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("liquidHelium", liquidHeliumTank.writeToNBT(provider, new CompoundTag()));
        tag.put("warmHelium", warmHeliumTank.writeToNBT(provider, new CompoundTag()));
        tag.putDouble("fuelRate", fuelRate);
        tag.putDouble("dtRatio", dtRatio);
        tag.putBoolean("heatingEnabled", heatingEnabled);
        tag.putDouble("heatingLevel", heatingLevel);
        tag.putString("phase", sequence.phase().name());
        tag.putString("tripCause", sequence.tripCause().name());
    }

    private void readCommon(CompoundTag tag, HolderLookup.Provider provider) {
        try {
            status = Status.valueOf(tag.getString("status"));
        } catch (IllegalArgumentException ignored) {
            status = Status.UNFORMED;
        }
        physics.deserializeNBT(tag.getCompound("physics"));
        deuteriumTank.readFromNBT(provider, tag.getCompound("deuterium"));
        tritiumTank.readFromNBT(provider, tag.getCompound("tritium"));
        coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        plasmaSteamTank.readFromNBT(provider, tag.getCompound("plasmaSteam"));
        liquidHeliumTank.readFromNBT(provider, tag.getCompound("liquidHelium"));
        warmHeliumTank.readFromNBT(provider, tag.getCompound("warmHelium"));
        fuelRate = Math.max(0, tag.getDouble("fuelRate"));
        dtRatio = tag.contains("dtRatio") ? Math.max(0, Math.min(1, tag.getDouble("dtRatio"))) : 0.5;
        heatingEnabled = !tag.contains("heatingEnabled") || tag.getBoolean("heatingEnabled");
        heatingLevel = tag.contains("heatingLevel") ? Math.max(0, Math.min(1, tag.getDouble("heatingLevel"))) : 1.0;
    }

    /**
     * Controllers from the flat fusion ring kept D-T pellets. Each unused pellet becomes
     * 250 mB of deuterium and of tritium; spent pellets are dropped next to the controller.
     */
    private void migrateLegacyPellets(CompoundTag inventoryTag, HolderLookup.Provider provider) {
        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.deserializeNBT(provider, inventoryTag);
        ItemStack pellets = legacy.getStackInSlot(0);
        if (pellets.is(AllNuclearItems.DT_FUEL_PELLET.get())) {
            int amount = pellets.getCount() * LEGACY_PELLET_FLUID;
            deuteriumTank.fill(new FluidStack(AllNuclearFluids.DEUTERIUM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
            tritiumTank.fill(new FluidStack(AllNuclearFluids.TRITIUM.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        }
        ItemStack spent = legacy.getStackInSlot(1);
        if (!spent.isEmpty()) {
            legacyDrops.add(spent.copy());
        }
    }

    private void dropLegacyItems() {
        if (legacyDrops.isEmpty() || level == null) {
            return;
        }
        BlockPos pos = getBlockPos();
        for (ItemStack stack : legacyDrops) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        }
        legacyDrops.clear();
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        writeCommon(tag, provider);
        tag.putInt("dispRadius", structure.map(TokamakStructure::majorRadius).orElse(0));
        tag.putInt("dispCoils", structure.map(s -> s.coilCount).orElse(0));
        tag.putInt("dispBlankets", structure.map(s -> s.blanketCount).orElse(0));
        tag.putDouble("dispCoverage", structure.map(s -> s.coilCoverage(FusionConfig.coilsPerSector)).orElse(0.0));
        tag.putString("dispValidationMessage", lastValidation.primaryMessage());
        tag.putLong("dispCenter", structure.map(s -> s.center).orElse(BlockPos.ZERO).asLong());
        tag.put("readings", TokamakReadingsNbt.write(readings()));
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
        readCommon(tag, provider);
        dispRadius = tag.getInt("dispRadius");
        dispCoils = tag.getInt("dispCoils");
        dispBlankets = tag.getInt("dispBlankets");
        dispCoverage = tag.getDouble("dispCoverage");
        dispValidationMessage = tag.getString("dispValidationMessage");
        dispCenter = BlockPos.of(tag.getLong("dispCenter"));
        clientReadings = TokamakReadingsNbt.read(tag.getCompound("readings"));
    }

    /** A tokamak block that proxies onto the controller. */
    public interface TokamakPart {
        void link(BlockPos controllerPos);

        void unlink(BlockPos controllerPos);
    }
}
