package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

/** The Radiation Scrubber on contaminated ground: rotation in, clean ground and low-level waste out. */
public class ScrubberScenes {

    public static void scrubber(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("radiation_scrubber", "Radiation Scrubbers");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos motor = util.grid().at(2, 1, 2);
        BlockPos scrubber = util.grid().at(2, 2, 2);
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 4, 0, 4), Blocks.GRASS_BLOCK.defaultBlockState(), false);
        scene.world().setBlock(motor, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"))
                .defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), false);
        scene.world().setBlock(scrubber, AllNuclearBlocks.RADIATION_SCRUBBER.get().defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(5);
        for (int[] spot : new int[][]{{0, 0}, {4, 1}, {1, 4}, {3, 3}, {0, 3}, {4, 4}}) {
            scene.effects().emitParticles(util.vector().topOf(util.grid().at(spot[0], 0, spot[1])),
                    scene.effects().simpleParticleEmitter(AllNuclearParticles.RADIOACTIVE_DUST.get(), new Vec3(0, 0.02, 0)), 1, 150);
        }
        scene.overlay().showText(70)
                .text("Fallout leaves the ground contaminated: green dust rises from it, and standing on it irradiates.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(util.grid().at(3, 0, 3)))
                .placeNearTarget();
        scene.idle(80);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(motor), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(scrubber), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 2, 2, 2), 256);
        scene.overlay().showText(100)
                .text("A Radiation Scrubber draws it out of the ground of its chunk and the eight around it, the worst first. Drive it from below: it cleans faster up to 256 RPM.")
                .pointAt(util.vector().topOf(scrubber))
                .placeNearTarget();
        scene.idle(110);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().blockSurface(scrubber, Direction.WEST), Pointing.LEFT, 60)
                .withItem(new ItemStack(WasteRegistry.LOW_LEVEL_WASTE.get()));
        scene.overlay().showText(100)
                .text("What it absorbs comes out as Low-Level Waste, for a Compacted Waste Drum. Take it out with a funnel or hopper: full, it stops, and the waste inside irradiates.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(scrubber, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
    }
}
