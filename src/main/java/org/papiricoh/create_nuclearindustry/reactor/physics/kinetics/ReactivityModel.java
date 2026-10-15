package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * The reactivity of a fission core, in pcm (1 pcm = 0.00001 dk/k): what the fuel has in excess,
 * what graphite, a reflector or heavy water add, and what the control rods, dissolved boron, xenon,
 * heat, steam voids and damage take away.
 *
 * <p>The temperature and void terms are what make a reactor manageable: fuel that heats broadens
 * its resonances (Doppler) and absorbs more, hot or boiling water moderates less, so a power rise
 * pulls its own reactivity down and the core settles at a new, higher power. A graphite-moderated
 * core loses most of that: graphite keeps moderating when the water boils, so its void coefficient
 * turns positive. Pure arithmetic.
 */
public final class ReactivityModel {
    /** Temperatures the coefficients are measured from, Celsius. */
    public static final double REFERENCE_TEMPERATURE = 20.0;
    /**
     * Water's critical point, Celsius. Above it there is no liquid left to moderate less: the void
     * term already counts the core as dry, so the moderator term stops growing here.
     */
    public static final double MODERATOR_TEMPERATURE_LIMIT = 374.0;

    public enum Coolant { WATER, HEAVY_WATER, PRESSURIZED }

    /**
     * @param fuelExcess           reactivity of fresh reference fuel with nothing absorbing, pcm
     * @param burnupDefect         share of it gone at the end of the fuel's life
     * @param emptyChannelWorth    reactivity lost when every fuel channel is empty, pcm
     * @param rodBankWorth         worth of a full set of control rods (the absorption ratio scales it), pcm
     * @param rodWorthShape        0 straight, 1 full S curve
     * @param graphiteWorth        added by graphite round every fuel column, pcm
     * @param reflectorWorth       added by a full beryllium reflector, pcm
     * @param heavyWaterWorth      added by heavy water as coolant, pcm
     * @param dopplerCoefficient   pcm per kelvin of fuel temperature
     * @param moderatorCoefficient pcm per kelvin of coolant temperature, water moderated
     * @param voidCoefficient      pcm per percent of steam void, water moderated
     * @param graphiteModeratorCoefficient pcm per kelvin with full graphite
     * @param graphiteVoidCoefficient      pcm per percent void with full graphite
     * @param heavyWaterVoidCoefficient    pcm per percent void with heavy water
     * @param damageWorth          lost when the core is fully damaged, pcm
     * @param dryCoreWorth         lost by a water moderated core with no water at all: without its moderator
     *                             the chain reaction stops (graphite keeps moderating, so it scales down with it), pcm
     * @param boronWorthPerPpm     taken by each ppm of boron dissolved in the water covering the core, pcm
     */
    public record Params(double fuelExcess, double burnupDefect, double emptyChannelWorth, double rodBankWorth,
                         double rodWorthShape, double graphiteWorth, double reflectorWorth, double heavyWaterWorth,
                         double dopplerCoefficient, double moderatorCoefficient, double voidCoefficient,
                         double graphiteModeratorCoefficient, double graphiteVoidCoefficient,
                         double heavyWaterVoidCoefficient, double damageWorth, double dryCoreWorth, double boronWorthPerPpm) {
        /** Boron worth when none is given, pcm per ppm. */
        public static final double DEFAULT_BORON_WORTH = 5.0;
        public static final Params DEFAULT = new Params(12000, 0.6, 24000, 22000, 0.5, 1500, 1000, 500,
                -2.0, -4.0, -15.0, -1.0, 3.0, -5.0, 2000, 20000, DEFAULT_BORON_WORTH);

        /** The coefficients from before soluble boron, with its default worth. */
        public Params(double fuelExcess, double burnupDefect, double emptyChannelWorth, double rodBankWorth,
                      double rodWorthShape, double graphiteWorth, double reflectorWorth, double heavyWaterWorth,
                      double dopplerCoefficient, double moderatorCoefficient, double voidCoefficient,
                      double graphiteModeratorCoefficient, double graphiteVoidCoefficient,
                      double heavyWaterVoidCoefficient, double damageWorth, double dryCoreWorth) {
            this(fuelExcess, burnupDefect, emptyChannelWorth, rodBankWorth, rodWorthShape, graphiteWorth, reflectorWorth,
                    heavyWaterWorth, dopplerCoefficient, moderatorCoefficient, voidCoefficient, graphiteModeratorCoefficient,
                    graphiteVoidCoefficient, heavyWaterVoidCoefficient, damageWorth, dryCoreWorth, DEFAULT_BORON_WORTH);
        }
    }

