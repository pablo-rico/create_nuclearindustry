package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;

/**
 * A blast's casing fragments striking the living things that can be seen from it ({@link ShrapnelModel}): a wall, a hill or a
 * trench between keeps them off. Each body is hit by a random number of fragments around what its size takes.
 */
public final class Shrapnel {
    /** Most bodies one blast's fragments are traced to. */
    private static final int MAX_TARGETS = 64;

    private Shrapnel() {}

    public static void strike(ServerLevel level, Vec3 centre, ConventionalProfile profile, DetonationCause cause) {
        double reach = profile.fragmentRadius();
        if (profile.fragments() <= 0 || reach <= 0) {
            return;
        }
        DamageSource source = cause.damage(level, ConventionalRegistry.SHRAPNEL, centre);
        RandomSource random = level.random;
        int traced = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(reach),
                e -> e.isAlive() && !e.isSpectator())) {
            if (traced++ >= MAX_TARGETS) {
                break;
            }
            Vec3 body = entity.getBoundingBox().getCenter();
            double distance = body.distanceTo(centre);
            double damage = ShrapnelModel.hitDamage(distance, reach);
            if (damage <= 0) {
                continue;
            }
            if (ExplosiveConfig.claimsProtectEntities
                    && ClaimGuards.protectsFromExplosions(level, ((int) Math.floor(body.x)) >> 4, ((int) Math.floor(body.z)) >> 4)) {
                continue;
            }
            double crossSection = entity.getBbWidth() * entity.getBbHeight();
            double expected = ShrapnelModel.expectedHits(profile.fragments(), distance, crossSection, profile.airburst());
            int hits = poisson(expected, random);
            if (hits <= 0 || !seen(level, centre, body)) {
                continue;
            }
            entity.invulnerableTime = 0;
            entity.hurt(source, (float) (hits * damage));
        }
    }

    private static boolean seen(ServerLevel level, Vec3 centre, Vec3 body) {
        HitResult hit = level.clip(new ClipContext(centre.add(0, 0.25, 0), body, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** A Poisson draw of mean {@code mean}; a normal approximation past 30. */
    static int poisson(double mean, RandomSource random) {
        if (mean <= 0) {
            return 0;
        }
        if (mean > 30) {
            return Math.max(0, (int) Math.round(mean + Math.sqrt(mean) * random.nextGaussian()));
        }
        double limit = Math.exp(-mean);
        double product = random.nextDouble();
        int count = 0;
        while (product > limit) {
            product *= random.nextDouble();
            count++;
        }
        return count;
    }
}
