package org.papiricoh.create_nuclearindustry.radiation.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The radiation system's caches and what keeps them honest: each player's {@link RayCache}, each
 * level's {@link RayCache} for monitored spots (control room area targets), each level's
 * {@link ChunkChangeLog} fed by block events, and {@link BlockRadioactivity}, forgotten when data
 * maps reload. Everything per level, including the {@link RadiationEmitters} index, is dropped
 * when the level unloads or the server stops, so no level outlives its world. Server side only.
 * <p>
 * Block events cover players, explosions, fluids and every change that updates its neighbours
 * (machines, pistons, commands, game tests). A change that tells nobody (a raw state change
 * without neighbour updates) is only noticed when the cached ray is cast again: once it goes
 * unused for {@code performance.radiationRayCacheTicks}, or at the latest after
 * {@code performance.radiationRayMaxAgeTicks}.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID)
public final class RadiationCaches {
    /** How often a level's change log forgets chunks no cached ray can depend on. */
    private static final int FORGET_INTERVAL = 1200;

    private static final Map<Level, Changes> CHANGES = new WeakHashMap<>();
    private static final Map<Player, PlayerCache> PLAYERS = new WeakHashMap<>();

    private static final class Changes {
        final ChunkChangeLog log = new ChunkChangeLog();
        long lastForget;
        /** Rays to fixed spots of this level; keys hold the spot's block, so one cache serves them all. */
        final RayCache spots = new RayCache();
        long lastSpotPrune = Long.MIN_VALUE;
        /** Whole readings of monitored spots by packed block position, see {@link SpotReading}. */
        final Long2ObjectOpenHashMap<SpotReading> spotReadings = new Long2ObjectOpenHashMap<>();
    }

    /**
     * A monitored spot's whole ambient rate: the block scan, the emitter and fallout lookups, the
     * ground and every ray, taken at {@code time} and valid for {@link #SPOT_READING_TICKS} (the
     * players' own update interval) as long as no chunk around the spot changed after
     * {@code sequence} was issued by the level's {@link ChunkChangeLog}.
     */
    public record SpotReading(double rate, long time, long sequence) {}

    /** Ticks a spot's whole reading is reused: players are read every 20 ticks, so are spots. */
    public static final int SPOT_READING_TICKS = 20;

    /** One player's cached rays, for the level they were cast in. */
    public static final class PlayerCache {
        private Level level;
        private final RayCache rays = new RayCache();

        public RayCache rays() {
            return rays;
        }
    }

    private RadiationCaches() {}

    /** The change log of a server level. */
    public static ChunkChangeLog changes(Level level) {
        return changesOf(level).log;
    }

    private static Changes changesOf(Level level) {
        return CHANGES.computeIfAbsent(level, key -> new Changes());
    }

    /** The player's cache, emptied when they change level and pruned of expired rays. */
    public static PlayerCache of(Player player) {
        PlayerCache cache = PLAYERS.computeIfAbsent(player, key -> new PlayerCache());
        if (cache.level != player.level()) {
            cache.level = player.level();
            cache.rays.clear();
        }
        cache.rays.prune(player.level().getGameTime(), PerformanceConfig.radiationRayCacheTicks, PerformanceConfig.radiationRayMaxAgeTicks);
        return cache;
    }

    /**
     * The level's cache of rays to fixed spots (the areas control room monitors watch), with the
     * same lifetime and invalidation as a player's; pruned at most once a tick.
     */
    public static RayCache spots(Level level) {
        Changes changes = changesOf(level);
        long now = level.getGameTime();
        if (changes.lastSpotPrune != now) {
            changes.lastSpotPrune = now;
            changes.spots.prune(now, PerformanceConfig.radiationRayCacheTicks, PerformanceConfig.radiationRayMaxAgeTicks);
            changes.spotReadings.values().removeIf(reading -> now - reading.time() >= SPOT_READING_TICKS || now < reading.time());
        }
        return changes.spots;
    }

    /** The last whole reading of the spot at packed position {@code key}, if any is kept. */
    @Nullable
    public static SpotReading spotReading(Level level, long key) {
        return changesOf(level).spotReadings.get(key);
    }

    public static void rememberSpotReading(Level level, long key, SpotReading reading) {
        changesOf(level).spotReadings.put(key, reading);
    }

    /** Whether any cache of {@code level} is still held, for tests of the unload clean-up. */
    public static boolean tracks(Level level) {
        return CHANGES.containsKey(level);
    }

    /** Whether a player's cache is still held, for tests of the stop clean-up. */
    public static boolean tracks(Player player) {
        return PLAYERS.containsKey(player);
    }

    /** Records that the block at {@code pos} changed, so rays across its chunk are cast again. */
    public static void blockChanged(LevelAccessor accessor, BlockPos pos) {
        if (!(accessor instanceof ServerLevel level)) {
            return;
        }
        Changes changes = changesOf(level);
        long now = level.getGameTime();
        changes.log.mark(pos.getX() >> 4, pos.getZ() >> 4, now);
        if (now - changes.lastForget >= FORGET_INTERVAL) {
            changes.lastForget = now;
            changes.log.forgetBefore(now - PerformanceConfig.radiationRayCacheTicks - 20);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        blockChanged(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            multi.getReplacedBlockSnapshots().forEach(snapshot -> blockChanged(event.getLevel(), snapshot.getPos()));
        } else {
            blockChanged(event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        blockChanged(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        blockChanged(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        for (BlockPos pos : event.getAffectedBlocks()) {
            blockChanged(event.getLevel(), pos);
        }
    }

    @SubscribeEvent
    public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
        BlockRadioactivity.invalidate();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            CHANGES.remove(level);
            RadiationEmitters.forget(level);
        }
    }

    /** Clears every level's caches and index: a single player world's levels must not outlive it. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CHANGES.clear();
        PLAYERS.clear();
        RadiationEmitters.forgetAll();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYERS.remove(event.getEntity());
    }
}
