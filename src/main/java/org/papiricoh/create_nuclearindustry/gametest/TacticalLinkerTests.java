package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomLinkerItem;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalDevice;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalLink;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalLinkerItem;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarScopeBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

/** The tactical tool stores missile sources and applies only compatible, bounded links. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TacticalLinkerTests {
    private static final String BATCH = "tactical_linker";
    private static final BlockPos RADAR = new BlockPos(11, 1, 11), CONTROLLER = new BlockPos(11, 1, 8), SCOPE = new BlockPos(9, 1, 11);

    static Player player(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    static void click(GameTestHelper helper, Player player, ItemStack stack, BlockPos relative) {
        BlockPos at = helper.absolutePos(relative);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void tacticalLinkerLinksRadarAndSiloToTable(GameTestHelper helper) {
        HoloTableBlockEntity table = HoloTableTests.table(helper);
        helper.setBlock(RADAR, RadarRegistry.RADAR.get());
        helper.setBlock(CONTROLLER, SiloRegistry.CONTROLLER.get());
        ItemStack linker = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
        Player player = player(helper, linker);
        click(helper, player, linker, RADAR);
        helper.assertTrue(TacticalLinkerItem.getLink(linker) != null && TacticalLinkerItem.getLink(linker).kind() == TacticalLink.Kind.RADAR, "stores radar kind");
        click(helper, player, linker, HoloTableTests.CENTRE);
        click(helper, player, linker, CONTROLLER);
        click(helper, player, linker, HoloTableTests.CENTRE);
        helper.assertTrue(table.radars().size() == 1 && table.silos().size() == 1, "both missile devices link to table");
        helper.assertTrue(linker.getItem().isFoil(linker), "stored source glints");
        player.setShiftKeyDown(true);
        click(helper, player, linker, HoloTableTests.CENTRE);
        helper.assertTrue(table.links().isEmpty(), "sneaking on table clears its links");
        linker.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(TacticalLinkerItem.getLink(linker) == null && !linker.getItem().isFoil(linker), "sneak-use in air clears stored source");
        helper.succeed();
    }

    /**
     * A ground station linked by hand reaches the table and shows on its picture even before it has a network code; with one, and
     * turning, it gives the table its network.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void tacticalLinkerLinksGroundStationToTable(GameTestHelper helper) {
        HoloTableBlockEntity table = HoloTableTests.table(helper);
        BlockPos stationPos = new BlockPos(9, 1, 8);
        helper.setBlock(stationPos, org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry.GROUND_STATION.get());
        ItemStack linker = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
        Player player = player(helper, linker);
        click(helper, player, linker, stationPos);
        helper.assertTrue(TacticalLinkerItem.getLink(linker) != null && TacticalLinkerItem.getLink(linker).kind() == TacticalLink.Kind.GROUND_STATION,
                "stores ground station kind");
        click(helper, player, linker, HoloTableTests.CENTRE);
        helper.assertTrue(table.stations().size() == 1, "the station links to the table without a code");
        helper.assertTrue(table.picture().orbital().stations().size() == 1 && table.network().isEmpty(),
                "the picture lists the codeless station, which gives no network");
        var station = (org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationBlockEntity) helper.getBlockEntity(stationPos);
        station.setCode("LNK1");
        station.setSpeed((float) org.papiricoh.create_nuclearindustry.missile.MissileConfig.minimumRpm);
        helper.assertTrue(table.network().equals("LNK1"), "a turning station with a code gives the table its network");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void linkerOnLaunchBaseStoresItsController(GameTestHelper helper) {
        var controller = new SiloTestBuilder().build(helper);
        helper.setBlock(SiloTestBuilder.FLOOR, SiloRegistry.LAUNCH_BASE.get());
        ItemStack linker = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
        Player player = player(helper, linker);
        helper.runAfterDelay(10, () -> {
            click(helper, player, linker, SiloTestBuilder.FLOOR);
            TacticalLink link = TacticalLinkerItem.getLink(linker);
            helper.assertTrue(link != null, "base resolves an indexed launch controller");
            helper.assertTrue(link.kind() == TacticalLink.Kind.LAUNCH_CONTROLLER && link.target().pos().equals(controller.getBlockPos()), "stores controller, not clicked base");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void controlRoomLinkerRefusesHoloTableAndScope(GameTestHelper helper) {
        var table = HoloTableTests.table(helper);
        helper.setBlock(SCOPE, RadarRegistry.SCOPE.get());
        RadarScopeBlockEntity scope = helper.getBlockEntity(SCOPE);
        helper.assertTrue(scope != null, "scope exists");
        ItemStack linker = new ItemStack(AllNuclearItems.CONTROL_ROOM_LINKER.get());
        var original = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(RADAR));
        linker.set(AllNuclearDataComponents.LINKED_TARGET.get(), original);
        Player player = player(helper, linker);
        click(helper, player, linker, HoloTableTests.CENTRE);
        click(helper, player, linker, SCOPE);
        helper.assertTrue(table.links().isEmpty() && scope.links().isEmpty(), "old tool never applies a missile link");
        helper.assertTrue(original.equals(ControlRoomLinkerItem.getLink(linker)), "old tool does not store missile destination either");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void scopeTakesOnlyRadars(GameTestHelper helper) {
        helper.setBlock(SCOPE, RadarRegistry.SCOPE.get());
        helper.setBlock(RADAR, RadarRegistry.RADAR.get());
        helper.setBlock(CONTROLLER, SiloRegistry.CONTROLLER.get());
        RadarScopeBlockEntity scope = helper.getBlockEntity(SCOPE);
        helper.assertTrue(scope != null, "scope exists");
        var dimension = helper.getLevel().dimension();
        helper.assertTrue(scope.toggleLink(GlobalPos.of(dimension, helper.absolutePos(CONTROLLER))) == TacticalDevice.Result.UNSUPPORTED, "scope refuses launch controller");
        var radar = GlobalPos.of(dimension, helper.absolutePos(RADAR));
        helper.assertTrue(scope.toggleLink(radar) == TacticalDevice.Result.ADDED && scope.links().equals(java.util.List.of(radar)), "scope accepts one radar");
        var second = RADAR.above();
        helper.setBlock(second, RadarRegistry.RADAR.get());
        helper.assertTrue(scope.toggleLink(GlobalPos.of(dimension, helper.absolutePos(second))) == TacticalDevice.Result.FULL, "scope has one radar slot");
        helper.assertTrue(scope.toggleLink(radar) == TacticalDevice.Result.REMOVED && scope.links().isEmpty(), "same radar toggles off");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void unlinkSingleDevice(GameTestHelper helper) {
        var table = HoloTableTests.table(helper);
        helper.setBlock(RADAR, RadarRegistry.RADAR.get());
        helper.setBlock(CONTROLLER, SiloRegistry.CONTROLLER.get());
        BlockPos second = CONTROLLER.above();
        helper.setBlock(second, SiloRegistry.CONTROLLER.get());
        var dimension = helper.getLevel().dimension();
        var radar = GlobalPos.of(dimension, helper.absolutePos(RADAR));
        var first = GlobalPos.of(dimension, helper.absolutePos(CONTROLLER));
        var last = GlobalPos.of(dimension, helper.absolutePos(second));
        table.toggleLink(radar); table.toggleLink(first); table.toggleLink(last);
        table.selectSilo(last.pos());
        helper.assertTrue(table.unlink(first) && table.silos().equals(java.util.List.of(last)) && last.equals(table.silo()), "unlink keeps selected controller identity after index shift");
        helper.assertTrue(!table.unlink(first) && table.radars().equals(java.util.List.of(radar)), "single unlink leaves other devices intact");
        table.unlink(radar);
        helper.assertTrue(table.radars().isEmpty() && table.silos().size() == 1, "radar unlinked independently");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void linkOutOfRangeRefused(GameTestHelper helper) {
        var table = HoloTableTests.table(helper);
        helper.setBlock(RADAR, RadarRegistry.RADAR.get());
        var target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(RADAR));
        ItemStack linker = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
        Player player = player(helper, linker);
        linker.set(AllNuclearDataComponents.TACTICAL_LINK.get(), new TacticalLink(target, TacticalLink.Kind.RADAR));
        double previous = MissileConfig.tacticalLinkRange;
        try {
            MissileConfig.tacticalLinkRange = 1;
            helper.assertTrue(table.toggleLink(target) == TacticalDevice.Result.OUT_OF_RANGE, "device itself checks link range");
            click(helper, player, linker, HoloTableTests.CENTRE);
            helper.assertTrue(table.links().isEmpty(), "linker refuses distant source");
            var otherDimension = GlobalPos.of(net.minecraft.world.level.Level.NETHER, helper.absolutePos(RADAR));
            helper.assertTrue(table.toggleLink(otherDimension) == TacticalDevice.Result.OUT_OF_RANGE, "cross-dimension source refused");
        } finally { MissileConfig.tacticalLinkRange = previous; }
        helper.succeed();
    }
}
