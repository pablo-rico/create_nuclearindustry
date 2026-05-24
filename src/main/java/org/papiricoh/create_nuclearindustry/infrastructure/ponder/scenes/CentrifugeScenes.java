package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;

public class CentrifugeScenes {
    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("centrifuge", "Processing uranium with a Centrifuge");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos centrifuge = util.grid().at(2, 1, 2);
        BlockPos shaft = util.grid().at(3, 1, 2);
        BlockPos cogwheel = util.grid().at(4, 1, 2);

        scene.world().setBlocks(
                util.select().fromTo(0, 0, 0, 4, 0, 4),
                Blocks.ANDESITE.defaultBlockState(),
                false
        );
        scene.world().setBlock(
                centrifuge,
                AllNuclearBlocks.CENTRIFUGE.get().defaultBlockState().setValue(CentrifugeBlock.FACING, Direction.NORTH),
                false
        );
        scene.world().setBlock(
                shaft,
                block("create", "shaft").defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X),
                false
        );
        scene.world().setBlock(
                cogwheel,
                block("create", "cogwheel").defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X),
                false
        );

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("The Centrifuge is a kinetic machine for refining uranium.")
                .pointAt(util.vector().topOf(centrifuge))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .text("It accepts rotational power from any horizontal side.")
                .pointAt(util.vector().centerOf(shaft))
                .placeNearTarget();
        scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 4, 1, 2), 32);
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(centrifuge, Direction.WEST), Pointing.RIGHT, 50)
                .rightClick()
                .withItem(new ItemStack(AllNuclearItems.RAW_URANIUM.get()));
        scene.overlay().showText(70)
                .text("Right-click with Raw Uranium or Uranium to insert one item.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(centrifuge, Direction.WEST))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(80)
                .text("At 16 RPM or faster, the Centrifuge processes the input over time.")
                .pointAt(util.vector().centerOf(centrifuge))
                .placeNearTarget();
        scene.idle(90);

        scene.world().createItemEntity(
                util.vector().blockSurface(centrifuge, Direction.DOWN).add(0, -0.25, 0),
                util.vector().of(0, -0.05, 0),
                new ItemStack(AllNuclearItems.URANIUM.get())
        );
        scene.overlay().showText(70)
                .text("Extract finished uranium from the bottom, or right-click with an empty hand.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(centrifuge, Direction.DOWN))
                .placeNearTarget();
        scene.idle(80);
    }

    private static Block block(String namespace, String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
