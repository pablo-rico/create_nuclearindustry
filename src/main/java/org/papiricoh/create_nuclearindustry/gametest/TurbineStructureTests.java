package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructureValidator;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TurbineStructureTests {
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 1);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void smallestBoxForms(GameTestHelper helper) {
        TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 3);
        MultiblockValidationResult<TurbineStructure> result = TurbineTestBuilder.validate(helper, TurbineTier.STEAM, CONTROLLER);
        helper.assertTrue(result.formed(), "3x3x3 should form: " + result.primaryMessage());
        TurbineStructure s = result.structure().orElseThrow();
        helper.assertTrue(s.rotorCount() == 1 && s.inputPorts == 1 && s.outputPorts == 1, "one rotor, one port pair");
        helper.assertTrue(s.throughputLimit() == 40, "one rotor limits flow to 40, got " + s.throughputLimit());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void nonSquareBoxIsRejected(GameTestHelper helper) {
        TurbineTestBuilder.build(helper, TurbineTier.PLASMA, CONTROLLER, 3, 5, 6);
        expectError(helper, TurbineTier.PLASMA, "Turbines are 3x3");
    }

    /** Turbines built before the jet-engine redesign were up to 7 wide; they now say why they stopped. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldWideBoxIsExplained(GameTestHelper helper) {
        TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 5, 5, 5);
        expectError(helper, TurbineTier.STEAM, "Turbines are 3x3");
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void largestBoxForms(GameTestHelper helper) {
        BlockPos controller = new BlockPos(5, 4, 1);
        TurbineTestBuilder.build(helper, TurbineTier.STEAM, controller, 3, 3, 16);
        MultiblockValidationResult<TurbineStructure> result = TurbineTestBuilder.validate(helper, TurbineTier.STEAM, controller);
        helper.assertTrue(result.formed(), "3x3x16 should form: " + result.primaryMessage());
        helper.assertTrue(result.structure().orElseThrow().rotorCount() == 14, "14 rotors expected");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void glassOnFaceIsAccepted(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        helper.setBlock(s.at(2, 0, s.hv), AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get());
        helper.setBlock(s.at(3, 0, s.hv), AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get());
        MultiblockValidationResult<TurbineStructure> result = TurbineTestBuilder.validate(helper, TurbineTier.STEAM, CONTROLLER);
        helper.assertTrue(result.formed() && result.structure().orElseThrow().glassCount == 2, "glass in a wall is fine: " + result.primaryMessage());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void glassOnEdgeIsRejected(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        helper.setBlock(s.at(2, s.hu, s.hv), AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get());
        expectError(helper, TurbineTier.STEAM, "Glass cannot");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void portOnEdgeIsRejected(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        TurbineTestBuilder.port(helper, TurbineTier.STEAM, s.at(3, s.hu, s.hv), Direction.UP,
                org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode.INPUT);
        expectError(helper, TurbineTier.STEAM, "Ports must be on a face");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void brokenRotorColumnIsRejected(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 6);
        helper.setBlock(s.at(3, 0, 0), Blocks.AIR);
        expectError(helper, TurbineTier.STEAM, "Rotor column broken");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void misalignedRotorIsRejected(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 6);
        helper.setBlock(s.at(3, 0, 0), AllNuclearBlocks.TURBINE_ROTOR.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        expectError(helper, TurbineTier.STEAM, "Rotor column broken");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void offCentreControllerIsRejected(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        // Widen the box on one side only by moving the east wall out.
        for (int a = 0; a < s.length; a++) {
            for (int i = -s.hu; i <= s.hu; i++) {
                helper.setBlock(s.at(a, i, s.hv), Blocks.AIR);
                helper.setBlock(s.at(a, i, s.hv + 1), TurbineTestBuilder.casing(TurbineTier.STEAM));
            }
        }
        expectError(helper, TurbineTier.STEAM, "Turbines are 3x3");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void mixedTiersAreRejected(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        helper.setBlock(s.at(2, 0, s.hv), AllNuclearBlocks.FUSION_TURBINE_CASING.get());
        expectError(helper, TurbineTier.STEAM, "Invalid wall block");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacyThreeBlockLineIsRecognised(GameTestHelper helper) {
        BlockPos controller = new BlockPos(6, 2, 2);
        helper.setBlock(controller, AllNuclearBlocks.TURBINE_OUTPUT.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
        helper.setBlock(controller.south(), AllNuclearBlocks.TURBINE_ROTOR.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(controller.south(2), AllNuclearBlocks.TURBINE_CASING.get());
        MultiblockValidationResult<TurbineStructure> result = TurbineTestBuilder.validate(helper, TurbineTier.STEAM, controller);
        helper.assertTrue(TurbineStructureValidator.LEGACY_LINE_ERROR.equals(result.primaryMessage()),
                "old line should be recognised, got: " + result.primaryMessage());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void reversedControllerIsExplained(GameTestHelper helper) {
        TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 4);
        helper.setBlock(CONTROLLER, AllNuclearBlocks.TURBINE_OUTPUT.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.SOUTH));
        expectError(helper, TurbineTier.STEAM, "Controller faces into the turbine");
    }

    /** Ponder schematics come from tools/structgen/ponder_turbine.py; both must be valid turbines. */
    @GameTest(template = "gametest/turbine_ponder")
    public static void steamPonderSchematicForms(GameTestHelper helper) {
        ponderSchematicForms(helper, TurbineTier.STEAM);
    }

    @GameTest(template = "gametest/fusion_turbine_ponder")
    public static void plasmaPonderSchematicForms(GameTestHelper helper) {
        ponderSchematicForms(helper, TurbineTier.PLASMA);
    }

    private static void ponderSchematicForms(GameTestHelper helper, TurbineTier tier) {
        // Templates are placed one block above the test origin.
        BlockPos controller = new BlockPos(3, 3, 1);
        helper.runAfterDelay(2, () -> {
            MultiblockValidationResult<TurbineStructure> result = TurbineTestBuilder.validate(helper, tier, controller);
            helper.assertTrue(result.formed(), "the Ponder turbine should be valid: " + result.primaryMessage());
            TurbineStructure s = result.structure().orElseThrow();
            helper.assertTrue(s.rotorCount() == 5 && s.glassCount == 5, "5 rotors and 5 glass expected");
            helper.succeed();
        });
    }

    private static void expectError(GameTestHelper helper, TurbineTier tier, String prefix) {
        MultiblockValidationResult<TurbineStructure> result = TurbineTestBuilder.validate(helper, tier, CONTROLLER);
        helper.assertFalse(result.formed(), "structure should not form");
        helper.assertTrue(result.primaryMessage().startsWith(prefix), "expected '" + prefix + "...', got: " + result.primaryMessage());
        helper.succeed();
    }
}
