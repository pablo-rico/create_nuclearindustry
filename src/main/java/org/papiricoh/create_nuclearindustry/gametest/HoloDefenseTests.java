package org.papiricoh.create_nuclearindustry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryPartBlock;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.holo.*;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalDevice;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalLink;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalLinkerItem;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.*;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.List;
import java.util.UUID;

/** The defense picture and commands use the table's links even when a battery has no network code. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public final class HoloDefenseTests {
    private static final String BATCH = "holo_defense";
    private static final BlockPos BATTERY = new BlockPos(7, 1, 4);
    private static final BlockPos RADAR = new BlockPos(12, 1, 10);

    private static InterceptorBatteryEntity battery(GameTestHelper h, BlockPos relative) {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(relative);
        level.setBlock(pos, DefenseRegistry.BATTERY.get().defaultBlockState(), 2);
        for (int i = 1; i < 8; i++) level.setBlock(pos.offset(i & 1, (i >> 1) & 1, (i >> 2) & 1),
                DefenseRegistry.BATTERY_PART.get().defaultBlockState().setValue(InterceptorBatteryPartBlock.OFFSET, i), 2);
        h.assertTrue(level.getBlockEntity(pos) instanceof InterceptorBatteryEntity, "battery master exists");
        return (InterceptorBatteryEntity) level.getBlockEntity(pos);
    }

    private static void link(GameTestHelper h, HoloTableBlockEntity table, BlockPos pos) {
        h.assertTrue(table.toggleLink(GlobalPos.of(h.getLevel().dimension(), pos)) == TacticalDevice.Result.ADDED, "link accepted");
    }

    private static FakePlayer operator(GameTestHelper h, HoloTableBlockEntity table) {
        FakePlayer player = HoloTableTests.player(h, HoloTableTests.CENTRE);
        player.containerMenu = new HoloTableMenu(1, player.getInventory(), table.getBlockPos());
        return player;
    }

    private static HoloCommandPayload.Command command(FakePlayer player, HoloTableBlockEntity table, byte action, int x, int z, int arg, UUID flight) {
        return HoloCommandPayload.execute(player, new HoloCommandPayload(table.getBlockPos(), action, BlockPos.ZERO, x, z, arg, flight, List.of()));
    }

    private static Flight coast(GameTestHelper h, HoloTableBlockEntity table, UUID owner) {
        Vec3 ground = table.getBlockPos().getCenter();
        Flight flight = FlightTracker.get(h.getLevel()).launch(h.getLevel(), new MissileDesign(List.of(), .4, 0),
                ground.add(100, 150, 0), new Vec3(-.4, -.1, 0), ground, BlockPos.containing(ground), Warhead.INERT, owner);
        flight.flight().step(MissileConfig.flightParams());
        h.assertTrue(flight.flight().burnedOut(), "target is coasting");
        return flight;
    }

    private static RadarBlockEntity radar(GameTestHelper h, HoloTableBlockEntity table) {
        h.setBlock(RADAR, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = h.getBlockEntity(RADAR);
        h.assertTrue(radar != null, "radar exists");
        link(h, table, radar.getBlockPos());
        return radar;
    }

    private static void refresh(GameTestHelper h, RadarBlockEntity radar) {
        double base = MissileConfig.radarBaseRange, earth = MissileConfig.radarEarthRadius;
        try {
            MissileConfig.radarBaseRange = 0;
            MissileConfig.radarEarthRadius = 0;
            radar.setSpeed((float) MissileConfig.minimumRpm);
            RadarNetwork.refresh(h.getLevel());
            radar.refresh();
        } finally {
            MissileConfig.radarBaseRange = base;
            MissileConfig.radarEarthRadius = earth;
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void linkerResolvesBatteryPartsAndNinthIsFull(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var player = operator(h, table);
        ItemStack linker = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        for (int i = 0; i < 9; i++) {
            var battery = battery(h, new BlockPos(5 + (i % 3) * 3, 1, 1 + (i / 3) * 3));
            BlockPos part = i == 0 ? battery.getBlockPos() : battery.getBlockPos().offset(1, 1, 1);
            linker.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(part.getCenter(), Direction.UP, part, false)));
            TacticalLink stored = TacticalLinkerItem.getLink(linker);
            h.assertTrue(stored != null, "linker remembers battery");
            h.assertTrue(stored.kind() == TacticalLink.Kind.INTERCEPTOR_BATTERY && stored.target().pos().equals(battery.getBlockPos()), "part resolves to master");
            if (i < 8) {
                BlockPos centre = table.getBlockPos();
                linker.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(centre.getCenter(), Direction.UP, centre, false)));
            } else h.assertTrue(table.toggleLink(stored.target()) == TacticalDevice.Result.FULL, "ninth battery is refused");
        }
        h.assertTrue(table.batteries().size() == 8 && table.links().size() == 8, "eight linked batteries");
        var saved = table.saveWithoutMetadata(h.getLevel().registryAccess());
        table.clearLinks();
        table.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(table.batteries().size() == 8, "battery links persist");
        var first = table.batteries().getFirst();
        h.assertTrue(table.unlink(first) && table.batteries().size() == 7, "individual battery unlinks");
        table.clearLinks();
        h.assertTrue(table.links().isEmpty(), "clear includes batteries");
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void settingsCommandsPersistAndClamp(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var player = operator(h, table);
        h.assertTrue(command(player, table, HoloCommandPayload.SET_AUTO, 0, 0, 1, null) == HoloCommandPayload.Command.OK, "auto accepted without a silo");
        h.assertTrue(command(player, table, HoloCommandPayload.SET_DOCTRINE, 0, 0, 1, null) == HoloCommandPayload.Command.OK, "doctrine accepted");
        h.assertTrue(command(player, table, HoloCommandPayload.SET_ZONE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, null) == HoloCommandPayload.Command.OK, "zone accepted");
        var expected = table.defense();
        h.assertTrue(expected.zoneX() == table.getBlockPos().getX() + RadarConfigurePayload.MAX_ZONE_OFFSET
                && expected.zoneZ() == table.getBlockPos().getZ() - RadarConfigurePayload.MAX_ZONE_OFFSET
                && expected.zoneRadius() == RadarBlockEntity.MAX_ZONE, "zone clamped");
        var saved = table.saveWithoutMetadata(h.getLevel().registryAccess());
        table.setDefense(DefenseSettings.defaults(table.getBlockPos()));
        table.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(table.defense().equals(expected) && table.defense().auto() && table.defense().doctrine() == Doctrine.SALVO_TWO, "settings persist");
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void pictureAndDefensePayloadRoundTrip(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var battery = battery(h, BATTERY);
        link(h, table, battery.getBlockPos());
        Flight target = coast(h, table, null);
        var tracker = FlightTracker.get(h.getLevel());
        var start = battery.launchPoint();
        Flight interceptor = tracker.launchInterceptor(h.getLevel(), UUID.randomUUID(), InterceptorDesigns.battery(9, .5),
                Flight.vec(start), new Vec3(0, 1, 0), 0,
                new Flight.Intercept(target.id(), .72, start.add(new org.papiricoh.create_nuclearindustry.missile.physics.V3(10, 40, 0)), 80,
                        battery.getBlockPos(), ShooterType.BATTERY, 1), null, InterceptorDesigns.BATTERY_SHAPE, null);
        h.assertTrue(interceptor != null, "interceptor launched");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            var picture = table.picture();
            h.assertTrue(picture.shooters().size() == 1 && picture.shooters().getFirst().capacity() == 4, "picture includes battery");
            h.assertTrue(picture.intercepts().stream().anyMatch(i -> i.interceptor().equals(interceptor.id()) && i.target().equals(target.id())), "own interceptor visible without a radar");
            HoloTracksPayload.CODEC.encode(buffer, picture);
            var back = HoloTracksPayload.CODEC.decode(buffer);
            h.assertTrue(back.shooters().equals(picture.shooters()) && back.intercepts().equals(picture.intercepts()) && back.defense().equals(picture.defense()), "new lists round trip");
            h.assertTrue(!buffer.isReadable(), "payload consumed exactly");
            var player = operator(h, table);
            h.assertTrue(command(player, table, HoloCommandPayload.ABORT, 0, 0, 0, interceptor.id()) == HoloCommandPayload.Command.OK, "own interceptor aborts");
        } finally {
            buffer.release();
            tracker.remove(h.getLevel(), interceptor);
            tracker.remove(h.getLevel(), target);
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void manualRequiresTrackAndRejectsFriendly(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var player = operator(h, table);
        var battery = battery(h, BATTERY);
        var tag = battery.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putUUID("Owner", player.getUUID());
        battery.loadWithComponents(tag, h.getLevel().registryAccess());
        link(h, table, battery.getBlockPos());
        Flight target = coast(h, table, player.getUUID());
        h.assertTrue(command(player, table, HoloCommandPayload.ENGAGE, 0, 0, -1, target.id()) == HoloCommandPayload.Command.NOT_TRACKED, "no radar means not tracked");
        var radar = radar(h, table);
        h.runAfterDelay(3, () -> {
            try {
                refresh(h, radar);
                h.assertTrue(table.tracks().stream().anyMatch(t -> t.id().equals(target.id())), "linked radar tracks own missile");
                h.assertTrue(command(player, table, HoloCommandPayload.ENGAGE, 0, 0, -1, target.id()) == HoloCommandPayload.Command.FRIENDLY, "friendly refused before shooter check");
            } finally { FlightTracker.get(h.getLevel()).remove(h.getLevel(), target); }
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void ceaseReleasesEveryLinkedReservation(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var battery = battery(h, BATTERY);
        link(h, table, battery.getBlockPos());
        Flight target = coast(h, table, null);
        BlockPos foreign = battery.getBlockPos().east(30);
        long expiry = h.getLevel().getGameTime() + 100;
        EngagementBoard.reserve(h.getLevel(), target.id(), battery.getBlockPos(), expiry);
        EngagementBoard.reserve(h.getLevel(), target.id(), battery.getBlockPos(), expiry);
        EngagementBoard.reserve(h.getLevel(), target.id(), foreign, expiry);
        try {
            table.setDefense(table.defense().withAuto(true));
            h.assertTrue(command(operator(h, table), table, HoloCommandPayload.CEASE, 0, 0, 0, null) == HoloCommandPayload.Command.OK, "cease accepted");
            h.assertTrue(!table.defense().auto(), "automatic disabled");
            h.assertTrue(EngagementBoard.reserved(h.getLevel(), target.id()) == 1, "both linked reservations released and foreign one retained");
        } finally {
            EngagementBoard.release(h.getLevel(), target.id(), foreign);
            EngagementBoard.release(h.getLevel(), target.id(), battery.getBlockPos());
            EngagementBoard.release(h.getLevel(), target.id(), battery.getBlockPos());
            FlightTracker.get(h.getLevel()).remove(h.getLevel(), target);
        }
        h.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void manualEngageOrdersBattery(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var player = operator(h, table);
        var battery = battery(h, BATTERY);
        link(h, table, battery.getBlockPos());
        var radar = radar(h, table);
        Flight target = coast(h, table, null);
        h.runAfterDelay(3, () -> {
            try {
                refresh(h, radar);
                prepare(h, battery);
                h.assertTrue(table.tracks().stream().anyMatch(t -> t.id().equals(target.id())), "target tracked");
                h.assertTrue(command(player, table, HoloCommandPayload.ENGAGE, 0, 0, 0, target.id()) == HoloCommandPayload.Command.OK, "manual creates an order");
                h.assertTrue(EngagementBoard.reserved(h.getLevel(), target.id()) > 0 || FlightTracker.get(h.getLevel()).interceptorsToward(target.id()) > 0, "order or flight exists");
                EngagementBoard.reserve(h.getLevel(), target.id(), battery.getBlockPos(), h.getLevel().getGameTime() + 100);
                command(player, table, HoloCommandPayload.CEASE, 0, 0, 0, null);
                h.assertTrue(EngagementBoard.reserved(h.getLevel(), target.id()) == 0, "cease releases reservations");
            } finally { clean(h, target); }
            h.succeed();
        });
    }

    private static void prepare(GameTestHelper h, InterceptorBatteryEntity battery) {
        battery.tubes().insertItem(0, new ItemStack(DefenseRegistry.INTERCEPTOR.get()), false);
        var tag = battery.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putIntArray("Loading", new int[4]);
        battery.loadWithComponents(tag, h.getLevel().registryAccess());
        battery.setSpeed(256);
        h.assertTrue(battery.readyRounds() > 0 && battery.operational(), "loaded powered battery ready");
    }

    private static void clean(GameTestHelper h, Flight target) {
        var tracker = FlightTracker.get(h.getLevel());
        for (Flight flight : List.copyOf(tracker.interceptors())) if (flight.intercept() != null && flight.intercept().target().equals(target.id())) tracker.remove(h.getLevel(), flight);
        tracker.remove(h.getLevel(), target);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void automaticTableFiresBatteryWithoutCode(GameTestHelper h) {
        var table = HoloTableTests.table(h);
        var battery = battery(h, BATTERY);
        link(h, table, battery.getBlockPos());
        var radar = radar(h, table);
        Flight target = coast(h, table, null);
        h.runAfterDelay(3, () -> {
            refresh(h, radar);
            prepare(h, battery);
            h.assertTrue(battery.code().isEmpty() && !battery.settings().auto(), "battery has no independent automatic source");
            table.setDefense(table.defense().withAuto(true));
        });
        h.succeedWhen(() -> {
            if (h.getLevel().getGameTime() < target.launchedAt() + 4) h.assertTrue(false, "waiting for radar registration");
            refresh(h, radar);
            battery.setSpeed(256);
            h.assertTrue(FlightTracker.get(h.getLevel()).interceptorsToward(target.id()) > 0, "table launches interceptor");
            table.cease();
            clean(h, target);
        });
    }
}
