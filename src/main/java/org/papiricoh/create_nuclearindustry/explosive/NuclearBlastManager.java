package org.papiricoh.create_nuclearindustry.explosive;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.PerformanceConfig;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.radiation.cache.RadiationCaches;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Digs nuclear craters a budget of columns and blocks per tick ({@code explosive.blastColumnsPerTick}
 * and {@code explosive.blastBlocksPerTick} in {@link PerformanceConfig}), from the centre outwards.
 * Craters being dug live in memory and hold their level: they are dropped with it, and all of them
 * when the server stops, so a world closed mid-crater does not stay in memory.
 * <p>
 * Blocks are removed with raw state changes (no neighbour updates, no block events), so after each
 * tick's digging the crater tells what it changed itself: the radiation caches of every chunk it
 * dug in, and the multiblock controllers whose structure the dug box reaches.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID)
public class NuclearBlastManager {
    // Amplitud del ondulado del borde del crater, como fraccion del radio (0 = borde perfectamente liso).
    private static final double RIM_NOISE_AMPLITUDE = 0.05;
    private static final List<BlastJob> JOBS = new ArrayList<>();

    public static void start(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius, float intensity) {
        BlastJob job = create(level, center, horizontalRadius, verticalRadius, intensity);
        if (job != null) {
            JOBS.add(job);
        }
    }

    /** A crater that is not dug on its own: {@link BlastJob#step()} digs one tick's worth. For tests. */
    @Nullable
    public static BlastJob create(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius, float intensity) {
        if (horizontalRadius <= 0 || verticalRadius <= 0) {
            return null;
        }
        return new BlastJob(level, center, horizontalRadius, verticalRadius, intensity);
    }

    /** Craters still being dug in {@code level}; for tests. */
    public static int pending(ServerLevel level) {
        int count = 0;
        for (BlastJob job : JOBS) {
            if (job.level == level) {
                count++;
            }
        }
        return count;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        JOBS.removeIf(job -> job.level == event.getLevel());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Iterator<BlastJob> iterator = JOBS.iterator();
        while (iterator.hasNext()) {
            BlastJob job = iterator.next();
            if (job.level != level) {
                continue;
            }
            if (job.step()) {
                iterator.remove();
            }
        }
    }

    public static final class BlastJob {
        private final ServerLevel level;
        private final BlockPos center;
        private final int horizontalRadius;
        private final int verticalRadius;
        private final int craterRadius;
        private final float intensity;
        // Cursor de anillo cuadrado (distancia de Chebyshev) para barrer del centro hacia afuera.
        private int ring;
        private int idx;
        // Cache del ultimo chunk forzado a cargar, para no repetir la llamada por cada columna.
        private int lastChunkX = Integer.MIN_VALUE;
        private int lastChunkZ = Integer.MIN_VALUE;
        private int lastColumns;
        private int lastBlocks;
        /** Chunks dug in during the current step, each announced to the radiation caches once. */
        private final LongOpenHashSet dugChunks = new LongOpenHashSet();
        // Box of the blocks removed during the current step; empty while minX > maxX.
        private int dugMinX, dugMinY, dugMinZ, dugMaxX, dugMaxY, dugMaxZ;
        @Nullable
        private BoundingBox lastDug;

        private BlastJob(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius, float intensity) {
            this.level = level;
            this.center = center;
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
            this.craterRadius = Math.max(8, Math.round(verticalRadius * 0.5f));
            this.intensity = intensity;
            this.ring = 0;
            this.idx = 0;
        }

