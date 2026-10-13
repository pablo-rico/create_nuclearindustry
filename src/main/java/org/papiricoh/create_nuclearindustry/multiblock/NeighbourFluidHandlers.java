package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * The fluid handlers on the six sides of a block that trades with its neighbours every tick (ports,
 * dual pipes, penetrations). Looking each one up every tick cost a block entity and a capability
 * lookup per side per tick; NeoForge's {@link BlockCapabilityCache} keeps the answer until the level
 * says it may have changed (a neighbour placed, removed or reconfigured, a chunk loaded or unloaded),
 * which is the same answer a fresh lookup gives. The caches are made the first time a side is asked
 * for and kept while the owner lives.
 *
 * <p>{@code skip} leaves out neighbours the owner must not trade with (another port of the same
 * machine, say); it is asked again only when that side's cache was invalidated, which happens
 * whenever a block entity is placed or removed there.
 */
public final class NeighbourFluidHandlers {
    private final BlockEntity owner;
    @Nullable
    private final Predicate<BlockEntity> skip;
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IFluidHandler, Direction>[] caches = new BlockCapabilityCache[6];
    private final boolean[] skipKnown = new boolean[6];
    private final boolean[] skipped = new boolean[6];
    @Nullable
    private ServerLevel cacheLevel;
    private int lookups;

    /** {@code skip} may be null when every neighbour counts. */
    public NeighbourFluidHandlers(BlockEntity owner, @Nullable Predicate<BlockEntity> skip) {
        this.owner = owner;
        this.skip = skip;
    }

    /** The handler on the face of the neighbour across {@code side}, or null when there is none or it is skipped. */
    @Nullable
    public IFluidHandler get(Direction side) {
        if (!(owner.getLevel() instanceof ServerLevel level) || owner.isRemoved()) {
            return null;
        }
        if (level != cacheLevel) {
            java.util.Arrays.fill(caches, null);
            java.util.Arrays.fill(skipKnown, false);
            cacheLevel = level;
        }
        int i = side.ordinal();
        BlockCapabilityCache<IFluidHandler, Direction> cache = caches[i];
        if (cache == null) {
            BlockPos pos = owner.getBlockPos().relative(side);
            cache = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, pos, side.getOpposite(),
                    () -> !owner.isRemoved(), () -> skipKnown[i] = false);
            caches[i] = cache;
        }
        if (!skipKnown[i]) {
            lookups++;
            BlockPos pos = cache.pos();
            skipped[i] = skip != null && level.isLoaded(pos) && skip.test(level.getBlockEntity(pos));
            skipKnown[i] = true;
        }
        // Asked every time: a query re-arms the cache's invalidation listener.
        IFluidHandler handler = cache.getCapability();
        return skipped[i] ? null : handler;
    }

    /** Sides looked up afresh since the owner was placed: once each, then once per change next to it. For tests. */
    public int lookups() {
        return lookups;
    }
}
