package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFuelPortBlock;

/**
 * Escena Ponder anclada al Reactor Controller: ensena, paso a paso, como construir el
 * reactor nuclear multibloque.
 *
 * A la manera de Create, TODA la geometria del reactor vive en el schematic
 * {@code ponder/reactor_controller.nbt} (formato structure template vanilla, con
 * size/pos como ListTag) y se coloca con {@code StructureTemplate.placeInWorld} antes
 * de ejecutar este storyboard. Aqui solo se REVELA por capas con {@code showSection}
 * y se "configuran" los puertos con {@code replaceBlocks}. No se construye nada con
 * {@code setBlocks} (un NBT con size mal formado da limites (0,0,0) y recorta lo alto).
 */
public class ReactorScenes {

    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("reactor_controller", "Assembling a Nuclear Reactor");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.5f);
        scene.setSceneOffsetY(-1.0f);

        // Coords = coords del NBT (placeInWorld en BlockPos.ZERO). Huella 5x5 en x,z [1,5];
        // suelo y=1, paredes y=2..4, techo y=5, base de andesita en y=0.
        BlockPos controller = util.grid().at(3, 5, 3);
        BlockPos reactorCenter = util.grid().at(3, 3, 3);
        BlockPos coolantPort = util.grid().at(1, 3, 3);   // pared oeste -> entrada de refrigerante
        BlockPos steamPort = util.grid().at(5, 3, 3);     // pared este  -> salida de vapor
        BlockPos fuelPort = util.grid().at(3, 3, 1);      // pared norte -> carga de combustible

        scene.showBasePlate();
        scene.idle(10);

        // 1 + 2: carcasa hueca (suelo + 4 paredes; interior y techo aun ocultos)
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 5), Direction.UP);
        scene.idle(6);
        scene.world().showSection(util.select().fromTo(1, 2, 1, 1, 4, 5), Direction.EAST);   // pared oeste
        scene.world().showSection(util.select().fromTo(5, 2, 1, 5, 4, 5), Direction.WEST);   // pared este
        scene.world().showSection(util.select().fromTo(2, 2, 1, 4, 4, 1), Direction.SOUTH);  // pared norte
        scene.world().showSection(util.select().fromTo(2, 2, 5, 4, 4, 5), Direction.NORTH);  // pared sur
        scene.idle(25);
        scene.overlay().showText(80)
                .text("The reactor is a multiblock: raise a hollow shell of Reactor Casing.")
                .pointAt(util.vector().centerOf(reactorCenter))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("The width must be odd (3-9) and the height 4-12.")
                .pointAt(util.vector().centerOf(coolantPort))
                .placeNearTarget();
        scene.idle(90);

        // 3: columnas de uranio
        scene.world().showSection(util.select().fromTo(2, 2, 2, 2, 4, 2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 2, 4, 4, 4, 4), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("Inside, raise at least one column of Uranium Rods: the fuel.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(util.grid().at(2, 3, 2)))
                .placeNearTarget();
        scene.idle(90);

        // 4: intercambiadores de calor
        scene.world().showSection(util.select().fromTo(4, 2, 2, 4, 4, 2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(2, 2, 4, 2, 4, 4), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("Add at least one column of Heat Exchangers to carry the heat into the coolant.")
                .pointAt(util.vector().centerOf(util.grid().at(4, 3, 2)))
                .placeNearTarget();
        scene.idle(90);

        // 5: barras de control
        scene.world().showSection(util.select().fromTo(3, 2, 3, 3, 4, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("Leave air columns and drop in Control Rods to throttle the reaction.")
                .pointAt(util.vector().centerOf(reactorCenter))
                .placeNearTarget();
        scene.idle(90);

        // 6: puerto de refrigerante (INPUT) -> se "configura" la carcasa ya visible
        scene.world().replaceBlocks(util.select().position(coolantPort),
                AllNuclearBlocks.REACTOR_FLUID_PORT.get().defaultBlockState()
                        .setValue(ReactorFluidPortBlock.FACING, Direction.WEST)
                        .setValue(ReactorFluidPortBlock.MODE, ReactorFluidPortMode.INPUT),
                false);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Swap wall casing for a Reactor Fluid Port set to INPUT for coolant...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(coolantPort, Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        // 7: puerto de vapor (OUTPUT)
        scene.world().replaceBlocks(util.select().position(steamPort),
                AllNuclearBlocks.REACTOR_FLUID_PORT.get().defaultBlockState()
                        .setValue(ReactorFluidPortBlock.FACING, Direction.EAST)
                        .setValue(ReactorFluidPortBlock.MODE, ReactorFluidPortMode.OUTPUT),
                false);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("...and another set to OUTPUT for steam.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(steamPort, Direction.EAST))
                .placeNearTarget();
        scene.idle(90);

        // 8: puerto de combustible
        scene.world().replaceBlocks(util.select().position(fuelPort),
                AllNuclearBlocks.REACTOR_FUEL_PORT.get().defaultBlockState()
                        .setValue(ReactorFuelPortBlock.FACING, Direction.NORTH)
                        .setValue(ReactorFuelPortBlock.MODE, ReactorFluidPortMode.INPUT),
                false);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("A Reactor Fuel Port loads uranium fuel and ejects depleted fuel.")
                .pointAt(util.vector().blockSurface(fuelPort, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        // 9: techo + controlador
        scene.world().showSection(util.select().fromTo(1, 5, 1, 5, 5, 5), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("Cap the reactor and place the Reactor Controller at the centre of the top face.")
                .pointAt(util.vector().topOf(controller))
                .placeNearTarget();
        scene.idle(90);

        // 10: formar el reactor
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.UP), Pointing.DOWN, 60)
                .rightClick();
        scene.overlay().showText(80)
                .text("Right-click the controller to form the reactor.")
                .pointAt(util.vector().topOf(controller))
                .placeNearTarget();
        scene.idle(90);
    }
}
