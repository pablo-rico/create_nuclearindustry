package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraptionEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything flying in a level, saved with it. Each flight is worked out here every tick from its
 * {@link MissileFlight}, with no entity and no chunks loaded, so a missile crosses the world without
 * dragging its chunks along. Near players it shows as a puppet: the {@link RocketContraptionEntity} a silo rocket was
 * assembled into, or a {@link MissileEntity} for flights without one; where its chunk
 * is loaded it can hit the ground on the way; in its last moments it forces the chunk it comes down in,
 * so the impact always lands. Only operational radars expose flights to players.
 */
public final class FlightTracker extends SavedData {
    private static final String NAME = "create_nuclearindustry_flights";
    /** Ticks before impact the impact's chunk is forced, so the warhead goes off in a loaded world. */
    private static final int FORCE_IMPACT_CHUNK_TICKS = 40;

    private final Map<UUID, Flight> flights = new LinkedHashMap<>();
    // Permanent receipts: replaying a silo transaction must not relaunch an already impacted missile.
    private final java.util.Set<UUID> launchedIds = new java.util.HashSet<>();

    public boolean hasLaunched(UUID id) { return launchedIds.contains(id); }

    /** Returns the existing flight on replay, or null if that launch already finished. */
    @Nullable
    public Flight launchModular(ServerLevel level, UUID launchId, MissileDesign design, Vec3 start, Vec3 aim,
                                BlockPos target, Warhead warhead, @Nullable UUID owner,
                                List<String> sections, List<Integer> stageStarts) {
        return launchModular(level, launchId, design, start, aim, target, warhead, owner, RocketShape.column(sections.size(), stageStarts),
                null, (float) RocketAttitude.heading(aim.x - start.x, aim.z - start.z), sections, stageStarts);
    }

    /**
     * Launches a silo rocket of outline {@code shape}, assembled from {@code cargo} (a saved contraption, or null), heading
     * {@code heading} radians over the ground. Returns the existing flight on replay, or null if that launch already finished.
     */
    @Nullable
    public Flight launchModular(ServerLevel level, UUID launchId, MissileDesign design, Vec3 start, Vec3 aim,
                                BlockPos target, Warhead warhead, @Nullable UUID owner, RocketShape shape,
                                @Nullable CompoundTag cargo, float heading) {
        return launchModular(level, launchId, design, start, aim, target, warhead, owner, shape, cargo, heading, List.of(), List.of());
    }

    @Nullable
    private Flight launchModular(ServerLevel level, UUID launchId, MissileDesign design, Vec3 start, Vec3 aim, BlockPos target,
                                 Warhead warhead, @Nullable UUID owner, RocketShape shape, @Nullable CompoundTag cargo, float heading,
                                 List<String> sections, List<Integer> stageStarts) {
        if (hasLaunched(launchId)) return flights.get(launchId);
        Flight launched = new Flight(launchId, Flight.Kind.MISSILE,
                new MissileFlight(design, Flight.v3(start), Flight.v3(aim), Flight.v3(Vec3.ZERO)),
                warhead, target, owner, level.getGameTime(), BlockPos.containing(start), sections, stageStarts, shape, cargo, heading);
        flights.put(launchId, launched);
        launchedIds.add(launchId);
        setDirty();
        FlightEvents.fire(level, launched, FlightFxEvent.LAUNCH, 0);
        return launched;
    }

