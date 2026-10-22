package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationRegistry;

/** The Decontamination Sprinkler on contaminated ground: rotation and water in, contaminated water out, a shower; then sunflowers. */
public class SprinklerScenes {

    public static void sprinkler(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("decontamination_sprinkler", "Decontamination Sprinklers");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos motor = util.grid().at(2, 1, 2);
        BlockPos sprinkler = util.grid().at(2, 2, 2);
        BlockPos flower = util.grid().at(0, 1, 4);
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 4, 0, 4), Blocks.GRASS_BLOCK.defaultBlockState(), false);
        scene.world().setBlock(motor, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"))
                .defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), false);
        scene.world().setBlock(sprinkler, DecontaminationRegistry.SPRINKLER.get().defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(5);
        for (int[] spot : new int[][]{{0, 0}, {4, 1}, {1, 4}, {3, 3}, {0, 3}, {4, 4}}) {
            scene.effects().emitParticles(util.vector().topOf(util.grid().at(spot[0], 0, spot[1])),
                    scene.effects().simpleParticleEmitter(AllNuclearParticles.RADIOACTIVE_DUST.get(), new Vec3(0, 0.02, 0)), 1, 60);
        }
        scene.world().showSection(util.select().position(motor), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(sprinkler), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 2, 2, 2), 256);
        scene.overlay().showText(90)
                .text("A Decontamination Sprinkler washes fallout off the ground of its chunk and the eight around it. Drive it from below: it works faster up to 256 RPM.")
                .pointAt(util.vector().topOf(sprinkler))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().blockSurface(sprinkler, Direction.WEST), Pointing.LEFT, 60)
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showText(60)
                .text("It sprays water: pipe it in from any side.")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(sprinkler, Direction.WEST))
                .placeNearTarget();
        Vec3 head = util.vector().topOf(sprinkler);
        scene.effects().emitParticles(head, scene.effects().simpleParticleEmitter(ParticleTypes.SPLASH, new Vec3(0, 0.2, 0)), 4, 260);
        scene.idle(70);

        scene.overlay().showControls(util.vector().blockSurface(sprinkler, Direction.NORTH), Pointing.RIGHT, 60)
                .withItem(new ItemStack(AllNuclearFluids.CONTAMINATED_WATER_BUCKET.get()));
        scene.overlay().showText(90)
                .text("What it washes off collects as Contaminated Water. Pipe it out to a centrifuge: with its tank full, the sprinkler stops.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(sprinkler, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.overlay().showText(60)
                .text("Stand in its spray to wash your own dose off.")
                .pointAt(util.vector().centerOf(util.grid().at(3, 1, 2)))
                .placeNearTarget();
        scene.idle(70);
        scene.addKeyframe();

        scene.world().setBlock(flower, Blocks.SUNFLOWER.defaultBlockState(), false);
        scene.world().setBlock(flower.above(), Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        scene.world().showSection(util.select().fromTo(flower, flower.above()), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Sunflowers on contaminated ground do it slowly by themselves, until they wilt into Irradiated Sunflowers: their biomass is low-level waste.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(flower.above()))
                .placeNearTarget();
        scene.idle(50);
        scene.world().setBlock(flower, DecontaminationRegistry.IRRADIATED_SUNFLOWER.get().defaultBlockState(), true);
        scene.world().setBlock(flower.above(), DecontaminationRegistry.IRRADIATED_SUNFLOWER.get().defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        scene.idle(50);
        scene.overlay().showControls(util.vector().blockSurface(flower, Direction.WEST), Pointing.LEFT, 40)
                .withItem(new ItemStack(DecontaminationRegistry.IRRADIATED_BIOMASS.get()));
        scene.idle(50);
    }
}
