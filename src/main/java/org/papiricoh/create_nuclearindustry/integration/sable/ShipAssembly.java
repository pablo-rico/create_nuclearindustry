package org.papiricoh.create_nuclearindustry.integration.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;

import java.util.function.UnaryOperator;

/**
 * A ship being put together, split or taken apart. Sable copies each block entity to its new place
 * and then removes the old block, so for a moment our blocks look broken: a Penning trap would
 * annihilate its antimatter and an inventory would spill a copy of what it holds. Removal code asks
 * {@link #isMoving()} first. Machines a control room may be linked to leave a forwarding address in
 * {@link BlockMoves}.
 */
public final class ShipAssembly {
    /** Server thread only: Sable moves blocks there. */
    private static int depth;

    private ShipAssembly() {}

    /** True while blocks are being moved to, from or between ships. */
    public static boolean isMoving() {
        return depth > 0;
    }

    public static void begin(ServerLevel level, UnaryOperator<BlockPos> transform, Iterable<BlockPos> blocks) {
        depth++;
        for (BlockPos pos : blocks) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null && LinkedTarget.isLinkTarget(entity)) {
                BlockMoves.record(level, pos, transform.apply(pos));
            }
        }
    }

    public static void end() {
        depth = Math.max(0, depth - 1);
    }
}
