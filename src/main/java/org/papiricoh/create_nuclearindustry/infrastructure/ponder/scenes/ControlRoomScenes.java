package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.NuclearAlarmBlock;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModuleItem;
import org.papiricoh.create_nuclearindustry.controlroom.desk.CoreMap;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlock;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;

import java.util.Arrays;

import static org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ControlRoomLayout.*;

/**
 * The control room series on the control_room schematic (tools/structgen/ponder_control_room.py),
 * laid out like a real plant's main control room: instrument boards on the back and east walls, the
 * operators' console wrapping round them three blocks out, and the shift supervisor's tables behind.
 * Every scene dresses the room as a running plant would sync it ({@link ControlRoomLayout}), shows it
 * whole and then closes in on one part at a time ({@link PonderZoom}).
 */
public class ControlRoomScenes {
    private static final int PLATE = 19;
    private static final int ZOOM_TICKS = 25;
    private static final PonderZoom.View WHOLE = new PonderZoom.View(0.7f, 9, 11, -1.0f);
    private static final PonderZoom.View CONSOLE = new PonderZoom.View(1.1f, 9, 12, -1.0f);
    private static final PonderZoom.View CENTRE_DESKS = new PonderZoom.View(3.0f, 9, 13, -1.0f);
    private static final PonderZoom.View WEST_DESKS = new PonderZoom.View(3.0f, 6, 13, -1.0f);
    private static final PonderZoom.View EAST_DESKS = new PonderZoom.View(3.0f, 12, 13, -1.0f);
    private static final PonderZoom.View BOARD = new PonderZoom.View(1.0f, 9, 16, -4.0f);
    private static final PonderZoom.View SCREEN = new PonderZoom.View(1.6f, 9, 16, -3.5f);
    private static final PonderZoom.View BOARD_WEST = new PonderZoom.View(2.4f, 2, 16, -2.5f);
    private static final PonderZoom.View BOARD_TOP = new PonderZoom.View(2.0f, 8, 16, -6.5f);
    private static final PonderZoom.View EAST_WALL = new PonderZoom.View(2.2f, 16, 11, -3.5f);
    private static final PonderZoom.View TABLES = new PonderZoom.View(2.4f, 9, 6, -1.0f);
    private static final PonderZoom.View WEST_ARM_VIEW = new PonderZoom.View(2.4f, 3, 9, -1.0f);
    private static final PonderZoom.View STAND_IN = new PonderZoom.View(2.0f, 1, 1, 0.0f);
    /** Turning the camera this far shows the east wall's panels face on. */
    private static final float FACE_EAST_WALL = -45;

    // ------------------------------------------------------------------ the room

