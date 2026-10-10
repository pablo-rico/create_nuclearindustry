package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlock;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.siren.HandCrankSirenBlockEntity;

/**
 * Air raid sirens: redstone, a threat in reach, and the hand-crank one's minimum speed. Threats are
 * shared by the whole level, so the threat test runs in a batch of its own and the others start by
 * clearing any left over.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SirenTests {
    private static final String BATCH = "civildefense_sirens";
    private static final BlockPos SIREN = new BlockPos(6, 3, 6);

    private static boolean sounding(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(AirRaidSirenBlock.SOUNDING);
    }

    private static int comparator(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void redstoneSoundsTheSiren(GameTestHelper helper) {
        ThreatRegistry.clear(helper.getLevel());
        helper.setBlock(SIREN, CivilDefenseRegistry.AIR_RAID_SIREN.get());
        helper.startSequence()
                .thenIdle(12)
                .thenExecute(() -> {
                    helper.assertFalse(sounding(helper, SIREN), "silent without redstone or threats");
                    helper.assertTrue(comparator(helper, SIREN) == 0, "a silent siren gives no comparator signal");
                    helper.setBlock(SIREN.east(), Blocks.REDSTONE_BLOCK);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(sounding(helper, SIREN), "redstone sounds it");
                    helper.assertTrue(comparator(helper, SIREN) == 15, "a comparator reads 15 while it sounds");
                    AirRaidSirenBlockEntity siren = helper.getBlockEntity(SIREN);
                    helper.assertTrue(siren != null && siren.getCause() == AirRaidSirenBlockEntity.Cause.REDSTONE,
                            "sounding a steady alert for redstone");
                })
                .thenExecute(() -> helper.setBlock(SIREN.east(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertFalse(sounding(helper, SIREN), "falls silent when the signal goes"))
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void handCrankSirenNeedsItsMinimumSpeed(GameTestHelper helper) {
        ThreatRegistry.clear(helper.getLevel());
        helper.setBlock(SIREN, CivilDefenseRegistry.HAND_CRANK_SIREN.get());
        BlockPos motorPos = SIREN.below();
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(motorPos);
        int slow = CivilDefenseConfig.crankSirenMinSpeed / 2;
        motorEntity.generatedSpeed.setValue(slow);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    HandCrankSirenBlockEntity siren = helper.getBlockEntity(SIREN);
                    helper.assertTrue(siren != null && Math.abs(siren.getSpeed()) == slow, "the shaft from below turns it");
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertFalse(sounding(helper, SIREN), "too slow to sound at " + slow + " RPM");
                    motorEntity.generatedSpeed.setValue(CivilDefenseConfig.crankSirenMinSpeed * 2);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(sounding(helper, SIREN), "sounds once turned fast enough");
                    helper.assertTrue(comparator(helper, SIREN) == 15, "a comparator reads 15 while it sounds");
                })
                .thenExecute(() -> motorEntity.generatedSpeed.setValue(0))
                .thenWaitUntil(() -> helper.assertFalse(sounding(helper, SIREN), "winds down when the crank stops"))
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "civildefense_threat_siren", timeoutTicks = 300)
    public static void threatInReachSoundsTheSiren(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ThreatRegistry.clear(level);
        helper.setBlock(SIREN, CivilDefenseRegistry.AIR_RAID_SIREN.get());
        BlockPos here = helper.absolutePos(SIREN);
        int reach = CivilDefenseConfig.sirenThreatRadius;
        helper.startSequence()
                .thenExecute(() -> ThreatRegistry.raise(level, here.east(reach + 40), 20, 100))
                .thenIdle(15)
                .thenExecute(() -> {
                    helper.assertFalse(sounding(helper, SIREN), "a threat out of reach leaves it silent");
                    ThreatRegistry.clear(level);
                    ThreatRegistry.raise(level, here.east(reach / 2), 0, 60);
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(sounding(helper, SIREN), "a threat in reach sounds it with no redstone");
                    AirRaidSirenBlockEntity siren = helper.getBlockEntity(SIREN);
                    helper.assertTrue(siren != null && siren.getCause() == AirRaidSirenBlockEntity.Cause.THREAT,
                            "sounding the attack warning");
                })
                .thenWaitUntil(() -> helper.assertFalse(sounding(helper, SIREN), "falls silent once the threat is over"))
                .thenExecute(() -> ThreatRegistry.clear(level))
                .thenSucceed();
    }
}
