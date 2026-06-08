package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineOutputBlock;

/**
 * Escena Ponder de la turbina de vapor: muestra como ensamblar el multibloque en linea
 * [carcasa] - [rotor] - [salida] y como alimentarlo con vapor para generar rotacion.
 *
 * Igual que la centrifugadora, la estructura es plana (1 bloque de alto) y se construye en
 * runtime con {@code setBlocks}/{@code setBlock} sobre un schematic {@code turbine.nbt} con
 * tamano valido (size como ListTag); el storyboard la revela por pasos con {@code showSection}.
 */
public class TurbineScenes {
    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("turbine", "Assembling a Steam Turbine");
        scene.configureBasePlate(0, 0, 6);
        scene.scaleSceneView(0.75f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos casing = util.grid().at(1, 1, 2);
        BlockPos rotor = util.grid().at(2, 1, 2);
        BlockPos output = util.grid().at(3, 1, 2);
        BlockPos shaft = util.grid().at(4, 1, 2);
        BlockPos steamPort = util.grid().at(1, 1, 1);  // norte de la carcasa -> entrada de vapor
        BlockPos condPort = util.grid().at(1, 1, 3);   // sur de la carcasa   -> salida de condensado

        // Base + linea de la turbina (rotor/salida/shaft con su eje en X)
        scene.world().setBlocks(util.select().fromTo(0, 0, 0, 5, 0, 4),
                Blocks.ANDESITE.defaultBlockState(), false);
        scene.world().setBlock(casing,
                AllNuclearBlocks.TURBINE_CASING.get().defaultBlockState(), false);
        scene.world().setBlock(rotor,
                AllNuclearBlocks.TURBINE_ROTOR.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), false);
        scene.world().setBlock(output,
                AllNuclearBlocks.TURBINE_OUTPUT.get().defaultBlockState()
                        .setValue(TurbineOutputBlock.FACING, Direction.EAST), false);
        scene.world().setBlock(shaft,
                block("create", "shaft").defaultBlockState()
                        .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X), false);
        // Celdas de puerto: arrancan como carcasa y se "configuran" con replaceBlocks (efecto IN/OUT)
        scene.world().setBlock(steamPort,
                AllNuclearBlocks.TURBINE_CASING.get().defaultBlockState(), false);
        scene.world().setBlock(condPort,
                AllNuclearBlocks.TURBINE_CASING.get().defaultBlockState(), false);

        scene.showBasePlate();
        scene.idle(5);

        // 1: carcasa
        scene.world().showSection(util.select().position(casing), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("The Steam Turbine turns steam into rotational power. Start with a Turbine Casing.")
                .pointAt(util.vector().topOf(casing))
                .placeNearTarget();
        scene.idle(90);

        // 2: rotor
        scene.world().showSection(util.select().position(rotor), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("Place a Turbine Rotor in front of the casing, its axis along the line.")
                .pointAt(util.vector().centerOf(rotor))
                .placeNearTarget();
        scene.idle(90);

        // 3: salida (genera la rotacion) + arrancar giro
        scene.world().showSection(util.select().position(output), Direction.DOWN);
        scene.world().showSection(util.select().position(shaft), Direction.DOWN);
        scene.idle(15);
        scene.world().setKineticSpeed(util.select().fromTo(2, 1, 2, 4, 1, 2), 48);
        scene.overlay().showText(90)
                .text("Cap the line with a Turbine Output facing outward: it generates the rotation for your machines.")
                .pointAt(util.vector().blockSurface(output, Direction.UP))
                .placeNearTarget();
        scene.idle(100);

        // 4: puerto de entrada de vapor
        scene.world().showSection(util.select().position(steamPort), Direction.DOWN);
        scene.world().replaceBlocks(util.select().position(steamPort),
                AllNuclearBlocks.TURBINE_FLUID_PORT.get().defaultBlockState()
                        .setValue(TurbineFluidPortBlock.FACING, Direction.NORTH)
                        .setValue(TurbineFluidPortBlock.MODE, ReactorFluidPortMode.INPUT), false);
        scene.idle(15);
        scene.overlay().showText(85)
                .text("Feed steam through a Turbine Fluid Port set to INPUT...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(steamPort, Direction.NORTH))
                .placeNearTarget();
        scene.idle(95);

        // 5: puerto de salida de condensado
        scene.world().showSection(util.select().position(condPort), Direction.DOWN);
        scene.world().replaceBlocks(util.select().position(condPort),
                AllNuclearBlocks.TURBINE_FLUID_PORT.get().defaultBlockState()
                        .setValue(TurbineFluidPortBlock.FACING, Direction.SOUTH)
                        .setValue(TurbineFluidPortBlock.MODE, ReactorFluidPortMode.OUTPUT), false);
        scene.idle(15);
        scene.overlay().showText(85)
                .text("...and drain the condensate through another set to OUTPUT.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(condPort, Direction.SOUTH))
                .placeNearTarget();
        scene.idle(95);

        // 6: cierre
        scene.overlay().showText(90)
                .text("With steam flowing, the turbine forms on its own and the output shaft powers your contraptions.")
                .pointAt(util.vector().blockSurface(shaft, Direction.EAST))
                .placeNearTarget();
        scene.idle(100);
    }

    private static Block block(String namespace, String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
