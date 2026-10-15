package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * An ionisation smoke detector. A speck of americium-241 ionises the air between two plates and a
 * small current flows; smoke particles soak up the ions and the current drops. The relative drop
 * saturates with the smoke in the chamber,
 * <pre>
 *   drop(s) = s / (s + halfDropSmoke)
 * </pre>
 * and the alarm sounds above {@code alarmDrop}, with a redstone signal that grows with the drop. The
 * drop does not depend on the source's activity, only the current does: a source too weak to ionise
 * ({@code minActivity}) leaves the detector dead (no signal), like one with a flat battery.
 *
 * <p>Smoke comes from fires within {@code radius} blocks, each counting less with distance:
 * {@code weight * (1 - d / (radius + 1))}. Pure arithmetic.
 */
public final class SmokeDetectorModel {

    /** What smokes near the detector, and how much; the block entity maps blocks to these. */
    public enum SmokeSource {
        /** Fire and soul fire blocks: open flame, the most smoke. */
        FIRE(1.0),
        /** A lit campfire (or soul campfire): its smoke column. */
        CAMPFIRE(0.8),
        /** Anything burning that is not fire itself: a burning entity, a lit furnace's flue. */
        BURNING(0.6),
        /** Lava: heat and fumes, little smoke. */
        LAVA(0.4);

        private final double weight;

        SmokeSource(double weight) {
            this.weight = weight;
        }

        public double weight() {
            return weight;
        }
    }

    /**
     * @param radius        blocks around the detector that count
     * @param halfDropSmoke smoke that halves the ion current
     * @param alarmDrop     current drop from which it alarms, 0..1
     * @param minActivity   source activity below which the detector is dead, relative
     */
    public record Params(int radius, double halfDropSmoke, double alarmDrop, double minActivity) {
        /** Five blocks; a fire anywhere in range trips it, lava only within three blocks. */
        public static final Params DEFAULT = new Params(5, 0.5, 0.2, 0.05);
    }

    private SmokeDetectorModel() {}

    /** Smoke one source adds at {@code distance} blocks (centre to centre); nothing outside the radius. */
    public static double contribution(Params p, SmokeSource source, double distance) {
        return contribution(p, source.weight(), distance);
    }

    /** Smoke a source of this weight adds at {@code distance} blocks; nothing outside the radius. */
    public static double contribution(Params p, double weight, double distance) {
        if (!(distance >= 0) || distance > p.radius()) {
            return 0.0;
        }
        return Math.max(0.0, weight) * Math.max(0.0, 1.0 - distance / (p.radius() + 1.0));
    }

    /** Relative drop of the ion current, 0..1. */
    public static double currentDrop(Params p, double smoke) {
        double s = Math.max(0.0, smoke);
        if (s <= 0) {
            return 0.0;
        }
        double half = Math.max(1.0e-9, p.halfDropSmoke());
        return s / (s + half);
    }

    /** Whether the detector works at all with a source this strong. */
    public static boolean alive(Params p, double activity) {
        return activity >= p.minActivity();
    }

    /** Whether it alarms. */
    public static boolean alarm(Params p, double smoke, double activity) {
        return alive(p, activity) && currentDrop(p, smoke) >= p.alarmDrop();
    }

    /** Redstone signal: 0 quiet (or dead), otherwise {@code max(1, round(15 * drop))}. */
    public static int signal(Params p, double smoke, double activity) {
        if (!alarm(p, smoke, activity)) {
            return 0;
        }
        return (int) Math.max(1, Math.min(15, Math.round(15.0 * currentDrop(p, smoke))));
    }
}
