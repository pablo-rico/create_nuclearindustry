package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;

/**
 * Builds the R=4 tokamak of {@code ponder/tokamak.nbt} (written by
 * tools/structgen/ponder_tokamak.py) layer by layer.
 */
public class TokamakScenes {

    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("tokamak", "Building a Tokamak");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.55f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos axis = util.grid().at(7, 3, 7);
        BlockPos plasma = util.grid().at(7, 3, 11);
        BlockPos controller = util.grid().at(13, 3, 7);
        BlockPos coolantPort = util.grid().at(13, 4, 7);
        BlockPos steamPort = util.grid().at(13, 2, 7);
        BlockPos injector = util.grid().at(1, 3, 7);
        BlockPos magnet = util.grid().at(7, 3, 13);

        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(7, 1, 7, 7, 5, 7), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("A tokamak holds a ring of plasma with magnets. Start with a Central Solenoid column, five blocks tall, at its axis.")
                .pointAt(util.vector().topOf(axis.above(2)))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("The torus has a 5x5 cross-section. Its floor and roof can hold Breeding Blankets, which turn fusion neutrons into tritium.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(7, 1, 11)))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(0, 2, 0, 14, 4, 14).substract(util.select().fromTo(7, 2, 7, 7, 4, 7)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("Toroidal Field Coils line the walls. The 3x3 core of the ring must stay empty: it is the plasma chamber.")
                .pointAt(util.vector().centerOf(plasma))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().layer(5), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("The ring may be 4 to 16 blocks from the solenoid to the plasma. A bigger ring holds more plasma and makes more power.")
                .pointAt(util.vector().topOf(util.grid().at(7, 5, 11)))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.EAST), Pointing.LEFT, 60).rightClick();
        scene.overlay().showText(90)
                .text("The Tokamak Controller sits on the outer equator. Right-click it to form the tokamak and open its control panel.")
                .pointAt(util.vector().blockSurface(controller, Direction.EAST))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(90)
                .text("A Fluid Port on INPUT takes water or heavy water as coolant...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(coolantPort, Direction.EAST))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("...and one on OUTPUT gives plasma steam for a Plasma Turbine.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(steamPort, Direction.EAST))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(injector, Direction.WEST), Pointing.RIGHT, 60)
                .rightClick()
                .withItem(new ItemStack(AllNuclearItems.DEUTERIUM_CELL.get()));
        scene.overlay().showText(100)
                .text("The Fuel Injector takes deuterium and tritium, by pipe or by hand with gas cells. On deuterium alone the plasma runs slowly and breeds its first tritium.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(injector, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);

        scene.world().setKineticSpeed(util.select().fromTo(7, 3, 13, 7, 3, 14), 128);
        scene.overlay().showText(90)
                .text("Magnet Drives turn rotation into the confining field: one for every 4 blocks of radius, at 64 RPM or more.")
                .pointAt(util.vector().blockSurface(magnet, Direction.SOUTH))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showText(90)
                .text("Keep the coolant flowing. A tokamak without coolant overheats and disrupts, blowing holes in its vessel.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(plasma))
                .placeNearTarget();
        scene.idle(100);
    }
}