    /**
     * @param fuelReactivity   loaded fuel's reactivity relative to fresh 5% uranium (enrichment, kind, breeding)
     * @param burnt            share of the loaded fuel burnt, 0..1
     * @param noDefectShare    share of the fuel that breeds instead of losing reactivity (thorium), 0..1
     * @param emptyFraction    share of the fuel channels with no fuel, 0..1
     * @param moderation       graphite round the fuel, 0..1
     * @param reflection       beryllium lining the core, 0..1
     * @param coolant          what cools the core
     * @param xenon            reactivity the xenon holds back, pcm (positive)
     * @param rods             reactivity the control rods hold back, pcm (positive)
     * @param fuelTemperature  Celsius
     * @param coolantTemperature Celsius
     * @param voidFraction     steam in the core, 0..1
     * @param coverage         share of the core under water, 0..1
     * @param damage           core damage, 0..1
     * @param pwrVoidWorth     pressurized water: share of reactivity a fully voided core loses
     * @param pwrTemperatureCoefficient pressurized water: share lost per kelvin
     * @param boronPpm         boron dissolved in the core's water, ppm
     */
    public record Inputs(double fuelReactivity, double burnt, double noDefectShare, double emptyFraction, double moderation,
                         double reflection, Coolant coolant, double xenon, double rods, double fuelTemperature,
                         double coolantTemperature, double voidFraction, double coverage, double damage, double pwrVoidWorth,
                         double pwrTemperatureCoefficient, double boronPpm) {
        /** A core with no boron in its water. */
        public Inputs(double fuelReactivity, double burnt, double noDefectShare, double emptyFraction, double moderation,
                      double reflection, Coolant coolant, double xenon, double rods, double fuelTemperature,
                      double coolantTemperature, double voidFraction, double coverage, double damage, double pwrVoidWorth,
                      double pwrTemperatureCoefficient) {
            this(fuelReactivity, burnt, noDefectShare, emptyFraction, moderation, reflection, coolant, xenon, rods, fuelTemperature,
                    coolantTemperature, voidFraction, coverage, damage, pwrVoidWorth, pwrTemperatureCoefficient, 0);
        }
    }

    /** Every term, pcm; positive adds reactivity. */
    public record Breakdown(double fuel, double moderation, double reflector, double heavyWater, double xenon, double rods,
                            double doppler, double moderator, double voids, double damage, double boron) {
        /** A breakdown with no boron. */
        public Breakdown(double fuel, double moderation, double reflector, double heavyWater, double xenon, double rods,
                         double doppler, double moderator, double voids, double damage) {
            this(fuel, moderation, reflector, heavyWater, xenon, rods, doppler, moderator, voids, damage, 0);
        }

        public double total() {
            return fuel + moderation + reflector + heavyWater + xenon + rods + doppler + moderator + voids + damage + boron;
        }

        /** Reactivity with the rods all the way out. */
        public double withoutRods() {
            return total() - rods;
        }

        /** The same reactivity grouped the way a player reads it. */
        public Parts parts() {
            return new Parts(fuel + moderation + reflector + heavyWater, rods, doppler + moderator, voids, xenon, damage, boron);
        }
    }

