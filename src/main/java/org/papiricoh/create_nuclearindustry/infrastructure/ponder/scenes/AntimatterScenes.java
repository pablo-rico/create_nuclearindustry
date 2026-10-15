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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.InjectorModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlock;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.heating.FusionViewportBlock;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Script;

/**
 * Antimatter containment, on the schematic of tools/structgen/ponder_antimatter.py, and its uses, the
 * annihilation generator and the antiproton injector, on those of tools/structgen/ponder_antimatter_use.py.
 * A Ponder world never runs a trap's, a generator's or an injector's physics, so the scenes set what
 * they show with their {@code preview} methods. Every text box takes the next line of {@link GuidePonderText}.
 */
public class AntimatterScenes {
    /** The whole R=4 tokamak of the injector scene, and a close look at the injector on its top corner. */
    private static final PonderZoom.View WHOLE_TOKAMAK = new PonderZoom.View(0.5f, 7, 7, -1.0f);
    private static final PonderZoom.View INJECTOR_CLOSE = new PonderZoom.View(1.5f, 2, 3, -4.5f);
    /** The generator scene, centred on the generator rather than the middle of its shaft. */
    private static final PonderZoom.View GENERATOR_VIEW = new PonderZoom.View(1.25f, 2, 2, -0.5f);

    /**
     * The three traps in a row at z 3, fronts north: generator (0,1,3), small trap (1,1,3), conduit
     * (2,1,3) with its generator behind (2,1,4), medium trap (3,1..2,3), generator (4,1,3), large
     * trap from (5,1,3).
     */
    public static void containment(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ANTIMATTER_CONTAINMENT;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 8);
        scene.scaleSceneView(0.9f);

