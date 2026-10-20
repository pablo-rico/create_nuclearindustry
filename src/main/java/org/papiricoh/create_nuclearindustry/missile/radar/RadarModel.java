package org.papiricoh.create_nuclearindustry.missile.radar;

import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.Locale;
import java.util.UUID;

/**
 * What a missile radar can see and what it makes of it, free of game classes so JUnit can check it.
 * <ul>
 *   <li>Reach grows with the antenna's speed and the target's radar signature, both to the quarter power (the radar
 *   equation): {@code base × (rpm / minimum)^¼ × signature^¼}.</li>
 *   <li>Nothing below the horizon is seen: over a world of radius {@code R} a radar {@code h_r} above sea level and a target
 *   {@code h_t} above it see each other up to {@code √(2Rh_r) + √(2Rh_t)} apart.</li>
 *   <li>A rocket burning is a bright target; after burnout only its reentry vehicle is left, faint until the air heats it.</li>
 *   <li>Where a target would come down is worked out from where it is and how it moves, as if it were falling freely from
 *   now on (its instantaneous impact point), with an error ellipse that shrinks as it nears the ground.</li>
 * </ul>
 * A base range of 0 sees every flight in the dimension; an Earth radius of 0 has no horizon.
 */
public final class RadarModel {
    /** Sea level: heights for the horizon are measured from it. */
    public static final double SEA_LEVEL = 63;
    /** A burning rocket's signature, against 1 for a bare rocket body. */
    public static final double BOOST_SIGNATURE = 8;
    /** A reentry vehicle on its own. */
    public static final double REENTRY_VEHICLE_SIGNATURE = 0.3;
    /** How much the air heating a reentry vehicle brightens it. */
    public static final double REENTRY_GLOW = 2;

    private RadarModel() {
    }

    /** How bright a target is to a radar in {@code phase}. */
    public static double signature(MissileFlight.Phase phase) {
        return switch (phase) {
            case BOOST -> BOOST_SIGNATURE;
            case MIDCOURSE -> REENTRY_VEHICLE_SIGNATURE;
            case REENTRY, TERMINAL -> REENTRY_VEHICLE_SIGNATURE * REENTRY_GLOW;
            case IMPACT -> 0;
        };
    }

    /** How far a radar of base range {@code base} turning at {@code rpm} (needing {@code minimumRpm}) sees a target of {@code signature}. */
    public static double range(double base, double rpm, double minimumRpm, double signature) {
        if (base <= 0) return Double.POSITIVE_INFINITY;
        if (signature <= 0 || rpm == 0) return 0;
        return base * Math.pow(Math.abs(rpm) / Math.max(1e-6, minimumRpm), 0.25) * Math.pow(signature, 0.25);
    }

    /** How far apart a radar at height {@code radarY} and a target at {@code targetY} can be and still see each other. */
    public static double horizon(double earthRadius, double radarY, double targetY) {
        if (earthRadius <= 0) return Double.POSITIVE_INFINITY;
        double hr = Math.max(1, radarY - SEA_LEVEL), ht = Math.max(1, targetY - SEA_LEVEL);
        return Math.sqrt(2 * earthRadius * hr) + Math.sqrt(2 * earthRadius * ht);
    }

    /** Whether a radar at {@code radar} sees a target at {@code target}: within its reach and above the horizon. */
    public static boolean detects(V3 radar, V3 target, double reach, double earthRadius) {
        double distance = radar.distance(target);
        return distance <= reach && distance <= horizon(earthRadius, radar.y(), target.y());
    }

    /** Ticks until something at {@code pos} moving at {@code vel} under {@code gravity} falls to height {@code groundY}. */
    public static double timeToGround(V3 pos, V3 vel, double gravity, double groundY) {
        double drop = pos.y() - groundY;
        if (gravity <= 0) return vel.y() < 0 && drop > 0 ? drop / -vel.y() : Double.POSITIVE_INFINITY;
        double disc = vel.y() * vel.y() + 2 * gravity * drop;
        if (disc < 0) return 0;
        return Math.max(0, (vel.y() + Math.sqrt(disc)) / gravity);
    }

    /** The instantaneous impact point: where it would come down on {@code groundY} if it fell freely from now on. */
    public static V3 impactPoint(V3 pos, V3 vel, double gravity, double groundY) {
        double t = timeToGround(pos, vel, gravity, groundY);
        if (!Double.isFinite(t)) t = 0;
        return new V3(pos.x() + vel.x() * t, groundY, pos.z() + vel.z() * t);
    }

    /**
     * The impact error ellipse: semi-axes along and across its track, blocks, and the track's heading over the ground (radians,
     * 0 is +Z). While the engines burn the prediction can only guess; once it coasts it tightens with the time left to fall.
     */
    public record Ellipse(double along, double across, double heading) {
        /** Whether the ellipse round {@code centre} reaches within {@code radius} of {@code zone}. */
        public boolean touches(double cx, double cz, double zx, double zz, double radius) {
            double dx = zx - cx, dz = zz - cz;
            // Into the ellipse's frame: u along the track, v across it.
            double sin = Math.sin(heading), cos = Math.cos(heading);
            double u = dx * sin + dz * cos, v = dx * cos - dz * sin;
            double a = Math.max(1e-6, along), b = Math.max(1e-6, across);
            if ((u * u) / (a * a) + (v * v) / (b * b) <= 1) return true;
            double nearest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 64; i++) {
                double t = i * Math.PI * 2 / 64;
                nearest = Math.min(nearest, Math.hypot(u - a * Math.cos(t), v - b * Math.sin(t)));
            }
            return nearest <= radius;
        }
    }

    public static Ellipse ellipse(V3 vel, double ticksToImpact, boolean boosting) {
        double speed = Math.hypot(vel.x(), vel.z());
        double t = Math.max(0, ticksToImpact);
        double along = boosting ? 60 + 0.25 * speed * t : 8 + 0.02 * speed * t;
        double across = boosting ? along * 0.45 : along * 0.35 + 4;
        double heading = speed < 1e-6 ? 0 : Math.atan2(vel.x(), vel.z());
        return new Ellipse(along, across, heading);
    }

    /** The name a flight is tracked under, the same on every radar: {@code T-} and four digits. */
    public static String designation(UUID flight) {
        long bits = flight.getMostSignificantBits() ^ flight.getLeastSignificantBits();
        return String.format(Locale.ROOT, "T-%04d", Math.floorMod(bits ^ (bits >>> 29), 10000));
    }
}
