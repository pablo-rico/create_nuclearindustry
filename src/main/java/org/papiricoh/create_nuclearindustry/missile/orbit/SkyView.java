package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarModel;

/**
 * Where a satellite shows in the sky of someone on the ground, and how brightly. It is drawn on the sky like a star, in the
 * direction it lies in, never at its real distance (a low one is kilometres up, a geostationary one ten times that). The world's
 * curvature is the radars' ({@link RadarModel#horizon}): past the horizon it is hidden, and nearer it sinks towards it as the
 * ground stations' dishes see it do. Free of game classes, for JUnit.
 */
public final class SkyView {
    /** Sine of the elevation below which it fades into the haze at the horizon. */
    static final double HAZE = 0.12;
    /** Ticks between a low satellite's sunlit flares, and how sharp they are. */
    static final double FLARE_PERIOD = 900, FLARE_SHARPNESS = 60;

    private SkyView() {
    }

    /** Whether something at {@code sat} is over the horizon of an eye at {@code eye} on a world of radius {@code earthRadius}. */
    public static boolean aboveHorizon(double earthRadius, V3 eye, V3 sat) {
        double dx = sat.x() - eye.x(), dz = sat.z() - eye.z();
        double ground = Math.sqrt(dx * dx + dz * dz);
        return ground <= RadarModel.horizon(earthRadius, eye.y(), sat.y());
    }

    /**
     * The unit direction an eye at {@code eye} sees something at {@code sat} in, lowered by the world's curvature over the
     * ground between them; null if it is below the horizon.
     */
    @Nullable
    public static V3 direction(double earthRadius, V3 eye, V3 sat) {
        if (!aboveHorizon(earthRadius, eye, sat)) return null;
        double dx = sat.x() - eye.x(), dz = sat.z() - eye.z();
        double ground2 = dx * dx + dz * dz;
        double drop = earthRadius > 0 ? ground2 / (2 * earthRadius) : 0;
        V3 d = new V3(dx, sat.y() - eye.y() - drop, dz);
        double length = d.length();
        return length < 1.0e-9 ? V3.UP : d.scale(1 / length);
    }

    /**
     * How visible satellites are, 0 to 1, with the stars at {@code starBrightness} (the game's, 0 by day to 0.5 at night) and
     * {@code rain} (0 to 1) clouding the sky over.
     */
    public static double skyDarkness(double starBrightness, double rain) {
        double dark = Math.max(0, Math.min(1, starBrightness * 2));
        return dark * (1 - Math.max(0, Math.min(1, rain)));
    }

    /** How much of a point at {@code direction} shows through the haze near the horizon, 0 to 1. */
    public static double haze(V3 direction) {
        double t = Math.max(0, Math.min(1, direction.y() / HAZE));
        return t * t * (3 - 2 * t);
    }

    /**
     * A moving satellite's brightness at {@code time} (ticks): 1, flaring up to {@code 1 + strength} for a few seconds now and then
     * as the sun catches its panels at the right angle, at its own moments ({@code seed}). A parked one does not flare.
     */
    public static double flare(double time, int seed, boolean moving, double strength) {
        if (!moving) return 1;
        double phase = (seed & 0xFFFF) / 65535.0 * Math.PI * 2;
        double s = Math.max(0, Math.sin(time * Math.PI * 2 / FLARE_PERIOD + phase));
        return 1 + strength * Math.pow(s, FLARE_SHARPNESS);
    }
}
