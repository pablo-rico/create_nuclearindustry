package org.papiricoh.create_nuclearindustry.radiation.contamination;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipContamination;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.ToDoubleBiFunction;

/**
 * Ground contamination, one value per chunk. Nothing ticks: values decay lazily when read,
 * fallout reaches unloaded chunks when they load, and clients only get a severity byte when it
 * changes (a synced chunk attachment), which they draw as dust on the ground.
 */
public final class Contamination {
    /** Below this a chunk counts as clean. */
    public static final float MIN_LEVEL = 0.01f;
    public static final int MAX_SEVERITY = ChunkContamination.MAX_SEVERITY;
    /** Loaded chunks with a contamination record, so a global clear does not walk every chunk. */
    private static final Map<Level, LongSet> TRACKED = new WeakHashMap<>();

    private Contamination() {}

    public static int severity(float level) {
        return ChunkContamination.severity(level);
    }

    @Nullable
    public static LevelChunk loaded(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    /** Current contamination of a chunk, or 0 if it is not loaded. */
    public static float get(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = loaded(level, pos.x, pos.z);
        return chunk == null ? 0 : read(chunk);
    }

    /** Current contamination of a loaded chunk, refreshing the synced severity if decay moved it. */
    public static float read(LevelChunk chunk) {
        ShipSpace.Ship ship = ShipContamination.shipOf(chunk.getLevel(), chunk.getPos());
        if (ship != null) {
            // A ship's chunks share its own contamination.
            return ShipContamination.get(ship);
        }
        ChunkContamination record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
        if (record == null || record.level() <= 0) {
            return 0;
        }
        float now = record.levelAt(chunk.getLevel().getGameTime(), Config.contaminationHalfLifeTicks);
        if (now < MIN_LEVEL || severity(now) != syncedSeverity(chunk)) {
            store(chunk, record, now);
        }
        return now < MIN_LEVEL ? 0 : now;
    }

    public static void set(LevelChunk chunk, float value) {
        ShipSpace.Ship ship = ShipContamination.shipOf(chunk.getLevel(), chunk.getPos());
        if (ship != null) {
            ShipContamination.set(ship, value);
            return;
        }
        ChunkContamination record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
        store(chunk, record == null ? fresh(chunk) : record, value);
    }

    /** Adds (or with a negative amount removes) contamination; returns the new value. */
    public static float add(LevelChunk chunk, float amount) {
        float value = Mth.clamp(read(chunk) + amount, 0, (float) Config.contaminationMax);
        set(chunk, value);
        return value < MIN_LEVEL ? 0 : value;
    }

    private static ChunkContamination fresh(LevelChunk chunk) {
        return chunk.getLevel() instanceof ServerLevel level
                ? ChunkContamination.EMPTY.withEpoch(ContaminationData.get(level).clearEpoch())
                : ChunkContamination.EMPTY;
    }

    private static void store(LevelChunk chunk, ChunkContamination record, float value) {
        value = Mth.clamp(value, 0, (float) Config.contaminationMax);
        if (value < MIN_LEVEL) {
            value = 0;
        }
        ChunkContamination updated = record.with(value, chunk.getLevel().getGameTime());
        if (value == 0 && updated.lastEvent() == 0) {
            // Nothing to remember: the default record is just as good.
            if (chunk.hasData(AllNuclearRadiation.CHUNK_CONTAMINATION.get())) {
                chunk.removeData(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
            }
            untrack(chunk);
        } else {
            chunk.setData(AllNuclearRadiation.CHUNK_CONTAMINATION.get(), updated);
            track(chunk);
        }
        syncSeverity(chunk, severity(value));
    }

    public static int syncedSeverity(LevelChunk chunk) {
        Byte severity = chunk.getExistingDataOrNull(AllNuclearRadiation.CONTAMINATION_SEVERITY.get());
        return severity == null ? 0 : severity;
    }

    /** Only a change of severity reaches the clients watching the chunk. */
    private static void syncSeverity(LevelChunk chunk, int severity) {
        if (severity == syncedSeverity(chunk)) {
            return;
        }
        if (severity == 0) {
            chunk.removeData(AllNuclearRadiation.CONTAMINATION_SEVERITY.get());
        } else {
            chunk.setData(AllNuclearRadiation.CONTAMINATION_SEVERITY.get(), (byte) severity);
        }
    }

    private static void track(LevelChunk chunk) {
        if (chunk.getLevel() instanceof ServerLevel level) {
            TRACKED.computeIfAbsent(level, l -> new LongOpenHashSet()).add(chunk.getPos().toLong());
        }
    }

    private static void untrack(LevelChunk chunk) {
        LongSet set = TRACKED.get(chunk.getLevel());
        if (set != null) {
            set.remove(chunk.getPos().toLong());
        }
    }

    // ---- fallout and clearing ----

    /**
     * Fallout centred on {@code center}: {@code intensity} rad/s there, falling linearly to
     * nothing at {@code radius} blocks. Loaded chunks get it now; the rest when they load.
     */
    public static void fallout(ServerLevel level, BlockPos center, int radius, float intensity) {
        fallout(level, center, radius, intensity, 0, 0);
    }

    /**
     * As {@link #fallout(ServerLevel, BlockPos, int, float)}, carried by a wind blowing towards
     * {@code wind} radians that stretches it by {@code stretch} into a plume (see {@code FalloutPlume}).
     */
    public static void fallout(ServerLevel level, BlockPos center, int radius, float intensity, double wind, double stretch) {
        if (!Config.contaminationEnabled || radius <= 0 || intensity <= 0) {
            return;
        }
        // Released aboard a ship: it comes down where the ship really is, on the ship too.
        center = ShipSpace.worldBlock(level, center);
        ContaminationData data = ContaminationData.get(level);
        ContaminationData.FalloutEvent event = data.addEvent(center.getX(), center.getZ(), radius, intensity, level.getGameTime(),
                (float) wind, (float) stretch);
        ChunkPos origin = new ChunkPos(center);
        int chunkRadius = ((int) Math.ceil(event.reach()) >> 4) + 1;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = loaded(level, origin.x + dx, origin.z + dz);
                if (chunk == null) {
                    continue;
                }
                float amount = event.at(chunk.getPos().getMiddleBlockX(), chunk.getPos().getMiddleBlockZ());
                if (amount > 0) {
                    settle(level, chunk, add(chunk, amount));
                    markSeen(chunk, event.id());
                }
            }
        }
        ShipContamination.fallout(level, event);
    }

