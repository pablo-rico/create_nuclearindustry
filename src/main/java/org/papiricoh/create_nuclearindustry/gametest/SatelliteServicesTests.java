package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.radar.*;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SatelliteServicesTests {
    private static final String BATCH = "satellite_services";
    private static Satellite satellite(GameTestHelper h, String code, SatelliteKind kind, double x, double z) {
        var level = h.getLevel();
        var satellite = new Satellite(UUID.randomUUID(), "TEST", code, kind,
                Orbit.geo(x, z, 10000, level.getGameTime(), Orbit.Bounds.UNBOUNDED), null, level.getGameTime());
        SatelliteTracker.get(level).add(level, satellite);
        return satellite;
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void earlyWarningIsCodeSpecificAndEndsAtBurnout(GameTestHelper h) {
        var level = h.getLevel();
        Vec3 start = new Vec3(-19_000_000, 1000, -19_000_000);
        var satellite = satellite(h, "S2EW", SatelliteKind.EARLY_WARNING, start.x, start.z);
        // A single brief burn: measured before and after stepping to burnout, in the same server tick.
        var design = new MissileDesign(List.of(new MissileDesign.Stage(1, .01, .1, 3)), 1, 0);
        var flight = FlightTracker.get(level).launch(level, design, start, Vec3.ZERO, start.add(10000, -1000, 0), BlockPos.containing(start), Warhead.INERT, null);
        boolean own, other, ended;
        int watchers;
        try {
            RadarNetwork.refresh(level);
            own = RadarNetwork.networkTracks(level, "S2EW", BlockPos.ZERO).stream().anyMatch(t -> t.id().equals(flight.id()));
            other = RadarNetwork.networkTracks(level, "S2XX", BlockPos.ZERO).stream().anyMatch(t -> t.id().equals(flight.id()));
            watchers = RadarNetwork.watchers(level, flight.id());
            for (int i = 0; i < 10; i++) flight.flight().step(MissileConfig.flightParams());
            RadarNetwork.refresh(level);
            ended = RadarNetwork.networkTracks(level, "S2EW", BlockPos.ZERO).stream().noneMatch(t -> t.id().equals(flight.id()));
        } finally {
            FlightTracker.get(level).remove(level, flight);
            SatelliteTracker.get(level).remove(level, satellite.id(), SatelliteTracker.Removal.DEORBITED);
            RadarNetwork.refresh(level);
        }
        h.succeedWhen(() -> {
            h.assertTrue(own && !other, "early warning sees boost on its own code regardless of distance");
            h.assertTrue(watchers == 1, "satellite network counts once as a watcher");
            h.assertTrue(ended, "satellite detection stops after burnout");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void communicationsRelayConnectsFarPeers(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 2, 2), RadarRegistry.RADAR.get());
        h.setBlock(new BlockPos(12, 2, 12), RadarRegistry.RADAR.get());
        RadarBlockEntity a = h.getBlockEntity(new BlockPos(2, 2, 2));
        RadarBlockEntity b = h.getBlockEntity(new BlockPos(12, 2, 12));
        a.setCode("S2CM"); b.setCode("S2CM");
        // Radars join the network on their first tick.
        h.runAfterDelay(2, () -> {
            a.setSpeed((float) MissileConfig.minimumRpm); b.setSpeed((float) MissileConfig.minimumRpm);
            double oldRange = MissileConfig.radarNetworkRange;
            double oldBase = MissileConfig.radarBaseRange;
            double oldEarth = MissileConfig.radarEarthRadius;
            var level = h.getLevel();
            Vec3 start = Vec3.atCenterOf(b.getBlockPos()).add(0, 3, 0);
            var flight = FlightTracker.get(level).launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO,
                    start.add(1000, -100, 0), BlockPos.containing(start), Warhead.INERT, null);
            Satellite sat = null;
            boolean before, after, foreign, relayed;
            try {
                MissileConfig.radarNetworkRange = 1;
                MissileConfig.radarBaseRange = 5;
                MissileConfig.radarEarthRadius = 0;
                before = RadarNetwork.peers(a, b);
                sat = satellite(h, "S2CM", SatelliteKind.COMMS, a.getBlockPos().getX(), a.getBlockPos().getZ());
                after = RadarNetwork.peers(a, b);
                RadarNetwork.refresh(level);
                relayed = RadarNetwork.networkTracks(level, "S2CM", a.getBlockPos()).stream().anyMatch(t -> t.id().equals(flight.id()));
                b.setCode("S2OT");
                foreign = RadarNetwork.peers(a, b);
            } finally {
                MissileConfig.radarNetworkRange = oldRange; MissileConfig.radarBaseRange = oldBase; MissileConfig.radarEarthRadius = oldEarth;
                FlightTracker.get(level).remove(level, flight);
                if (sat != null) SatelliteTracker.get(level).remove(level, sat.id(), SatelliteTracker.Removal.DEORBITED);
                RadarNetwork.refresh(level);
            }
            boolean b1 = before, a1 = after, f1 = foreign, r1 = relayed;
            h.succeedWhen(() -> {
                h.assertTrue(!b1 && a1 && !f1, "relay connects distant peers only on its code");
                h.assertTrue(r1, "network picture includes remote radar through relay");
            });
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void communicationsGpsReducesControllerCep(GameTestHelper h) {
        var controller = new SiloTestBuilder().build(h);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos target = controller.floor().east(1000);
        controller.configure(player, target, false);
        controller.setSpeed((float) MissileConfig.minimumRpm);
        controller.validate();
        controller.setNetwork("S2GP");
        double before = controller.cep();
        double oldFactor = OrbitConfig.gpsCepFactor;
        double after;
        boolean guided, other;
        Satellite sat = satellite(h, "S2GP", SatelliteKind.COMMS, controller.floor().getX(), controller.floor().getZ());
        try {
            OrbitConfig.gpsCepFactor = .25;
            after = controller.cep(); guided = controller.gpsGuided();
            controller.setNetwork("S2NO"); other = controller.gpsGuided();
        } finally {
            OrbitConfig.gpsCepFactor = oldFactor;
            SatelliteTracker.get(h.getLevel()).remove(h.getLevel(), sat.id(), SatelliteTracker.Removal.DEORBITED);
        }
        h.succeedWhen(() -> {
            h.assertTrue(before > 0 && Math.abs(after - before * .25) < 1e-8, "comms reduces controller CEP");
            h.assertTrue(guided && !other, "GPS applies only to its network");
        });
    }
}
