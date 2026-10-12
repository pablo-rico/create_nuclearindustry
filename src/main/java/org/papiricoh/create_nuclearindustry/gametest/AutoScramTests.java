package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.control.AutoScram;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.reactor.gui.ReactorControlMenu;

/** The automatic SCRAM trips on temperature, latches, and only raises the SCRAM signal. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AutoScramTests {
    private static final BlockPos PORT = FissionTestBuilder.WALL_CASING;

    private static ReactorBlockEntity scramReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        helper.setBlock(PORT, AllNuclearBlocks.REACTOR_REDSTONE_PORT.get().defaultBlockState()
                .setValue(ReactorRedstonePortBlock.FACING, Direction.NORTH)
                .setValue(ReactorRedstonePortBlock.MODE, RedstonePortMode.SCRAM));
        return FissionTestBuilder.controller(helper);
    }

    private static int scramSignal(GameTestHelper helper) {
        return helper.getLevel().getBestNeighborSignal(helper.absolutePos(PORT.north()));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void defaultsOnJustUnderCladdingFailure(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AutoScram auto = FissionTestBuilder.controller(helper).getAutoScram();
        helper.assertTrue(auto.isEnabled(), "automatic SCRAM is on by default");
        helper.assertTrue(auto.getThreshold() == AutoScram.DEFAULT_THRESHOLD && auto.getThreshold() < FissionConfig.claddingFailureTemperature,
                "the default trips before the cladding fails, got " + auto.getThreshold());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldSavesWithoutKeysUseDefaults(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        reactor.adjustAutoScramThreshold(-500);
        reactor.toggleAutoScram();
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = reactor.saveWithoutMetadata(registries);
        tag.remove("autoScramEnabled");
        tag.remove("autoScramThreshold");
        tag.remove("autoScramTripped");
        reactor.loadWithComponents(tag, registries);
        helper.assertTrue(reactor.getAutoScram().isEnabled() && reactor.getAutoScram().getThreshold() == AutoScram.DEFAULT_THRESHOLD,
                "a save without the keys gets the defaults");
        helper.succeed();
    }

    /** A reactor saved on the old 2800 °C default (where the fuel now melts) takes the new default; a set threshold is kept. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theOldDefaultThresholdMigratesButASetOneIsKept(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = reactor.saveWithoutMetadata(registries);
        tag.remove("autoScramVersion");
        tag.putInt("autoScramThreshold", 2800);
        reactor.loadWithComponents(tag, registries);
        helper.assertTrue(reactor.getAutoScram().getThreshold() == AutoScram.DEFAULT_THRESHOLD,
                "the old default moves to the new one, got " + reactor.getAutoScram().getThreshold());
        tag.putInt("autoScramThreshold", 2600);
        reactor.loadWithComponents(tag, registries);
        helper.assertTrue(reactor.getAutoScram().getThreshold() == 2600, "a threshold the operator set is kept");
        tag = reactor.saveWithoutMetadata(registries);
        tag.putInt("autoScramThreshold", 2800);
        reactor.loadWithComponents(tag, registries);
        helper.assertTrue(reactor.getAutoScram().getThreshold() == 2800, "and 2800 chosen on purpose after the change stays");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void overheatTripsAndLatches(GameTestHelper helper) {
        ReactorBlockEntity reactor = scramReactor(helper);
        helper.runAfterDelay(5, () -> {
            reactor.adjustAutoScramThreshold(600 - AutoScram.DEFAULT_THRESHOLD);
            reactor.getPhysicsSimulator().applyExternalHeat(700);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(reactor.getAutoScram().isTripped(), "a 720 C core should trip a 600 C threshold");
            helper.assertTrue(scramSignal(helper) == 15, "the trip should light the SCRAM port, got " + scramSignal(helper));
            helper.assertTrue(reactor.getPhysicsSimulator().getControlRodInsertion() == 0.0f,
                    "the trip must not move rods by itself");
            reactor.getPhysicsSimulator().applyExternalCooling(1000, 0);
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(reactor.getAutoScram().isTripped(), "the trip stays latched after cooling");
            helper.assertTrue(scramSignal(helper) == 15, "the SCRAM signal stays on until reset");
            helper.assertTrue(reactor.resetScram(), "a cooled core can be reset");
            helper.assertTrue(scramSignal(helper) == 0, "reset turns the SCRAM signal off");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void resetRefusedWhileHot(GameTestHelper helper) {
        ReactorBlockEntity reactor = scramReactor(helper);
        helper.runAfterDelay(5, () -> {
            reactor.adjustAutoScramThreshold(600 - AutoScram.DEFAULT_THRESHOLD);
            reactor.getPhysicsSimulator().applyExternalHeat(700);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertFalse(reactor.resetScram(), "reset must be refused above threshold - 200");
            helper.assertTrue(reactor.isScramRequested(), "the SCRAM request stays on");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void disabledNeverTrips(GameTestHelper helper) {
        ReactorBlockEntity reactor = scramReactor(helper);
        helper.runAfterDelay(5, () -> {
            reactor.toggleAutoScram();
            reactor.getPhysicsSimulator().applyExternalHeat(3000);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertFalse(reactor.getAutoScram().isTripped(), "a disabled automatic SCRAM never trips");
            helper.assertTrue(scramSignal(helper) == 0, "no SCRAM signal while disabled");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void menuButtonsAdjustAndClamp(GameTestHelper helper) {
        ReactorBlockEntity reactor = scramReactor(helper);
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ReactorControlMenu menu = new ReactorControlMenu(0, player.getInventory(), reactor);
            menu.clickMenuButton(player, ReactorControlMenu.BUTTON_THRESHOLD_UP);
            helper.assertTrue(reactor.getAutoScram().getThreshold() == AutoScram.DEFAULT_THRESHOLD + 100, "+ adds 100");
            for (int i = 0; i < 20; i++) {
                menu.clickMenuButton(player, ReactorControlMenu.BUTTON_THRESHOLD_UP_BIG);
            }
            helper.assertTrue(reactor.getAutoScram().getThreshold() == AutoScram.MAX_THRESHOLD, "the threshold clamps at 3900");
            menu.clickMenuButton(player, ReactorControlMenu.BUTTON_TOGGLE_AUTO);
            helper.assertFalse(reactor.getAutoScram().isEnabled(), "the middle button turns it off");
            menu.clickMenuButton(player, ReactorControlMenu.BUTTON_TOGGLE_SCRAM);
            helper.assertTrue(reactor.isScramRequested() && scramSignal(helper) == 15, "SCRAM turns the signal on");
            menu.clickMenuButton(player, ReactorControlMenu.BUTTON_TOGGLE_SCRAM);
            helper.assertFalse(reactor.isScramRequested(), "pressing it again resets");
            helper.succeed();
        });
    }
}