    /** Heavy fallout also reaches the chunk's open water, a few columns per tick. */
    private static void settle(ServerLevel level, LevelChunk chunk, float value) {
        if (severity(value) >= WaterContamination.MIN_SEVERITY) {
            WaterContamination.queue(level, chunk.getPos());
        }
    }

    /** Records that the chunk has seen fallout events up to {@code eventId}, so it never gets them twice. */
    private static ChunkContamination markSeen(LevelChunk chunk, int eventId) {
        ChunkContamination record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
        ChunkContamination updated = (record == null ? fresh(chunk) : record).withLastEvent(eventId);
        chunk.setData(AllNuclearRadiation.CHUNK_CONTAMINATION.get(), updated);
        track(chunk);
        return updated;
    }

    /** Applies whatever happened while the chunk was unloaded: a global clear, then missed fallout. */
    public static void catchUp(ServerLevel level, LevelChunk chunk) {
        ContaminationData data = ContaminationData.get(level);
        ChunkContamination record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
        if (record != null && record.epoch() < data.clearEpoch()) {
            record = new ChunkContamination(0, level.getGameTime(), data.clearEpoch(), record.lastEvent());
            store(chunk, record, 0);
            record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
        }
        if (!data.events().isEmpty()) {
            int seen = record == null ? 0 : record.lastEvent();
            float missed = 0;
            int latest = seen;
            double cx = chunk.getPos().getMiddleBlockX();
            double cz = chunk.getPos().getMiddleBlockZ();
            for (ContaminationData.FalloutEvent event : data.events()) {
                if (event.id() <= seen) {
                    continue;
                }
                float amount = event.at(cx, cz);
                if (amount > 0) {
                    missed += ChunkContamination.decay(amount, level.getGameTime() - event.gameTime(), Config.contaminationHalfLifeTicks);
                    latest = Math.max(latest, event.id());
                }
            }
            if (latest > seen) {
                settle(level, chunk, add(chunk, missed));
                record = markSeen(chunk, latest);
            }
        }
        if (record != null) {
            track(chunk);
            read(chunk);
        }
    }

