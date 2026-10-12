package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Where the structure builders put their blocks: a game test arena (positions relative to the
 * test) or a corner of a real world (positions relative to that corner). The same builders then
 * make the machines the tests prove and the ones {@code /nuclear spawn} places for players.
 */
public interface BlockPlacer {

    void set(BlockPos pos, BlockState state);

    default void set(BlockPos pos, Block block) {
        set(pos, block.defaultBlockState());
    }

    /** The block entity at {@code pos} if it is a {@code T}, else null. */
    @Nullable
    <T extends BlockEntity> T blockEntity(BlockPos pos, Class<T> type);

    /** Inside a game test, with the test's relative positions. */
    static BlockPlacer of(GameTestHelper helper) {
        return new BlockPlacer() {
            @Override
            public void set(BlockPos pos, BlockState state) {
                helper.setBlock(pos, state);
            }

            @Override
            public <T extends BlockEntity> T blockEntity(BlockPos pos, Class<T> type) {
                BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                return type.isInstance(entity) ? type.cast(entity) : null;
            }
        };
    }

    /** In a world, with positions relative to {@code origin}. */
    static BlockPlacer of(ServerLevel level, BlockPos origin) {
        return new BlockPlacer() {
            @Override
            public void set(BlockPos pos, BlockState state) {
                level.setBlock(origin.offset(pos), state, Block.UPDATE_ALL);
            }

            @Override
            public <T extends BlockEntity> T blockEntity(BlockPos pos, Class<T> type) {
                BlockEntity entity = level.getBlockEntity(origin.offset(pos));
                return type.isInstance(entity) ? type.cast(entity) : null;
            }
        };
    }
}
