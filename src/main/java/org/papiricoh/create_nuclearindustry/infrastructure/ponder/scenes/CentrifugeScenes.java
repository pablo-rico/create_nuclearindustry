package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;

/**
 * Centrifuge and electrolyzer share a layout: the machine stands one block up so all three
 * fluid connections can be seen from the camera, a pipe on top (feed), at the front
 * (product) and underneath (tails), with the drive shaft coming in from behind.
 */
public class CentrifugeScenes {
    private static final int MACHINE_Y = 2;

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = machine(builder, util, "centrifuge", "Enriching uranium with Centrifuges", AllNuclearBlocks.CENTRIFUGE.get());
        BlockPos machine = util.grid().at(2, MACHINE_Y, 2);

        scene.overlay().showText(80)
                .text("The Centrifuge separates uranium hexafluoride by enrichment. It turns through its back, at 32 RPM or more.")
                .pointAt(util.vector().topOf(util.grid().at(2, MACHINE_Y, 3)))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        showPipe(scene, util, util.grid().at(2, MACHINE_Y + 1, 2), Direction.DOWN);
        scene.overlay().showText(70)
                .text("Pipe UF6 into the top or the sides.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().topOf(util.grid().at(2, MACHINE_Y + 1, 2)))
                .placeNearTarget();
        scene.idle(80);

        showPipe(scene, util, util.grid().at(2, MACHINE_Y, 1), Direction.SOUTH);
        scene.overlay().showText(70)
                .text("The enriched product leaves through the front...")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(util.grid().at(2, MACHINE_Y, 1), Direction.WEST))
                .placeNearTarget();
        scene.idle(80);

        showPipe(scene, util, util.grid().at(2, MACHINE_Y - 1, 2), Direction.UP);
        scene.overlay().showText(70)
                .text("...and the depleted tails through the bottom.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(util.grid().at(2, MACHINE_Y - 1, 2), Direction.WEST))
                .placeNearTarget();
        scene.idle(80);
        scene.addKeyframe();

        scene.overlay().showText(110)
                .text("Chain centrifuges into a cascade: natural to LEU, LEU to HEU, HEU to weapons grade. Pipe each stage's tails back into the stage before it.")
                .pointAt(util.vector().centerOf(machine))
                .placeNearTarget();
        scene.idle(120);
        scene.overlay().showText(70)
                .text("It also separates Lithium-6 from lithium.")
                .pointAt(util.vector().centerOf(machine))
                .placeNearTarget();
        scene.idle(80);
    }

    public static void electrolysis(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = machine(builder, util, "electrolyzer", "Electrolysis", AllNuclearBlocks.ELECTROLYZER.get());
        BlockPos machine = util.grid().at(2, MACHINE_Y, 2);

        scene.overlay().showText(80)
                .text("The Electrolyzer uses rotation in place of electric current, at 32 RPM or more.")
                .pointAt(util.vector().topOf(util.grid().at(2, MACHINE_Y, 3)))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        showPipe(scene, util, util.grid().at(2, MACHINE_Y + 1, 2), Direction.DOWN);
        scene.overlay().showText(100)
                .text("Fed water, it slowly concentrates heavy water. Heavy water then splits into deuterium gas.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().topOf(util.grid().at(2, MACHINE_Y + 1, 2)))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(80)
                .text("It also reduces lithium carbonate to lithium metal.")
                .pointAt(util.vector().centerOf(machine))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        showPipe(scene, util, util.grid().at(2, MACHINE_Y, 1), Direction.SOUTH);
        showPipe(scene, util, util.grid().at(2, MACHINE_Y - 1, 2), Direction.UP);
        scene.overlay().showText(80)
                .text("Fluids leave through the front, items through the bottom.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(util.grid().at(2, MACHINE_Y, 1), Direction.WEST))
                .placeNearTarget();
        scene.idle(90);
    }

    private static CreateSceneBuilder machine(SceneBuilder builder, SceneBuildingUtil util, String id, String title, Block block) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(id, title);
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos machine = util.grid().at(2, MACHINE_Y, 2);
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 4, 0, 4), Blocks.ANDESITE.defaultBlockState(), false);
        scene.world().setBlock(machine, block.defaultBlockState().setValue(KineticProcessingBlock.FACING, Direction.NORTH), false);
        // Drive shaft from behind, on casing supports.
        scene.world().setBlock(util.grid().at(2, MACHINE_Y, 3),
                create("shaft").defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlock(util.grid().at(2, MACHINE_Y, 4),
                create("cogwheel").defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z), false);
        scene.world().setBlocks(util.select().fromTo(2, 1, 3, 2, 1, 4), create("andesite_casing").defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 4), Direction.UP);
        scene.world().showSection(util.select().fromTo(2, MACHINE_Y, 2, 2, MACHINE_Y, 4), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(2, MACHINE_Y, 2, 2, MACHINE_Y, 4), 64);
        scene.idle(20);
        return scene;
    }

    /** Places a straight Create fluid pipe connected towards {@code toMachine} and shows it. */
    private static void showPipe(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Direction toMachine) {
        BlockState pipe = create("fluid_pipe").defaultBlockState();
        for (Direction direction : Direction.values()) {
            pipe = pipe.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), direction.getAxis() == toMachine.getAxis());
        }
        scene.world().setBlock(pos, pipe, false);
        scene.world().showSection(util.select().position(pos), toMachine.getOpposite());
        scene.idle(15);
    }

    private static Block create(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }
}
