package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;

/**
 * How far each effect of a nuclear detonation reaches, worked out once when it goes off from the
 * warhead's figures ({@code WarheadStats}) and the height it bursts at. Inside out: the fireball; the
 * crater bowl, its raised rim and the ejecta thrown round it; the severe zone where the blast flattens
 * whatever stands above ground; the moderate zone where it wrecks part of it; the thermal flash that
 * scorches, chars and sets fires; prompt radiation; and the fallout.
 * <p>
 * A burst higher than its fireball (an airburst) digs no crater and leaves a tenth of the fallout, but
 * its blast and flash reach further. No game classes, so it is tested on its own.
 *
 * @param power          the warhead's blast power, as {@code WarheadStats} gives it
 * @param yieldKg        TNT equivalent, kg, for the blast wave's overpressure
 * @param horizontalRadius the warhead's destruction radius: the zones never reach past a multiple of it
 * @param intensity      the warhead's intensity (1 at weapons grade), roughens the crater's edge
 * @param burstHeight    blocks above the ground the warhead went off at; below 0 underground
 * @param airburst       whether it went off above its fireball
 * @param fireballRadius blocks
 * @param craterRadius   radius of the bowl at ground level, 0 for an airburst
 * @param craterDepth    depth of the bowl at its centre
 * @param rimRadius      where the raised rim ends
 * @param rimHeight      how high the rim stands at the bowl's edge
 * @param ejectaRadius   where scattered ejecta ends
 * @param glassRadius    the bowl's floor turns to glass inside this
 * @param severeRadius   everything above ground that is not blast-proof is blown away inside this
 * @param moderateRadius part of it is wrecked out to this
 * @param thermalRadius  the flash scorches the ground and sets fires out to this
 * @param promptRadius   prompt radiation is worth counting out to this
 * @param falloutRadius  the fallout's reach, before the wind stretches it
 * @param falloutIntensity contamination at ground zero, rad/s
 */
