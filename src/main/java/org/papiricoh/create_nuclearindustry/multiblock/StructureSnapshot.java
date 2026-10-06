package org.papiricoh.create_nuclearindustry.multiblock;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block found at every position of a formed structure. Lets a controller ignore
 * state-only updates (a lamp turning on, a sensor's redstone power) and react only when
 * a block is actually replaced.
 */
public final class StructureSnapshot {
    private final Long2ObjectOpenHashMap<Block> blocks;

    private StructureSnapshot(Long2ObjectOpenHashMap<Block> blocks) {
        this.blocks = blocks;
    }

    public static StructureSnapshot capture(Level level, Iterable<BlockPos> positions) {
        Long2ObjectOpenHashMap<Block> blocks = new Long2ObjectOpenHashMap<>();
        for (BlockPos pos : positions) {
            blocks.put(pos.asLong(), level.getBlockState(pos).getBlock());
        }
        return new StructureSnapshot(blocks);
    }

    public boolean contains(BlockPos pos) {
        return blocks.containsKey(pos.asLong());
    }

    /** True if {@code pos} is tracked and the change leaves a different block there. */
    public boolean isAffectedBy(BlockChange change) {
        Block expected = blocks.get(change.pos().asLong());
        if (expected == null) {
            return false;
        }
        if (change.removed()) {
            return true;
        }
        BlockState state = change.state();
        return state.getBlock() != expected;
    }
}
