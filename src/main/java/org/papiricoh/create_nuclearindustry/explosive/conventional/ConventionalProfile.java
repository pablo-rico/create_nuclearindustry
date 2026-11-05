package org.papiricoh.create_nuclearindustry.explosive.conventional;

import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;

/**
 * How far each effect of a non-nuclear blast reaches, worked out once when it goes off from its charge (in TNT-block
 * equivalents, as the {@code explosive_charge} data map weighs each item) and the height it bursts at. Everything grows with
 * the cube root of the charge, as real blasts do, so a warhead with eight times the charge reaches twice as far. A full bay
 * of the strongest explosive ({@link Settings#referenceCharge()}) digs a crater of {@link Settings#maxCraterRadius()} and
 * sends a shock wave {@link Settings#maxWaveRadius()} blocks: a fearsome bomb, far short of the smallest atomic one.
 * <p>
 * A high-explosive charge digs a bowl and throws casing fragments. A thermobaric charge disperses a cloud of fuel and lights
 * it: its fireball is wide, its pressure wave longer and stronger for its weight, it reaches into shelters, sets everything
 * round it alight and digs little. A burst in the air, clear of its fireball, digs nothing but its wave reaches further. No
 * game classes, so it is tested on its own.
 *
 * @param kind           what the charge was made of
 * @param charge         TNT-block equivalents
 * @param yieldKg        the shock wave's TNT equivalent, kg, for {@link BlastWaveModel}: what makes its harm stop where the
 *                       wave's reach says
 * @param burstHeight    blocks above the ground
 * @param airburst       whether it went off clear of its fireball
 * @param fireballRadius blocks
 * @param craterRadius   the bowl at ground level, 0 for an airburst
 * @param craterDepth    the bowl's depth at its centre
 * @param severeRadius   what stands above ground is blown away inside this
 * @param moderateRadius part of it is wrecked out to this
 * @param waveRadius     how far the shock wave travels
 * @param fireRadius     the ground is scorched and set alight inside this
 * @param fragments      casing fragments thrown out
 * @param fragmentRadius fragments still hurt out to this
 * @param shelterReach   how much of a shelter's protection the wave gets through, 0 none to 1 all
 */