public record DetonationProfile(float power, double yieldKg, int horizontalRadius, float intensity, double burstHeight, boolean airburst,
                                double fireballRadius, double craterRadius, double craterDepth, double rimRadius, double rimHeight,
                                double ejectaRadius, double glassRadius, double severeRadius, double moderateRadius,
                                double thermalRadius, double promptRadius, double falloutRadius, double falloutIntensity) {

    /**
     * What the effects are measured by, from the server config.
     *
     * @param kgPerPower     TNT equivalent per point of power (the blast wave's)
     * @param severeKPa      overpressure that flattens what stands above ground
     * @param moderateKPa    overpressure that wrecks part of it
     * @param thermalReach   the flash's reach, a multiple of the destruction radius
     * @param falloutPerPower contamination at ground zero per point of power
     * @param falloutMax     most contamination at ground zero
     */
    public record Settings(double kgPerPower, double severeKPa, double moderateKPa, double thermalReach, double falloutPerPower,
                           double falloutMax) {
        public static final Settings DEFAULT = new Settings(250, 35, 12, 1.6, 0.2, 50);
    }

    /** The fireball, the smallest of any warhead's. */
    public static final double MIN_FIREBALL = 6;
    /** How much an airburst's blast and flash reach further. */
    public static final double AIRBURST_REACH = 1.25;
    /** What an airburst leaves of the fallout. */
    public static final double AIRBURST_FALLOUT = 0.1;
    /** The zones never reach past this multiple of the destruction radius. */
    public static final double MAX_ZONE_REACH = 1.2;

    public static DetonationProfile of(float power, int horizontalRadius, float intensity, double burstHeight, Settings settings) {
        return of(power, horizontalRadius, intensity, burstHeight, settings, Double.POSITIVE_INFINITY);
    }

    /**
     * As {@link #of(float, int, float, double, Settings)}, with the crater bowl no wider than
     * {@code maxCraterRadius}: a thermonuclear warhead's blast reaches far past the hole it digs, and
     * digging a bowl that scales with it would cost the server too much. Its rim, ejecta and glass follow
     * the smaller bowl; the blast zones do not.
     */
    public static DetonationProfile of(float power, int horizontalRadius, float intensity, double burstHeight, Settings settings,
                                       double maxCraterRadius) {
        double yieldKg = BlastWaveModel.yield(power, settings.kgPerPower());
        double fireball = Math.max(MIN_FIREBALL, 0.12 * horizontalRadius);
        boolean airburst = burstHeight > fireball;
        double reach = airburst ? AIRBURST_REACH : 1.0;
        double crater = airburst ? 0 : Math.min(Math.max(0, maxCraterRadius), 0.3 * horizontalRadius * (burstHeight < 0 ? 1.15 : 1.0));
        double depth = 0.32 * crater;
        double cap = MAX_ZONE_REACH * horizontalRadius;
        double severe = Math.min(horizontalRadius, radiusAt(settings.severeKPa(), yieldKg) * reach);
        double moderate = Math.max(severe, Math.min(cap, radiusAt(settings.moderateKPa(), yieldKg) * reach));
        double thermal = settings.thermalReach() * horizontalRadius * reach;
        double fallout = Math.min(settings.falloutMax(), power * settings.falloutPerPower()) * (airburst ? AIRBURST_FALLOUT : 1.0);
        return new DetonationProfile(power, yieldKg, horizontalRadius, intensity, burstHeight, airburst, fireball,
                crater, depth, 1.3 * crater, crater > 0 ? Math.max(1, Math.round(0.07 * crater)) : 0, 2.0 * crater, 0.8 * crater,
                Math.max(severe, crater), Math.max(moderate, 1.3 * crater), thermal, 0.7 * horizontalRadius,
                1.5 * horizontalRadius, fallout);
    }

    /**
     * Only a bowl, for tests and anything that wants a hole without a detonation's other effects:
     * {@code radius} wide and {@code depth} deep below its centre.
     */
    public static DetonationProfile craterOnly(double radius, double depth, float intensity) {
        return new DetonationProfile(0, 0, (int) Math.ceil(radius), intensity, 0, false, 0, radius, depth, radius, 0, radius, 0,
                radius, radius, 0, 0, 0, 0);
    }

    /** How far, blocks, the blast wave keeps {@code kPa} of overpressure in free air. */
    public static double radiusAt(double kPa, double yieldKg) {
        if (yieldKg <= 0 || kPa <= 0) {
            return 0;
        }
        double low = 0;
        double high = 1;
        while (BlastWaveModel.overpressure(high, yieldKg) > kPa && high < 1.0e6) {
            high *= 2;
        }
        for (int i = 0; i < 50; i++) {
            double middle = (low + high) / 2;
            if (BlastWaveModel.overpressure(middle, yieldKg) > kPa) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) / 2;
    }

    /** Every figure as numbers, in the record's order, for saving a crater being dug. */
    public double[] encode() {
        return new double[]{power, yieldKg, horizontalRadius, intensity, burstHeight, airburst ? 1 : 0, fireballRadius, craterRadius,
                craterDepth, rimRadius, rimHeight, ejectaRadius, glassRadius, severeRadius, moderateRadius, thermalRadius, promptRadius,
                falloutRadius, falloutIntensity};
    }

    /** A profile saved by {@link #encode}, or null when the numbers are not one. */
    public static DetonationProfile decode(double[] v) {
        if (v.length != 19) {
            return null;
        }
        return new DetonationProfile((float) v[0], v[1], (int) v[2], (float) v[3], v[4], v[5] != 0, v[6], v[7], v[8], v[9], v[10],
                v[11], v[12], v[13], v[14], v[15], v[16], v[17], v[18]);
    }

    /**
     * This profile with what it does to blocks kept within {@code maxReach} blocks of ground zero, so the
     * columns a detonation works through stay bounded. The wrecked and scorched zones shrink together by
     * the same share, so they still fade out instead of stopping at a hard edge, and the severe zone and
     * the crater's ring are cut at the reach. Unchanged if it already fits.
     */
    public DetonationProfile withinReach(double maxReach) {
        double reach = blockReach();
        if (reach <= maxReach || reach <= 0) {
            return this;
        }
        double squeeze = maxReach / reach;
        double severe = Math.min(severeRadius, maxReach);
        double moderate = Math.min(maxReach, Math.max(severe, moderateRadius * squeeze));
        return new DetonationProfile(power, yieldKg, horizontalRadius, intensity, burstHeight, airburst, fireballRadius, craterRadius,
                craterDepth, Math.min(rimRadius, maxReach), rimHeight, Math.min(ejectaRadius, maxReach), glassRadius, severe, moderate,
                thermalRadius * squeeze, promptRadius, falloutRadius, falloutIntensity);
    }

    /** The furthest any effect on blocks reaches: how far out the columns are worked through. */
    public double blockReach() {
        return Math.max(Math.max(ejectaRadius, rimRadius), Math.max(moderateRadius, thermalRadius));
    }
}
