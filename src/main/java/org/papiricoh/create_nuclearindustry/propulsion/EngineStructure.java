package org.papiricoh.create_nuclearindustry.propulsion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * A nuclear engine laid out in a straight line from its controller towards the way the exhaust leaves:
 * one or more segments along that axis, then a nozzle facing the same way. A nozzle with nothing behind
 * it is still a nozzle, so a ship carries the engine wherever it points.
 *
 * @param segments segments between the controller and the nozzle
 * @param nozzle   the nozzle's position
 */
public record EngineStructure(int segments, BlockPos nozzle) {

    /** The engine whose controller is at {@code controller} with the exhaust towards {@code exhaust}, or {@code null} if it is not built. */
    @Nullable
    public static EngineStructure scan(BlockGetter level, BlockPos controller, Direction exhaust, Block segment, Block nozzle, int maxSegments) {
        BlockPos.MutableBlockPos at = controller.mutable();
        int count = 0;
        while (count <= maxSegments) {
            at.move(exhaust);
            BlockState state = level.getBlockState(at);
            if (state.is(segment) && state.getValue(RotatedPillarBlock.AXIS) == exhaust.getAxis()) {
                count++;
                continue;
            }
            if (state.is(nozzle) && state.getValue(BlockStateProperties.FACING) == exhaust && count > 0) {
                return new EngineStructure(count, at.immutable());
            }
            return null;
        }
        return null;
    }
}
