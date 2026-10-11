package org.papiricoh.create_nuclearindustry.radiation.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The radiation system's caches and what keeps them honest: each player's {@link RayCache}, each
 * level's {@link ChunkChangeLog} fed by block events, and {@link BlockRadioactivity}, forgotten
 * when data maps reload. Server side only.
 * <p>
 * Block events cover players, explosions, fluids and every change that updates its neighbours
 * (machines, pistons, commands, game tests). A change that tells nobody (a raw state change
 * without neighbour updates) is only noticed when the cached ray expires, after
 * {@code performance.radiationRayCacheTicks}.
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
    }

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
        cache.rays.prune(player.level().getGameTime());
        return cache;
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
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PLAYERS.remove(event.getEntity());
    }
}
