package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;

/**
 * Graphite moderator and beryllium reflector, on the reactor_controller schematic. Its 3x3 core has
 * control rods on the corners, fuel on the sides and a heat exchanger in the middle; the scene swaps
 * corner columns for graphite and beryllium to show where each one counts.
 */
public class ReactorModerationScenes {

    public static void moderation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reactor_moderation", "Moderating and Reflecting the Core");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.6f);
        scene.setSceneOffsetY(-1.0f);

        Selection floor = util.select().fromTo(1, 1, 1, 5, 1, 5);
        Selection backWalls = util.select().fromTo(5, 2, 1, 5, 4, 5).add(util.select().fromTo(1, 2, 5, 4, 4, 5));
        Selection core = util.select().fromTo(2, 2, 2, 4, 4, 4);
        Selection graphite = util.select().fromTo(4, 2, 2, 4, 4, 2).add(util.select().fromTo(4, 2, 4, 4, 4, 4));
        Selection reflector = util.select().fromTo(2, 2, 4, 2, 4, 4);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(floor, Direction.UP);
        scene.world().showSection(backWalls, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(core, Direction.DOWN);
        scene.idle(20);

        scene.world().replaceBlocks(graphite, AllNuclearBlocks.GRAPHITE_MODERATOR.get().defaultBlockState(), true);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.GREEN, "graphite", graphite, 90);
        scene.overlay().showText(90)
                .text("Graphite Moderator fills whole interior columns. It slows neutrons, so the Fuel Channels sharing a face with it split more often.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(util.grid().at(4, 3, 2)))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.RED, "moderated", util.select().fromTo(4, 2, 3, 4, 4, 3), 90);
        scene.overlay().showText(90)
                .text("A fuel column with two graphite neighbours is fully moderated; one with a single neighbour, half. A fully moderated core is 35%% more reactive.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(util.grid().at(4, 3, 3)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().replaceBlocks(reflector, AllNuclearBlocks.BERYLLIUM_REFLECTOR.get().defaultBlockState(), true);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "reflector", reflector, 90);
        scene.overlay().showText(90)
                .text("Beryllium Reflector belongs in the columns that touch the shell: it bounces escaping neutrons back into the core.")
                .pointAt(util.vector().centerOf(util.grid().at(2, 3, 4)))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(100)
                .text("A ring lined all round loses a quarter fewer neutrons. Both make more power from the same fuel, so leave room for enough Control Rods.")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().centerOf(util.grid().at(2, 3, 2)))
                .placeNearTarget();
        scene.idle(110);
    }
}
