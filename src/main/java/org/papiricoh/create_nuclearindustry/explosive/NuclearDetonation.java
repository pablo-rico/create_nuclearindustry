package org.papiricoh.create_nuclearindustry.explosive;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class NuclearDetonation {
    private static final Logger LOGGER = LogUtils.getLogger();

    private NuclearDetonation() {}

    public static void detonateMaxWarhead(ServerLevel level, Position position) {
        detonate(level, position, WarheadStats.MAX_URANIUM_UNITS, WarheadStats.REQUIRED_ENRICHMENT);
    }

    public static void detonate(ServerLevel level, Position position, int uraniumUnits, float averageEnrichment) {
        float power = WarheadStats.power(uraniumUnits, averageEnrichment);
        int horizontalRadius = WarheadStats.horizontalRadius(uraniumUnits, averageEnrichment);
        int verticalRadius = WarheadStats.verticalRadius(uraniumUnits, averageEnrichment);
        float intensity = WarheadStats.intensity(averageEnrichment);

        Vec3 impact = new Vec3(position.x(), position.y(), position.z());
        BlockPos center = BlockPos.containing(position.x(), position.y(), position.z());
        LOGGER.info("Nuclear detonation at {} with power={}, horizontalRadius={}, verticalRadius={}",
                center, power, horizontalRadius, verticalRadius);

        NuclearExplosionSoundManager.playDelayedForWorld(level, impact);
        level.explode(null, position.x(), position.y(), position.z(),
                Math.min(64.0f, power * 0.35f), Level.ExplosionInteraction.BLOCK);
        NuclearBlastManager.start(level, center, horizontalRadius, verticalRadius, intensity);
    }
}
