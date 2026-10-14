package org.papiricoh.create_nuclearindustry.explosive;

/**
 * The yield of a thermonuclear (Teller-Ulam) warhead from its three parts: a fission primary (one
 * weapons-grade core), whose X-rays compress and light a secondary of lithium-6 deuteride, wrapped in an
 * optional tamper of depleted uranium that the fusion neutrons split in turn. Without a primary nothing
 * goes off; a primary with no fuel fizzles as the lone core it is. The secondary multiplies the yield,
 * the tamper adds to it and to the fallout. The destruction radius grows with the cube root of the yield,
 * as the blast zones do. No game classes and no config: the caps are passed in.
 */
public final class ThermonuclearStats {
    /** Lithium deuteride that fills the secondary. */
    public static final int SECONDARY_FULL = 16;
    /** Depleted uranium blocks that fill the tamper. */
    public static final int TAMPER_FULL = 4;
    /** How many times the primary's yield a full secondary adds, with a plutonium primary. */
    public static final double SECONDARY_GAIN = 16;
    /** The share a full tamper adds to the yield. */
    public static final double TAMPER_GAIN = 0.6;
    /** A fission warhead's figures the ratios are measured against: a full atomic bomb. */
    public static final double REF_RADIUS = 250;
    public static final double REF_POWER = 160;
    /** The primary is one core: the warhead units of a fizzle. */
    public static final int PRIMARY_UNITS = 8;

    /** The fission primary, by how well it compresses the secondary. */
    public enum Primary {
        NONE(0),
        URANIUM(0.85),
        PLUTONIUM(1.0);

        private static final Primary[] VALUES = values();
        private final double efficiency;

        Primary(double efficiency) {
            this.efficiency = efficiency;
        }

        public double efficiency() {
            return efficiency;
        }

        public static Primary byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    /**
     * What the server lets a thermonuclear warhead reach.
     *
     * @param maxRadius          destruction radius
     * @param maxPower           blast power
     * @param maxCrater          crater bowl radius
     * @param tamperFalloutReach how much wider a full tamper makes the fallout, a multiple of the radius
     * @param tamperHotSpot      the hot spot a full tamper leaves at ground zero, rad/s
     */
    public record Caps(int maxRadius, double maxPower, double maxCrater, double tamperFalloutReach, double tamperHotSpot) {
        public static final Caps DEFAULT = new Caps(750, 4320, 110, 0.5, 150);
    }

    /**
     * A thermonuclear warhead's figures.
     *
     * @param power         blast power, on the fission warheads' scale
     * @param radius        destruction radius
     * @param intensity     roughens the crater's edge
     * @param falloutRadius how far its fallout reaches, before the wind
     * @param hotSpot       the lasting hot spot its tamper leaves, rad/s
     * @param maxCrater     the crater bowl's cap
     * @param fizzle        no secondary: only the primary went off
     */
    public record Result(Primary primary, int secondary, int tamper, float power, int radius, float intensity, int falloutRadius,
                         float hotSpot, double maxCrater, boolean fizzle) {
        public static final Result NONE = new Result(Primary.NONE, 0, 0, 0, 0, 0, 0, 0, 0, false);

        /** Whether it goes off at all. */
        public boolean live() {
            return primary != Primary.NONE && radius > 0;
        }
    }

    private ThermonuclearStats() {}

    /** The yield against a full atomic bomb's, or 0 without a primary or a secondary. */
    public static double yieldRatio(Primary primary, int secondary, int tamper) {
        if (primary == Primary.NONE || secondary <= 0) {
            return 0;
        }
        double fusion = 1 + SECONDARY_GAIN * primary.efficiency() * Math.min(secondary, SECONDARY_FULL) / SECONDARY_FULL;
        return fusion * (1 + TAMPER_GAIN * tamperShare(tamper));
    }

    public static Result of(Primary primary, int secondary, int tamper, Caps caps) {
        if (primary == null || primary == Primary.NONE) {
            return Result.NONE;
        }
        secondary = Math.max(0, Math.min(secondary, SECONDARY_FULL));
        tamper = Math.max(0, Math.min(tamper, TAMPER_FULL));
        if (secondary == 0) {
            // A fizzle: the primary alone, a small fission blast with nothing to light.
            int radius = WarheadStats.horizontalRadius(PRIMARY_UNITS, WarheadStats.REQUIRED_ENRICHMENT);
            return new Result(primary, 0, tamper, WarheadStats.power(PRIMARY_UNITS, WarheadStats.REQUIRED_ENRICHMENT), radius,
                    WarheadStats.intensity(WarheadStats.REQUIRED_ENRICHMENT), Math.round(radius * NuclearDetonation.FALLOUT_RADIUS_FACTOR),
                    0, Double.POSITIVE_INFINITY, true);
        }
        double ratio = yieldRatio(primary, secondary, tamper);
        int radius = (int) Math.min(caps.maxRadius(), Math.round(REF_RADIUS * Math.cbrt(ratio)));
        float power = (float) Math.min(caps.maxPower(), REF_POWER * ratio);
        double share = tamperShare(tamper);
        float intensity = (float) (1 + 0.25 * share);
        int fallout = (int) Math.round(radius * (NuclearDetonation.FALLOUT_RADIUS_FACTOR + caps.tamperFalloutReach() * share));
        float hotSpot = (float) (caps.tamperHotSpot() * share);
        return new Result(primary, secondary, tamper, power, radius, intensity, fallout, hotSpot, caps.maxCrater(), false);
    }

    private static double tamperShare(int tamper) {
        return Math.max(0, Math.min(tamper, TAMPER_FULL)) / (double) TAMPER_FULL;
    }
}
