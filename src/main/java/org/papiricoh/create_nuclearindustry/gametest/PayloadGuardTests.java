package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.DeviceMenu;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.network.PayloadVerdict;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * The server's checks on what a screen asks for: only a living player who could use the block by
 * hand right now gets it (not a spectator, not from afar, not where spawn protection or the world
 * border forbid it, not in adventure mode), and a block menu closes once its block is gone or the
 * player walks off.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PayloadGuardTests {
    private static final BlockPos RECORDER = new BlockPos(6, 1, 6);

    private static ChartRecorderBlockEntity recorder(GameTestHelper helper) {
        helper.setBlock(RECORDER, ControlRoom2Registry.CHART_RECORDER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(RECORDER);
    }

    /** A player of {@code mode} standing two blocks from the recorder. */
    private static Player near(GameTestHelper helper, GameType mode) {
        Player player = helper.makeMockPlayer(mode);
        BlockPos at = helper.absolutePos(RECORDER);
        player.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static PayloadVerdict verdict(GameTestHelper helper, Player player) {
        return ServerPayloadGuard.verdict(player, helper.getLevel(), helper.absolutePos(RECORDER), ServerPayloadGuard.SCREEN_REACH);
    }

    private static CompoundTag speed(int speed) {
        CompoundTag config = new CompoundTag();
        config.putInt("Speed", speed);
        return config;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theGuardTurnsAwayWhoCannotUseTheBlock(GameTestHelper helper) {
        ChartRecorderBlockEntity recorder = recorder(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(recorder != null, "the recorder is placed");
            BlockPos at = helper.absolutePos(RECORDER);
            int speed = recorder.ticksPerSample();

            Player spectator = near(helper, GameType.SPECTATOR);
            helper.assertTrue(verdict(helper, spectator) == PayloadVerdict.NOT_PLAYING, "a spectator is not playing");
            helper.assertFalse(ConfigureDevicePayload.apply(spectator, new ConfigureDevicePayload(at, 0, speed(2))),
                    "so a spectator cannot set the recorder up");

            Player far = near(helper, GameType.SURVIVAL);
            far.setPos(at.getX() + 12.5, at.getY(), at.getZ() + 0.5);
            helper.assertTrue(verdict(helper, far) == PayloadVerdict.OUT_OF_REACH, "twelve blocks off is out of reach, " + verdict(helper, far));
            helper.assertFalse(ConfigureDevicePayload.apply(far, new ConfigureDevicePayload(at, 0, speed(2))),
                    "nor can a player out of reach");

            Player adventurer = near(helper, GameType.ADVENTURE);
            adventurer.getAbilities().mayBuild = false;
            helper.assertTrue(verdict(helper, adventurer) == PayloadVerdict.MAY_NOT_BUILD, "adventure mode may not build");
            helper.assertFalse(ConfigureDevicePayload.apply(adventurer, new ConfigureDevicePayload(at, 0, speed(2))),
                    "nor can a player in adventure mode");

            Player dead = near(helper, GameType.SURVIVAL);
            dead.setHealth(0);
            helper.assertTrue(verdict(helper, dead) == PayloadVerdict.NOT_PLAYING, "a dead player is not playing");

            Player unloaded = near(helper, GameType.SURVIVAL);
            helper.assertTrue(ServerPayloadGuard.verdict(unloaded, helper.getLevel(), at.offset(1_000_000, 0, 0), ServerPayloadGuard.SCREEN_REACH)
                    == PayloadVerdict.UNKNOWN_BLOCK, "a block in an unloaded chunk is unknown");
            helper.assertTrue(recorder.ticksPerSample() == speed, "nothing was changed by any of them");

            Player player = near(helper, GameType.SURVIVAL);
            helper.assertTrue(verdict(helper, player) == PayloadVerdict.OK, "a survival player in reach may, " + verdict(helper, player));
            helper.assertTrue(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, speed(2))), "and sets it up");
            helper.succeed();
        });
    }

    /**
     * Spawn protection and the world border both answer through the level's {@code mayInteract}; a
     * game test server has no spawn protection, so the border stands in for it, moved away and put
     * back within the same tick.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aProtectedBlockIsRefused(GameTestHelper helper) {
        ChartRecorderBlockEntity recorder = recorder(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(recorder != null, "the recorder is placed");
            BlockPos at = helper.absolutePos(RECORDER);
            int speed = recorder.ticksPerSample();
            Player player = near(helper, GameType.SURVIVAL);
            WorldBorder border = helper.getLevel().getWorldBorder();
            double centerX = border.getCenterX(), centerZ = border.getCenterZ(), size = border.getSize();
            PayloadVerdict verdict;
            boolean applied;
            try {
                border.setCenter(at.getX() + 1000, at.getZ() + 1000);
                border.setSize(16);
                verdict = verdict(helper, player);
                applied = ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, speed(2)));
            } finally {
                border.setCenter(centerX, centerZ);
                border.setSize(size);
            }
            helper.assertTrue(verdict == PayloadVerdict.PROTECTED, "the level forbids the block, " + verdict);
            helper.assertFalse(applied, "so the setup is refused");
            helper.assertTrue(recorder.ticksPerSample() == speed, "and nothing changed");
            helper.assertTrue(verdict(helper, player) == PayloadVerdict.OK, "with the border back it is allowed again");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aDeviceMenuClosesWhenItsBlockGoesOrThePlayerLeaves(GameTestHelper helper) {
        ChartRecorderBlockEntity recorder = recorder(helper);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(recorder != null, "the recorder is placed");
            Player player = near(helper, GameType.SURVIVAL);
            DeviceMenu menu = new DeviceMenu(ControlRoom2Registry.CHART_RECORDER_MENU.get(), 0, player.getInventory(),
                    helper.absolutePos(RECORDER), 0);
            helper.assertTrue(menu.stillValid(player), "the menu works beside its recorder");
            BlockPos at = helper.absolutePos(RECORDER);
            player.setPos(at.getX() + 20.5, at.getY(), at.getZ() + 0.5);
            helper.assertFalse(menu.stillValid(player), "it closes when the player walks off");
            player.setPos(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
            helper.assertTrue(menu.stillValid(player), "and works again back beside it");
            helper.assertTrue(ServerPayloadGuard.stillValid(recorder, player), "the recorder itself is still valid");
            helper.setBlock(RECORDER, Blocks.AIR);
            helper.assertFalse(menu.stillValid(player), "it closes once the recorder is gone");
            helper.assertFalse(ServerPayloadGuard.stillValid(recorder, player), "a removed block entity is never valid");
            helper.succeed();
        });
    }
}
