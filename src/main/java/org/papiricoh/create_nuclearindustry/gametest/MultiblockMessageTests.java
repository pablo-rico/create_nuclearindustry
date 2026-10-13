package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockMessenger;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * Multiblocks keep the chat for serious events: what a player's own right-click finds goes to their
 * action bar, a structure revalidating after a block change says nothing, and a SCRAM is an alert.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MultiblockMessageTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aRightClickAnswersOnTheActionBar(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor != null, "the reactor controller should be there");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            reactor.attemptFormation(player);
            helper.assertTrue(reactor.messenger().lastKind() == MultiblockMessenger.Kind.STATUS,
                    "forming by hand answers the player, got " + reactor.messenger().lastKind());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void breakingAColdStructureSaysNothing(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor != null, "the reactor controller should be there");
            reactor.attemptFormation(null);
            helper.assertTrue(reactor.isFormed(), "formed first");
            helper.assertTrue(reactor.messenger().lastKind() != MultiblockMessenger.Kind.ALERT, "forming is no alert");
            helper.setBlock(FissionTestBuilder.WALL_CASING, Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(15, () -> {
            helper.assertFalse(reactor.isFormed(), "the broken reactor is no longer formed");
            helper.assertTrue(reactor.messenger().lastKind() != MultiblockMessenger.Kind.ALERT
                            && reactor.messenger().lastKind() != MultiblockMessenger.Kind.STATUS,
                    "a cold structure breaking writes to nobody, got " + reactor.messenger().lastKind());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aScramIsAnAlert(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor != null, "the reactor controller should be there");
            reactor.attemptFormation(null);
            reactor.setManualScram(true);
            helper.assertTrue(reactor.messenger().lastKind() == MultiblockMessenger.Kind.ALERT, "a SCRAM is raised as an alert");
            reactor.setManualScram(false);
            helper.assertTrue(reactor.messenger().lastKind() != MultiblockMessenger.Kind.ALERT, "releasing it is not");
            helper.succeed();
        });
    }
}
