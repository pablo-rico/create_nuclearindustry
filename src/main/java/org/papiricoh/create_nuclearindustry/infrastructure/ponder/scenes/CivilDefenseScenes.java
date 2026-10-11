package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.LeafState;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlock;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;

/**
 * Civil defense: the two sirens, a blast door and a fallout shelter with a seismic detector, on
 * the schematics of tools/structgen/ponder_civil_defense.py. Every text box takes the next line
 * of {@link GuidePonderText}. Machines only get their looks here (siren and leaf states, a
 * comparator signal), so nothing depends on a running block entity.
 */
public class CivilDefenseScenes {

    public static void sirens(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.SIRENS;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 6);
        scene.scaleSceneView(0.9f);

        BlockPos airRaid = util.grid().at(1, 2, 2);
        BlockPos lever = util.grid().at(0, 1, 2);
        BlockPos crankSiren = util.grid().at(4, 2, 2);
        BlockPos crank = util.grid().at(4, 1, 1);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 2, 2), Direction.DOWN);
        scene.idle(20);

        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.effects().indicateRedstone(lever);
        sound(scene, airRaid, true);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(airRaid, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
        scene.world().toggleRedstonePower(util.select().position(lever));
        sound(scene, airRaid, false);
        scene.idle(10);

        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(airRaid))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);
        sound(scene, airRaid, true);
        scene.idle(100);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(airRaid, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        sound(scene, airRaid, false);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(4, 1, 1, 4, 2, 2), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().blockSurface(crank, Direction.NORTH), Pointing.DOWN, 40).rightClick();
        scene.world().setKineticSpeed(util.select().fromTo(4, 1, 1, 4, 2, 2), 48);
        sound(scene, crankSiren, true);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(crankSiren, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().setKineticSpeed(util.select().fromTo(4, 1, 1, 4, 2, 2), 128);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(crankSiren))
                .placeNearTarget();
        scene.idle(90);
        scene.world().setKineticSpeed(util.select().fromTo(4, 1, 1, 4, 2, 2), 0);
        sound(scene, crankSiren, false);
        scene.idle(20);
    }

    public static void blastDoor(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.BLAST_DOOR;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.8f);

        BlockPos controller = util.grid().at(2, 2, 2);
        BlockPos lever = util.grid().at(2, 2, 1);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(3, 2, 2, 5, 4, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 6, 5, 2).substract(util.select().fromTo(3, 2, 2, 5, 4, 2)),
                Direction.DOWN);
        scene.idle(20);
        scene.overlay().showOutline(PonderPalette.WHITE, "leaves", util.select().fromTo(3, 2, 2, 5, 4, 2), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(4, 3, 2), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.GREEN, "controller", util.select().position(controller), 80);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(controller, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 5, 2), Direction.EAST);
        scene.world().showSection(util.select().fromTo(7, 1, 2, 8, 5, 2), Direction.WEST);
        scene.world().showSection(util.select().position(lever), Direction.SOUTH);
        scene.idle(20);

        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(controller, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        // The column farthest from the pocket (the controller's side) clears first.
        for (int x = 5; x >= 3; x--) {
            leaves(scene, util, x, LeafState.OPEN);
            scene.idle(12);
        }
        scene.idle(64);

        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(controller, Direction.NORTH))
                .placeNearTarget();
        for (int x = 3; x <= 5; x++) {
            leaves(scene, util, x, LeafState.CLOSED);
            scene.idle(12);
        }
        scene.idle(74);

        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(4, 3, 2), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(util.grid().at(6, 3, 2), Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);
    }

    public static void shelter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.SHELTER;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.8f);

        BlockPos unit = util.grid().at(4, 2, 4);
        BlockPos detector = util.grid().at(2, 2, 4);
        BlockPos comparator = util.grid().at(2, 2, 3);
        BlockPos dust = util.grid().at(2, 2, 2);

        scene.showBasePlate();
        scene.idle(10);
        // The room is cut open on the camera's sides: no north or west wall and no roof.
        scene.world().showSection(util.select().fromTo(1, 1, 1, 6, 1, 6), Direction.UP);
        scene.world().showSection(util.select().fromTo(2, 2, 6, 6, 3, 6), Direction.NORTH);
        scene.world().showSection(util.select().fromTo(6, 2, 2, 6, 3, 5), Direction.WEST);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(4, 2, 4, 5, 2, 4), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().fromTo(4, 2, 4, 5, 2, 4), 32);
        scene.idle(20);

        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(unit, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showOutline(PonderPalette.WHITE, "walls", util.select().fromTo(2, 2, 6, 6, 3, 6)
                .add(util.select().fromTo(6, 2, 2, 6, 3, 5)), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(util.grid().at(5, 3, 6), Direction.NORTH))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 3)))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(unit))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().showSection(util.select().position(detector), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(detector, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().showSection(util.select().fromTo(2, 2, 2, 2, 2, 3), Direction.DOWN);
        scene.idle(15);
        // A far detonation: the ground shakes and the detector's signal jumps.
        scene.effects().emitParticles(util.vector().topOf(detector),
                scene.effects().simpleParticleEmitter(ParticleTypes.POOF, new Vec3(0, 0.05, 0)), 2, 6);
        scene.world().modifyBlock(comparator, state -> state.setValue(ComparatorBlock.POWERED, true), false);
        scene.world().modifyBlock(dust, state -> state.setValue(RedStoneWireBlock.POWER, 12), false);
        scene.effects().indicateRedstone(dust);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().topOf(comparator))
                .placeNearTarget();
        scene.idle(60);
        for (int power = 9; power >= 0; power -= 3) {
            int signal = power;
            scene.world().modifyBlock(dust, state -> state.setValue(RedStoneWireBlock.POWER, signal), false);
            scene.idle(10);
        }
        scene.world().modifyBlock(comparator, state -> state.setValue(ComparatorBlock.POWERED, false), false);
        scene.idle(20);
    }

    private static void sound(CreateSceneBuilder scene, BlockPos siren, boolean sounding) {
        scene.world().modifyBlock(siren, state -> state.setValue(AirRaidSirenBlock.SOUNDING, sounding), false);
    }

    private static void leaves(CreateSceneBuilder scene, SceneBuildingUtil util, int x, LeafState leaf) {
        for (int y = 2; y <= 4; y++) {
            scene.world().modifyBlock(util.grid().at(x, y, 2), state -> state.getBlock() instanceof BlastDoorBlock
                    ? state.setValue(BlastDoorBlock.LEAF, leaf) : state, false);
        }
    }

    private CivilDefenseScenes() {}
}
