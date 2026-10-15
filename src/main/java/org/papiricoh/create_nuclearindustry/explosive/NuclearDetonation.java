package org.papiricoh.create_nuclearindustry.explosive;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveManager;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationStyle;
import org.papiricoh.create_nuclearindustry.explosive.blast.Wind;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.slf4j.Logger;

/**
 * Every nuclear detonation (bomb, missile, cannon shell): works out its {@link DetonationProfile} from the
 * warhead and the height it bursts at, then sets each effect going. At once: the flash that burns and the
 * prompt radiation ({@link NuclearFlash}), a small vanilla explosion where it went off, the fallout,
 * carried by the wind, and the packet that shows players the explosion. Over the next ticks: the blast
 * wave ({@link BlastWaveManager}), and the crater and the damage to the ground ({@link NuclearBlastManager}).
 * Dirty bombs keep a plain conventional blast. A thermonuclear warhead goes the same way, scaled up, with
 * its crater kept small and a hot spot from its tamper.
 */
public final class NuclearDetonation {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Fallout reaches this far, as a multiple of the warhead's destruction radius, before the wind stretches it. */
    public static final float FALLOUT_RADIUS_FACTOR = 1.5f;
    /** Ground contamination at ground zero per point of blast power. */
    public static final float FALLOUT_PER_POWER = 0.2f;
    /** Strongest vanilla explosion a nuclear fireball makes; the blast wave does the rest. */
    public static final float FIREBALL_MAX_POWER = 12.0f;
    /** How far from a nuclear shell's impact the crew still sees it go off. */
    private static final double SHELL_WITNESS_RADIUS = 512.0;

    private NuclearDetonation() {}

    /**
     * How a kind of warhead's detonation differs from a fission one's.
     *
     * @param craterCap     widest crater bowl
     * @param waveCap       furthest the blast wave travels
     * @param thermalReach  the flash's reach, a multiple of the destruction radius
     * @param falloutRadius the fallout's reach, or 0 for the usual 1.5 radii
     * @param hotSpot       the lasting hot spot left at ground zero, rad/s
     */
    private record Tuning(double craterCap, int waveCap, double thermalReach, int falloutRadius, float hotSpot) {
        static Tuning fission() {
            return new Tuning(Double.POSITIVE_INFINITY, CivilDefenseConfig.blastWaveMaxRadius, ExplosiveConfig.thermalReach, 0, 0);
        }
    }

    /** A cannon shell's warhead of {@code uraniumUnits} at {@code averageEnrichment}. */
    public static void detonateShell(ServerLevel level, Position position, int uraniumUnits, float averageEnrichment) {
        detonate(level, position, uraniumUnits, averageEnrichment);
        NuclearAdvancements.awardNearby(level, BlockPos.containing(position.x(), position.y(), position.z()), SHELL_WITNESS_RADIUS,
                NuclearEvent.NUCLEAR_SHELL_FIRED);
    }

    /** A thermonuclear cannon shell's warhead. */
    public static void detonateShellThermonuclear(ServerLevel level, Position position, ThermonuclearStats.Result stats) {
        if (!stats.live()) {
            return;
        }
        detonateThermonuclear(level, new Vec3(position.x(), position.y(), position.z()), stats);
        NuclearAdvancements.awardNearby(level, BlockPos.containing(position.x(), position.y(), position.z()), SHELL_WITNESS_RADIUS,
                NuclearEvent.NUCLEAR_SHELL_FIRED);
    }

    /**
     * A thermonuclear warhead of {@code stats}. One whose secondary did not light (none loaded, or the
     * server turned them off) goes off as its fission primary alone.
     */
    public static void detonateThermonuclear(ServerLevel level, Vec3 impact, ThermonuclearStats.Result stats) {
        if (!stats.live()) {
            return;
        }
        if (stats.fizzle() || !ExplosiveConfig.thermonuclearEnabled) {
            int units = ThermonuclearStats.PRIMARY_UNITS;
            float enrichment = WarheadStats.REQUIRED_ENRICHMENT;
            detonate(level, impact, WarheadStats.power(units, enrichment), WarheadStats.horizontalRadius(units, enrichment),
                    WarheadStats.intensity(enrichment), 0.0f);
            return;
        }
        explode(level, impact, stats.power(), stats.radius(), stats.intensity(), 0.0f, DetonationStyle.THERMONUCLEAR,
                new Tuning(stats.maxCrater(), ExplosiveConfig.thermonuclearBlastWaveMaxRadius, ExplosiveConfig.thermonuclearThermalReach,
                        stats.falloutRadius(), stats.hotSpot()));
    }

    public static void detonate(ServerLevel level, Position position, int uraniumUnits, float averageEnrichment) {
        detonate(level, new Vec3(position.x(), position.y(), position.z()),
                WarheadStats.power(uraniumUnits, averageEnrichment),
                WarheadStats.horizontalRadius(uraniumUnits, averageEnrichment),
                WarheadStats.intensity(averageEnrichment), 0.0f);
    }

    /**
     * A nuclear detonation "salted" with {@code salting} rad/s of radioactive material packed around the
     * core: same blast, dirtier and wider fallout.
     */
    public static void detonate(ServerLevel level, Vec3 impact, float power, int horizontalRadius, float intensity, float salting) {
        explode(level, impact, power, horizontalRadius, intensity, salting, DetonationStyle.FISSION, Tuning.fission());
    }

