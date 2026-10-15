package org.papiricoh.create_nuclearindustry.isotope.physics;

import java.util.Locale;

/**
 * The radioisotopes the mod makes, with their default half-lives in game days (24000 ticks).
 *
 * <p>Two clocks, so that both kinds of isotope are playable:
 * <ul>
 *   <li><b>Short-lived medical isotopes run in real time</b>, one game day for one real day:
 *       technetium-99m (6.0 h, so 0.25 d) and its parent molybdenum-99 (66 h, so 2.75 d). The
 *       technetium cow then behaves like a real one: milked daily, it peaks about a day after an
 *       elution and is worn out in a little over a week.</li>
 *   <li><b>Long-lived isotopes are compressed by one common factor of 32</b> (real days / 32), which
 *       keeps their real ratios: cobalt-60 (5.27 y) 60 d, strontium-90 (28.8 y, 5.5 times cobalt)
 *       330 d, americium-241 (432 y) about 4900 d, so 5000 d: it never visibly decays.</li>
 * </ul>
 * A server's {@code timeScale} speeds every clock up or slows it down on top of this. Activities
 * are relative: 1 is a fresh item of nominal strength. No game classes.
 */
public enum Isotope {
    TECHNETIUM_99M(0.25, 0.10),
    MOLYBDENUM_99(2.75, 0.05),
    COBALT_60(60.0, 0.05),
    STRONTIUM_90(330.0, 0.05),
    AMERICIUM_241(5000.0, 0.05);

    private final double halfLifeDays;
    private final double usableAbove;

    Isotope(double halfLifeDays, double usableAbove) {
        this.halfLifeDays = halfLifeDays;
        this.usableAbove = usableAbove;
    }

    /** Default half-life, game days at time scale 1 (the config may override it). */
    public double halfLifeDays() {
        return halfLifeDays;
    }

    /** Relative activity below which an item of it counts as decayed (no use, tooltip "decayed"). */
    public double usableAbove() {
        return usableAbove;
    }

    /** Lower-case id for config keys and lang: {@code technetium_99m}, {@code cobalt_60}... */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The isotope with this {@link #id()}, or {@code null}. */
    public static Isotope byId(String id) {
        for (Isotope isotope : values()) {
            if (isotope.id().equals(id)) {
                return isotope;
            }
        }
        return null;
    }
}
