package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
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
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;

/** The RTG on a small plate: loading, its steady output, and walling it in. */
public class RtgScenes {

    public static void rtg(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("rtg", "Radioisotope Generators");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos rtg = util.grid().at(1, 1, 2);
        BlockPos shaft = util.grid().at(2, 1, 2);
        BlockPos cog = util.grid().at(3, 1, 2);
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 4, 0, 4), Blocks.SMOOTH_STONE.defaultBlockState(), false);
        scene.world().setBlock(rtg, AllNuclearBlocks.RTG.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST), false);
        scene.world().setBlock(shaft, create("shaft").defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(cog, create("cogwheel").defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X), false);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(rtg), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.WEST);
        scene.idle(15);

        scene.overlay().showControls(util.vector().topOf(rtg), Pointing.DOWN, 50).rightClick()
                .withItem(new ItemStack(AllNuclearItems.PLUTONIUM_RTG_PELLET.get()));
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(1, 1, 2, 3, 1, 2), 16);
        scene.overlay().showText(90)
                .text("A Radioisotope Generator turns the decay heat of a pellet into a steady 16 RPM on its front shaft. Right-click with a pellet, or feed it by hopper.")
                .pointAt(util.vector().topOf(rtg))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.overlay().showText(110)
                .text("Plutonium pellets give 256 stress capacity per RPM and lose half of it every 10 in-game days; waste pellets give 64 and halve every 2. Below a quarter they come out spent.")
                .pointAt(util.vector().centerOf(cog))
                .placeNearTarget();
        scene.idle(120);
        scene.addKeyframe();

        // It irradiates its surroundings: a depleted uranium wall stops 90%.
        scene.overlay().showOutline(PonderPalette.RED, "rtg", util.select().position(rtg), 80);
        scene.overlay().showText(80)
                .text("It irradiates whoever stands near it...")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(rtg, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);
        scene.world().setBlocks(util.select().fromTo(0, 1, 1, 0, 2, 3).add(util.select().fromTo(1, 2, 1, 1, 2, 3))
                .add(util.select().position(1, 1, 1)).add(util.select().position(1, 1, 3)),
                AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get().defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(0, 1, 1, 0, 2, 3).add(util.select().fromTo(1, 2, 1, 1, 2, 3))
                .add(util.select().position(1, 1, 1)).add(util.select().position(1, 1, 3)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("...so wall it in. A Block of Depleted Uranium lets only a tenth of the radiation through.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(1, 2, 2)))
                .placeNearTarget();
        scene.idle(100);
    }

    private static net.minecraft.world.level.block.Block create(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }
}
