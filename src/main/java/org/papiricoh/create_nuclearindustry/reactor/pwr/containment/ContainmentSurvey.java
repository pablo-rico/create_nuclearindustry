package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;

/**
 * One measurement of a containment building: a {@link SealedVolume} flood fill from the monitor,
 * run a budget of blocks per tick. Gas passes every block without a full collision box, so open
 * doors, slabs, pipes and air leak; the monitor's own block counts as air, since it stands inside.
 * Remembers whether the fill ran into unloaded chunks, which says nothing about the building.
 */
public final class ContainmentSurvey implements SealedVolume.World {
    private final Level level;
    private final BlockPos origin;
    private final SealedVolume fill;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private ChunkReader reader;
    private boolean reachedUnloaded;

    /**
     * Reads blocks through the chunk it last looked up: the fills walk from cell to neighbouring
     * cell, so that chunk answers most reads without asking the chunk cache. Unloaded chunks are
     * never loaded. Chunks can unload between ticks, so a reader lives for one tick at most.
     */
    static final class ChunkReader {
        private final Level level;
        @Nullable
        private ChunkAccess chunk;
        private int chunkX = Integer.MIN_VALUE;
        private int chunkZ;

        ChunkReader(Level level) {
            this.level = level;
        }

        /** The loaded chunk holding column ({@code x}, {@code z}), or {@code null}. */
        @Nullable
        ChunkAccess chunk(int x, int z) {
            int cx = SectionPos.blockToSectionCoord(x), cz = SectionPos.blockToSectionCoord(z);
            if (cx != chunkX || cz != chunkZ) {
                chunkX = cx;
                chunkZ = cz;
                chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
            }
            return chunk;
        }

        /** Whether the cell is inside the build height and its chunk is loaded. */
        boolean loaded(int x, int y, int z) {
            return !level.isOutsideBuildHeight(y) && chunk(x, z) != null;
        }

        /** The block at a loaded {@code pos}. */
        BlockState state(BlockPos pos) {
            ChunkAccess at = chunk(pos.getX(), pos.getZ());
            return at != null ? at.getBlockState(pos) : level.getBlockState(pos);
        }
    }

    ContainmentSurvey(Level level, BlockPos origin, int maxVolume) {
        this.level = level;
        this.origin = origin.immutable();
        this.fill = new SealedVolume(origin.getX(), origin.getY(), origin.getZ(), maxVolume);
    }

    /** A whole survey at once, for benchmarks and tests: the fill from {@code origin}, finished. */
    public static SealedVolume measure(Level level, BlockPos origin, int maxVolume) {
        ContainmentSurvey survey = new ContainmentSurvey(level, origin, maxVolume);
        while (survey.step(Integer.MAX_VALUE) == SealedVolume.State.FILLING) {
            // keeps going until sealed or leaked
        }
        return survey.fill();
    }

    /** Whether gas stops at this block. */
    static boolean isWall(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isCollisionShapeFullBlock(level, pos);
    }

    SealedVolume.State step(int budget) {
        reader = new ChunkReader(level);
        return fill.step(this, budget);
    }

    SealedVolume fill() {
        return fill;
    }

    boolean reachedUnloaded() {
        return reachedUnloaded;
    }

    @Override
    public boolean known(int x, int y, int z) {
        if (level.isOutsideBuildHeight(y)) {
            return false;
        }
        if (reader.chunk(x, z) == null) {
            reachedUnloaded = true;
            return false;
        }
        return true;
    }

    @Override
    public boolean passable(int x, int y, int z) {
        if (x == origin.getX() && y == origin.getY() && z == origin.getZ()) {
            return true;
        }
        cursor.set(x, y, z);
        return !isWall(reader.state(cursor), level, cursor);
    }

    @Override
    public double wallRating(int x, int y, int z) {
        // ContainmentShape rates the walls that face out once the fill is done.
        return 0.0;
    }
}
