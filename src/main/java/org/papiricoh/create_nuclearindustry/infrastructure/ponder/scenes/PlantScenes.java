package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.CoriumBlock;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

/**
 * The "Nuclear power plant" series: four chained scenes that follow the heat from the core to
 * the turbine, then the control room and containment around it, then the spent fuel and the
 * waste it leaves. Geometry lives in {@code ponder/plant_*.nbt} (tools/structgen/ponder_plant_*.py);
 * every text box takes the next line of {@link PlantPonderText}, whose lists also feed the lang
 * keys. Block entities only get their looks here (desk modules, annunciator windows, a chart),
 * through NBT, so nothing in a scene depends on a running machine.
 */
public class PlantScenes {
    private static final int KINETIC = 64;

    // ------------------------------------------------------------------ 1. the primary loop

    public static void heatLoop(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = PlantPonderText.HEAT_LOOP;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 13);
        scene.scaleSceneView(0.55f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos controller = util.grid().at(3, 5, 7);
        BlockPos heater = util.grid().at(1, 6, 5);
        BlockPos valve = util.grid().at(1, 8, 5);
        BlockPos coldPort = util.grid().at(2, 2, 5);
        BlockPos hotPort = util.grid().at(4, 3, 5);
        BlockPos sgController = util.grid().at(10, 2, 5);
        BlockPos primaryIn = util.grid().at(9, 3, 5);
        BlockPos hotPump = util.grid().at(7, 3, 4);
        BlockPos coldPump = util.grid().at(7, 2, 2);
        BlockPos brokenPipe = util.grid().at(8, 3, 4);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 5, 5, 5, 9), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(controller))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // The pressurizer on the lid.
        scene.world().showSection(util.select().fromTo(1, 6, 5, 1, 8, 5), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "pressurizer", util.select().fromTo(1, 6, 5, 1, 8, 5), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(1, 7, 5), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(heater, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(valve))
                .placeNearTarget();
        scene.idle(100);

        // Hot and cold leg ports.
        scene.overlay().showOutline(PonderPalette.RED, "hot", util.select().position(hotPort), 90);
        scene.overlay().showOutline(PonderPalette.BLUE, "cold", util.select().position(coldPort), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(hotPort, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // The steam generator: floor, tubes, walls.
        scene.world().showSection(util.select().fromTo(9, 1, 5, 11, 1, 7), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(10, 2, 6, 10, 3, 6), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "tubes", util.select().fromTo(10, 2, 6, 10, 3, 6), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().centerOf(util.grid().at(10, 3, 6)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().showSection(util.select().fromTo(9, 2, 5, 11, 4, 7)
                .substract(util.select().fromTo(10, 2, 6, 10, 3, 6)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(sgController, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showOutline(PonderPalette.INPUT, "primary_in", util.select().position(primaryIn), 100);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "primary_out", util.select().position(11, 2, 5), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(primaryIn, Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);

        // The hot leg, then the cold leg, each moved by a pump.
        scene.world().showSection(util.select().fromTo(4, 3, 4, 9, 3, 4)
                .add(util.select().fromTo(6, 4, 4, 7, 4, 4)), Direction.DOWN);
        scene.idle(10);
        spin(scene, util.select().fromTo(6, 4, 4, 7, 4, 4), util.select().position(hotPump), KINETIC);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(util.grid().at(5, 3, 4)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().showSection(util.select().fromTo(2, 2, 2, 11, 2, 2)
                .add(util.select().fromTo(2, 2, 3, 2, 2, 4))
                .add(util.select().fromTo(11, 2, 3, 11, 2, 4))
                .add(util.select().fromTo(7, 3, 2, 8, 3, 2)), Direction.DOWN);
        scene.idle(10);
        spin(scene, util.select().fromTo(7, 3, 2, 8, 3, 2), util.select().position(coldPump), KINETIC);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(util.grid().at(4, 2, 2)))
                .placeNearTarget();
        scene.idle(110);

        // The secondary side, picked up by the next scene.
        scene.world().showSection(util.select().fromTo(10, 5, 5, 10, 5, 6)
                .add(util.select().fromTo(7, 2, 6, 8, 2, 6)), Direction.DOWN);
        scene.idle(10);
        scene.effects().emitParticles(util.vector().blockSurface(util.grid().at(10, 5, 5), Direction.NORTH),
                scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.03, -0.04)), 1, 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(10, 4, 6)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // A stopped pump.
        spin(scene, util.select().fromTo(6, 4, 4, 7, 4, 4), util.select().position(hotPump), 0);
        scene.idle(20);
        scene.world().modifyBlock(valve, state -> withOpen(state, true), false);
        scene.effects().emitParticles(util.vector().topOf(valve),
                scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.12, 0)), 2, 80);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(valve))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().modifyBlock(valve, state -> withOpen(state, false), false);
        spin(scene, util.select().fromTo(6, 4, 4, 7, 4, 4), util.select().position(hotPump), KINETIC);
        scene.idle(20);

        // A broken pipe.
        scene.world().destroyBlock(brokenPipe);
        scene.effects().emitParticles(util.vector().centerOf(brokenPipe),
                scene.effects().simpleParticleEmitter(ParticleTypes.FALLING_WATER, new Vec3(0, -0.05, 0)), 4, 80);
        scene.effects().emitParticles(util.vector().centerOf(brokenPipe),
                scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.05, 0)), 1, 80);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(brokenPipe))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.world().restoreBlocks(util.select().position(brokenPipe));
        scene.idle(20);
    }

    // ------------------------------------------------------------------ 2. the secondary loop

    public static void generation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = PlantPonderText.GENERATION;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.45f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos steamPump = util.grid().at(4, 5, 2);
        BlockPos exhaust = util.grid().at(7, 2, 3);
        BlockPos exhaustPump = util.grid().at(7, 2, 5);
        BlockPos towerInput = util.grid().at(7, 2, 7);
        BlockPos towerOutput = util.grid().at(6, 2, 10);
        BlockPos feedPump = util.grid().at(2, 2, 7);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 4, 3)
                .add(util.select().fromTo(0, 2, 2, 0, 3, 2)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(2, 4, 2)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // Steam to the turbine.
        scene.world().showSection(util.select().fromTo(2, 5, 2, 7, 5, 2)
                .add(util.select().position(7, 4, 2))
                .add(util.select().fromTo(3, 6, 2, 4, 6, 2)), Direction.DOWN);
        scene.idle(10);
        spin(scene, util.select().fromTo(3, 6, 2, 4, 6, 2), util.select().position(steamPump), KINETIC);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(5, 5, 2)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().showSection(util.select().fromTo(5, 1, 1, 9, 3, 3)
                .add(util.select().position(4, 2, 2)), Direction.DOWN);
        scene.idle(15);
        scene.world().setKineticSpeed(util.select().fromTo(4, 2, 2, 5, 2, 2), 32);
        scene.effects().emitParticles(util.vector().topOf(util.grid().at(7, 3, 2)),
                scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, -0.04, 0)), 1, 40);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(7, 2, 1), Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);

        // The exhaust port.
        scene.overlay().showOutline(PonderPalette.OUTPUT, "exhaust", util.select().position(exhaust), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(7, 3, 3)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        // The cooling tower, ring by ring, and the exhaust line into it.
        Selection tower = util.select().fromTo(6, 1, 6, 14, 4, 14).substract(util.select().position(7, 2, 6));
        scene.world().showSection(tower, Direction.UP);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(6, 5, 6, 14, 8, 14), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(6, 9, 6, 14, 12, 14), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(7, 2, 4, 7, 2, 6)
                .add(util.select().fromTo(7, 3, 4, 7, 3, 5)), Direction.DOWN);
        scene.idle(10);
        spin(scene, util.select().fromTo(7, 3, 4, 7, 3, 5), util.select().position(exhaustPump), KINETIC);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(towerInput, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        plume(scene, util);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(10, 12, 7)))
                .placeNearTarget();
        scene.idle(110);

        // Feedwater back to the steam generator.
        scene.world().showSection(util.select().fromTo(2, 2, 10, 5, 2, 10)
                .add(util.select().fromTo(2, 2, 4, 2, 2, 9))
                .add(util.select().fromTo(2, 3, 7, 2, 3, 8)), Direction.DOWN);
        scene.idle(10);
        spin(scene, util.select().fromTo(2, 3, 7, 2, 3, 8), util.select().position(feedPump), KINETIC);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(towerOutput, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(util.grid().at(2, 2, 5)))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(2, 5, 2)))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 1), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);
        PonderCamera.showEast(scene);
        scene.idle(50);
        PonderCamera.backFromEast(scene);
        scene.idle(30);
    }

    // ------------------------------------------------------------------ 3. control room and containment

    public static void controlSafety(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = PlantPonderText.CONTROL_SAFETY;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.5f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos controller = util.grid().at(9, 6, 9);
        BlockPos leftDesk = util.grid().at(1, 1, 1);
        BlockPos rightDesk = util.grid().at(2, 1, 1);
        BlockPos annunciator = util.grid().at(1, 1, 3);
        BlockPos recorder = util.grid().at(2, 1, 3);
        BlockPos withdrawPort = util.grid().at(7, 4, 8);
        BlockPos insertPort = util.grid().at(7, 4, 10);
        BlockPos gearshift = util.grid().at(11, 8, 8);
        BlockPos penetration = util.grid().at(9, 3, 5);
        BlockPos window = util.grid().at(5, 4, 9);
        BlockPos monitor = util.grid().at(6, 1, 12);
        BlockPos vent = util.grid().at(13, 7, 9);
        ItemStack linker = new ItemStack(AllNuclearItems.CONTROL_ROOM_LINKER.get());
        GlobalPos reactorTarget = GlobalPos.of(Level.OVERWORLD, controller);

        // Desk modules and instrument faces are only looks: set them before anything shows.
        scene.world().modifyBlockEntityNBT(util.select().position(leftDesk), ControlDeskBlockEntity.class, nbt -> modules(nbt,
                new ControlModule(ModuleType.PUSH_BUTTON).withFunction(ModuleFunction.SCRAM),
                new ControlModule(ModuleType.DIAL).withFunction(ModuleFunction.ROD_TARGET),
                new ControlModule(ModuleType.LAMP).withFunction(ModuleFunction.LAMP_SCRAM),
                new ControlModule(ModuleType.LEVER).withFunction(ModuleFunction.AUTO_SCRAM)), true);
        scene.world().modifyBlockEntityNBT(util.select().position(rightDesk), ControlDeskBlockEntity.class, nbt -> modules(nbt,
                new ControlModule(ModuleType.CONTROLLER).withFunction(ModuleFunction.AUTO_POWER),
                new ControlModule(ModuleType.GAUGE).withFunction(ModuleFunction.READING),
                new ControlModule(ModuleType.LAMP).withFunction(ModuleFunction.LAMP_RUNNING),
                new ControlModule(ModuleType.DIAL).withFunction(ModuleFunction.SCRAM_THRESHOLD)), true);
        scene.world().modifyBlockEntityNBT(util.select().position(annunciator), AnnunciatorPanelBlockEntity.class,
                nbt -> annunciatorWindows(nbt, reactorTarget), true);
        scene.world().modifyBlockEntityNBT(util.select().position(recorder), ChartRecorderBlockEntity.class,
                nbt -> chart(nbt, reactorTarget), true);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(7, 1, 7, 11, 6, 11)
                .add(util.select().fromTo(8, 7, 8, 12, 8, 8))
                .add(util.select().fromTo(9, 3, 6, 9, 4, 6))
                .add(util.select().fromTo(9, 3, 3, 9, 4, 4)), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 2, 1, 1), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(controller), Pointing.DOWN, 60).rightClick().withItem(linker);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(leftDesk))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(leftDesk))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(rightDesk))
                .placeNearTarget();
        scene.idle(120);

        // Ports and the rod drive: withdraw the north-west rods by one block.
        scene.overlay().showOutline(PonderPalette.RED, "ports", util.select().position(withdrawPort)
                .add(util.select().position(insertPort)), 100);
        scene.world().modifyBlock(withdrawPort, state -> withPower(state, 15), false);
        scene.effects().indicateRedstone(withdrawPort);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(withdrawPort, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        ElementLink<WorldSectionElement> rods = scene.world().makeSectionIndependent(util.select().fromTo(8, 3, 8, 8, 6, 8));
        Selection drive = util.select().fromTo(8, 8, 8, 12, 8, 8);
        scene.world().setKineticSpeed(drive, 32);
        scene.idle(10);
        scene.world().hideSection(util.select().position(8, 7, 8), Direction.UP);
        scene.idle(5);
        scene.world().moveSection(rods, util.vector().of(0, 1, 0), 25);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(gearshift))
                .placeNearTarget();
        scene.idle(30);
        scene.world().setKineticSpeed(util.select().fromTo(8, 8, 8, 12, 8, 8), 0);
        scene.world().modifyBlock(withdrawPort, state -> withPower(state, 0), false);
        scene.idle(80);

        // SCRAM: the insert port lights and the rods go back in.
        scene.world().modifyBlock(insertPort, state -> withPower(state, 15), false);
        scene.effects().indicateRedstone(insertPort);
        scene.world().setKineticSpeed(util.select().fromTo(8, 8, 8, 12, 8, 8), -32);
        scene.world().moveSection(rods, util.vector().of(0, -1, 0), 25);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(leftDesk))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(30);
        scene.world().showSection(util.select().position(8, 7, 8), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(8, 8, 8, 12, 8, 8), 0);
        scene.world().modifyBlock(insertPort, state -> withPower(state, 0), false);
        scene.idle(70);

        // Annunciator and chart recorder.
        scene.world().showSection(util.select().fromTo(1, 1, 3, 2, 1, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(annunciator, Direction.NORTH), Pointing.DOWN, 50)
                .rightClick().withItem(linker);
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(annunciator, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        scene.overlay().showControls(util.vector().blockSurface(recorder, Direction.NORTH), Pointing.DOWN, 50)
                .withItem(new ItemStack(Items.PAPER));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(recorder, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);

        // The containment building closes around the reactor.
        scene.world().showSection(containmentWalls(util), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(5, 9, 5, 13, 9, 13), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(9, 9, 9)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showOutline(PonderPalette.GREEN, "penetrations", util.select().fromTo(9, 3, 5, 9, 4, 5), 80);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(penetration, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showOutline(PonderPalette.RED, "window", util.select().position(window), 110);
        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(window, Direction.WEST))
                .placeNearTarget();
        scene.idle(120);

        // Cut the front walls away to look inside.
        scene.world().hideSection(util.select().fromTo(5, 1, 5, 5, 8, 13), Direction.WEST);
        scene.world().hideSection(util.select().fromTo(6, 1, 5, 13, 8, 5), Direction.NORTH);
        scene.idle(20);
        scene.world().showSection(util.select().position(monitor)
                .add(util.select().position(6, 1, 6))
                .add(util.select().position(12, 1, 6)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(monitor, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(6, 1, 6)))
                .placeNearTarget();
        scene.idle(110);
        scene.world().modifyBlock(vent, state -> withOpen(state, true), false);
        scene.effects().indicateRedstone(vent);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(vent, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.world().modifyBlock(vent, state -> withOpen(state, false), false);

        // A meltdown: open the reactor's front walls, melt the core and let it fall onto the catcher.
        scene.world().hideSection(util.select().fromTo(7, 3, 7, 11, 5, 7), Direction.NORTH);
        scene.world().hideSection(util.select().fromTo(7, 3, 8, 7, 5, 11), Direction.WEST);
        scene.idle(20);
        BlockState corium = PwrRegistry.CORIUM.get().defaultBlockState();
        scene.world().setBlocks(util.select().fromTo(8, 3, 8, 10, 5, 10), corium, true);
        scene.effects().emitParticles(util.vector().topOf(util.grid().at(9, 6, 9)),
                scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.05, 0)), 1, 80);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(util.grid().at(9, 4, 9)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().setBlocks(util.select().fromTo(8, 4, 8, 10, 5, 10), Blocks.AIR.defaultBlockState(), false);
        scene.idle(10);
        scene.world().setBlocks(util.select().fromTo(8, 3, 8, 10, 3, 10), Blocks.AIR.defaultBlockState(), false);
        scene.world().setBlocks(util.select().fromTo(8, 2, 8, 10, 2, 10), corium, true);
        scene.idle(15);
        scene.world().setBlocks(util.select().fromTo(7, 2, 7, 11, 2, 11), corium.trySetValue(CoriumBlock.HEAT, 3), false);
        scene.idle(15);
        scene.world().setBlocks(util.select().fromTo(7, 2, 7, 11, 2, 11),
                PwrRegistry.SOLIDIFIED_CORIUM.get().defaultBlockState(), true);
        scene.overlay().showOutline(PonderPalette.GREEN, "catcher", util.select().fromTo(7, 1, 7, 11, 1, 11), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(9, 2, 9)))
                .placeNearTarget();
        scene.idle(110);
    }

    // ------------------------------------------------------------------ 4. spent fuel and waste

    public static void waste(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = PlantPonderText.WASTE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.5f);
        scene.setSceneOffsetY(-2.0f);

        BlockPos glowingRack = util.grid().at(2, 5, 2);
        BlockPos cooler = util.grid().at(5, 5, 5);
        BlockPos cask = util.grid().at(8, 9, 1);
        BlockPos compacted = util.grid().at(8, 9, 5);
        BlockPos cemented = util.grid().at(10, 9, 5);
        BlockPos canister = util.grid().at(12, 9, 5);
        BlockPos melter = util.grid().at(12, 10, 1);
        BlockPos stored = util.grid().at(10, 1, 1);
        BlockPos bentonite = util.grid().at(9, 1, 1);
        BlockPos water = util.grid().at(7, 2, 0);
        BlockPos inspector = util.grid().at(7, 1, 1);

        // The ground, the pool with its racks and cooler; everything else is revealed later.
        scene.world().showSection(ground(util), Direction.UP);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(3, 8, 2)), Pointing.DOWN, 60)
                .withItem(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(3, 8, 3)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.WHITE, "racks", util.select().fromTo(2, 5, 2, 4, 5, 2)
                .add(util.select().fromTo(2, 5, 4, 4, 5, 4)), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().centerOf(util.grid().at(3, 5, 4)))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().centerOf(glowingRack))
                .placeNearTarget();
        scene.idle(110);

        scene.world().setKineticSpeed(util.select().fromTo(5, 5, 5, 5, 10, 5), KINETIC);
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().centerOf(cooler))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);

        scene.world().showSection(util.select().fromTo(8, 9, 1, 9, 9, 1), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(cask))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showControls(util.vector().topOf(util.grid().at(9, 9, 1)), Pointing.DOWN, 60)
                .withItem(new ItemStack(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get()));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(9, 9, 1)))
                .placeNearTarget();
        scene.idle(120);

        // The three classes of waste and their packages.
        scene.world().showSection(util.select().position(compacted), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showControls(util.vector().topOf(compacted), Pointing.DOWN, 60)
                .withItem(new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get()));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().topOf(compacted))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        scene.world().showSection(util.select().position(cemented), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showControls(util.vector().topOf(cemented), Pointing.DOWN, 60)
                .withItem(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(cemented))
                .placeNearTarget();
        scene.idle(110);
        scene.world().showSection(util.select().fromTo(12, 9, 1, 12, 10, 1), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(canister), Direction.DOWN);
        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(util.grid().at(12, 9, 1)).add(0, -0.4, 0),
                scene.effects().simpleParticleEmitter(ParticleTypes.FLAME, new Vec3(0, 0.02, 0)), 1, 100);
        scene.overlay().showControls(util.vector().topOf(melter), Pointing.DOWN, 60)
                .withItem(new ItemStack(WasteRegistry.HIGH_LEVEL_WASTE.get()));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(melter, Direction.WEST))
                .placeNearTarget();
        scene.idle(120);

        // The repository, seen through the cut face of the ground.
        scene.world().showSection(util.select().position(8, 1, 1)
                .add(util.select().position(stored))
                .add(util.select().position(12, 1, 1)), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showBigLine(PonderPalette.WHITE, util.vector().blockSurface(util.grid().at(10, 8, 0), Direction.UP),
                util.vector().blockSurface(stored, Direction.UP), 100);
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(stored, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        scene.overlay().showOutline(PonderPalette.WHITE, "host_rock", util.select().fromTo(7, 3, 0, 13, 4, 0), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(10, 3, 0), Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.world().showSection(bentoniteFill(util), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(bentonite, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // Water spoils it.
        scene.world().setBlock(water, Blocks.WATER.defaultBlockState(), false);
        scene.world().showSection(util.select().position(water), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, "water", util.select().position(water), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(water, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.world().hideSection(util.select().position(water), Direction.UP);
        scene.idle(15);
        scene.world().setBlock(water, Blocks.AIR.defaultBlockState(), false);

        scene.world().showSection(util.select().position(inspector), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(inspector, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
    }

    // ------------------------------------------------------------------ helpers

    /** Turns a pump's motor and cog one way and the pump, meshed under the cog, the other. */
    private static void spin(CreateSceneBuilder scene, Selection motorAndCog, Selection pump, int speed) {
        scene.world().setKineticSpeed(motorAndCog, speed);
        scene.world().setKineticSpeed(pump, -speed);
    }

    private static void plume(CreateSceneBuilder scene, SceneBuildingUtil util) {
        Vec3 crown = util.vector().of(10.5, 12.6, 10.5);
        for (int i = 0; i < 6; i++) {
            Vec3 offset = new Vec3(Math.cos(i * Math.PI / 3) * 1.2, 0, Math.sin(i * Math.PI / 3) * 1.2);
            scene.effects().emitParticles(crown.add(offset),
                    scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.12, 0)), 1, 120);
        }
    }

    private static BlockState withOpen(BlockState state, boolean open) {
        return state.hasProperty(BlockStateProperties.OPEN) ? state.setValue(BlockStateProperties.OPEN, open) : state;
    }

    private static BlockState withPower(BlockState state, int power) {
        return state.hasProperty(BlockStateProperties.POWER) ? state.setValue(BlockStateProperties.POWER, power) : state;
    }

    /** The four walls of the containment, without the roof. */
    private static Selection containmentWalls(SceneBuildingUtil util) {
        return util.select().fromTo(5, 1, 5, 13, 8, 13).substract(util.select().fromTo(6, 1, 6, 12, 8, 12));
    }

    /** Everything of the waste schematic shown at the start: ground, pool, racks and cooler. */
    private static Selection ground(SceneBuildingUtil util) {
        return util.select().everywhere()
                .substract(util.select().fromTo(8, 9, 1, 12, 10, 5))
                .substract(util.select().fromTo(7, 1, 1, 13, 2, 3));
    }

    /** The bentonite packed round the stored packages. */
    private static Selection bentoniteFill(SceneBuildingUtil util) {
        return util.select().fromTo(7, 1, 1, 13, 2, 3)
                .substract(util.select().position(7, 1, 1))
                .substract(util.select().position(8, 1, 1))
                .substract(util.select().position(10, 1, 1))
                .substract(util.select().position(12, 1, 1));
    }

    private static HolderLookup.Provider registries() {
        Level level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
    }

    /** Fills a desk's four slots, in order. */
    private static void modules(CompoundTag nbt, ControlModule... modules) {
        HolderLookup.Provider registries = registries();
        ListTag list = new ListTag();
        for (int slot = 0; slot < modules.length && slot < ControlDeskBlockEntity.SLOTS; slot++) {
            CompoundTag entry = modules[slot].write(registries);
            entry.putInt("Slot", slot);
            list.add(entry);
        }
        nbt.put("Modules", list);
    }

    private static CompoundTag target(CompoundTag tag, GlobalPos target) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().ifPresent(encoded -> tag.put("Target", encoded));
        return tag;
    }

    /** Six windows linked to the reactor: one alarm new, one acknowledged and one clearing. */
    private static void annunciatorWindows(CompoundTag nbt, GlobalPos reactor) {
        String[][] windows = {{"PRESSURE_HIGH", "PRESS HI"}, {"SUBCOOLING_LOW", "SUBCOOL LO"}, {"PZR_LEVEL", "PZR LEVEL"},
                {"LOSS_OF_FLOW", "FLOW LO"}, {"TEMPERATURE", "CORE TEMP"}, {"SCRAM", "SCRAM"}};
        ListTag list = new ListTag();
        for (String[] window : windows) {
            CompoundTag tag = target(new CompoundTag(), reactor);
            tag.putString("Kind", "REACTOR");
            tag.putString("Metric", window[0]);
            tag.putInt("Setting", 50);
            tag.putString("Label", window[1]);
            list.add(tag);
        }
        nbt.put("Windows", list);
        // Ordinals of AnnunciatorLogic.State, 0x100 marks a window that tripped.
        nbt.put("States", new IntArrayTag(new int[]{1 | 0x100, 2 | 0x100, 0, 3, 0, 0}));
        nbt.putInt("FirstOut", 0);
    }

    /** Three pens of a reactor ramping up to power, with paper in the tray. */
    private static void chart(CompoundTag nbt, GlobalPos reactor) {
        target(nbt, reactor);
        nbt.putString("Kind", "REACTOR");
        ListTag pens = new ListTag();
        for (String pen : new String[]{"REACTOR_POWER", "REACTOR_TEMPERATURE", "REACTOR_PRESSURE"}) {
            pens.add(StringTag.valueOf(pen));
        }
        nbt.put("Pens", pens);
        nbt.putInt("Paper", ChartRecorderBlockEntity.MAX_PAPER / 2);
        int samples = ChartRecorderBlockEntity.VISIBLE;
        nbt.putLong("Drawn", samples);
        byte[][] traces = new byte[3][samples];
        for (int i = 0; i < samples; i++) {
            float t = i / (float) (samples - 1);
            float power = Math.min(0.75f, 0.1f + t * 1.1f);
            traces[0][i] = ChartBuffer.quantize(power + 0.02f * (float) Math.sin(i * 0.9));
            traces[1][i] = ChartBuffer.quantize(0.2f + 0.45f * (1 - (float) Math.exp(-3 * t)));
            traces[2][i] = ChartBuffer.quantize(0.6f + 0.03f * (float) Math.sin(i * 0.35));
        }
        for (int pen = 0; pen < 3; pen++) {
            nbt.putByteArray("Trace" + pen, traces[pen]);
        }
    }
}
