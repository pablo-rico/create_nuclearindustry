package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;

/**
 * A cut through the ground: a waste barrel at the surface contaminates its surroundings; the
 * same barrel deep in a gallery below Y=0 is inert.
 */
public class WasteStorageScenes {

    public static void storage(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("waste_storage", "Geological Waste Storage");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.65f);
        scene.setSceneOffsetY(-3.0f);

        Selection ground = util.select().fromTo(0, 0, 0, 6, 10, 6);
        scene.world().showSection(ground, Direction.UP);
        scene.idle(20);

        BlockPos surfaceBarrel = util.grid().at(2, 11, 4);
        scene.world().setBlock(surfaceBarrel, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(surfaceBarrel), Direction.DOWN);
        scene.idle(10);
        scene.effects().emitParticles(util.vector().topOf(util.grid().at(2, 10, 4)),
                scene.effects().simpleParticleEmitter(AllNuclearParticles.RADIOACTIVE_DUST.get(), new Vec3(0, 0.02, 0)), 3, 160);
        scene.overlay().showText(90)
                .text("At the surface a Waste Barrel irradiates whoever comes close, and slowly leaks into the ground around it.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(surfaceBarrel))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        // The north face is cut away: turn a little so the gallery shows.
        PonderCamera.turn(scene, -30);
        Selection depth = util.select().fromTo(0, 3, 0, 6, 3, 0);
        scene.overlay().showOutline(PonderPalette.WHITE, "depth", depth, 90);
        scene.overlay().showText(90)
                .text("Waste stored at Y=0 or lower counts as geologically stored.")
                .pointAt(util.vector().blockSurface(util.grid().at(0, 3, 0), Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().hideSection(util.select().position(surfaceBarrel), Direction.UP);
        scene.idle(10);
        Selection stored = util.select().fromTo(1, 1, 1, 4, 1, 1).add(util.select().fromTo(1, 1, 2, 2, 1, 2));
        scene.world().setBlocks(stored, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState(), false);
        scene.world().showSection(stored, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text("Down there barrels neither irradiate nor leak. Dig a deep gallery, fill it and seal it off: only the depth matters.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 1)))
                .placeNearTarget();
        scene.idle(110);
        PonderCamera.turn(scene, 30);
    }
}
