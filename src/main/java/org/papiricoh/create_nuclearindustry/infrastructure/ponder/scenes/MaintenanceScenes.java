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
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry;

/** Plant operation and maintenance: wear, the refuelling outage, the Maintenance Kit, earthquakes and the incident log. */
public final class MaintenanceScenes {

    private MaintenanceScenes() {}

    /**
     * The 5x5x5 fission reactor with its lowest corner at (1,1,1), its fuel port in the middle of the
     * west wall (1,3,3) between its coolant input (1,3,2) and steam output (1,3,4), all facing west;
     * a seismic detector on the floor at (7,1,2).
     */
    public static void maintenance(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.MAINTENANCE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.85f);

        BlockPos port = util.grid().at(1, 3, 3);
        BlockPos lid = util.grid().at(3, 5, 3);
        BlockPos detector = util.grid().at(7, 1, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 5, 5), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(lid, Direction.UP))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showOutline(PonderPalette.GREEN, "outage", util.select().fromTo(1, 1, 1, 5, 5, 5), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(port, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);
        scene.overlay().showControls(util.vector().blockSurface(port, Direction.WEST), Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(AllNuclearItems.URANIUM_REACTOR_FUEL.get()));
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(port.above(), Direction.WEST), Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(MaintenanceRegistry.MAINTENANCE_KIT.get()));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(port.above(), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showControls(util.vector().blockSurface(port.below(), Direction.WEST), Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(IsotopeRegistry.RADIOGRAPHY_CAMERA.get()));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(port.below(), Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(detector), Direction.DOWN);
        scene.idle(15);
        scene.effects().indicateRedstone(detector);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(detector))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
    }
}
