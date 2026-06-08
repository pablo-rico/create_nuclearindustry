package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NuclearBlastManager {
    private static final int COLUMNS_PER_TICK = 600;
    private static final int BLOCKS_PER_TICK = 3_500;
    // Amplitud del ondulado del borde del crater, como fraccion del radio (0 = borde perfectamente liso).
    private static final double RIM_NOISE_AMPLITUDE = 0.05;
    private static final List<BlastJob> JOBS = new ArrayList<>();

    public static void start(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius, float intensity) {
        if (horizontalRadius <= 0 || verticalRadius <= 0) {
            return;
        }
        JOBS.add(new BlastJob(level, center, horizontalRadius, verticalRadius, intensity));
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
            if (job.tick()) {
                iterator.remove();
            }
        }
    }

    private static class BlastJob {
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

        private boolean tick() {
            int processedColumns = 0;
            int changedBlocks = 0;
            double radiusSq = (double) horizontalRadius * horizontalRadius;
            while (ring <= horizontalRadius && processedColumns < COLUMNS_PER_TICK && changedBlocks < BLOCKS_PER_TICK) {
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
            return ring > horizontalRadius;
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
                changed++;
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
