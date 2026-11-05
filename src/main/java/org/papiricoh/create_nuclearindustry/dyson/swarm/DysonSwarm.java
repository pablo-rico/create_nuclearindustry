package org.papiricoh.create_nuclearindustry.dyson.swarm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.dyson.DysonConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * The world's one Dyson swarm: {@link OrbitShape#COUNT} orbits round the sun and the sails in each ({@link SailCohorts}), kept with
 * the Overworld, the only level under that sun. Ejectors add sails; once a second the worn-out ones go, the swarm works out how much
 * power it beams down ({@link SwarmPower}) and splits it among the ray receivers that asked for some during the last second.
 * Players get a copy ({@link SwarmSyncPayload}) when it changes, on joining and every {@link DysonConfig#syncSeconds}.
 */
public final class DysonSwarm extends SavedData {
    public static final String NAME = "create_nuclearindustry_dyson_swarm";
    /** Sail counts that award the swarm's advancements as they are passed. */
    public static final long MILESTONE_SMALL = 100, MILESTONE_LARGE = 1000;

    private final OrbitShape[] shapes = new OrbitShape[OrbitShape.COUNT];
    private final SailCohorts[] sails = new SailCohorts[OrbitShape.COUNT];
    /** What each receiver asked for since the last second's split, and what it was given then. */
    private final Map<BlockPos, Integer> requests = new HashMap<>();
    private Map<BlockPos, Integer> shares = Map.of();
    private double pool;
    private double sunFactor;
    private int delivered;
    private long seconds;
    private boolean changed = true;

    private DysonSwarm() {
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            shapes[i] = OrbitShape.defaults(i);
            sails[i] = new SailCohorts();
        }
    }

    private static Factory<DysonSwarm> factory() {
        return new Factory<>(DysonSwarm::new, DysonSwarm::load, null);
    }

    /** The swarm, kept with the Overworld of {@code level}'s server; made on first use. */
    public static DysonSwarm get(ServerLevel level) {
        return get(level.getServer());
    }

    public static DysonSwarm get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    @Nullable
    public static DysonSwarm getIfPresent(MinecraftServer server) {
        return server.overworld().getDataStorage().get(factory(), NAME);
    }

    /** Whether {@code level} is under the swarm's sun: only the Overworld. */
    public static boolean underSwarm(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && level.getGameTime() % 20 == 0) {
            DysonSwarm swarm = getIfPresent(level.getServer());
            if (swarm != null) swarm.tick(level);
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    private static void sync(ServerPlayer player) {
        DysonSwarm swarm = getIfPresent(player.server);
        SwarmSyncPayload.send(player, swarm == null ? SwarmSyncPayload.EMPTY : swarm.snapshot(player.server.overworld().getGameTime()));
    }

    public OrbitShape shape(int orbit) {
        return shapes[orbit];
    }

    public void setShape(int orbit, OrbitShape shape) {
        if (!OrbitShape.validIndex(orbit) || shape.equals(shapes[orbit])) return;
        shapes[orbit] = shape;
        changed();
    }

    public long sails(int orbit) {
        return sails[orbit].total();
    }

    public long totalSails() {
        long total = 0;
        for (SailCohorts cohorts : sails) total += cohorts.total();
        return total;
    }

    /** Whether one more sail fits under {@link DysonConfig#maxSails}. */
    public boolean full() {
        return totalSails() >= DysonConfig.maxSails;
    }

    /**
     * Puts a sail into {@code orbit} at {@code now}, to wear out {@link DysonConfig#sailLifetimeTicks} later. Fails, returning
     * false, if the orbit is closed or the swarm full.
     */
    public boolean launch(int orbit, long now) {
        if (!OrbitShape.validIndex(orbit) || !shapes[orbit].enabled() || full()) return false;
        sails[orbit].add(now + DysonConfig.sailLifetimeTicks(), 1);
        changed();
        return true;
    }

    /** Sails for tests and commands: {@code count} into {@code orbit}, wearing out at {@code expiresAt}, past any cap. */
    public void addSails(int orbit, int count, long expiresAt) {
        if (!OrbitShape.validIndex(orbit) || count <= 0) return;
        sails[orbit].add(expiresAt, count);
        changed();
    }

    /** Takes {@code count} sails out of {@code orbit}, the oldest first. */
    public long removeSails(int orbit, long count) {
        if (!OrbitShape.validIndex(orbit)) return 0;
        long gone = sails[orbit].removeOldest(count);
        if (gone > 0) changed();
        return gone;
    }

    /** Sails of {@code orbit} that wear out within {@code ticks} of {@code now}. */
    public long expiringWithin(int orbit, long now, long ticks) {
        return sails[orbit].expiringBefore(now + ticks);
    }

    /** A ray receiver at {@code pos} asks for up to {@code demand} FE per tick until the next split. */
    public void request(BlockPos pos, int demand) {
        if (demand > 0) requests.put(pos.immutable(), demand);
    }

    /** What the receiver at {@code pos} was given at the last split, FE per tick. */
    public int share(BlockPos pos) {
        return shares.getOrDefault(pos, 0);
    }

    /** Receivers given a share at the last split. */
    public int receivers() {
        return shares.size();
    }

    /** FE per tick the swarm sent down at the last split, before sharing. */
    public double pool() {
        return pool;
    }

    /** What the receivers took of it, FE per tick. */
    public int delivered() {
        return delivered;
    }

    /** The share of the sails' light reaching the ground at the last split, 0 to 1. */
    public double sunFactor() {
        return sunFactor;
    }

    /** The sun's share of the sails' light over {@code level} right now. */
    public static double sunFactorNow(Level level) {
        return SwarmPower.sunFactor(SunPath.sinElevation(level.getTimeOfDay(1)), level.getRainLevel(1), level.getThunderLevel(1),
                DysonConfig.minSinElevation(), DysonConfig.rainFactor, DysonConfig.thunderFactor);
    }

    /** Once a second, on the Overworld: wear, the receivers' split, and the players' copy. */
    public void tick(ServerLevel overworld) {
        seconds++;
        long now = overworld.getGameTime();
        for (SailCohorts cohorts : sails) if (cohorts.expire(now) > 0) changed();
        long over = totalSails() - DysonConfig.maxSails;
        for (int i = 0; i < sails.length && over > 0; i++) over -= removeSails(i, over);

        sunFactor = sunFactorNow(overworld);
        pool = SwarmPower.pool(totalSails(), DysonConfig.fePerSail, sunFactor);
        BlockPos[] asking = requests.keySet().toArray(new BlockPos[0]);
        int[] demands = new int[asking.length];
        for (int i = 0; i < asking.length; i++) demands[i] = requests.get(asking[i]);
        int[] given = SwarmPower.allocate(pool, demands);
        Map<BlockPos, Integer> next = new HashMap<>();
        int sum = 0;
        for (int i = 0; i < asking.length; i++) {
            next.put(asking[i], given[i]);
            sum += given[i];
        }
        // The beams brighten and dim with the sun; players hear of it when it is enough to see.
        if (next.size() != shares.size() || Math.abs(sum - delivered) > Math.max(16, 0.03 * delivered)) changed = true;
        shares = next;
        delivered = sum;
        requests.clear();

        if (changed || seconds % Math.max(1, DysonConfig.syncSeconds) == 0) {
            changed = false;
            SwarmSyncPayload.send(overworld.getServer(), snapshot(now));
        }
    }

    /** The swarm as the players are told it. */
    public SwarmSyncPayload snapshot(long now) {
        long[] counts = new long[OrbitShape.COUNT], expiring = new long[OrbitShape.COUNT];
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            counts[i] = sails[i].total();
            expiring[i] = sails[i].expiringBefore(now + 24000);
        }
        return new SwarmSyncPayload(shapes.clone(), counts, expiring, (float) pool, (float) sunFactor, shares.size(), delivered);
    }

    /** Back to a new world's swarm: no sails, default orbits, no receivers. For tests. */
    public void reset() {
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            shapes[i] = OrbitShape.defaults(i);
            sails[i].clear();
        }
        requests.clear();
        shares = Map.of();
        pool = 0;
        delivered = 0;
        changed();
    }

    private void changed() {
        changed = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag orbits = new ListTag();
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            CompoundTag entry = new CompoundTag();
            OrbitShape shape = shapes[i];
            entry.putBoolean("Enabled", shape.enabled());
            entry.putFloat("Radius", shape.radius());
            entry.putFloat("Inclination", shape.inclination());
            entry.putFloat("Node", shape.node());
            entry.putLongArray("Expires", sails[i].marks());
            entry.putIntArray("Counts", sails[i].counts());
            orbits.add(entry);
        }
        tag.put("Orbits", orbits);
        return tag;
    }

    /** Public for tests: a swarm as it would load from {@code tag}. */
    public static DysonSwarm load(CompoundTag tag, HolderLookup.Provider registries) {
        DysonSwarm swarm = new DysonSwarm();
        ListTag orbits = tag.getList("Orbits", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(OrbitShape.COUNT, orbits.size()); i++) {
            CompoundTag entry = orbits.getCompound(i);
            swarm.shapes[i] = new OrbitShape(entry.getBoolean("Enabled"), entry.getFloat("Radius"), entry.getFloat("Inclination"),
                    entry.getFloat("Node"));
            swarm.sails[i] = SailCohorts.of(entry.getLongArray("Expires"), entry.getIntArray("Counts"));
        }
        return swarm;
    }
}
