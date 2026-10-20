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
import org.papiricoh.create_nuclearindustry.missile.physics.SatelliteClimb;
import org.papiricoh.create_nuclearindustry.missile.orbit.Orbit;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitConfig;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitInsertion;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;
import org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteTracker;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraptionEntity;

import org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.TargetArc;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import java.util.ArrayList;
import java.util.Random;
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

    private record Debris(Flight flight, long due, Vec3 point, Vec3 velocity, int index) {}
    private final List<Debris> debris = new ArrayList<>();
    private long steps;

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

    /**
     * Launches a launch vehicle from {@code start}, heading {@code heading} radians over the ground, to climb to the insertion
     * altitude and put {@code satellite} in orbit when it gets there ({@link Flight.Kind#SATELLITE}). It carries no warhead and never
     * comes down. Returns the existing flight on replay, or null if that launch already finished.
     */
    @Nullable
    public Flight launchSatellite(ServerLevel level, UUID launchId, MissileDesign design, Vec3 start, float heading,
                                  Satellite satellite, @Nullable UUID owner,
                                  RocketShape shape, @Nullable CompoundTag cargo) {
        if (hasLaunched(launchId)) return flights.get(launchId);
        MissileFlight climb = SatelliteClimb.flight(design, Flight.v3(start), heading,
                Orbit.SEA_LEVEL + OrbitConfig.insertionAltitude, OrbitConfig.insertionAltitude / 2, MissileConfig.flightParams());
        Flight launched = new Flight(launchId, Flight.Kind.SATELLITE, climb, Warhead.INERT, BlockPos.containing(Flight.vec(climb.aim())),
                owner, level.getGameTime(), BlockPos.containing(start), List.of(), List.of(), shape, cargo, heading, null, satellite);
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

    /** Chance of the first interceptor flying at a target, or -1 when none is in flight. */
    public double interceptorPkToward(UUID target) {
        for (Flight flight : flights.values())
            if (flight.intercept() != null && flight.intercept().target().equals(target)) return flight.intercept().pk();
        return -1;
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
        if (flight.kind() != Flight.Kind.SATELLITE && MissileConfig.selfDestructPower > 0 && !level.isOutsideBuildHeight(block) && level.isLoaded(block))
            level.explode(null, position.x, position.y, position.z, (float) MissileConfig.selfDestructPower,
                    net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        return true;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FlightTracker tracker = getIfPresent(level);
            if (tracker != null && (!tracker.flights.isEmpty() || !tracker.debris.isEmpty())) {
                tracker.tick(level);
            }
        }
    }

    /** Flies everything one tick on. Public for tests and benchmarks; the level ticks it itself. */
    public void tick(ServerLevel level) {
        BallisticModel.Params params = MissileConfig.flightParams();
        steps++;
        List<Flight> arrived = new ArrayList<>();
        for (Flight flight : List.copyOf(flights.values())) {
            // Taken out earlier this tick (an interceptor's kill): it must not fly on, or land.
            if (!flights.containsKey(flight.id())) continue;
            Vec3 from = flight.position();
            MissileFlight.Step step = flight.flight().step(params);
            Vec3 impact = step.impact() ? flight.position() : null;
            boolean interceptor = flight.kind() == Flight.Kind.INTERCEPTOR;
            boolean satellite = flight.kind() == Flight.Kind.SATELLITE;
            if (!satellite && impact == null && !flight.flight().rising(params) && (!interceptor || flight.flight().tick() > 10)) {
                impact = groundHit(level, from, flight.position());
            }
            fireStepEvents(level, flight, step, params);
            if (satellite) {
                MissileFlight f = flight.flight();
                if (step.impact()) insertSatellite(level, flight);
                else if (f.burnedOut() && !f.guidedCutoff() && f.velocity().y() <= 0) remove(level, flight);
                else showPuppet(level, flight);
                continue;
            }
            if (interceptor) {
                if (step.impact()) arrived.add(flight);
                else if (impact != null) remove(level, flight, FlightFxEvent.INTERCEPTOR_MISS, impact, flight.velocity());
                else showPuppet(level, flight);
                continue;
            }
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
        for (Flight interceptor : arrived) resolve(level, interceptor, params);
        for (Debris piece : List.copyOf(debris)) {
            if (piece.due() > steps) continue;
            FlightEvents.fireDebris(level, piece.flight(), FlightFxEvent.DEBRIS_IMPACT, piece.index(), piece.point(), piece.velocity());
            debris.remove(piece);
        }
        FlightFxBroadcaster.tick(level, flights.values());
        setDirty();
    }

    /** An insertion is a rendezvous in the sky, never an impact on the world. */
    private void insertSatellite(ServerLevel level, Flight flight) {
        Satellite satellite = flight.satellite();
        if (satellite == null || !flight.flight().guidedCutoff() || !OrbitInsertion.reaches(flight.flight().design(), satellite.orbit().type(),
                OrbitConfig.leoDeltaV, OrbitConfig.geoDeltaV) || flight.position().distanceTo(flight.aim()) > 1) {
            remove(level, flight);
            return;
        }
        Orbit orbit = satellite.orbit();
        Orbit arrived = new Orbit(orbit.type(), orbit.anchorX(), orbit.anchorZ(), orbit.heading(), orbit.altitude(),
                level.getGameTime(), orbit.speed(), orbit.bounds());
        SatelliteTracker.get(level).add(level, satellite.withOrbit(arrived));
        org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level,
                flight.origin() != null ? flight.origin() : BlockPos.containing(flight.position()),
                flight.owner() == null ? null : level.getServer().getPlayerList().getPlayer(flight.owner()),
                org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.SATELLITE_IN_ORBIT);
        remove(level, flight, FlightFxEvent.ORBIT_INSERTION, flight.position(), flight.velocity());
    }

    /** Resolves only after every target has advanced, independent of insertion order. */
    private void resolve(ServerLevel level, Flight interceptor, BallisticModel.Params params) {
        Flight.Intercept intercept = interceptor.intercept();
        Flight target = intercept == null ? null : flights.get(intercept.target());
        if (target == null && intercept != null) {
            SatelliteTracker satellites = SatelliteTracker.getIfPresent(level);
            Satellite satellite = satellites == null ? null : satellites.get(intercept.target());
            if (satellite != null) {
                resolveSatellite(level, interceptor, satellite, params);
                return;
            }
        }
        if (target == null || target.kind() != Flight.Kind.MISSILE || target.flight().impacted()) {
            remove(level, interceptor, FlightFxEvent.INTERCEPTOR_MISS, interceptor.position(), interceptor.velocity());
            return;
        }
        TargetArc targetArc = TargetArc.of(target.flight(), params.gravity());
        double distance = Double.POSITIVE_INFINITY;
        if (targetArc != null) {
            for (int offset = -1; offset <= 1; offset++) {
                MissileFlight flight = interceptor.flight();
                V3 point = flight.burnedOut()
                        ? BallisticModel.position(flight.burnoutPosition(), flight.burnoutVelocity(),
                                flight.tick() - flight.burnoutTick() + offset, params.gravity())
                        : BallisticModel.position(flight.position(), flight.velocity(), offset, params.gravity());
                distance = Math.min(distance, point.distance(targetArc.at(offset)));
            }
        }
        Random random = new Random(intercept.seed());
        if (distance <= DefenseConfig.fuseRadius && random.nextDouble() < intercept.pk()) {
            Vec3 point = Flight.vec(intercept.point());
            Vec3 velocity = target.velocity();
            remove(level, target, FlightFxEvent.INTERCEPT, point, velocity);
            // Whoever owns the launcher, wherever they are, and anyone watching it.
            BlockPos launcher = intercept.launcher() != null ? intercept.launcher() : BlockPos.containing(interceptor.position());
            org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level, launcher,
                    interceptor.owner() == null ? null : level.getServer().getPlayerList().getPlayer(interceptor.owner()),
                    org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.MISSILE_INTERCEPTED);
            BlockPos block = BlockPos.containing(point);
            if (DefenseConfig.killExplosionPower > 0 && !level.isOutsideBuildHeight(block) && level.isLoaded(block))
                level.explode(null, point.x, point.y, point.z, (float) DefenseConfig.killExplosionPower,
                        net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            if (DefenseConfig.debris && targetArc != null) {
                V3 ground = targetArc.at(targetArc.remaining());
                int count = 2 + random.nextInt(2);
                for (int i = 0; i < count; i++) {
                    Vec3 scattered = Flight.vec(ground).add(random.nextGaussian() * 12, 0, random.nextGaussian() * 12);
                    debris.add(new Debris(target, steps + Math.max(1, (long) Math.ceil(targetArc.remaining())) + i * 3L,
                            scattered, velocity.scale(0.5), i));
                }
            }
            remove(level, interceptor);
        } else remove(level, interceptor, FlightFxEvent.INTERCEPTOR_MISS, interceptor.position(), interceptor.velocity());
    }

    private void resolveSatellite(ServerLevel level, Flight interceptor, Satellite satellite, BallisticModel.Params params) {
        Flight.Intercept intercept = interceptor.intercept();
        double distance = Double.POSITIVE_INFINITY;
        Vec3 point = Flight.vec(satellite.orbit().position(level.getGameTime()));
        for (int offset = -1; offset <= 1; offset++) {
            MissileFlight flight = interceptor.flight();
            V3 at = flight.burnedOut()
                    ? BallisticModel.position(flight.burnoutPosition(), flight.burnoutVelocity(),
                            flight.tick() - flight.burnoutTick() + offset, params.gravity())
                    : BallisticModel.position(flight.position(), flight.velocity(), offset, params.gravity());
            double tick = level.getGameTime() + offset;
            V3 unwrapped = satellite.orbit().unwrapped(tick, interceptor.launchedAt());
            V3 wrapped = satellite.orbit().position(tick);
            V3 nearest = at.distance(unwrapped) < at.distance(wrapped) ? unwrapped : wrapped;
            double miss = at.distance(nearest);
            if (miss < distance) { distance = miss; point = Flight.vec(nearest); }
        }
        if (distance <= DefenseConfig.fuseRadius && new Random(intercept.seed()).nextDouble() < intercept.pk()) {
            SatelliteTracker.get(level).remove(level, satellite.id(), SatelliteTracker.Removal.DESTROYED);
            BlockPos launcher = intercept.launcher() != null ? intercept.launcher() : BlockPos.containing(interceptor.position());
            org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardMachine(level, launcher,
                    interceptor.owner() == null ? null : level.getServer().getPlayerList().getPlayer(interceptor.owner()),
                    org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.SATELLITE_DESTROYED);
            FlightFxBroadcaster.event(level, satellite.id(), FlightFxEvent.SATELLITE_KILLED, 0, point,
                    Flight.vec(satellite.orbit().velocity(level.getGameTime())));
            remove(level, interceptor);
        } else remove(level, interceptor, FlightFxEvent.INTERCEPTOR_MISS, interceptor.position(), interceptor.velocity());
    }

    private void remove(ServerLevel level, Flight flight, FlightFxEvent event, Vec3 point, Vec3 velocity) {
        if (flights.remove(flight.id()) == null) return;
        FlightEvents.fire(level, flight, event, flight.flight().stage(), point, velocity);
        release(level, flight);
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
        if (flight.kind() != Flight.Kind.MISSILE) return;
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
