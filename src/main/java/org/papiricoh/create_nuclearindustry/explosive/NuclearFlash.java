package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterRegistry;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.PromptDose;
import org.papiricoh.create_nuclearindustry.explosive.blast.ThermalPulse;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;

/**
 * What a nuclear detonation does to living things the instant it goes off, before its blast wave arrives:
 * the thermal flash sets alight and burns whatever sees the fireball ({@link ThermalPulse}), and prompt
 * radiation gives everyone near it a dose at once ({@link PromptDose}), cut by the blocks in between as
 * other radiation is. A wall, a hill or a shelter between the fireball and a body keeps the flash off it.
 */
public final class NuclearFlash {
    private NuclearFlash() {}

    public static void strike(ServerLevel level, Vec3 centre, DetonationProfile profile) {
        double reach = Math.max(profile.thermalRadius(), profile.promptRadius());
        if (reach <= 0) {
            return;
        }
        // The fireball's top half shines over low cover around ground zero.
        Vec3 fireball = centre.add(0, profile.fireballRadius() * 0.5, 0);
        DamageSource flash = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ExplosiveRegistry.NUCLEAR_FLASH), fireball);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(reach),
                entity -> entity.isAlive() && !entity.isSpectator())) {
            Vec3 eyes = entity.getEyePosition();
            double distance = eyes.distanceTo(centre);
            burn(level, entity, fireball, eyes, distance, profile, flash);
            irradiate(level, entity, centre, distance, profile);
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
