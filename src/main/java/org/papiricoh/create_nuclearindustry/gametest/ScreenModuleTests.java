package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.BlockScreen;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.CoreMap;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.LedGroups;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.PlasmaSection;
import org.papiricoh.create_nuclearindustry.controlroom.desk.TrendRing;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/** The guarded button and the screen modules: bar graph, trend recorder, core map and plasma section. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ScreenModuleTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aGuardedButtonPressesOnlyUnderItsLiftedCover(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        BlockPos pos = new BlockPos(8, 1, 8);
        place(helper, table(), pos);
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            helper.assertTrue(desk.insert(0, ModuleType.GUARDED_BUTTON), "the guarded button fits");
            desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
            helper.assertTrue(desk.press(0, false), "a click lifts the cover");
            helper.assertTrue(desk.module(0).coverOpen(), "the cover is up");
            helper.assertFalse(reactor.isScramRequested(), "lifting the cover is no SCRAM");
            helper.assertTrue(desk.press(0, false), "the next click presses the button");
            helper.assertTrue(reactor.isScramRequested(), "which SCRAMs the reactor");
            helper.assertTrue(desk.press(0, true), "a sneaking click");
            helper.assertFalse(desk.module(0).coverOpen(), "closes the cover");
            helper.assertTrue(desk.press(0, false), "lifted again");
        });
        helper.runAfterDelay(10 + ControlModule.COVER_TICKS, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            helper.assertTrue(desk.module(0) != null && !desk.module(0).coverOpen(), "the cover falls shut by itself");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void screenSettingsAreKeptInRange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        place(helper, table(), pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        helper.assertTrue(desk.insert(0, ModuleType.BAR_GRAPH) && desk.insert(1, ModuleType.TREND_RECORDER), "the screens fit");
        helper.assertTrue(desk.module(0).setting() == 90, "a bar graph starts red from 90%");
        // As a screen sending out-of-range numbers would.
        CompoundTag bar = desk.module(0).writeConfig(helper.getLevel().registryAccess());
        bar.putInt("Setting", 150);
        CompoundTag trend = desk.module(1).writeConfig(helper.getLevel().registryAccess());
        trend.putInt("Setting", 9);
        desk.configure(0, bar);
        desk.configure(1, trend);
        helper.assertTrue(desk.module(0).setting() == 100, "the alarm level stops at 100%, got " + desk.module(0).setting());
        helper.assertTrue(desk.module(1).setting() == TrendRing.SECONDS.length - 1, "the speed stops at the slowest");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aTrendRecorderFillsWithItsReading(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        place(helper, table(), pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        helper.assertTrue(desk.insert(0, ModuleType.TREND_RECORDER), "the trend recorder fits");
        desk.configure(0, desk.module(0).withReading(GaugeReading.AREA_EXPOSURE).writeConfig(helper.getLevel().registryAccess()));
        desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(2, 1, 2))));
        helper.succeedWhen(() -> {
            ControlModule trend = desk.module(0);
            helper.assertTrue(trend != null && trend.active(), "the trend recorder reads the area");
            byte[] samples = trend.display();
            helper.assertTrue(samples.length >= 3, "a sample a second, got " + samples.length);
            helper.assertFalse(Float.isNaN(TrendRing.value(samples[samples.length - 1])), "the newest is a reading, not a gap");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theCoreMapShowsTheReactorsColumns(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos pos = new BlockPos(8, 1, 8);
        place(helper, table(), pos);
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            helper.assertTrue(desk.insert(0, ModuleType.CORE_MAP), "the map fits");
            desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
        });
        helper.succeedWhen(() -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            ControlModule map = desk.module(0);
            helper.assertTrue(map != null && map.active(), "the map reads the reactor");
            byte[] data = map.display();
            // The builder's 3x3 interior, north-west first: fuel in two corners, the heat exchanger in the
            // middle and the control channel in the south-west corner.
            helper.assertTrue(CoreMap.side(data) == 3, "a 3x3 interior, got " + CoreMap.side(data));
            helper.assertTrue(CoreMap.kind(data, 0, 0) == CoreMap.Kind.FUEL, "fuel in the north-west");
            helper.assertTrue(CoreMap.kind(data, 2, 2) == CoreMap.Kind.FUEL, "and in the south-east");
            helper.assertTrue(CoreMap.kind(data, 1, 1) == CoreMap.Kind.EXCHANGER, "the heat exchanger in the middle");
            helper.assertTrue(CoreMap.kind(data, 0, 2) == CoreMap.Kind.CONTROL, "the control channel in the south-west");
            helper.assertTrue(CoreMap.value(data, 0, 2) > 0.9f, "with its rods all in, got " + CoreMap.value(data, 0, 2));
            helper.assertTrue(CoreMap.kind(data, 2, 0) == CoreMap.Kind.EMPTY, "the other columns are empty");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aCoreMapOnAnAreaShowsNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        place(helper, table(), pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        helper.assertTrue(desk.insert(0, ModuleType.CORE_MAP), "the map fits");
        desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(2, 1, 2))));
        helper.succeedWhen(() -> {
            ControlModule map = desk.module(0);
            helper.assertTrue(map != null && !map.active(), "a core map needs a reactor");
            helper.assertTrue(map.display().length == 0, "and draws nothing without one");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fourCoreMapsInOneBlockMakeOneScreen(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos pos = new BlockPos(8, 1, 8);
        place(helper, table(), pos);
        helper.runAfterDelay(5, () -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
                helper.assertTrue(desk.insert(slot, ModuleType.CORE_MAP), "a map fits slot " + slot);
            }
            desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER)));
        });
        helper.succeedWhen(() -> {
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            helper.assertTrue(desk.mergedScreen(), "four maps in one block are one screen");
            LedGroups.Cell head = ModuleSurfaceBlock.headOf(helper.getLevel(), helper.absolutePos(pos), 3);
            helper.assertTrue(head.slot() == BlockScreen.HEAD, "any of them sets up slot 0");
            helper.assertTrue(desk.module(BlockScreen.HEAD) != null && CoreMap.side(desk.module(BlockScreen.HEAD).display()) == 3,
                    "slot 0 draws the map");
            helper.assertTrue(desk.module(2) != null && desk.module(2).display().length == 0, "the others keep no picture of their own");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aMissingModuleSplitsTheScreen(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        place(helper, table(), pos);
        ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
        for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
            helper.assertTrue(desk.insert(slot, slot == 3 ? ModuleType.PLASMA_SECTION : ModuleType.CORE_MAP), "a screen fits slot " + slot);
        }
        helper.assertFalse(desk.mergedScreen(), "a plasma section among core maps is no screen");
        LedGroups.Cell head = ModuleSurfaceBlock.headOf(helper.getLevel(), helper.absolutePos(pos), 2);
        helper.assertTrue(head.slot() == 2, "each module sets itself up");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void thePlasmaSectionShowsTheTokamak(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        BlockPos pos = new BlockPos(1, 1, 1);
        place(helper, table(), pos);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            ControlDeskBlockEntity desk = helper.getBlockEntity(pos);
            if (desk.module(0) == null) {
                desk.insert(0, ModuleType.PLASMA_SECTION);
                desk.setTarget(GlobalPos.of(helper.getLevel().dimension(), tokamak.getBlockPos()));
            }
            ControlModule section = desk.module(0);
            helper.assertTrue(section != null && section.active(), "the section reads the tokamak");
            PlasmaSection.Look look = PlasmaSection.decode(section.display());
            helper.assertTrue(look != null, "it carries a picture");
            helper.assertFalse(look.plasma(), "an offline tokamak has no plasma");
            helper.assertFalse(look.coils(), "nor current in its coils");
        });
    }

    // ------------------------------------------------------------------ helpers

    private static Block table() {
        return ControlRoom2Registry.CONTROL_TABLE.get();
    }

    private static void place(GameTestHelper helper, Block block, BlockPos pos) {
        helper.setBlock(pos, block.defaultBlockState().setValue(ModuleSurfaceBlock.FACING, Direction.NORTH));
        BlockPos abs = helper.absolutePos(pos);
        helper.getLevel().setBlock(abs, ModuleSurfaceBlock.refresh(helper.getBlockState(pos), helper.getLevel(), abs), 3);
    }
}