    private static void explode(ServerLevel level, Vec3 impact, float power, int horizontalRadius, float intensity, float salting,
                                DetonationStyle style, Tuning tuning) {
        // A warhead aboard a ship goes off where the ship really is, and takes its ship with it.
        ShipSpace.Ship carrier = ShipSpace.shipAt(level, impact);
        impact = ShipSpace.toWorld(level, impact);
        BlockPos center = BlockPos.containing(impact);
        DetonationProfile profile = DetonationProfile.of(power, horizontalRadius, intensity, burstHeight(level, impact),
                ExplosiveConfig.settings(tuning.thermalReach()), tuning.craterCap());
        LOGGER.info("{} detonation at {} with power={}, horizontalRadius={}, burstHeight={}, airburst={}, salting={}",
                style == DetonationStyle.THERMONUCLEAR ? "Thermonuclear" : "Nuclear", center, power, horizontalRadius,
                Math.round(profile.burstHeight()), profile.airburst(), salting);

        NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(level, impact, power, horizontalRadius, style));
        NuclearExplosionSoundManager.playDelayedForWorld(level, impact, style);
        double waveRadius = Math.min(tuning.waveCap(), BlastWaveModel.maxRadius(horizontalRadius, tuning.waveCap())
                * (profile.airburst() ? DetonationProfile.AIRBURST_REACH : 1.0));
        DetonationPayload.of(impact, profile, waveRadius, level.random.nextLong(), style).send(level);
        NuclearFlash.strike(level, impact, profile);
        // Only the bang and its hurt: the crater does the digging, without waking the water round ground zero.
        level.explode(null, impact.x, impact.y, impact.z, fireballPower(power), Level.ExplosionInteraction.NONE);
        if (ExplosiveConfig.crater) {
            NuclearBlastManager.start(level, center, profile.withinReach(ExplosiveConfig.craterMaxReach));
        }
        ShipBlastManager.start(level, impact, profile, carrier);
        BlastWaveManager.start(level, impact, BlastWaveModel.yield(power, CivilDefenseConfig.blastWaveYieldPerPower), waveRadius);
        int radius = tuning.falloutRadius() > 0 ? tuning.falloutRadius() : falloutRadius(horizontalRadius);
        float fallout = (float) profile.falloutIntensity();
        if (salting > 0) {
            radius = Math.max(radius, DirtyWarheadStats.falloutRadius(salting));
            fallout = (float) Math.min(Config.contaminationMax, fallout + DirtyWarheadStats.falloutIntensity(salting));
        }
        Contamination.fallout(level, center, radius, fallout, wind(level), ExplosiveConfig.windStretch);
        if (tuning.hotSpot() > 0) {
            FalloutData.contaminate(level, center, tuning.hotSpot(), DirtyWarheadStats.HOT_SPOT_TICKS);
        }
        NuclearAdvancements.awardNearby(level, center, Math.max(64, radius * 2), NuclearEvent.NUCLEAR_DETONATION);
        if (style == DetonationStyle.THERMONUCLEAR) {
            NuclearAdvancements.awardNearby(level, center, Math.max(64, radius * 2), NuclearEvent.THERMONUCLEAR_DETONATION);
        }
    }

    /**
     * A dirty bomb: a small conventional blast that scatters {@code load} rad/s of radioactive
     * material. No crater, but wide fallout, drifting with the wind, and a hot spot at ground zero that
     * fades over days.
     */
    public static void detonateDirty(ServerLevel level, Vec3 impact, float load) {
        impact = ShipSpace.toWorld(level, impact);
        BlockPos center = BlockPos.containing(impact);
        LOGGER.info("Dirty bomb detonation at {} with load={}", center, load);
        float power = DirtyWarheadStats.blastPower(load);
        NeoForge.EVENT_BUS.post(new NuclearDetonationEvent(level, impact, power, DirtyWarheadStats.falloutRadius(load), DetonationStyle.DIRTY));
        DetonationPayload.dirty(impact, power, level.random.nextLong()).send(level);
        level.explode(null, impact.x, impact.y, impact.z, power, Level.ExplosionInteraction.BLOCK);
        Contamination.fallout(level, center, DirtyWarheadStats.falloutRadius(load), DirtyWarheadStats.falloutIntensity(load),
                wind(level), ExplosiveConfig.windStretch);
        FalloutData.contaminate(level, center, DirtyWarheadStats.hotSpot(load), DirtyWarheadStats.HOT_SPOT_TICKS);
        NuclearAdvancements.awardNearby(level, center, Math.max(64, DirtyWarheadStats.falloutRadius(load)), NuclearEvent.DIRTY_DETONATION);
    }

    /** Which way the wind blows in the level now, radians (see {@link Wind}). */
    public static double wind(ServerLevel level) {
        return Wind.angle(level.getSeed(), level.getGameTime());
    }

    /**
     * Blocks above the ground the detonation is at: the top of the terrain under it (leaves do not count).
     * Below 0 underground. Ground zero's chunk is loaded for it if it is not: one chunk, once.
     */
    public static double burstHeight(ServerLevel level, Vec3 impact) {
        int x = (int) Math.floor(impact.x);
        int z = (int) Math.floor(impact.z);
        int top = level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        return impact.y - top;
    }

    /** Power of the vanilla explosion at ground zero: the fireball, which wrecks the bomb's surroundings. */
    public static float fireballPower(float power) {
        return Math.min(FIREBALL_MAX_POWER, power * 0.35f);
    }

    public static int falloutRadius(int horizontalRadius) {
        return Math.round(horizontalRadius * FALLOUT_RADIUS_FACTOR);
    }

    public static float falloutIntensity(float power) {
        return (float) Math.min(Config.contaminationMax, power * FALLOUT_PER_POWER);
    }
}