        BlockPos small = util.grid().at(1, 1, 3);
        BlockPos conduit = util.grid().at(2, 1, 3);
        BlockPos medium = util.grid().at(3, 1, 3);
        BlockPos large = util.grid().at(5, 1, 3);
        BlockPos firstGenerator = util.grid().at(0, 1, 3);
        BlockPos secondGenerator = util.grid().at(4, 1, 3);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(small), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 3, 3, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(5, 1, 3, 6, 3, 4), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(medium.above(), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.WHITE, "small", util.select().position(small), 80);
        scene.overlay().showOutline(PonderPalette.WHITE, "medium", util.select().fromTo(3, 1, 3, 3, 2, 3), 80);
        scene.overlay().showOutline(PonderPalette.WHITE, "large", util.select().fromTo(5, 1, 3, 6, 3, 4), 80);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(large.above(2)))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(firstGenerator), Direction.DOWN);
        scene.world().showSection(util.select().position(secondGenerator), Direction.DOWN);
        scene.idle(15);
        // Fed: the field comes up, and the scene puts some antimatter in each trap.
        preview(scene, small, 12, TrapState.STABLE);
        preview(scene, medium, 110, TrapState.STABLE);
        preview(scene, large, 900, TrapState.STABLE);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(firstGenerator, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(small, Direction.NORTH).add(0, 0.4, 0))
                .placeNearTarget();
        scene.idle(40);
        preview(scene, medium, 110, TrapState.LOW_POWER);
        scene.idle(40);
        preview(scene, medium, 110, TrapState.STABLE);
        scene.idle(10);

        ItemStack vial = AntimatterRegistry.ANTIMATTER_VIAL.get().filled(4.0);
        scene.overlay().showControls(util.vector().blockSurface(small, Direction.NORTH), Pointing.DOWN, 50).rightClick()
                .withItem(vial);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(small, Direction.NORTH).add(0, -0.3, 0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);
        scene.overlay().showControls(util.vector().blockSurface(small, Direction.NORTH), Pointing.DOWN, 30)
                .withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
        scene.idle(40);

        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().blockSurface(small, Direction.NORTH).add(0, -0.3, 0))
                .placeNearTarget();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 4), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text(lines.next())
                .pointAt(util.vector().topOf(conduit))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        // The power goes: the medium and large traps run on their buffer, then quench.
        scene.world().hideSection(util.select().position(secondGenerator), Direction.UP);
        scene.idle(10);
        preview(scene, medium, 110, TrapState.LOW_POWER);
        preview(scene, large, 900, TrapState.LOW_POWER);
        scene.idle(25);
        for (int left = 60; left > 0; left -= 6) {
            int quench = left;
            scene.world().modifyBlockEntity(medium, PenningTrapBlockEntity.class, be -> be.preview(110, TrapState.QUENCHING, 0.3f, quench));
            scene.world().modifyBlockEntity(large, PenningTrapBlockEntity.class, be -> be.preview(900, TrapState.QUENCHING, 0.3f, quench));
            if (left == 60) {
                scene.overlay().showText(100)
                        .text(lines.next())
                        .colored(PonderPalette.RED)
                        .pointAt(util.vector().blockSurface(medium.above(), Direction.WEST))
                        .placeNearTarget()
                        .attachKeyFrame();
            }
            scene.idle(10);
        }

        // Annihilation: the medium trap goes, and its blast sets off the large one beside it.
        blast(scene, medium, 0.5, 1.0, 0.5);
        scene.world().destroyBlock(medium);
        scene.world().destroyBlock(medium.above());
        scene.world().modifyBlock(conduit, state -> state.setValue(AntimatterConduitBlock.SIDES.get(Direction.EAST), false), false);
        scene.idle(6);
        blast(scene, large, 1.0, 1.5, 1.0);
        for (BlockPos part : util.select().fromTo(5, 1, 3, 6, 3, 4)) {
            scene.world().destroyBlock(part);
        }
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().centerOf(util.grid().at(4, 2, 3)))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showOutline(PonderPalette.RED, "chain", util.select().fromTo(1, 1, 3, 2, 1, 3), 100);
        scene.overlay().showText(100)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(small))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
    }

    /**
     * The annihilation generator at (1,1,2), shaft east along (2..5,1,2) so its porthole looks west at
     * the camera; the small trap that feeds it south of it at (1,1,3), front west, and a creative RF
     * generator on the trap (1,2,3), which the scene takes away once the generator powers itself.
     */
    public static void generator(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ANTIMATTER_GENERATOR;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(GENERATOR_VIEW.scale());
        PonderZoom.start(scene, GENERATOR_VIEW);

        BlockPos generator = util.grid().at(1, 1, 2);
        BlockPos trap = util.grid().at(1, 1, 3);
        BlockPos creative = util.grid().at(1, 2, 3);
        Selection shafts = util.select().fromTo(2, 1, 2, 5, 1, 2);
        Selection kinetics = shafts.copy().add(util.select().position(generator));

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(generator), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(generator, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().position(trap), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(creative), Direction.DOWN);
        scene.idle(10);
        previewTrap(scene, trap, 20, TrapState.STABLE);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(trap, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(50);
        scene.overlay().showControls(util.vector().topOf(generator).add(0, 0, -0.2), Pointing.DOWN, 40).rightClick()
                .withItem(AntimatterRegistry.ANTIMATTER_VIAL.get().filled(4.0));
        scene.idle(50);

        scene.overlay().showOutline(PonderPalette.RED, "field", util.select().position(generator), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(creative))
                .placeNearTarget();
        scene.idle(100);

        // Fuelled and lit, with no redstone: the standby burn.
        previewGenerator(scene, generator, 0.8, 0, AnnihilationModel.Mode.ROTATION);
        scene.world().showSection(shafts, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(generator, Direction.WEST).add(0, 0.1, 0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        previewGenerator(scene, generator, 0.8, 1.0, AnnihilationModel.Mode.ROTATION);
        scene.world().setKineticSpeed(kinetics, 256);
        scene.effects().rotationSpeedIndicator(generator);
        scene.overlay().showControls(util.vector().topOf(generator).add(0, 0, 0.2), Pointing.DOWN, 40)
                .withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().topOf(generator))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        previewGenerator(scene, generator, 0.8, 1.0, AnnihilationModel.Mode.ELECTRIC);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.overlay().showOutline(PonderPalette.GREEN, "terminals", util.select().position(trap).add(util.select().position(generator)), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(generator, Direction.NORTH))
                .placeNearTarget();
        scene.idle(60);
        previewGenerator(scene, generator, 0.8, 1.0, AnnihilationModel.Mode.SPLIT);
        scene.world().setKineticSpeed(kinetics, 256);
        scene.idle(45);

        // The loop closes: the creative generator goes, and the trap keeps its field.
        scene.world().hideSection(util.select().position(creative), Direction.UP);
        scene.idle(15);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(trap, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        // Throttled back: the core dims with the output.
        previewGenerator(scene, generator, 0.8, 0.3, AnnihilationModel.Mode.SPLIT);
        scene.world().setKineticSpeed(kinetics, 128);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(generator, Direction.WEST))
                .placeNearTarget();
        scene.idle(110);
    }

    /**
     * The tokamak of tools/structgen/ponder_tokamak_startup.py with the antiproton injector in place of
     * a top corner of its shell (2,5,3), port west; a small trap north of it (2,5,2), front west, and a
     * creative RF generator on each (2,6,3), (2,6,2). The controller (1,3,7) shows the plasma.
     */
    public static void injector(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        PlantPonderText.Scene text = GuidePonderText.ANTIPROTON_INJECTOR;
        Script lines = text.script();
        scene.title(text.id(), text.title().english());
        scene.configureBasePlate(0, 0, 15);
        scene.scaleSceneView(WHOLE_TOKAMAK.scale());
        PonderZoom.start(scene, WHOLE_TOKAMAK);

        BlockPos injector = util.grid().at(2, 5, 3);
        BlockPos trap = util.grid().at(2, 5, 2);
        BlockPos controller = util.grid().at(1, 3, 7);
        BlockPos axis = util.grid().at(7, 3, 7);
        Selection extras = util.select().position(injector).add(util.select().position(trap))
                .add(util.select().fromTo(2, 6, 2, 2, 6, 3));
        Selection viewports = util.select().position(6, 4, 1).add(util.select().position(8, 4, 1));

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 14, 5, 14).substract(extras), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().position(injector), Direction.DOWN);
        PonderZoom.to(scene, INJECTOR_CLOSE, 25);
        scene.overlay().showOutline(PonderPalette.WHITE, "injector", util.select().position(injector), 90);
        scene.overlay().showText(90)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(injector, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().position(trap), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 6, 2, 2, 6, 3), Direction.DOWN);
        scene.idle(10);
        previewTrap(scene, trap, 24, TrapState.STABLE);
        previewInjector(scene, injector, 8, false, InjectorModel.Verdict.SAFE, false);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(trap, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showControls(util.vector().blockSurface(injector, Direction.WEST), Pointing.RIGHT, 40)
                .withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
        scene.idle(10);
        previewInjector(scene, injector, 8, true, InjectorModel.Verdict.NO_POWER, false);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.MEDIUM)
                .pointAt(util.vector().blockSurface(injector, Direction.WEST).add(0, 0.35, 0))
                .placeNearTarget();
        scene.idle(100);

        previewInjector(scene, injector, 8, true, InjectorModel.Verdict.WAITING, false);
        scene.overlay().showText(90)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(injector, Direction.WEST).add(0, 0.35, 0))
                .placeNearTarget();
        scene.idle(100);

        // Back out, and lift the roof (all but the injector and what feeds it) so the plasma shows.
        PonderZoom.to(scene, WHOLE_TOKAMAK, 25);
        ElementLink<WorldSectionElement> roof = scene.world().makeSectionIndependent(
                util.select().layer(5).substract(util.select().position(7, 5, 7)).substract(extras));
        scene.world().moveSection(roof, util.vector().of(0, 3, 0), 20);
        scene.idle(20);
        scene.world().modifyBlock(controller, state -> state.setValue(FusionReactorControllerBlock.ON, true), false);
        TokamakScenes.showPlasma(scene, controller, axis, TokamakScenes.plasma(TokamakPhase.HEATING, 2400, 0.6, 5));
        scene.world().modifyBlocks(viewports, state -> state.setValue(FusionViewportBlock.GLOW, 9), false);
        previewInjector(scene, injector, 8, true, InjectorModel.Verdict.READY, false);
        scene.overlay().showText(100)
                .text(lines.next())
                .pointAt(util.vector().blockSurface(injector, Direction.UP).add(0.3, 0, 0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        // The shot: the dose leaves the chamber and streaks down into the plasma, which ignites.
        previewInjector(scene, injector, 6, true, InjectorModel.Verdict.COOLDOWN, true);
        shot(scene, injector, axis);
        scene.idle(10);
        scene.world().modifyBlock(controller, state -> state.setValue(FusionReactorControllerBlock.IGNITED, true), false);
        TokamakScenes.showPlasma(scene, controller, axis, TokamakScenes.plasma(TokamakPhase.BURN, 4200, 0.85, 10));
        scene.world().modifyBlocks(viewports, state -> state.setValue(FusionViewportBlock.GLOW, 14), false);
        scene.idle(10);
        scene.overlay().showText(110)
                .text(lines.next())
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().centerOf(util.grid().at(3, 3, 4)))
                .placeNearTarget();
        scene.idle(120);
        scene.world().moveSection(roof, util.vector().of(0, -3, 0), 20);
        scene.idle(25);
    }

    private static void previewGenerator(CreateSceneBuilder scene, BlockPos generator, double ng, double fraction, AnnihilationModel.Mode mode) {
        scene.world().modifyBlockEntity(generator, AnnihilationGeneratorBlockEntity.class, be -> be.preview(ng, fraction, ng > 0, mode));
    }

    private static void previewInjector(CreateSceneBuilder scene, BlockPos injector, double ng, boolean armed, InjectorModel.Verdict verdict,
                                        boolean fired) {
        scene.world().modifyBlockEntity(injector, AntiprotonInjectorBlockEntity.class, be -> be.preview(ng, armed, verdict, fired));
    }

    private static void previewTrap(CreateSceneBuilder scene, BlockPos trap, double ng, TrapState state) {
        scene.world().modifyBlockEntity(trap, PenningTrapBlockEntity.class, be -> be.preview(ng, state, 1f, 60));
    }

    /** Sparks from the injector down into the north-west of the plasma ring beneath it. */
    private static void shot(CreateSceneBuilder scene, BlockPos injector, BlockPos axis) {
        Vec3 from = Vec3.atCenterOf(injector).add(0.4, -0.4, 0.3);
        Vec3 to = Vec3.atCenterOf(axis).add(-3.2, 0, -2.4);
        Vec3 step = to.subtract(from).scale(1 / 8.0);
        scene.effects().emitParticles(from, scene.effects().simpleParticleEmitter(ParticleTypes.FLASH, Vec3.ZERO), 1, 1);
        for (int i = 0; i <= 8; i++) {
            Vec3 at = from.add(step.scale(i));
            scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.END_ROD, step.scale(0.15)), 2, 2);
            scene.effects().emitParticles(at, scene.effects().simpleParticleEmitter(ParticleTypes.ELECTRIC_SPARK, step.scale(0.3)), 2, 2);
        }
        scene.effects().emitParticles(to, scene.effects().simpleParticleEmitter(ParticleTypes.FLASH, Vec3.ZERO), 1, 1);
    }

    private static void preview(CreateSceneBuilder scene, BlockPos trap, double ng, TrapState state) {
        scene.world().modifyBlockEntity(trap, PenningTrapBlockEntity.class, be -> be.preview(ng, state, 1f, 60));
    }

    /** A flash of light and smoke where a trap's cloud touched the wall. */
    private static void blast(CreateSceneBuilder scene, BlockPos master, double x, double y, double z) {
        Vec3 centre = Vec3.atLowerCornerOf(master).add(x, y, z);
        scene.effects().emitParticles(centre, scene.effects().simpleParticleEmitter(ParticleTypes.EXPLOSION_EMITTER, Vec3.ZERO), 1, 1);
        scene.effects().emitParticles(centre, scene.effects().simpleParticleEmitter(ParticleTypes.FLASH, Vec3.ZERO), 1, 1);
        scene.effects().emitParticles(centre, scene.effects().particleEmitterWithinBlockSpace(ParticleTypes.END_ROD, new Vec3(0, 0.1, 0)), 8, 2);
    }
}
