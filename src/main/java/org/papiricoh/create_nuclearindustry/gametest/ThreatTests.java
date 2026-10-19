package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatBoard;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;

/**
 * Bombs still report threats; missiles require radar and never report direct siren threats. Every test stops its countdown or missile before asserting,
 * so a failure never leaves a live weapon behind.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ThreatTests {
    private static final String BATCH = "civildefense_threat_sources";
    private static final BlockPos WEAPON = new BlockPos(6, 3, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void bombCountdownIsAThreatUntilCancelled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(WEAPON, AllNuclearBlocks.DIRTY_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(WEAPON);
        bomb.getInventory().setStackInSlot(0, new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        BlockPos at = helper.absolutePos(WEAPON);
        helper.setBlock(WEAPON.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            ThreatBoard.Threat threat = ThreatRegistry.find(level, at);
            long ticks = threat == null ? 0 : threat.ticksToImpact(level.getGameTime());
            helper.setBlock(WEAPON.east(), Blocks.AIR);
            helper.assertTrue(threat != null, "a counting bomb warns the sirens");
            helper.assertTrue(threat.x() == at.getX() && threat.y() == at.getY() && threat.z() == at.getZ(), "aimed at the bomb itself");
            helper.assertTrue(ticks > 150 && ticks <= bomb.getCountdownDuration(), "lands when the countdown ends, got " + ticks);
            helper.assertTrue(threat.radius() == DirtyWarheadStats.falloutRadius(bomb.getRadiologicalLoad()),
                    "reaches as far as its fallout");
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(ThreatRegistry.find(level, at) == null, "cancelling the countdown withdraws the threat");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void legacyLaunchPadDoesNotWarnTheTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(WEAPON, MissileRegistry.LAUNCH_PAD.get());
        LaunchPadBlockEntity pad = helper.getBlockEntity(WEAPON);
        BlockPos target = helper.absolutePos(WEAPON).offset(150, 0, 0);
        ItemStack designator = new ItemStack(MissileRegistry.TARGET_DESIGNATOR.get());
        designator.set(AllNuclearDataComponents.TARGET_POS.get(), target);
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.MISSILE_SLOT, new ItemStack(MissileRegistry.MISSILE_ITEM.get()));
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.DESIGNATOR_SLOT, designator);
        pad.getInventory().setStackInSlot(LaunchPadBlockEntity.FIRST_URANIUM_SLOT, new ItemStack(AllNuclearItems.RADIOACTIVE_WASTE.get()));
        BlockPos at = helper.absolutePos(WEAPON);
        helper.setBlock(WEAPON.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            ThreatBoard.Threat threat = ThreatRegistry.find(level, at);
            helper.setBlock(WEAPON.east(), Blocks.AIR);
            helper.assertTrue(threat == null, "a disabled platform does not warn the sirens");
            helper.assertTrue(pad.getCountdown() < 0 && pad.hasMissile(), "legacy platform preserves inventory and never launches");
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(ThreatRegistry.find(level, at) == null, "cancelling the launch withdraws the threat");
            helper.assertTrue(pad.getCountdown() < 0, "and nothing was launched");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void oldSavedMissileDoesNotBypassRadar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(WEAPON));
        Vec3 target = start.add(200, 0, 0);
        MissileEntity missile = new MissileEntity(MissileRegistry.MISSILE.get(), level);
        missile.configureDirty(start, target, 1.0f);
        level.addFreshEntity(missile);
        int flight = MissileEntity.legacyFlightTicks(start, target);
        helper.runAfterDelay(3, () -> {
            ThreatBoard.Threat threat = ThreatRegistry.find(level, missile.getUUID());
            missile.discard();
            helper.assertTrue(threat == null, "old saved missiles cannot bypass radar requirements");
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(ThreatRegistry.find(level, missile.getUUID()) == null, "a removed missile withdraws its threat");
            helper.succeed();
        });
    }
}
