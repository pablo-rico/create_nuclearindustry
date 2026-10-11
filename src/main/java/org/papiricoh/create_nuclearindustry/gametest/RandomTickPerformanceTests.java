package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.LeafState;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineRotorBlock;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/**
 * The safety-net random ticks keep doing their job with their cheaper lookups: a formed rotor
 * finds its turbine through a set of rotor positions, and a blast door leaf looks for its
 * controller among the block entities of the chunks around (or position by position, whichever is
 * cheaper): a loose leaf shuts, one its controller holds open stays open.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RandomTickPerformanceTests {
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 1);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formedRotorsStayFormedOnRandomTicks(GameTestHelper helper) {
        TurbineStructure expected = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        helper.runAfterDelay(5, () -> {
            AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
            TurbineStructure formed = turbine.getStructure().orElse(null);
            helper.assertTrue(formed != null, "the turbine forms");
            for (int a = 1; a < expected.length - 1; a++) {
                BlockPos rotor = expected.at(a, 0, 0);
                BlockPos absolute = helper.absolutePos(rotor);
                helper.assertTrue(formed.ownsRotor(absolute), "the turbine owns its rotor at " + rotor);
                BlockState state = helper.getBlockState(rotor);
                state.randomTick(helper.getLevel(), absolute, helper.getLevel().random);
                helper.assertBlockProperty(rotor, TurbineRotorBlock.FORMED, true);
            }
            helper.assertTrue(!formed.ownsRotor(helper.absolutePos(CONTROLLER)), "the controller is no rotor");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void looseOpenLeafShutsOnARandomTick(GameTestHelper helper) {
        BlockPos leaf = new BlockPos(6, 2, 6);
        helper.setBlock(leaf, CivilDefenseRegistry.STEEL_BLAST_DOOR.get().defaultBlockState().setValue(BlastDoorBlock.LEAF, LeafState.OPEN));
        helper.runAfterDelay(1, () -> {
            BlockPos absolute = helper.absolutePos(leaf);
            helper.assertTrue(!BlastDoorBlock.hasControllerFor(helper.getLevel(), absolute), "no controller around");
            helper.getBlockState(leaf).randomTick(helper.getLevel(), absolute, helper.getLevel().random);
            helper.assertTrue(helper.getBlockState(leaf).getValue(BlastDoorBlock.LEAF) == LeafState.CLOSED,
                    "a leaf left open with no controller shuts itself");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void heldOpenLeafStaysOpenOnARandomTick(GameTestHelper helper) {
        BlockPos controllerPos = BlastDoorTests.buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 5, 3, 6, 2, 2);
        BlockPos leaf = new BlockPos(6, 4, 6);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity controller = helper.getBlockEntity(controllerPos);
                    helper.assertTrue(controller != null && controller.getLayout() != null, "the door forms");
                })
                .thenExecute(() -> helper.setBlock(controllerPos.north(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(leaf).getBlock() instanceof BlastDoorBlock
                        && helper.getBlockState(leaf).getValue(BlastDoorBlock.LEAF) == LeafState.OPEN, "the door opens"))
                .thenExecute(() -> {
                    BlockPos absolute = helper.absolutePos(leaf);
                    helper.assertTrue(BlastDoorBlock.hasControllerFor(helper.getLevel(), absolute), "its controller covers the leaf");
                    helper.getBlockState(leaf).randomTick(helper.getLevel(), absolute, helper.getLevel().random);
                    helper.assertTrue(helper.getBlockState(leaf).getValue(BlastDoorBlock.LEAF) == LeafState.OPEN,
                            "a leaf its controller holds open stays open");
                })
                .thenSucceed();
    }
}
