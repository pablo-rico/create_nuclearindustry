package org.papiricoh.create_nuclearindustry.waste.repository;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.papiricoh.create_nuclearindustry.waste.WasteConfig;
import org.papiricoh.create_nuclearindustry.waste.WastePackage;
import org.papiricoh.create_nuclearindustry.waste.WasteTags;

import java.util.HashMap;
import java.util.Map;

/**
 * Samples the world around a waste package into a {@link RepositoryQuality.Site}. The scan
 * touches about a thousand blocks, and radiation and random ticks ask for the same packages
 * over and over, so sites are cached per position for {@link WasteConfig#probeCacheTicks}.
 * Only the surroundings are cached; the package's own form is applied on every call, so
 * swapping a barrel for a canister shows at once.
 */
public final class RepositoryProbe {
    /** Radius of the host rock shell around a package. */
    public static final int HOST_ROCK_RADIUS = 3;
    /** A fluid source this close counts as groundwater reaching the package. */
    public static final int FLUID_RADIUS = 4;
    /** Above this many cached sites in a level, expired ones are swept out. */
    private static final int SWEEP_THRESHOLD = 4096;

    private record Cached(RepositoryQuality.Site site, long expires) {}

    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Cached>> CACHE = new HashMap<>();

    private RepositoryProbe() {}

    /** The score breakdown of a package of {@code pack} at {@code pos}, with the configured weights. */
    public static RepositoryQuality.Breakdown assess(Level level, BlockPos pos, WastePackage pack) {
        return RepositoryQuality.assess(site(level, pos), pack.form(), WasteConfig.weights, WasteConfig.depthSpan);
    }

    /** The surroundings of {@code pos}, from the cache while still fresh. */
    public static synchronized RepositoryQuality.Site site(Level level, BlockPos pos) {
        long now = level.getGameTime();
        Long2ObjectOpenHashMap<Cached> sites = CACHE.computeIfAbsent(level.dimension(), key -> new Long2ObjectOpenHashMap<>());
        long key = pos.asLong();
        Cached cached = sites.get(key);
        if (cached != null && cached.expires() > now && cached.expires() - now <= WasteConfig.probeCacheTicks) {
            return cached.site();
        }
        if (sites.size() > SWEEP_THRESHOLD) {
            sites.values().removeIf(entry -> entry.expires() <= now);
        }
        RepositoryQuality.Site site = sample(level, pos);
        sites.put(key, new Cached(site, now + WasteConfig.probeCacheTicks));
        return site;
    }

    /** Drops every cached site, for tests and after the world around packages changed in bulk. */
    public static synchronized void clearCache() {
        CACHE.clear();
    }

    /** Looks at the blocks around {@code pos} now, without the cache. */
    public static RepositoryQuality.Site sample(Level level, BlockPos pos) {
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, pos.getX(), pos.getZ());
        int depth = Math.max(0, surface - 1 - pos.getY());
        return new RepositoryQuality.Site(depth, hostRockFraction(level, pos), backfilledFaces(level, pos), fluidNearby(level, pos));
    }

    /**
     * Share of the natural blocks within {@link #HOST_ROCK_RADIUS} that are host rock. Backfill and
     * other packages are the repository's own barrier, not the rock it sits in, so they are left
     * out; a package wrapped entirely in them counts as fully hosted.
     */
    static double hostRockFraction(Level level, BlockPos pos) {
        int rock = 0;
        int natural = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -HOST_ROCK_RADIUS; dx <= HOST_ROCK_RADIUS; dx++) {
            for (int dy = -HOST_ROCK_RADIUS; dy <= HOST_ROCK_RADIUS; dy++) {
                for (int dz = -HOST_ROCK_RADIUS; dz <= HOST_ROCK_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState state = level.getBlockState(at);
                    if (isBackfill(state)) {
                        continue;
                    }
                    natural++;
                    if (state.is(WasteTags.REPOSITORY_HOST_ROCK)) {
                        rock++;
                    }
                }
            }
        }
        return natural == 0 ? 1.0 : rock / (double) natural;
    }

    static int backfilledFaces(Level level, BlockPos pos) {
        int faces = 0;
        for (Direction direction : Direction.values()) {
            if (isBackfill(level.getBlockState(pos.relative(direction)))) {
                faces++;
            }
        }
        return faces;
    }

    static boolean fluidNearby(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -FLUID_RADIUS; dx <= FLUID_RADIUS; dx++) {
            for (int dy = -FLUID_RADIUS; dy <= FLUID_RADIUS; dy++) {
                for (int dz = -FLUID_RADIUS; dz <= FLUID_RADIUS; dz++) {
                    at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    var fluid = level.getFluidState(at);
                    if (!fluid.isEmpty() && fluid.isSource()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isBackfill(BlockState state) {
        return state.is(WasteTags.REPOSITORY_BACKFILL) || WastePackage.of(state) != null;
    }
}
