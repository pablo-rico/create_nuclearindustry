package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The floor is y=0; walls run from y=1 through the nose. No reads load chunks. */
public final class SiloValidator {
    public record Result(String error, BlockPos position) { public boolean valid() { return error.isEmpty(); } }
    public static Result validate(Level level, BlockPos floor, BlockPos controller, int height) {
        for (int y = 0; y <= height; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos p = floor.offset(x, y, z);
            if (!level.isLoaded(p)) return new Result("unloaded", p);
            if (p.equals(controller)) continue;
            if (y == 0 || Math.abs(x) == 2 || Math.abs(z) == 2) {
                if (!level.getBlockState(p).is(SiloRegistry.SILO_WALLS)) return new Result("wall", p);
            } else if ((x != 0 || z != 0) && !level.getBlockState(p).isAir()) return new Result("interior", p);
        }
        return exit(level, floor, height);
    }
    public static Result exit(Level level, BlockPos floor, int height) {
        // The entire 3x3 opening must reach the sky; this catches caps at arbitrary heights.
        for (int y = floor.getY() + height + 1; y < level.getMaxBuildHeight(); y++)
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos p = new BlockPos(floor.getX() + x, y, floor.getZ() + z);
                if (!level.isLoaded(p)) return new Result("unloaded", p);
                if (!level.getBlockState(p).isAir()) return new Result("exit", p);
            }
        return new Result("", floor);
    }
}
