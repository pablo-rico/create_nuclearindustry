package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * Which way a rocket points: its axis (local +Y) along its velocity, turned about that axis so its local +Z faces the way it
 * travels over the ground. Shared by the puppet entity, the debris and the client effects so a nozzle drawn by one sits where
 * the other draws the plume. While it climbs straight up its heading is the one it was launched with.
 */
public final class RocketAttitude {

    /** A unit rotation quaternion. */
    public record Quat(double x, double y, double z, double w) {
        public static final Quat IDENTITY = new Quat(0, 0, 0, 1);

        /** {@code this} applied after {@code other}. */
        public Quat mul(Quat o) {
            return new Quat(
                    w * o.x + x * o.w + y * o.z - z * o.y,
                    w * o.y - x * o.z + y * o.w + z * o.x,
                    w * o.z + x * o.y - y * o.x + z * o.w,
                    w * o.w - x * o.x - y * o.y - z * o.z);
        }

        public V3 rotate(V3 v) {
            // v + 2w (q × v) + 2 q × (q × v)
            double tx = 2 * (y * v.z() - z * v.y());
            double ty = 2 * (z * v.x() - x * v.z());
            double tz = 2 * (x * v.y() - y * v.x());
            return new V3(v.x() + w * tx + (y * tz - z * ty), v.y() + w * ty + (z * tx - x * tz), v.z() + w * tz + (x * ty - y * tx));
        }

        static Quat axisAngle(double ax, double ay, double az, double angle) {
            double s = Math.sin(angle / 2);
            return new Quat(ax * s, ay * s, az * s, Math.cos(angle / 2));
        }
    }

    /** Below this share of its speed going sideways a rocket counts as vertical and keeps its launch heading. */
    static final double VERTICAL = 1.0e-4;

    private RocketAttitude() {
    }

    /** The heading, radians, of travel along {@code (dx, dz)}: 0 is +Z, π/2 is +X. */
    public static double heading(double dx, double dz) {
        return Math.atan2(dx, dz);
    }

    /** The heading of {@code velocity} over the ground, or {@code fallback} while it is (nearly) vertical. */
    public static double heading(V3 velocity, double fallback) {
        double horizontal = velocity.horizontalLength();
        return horizontal <= VERTICAL * Math.max(velocity.length(), 1.0e-9) ? fallback : heading(velocity.x(), velocity.z());
    }

    /** Radians the axis leans from straight up: 0 climbing, π/2 level, π diving straight down. */
    public static double tilt(V3 velocity) {
        double speed = velocity.length();
        return speed < 1.0e-9 ? 0 : Math.acos(Math.max(-1, Math.min(1, velocity.y() / speed)));
    }

    /** The rotation taking the rocket's local frame to the world for a rocket moving at {@code velocity}. */
    public static Quat orientation(V3 velocity, double fallbackHeading) {
        double heading = heading(velocity, fallbackHeading);
        Quat yaw = Quat.axisAngle(0, 1, 0, heading);
        double tilt = tilt(velocity);
        if (tilt < 1.0e-9) {
            return yaw;
        }
        // Lean +Y towards the heading: about the horizontal axis UP × heading.
        Quat lean = Quat.axisAngle(Math.cos(heading), 0, -Math.sin(heading), tilt);
        return lean.mul(yaw);
    }

    /** Where a point fixed to the rocket at {@code local} (from its pivot, the tail's bottom centre) is in the world. */
    public static V3 toWorld(V3 pivot, Quat attitude, V3 local) {
        return pivot.add(attitude.rotate(local));
    }
}
