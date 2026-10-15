package org.papiricoh.create_nuclearindustry.integration.sable;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Forwarding addresses of machines that moved with a ship, so a control room linked to one still
 * finds it after it is assembled into a ship, split off or set down again. A link keeps its old
 * position; {@link #follow} takes it to where the machine went, as long as nothing else took the
 * old place. Only the latest moves are kept.
 */
public final class BlockMoves extends SavedData {
    private static final String NAME = "create_nuclearindustry_block_moves";
    static final int MAX_MOVES = 4096;
    private static final int MAX_HOPS = 16;

    private final Long2LongLinkedOpenHashMap moves = new Long2LongLinkedOpenHashMap();

    public static BlockMoves get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(BlockMoves::new, BlockMoves::load, null), NAME);
    }

    static void record(ServerLevel level, BlockPos from, BlockPos to) {
        if (from.equals(to)) {
            return;
        }
        BlockMoves data = get(level);
        data.moves.remove(to.asLong());
        data.moves.putAndMoveToLast(from.asLong(), to.asLong());
        while (data.moves.size() > MAX_MOVES) {
            data.moves.removeFirstLong();
        }
        data.setDirty();
    }

    /**
     * Where the machine last seen at {@code pos} is now: {@code pos} itself while a block entity is
     * still there, otherwise the end of its chain of moves.
     */
    public static BlockPos follow(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !level.isLoaded(pos) || level.getBlockEntity(pos) != null) {
            return pos;
        }
        BlockMoves data = get(server);
        long at = pos.asLong();
        for (int hop = 0; hop < MAX_HOPS && data.moves.containsKey(at); hop++) {
            at = data.moves.get(at);
        }
        return BlockPos.of(at);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] pairs = new long[moves.size() * 2];
        int i = 0;
        for (var entry : moves.long2LongEntrySet()) {
            pairs[i++] = entry.getLongKey();
            pairs[i++] = entry.getLongValue();
        }
        tag.putLongArray("Moves", pairs);
        return tag;
    }

    private static BlockMoves load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockMoves data = new BlockMoves();
        long[] pairs = tag.getLongArray("Moves");
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            data.moves.put(pairs[i], pairs[i + 1]);
        }
        return data;
    }
}
