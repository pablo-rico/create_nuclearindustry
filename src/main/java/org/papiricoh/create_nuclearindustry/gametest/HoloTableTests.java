package org.papiricoh.create_nuclearindustry.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloPart;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloProjection;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloView;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTable;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTracksPayload;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarNetwork;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The holographic command table: nine pieces fuse and break apart, the linker adds and takes away radars and its silo, designating
 * refuses whatever it should and sends the silo where it should, and its picture stays small whatever flies.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class HoloTableTests {
    private static final String BATCH = "holo_table";
    /** Where the test tables stand, clear of a silo built by {@link SiloTestBuilder}. */
    static final BlockPos CENTRE = new BlockPos(2, 1, 10);

    static HoloTableBlockEntity table(GameTestHelper helper) {
        for (int i = 0; i < HoloGrid.PARTS; i++) helper.setBlock(CENTRE.offset(HoloGrid.dx(i), 0, HoloGrid.dz(i)), HoloRegistry.TABLE.get());
        HoloTable.tryForm(helper.getLevel(), helper.absolutePos(CENTRE.offset(1, 0, 1)));
        return helper.getBlockEntity(CENTRE);
    }

    private static FakePlayer player(GameTestHelper helper, BlockPos near) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "holo_test"));
        BlockPos at = helper.absolutePos(near);
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 2.5);
        return player;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void nineLoosePiecesFuseAndBreakApart(GameTestHelper helper) {
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(CENTRE);
        helper.assertTrue(HoloTable.intact(level, centre), "nine pieces in a square make a table");
        helper.assertTrue(table != null, "with its heart in the centre");
        helper.assertTrue(HoloTable.part(level.getBlockState(centre.offset(-1, 0, -1))) == HoloPart.NW, "each piece knows its place");
        helper.setBlock(CENTRE.offset(1, 0, 0), Blocks.AIR);
        helper.assertTrue(!HoloTable.intact(level, centre), "a piece taken out breaks the table");
        helper.assertTrue(HoloTable.part(level.getBlockState(centre.offset(-1, 0, -1))) == HoloPart.SINGLE, "and the rest come loose");
        helper.assertTrue(!(level.getBlockEntity(centre) instanceof HoloTableBlockEntity), "its heart is gone");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theLinkerAddsAndRemovesRadarsAndTheSilo(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).build(helper);
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        List<GlobalPos> radars = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BlockPos pos = new BlockPos(10 + (i % 2), 1, 9 + i / 2);
            helper.setBlock(pos, RadarRegistry.RADAR.get());
            radars.add(GlobalPos.of(level.dimension(), helper.absolutePos(pos)));
        }
        radars.forEach(table::setTarget);
        helper.assertTrue(table.radars().size() == HoloTableBlockEntity.MAX_RADARS, "at most four radars, got " + table.radars().size());
        table.setTarget(radars.get(0));
        helper.assertTrue(!table.radars().contains(radars.get(0)), "a radar linked again is taken off");
        GlobalPos siloPos = GlobalPos.of(level.dimension(), silo.getBlockPos());
        table.setTarget(siloPos);
        helper.assertTrue(siloPos.equals(table.silo()), "a silo controller becomes its silo");
        table.setTarget(GlobalPos.of(level.dimension(), helper.absolutePos(new BlockPos(0, 0, 0))));
        helper.assertTrue(siloPos.equals(table.silo()) && table.radars().size() == 3, "anything else is ignored");
        table.setTarget(null);
        helper.assertTrue(table.silo() == null && table.radars().isEmpty(), "unlinking clears everything");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aLinkerOnAnyPieceLinksTheTable(GameTestHelper helper) {
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        BlockPos radarPos = new BlockPos(10, 1, 10);
        helper.setBlock(radarPos, RadarRegistry.RADAR.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack linker = new ItemStack(AllNuclearItems.CONTROL_ROOM_LINKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, linker);
        BlockPos radar = helper.absolutePos(radarPos);
        linker.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(radar), Direction.UP, radar, false)));
        BlockPos corner = helper.absolutePos(CENTRE.offset(1, 0, -1));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(corner), Direction.UP, corner, false);
        level.getBlockState(corner).useItemOn(linker, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(table.radars().contains(GlobalPos.of(level.dimension(), radar)), "the radar is linked through a corner piece");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void designatingChecksEverythingThenAimsTheSilo(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).build(helper);
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        FakePlayer near = player(helper, CENTRE);
        FakePlayer far = player(helper, CENTRE.offset(0, 0, 0));
        far.moveTo(far.getX() + 40, far.getY(), far.getZ());
        BlockPos floor = helper.absolutePos(SiloTestBuilder.FLOOR);
        helper.runAfterDelay(10, () -> {
            silo.validate();
            helper.assertTrue(silo.reach() > 1000, "the silo's rocket reaches over a kilometre, got " + silo.reach());
            helper.assertTrue(table.designate(near, floor.getX() + 150, floor.getZ(), false) == HoloTableBlockEntity.Designation.NO_SILO, "no silo linked");
            table.linkSiloUnchecked(GlobalPos.of(Level.NETHER, silo.getBlockPos()));
            helper.assertTrue(table.designate(near, floor.getX() + 150, floor.getZ(), false) == HoloTableBlockEntity.Designation.OTHER_DIMENSION,
                    "a silo in another dimension");
            table.setTarget(null);
            table.setTarget(GlobalPos.of(level.dimension(), silo.getBlockPos()));
            helper.assertTrue(table.designate(far, floor.getX() + 150, floor.getZ(), false) == HoloTableBlockEntity.Designation.TOO_FAR,
                    "a player away from the table");
            helper.assertTrue(table.designate(near, floor.getX() + 6000, floor.getZ(), false) == HoloTableBlockEntity.Designation.OUT_OF_REACH,
                    "beyond the rocket's reach");
            helper.assertTrue(table.designate(near, floor.getX() + 20000, floor.getZ(), false) == HoloTableBlockEntity.Designation.OFF_TABLE,
                    "beyond the table's map");
            HoloTableBlockEntity.Designation ok = table.designate(near, floor.getX() + 150, floor.getZ() + 3, true);
            helper.assertTrue(ok == HoloTableBlockEntity.Designation.OK, "a reachable target is taken, got " + ok);
            BlockPos target = silo.target();
            helper.assertTrue(target != null && target.getX() == floor.getX() + 150 && target.getZ() == floor.getZ() + 3,
                    "and the silo is aimed there, got " + target);
            helper.assertTrue(target != null && target.getY() >= level.getMinBuildHeight(), "at the height of the ground there");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void thePictureStaysSmallWhateverFlies(GameTestHelper helper) {
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        BlockPos radarPos = new BlockPos(10, 1, 10);
        helper.setBlock(radarPos, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = helper.getBlockEntity(radarPos);
        table.setTarget(GlobalPos.of(level.dimension(), helper.absolutePos(radarPos)));
        BlockPos at = helper.absolutePos(radarPos);
        List<Flight> flights = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Vec3 start = new Vec3(at.getX() + 200 + i * 10, at.getY() + 100, at.getZ() + i * 7);
            flights.add(FlightTracker.get(level).launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, start.add(800, -100, 0), at, Warhead.INERT, null));
        }
        helper.runAfterDelay(2, () -> {
            double base = MissileConfig.radarBaseRange, earth = MissileConfig.radarEarthRadius;
            HoloTracksPayload picture;
            int bytes;
            try {
                MissileConfig.radarBaseRange = 0;
                MissileConfig.radarEarthRadius = 0;
                radar.setSpeed((float) MissileConfig.minimumRpm);
                RadarNetwork.refresh(level);
                radar.refresh();
                picture = table.picture();
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
                HoloTracksPayload.CODEC.encode(buffer, picture);
                bytes = buffer.readableBytes();
                HoloTracksPayload back = HoloTracksPayload.CODEC.decode(buffer);
                helper.assertTrue(back.tracks().size() == picture.tracks().size(), "the picture survives the network");
            } finally {
                MissileConfig.radarBaseRange = base;
                MissileConfig.radarEarthRadius = earth;
                flights.forEach(f -> FlightTracker.get(level).remove(level, f));
            }
            helper.assertTrue(picture.tracks().size() == HoloTracksPayload.MAX_TRACKS, "at most 32 tracks, got " + picture.tracks().size());
            helper.assertTrue(picture.tracks().stream().allMatch(t -> t.history().length <= HoloTracksPayload.HISTORY * 3), "each with a short history");
            helper.assertTrue(bytes < 32 * 1024, "well under the payload limit, got " + bytes + " bytes");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ using the table in the world

    /** A point of the table (table coordinates) in the world. */
    private static Vec3 world(HoloTableBlockEntity table, double lx, double ly, double lz) {
        BlockPos c = table.getBlockPos();
        return new Vec3(c.getX() + lx, c.getY() + ly, c.getZ() + lz);
    }

    /** A sneaking click on the glass over world offset ({@code dx, dz}) from the table, as the table lays it out now. */
    private static HoloTableBlockEntity.Click sneakAt(HoloTableBlockEntity table, FakePlayer player, int dx, int dz) {
        HoloProjection p = table.picture().projection();
        Vec3 hit = world(table, p.x(dx), HoloProjection.Y, p.z(dz));
        return table.click(player, hit, player.getEyePosition(), hit.subtract(player.getEyePosition()), true);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void aClickOnAMissilePinsIt(GameTestHelper helper) {
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        BlockPos radarPos = new BlockPos(10, 1, 10);
        helper.setBlock(radarPos, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = helper.getBlockEntity(radarPos);
        table.setTarget(GlobalPos.of(level.dimension(), helper.absolutePos(radarPos)));
        BlockPos at = helper.absolutePos(radarPos);
        Vec3 start = new Vec3(at.getX() + 300, at.getY() + 100, at.getZ());
        Flight flight = FlightTracker.get(level).launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, start.add(600, -100, 0), at, Warhead.INERT, null);
        FakePlayer player = player(helper, CENTRE);
        helper.runAfterDelay(2, () -> {
            double base = MissileConfig.radarBaseRange, earth = MissileConfig.radarEarthRadius;
            HoloTableBlockEntity.Click first, second;
            boolean pinned;
            int tracks;
            try {
                MissileConfig.radarBaseRange = 0;
                MissileConfig.radarEarthRadius = 0;
                radar.setSpeed((float) MissileConfig.minimumRpm);
                RadarNetwork.refresh(level);
                radar.refresh();
                HoloTracksPayload picture = table.picture();
                tracks = picture.tracks().size();
                double[] marks = picture.marks(picture.projection(), 0);
                Vec3 mark = marks.length < 3 ? Vec3.ZERO : world(table, marks[0], marks[1], marks[2]);
                Vec3 eye = player.getEyePosition();
                Vec3 glass = world(table, 0.5, HoloProjection.Y, 0.5);
                first = table.click(player, glass, eye, mark.subtract(eye), false);
                pinned = picture.tracks().size() > 0 && picture.tracks().get(0).id().equals(table.selected())
                        && table.picture().board().selected() != null;
                second = table.click(player, glass, eye, mark.subtract(eye), false);
            } finally {
                MissileConfig.radarBaseRange = base;
                MissileConfig.radarEarthRadius = earth;
                FlightTracker.get(level).remove(level, flight);
            }
            helper.assertTrue(tracks == 1, "the radar tracks the flight, got " + tracks + " tracks");
            helper.assertTrue(first == HoloTableBlockEntity.Click.SELECTED && pinned, "a click on its mark pins it, got " + first);
            helper.assertTrue(second == HoloTableBlockEntity.Click.RELEASED && table.selected() == null, "a second click lets it go, got " + second);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aClickOnTheFrameStepsTheZoom(GameTestHelper helper) {
        HoloTableBlockEntity table = table(helper);
        FakePlayer player = player(helper, CENTRE);
        Vec3 rim = world(table, -0.95, 14.6 / 16, 0.5);
        Vec3 down = new Vec3(0, -1, 0);
        HoloTableBlockEntity.Click click = table.click(player, rim, rim.add(0, 1, 0), down, false);
        helper.assertTrue(click == HoloTableBlockEntity.Click.ZOOM && table.zoom() == 1, "the rim steps from automatic to the first view, got " + click);
        helper.assertTrue(table.picture().board().view() == HoloView.VIEWS[0], "which the picture shows");
        for (int i = 0; i < HoloView.VIEWS.length; i++) table.click(player, rim, rim.add(0, 1, 0), down, false);
        helper.assertTrue(table.zoom() == 0, "and round again to automatic, got " + table.zoom());
        Vec3 glass = world(table, 0.5, HoloProjection.Y, 0.5);
        helper.assertTrue(table.click(player, glass, glass.add(0, 1, 0), down, false) == HoloTableBlockEntity.Click.OPEN, "bare glass opens the screen");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void twoSneakingClicksSendTheSilo(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).build(helper);
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, CENTRE);
        BlockPos floor = helper.absolutePos(SiloTestBuilder.FLOOR);
        BlockPos centre = table.getBlockPos();
        helper.runAfterDelay(10, () -> {
            silo.validate();
            table.setTarget(GlobalPos.of(level.dimension(), silo.getBlockPos()));
            int dx = floor.getX() + 150 - centre.getX(), dz = floor.getZ() + 20 - centre.getZ();
            HoloTableBlockEntity.Click first = sneakAt(table, player, dx, dz);
            HoloTracksPayload.Mark mark = table.mark();
            helper.assertTrue(first == HoloTableBlockEntity.Click.MARKED && mark != null && mark.result() == 0,
                    "a sneaking click marks a target the silo can take, got " + first + " " + mark);
            helper.assertTrue(silo.target() == null || silo.target().getX() != floor.getX() + 150, "marking alone does not aim the silo");
            HoloTableBlockEntity.Click second = sneakAt(table, player, dx, dz);
            BlockPos target = silo.target();
            helper.assertTrue(second == HoloTableBlockEntity.Click.CONFIRMED, "a second click on the mark confirms it, got " + second);
            helper.assertTrue(target != null && target.getX() == floor.getX() + 150 && target.getZ() == floor.getZ() + 20,
                    "and the silo is aimed there, got " + target);
            helper.assertTrue(table.mark() == null, "the mark is spent");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aTargetBeyondReachIsRefused(GameTestHelper helper) {
        SiloControllerEntity silo = new SiloTestBuilder().tanks(2).build(helper);
        HoloTableBlockEntity table = table(helper);
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, CENTRE);
        helper.runAfterDelay(10, () -> {
            silo.validate();
            table.setTarget(GlobalPos.of(level.dimension(), silo.getBlockPos()));
            BlockPos before = silo.target();
            Vec3 rim = world(table, -0.95, 14.6 / 16, 0.5);
            for (int i = 0; i < HoloView.VIEWS.length; i++) table.click(player, rim, rim.add(0, 1, 0), new Vec3(0, -1, 0), false);
            helper.assertTrue(table.picture().board().view() == HoloView.VIEWS[HoloView.VIEWS.length - 1], "zoomed out to the widest view");
            HoloTableBlockEntity.Click first = sneakAt(table, player, 7000, 0);
            HoloTracksPayload.Mark mark = table.mark();
            helper.assertTrue(first == HoloTableBlockEntity.Click.MARKED && mark != null
                    && mark.result() == HoloTableBlockEntity.Designation.OUT_OF_REACH.ordinal(), "the mark shows it beyond reach, got " + mark);
            HoloTableBlockEntity.Click second = sneakAt(table, player, 7000, 0);
            helper.assertTrue(second == HoloTableBlockEntity.Click.REFUSED && table.lastDesignation() == HoloTableBlockEntity.Designation.OUT_OF_REACH,
                    "confirming it is refused, got " + second);
            helper.assertTrue(java.util.Objects.equals(before, silo.target()), "and the silo keeps its target");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aPlayerAwayFromTheTableCannotUseIt(GameTestHelper helper) {
        HoloTableBlockEntity table = table(helper);
        FakePlayer far = player(helper, CENTRE);
        far.moveTo(far.getX() + 12, far.getY(), far.getZ());
        Vec3 rim = world(table, -0.95, 14.6 / 16, 0.5);
        HoloTableBlockEntity.Click click = table.click(far, rim, far.getEyePosition(), rim.subtract(far.getEyePosition()), false);
        helper.assertTrue(click == HoloTableBlockEntity.Click.TOO_FAR && table.zoom() == 0, "nothing changes from 12 blocks away, got " + click);
        helper.assertTrue(sneakAt(table, far, 10, 10) == HoloTableBlockEntity.Click.TOO_FAR && table.mark() == null, "nor can they mark a target");
        helper.succeed();
    }
}
