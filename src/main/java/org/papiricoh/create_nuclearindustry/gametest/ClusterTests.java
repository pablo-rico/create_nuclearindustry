package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalConfig;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalRegistry;
import org.papiricoh.create_nuclearindustry.missile.cluster.BombletEntity;
import org.papiricoh.create_nuclearindustry.missile.cluster.ClusterDispenser;

import java.util.List;

/** A cluster warhead opening into its bomblets, the duds that go off late, and a dud lying through a save. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ClusterTests {

    private static void ground(GameTestHelper helper) {
        for (int x = 1; x <= 39; x++) {
            for (int z = 1; z <= 39; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static List<BombletEntity> bomblets(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(BombletEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(80));
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "cluster_open", timeoutTicks = 300)
    public static void aClusterWarheadOpensIntoItsBomblets(GameTestHelper helper) {
        ground(helper);
        ServerLevel level = helper.getLevel();
        double chance = ConventionalConfig.clusterDudChance;
        double footprint = ConventionalConfig.clusterFootprintRadius;
        ConventionalConfig.clusterDudChance = 0;
        ConventionalConfig.clusterFootprintRadius = 8;
        try {
            ClusterDispenser.open(level, helper.absoluteVec(new Vec3(20.5, 8, 20.5)), new Vec3(0, -1, 0), 6, 1.5f, DetonationCause.NONE);
        } finally {
            ConventionalConfig.clusterDudChance = chance;
            ConventionalConfig.clusterFootprintRadius = footprint;
        }
        helper.assertTrue(bomblets(helper).size() == 6, "six bomblets fall: " + bomblets(helper).size());
        helper.succeedWhen(() -> {
            helper.assertTrue(bomblets(helper).isEmpty(), "they all go off where they land");
            helper.assertTrue(org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager.active(level) == 0, "and their waves pass");
        });
    }

    @GameTest(template = NuclearGameTests.BLAST_ARENA, batch = "cluster_duds", timeoutTicks = 300)
    public static void aDudGoesOffLate(GameTestHelper helper) {
        ground(helper);
        ServerLevel level = helper.getLevel();
        double chance = ConventionalConfig.clusterDudChance;
        int min = ConventionalConfig.clusterDudMinSeconds;
        int max = ConventionalConfig.clusterDudMaxSeconds;
        double footprint = ConventionalConfig.clusterFootprintRadius;
        ConventionalConfig.clusterFootprintRadius = 6;
        ConventionalConfig.clusterDudChance = 1;
        ConventionalConfig.clusterDudMinSeconds = 3;
        ConventionalConfig.clusterDudMaxSeconds = 3;
        try {
            ClusterDispenser.open(level, helper.absoluteVec(new Vec3(20.5, 6, 20.5)), Vec3.ZERO, 3, 1.5f, DetonationCause.NONE);
        } finally {
            ConventionalConfig.clusterDudChance = chance;
            ConventionalConfig.clusterDudMinSeconds = min;
            ConventionalConfig.clusterDudMaxSeconds = max;
            ConventionalConfig.clusterFootprintRadius = footprint;
        }
        helper.runAfterDelay(40, () -> {
            List<BombletEntity> lying = bomblets(helper);
            helper.assertTrue(lying.size() == 3 && lying.stream().allMatch(b -> b.dud() && b.landed()), "the duds lie where they fell");
        });
        helper.runAfterDelay(120, () -> helper.assertTrue(bomblets(helper).isEmpty(), "and go off a few seconds later"));
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager.active(level) == 0, "their waves have passed");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "cluster_save")
    public static void aDudLiesThroughASave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BombletEntity dud = BombletEntity.create(level, helper.absoluteVec(new Vec3(5, 3, 5)), Vec3.ZERO, 1.5f, true, 400,
                DetonationCause.NONE);
        CompoundTag tag = new CompoundTag();
        dud.saveWithoutId(tag);
        BombletEntity loaded = new BombletEntity(ConventionalRegistry.BOMBLET.get(), level);
        loaded.load(tag);
        helper.assertTrue(loaded.dud() && !loaded.landed(), "a saved dud is still a dud");
        helper.succeed();
    }
}
