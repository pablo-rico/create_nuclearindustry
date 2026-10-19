package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuard;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;
import org.papiricoh.create_nuclearindustry.missile.LaunchCooldowns;
import org.papiricoh.create_nuclearindustry.missile.LaunchRules;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;

import java.util.List;
import java.util.UUID;

/**
 * What a server can forbid: launch pads keep to {@code missile.maxRange}, the protected spawn, the
 * allowed dimensions, the launch permission level and the per-player cooldown, and nuclear craters leave
 * claimed chunks alone. Each test changes the config and puts it back within one tick, so tests running
 * beside it never see the change.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class MissileLimitsTests {
    private static final String BATCH = "missile_limits";
    private static final BlockPos PAD = new BlockPos(6, 3, 6);

    /** A pad loaded with a missile, a dirty warhead and a designator aimed {@code dx} blocks east. */
    private static LaunchPadBlockEntity loadedPad(GameTestHelper helper, int dx) {
        helper.setBlock(PAD, MissileRegistry.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        ItemStack designator = new ItemStack(MissileRegistry.TARGET_DESIGNATOR.get());
        designator.set(AllNuclearDataComponents.TARGET_POS.get(), helper.absolutePos(PAD).offset(dx, 0, 0));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT, new ItemStack(MissileRegistry.MISSILE_ITEM.get()));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.DESIGNATOR_SLOT, designator);
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT, new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        return pad;
    }

    /** A target past {@code missile.maxRange}: the powered pad never counts down, and says why. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aTargetOutOfRangeIsRefused(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LaunchPadBlockEntity pad = loadedPad(helper, 400);
        helper.runAfterDelay(2, () -> {
            double saved = MissileConfig.maxRange;
            LaunchRules.Refusal refused;
            LaunchRules.Refusal inRange;
            int countdown;
            boolean warned;
            try {
                MissileConfig.maxRange = 200;
                helper.setBlock(PAD.east(), Blocks.REDSTONE_BLOCK);
                pad.tick();
                pad.tick();
                refused = pad.getRefusal();
                countdown = pad.getCountdown();
                warned = ThreatRegistry.find(level, helper.absolutePos(PAD)) != null;
                MissileConfig.maxRange = 500;
                inRange = pad.checkLimits(level);
            } finally {
                MissileConfig.maxRange = saved;
                helper.setBlock(PAD.east(), Blocks.AIR);
                pad.clearContent();
            }
            helper.assertTrue(refused == LaunchRules.Refusal.OUT_OF_RANGE, "a target 400 blocks off is past a 200 block limit, got " + refused);
            helper.assertTrue(countdown < 0 && !warned, "so the pad does not count down nor warn the target");
            helper.assertTrue(inRange == LaunchRules.Refusal.NONE, "within a 500 block limit it may launch, got " + inRange);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theSpawnIsProtected(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PAD, MissileRegistry.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(PAD);
        ItemStack designator = new ItemStack(MissileRegistry.TARGET_DESIGNATOR.get());
        designator.set(AllNuclearDataComponents.TARGET_POS.get(), level.getSharedSpawnPos().offset(20, 0, 0));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.DESIGNATOR_SLOT, designator);
        helper.runAfterDelay(1, () -> {
            int saved = MissileConfig.protectedSpawnRadius;
            LaunchRules.Refusal near;
            LaunchRules.Refusal off;
            try {
                MissileConfig.protectedSpawnRadius = 64;
                near = pad.checkLimits(level);
                MissileConfig.protectedSpawnRadius = 0;
                off = pad.checkLimits(level);
            } finally {
                MissileConfig.protectedSpawnRadius = saved;
            }
            helper.assertTrue(level.dimension() == Level.OVERWORLD, "game tests run in the overworld");
            helper.assertTrue(near == LaunchRules.Refusal.SPAWN_PROTECTED, "20 blocks from spawn is inside 64, got " + near);
            helper.assertTrue(off == LaunchRules.Refusal.NONE, "with no protection it may launch, got " + off);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void onlyAllowedDimensionsLaunch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LaunchPadBlockEntity pad = loadedPad(helper, 100);
        helper.runAfterDelay(1, () -> {
            List<String> saved = MissileConfig.allowedDimensions;
            LaunchRules.Refusal elsewhere;
            LaunchRules.Refusal here;
            try {
                MissileConfig.allowedDimensions = List.of("minecraft:the_nether");
                elsewhere = pad.checkLimits(level);
                MissileConfig.allowedDimensions = List.of("minecraft:overworld");
                here = pad.checkLimits(level);
            } finally {
                MissileConfig.allowedDimensions = saved;
                pad.clearContent();
            }
            helper.assertTrue(elsewhere == LaunchRules.Refusal.DIMENSION, "only the Nether allowed: refused, got " + elsewhere);
            helper.assertTrue(here == LaunchRules.Refusal.NONE, "the overworld allowed: launches, got " + here);
            helper.succeed();
        });
    }

    /** The player who loaded the pad needs {@code missile.launchPermissionLevel}; an operator has it. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void itsPlayerNeedsThePermissionLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        LaunchPadBlockEntity pad = loadedPad(helper, 100);
        GameProfile profile = new GameProfile(UUID.randomUUID(), "nuclear_test_op");
        helper.runAfterDelay(1, () -> {
            int saved = MissileConfig.launchPermissionLevel;
            LaunchRules.Refusal nobody;
            LaunchRules.Refusal player;
            LaunchRules.Refusal op;
            LaunchRules.Refusal open;
            try {
                MissileConfig.launchPermissionLevel = 2;
                nobody = pad.checkLimits(level);
                pad.setOperator(profile.getId());
                player = pad.checkLimits(level);
                // An entry of its own: the game test server makes operators at level 0.
                server.getPlayerList().getOps().add(new ServerOpListEntry(profile, 4, false));
                op = pad.checkLimits(level);
                server.getPlayerList().getOps().remove(profile);
                MissileConfig.launchPermissionLevel = 0;
                open = pad.checkLimits(level);
            } finally {
                MissileConfig.launchPermissionLevel = saved;
                server.getPlayerList().getOps().remove(profile);
                pad.clearContent();
            }
            helper.assertTrue(nobody == LaunchRules.Refusal.NO_PERMISSION, "a pad nobody answers for is refused, got " + nobody);
            helper.assertTrue(player == LaunchRules.Refusal.NO_PERMISSION, "so is one loaded by a plain player, got " + player);
            helper.assertTrue(op == LaunchRules.Refusal.NONE, "an operator may launch, got " + op);
            helper.assertTrue(open == LaunchRules.Refusal.NONE, "and anyone may at level 0, got " + open);
            helper.succeed();
        });
    }

    /** A launch starts its player's cooldown, during which their pads are refused. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aLaunchStartsItsPlayersCooldown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        UUID player = helper.makeMockPlayer(GameType.SURVIVAL).getUUID();
        LaunchPadBlockEntity pad = loadedPad(helper, 400);
        BlockPos target = helper.absolutePos(PAD).offset(400, 0, 0);
        pad.setOperator(player);
        helper.setBlock(PAD.east(), Blocks.REDSTONE_BLOCK);
        FlightTracker tracker = FlightTracker.get(level);
        LaunchCooldowns cooldowns = LaunchCooldowns.get(server);
        helper.succeedWhen(() -> {
            Flight launched = tracker.flights().stream().filter(f -> f.target().equals(target)).findFirst().orElse(null);
            helper.assertTrue(launched != null, "the pad launches");
            tracker.remove(level, launched);
            helper.setBlock(PAD.east(), Blocks.AIR);
            helper.assertTrue(player.equals(launched.owner()), "the flight belongs to the player who loaded the pad");
            long last = cooldowns.lastLaunch(player);
            int saved = MissileConfig.launchCooldownSeconds;
            LaunchRules.Refusal cooling;
            LaunchRules.Refusal cleared;
            try {
                MissileConfig.launchCooldownSeconds = 60;
                cooling = pad.checkLimits(level);
                cooldowns.clear(player);
                cleared = pad.checkLimits(level);
            } finally {
                MissileConfig.launchCooldownSeconds = saved;
                cooldowns.clear(player);
            }
            helper.assertTrue(last >= 0, "the launch is recorded against the player");
            helper.assertTrue(cooling == LaunchRules.Refusal.COOLDOWN, "with a minute's cooldown their pad is refused, got " + cooling);
            helper.assertTrue(cleared == LaunchRules.Refusal.NONE, "and free again once it is over, got " + cleared);
        });
    }

    /**
     * A crater leaves a chunk a claim protects as it is, unless {@code explosive.respectClaims} is off.
     * A claim mod's guard stood in for by one protecting the crater's own chunk.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aCraterLeavesClaimedChunksAlone(GameTestHelper helper) {
        BlockPos center = new BlockPos(6, 12, 6);
        helper.setBlock(center.below(2), Blocks.STONE);
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            ChunkPos claimed = new ChunkPos(helper.absolutePos(center));
            ClaimGuard guard = (l, x, z) -> l == level && x == claimed.x && z == claimed.z;
            int columns = PerformanceConfig.blastColumnsPerTick;
            int blocks = PerformanceConfig.blastBlocksPerTick;
            boolean respect = ExplosiveConfig.respectClaims;
            boolean keptWhenClaimed;
            boolean dugWhenIgnored;
            ClaimGuards.add(guard);
            try {
                PerformanceConfig.blastColumnsPerTick = 600;
                PerformanceConfig.blastBlocksPerTick = 1_000_000;
                ExplosiveConfig.respectClaims = true;
                NuclearBlastManager.BlastJob job = NuclearBlastManager.create(level, helper.absolutePos(center), 3, 3, 1.0f);
                helper.assertTrue(job != null, "a crater is made");
                job.step();
                keptWhenClaimed = helper.getBlockState(center.below(2)).is(Blocks.STONE);

                ExplosiveConfig.respectClaims = false;
                NuclearBlastManager.BlastJob ignoring = NuclearBlastManager.create(level, helper.absolutePos(center), 3, 3, 1.0f);
                helper.assertTrue(ignoring != null, "a crater is made");
                ignoring.step();
                dugWhenIgnored = helper.getBlockState(center.below(2)).isAir();
            } finally {
                ClaimGuards.remove(guard);
                PerformanceConfig.blastColumnsPerTick = columns;
                PerformanceConfig.blastBlocksPerTick = blocks;
                ExplosiveConfig.respectClaims = respect;
            }
            helper.assertTrue(keptWhenClaimed, "the claimed chunk keeps its stone");
            helper.assertTrue(dugWhenIgnored, "with explosive.respectClaims off the crater digs it");
            helper.succeed();
        });
    }
}
