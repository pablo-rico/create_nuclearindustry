package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModuleItem;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.LedGroups;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

import java.util.Set;
import java.util.TreeSet;

/** Control tables, wall control panels and LED displays: how they join, where clicks land and what the LEDs show. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ControlSurfaceTests {

    // ------------------------------------------------------------------ tables

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void tablesInARowJoin(GameTestHelper helper) {
        BlockPos[] row = {new BlockPos(4, 1, 8), new BlockPos(5, 1, 8), new BlockPos(6, 1, 8)};
        place(helper, table(), Direction.NORTH, row);
        BlockState middle = helper.getBlockState(row[1]);
        helper.assertTrue(middle.getValue(ModuleSurfaceBlock.LEFT) && middle.getValue(ModuleSurfaceBlock.RIGHT),
                "the middle table is joined on both ends");
        // Operator to the north: their left is east, so the westmost table keeps its right end.
        BlockState west = helper.getBlockState(row[0]);
        helper.assertTrue(west.getValue(ModuleSurfaceBlock.LEFT) && !west.getValue(ModuleSurfaceBlock.RIGHT),
                "an end table keeps one end panel");
        helper.succeedWhen(() -> assertOneConsole(helper, row));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aTableSharesTheConsoleOfADesk(GameTestHelper helper) {
        BlockPos desk = new BlockPos(4, 1, 8);
        BlockPos table = new BlockPos(5, 1, 8);
        place(helper, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get(), Direction.NORTH, desk);
        place(helper, table(), Direction.NORTH, table);
        helper.assertFalse(helper.getBlockState(desk).getValue(ModuleSurfaceBlock.LEFT), "the desk keeps its end next to a table");
        helper.assertFalse(helper.getBlockState(table).getValue(ModuleSurfaceBlock.RIGHT), "and the table its end next to the desk");
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(2, 1, 2)));
        helper.runAfterDelay(3, () -> helper.<ControlDeskBlockEntity>getBlockEntity(table).setTarget(target));
        helper.succeedWhen(() -> {
            assertOneConsole(helper, desk, table);
            ControlDeskBlockEntity deskEntity = helper.getBlockEntity(desk);
            helper.assertTrue(target.equals(deskEntity.getTarget()), "linking the table links the desk beside it");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void tableSlotsFollowWhereTheTopIsClicked(GameTestHelper helper) {
        double top = 15.5 / 16;
        // Operator to the north: their left is east (+x), the near row at low z.
        BlockPos north = new BlockPos(2, 1, 2);
        place(helper, table(), Direction.NORTH, north);
        assertSlot(helper, north, 0.75, top, 0.25, 0, "north: near left");
        assertSlot(helper, north, 0.25, top, 0.25, 1, "north: near right");
        assertSlot(helper, north, 0.75, top, 0.75, 2, "north: far left");
        assertSlot(helper, north, 0.25, top, 0.75, 3, "north: far right");
        assertSlot(helper, north, 0.5, 0.5, 0.0, -1, "north: the cabinet is no slot");
        // Operator to the south: their left is west, the near row at high z.
        BlockPos south = new BlockPos(2, 1, 6);
        place(helper, table(), Direction.SOUTH, south);
        assertSlot(helper, south, 0.25, top, 0.75, 0, "south: near left");
        assertSlot(helper, south, 0.75, top, 0.25, 3, "south: far right");
        // Operator to the east: their left is south, the near row at high x.
        BlockPos east = new BlockPos(2, 1, 10);
        place(helper, table(), Direction.EAST, east);
        assertSlot(helper, east, 0.75, top, 0.75, 0, "east: near left");
        assertSlot(helper, east, 0.25, top, 0.25, 3, "east: far right");
        helper.assertTrue(slotsClickable(helper, north, top + 0.001).equals(Set.of(0, 1, 2, 3)), "every slot of the top can be clicked");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aTableTakesModulesAndDropsThem(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        place(helper, table(), Direction.NORTH, pos);
        ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
        for (int slot = 0; slot < 4; slot++) {
            helper.assertTrue(entity.insert(slot, ModuleType.LAMP), "a lamp fits slot " + slot);
        }
        helper.assertTrue(entity.remove(3) == ModuleType.LAMP, "and comes out again");
        helper.destroyBlock(pos);
        helper.succeedWhen(() -> {
            helper.assertItemEntityCountIs(AllNuclearItems.CONTROL_MODULES.get(ModuleType.LAMP).get(), pos, 2.0, 3);
        });
    }

    // ------------------------------------------------------------------ wall panels

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wallPanelsJoinSidewaysAndStack(GameTestHelper helper) {
        wall(helper, 4, 5, 1, 2, 9);
        BlockPos[] panels = {new BlockPos(4, 1, 8), new BlockPos(5, 1, 8), new BlockPos(4, 2, 8)};
        place(helper, wallPanel(), Direction.NORTH, panels);
        helper.assertTrue(helper.getBlockState(panels[0]).getValue(ModuleSurfaceBlock.LEFT), "panels side by side join");
        helper.assertTrue(helper.getBlockState(panels[1]).getValue(ModuleSurfaceBlock.RIGHT), "from both sides");
        helper.assertFalse(helper.getBlockState(panels[2]).getValue(ModuleSurfaceBlock.LEFT), "a panel above has nothing beside it");
        helper.succeedWhen(() -> assertOneConsole(helper, panels));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aWallPanelBesideADeskSharesItsConsole(GameTestHelper helper) {
        wall(helper, 5, 5, 1, 1, 9);
        BlockPos desk = new BlockPos(4, 1, 8);
        BlockPos panel = new BlockPos(5, 1, 8);
        place(helper, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get(), Direction.NORTH, desk);
        place(helper, wallPanel(), Direction.NORTH, panel);
        helper.succeedWhen(() -> assertOneConsole(helper, desk, panel));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aWallPanelFallsWhenItsWallGoes(GameTestHelper helper) {
        BlockPos support = new BlockPos(5, 1, 9);
        BlockPos panel = new BlockPos(5, 1, 8);
        helper.setBlock(support, Blocks.STONE);
        place(helper, wallPanel(), Direction.NORTH, panel);
        ControlDeskBlockEntity entity = helper.getBlockEntity(panel);
        helper.assertTrue(entity.insert(0, ModuleType.LAMP), "a lamp fits the panel");
        helper.destroyBlock(support);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, panel);
            helper.assertItemEntityPresent(ControlRoom2Registry.WALL_CONTROL_PANEL_ITEM.get(), panel, 2.0);
            helper.assertItemEntityPresent(AllNuclearItems.CONTROL_MODULES.get(ModuleType.LAMP).get(), panel, 2.0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aWallPanelNeedsAWall(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        BlockState state = wallPanel().defaultBlockState().setValue(ModuleSurfaceBlock.FACING, Direction.NORTH);
        helper.assertFalse(state.canSurvive(helper.getLevel(), helper.absolutePos(pos)), "nothing behind it to hang on");
        helper.setBlock(pos.south(), Blocks.STONE);
        helper.assertTrue(state.canSurvive(helper.getLevel(), helper.absolutePos(pos)), "a stone wall holds it");
        helper.assertFalse(state.setValue(ModuleSurfaceBlock.FACING, Direction.SOUTH).canSurvive(helper.getLevel(), helper.absolutePos(pos)),
                "but not facing into the wall");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wallSlotsFollowWhereTheFaceIsClicked(GameTestHelper helper) {
        double face = 13 / 16.0;
        // Wall to the south, operator to the north: their left is east (+x).
        BlockPos north = new BlockPos(4, 2, 4);
        helper.setBlock(north.south(), Blocks.STONE);
        place(helper, wallPanel(), Direction.NORTH, north);
        assertSlot(helper, north, 0.9, 0.1, face, 0, "north: lower left");
        assertSlot(helper, north, 0.1, 0.1, face, 1, "north: lower right");
        assertSlot(helper, north, 0.9, 0.9, face, 2, "north: upper left");
        assertSlot(helper, north, 0.1, 0.9, face, 3, "north: upper right");
        assertSlot(helper, north, 1.0, 0.5, 14.5 / 16, -1, "north: the plate's side is no slot");
        // Wall to the north, operator to the south: their left is west, the face at low z.
        BlockPos south = new BlockPos(8, 2, 8);
        helper.setBlock(south.north(), Blocks.STONE);
        place(helper, wallPanel(), Direction.SOUTH, south);
        assertSlot(helper, south, 0.1, 0.1, 3 / 16.0, 0, "south: lower left");
        assertSlot(helper, south, 0.9, 0.9, 3 / 16.0, 3, "south: upper right");
        helper.succeed();
    }

    // ------------------------------------------------------------------ LED displays

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void ledModulesJoinAcrossJoinedTables(GameTestHelper helper) {
        // Operator to the north: their left is east, so the row runs from the table at x 5 to the one at x 4.
        BlockPos left = new BlockPos(5, 1, 8);
        BlockPos right = new BlockPos(4, 1, 8);
        place(helper, table(), Direction.NORTH, left, right);
        insert(helper, left, 0, ModuleType.LED_DISPLAY);
        insert(helper, left, 1, ModuleType.LED_DISPLAY);
        insert(helper, right, 0, ModuleType.LED_DISPLAY);
        insert(helper, right, 2, ModuleType.LED_DISPLAY);
        insert(helper, right, 1, ModuleType.LAMP);
        LedGroups.Group group = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(right), 0);
        helper.assertTrue(group != null && group.length() == 3, "three LEDs side by side across two tables make one display");
        helper.assertTrue(group.head().equals(new LedGroups.Cell(helper.absolutePos(left), 0)), "headed by the leftmost");
        helper.assertTrue(LedGroups.headOf(helper.getLevel(), helper.absolutePos(left), 1).equals(group.head()),
                "every module of it leads to the head");
        LedGroups.Group back = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(right), 2);
        helper.assertTrue(back != null && back.length() == 1, "an LED in the other row stands alone");
        helper.assertTrue(LedGroups.groupOf(helper.getLevel(), helper.absolutePos(right), 1) == null, "a lamp is no display");
        helper.assertTrue(LedGroups.headedAt(helper.getLevel(), helper.absolutePos(left)).size() == 1, "the left table heads one display");
        helper.assertTrue(LedGroups.headedAt(helper.getLevel(), helper.absolutePos(right)).size() == 1, "the right table heads the other");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void ledDisplaysStopWhereBlocksDoNotJoin(GameTestHelper helper) {
        // A table and a desk side by side share a console but not a row.
        BlockPos table = new BlockPos(5, 1, 8);
        BlockPos desk = new BlockPos(4, 1, 8);
        place(helper, table(), Direction.NORTH, table);
        place(helper, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get(), Direction.NORTH, desk);
        insert(helper, table, 1, ModuleType.LED_DISPLAY);
        insert(helper, desk, 0, ModuleType.LED_DISPLAY);
        LedGroups.Group group = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(table), 1);
        helper.assertTrue(group != null && group.length() == 1, "a desk does not carry on a table's display");
        // Tables operated from opposite sides do not join either.
        BlockPos facingNorth = new BlockPos(5, 1, 4);
        BlockPos facingSouth = new BlockPos(4, 1, 4);
        place(helper, table(), Direction.NORTH, facingNorth);
        place(helper, table(), Direction.SOUTH, facingSouth);
        insert(helper, facingNorth, 1, ModuleType.LED_DISPLAY);
        insert(helper, facingSouth, 1, ModuleType.LED_DISPLAY);
        insert(helper, facingSouth, 0, ModuleType.LED_DISPLAY);
        LedGroups.Group lone = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(facingNorth), 1);
        helper.assertTrue(lone != null && lone.length() == 1, "nor does a table facing the other way");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void longLedRowsAreCutIntoDisplaysOfEight(GameTestHelper helper) {
        BlockPos[] row = {new BlockPos(7, 1, 8), new BlockPos(6, 1, 8), new BlockPos(5, 1, 8), new BlockPos(4, 1, 8), new BlockPos(3, 1, 8)};
        place(helper, table(), Direction.NORTH, row);
        for (BlockPos pos : row) {
            insert(helper, pos, 0, ModuleType.LED_DISPLAY);
            insert(helper, pos, 1, ModuleType.LED_DISPLAY);
        }
        LedGroups.Group first = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(row[3]), 1);
        helper.assertTrue(first != null && first.length() == LedGroups.MAX_CELLS, "the first eight make one display");
        helper.assertTrue(first.head().equals(new LedGroups.Cell(helper.absolutePos(row[0]), 0)), "headed by the leftmost");
        LedGroups.Group rest = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(row[4]), 1);
        helper.assertTrue(rest != null && rest.length() == 2, "the last two make another");
        helper.assertTrue(rest.head().equals(new LedGroups.Cell(helper.absolutePos(row[4]), 0)), "headed by its own leftmost");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void ledModulesJoinOnDesksAndWalls(GameTestHelper helper) {
        BlockPos leftDesk = new BlockPos(5, 1, 8);
        BlockPos rightDesk = new BlockPos(4, 1, 8);
        place(helper, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get(), Direction.NORTH, leftDesk, rightDesk);
        insert(helper, leftDesk, 3, ModuleType.LED_DISPLAY);
        insert(helper, rightDesk, 2, ModuleType.LED_DISPLAY);
        LedGroups.Group onDesks = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(rightDesk), 2);
        helper.assertTrue(onDesks != null && onDesks.length() == 2, "the far rows of two straight desks run on");
        wall(helper, 4, 5, 1, 1, 3);
        BlockPos leftPanel = new BlockPos(5, 1, 2);
        BlockPos rightPanel = new BlockPos(4, 1, 2);
        place(helper, wallPanel(), Direction.NORTH, leftPanel, rightPanel);
        insert(helper, leftPanel, 2, ModuleType.LED_DISPLAY);
        insert(helper, leftPanel, 3, ModuleType.LED_DISPLAY);
        insert(helper, rightPanel, 2, ModuleType.LED_DISPLAY);
        LedGroups.Group onWall = LedGroups.groupOf(helper.getLevel(), helper.absolutePos(rightPanel), 2);
        helper.assertTrue(onWall != null && onWall.length() == 3, "and so do the upper rows of wall panels");
        helper.assertTrue(onWall.head().equals(new LedGroups.Cell(helper.absolutePos(leftPanel), 2)), "headed by the leftmost");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anLedShowsItsReadingInItsUnits(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        place(helper, table(), Direction.NORTH, pos);
        ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
        insert(helper, pos, 0, ModuleType.LED_DISPLAY);
        insert(helper, pos, 1, ModuleType.LED_DISPLAY);
        entity.configure(0, entity.module(0).withReading(GaugeReading.AREA_EXPOSURE).writeConfig(helper.getLevel().registryAccess()));
        entity.configure(1, entity.module(1).withReading(GaugeReading.REACTOR_TEMPERATURE).writeConfig(helper.getLevel().registryAccess()));
        entity.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(2, 1, 2))));
        helper.succeedWhen(() -> {
            ControlModule area = entity.module(0);
            ControlModule reactor = entity.module(1);
            helper.assertTrue(area != null && reactor != null, "the modules should be fitted");
            helper.assertTrue(area.active() && !Float.isNaN(area.value()), "an area reading shows a number, got " + area.value());
            helper.assertTrue(!reactor.active() && Float.isNaN(reactor.value()), "a reactor reading of an area shows none");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anLedShowsTheReactorTemperature(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        BlockPos pos = new BlockPos(8, 1, 8);
        place(helper, table(), Direction.NORTH, pos);
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
            entity.insert(0, ModuleType.LED_DISPLAY);
            entity.configure(0, entity.module(0).withReading(GaugeReading.REACTOR_TEMPERATURE).writeConfig(helper.getLevel().registryAccess()));
            entity.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
        });
        helper.succeedWhen(() -> {
            ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
            ControlModule led = entity.module(0);
            helper.assertTrue(led != null && led.active(), "the LED should be fitted and reading the reactor");
            double temperature = reactor.getCoreTemperature();
            helper.assertTrue(Math.abs(led.value() - temperature) < 1.0,
                    "the LED reads the core temperature in °C: " + led.value() + " vs " + temperature);
        });
    }

    /**
     * A sneaking player's wrench goes straight to Create's wrench, past the block's own click: on a
     * module it takes out that module alone, off the modules it picks the whole table up.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aSneakWrenchTakesOutOnlyTheModuleItHits(GameTestHelper helper) {
        double top = 15.5 / 16;
        BlockPos pos = new BlockPos(2, 1, 2);
        place(helper, table(), Direction.NORTH, pos);
        insert(helper, pos, 0, ModuleType.GUARDED_BUTTON);
        insert(helper, pos, 3, ModuleType.LEVER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));

        sneakWrench(helper, player, pos, 0.75, top, 0.25);
        helper.assertBlockPresent(table(), pos);
        ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity.module(0) == null, "the guarded button is taken out");
        helper.assertTrue(entity.module(3) != null && entity.module(3).type() == ModuleType.LEVER, "the lever stays");
        helper.assertTrue(player.getInventory().countItem(ControlModuleItem.stackOf(ModuleType.GUARDED_BUTTON).getItem()) == 1,
                "the player gets the guarded button back");

        sneakWrench(helper, player, pos, 0.5, 0.5, 0.0);
        helper.assertBlockNotPresent(table(), pos);
        helper.succeed();
    }

    /** Uses the player's wrench on block-local {@code (x, y, z)} of {@code pos}, as a click there would. */
    private static void sneakWrench(GameTestHelper helper, Player player, BlockPos pos, double x, double y, double z) {
        BlockPos abs = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(abs).add(x, y, z), Direction.UP, abs, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    // ------------------------------------------------------------------ helpers

    private static Block table() {
        return ControlRoom2Registry.CONTROL_TABLE.get();
    }

    private static Block wallPanel() {
        return ControlRoom2Registry.WALL_CONTROL_PANEL.get();
    }

    /** A stone wall from x0 to x1 and y0 to y1 at z. */
    private static void wall(GameTestHelper helper, int x0, int x1, int y0, int y1, int z) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /** Places module surfaces then works out their joins, as placing them by hand would. */
    private static void place(GameTestHelper helper, Block block, Direction facing, BlockPos... positions) {
        for (BlockPos pos : positions) {
            helper.setBlock(pos, block.defaultBlockState().setValue(ModuleSurfaceBlock.FACING, facing));
        }
        for (int pass = 0; pass < 2; pass++) {
            for (BlockPos pos : positions) {
                BlockPos abs = helper.absolutePos(pos);
                helper.getLevel().setBlock(abs, ModuleSurfaceBlock.refresh(helper.getBlockState(pos), helper.getLevel(), abs), 3);
            }
        }
    }

    private static void insert(GameTestHelper helper, BlockPos pos, int slot, ModuleType type) {
        ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
        helper.assertTrue(entity.insert(slot, type), type + " fits slot " + slot);
    }

    private static void assertOneConsole(GameTestHelper helper, BlockPos... members) {
        BlockPos origin = null;
        for (BlockPos pos : members) {
            ControlDeskBlockEntity entity = helper.getBlockEntity(pos);
            helper.assertTrue(entity.consoleSize() == members.length, "one console of " + members.length + ", got " + entity.consoleSize());
            BlockPos its = entity.origin().map(ControlDeskBlockEntity::getBlockPos).orElse(null);
            helper.assertTrue(its != null, "every member finds the console's origin");
            if (origin == null) {
                origin = its;
            }
            helper.assertTrue(origin.equals(its), "with one origin");
        }
    }

    /** Asserts a click at block-local {@code (x, y, z)} lands on {@code slot}. */
    private static void assertSlot(GameTestHelper helper, BlockPos pos, double x, double y, double z, int slot, String what) {
        BlockPos abs = helper.absolutePos(pos);
        int got = ModuleSurfaceBlock.slotAt(helper.getBlockState(pos), abs, Vec3.atLowerCornerOf(abs).add(x, y, z));
        helper.assertTrue(got == slot, what + ": expected slot " + slot + ", got " + got);
    }

    /** Every slot a click on a flat top at height {@code y} can land on, sampled over a grid. */
    private static Set<Integer> slotsClickable(GameTestHelper helper, BlockPos pos, double y) {
        BlockPos abs = helper.absolutePos(pos);
        BlockState state = helper.getBlockState(pos);
        Set<Integer> slots = new TreeSet<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int slot = ModuleSurfaceBlock.slotAt(state, abs, Vec3.atLowerCornerOf(abs).add(0.05 + 0.1 * i, y, 0.05 + 0.1 * j));
                if (slot >= 0) {
                    slots.add(slot);
                }
            }
        }
        return slots;
    }
}
