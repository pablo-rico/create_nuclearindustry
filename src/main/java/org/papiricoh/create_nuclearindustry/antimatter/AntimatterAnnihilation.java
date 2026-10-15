package org.papiricoh.create_nuclearindustry.antimatter;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationBlastModel;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonationEvent;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationRay;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Antimatter meeting matter all at once ({@link AnnihilationBlastModel}): a vanilla explosion whose
 * radius grows with the cube root of the charge, the explosive area's small flash, a gamma pulse
 * that reaches twice as far through the radiation dose API, green sparks, and a
 * {@link NuclearDetonationEvent} of style {@link DetonationStyle#ANTIMATTER} for seismic detectors.
 * Annihilation leaves no fission products: no fallout and no contamination.
 *
 * <p>Traps, vials and collectors call {@link #schedule}: the blast goes off at the end of the
 * level's tick, after the trap's blocks are cleared, so a blast that breaks a neighbouring loaded
 * trap sets that one off on the next tick instead of recursing, and a chain runs one link per tick.
 */
public final class AntimatterAnnihilation {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The colour of annihilation sparks, the vial's green. */
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(0x6B / 255f, 1.0f, 0x8A / 255f), 1.6f);
    private static final Map<ServerLevel, List<Pending>> PENDING = new WeakHashMap<>();

    /**
     * One annihilation waiting for the end of the tick.
     *
     * @param clear blocks to remove first (the trap that held it), without drops
     */
    private record Pending(Vec3 centre, double nanograms, List<BlockPos> clear) {}

    private AntimatterAnnihilation() {}

    /** Hooks the end-of-tick queue; called once from the area's registration. */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(AntimatterAnnihilation::onLevelTick);
        NeoForge.EVENT_BUS.addListener(AntimatterAnnihilation::onLevelUnload);
    }

    /** {@code ng} of antimatter annihilates at {@code centre} at the end of this tick. */
    public static void schedule(ServerLevel level, Vec3 centre, double ng) {
        schedule(level, centre, ng, List.of());
    }

    /** The same, removing {@code clear} (the holder's own blocks) right before. */
    public static void schedule(ServerLevel level, Vec3 centre, double ng, List<BlockPos> clear) {
        PENDING.computeIfAbsent(level, l -> new ArrayList<>()).add(new Pending(centre, ng, List.copyOf(clear)));
    }

    /** Annihilations waiting in {@code level} (game tests). */
    public static int pending(ServerLevel level) {
        List<Pending> list = PENDING.get(level);
        return list == null ? 0 : list.size();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Pending> due = PENDING.remove(level);
        if (due == null) {
            return;
        }
        // Blasts here may break more loaded traps: those queue up anew and go off next tick.
        for (Pending pending : due) {
            for (BlockPos pos : pending.clear()) {
                if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
            explode(level, pending.centre(), pending.nanograms());
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PENDING.remove(level);
        }
    }

    /** Annihilates {@code ng} at {@code centre} now. */
    public static void explode(ServerLevel level, BlockPos pos, double ng) {
        explode(level, Vec3.atCenterOf(pos), ng);
    }

    /** Annihilates {@code ng} at {@code centre} now. */
    public static void explode(ServerLevel level, Vec3 centre, double ng) {
        // Aboard a ship it goes off where the ship really is; vanilla's explosion is moved there by Sable.
        centre = org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.toWorld(level, centre);
        AnnihilationBlastModel.Params p = AntimatterConfig.blast();
        if (!AnnihilationBlastModel.explodes(p, ng)) {
            fizzle(level, centre);
            return;
        }
        float radius = (float) AnnihilationBlastModel.radius(p, ng);
        int pulse = (int) Math.ceil(AnnihilationBlastModel.pulseRadius(p, ng));
        LOGGER.info("Antimatter annihilation at {} of {} ng: radius {}, gamma pulse {}", BlockPos.containing(centre),
                String.format(java.util.Locale.ROOT, "%.3f", ng), radius, pulse);
        NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(level, centre, radius, pulse, DetonationStyle.ANTIMATTER));
        DetonationPayload.antimatter(centre, radius, level.random.nextLong()).send(level);
        level.playSound(null, centre.x, centre.y, centre.z, AllNuclearSounds.ANNIHILATION_BLAST.get(), SoundSource.BLOCKS,
                Math.min(8.0f, 2.0f + radius), 1.0f);
        gammaPulse(level, centre, ng, p);
        sparks(level, centre, radius);
        // A flash instead of vanilla's grey puffs: annihilation leaves light, not smoke (the client
        // draws the rest, see NuclearExplosionEffects).
        level.explode(null, null, null, centre.x, centre.y, centre.z, radius, false,
                AntimatterConfig.blastBreaksBlocks ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE,
                ParticleTypes.FLASH, ParticleTypes.FLASH, SoundEvents.GENERIC_EXPLODE);
    }

    /** Less than a fizzle's worth: a crackle and a few sparks. */
    private static void fizzle(ServerLevel level, Vec3 centre) {
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.8f);
        level.sendParticles(SPARK, centre.x, centre.y, centre.z, 12, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, 8, 0.2, 0.2, 0.2, 0.1);
    }

    private static void sparks(ServerLevel level, Vec3 centre, float radius) {
        level.sendParticles(ParticleTypes.FLASH, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(SPARK, centre.x, centre.y, centre.z, (int) (40 * radius), radius * 0.4, radius * 0.4, radius * 0.4, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y, centre.z, (int) (12 * radius), 0.3, 0.3, 0.3, 0.35);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y, centre.z, (int) (10 * radius), radius * 0.3,
                radius * 0.3, radius * 0.3, 0.6);
    }

    /**
     * The gamma pulse: a player takes the dose as prompt dose, cut by the blocks between; a mob is
     * made sick if the dose would sicken a player.
     */
    private static void gammaPulse(ServerLevel level, Vec3 centre, double ng, AnnihilationBlastModel.Params p) {
        if (!Config.radiationEnabled) {
            return;
        }
        double reach = AnnihilationBlastModel.pulseRadius(p, ng);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(reach),
                entity -> entity.isAlive() && !entity.isSpectator())) {
            Vec3 eyes = entity.getEyePosition();
            double open = AnnihilationBlastModel.dose(p, ng, eyes.distanceTo(centre));
            if (open <= 0) {
                continue;
            }
            double dose = open * RadiationRay.transmission(level, centre, eyes);
            if (dose < AnnihilationBlastModel.NEGLIGIBLE_DOSE) {
                continue;
            }
            if (entity instanceof Player player) {
                RadiationHandler.addPromptDose(player, (float) dose);
            } else if (dose >= Config.radiationSicknessThreshold) {
                entity.addEffect(new MobEffectInstance(AllNuclearRadiation.RADIATION_SICKNESS, 20 * 60,
                        dose >= 2 * Config.radiationSicknessThreshold ? 1 : 0));
            }
        }
    }
}
