package org.papiricoh.create_nuclearindustry.propulsion.physics;

/**
 * A nuclear thermal rocket: propellant runs through a heat exchanger heated by hot primary coolant from
 * a reactor and leaves through a nozzle. The exchanger hands back the coolant cold, and the hotter and
 * lighter the gas, the faster it leaves: thrust grows with the flow and with √(T/M), so hydrogen gives
 * three times what water does. Each exchanger segment takes a share of the flow. The nozzle heats with
 * the flow, more with a propellant that cools it badly; above its limit it wears, and worn through it
 * fails. A trace of the primary's activity leaks into the exhaust, more as the nozzle wears. No game
 * classes.
 */
public final class ThermalRocketModel {
    public static final double AMBIENT = 20.0;
    /** Exchanger segments an engine may have. */
    public static final int MIN_SEGMENTS = 1, MAX_SEGMENTS = 8;

    /** What goes through the engine: its molar mass, g/mol, and how much it heats the nozzle compared with hydrogen. */
    public enum Propellant {
        WATER(18.0, 1.45),
        DEUTERIUM(4.0, 1.1),
        HYDROGEN(2.0, 1.0);

        public final double molarMass;
        public final double nozzleHeat;

        Propellant(double molarMass, double nozzleHeat) {
            this.molarMass = molarMass;
            this.nozzleHeat = nozzleHeat;
        }
    }

    /**
     * @param flowPerSegment    propellant one segment passes at full throttle, mB per tick
     * @param thrustPerMb       thrust per mB/tick of hydrogen at the coolant's temperature
     * @param primaryTemperature temperature of the hot primary coming in, °C
     * @param primaryPerMb      hot primary cooled per mB of propellant heated, mB
     * @param chamberDrop       how far below the primary the gas leaves at full flow, °C
     * @param nozzleRise        nozzle temperature above ambient at full flow of hydrogen, °C
     * @param nozzleLimit       nozzle temperature above which it wears, °C
     * @param nozzleResponse    share of the gap to its running temperature the nozzle closes each tick
     * @param wearPerTick       wear a tick at twice the limit (it scales with the excess), 0-1
     * @param leakPerMb         contamination released per mB of propellant, new nozzle
     */
    public record Params(int flowPerSegment, double thrustPerMb, double primaryTemperature, double primaryPerMb,
                         double chamberDrop, double nozzleRise, double nozzleLimit, double nozzleResponse,
                         double wearPerTick, double leakPerMb) {
        public static final Params DEFAULT = new Params(5, 150.0, 320.0, 1.0, 60.0, 1100.0, 1400.0, 0.02, 0.002, 1.0e-5);
    }

    /**
     * One tick.
     *
     * @param propellantUsed propellant that left through the nozzle, mB
     * @param primaryCooled  hot primary handed back cold, mB
     * @param chamber        temperature of the gas leaving the exchanger, °C
     * @param thrust         thrust along the engine's axis
     * @param nozzle         nozzle temperature, °C
     * @param wear           nozzle wear, 0-1; 1 is a failed nozzle
     * @param contamination  activity released with the exhaust
     */
    public record Step(int propellantUsed, int primaryCooled, double chamber, double thrust, double nozzle, double wear,
                       double contamination) {
        public boolean failed() {
            return wear >= 1.0;
        }

        public double share(int segments, Params p) {
            return propellantUsed / (double) Math.max(1, maxFlow(segments, p));
        }
    }

    private ThermalRocketModel() {}

    public static int maxFlow(int segments, Params p) {
        return Math.max(0, clampSegments(segments)) * Math.max(0, p.flowPerSegment());
    }

    private static int clampSegments(int segments) {
        return Math.max(0, Math.min(MAX_SEGMENTS, segments));
    }

    /**
     * Advances one tick.
     *
     * @param throttle   0-1, from the redstone signal
     * @param segments   heat exchanger segments
     * @param propellant what is in the propellant tank, or null for nothing
     * @param available  propellant in the tank, mB
     * @param hotPrimary hot primary waiting, mB
     * @param coldSpace  room for the cooled primary, mB
     * @param nozzle     nozzle temperature now, °C
     * @param wear       nozzle wear now, 0-1
     */
    public static Step tick(double throttle, int segments, Propellant propellant, int available, int hotPrimary, int coldSpace,
                            double nozzle, double wear, Params p) {
        double t = Math.max(0, Math.min(1, throttle));
        int max = maxFlow(segments, p);
        int flow = 0;
        if (propellant != null && max > 0 && t > 0 && wear < 1.0) {
            int demand = (int) Math.round(t * max);
            double perMb = Math.max(1.0e-9, p.primaryPerMb());
            int byPrimary = (int) Math.floor(Math.min(hotPrimary, coldSpace) / perMb);
            flow = Math.max(0, Math.min(demand, Math.min(available, byPrimary)));
        }
        double share = max <= 0 ? 0 : flow / (double) max;
        int cooled = (int) Math.ceil(flow * p.primaryPerMb());
        double chamber = flow > 0 ? p.primaryTemperature() - p.chamberDrop() * share : AMBIENT;
        double thrust = flow > 0 ? flow * p.thrustPerMb() * exhaustFactor(chamber, propellant, p) : 0;
        double heat = propellant == null ? 1.0 : propellant.nozzleHeat;
        double running = AMBIENT + share * p.nozzleRise() * heat;
        double response = Math.max(0, Math.min(1, p.nozzleResponse()));
        double next = nozzle + (running - nozzle) * response;
        double nextWear = Math.min(1.0, wear + wearAt(next, p));
        double released = flow * p.leakPerMb() * (1 + 4 * Math.max(0, Math.min(1, wear)));
        return new Step(flow, cooled, chamber, thrust, next, nextWear, released);
    }

    /** How fast the gas leaves compared with hydrogen at the coolant's temperature: √(T/T₀ · 2/M). */
    public static double exhaustFactor(double chamber, Propellant propellant, Params p) {
        double molar = propellant == null ? Propellant.HYDROGEN.molarMass : propellant.molarMass;
        double ratio = (chamber + 273.15) / (p.primaryTemperature() + 273.15) * (Propellant.HYDROGEN.molarMass / molar);
        return Math.sqrt(Math.max(0, ratio));
    }

    /** Wear one tick at nozzle temperature {@code nozzle}: none up to the limit, growing with the excess. */
    public static double wearAt(double nozzle, Params p) {
        if (nozzle <= p.nozzleLimit() || p.nozzleLimit() <= 0) {
            return 0;
        }
        return p.wearPerTick() * (nozzle - p.nozzleLimit()) / p.nozzleLimit();
    }
}
