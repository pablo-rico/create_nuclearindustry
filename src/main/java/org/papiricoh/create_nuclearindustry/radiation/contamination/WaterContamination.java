package org.papiricoh.create_nuclearindustry.radiation.contamination;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fallout settling on open water: the surface source block of each water column in a badly
 * contaminated chunk (from {@code contamination.waterMinSeverity}) may turn into contaminated
 * water. Chunks wait in a queue and are worked through a few columns per tick, so even a large
 * blast never stalls the server. Once the chunk is below {@link #CLEAN_SEVERITY} the water
 * settles back to clean water on its random ticks.
 */
public final class WaterContamination {
    /** Below this severity contaminated water clears on its random ticks. */
    public static final int CLEAN_SEVERITY = 3;
    /** Chance per random tick (about every 68 seconds) that such a source block turns clean. */
    public static final float CLEAN_CHANCE = 0.25f;
    private static final Map<Level, LongLinkedOpenHashSet> QUEUES = new WeakHashMap<>();
    /** Set while fallout itself turns water, which must not count as a spill. */
    private static boolean converting;

    private WaterContamination() {}

    public static void queue(ServerLevel level, ChunkPos pos) {
        if (Config.contaminationEnabled && Config.waterColumnsPerTick > 0) {
            QUEUES.computeIfAbsent(level, l -> new LongLinkedOpenHashSet()).add(pos.toLong());
        }
    }

    public static boolean isQueued(ServerLevel level, ChunkPos pos) {
        LongLinkedOpenHashSet queue = QUEUES.get(level);
        return queue != null && queue.contains(pos.toLong());
    }

    public static int queued(ServerLevel level) {
        LongLinkedOpenHashSet queue = QUEUES.get(level);
        return queue == null ? 0 : queue.size();
    }

    public static void clear(ServerLevel level) {
        QUEUES.remove(level);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            process(level, Config.waterColumnsPerTick);
        }
    }

    /** Works through whole queued chunks until {@code columnBudget} columns are spent; returns blocks converted. */
    public static int process(ServerLevel level, int columnBudget) {
        LongLinkedOpenHashSet queue = QUEUES.get(level);
        int converted = 0;
        while (queue != null && !queue.isEmpty() && columnBudget > 0) {
            long packed = queue.removeFirstLong();
            LevelChunk chunk = Contamination.loaded(level, ChunkPos.getX(packed), ChunkPos.getZ(packed));
            if (chunk == null) {
                continue;
            }
            converted += convert(level, chunk, Contamination.severity(Contamination.read(chunk)), level.random);
            columnBudget -= 256;
        }
        return converted;
    }

    /**
     * Turns the top water source of each column into contaminated water, each with a chance of
     * {@link #chance}. Waterlogged blocks and deeper water stay as they are.
     */
    public static int convert(ServerLevel level, LevelChunk chunk, int severity, RandomSource random) {
        float chance = chance(severity);
        if (chance <= 0) {
            return 0;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int converted = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                pos.set(chunk.getPos().getMinBlockX() + x, top, chunk.getPos().getMinBlockZ() + z);
                if (convertColumn(level, pos, chance, random)) {
                    converted++;
                }
            }
        }
        return converted;
    }

    /**
     * Turns {@code surface} into contaminated water if it is a plain water source, with the given chance.
     * The change updates no neighbours, so it is told to the radiation caches here: the water's
     * radioactivity and shielding are part of the rays cast across its chunk.
     */
    public static boolean convertColumn(ServerLevel level, BlockPos surface, float chance, RandomSource random) {
        if (level.getBlockState(surface).is(Blocks.WATER) && level.getFluidState(surface).isSource() && random.nextFloat() < chance) {
            converting = true;
            try {
                level.setBlock(surface, AllNuclearFluids.CONTAMINATED_WATER_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            } finally {
                converting = false;
            }
            RadiationCaches.blockChanged(level, surface);
            return true;
        }
        return false;
    }

    /**
     * Chance a surface source turns at {@code severity}: none below {@code contamination.waterMinSeverity},
     * then rising to one in two at the top severity.
     */
    public static float chance(int severity) {
        int min = Config.waterMinSeverity;
        if (severity < min) {
            return 0;
        }
        return (severity - min + 1) / (2.0f * (Contamination.MAX_SEVERITY - min + 1));
    }

    /**
     * A random tick of a contaminated water source: once its chunk is below {@link #CLEAN_SEVERITY}
     * it may turn back into clean water.
     */
    public static boolean settle(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!state.getFluidState().isSource()) {
            return false;
        }
        LevelChunk chunk = Contamination.loaded(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || Contamination.severity(Contamination.read(chunk)) >= CLEAN_SEVERITY || random.nextFloat() >= CLEAN_CHANCE) {
            return false;
        }
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        RadiationCaches.blockChanged(level, pos);
        return true;
    }

    /**
     * Contaminated water poured out (a bucket, a pipe, a spill) gives what it carries back to the
     * ground of its chunk: {@code contamination.spilledWaterLevel} per source block.
     */
    public static void spilled(ServerLevel level, BlockPos pos) {
        if (converting || !Config.contaminationEnabled || Config.spilledWaterLevel <= 0) {
            return;
        }
        LevelChunk chunk = Contamination.loaded(level, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null) {
            Contamination.add(chunk, (float) Config.spilledWaterLevel);
        }
    }
}