        /** Digs one tick's worth of columns; returns whether the crater is finished. */
        public boolean step() {
            int processedColumns = 0;
            int changedBlocks = 0;
            int columnBudget = PerformanceConfig.blastColumnsPerTick;
            int blockBudget = PerformanceConfig.blastBlocksPerTick;
            double radiusSq = (double) horizontalRadius * horizontalRadius;
            dugMinX = dugMinY = dugMinZ = Integer.MAX_VALUE;
            dugMaxX = dugMaxY = dugMaxZ = Integer.MIN_VALUE;
            while (ring <= horizontalRadius && processedColumns < columnBudget && changedBlocks < blockBudget) {
                Column column = nextColumn();

                double horizontalNorm = ((double) column.x * column.x + (double) column.z * column.z) / radiusSq;
                if (horizontalNorm > 1.0) {
                    // Esquina del anillo cuadrado fuera del disco: descartar sin tocar el mundo.
                    continue;
                }
                // Cada columna se vacia entera (nunca a medias). El presupuesto por tick se controla
                // a nivel de bucle: una vez superado, no se empiezan columnas nuevas. El sobrepaso
                // queda acotado a una sola columna, evitando los munones que dejaba el corte previo.
                changedBlocks += destroyColumn(column.x, column.z, horizontalNorm);
                processedColumns++;
            }
            lastColumns = processedColumns;
            lastBlocks = changedBlocks;
            announce();
            return ring > horizontalRadius;
        }

        /**
         * Tells the radiation caches and the multiblock controllers about this step's digging: raw
         * state changes fire no block event, so without this cached rays would still see the
         * vanished walls and a sealed building would keep thinking itself sealed. One change per
         * chunk is enough (the radiation change log is per chunk), and one box for the controllers.
         */
        private void announce() {
            if (dugMinX > dugMaxX) {
                lastDug = null;
                dugChunks.clear();
                return;
            }
            BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
            var iterator = dugChunks.iterator();
            while (iterator.hasNext()) {
                long chunk = iterator.nextLong();
                probe.set(ChunkPos.getX(chunk) << 4, center.getY(), ChunkPos.getZ(chunk) << 4);
                RadiationCaches.blockChanged(level, probe);
            }
            dugChunks.clear();
            lastDug = new BoundingBox(dugMinX, dugMinY, dugMinZ, dugMaxX, dugMaxY, dugMaxZ);
            MultiblockRegistry.areaChanged(level, lastDug);
        }

        /** The box of the blocks the last {@link #step()} removed, or null if it removed none. */
        @Nullable
        public BoundingBox lastDug() {
            return lastDug;
        }

        private void dug(BlockPos pos) {
            dugMinX = Math.min(dugMinX, pos.getX());
            dugMinY = Math.min(dugMinY, pos.getY());
            dugMinZ = Math.min(dugMinZ, pos.getZ());
            dugMaxX = Math.max(dugMaxX, pos.getX());
            dugMaxY = Math.max(dugMaxY, pos.getY());
            dugMaxZ = Math.max(dugMaxZ, pos.getZ());
        }

        /** Columns of the crater dug by the last {@link #step()}. */
        public int lastColumns() {
            return lastColumns;
        }

        /** Blocks removed by the last {@link #step()}. */
        public int lastBlocks() {
            return lastBlocks;
        }

        /**
         * Devuelve la siguiente columna recorriendo el perímetro de anillos cuadrados crecientes
         * (Chebyshev) desde el centro. Garantiza visitar cada columna (x,z) exactamente una vez y
         * en orden centro -> afuera, de modo que el cráter se forma alrededor de la bomba al instante.
         */
        private Column nextColumn() {
            Column column = perimeterCell(ring, idx);
            int count = (ring == 0) ? 1 : 8 * ring;
            idx++;
            if (idx >= count) {
                idx = 0;
                ring++;
            }
            return column;
        }

        private Column perimeterCell(int r, int i) {
            if (r == 0) {
                return new Column(0, 0);
            }
            int side = 2 * r;
            if (i < side) {
                return new Column(-r + i, -r);          // borde superior: x de -r a r-1
            }
            i -= side;
            if (i < side) {
                return new Column(r, -r + i);            // borde derecho: z de -r a r-1
            }
            i -= side;
            if (i < side) {
                return new Column(r - i, r);             // borde inferior: x de r a -r+1
            }
            i -= side;
            return new Column(-r, r - i);                // borde izquierdo: z de r a -r+1
        }

