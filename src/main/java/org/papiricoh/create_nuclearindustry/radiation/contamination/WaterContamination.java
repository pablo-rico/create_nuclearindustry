package org.papiricoh.create_nuclearindustry.radiation.contamination;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * contaminated chunk may turn into contaminated water. Chunks wait in a queue and are worked
 * through a few columns per tick, so even a large blast never stalls the server.
 */
public final class WaterContamination {
    /** Chunks below this severity leave their water alone. */
    public static final int MIN_SEVERITY = 3;
    private static final Map<Level, LongLinkedOpenHashSet> QUEUES = new WeakHashMap<>();

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
     * {@code severity / 7}. Waterlogged blocks and deeper water stay as they are.
     */
    public static int convert(ServerLevel level, LevelChunk chunk, int severity, RandomSource random) {
        if (severity < MIN_SEVERITY) {
            return 0;
        }
        float chance = severity / (float) Contamination.MAX_SEVERITY;
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
            level.setBlock(surface, AllNuclearFluids.CONTAMINATED_WATER_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            RadiationCaches.blockChanged(level, surface);
            return true;
        }
        return false;
    }
}
