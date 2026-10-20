package org.papiricoh.create_nuclearindustry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.holo.*;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalDevice;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.silo.*;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class HoloSatelliteTests {
    private static final String BATCH = "holo_satellite";

    static GroundStationBlockEntity station(GameTestHelper h, HoloTableBlockEntity table, int index, String code) {
        BlockPos pos = h.absolutePos(new BlockPos(11 + index, 1, 11));
        h.getLevel().setBlock(pos, OrbitRegistry.GROUND_STATION.get().defaultBlockState(), 2);
        GroundStationBlockEntity station = (GroundStationBlockEntity) h.getLevel().getBlockEntity(pos);
        station.setCode(code);
        station.setSpeed((float) MissileConfig.minimumRpm);
        table.toggleLink(GlobalPos.of(h.getLevel().dimension(), pos));
        return station;
    }

    static FakePlayer operator(GameTestHelper h, HoloTableBlockEntity table) {
        var player = HoloTableTests.player(h, HoloTableTests.CENTRE);
        player.containerMenu = new HoloTableMenu(1, player.getInventory(), table.getBlockPos());
        return player;
    }

    static HoloCommandPayload.Command command(FakePlayer player, HoloTableBlockEntity table, byte action,
                                              BlockPos silo, int x, int z, int arg, UUID id) {
        return HoloCommandPayload.execute(player, new HoloCommandPayload(table.getBlockPos(), action, silo, x, z, arg, id, List.of()));
    }

    static Satellite satellite(GameTestHelper h, String code, boolean geo, double x, double z) {
        long now = h.getLevel().getGameTime();
        Orbit.Bounds bounds = new Orbit.Bounds(x - 10000, z - 10000, x + 10000, z + 10000);
        Orbit orbit = geo ? Orbit.geo(x, z, 8000, now, bounds) : Orbit.leo(x, z, 0, 1600, now, .1, bounds);
        Satellite sat = new Satellite(UUID.randomUUID(), "CM-S3", code, SatelliteKind.COMMS, orbit, null, now);
        SatelliteTracker.get(h.getLevel()).add(h.getLevel(), sat);
        return sat;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void stationsPersistAndSupplyNetwork(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var table = HoloTableTests.table(h);
            var silo = new SiloTestBuilder().tanks(2).build(h);
            table.toggleLink(GlobalPos.of(h.getLevel().dimension(), silo.getBlockPos()));
            var first = station(h, table, 0, "S3A1");
            var second = station(h, table, 1, "S3A2");
            var third = station(h, table, 2, "S3A3");
            h.assertTrue(table.stations().size() == 2, "only two stations link");
            h.assertTrue(table.toggleLink(GlobalPos.of(h.getLevel().dimension(), third.getBlockPos())) == TacticalDevice.Result.FULL, "third refused");
            h.assertTrue(table.network().equals("S3A1"), "first operational station supplies code");
            first.setSpeed(0);
            table.picture();
            h.assertTrue(table.network().equals("S3A2") && silo.network().equals("S3A2"), "fallback code reaches silo");
            var saved = table.saveWithoutMetadata(h.getLevel().registryAccess());
            table.clearLinks();
            table.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(table.stations().size() == 2, "stations saved");
            var player = operator(h, table);
            h.assertTrue(command(player, table, HoloCommandPayload.UNLINK_STATION, second.getBlockPos(), 0, 0, 0, null)
                    == HoloCommandPayload.Command.OK, "station unlink command");
            table.picture();
            h.assertTrue(table.network().isEmpty() && silo.network().isEmpty(), "no operational station clears silo code");
            h.assertTrue(table.toggleLink(GlobalPos.of(h.getLevel().dimension(), first.getBlockPos())) == TacticalDevice.Result.REMOVED,
                    "toggle removes station");
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void pictureCatalogueAndOrbitalRoundTrip(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var table = HoloTableTests.table(h);
            station(h, table, 0, "S3PC");
            var tracker = SatelliteTracker.get(h.getLevel());
            var own = satellite(h, "S3PC", false, 20_000_000, 20_000_000);
            var known = satellite(h, "S3FK", false, 20_001_000, 20_000_000);
            var hidden = satellite(h, "S3FH", false, 20_002_000, 20_000_000);
            tracker.catalogue(known.id(), "S3PC");
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                var picture = table.picture();
                var sats = picture.orbital().satellites();
                h.assertTrue(sats.size() == 2 && sats.getFirst().id().equals(own.id()) && sats.getFirst().own(), "own satellites first, hidden foreign excluded");
                h.assertTrue(sats.stream().anyMatch(s -> s.id().equals(known.id()) && !s.own()), "catalogued foreign visible");
                h.assertTrue(picture.orbital().stations().size() == 1 && picture.orbital().code().equals("S3PC"), "station picture");
                HoloTracksPayload.CODEC.encode(buffer, picture);
                var back = HoloTracksPayload.CODEC.decode(buffer);
                h.assertTrue(back.orbital().equals(picture.orbital()) && !buffer.isReadable(), "orbital payload round trip");
                var player = operator(h, table);
                h.assertTrue(command(player, table, HoloCommandPayload.DEORBIT, BlockPos.ZERO, 0, 0, 0, known.id())
                        == HoloCommandPayload.Command.NOT_OWN, "foreign cannot deorbit");
                h.assertTrue(command(player, table, HoloCommandPayload.DEORBIT, BlockPos.ZERO, 0, 0, 0, own.id())
                        == HoloCommandPayload.Command.OK && tracker.get(own.id()) == null, "own deorbits");
            } finally {
                buffer.release();
                for (var sat : List.of(own, known, hidden)) tracker.remove(h.getLevel(), sat.id(), SatelliteTracker.Removal.DEORBITED);
            }
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void communicationsExtendLinkerAndCommandRange(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var table = HoloTableTests.table(h);
            station(h, table, 0, "S3RL");
            var silo = new SiloTestBuilder().tanks(2).build(h);
            var player = operator(h, table);
            var relay = satellite(h, "S3RL", true, table.getBlockPos().getX(), table.getBlockPos().getZ());
            var tracker = SatelliteTracker.get(h.getLevel());
            double range = MissileConfig.tacticalLinkRange;
            try {
                MissileConfig.tacticalLinkRange = 1;
                var link = GlobalPos.of(h.getLevel().dimension(), silo.getBlockPos());
                h.assertTrue(table.inLinkRange(silo.getBlockPos()), "communications relay extends range");
                var stack = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
                stack.set(org.papiricoh.create_nuclearindustry.AllNuclearDataComponents.TACTICAL_LINK.get(),
                        new org.papiricoh.create_nuclearindustry.missile.link.TacticalLink(link,
                                org.papiricoh.create_nuclearindustry.missile.link.TacticalLink.Kind.LAUNCH_CONTROLLER));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                BlockPos at = table.getBlockPos();
                stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(at.getCenter(), net.minecraft.core.Direction.UP, at, false)));
                h.assertTrue(table.silos().contains(link), "linker accepts relay beyond normal range");
                h.assertTrue(command(player, table, HoloCommandPayload.SELECT, silo.getBlockPos(), 0, 0, 0, null)
                        == HoloCommandPayload.Command.OK, "relay allows controller command");
                tracker.remove(h.getLevel(), relay.id(), SatelliteTracker.Removal.DEORBITED);
                h.assertTrue(!table.inLinkRange(silo.getBlockPos()), "range extension ends without coverage");
            } finally {
                MissileConfig.tacticalLinkRange = range;
                tracker.remove(h.getLevel(), relay.id(), SatelliteTracker.Removal.DEORBITED);
            }
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void earlyWarningTracksReachTableWithoutLinkedRadar(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var table = HoloTableTests.table(h);
            station(h, table, 0, "S3EW");
            long now = h.getLevel().getGameTime();
            var orbit = Orbit.geo(20_000_000, 20_000_000, 1600, now, Orbit.Bounds.UNBOUNDED);
            var sat = new Satellite(UUID.randomUUID(), "EW-S3", "S3EW", SatelliteKind.EARLY_WARNING, orbit, null, now);
            var satellites = SatelliteTracker.get(h.getLevel());
            satellites.add(h.getLevel(), sat);
            var flights = org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker.get(h.getLevel());
            var start = new net.minecraft.world.phys.Vec3(20_000_000, 1000, 20_000_000);
            var flight = flights.launch(h.getLevel(), org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign.LEGACY_PAD,
                    start, net.minecraft.world.phys.Vec3.ZERO, start.add(1000, -937, 0), BlockPos.containing(start),
                    org.papiricoh.create_nuclearindustry.missile.flight.Warhead.INERT, null);
            try {
                org.papiricoh.create_nuclearindustry.missile.radar.RadarNetwork.refresh(h.getLevel());
                h.assertTrue(table.tracks().stream().filter(t -> t.id().equals(flight.id())).count() == 1, "network early warning merged once");
                h.assertTrue(table.picture().tracks().stream().anyMatch(t -> t.id().equals(flight.id())), "network track in picture");
            } finally {
                flights.remove(h.getLevel(), flight);
                satellites.remove(h.getLevel(), sat.id(), SatelliteTracker.Removal.DEORBITED);
            }
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void launchCommandsValidateAndCountDown(GameTestHelper h) {
        h.runAfterDelay(1, () -> {
            var builder = new SiloTestBuilder().liquid().tanks(4).stages(2);
            var silo = builder.build(h);
            BlockPos payload = SiloTestBuilder.FLOOR.above(builder.height());
            h.setBlock(payload, SiloRegistry.SATELLITE_FAIRING.get());
            RocketSectionEntity fairing = h.getBlockEntity(payload);
            fairing.contents().insertItem(0, new ItemStack(OrbitRegistry.COMMS_SATELLITE.get()), false);
            silo.setSpeed((float) MissileConfig.minimumRpm);
            silo.validate();
            var table = HoloTableTests.table(h);
            table.toggleLink(GlobalPos.of(h.getLevel().dimension(), silo.getBlockPos()));
            var player = operator(h, table);
            var pos = silo.getBlockPos();
            var bounds = OrbitConfig.bounds(h.getLevel().getWorldBorder());
            int x = (int) ((bounds.minX() + bounds.maxX()) / 2), z = (int) ((bounds.minZ() + bounds.maxZ()) / 2);
            try {
                h.assertTrue(command(player, table, HoloCommandPayload.LAUNCH_SAT, pos, x, z, 0, null)
                        == HoloCommandPayload.Command.NO_STATION, "launch requires station");
                h.assertTrue(table.picture().orbital().launchers().size() == 1, "launcher visible without station");
                station(h, table, 0, "S3LA");
                h.assertTrue(command(player, table, HoloCommandPayload.LAUNCH_SAT, pos, (int) bounds.maxX() + 1, z, 0, null)
                        == HoloCommandPayload.Command.OUT_OF_BOUNDS, "outside orbit bounds refused");
                h.assertTrue(command(player, table, HoloCommandPayload.LAUNCH_SAT, pos, x, z, 1, null)
                        == HoloCommandPayload.Command.NO_ORBIT, "two stages cannot reach GEO");
                h.assertTrue(command(player, table, HoloCommandPayload.LAUNCH_SAT, pos, x, z, 0, null)
                        == HoloCommandPayload.Command.OK, "LEO accepted");
                h.assertTrue(silo.state() == SiloControllerEntity.State.COUNTDOWN && silo.countdown() >= 0, "satellite counts down");
            } finally { silo.disarm(); }
            h.succeed();
        });
    }
}
