package org.papiricoh.create_nuclearindustry.dyson.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SunPath;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

/**
 * Where one of the swarm's two-axis mounts points: azimuth (degrees clockwise from north) and elevation (degrees above level, past
 * 90 leaning back over the top). Stepped by the time since the last frame, so it slews alike at any frame rate.
 *
 * <p>The sun crosses the zenith at noon, where its azimuth jumps from east to west. Rather than spin the turntable half round
 * there, a mount takes whichever of the two poses that look the same way is nearer, so it tilts on past vertical and follows the
 * sun down the far side. Parked, it only lowers its elevation, back over the top if it has to: the stow sequence at sunset.
 */
final class TurretAim {
    /** A fixed sun for Ponder, whose level has no real time of day: high in the south-east (azimuth 135, elevation 55). */
    static final double[] PONDER_SUN = {0.4056, 0.8192, 0.4056};
    /** Ticks of ease-in near the target. */
    private static final float EASE_TICKS = 5;

    float azimuth = 90, elevation;
    private double time = Double.NaN;

    TurretAim(float park) {
        elevation = park;
    }

    /**
     * Steps to {@code now} (ticks), slewing at most {@code slew} degrees a tick: towards the direction {@code (lx, ly, lz)} (unit,
     * the block's frame) if {@code track}, kept between {@code min} and {@code 180 - min} degrees up, else down to {@code park}.
     */
    void step(double now, float slew, boolean track, double lx, double ly, double lz, float min, float park) {
        double dt = Double.isNaN(time) ? 0 : Mth.clamp(now - time, 0, 20);
        time = now;
        if (dt <= 0) return;
        float move = (float) (slew * dt);
        float targetAz = azimuth, targetEl = park;
        if (track) {
            double ground = Math.sqrt(lx * lx + lz * lz);
            float az = ground < 1.0e-4 ? azimuth : (float) Math.toDegrees(Math.atan2(lx, -lz));
            float el = Mth.clamp((float) Math.toDegrees(Math.atan2(ly, ground)), min, 90);
            float flipped = az + 180;
            if (Math.abs(Mth.wrapDegrees(flipped - azimuth)) < Math.abs(Mth.wrapDegrees(az - azimuth))) {
                targetAz = flipped;
                targetEl = 180 - el;
            } else {
                targetAz = az;
                targetEl = el;
            }
        }
        azimuth = Mth.wrapDegrees(azimuth + ease(Mth.wrapDegrees(targetAz - azimuth), move, dt));
        elevation = Mth.clamp(elevation + ease(targetEl - elevation, move, dt), min, 180 - min);
    }

    private static float ease(float delta, float move, double dt) {
        return Mth.clamp((float) (delta * (1 - Math.exp(-dt / EASE_TICKS))), -move, move);
    }

    /** Unit direction the upper part looks along, in the block's frame, into {@code out}. */
    void sight(double[] out) {
        double az = Math.toRadians(azimuth), el = Math.toRadians(elevation);
        double c = Math.cos(el);
        out[0] = c * Math.sin(az);
        out[1] = Math.sin(el);
        out[2] = -c * Math.cos(az);
    }

    /** Turns {@code ms} as the mount is turned, about the vertical through {@code axis}. */
    void turn(PoseStack ms, float[] axis) {
        ms.translate(axis[0], axis[1], axis[2]);
        ms.mulPose(Axis.YP.rotationDegrees(-azimuth));
        ms.translate(-axis[0], -axis[1], -axis[2]);
    }

    /** Then raises it as the upper part is raised, about its pivot. */
    void raise(PoseStack ms, float[] pivot) {
        ms.translate(pivot[0], pivot[1], pivot[2]);
        ms.mulPose(Axis.XP.rotationDegrees(elevation));
        ms.translate(-pivot[0], -pivot[1], -pivot[2]);
    }

    /**
     * The sun's direction at {@code partial} in the frame {@code pos}'s blocks are laid out in, into {@code out}: the sky's own,
     * unless the block rides a ship.
     */
    static void localSun(Level level, BlockPos pos, float partial, double[] out) {
        ClientSun.direction(level, partial, out);
        if (!ShipSpace.onShip(level, pos)) return;
        Vec3 centre = ShipSpace.toWorld(level, pos);
        Vec3 far = ShipSpace.localView(level, pos, centre.add(out[0] * 256, out[1] * 256, out[2] * 256));
        Vec3 d = far.subtract(Vec3.atCenterOf(pos)).normalize();
        out[0] = d.x;
        out[1] = d.y;
        out[2] = d.z;
    }
}
