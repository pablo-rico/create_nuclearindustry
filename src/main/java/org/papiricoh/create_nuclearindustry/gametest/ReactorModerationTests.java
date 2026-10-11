package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;

/** Graphite moderator beside the fuel and beryllium reflector around the core. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorModerationTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fullyModeratedCoreForms(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        for (int[] free : new int[][]{{1, 2}, {2, 1}, {2, 3}, {3, 2}}) {
            FissionTestBuilder.column(helper, free[0], free[1], AllNuclearBlocks.GRAPHITE_MODERATOR.get());
        }
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "graphite columns are valid core columns");
            ReactorStructureValidator.ReactorStructure structure = structure(helper, reactor);
            helper.assertTrue(structure.moderation == 1.0, "both fuel columns have two graphite neighbours, got " + structure.moderation);
            helper.assertTrue(reactor.getPhysicsSimulator().getModeration() == 1.0, "the simulator gets the moderation");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void graphiteAwayFromFuelDoesNothing(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        // (3,1) touches neither fuel column.
        FissionTestBuilder.column(helper, 3, 1, AllNuclearBlocks.GRAPHITE_MODERATOR.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            helper.assertTrue(structure(helper, reactor).moderation == 0.0, "graphite must sit beside the fuel");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oneGraphiteNeighbourIsHalfModerated(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        // (1,2) is beside the (1,1) fuel column only; the (3,3) column stays bare.
        FissionTestBuilder.column(helper, 1, 2, AllNuclearBlocks.GRAPHITE_MODERATOR.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            double moderation = structure(helper, reactor).moderation;
            helper.assertTrue(moderation == 0.25, "one column half moderated, one bare, got " + moderation);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void reflectorCountsAgainstTheInnerRing(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.column(helper, 3, 1, AllNuclearBlocks.BERYLLIUM_REFLECTOR.get());
        FissionTestBuilder.column(helper, 3, 2, AllNuclearBlocks.BERYLLIUM_REFLECTOR.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reflector columns are valid core columns");
            double reflection = structure(helper, reactor).reflection;
            helper.assertTrue(reflection == 2.0 / 8.0, "two of the eight ring columns, got " + reflection);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void graphiteRaisesNeutronProduction(GameTestHelper helper) {
        double bare = steadyNeutrons(0.0, 0.0);
        double moderated = steadyNeutrons(1.0, 0.0);
        helper.assertTrue(moderated > bare * 1.2, "a moderated core runs hotter: " + moderated + " vs " + bare);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void reflectorRaisesSteadyNeutrons(GameTestHelper helper) {
        double bare = steadyNeutrons(0.0, 0.0);
        double reflected = steadyNeutrons(0.0, 1.0);
        helper.assertTrue(reflected > bare * 1.2, "a reflected core loses fewer neutrons: " + reflected + " vs " + bare);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void moderationSurvivesSaving(GameTestHelper helper) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(2, 1, 0.5, 0.75);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        physics.serializeNBT(tag);
        ReactorPhysicsSimulator loaded = new ReactorPhysicsSimulator(0);
        loaded.deserializeNBT(tag);
        helper.assertTrue(loaded.getModeration() == 0.5 && loaded.getReflection() == 0.75, "moderation and reflection are saved");
        helper.succeed();
    }

    /** The formed structure, failing the attempt (not crashing the server) while it is still unformed. */
    private static ReactorStructureValidator.ReactorStructure structure(GameTestHelper helper, ReactorBlockEntity reactor) {
        helper.assertTrue(reactor.getStructure().isPresent(), "reactor should form");
        return reactor.getStructure().get();
    }

    /**
     * Neutron level a minute and a half after pulling the rods to just past half, where a bare core
     * runs at part power once its fuel and water have warmed up.
     */
    private static double steadyNeutrons(double moderation, double reflection) {
        ReactorPhysicsSimulator physics = new ReactorPhysicsSimulator(4, 4, moderation, reflection);
        for (int i = 0; i < 4; i++) {
            physics.loadFuelAssembly(5f);
        }
        physics.setControlRodInsertion(0.45f);
        for (int i = 0; i < 1800; i++) {
            physics.tick();
        }
        return physics.getNeutronLevel();
    }
}
