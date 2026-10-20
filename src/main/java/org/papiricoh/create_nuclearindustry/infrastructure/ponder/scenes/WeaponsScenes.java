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
        scene.scaleSceneView(0.75f);
        scene.world().setBlocks(util.select().fromTo(0, 1, 0, 4, 7, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            scene.world().setBlock(util.grid().at(x, 0, z), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WALL.get().defaultBlockState(), false);
            if (x == 0 || z == 4) for (int y = 1; y <= 4; y++)
                scene.world().setBlock(util.grid().at(x, y, z), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WALL.get().defaultBlockState(), false);
        }
        BlockPos controller = util.grid().at(2, 0, 0);
        BlockPos port = util.grid().at(0, 1, 2);
        scene.world().setBlock(controller, org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.CONTROLLER.get().defaultBlockState(), false);
        scene.world().setBlock(port, org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.FUEL_PORT.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 1, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 2, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.TANK.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 3, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.GUIDANCE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 4, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WARHEAD.get().defaultBlockState(), false);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 0, 4, 4, 4), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(controller)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().centerOf(2, 2, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().blockSurface(port, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().centerOf(2, 3, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(controller)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
    }

    public static void siloLaunch(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.SILO_LAUNCH;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.75f);
        scene.world().setBlocks(util.select().fromTo(0, 1, 0, 4, 7, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            scene.world().setBlock(util.grid().at(x, 0, z), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WALL.get().defaultBlockState(), false);
            if (x == 0 || z == 4) for (int y = 1; y <= 4; y++)
                scene.world().setBlock(util.grid().at(x, y, z), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WALL.get().defaultBlockState(), false);
        }
        BlockPos controller = util.grid().at(2, 0, 0);
        scene.world().setBlock(controller, org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.CONTROLLER.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 1, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.ENGINE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 2, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.TANK.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 3, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.GUIDANCE.get().defaultBlockState(), false);
        scene.world().setBlock(util.grid().at(2, 4, 2), org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.WARHEAD.get().defaultBlockState(), false);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 0, 4, 4, 4), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(controller)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(controller)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(controller)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(controller)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.world().hideSection(util.select().fromTo(2, 1, 2, 2, 4, 2), Direction.UP);
        scene.effects().emitParticles(util.vector().centerOf(2, 1, 2), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, new Vec3(0, 0.2, 0)), 4, 20);
        scene.idle(30);
    }

    public static void radar(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.RADAR;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 5);
        scene.world().setBlocks(util.select().fromTo(0, 1, 0, 4, 5, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        BlockPos radar = util.grid().at(2, 1, 2);
        BlockPos scope = util.grid().at(3, 1, 4);
        scene.world().setBlock(radar, org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry.RADAR.get().defaultBlockState(), false);
        scene.world().setBlock(scope, org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry.SCOPE.get().defaultBlockState(), false);
        scene.showBasePlate();
        scene.world().showSection(util.select().position(radar), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(radar), 32);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(radar)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(radar)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(radar)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.world().showSection(util.select().position(scope), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().blockSurface(scope, Direction.NORTH)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
    }

    /** The command table: nine pieces fuse into one, then a radar and a silo controller are linked to it. */
    public static void holoTable(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.HOLO_TABLE;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 5);
        BlockPos centre = util.grid().at(2, 1, 2);
        BlockPos radar = util.grid().at(4, 1, 0);
        BlockPos controller = util.grid().at(0, 1, 4);
        var table = org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry.TABLE.get().defaultBlockState();
        scene.showBasePlate();
        scene.idle(10);
        // Loose pieces first, then the fused table. The schematic's centre holds the table's block entity, which a loose
        // piece may not: clear it first.
        scene.world().setBlock(centre, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        for (int i = 0; i < org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid.PARTS; i++) {
            BlockPos at = centre.offset(org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid.dx(i), 0,
                    org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid.dz(i));
            scene.world().setBlock(at, table, false);
            scene.world().showSection(util.select().position(at), Direction.DOWN);
            scene.idle(3);
        }
        scene.overlay().showText(90).text(lines.next()).pointAt(util.vector().topOf(centre)).placeNearTarget().attachKeyFrame();
        scene.idle(30);
        for (int i = 0; i < org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid.PARTS; i++) {
            BlockPos at = centre.offset(org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid.dx(i), 0,
                    org.papiricoh.create_nuclearindustry.missile.holo.HoloGrid.dz(i));
            scene.world().setBlock(at, table.setValue(org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlock.PART,
                    org.papiricoh.create_nuclearindustry.missile.holo.HoloPart.of(i)), true);
        }
        scene.idle(70);
        scene.world().showSection(util.select().position(radar), Direction.DOWN);
        scene.world().showSection(util.select().position(controller), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(radar), 32);
        scene.idle(10);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(radar)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(centre)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100).text(lines.next()).pointAt(util.vector().topOf(centre.offset(-1, 0, 1))).placeNearTarget().attachKeyFrame();
        scene.idle(110);
    }

    private WeaponsScenes() {}
}
