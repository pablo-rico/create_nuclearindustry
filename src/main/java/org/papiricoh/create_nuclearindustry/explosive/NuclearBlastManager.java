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
                changedBlocks += destroyColumn(column.x, column.z, horizontalNorm, BLOCKS_PER_TICK - changedBlocks);
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

        private int destroyColumn(int offsetX, int offsetZ, double horizontalNorm, int budget) {
            int changed = 0;
            for (int y = -craterRadius; y <= verticalRadius && changed < budget; y++) {
                int vRad = (y >= 0) ? verticalRadius : craterRadius;
                double verticalNorm = ((double) y * y) / ((double) vRad * vRad);
                double ellipsoidNorm = horizontalNorm + verticalNorm;
                if (ellipsoidNorm > 1.0) {
                    continue;
                }

                BlockPos pos = center.offset(offsetX, y, offsetZ);
                if (!level.isLoaded(pos)) {
                    continue;
                }

                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME)) {
                    continue;
                }

                float resistance = state.getBlock().getExplosionResistance();
                if (resistance > 1200.0f) {
                    continue;
                }

                double centerPressure = 1.0 - ellipsoidNorm;
                double shockPressure = 0.22 + centerPressure * 1.25 * intensity;
                double resistanceFactor = Math.min(0.92, resistance / 180.0);
                double chance = Math.max(0.03, Math.min(0.99, shockPressure - resistanceFactor));
                if (centerPressure > 0.62 || level.random.nextDouble() < chance) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    changed++;
                }
            }
            return changed;
        }

        private record Column(int x, int z) {}
    }
}
