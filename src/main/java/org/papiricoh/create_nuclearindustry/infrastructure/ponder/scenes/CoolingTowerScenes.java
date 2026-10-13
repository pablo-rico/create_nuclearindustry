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
import net.minecraft.world.phys.Vec3;

/**
 * Builds the base radius 4, 12 high tower of {@code ponder/cooling_tower.nbt}
 * (tools/structgen/ponder_cooling_tower.py) ring by ring. Controller, ports and fan face the
 * camera; the top half lifts off to show the hollow inside, and a plume rises once it runs.
 */
public class CoolingTowerScenes {

    public static void tower(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cooling_tower", "Cooling Towers");
        scene.configureBasePlate(0, 0, 11);
        scene.scaleSceneView(0.5f);
        scene.setSceneOffsetY(-1.0f);

        BlockPos controller = util.grid().at(1, 2, 5);
        BlockPos input = util.grid().at(2, 1, 2);
        BlockPos output = util.grid().at(3, 3, 3);
        BlockPos fan = util.grid().at(5, 2, 1);
        BlockPos shaft = util.grid().at(5, 2, 0);
        Selection parts = util.select().position(controller).add(util.select().position(input))
                .add(util.select().position(output)).add(util.select().position(fan)).add(util.select().position(shaft));

        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().layer(1).substract(parts), Direction.UP);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("A Cooling Tower stands on a full round floor of Cooling Tower Casing, 4 to 16 blocks in radius.")
                .pointAt(util.vector().topOf(util.grid().at(5, 1, 5)))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        scene.world().showSection(util.select().fromTo(0, 2, 0, 10, 4, 10).substract(parts), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 5, 0, 10, 8, 10), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 9, 0, 10, 12, 10), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(100)
                .text("Ring by ring, the wall narrows to a waist about three quarters of the way up, then flares out again. The top stays open.")
                .pointAt(util.vector().blockSurface(util.grid().at(3, 9, 5), Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("Its height may be from twice the radius plus 4 up to five times the radius. A bigger tower condenses more.")
                .pointAt(util.vector().topOf(util.grid().at(5, 12, 3)))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        // Lift the top half off to show the hollow inside.
        ElementLink<WorldSectionElement> top = scene.world().makeSectionIndependent(util.select().fromTo(0, 7, 0, 10, 12, 10));
        scene.world().moveSection(top, util.vector().of(0, 5, 0), 20);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.WHITE, "inside", util.select().fromTo(4, 2, 4, 6, 6, 6), 80);
        scene.overlay().showText(80)
                .text("The inside must stay empty.")
                .pointAt(util.vector().centerOf(util.grid().at(5, 4, 5)))
                .placeNearTarget();
        scene.idle(90);
        scene.world().moveSection(top, util.vector().of(0, -5, 0), 20);
        scene.idle(25);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(controller), Direction.EAST);
        scene.idle(10);
        scene.overlay().showControls(util.vector().blockSurface(controller, Direction.WEST), Pointing.RIGHT, 60).rightClick();
        scene.overlay().showText(90)
                .text("The Cooling Tower Controller goes in the bottom wall layer, centred on a side and facing out. Right-click it to form the tower.")
                .pointAt(util.vector().blockSurface(controller, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(input), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Fluid Ports go in the floor or the two wall layers above it. One on INPUT takes steam or heavy steam...")
                .colored(PonderPalette.INPUT)
                .pointAt(util.vector().blockSurface(input, Direction.NORTH))
                .placeNearTarget();
        scene.idle(100);
        scene.world().showSection(util.select().position(output), Direction.SOUTH);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("...and one on OUTPUT gives back the same amount of water or heavy water.")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().blockSurface(output, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        scene.addKeyframe();

        scene.world().showSection(util.select().position(fan).add(util.select().position(shaft)), Direction.SOUTH);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().position(fan).add(util.select().position(shaft)), 128);
        scene.overlay().showText(100)
                .text("Fans in the three bottom wall layers, facing out, add up to a quarter more each at 256 RPM, at most doubling the tower.")
                .pointAt(util.vector().blockSurface(fan, Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.addKeyframe();

        // Running: a plume rises from the open top while the camera swings round.
        Vec3 crown = util.vector().of(5.5, 12.6, 5.5);
        for (int i = 0; i < 6; i++) {
            Vec3 offset = new Vec3(Math.cos(i * Math.PI / 3) * 1.2, 0, Math.sin(i * Math.PI / 3) * 1.2);
            scene.effects().emitParticles(crown.add(offset),
                    scene.effects().simpleParticleEmitter(ParticleTypes.CLOUD, new Vec3(0, 0.12, 0)), 1, 140);
        }
        scene.overlay().showText(110)
                .text("It condenses half its base radius times its height in mB/t, 24 here. A turbine with an Exhaust Port sends it its spent steam instead of condensing it, and makes twice the power.")
                .pointAt(util.vector().topOf(util.grid().at(5, 12, 5)))
                .placeNearTarget();
        scene.idle(30);
        PonderCamera.showEast(scene);
        scene.idle(60);
        PonderCamera.backFromEast(scene);
        scene.idle(30);
    }
}
