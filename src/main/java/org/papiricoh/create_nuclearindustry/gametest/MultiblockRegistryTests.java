package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MultiblockRegistryTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formedReactorIsIndexedByItsShell(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            BlockPos shell = helper.absolutePos(FissionTestBuilder.WALL_CASING);
            helper.assertTrue(MultiblockRegistry.affecting(helper.getLevel(), shell, ReactorBlockEntity.class).contains(reactor),
                    "a shell block should resolve to its controller");
            BlockPos outside = helper.absolutePos(FissionTestBuilder.ORIGIN.offset(FissionTestBuilder.SIZE + 3, 0, 0));
            helper.assertTrue(MultiblockRegistry.affecting(helper.getLevel(), outside, ReactorBlockEntity.class).isEmpty(),
                    "a block outside the shell should not resolve to the controller");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void unrelatedBlockOutsideDoesNotUnform(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> helper.setBlock(FissionTestBuilder.ORIGIN.offset(FissionTestBuilder.SIZE + 1, 0, 0), Blocks.STONE));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.isFormed(), "placing stone next to the reactor must not affect it");
            helper.succeed();
        });
    }
}
