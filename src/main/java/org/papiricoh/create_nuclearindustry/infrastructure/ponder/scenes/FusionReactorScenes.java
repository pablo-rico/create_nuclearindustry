package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;

/**
 * Escena Ponder para el reactor de fusion. La implementacion real valida un anillo cuadrado
 * horizontal, con el controlador en el centro y una camara de plasma vacia.
 *
 * <p>Toda la geometria vive en {@code ponder/fusion_controller.nbt}; esta escena solo revela
 * secciones. Eso evita que Ponder dependa de bloques construidos con setBlock sobre un schematic
 * vacio o con limites incorrectos.
 */
public class FusionReactorScenes {
    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fusion_controller", tr("header"));
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.72f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos core = util.grid().at(4, 1, 4);
        BlockPos magnetInput = util.grid().at(4, 1, 2);
        BlockPos driveShaft = util.grid().at(4, 1, 1);
        BlockPos coolantPort = util.grid().at(2, 1, 4);
        BlockPos plasmaPort = util.grid().at(6, 1, 4);
        BlockPos fuelInjector = util.grid().at(4, 1, 6);

        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(core), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(tr(1))
                .pointAt(util.vector().topOf(core))
                .placeNearTarget();
        scene.idle(90);

        showPositions(scene, util, Direction.DOWN, new int[][]{
                {2, 1, 2}, {6, 1, 2}, {2, 1, 6}, {6, 1, 6}
        });
        scene.idle(15);
        scene.overlay().showText(80)
                .text(tr(2))
                .pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
                .placeNearTarget();
        scene.idle(90);

        showPositions(scene, util, Direction.SOUTH, new int[][]{
                {3, 1, 2}, {5, 1, 2},
                {2, 1, 3}, {6, 1, 3}, {2, 1, 5}, {6, 1, 5},
                {3, 1, 6}, {5, 1, 6}
        });
        scene.idle(15);
        scene.overlay().showText(85)
                .text(tr(3))
                .pointAt(util.vector().centerOf(core))
                .placeNearTarget();
        scene.idle(95);

        scene.overlay().showText(80)
                .text(tr(4))
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().centerOf(util.grid().at(3, 1, 4)))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(85)
                .text(tr(5))
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().centerOf(util.grid().at(3, 1, 2)))
                .placeNearTarget();
        scene.idle(95);

        scene.world().showSection(util.select().position(magnetInput), Direction.SOUTH);
        scene.world().showSection(util.select().position(driveShaft), Direction.SOUTH);
        scene.world().setKineticSpeed(util.select().fromTo(4, 1, 1, 4, 1, 2), 64);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(tr(6))
                .pointAt(util.vector().blockSurface(magnetInput, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(coolantPort), Direction.EAST);
        scene.idle(15);
        scene.overlay().showText(85)
                .text(tr(7))
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(coolantPort, Direction.WEST))
                .placeNearTarget();
        scene.idle(95);

        scene.world().showSection(util.select().position(plasmaPort), Direction.WEST);
        scene.idle(15);
        scene.overlay().showText(85)
                .text(tr(8))
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(plasmaPort, Direction.EAST))
                .placeNearTarget();
        scene.idle(95);

        scene.world().showSection(util.select().position(fuelInjector), Direction.NORTH);
        scene.idle(15);
        scene.overlay().showControls(util.vector().blockSurface(fuelInjector, Direction.SOUTH), Pointing.RIGHT, 50)
                .rightClick()
                .withItem(new ItemStack(AllNuclearItems.DT_FUEL_PELLET.get()));
        scene.overlay().showText(85)
                .text(tr(9))
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(fuelInjector, Direction.SOUTH))
                .placeNearTarget();
        scene.idle(95);

        scene.overlay().showControls(util.vector().topOf(core), Pointing.DOWN, 60)
                .rightClick();
        scene.overlay().showText(85)
                .text(tr(10))
                .pointAt(util.vector().topOf(core))
                .placeNearTarget();
        scene.idle(95);
    }

    private static void showPositions(CreateSceneBuilder scene, SceneBuildingUtil util, Direction direction, int[][] positions) {
        for (int[] position : positions) {
            scene.world().showSection(util.select().position(position[0], position[1], position[2]), direction);
        }
    }

    private static String tr(String suffix) {
        return I18n.get("create_nuclearindustry.ponder.fusion_controller." + suffix);
    }

    private static String tr(int index) {
        return tr("text_" + index);
    }
}