    public static void controlRoom(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.CONTROL_ROOM;
        Script lines = text.script();
        begin(scene, util, text);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(walls(util), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(backBoard(util), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(eastBoard(util), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(console(util), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(supervisor(util), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(util.vector().topOf(SCRAM_DESK))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);

        // Linking.
        PonderZoom.to(scene, CONSOLE, ZOOM_TICKS);
        scene.overlay().showControls(util.vector().topOf(SCRAM_DESK), Pointing.DOWN, 80).rightClick().withItem(linker());
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(util.vector().topOf(SCRAM_DESK))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(140);
        scene.overlay().showControls(util.vector().topOf(SCRAM_DESK), Pointing.DOWN, 50).rightClick().whileSneaking().withItem(linker());
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(SCRAM_DESK))
                .placeNearTarget();
        scene.idle(110);

        // One console, one link.
        scene.overlay().showOutline(PonderPalette.GREEN, "console", desks(util), 130);
        scene.overlay().showText(130)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(EAST_ARM))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(140);
        PonderZoom.to(scene, BOARD, ZOOM_TICKS);
        scene.overlay().showOutline(PonderPalette.GREEN, "board", wallPanels(util), 120);
        scene.overlay().showText(120)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(BACK_TRENDS, Direction.NORTH))
                .placeNearTarget();
        scene.idle(130);

        // The big screen.
        PonderZoom.to(scene, SCREEN, ZOOM_TICKS);
        Vec3 screen = util.vector().blockSurface(new BlockPos(9, 3, 17), Direction.NORTH);
        scene.overlay().showOutline(PonderPalette.WHITE, "screen", util.select().fromTo(6, 2, 17, 11, 4, 17), 120);
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(screen)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);
        scene.overlay().showControls(screen, Pointing.DOWN, 60).rightClick().withItem(wrench());
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(screen)
                .placeNearTarget();
        scene.idle(110);

        // Desks turning corners.
        PonderZoom.to(scene, CONSOLE, ZOOM_TICKS);
        scene.overlay().showOutline(PonderPalette.BLUE, "corners", util.select().position(WEST_CORNER).add(util.select().position(EAST_CORNER)), 110);
        scene.overlay().showText(120)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(EAST_CORNER))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);

        // The east wall: another console, and screens linked one by one.
        PonderCamera.turn(scene, FACE_EAST_WALL);
        PonderZoom.to(scene, EAST_WALL, ZOOM_TICKS);
        scene.overlay().showOutline(PonderPalette.GREEN, "tokamak", eastPanels(util), 120);
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(PLASMA_PANEL, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);
        PonderCamera.turn(scene, -FACE_EAST_WALL);

        // The supervisor's tables.
        PonderZoom.to(scene, TABLES, ZOOM_TICKS);
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(util.vector().topOf(SUPERVISOR_LEDS))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);

        // The card on the right of the screen.
        PonderZoom.to(scene, CENTRE_DESKS, ZOOM_TICKS);
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(util.vector().topOf(SCRAM_DESK))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);
        PonderZoom.to(scene, WHOLE, ZOOM_TICKS);
        scene.idle(20);
    }

    // ------------------------------------------------------------------ the modules

    public static void modules(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.CONTROL_MODULES;
        Script lines = text.script();
        begin(scene, util, text);
        Fit scram = fitAt(SCRAM_DESK);
        // The guarded button goes in during the scene.
        refit(scene, util, scram, 0, null);
        showRoom(scene, util);
        PonderZoom.to(scene, CENTRE_DESKS, ZOOM_TICKS);

        // Fitting, setting up and taking out.
        Vec3 guardedSlot = slot(SCRAM_DESK, 0);
        scene.overlay().showControls(guardedSlot, Pointing.DOWN, 50).rightClick().withItem(ControlModuleItem.stackOf(ModuleType.GUARDED_BUTTON));
        scene.idle(20);
        refit(scene, util, scram, 0, scram.faces()[0]);
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(guardedSlot)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        Vec3 controllerSlot = slot(CONTROLLER_DESK, 0);
        scene.overlay().showControls(controllerSlot, Pointing.DOWN, 60).rightClick().withItem(wrench());
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(controllerSlot)
                .placeNearTarget();
        scene.idle(120);
        BlockPos pulseDesk = new BlockPos(13, 1, 13);
        Fit pulse = fitAt(pulseDesk);
        Vec3 pulseSlot = slot(pulseDesk, 0);
        scene.overlay().showControls(pulseSlot, Pointing.DOWN, 50).rightClick().whileSneaking().withItem(wrench());
        scene.idle(20);
        refit(scene, util, pulse, 0, null);
        ElementLink<EntityElement> dropped = scene.world().createItemEntity(pulseSlot.add(0, 0.3, 0), new Vec3(0, 0.15, -0.05),
                ControlModuleItem.stackOf(ModuleType.PUSH_BUTTON));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(pulseSlot)
                .placeNearTarget();
        scene.idle(120);
        scene.world().modifyEntity(dropped, Entity::discard);
        refit(scene, util, pulse, 0, pulse.faces()[0]);

        // Buttons.
        Vec3 buttonSlot = slot(SCRAM_DESK, 2);
        scene.overlay().showControls(buttonSlot, Pointing.DOWN, 40).rightClick();
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(buttonSlot)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(10);
        refit(scene, util, scram, 2, scram.faces()[2].withShown(1));
        scene.idle(8);
        refit(scene, util, scram, 2, scram.faces()[2]);
        scene.idle(102);

        scene.overlay().showControls(guardedSlot, Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        refit(scene, util, scram, 0, scram.faces()[0].withCover(true));
        scene.overlay().showText(120)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(guardedSlot)
                .placeNearTarget();
        scene.idle(50);
        scene.overlay().showControls(guardedSlot, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        // Pressed: the SCRAM lamp beside it lights.
        refit(scene, util, scram, scram.faces()[0].withCover(true).withShown(1), scram.faces()[1].withShown(1),
                scram.faces()[2], scram.faces()[3]);
        scene.idle(10);
        refit(scene, util, scram, scram.faces()[0].withCover(true), scram.faces()[1].withShown(1), scram.faces()[2], scram.faces()[3]);
        scene.idle(60);
        scene.overlay().showControls(guardedSlot, Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.idle(10);
        refit(scene, util, scram, scram.faces()[0], scram.faces()[1].withShown(1), scram.faces()[2], scram.faces()[3]);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(guardedSlot)
                .placeNearTarget();
        scene.idle(100);
        refit(scene, util, scram, scram.faces());

        // Switches and dials.
        PonderZoom.to(scene, WEST_DESKS, ZOOM_TICKS);
        Fit pressurizer = fitAt(PRESSURIZER_DESK);
        Vec3 spray = slot(PRESSURIZER_DESK, 3);
        scene.overlay().showControls(spray, Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        refit(scene, util, pressurizer, 3, pressurizer.faces()[3].withShown(1));
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(spray)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(140);
        refit(scene, util, pressurizer, 3, pressurizer.faces()[3]);
        Fit controls = fitAt(CONTROLLER_DESK);
        Vec3 rodDial = slot(CONTROLLER_DESK, 2);
        scene.overlay().showControls(rodDial, Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        refit(scene, util, controls, controls.faces()[0], controls.faces()[1], controls.faces()[2].withShown(0.5f),
                controls.faces()[3].withShown(0.5f));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(rodDial)
                .placeNearTarget();
        scene.idle(120);
        refit(scene, util, controls, controls.faces());

        // The controller.
        Vec3 controller = slot(CONTROLLER_DESK, 0);
        scene.overlay().showText(120)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(controller)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);
        scene.overlay().showControls(controller, Pointing.DOWN, 40).rightClick();
        scene.idle(10);
        refit(scene, util, controls, 0, controls.faces()[0].withShown(0.95f));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(controller)
                .placeNearTarget();
        scene.idle(110);
        refit(scene, util, controls, controls.faces());

        // Lamps and gauges.
        PonderZoom.to(scene, EAST_DESKS, ZOOM_TICKS);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(slot(new BlockPos(13, 1, 13), 2))
                .placeNearTarget();
        scene.idle(110);
        PonderZoom.to(scene, WHOLE, ZOOM_TICKS);
    }

    // ------------------------------------------------------------------ the instruments

    public static void instruments(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.CONTROL_INSTRUMENTS;
        Script lines = text.script();
        begin(scene, util, text);
        showRoom(scene, util);

        // LED displays.
        PonderZoom.to(scene, EAST_DESKS, ZOOM_TICKS);
        Fit leds = fitAt(LED_DESK);
        scene.overlay().showOutline(PonderPalette.RED, "leds", util.select().position(LED_DESK), 110);
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(slot(LED_DESK, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(140);
        refit(scene, util, leds, led(leds.faces()[0], 1.0e9f), led(leds.faces()[1], 1.0e9f), led(leds.faces()[2], Float.NaN),
                led(leds.faces()[3], Float.NaN));
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(slot(LED_DESK, 0))
                .placeNearTarget();
        scene.idle(90);
        refit(scene, util, leds, leds.faces());

        // Bar graphs: the pressure climbs through amber into red.
        Fit bars = fitAt(BAR_DESK);
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(slot(BAR_DESK, 1))
                .placeNearTarget()
                .attachKeyFrame();
        for (float level : new float[]{0.7f, 0.8f, 0.86f, 0.92f, 0.97f}) {
            scene.idle(12);
            refit(scene, util, bars, 1, bars.faces()[1].withShown(level));
        }
        scene.idle(80);
        refit(scene, util, bars, bars.faces());

        // Trend recorders.
        PonderZoom.to(scene, WEST_DESKS, ZOOM_TICKS);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(slot(SCREEN_DESK, 0))
                .placeNearTarget();
        scene.idle(100);

        // The core map: a SCRAM drives every rod in.
        PonderZoom.to(scene, CENTRE_DESKS, ZOOM_TICKS);
        Fit core = fitAt(CORE_MAP_DESK);
        scene.overlay().showText(140)
                .text(lines.next())
                .pointAt(util.vector().topOf(CORE_MAP_DESK))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);
        Face scrammed = coreWithRods(core.faces()[0], 1.0f);
        refit(scene, util, core, scrammed, scrammed, scrammed, scrammed);
        scene.idle(70);
        refit(scene, util, core, core.faces());

        // The plasma section.
        PonderCamera.turn(scene, FACE_EAST_WALL);
        PonderZoom.to(scene, EAST_WALL, ZOOM_TICKS);
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(PLASMA_PANEL, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(140);
        PonderCamera.turn(scene, -FACE_EAST_WALL);

        // Annunciators.
        PonderZoom.to(scene, BOARD_TOP, ZOOM_TICKS);
        Vec3 window = util.vector().blockSurface(ANNUNCIATOR, Direction.NORTH);
        scene.overlay().showControls(window, Pointing.DOWN, 50).rightClick().withItem(linker());
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(window)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(140);
        annunciatorState(scene, util, ANNUNCIATOR, 1, 0);
        scene.overlay().showText(160)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(window)
                .placeNearTarget();
        scene.idle(50);
        scene.overlay().showControls(window, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        annunciatorState(scene, util, ANNUNCIATOR, 2, 0);
        scene.idle(45);
        annunciatorState(scene, util, ANNUNCIATOR, 3, 0);
        scene.idle(35);
        scene.overlay().showControls(window, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        annunciatorState(scene, util, ANNUNCIATOR, 0, -1);
        scene.idle(20);
        scene.effects().indicateRedstone(ANNUNCIATOR.east());
        scene.effects().indicateRedstone(ANNUNCIATOR.west());
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(window)
                .placeNearTarget();
        scene.idle(130);

        // Chart recorders.
        PonderZoom.to(scene, BOARD_WEST, ZOOM_TICKS);
        Vec3 chart = util.vector().blockSurface(CHART_RECORDER, Direction.NORTH);
        scene.overlay().showControls(chart, Pointing.DOWN, 50).rightClick().withItem(new ItemStack(Items.PAPER));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(chart)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        scene.overlay().showControls(chart, Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.idle(10);
        ElementLink<EntityElement> roll = scene.world().createItemEntity(chart.add(0, 0, -0.2), new Vec3(0, 0.15, -0.08),
                new ItemStack(ControlRoom2Registry.CHART_ROLL.get()));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(chart)
                .placeNearTarget();
        scene.idle(110);
        scene.world().modifyEntity(roll, Entity::discard);

        // The nuclear alarm goes off.
        PonderZoom.to(scene, BOARD_TOP, ZOOM_TICKS);
        scene.world().modifyBlock(ALARM, state -> state.setValue(NuclearAlarmBlock.LIT, true), false);
        scene.world().modifyBlock(ALARM_EAST, state -> state.setValue(NuclearAlarmBlock.LIT, true), false);
        scene.effects().indicateRedstone(ALARM);
        scene.overlay().showText(120)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(ALARM))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);
        scene.world().modifyBlock(ALARM, state -> state.setValue(NuclearAlarmBlock.LIT, false), false);
        scene.world().modifyBlock(ALARM_EAST, state -> state.setValue(NuclearAlarmBlock.LIT, false), false);
        PonderZoom.to(scene, WHOLE, ZOOM_TICKS);
    }

    // ------------------------------------------------------------------ signals

    public static void signals(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.CONTROL_SIGNALS;
        Script lines = text.script();
        begin(scene, util, text);
        showRoom(scene, util);
        PonderZoom.to(scene, WEST_ARM_VIEW, ZOOM_TICKS);

        Fit arm = fitAt(WEST_ARM_END);
        Vec3 button = slot(WEST_ARM_END, 3);
        scene.overlay().showControls(button, Pointing.DOWN, 60).rightClick().withItem(wrench());
        scene.overlay().showText(120)
                .text(lines.next())
                .pointAt(button)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(130);

        // Out of the back of the desk: the arm's operator stands east, so its back is west.
        BlockPos lamp = WEST_ARM_END.west();
        scene.world().setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState(), false);
        scene.idle(15);
        scene.overlay().showControls(button, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        refit(scene, util, arm, 3, arm.faces()[3].withShown(1));
        scene.world().modifyBlock(lamp, state -> state.setValue(RedstoneLampBlock.LIT, true), false);
        scene.effects().indicateRedstone(lamp);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(lamp))
                .placeNearTarget();
        scene.idle(20);
        refit(scene, util, arm, arm.faces());
        scene.world().modifyBlock(lamp, state -> state.setValue(RedstoneLampBlock.LIT, false), false);
        scene.idle(80);

        // A Create Redstone Link receiver on the same frequency.
        BlockPos link = new BlockPos(2, 1, 9);
        BlockPos linkLamp = link.north();
        scene.world().setBlock(link, receiver(createBlock("redstone_link").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP).setValue(BlockStateProperties.POWERED, false)), false);
        scene.world().setBlock(linkLamp, Blocks.REDSTONE_LAMP.defaultBlockState(), false);
        scene.idle(15);
        scene.overlay().showControls(button, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        refit(scene, util, arm, 3, arm.faces()[3].withShown(1));
        scene.world().modifyBlock(link, state -> state.setValue(BlockStateProperties.POWERED, true), false);
        scene.world().modifyBlock(linkLamp, state -> state.setValue(RedstoneLampBlock.LIT, true), false);
        scene.effects().indicateRedstone(link);
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().topOf(link))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);
        refit(scene, util, arm, arm.faces());
        scene.world().modifyBlock(link, state -> state.setValue(BlockStateProperties.POWERED, false), false);
        scene.world().modifyBlock(linkLamp, state -> state.setValue(RedstoneLampBlock.LIT, false), false);
        scene.idle(100);

        // An Advanced Redstone Link receiver on a code.
        BlockPos code = new BlockPos(2, 1, 6);
        BlockPos codeLamp = code.north();
        scene.world().setBlock(code, ControlRoom2Registry.ADVANCED_REDSTONE_LINK.get().defaultBlockState()
                .setValue(AdvancedRedstoneLinkBlock.FACING, Direction.UP).setValue(AdvancedRedstoneLinkBlock.RECEIVER, true), false);
        scene.world().setBlock(codeLamp, Blocks.REDSTONE_LAMP.defaultBlockState(), false);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(code), Pointing.DOWN, 50).rightClick();
        scene.overlay().showText(130)
                .text(lines.next())
                .pointAt(util.vector().topOf(code))
                .placeNearTarget();
        scene.idle(40);
        refit(scene, util, arm, 3, arm.faces()[3].withShown(1));
        scene.world().modifyBlock(code, state -> state.setValue(AdvancedRedstoneLinkBlock.POWERED, true), false);
        scene.world().modifyBlock(codeLamp, state -> state.setValue(RedstoneLampBlock.LIT, true), false);
        scene.effects().indicateRedstone(code);
        scene.idle(20);
        refit(scene, util, arm, arm.faces());
        scene.world().modifyBlock(code, state -> state.setValue(AdvancedRedstoneLinkBlock.POWERED, false), false);
        scene.world().modifyBlock(codeLamp, state -> state.setValue(RedstoneLampBlock.LIT, false), false);
        scene.idle(80);

        // The reactor's Redstone Ports, on the north face of a stand-in block of its casing in the near corner.
        Selection reactor = util.select().fromTo(0, 1, 0, 2, 3, 2);
        scene.world().setBlocks(reactor, AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState(), false);
        scene.world().setBlock(new BlockPos(1, 3, 1), AllNuclearBlocks.REACTOR_CONTROLLER.get().defaultBlockState(), false);
        BlockPos withdraw = new BlockPos(2, 1, 0);
        BlockPos insert = new BlockPos(0, 1, 0);
        BlockPos scramPort = new BlockPos(1, 2, 0);
        scene.world().setBlock(withdraw, port(RedstonePortMode.ROD_WITHDRAW, 15), false);
        scene.world().setBlock(insert, port(RedstonePortMode.ROD_INSERT, 0), false);
        scene.world().setBlock(scramPort, port(RedstonePortMode.SCRAM, 0), false);
        PonderZoom.to(scene, STAND_IN, ZOOM_TICKS);
        scene.effects().indicateRedstone(withdraw);
        scene.overlay().showText(150)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(withdraw, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(160);
        scene.world().modifyBlock(withdraw, state -> state.setValue(ReactorRedstonePortBlock.POWER, 0), false);
        scene.world().modifyBlock(scramPort, state -> state.setValue(ReactorRedstonePortBlock.POWER, 15), false);
        scene.world().modifyBlock(insert, state -> state.setValue(ReactorRedstonePortBlock.POWER, 15), false);
        scene.effects().indicateRedstone(scramPort);
        scene.overlay().showText(120)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(scramPort, Direction.NORTH))
                .placeNearTarget();
        scene.idle(130);
        PonderZoom.to(scene, WHOLE, ZOOM_TICKS);
    }

    // ------------------------------------------------------------------ setting the room up

    /** Title, plate and opening view, then the room dressed as a running plant before anything shows. */
    private static void begin(CreateSceneBuilder scene, SceneBuildingUtil util, PlantPonderText.Scene text) {
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, PLATE);
        scene.scaleSceneView(WHOLE.scale());
        PonderZoom.start(scene, WHOLE);
        HolderLookup.Provider registries = scene.world().getHolderLookupProvider();
        for (Fit fit : fits()) {
            refit(scene, util, fit, fit.faces(), registries);
        }
        for (Screen screen : SCREENS) {
            dressScreen(scene, util, screen);
        }
        for (int x = 1; x <= 16; x++) {
            int index = x;
            int lit = x == 4 || x == 13 ? 1 : x == 10 ? 2 : 0;
            scene.world().modifyBlockEntityNBT(util.select().position(x, 5, 17), AnnunciatorPanelBlockEntity.class,
                    nbt -> annunciator(nbt, REACTOR, false, index, lit), false);
        }
        for (int z = 7; z <= 16; z++) {
            int index = z;
            int lit = z == 9 ? 1 : 0;
            scene.world().modifyBlockEntityNBT(util.select().position(17, 4, z), AnnunciatorPanelBlockEntity.class,
                    nbt -> annunciator(nbt, TOKAMAK, true, index, lit), false);
        }
        for (int x : new int[]{1, 2, 15, 16}) {
            int seed = x;
            scene.world().modifyBlockEntityNBT(util.select().position(x, 1, 17), ChartRecorderBlockEntity.class,
                    nbt -> chart(nbt, REACTOR, seed), false);
        }
    }

    /** The whole room at once, for the scenes after the first. */
    private static void showRoom(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 0, PLATE - 1, 6, PLATE - 1), Direction.DOWN);
        scene.idle(20);
    }

    private static void dressScreen(CreateSceneBuilder scene, SceneBuildingUtil util, Screen screen) {
        boolean east = screen.origin().getX() == 17;
        Direction right = east ? Direction.SOUTH : Direction.WEST;
        MonitorReading reading = screen == REACTOR_SCREEN ? reactorReading() : screen == TOKAMAK_SCREEN ? tokamakReading() : turbineReading();
        GlobalPos target = screen == REACTOR_SCREEN ? REACTOR : screen == TOKAMAK_SCREEN ? TOKAMAK : TURBINE;
        for (int i = 0; i < screen.width(); i++) {
            for (int j = 0; j < screen.height(); j++) {
                BlockPos member = screen.origin().relative(right, i).above(j);
                boolean origin = i == 0 && j == 0;
                scene.world().modifyBlockEntityNBT(util.select().position(member), ControlMonitorBlockEntity.class,
                        nbt -> {
                            if (origin) {
                                ControlRoomLayout.screen(nbt, screen, target, reading);
                            } else {
                                screenMember(nbt, screen);
                            }
                        }, false);
            }
        }
    }

    /** Writes a desk, table or wall panel's modules as {@code faces}, linked to its console's machine. */
    private static void refit(CreateSceneBuilder scene, SceneBuildingUtil util, Fit fit, Face[] faces, HolderLookup.Provider registries) {
        ListTag modules = new Fit(fit.pos(), faces).write(registries);
        GlobalPos target = fit.pos().getX() == 17 ? TOKAMAK : REACTOR;
        scene.world().modifyBlockEntityNBT(util.select().position(fit.pos()), ControlDeskBlockEntity.class, nbt -> {
            nbt.put("Modules", modules.copy());
            target(nbt, target);
        }, false);
    }

    private static void refit(CreateSceneBuilder scene, SceneBuildingUtil util, Fit fit, Face... faces) {
        refit(scene, util, fit, faces, scene.world().getHolderLookupProvider());
    }

    /** As {@link #refit}, with one slot changed. */
    private static void refit(CreateSceneBuilder scene, SceneBuildingUtil util, Fit fit, int slot, Face face) {
        Face[] faces = Arrays.copyOf(fit.faces(), fit.faces().length);
        faces[slot] = face;
        refit(scene, util, fit, faces);
    }

    private static Fit fitAt(BlockPos pos) {
        return fits().stream().filter(fit -> fit.pos().equals(pos)).findFirst()
                .orElseThrow(() -> new IllegalStateException("no modules at " + pos));
    }

    /** The middle of a desk's slot, as seen from above: slots run near left, near right, far left, far right. */
    private static Vec3 slot(BlockPos desk, int slot) {
        // Desks face north here: the operator's left is east, the near row is to the north.
        double x = slot % 2 == 0 ? 0.75 : 0.25;
        double z = slot < 2 ? 0.35 : 0.7;
        return new Vec3(desk.getX() + x, desk.getY() + 0.8, desk.getZ() + z);
    }

    /** An LED display module showing another value. */
    private static Face led(Face face, float value) {
        return new Face(face.module(), face.shown(), value, face.display(), face.coverOpen(), face.demand());
    }

    /** A core map with every control channel's rods {@code inserted} of the way in. */
    private static Face coreWithRods(Face face, float inserted) {
        byte[] picture = face.display().clone();
        int side = CoreMap.side(picture);
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                if (CoreMap.kind(picture, column, row) == CoreMap.Kind.CONTROL) {
                    CoreMap.set(picture, column, row, CoreMap.Kind.CONTROL, inserted);
                }
            }
        }
        return new Face(face.module(), face.shown(), face.value(), picture, face.coverOpen(), face.demand());
    }

    /** One annunciator window: {@code state} (ordinal of AnnunciatorLogic.State) and the first out. */
    private static void annunciatorState(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, int state, int firstOut) {
        scene.world().modifyBlockEntityNBT(util.select().position(pos), AnnunciatorPanelBlockEntity.class, nbt -> {
            int[] states = new int[6];
            states[0] = state == 0 ? 0 : state | 0x100;
            nbt.putIntArray("States", states);
            nbt.putInt("FirstOut", firstOut);
        }, false);
    }

    private static BlockState port(RedstonePortMode mode, int power) {
        return AllNuclearBlocks.REACTOR_REDSTONE_PORT.get().defaultBlockState()
                .setValue(ReactorRedstonePortBlock.FACING, Direction.NORTH)
                .setValue(ReactorRedstonePortBlock.MODE, mode)
                .setValue(ReactorRedstonePortBlock.POWER, power);
    }

    /** A Create Redstone Link set to receive, through the property its block declares. */
    private static BlockState receiver(BlockState link) {
        return link.getBlock().getStateDefinition().getProperty("receiver") instanceof BooleanProperty receiver
                ? link.setValue(receiver, true) : link;
    }

    private static Block createBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    private static ItemStack linker() {
        return new ItemStack(AllNuclearItems.CONTROL_ROOM_LINKER.get());
    }

    private static ItemStack wrench() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
    }

    // ------------------------------------------------------------------ selections

    private static Selection walls(SceneBuildingUtil util) {
        return util.select().fromTo(0, 1, 18, 18, 6, 18)
                .add(util.select().fromTo(18, 1, 0, 18, 6, 17))
                .add(util.select().fromTo(17, 1, 17, 17, 6, 17));
    }

    private static Selection backBoard(SceneBuildingUtil util) {
        return util.select().fromTo(1, 1, 17, 16, 6, 17);
    }

    private static Selection eastBoard(SceneBuildingUtil util) {
        return util.select().fromTo(17, 1, 7, 17, 4, 16);
    }

    private static Selection console(SceneBuildingUtil util) {
        return util.select().fromTo(4, 1, 10, 14, 1, 13);
    }

    private static Selection supervisor(SceneBuildingUtil util) {
        return util.select().fromTo(7, 1, 5, 11, 1, 6);
    }

    private static Selection desks(SceneBuildingUtil util) {
        return util.select().fromTo(4, 1, 13, 14, 1, 13)
                .add(util.select().fromTo(4, 1, 11, 4, 1, 12))
                .add(util.select().fromTo(14, 1, 11, 14, 1, 12));
    }

    /** The back wall's wall control panels, without the screen and the chart recorders. */
    private static Selection wallPanels(SceneBuildingUtil util) {
        return util.select().fromTo(1, 1, 17, 16, 4, 17)
                .substract(util.select().fromTo(6, 2, 17, 11, 4, 17))
                .substract(util.select().fromTo(1, 1, 17, 2, 1, 17))
                .substract(util.select().fromTo(15, 1, 17, 16, 1, 17));
    }

    private static Selection eastPanels(SceneBuildingUtil util) {
        return util.select().fromTo(17, 1, 7, 17, 3, 16)
                .substract(util.select().fromTo(17, 2, 8, 17, 3, 10))
                .substract(util.select().fromTo(17, 2, 13, 17, 3, 15));
    }
}
