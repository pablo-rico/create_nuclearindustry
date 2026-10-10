package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Client tick for cryogenic plants: keeps the compressor hum playing while a plant runs, lets frost
 * crystals form on its casing and cold vapour roll off it.
 */
public final class CryoplantClientEffects {
    private static final Map<BlockPos, CryoplantSoundInstance> SOUNDS = new HashMap<>();
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

    private CryoplantClientEffects() {}

    public static void tick(CryoplantControllerBlockEntity plant) {
        Minecraft minecraft = Minecraft.getInstance();
        // Ponder scenes run in their own client level; keep them silent.
        boolean inWorld = plant.getLevel() == minecraft.level;
        BlockPos pos = plant.getBlockPos();
        CryoplantSoundInstance sound = SOUNDS.get(pos);
        if (!plant.isRunning()) {
            if (sound != null) {
                sound.end();
                SOUNDS.remove(pos);
            }
            return;
        }
        spawnFrost(plant);
        if (!inWorld) {
            return;
        }
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            BlockPos bottom = plant.getBottomCenter();
            sound = new CryoplantSoundInstance(plant, bottom.getX() + 0.5, bottom.getY() + plant.getHeight() / 2.0,
                    bottom.getZ() + 0.5);
            SOUNDS.put(pos.immutable(), sound);
            minecraft.getSoundManager().play(sound);
        }
    }

    /** Frost glints on the walls and top, and a wisp of cold vapour that sinks down the sides. */
    private static void spawnFrost(CryoplantControllerBlockEntity plant) {
        Level level = plant.getLevel();
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        int height = Math.max(1, plant.getHeight());
        double activity = Math.max(0.2, Math.min(1.0, plant.getSpeed())) * height / 3.0;
        BlockPos bottom = plant.getBottomCenter();
        double minX = bottom.getX() - 1;
        double minZ = bottom.getZ() - 1;
        double minY = bottom.getY();
        if (random.nextDouble() < 0.6 * activity) {
            Direction side = SIDES[random.nextInt(SIDES.length)];
            double u = random.nextDouble() * 3.0;
            double v = random.nextDouble() * height;
            double x;
            double y;
            double z;
            switch (side) {
                case NORTH -> { x = minX + u; y = minY + v; z = minZ - 0.05; }
                case SOUTH -> { x = minX + u; y = minY + v; z = minZ + 3.05; }
                case EAST -> { x = minX + 3.05; y = minY + v; z = minZ + u; }
                case WEST -> { x = minX - 0.05; y = minY + v; z = minZ + u; }
                default -> { x = minX + u; y = minY + height + 0.05; z = minZ + random.nextDouble() * 3.0; }
            }
            level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, side.getStepX() * 0.01, -0.01, side.getStepZ() * 0.01);
        }
        if (random.nextDouble() < 0.15 * activity) {
            Direction side = SIDES[random.nextInt(4)];
            double u = random.nextDouble() * 3.0;
            double x = side.getAxis() == Direction.Axis.X ? (side == Direction.EAST ? minX + 3.1 : minX - 0.1) : minX + u;
            double z = side.getAxis() == Direction.Axis.Z ? (side == Direction.SOUTH ? minZ + 3.1 : minZ - 0.1) : minZ + u;
            double y = minY + height - 0.3 - random.nextDouble() * 0.6;
            // Cold vapour is denser than the air around it: it rolls off the top and sinks.
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, side.getStepX() * 0.015, -0.03, side.getStepZ() * 0.015);
        }
    }
}
