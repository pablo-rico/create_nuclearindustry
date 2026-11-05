package org.papiricoh.create_nuclearindustry.explosive.conventional;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.ShipBlastManager;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.slf4j.Logger;

/**
 * A non-nuclear blast going off: a high-explosive warhead, a thermobaric charge, a cluster bomblet or a dirty bomb's charge.
 * Its fireball engulfs whatever is closest, its crater is dug and the zones round it flattened and wrecked by the same job as a
 * nuclear crater's ({@link NuclearBlastManager}), its shock wave travels out and strikes what it reaches through walls and
 * shelters ({@link BlastWaveManager}), its fragments hit whatever can be seen from it ({@link Shrapnel}) and a few clods are
 * thrown from the crater. Protection mods are asked first, as for any explosion; so are listeners of
 * {@link ConventionalDetonationEvent}. Its harm is whoever set it off's.
 */
public final class ConventionalDetonation {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Damage, half hearts, at the very middle of a high-explosive fireball. */
    private static final double FIREBALL_PEAK = 60;

    private ConventionalDetonation() {}

    /** Sets off {@code charge} TNT equivalents of {@code kind} at {@code at}. */
    public static void detonate(ServerLevel level, Vec3 at, ConventionalKind kind, double charge, DetonationCause cause) {
        if (charge <= 0) {
            return;
        }
        ShipSpace.Ship carrier = ShipSpace.shipAt(level, at);
        Vec3 world = ShipSpace.toWorld(level, at);
        ConventionalProfile profile = ConventionalProfile.of(kind, charge, NuclearDetonation.burstHeight(level, world),
                ConventionalConfig.settings());
        if (NeoForge.EVENT_BUS.post(new ConventionalDetonationEvent(level, world, profile, cause)).isCanceled()) {
            return;
        }
        explode(level, world, profile, cause, carrier);
    }

    /**
     * The blast of {@code profile} at {@code at} (in the world, not a ship's plot), without posting the event: for callers that
     * post their own, as a dirty bomb does. False if a protection mod stopped it.
     */
    public static boolean explode(ServerLevel level, Vec3 at, ConventionalProfile profile, DetonationCause cause,
                                  @Nullable ShipSpace.Ship carrier) {
        if (profile.charge() <= 0) {
            return false;
        }
        Explosion probe = new Explosion(level, cause.entity(level), at.x, at.y, at.z, (float) Math.max(1, profile.fireballRadius()), false,
                Explosion.BlockInteraction.DESTROY);
        if (EventHooks.onExplosionStart(level, probe)) {
            return false;
        }
        LOGGER.debug("{} blast at {} of {} TNT: crater {}, wave {}, airburst {}", profile.kind(), BlockPos.containing(at),
                profile.charge(), Math.round(profile.craterRadius()), Math.round(profile.waveRadius()), profile.airburst());
        new ConventionalBlastPayload(at, profile.kind(), level.random.nextLong(), level.getGameTime(), (float) profile.charge(),
                (float) profile.burstHeight(), profile.airburst(), (float) profile.fireballRadius(), (float) profile.craterRadius(),
                (float) profile.waveRadius(), (float) org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig.blastWaveSpeed,
                (float) profile.yieldKg(), (float) profile.fireRadius()).send(level);
        boolean thermobaric = profile.kind() == ConventionalKind.THERMOBARIC;
        FireballStrike.strike(level, at, profile.fireballRadius() * (thermobaric ? 1.2 : 1.5), FIREBALL_PEAK * (thermobaric ? 1.5 : 1),
                thermobaric ? 12 : 4, cause, thermobaric ? ConventionalRegistry.THERMOBARIC_BURN : ConventionalRegistry.CONVENTIONAL_BLAST);
        DetonationProfile blocks = profile.toDetonationProfile(ConventionalConfig.blastProofResistance);
        if (ExplosiveConfig.crater) {
            NuclearBlastManager.start(level, BlockPos.containing(at), blocks);
        }
        ShipBlastManager.start(level, at, blocks, carrier);
        BlastWaveManager.start(level, at, profile.yieldKg(), profile.waveRadius(), cause, profile.shelterReach());
        Shrapnel.strike(level, at, profile, cause);
        if (!profile.airburst()) {
            DebrisThrower.throwOut(level, at, profile, ConventionalConfig.debrisMax, ConventionalConfig.blastProofResistance);
        }
        return true;
    }
}
