package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.TargetStationBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.BeamInterlockBlock;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;

/**
 * The particle accelerator's scenes, on the schematics of tools/structgen/ponder_accelerator.py:
 * the linac and the synchrotron ring.
 * Every text box takes the next line of {@link GuidePonderText}.
 */
public class AcceleratorScenes {

    /**
     * The linac along x at z 2: controller (0,1,2) facing east, ion source (1,1,2), a quadrupole,
     * cavities and pipes, another quadrupole, and the dump at (8,1,2). The machines stand north of
     * the line, where the camera sees them.
     */
    public static void linac(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ACCELERATOR_LINAC;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);

        BlockPos controller = util.grid().at(0, 1, 2);
        BlockPos source = util.grid().at(1, 1, 2);
        BlockPos cavity = util.grid().at(3, 1, 2);
        BlockPos quadrupole = util.grid().at(6, 1, 2);
        BlockPos dump = util.grid().at(8, 1, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(controller), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(source), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(source))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(2, 1, 2, 5, 1, 2), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "cavities", util.select().fromTo(3, 1, 2, 4, 1, 2), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(cavity))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(6, 1, 2, 7, 1, 2), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.RED, "quadrupoles",
                util.select().position(2, 1, 2).add(util.select().position(quadrupole)), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(quadrupole))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(dump), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(dump))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(2, 1, 1, 6, 1, 1), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 1), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.LEFT, 40).rightClick();
        scene.idle(20);
        scene.world().modifyBlockEntity(controller, AcceleratorControllerBlockEntity.class, be -> be.previewBeam(25.0, 180.0));
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(5, 1, 2)))
                .placeNearTarget();
        scene.idle(110);
    }

    /**
     * The synchrotron of radius 6 round (8,1,8): injector from the west (controller (0,1,8), ion
     * source, injection septum (2,1,8)), extraction septum (14,1,8) into the target station
     * (15,1,8) with a small trap (15,1,7) and its generator (15,1,6) beside it; the cryo feed (1,1,10)
     * and interlock (1,1,6) by the superconducting dipoles of the west arc, the RF and magnet
     * machines north of the ring.
     */
    public static void synchrotron(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ACCELERATOR_SYNCHROTRON;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 16);
        scene.scaleSceneView(0.55f);

        BlockPos controller = util.grid().at(0, 1, 8);
        BlockPos injection = util.grid().at(2, 1, 8);
        BlockPos extraction = util.grid().at(14, 1, 8);
        BlockPos station = util.grid().at(15, 1, 8);
        BlockPos trap = util.grid().at(15, 1, 7);
        BlockPos cryo = util.grid().at(1, 1, 10);
        BlockPos interlock = util.grid().at(1, 1, 6);
        BlockPos cavity = util.grid().at(7, 1, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 8, 2, 1, 8), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(injection))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        // The ring itself: everything between x 2..14 and z 2..14 at y 1 is a ring cell.
        scene.world().showSection(util.select().fromTo(2, 1, 2, 14, 1, 14).substract(util.select().position(injection)), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(8, 1, 14)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.RED, "quadrupoles", util.select().position(4, 1, 4).add(util.select().position(9, 1, 2))
                .add(util.select().position(14, 1, 10)).add(util.select().position(8, 1, 14)).add(util.select().position(4, 1, 12)), 90);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "cavity", util.select().position(cavity), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(9, 1, 2)))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(cryo), Direction.EAST);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.BLUE, "superconducting", util.select().fromTo(2, 1, 6, 2, 1, 7)
                .add(util.select().fromTo(2, 1, 9, 2, 1, 10)).add(util.select().position(cavity)), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(cryo, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().showSection(util.select().fromTo(7, 1, 1, 10, 1, 1), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(8, 1, 1), Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showOutline(PonderPalette.GREEN, "extraction", util.select().position(extraction), 80);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(extraction))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(station), Direction.WEST);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(station))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(15, 1, 6, 15, 1, 7), Direction.DOWN);
        scene.idle(10);
        scene.world().modifyBlockEntity(trap, PenningTrapBlockEntity.class, be -> be.preview(0, TrapState.STABLE, 1f, 60));
        scene.idle(10);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(trap))
                .placeNearTarget();
        scene.idle(110);

        scene.world().showSection(util.select().position(interlock), Direction.EAST);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(interlock, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(50);
        scene.overlay().showControls(util.vector().blockSurface(interlock, Direction.WEST), Pointing.LEFT, 30).rightClick();
        scene.idle(10);
        scene.world().modifyBlock(interlock, state -> state.setValue(BeamInterlockBlock.SECURED, true), false);
        scene.idle(50);

        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(interlock, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.LEFT, 40).rightClick();
        scene.idle(20);
        scene.world().modifyBlockEntity(controller, AcceleratorControllerBlockEntity.class, be -> be.previewBeam(30000.0, 2.0));
        scene.world().modifyBlockEntity(station, TargetStationBlockEntity.class, be -> be.receiveBeam(30000.0, 2.0, 60.0));
        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(8, 1, 2)))
                .placeNearTarget();
        for (int i = 1; i <= 10; i++) {
            double ng = 0.6 * i;
            scene.world().modifyBlockEntity(trap, PenningTrapBlockEntity.class, be -> be.preview(ng, TrapState.STABLE, 1f, 60));
            scene.idle(12);
        }
    }
}