public record ConventionalProfile(ConventionalKind kind, double charge, double yieldKg, double burstHeight, boolean airburst,
                                  double fireballRadius, double craterRadius, double craterDepth, double severeRadius,
                                  double moderateRadius, double waveRadius, double fireRadius, int fragments,
                                  double fragmentRadius, double shelterReach) {

    /**
     * What the effects are measured by, from the server config.
     *
     * @param maxCraterRadius    crater of the reference charge, blocks
     * @param maxWaveRadius      shock wave reach of the reference charge, blocks
     * @param referenceCharge    the charge they are measured for: a full bay of the strongest explosive
     * @param airburstWaveBonus  how much further an airburst's wave reaches
     * @param thermobaricWave    how much further a thermobaric charge's wave reaches than high explosive's
     * @param thermobaricCrater  how much of high explosive's crater a thermobaric charge digs
     * @param thermobaricFire    how far a thermobaric charge sets fires, a share of its wave's reach
     * @param thermobaricShelter how much of a shelter's protection a thermobaric wave gets through
     * @param fragmentsPerCharge casing fragments per TNT-block equivalent of high explosive
     * @param damageThresholdKPa overpressure below which the wave no longer hurts (the blast wave's own)
     * @param severeKPa          overpressure that flattens what stands above ground
     * @param moderateKPa        overpressure that wrecks part of it
     */
    public record Settings(double maxCraterRadius, double maxWaveRadius, double referenceCharge, double airburstWaveBonus,
                           double thermobaricWave, double thermobaricCrater, double thermobaricFire, double thermobaricShelter,
                           double fragmentsPerCharge, double damageThresholdKPa, double severeKPa, double moderateKPa) {
        public static final Settings DEFAULT = new Settings(20, 64, 192, 1.25, 1.6, 0.25, 0.5, 0.5, 40, 5, 35, 12);
    }

    /** The wave hurts out to this share of its reach; past it, it only shoves and rattles windows. */
    public static final double HARM_REACH = 0.85;
    /** The smallest fireball, blocks. */
    public static final double MIN_FIREBALL = 1.0;
    /** Most fragments one blast throws, however big. */
    public static final int MAX_FRAGMENTS = 4000;
    /** A bomblet's casing is thin and scored to break into many small fragments. */
    private static final double BOMBLET_FRAGMENTS = 2.5;
    /** A thermobaric charge's thin case throws few fragments. */
    private static final double THERMOBARIC_FRAGMENTS = 0.15;

    /** How a blast of {@code charge} TNT-block equivalents of {@code kind} goes off {@code burstHeight} blocks above the ground. */
    public static ConventionalProfile of(ConventionalKind kind, double charge, double burstHeight, Settings s) {
        double c = Math.max(0, charge);
        double scale = s.referenceCharge() > 0 ? Math.cbrt(c / s.referenceCharge()) : 0;
        boolean thermobaric = kind == ConventionalKind.THERMOBARIC;
        double crater = s.maxCraterRadius() * scale;
        double wave = s.maxWaveRadius() * scale;
        double fireball = Math.max(c > 0 ? MIN_FIREBALL : 0, 0.4 * crater);
        if (thermobaric) {
            fireball *= 2.2;
            wave *= s.thermobaricWave();
            crater *= s.thermobaricCrater();
        }
        boolean airburst = burstHeight > fireball && c > 0;
        if (airburst) {
            crater = 0;
            wave *= s.airburstWaveBonus();
        }
        double yieldKg = yieldFor(HARM_REACH * wave, s.damageThresholdKPa());
        double severe = Math.max(crater, DetonationProfile.radiusAt(s.severeKPa(), yieldKg));
        double moderate = Math.max(Math.max(severe, 1.3 * crater), DetonationProfile.radiusAt(s.moderateKPa(), yieldKg));
        double fire = thermobaric ? s.thermobaricFire() * wave : 1.3 * fireball;
        double perCharge = switch (kind) {
            case BOMBLET -> s.fragmentsPerCharge() * BOMBLET_FRAGMENTS;
            case THERMOBARIC -> s.fragmentsPerCharge() * THERMOBARIC_FRAGMENTS;
            default -> s.fragmentsPerCharge();
        };
        int fragments = (int) Math.min(MAX_FRAGMENTS, Math.round(perCharge * c));
        double fragmentRadius = fragments > 0 ? Math.max(4, 1.2 * wave) : 0;
        return new ConventionalProfile(kind, c, yieldKg, burstHeight, airburst, fireball, crater, 0.35 * crater, severe, moderate,
                wave, fire, fragments, fragmentRadius, thermobaric ? s.thermobaricShelter() : 0);
    }

    /** The TNT equivalent, kg, whose shock wave falls to {@code kPa} at {@code distance} blocks. */
    public static double yieldFor(double distance, double kPa) {
        if (distance <= 0 || kPa <= 0) {
            return 0;
        }
        double low = 0.05;
        double high = 1;
        while (BlastWaveModel.overpressureAtScaled(high) > kPa && high < 1.0e6) {
            high *= 2;
        }
        for (int i = 0; i < 60; i++) {
            double middle = (low + high) / 2;
            if (BlastWaveModel.overpressureAtScaled(middle) > kPa) {
                low = middle;
            } else {
                high = middle;
            }
        }
        double z = (low + high) / 2;
        double w = distance / z;
        return w * w * w;
    }

    /** Where it bursts in the air when set to: clear of its fireball, low enough for its wave to hit the ground hard. */
    public static double airburstHeight(ConventionalKind kind, double charge, Settings s, double heightFactor) {
        ConventionalProfile ground = of(kind, charge, 0, s);
        return Math.max(2, ground.fireballRadius() * Math.max(1.05, heightFactor));
    }

    /**
     * As a nuclear detonation's profile, for the crater job and the ships' blast: the bowl, its rim and ejecta, the flattened
     * and wrecked zones and the scorched ring, with no glass, no prompt radiation and no fallout. Blocks resisting more than
     * {@code blastProof} stand.
     */
    public DetonationProfile toDetonationProfile(double blastProof) {
        double rim = 1.25 * craterRadius;
        return new DetonationProfile((float) Math.max(charge, 1e-3), yieldKg, (int) Math.ceil(waveRadius), 0.6f, burstHeight, airburst,
                fireballRadius, craterRadius, craterDepth, rim, craterRadius > 0 ? Math.max(1, Math.round(0.08 * craterRadius)) : 0,
                1.8 * craterRadius, 0, severeRadius, moderateRadius, fireRadius, 0, 0, 0, Math.max(1, blastProof));
    }
}
