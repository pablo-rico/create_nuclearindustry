package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.block.CentralSolenoidBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.TokamakSupportBlock;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.gui.TokamakControlPayload;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakFormedLook;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

/**
 * The formed tokamak's look: the octagonal section, the hull hiding under the skin, the parts
 * turning into nozzles, and the supports and solenoid joining up.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakLookTests {
    private static final int RADIUS = 4;

    /** Forming hides the hull and dresses the parts; a hole in the vessel undoes it all. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void formingHidesTheHullAndBreakingRestoresIt(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        BlockPos coil = TokamakTestBuilder.CENTER.offset(-TokamakGeometry.controllerToAxis(RADIUS), 1, 0);
        BlockPos blanket = TokamakTestBuilder.CENTER.offset(RADIUS, TokamakGeometry.HALF_HEIGHT, 0);
        BlockPos controller = TokamakTestBuilder.controller(RADIUS);
        BlockPos port = controller.above();
        BlockPos innerWall = TokamakTestBuilder.CENTER.offset(-2, 0, 0);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "tokamak should form: " + tokamak.getValidationMessage());
            helper.assertBlockProperty(coil, TokamakFormedLook.HIDDEN, true);
            helper.assertBlockProperty(blanket, TokamakFormedLook.HIDDEN, true);
            helper.assertBlockProperty(controller, TokamakFormedLook.FORMED, true);
            helper.assertBlockProperty(port, TokamakFormedLook.FORMED, true);
        });
        // Still formed a while later: changing the look must not make the tokamak revalidate itself away.
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(tokamak.isFormed(), "the formed look must not unform the tokamak");
            helper.destroyBlock(innerWall);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(!tokamak.isFormed(), "a hole in the vessel must unform the tokamak");
            for (BlockPos pos : new BlockPos[]{coil, blanket, controller, port}) {
                BlockState state = helper.getBlockState(pos);
                helper.assertTrue(!TokamakFormedLook.isShaped(state), "still shaped after unforming at " + pos);
            }
            helper.succeed();
        });
    }

    /** The panel's 3D switch puts the plain blocks back without unforming, and the setting survives a revalidation. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void theModelSwitchRestoresThePlainBlocks(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        BlockPos coil = TokamakTestBuilder.CENTER.offset(-TokamakGeometry.controllerToAxis(RADIUS), 1, 0);
        BlockPos controller = TokamakTestBuilder.controller(RADIUS);
        BlockPos port = controller.above();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "tokamak should form: " + tokamak.getValidationMessage());
            helper.assertBlockProperty(coil, TokamakFormedLook.HIDDEN, true);
            Player near = helper.makeMockPlayer(GameType.SURVIVAL);
            near.setPos(helper.absolutePos(controller).getCenter().add(2, 0, 0));
            helper.assertTrue(TokamakControlPayload.apply(near,
                            new TokamakControlPayload(helper.absolutePos(controller), TokamakControlPayload.MODEL_TOGGLE, 0)),
                    "a player in reach can switch the 3D look");
            helper.assertFalse(tokamak.isShowingModel(), "the 3D look should be off");
            helper.assertFalse(tokamak.showsSkin(), "and the skin not drawn");
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(tokamak.isFormed(), "switching the look must not unform the tokamak");
            for (BlockPos pos : new BlockPos[]{coil, controller, port}) {
                helper.assertTrue(!TokamakFormedLook.isShaped(helper.getBlockState(pos)), "still shaped with the 3D look off at " + pos);
            }
            tokamak.setShowModel(true);
            helper.assertBlockProperty(coil, TokamakFormedLook.HIDDEN, true);
            helper.assertBlockProperty(port, TokamakFormedLook.FORMED, true);
            helper.succeed();
        });
    }

    /** A ring built before the octagonal section, corners filled in, still forms and hides its corners. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void ringWithCornersStillForms(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        TokamakGeometry geometry = TokamakGeometry.of(RADIUS);
        for (int i = 0; i < geometry.cornerCount(); i++) {
            helper.setBlock(TokamakTestBuilder.CENTER.offset(geometry.cornerX(i), geometry.cornerY(i), geometry.cornerZ(i)),
                    AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get());
        }
        BlockPos corner = TokamakTestBuilder.CENTER.offset(geometry.cornerX(0), geometry.cornerY(0), geometry.cornerZ(0));
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.isFormed(), "a ring with its corners should still form: " + tokamak.getValidationMessage());
            helper.assertBlockProperty(corner, TokamakFormedLook.HIDDEN, true);
        });
    }

    /** Supports stack into one pillar with a foot at the bottom and a saddle under the ring. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void supportsJoinUpUnderTheRing(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        BlockPos under = TokamakTestBuilder.CENTER.offset(RADIUS, -TokamakGeometry.HALF_HEIGHT - 1, 0);
        BlockPos low = new BlockPos(2, 1, 2);
        helper.setBlock(under, AllNuclearBlocks.TOKAMAK_SUPPORT.get());
        helper.setBlock(low, AllNuclearBlocks.TOKAMAK_SUPPORT.get());
        helper.setBlock(low.above(), AllNuclearBlocks.TOKAMAK_SUPPORT.get());
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.isFormed(), "a support is not part of the tokamak and must not stop it forming");
            helper.assertBlockProperty(under, TokamakSupportBlock.CRADLE, true);
            helper.assertBlockProperty(low, TokamakSupportBlock.FOOT, true);
            helper.assertBlockProperty(low.above(), TokamakSupportBlock.FOOT, false);
            helper.assertBlockProperty(low.above(), TokamakSupportBlock.CRADLE, false);
        });
    }

    /** The solenoid column caps its two ends and joins in the middle. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void solenoidColumnJoinsUp(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        BlockPos center = TokamakTestBuilder.CENTER;
        int h = TokamakGeometry.HALF_HEIGHT;
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(center.above(h), CentralSolenoidBlock.SEGMENT, CentralSolenoidBlock.Segment.TOP);
            helper.assertBlockProperty(center, CentralSolenoidBlock.SEGMENT, CentralSolenoidBlock.Segment.MIDDLE);
            helper.assertBlockProperty(center.below(h), CentralSolenoidBlock.SEGMENT, CentralSolenoidBlock.Segment.BOTTOM);
        });
    }
}
