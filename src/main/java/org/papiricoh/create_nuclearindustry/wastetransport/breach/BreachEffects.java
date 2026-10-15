package org.papiricoh.create_nuclearindustry.wastetransport.breach;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportConfig;

/** What a breach does to the world: the spill when it happens, the leak while it lasts, and how both look. */
public final class BreachEffects {

    private BreachEffects() {}

    /**
     * A container holding {@code activity} rad/s of waste bursts at {@code where}: contamination
     * proportional to the activity settles around it, with a puff of radioactive dust.
     */
    public static void spill(ServerLevel level, BlockPos where, double activity) {
        Vec3 at = ShipSpace.toWorld(level, where);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SHIELD_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.8f);
        level.sendParticles(AllNuclearParticles.RADIOACTIVE_DUST.get(), at.x, at.y, at.z, 40, 1.2, 0.6, 1.2, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, 0.6, 0.4, 0.6, 0.02);
        if (!Config.contaminationEnabled || activity <= 0) {
            return;
        }
        int radius = BreachModel.spillRadius(activity, WasteTransportConfig.spillRadiusMax);
        float intensity = (float) BreachModel.spillIntensity(activity, WasteTransportConfig.spillIntensityPerActivity);
        // Contamination files the spill under the real world position, even aboard a ship.
        Contamination.fallout(level, where, radius, intensity);
    }

    /** One second of leak from a breached container holding {@code activity} rad/s, into the ground under {@code where}. */
    public static void leak(ServerLevel level, BlockPos where, double activity) {
        if (level.random.nextInt(3) == 0) {
            Vec3 at = ShipSpace.toWorld(level, where);
            level.sendParticles(AllNuclearParticles.RADIOACTIVE_DUST.get(), at.x, at.y - 0.4, at.z, 3, 0.4, 0.2, 0.4, 0.0);
        }
        if (!Config.contaminationEnabled || activity <= 0) {
            return;
        }
        double amount = BreachModel.leakPerSecond(activity, WasteTransportConfig.spillIntensityPerActivity,
                WasteTransportConfig.breachLeakFactor);
        if (amount <= 0) {
            return;
        }
        BlockPos ground = ShipSpace.worldBlock(level, where);
        LevelChunk chunk = Contamination.loaded(level, ground.getX() >> 4, ground.getZ() >> 4);
        if (chunk != null) {
            Contamination.add(chunk, (float) amount);
        }
    }

    /** Client side: a drip of radioactive dust falling out of a breached container moving at {@code at}. */
    public static void drip(Level level, Vec3 at, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        level.addParticle(AllNuclearParticles.RADIOACTIVE_DUST.get(), at.x + (random.nextDouble() - 0.5) * 0.8,
                at.y - 0.5, at.z + (random.nextDouble() - 0.5) * 0.8, 0, -0.03, 0);
    }
}