    /**
     * Launches an interceptor from {@code start}, leaving at {@code ejectVelocity} and climbing {@code rise} blocks before it turns,
     * to meet {@code intercept.target()} at {@code intercept.point()} on tick {@code intercept.arrivalTick()} of its flight. It
     * carries no warhead: what happens when it gets there is drawn from {@code intercept.pk()}. Returns the existing flight on
     * replay, or null if that launch already finished.
     */
    @Nullable
    public Flight launchInterceptor(ServerLevel level, UUID launchId, MissileDesign design, Vec3 start, Vec3 ejectVelocity, double rise,
                                    Flight.Intercept intercept, @Nullable UUID owner, RocketShape shape, @Nullable CompoundTag cargo) {
        if (hasLaunched(launchId)) return flights.get(launchId);
        MissileFlight flight = MissileFlight.timed(design, Flight.v3(start), intercept.point(), Flight.v3(ejectVelocity),
                intercept.arrivalTick(), rise);
        Vec3 point = Flight.vec(intercept.point());
        Flight launched = new Flight(launchId, Flight.Kind.INTERCEPTOR, flight, Warhead.INERT, BlockPos.containing(point), owner,
                level.getGameTime(), BlockPos.containing(start), List.of(), List.of(), shape, cargo,
                (float) RocketAttitude.heading(point.x - start.x, point.z - start.z), intercept);
        flights.put(launchId, launched);
        launchedIds.add(launchId);
        setDirty();
        FlightEvents.fire(level, launched, FlightFxEvent.LAUNCH, 0);
        return launched;
    }

    /** How many interceptors are flying at flight {@code target}. */
    public int interceptorsToward(UUID target) {
        int count = 0;
        for (Flight flight : flights.values())
            if (flight.intercept() != null && flight.intercept().target().equals(target)) count++;
        return count;
    }

    /** Every interceptor in flight, in launch order. */
    public List<Flight> interceptors() {
        return flights.values().stream().filter(flight -> flight.kind() == Flight.Kind.INTERCEPTOR).toList();
    }

