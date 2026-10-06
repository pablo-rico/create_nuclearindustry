package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.GeologicalStorage;

/** What leaves contamination behind, and how deep waste has to go to stop. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FalloutSourcesTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_detonationFalloutReachesPastTheCraterAndFadesOut")
    public static void detonationFalloutReachesPastTheCraterAndFadesOut(GameTestHelper helper) {
        int crater = WarheadStats.horizontalRadius(WarheadStats.MAX_URANIUM_UNITS, WarheadStats.REQUIRED_ENRICHMENT);
        float power = WarheadStats.power(WarheadStats.MAX_URANIUM_UNITS, WarheadStats.REQUIRED_ENRICHMENT);
        int radius = NuclearDetonation.falloutRadius(crater);
        float intensity = NuclearDetonation.falloutIntensity(power);
        helper.assertTrue(radius > crater, "fallout should reach beyond the crater");
        helper.assertTrue(intensity > 10 && intensity <= Config.contaminationMax, "a full warhead should leave heavy fallout, got " + intensity);

        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 1, 6));
        ChunkPos origin = new ChunkPos(center);
        ContaminationData data = ContaminationData.get(level);
        try {
            ContaminationTests.loadAround(level, origin, 3);
            Contamination.fallout(level, center, 40, 10);
            float near = Contamination.get(level, origin);
            float far = Contamination.get(level, new ChunkPos(origin.x + 2, origin.z));
            float out = Contamination.get(level, new ChunkPos(origin.x + 3, origin.z + 3));
            helper.assertTrue(near > far && far > 0, "fallout should fade with distance: " + near + " then " + far);
            helper.assertTrue(out == 0, "nothing should land beyond the radius, got " + out);
            data.events().removeLast();
        } finally {
            ContaminationTests.clean(helper, 3);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_barrelsLeakAboveTheStorageDepthOnly")
    public static void barrelsLeakAboveTheStorageDepthOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlockAndUpdate(pos, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState());
        BlockState barrel = level.getBlockState(pos);
        helper.assertTrue(barrel.isRandomlyTicking(), "barrels should take random ticks");
        int storage = Config.geologicalStorageMaxY;
        try {
            Config.geologicalStorageMaxY = pos.getY() - 1;
            barrel.randomTick(level, pos, level.random);
            float leaked = Contamination.get(level, new ChunkPos(pos));
            helper.assertTrue(Math.abs(leaked - (float) Config.barrelLeak) < 1e-4, "a surface barrel should leak, got " + leaked);

            Config.geologicalStorageMaxY = pos.getY();
            barrel.randomTick(level, pos, level.random);
            helper.assertTrue(Contamination.get(level, new ChunkPos(pos)) == leaked, "a stored barrel must not leak");
        } finally {
            Config.geologicalStorageMaxY = storage;
            ContaminationTests.clean(helper, 0);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_storedBarrelsStopIrradiating")
    public static void storedBarrelsStopIrradiating(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlockAndUpdate(pos, AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get().defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
        int storage = Config.geologicalStorageMaxY;
        try {
            Config.geologicalStorageMaxY = pos.getY() - 1;
            helper.assertFalse(GeologicalStorage.isStored(level.getBlockState(pos), pos), "above the depth it is not stored");
            double surface = RadiationHandler.exposureRate(player);
            helper.assertTrue(surface > 0.2, "a barrel next to you should irradiate, got " + surface);

            Config.geologicalStorageMaxY = pos.getY();
            helper.assertTrue(GeologicalStorage.isStored(level.getBlockState(pos), pos), "at the depth it is stored");
            double stored = RadiationHandler.exposureRate(player);
            helper.assertTrue(stored == 0, "a stored barrel should not irradiate, got " + stored);
        } finally {
            Config.geologicalStorageMaxY = storage;
        }
        helper.succeed();
    }
}
