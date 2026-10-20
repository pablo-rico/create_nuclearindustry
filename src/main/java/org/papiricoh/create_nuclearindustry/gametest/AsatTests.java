package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.holo.*;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.silo.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AsatTests {
    private static final String BATCH = "asat";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void commandsRejectUnknownFriendlyAndGeo(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var table = HoloTableTests.table(h);
            HoloSatelliteTests.station(h, table, 0, "S3AT");
            var player = HoloSatelliteTests.operator(h, table);
            var own = HoloSatelliteTests.satellite(h, "S3AT", false, 20_000_000, 20_000_000);
            var hidden = HoloSatelliteTests.satellite(h, "S3UN", false, 20_001_000, 20_000_000);
            var geo = HoloSatelliteTests.satellite(h, "S3GE", true, 20_002_000, 20_000_000);
            var tracker = SatelliteTracker.get(h.getLevel());
            tracker.catalogue(geo.id(), "S3AT");
            try {
                h.assertTrue(HoloSatelliteTests.command(player, table, HoloCommandPayload.ENGAGE_SAT, BlockPos.ZERO, 0, 0, -1, hidden.id())
                        == HoloCommandPayload.Command.NOT_TRACKED, "uncatalogued satellite refused");
                h.assertTrue(HoloSatelliteTests.command(player, table, HoloCommandPayload.ENGAGE_SAT, BlockPos.ZERO, 0, 0, -1, own.id())
                        == HoloCommandPayload.Command.FRIENDLY, "own satellite refused");
                h.assertTrue(HoloSatelliteTests.command(player, table, HoloCommandPayload.ENGAGE_SAT, BlockPos.ZERO, 0, 0, -1, geo.id())
                        == HoloCommandPayload.Command.NO_SOLUTION, "GEO out of reach");
            } finally {
                for (var sat : List.of(own, hidden, geo)) tracker.remove(h.getLevel(), sat.id(), SatelliteTracker.Removal.DEORBITED);
            }
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void satelliteOrderLaunchesFromSilo(GameTestHelper h) { siloEngagement(h, true); }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void siloZeroPkMissesSatellite(GameTestHelper h) { siloEngagement(h, false); }

    /**
     * The whole engagement on the game's own clock, a satellite at full speed: the shot is planned on one tick and judged on
     * whichever the tracker runs, which need not line up.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "_live", timeoutTicks = 1600)
    public static void liveEngagementDestroysFastSatellite(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var builder = new SiloTestBuilder().liquid().tanks(4).stages(3);
            var silo = builder.build(h);
            h.setBlock(SiloTestBuilder.FLOOR.above(builder.height()), SiloRegistry.KILL_VEHICLE.get());
            silo.setSpeed((float) MissileConfig.minimumRpm);
            silo.validate();
            var level = h.getLevel();
            long now = level.getGameTime();
            var launch = silo.launchPoint();
            Orbit.Bounds bounds = new Orbit.Bounds(launch.x() - 30000, launch.z() - 30000, launch.x() + 30000, launch.z() + 30000);
            // Coming from the west towards the silo, passing 800 blocks north of it about now + 650 ticks: an interceptor takes hundreds of ticks to climb.
            Orbit orbit = Orbit.leo(launch.x(), launch.z() - 800, Math.PI / 2, 1600, now + 650, OrbitConfig.leoSpeed, bounds);
            Satellite sat = new Satellite(UUID.randomUUID(), "EW-LV", "S3LV", SatelliteKind.EARLY_WARNING, orbit, null, now);
            var satellites = SatelliteTracker.get(level);
            satellites.add(level, sat);
            double scale = DefenseConfig.pkScale, max = DefenseConfig.maxPk, base = OrbitConfig.satelliteBasePk;
            DefenseConfig.pkScale = 100; DefenseConfig.maxPk = 1; OrbitConfig.satelliteBasePk = 1;
            var result = silo.order(level, sat.id(), org.papiricoh.create_nuclearindustry.missile.defense.EngagementSource.MANUAL_TABLE);
            // The chance is fixed when the interceptor leaves; put the settings back once it has.
            h.runAfterDelay(DefenseConfig.siloLaunchDelayTicks + 20, () -> {
                DefenseConfig.pkScale = scale; DefenseConfig.maxPk = max; OrbitConfig.satelliteBasePk = base;
            });
            h.succeedWhen(() -> {
                h.assertTrue(result == org.papiricoh.create_nuclearindustry.missile.defense.FireResult.QUEUED, "the silo takes the order: " + result);
                h.assertTrue(satellites.get(sat.id()) == null, "the satellite is destroyed");
                h.assertTrue(FlightTracker.get(level).interceptorsToward(sat.id()) == 0, "the interceptor is gone");
            });
        });
    }

    private static void siloEngagement(GameTestHelper h, boolean kill) {
        h.runAfterDelay(1, () -> {
            var builder = new SiloTestBuilder().liquid().tanks(4).stages(3);
            var silo = builder.build(h);
            h.setBlock(SiloTestBuilder.FLOOR.above(builder.height()), SiloRegistry.KILL_VEHICLE.get());
            silo.setSpeed((float) MissileConfig.minimumRpm);
            silo.validate();
            var table = HoloTableTests.table(h);
            table.toggleLink(net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), silo.getBlockPos()));
            HoloSatelliteTests.station(h, table, 0, "S3SI");
            var player = HoloSatelliteTests.operator(h, table);
            var sat = HoloSatelliteTests.satellite(h, "S3EN", false, silo.launchPoint().x() + 100, silo.launchPoint().z());
            var satellites = SatelliteTracker.get(h.getLevel());
            satellites.catalogue(sat.id(), "S3SI");
            var flights = FlightTracker.get(h.getLevel());
            int delay = DefenseConfig.siloLaunchDelayTicks;
            boolean enabled = DefenseConfig.enabled;
            double scale = DefenseConfig.pkScale, max = DefenseConfig.maxPk, base = OrbitConfig.satelliteBasePk;
            long now = h.getLevel().getGameTime();
            List<FlightFxEvent> events = new ArrayList<>();
            FlightEvents.Listener listener = (l, f, event, stage, point, velocity) -> {
                if (f.intercept() != null && f.intercept().target().equals(sat.id())) events.add(event);
            };
            FlightEvents.addListener(listener);
            try {
                DefenseConfig.enabled = true;
                DefenseConfig.siloLaunchDelayTicks = 1;
                DefenseConfig.pkScale = kill ? 100 : 0;
                DefenseConfig.maxPk = 1;
                OrbitConfig.satelliteBasePk = 1;
                h.assertTrue(HoloSatelliteTests.command(player, table, HoloCommandPayload.ENGAGE_SAT, BlockPos.ZERO, 0, 0, 0, sat.id())
                        == HoloCommandPayload.Command.NO_SHOOTER, "battery selector cannot launch ASAT");
                h.assertTrue(HoloSatelliteTests.command(player, table, HoloCommandPayload.ENGAGE_SAT, BlockPos.ZERO, 0, 0, 100, sat.id())
                        == HoloCommandPayload.Command.OK, "linked interceptor silo accepts satellite");
                h.assertTrue(sat.id().equals(silo.pendingTarget()), "satellite id is pending target");
                silo.tick();
                h.assertTrue(flights.interceptorsToward(sat.id()) == 1, "silo countdown launches satellite interceptor");
                h.assertTrue(HoloSatelliteTests.command(player, table, HoloCommandPayload.ENGAGE_SAT, BlockPos.ZERO, 0, 0, -1, sat.id())
                        == HoloCommandPayload.Command.ALREADY_ENGAGED, "in flight interceptor prevents duplicate order");
                h.assertTrue(table.picture().orbital().satellites().stream().anyMatch(s -> s.id().equals(sat.id()) && s.pk() >= 0),
                        "picture exposes in flight Pk");
                Flight interceptor = flights.interceptors().stream().filter(f -> f.intercept().target().equals(sat.id())).findFirst().orElse(null);
                h.assertTrue(interceptor != null, "ordered interceptor exists");
                h.assertTrue(interceptor.intercept().pk() == (kill ? 1 : 0), "silo uses forced satellite Pk");
                for (int i = 1; i <= interceptor.intercept().arrivalTick(); i++) {
                    setTime(h.getLevel(), now + i);
                    flights.tick(h.getLevel());
                }
                h.assertTrue(flights.get(interceptor.id()) == null, "silo interceptor resolves at arrival");
                h.assertTrue((satellites.get(sat.id()) == null) == kill, "silo engagement survival follows Pk");
                h.assertTrue(events.contains(FlightFxEvent.INTERCEPTOR_MISS) != kill, "failed silo engagement emits miss");
            } finally {
                setTime(h.getLevel(), now);
                DefenseConfig.pkScale = scale;
                DefenseConfig.maxPk = max;
                OrbitConfig.satelliteBasePk = base;
                FlightEvents.removeListener(listener);
                DefenseConfig.siloLaunchDelayTicks = delay;
                DefenseConfig.enabled = enabled;
                silo.disarm();
                EngagementBoard.release(h.getLevel(), sat.id(), silo.getBlockPos());
                for (Flight f : List.copyOf(flights.interceptors())) if (f.intercept().target().equals(sat.id())) flights.remove(h.getLevel(), f);
                satellites.remove(h.getLevel(), sat.id(), SatelliteTracker.Removal.DEORBITED);
            }
            h.succeed();
        });
    }

    private static void resolution(GameTestHelper h, boolean kill, boolean wrap) {
        h.runAfterDelay(1, () -> {
            var level = h.getLevel();
            long now = level.getGameTime();
            var satellites = SatelliteTracker.get(level);
            double x = 20_000_000, z = 20_000_000;
            Orbit.Bounds bounds = new Orbit.Bounds(x - 1000, z - 1000, x + 2, z + 1000);
            Orbit orbit = Orbit.leo(x, z, Math.PI / 2, 1600, now, wrap ? 1 : .1, bounds);
            Satellite sat = new Satellite(UUID.randomUUID(), "CM-AS", "S3FT", SatelliteKind.COMMS, orbit, null, now - 100);
            satellites.add(level, sat);
            satellites.catalogue(sat.id(), "S3RS");
            FlightTracker tracker = new FlightTracker();
            List<FlightFxEvent> events = new ArrayList<>();
            FlightEvents.Listener listener = (l, f, event, stage, point, velocity) -> {
                if (f.intercept() != null && f.intercept().target().equals(sat.id())) events.add(event);
            };
            FlightEvents.addListener(listener);
            double scale = DefenseConfig.pkScale, max = DefenseConfig.maxPk, base = OrbitConfig.satelliteBasePk;
            try {
                DefenseConfig.pkScale = kill ? 100 : 0;
                DefenseConfig.maxPk = 1;
                OrbitConfig.satelliteBasePk = 1;
                V3 launch = orbit.position(now).sub(new V3(100, 100, 0));
                var design = InterceptorDesigns.battery(9, .5);
                var env = new InterceptorEnvelope(design, 0, .6, 2000, 800, 4000, true, 1, 20, 200);
                var solution = InterceptSolver.solve(new OrbitPath(orbit, now, 200), launch, env, MissileConfig.flightParams(), 0);
                h.assertTrue(solution.isPresent(), "nearby LEO has a solution");
                var meet = solution.get();
                double pk = KillProbability.of(new KillProbability.Inputs(OrbitConfig.satelliteBasePk, MissileFlight.Phase.MIDCOURSE,
                        1, .1, 1, 4, 1, 1, DefenseConfig.pkScale, DefenseConfig.maxPk));
                Flight interceptor = tracker.launchInterceptor(level, UUID.randomUUID(), design, Flight.vec(launch), Flight.vec(meet.ejectVelocity()), 0,
                        new Flight.Intercept(sat.id(), pk, meet.point(), meet.arrivalTicks(), BlockPos.containing(Flight.vec(launch)), ShooterType.SILO, 12),
                        null, RocketShape.LEGACY, null);
                h.assertTrue(interceptor != null && tracker.interceptorPkToward(sat.id()) == (kill ? 1 : 0), "forced Pk exposed");
                for (int i = 1; i <= meet.arrivalTicks(); i++) {
                    setTime(level, now + i);
                    tracker.tick(level);
                }
                h.assertTrue(tracker.get(interceptor.id()) == null, "arrival removes interceptor");
                h.assertTrue((satellites.get(sat.id()) == null) == kill, "satellite survival follows Pk");
                h.assertTrue(events.contains(FlightFxEvent.INTERCEPTOR_MISS) != kill, "only misses emit interceptor miss");
                h.assertTrue(!events.contains(FlightFxEvent.INTERCEPT) && !events.contains(FlightFxEvent.IMPACT), "no missile kill or ground impact");
            } finally {
                setTime(level, now);
                DefenseConfig.pkScale = scale;
                DefenseConfig.maxPk = max;
                OrbitConfig.satelliteBasePk = base;
                FlightEvents.removeListener(listener);
                for (Flight f : List.copyOf(tracker.flights())) tracker.remove(level, f);
                satellites.remove(level, sat.id(), SatelliteTracker.Removal.DEORBITED);
            }
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void onePkDestroysLeo(GameTestHelper h) { resolution(h, true, false); }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void zeroPkEmitsMissAndPreservesLeo(GameTestHelper h) { resolution(h, false, false); }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void unwrappedMeetingSurvivesBorderCrossing(GameTestHelper h) { resolution(h, true, true); }

    /** The game time the trackers see: stepped by hand so an interceptor's flight resolves within one test tick. */
    private static void setTime(net.minecraft.server.level.ServerLevel level, long time) {
        ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(time);
    }
}
