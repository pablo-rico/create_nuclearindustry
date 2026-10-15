package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.PlantReadable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.multiblock.BlockChange;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.PlantAlarm;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReleasePath;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The controller of a containment building, placed inside it. It measures the sealed air around
 * itself (a budgeted flood fill, repeated when a wall or the inside changes) and runs a
 * {@link ContainmentModel} of that air: steam,
 * hydrogen and radioactivity let in by leaks, condensation on the walls, recombiners, filtered
 * vents, hydrogen burns, and a burst at the weakest wall when the pressure passes its rating.
 * Monitors and alarms read it as a {@link TargetKind#CONTAINMENT}; comparators read the pressure as
 * a share of what the weakest wall holds.
 *
 * <p>Measuring is the expensive part (up to {@code pwr.containmentMaxVolume} cells and a shape
 * analysis), so the routine re-measurement every {@link #RESURVEY_INTERVAL} ticks only runs when a
 * block changed in the building since the last one (or, as a safety net, every
 * {@link #SEALED_QUIET_ROUTINES} intervals); an open monitor retries with a back-off that
 * doubles from {@link #RESURVEY_INTERVAL} up to {@link #MAX_UNSEALED_INTERVAL}. Clients get the
 * readings when what they show changes, and at least every
 * {@link PerformanceConfig#forcedSyncTicks} ticks.
 */
public class ContainmentMonitorBlockEntity extends SmartBlockEntity
        implements MultiblockController, PlantReadable, IHaveGoggleInformation {
    /** Ticks between routine re-measurements, when a block changed in the building since the last one. */
    public static final int RESURVEY_INTERVAL = 600;
    /**
     * Routine measurements a sealed building skips while no change reached it, before measuring it
     * anyway: a safety net for blocks changed without telling anyone (some contraptions move blocks
     * without a neighbour update).
     */
    public static final int SEALED_QUIET_ROUTINES = 8;
    /** Longest wait between measurements of an open monitor while nothing changes around it. */
    public static final int MAX_UNSEALED_INTERVAL = 4 * RESURVEY_INTERVAL;
    /** Ticks a change waits for others before the building is measured again. */
    private static final int SETTLE_TICKS = 10;
    /** The same while unsealed: building work nearby should not keep it measuring the open air. */
    private static final int UNSEALED_SETTLE_TICKS = 40;
    private static final int UNSEALED_REACH = 16;
    private static final int SYNC_INTERVAL = 10;
    private static final int VENT_RELEASE_INTERVAL = 100;
    /** Share of the gas's excess temperature lost per tick per m² of wall per m³ of air. */
    private static final double COOLING = 0.008;
    /** Share of the airborne activity that settles out each tick. */
    private static final double DEPOSITION = 0.0002;
    private static final double MAX_BURN_DAMAGE = 30.0;

    private final ContainmentModel model = new ContainmentModel();
    @Nullable
    private ContainmentShape shape;
    @Nullable
    private ContainmentSurvey survey;
    private boolean surveyAfterChange;
    private boolean resurveyRequested = true;
    private int settleTimer;
    private int resurveyTimer;
    /** The building was sealed when saved: the first measurement keeps its atmosphere. */
    private boolean restoredSealed;
    @Nullable
    private BoundingBox savedBounds;
    /** Burst while blocks were left alone: unsealed until a player checks the monitor or the wall is rebuilt. */
    private boolean ruptured;
    private double pendingVentActivity;
    /** Times the building has burst since it was placed. */
    private int ruptures;
    private int ventTimer;
    private int syncTimer;
    /** A block changed inside {@link #interestBounds()} since the last measurement began. */
    private boolean changedSinceSurvey;
    /** Current wait between routine measurements while unsealed. */
    private int unsealedInterval = RESURVEY_INTERVAL;
    /** Routine measurements skipped in a row while sealed. */
    private int quietRoutines;
    private int ticksSinceSync;
    @Nullable
    private int[] lastSent;
    private int surveysStarted;
    private int syncsSent;
    private long lastBurn = -1_000_000;
    private int openVents;
    // client copy
    private boolean sealedClient;
    private boolean surveyingClient;
    private float pressureClient = (float) ContainmentModel.ATMOSPHERE;
    private float hydrogenClient;
    private float steamClient;
    private float temperatureClient;
    private float activityClient;
    private float ratingClient;
    private int volumeClient;
    private int recombinersClient;
    private int ventsClient;
    private int openVentsClient;
    private boolean burnClient;

    public ContainmentMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(PwrRegistry.CONTAINMENT_MONITOR_ENTITY.get(), pos, state);
        model.reset(1, 0);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.register(this);
            ContainmentRegistry.register(this);
        }
    }

    @Override
    public void invalidate() {
        if (level != null && !level.isClientSide) {
            MultiblockRegistry.unregister(this);
            ContainmentRegistry.unregister(this);
        }
        super.invalidate();
    }

    boolean isLive() {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (++resurveyTimer >= (shape == null ? unsealedInterval : RESURVEY_INTERVAL)) {
            resurveyTimer = 0;
            routineResurvey();
        }
        if (settleTimer > 0) {
            settleTimer--;
        }
        if (survey == null && resurveyRequested && settleTimer == 0 && boundsLoaded()) {
            resurveyRequested = false;
            changedSinceSurvey = false;
            quietRoutines = 0;
            surveysStarted++;
            survey = new ContainmentSurvey(level, worldPosition, PwrConfig.containmentMaxVolume);
        }
        if (survey != null) {
            SealedVolume.State state = survey.step(PwrConfig.containmentCellsPerTick);
            if (state != SealedVolume.State.FILLING) {
                finishSurvey(survey, state);
            }
        }
        if (sealed()) {
            breathe();
        }
        ticksSinceSync++;
        if (++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            if (ticksSinceSync >= PerformanceConfig.forcedSyncTicks || !Arrays.equals(lastSent, shownToClients())) {
                sendData();
            }
        }
    }

    /**
     * The routine re-measurement. A sealed building whose blocks have not changed is still the same
     * building, so it is not measured again; changes that open or close it are measured at once
     * anyway ({@link #shouldReactToBlockChange}). An open monitor keeps looking, less and less often,
     * for walls closed out of its reach or without a block update. Its back-off ignores changes
     * around it: a solid block placed nearby is measured at once anyway, and other changes (grass
     * spreading, crops, doors) say nothing about walls, yet near a base they never stop and would
     * keep the wait at its shortest. Sealing resets the back-off.
     */
    private void routineResurvey() {
        if (shape != null) {
            if (changedSinceSurvey || ++quietRoutines >= SEALED_QUIET_ROUTINES) {
                resurveyRequested = true;
            }
            return;
        }
        unsealedInterval = Math.min(MAX_UNSEALED_INTERVAL, unsealedInterval * 2);
        resurveyRequested = true;
    }

    /** What the goggles show, rounded as they show it: a change here is worth an update to clients. */
    private int[] shownToClients() {
        boolean formed = shape != null;
        return new int[]{
                sealed() ? 1 : 0,
                survey != null ? 1 : 0,
                (int) Math.round(model.pressure() * 200),
                (int) Math.round(model.hydrogenFraction() * 10_000),
                (int) Math.round(model.steamFraction() * 100),
                (int) Math.round(model.temperature()),
                (int) Math.round(model.activity() * 100),
                formed && Double.isFinite(shape.weakestRating()) ? (int) Math.round(shape.weakestRating() * 100) : 0,
                formed ? shape.volume() : 0,
                formed ? shape.recombiners().size() : 0,
                formed ? shape.vents().size() : 0,
                openVents,
                level != null && level.getGameTime() - lastBurn < 100 ? 1 : 0,
                ruptured ? 1 : 0
        };
    }

    @Override
    public void sendData() {
        if (level != null && !level.isClientSide) {
            lastSent = shownToClients();
            ticksSinceSync = 0;
            syncsSent++;
        }
        super.sendData();
    }

    /** The chunks of the building as last saved are loaded, so a measurement now means something. */
    private boolean boundsLoaded() {
        BoundingBox box = shape != null ? shape.bounds() : savedBounds;
        return box == null || level.hasChunksAt(box.minX() - 1, box.minZ() - 1, box.maxX() + 1, box.maxZ() + 1);
    }

    private void finishSurvey(ContainmentSurvey done, SealedVolume.State state) {
        survey = null;
        boolean wasSealed = shape != null || restoredSealed;
        if (state == SealedVolume.State.SEALED) {
            ContainmentShape next = ContainmentShape.analyze(level, done.fill());
            if (wasSealed) {
                model.resize(next.volume(), next.wallArea());
            } else {
                model.reset(next.volume(), next.wallArea());
            }
            if (surveyAfterChange) {
                ruptured = false;
            }
            unsealedInterval = RESURVEY_INTERVAL;
            setShape(next);
        } else if (done.reachedUnloaded() && wasSealed) {
            // Part of the building is not loaded: that says nothing about its walls; ask again later.
            resurveyRequested = true;
            settleTimer = RESURVEY_INTERVAL / 6;
            return;
        } else {
            if (wasSealed && level instanceof ServerLevel server) {
                // A wall opened: the gas rushes out and takes the airborne activity with it.
                ReleasePath.UNFILTERED.release(server, worldPosition, model.depressurize());
            }
            model.reset(1, 0);
            ruptured = false;
            setShape(null);
        }
        restoredSealed = false;
        surveyAfterChange = false;
        sendData();
        setChanged();
    }

    private void setShape(@Nullable ContainmentShape next) {
        boolean boundsChanged = shape == null || next == null || !shape.bounds().equals(next.bounds());
        shape = next;
        savedBounds = next == null ? null : next.bounds();
        if (boundsChanged) {
            MultiblockRegistry.reindex(this);
        }
    }

    /** One tick of the sealed atmosphere: the model, vents, hydrogen burns and the walls' limit. */
    private void breathe() {
        openVents = 0;
        for (BlockPos vent : shape.vents()) {
            BlockState state = level.getBlockState(vent);
            if (state.is(PwrRegistry.FILTERED_VENT.get())
                    && state.getValue(FilteredVentBlock.OPEN)) {
                openVents++;
            }
        }
        ContainmentModel.Tick tick = model.tick(shape.recombiners().size(), openVents, params());
        pendingVentActivity += tick.ventedActivity();
        if (++ventTimer >= VENT_RELEASE_INTERVAL) {
            ventTimer = 0;
            if (pendingVentActivity > 0 && level instanceof ServerLevel server && !shape.vents().isEmpty()) {
                ReleasePath.FILTERED.release(server, shape.vents().get(0).above(), pendingVentActivity);
            }
            pendingVentActivity = 0;
        }
        if (PwrConfig.hydrogen && level.getGameTime() % 20 == 0 && model.flammable()
                && level.random.nextDouble() < ContainmentModel.ignitionChance(model.hydrogenFraction(), PwrConfig.hydrogenIgnitionChance)) {
            deflagrate();
        }
        if (PwrConfig.containmentRupture && model.pressure() > shape.weakestRating()) {
            rupture();
        }
        if (level.getGameTime() % 20 == 0 && sealed() && model.pressure() > (ContainmentModel.ATMOSPHERE + shape.weakestRating()) / 2) org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, worldPosition, 64, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.CONTAINMENT_HELD);
    }

    private static ContainmentModel.Params params() {
        return new ContainmentModel.Params(PwrConfig.condensation, COOLING, PwrConfig.recombinerRate, PwrConfig.ventFlow, DEPOSITION);
    }

    /** The hydrogen burns: a pressure spike, flames through the building and burns on whoever is inside. */
    public void deflagrate() {
        if (shape == null) {
            return;
        }
        double rise = model.burn();
        if (rise <= 0) {
            return;
        }
        lastBurn = level.getGameTime();
        PlantAlarm.raise(level, worldPosition);
        BoundingBox box = shape.bounds();
        if (level instanceof ServerLevel server) {
            for (int i = 0; i < 12; i++) {
                double x = Mth.nextDouble(level.random, box.minX(), box.maxX() + 1);
                double y = Mth.nextDouble(level.random, box.minY(), box.maxY() + 1);
                double z = Mth.nextDouble(level.random, box.minZ(), box.maxZ() + 1);
                server.sendParticles(ParticleTypes.FLAME, x, y, z, 8, 0.6, 0.6, 0.6, 0.05);
                server.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
            }
            server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, 0.6f);
        }
        float damage = (float) Math.min(MAX_BURN_DAMAGE, 4.0 + 6.0 * rise);
        AABB area = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            // Inside a containment aboard a ship means inside it in the ship's plot.
            if (shape.contains(BlockPos.containing(org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.localView(level, worldPosition, entity.position())))) {
                entity.hurt(level.damageSources().explosion(null, null), damage);
                entity.igniteForSeconds(3.0f);
            }
        }
        sendData();
    }

    /** The pressure beat the weakest wall: it bursts, the gas and its activity escape. */
    private void rupture() {
        List<BlockPos> wall = shape.weakestWall();
        BlockPos at = wall.isEmpty() ? worldPosition : wall.get(0);
        double overpressure = model.pressure() - shape.weakestRating();
        double released = model.depressurize();
        ruptures++;
        PlantAlarm.raise(level, at);
        if (level instanceof ServerLevel server) {
            ReleasePath.UNFILTERED.release(server, at, released);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 40, 1.5, 1.5, 1.5, 0.2);
            server.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6.0f, 0.5f);
        }
        if (PwrConfig.blockDestruction) {
            float power = (float) Math.min(6.0, 2.0 + overpressure);
            level.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
            for (BlockPos layer : wall) {
                BlockState state = level.getBlockState(layer);
                if (!state.isAir() && state.getDestroySpeed(level, layer) >= 0) {
                    level.destroyBlock(layer, false);
                }
            }
        } else {
            ruptured = true;
        }
        sendData();
        setChanged();
    }

    // ------------------------------------------------------------------ what goes in

    /** Whether the building is closed and holding its atmosphere. */
    public boolean sealed() {
        return shape != null && !ruptured;
    }

    /** Steam let into the building, mol, at {@code temperatureK}. */
    public void addSteam(double moles, double temperatureK) {
        if (sealed()) {
            model.addSteam(moles, temperatureK);
        }
    }

    public void addHydrogen(double moles, double temperatureK) {
        if (sealed()) {
            model.addHydrogen(moles, temperatureK);
        }
    }

    public void addActivity(double activity) {
        if (sealed()) {
            model.addActivity(activity);
        }
    }

    boolean encloses(BoundingBox box) {
        return sealed() && shape.encloses(box);
    }

    boolean covers(BlockPos pos) {
        return sealed() && (shape.contains(pos) || shape.isWall(pos));
    }

    /** A player checks the monitor: a burst wall counts as repaired and the building is measured again. */
    public void inspect(@Nullable Player player) {
        ruptured = false;
        requestStructureRevalidation(player);
        settleTimer = 0;
    }

    // ------------------------------------------------------------------ multiblock

    @Override
    public BoundingBox interestBounds() {
        if (shape != null) {
            return shape.bounds().inflatedBy(1);
        }
        return new BoundingBox(worldPosition).inflatedBy(UNSEALED_REACH);
    }

    /**
     * Openings and new obstacles matter: a wall block broken or made passable, or a solid block
     * placed in the air. Blocks changing state in place stay walls and are ignored. While unsealed,
     * only a solid block placed nearby can close the building.
     */
    @Override
    public boolean shouldReactToBlockChange(BlockChange change) {
        // Only changes inside the interest bounds get here: any of them makes the next routine measurement worth it.
        changedSinceSurvey = true;
        BlockPos pos = change.pos();
        boolean wall = !change.removed() && level != null && ContainmentSurvey.isWall(change.state(), level, pos);
        if (shape == null) {
            return wall;
        }
        if (shape.isWall(pos)) {
            return !wall || change.removed();
        }
        return shape.contains(pos) && wall;
    }

    /**
     * Measures the building again once the changes settle. Also marks the building changed, so
     * callers that skip {@link #shouldReactToBlockChange} (a crater through it, via
     * {@code MultiblockRegistry.areaChanged}) are not mistaken for a quiet building by the routine
     * measurement if this one cannot run yet.
     */
    @Override
    public void requestStructureRevalidation(@Nullable Player player) {
        changedSinceSurvey = true;
        resurveyRequested = true;
        surveyAfterChange = true;
        settleTimer = shape == null ? UNSEALED_SETTLE_TICKS : SETTLE_TICKS;
    }

    // ------------------------------------------------------------------ readings

    public ContainmentModel getModel() {
        return model;
    }

    @Nullable
    public ContainmentShape getShape() {
        return shape;
    }

    public boolean isRuptured() {
        return ruptured;
    }

    /** Times the building has burst since it was placed. */
    public int getRuptures() {
        return ruptures;
    }

    public boolean isSurveying() {
        return survey != null;
    }

    /** Measurements started since the monitor was loaded; for tests. */
    public int getSurveysStarted() {
        return surveysStarted;
    }

    /** Updates sent to clients since the monitor was loaded; for tests. */
    public int getSyncsSent() {
        return syncsSent;
    }

    /** Current wait between measurements while unsealed; for tests. */
    public int getUnsealedInterval() {
        return unsealedInterval;
    }

    /** Makes the next tick the routine re-measurement, so tests need not wait for it. */
    public void expireRoutineTimer() {
        resurveyTimer = Integer.MAX_VALUE - 1;
    }

    public int getOpenVents() {
        return openVents;
    }

    public float getPressure() {
        return level != null && level.isClientSide ? pressureClient : (float) (sealed() ? model.pressure() : ContainmentModel.ATMOSPHERE);
    }

    public float getHydrogen() {
        return level != null && level.isClientSide ? hydrogenClient : (float) (sealed() ? model.hydrogenFraction() : 0.0);
    }

    /** 0-15: pressure as a share of what the weakest wall holds. */
    public int comparatorSignal() {
        if (!sealed() || !Double.isFinite(shape.weakestRating())) {
            return 0;
        }
        double share = (model.pressure() - ContainmentModel.ATMOSPHERE) / Math.max(0.01, shape.weakestRating() - ContainmentModel.ATMOSPHERE);
        return Mth.clamp((int) Math.ceil(share * 15.0), 0, 15);
    }

    @Override
    public TargetKind plantKind() {
        return TargetKind.CONTAINMENT;
    }

    @Override
    public boolean plantFormed() {
        return sealed();
    }

    @Override
    public MonitorReading plantSample() {
        float[] v = new float[6];
        boolean sealed = sealed();
        v[MonitorReading.N_PRESSURE] = getPressure();
        v[MonitorReading.N_H2] = getHydrogen();
        v[MonitorReading.N_SEALED] = sealed ? 1 : 0;
        v[MonitorReading.N_VENT] = openVents > 0 ? 1 : 0;
        v[MonitorReading.N_ACTIVITY] = sealed ? (float) model.activity() : 0;
        v[MonitorReading.N_VOLUME] = shape == null ? 0 : shape.volume();
        String status = ruptured ? STATUS + "ruptured"
                : !sealed ? "create_nuclearindustry.control_room.status.unformed"
                : model.hydrogenFraction() >= ContainmentModel.LOWER_FLAMMABILITY ? STATUS + "hydrogen"
                : openVents > 0 ? STATUS + "venting"
                : STATUS + "sealed";
        float rating = sealed && Double.isFinite(shape.weakestRating()) ? (float) shape.weakestRating() : 10.0f;
        float pressureSeries = Mth.clamp((v[MonitorReading.N_PRESSURE] - 1.0f) / Math.max(0.01f, rating - 1.0f), 0.0f, 1.0f);
        float hydrogenSeries = Mth.clamp(v[MonitorReading.N_H2] / (float) ContainmentModel.CERTAIN_IGNITION, 0.0f, 1.0f);
        return new MonitorReading(TargetKind.CONTAINMENT, status, v, pressureSeries, hydrogenSeries);
    }

    private static final String STATUS = "create_nuclearindustry.containment.status.";

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.containment.goggles.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.containment_monitor"))
                .withStyle(ChatFormatting.GOLD));
        if (!sealedClient) {
            tooltip.add(line(Component.translatable(surveyingClient ? k + "surveying" : k + "open"),
                    surveyingClient ? ChatFormatting.YELLOW : ChatFormatting.RED));
            return true;
        }
        tooltip.add(line(Component.translatable(k + "volume", volumeClient, format(ratingClient)), ChatFormatting.GRAY));
        double share = (pressureClient - ContainmentModel.ATMOSPHERE) / Math.max(0.01, ratingClient - ContainmentModel.ATMOSPHERE);
        tooltip.add(line(Component.translatable(k + "pressure", format(pressureClient), Math.round(temperatureClient - 273.15f)),
                share >= 0.8 ? ChatFormatting.RED : share >= 0.4 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(k + "gas", format(hydrogenClient * 100), Math.round(steamClient * 100)),
                hydrogenClient >= ContainmentModel.CERTAIN_IGNITION ? ChatFormatting.RED
                        : hydrogenClient >= ContainmentModel.LOWER_FLAMMABILITY ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(k + "devices", recombinersClient, ventsClient, openVentsClient), ChatFormatting.GRAY));
        if (activityClient > 0.01f) {
            tooltip.add(line(Component.translatable(k + "activity", format(activityClient)), ChatFormatting.YELLOW));
        }
        if (burnClient) {
            tooltip.add(line(Component.translatable(k + "burn"), ChatFormatting.RED));
        }
        return true;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, value < 10 ? "%.2f" : "%.0f", value);
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag gas = new CompoundTag();
        gas.putDouble("Volume", model.volume());
        gas.putDouble("Walls", model.wallArea());
        gas.putDouble("Inert", model.inert());
        gas.putDouble("Oxygen", model.oxygen());
        gas.putDouble("Steam", model.steam());
        gas.putDouble("Hydrogen", model.hydrogen());
        gas.putDouble("Temperature", model.temperature());
        gas.putDouble("Activity", model.activity());
        tag.put("Gas", gas);
        tag.putBoolean("Sealed", shape != null || restoredSealed);
        tag.putBoolean("Ruptured", ruptured);
        tag.putDouble("VentActivity", pendingVentActivity);
        BoundingBox bounds = shape != null ? shape.bounds() : savedBounds;
        if (bounds != null) {
            tag.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        }
        if (clientPacket) {
            tag.putBoolean("ClientSealed", sealed());
            tag.putBoolean("Surveying", survey != null);
            tag.putFloat("Pressure", (float) model.pressure());
            tag.putFloat("H2", (float) model.hydrogenFraction());
            tag.putFloat("SteamFraction", (float) model.steamFraction());
            tag.putFloat("Rating", shape == null || !Double.isFinite(shape.weakestRating()) ? 0 : (float) shape.weakestRating());
            tag.putInt("Cells", shape == null ? 0 : shape.volume());
            tag.putInt("Recombiners", shape == null ? 0 : shape.recombiners().size());
            tag.putInt("Vents", shape == null ? 0 : shape.vents().size());
            tag.putInt("OpenVents", openVents);
            tag.putBoolean("Burn", level != null && level.getGameTime() - lastBurn < 100);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        CompoundTag gas = tag.getCompound("Gas");
        model.restore(gas.getDouble("Volume"), gas.getDouble("Walls"), gas.getDouble("Inert"), gas.getDouble("Oxygen"),
                gas.getDouble("Steam"), gas.getDouble("Hydrogen"), gas.getDouble("Temperature"), gas.getDouble("Activity"));
        ruptured = tag.getBoolean("Ruptured");
        pendingVentActivity = Math.max(0, tag.getDouble("VentActivity"));
        int[] bounds = tag.getIntArray("Bounds");
        savedBounds = bounds.length == 6 ? new BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]) : null;
        if (clientPacket) {
            sealedClient = tag.getBoolean("ClientSealed");
            surveyingClient = tag.getBoolean("Surveying");
            pressureClient = tag.getFloat("Pressure");
            hydrogenClient = tag.getFloat("H2");
            steamClient = tag.getFloat("SteamFraction");
            temperatureClient = (float) model.temperature();
            activityClient = (float) model.activity();
            ratingClient = tag.getFloat("Rating");
            volumeClient = tag.getInt("Cells");
            recombinersClient = tag.getInt("Recombiners");
            ventsClient = tag.getInt("Vents");
            openVentsClient = tag.getInt("OpenVents");
            burnClient = tag.getBoolean("Burn");
        } else {
            restoredSealed = tag.getBoolean("Sealed");
            resurveyRequested = true;
        }
    }
}
