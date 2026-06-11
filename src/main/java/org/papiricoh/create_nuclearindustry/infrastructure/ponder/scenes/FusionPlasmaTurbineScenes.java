package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionPlasmaTurbineBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionTurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

public class FusionPlasmaTurbineScenes {
    public static void generation(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("fusion_plasma_turbine", tr("header"));
        scene.configureBasePlate(0, 0, 6);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos casing = util.grid().at(1, 1, 3);
        BlockPos rotor = util.grid().at(2, 1, 3);
        BlockPos turbine = util.grid().at(3, 1, 3);
        BlockPos shaft = util.grid().at(4, 1, 3);
        BlockPos plasmaPort = util.grid().at(1, 1, 2);
        BlockPos condensatePort = util.grid().at(1, 1, 4);

        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 5, 0, 4),
                Blocks.ANDESITE.defaultBlockState(), false);
        scene.world().setBlock(casing,
                AllNuclearBlocks.FUSION_TURBINE_CASING.get().defaultBlockState(), false);
        scene.world().setBlock(rotor,
                AllNuclearBlocks.FUSION_TURBINE_ROTOR.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(turbine,
                AllNuclearBlocks.FUSION_PLASMA_TURBINE.get().defaultBlockState()
                        .setValue(FusionPlasmaTurbineBlock.FACING, Direction.EAST), false);
        scene.world().setBlock(shaft,
                block("create", "shaft").defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(plasmaPort,
                AllNuclearBlocks.FUSION_TURBINE_CASING.get().defaultBlockState(), false);
        scene.world().setBlock(condensatePort,
                AllNuclearBlocks.FUSION_TURBINE_CASING.get().defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().position(casing), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(tr(1))
                .pointAt(util.vector().topOf(casing))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(rotor), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(tr(2))
                .pointAt(util.vector().centerOf(rotor))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(turbine), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(tr(3))
                .pointAt(util.vector().topOf(turbine))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(shaft), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(2, 1, 3, 4, 1, 3), 64);
        scene.idle(15);
        scene.world().showSection(util.select().position(plasmaPort), Direction.EAST);
        scene.world().replaceBlocks(util.select().position(plasmaPort),
                AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT.get().defaultBlockState()
                        .setValue(FusionTurbineFluidPortBlock.FACING, Direction.NORTH)
                        .setValue(FusionTurbineFluidPortBlock.MODE, ReactorFluidPortMode.INPUT), false);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(tr(4))
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(plasmaPort, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(condensatePort), Direction.SOUTH);
        scene.world().replaceBlocks(util.select().position(condensatePort),
                AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT.get().defaultBlockState()
                        .setValue(FusionTurbineFluidPortBlock.FACING, Direction.SOUTH)
                        .setValue(FusionTurbineFluidPortBlock.MODE, ReactorFluidPortMode.OUTPUT), false);
        scene.idle(15);
        scene.overlay().showText(85)
                .text(tr(5))
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(condensatePort, Direction.SOUTH))
                .placeNearTarget();
        scene.idle(95);

        scene.overlay().showText(90)
                .text(tr(6))
                .pointAt(util.vector().blockSurface(shaft, Direction.EAST))
                .placeNearTarget();
        scene.idle(100);
    }

    private static Block block(String namespace, String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static String tr(String suffix) {
        return I18n.get("create_nuclearindustry.ponder.fusion_plasma_turbine." + suffix);
    }

    private static String tr(int index) {
        return tr("text_" + index);
    }
}
