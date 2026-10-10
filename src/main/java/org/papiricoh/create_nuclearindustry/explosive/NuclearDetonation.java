package org.papiricoh.create_nuclearindustry.explosive;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.slf4j.Logger;

/** Every nuclear detonation (bomb, missile, cannon shell): blast, crater and fallout. */
public final class NuclearDetonation {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Fallout reaches this far beyond the crater rim, as a multiple of the crater radius. */
    public static final float FALLOUT_RADIUS_FACTOR = 1.5f;
    /** Ground contamination at ground zero per point of blast power. */
    public static final float FALLOUT_PER_POWER = 0.2f;

    private NuclearDetonation() {}

    public static void detonateMaxWarhead(ServerLevel level, Position position) {
        detonate(level, position, WarheadStats.MAX_URANIUM_UNITS, WarheadStats.REQUIRED_ENRICHMENT);
    }

    public static void detonate(ServerLevel level, Position position, int uraniumUnits, float averageEnrichment) {
        detonate(level, new Vec3(position.x(), position.y(), position.z()),
                WarheadStats.power(uraniumUnits, averageEnrichment),
                WarheadStats.horizontalRadius(uraniumUnits, averageEnrichment),
                WarheadStats.verticalRadius(uraniumUnits, averageEnrichment),
                WarheadStats.intensity(averageEnrichment));
    }

    public static void detonate(ServerLevel level, Vec3 impact, float power, int horizontalRadius, int verticalRadius, float intensity) {
        detonate(level, impact, power, horizontalRadius, verticalRadius, intensity, 0.0f);
    }

    /**
     * A nuclear detonation "salted" with {@code salting} rad/s of radioactive material packed
     * around the core: same blast, dirtier and wider fallout.
     */
    public static void detonate(ServerLevel level, Vec3 impact, float power, int horizontalRadius, int verticalRadius,
                                float intensity, float salting) {
        BlockPos center = BlockPos.containing(impact);
        LOGGER.info("Nuclear detonation at {} with power={}, horizontalRadius={}, verticalRadius={}, salting={}",
                center, power, horizontalRadius, verticalRadius, salting);

        NuclearExplosionSoundManager.playDelayedForWorld(level, impact);
        level.explode(null, impact.x, impact.y, impact.z, Math.min(64.0f, power * 0.35f), Level.ExplosionInteraction.BLOCK);
        NuclearBlastManager.start(level, center, horizontalRadius, verticalRadius, intensity);
        int radius = falloutRadius(horizontalRadius);
        float fallout = falloutIntensity(power);
        if (salting > 0) {
            radius = Math.max(radius, DirtyWarheadStats.falloutRadius(salting));
            fallout = (float) Math.min(Config.contaminationMax, fallout + DirtyWarheadStats.falloutIntensity(salting));
        }
        Contamination.fallout(level, center, radius, fallout);
        org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, center, Math.max(64, radius * 2),
                org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.NUCLEAR_DETONATION);
    }

    /**
     * A dirty bomb: a small conventional blast that scatters {@code load} rad/s of radioactive
     * material. No crater, but wide fallout and a hot spot at ground zero that fades over days.
     */
    public static void detonateDirty(ServerLevel level, Vec3 impact, float load) {
        BlockPos center = BlockPos.containing(impact);
        LOGGER.info("Dirty bomb detonation at {} with load={}", center, load);
        level.explode(null, impact.x, impact.y, impact.z, DirtyWarheadStats.blastPower(load), Level.ExplosionInteraction.BLOCK);
        Contamination.fallout(level, center, DirtyWarheadStats.falloutRadius(load), DirtyWarheadStats.falloutIntensity(load));
        org.papiricoh.create_nuclearindustry.radiation.FalloutData.contaminate(level, center, DirtyWarheadStats.hotSpot(load),
                DirtyWarheadStats.HOT_SPOT_TICKS);
        org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, center, Math.max(64, DirtyWarheadStats.falloutRadius(load)),
                org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.DIRTY_DETONATION);
    }

    public static int falloutRadius(int horizontalRadius) {
        return Math.round(horizontalRadius * FALLOUT_RADIUS_FACTOR);
    }

    public static float falloutIntensity(float power) {
        return (float) Math.min(Config.contaminationMax, power * FALLOUT_PER_POWER);
    }
}
