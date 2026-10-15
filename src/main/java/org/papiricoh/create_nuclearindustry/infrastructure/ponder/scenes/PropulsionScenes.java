package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.block.RocketNozzleBlock;

/** The nuclear engines for Create Aeronautics ships, on their test stands. */
public final class PropulsionScenes {

    private PropulsionScenes() {}

    /**
     * The thermal engine along x with its exhaust east: the controller at (1,1,2), three heat
     * exchanger segments (2..4,1,2), the nozzle at (5,1,2) and a lever on the controller (1,2,2).
     */
    public static void thermal(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.THERMAL_ENGINE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);

        BlockPos controller = util.grid().at(1, 1, 2);
        BlockPos nozzle = util.grid().at(5, 1, 2);
        BlockPos lever = util.grid().at(1, 2, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(controller), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().position(nozzle), Direction.WEST);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "engine", util.select().fromTo(1, 1, 2, 5, 1, 2), 100);
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);

        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(controller, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 2)))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(lever), Direction.DOWN);
        scene.idle(10);
        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.effects().indicateRedstone(lever);
        scene.world().modifyBlock(nozzle, state -> state.setValue(RocketNozzleBlock.LIT, true), false);
        jet(scene, nozzle, false);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(nozzle, Direction.EAST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(nozzle))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);
        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.world().modifyBlock(nozzle, state -> state.setValue(RocketNozzleBlock.LIT, false), false);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.RIGHT, 40).rightClick()
                .withItem(new ItemStack(MaintenanceRegistry.MAINTENANCE_KIT.get()));
        scene.idle(60);
    }

    /**
     * The antimatter engine along x with its exhaust east: the controller at (1,1,2), four magnetic
     * coils (2..5,1,2), the magnetic nozzle at (6,1,2) and a creative RF generator on the controller.
     */
    public static void antimatter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ANTIMATTER_ENGINE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.85f);

        BlockPos controller = util.grid().at(1, 1, 2);
        BlockPos nozzle = util.grid().at(6, 1, 2);
        BlockPos generator = util.grid().at(1, 2, 2);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(controller), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 5, 1, 2), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().position(nozzle), Direction.WEST);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().showSection(util.select().position(generator), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.RIGHT, 50).rightClick()
                .withItem(new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get()));
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(generator))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().modifyBlock(nozzle, state -> state.setValue(RocketNozzleBlock.LIT, true), false);
        jet(scene, nozzle, true);
        scene.overlay().showOutline(PonderPalette.GREEN, "coils", util.select().fromTo(2, 1, 2, 5, 1, 2), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 2)))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(nozzle, Direction.EAST))
                .placeNearTarget();
        scene.idle(100);

        scene.world().hideSection(util.select().position(generator), Direction.UP);
        scene.world().modifyBlock(nozzle, state -> state.setValue(RocketNozzleBlock.LIT, false), false);
        scene.idle(10);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
    }

    /** A short burst of exhaust out of the nozzle's open face. */
    private static void jet(CreateSceneBuilder scene, BlockPos nozzle, boolean magnetic) {
        Vec3 mouth = Vec3.atCenterOf(nozzle).add(0.6, 0, 0);
        scene.effects().emitParticles(mouth, scene.effects().simpleParticleEmitter(
                magnetic ? ParticleTypes.END_ROD : ParticleTypes.CLOUD, new Vec3(0.4, 0, 0)), 3, 60);
    }
}
