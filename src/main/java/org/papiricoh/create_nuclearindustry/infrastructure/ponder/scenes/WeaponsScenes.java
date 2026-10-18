package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;

/**
 * Weapons: the nuclear, dirty and thermonuclear bombs and the missile launch pad, on the schematics of
 * tools/structgen/ponder_weapons.py. Nothing goes off: the scenes show what goes in and what
 * arms them, and the text tells the rest. The nuclear cannon shell has no scene, since it exists
 * only with Create Big Cannons. Every text box takes the next line of {@link GuidePonderText}.
 */
public class WeaponsScenes {

    public static void bombs(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.BOMBS;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.9f);

        BlockPos bomb = util.grid().at(2, 1, 2);
        BlockPos dirty = util.grid().at(4, 1, 2);
        BlockPos bombLever = util.grid().at(2, 1, 1);
        BlockPos dirtyLever = util.grid().at(4, 1, 1);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(bomb), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(bomb), Pointing.DOWN, 80)
                .withItem(new ItemStack(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get()));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(bomb, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(bomb))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(bombLever), Direction.SOUTH);
        scene.idle(15);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 1, 2, 1, 2));
        scene.effects().indicateRedstone(bombLever);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(bombLever, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(bomb))
                .placeNearTarget();
        scene.idle(100);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 1, 2, 1, 2));
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(4, 1, 1, 4, 1, 2), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(dirty), Pointing.DOWN, 80)
                .withItem(new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(dirty, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);
        scene.world().toggleRedstonePower(util.select().fromTo(4, 1, 1, 4, 1, 2));
        scene.effects().indicateRedstone(dirtyLever);
        scene.effects().emitParticles(util.vector().topOf(dirty),
                scene.effects().simpleParticleEmitter(ParticleTypes.HAPPY_VILLAGER, new Vec3(0, 0.05, 0)), 3, 20);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(dirty))
                .placeNearTarget();
        scene.idle(100);
        scene.world().toggleRedstonePower(util.select().fromTo(4, 1, 1, 4, 1, 2));
        scene.idle(10);
    }

    /** The thermonuclear bomb's three parts, one at a time, and its countdown. */
    public static void thermonuclear(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.THERMONUCLEAR;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9f);

        BlockPos bomb = util.grid().at(2, 1, 2);
        BlockPos lever = util.grid().at(2, 1, 1);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(bomb), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(bomb), Pointing.DOWN, 90)
                .withItem(new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()));
        scene.overlay().showText(110)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(bomb, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);
        scene.overlay().showControls(util.vector().topOf(bomb), Pointing.DOWN, 80)
                .withItem(new ItemStack(ExplosiveRegistry.LITHIUM_DEUTERIDE.get(), 16));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(bomb, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showControls(util.vector().topOf(bomb), Pointing.DOWN, 80)
                .withItem(new ItemStack(AllNuclearItems.DEPLETED_URANIUM_BLOCK.get(), 4));
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(bomb, Direction.WEST))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().position(lever), Direction.SOUTH);
        scene.idle(15);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 1, 2, 1, 2));
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(lever, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(80)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(bomb))
                .placeNearTarget();
        scene.idle(90);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 1, 2, 1, 2));
        scene.idle(10);
    }

    public static void missile(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.MISSILE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9f);

        BlockPos pad = util.grid().at(2, 1, 2);
        BlockPos lever = util.grid().at(2, 1, 1);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(pad), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(pad))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(util.grid().at(0, 0, 4)), Pointing.DOWN, 40)
                .rightClick().withItem(new ItemStack(MissileRegistry.TARGET_DESIGNATOR.get()));
        scene.idle(45);
        scene.overlay().showControls(util.vector().topOf(pad), Pointing.DOWN, 50)
                .withItem(new ItemStack(MissileRegistry.MISSILE_ITEM.get()));
        scene.world().modifyBlockEntity(pad, LaunchPadBlockEntity.class, launchPad -> launchPad.getInventory()
                .setStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT, new ItemStack(MissileRegistry.MISSILE_ITEM.get())));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(pad, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
        scene.overlay().showControls(util.vector().topOf(pad), Pointing.DOWN, 80)
                .withItem(new ItemStack(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(pad, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.world().showSection(util.select().position(lever), Direction.SOUTH);
        scene.idle(15);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 1, 2, 1, 2));
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(lever, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);
        // Liftoff: the missile leaves the pad in a plume of smoke.
        scene.world().modifyBlockEntity(pad, LaunchPadBlockEntity.class, launchPad -> launchPad.getInventory()
                .setStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT, ItemStack.EMPTY));
        scene.effects().emitParticles(util.vector().topOf(pad),
                scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.2, 0)), 4, 20);
        scene.idle(30);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 1, 2, 1, 2));
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(pad))
                .placeNearTarget();
        scene.idle(100);
    }

    private WeaponsScenes() {}
}
