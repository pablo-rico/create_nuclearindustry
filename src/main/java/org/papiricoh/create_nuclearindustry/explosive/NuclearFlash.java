package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterRegistry;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.FlashBlindnessModel;
import org.papiricoh.create_nuclearindustry.explosive.blast.PromptDose;
import org.papiricoh.create_nuclearindustry.explosive.blast.ThermalPulse;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;
import org.papiricoh.create_nuclearindustry.integration.claims.ClaimGuards;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * What a nuclear detonation does to living things in its first second, before its blast wave arrives:
 * the thermal flash sets alight and burns whatever sees the fireball ({@link ThermalPulse}), prompt
 * radiation gives everyone near it a dose ({@link PromptDose}), cut by the blocks in between as other
 * radiation is, and players looking towards it are dazzled ({@link FlashBlindnessModel}). A wall, a hill or
 * a shelter between the fireball and a body keeps the flash off it.
 * <p>
 * Each body costs a line of sight and a walk of the radiation through the blocks to it, so the bodies are
 * worked through nearest first, {@code explosive.flash.tracesPerTick} a tick: a big detonation's flash
 * takes about a second to reach everyone, as a real thermal pulse lasts about that long. Flash jobs live in
 * memory; they never outlast a second or two.
 */
public final class NuclearFlash {
    /** Players further than this are dazzled without a line of sight traced: at that range only the sky glows. */
    private static final double BLINDNESS_TRACE_RANGE = 1024;

    private static final List<FlashJob> JOBS = new ArrayList<>();

    private NuclearFlash() {}

    /** Flashes a detonation of {@code profile} at {@code centre}, nobody's doing; the nearest bodies at once. */
    public static void strike(ServerLevel level, Vec3 centre, DetonationProfile profile) {
        strike(level, centre, profile, DetonationCause.NONE);
    }

