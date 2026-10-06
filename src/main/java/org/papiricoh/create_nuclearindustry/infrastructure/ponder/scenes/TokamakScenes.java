package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

/**
 * Builds the R=4 tokamak of {@code ponder/tokamak.nbt} (tools/structgen/ponder_tokamak.py)
 * layer by layer. The controller, ports and magnet drive face the camera; the scene turns
 * to show the injector, and lifts the roof to show the plasma burning inside.
 */
public class TokamakScenes {
    private static final int RADIUS = 4;

    public static void assembly(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("tokamak", "Building a Tokamak");
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(0.55f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos axis = util.grid().at(7, 3, 7);
        BlockPos nearPlasma = util.grid().at(7, 3, 7 - RADIUS);
        BlockPos controller = util.grid().at(1, 3, 7);
        BlockPos coolantPort = util.grid().at(1, 4, 7);
        BlockPos steamPort = util.grid().at(1, 2, 7);
        BlockPos injector = util.grid().at(13, 3, 7);
        BlockPos magnet = util.grid().at(7, 3, 1);
        Selection chamber = plasmaChamber(util, axis);

        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(7, 1, 7, 7, 5, 7), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("A tokamak holds a ring of plasma with magnets. Start with a Central Solenoid column, five blocks tall, at its axis.")
                .pointAt(util.vector().topOf(axis.above(2)))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        scene.world().showSection(util.select().layer(1).substract(util.select().position(7, 1, 7)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("The torus has a 5x5 cross-section. Its floor and roof can hold Breeding Blankets, which turn fusion neutrons into tritium.")
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(util.grid().at(7, 1, 7 - RADIUS)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        // Walls go up without the roof, so the empty chamber stays in view.
        scene.world().showSection(util.select().fromTo(0, 2, 0, 14, 4, 14)
                .substract(util.select().fromTo(7, 2, 7, 7, 4, 7))
                .substract(util.select().fromTo(7, 3, 0, 7, 3, 1)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "chamber", chamber, 100);
        scene.overlay().showText(90)
                .text("Toroidal Field Coils line the walls. The 3x3 core of the ring must stay empty: it is the plasma chamber.")
                .pointAt(util.vector().centerOf(nearPlasma))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().layer(5).substract(util.select().position(7, 5, 7)), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("The ring may be 4 to 16 blocks from the solenoid to the plasma. A bigger ring holds more plasma and makes more power.")
                .pointAt(util.vector().topOf(util.grid().at(7, 5, 7 - RADIUS)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.RIGHT, 60).rightClick();
        scene.overlay().showText(90)
                .text("The Tokamak Controller sits on the outer equator. Right-click it to form the tokamak and open its control panel.")
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.INPUT, "coolant", util.select().position(coolantPort), 90);
        scene.overlay().showText(90)
                .text("A Fluid Port on INPUT takes water or heavy water as coolant...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(coolantPort, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.OUTPUT, "steam", util.select().position(steamPort), 90);
        scene.overlay().showText(90)
                .text("...and one on OUTPUT gives plasma steam for a Plasma Turbine.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(steamPort, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        // The injector is on the far (east) side.
        PonderCamera.showEast(scene);
        scene.overlay().showControls(util.vector().blockSurface(injector, Direction.EAST), Pointing.LEFT, 60)
                .rightClick()
                .withItem(new ItemStack(AllNuclearItems.DEUTERIUM_CELL.get()));
        scene.overlay().showText(100)
                .text("The Fuel Injector takes deuterium and tritium, by pipe or by hand with gas cells. On deuterium alone the plasma runs slowly and breeds its first tritium.")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(injector, Direction.EAST))
                .placeNearTarget();
        scene.idle(110);
        PonderCamera.backFromEast(scene);
        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(7, 3, 0, 7, 3, 1), Direction.SOUTH);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().fromTo(7, 3, 0, 7, 3, 1), 128);
        scene.overlay().showText(90)
                .text("Magnet Drives turn rotation into the confining field: one for every 4 blocks of radius, at 64 RPM or more.")
                .pointAt(util.vector().blockSurface(magnet, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        // Lift the roof and light the plasma.
        ElementLink<WorldSectionElement> roof = scene.world().makeSectionIndependent(
                util.select().layer(5).substract(util.select().position(7, 5, 7)));
        scene.world().moveSection(roof, util.vector().of(0, 3, 0), 20);
        scene.idle(20);
        scene.world().modifyBlock(controller, state -> state.setValue(FusionReactorControllerBlock.ON, true)
                .setValue(FusionReactorControllerBlock.IGNITED, true), false);
        Vec3 centre = util.vector().centerOf(axis);
        for (int step = 0; step < 16; step++) {
            double angle = step * Math.PI / 8;
            Vec3 point = centre.add(Math.cos(angle) * RADIUS, 0, Math.sin(angle) * RADIUS);
            scene.effects().emitParticles(point, scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD, Vec3.ZERO), 1, 8);
        }
        scene.overlay().showText(110)
                .text("Keep the coolant flowing. A tokamak without coolant overheats and disrupts, blowing holes in its vessel.")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().centerOf(nearPlasma))
                .placeNearTarget();
        scene.idle(120);
        scene.world().moveSection(roof, util.vector().of(0, -3, 0), 20);
        scene.idle(25);
    }

    /** Every plasma cell of the R=4 ring, for the outline. */
    private static Selection plasmaChamber(SceneBuildingUtil util, BlockPos axis) {
        TokamakGeometry geometry = TokamakGeometry.of(RADIUS);
        Selection chamber = util.select().position(axis.offset(geometry.plasmaX(0), geometry.plasmaY(0), geometry.plasmaZ(0)));
        for (int i = 1; i < geometry.plasmaCount(); i++) {
            chamber = chamber.add(util.select().position(axis.offset(geometry.plasmaX(i), geometry.plasmaY(i), geometry.plasmaZ(i))));
        }
        return chamber;
    }
}
