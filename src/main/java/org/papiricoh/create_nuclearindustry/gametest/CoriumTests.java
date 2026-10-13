package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.CoriumBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.CoriumModel;

/**
 * Corium: a meltdown turns the fuel into molten core instead of exploding; corium melts down a
 * bounded number of blocks and freezes into slag, spreads and freezes on a core catcher, and with
 * {@code blockDestruction} off it melts nothing. Slag leaks into the ground, so these run in
 * contamination batches and clean up after themselves.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CoriumTests {
    private static final String BATCH = "contamination_corium";
    /** A little corium: HEAT 1, STEP 3, an energy budget of 7. */
    private static final int ENERGY = 7;
    private static final BlockPos COLUMN = new BlockPos(6, 1, 6);
    private static final int TOP = 11;

    private static BlockState corium(int energy) {
        return PwrRegistry.CORIUM.get().defaultBlockState()
                .setValue(CoriumBlock.HEAT, CoriumModel.heat(energy)).setValue(CoriumBlock.STEP, CoriumModel.step(energy));
    }

    /** A stone column from y=1 to y=TOP-1 with a little corium on top. */
    private static void stoneColumnWithCorium(GameTestHelper helper) {
        for (int y = COLUMN.getY(); y < TOP; y++) {
            helper.setBlock(COLUMN.atY(y), Blocks.STONE);
        }
        helper.setBlock(COLUMN.atY(TOP), corium(ENERGY));
    }

    private static int countCorium(GameTestHelper helper) {
        int count = 0;
        for (int x = 0; x <= 12; x++) {
            for (int y = 0; y <= 15; y++) {
                for (int z = 0; z <= 12; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof CoriumBlock) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 700)
    public static void aMeltdownTurnsTheFuelIntoCorium(GameTestHelper helper) {
        int events = ContainmentTestBuilder.falloutEvents(helper);
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            reactor.getPhysicsSimulator().applyExternalHeat(4_200);
            PwrModeTests.heat(reactor, 60.0);
        });
        helper.succeedWhen(() -> {
            helper.assertFalse(reactor.isFormed(), "the core melted");
            int corium = countCorium(helper);
            helper.assertTrue(corium == 6, "each of the six fuel rods became corium, found " + corium);
            helper.assertBlockPresent(PwrRegistry.CORIUM.get(), FissionTestBuilder.ORIGIN.offset(1, 1, 1));
            helper.assertBlockPresent(AllNuclearBlocks.REACTOR_CASING.get(), FissionTestBuilder.WALL_CASING);
            helper.assertBlockPresent(AllNuclearBlocks.REACTOR_CASING.get(), FissionTestBuilder.ORIGIN.offset(1, 0, 1));
            helper.assertBlockPresent(AllNuclearBlocks.REACTOR_CONTROLLER.get(), FissionTestBuilder.CONTROLLER);
            helper.assertTrue(ContainmentTestBuilder.falloutEvents(helper) > events, "with no containment the release falls out");
            ContainmentTestBuilder.clearArena(helper);
            ContainmentTestBuilder.cleanFallout(helper, events, 3);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void coriumMeltsABoundedDepthThenFreezes(GameTestHelper helper) {
        stoneColumnWithCorium(helper);
        // energy 7 melts twice-cost blocks: three melts leave 1, the fourth freezes it in the fourth block down
        BlockPos frozen = COLUMN.atY(TOP - 4);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(PwrRegistry.SOLIDIFIED_CORIUM.get(), frozen);
            for (int y = TOP - 3; y <= TOP; y++) {
                helper.assertBlockPresent(Blocks.AIR, COLUMN.atY(y));
            }
            for (int y = COLUMN.getY(); y < TOP - 4; y++) {
                helper.assertBlockPresent(Blocks.STONE, COLUMN.atY(y));
            }
            helper.assertTrue(countCorium(helper) == 0, "no molten corium is left");
            ContainmentTestBuilder.clearArena(helper);
            ContaminationTests.clean(helper, 0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aCoreCatcherStopsIt(GameTestHelper helper) {
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), PwrRegistry.CORE_CATCHER.get());
            }
        }
        helper.setBlock(new BlockPos(6, 2, 6), corium(ENERGY));
        helper.succeedWhen(() -> {
            helper.assertTrue(countCorium(helper) == 0, "it froze");
            int slag = 0;
            for (int x = 5; x <= 7; x++) {
                for (int z = 5; z <= 7; z++) {
                    helper.assertBlockPresent(PwrRegistry.CORE_CATCHER.get(), new BlockPos(x, 1, z));
                    if (helper.getBlockState(new BlockPos(x, 2, z)).is(PwrRegistry.SOLIDIFIED_CORIUM.get())) {
                        slag++;
                    }
                }
            }
            helper.assertTrue(slag == 1, "the slag lies on the catcher, found " + slag);
            ContainmentTestBuilder.clearArena(helper);
            ContaminationTests.clean(helper, 0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "_safe", timeoutTicks = 400)
    public static void withoutBlockDestructionItMeltsNothing(GameTestHelper helper) {
        boolean destruction = PwrConfig.blockDestruction;
        PwrConfig.blockDestruction = false;
        stoneColumnWithCorium(helper);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(PwrRegistry.SOLIDIFIED_CORIUM.get(), COLUMN.atY(TOP));
            for (int y = COLUMN.getY(); y < TOP; y++) {
                helper.assertBlockPresent(Blocks.STONE, COLUMN.atY(y));
            }
            PwrConfig.blockDestruction = destruction;
            ContainmentTestBuilder.clearArena(helper);
            ContaminationTests.clean(helper, 0);
        });
    }
}
