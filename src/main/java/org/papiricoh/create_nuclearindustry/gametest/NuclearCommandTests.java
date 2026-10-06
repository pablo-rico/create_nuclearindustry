package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;

/** The /nuclear debug commands, run as an operator would. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class NuclearCommandTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH)
    public static void contaminationCommandsCreateSpreadAndClear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 1, 6));
        ChunkPos center = new ChunkPos(pos);
        String at = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        try {
            ContaminationTests.loadAround(level, center, 3);
            run(helper, "nuclear contamination add " + at + " 6");
            helper.assertTrue(Math.abs(Contamination.get(level, center) - 6f) < 1e-4, "add should contaminate the chunk");
            helper.assertTrue(Contamination.get(level, new ChunkPos(center.x + 1, center.z)) == 0, "radius 0 should stay in one chunk");

            run(helper, "nuclear contamination set " + at + " 8");
            helper.assertTrue(Math.abs(Contamination.get(level, center) - 8f) < 1e-4, "set should replace the value");

            run(helper, "nuclear contamination expand " + at + " 2 3");
            helper.assertTrue(Contamination.get(level, new ChunkPos(center.x + 1, center.z)) > 0, "expand should spread it");

            run(helper, "nuclear contamination clear " + at + " 3");
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    helper.assertTrue(Contamination.get(level, new ChunkPos(center.x + dx, center.z + dz)) == 0, "clear should leave the area clean");
                }
            }

            run(helper, "nuclear contamination add " + at + " 4 2");
            float edge = Contamination.get(level, new ChunkPos(center.x + 2, center.z));
            helper.assertTrue(edge > 0 && edge < 4, "a radius should fall off towards the edge, got " + edge);
            run(helper, "nuclear contamination clear all");
            helper.assertTrue(Contamination.get(level, center) == 0, "clear all should clean loaded chunks");
        } finally {
            ContaminationTests.clean(helper, 3);
        }
        helper.succeed();
    }

    private static void run(GameTestHelper helper, String command) {
        ServerLevel level = helper.getLevel();
        CommandSourceStack source = level.getServer().createCommandSourceStack()
                .withLevel(level)
                .withPosition(Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .withPermission(2)
                .withSuppressedOutput();
        level.getServer().getCommands().performPrefixedCommand(source, command);
    }
}
