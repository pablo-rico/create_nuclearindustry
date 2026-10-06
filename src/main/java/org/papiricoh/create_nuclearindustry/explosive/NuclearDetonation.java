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
        BlockPos center = BlockPos.containing(impact);
        LOGGER.info("Nuclear detonation at {} with power={}, horizontalRadius={}, verticalRadius={}",
                center, power, horizontalRadius, verticalRadius);

        NuclearExplosionSoundManager.playDelayedForWorld(level, impact);
        level.explode(null, impact.x, impact.y, impact.z, Math.min(64.0f, power * 0.35f), Level.ExplosionInteraction.BLOCK);
        NuclearBlastManager.start(level, center, horizontalRadius, verticalRadius, intensity);
        Contamination.fallout(level, center, falloutRadius(horizontalRadius), falloutIntensity(power));
    }

    public static int falloutRadius(int horizontalRadius) {
        return Math.round(horizontalRadius * FALLOUT_RADIUS_FACTOR);
    }

    public static float falloutIntensity(float power) {
        return (float) Math.min(Config.contaminationMax, power * FALLOUT_PER_POWER);
    }
}