    /**
     * The breakdown grouped into what a player can act on, pcm: the fuel (with its graphite, reflector
     * and heavy water), the rods, the temperature feedback (Doppler and moderator), the steam and
     * missing water, the xenon, the damage and the boron dissolved in the water. They add up to the
     * net reactivity.
     */
    public record Parts(double fuel, double rods, double temperature, double voids, double xenon, double damage, double boron) {
        public static final Parts NONE = new Parts(0, 0, 0, 0, 0, 0, 0);

        /** Parts with no boron. */
        public Parts(double fuel, double rods, double temperature, double voids, double xenon, double damage) {
            this(fuel, rods, temperature, voids, xenon, damage, 0);
        }

        public double total() {
            return fuel + rods + temperature + voids + xenon + damage + boron;
        }
    }

    private ReactivityModel() {}

    public static Breakdown compute(Params p, Inputs in) {
        double m = clamp(in.moderation());
        double burnDefect = p.burnupDefect() * clamp(in.burnt()) * (1 - clamp(in.noDefectShare()));
        double fuel = p.fuelExcess() * Math.max(0, in.fuelReactivity()) * (1 - burnDefect)
                - p.emptyChannelWorth() * clamp(in.emptyFraction());
        double heavy = in.coolant() == Coolant.HEAVY_WATER ? p.heavyWaterWorth() : 0;
        double alphaModerator;
        double alphaVoid;
        if (in.coolant() == Coolant.PRESSURIZED) {
            // The pressurized loop's coefficients keep their meaning: a share of a 2000 pcm reference per
            // kelvin, and of 2500 pcm for a fully voided core (the water defaults give -4 and -15).
            alphaModerator = -2000.0 * in.pwrTemperatureCoefficient();
            alphaVoid = -25.0 * in.pwrVoidWorth();
        } else {
            alphaModerator = p.moderatorCoefficient();
            alphaVoid = in.coolant() == Coolant.HEAVY_WATER ? p.heavyWaterVoidCoefficient() : p.voidCoefficient();
        }
        alphaModerator = lerp(alphaModerator, p.graphiteModeratorCoefficient(), m);
        alphaVoid = lerp(alphaVoid, p.graphiteVoidCoefficient(), m);
        double doppler = p.dopplerCoefficient() * (in.fuelTemperature() - REFERENCE_TEMPERATURE);
        double moderator = alphaModerator * (Math.min(in.coolantTemperature(), MODERATOR_TEMPERATURE_LIMIT) - REFERENCE_TEMPERATURE);
        double voids = alphaVoid * 100.0 * clamp(in.voidFraction())
                - p.dryCoreWorth() * (1 - clamp(in.coverage())) * (1 - m);
        return new Breakdown(fuel, p.graphiteWorth() * m, p.reflectorWorth() * clamp(in.reflection()), heavy,
                -Math.max(0, in.xenon()), -Math.max(0, in.rods()), doppler, moderator, voids, -p.damageWorth() * clamp(in.damage()),
                -boron(p, in.boronPpm(), in.coverage()));
    }

    /**
     * Reactivity the dissolved boron holds back, pcm (positive): its worth per ppm times the ppm, for
     * the part of the core its water covers. Boron in water that has boiled out of the core holds
     * nothing back there.
     */
    public static double boron(Params p, double ppm, double coverage) {
        return Math.max(0, p.boronWorthPerPpm()) * Math.max(0, ppm) * clamp(coverage);
    }

    /**
     * Worth of the full set of rods, pcm: the bank worth times the absorption ratio of rods to fuel.
     * The ratio is not capped at one: a core with more rod blocks than it needs has stronger rods (the
     * simulator only guards it with {@code fission.reactivity.maxRodAbsorption}).
     */
    public static double rodBank(Params p, double maxAbsorption) {
        return p.rodBankWorth() * Math.max(0, maxAbsorption);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
