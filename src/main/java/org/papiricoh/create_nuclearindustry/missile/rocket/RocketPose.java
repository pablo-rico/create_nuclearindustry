package org.papiricoh.create_nuclearindustry.missile.rocket;

import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

/**
 * Where a rocket contraption's entity has to stand so its blocks sit where the flight says. Create turns a contraption about
 * the middle of its local block 0 and places that block's corner at the entity's position; a flight is the bottom centre of
 * the rocket's tail, local {@code (0.5, 1, 0.5)} (the silo floor is local block 0), turned by
 * {@link RocketAttitude#orientation}. Kept free of game classes so JUnit can check the algebra.
 */
public final class RocketPose {
    /** The point Create rotates a contraption about, in its local frame. */
    public static final V3 CENTRE = new V3(0.5, 0.5, 0.5);
    /** The flight's own point in the contraption's local frame: the bottom centre of the block over the silo floor. */
    public static final V3 PIVOT = new V3(0.5, 1, 0.5);

    private RocketPose() {
    }

    /** The entity position (Create's anchor) that puts {@link #PIVOT} at {@code pivot} turned by {@code attitude}. */
    public static V3 anchor(V3 pivot, RocketAttitude.Quat attitude) {
        return pivot.sub(CENTRE).sub(attitude.rotate(PIVOT.sub(CENTRE)));
    }

    /** Where local point {@code local} is in the world, the way Create draws it from {@code anchor}. */
    public static V3 toWorld(V3 anchor, RocketAttitude.Quat attitude, V3 local) {
        return anchor.add(CENTRE).add(attitude.rotate(local.sub(CENTRE)));
    }

    /** The inverse rotation. */
    public static RocketAttitude.Quat inverse(RocketAttitude.Quat q) {
        return new RocketAttitude.Quat(-q.x(), -q.y(), -q.z(), q.w());
    }

    /**
     * Euler angles, degrees, {x, y, z} with {@code R = Rz(z)·Ry(y)·Rx(x)}: what Create's {@code ContraptionRotationState} turns
     * back into the same rotation (its matrix is the inverse, {@code Rx(-x)·Ry(-y)·Rz(-z)}).
     */
    public static double[] euler(RocketAttitude.Quat q) {
        V3 ex = q.rotate(new V3(1, 0, 0));
        V3 ey = q.rotate(new V3(0, 1, 0));
        V3 ez = q.rotate(new V3(0, 0, 1));
        // Columns of R are the rotated basis vectors: R00 = ex.x, R10 = ex.y, R20 = ex.z, R21 = ey.z, R22 = ez.z.
        double r20 = Math.max(-1, Math.min(1, ex.z()));
        double y = Math.asin(-r20);
        double x, z;
        if (Math.abs(r20) < 0.999999) {
            x = Math.atan2(ey.z(), ez.z());
            z = Math.atan2(ex.y(), ex.x());
        } else {
            // Gimbal lock: fold the roll into the yaw.
            x = 0;
            z = Math.atan2(-ey.x(), ey.y());
        }
        return new double[]{Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z)};
    }
}
