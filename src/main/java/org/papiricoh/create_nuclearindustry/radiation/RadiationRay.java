package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Fraction of radiation that crosses the blocks between two points: a voxel walk
 * (Amanatides-Woo) multiplying {@link RadiationTransmission} of every block crossed. The
 * source's and target's own blocks do not count, nor do blocks inside {@code ignore}
 * (a reactor's core, say), nor {@link Passable} blocks in a passable state. Never loads chunks:
 * unloaded blocks count as air.
 */
public final class RadiationRay {
    private static final double CUTOFF = 1.0e-3;

    /**
     * A block whose shielding depends on its state, which a per-block data map cannot say: an open
     * blast door lets everything through, a shut one shields by its {@code radiation_transmission}.
     */
    public interface Passable {
        boolean letsRadiationThrough(BlockState state);
    }

    private RadiationRay() {}

    public static double transmission(Level level, Vec3 from, Vec3 to) {
        return transmission(level, from, to, null);
    }

    public static double transmission(Level level, Vec3 from, Vec3 to, @Nullable BoundingBox ignore) {
        int x = Mth.floor(from.x);
        int y = Mth.floor(from.y);
        int z = Mth.floor(from.z);
        int endX = Mth.floor(to.x);
        int endY = Mth.floor(to.y);
        int endZ = Mth.floor(to.z);
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        int stepX = (int) Math.signum(dx);
        int stepY = (int) Math.signum(dy);
        int stepZ = (int) Math.signum(dz);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double deltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double maxX = stepX == 0 ? Double.POSITIVE_INFINITY : (stepX > 0 ? x + 1 - from.x : from.x - x) * deltaX;
        double maxY = stepY == 0 ? Double.POSITIVE_INFINITY : (stepY > 0 ? y + 1 - from.y : from.y - y) * deltaY;
        double maxZ = stepZ == 0 ? Double.POSITIVE_INFINITY : (stepZ > 0 ? z + 1 - from.z : from.z - z) * deltaZ;

        double transmission = 1.0;
        int steps = Math.abs(endX - x) + Math.abs(endY - y) + Math.abs(endZ - z) + 3;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < steps; i++) {
            if (maxX < maxY && maxX < maxZ) {
                if (maxX > 1.0) {
                    break;
                }
                x += stepX;
                maxX += deltaX;
            } else if (maxY < maxZ) {
                if (maxY > 1.0) {
                    break;
                }
                y += stepY;
                maxY += deltaY;
            } else {
                if (maxZ > 1.0) {
                    break;
                }
                z += stepZ;
                maxZ += deltaZ;
            }
            if (x == endX && y == endY && z == endZ) {
                break;
            }
            pos.set(x, y, z);
            if ((ignore != null && ignore.isInside(pos)) || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof Passable passable && passable.letsRadiationThrough(state)) {
                continue;
            }
            transmission *= RadiationTransmission.of(state);
            if (transmission < CUTOFF) {
                return 0.0;
            }
        }
        return transmission;
    }
}
