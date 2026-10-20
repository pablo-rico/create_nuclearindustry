package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import org.papiricoh.create_nuclearindustry.missile.physics.V3;

/**
 * The battery turret's angles, kept apart from the game so aiming can be tested. Yaw is degrees clockwise from north seen from
 * above (east 90), pitch degrees above the horizon, limited to what the canister can be raised to; at rest the canister points
 * north (-z), as its partials are modelled.
 */
public final class TurretAim {
    public static final float MIN_PITCH = 20, MAX_PITCH = 85;

    private TurretAim() {}

    public record Angles(float yaw, float pitch) {}

    /** The angles that point the canister along {@code direction}, its pitch limited to the canister's travel. */
    public static Angles fromDirection(V3 direction) {
        double horizontal = Math.hypot(direction.x(), direction.z());
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x(), -direction.z()));
        float pitch = (float) Math.toDegrees(Math.atan2(direction.y(), horizontal));
        return new Angles(wrap(yaw), Math.clamp(pitch, MIN_PITCH, MAX_PITCH));
    }

    /** The unit vector the canister points along at {@code yaw} and {@code pitch}. */
    public static V3 direction(float yaw, float pitch) {
        return rotate(new V3(0, 0, -1), yaw, pitch);
    }

    /**
     * Where {@code local}, a point in the canister's frame at rest (x east, y up, z south, from its pivot), lies once the canister
     * is raised by {@code pitch} and turned by {@code yaw}, still from its pivot.
     */
    public static V3 rotate(V3 local, float yaw, float pitch) {
        double p = Math.toRadians(pitch), y = Math.toRadians(yaw);
        // Raising the muzzle turns -z towards +y about the east-west axis.
        double raisedY = local.y() * Math.cos(p) - local.z() * Math.sin(p);
        double raisedZ = local.y() * Math.sin(p) + local.z() * Math.cos(p);
        // Clockwise from above takes north to east.
        return new V3(local.x() * Math.cos(y) - raisedZ * Math.sin(y), raisedY, local.x() * Math.sin(y) + raisedZ * Math.cos(y));
    }

    /** {@code current} turned towards {@code target} by at most {@code amount} degrees, the short way round. */
    public static float yawStep(float current, float target, float amount) {
        float delta = wrap(target - current);
        return wrap(current + Math.clamp(delta, -amount, amount));
    }

    public static float pitchStep(float current, float target, float amount) {
        return Math.clamp(current + Math.clamp(target - current, -amount, amount), MIN_PITCH, MAX_PITCH);
    }

    /** Degrees the turret still has to turn to reach {@code target}: the larger of its two axes. */
    public static float error(float yaw, float pitch, Angles target) {
        return Math.max(Math.abs(wrap(target.yaw - yaw)), Math.abs(target.pitch - pitch));
    }

    public static boolean aligned(float yaw, float pitch, Angles target, float tolerance) {
        return error(yaw, pitch, target) <= tolerance;
    }

    public static float wrap(float degrees) {
        float result = degrees % 360;
        return result <= -180 ? result + 360 : result > 180 ? result - 360 : result;
    }
}
