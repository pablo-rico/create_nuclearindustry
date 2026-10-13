package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;

/**
 * The particle accelerator's scenes, on the schematics of tools/structgen/ponder_accelerator.py.
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
}
