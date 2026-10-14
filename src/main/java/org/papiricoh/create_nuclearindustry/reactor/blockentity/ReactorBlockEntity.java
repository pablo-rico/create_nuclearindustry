package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;
import org.papiricoh.create_nuclearindustry.panel.PanelSource;
import org.papiricoh.create_nuclearindustry.reactor.display.FissionPanel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PressurizerStack;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PrimaryLoop;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.CladdingOxidationModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.PlantAlarm;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReactorAccidents;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReleasePath;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.SteamExplosion;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.PressurizerHeaterPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.ReliefValveBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelHooks;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.StructureSnapshot;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.reactor.block.NuclearReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.reactor.control.AutoScram;
import org.papiricoh.create_nuclearindustry.reactor.physics.FuelKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.SpentAssembly;
import org.papiricoh.create_nuclearindustry.reactor.control.ControlRodTracker;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ReactivityModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.FissionCore;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.CoreThermalModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorSoundModel;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks;
import org.papiricoh.create_nuclearindustry.reactor.visual.RodVisualSync;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ReactorBlockEntity extends BlockEntity implements PanelSource, Clearable, MultiblockController, RadiationEmitter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TANK_CAPACITY = 16_000;
    /**
     * Heat one heat exchanger takes from the core, MW: an exchanger moves heat, so its rating does not
     * depend on how much heat a mB of steam carries ({@link FissionConfig#steamEnergy}).
     */
    private static final double HEAT_EXCHANGER_MW = 160.0;
    /** Water in or steam out one fluid port passes, mB/t: a port moves fluid, so this is in mB. */
    private static final int FLUID_PORT_STEAM_PER_TICK = 160;
    // Balance parameters for external coolant heat removal.
    /** Water that keeps the whole core covered, mB: less uncovers part of it. */
    private static final int CORE_WATER = 4000;
    /** Heat units of the pressurized loop's coolant model per MJ the core gives it. */
    private static final double HEAT_UNITS_PER_MJ = 1.0;
    private static final int CONTROL_ROD_SCAN_INTERVAL = 5;
    private static final int SCRAM_RECOVERY_SCAN_INTERVAL = 20;
    private static final int LOAD_MELTDOWN_GRACE_TICKS = 200;
    /** 2: a manual SCRAM no longer forces the rods in. Absent in older saves. */
    private static final int SCRAM_VERSION = 2;
    /** Share of the xenon worth a shut-down core must reach to count as in the iodine pit. */
    private static final double IODINE_PIT_FRACTION = 0.6;
    private static final float MELTDOWN_EXPLOSION_POWER = 80.0f;
    /** Most blocks of corium one meltdown leaves; the rest of the molten fuel joins them. */
    public static final int MAX_CORIUM = 16;
    /** Water in the vessel below which the core counts as dried out, for a steam explosion on reflooding. */
    private static final int DRY_CORE = 1_000;
    /** Coolant a breached vessel loses per tick, mB. */
    private static final int BREACH_LEAK_PER_TICK = 500;
    /** Temperature of the steam flashing from a pressurized primary, K. */
    private static final double PRIMARY_STEAM_K = 618.0;
    /** Temperature of the steam off water hitting a molten core, and of hydrogen leaving the core, K. */
    private static final double CORE_GAS_K = 1_000.0;
    /** Core cooling when water flashes on a molten core, °C. */
    private static final double REFLOOD_QUENCH = 400.0;
    /** Share of the cladding still unoxidised that reacts in the melt itself. */
    private static final double MELT_OXIDATION = 0.5;
    public static final int FUEL_INPUT_SLOTS = 4;
    public static final int FUEL_OUTPUT_SLOTS = 2;
    public static final int FUEL_SLOT_COUNT = FUEL_INPUT_SLOTS + FUEL_OUTPUT_SLOTS;
    public static final float MIN_FUEL_ENRICHMENT = EnrichmentTier.LOW.min();
    /** Lang key prefixes of the reactor's chat messages, goggle lines and statuses. */
    public static final String MESSAGE = "create_nuclearindustry.reactor.message.";
    public static final String GOGGLES = "create_nuclearindustry.reactor.goggles.";
    public static final String STATUS = "create_nuclearindustry.reactor.status.";
    public static final String PORT = "create_nuclearindustry.reactor.port.";
    /** Validation message of a reactor that has not been checked yet. */
    public static final String NOT_FORMED = "create_nuclearindustry.reactor.validation.not_formed";

    private ReactorStatus status = ReactorStatus.UNFORMED;
    private Optional<ReactorStructureValidator.ReactorStructure> currentStructure = Optional.empty();
    private MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> lastValidation =
            MultiblockValidationResult.invalid(List.of(NOT_FORMED), List.of());
    private ReactorPhysicsSimulator physicsSimulator = new ReactorPhysicsSimulator(0);
    private boolean pendingRevalidation = true;
    private int syncCounter;
    private int controlRodScanCounter;
    /**
     * A block changed where it can make a column a control channel or stop it being one: the next
     * rod scan compares the channel set (see {@link #checkControlChannels}) instead of revalidating.
     */
    private boolean pendingChannelCheck;
    /** The last channel check already found a different set; a second one in a row revalidates. */
    private boolean channelMismatchSeen;
    private int scramRecoveryScanCounter;
    private int ticksSinceLoad;
    /** Enrichment of each burnt-out assembly waiting for space in the output slots. */
    private final List<SpentAssembly> pendingDepletedFuel = new ArrayList<>();
    /** Set from the control panel: holds every rod fully inserted regardless of the physical rods. */
    private boolean manualScram;
    /** A reactor saved in manual SCRAM when that still forced the rods in keeps them in until reset. */
    private boolean legacyScramHold;
    private final AutoScram autoScram = new AutoScram();
    /** Rod insertion a control panel asks for, 0-1, or below 0 while no panel has set one. */
    private float rodTarget = -1.0f;
    /** A meltdown leaves fallout that fades over a few in-game days. */
    private static final int MELTDOWN_CONTAMINATION_TICKS = 72_000;
    private static final double MELTDOWN_RADIATION = 40.0;
    /** Ground contamination a meltdown leaves: this many rad/s at the reactor, nothing beyond the radius. */
    static final int MELTDOWN_FALLOUT_RADIUS = 48;
    static final float MELTDOWN_FALLOUT_INTENSITY = 20.0f;
    private ControlRodTracker.ControlRodScan lastControlRodScan = ControlRodTracker.ControlRodScan.empty();
    private ReactorDisplaySnapshot displaySnapshot = ReactorDisplaySnapshot.empty();
    private boolean hasDisplaySnapshot;
    private final MultiblockMessenger messenger = new MultiblockMessenger("Reactor");
    private StructureSnapshot structureSnapshot;
    /** The pressurized loop, run instead of boiling when the coolant is primary coolant and a pressurizer stands on the lid. */
    private final PrimaryLoop primaryLoop = new PrimaryLoop();
    /** Cladding, vessel and containment: the accident chain of this reactor. */
    private final ReactorAccidents accidents = new ReactorAccidents();
    /** What the fuel channels show: fuel, glow and coolant. */
    private final RodVisualSync rodVisuals = new RodVisualSync();
    /** Validations run so far, so tests can tell that the channels' own updates never cause one. */
    private int validations;
    /** The last chat message, kept for tests. */
    private Component lastMessage = Component.empty();
    /** Steam boiled but not yet a whole mB. */
    private double boilCarry;
    /** Core damage at the last accident tick, so new damage releases what it frees. */
    private double lastDamage;
    private boolean fuelMeltingAlerted;
    /** Damage at which the operator is warned that the fuel is failing. */
    private static final double FUEL_DAMAGE_ALERT = 0.1;
    /** Whether the SCRAM signal was on last tick, so the rods drop with a thud once. */
    private boolean scramShown;
    /** Set by a SCRAM of a hot core, cleared once it is cold: a cold shutdown. */
    private boolean coolingAfterScram;
    /** Reached full power and has not fallen well below it since. */
    private boolean atFullPower;

    /**
     * Least ticks between two client updates, unless the state the clients act on changes (see
     * {@link #syncState()}): a player action goes out at once if the last update is this old, held
     * back until it is otherwise.
     */
    public static final int SYNC_INTERVAL = 20;
    /** Steps of the readings that count as a visible change (see {@link #fillVisibleKey}). */
    private static final double SYNC_TEMPERATURE_STEP = 2.0;
    private static final double SYNC_POWER_STEPS = 200.0;
    /** Steps per decade of the power on the logarithmic scale, so a source-range startup shows. */
    private static final double SYNC_LOG_POWER_STEPS = 20.0;
    private static final double SYNC_REACTIVITY_STEP = 10.0;
    private static final double SYNC_MARGIN_STEP = 10.0;
    private static final double SYNC_REACTIVITY_PART_STEP = 100.0;
    /**
     * Least change of a tank's percent that goes out on its own: a tank drained or filled slowly
     * would otherwise send an update at every percent it crosses. Empty and full always go out.
     */
    private static final int SYNC_TANK_HYSTERESIS = 2;
    /** Where the tanks' percents sit in the visible key, compared with {@link #SYNC_TANK_HYSTERESIS}. */
    private static final int KEY_COOLANT_PERCENT = 1;
    private static final int KEY_STEAM_PERCENT = 3;
    private static final int VISIBLE_KEY_SIZE = 45;
    /** Something a player did (or the reactor's state) changed since the last update: it goes out when allowed. */
    private boolean syncDirty;
    /** Tanks or fuel slots changed since the last update; they go out once the change is visible. */
    private boolean contentsDirty;
    private long lastSyncTime = Long.MIN_VALUE / 2;
    /** What the clients see, quantized, as last sent and as now: readings go out when the two differ. */
    private final int[] sentKey = new int[VISIBLE_KEY_SIZE];
    private final int[] visibleKey = new int[VISIBLE_KEY_SIZE];
    /** The loaded assemblies, asked for only when the fuel channels refresh. */
    private final java.util.function.Supplier<List<ReactorPhysicsSimulator.AssemblyView>> assemblyViews =
            () -> physicsSimulator.assemblyViews();
    /** {@link #syncState()} as the clients last got it: a change is sent at once. */
    private int lastSyncedState = -1;
    /** The structure as last sent in an update packet; later packets leave it out while it stands. */
    @org.jetbrains.annotations.Nullable
    private ReactorStructureValidator.ReactorStructure lastSentStructure;
    private int clientSyncs;
    /** Visible keys compared with the last sent one, for tests. */
    private int visibleKeyChecks;
    /** Last time {@link #flushSync} compared the visible keys (or an update filled the sent one). */
    private long lastKeyCheckTime = Long.MIN_VALUE / 2;
    /** Contents or readings may have changed since the last comparison. */
    private boolean keyCheckDue;
    /** {@link #radiationOrigin()} as worked out for {@link #radiationOriginOf}; null when it must be redone. */
    @org.jetbrains.annotations.Nullable
    private Vec3 radiationOrigin;
    private Optional<ReactorStructureValidator.ReactorStructure> radiationOriginOf = Optional.empty();

    private final FluidTank coolantTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isReactorCoolant) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private final FluidTank steamTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isReactorOutput) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private final ItemStackHandler fuelInventory = new ItemStackHandler(FUEL_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < FUEL_INPUT_SLOTS) {
                return isFreshFuel(stack);
            }
            return isSpentFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            contentsChanged();
        }
    };
    private final IItemHandler fuelInputHandler = new FuelSlotView(true);
    private final IItemHandler fuelOutputHandler = new FuelSlotView(false);

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReactorBlockEntity entity) {
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            ReactorVisualHooks.reactorClientTick.accept(entity);
            return;
        }

        if (entity.pendingRevalidation) {
            entity.revalidate(false, null);
        }
        entity.ticksSinceLoad++;

        if (entity.status == ReactorStatus.FORMED) {
            entity.checkAutoScram();
            entity.updateControlRodInsertion(false);
            entity.tryStorePendingDepletedFuel();
            entity.tryLoadFuelAssembly();
            boolean stillRunning = entity.physicsSimulator.tick();
            entity.collectSpentFuel();
            entity.processLoops();
            if (entity.runAccidents() || entity.isRemoved()) {
                // an explosion took the reactor apart; nothing of it is left to update this tick
                return;
            }
            entity.updateControllerRunningState();
            if (!stillRunning) {
                if (entity.canTriggerMeltdown()) {
                    entity.handleMeltdown();
                } else {
                    entity.status = ReactorStatus.SCRAMMED_HOT;
                    entity.physicsSimulator.forceScramForLoad();
                    entity.alertMessage(message(ChatFormatting.RED, "scrammed_hot_load"));
                    entity.markDirtyAndSync();
                }
                return;
            }
        } else if (entity.status == ReactorStatus.SCRAMMED || entity.status == ReactorStatus.SCRAMMED_HOT) {
            entity.physicsSimulator.scramTick();
            if (entity.currentStructure.isPresent() && entity.status == ReactorStatus.SCRAMMED_HOT && !entity.physicsSimulator.isMeltingDown()) {
                entity.status = ReactorStatus.FORMED;
                entity.markDirtyAndSync();
            } else if (entity.currentStructure.isEmpty() && ++entity.scramRecoveryScanCounter >= SCRAM_RECOVERY_SCAN_INTERVAL) {
                entity.scramRecoveryScanCounter = 0;
                entity.revalidate(false, null);
            }
            entity.updateControllerRunningState();
        }
        entity.tickCuesAndVisuals();

        boolean readingsDue = false;
        if (++entity.syncCounter >= 20) {
            entity.setChanged();
            entity.syncCounter = 0;
            entity.checkIodinePit();
            readingsDue = true;
        }
        entity.flushSync(readingsDue);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.register(this);
            RadiationEmitters.register(this);
            ticksSinceLoad = 0;
        }
    }

    // ------------------------------------------------------------------ radiation

    /**
     * The middle of the reactor's core. Asked for every emitter on each reindex of the radiation
     * sources, so it is worked out once per structure (see {@link #setStructure}) and kept.
     */
    @Override
    public Vec3 radiationOrigin() {
        Vec3 origin = radiationOrigin;
        if (origin == null || radiationOriginOf != currentStructure) {
            // also covers a structure read from a packet, which does not go through setStructure
            origin = currentStructure.map(structure -> structure.bounds().getCenter().getCenter())
                    .orElseGet(() -> Vec3.atCenterOf(getBlockPos()));
            radiationOrigin = origin;
            radiationOriginOf = currentStructure;
        }
        return origin;
    }

    /** Proportional to the chain reaction: an idle or unformed reactor emits nothing. */
    @Override
    public double radiationStrength() {
        if (status == ReactorStatus.UNFORMED || currentStructure.isEmpty()) {
            return 0.0;
        }
        return Config.reactorEmissionPerNeutron * physicsSimulator.getNeutronLevel();
    }

    /** Radiation leaves the core freely; only the shell and what lies beyond it shield. */
    @Override
    public BoundingBox radiationIgnoreBox() {
        return currentStructure.map(structure -> structure.bounds().inflatedBy(-1)).orElse(null);
    }

    @Override
    public void setRemoved() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.unregister(this);
            RadiationEmitters.unregister(this);
            // The controller was broken or blown up (not just unloaded): its channels go dark.
            BlockPos pos = getBlockPos();
            if (level.isLoaded(pos) && !level.getBlockState(pos).is(AllNuclearBlocks.REACTOR_CONTROLLER.get())) {
                rodVisuals.reset(level, currentStructure.map(RodVisualSync::rodsOf).orElse(List.of()));
            }
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
        if (currentStructure.isPresent()) {
            return currentStructure.get().interestBounds();
        }
        BlockPos pos = getBlockPos();
        int halfWidth = ReactorStructureValidator.MAX_WIDTH / 2;
        return new BoundingBox(pos.getX() - halfWidth, pos.getY() - ReactorStructureValidator.MAX_HEIGHT, pos.getZ() - halfWidth,
                pos.getX() + halfWidth, pos.getY(), pos.getZ() + halfWidth);
    }

    /**
     * Rods move through their channels all the time (Create pistons), so a change in a control channel
     * or in the rods' travel space never revalidates by itself: it only flags a channel check, which
     * the next rod scan runs cheaply. As this is the only hook that sees the position, the flag is set
     * here and the method answers false. Everything else revalidates when it differs from the snapshot.
     */
    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        if (currentStructure.isPresent()) {
            ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
            if (structure.isControlArea(change.pos())) {
                pendingChannelCheck = true;
                return false;
            }
            if (structureSnapshot != null && structureSnapshot.isAffectedBy(change)) {
                return true;
            }
            if (change.pos().getY() > structure.topY && structure.isChannelReach(change.pos())) {
                // A rod lowered towards a column that is not a channel yet; below the lid the snapshot
                // already catches every change of block (fuel channels repainting their state are not one).
                pendingChannelCheck = true;
            }
            return false;
        }
        return change.removed() || change.isMultiblockPart();
    }

    private void setStructure(Optional<ReactorStructureValidator.ReactorStructure> structure) {
        boolean boundsChanged = !currentStructure.map(ReactorStructureValidator.ReactorStructure::bounds)
                .equals(structure.map(ReactorStructureValidator.ReactorStructure::bounds));
        Optional<ReactorStructureValidator.ReactorStructure> previous = currentStructure;
        currentStructure = structure;
        radiationOrigin = null;
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            if (structure.isEmpty()) {
                rodVisuals.reset(level, previous.map(RodVisualSync::rodsOf).orElse(List.of()));
            } else if (previous.isEmpty() || previous.get() != structure.get()) {
                rodVisuals.forgetOutside(level, RodVisualSync.rodsOf(structure.get()));
            }
        }
        structureSnapshot = structure.isPresent() && level != null && !level.isClientSide
                ? StructureSnapshot.capture(level, snapshotPositions(structure.get()))
                : null;
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    /** The structure plus the space above the lid, so building or breaking a pressurizer revalidates. */
    private static List<BlockPos> snapshotPositions(ReactorStructureValidator.ReactorStructure structure) {
        List<BlockPos> positions = new ArrayList<>(structure.allBlocks);
        positions.addAll(structure.pressurizerZone());
        return positions;
    }

    private void revalidate(boolean playerInitiated, Player player) {
        messenger.setOwner(player);
        pendingRevalidation = false;
        // the validation detects the channels afresh
        pendingChannelCheck = false;
        channelMismatchSeen = false;

        Level level = getLevel();
        if (level == null) {
            return;
        }
        validations++;

        ReactorStatus previousStatus = status;
        MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> result =
                ReactorStructureValidator.validate(level, getBlockPos(), holdsPrimaryCoolant());
        lastValidation = result;

        if (result.formed() && result.structure().isPresent()) {
            scramRecoveryScanCounter = 0;
            ReactorStructureValidator.ReactorStructure newStructure = result.structure().get();
            boolean structureChanged = currentStructure.isEmpty() || !sameStructure(currentStructure.get(), newStructure);
            boolean loadedStructureWithoutSnapshot = currentStructure.isEmpty()
                    && (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED || previousStatus == ReactorStatus.SCRAMMED_HOT);
            setStructure(Optional.of(newStructure));
            status = previousStatus == ReactorStatus.SCRAMMED_HOT && ticksSinceLoad < LOAD_MELTDOWN_GRACE_TICKS
                    ? ReactorStatus.SCRAMMED_HOT
                    : ReactorStatus.FORMED;
            // A reactor just loaded keeps the simulator it saved, unless the core was changed while unloaded.
            if (structureChanged && (!loadedStructureWithoutSnapshot || !simulatorMatches(newStructure))) {
                ReactorPhysicsSimulator previousSimulator = physicsSimulator;
                physicsSimulator = new ReactorPhysicsSimulator(newStructure.getUraniumRodCount(), newStructure.getControlRodCount(),
                        newStructure.moderation, newStructure.reflection);
                physicsSimulator.inheritFuel(previousSimulator);
                physicsSimulator.inheritThermalState(previousSimulator);
                collectSpentFuel();
            }
            updateControlRodInsertion(true);
            linkStructureParts(newStructure);
            if (playerInitiated && accidents.vessel().breached()) {
                // the player rebuilt and formed it again: a new vessel
                accidents.vessel().repair();
            }
            if (status == ReactorStatus.FORMED && (playerInitiated || previousStatus != ReactorStatus.FORMED)) {
                awardMachine(NuclearEvent.REACTOR_FORMED);
                if (newStructure.moderation > 0 || newStructure.reflection > 0) {
                    awardMachine(NuclearEvent.MODERATED_CORE);
                }
            }
            if (playerInitiated || previousStatus != ReactorStatus.FORMED) {
                statusMessage(playerInitiated ? player : null, status == ReactorStatus.FORMED
                        ? message(ChatFormatting.DARK_GREEN, "formed",
                                Component.literal("[" + newStructure.dimensionsText() + "]").withStyle(ChatFormatting.DARK_GRAY))
                        : message(ChatFormatting.YELLOW, "valid_scrammed_hot"));
            }
            markDirtyAndSync();
            return;
        }

        if (isDynamicControlChannelFailure(result) && currentStructure.isPresent()) {
            status = previousStatus == ReactorStatus.SCRAMMED_HOT ? ReactorStatus.SCRAMMED_HOT : ReactorStatus.FORMED;
            updateControlRodInsertion(true);
            linkStructureParts(currentStructure.get());
            if (playerInitiated) {
                statusMessage(player, message(ChatFormatting.YELLOW, "formed_rods_moving"));
            }
            markDirtyAndSync();
            return;
        }

        if (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED || previousStatus == ReactorStatus.SCRAMMED_HOT) {
            currentStructure.ifPresent(this::clearStructureParts);
            setStructure(Optional.empty());
            status = ReactorStatus.SCRAMMED;
            physicsSimulator.setControlRodInsertion(1.0f);
            if (playerInitiated) {
                statusMessage(player, message(ChatFormatting.RED, "scrammed", MultiblockText.text(result.primaryMessage())));
            } else if (previousStatus != ReactorStatus.SCRAMMED && (physicsSimulator.isRunning() || physicsSimulator.getCoreTemperature() > 100)) {
                // Breaking a hot or running core is serious; breaking a cold one while building it is not.
                alertMessage(message(ChatFormatting.RED, "structure_scram"));
            }
        } else {
            status = ReactorStatus.UNFORMED;
            setStructure(Optional.empty());
            if (playerInitiated) {
                statusMessage(player, message(ChatFormatting.RED, "incomplete", MultiblockText.text(result.primaryMessage())));
            }
        }

        markDirtyAndSync();
    }

    /** Whether the simulator was built for this core: same fuel, control channels, moderation and reflection. */
    private boolean simulatorMatches(ReactorStructureValidator.ReactorStructure structure) {
        return physicsSimulator.getUraniumRodCount() == structure.getUraniumRodCount()
                && physicsSimulator.getControlRodCount() == structure.getControlRodCount()
                && Math.abs(physicsSimulator.getModeration() - structure.moderation) < 1e-9
                && Math.abs(physicsSimulator.getReflection() - structure.reflection) < 1e-9;
    }

    private boolean isDynamicControlChannelFailure(MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> result) {
        return !result.errors().isEmpty()
                && result.errors().stream().allMatch(error -> MultiblockText.key(error).equals(ReactorStructureValidator.CHANNEL_BLOCKED));
    }

    private boolean sameStructure(ReactorStructureValidator.ReactorStructure oldStructure, ReactorStructureValidator.ReactorStructure newStructure) {
        return oldStructure.width == newStructure.width
                && oldStructure.height == newStructure.height
                && oldStructure.bottomY == newStructure.bottomY
                && oldStructure.uraniumRodCount == newStructure.uraniumRodCount
                && oldStructure.controlRodCount == newStructure.controlRodCount
                && oldStructure.heatExchangerCount == newStructure.heatExchangerCount
                && oldStructure.coolantInputPortCount == newStructure.coolantInputPortCount
                && oldStructure.steamOutputPortCount == newStructure.steamOutputPortCount
                && oldStructure.fuelPortCount == newStructure.fuelPortCount
                && oldStructure.controlChannels.equals(newStructure.controlChannels)
                && oldStructure.fluidPorts.equals(newStructure.fluidPorts)
                && oldStructure.fuelPorts.equals(newStructure.fuelPorts)
                && oldStructure.temperatureSensors.equals(newStructure.temperatureSensors)
                && oldStructure.redstonePorts.equals(newStructure.redstonePorts)
                && oldStructure.fuelColumns.equals(newStructure.fuelColumns)
                && oldStructure.moderatorColumns.equals(newStructure.moderatorColumns)
                && oldStructure.reflectorColumns.equals(newStructure.reflectorColumns);
    }

    private void linkStructureParts(ReactorStructureValidator.ReactorStructure structure) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos portPos : structure.fluidPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFluidPortBlockEntity port) {
                port.setLinkedController(getBlockPos());
            }
        }
        for (BlockPos portPos : structure.fuelPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFuelPortBlockEntity port) {
                port.setLinkedController(getBlockPos());
            }
        }
        for (BlockPos sensorPos : structure.temperatureSensors) {
            if (level.getBlockEntity(sensorPos) instanceof ReactorTemperatureSensorBlockEntity sensor) {
                sensor.setLinkedController(getBlockPos());
            }
        }
        for (BlockPos portPos : structure.redstonePorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorRedstonePortBlockEntity port) {
                port.setLinkedController(getBlockPos());
            }
        }
    }

    private void clearStructureParts(ReactorStructureValidator.ReactorStructure structure) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos portPos : structure.fluidPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFluidPortBlockEntity port) {
                port.clearLinkedController(getBlockPos());
            }
        }
        for (BlockPos portPos : structure.fuelPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFuelPortBlockEntity port) {
                port.clearLinkedController(getBlockPos());
            }
        }
        for (BlockPos sensorPos : structure.temperatureSensors) {
            if (level.getBlockEntity(sensorPos) instanceof ReactorTemperatureSensorBlockEntity sensor) {
                sensor.clearLinkedController(getBlockPos());
            }
        }
        for (BlockPos portPos : structure.redstonePorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorRedstonePortBlockEntity port) {
                port.clearLinkedController(getBlockPos());
            }
        }
    }

    private void updateControlRodInsertion(boolean force) {
        if (getLevel() == null || currentStructure.isEmpty()) {
            lastControlRodScan = ControlRodTracker.ControlRodScan.empty();
            physicsSimulator.setControlRodInsertion(0.0f);
            return;
        }

        if (!force && controlRodScanCounter++ < CONTROL_ROD_SCAN_INTERVAL) {
            return;
        }
        controlRodScanCounter = 0;
        if (!force && checkControlChannels(getLevel(), currentStructure.get())) {
            return;
        }
        lastControlRodScan = ControlRodTracker.scan(getLevel(), currentStructure.get());
        // Rods only move physically (Create pistons). A SCRAM is a redstone request, except for
        // reactors saved in manual SCRAM before that change, which hold their rods in until reset.
        if (legacyScramHold) {
            physicsSimulator.setControlRodInsertion(1.0f);
        } else {
            physicsSimulator.setChannelInsertions(lastControlRodScan.channelInsertions());
        }
    }

    /**
     * Runs a flagged channel check: detects the channels again and, if they differ from the
     * structure's on two checks in a row, asks for a revalidation (which keeps the core's fuel and
     * heat, see {@link #revalidate}). The second look covers the tick in which Create turns the rods
     * into a contraption and back. Rods moving inside the channels they already had change nothing.
     *
     * @return true when a revalidation was requested, so the rod scan waits for the new structure
     */
    private boolean checkControlChannels(Level level, ReactorStructureValidator.ReactorStructure structure) {
        if (!pendingChannelCheck) {
            return false;
        }
        if (ReactorStructureValidator.detectControlChannels(level, structure).equals(structure.controlChannels)
                && !ReactorStructureValidator.hasBlockedChannel(level, structure)) {
            pendingChannelCheck = false;
            channelMismatchSeen = false;
            return false;
        }
        if (!channelMismatchSeen) {
            channelMismatchSeen = true;
            return false;
        }
        pendingChannelCheck = false;
        channelMismatchSeen = false;
        pendingRevalidation = true;
        return true;
    }

    public boolean isManualScram() {
        return manualScram;
    }

    /** True while the reactor asks for its rods to go in: Redstone Ports in SCRAM mode emit. */
    public boolean isScramRequested() {
        return manualScram || legacyScramHold || autoScram.isTripped();
    }

    public AutoScram getAutoScram() {
        return autoScram;
    }

    /** Rod insertion a control panel asked for, 0-1, or below 0 when none has. */
    public float getRodTarget() {
        return rodTarget;
    }

    /** What the rods should be driven to: fully in while a SCRAM is requested, otherwise the panel target. */
    public float getEffectiveRodTarget() {
        return isScramRequested() ? 1.0f : rodTarget;
    }

    public void setRodTarget(float target) {
        float clamped = target < 0 ? -1.0f : Math.max(0.0f, Math.min(1.0f, target));
        if (clamped == rodTarget) {
            return;
        }
        rodTarget = clamped;
        refreshRedstonePorts();
        markDirtyAndSync();
    }

    /** Moves the panel target by {@code delta}, starting from where the rods are now if none is set. */
    public void adjustRodTarget(float delta) {
        float base = rodTarget >= 0 ? rodTarget : Math.round(lastControlRodScan.insertionRatio() * 10.0f) / 10.0f;
        setRodTarget(Math.round((base + delta) * 100.0f) / 100.0f);
    }

    /**
     * Which way the rods must move to reach {@link #getEffectiveRodTarget()}: 1 in, -1 out, 0 hold.
     * Pistons stop on whole blocks, so the rods count as there once within half a rod segment.
     */
    public int rodDrive() {
        float target = getEffectiveRodTarget();
        if (target < 0 || currentStructure.isEmpty() || status == ReactorStatus.UNFORMED) {
            return 0;
        }
        float tolerance = 0.5f / Math.max(1, currentStructure.get().innerHeight()) + 0.01f;
        float insertion = lastControlRodScan.insertionRatio();
        if (insertion < target - tolerance) {
            return 1;
        }
        if (insertion > target + tolerance) {
            return -1;
        }
        return 0;
    }

    private void checkAutoScram() {
        if (autoScram.check(physicsSimulator.getCoreTemperature())) {
            awardMachine(NuclearEvent.SCRAM);
            refreshRedstonePorts();
            alertMessage(message(ChatFormatting.RED, "auto_scram", autoScram.getThreshold()));
            markDirtyAndSync();
        }
    }

    /**
     * Control panel Reset: clears the manual SCRAM and, if the core has cooled enough, the
     * automatic trip. Returns false (and changes nothing) while a tripped core is still too hot.
     */
    public boolean resetScram() {
        double temperature = physicsSimulator.getCoreTemperature();
        if (!autoScram.canReset(temperature)) {
            statusMessage(operator(), message(ChatFormatting.RED, "reset_too_hot", autoScram.getThreshold() - AutoScram.RESET_MARGIN));
            return false;
        }
        autoScram.reset(temperature);
        manualScram = false;
        legacyScramHold = false;
        updateControlRodInsertion(true);
        refreshRedstonePorts();
        statusMessage(operator(), message(ChatFormatting.GREEN, "scram_reset"));
        markDirtyAndSync();
        return true;
    }

    public void adjustAutoScramThreshold(int delta) {
        autoScram.adjust(delta);
        markDirtyAndSync();
    }

    public void toggleAutoScram() {
        autoScram.setEnabled(!autoScram.isEnabled());
        markDirtyAndSync();
    }

    /**
     * Control panel SCRAM switch. Engaging it only raises the SCRAM signal on the reactor's
     * Redstone Ports; the rods themselves are moved by whatever the player wires to them.
     */
    public void setManualScram(boolean engaged) {
        if (manualScram == engaged && (engaged || !legacyScramHold)) {
            return;
        }
        manualScram = engaged;
        if (!engaged) {
            legacyScramHold = false;
        } else {
            awardMachine(NuclearEvent.SCRAM);
        }
        updateControlRodInsertion(true);
        refreshRedstonePorts();
        if (engaged) {
            alertMessage(message(ChatFormatting.RED, "manual_scram"));
        } else {
            statusMessage(operator(), message(ChatFormatting.GREEN, "manual_scram_released"));
        }
        markDirtyAndSync();
    }

    /** A shut-down core held back by most of the xenon it can hold: the iodine pit. */
    private void checkIodinePit() {
        if (status == ReactorStatus.FORMED && Config.xenonPoisoning && FissionConfig.xenonWorthPcm > 0 && !physicsSimulator.isRunning()
                && physicsSimulator.getXenonPenaltyPcm() >= FissionConfig.xenonWorthPcm * IODINE_PIT_FRACTION) {
            awardMachine(NuclearEvent.IODINE_PIT);
        }
    }

    /**
     * What the reactor shows and marks each tick: the thud of the rods dropping when the SCRAM
     * signal comes on, the cold shutdown that ends a SCRAM, reaching full power, and the fuel
     * channels' looks.
     */
    private void tickCuesAndVisuals() {
        Level level = getLevel();
        double temperature = physicsSimulator.getCoreTemperature();
        boolean scram = isScramRequested() || status == ReactorStatus.SCRAMMED_HOT;
        if (scram && !scramShown && currentStructure.isPresent()) {
            level.playSound(null, BlockPos.containing(radiationOrigin()), AllNuclearSounds.REACTOR_SCRAM.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
            if (temperature >= ReactorSoundModel.COLD_SHUTDOWN_TEMPERATURE) {
                coolingAfterScram = true;
                setChanged();
            }
        }
        scramShown = scram;
        if (coolingAfterScram) {
            if (!scram && physicsSimulator.isRunning()) {
                // Restarted before it cooled down: no cold shutdown.
                coolingAfterScram = false;
                setChanged();
            } else if (temperature < ReactorSoundModel.COLD_SHUTDOWN_TEMPERATURE && !physicsSimulator.isRunning()) {
                coolingAfterScram = false;
                setChanged();
                if (isFormed()) {
                    awardMachine(NuclearEvent.COLD_SHUTDOWN);
                }
            }
        }

        double power = ReactorSoundModel.powerFraction(getPowerOutput(), getRatedPower());
        if (!atFullPower && status == ReactorStatus.FORMED && power >= ReactorSoundModel.FULL_POWER_FRACTION) {
            atFullPower = true;
            awardMachine(NuclearEvent.REACTOR_FULL_POWER);
        } else if (atFullPower && power < ReactorSoundModel.FULL_POWER_RESET_FRACTION) {
            atFullPower = false;
        }

        if (currentStructure.isPresent() && status != ReactorStatus.MELTDOWN) {
            rodVisuals.tick(level, currentStructure.get(), assemblyViews, power, coolantTank.getFluidAmount() > 0);
        }
    }

    /** A hot core was scrammed and has not cooled down yet: the cold shutdown is still to come. */
    public boolean isCoolingAfterScram() {
        return coolingAfterScram;
    }

    /** What the fuel channels show, for tests. */
    public RodVisualSync getRodVisuals() {
        return rodVisuals;
    }

    /** The SCRAM signal is on, or the core was scrammed hot on load: the control room klaxon sounds while it is hot. */
    public boolean isScramActive() {
        return isScramRequested() || status == ReactorStatus.SCRAMMED_HOT;
    }

    /** Awards an event to whoever last worked on the reactor and anyone near it. */
    private void awardMachine(NuclearEvent event) {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            NuclearAdvancements.awardMachine(level, getBlockPos(), messenger.owner(level), event);
        }
    }

    private void refreshRedstonePorts() {
        Level level = getLevel();
        if (level == null || level.isClientSide || currentStructure.isEmpty()) {
            return;
        }
        for (BlockPos portPos : currentStructure.get().redstonePorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorRedstonePortBlockEntity port) {
                port.refreshSignal();
            }
        }
    }

    /** Runs the pressurized loop when the reactor has one, otherwise boils its water as it always did. */
    private void processLoops() {
        if (isPrimaryLoopActive()) {
            processPrimaryLoop();
            return;
        }
        if (primaryLoop.readings().pwr()) {
            primaryLoop.idle();
            physicsSimulator.setCoolantFeedback(1.0);
            showReliefValve(false);
        }
        processCoolantLoop();
    }

    /** The coolant tank holds primary coolant (or it all sits hot in the output tank) and a pressurizer stands on the lid. */
    public boolean isPrimaryLoopActive() {
        return hasPressurizer() && holdsPrimaryCoolant();
    }

    /** Either tank holds pressurized loop coolant. */
    public boolean holdsPrimaryCoolant() {
        return NuclearFluidHelper.isPrimaryCoolant(coolantTank.getFluid()) || NuclearFluidHelper.isHotPrimary(steamTank.getFluid());
    }

    public boolean hasPressurizer() {
        return currentStructure.map(ReactorStructureValidator.ReactorStructure::hasPressurizer).orElse(false);
    }

    /**
     * One tick of the pressurized loop: the core heats the vessel water, circulation turns cold
     * coolant into hot coolant for the steam generators, and the pressurizer and its relief valve
     * answer to whatever the water does. The pumps outside decide how fast it all moves.
     */
    private void processPrimaryLoop() {
        Level level = getLevel();
        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        PressurizerStack stack = structure.pressurizer;
        int autoHeaters = 0;
        int manualHeaters = 0;
        for (BlockPos heater : stack.heaterPorts()) {
            BlockState state = level.getBlockState(heater);
            if (!state.is(PwrRegistry.PRESSURIZER_HEATER_PORT.get())) {
                continue;
            }
            if (state.getValue(PressurizerHeaterPortBlock.AUTOMATIC)) {
                autoHeaters++;
            } else if (PressurizerHeaterPortBlock.manualOn(level, heater, state)) {
                manualHeaters++;
            }
        }
        boolean hotTank = steamTank.isEmpty() || NuclearFluidHelper.isHotPrimary(steamTank.getFluid());
        int cold = NuclearFluidHelper.isPrimaryCoolant(coolantTank.getFluid()) ? coolantTank.getFluidAmount() : 0;
        int hot = hotTank ? steamTank.getFluidAmount() : 0;
        int hotCapacity = hotTank ? steamTank.getCapacity() : 0;
        // The core gave the loop its heat this tick; the loop's water sets what the core sees next tick.
        double coreHeat = physicsSimulator.getRemovedHeat() * FissionCore.TICK * HEAT_UNITS_PER_MJ;
        PrimaryLoop.Result result = primaryLoop.tick(new PrimaryLoop.Context(coreHeat, cold, hot, hotCapacity, stack, autoHeaters,
                manualHeaters, ReliefValveBlock.isolated(level, stack.reliefValve()), level.random.nextDouble()));
        double maxHeatMw = structure.heatExchangerCount * HEAT_EXCHANGER_MW;
        physicsSimulator.setCoolant(new CoreThermalModel.Pressurized(primaryLoop.averageTemperature(), primaryLoop.voidFraction(),
                primaryLoop.coverage(), maxHeatMw), ReactivityModel.Coolant.PRESSURIZED);
        physicsSimulator.setCoolantFeedback(result.feedback());
        if (result.moved() > 0) {
            FluidStack drained = coolantTank.drain(result.moved(), IFluidHandler.FluidAction.EXECUTE);
            steamTank.fill(new FluidStack(AllNuclearFluids.HOT_PRIMARY_COOLANT.get(), drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        }
        if (result.vented() > 0) {
            int left = result.vented() - coolantTank.drain(result.vented(), IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (left > 0 && hotTank) {
                steamTank.drain(left, IFluidHandler.FluidAction.EXECUTE);
            }
            if (level instanceof ServerLevel serverLevel) {
                accidents.releaseCoolant(serverLevel, stack.reliefValve(), result.vented(), PRIMARY_STEAM_K,
                        accidents.coolantActivityPerMb(structure.uraniumRodCount));
            }
            if (level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % 4 == 0) {
                BlockPos valve = stack.reliefValve();
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        valve.getX() + 0.5, valve.getY() + 1.0, valve.getZ() + 0.5, 4, 0.15, 0.1, 0.15, 0.05);
            }
        }
        showReliefValve(primaryLoop.reliefOpen());
    }

    /** Shows the relief valve open or shut, for its texture and comparators. */
    private void showReliefValve(boolean open) {
        Level level = getLevel();
        if (level == null || currentStructure.isEmpty() || currentStructure.get().pressurizer == null) {
            return;
        }
        BlockPos valve = currentStructure.get().pressurizer.reliefValve();
        BlockState state = level.getBlockState(valve);
        if (state.is(PwrRegistry.RELIEF_VALVE.get()) && state.getValue(ReliefValveBlock.OPEN) != open) {
            level.setBlock(valve, state.setValue(ReliefValveBlock.OPEN, open), Block.UPDATE_ALL);
            if (open && !level.isClientSide) {
                level.playSound(null, valve, AllNuclearSounds.RELIEF_VALVE_LIFT.get(), SoundSource.BLOCKS, 1.2f, 1.0f);
                awardMachine(NuclearEvent.RELIEF_VALVE_LIFTED);
            }
        }
    }

    /**
     * The core boils its water directly. Last tick's boiling turns into steam here, by the energy it
     * took (steam out = heat removed / energy per mB), and the coolant tank, the heat exchangers, the
     * ports and the room in the steam tank set what the core can boil off next tick. A full steam
     * tank or a blocked outlet stops the boiling: the water in the core then warms up, gradually.
     */
    private void processCoolantLoop() {
        FluidStack coolant = coolantTank.getFluid();
        boolean water = !coolant.isEmpty() && NuclearFluidHelper.isCoolant(coolant);
        boolean heavy = water && NuclearFluidHelper.isHeavyWater(coolant);
        double energyPerMb = heavy ? FissionConfig.heavyWaterSteamEnergy : FissionConfig.steamEnergy;

        boilCarry += physicsSimulator.getBoiledHeat() / Math.max(1.0e-9, energyPerMb);
        int steam = (int) Math.floor(boilCarry);
        if (water && steam > 0) {
            FluidStack produced = new FluidStack(heavy ? AllNuclearFluids.HEAVY_STEAM.get() : AllNuclearFluids.STEAM.get(), steam);
            int room = steamTank.fill(produced, IFluidHandler.FluidAction.SIMULATE);
            FluidStack drained = coolantTank.drain(Math.min(steam, room), IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                steamTank.fill(new FluidStack(produced.getFluid(), drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        boilCarry = Math.max(0, Math.min(1, boilCarry - steam));

        if (currentStructure.isEmpty()) {
            return;
        }
        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        double chf = structure.heatExchangerCount * exchangerSteamPerTick() * energyPerMb * FissionConfig.chfMargin;
        if (coolant.isEmpty()) {
            physicsSimulator.setCoolant(new CoreThermalModel.Dry(), ReactivityModel.Coolant.WATER);
            return;
        }
        double coverage = Math.min(1.0, coolantTank.getFluidAmount() / (double) CORE_WATER);
        // Primary coolant without a pressurizer only sits in the core: it cannot boil away.
        int removal = water ? Math.min(getSteamThroughputLimit(), steamTank.getSpace()) : 0;
        physicsSimulator.setCoolant(new CoreThermalModel.Boiling(coverage, removal * energyPerMb, chf),
                heavy ? ReactivityModel.Coolant.HEAVY_WATER : ReactivityModel.Coolant.WATER);
    }

    private int getSteamThroughputLimit() {
        if (currentStructure.isEmpty()) {
            return 0;
        }
        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        int exchangerLimit = (int) Math.floor(structure.heatExchangerCount * exchangerSteamPerTick());
        int coolantPortLimit = structure.coolantInputPortCount * FLUID_PORT_STEAM_PER_TICK;
        int steamPortLimit = structure.steamOutputPortCount * FLUID_PORT_STEAM_PER_TICK;
        return Math.min(exchangerLimit, Math.min(coolantPortLimit, steamPortLimit));
    }

    /**
     * Steam one heat exchanger boils, mB/t: its MW in light-water steam. Heavy water boils the same
     * mB, each carrying more heat, as it always has.
     */
    private static double exchangerSteamPerTick() {
        return HEAT_EXCHANGER_MW / Math.max(1.0e-9, FissionConfig.steamEnergy);
    }

    private void tryLoadFuelAssembly() {
        if (!pendingDepletedFuel.isEmpty()) {
            return;
        }
        while (physicsSimulator.canLoadFuelAssembly()) {
            boolean loaded = false;
            for (int slot = 0; slot < FUEL_INPUT_SLOTS; slot++) {
                if (!isFreshFuel(fuelInventory.getStackInSlot(slot))) {
                    continue;
                }
                ItemStack extracted = fuelInventory.extractItem(slot, 1, false);
                if (!extracted.isEmpty()) {
                    if (physicsSimulator.loadFuelAssembly(fuelKindOf(extracted), getFuelEnrichment(extracted))) {
                        accidents.cladding().refuel(1, PwrConfig.zirconiumPerRod);
                        loaded = true;
                    } else {
                        fuelInventory.insertItem(slot, extracted, false);
                    }
                    break;
                }
            }
            if (!loaded) {
                return;
            }
        }
    }

    private void collectSpentFuel() {
        List<SpentAssembly> spent = physicsSimulator.drainSpent();
        if (!spent.isEmpty()) {
            pendingDepletedFuel.addAll(spent);
            tryStorePendingDepletedFuel();
        }
    }

    private void tryStorePendingDepletedFuel() {
        while (!pendingDepletedFuel.isEmpty()) {
            SpentAssembly next = pendingDepletedFuel.get(0);
            ItemStack depleted;
            if (next.kind() == FuelKind.MOX) {
                depleted = new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get());
            } else if (next.kind() == FuelKind.THORIUM) {
                depleted = new ItemStack(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get());
            } else {
                depleted = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
                UraniumItem.setEnrichment(depleted, next.enrichment());
            }
            SpentFuelHooks.onDischarge(depleted, level);
            ItemStack remainder = insertIntoOutputSlots(depleted, false);
            if (!remainder.isEmpty()) {
                return;
            }
            pendingDepletedFuel.remove(0);
        }
    }

    private ItemStack insertIntoOutputSlots(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack;
        for (int slot = FUEL_INPUT_SLOTS; slot < FUEL_SLOT_COUNT && !remainder.isEmpty(); slot++) {
            remainder = fuelInventory.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private boolean canTriggerMeltdown() {
        return status == ReactorStatus.FORMED && ticksSinceLoad >= LOAD_MELTDOWN_GRACE_TICKS;
    }

    private void updateControllerRunningState() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(NuclearReactorControllerBlock.ON)) {
            return;
        }
        boolean running = status == ReactorStatus.FORMED && physicsSimulator.isRunning();
        if (running && !state.getValue(NuclearReactorControllerBlock.ON)) {
            awardMachine(NuclearEvent.REACTOR_RUNNING);
        }
        if (state.getValue(NuclearReactorControllerBlock.ON) != running) {
            level.setBlock(getBlockPos(), state.setValue(NuclearReactorControllerBlock.ON, running), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Saves and asks for a client update: sent at once when the state the clients act on changed
     * (formed or not, SCRAM, meltdown, breach), otherwise at most once every {@link #SYNC_INTERVAL}
     * ticks, the rest held back for {@link #flushSync}.
     */
    private void markDirtyAndSync() {
        setChanged();
        syncDirty = true;
        Level level = getLevel();
        if (level != null && !level.isClientSide
                && (syncState() != lastSyncedState || level.getGameTime() - lastSyncTime >= SYNC_INTERVAL)) {
            sendSync(level);
        }
    }

    /**
     * Tanks and fuel slots changed: saved now, shown once the change is one the clients can see
     * (see {@link #flushSync}). A running reactor's tanks change every tick; this keeps that from
     * sending an update every {@link #SYNC_INTERVAL} ticks by itself.
     */
    private void contentsChanged() {
        setChanged();
        contentsDirty = true;
    }

    /**
     * Most ticks between two client updates ({@link PerformanceConfig#forcedSyncTicks}): the slow
     * readings (fuel burnup) go out at least this often. Never below {@link #SYNC_INTERVAL}.
     */
    public static int forcedSyncInterval() {
        return Math.max(SYNC_INTERVAL, PerformanceConfig.forcedSyncTicks);
    }

    /**
     * Every tick: sends what was held back, no sooner than {@link #SYNC_INTERVAL} ticks after the last
     * update. A player's change goes out then; contents and readings only once they changed by a step
     * the clients would see; and every {@link #forcedSyncInterval()} ticks an update goes out anyway.
     * A running reactor's tanks change every tick, so the visible key is compared at most once every
     * {@link #SYNC_INTERVAL} ticks, when the readings are due ({@code readingsDue}, once a second) or
     * the contents changed since the last comparison.
     */
    private void flushSync(boolean readingsDue) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (contentsDirty || readingsDue) {
            contentsDirty = false;
            keyCheckDue = true;
        }
        long now = level.getGameTime();
        long since = now - lastSyncTime;
        if (since < SYNC_INTERVAL) {
            return;
        }
        if (syncDirty || since >= forcedSyncInterval()) {
            sendSync(level);
            return;
        }
        if (keyCheckDue && now - lastKeyCheckTime >= SYNC_INTERVAL) {
            keyCheckDue = false;
            lastKeyCheckTime = now;
            visibleKeyChecks++;
            fillVisibleKey(visibleKey);
            if (visibleKeyDiffers(visibleKey, sentKey)) {
                sendSync(level);
            }
        }
    }

    /** Whether {@code now} shows something {@code sent} did not; the tanks only past their hysteresis. */
    static boolean visibleKeyDiffers(int[] now, int[] sent) {
        for (int i = 0; i < now.length; i++) {
            if (now[i] == sent[i]) {
                continue;
            }
            if (i == KEY_COOLANT_PERCENT || i == KEY_STEAM_PERCENT) {
                if (Math.abs(now[i] - sent[i]) >= SYNC_TANK_HYSTERESIS || now[i] == 0 || now[i] == 100) {
                    return true;
                }
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * What the clients show (goggles, sounds, the control room), each reading cut to the step that
     * shows: tanks by the percent, temperatures by {@link #SYNC_TEMPERATURE_STEP} degrees, power by
     * half a percent of rated and by a twentieth of a decade, reactivity by {@link #SYNC_REACTIVITY_STEP}
     * pcm and its parts by {@link #SYNC_REACTIVITY_PART_STEP}, the period by sign and decade, plus
     * counts and flags. The fuel totals add up the assemblies, which is why {@link #flushSync} runs
     * this at most once every {@link #SYNC_INTERVAL} ticks.
     */
    private void fillVisibleKey(int[] key) {
        int i = 0;
        key[i++] = syncState();
        key[i++] = percent(coolantTank.getFluidAmount(), coolantTank.getCapacity());
        key[i++] = coolantTank.isEmpty() ? 0 : System.identityHashCode(coolantTank.getFluid().getFluid());
        key[i++] = percent(steamTank.getFluidAmount(), steamTank.getCapacity());
        key[i++] = steamTank.isEmpty() ? 0 : System.identityHashCode(steamTank.getFluid().getFluid());
        key[i++] = (int) Math.floor(physicsSimulator.getCoreTemperature() / SYNC_TEMPERATURE_STEP);
        key[i++] = (int) Math.floor(physicsSimulator.getCoolantTemperature() / SYNC_TEMPERATURE_STEP);
        key[i++] = (int) Math.floor(physicsSimulator.getRelativePower() * SYNC_POWER_STEPS);
        key[i++] = (int) Math.floor(physicsSimulator.getNetReactivity() / SYNC_REACTIVITY_STEP);
        key[i++] = (int) Math.floor(physicsSimulator.getCoreDamage() * 100.0);
        key[i++] = (int) Math.floor(physicsSimulator.getXenonPenalty() * 100.0);
        key[i++] = Math.round(physicsSimulator.getControlRodInsertion() * 100.0f);
        key[i++] = Math.round(lastControlRodScan.insertionRatio() * 100.0f);
        key[i++] = lastControlRodScan.insertedSegments();
        key[i++] = lastControlRodScan.movingSegments();
        key[i++] = lastControlRodScan.expectedSegments();
        key[i++] = physicsSimulator.getLoadedAssemblyCount();
        key[i++] = countInputFuel();
        key[i++] = countOutputFuel();
        key[i++] = pendingDepletedFuel.size();
        key[i++] = Math.round(rodTarget * 100.0f);
        key[i++] = (legacyScramHold ? 1 : 0) | (autoScram.isTripped() ? 2 : 0) | (autoScram.isEnabled() ? 4 : 0)
                | (primaryLoop.heatersRunning() ? 8 : 0) | (primaryLoop.reliefStuck() ? 16 : 0)
                | (physicsSimulator.isThermalExcursionActive() ? 32 : 0);
        key[i++] = autoScram.getThreshold();
        PlantReadings loop = primaryLoop.readings();
        key[i++] = loop.pwr() ? 1 : 0;
        key[i++] = Math.round(loop.pressure());
        key[i++] = Math.round(loop.pressurizerLevel() * 100.0f);
        key[i++] = Math.round(loop.vesselLevel() * 100.0f);
        key[i++] = Math.round(loop.flow());
        key[i++] = Math.round(loop.hotLeg());
        key[i++] = Math.round(loop.coldLeg());
        key[i++] = Math.round(accidents.containmentPressure() * 100.0f);
        key[i++] = Math.round(accidents.containmentHydrogen() * 1000.0f);
        key[i++] = getReactorState().ordinal();
        key[i++] = (int) Math.round(physicsSimulator.getThermalStress());
        key[i++] = (int) Math.floor(Math.log10(Math.max(1e-9, physicsSimulator.getRelativePower())) * SYNC_LOG_POWER_STEPS);
        key[i++] = periodKey(physicsSimulator.getPeriod());
        key[i++] = (int) Math.floor(physicsSimulator.core().decay().heat() * 100.0);
        key[i++] = (int) Math.floor(physicsSimulator.getShutdownMargin() / SYNC_MARGIN_STEP);
        double capacity = physicsSimulator.getFuelCapacity();
        key[i++] = capacity <= 0 ? 0 : (int) Math.floor(physicsSimulator.getFuelRemaining() * 100.0 / capacity);
        ReactivityModel.Parts parts = physicsSimulator.getReactivityParts();
        key[i++] = (int) Math.floor(parts.fuel() / SYNC_REACTIVITY_PART_STEP);
        key[i++] = (int) Math.floor(parts.rods() / SYNC_REACTIVITY_PART_STEP);
        key[i++] = (int) Math.floor(parts.temperature() / SYNC_REACTIVITY_PART_STEP);
        key[i++] = (int) Math.floor(parts.voids() / SYNC_REACTIVITY_PART_STEP);
        key[i++] = (int) Math.floor(parts.xenon() / SYNC_REACTIVITY_PART_STEP);
        key[i] = (int) Math.floor(parts.damage() / SYNC_REACTIVITY_PART_STEP);
    }

    /**
     * The reactor period as the clients read it: its sign and its decade (seconds), 0 when steady
     * (infinite, or longer than ten thousand seconds).
     */
    static int periodKey(double period) {
        if (!Double.isFinite(period) || Math.abs(period) >= 1.0e4) {
            return 0;
        }
        int decade = (int) Math.floor(Math.log10(Math.max(1.0e-3, Math.abs(period))));
        return (period < 0 ? -1 : 1) * (10 + decade);
    }

    private static int percent(int amount, int capacity) {
        return capacity <= 0 ? 0 : (int) (amount * 100L / capacity);
    }

    private void sendSync(Level level) {
        syncDirty = false;
        contentsDirty = false;
        lastSyncTime = level.getGameTime();
        lastSyncedState = syncState();
        fillVisibleKey(sentKey);
        // the key was just worked out: the next comparison waits a full interval
        keyCheckDue = false;
        lastKeyCheckTime = lastSyncTime;
        clientSyncs++;
        BlockState state = getBlockState();
        level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    /** What clients must see without delay, packed: status, formed, SCRAM, meltdown and breach. */
    private int syncState() {
        return status.ordinal()
                | (currentStructure.isPresent() ? 1 << 8 : 0)
                | (manualScram ? 1 << 9 : 0)
                | (physicsSimulator.isMeltingDown() ? 1 << 10 : 0)
                | (accidents.vessel().breached() ? 1 << 11 : 0);
    }

    /** Client updates this reactor has sent, for tests. */
    public int clientSyncCount() {
        return clientSyncs;
    }

    /** Times {@link #flushSync} compared the visible key with the last sent one, for tests. */
    public int visibleKeyCheckCount() {
        return visibleKeyChecks;
    }

    /** The answer to what {@code actor} just did, on their action bar; nothing without an actor. */
    private void statusMessage(@Nullable Player actor, Component message) {
        lastMessage = message;
        messenger.status(getLevel(), actor, message);
    }

    /** A serious event, in the chat of the operator and of everyone near the reactor. */
    private void alertMessage(Component message) {
        lastMessage = message;
        messenger.alert(getLevel(), getBlockPos(), message);
    }

    /** The player who last worked the reactor, if still around. */
    @Nullable
    private Player operator() {
        Level level = getLevel();
        return level == null ? null : messenger.owner(level);
    }

    /** A reactor chat message: {@code create_nuclearindustry.reactor.message.<id>} in one colour. */
    private static Component message(ChatFormatting style, String id, Object... args) {
        return Component.translatable(MESSAGE + id, args).withStyle(style);
    }

    /** How the reactor talks to players, for tests. */
    public MultiblockMessenger messenger() {
        return messenger;
    }

    /** The last chat message the reactor sent (or would have, with nobody to read it). */
    public Component getLastMessage() {
        return lastMessage;
    }

    /** How many times the structure was validated, for tests. */
    public int getValidationCount() {
        return validations;
    }

    /** The result of the last validation, errors and warnings included. */
    public MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> getLastValidation() {
        return lastValidation;
    }

    /**
     * The core melts. By default this is the realistic chain: the fuel becomes corium, the vessel's
     * water and the rest of the cladding's hydrogen go into the containment (or the open air) with
     * the core's volatile activity, and the corium does the rest. With {@code legacyMeltdown} it is
     * the old power 80 explosion and wide fallout.
     */
    private void handleMeltdown() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        PlantAlarm.raise(level, getBlockPos());
        if (!PwrConfig.legacyMeltdown && level instanceof ServerLevel serverLevel && currentStructure.isPresent()) {
            meltCore(serverLevel, currentStructure.get());
            return;
        }

        BlockPos reactorPos = getBlockPos();
        LOGGER.warn("Nuclear meltdown at {}", reactorPos);

        status = ReactorStatus.MELTDOWN;
        awardMachine(NuclearEvent.MELTDOWN);
        NuclearAdvancements.awardNearby(level, reactorPos, MELTDOWN_FALLOUT_RADIUS, NuclearEvent.MELTDOWN);
        FalloutData.contaminate(level, reactorPos, MELTDOWN_RADIATION, MELTDOWN_CONTAMINATION_TICKS);
        if (level instanceof ServerLevel serverLevel) {
            org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination.fallout(serverLevel, reactorPos,
                    MELTDOWN_FALLOUT_RADIUS, MELTDOWN_FALLOUT_INTENSITY);
        }
        currentStructure.ifPresent(this::clearStructureParts);
        setStructure(Optional.empty());
        physicsSimulator = new ReactorPhysicsSimulator(0);
        markDirtyAndSync();
        alertMessage(message(ChatFormatting.DARK_RED, "meltdown", getBlockPos().toShortString()));
        level.explode(null, reactorPos.getX() + 0.5, reactorPos.getY() + 0.5, reactorPos.getZ() + 0.5,
                MELTDOWN_EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
    }

    private void meltCore(ServerLevel level, ReactorStructureValidator.ReactorStructure structure) {
        BlockPos reactorPos = getBlockPos();
        LOGGER.warn("Nuclear meltdown at {}", reactorPos);
        status = ReactorStatus.MELTDOWN;
        awardMachine(NuclearEvent.MELTDOWN);
        NuclearAdvancements.awardNearby(level, reactorPos, MELTDOWN_FALLOUT_RADIUS, NuclearEvent.MELTDOWN);
        BoundingBox box = vesselBox(structure);
        ContainmentMonitorBlockEntity containment = accidents.findContainment(level, box);
        BlockPos center = BlockPos.containing(radiationOrigin());
        int rods = structure.uraniumRodCount;

        // The vessel's water flashes off the molten core; what cladding is left reacts in the melt.
        int water = coolantTank.getFluidAmount() + steamTank.getFluidAmount();
        coolantTank.setFluid(FluidStack.EMPTY);
        steamTank.setFluid(FluidStack.EMPTY);
        accidents.releaseCoolant(level, center, water, CORE_GAS_K, accidents.coolantActivityPerMb(rods));
        if (PwrConfig.hydrogen) {
            double unoxidised = Math.max(0.0, rods * PwrConfig.zirconiumPerRod - accidents.cladding().oxidized());
            accidents.releaseHydrogen(level, 2.0 * unoxidised * MELT_OXIDATION, CORE_GAS_K);
        }
        accidents.releaseActivity(level, center, rods * ReleasePath.ACTIVITY_PER_ROD);
        // The debris itself stays radioactive, inside or out.
        FalloutData.contaminate(level, reactorPos, MELTDOWN_RADIATION, MELTDOWN_CONTAMINATION_TICKS);

        currentStructure.ifPresent(this::clearStructureParts);
        setStructure(Optional.empty());
        physicsSimulator = new ReactorPhysicsSimulator(0);
        primaryLoop.idle();
        markDirtyAndSync();
        alertMessage(message(ChatFormatting.DARK_RED, containment != null ? "meltdown_contained" : "meltdown_release",
                reactorPos.toShortString()));
        formCorium(level, structure, rods);
    }

    /** The fuel rods turn into corium, lowest first, at most {@link #MAX_CORIUM} blocks; the rest of the melt joins it. */
    private void formCorium(ServerLevel level, ReactorStructureValidator.ReactorStructure structure, int rods) {
        List<BlockPos> fuel = new ArrayList<>();
        for (BlockPos column : structure.fuelColumns) {
            for (int y = structure.bottomY + 1; y < structure.topY; y++) {
                BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                if (level.getBlockState(pos).is(AllNuclearBlocks.URANIUM_ROD.get())) {
                    fuel.add(pos);
                }
            }
        }
        fuel.sort(Comparator.<BlockPos>comparingInt(pos -> pos.getY()).thenComparingInt(pos -> pos.getX()).thenComparingInt(pos -> pos.getZ()));
        int corium = Math.min(Math.min(rods, MAX_CORIUM), fuel.size());
        for (int i = 0; i < fuel.size(); i++) {
            BlockState melt;
            if (i >= corium) {
                melt = Blocks.AIR.defaultBlockState();
            } else if (PwrConfig.corium) {
                melt = PwrRegistry.CORIUM.get().fresh();
            } else {
                melt = PwrRegistry.SOLIDIFIED_CORIUM.get().defaultBlockState();
            }
            level.setBlock(fuel.get(i), melt, Block.UPDATE_ALL);
        }
    }

    /** The reactor's own blocks, without the space for control rods above the lid. */
    private static BoundingBox vesselBox(ReactorStructureValidator.ReactorStructure structure) {
        BoundingBox bounds = structure.bounds();
        return new BoundingBox(bounds.minX(), structure.bottomY, bounds.minZ(), bounds.maxX(), structure.topY, bounds.maxZ());
    }

    /**
     * One tick of the accident chain while the reactor runs: hot cladding oxidising, the vessel
     * holding its pressure or not, a breached vessel leaking, and water reaching a molten core.
     * Returns true if an explosion went off, since it may have taken the reactor apart.
     */
    private boolean runAccidents() {
        if (!(getLevel() instanceof ServerLevel level) || currentStructure.isEmpty()) {
            return false;
        }
        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        accidents.tick(level, vesselBox(structure));
        BlockPos center = BlockPos.containing(radiationOrigin());
        int rods = structure.uraniumRodCount;
        double temperature = physicsSimulator.getCoreTemperature();
        // The cladding sits between the fuel and its coolant.
        double cladding = physicsSimulator.getCoolantTemperature() + 0.3 * (temperature - physicsSimulator.getCoolantTemperature());

        double steamReach = coolantTank.getFluidAmount() + steamTank.getFluidAmount() > 0 ? 1.0 : 0.2;
        CladdingOxidationModel.Step oxidation = accidents.cladding().step(cladding, rods, steamReach, ReactorAccidents.claddingParams());
        // The model gives degrees per tick of one rod's fuel; the core takes megawatts.
        physicsSimulator.setOxidationHeat(oxidation.heat() * FissionConfig.fuelHeatCapacity / FissionCore.TICK);
        double damage = physicsSimulator.getCoreDamage();
        if (damage > lastDamage) {
            // Failed cladding lets the fission products in the fuel gap out.
            accidents.releaseActivity(level, center, (damage - lastDamage) * rods * ReleasePath.ACTIVITY_PER_ROD * 0.1);
            if (lastDamage < FUEL_DAMAGE_ALERT && damage >= FUEL_DAMAGE_ALERT) {
                alertMessage(message(ChatFormatting.DARK_RED, "fuel_damage"));
            }
        }
        lastDamage = damage;
        boolean melting = physicsSimulator.isFuelMelting();
        if (melting && !fuelMeltingAlerted) {
            alertMessage(message(ChatFormatting.DARK_RED, "fuel_melting"));
        }
        fuelMeltingAlerted = melting;
        if (PwrConfig.hydrogen && oxidation.hydrogen() > 0) {
            accidents.releaseHydrogen(level, oxidation.hydrogen(), CORE_GAS_K);
        }

        if (isPrimaryLoopActive() && accidents.vessel().tick(primaryLoop.readings().pressure(), PwrConfig.vesselFailurePressure,
                PwrConfig.vesselFailureTicks)) {
            breachVessel(level, structure);
            return true;
        }
        if (accidents.vessel().breached()) {
            int leak = coolantTank.drain(BREACH_LEAK_PER_TICK, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (leak < BREACH_LEAK_PER_TICK) {
                leak += steamTank.drain(BREACH_LEAK_PER_TICK - leak, IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
            accidents.releaseCoolant(level, center, leak, PRIMARY_STEAM_K, accidents.coolantActivityPerMb(rods));
        }

        if (accidents.takeReflood(temperature)) {
            int water = coolantTank.getFluidAmount();
            coolantTank.setFluid(FluidStack.EMPTY);
            accidents.releaseCoolant(level, center, water, CORE_GAS_K, accidents.coolantActivityPerMb(rods));
            physicsSimulator.applyExternalCooling(REFLOOD_QUENCH, 0.0);
            alertMessage(message(ChatFormatting.DARK_RED, "steam_explosion", getBlockPos().toShortString()));
            markDirtyAndSync();
            SteamExplosion.explode(level, radiationOrigin(), SteamExplosion.REFLOOD_POWER);
            return true;
        }
        return false;
    }

    /** The vessel gave way: its water flashes into the containment and the head blows off. */
    private void breachVessel(ServerLevel level, ReactorStructureValidator.ReactorStructure structure) {
        BlockPos center = BlockPos.containing(radiationOrigin());
        accidents.findContainment(level, vesselBox(structure));
        int water = coolantTank.getFluidAmount() + steamTank.getFluidAmount();
        coolantTank.setFluid(FluidStack.EMPTY);
        steamTank.setFluid(FluidStack.EMPTY);
        accidents.releaseCoolant(level, center, water, PRIMARY_STEAM_K, accidents.coolantActivityPerMb(structure.uraniumRodCount));
        primaryLoop.idle();
        physicsSimulator.setCoolantFeedback(1.0);
        alertMessage(message(ChatFormatting.DARK_RED, "vessel_breached", getBlockPos().toShortString()));
        PlantAlarm.raise(level, getBlockPos());
        markDirtyAndSync();
        BlockPos head = getBlockPos().above();
        SteamExplosion.explode(level, Vec3.atCenterOf(head), SteamExplosion.REFLOOD_POWER);
    }

    public ReactorAccidents getAccidents() {
        return accidents;
    }

    public void setControlRodPosition(float position) {
        physicsSimulator.setControlRodPosition(position);
        markDirtyAndSync();
    }

    public void moveControlRod(float delta) {
        physicsSimulator.moveControlRod(delta);
        markDirtyAndSync();
    }

    public boolean isFormed() {
        return currentStructure.isPresent() && status != ReactorStatus.UNFORMED && status != ReactorStatus.MELTDOWN;
    }

    public double getCoreTemperature() {
        return status == ReactorStatus.UNFORMED ? 20.0 : physicsSimulator.getCoreTemperature();
    }

    public double getNeutronLevel() {
        return status == ReactorStatus.UNFORMED ? 0.0 : physicsSimulator.getNeutronLevel();
    }

    public double getFuelRemaining() {
        return status == ReactorStatus.UNFORMED ? 0.0 : physicsSimulator.getFuelRemaining();
    }

    public double getFuelCapacity() {
        return status == ReactorStatus.UNFORMED ? 0.0 : physicsSimulator.getFuelCapacity();
    }

    public int getInputFuelCount() {
        return countInputFuel();
    }

    public int getOutputFuelCount() {
        return countOutputFuel();
    }

    public int getPendingDepletedFuel() {
        return pendingDepletedFuel.size();
    }

    public float getAverageFuelEnrichment() {
        return isFormed() ? physicsSimulator.getAverageFuelEnrichment() : 0.0f;
    }

    public ControlRodTracker.ControlRodScan getControlRodScan() {
        return lastControlRodScan;
    }

    /**
     * The loaded fuel assemblies in burn order, each with how burnt it is, as the fuel channels show them
     * (server side; a new list on each call).
     */
    public List<ReactorPhysicsSimulator.AssemblyView> fuelAssemblies() {
        return physicsSimulator.assemblyViews();
    }

    public int getStaticControlRodSegments() {
        return lastControlRodScan.staticSegments();
    }

    public int getMovingControlRodSegments() {
        return lastControlRodScan.movingSegments();
    }

    public int getInsertedControlRodSegments() {
        return lastControlRodScan.insertedSegments();
    }

    public int getExpectedControlRodSegments() {
        return lastControlRodScan.expectedSegments();
    }

    public float getControlRodInsertionRatio() {
        return lastControlRodScan.insertionRatio();
    }

    public float getControlRodPosition() {
        return isFormed() ? physicsSimulator.getControlRodPosition() : 0.0f;
    }

    public double getPowerOutput() {
        return isFormed() ? physicsSimulator.getPowerOutput() : 0.0;
    }

    /** Rated thermal power of the core with its fuel, MW. */
    public double getRatedPower() {
        return isFormed() ? physicsSimulator.getRatedPower() : 0.0;
    }

    public double getCoolantTemperature() {
        return isFormed() ? physicsSimulator.getCoolantTemperature() : 20.0;
    }

    public double getNetReactivity() {
        return isFormed() ? physicsSimulator.getNetReactivity() : 0.0;
    }

    /** The net reactivity split into fuel, rods, temperature, voids, xenon and damage, pcm. */
    public ReactivityModel.Parts getReactivityParts() {
        return isFormed() ? physicsSimulator.getReactivityParts() : ReactivityModel.Parts.NONE;
    }

    public double getCoreDamage() {
        return isFormed() ? physicsSimulator.getCoreDamage() : 0.0;
    }

    public double getPeriod() {
        return isFormed() ? physicsSimulator.getPeriod() : Double.POSITIVE_INFINITY;
    }

    public double getShutdownMargin() {
        return isFormed() ? physicsSimulator.getShutdownMargin() : 0.0;
    }

    public double getSteamGenerationRate() {
        return isFormed() ? physicsSimulator.getSteamGenerationRate() : 0.0;
    }

    public double getThermalStress() {
        return isFormed() ? physicsSimulator.getThermalStress() : 0.0;
    }

    /** The pressurized water loop and containment around this reactor; {@link PlantReadings#NONE} when it boils directly. */
    public PlantReadings plantReadings() {
        if (!isFormed()) {
            return PlantReadings.NONE;
        }
        PlantReadings loop = primaryLoop.readings();
        float containment = accidents.containmentPressure();
        if (containment <= 0) {
            return loop;
        }
        return new PlantReadings(loop.pwr(), loop.pressure(), loop.hotLeg(), loop.coldLeg(), loop.subcooling(), loop.pressurizerLevel(),
                loop.flow(), loop.vesselLevel(), containment, accidents.containmentHydrogen());
    }

    public PrimaryLoop getPrimaryLoop() {
        return primaryLoop;
    }

    /** Fraction of neutron production xenon-135 swallows right now. */
    public double getXenonPenalty() {
        return isFormed() ? physicsSimulator.getXenonPenalty() : 0.0;
    }

    /** Decay heat of the fission products, in MW. */
    public double getDecayHeatPower() {
        return isFormed() ? physicsSimulator.getDecayHeatPower() : 0.0;
    }

    /** Multiplication of the core relative to a bare, clean LEU core, regardless of the rods. */
    public double getReactivity() {
        return isFormed() ? physicsSimulator.getReactivity() : 0.0;
    }

    public boolean isThermalExcursionActive() {
        return isFormed() && physicsSimulator.isThermalExcursionActive();
    }

    public ReactorPhysicsSimulator.ReactorState getReactorState() {
        if (status == ReactorStatus.SCRAMMED && currentStructure.isEmpty()) {
            return ReactorPhysicsSimulator.ReactorState.CRITICAL;
        }
        if (!isFormed()) {
            return ReactorPhysicsSimulator.ReactorState.IDLE;
        }
        if (physicsSimulator.getFuelRemaining() > 0.0
                && physicsSimulator.getNeutronLevel() <= 1.0
                && lastControlRodScan.insertionRatio() >= 0.99f) {
            return ReactorPhysicsSimulator.ReactorState.SUPPRESSED;
        }
        if (physicsSimulator.getFuelRemaining() <= 0.0 && countInputFuel() > 0) {
            return ReactorPhysicsSimulator.ReactorState.FUELING;
        }
        return physicsSimulator.getState();
    }

    public Optional<ReactorStructureValidator.ReactorStructure> getStructure() {
        return currentStructure;
    }

    public ReactorPhysicsSimulator getPhysicsSimulator() {
        return physicsSimulator;
    }

    public ReactorDisplaySnapshot getDisplaySnapshot() {
        if (getLevel() != null && getLevel().isClientSide) {
            return hasUsableDisplaySnapshot() ? displaySnapshot : ReactorDisplaySnapshot.unsynced();
        }
        return createDisplaySnapshot();
    }

    public int getTankCapacity() {
        return TANK_CAPACITY;
    }

    public int getCoolantAmount() {
        return coolantTank.getFluidAmount();
    }

    public int getSteamAmount() {
        return steamTank.getFluidAmount();
    }

    public FluidStack getCoolantFluid() {
        return coolantTank.getFluid().copy();
    }

    public FluidStack getSteamFluid() {
        return steamTank.getFluid().copy();
    }

    /**
     * Water and heavy water to boil, or primary coolant for the pressurized loop, which only a reactor
     * with a pressurizer takes. Hot primary coolant pumped back in brings its heat with it.
     */
    public int fillCoolant(FluidStack resource, IFluidHandler.FluidAction action) {
        boolean dry = coolantTank.getFluidAmount() < DRY_CORE;
        int filled = fillCoolantTank(resource, action);
        if (action.execute() && filled > 0 && isFormed()) {
            accidents.noteReflood(physicsSimulator.getCoreTemperature(), dry);
        }
        return filled;
    }

    private int fillCoolantTank(FluidStack resource, IFluidHandler.FluidAction action) {
        if (NuclearFluidHelper.isPrimaryLoopFluid(resource)) {
            if (!hasPressurizer()) {
                return 0;
            }
            boolean hot = NuclearFluidHelper.isHotPrimary(resource);
            FluidStack cold = hot ? new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), resource.getAmount()) : resource;
            int filled = coolantTank.fill(cold, action);
            if (action.execute() && filled > 0) {
                primaryLoop.recordInflow(filled, hot);
            }
            return filled;
        }
        return NuclearFluidHelper.isCoolant(resource) ? coolantTank.fill(resource, action) : 0;
    }

    /** Whether a coolant port would take {@code stack} at all. */
    public boolean acceptsCoolant(FluidStack stack) {
        return NuclearFluidHelper.isCoolant(stack) || NuclearFluidHelper.isPrimaryLoopFluid(stack) && hasPressurizer();
    }

    public FluidStack drainSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isReactorOutput(resource) ? recordDrain(steamTank.drain(resource, action), action) : FluidStack.EMPTY;
    }

    public FluidStack drainSteam(int maxDrain, IFluidHandler.FluidAction action) {
        return recordDrain(steamTank.drain(maxDrain, action), action);
    }

    private FluidStack recordDrain(FluidStack drained, IFluidHandler.FluidAction action) {
        if (action.execute() && NuclearFluidHelper.isHotPrimary(drained)) {
            primaryLoop.recordOutflow(drained.getAmount());
        }
        return drained;
    }

    public IItemHandler getFuelItemHandler(ReactorFluidPortMode mode) {
        return mode == ReactorFluidPortMode.INPUT ? fuelInputHandler : fuelOutputHandler;
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    /** Only assembled fuel: uranium at low enrichment or better, MOX or thorium. Bare uranium metal is not fuel. */
    public boolean isFreshFuel(ItemStack stack) {
        if (stack.is(AllNuclearItems.MOX_REACTOR_FUEL.get()) || stack.is(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get())) {
            return true;
        }
        return stack.is(AllNuclearItems.URANIUM_REACTOR_FUEL.get())
                && getFuelEnrichment(stack) >= MIN_FUEL_ENRICHMENT;
    }

    public static boolean isSpentFuel(ItemStack stack) {
        return stack.is(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()) || stack.is(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get())
                || stack.is(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get());
    }

    public static FuelKind fuelKindOf(ItemStack stack) {
        if (stack.is(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get())) {
            return FuelKind.THORIUM;
        }
        return stack.is(AllNuclearItems.MOX_REACTOR_FUEL.get()) ? FuelKind.MOX : FuelKind.URANIUM;
    }

    /** Enrichment a fresh assembly burns at; MOX counts as {@link FuelKind#MOX_EQUIVALENT_ENRICHMENT}. */
    public static float getFuelEnrichment(ItemStack stack) {
        if (stack.is(AllNuclearItems.MOX_REACTOR_FUEL.get())) {
            return FuelKind.MOX_EQUIVALENT_ENRICHMENT;
        }
        if (stack.is(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get())) {
            return FuelKind.THORIUM_EQUIVALENT_ENRICHMENT;
        }
        if (stack.is(AllNuclearItems.URANIUM_REACTOR_FUEL.get())) {
            return stack.has(AllNuclearDataComponents.ENRICHMENT.get())
                    ? UraniumItem.getEnrichment(stack)
                    : UraniumItem.REACTOR_FUEL_ENRICHMENT;
        }
        return 0.0f;
    }

    /** The reactor's card on the panel: a summary, or its details while sneaking. */
    @Override
    public PanelCard panelCard(net.minecraft.world.phys.Vec3 hit, boolean detailed) {
        return FissionPanel.card(this, detailed);
    }

    /** One indented goggle line, {@code create_nuclearindustry.reactor.goggles.<id>}. */
    private static Component line(String id, ChatFormatting style, Object... args) {
        return goggleLine(Component.translatable(GOGGLES + id, args), style);
    }

    /** {@link #line} for the reactor's ports, which show some of the same readings. */
    public static Component goggleLine(String id, ChatFormatting style, Object... args) {
        return line(id, style, args);
    }

    /** One indented port goggle line, {@code create_nuclearindustry.reactor.port.<id>}. */
    public static Component portLine(String id, ChatFormatting style, Object... args) {
        return goggleLine(Component.translatable(PORT + id, args), style);
    }

    /** Whether a port reaches a formed reactor, one that is not formed, or none. */
    public static Component portLinkStatus(boolean linked, boolean remembered) {
        return Component.translatable(PORT + (linked ? "linked" : remembered ? "structure_incomplete" : "no_reactor"));
    }

    public static Component portMode(ReactorFluidPortMode mode) {
        return Component.translatable(PORT + "mode." + mode.getSerializedName());
    }

    public static Component portSide(Direction side) {
        return Component.translatable(PORT + "side." + side.getSerializedName());
    }

    private static Component goggleLine(Component text, ChatFormatting style) {
        return Component.literal("  ").append(text).withStyle(style);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("status", status.name());
        tag.put("physics", writePhysicsTag());
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("fuelInventory", fuelInventory.serializeNBT(provider));
        writePendingDepleted(tag);
        tag.putBoolean("manualScram", manualScram);
        tag.putInt("scramVersion", SCRAM_VERSION);
        tag.putBoolean("legacyScramHold", legacyScramHold);
        tag.putFloat("rodTarget", rodTarget);
        autoScram.save(tag);
        tag.put("Pwr", primaryLoop.save());
        tag.put("Accident", accidents.save());
        tag.putBoolean("CoolingAfterScram", coolingAfterScram);
        tag.putBoolean("AtFullPower", atFullPower);
    }

    private void writePendingDepleted(CompoundTag tag) {
        ListTag pending = new ListTag();
        for (SpentAssembly assembly : pendingDepletedFuel) {
            CompoundTag entry = new CompoundTag();
            entry.putString("type", assembly.kind().id());
            entry.putFloat("enrichment", assembly.enrichment());
            pending.add(entry);
        }
        tag.put("pendingSpent", pending);
    }

    private void readPendingDepleted(CompoundTag tag) {
        pendingDepletedFuel.clear();
        if (tag.contains("pendingSpent", Tag.TAG_LIST)) {
            for (Tag element : tag.getList("pendingSpent", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) element;
                pendingDepletedFuel.add(new SpentAssembly(FuelKind.fromId(entry.getString("type")), entry.getFloat("enrichment")));
            }
        } else if (tag.contains("pendingDepleted", Tag.TAG_LIST)) {
            // Saves from before MOX kept only the enrichment of uranium assemblies.
            for (Tag element : tag.getList("pendingDepleted", Tag.TAG_FLOAT)) {
                pendingDepletedFuel.add(SpentAssembly.uranium(((FloatTag) element).getAsFloat()));
            }
        } else {
            // Older saves only counted pending assemblies; they were all 20% fuel.
            for (int i = 0; i < Math.max(0, tag.getInt("pendingDepletedFuel")); i++) {
                pendingDepletedFuel.add(SpentAssembly.uranium(UraniumItem.REACTOR_FUEL_ENRICHMENT));
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        status = readStatus(tag.getString("status"));
        if (tag.contains("physics")) {
            physicsSimulator.deserializeNBT(tag.getCompound("physics"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
        if (tag.contains("steam")) {
            steamTank.readFromNBT(provider, tag.getCompound("steam"));
        }
        if (tag.contains("fuelInventory")) {
            fuelInventory.deserializeNBT(provider, tag.getCompound("fuelInventory"));
        } else {
            physicsSimulator.clearFuel();
        }
        readPendingDepleted(tag);
        manualScram = tag.getBoolean("manualScram");
        legacyScramHold = tag.contains("scramVersion") ? tag.getBoolean("legacyScramHold") : manualScram;
        rodTarget = tag.contains("rodTarget") ? tag.getFloat("rodTarget") : -1.0f;
        autoScram.load(tag);
        // Saves from before the pressurized loop have none: the reactor boils its water as before.
        if (tag.contains("Pwr")) {
            primaryLoop.load(tag.getCompound("Pwr"));
        }
        accidents.load(tag.getCompound("Accident"));
        coolingAfterScram = tag.getBoolean("CoolingAfterScram");
        atFullPower = tag.getBoolean("AtFullPower");
        // A SCRAM saved with the world was already heard: no thud on load.
        scramShown = isScramRequested();
        pendingRevalidation = true;
    }

    /** Everything the client shows, as sent with the chunk. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return writeUpdateTag(provider, true);
    }

    /**
     * An update packet leaves out the structure while it is the one the clients already have: they
     * got it with the chunk or an earlier packet, and it only changes when the reactor reforms.
     */
    private CompoundTag packetTag(HolderLookup.Provider provider) {
        ReactorStructureValidator.ReactorStructure structure = currentStructure.orElse(null);
        boolean changed = structure != lastSentStructure;
        lastSentStructure = structure;
        CompoundTag tag = writeUpdateTag(provider, changed);
        if (!changed && structure != null) {
            tag.putBoolean("structureUnchanged", true);
        }
        return tag;
    }

    private CompoundTag writeUpdateTag(HolderLookup.Provider provider, boolean withStructure) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putString("status", status.name());
        // A summary of the fuel, not every assembly: the packet does not grow with the core.
        CompoundTag physics = new CompoundTag();
        physicsSimulator.serializeForClient(physics);
        tag.put("physics", physics);
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("fuelInventory", fuelInventory.serializeNBT(provider));
        // Clients only count the assemblies waiting for an output slot.
        tag.putInt("pendingDepletedFuel", pendingDepletedFuel.size());
        tag.putBoolean("manualScram", manualScram);
        tag.putBoolean("legacyScramHold", legacyScramHold);
        tag.putFloat("rodTarget", rodTarget);
        autoScram.save(tag);
        tag.put("Pwr", primaryLoop.save());
        tag.put("Accident", accidents.save());
        currentStructure.ifPresent(structure -> {
            if (withStructure) {
                tag.put("structure", writeStructureTag(structure));
            }
            tag.put("controlRodScan", writeControlRodScanTag(lastControlRodScan));
        });
        tag.put("validation", writeValidationTag(lastValidation));
        tag.put("displaySnapshot", packDisplaySnapshot(writeDisplaySnapshot(createDisplaySnapshot())));
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registries) -> packetTag(registries));
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        status = readStatus(tag.getString("status"));
        if (tag.contains("physics")) {
            physicsSimulator.deserializeNBT(tag.getCompound("physics"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
        if (tag.contains("steam")) {
            steamTank.readFromNBT(provider, tag.getCompound("steam"));
        }
        if (tag.contains("fuelInventory")) {
            fuelInventory.deserializeNBT(provider, tag.getCompound("fuelInventory"));
        }
        readPendingDepleted(tag);
        manualScram = tag.getBoolean("manualScram");
        legacyScramHold = tag.getBoolean("legacyScramHold");
        rodTarget = tag.contains("rodTarget") ? tag.getFloat("rodTarget") : -1.0f;
        autoScram.load(tag);
        primaryLoop.load(tag.getCompound("Pwr"));
        accidents.load(tag.getCompound("Accident"));
        if (!tag.getBoolean("structureUnchanged")) {
            currentStructure = tag.contains("structure")
                    ? readStructureTag(tag.getCompound("structure"))
                    : Optional.empty();
        }
        lastControlRodScan = currentStructure.isPresent() && tag.contains("controlRodScan")
                ? readControlRodScanTag(tag.getCompound("controlRodScan"))
                : ControlRodTracker.ControlRodScan.empty();
        lastValidation = tag.contains("validation")
                ? readValidationTag(tag.getCompound("validation"))
                : MultiblockValidationResult.invalid(List.of("Not formed"), List.of());
        displaySnapshot = tag.contains("displaySnapshot")
                ? readDisplaySnapshot(unpackDisplaySnapshot(tag.getCompound("displaySnapshot")))
                : ReactorDisplaySnapshot.unsynced();
        hasDisplaySnapshot = tag.contains("displaySnapshot");
    }

    private ReactorDisplaySnapshot createDisplaySnapshot() {
        if (!isFormed() || currentStructure.isEmpty()) {
            return ReactorDisplaySnapshot.empty(status.displayName, getReactorState(), lastValidation.primaryMessage());
        }

        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        return new ReactorDisplaySnapshot(
                true,
                status.displayName,
                getReactorState(),
                lastValidation.primaryMessage(),
                structure.dimensionsText(),
                structure.uraniumRodCount,
                structure.controlRodCount,
                structure.heatExchangerCount,
                structure.uraniumColumnCount,
                structure.controlRodColumnCount,
                structure.heatExchangerColumnCount,
                structure.coolantInputPortCount,
                structure.steamOutputPortCount,
                structure.fuelPortCount,
                structure.fuelInputPortCount,
                structure.fuelOutputPortCount,
                coolantTank.getFluidAmount(),
                steamTank.getFluidAmount(),
                coolantTank.getCapacity(),
                physicsSimulator.getCoreTemperature(),
                physicsSimulator.getNeutronLevel(),
                physicsSimulator.getFuelRemaining(),
                physicsSimulator.getFuelCapacity(),
                physicsSimulator.getControlRodPosition(),
                physicsSimulator.getPowerOutput(),
                physicsSimulator.getSteamGenerationRate(),
                physicsSimulator.getThermalStress(),
                physicsSimulator.isThermalExcursionActive(),
                lastControlRodScan.insertionRatio(),
                lastControlRodScan.staticSegments(),
                lastControlRodScan.movingSegments(),
                lastControlRodScan.insertedSegments(),
                lastControlRodScan.expectedSegments(),
                countInputFuel(),
                countOutputFuel(),
                pendingDepletedFuel.size(),
                physicsSimulator.getAverageFuelEnrichment(),
                manualScram,
                structure.moderatorColumns.size(),
                structure.reflectorColumns.size(),
                physicsSimulator.getModeration(),
                physicsSimulator.getReflection(),
                physicsSimulator.getXenonPenalty(),
                physicsSimulator.getDecayHeatPower(),
                physicsSimulator.getReactivity(),
                physicsSimulator.getRatedPower(),
                physicsSimulator.getCoolantTemperature(),
                physicsSimulator.getNetReactivity(),
                physicsSimulator.getPeriod(),
                physicsSimulator.getCoreDamage(),
                physicsSimulator.getShutdownMargin(),
                physicsSimulator.getReactivityParts()
        );
    }

    private boolean hasUsableDisplaySnapshot() {
        return hasDisplaySnapshot && displaySnapshot != null;
    }

    private CompoundTag writeDisplaySnapshot(ReactorDisplaySnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", snapshot.formed());
        tag.putString("statusDisplayName", snapshot.statusDisplayName());
        tag.putString("state", snapshot.state().name());
        tag.putString("validationMessage", snapshot.validationMessage());
        tag.putString("dimensions", snapshot.dimensions());
        tag.putInt("uraniumRodCount", snapshot.uraniumRodCount());
        tag.putInt("controlRodCount", snapshot.controlRodCount());
        tag.putInt("heatExchangerCount", snapshot.heatExchangerCount());
        tag.putInt("uraniumColumnCount", snapshot.uraniumColumnCount());
        tag.putInt("controlRodColumnCount", snapshot.controlRodColumnCount());
        tag.putInt("heatExchangerColumnCount", snapshot.heatExchangerColumnCount());
        tag.putInt("coolantInputPortCount", snapshot.coolantInputPortCount());
        tag.putInt("steamOutputPortCount", snapshot.steamOutputPortCount());
        tag.putInt("fuelPortCount", snapshot.fuelPortCount());
        tag.putInt("fuelInputPortCount", snapshot.fuelInputPortCount());
        tag.putInt("fuelOutputPortCount", snapshot.fuelOutputPortCount());
        tag.putInt("coolantAmount", snapshot.coolantAmount());
        tag.putInt("steamAmount", snapshot.steamAmount());
        tag.putInt("tankCapacity", snapshot.tankCapacity());
        tag.putDouble("coreTemperature", snapshot.coreTemperature());
        tag.putDouble("neutronLevel", snapshot.neutronLevel());
        tag.putDouble("fuelRemaining", snapshot.fuelRemaining());
        tag.putDouble("fuelCapacity", snapshot.fuelCapacity());
        tag.putFloat("controlRodPosition", snapshot.controlRodPosition());
        tag.putDouble("powerOutput", snapshot.powerOutput());
        tag.putDouble("steamGenerationRate", snapshot.steamGenerationRate());
        tag.putDouble("thermalStress", snapshot.thermalStress());
        tag.putBoolean("thermalExcursionActive", snapshot.thermalExcursionActive());
        tag.putFloat("controlRodInsertionRatio", snapshot.controlRodInsertionRatio());
        tag.putInt("staticControlRodSegments", snapshot.staticControlRodSegments());
        tag.putInt("movingControlRodSegments", snapshot.movingControlRodSegments());
        tag.putInt("insertedControlRodSegments", snapshot.insertedControlRodSegments());
        tag.putInt("expectedControlRodSegments", snapshot.expectedControlRodSegments());
        tag.putInt("inputFuelCount", snapshot.inputFuelCount());
        tag.putInt("outputFuelCount", snapshot.outputFuelCount());
        tag.putInt("pendingDepletedFuel", snapshot.pendingDepletedFuel());
        tag.putFloat("fuelEnrichment", snapshot.fuelEnrichment());
        tag.putBoolean("manualScram", snapshot.manualScram());
        tag.putInt("moderatorColumnCount", snapshot.moderatorColumnCount());
        tag.putInt("reflectorColumnCount", snapshot.reflectorColumnCount());
        tag.putDouble("moderation", snapshot.moderation());
        tag.putDouble("reflection", snapshot.reflection());
        tag.putDouble("xenonPenalty", snapshot.xenonPenalty());
        tag.putDouble("decayHeatPower", snapshot.decayHeatPower());
        tag.putDouble("reactivity", snapshot.reactivity());
        tag.putDouble("ratedPower", snapshot.ratedPower());
        tag.putDouble("coolantTemperature", snapshot.coolantTemperature());
        tag.putDouble("netReactivity", snapshot.netReactivity());
        tag.putDouble("period", Double.isFinite(snapshot.period()) ? snapshot.period() : 0);
        tag.putDouble("coreDamage", snapshot.coreDamage());
        tag.putDouble("shutdownMargin", snapshot.shutdownMargin());
        ReactivityModel.Parts parts = snapshot.reactivityParts();
        tag.putIntArray("reactivityParts", new int[] {(int) Math.round(parts.fuel()), (int) Math.round(parts.rods()),
                (int) Math.round(parts.temperature()), (int) Math.round(parts.voids()), (int) Math.round(parts.xenon()),
                (int) Math.round(parts.damage())});
        return tag;
    }

    private ReactorDisplaySnapshot readDisplaySnapshot(CompoundTag tag) {
        return new ReactorDisplaySnapshot(
                tag.getBoolean("formed"),
                tag.getString("statusDisplayName"),
                readReactorState(tag.getString("state")),
                defaultString(tag.getString("validationMessage"), NOT_FORMED),
                tag.getString("dimensions"),
                Math.max(0, tag.getInt("uraniumRodCount")),
                Math.max(0, tag.getInt("controlRodCount")),
                Math.max(0, tag.getInt("heatExchangerCount")),
                Math.max(0, tag.getInt("uraniumColumnCount")),
                Math.max(0, tag.getInt("controlRodColumnCount")),
                Math.max(0, tag.getInt("heatExchangerColumnCount")),
                Math.max(0, tag.getInt("coolantInputPortCount")),
                Math.max(0, tag.getInt("steamOutputPortCount")),
                Math.max(0, tag.getInt("fuelPortCount")),
                Math.max(0, tag.getInt("fuelInputPortCount")),
                Math.max(0, tag.getInt("fuelOutputPortCount")),
                Math.max(0, tag.getInt("coolantAmount")),
                Math.max(0, tag.getInt("steamAmount")),
                Math.max(1, tag.getInt("tankCapacity")),
                Math.max(20.0, tag.getDouble("coreTemperature")),
                Math.max(0.0, tag.getDouble("neutronLevel")),
                Math.max(0.0, tag.getDouble("fuelRemaining")),
                Math.max(0.0, tag.getDouble("fuelCapacity")),
                Math.max(0.0f, Math.min(1.0f, tag.getFloat("controlRodPosition"))),
                Math.max(0.0, tag.getDouble("powerOutput")),
                Math.max(0.0, tag.getDouble("steamGenerationRate")),
                Math.max(0.0, Math.min(100.0, tag.getDouble("thermalStress"))),
                tag.getBoolean("thermalExcursionActive"),
                Math.max(0.0f, Math.min(1.0f, tag.getFloat("controlRodInsertionRatio"))),
                Math.max(0, tag.getInt("staticControlRodSegments")),
                Math.max(0, tag.getInt("movingControlRodSegments")),
                Math.max(0, tag.getInt("insertedControlRodSegments")),
                Math.max(0, tag.getInt("expectedControlRodSegments")),
                Math.max(0, tag.getInt("inputFuelCount")),
                Math.max(0, tag.getInt("outputFuelCount")),
                Math.max(0, tag.getInt("pendingDepletedFuel")),
                Math.max(0.0f, tag.getFloat("fuelEnrichment")),
                tag.getBoolean("manualScram"),
                Math.max(0, tag.getInt("moderatorColumnCount")),
                Math.max(0, tag.getInt("reflectorColumnCount")),
                Math.max(0.0, Math.min(1.0, tag.getDouble("moderation"))),
                Math.max(0.0, Math.min(1.0, tag.getDouble("reflection"))),
                Math.max(0.0, Math.min(1.0, tag.getDouble("xenonPenalty"))),
                Math.max(0.0, tag.getDouble("decayHeatPower")),
                Math.max(0.0, tag.getDouble("reactivity")),
                Math.max(0.0, tag.getDouble("ratedPower")),
                Math.max(20.0, tag.getDouble("coolantTemperature")),
                tag.getDouble("netReactivity"),
                tag.getDouble("period") == 0 ? Double.POSITIVE_INFINITY : tag.getDouble("period"),
                Math.max(0.0, Math.min(1.0, tag.getDouble("coreDamage"))),
                tag.getDouble("shutdownMargin"),
                readReactivityParts(tag.getIntArray("reactivityParts"))
        );
    }

    private static ReactivityModel.Parts readReactivityParts(int[] parts) {
        if (parts == null || parts.length != 6) {
            return ReactivityModel.Parts.NONE;
        }
        return new ReactivityModel.Parts(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    /** Keys and tag types {@link #writeDisplaySnapshot} writes, sorted: the order a packed snapshot keeps. */
    private record SnapshotSchema(String[] keys, byte[] types) {}

    private static volatile SnapshotSchema snapshotSchema;

    /** Worked out once from an empty snapshot, the same way on the server and on the client. */
    private SnapshotSchema snapshotSchema() {
        SnapshotSchema schema = snapshotSchema;
        if (schema == null) {
            CompoundTag template = writeDisplaySnapshot(ReactorDisplaySnapshot.empty());
            String[] keys = template.getAllKeys().stream().sorted().toArray(String[]::new);
            byte[] types = new byte[keys.length];
            for (int i = 0; i < keys.length; i++) {
                types[i] = template.getTagType(keys[i]);
            }
            schema = new SnapshotSchema(keys, types);
            snapshotSchema = schema;
        }
        return schema;
    }

    /**
     * The display snapshot for an update packet, without its fifty-odd key names: doubles in one
     * array, ints, floats and flags in another, strings in a list, all in the order of
     * {@link #snapshotSchema()}. Anything else (and any key the schema lacks) goes as it is under
     * {@code x}. A third of the size of the named compound.
     */
    private CompoundTag packDisplaySnapshot(CompoundTag named) {
        SnapshotSchema schema = snapshotSchema();
        long[] doubles = new long[schema.keys().length];
        int[] ints = new int[schema.keys().length];
        int d = 0;
        int n = 0;
        ListTag strings = new ListTag();
        CompoundTag rest = new CompoundTag();
        Set<String> packed = new java.util.HashSet<>();
        for (int k = 0; k < schema.keys().length; k++) {
            String key = schema.keys()[k];
            switch (schema.types()[k]) {
                case Tag.TAG_DOUBLE -> doubles[d++] = Double.doubleToLongBits(named.getDouble(key));
                case Tag.TAG_FLOAT -> ints[n++] = Float.floatToIntBits(named.getFloat(key));
                case Tag.TAG_INT -> ints[n++] = named.getInt(key);
                case Tag.TAG_BYTE -> ints[n++] = named.getByte(key);
                case Tag.TAG_STRING -> strings.add(net.minecraft.nbt.StringTag.valueOf(named.getString(key)));
                default -> {
                    continue;
                }
            }
            packed.add(key);
        }
        for (String key : named.getAllKeys()) {
            if (!packed.contains(key)) {
                rest.put(key, named.get(key));
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("n", schema.keys().length);
        tag.putLongArray("d", java.util.Arrays.copyOf(doubles, d));
        tag.putIntArray("i", java.util.Arrays.copyOf(ints, n));
        tag.put("s", strings);
        if (!rest.isEmpty()) {
            tag.put("x", rest);
        }
        return tag;
    }

    /** Back to the named compound {@link #readDisplaySnapshot} reads; defaults if the sides' schemas differ. */
    private CompoundTag unpackDisplaySnapshot(CompoundTag tag) {
        if (!tag.contains("d", Tag.TAG_LONG_ARRAY)) {
            return tag;
        }
        CompoundTag named = new CompoundTag();
        SnapshotSchema schema = snapshotSchema();
        long[] doubles = tag.getLongArray("d");
        int[] ints = tag.getIntArray("i");
        ListTag strings = tag.getList("s", Tag.TAG_STRING);
        if (tag.getInt("n") == schema.keys().length) {
            int d = 0;
            int n = 0;
            int t = 0;
            for (int k = 0; k < schema.keys().length; k++) {
                String key = schema.keys()[k];
                switch (schema.types()[k]) {
                    case Tag.TAG_DOUBLE -> {
                        if (d < doubles.length) {
                            named.putDouble(key, Double.longBitsToDouble(doubles[d++]));
                        }
                    }
                    case Tag.TAG_FLOAT -> {
                        if (n < ints.length) {
                            named.putFloat(key, Float.intBitsToFloat(ints[n++]));
                        }
                    }
                    case Tag.TAG_INT -> {
                        if (n < ints.length) {
                            named.putInt(key, ints[n++]);
                        }
                    }
                    case Tag.TAG_BYTE -> {
                        if (n < ints.length) {
                            named.putByte(key, (byte) ints[n++]);
                        }
                    }
                    case Tag.TAG_STRING -> {
                        if (t < strings.size()) {
                            named.putString(key, strings.getString(t++));
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        CompoundTag rest = tag.getCompound("x");
        for (String key : rest.getAllKeys()) {
            named.put(key, rest.get(key));
        }
        return named;
    }

    /** The display snapshot as the last update brought it, on a client. For tests. */
    public ReactorDisplaySnapshot receivedDisplaySnapshot() {
        return displaySnapshot;
    }

    private CompoundTag writePhysicsTag() {
        CompoundTag physicsTag = new CompoundTag();
        physicsSimulator.serializeNBT(physicsTag);
        return physicsTag;
    }

    private CompoundTag writeStructureTag(ReactorStructureValidator.ReactorStructure structure) {
        CompoundTag structureTag = new CompoundTag();
        structureTag.putInt("width", structure.width);
        structureTag.putInt("height", structure.height);
        structureTag.putInt("bottomY", structure.bottomY);
        structureTag.putInt("uraniumRodCount", structure.uraniumRodCount);
        structureTag.putInt("controlRodCount", structure.controlRodCount);
        structureTag.putInt("heatExchangerCount", structure.heatExchangerCount);
        structureTag.putInt("uraniumColumnCount", structure.uraniumColumnCount);
        structureTag.putInt("controlRodColumnCount", structure.controlRodColumnCount);
        structureTag.putInt("heatExchangerColumnCount", structure.heatExchangerColumnCount);
        structureTag.putInt("coolantInputPortCount", structure.coolantInputPortCount);
        structureTag.putInt("steamOutputPortCount", structure.steamOutputPortCount);
        structureTag.putInt("fuelPortCount", structure.fuelPortCount);
        structureTag.putInt("fuelInputPortCount", structure.fuelInputPortCount);
        structureTag.putInt("fuelOutputPortCount", structure.fuelOutputPortCount);
        structureTag.putLongArray("controlChannels", writePositions(structure.controlChannels));
        structureTag.putLongArray("fluidPorts", writePositions(structure.fluidPorts));
        structureTag.putLongArray("fuelPorts", writePositions(structure.fuelPorts));
        structureTag.putLongArray("temperatureSensors", writePositions(structure.temperatureSensors));
        structureTag.putLongArray("redstonePorts", writePositions(structure.redstonePorts));
        structureTag.putLongArray("fuelColumns", writePositions(structure.fuelColumns));
        structureTag.putLongArray("moderatorColumns", writePositions(structure.moderatorColumns));
        structureTag.putLongArray("reflectorColumns", writePositions(structure.reflectorColumns));
        structureTag.putDouble("moderation", structure.moderation);
        structureTag.putDouble("reflection", structure.reflection);
        if (structure.pressurizer != null) {
            CompoundTag pressurizer = new CompoundTag();
            pressurizer.putLong("base", structure.pressurizer.base().asLong());
            pressurizer.putInt("segments", structure.pressurizer.segments());
            pressurizer.putLongArray("heaters", structure.pressurizer.heaterPorts().stream().mapToLong(BlockPos::asLong).toArray());
            pressurizer.putLong("relief", structure.pressurizer.reliefValve().asLong());
            structureTag.put("pressurizer", pressurizer);
        }
        return structureTag;
    }

    private Optional<ReactorStructureValidator.ReactorStructure> readStructureTag(CompoundTag structureTag) {
        int width = structureTag.getInt("width");
        int height = structureTag.getInt("height");
        if (width < ReactorStructureValidator.MIN_WIDTH || width > ReactorStructureValidator.MAX_WIDTH
                || height < ReactorStructureValidator.MIN_HEIGHT || height > ReactorStructureValidator.MAX_HEIGHT
                || width % 2 == 0) {
            return Optional.empty();
        }

        ReactorStructureValidator.ReactorStructure structure =
                new ReactorStructureValidator.ReactorStructure(getBlockPos(), width, height, structureTag.getInt("bottomY"));
        structure.uraniumRodCount = Math.max(0, structureTag.getInt("uraniumRodCount"));
        structure.controlRodCount = Math.max(0, structureTag.getInt("controlRodCount"));
        structure.heatExchangerCount = Math.max(0, structureTag.getInt("heatExchangerCount"));
        structure.uraniumColumnCount = Math.max(0, structureTag.getInt("uraniumColumnCount"));
        structure.controlRodColumnCount = Math.max(0, structureTag.getInt("controlRodColumnCount"));
        structure.heatExchangerColumnCount = Math.max(0, structureTag.getInt("heatExchangerColumnCount"));
        structure.coolantInputPortCount = Math.max(0, structureTag.getInt("coolantInputPortCount"));
        structure.steamOutputPortCount = Math.max(0, structureTag.getInt("steamOutputPortCount"));
        structure.fuelPortCount = Math.max(0, structureTag.getInt("fuelPortCount"));
        structure.fuelInputPortCount = Math.max(0, structureTag.getInt("fuelInputPortCount"));
        structure.fuelOutputPortCount = Math.max(0, structureTag.getInt("fuelOutputPortCount"));
        readPositions(structureTag.getLongArray("controlChannels"), structure.controlChannels);
        readPositions(structureTag.getLongArray("fluidPorts"), structure.fluidPorts);
        readPositions(structureTag.getLongArray("fuelPorts"), structure.fuelPorts);
        readPositions(structureTag.getLongArray("temperatureSensors"), structure.temperatureSensors);
        readPositions(structureTag.getLongArray("redstonePorts"), structure.redstonePorts);
        readPositions(structureTag.getLongArray("fuelColumns"), structure.fuelColumns);
        readPositions(structureTag.getLongArray("moderatorColumns"), structure.moderatorColumns);
        readPositions(structureTag.getLongArray("reflectorColumns"), structure.reflectorColumns);
        structure.moderation = structureTag.getDouble("moderation");
        structure.reflection = structureTag.getDouble("reflection");
        if (structureTag.contains("pressurizer")) {
            CompoundTag pressurizer = structureTag.getCompound("pressurizer");
            List<BlockPos> heaters = new ArrayList<>();
            for (long heater : pressurizer.getLongArray("heaters")) {
                heaters.add(BlockPos.of(heater));
            }
            structure.pressurizer = new PressurizerStack(BlockPos.of(pressurizer.getLong("base")), pressurizer.getInt("segments"),
                    heaters, BlockPos.of(pressurizer.getLong("relief")));
        }
        return Optional.of(structure);
    }

    private long[] writePositions(Set<BlockPos> positions) {
        long[] packed = new long[positions.size()];
        int index = 0;
        for (BlockPos pos : positions) {
            packed[index++] = pos.asLong();
        }
        return packed;
    }

    private void readPositions(long[] packed, Set<BlockPos> positions) {
        positions.clear();
        for (long value : packed) {
            positions.add(BlockPos.of(value));
        }
    }

    private CompoundTag writeControlRodScanTag(ControlRodTracker.ControlRodScan scan) {
        CompoundTag scanTag = new CompoundTag();
        scanTag.putFloat("insertionRatio", scan.insertionRatio());
        scanTag.putInt("staticSegments", scan.staticSegments());
        scanTag.putInt("movingSegments", scan.movingSegments());
        scanTag.putInt("insertedSegments", scan.insertedSegments());
        scanTag.putInt("expectedSegments", scan.expectedSegments());
        return scanTag;
    }

    private ControlRodTracker.ControlRodScan readControlRodScanTag(CompoundTag scanTag) {
        int expectedSegments = Math.max(0, scanTag.getInt("expectedSegments"));
        int insertedSegments = Math.max(0, scanTag.getInt("insertedSegments"));
        int staticSegments = Math.max(0, scanTag.getInt("staticSegments"));
        int movingSegments = Math.max(0, scanTag.getInt("movingSegments"));
        float insertionRatio = Math.max(0.0f, Math.min(1.0f, scanTag.getFloat("insertionRatio")));
        return new ControlRodTracker.ControlRodScan(insertionRatio, staticSegments, movingSegments, insertedSegments, expectedSegments);
    }

    private CompoundTag writeValidationTag(MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> validation) {
        CompoundTag validationTag = new CompoundTag();
        validationTag.putBoolean("formed", validation.formed());
        validationTag.putString("errors", String.join("\n", validation.errors()));
        validationTag.putString("warnings", String.join("\n", validation.warnings()));
        return validationTag;
    }

    private MultiblockValidationResult<ReactorStructureValidator.ReactorStructure> readValidationTag(CompoundTag validationTag) {
        List<String> errors = readStringList(validationTag.getString("errors"));
        List<String> warnings = readStringList(validationTag.getString("warnings"));
        if (validationTag.getBoolean("formed") && currentStructure.isPresent()) {
            return MultiblockValidationResult.valid(currentStructure.get(), warnings);
        }
        return MultiblockValidationResult.invalid(
                errors.isEmpty() ? List.of(NOT_FORMED) : errors,
                warnings);
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return List.of(value.split("\n"));
    }

    private ReactorPhysicsSimulator.ReactorState readReactorState(String name) {
        try {
            return ReactorPhysicsSimulator.ReactorState.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ReactorPhysicsSimulator.ReactorState.IDLE;
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private ReactorStatus readStatus(String name) {
        if ("INVALID_HOT".equals(name)) {
            return ReactorStatus.SCRAMMED;
        }
        try {
            return ReactorStatus.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ReactorStatus.UNFORMED;
        }
    }

    public record ReactorDisplaySnapshot(
            boolean formed,
            String statusDisplayName,
            ReactorPhysicsSimulator.ReactorState state,
            String validationMessage,
            String dimensions,
            int uraniumRodCount,
            int controlRodCount,
            int heatExchangerCount,
            int uraniumColumnCount,
            int controlRodColumnCount,
            int heatExchangerColumnCount,
            int coolantInputPortCount,
            int steamOutputPortCount,
            int fuelPortCount,
            int fuelInputPortCount,
            int fuelOutputPortCount,
            int coolantAmount,
            int steamAmount,
            int tankCapacity,
            double coreTemperature,
            double neutronLevel,
            double fuelRemaining,
            double fuelCapacity,
            float controlRodPosition,
            double powerOutput,
            double steamGenerationRate,
            double thermalStress,
            boolean thermalExcursionActive,
            float controlRodInsertionRatio,
            int staticControlRodSegments,
            int movingControlRodSegments,
            int insertedControlRodSegments,
            int expectedControlRodSegments,
            int inputFuelCount,
            int outputFuelCount,
            int pendingDepletedFuel,
            float fuelEnrichment,
            boolean manualScram,
            int moderatorColumnCount,
            int reflectorColumnCount,
            double moderation,
            double reflection,
            double xenonPenalty,
            double decayHeatPower,
            double reactivity,
            double ratedPower,
            double coolantTemperature,
            double netReactivity,
            double period,
            double coreDamage,
            double shutdownMargin,
            ReactivityModel.Parts reactivityParts
    ) {
        /** Fission power as a share of the rated power, 0 when there is none. */
        public double powerFraction() {
            return ratedPower <= 0 ? 0 : powerOutput / ratedPower;
        }

        public static ReactorDisplaySnapshot empty() {
            return empty(STATUS + "incomplete", ReactorPhysicsSimulator.ReactorState.IDLE, NOT_FORMED);
        }

        public static ReactorDisplaySnapshot unsynced() {
            return empty(STATUS + "syncing", ReactorPhysicsSimulator.ReactorState.IDLE, STATUS + "waiting_for_sync");
        }

        public static ReactorDisplaySnapshot empty(String statusDisplayName, ReactorPhysicsSimulator.ReactorState state, String validationMessage) {
            return new ReactorDisplaySnapshot(
                    false,
                    statusDisplayName == null || statusDisplayName.isEmpty() ? STATUS + "incomplete" : statusDisplayName,
                    state == null ? ReactorPhysicsSimulator.ReactorState.IDLE : state,
                    validationMessage == null || validationMessage.isEmpty() ? NOT_FORMED : validationMessage,
                    "",
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    TANK_CAPACITY,
                    20.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0f,
                    0.0,
                    0.0,
                    0.0,
                    false,
                    0.0f,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0.0f,
                    false,
                    0,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    20.0,
                    0.0,
                    Double.POSITIVE_INFINITY,
                    0.0,
                    0.0,
                    ReactivityModel.Parts.NONE
            );
        }
    }

    private enum ReactorStatus {
        UNFORMED("incomplete"),
        FORMED("formed"),
        SCRAMMED("scrammed"),
        SCRAMMED_HOT("scrammed_hot"),
        MELTDOWN("meltdown");

        /** Lang key of the status, kept as a string in the synced display snapshot. */
        private final String displayName;

        ReactorStatus(String id) {
            this.displayName = STATUS + id;
        }
    }

    private int countInputFuel() {
        int count = 0;
        for (int slot = 0; slot < FUEL_INPUT_SLOTS; slot++) {
            ItemStack stack = fuelInventory.getStackInSlot(slot);
            if (isFreshFuel(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countOutputFuel() {
        int count = pendingDepletedFuel.size();
        for (int slot = FUEL_INPUT_SLOTS; slot < FUEL_SLOT_COUNT; slot++) {
            count += fuelInventory.getStackInSlot(slot).getCount();
        }
        return count;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            fuelInventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private class FuelSlotView implements IItemHandlerModifiable {
        private final boolean input;

        private FuelSlotView(boolean input) {
            this.input = input;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            int actual = actualSlot(slot);
            if (actual >= 0 && isValidForView(actual, stack)) {
                fuelInventory.setStackInSlot(actual, stack);
            }
        }

        @Override
        public int getSlots() {
            return input ? FUEL_INPUT_SLOTS : FUEL_OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            int actual = actualSlot(slot);
            return actual >= 0 ? fuelInventory.getStackInSlot(actual) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int actual = actualSlot(slot);
            if (actual < 0 || !input || !isFreshFuel(stack)) {
                return stack;
            }
            return fuelInventory.insertItem(actual, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int actual = actualSlot(slot);
            if (actual < 0 || input) {
                return ItemStack.EMPTY;
            }
            return fuelInventory.extractItem(actual, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int actual = actualSlot(slot);
            return actual >= 0 && isValidForView(actual, stack);
        }

        private int actualSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                return -1;
            }
            return input ? slot : FUEL_INPUT_SLOTS + slot;
        }

        private boolean isValidForView(int actual, ItemStack stack) {
            return input ? isFreshFuel(stack) : actual >= FUEL_INPUT_SLOTS && isSpentFuel(stack);
        }
    }

}