    public static FlightTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(FlightTracker::new, FlightTracker::load, null), NAME);
    }

    /** The level's tracker if it ever had one, else null; never creates it. */
    @Nullable
    public static FlightTracker getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(FlightTracker::new, FlightTracker::load, null), NAME);
    }

    public Collection<Flight> flights() {
        return Collections.unmodifiableCollection(flights.values());
    }

    @Nullable
    public Flight get(UUID id) {
        return flights.get(id);
    }

    /**
     * Sends {@code design} from {@code start} towards {@code aim}, the point it is to come down on (above
     * {@code target} for an airburst, or off it by the guidance's miss).
     */
    public Flight launch(ServerLevel level, MissileDesign design, Vec3 start, Vec3 velocity, Vec3 aim, BlockPos target,
                         Warhead warhead, @Nullable UUID owner) {
        MissileFlight flight = new MissileFlight(design, Flight.v3(start), Flight.v3(aim), Flight.v3(velocity));
        Flight launched = new Flight(UUID.randomUUID(), Flight.Kind.MISSILE, flight, warhead, target, owner, level.getGameTime(),
                BlockPos.containing(start), List.of(), List.of(), RocketShape.LEGACY, null, Flight.headingOf(flight));
        flights.put(launched.id(), launched);
        launchedIds.add(launched.id());
        setDirty();
        FlightEvents.fire(level, launched, FlightFxEvent.LAUNCH, 0);
        return launched;
    }

    /** Takes a flight out without it landing: its threat, puppet and forced chunk go with it. */
    public void remove(ServerLevel level, Flight flight) {
        if (flights.remove(flight.id()) != null) {
            FlightEvents.fire(level, flight, FlightFxEvent.ABORT, flight.flight().stage());
            release(level, flight);
            setDirty();
        }
    }

    /** Removes the flight and releases its resources before a small conventional self-destruct. */
    public boolean abort(ServerLevel level, UUID id) {
        Flight flight = flights.get(id);
        if (flight == null) return false;
        Vec3 position = flight.position();
        remove(level, flight);
        BlockPos block = BlockPos.containing(position);
        if (MissileConfig.selfDestructPower > 0 && !level.isOutsideBuildHeight(block) && level.isLoaded(block))
            level.explode(null, position.x, position.y, position.z, (float) MissileConfig.selfDestructPower,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        return true;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FlightTracker tracker = getIfPresent(level);
            if (tracker != null && !tracker.flights.isEmpty()) {
                tracker.tick(level);
            }
        }
    }

    /** Flies everything one tick on. Public for tests and benchmarks; the level ticks it itself. */
    public void tick(ServerLevel level) {
        BallisticModel.Params params = MissileConfig.flightParams();
        for (Flight flight : List.copyOf(flights.values())) {
            // Taken out earlier this tick (an interceptor's kill): it must not fly on, or land.
            if (!flights.containsKey(flight.id())) continue;
            Vec3 from = flight.position();
            MissileFlight.Step step = flight.flight().step(params);
            Vec3 impact = step.impact() ? flight.position() : null;
            if (impact == null && !flight.flight().rising(params)) {
                impact = groundHit(level, from, flight.position());
            }
            fireStepEvents(level, flight, step, params);
            if (impact != null) {
                flights.remove(flight.id());
                FlightEvents.fire(level, flight, FlightFxEvent.IMPACT, flight.flight().stage(), impact, flight.velocity());
                release(level, flight);
                flight.warhead().detonate(level, impact);
                continue;
            }
            forceImpactChunk(level, flight, params);
            showPuppet(level, flight);
        }
        FlightFxBroadcaster.tick(level, flights.values());
        setDirty();
    }

    /** The moments of this tick's step: stages dropping and lighting, burnout, and the phases the effects mark. */
    private static void fireStepEvents(ServerLevel level, Flight flight, MissileFlight.Step step, BallisticModel.Params params) {
        MissileFlight f = flight.flight();
        int stages = f.design().stages().size();
        if (step.stageDropped()) {
            FlightEvents.fire(level, flight, FlightFxEvent.STAGE_SEPARATION, f.stage() - 1);
            if (!f.burnedOut() && f.stage() < stages) FlightEvents.fire(level, flight, FlightFxEvent.STAGE_IGNITION, f.stage());
        }
        if (step.burnout()) {
            FlightEvents.fire(level, flight, FlightFxEvent.BURNOUT, f.stage());
            if (f.stage() < stages) FlightEvents.fire(level, flight, FlightFxEvent.PAYLOAD_SEPARATION, f.stage());
        }
        MissileFlight.Phase phase = f.phase(params);
        // Re-entry is coming back down into the air, not burning out below its top on the way up.
        boolean inAir = f.burnedOut() && f.velocity().y() < 0 && f.position().y() <= params.atmosphereTop();
        if (flight.lastPhase == null) {
            // First tick after a launch or a load: take things as they are without firing.
            flight.reentered = inAir;
        } else {
            if (inAir && !flight.reentered) {
                flight.reentered = true;
                FlightEvents.fire(level, flight, FlightFxEvent.REENTRY, f.stage());
            }
            if (phase == MissileFlight.Phase.TERMINAL && flight.lastPhase != MissileFlight.Phase.TERMINAL) {
                FlightEvents.fire(level, flight, FlightFxEvent.TERMINAL, f.stage());
            }
        }
        flight.lastPhase = phase;
    }

    private static void forceImpactChunk(ServerLevel level, Flight flight, BallisticModel.Params params) {
        if (flight.forcedChunk != Flight.NO_CHUNK || !flight.flight().burnedOut()
                || flight.flight().ticksToImpact(params) > FORCE_IMPACT_CHUNK_TICKS) {
            return;
        }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(flight.aim()));
        if (!level.getForcedChunks().contains(chunk.toLong())) {
            level.setChunkForced(chunk.x, chunk.z, true);
            flight.forcedChunk = chunk.toLong();
        }
    }

    /**
     * Where the flight met the ground between {@code from} and {@code to}, or null. Only looked for where
     * every chunk on the way is loaded: a flight never loads one to find out.
     */
    @Nullable
    private static Vec3 groundHit(ServerLevel level, Vec3 from, Vec3 to) {
        if (to.y < level.getMinBuildHeight()) {
            return to;
        }
        if (Math.min(from.y, to.y) >= level.getMaxBuildHeight()) {
            return null;
        }
        int samples = Math.max(1, Mth.ceil(from.distanceTo(to) / 8.0));
        for (int i = 0; i <= samples; i++) {
            Vec3 at = from.lerp(to, i / (double) samples);
            if (!level.hasChunk(Mth.floor(at.x) >> 4, Mth.floor(at.z) >> 4)) {
                return null;
            }
        }
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    /** Shows the flight as a puppet entity while a player is near and its chunk has its entities loaded. */
    private static void showPuppet(ServerLevel level, Flight flight) {
        Vec3 pos = flight.position();
        Entity puppet = flight.puppet == null ? null : level.getEntity(flight.puppet);
        BlockPos block = BlockPos.containing(pos);
        boolean loaded = pos.y >= level.getMinBuildHeight() && level.areEntitiesLoaded(ChunkPos.asLong(block));
        if (!loaded || !watched(level, pos)) {
            if (puppet != null) {
                puppet.discard();
            }
            flight.puppet = null;
            return;
        }
        // A silo rocket shows as the contraption it was built from; older flights, and any whose cargo cannot be read, as a missile.
        if (flight.cargo() != null && !flight.cargoUnreadable) {
            if (puppet instanceof RocketContraptionEntity rocket) {
                rocket.follow(flight);
                return;
            }
            if (puppet != null) puppet.discard();
            RocketContraptionEntity rocket = RocketContraptionEntity.create(level, flight);
            if (rocket != null) {
                flight.puppet = level.addFreshEntity(rocket) ? rocket.getUUID() : null;
                return;
            }
            flight.cargoUnreadable = true;
            puppet = null;
        }
        if (puppet instanceof MissileEntity missile) {
            missile.follow(flight);
            return;
        }
        if (puppet != null) puppet.discard();
        MissileEntity missile = new MissileEntity(MissileRegistry.MISSILE.get(), level);
        missile.follow(flight);
        if (level.addFreshEntity(missile)) {
            flight.puppet = missile.getUUID();
        }
    }

    private static boolean watched(ServerLevel level, Vec3 pos) {
        double range = MissileConfig.showDistance;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - pos.x;
            double dz = player.getZ() - pos.z;
            if (dx * dx + dz * dz <= range * range) {
                return true;
            }
        }
        return false;
    }

    private static void release(ServerLevel level, Flight flight) {
        ThreatRegistry.withdraw(level, flight.id());
        if (flight.puppet != null) {
            Entity puppet = level.getEntity(flight.puppet);
            if (puppet != null) {
                puppet.discard();
            }
            flight.puppet = null;
        }
        if (flight.forcedChunk != Flight.NO_CHUNK) {
            ChunkPos chunk = new ChunkPos(flight.forcedChunk);
            level.setChunkForced(chunk.x, chunk.z, false);
            flight.forcedChunk = Flight.NO_CHUNK;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        flights.values().forEach(flight -> list.add(flight.save()));
        tag.put("Flights", list);
        ListTag receipts = new ListTag();
        launchedIds.forEach(id -> receipts.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
        tag.put("LaunchedIds", receipts);
        return tag;
    }

    /** Public for tests: a tracker as it would load from {@code tag}. */
    public static FlightTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        FlightTracker tracker = new FlightTracker();
        for (Tag receipt : tag.getList("LaunchedIds", Tag.TAG_INT_ARRAY)) {
            tracker.launchedIds.add(net.minecraft.nbt.NbtUtils.loadUUID(receipt));
        }
        for (Tag t : tag.getList("Flights", Tag.TAG_COMPOUND)) {
            Flight flight = Flight.load((CompoundTag) t);
            tracker.flights.put(flight.id(), flight);
            tracker.launchedIds.add(flight.id());
        }
        return tracker;
    }
}
