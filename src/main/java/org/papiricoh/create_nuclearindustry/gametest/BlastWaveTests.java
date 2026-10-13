package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager;

/**
 * The blast wave of nuclear detonations, with a tiny yield so it stays inside the arena: a few
 * kilograms of TNT whose wave travels 18 blocks from the arena's centre.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class BlastWaveTests {
    private static final String BATCH = "civildefense_blastwave";
    private static final BlockPos CENTRE = new BlockPos(20, 2, 20);
    private static final double YIELD = 2.5;
    private static final double REACH = 18;

    private static void startWave(GameTestHelper helper) {
        BlastWaveManager.start(helper.getLevel(), helper.absoluteVec(Vec3.atCenterOf(CENTRE)), YIELD, REACH);
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = BATCH, timeoutTicks = 60)
    public static void wallsShelterFromTheBlastWave(GameTestHelper helper) {
        // Three blocks of stone bricks between the centre and one husk; the other stands in the open.
        for (int x = 14; x <= 16; x++) {
            for (int y = 1; y <= 6; y++) {
                for (int z = 16; z <= 24; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS);
                }
            }
        }
        Husk open = helper.spawnWithNoFreeWill(EntityType.HUSK, 28.5f, 2, 20.5f);
        Husk walled = helper.spawnWithNoFreeWill(EntityType.HUSK, 12.5f, 2, 20.5f);
        startWave(helper);
        helper.succeedWhen(() -> {
            float openLoss = open.getMaxHealth() - open.getHealth();
            float walledLoss = walled.getMaxHealth() - walled.getHealth();
            helper.assertTrue(openLoss > 1, "the husk in the open is hurt, lost " + openLoss);
            helper.assertTrue(open.isAlive(), "but survives so small a blast");
            helper.assertTrue(walledLoss < openLoss / 4, "the walled husk takes far less: " + walledLoss + " against " + openLoss);
            helper.assertTrue(open.getDeltaMovement().x > 0.05, "the wave throws the open husk outwards, got " + open.getDeltaMovement());
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theWaveShattersExposedGlassAndLeaves(GameTestHelper helper) {
        BlockPos glass = new BlockPos(20, 2, 26);
        BlockPos leaves = new BlockPos(26, 2, 20);
        BlockPos window = new BlockPos(12, 2, 20);
        BlockPos shielded = new BlockPos(20, 2, 14);
        BlockPos beyond = new BlockPos(20, 2, 40);
        helper.setBlock(glass, Blocks.GLASS);
        helper.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        // A window under a roof but facing the blast, and a pane behind three blocks of stone.
        helper.setBlock(window, Blocks.GLASS);
        helper.setBlock(window.above(), Blocks.STONE);
        helper.setBlock(shielded, Blocks.GLASS);
        helper.setBlock(shielded.above(), Blocks.STONE);
        for (int z = 15; z <= 17; z++) {
            helper.setBlock(new BlockPos(20, 2, z), Blocks.STONE);
        }
        helper.setBlock(beyond, Blocks.GLASS);
        boolean breaks = CivilDefenseConfig.blastWaveBreakFragileBlocks;
        CivilDefenseConfig.blastWaveBreakFragileBlocks = true;
        startWave(helper);
        helper.runAfterDelay(6, () -> {
            try {
                helper.assertBlockPresent(Blocks.AIR, glass);
                helper.assertBlockPresent(Blocks.AIR, leaves);
                helper.assertBlockPresent(Blocks.AIR, window);
                helper.assertBlockPresent(Blocks.GLASS, shielded);
                helper.assertBlockPresent(Blocks.STONE, shielded.above());
                helper.assertBlockPresent(Blocks.GLASS, beyond);
            } finally {
                CivilDefenseConfig.blastWaveBreakFragileBlocks = breaks;
            }
            helper.succeed();
        });
    }
}
