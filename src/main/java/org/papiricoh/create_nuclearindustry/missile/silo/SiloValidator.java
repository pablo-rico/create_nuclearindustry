package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The 3×3 base and rocket clearance are mandatory; walls only shape launch effects. No reads load chunks. */
public final class SiloValidator {
    public record Result(String error, BlockPos position) { public boolean valid() { return error.isEmpty(); } }
    public static Result validate(Level level, BlockPos floor, BlockPos controller, int height) {
        return validate(level, floor, controller, height, java.util.Set.of());
    }
    public static Result validate(Level level, BlockPos floor, BlockPos controller, int height, java.util.Set<BlockPos> captured) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos p = floor.offset(x, 0, z);
            if (!level.isLoaded(p)) return new Result("unloaded", p);
            if (!level.getBlockState(p).is(SiloRegistry.LAUNCH_BASES)) return new Result("base", p);
        }
        for (int y = 1; y <= height; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (!LaunchSiteGeometry.clearanceOffset(x, y, z, height)) continue;
            BlockPos p = floor.offset(x, y, z);
            if (!level.isLoaded(p)) return new Result("unloaded", p);
            if (!level.getBlockState(p).isAir() && !captured.contains(p)) return new Result("interior", p);
        }
        return exit(level, floor, height);
    }

    /** Consecutive complete wall rings from the base up, capped at the rocket's height. */
    public static int shaft(Level level, BlockPos floor, BlockPos controller, int height) {
        for (int y = 1; y <= height; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) != 2) continue;
            BlockPos p = floor.offset(x, y, z);
            if (p.equals(controller)) continue;
            if (!level.isLoaded(p) || !level.getBlockState(p).is(SiloRegistry.SILO_WALLS)) return y - 1;
        }
        return height;
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
