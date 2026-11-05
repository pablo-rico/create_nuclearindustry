package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;

/**
 * Whatever the fireball engulfs: the gas of the explosion itself, hot and at a pressure far beyond what the shock wave's fit
 * describes, kills outright near its middle and burns at its edge. Bodies inside {@code radius} take damage falling from
 * {@code peak} at the centre to nothing at the edge, and catch fire.
 */
public final class FireballStrike {
    private FireballStrike() {}

    public static void strike(ServerLevel level, Vec3 centre, double radius, double peak, int fireSeconds, DetonationCause cause,
                              ResourceKey<DamageType> type) {
        if (radius <= 0 || peak <= 0) {
            return;
        }
        DamageSource source = cause.damage(level, type, centre);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius),
                e -> e.isAlive() && !e.isSpectator())) {
            Vec3 body = entity.getBoundingBox().getCenter();
            double distance = body.distanceTo(centre);
            if (distance > radius || entity instanceof Player player && player.isCreative()) {
                continue;
            }
            if (ExplosiveConfig.claimsProtectEntities
                    && ClaimGuards.protectsFromExplosions(level, ((int) Math.floor(body.x)) >> 4, ((int) Math.floor(body.z)) >> 4)) {
                continue;
            }
            double share = 1 - distance / radius;
            entity.hurt(source, (float) (peak * share * share));
            if (fireSeconds > 0 && !entity.fireImmune()) {
                entity.igniteForSeconds(Math.max(1, (int) Math.round(fireSeconds * share)));
            }
        }
    }
}
