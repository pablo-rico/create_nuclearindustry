package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;

/**
 * One measurement of a containment building: a {@link SealedVolume} flood fill from the monitor,
 * run a budget of blocks per tick. Gas passes every block without a full collision box, so open
 * doors, slabs, pipes and air leak; the monitor's own block counts as air, since it stands inside.
 * Remembers whether the fill ran into unloaded chunks, which says nothing about the building.
 */
final class ContainmentSurvey implements SealedVolume.World {
    private final Level level;
    private final BlockPos origin;
    private final SealedVolume fill;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private boolean reachedUnloaded;

    ContainmentSurvey(Level level, BlockPos origin, int maxVolume) {
        this.level = level;
        this.origin = origin.immutable();
        this.fill = new SealedVolume(origin.getX(), origin.getY(), origin.getZ(), maxVolume);
    }

    /** Whether gas stops at this block. */
    static boolean isWall(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isCollisionShapeFullBlock(level, pos);
    }

    SealedVolume.State step(int budget) {
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
        cursor.set(x, y, z);
        boolean known = !level.isOutsideBuildHeight(cursor) && level.isLoaded(cursor);
        if (!known && !level.isOutsideBuildHeight(cursor)) {
            reachedUnloaded = true;
        }
        return known;
    }

    @Override
    public boolean passable(int x, int y, int z) {
        if (x == origin.getX() && y == origin.getY() && z == origin.getZ()) {
            return true;
        }
        cursor.set(x, y, z);
        return !isWall(level.getBlockState(cursor), level, cursor);
    }

    @Override
    public double wallRating(int x, int y, int z) {
        // ContainmentShape rates the walls that face out once the fill is done.
        return 0.0;
    }
}
