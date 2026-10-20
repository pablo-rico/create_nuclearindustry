package org.papiricoh.create_nuclearindustry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Fresh trackers and distant launch points keep the shared GameTest level's other flights out of these checks. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public final class OrbitTests {
    private static final String BATCH = "orbit";
    private static final Vec3 START = new Vec3(19_000_000, 64, 19_000_000);

    private static MissileDesign liquid() {
        var stage = PropellantModel.stage(Collections.nCopies(4, new PropellantModel.Tank(false, 1000, 1000))).design();
        return new MissileDesign(List.of(stage, stage), 1, 0);
    }

    private static Satellite satellite(String code) {
        return new Satellite(UUID.randomUUID(), "EW-test", code, SatelliteKind.EARLY_WARNING,
                Orbit.leo(START.x, START.z, .7, 1600, -100, 12, Orbit.Bounds.UNBOUNDED), UUID.randomUUID(), -100);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void launchClimbsSeparatesAndInserts(GameTestHelper h) {
        var level = h.getLevel();
        var tracker = new FlightTracker();
        var satellites = SatelliteTracker.get(level);
        Satellite ordered = satellite("BBBB");
        UUID id = UUID.randomUUID();
        List<FlightFxEvent> events = new ArrayList<>();
        long[] arrival = {-1};
        int[] explosions = {0};
        FlightEvents.Listener listener = (world, flight, event, stage, pos, vel) -> {
            if (!flight.id().equals(id)) return;
            events.add(event);
            if (event == FlightFxEvent.ORBIT_INSERTION) arrival[0] = world.getGameTime();
        };
        Consumer<ExplosionEvent.Start> explosion = event -> explosions[0]++;
        FlightEvents.addListener(listener);
        NeoForge.EVENT_BUS.addListener(explosion);
        double altitude = OrbitConfig.insertionAltitude;
        double leo = OrbitConfig.leoDeltaV, geo = OrbitConfig.geoDeltaV;
        try {
            OrbitConfig.insertionAltitude = 1200;
            OrbitConfig.leoDeltaV = 10.5;
            OrbitConfig.geoDeltaV = 12.5;
            Flight flight = tracker.launchSatellite(level, id, liquid(), START, .7f, ordered, null, RocketShape.LEGACY, null);
            h.assertTrue(flight != null, "vehicle launched");
            h.assertTrue(flight.kind() == Flight.Kind.SATELLITE && flight.warhead().equals(Warhead.INERT), "inert satellite vehicle");
            h.assertTrue(tracker.launchSatellite(level, id, liquid(), START, .7f, ordered, null, RocketShape.LEGACY, null) == flight,
                    "replay returns existing vehicle");
            tracker.tick(level);
            h.assertTrue(flight.position().y > START.y && flight.position().x == START.x && flight.position().z == START.z,
                    "first climbs straight up");
            for (int i = 0; i < 10000 && tracker.get(id) != null; i++) tracker.tick(level);
            Satellite inserted = satellites.get(ordered.id());
            h.assertTrue(tracker.get(id) == null && inserted != null, "arrival replaces vehicle with satellite");
            h.assertTrue(inserted.orbit().epoch() == arrival[0] && arrival[0] == level.getGameTime(), "orbit starts at arrival tick");
            h.assertTrue(inserted.orbit().anchorX() == ordered.orbit().anchorX()
                    && inserted.orbit().anchorZ() == ordered.orbit().anchorZ() && inserted.orbit().heading() == ordered.orbit().heading(),
                    "insertion preserves ordered ground track");
            h.assertTrue(flight.position().distanceTo(flight.aim()) < 1 && flight.target().equals(BlockPos.containing(flight.aim())),
                    "arrival is at the high aim point");
            h.assertTrue(events.containsAll(List.of(FlightFxEvent.LAUNCH, FlightFxEvent.STAGE_SEPARATION,
                    FlightFxEvent.STAGE_IGNITION, FlightFxEvent.ORBIT_INSERTION)), "launch and stage events precede insertion");
            h.assertTrue(!events.contains(FlightFxEvent.IMPACT) && !events.contains(FlightFxEvent.REENTRY)
                    && !events.contains(FlightFxEvent.TERMINAL) && !events.contains(FlightFxEvent.ABORT), "no missile impact phases");
            h.assertTrue(explosions[0] == 0 && !level.hasChunkAt(BlockPos.containing(START)), "no explosion or forced impact chunk");
            h.assertTrue(tracker.hasLaunched(id) && tracker.launchSatellite(level, id, liquid(), START, .7f, ordered, null,
                    RocketShape.LEGACY, null) == null, "completed launch cannot be replayed");
        } finally {
            FlightEvents.removeListener(listener);
            NeoForge.EVENT_BUS.unregister(explosion);
            for (Flight flight : List.copyOf(tracker.flights())) tracker.remove(level, flight);
            satellites.remove(level, ordered.id(), SatelliteTracker.Removal.DEORBITED);
            OrbitConfig.insertionAltitude = altitude;
            OrbitConfig.leoDeltaV = leo;
            OrbitConfig.geoDeltaV = geo;
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void exhaustedVehicleAbortsAtApogee(GameTestHelper h) {
        var level = h.getLevel();
        var tracker = new FlightTracker();
        Satellite ordered = satellite("BBBB");
        UUID id = UUID.randomUUID();
        List<FlightFxEvent> events = new ArrayList<>();
        FlightEvents.Listener listener = (world, flight, event, stage, pos, vel) -> {
            if (flight.id().equals(id)) events.add(event);
        };
        FlightEvents.addListener(listener);
        try {
            Flight flight = tracker.launchSatellite(level, id, new MissileDesign(List.of(), 1, 0), START, 0, ordered,
                    null, RocketShape.LEGACY, null);
            h.assertTrue(flight != null, "empty vehicle launched");
            tracker.tick(level);
            h.assertTrue(tracker.get(id) == null && SatelliteTracker.get(level).get(ordered.id()) == null,
                    "failed climb leaves no vehicle or satellite");
            h.assertTrue(events.contains(FlightFxEvent.ABORT) && !events.contains(FlightFxEvent.ORBIT_INSERTION)
                    && !events.contains(FlightFxEvent.IMPACT), "failure aborts without impact");
        } finally {
            FlightEvents.removeListener(listener);
            for (Flight flight : List.copyOf(tracker.flights())) tracker.remove(level, flight);
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void lowOrbitsDecayAndBurnUp(GameTestHelper h) {
        var level = h.getLevel();
        var tracker = new SatelliteTracker();
        Satellite low = satellite("BBBB");
        Satellite parked = new Satellite(UUID.randomUUID(), "CM-test", "BBBB", SatelliteKind.COMMS,
                Orbit.geo(0, 0, 12000, 0, Orbit.Bounds.UNBOUNDED), null, 0);
        double decay = OrbitConfig.leoDecayPerDay, minimum = OrbitConfig.leoMinAltitude;
        try {
            OrbitConfig.leoDecayPerDay = 120000;
            OrbitConfig.leoMinAltitude = 1450;
            tracker.add(level, low);
            tracker.add(level, parked);
            tracker.tick(level);
            Satellite lower = tracker.get(low.id());
            h.assertTrue(lower != null, "first decay step stays above minimum");
            h.assertTrue(lower.orbit().altitude() == 1500, "one second loses daily decay divided by 1200");
            tracker.tick(level);
            h.assertTrue(tracker.get(low.id()) == null && tracker.count("BBBB") == 1, "next step burns up below minimum");
            h.assertTrue(tracker.get(parked.id()).orbit().altitude() == 12000, "geostationary orbit never decays");
        } finally {
            OrbitConfig.leoDecayPerDay = decay;
            OrbitConfig.leoMinAltitude = minimum;
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void radarCataloguesForeignSatellite(GameTestHelper h) {
        var level = h.getLevel();
        var tracker = new SatelliteTracker();
        Satellite foreign = satellite("BBBB");
        h.setBlock(new BlockPos(3, 1, 3), RadarRegistry.RADAR.get());
        RadarBlockEntity radar = h.getBlockEntity(new BlockPos(3, 1, 3));
        h.assertTrue(radar != null, "radar exists");
        radar.initialize();
        radar.setCode("AAAA");
        double base = MissileConfig.radarBaseRange, earth = MissileConfig.radarEarthRadius;
        try {
            MissileConfig.radarBaseRange = 0;
            MissileConfig.radarEarthRadius = 0;
            radar.setSpeed((float) MissileConfig.minimumRpm);
            tracker.add(level, foreign);
            h.assertTrue(tracker.knownTo("AAAA").isEmpty(), "foreign satellite initially unknown");
            tracker.tick(level);
            h.assertTrue(radar.operational() && tracker.knownTo("AAAA").stream().anyMatch(s -> s.id().equals(foreign.id())),
                    "working omniscient radar catalogues foreign satellite");
            radar.setSpeed(0);
            tracker.tick(level);
            h.assertTrue(tracker.known(foreign.id(), "AAAA") && tracker.knownTo("CCCC").isEmpty(), "catalogue persists only for seeing network");
        } finally {
            radar.setSpeed(0);
            MissileConfig.radarBaseRange = base;
            MissileConfig.radarEarthRadius = earth;
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void catalogueAndPayloadRoundTrip(GameTestHelper h) {
        var level = h.getLevel();
        var tracker = new SatelliteTracker();
        Satellite satellite = satellite("BBBB");
        tracker.add(level, satellite);
        tracker.catalogue(satellite.id(), "AAAA");
        var loaded = SatelliteTracker.load(tracker.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        h.assertTrue(satellite.equals(loaded.get(satellite.id())) && loaded.known(satellite.id(), "AAAA")
                && !loaded.known(satellite.id(), "CCCC") && loaded.count("BBBB") == 1, "satellite and catalogue saved");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            OrbitSyncPayload.CODEC.encode(buffer, new OrbitSyncPayload(List.of(satellite)));
            var decoded = OrbitSyncPayload.CODEC.decode(buffer);
            h.assertTrue(decoded.satellites().size() == 1, "payload has one satellite");
            Satellite back = decoded.satellites().getFirst();
            h.assertTrue(back.id().equals(satellite.id()) && back.code().equals("BBBB") && back.designation().equals(satellite.designation())
                    && back.kind() == satellite.kind() && back.owner() == null && back.launchedAt() == satellite.launchedAt(), "public satellite fields round trip");
            h.assertTrue(back.orbit().type() == satellite.orbit().type() && back.orbit().epoch() == satellite.orbit().epoch()
                    && back.orbit().position(0).distance(satellite.orbit().position(0)) < .01, "orbit round trips within float precision");
            h.assertTrue(!buffer.isReadable(), "payload consumed exactly");
            buffer.clear();
            OrbitSyncPayload.CODEC.encode(buffer, new OrbitSyncPayload(List.of()));
            h.assertTrue(OrbitSyncPayload.CODEC.decode(buffer).satellites().isEmpty() && !buffer.isReadable(), "empty level payload round trips");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void launchVehicleAndSatelliteSurviveReload(GameTestHelper h) {
        var level = h.getLevel();
        var tracker = new FlightTracker();
        Satellite satellite = satellite("BBBB");
        UUID id = UUID.randomUUID();
        Flight flight = tracker.launchSatellite(level, id, liquid(), START, .7f, satellite, satellite.owner(), RocketShape.LEGACY, null);
        h.assertTrue(flight != null, "vehicle launched");
        for (int i = 0; i < 50; i++) tracker.tick(level);
        var loaded = FlightTracker.load(tracker.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        Flight back = loaded.get(id);
        h.assertTrue(back != null && back.kind() == Flight.Kind.SATELLITE, "satellite flight restored");
        h.assertTrue(satellite.equals(back.satellite()) && back.owner().equals(satellite.owner()) && loaded.hasLaunched(id), "payload and receipt restored");
        h.assertTrue(back.flight().timed() && back.flight().arrivalTick() == flight.flight().arrivalTick()
                && back.flight().rise() == flight.flight().rise() && back.position().equals(flight.position()), "climb timing and position restored");
        try {
            for (int i = 0; i < 10000 && loaded.get(id) != null; i++) loaded.tick(level);
            h.assertTrue(loaded.get(id) == null && SatelliteTracker.get(level).get(satellite.id()) != null, "restored climb inserts satellite");
        } finally {
            for (Flight active : List.copyOf(loaded.flights())) loaded.remove(level, active);
            for (Flight active : List.copyOf(tracker.flights())) tracker.remove(level, active);
            SatelliteTracker.get(level).remove(level, satellite.id(), SatelliteTracker.Removal.DEORBITED);
        }
        h.succeed();
    }
}