    /** Clears every chunk: loaded ones now, the others when they next load. Returns the chunks cleared now. */
    public static int clearAll(ServerLevel level) {
        int epoch = ContaminationData.get(level).clearAll();
        LongSet tracked = TRACKED.get(level);
        int cleared = 0;
        if (tracked != null) {
            for (long packed : tracked.toLongArray()) {
                LevelChunk chunk = loaded(level, ChunkPos.getX(packed), ChunkPos.getZ(packed));
                if (chunk == null) {
                    tracked.remove(packed);
                    continue;
                }
                ChunkContamination record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
                if (record != null) {
                    if (record.level() > 0) {
                        cleared++;
                    }
                    store(chunk, record.withEpoch(epoch), 0);
                }
            }
        }
        return cleared;
    }

    /**
     * Lets contamination spread: each step, every loaded chunk within {@code radius} chunks of
     * {@code center} passes {@code fraction} of the difference to each lower neighbour (split four
     * ways). The total stays the same; nothing leaves the area.
     */
    public static int expand(ServerLevel level, ChunkPos center, int radius, int steps, float fraction) {
        int size = 2 * radius + 1;
        LevelChunk[] chunks = new LevelChunk[size * size];
        float[] values = new float[size * size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                LevelChunk chunk = loaded(level, center.x - radius + i, center.z - radius + j);
                chunks[i * size + j] = chunk;
                values[i * size + j] = chunk == null ? 0 : read(chunk);
            }
        }
        int[][] neighbours = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int step = 0; step < steps; step++) {
            float[] next = values.clone();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    int from = i * size + j;
                    if (chunks[from] == null || values[from] <= 0) {
                        continue;
                    }
                    for (int[] offset : neighbours) {
                        int ni = i + offset[0];
                        int nj = j + offset[1];
                        if (ni < 0 || nj < 0 || ni >= size || nj >= size || chunks[ni * size + nj] == null) {
                            continue;
                        }
                        int to = ni * size + nj;
                        float flow = (values[from] - values[to]) * fraction / 4;
                        if (flow > 0) {
                            next[from] -= flow;
                            next[to] += flow;
                        }
                    }
                }
            }
            values = next;
        }
        int touched = 0;
        for (int k = 0; k < chunks.length; k++) {
            if (chunks[k] != null && (values[k] > 0 || read(chunks[k]) > 0)) {
                set(chunks[k], values[k]);
                touched++;
            }
        }
        return touched;
    }

    // ---- exposure ----

    /**
     * Dose rate at {@code body} from contaminated ground: the four nearest chunk values blended
     * by position, less with height above the ground, and only what gets through the roof when
     * the player is under cover.
     */
    public static double exposure(ServerLevel level, Vec3 body, double feetY) {
        return exposure(level, body, feetY, (from, to) -> RadiationRay.transmission(level, from, to));
    }

    /** The same, with the ray through the roof cast (or looked up in a cache) by {@code roofRay}. */
    public static double exposure(ServerLevel level, Vec3 body, double feetY, ToDoubleBiFunction<Vec3, Vec3> roofRay) {
        double x = body.x / 16.0 - 0.5;
        double z = body.z / 16.0 - 0.5;
        int x0 = Mth.floor(x);
        int z0 = Mth.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        double value = (1 - fx) * (1 - fz) * get(level, new ChunkPos(x0, z0))
                + fx * (1 - fz) * get(level, new ChunkPos(x0 + 1, z0))
                + (1 - fx) * fz * get(level, new ChunkPos(x0, z0 + 1))
                + fx * fz * get(level, new ChunkPos(x0 + 1, z0 + 1));
        if (value <= 0) {
            return 0;
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(body.x), Mth.floor(body.z));
        double height = feetY - surface;
        double factor;
        if (height > -0.5) {
            double above = Math.max(0, height);
            factor = 1.0 / (1.0 + (above / 8.0) * (above / 8.0));
        } else {
            factor = roofRay.applyAsDouble(new Vec3(body.x, surface + 0.5, body.z), body);
        }
        return value * factor * Config.contaminationDoseScale;
    }

    // ---- events ----

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            catchUp(level, chunk);
        }
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            untrack(chunk);
        }
    }

    /** Before a chunk is sent, bring its severity up to date so the initial sync is right. */
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        if (event.getChunk().hasData(AllNuclearRadiation.CHUNK_CONTAMINATION.get())) {
            read(event.getChunk());
        }
    }
}
