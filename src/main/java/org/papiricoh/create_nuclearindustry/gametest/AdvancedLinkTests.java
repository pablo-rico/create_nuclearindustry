package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.OutputMode;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlock;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlockEntity;

/** Advanced Redstone Links: codes, transmitting and receiving, and control modules sending on a code. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AdvancedLinkTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aReceiverPutsOutWhatATransmitterWithItsCodeSends(GameTestHelper helper) {
        BlockPos transmitter = new BlockPos(2, 1, 2);
        BlockPos receiver = new BlockPos(9, 1, 9);
        BlockPos stranger = new BlockPos(9, 1, 3);
        BlockPos lamp = new BlockPos(10, 1, 9);
        placeLink(helper, transmitter, false, "AB12");
        placeLink(helper, receiver, true, "AB12");
        placeLink(helper, stranger, true, "AB13");
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        helper.runAfterDelay(2, () -> helper.setBlock(transmitter.east(), Blocks.REDSTONE_BLOCK));
        helper.succeedWhen(() -> {
            AdvancedRedstoneLinkBlockEntity sent = helper.getBlockEntity(transmitter);
            AdvancedRedstoneLinkBlockEntity got = helper.getBlockEntity(receiver);
            AdvancedRedstoneLinkBlockEntity other = helper.getBlockEntity(stranger);
            helper.assertTrue(sent.signal() == 15, "the transmitter picks up the redstone block, got " + sent.signal());
            helper.assertTrue(got.receivedSignal() == 15, "the receiver with the same code gets it, got " + got.receivedSignal());
            helper.assertTrue(helper.getBlockState(receiver).getValue(AdvancedRedstoneLinkBlock.POWERED), "and shows it");
            helper.assertTrue(other.receivedSignal() == 0, "a receiver on another code gets nothing");
            helper.assertBlockProperty(lamp, RedstoneLampBlock.LIT, true);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aReceiverGoesDarkWhenItsTransmitterGoes(GameTestHelper helper) {
        BlockPos transmitter = new BlockPos(2, 1, 2);
        BlockPos receiver = new BlockPos(6, 1, 6);
        placeLink(helper, transmitter, false, "QW77");
        placeLink(helper, receiver, true, "QW77");
        helper.setBlock(transmitter.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            AdvancedRedstoneLinkBlockEntity got = helper.getBlockEntity(receiver);
            helper.assertTrue(got.receivedSignal() == 15, "the receiver gets the signal first, got " + got.receivedSignal());
            helper.destroyBlock(transmitter);
        });
        helper.runAfterDelay(10, () -> {
            AdvancedRedstoneLinkBlockEntity got = helper.getBlockEntity(receiver);
            helper.assertTrue(got.receivedSignal() == 0, "and loses it with the transmitter, got " + got.receivedSignal());
            helper.assertFalse(helper.getBlockState(receiver).getValue(AdvancedRedstoneLinkBlock.POWERED), "going dark");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void codesAreSanitisedAndModesSwitch(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 1, 5);
        placeLink(helper, pos, false, "");
        AdvancedRedstoneLinkBlockEntity link = helper.getBlockEntity(pos);
        CompoundTag config = new CompoundTag();
        config.putString("Code", "ab-12c");
        helper.assertTrue(link.configure(0, config), "a code is taken");
        helper.assertTrue("AB12".equals(link.getCode()), "in capitals and digits only, got " + link.getCode());
        helper.assertFalse(link.configure(0, new CompoundTag()), "a setup without a code changes nothing");
        helper.assertFalse(link.isReceiver(), "placed as a transmitter");
        link.toggleMode();
        helper.assertTrue(link.isReceiver(), "and switched to a receiver");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aLinkNeedsSomethingToSitOn(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 3, 5);
        BlockState state = ControlRoom2Registry.ADVANCED_REDSTONE_LINK.get().defaultBlockState()
                .setValue(AdvancedRedstoneLinkBlock.FACING, Direction.NORTH);
        helper.assertFalse(state.canSurvive(helper.getLevel(), helper.absolutePos(pos)), "nothing behind it");
        helper.setBlock(pos.south(), Blocks.STONE);
        helper.assertTrue(state.canSurvive(helper.getLevel(), helper.absolutePos(pos)), "a block behind holds it");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aModuleSendsOnACode(GameTestHelper helper) {
        BlockPos table = new BlockPos(3, 1, 3);
        BlockPos receiver = new BlockPos(9, 1, 9);
        helper.setBlock(table, ControlRoom2Registry.CONTROL_TABLE.get().defaultBlockState().setValue(ModuleSurfaceBlock.FACING, Direction.NORTH));
        placeLink(helper, receiver, true, "ZZ99");
        ControlDeskBlockEntity entity = helper.getBlockEntity(table);
        helper.assertTrue(entity.insert(0, ModuleType.KEY_SWITCH), "a key switch fits");
        entity.configure(0, entity.module(0).withOutput(OutputMode.ADVANCED_LINK).withLinkCode("zz-99")
                .writeConfig(helper.getLevel().registryAccess()));
        helper.assertTrue("ZZ99".equals(entity.module(0).linkCode()), "the module keeps a clean code");
        helper.runAfterDelay(2, () -> entity.press(0, false));
        helper.succeedWhen(() -> {
            AdvancedRedstoneLinkBlockEntity got = helper.getBlockEntity(receiver);
            helper.assertTrue(got.receivedSignal() == 15, "the switch reaches the link on its code, got " + got.receivedSignal());
        });
    }

    /** A link on the floor, facing up, with a code, as a receiver or a transmitter. */
    private static void placeLink(GameTestHelper helper, BlockPos pos, boolean receiver, String code) {
        helper.setBlock(pos, ControlRoom2Registry.ADVANCED_REDSTONE_LINK.get().defaultBlockState()
                .setValue(AdvancedRedstoneLinkBlock.FACING, Direction.UP).setValue(AdvancedRedstoneLinkBlock.RECEIVER, receiver));
        AdvancedRedstoneLinkBlockEntity link = helper.getBlockEntity(pos);
        link.setCode(code);
    }
}
