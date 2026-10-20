package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.radar.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Synchronous tracker steps also exercise the block-entity-before-level-tick launch order. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public final class InterceptTests {
    private static final String BATCH = "intercept";
    private static final V3 START = new V3(20_000_000, 1000, 20_000_000);

    private static Flight coast(GameTestHelper h, FlightTracker tracker, Warhead warhead, UUID owner) {
        ServerLevel level = h.getLevel();
        Flight flight = tracker.launch(level, MissileDesign.LEGACY_PAD, Flight.vec(START), Vec3.ZERO,
                Flight.vec(START.add(new V3(1500, -937, 0))), BlockPos.containing(START.x() + 1500, 63, START.z()), warhead, owner);
        for (int i = 0; i < 10000 && !flight.flight().burnedOut(); i++) tracker.tick(level);
        h.assertTrue(flight.flight().burnedOut() && !flight.flight().impacted(), "target must be coasting");
        return flight;
    }

    private static InterceptorEnvelope envelope(V3 launch) {
        return new InterceptorEnvelope(InterceptorDesigns.battery(9, 0.5), 0, 0.6, 2000,
                launch.y() - 1000, launch.y() + 2000, true, 0.75, 20, 200);
    }

    private static InterceptSolution solution(GameTestHelper h, Flight target, V3 launch) {
        var params = MissileConfig.flightParams();
        TargetArc arc = TargetArc.of(target.flight(), params.gravity());
        h.assertTrue(arc != null, "only coasting missiles are engaged");
        var result = InterceptSolver.solve(arc, launch, envelope(launch), params, 0);
        h.assertTrue(result.isPresent(), "nearby interceptor must have a solution");
        return result.get();
    }

    private static Flight interceptor(GameTestHelper h, FlightTracker tracker, Flight target, V3 launch,
                                      InterceptSolution solution, double pk) {
        Flight.Intercept intercept = new Flight.Intercept(target.id(), pk, solution.point(), solution.arrivalTicks(),
                BlockPos.containing(launch.x(), launch.y(), launch.z()), ShooterType.BATTERY, 12);
        Flight flight = tracker.launchInterceptor(h.getLevel(), UUID.randomUUID(), envelope(launch).design(), Flight.vec(launch),
                Flight.vec(solution.ejectVelocity()), 0, intercept, null, InterceptorDesigns.BATTERY_SHAPE, null);
        h.assertTrue(flight != null, "new interceptor must launch");
        return flight;
    }

    private static void resolve(GameTestHelper h, double pk, boolean abort, Warhead warhead) {
        FlightTracker tracker = new FlightTracker();
        boolean debris = DefenseConfig.debris;
        double explosion = DefenseConfig.killExplosionPower;
        List<FlightFxEvent> events = new ArrayList<>();
        int[] detonations = {0};
        Flight target = coast(h, tracker, warhead, null);
        FlightEvents.Listener listener = (level, flight, event, stage, point, velocity) -> {
            if (flight.id().equals(target.id()) || flight.kind() == Flight.Kind.INTERCEPTOR) events.add(event);
        };
        Consumer<NuclearDetonationEvent> nuclear = event -> detonations[0]++;
        FlightEvents.addListener(listener);
        NeoForge.EVENT_BUS.addListener(nuclear);
        try {
            DefenseConfig.debris = false;
            DefenseConfig.killExplosionPower = 0;
            V3 launch = target.flight().position().sub(new V3(100, 100, 0));
            var solution = solution(h, target, launch);
            Flight interceptor = interceptor(h, tracker, target, launch, solution, pk);
            if (abort) tracker.remove(h.getLevel(), target);
            for (int i = 0; i < solution.arrivalTicks(); i++) tracker.tick(h.getLevel());
            h.assertTrue(tracker.get(interceptor.id()) == null, "arrived interceptor is removed");
            if (pk == 1 && !abort) {
                h.assertTrue(tracker.get(target.id()) == null, "successful interception removes target");
                h.assertTrue(events.contains(FlightFxEvent.INTERCEPT), "kill fires INTERCEPT");
            } else {
                h.assertTrue(events.contains(FlightFxEvent.INTERCEPTOR_MISS), "miss or gone target fires INTERCEPTOR_MISS");
                if (!abort) h.assertTrue(tracker.get(target.id()) != null, "zero Pk leaves target flying");
            }
            h.assertTrue(!events.contains(FlightFxEvent.IMPACT), "interception never fires a ground impact");
            h.assertTrue(detonations[0] == 0, "intercepted warhead never detonates");
        } finally {
            FlightEvents.removeListener(listener);
            NeoForge.EVENT_BUS.unregister(nuclear);
            for (Flight flight : List.copyOf(tracker.flights())) tracker.remove(h.getLevel(), flight);
            DefenseConfig.debris = debris;
            DefenseConfig.killExplosionPower = explosion;
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void killsCoastingMissile(GameTestHelper h) { resolve(h, 1, false, Warhead.INERT); }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void zeroPkMisses(GameTestHelper h) { resolve(h, 0, false, Warhead.INERT); }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void targetAbortedBeforeArrival(GameTestHelper h) { resolve(h, 1, true, Warhead.INERT); }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_intercept")
    public static void nuclearTargetNeverDetonates(GameTestHelper h) {
        try {
            resolve(h, 1, false, Warhead.fission(1, 1, 1, 0));
        } finally {
            ContaminationTests.clean(h, 0);
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void arrivesOnTheTick(GameTestHelper h) {
        FlightTracker tracker = new FlightTracker();
        Flight target = coast(h, tracker, Warhead.INERT, null);
        try {
            V3 launch = target.flight().position().sub(new V3(100, 100, 0));
            var solution = solution(h, target, launch);
            Flight interceptor = interceptor(h, tracker, target, launch, solution, 0);
            // Put the interceptor first: resolution must wait for the target's step too.
            CompoundTag saved = tracker.save(new CompoundTag(), h.getLevel().registryAccess());
            var reversed = new net.minecraft.nbt.ListTag();
            var flights = saved.getList("Flights", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = flights.size() - 1; i >= 0; i--) reversed.add(flights.get(i));
            saved.put("Flights", reversed);
            UUID targetId = target.id(), interceptorId = interceptor.id();
            tracker = FlightTracker.load(saved, h.getLevel().registryAccess());
            target = tracker.get(targetId);
            interceptor = tracker.get(interceptorId);
            h.assertTrue(target != null && interceptor != null, "both flights restored in reverse order");
            for (int n = 1; n < solution.arrivalTicks(); n++) {
                tracker.tick(h.getLevel());
                h.assertTrue(tracker.get(interceptor.id()) != null, "interceptor does not arrive early");
            }
            tracker.tick(h.getLevel());
            h.assertTrue(interceptor.flight().tick() == solution.arrivalTicks(), "arrival uses exactly n tracker steps");
            h.assertTrue(interceptor.position().distanceTo(target.position()) < 1, "target and interceptor share the arrival tick");
        } finally {
            for (Flight flight : List.copyOf(tracker.flights())) tracker.remove(h.getLevel(), flight);
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void interceptorSurvivesSaveLoad(GameTestHelper h) {
        FlightTracker tracker = new FlightTracker();
        Flight target = coast(h, tracker, Warhead.INERT, null);
        try {
            V3 launch = target.flight().position().sub(new V3(100, 100, 0));
            var solution = solution(h, target, launch);
            Flight interceptor = interceptor(h, tracker, target, launch, solution, 1);
            tracker.tick(h.getLevel());
            var saved = tracker.save(new CompoundTag(), h.getLevel().registryAccess());
            var restored = FlightTracker.load(saved, h.getLevel().registryAccess());
            Flight copy = restored.get(interceptor.id());
            h.assertTrue(copy != null, "interceptor is saved");
            h.assertTrue(interceptor.intercept().equals(copy.intercept()), "interception metadata survives");
            h.assertTrue(copy.flight().arrivalTick() == solution.arrivalTicks() && copy.flight().rise() == 0, "timing and rise survive");
            for (int n = 1; n < solution.arrivalTicks(); n++) restored.tick(h.getLevel());
            h.assertTrue(restored.get(target.id()) == null, "restored interceptor resolves its kill");
        } finally {
            for (Flight flight : List.copyOf(tracker.flights())) tracker.remove(h.getLevel(), flight);
        }
        h.succeed();
    }

    private static RadarBlockEntity radar(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = h.getBlockEntity(pos);
        h.assertTrue(radar != null, "radar exists");
        radar.setSpeed((float) MissileConfig.minimumRpm);
        return radar;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void radarIgnoresInterceptors(GameTestHelper h) {
        RadarBlockEntity radar = radar(h);
        // A radar joins the network on its first tick.
        h.runAfterDelay(2, () -> radarIgnoresInterceptors(h, radar));
    }

    private static void radarIgnoresInterceptors(GameTestHelper h, RadarBlockEntity radar) {
        radar.setSpeed((float) MissileConfig.minimumRpm);
        FlightTracker tracker = FlightTracker.get(h.getLevel());
        Flight target = coast(h, tracker, Warhead.INERT, null);
        double range = MissileConfig.radarBaseRange, earth = MissileConfig.radarEarthRadius;
        Flight interceptor = null;
        try {
            MissileConfig.radarBaseRange = 0;
            MissileConfig.radarEarthRadius = 0;
            V3 launch = target.flight().position().sub(new V3(100, 100, 0));
            interceptor = interceptor(h, tracker, target, launch, solution(h, target, launch), 0);
            RadarNetwork.refresh(h.getLevel());
            h.assertTrue(RadarNetwork.watchers(h.getLevel(), target.id()) > 0, "powered radar sees target");
            h.assertTrue(RadarNetwork.watchers(h.getLevel(), interceptor.id()) == 0, "radar never watches interceptors");
            h.assertTrue(!RadarNetwork.tracks(h.getLevel()).containsKey(interceptor.id()), "interceptor has no radar track");
        } finally {
            tracker.remove(h.getLevel(), target);
            if (interceptor != null) tracker.remove(h.getLevel(), interceptor);
            radar.setSpeed(0);
            MissileConfig.radarBaseRange = range;
            MissileConfig.radarEarthRadius = earth;
        }
        h.succeed();
    }

    private static final class Shooter implements InterceptorShooter {
        final V3 launch;
        int orders;
        Shooter(V3 launch) { this.launch = launch; }
        public BlockPos shooterPos() { return BlockPos.containing(launch.x(), launch.y(), launch.z()); }
        public ShooterType shooterType() { return ShooterType.BATTERY; }
        public UUID shooterOwner() { return null; }
        public boolean operational() { return true; }
        public int readyRounds() { return 4 - orders; }
        public int reactionTicks(V3 direction) { return 0; }
        public V3 launchPoint() { return launch; }
        public InterceptorEnvelope envelope(BallisticModel.Params params) { return InterceptTests.envelope(launch); }
        public double seekerFactor() { return 1; }
        public FireResult order(ServerLevel level, UUID target, EngagementSource source) { orders++; return FireResult.QUEUED; }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void autoEngageRespectsDoctrine(GameTestHelper h) {
        RadarBlockEntity radar = radar(h);
        // A radar joins the network on its first tick.
        h.runAfterDelay(2, () -> autoEngageRespectsDoctrine(h, radar));
    }

    private static void autoEngageRespectsDoctrine(GameTestHelper h, RadarBlockEntity radar) {
        radar.setSpeed((float) MissileConfig.minimumRpm);
        ServerLevel level = h.getLevel();
        FlightTracker tracker = FlightTracker.get(level);
        Flight target = coast(h, tracker, Warhead.INERT, UUID.randomUUID());
        double range = MissileConfig.radarBaseRange, earth = MissileConfig.radarEarthRadius;
        boolean enabled = DefenseConfig.enabled, own = DefenseConfig.engageOwnMissiles;
        int age = DefenseConfig.trackMinSeconds;
        Shooter shooter = new Shooter(target.flight().position().sub(new V3(100, 100, 0)));
        try {
            MissileConfig.radarBaseRange = 0;
            MissileConfig.radarEarthRadius = 0;
            DefenseConfig.enabled = true;
            DefenseConfig.engageOwnMissiles = false;
            DefenseConfig.trackMinSeconds = 0;
            RadarNetwork.refresh(level);
            var track = RadarNetwork.tracks(level).get(target.id());
            h.assertTrue(track != null, "target is tracked");
            var tracks = List.of(track);
            EngagementBoard.reserve(level, target.id(), shooter.shooterPos(), level.getGameTime());
            h.assertTrue(EngagementBoard.reserved(level, target.id()) == 0, "expired reservations do not consume a slot");
            var zone = new DefenseSettings(true, Doctrine.SHOOT_LOOK_SHOOT, (int) track.impact().x(), (int) track.impact().z(), 200);
            h.assertTrue(DefenseCommand.autoEngage(level, tracks, List.of(shooter), zone, f -> true, EngagementSource.AUTO_BATTERY) == 0,
                    "own missiles are ignored");
            h.assertTrue(DefenseCommand.autoEngage(level, tracks, List.of(shooter), zone.withZone(0, 0, 1), f -> false,
                    EngagementSource.AUTO_BATTERY) == 0, "out-of-zone missiles are ignored");
            h.assertTrue(DefenseCommand.autoEngage(level, tracks, List.of(shooter), zone, f -> false, EngagementSource.AUTO_BATTERY) == 1,
                    "shoot-look-shoot orders one round");
            h.assertTrue(DefenseCommand.autoEngage(level, tracks, List.of(shooter), zone, f -> false, EngagementSource.AUTO_TABLE) == 0,
                    "another source respects reservation");
            EngagementBoard.release(level, target.id(), shooter.shooterPos());
            shooter.orders = 0;
            h.assertTrue(DefenseCommand.autoEngage(level, tracks, List.of(shooter), zone.withDoctrine(Doctrine.SALVO_TWO), f -> false,
                    EngagementSource.AUTO_BATTERY) == 2, "salvo orders two rounds");
            h.assertTrue(EngagementBoard.reserved(level, target.id()) == 2, "two tubes at one shooter have two reservations");
        } finally {
            for (int i = 0; i < 4; i++) EngagementBoard.release(level, target.id(), shooter.shooterPos());
            tracker.remove(level, target);
            radar.setSpeed(0);
            MissileConfig.radarBaseRange = range;
            MissileConfig.radarEarthRadius = earth;
            DefenseConfig.enabled = enabled;
            DefenseConfig.engageOwnMissiles = own;
            DefenseConfig.trackMinSeconds = age;
        }
        h.succeed();
    }
}
