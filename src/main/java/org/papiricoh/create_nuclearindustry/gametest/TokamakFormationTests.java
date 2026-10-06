package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructureValidator;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockValidationResult;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakFormationTests {

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void smallestTokamakForms(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "R=4 tokamak should form: " + tokamak.getValidationMessage());
            helper.assertTrue(tokamak.getMajorRadius() == 4, "radius should be 4, got " + tokamak.getMajorRadius());
            helper.assertTrue(tokamak.getBlanketCount() > 0 && tokamak.getCoilCount() > 0, "coils and blankets should be counted");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void largerTokamakForms(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 8, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 8);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "R=8 tokamak should form: " + tokamak.getValidationMessage());
            helper.assertTrue(tokamak.getStructure().get().plasmaVolume() > 2 * TokamakGeometry.of(4).plasmaCount(),
                    "a larger tokamak should hold more plasma");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void breakingTheVesselUnforms(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        BlockPos innerWall = TokamakTestBuilder.CENTER.offset(-2, 0, 0);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "tokamak should form first");
            helper.destroyBlock(innerWall);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(tokamak.getStructure().isEmpty(), "a hole in the vessel must unform the tokamak");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void blockedPlasmaChamberIsReported(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        helper.setBlock(TokamakTestBuilder.CENTER.offset(0, 0, -4), Blocks.STONE);
        MultiblockValidationResult<?> result = TokamakStructureValidator.validate(helper.getLevel(),
                helper.absolutePos(TokamakTestBuilder.controller(4)));
        helper.assertFalse(result.formed(), "an obstructed chamber must not form");
        helper.assertTrue(result.primaryMessage().startsWith("Plasma chamber blocked"), "got: " + result.primaryMessage());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void legacyFlatRingIsRecognised(GameTestHelper helper) {
        BlockPos controller = TokamakTestBuilder.CENTER;
        helper.setBlock(controller, AllNuclearBlocks.FUSION_CONTROLLER.get());
        for (int[] corner : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            helper.setBlock(controller.offset(corner[0], 0, corner[1]), AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get());
        }
        MultiblockValidationResult<?> result = TokamakStructureValidator.validate(helper.getLevel(), helper.absolutePos(controller));
        helper.assertTrue(TokamakStructureValidator.LEGACY_RING_ERROR.equals(result.primaryMessage()),
                "old ring should be recognised, got: " + result.primaryMessage());
        helper.succeed();
    }
}