    /** As {@link #strike(ServerLevel, Vec3, DetonationProfile)}, its burns {@code cause}'s. */
    public static void strike(ServerLevel level, Vec3 centre, DetonationProfile profile, DetonationCause cause) {
        dazzle(level, centre, profile);
        double reach = Math.max(profile.thermalRadius(), profile.promptRadius());
        if (reach <= 0) {
            return;
        }
        List<LivingEntity> bodies = level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(reach),
                entity -> entity.isAlive() && !entity.isSpectator() && entity.getEyePosition().distanceToSqr(centre) <= reach * reach);
        bodies.sort(Comparator.comparingDouble(entity -> entity.getEyePosition().distanceToSqr(centre)));
        FlashJob job = new FlashJob(level, centre, profile, cause, bodies.stream().map(LivingEntity::getUUID).toList());
        if (!job.step()) {
            JOBS.add(job);
        }
    }

    /** Flash jobs still reaching bodies in {@code level}. */
    public static int pending(ServerLevel level) {
        return (int) JOBS.stream().filter(job -> job.level == level).count();
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (JOBS.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Iterator<FlashJob> iterator = JOBS.iterator();
        while (iterator.hasNext()) {
            FlashJob job = iterator.next();
            if (job.level == level && job.step()) {
                iterator.remove();
            }
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        JOBS.removeIf(job -> job.level == event.getLevel());
    }

    /**
     * Dazzles every player in the level who sees the fireball: the closer, the bigger the fireball and the straighter they
     * look at it, the longer. Only a few players, so all at once.
     */
    private static void dazzle(ServerLevel level, Vec3 centre, DetonationProfile profile) {
        if (!ExplosiveConfig.flashBlindness || profile.fireballRadius() <= 0) {
            return;
        }
        Vec3 fireball = centre.add(0, profile.fireballRadius() * 0.5, 0);
        boolean night = !level.isDay();
        double range = ExplosiveConfig.visualRange;
        for (ServerPlayer player : level.players()) {
            Vec3 eyes = player.getEyePosition();
            double distance = eyes.distanceTo(fireball);
            if (distance > range || player.isSpectator()) {
                continue;
            }
            Vec3 toward = fireball.subtract(eyes);
            double facing = toward.lengthSqr() < 1e-6 ? 1 : player.getLookAngle().dot(toward.normalize());
            boolean goggles = player.getItemBySlot(EquipmentSlot.HEAD).is(AllNuclearTags.Items.FLASH_PROTECTION);
            double seconds = FlashBlindnessModel.seconds(distance, profile.fireballRadius(), facing, night, goggles,
                    ExplosiveConfig.flashBlindnessMaxSeconds);
            if (seconds <= 0 || ShelterRegistry.isSheltered(level, eyes)
                    || distance <= BLINDNESS_TRACE_RANGE && !seesFireball(level, fireball, eyes, player)) {
                continue;
            }
            int grade = FlashBlindnessModel.grade(seconds, ExplosiveConfig.flashBlindnessMaxSeconds);
            player.addEffect(new MobEffectInstance(ExplosionFxRegistry.FLASH_BLINDED, (int) Math.round(seconds * 20), Math.max(0, grade),
                    false, false, true));
        }
    }

    /** One detonation's flash still reaching the bodies it caught, nearest first. */
    private static final class FlashJob {
        private final ServerLevel level;
        private final Vec3 centre;
        private final Vec3 fireball;
        private final DetonationProfile profile;
        private final DamageSource flash;
        private final Deque<UUID> bodies;

        private FlashJob(ServerLevel level, Vec3 centre, DetonationProfile profile, DetonationCause cause, List<UUID> bodies) {
            this.level = level;
            this.centre = centre;
            // The fireball's top half shines over low cover around ground zero.
            this.fireball = centre.add(0, profile.fireballRadius() * 0.5, 0);
            this.profile = profile;
            this.flash = cause.damage(level, ExplosiveRegistry.NUCLEAR_FLASH, fireball);
            this.bodies = new ArrayDeque<>(bodies);
        }

        /** Reaches this tick's share of the bodies; true once all are done. */
        private boolean step() {
            int traces = Math.max(1, ExplosiveConfig.flashTracesPerTick);
            while (traces-- > 0 && !bodies.isEmpty()) {
                if (level.getEntity(bodies.poll()) instanceof LivingEntity entity && entity.isAlive()) {
                    Vec3 eyes = entity.getEyePosition();
                    if (ExplosiveConfig.claimsProtectEntities
                            && ClaimGuards.protectsFromExplosions(level, ((int) Math.floor(eyes.x)) >> 4, ((int) Math.floor(eyes.z)) >> 4)) {
                        continue;
                    }
                    double distance = eyes.distanceTo(centre);
                    burn(level, entity, fireball, eyes, distance, profile, flash);
                    irradiate(level, entity, centre, distance, profile);
                }
            }
            return bodies.isEmpty();
        }
    }

    /** The flash: fire and burns for a body that can see the fireball and is not in a shelter. */
    private static void burn(ServerLevel level, LivingEntity entity, Vec3 fireball, Vec3 eyes, double distance,
                             DetonationProfile profile, DamageSource flash) {
        double strength = ThermalPulse.strength(distance, profile.thermalRadius());
        if (strength < 1 || entity.fireImmune() || entity instanceof Player player && player.isCreative()) {
            return;
        }
        if (ShelterRegistry.isSheltered(level, eyes) || !seesFireball(level, fireball, eyes, entity)) {
            return;
        }
        entity.igniteForSeconds(ThermalPulse.fireSeconds(strength));
        float damage = ThermalPulse.damage(strength);
        if (damage > 0) {
            entity.hurt(flash, damage);
        }
    }

    private static boolean seesFireball(ServerLevel level, Vec3 fireball, Vec3 eyes, LivingEntity entity) {
        HitResult hit = level.clip(new ClipContext(eyes, fireball, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(fireball) < 9;
    }

    /**
     * Prompt radiation: a player takes it as dose (less their armour's shielding and iodide's share), a
     * mob is made sick for a while if the dose would sicken a player.
     */
    public static void irradiate(ServerLevel level, LivingEntity entity, Vec3 centre, double distance, DetonationProfile profile) {
        if (!Config.radiationEnabled || ExplosiveConfig.promptEdgeDose <= 0) {
            return;
        }
        double open = PromptDose.dose(profile, distance, ExplosiveConfig.promptEdgeDose);
        if (open <= 0) {
            return;
        }
        double dose = open * RadiationRay.transmission(level, centre, entity.getEyePosition());
        if (dose < PromptDose.NEGLIGIBLE) {
            return;
        }
        if (entity instanceof Player player) {
            RadiationHandler.addPromptDose(player, (float) dose);
        } else if (dose >= Config.radiationSicknessThreshold) {
            entity.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_SICKNESS, 20 * 60,
                    dose >= 2 * Config.radiationSicknessThreshold ? 1 : 0));
        }
    }
}