        private int destroyColumn(int offsetX, int offsetZ, double horizontalNorm) {
            // Factor del semieje vertical del elipsoide para esta columna (1 en el centro, 0 en el borde).
            double vertFactor = Math.sqrt(Math.max(0.0, 1.0 - horizontalNorm));
            if (vertFactor <= 0.0) {
                return 0;
            }

            // Ruido suave de baja frecuencia aplicado UNA vez por columna (nunca por bloque),
            // para que el borde del cráter ondule de forma natural sin dejar picos ni huecos.
            double rim = 1.0 + RIM_NOISE_AMPLITUDE * columnNoise(offsetX, offsetZ);

            int topY = (int) Math.round(verticalRadius * vertFactor * rim);
            int bottomY = (int) Math.round(craterRadius * vertFactor * rim);

            // Fuerza la carga/generacion del chunk de esta columna para que la explosion se complete
            // tambien en chunks descargados (si no, quedaban filas/paredes enteras sin volar).
            ensureChunkLoaded(center.getX() + offsetX, center.getZ() + offsetZ);

            int changed = 0;
            // Vaciado determinista y contiguo de toda la columna: cuenco liso, sin bloques sueltos.
            for (int y = topY; y >= -bottomY; y--) {
                BlockPos pos = center.offset(offsetX, y, offsetZ);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)) {
                    continue;
                }

                // Solo se respetan los bloques prácticamente indestructibles (p. ej. obsidiana reforzada).
                if (state.getBlock().getExplosionResistance() > 1200.0f) {
                    continue;
                }

                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                dug(pos);
                changed++;
            }

            // Drenado de liquidos (agua source y corriente, lava) que queden justo encima del cuenco,
            // para que el crater no se rellene ni deje charcos artificiales. Drenaje contiguo hacia
            // arriba: se detiene en cuanto encuentra aire o un bloque solido, asi queda acotado.
            for (int y = topY + 1; ; y++) {
                BlockPos pos = center.offset(offsetX, y, offsetZ);
                BlockState state = level.getBlockState(pos);
                if (state.getFluidState().isEmpty()) {
                    break;
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                dug(pos);
                changed++;
            }
            if (changed > 0) {
                dugChunks.add(ChunkPos.asLong((center.getX() + offsetX) >> 4, (center.getZ() + offsetZ) >> 4));
            }
            return changed;
        }

        /** Carga (generando si hace falta) el chunk que contiene la coordenada de mundo dada. */
        private void ensureChunkLoaded(int worldX, int worldZ) {
            int chunkX = worldX >> 4;
            int chunkZ = worldZ >> 4;
            if (chunkX == lastChunkX && chunkZ == lastChunkZ) {
                return;
            }
            lastChunkX = chunkX;
            lastChunkZ = chunkZ;
            // getChunk(x, z) carga/genera el chunk de forma sincrona y lo deja editable;
            // las modificaciones quedan marcadas para guardarse y el chunk se descarga solo despues.
            level.getChunk(chunkX, chunkZ);
        }

        /**
         * Ruido determinista y suave en funcion del angulo y la distancia, en ~[-1, 1].
         * Suma de senos de distintas frecuencias: produce un contorno ondulado y organico
         * sin discontinuidades, evitando un borde perfectamente circular.
         */
        private double columnNoise(int offsetX, int offsetZ) {
            double angle = Math.atan2(offsetZ, offsetX);
            double dist = Math.sqrt((double) offsetX * offsetX + (double) offsetZ * offsetZ);
            double n = 0.55 * Math.sin(angle * 5.0 + dist * 0.06)
                     + 0.30 * Math.sin(angle * 11.0 - dist * 0.03 + 1.7)
                     + 0.15 * Math.sin(angle * 23.0 + 3.1);
            return Math.max(-1.0, Math.min(1.0, n * intensity));
        }

        private record Column(int x, int z) {}
    }
}
