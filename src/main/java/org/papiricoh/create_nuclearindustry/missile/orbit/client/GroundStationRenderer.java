package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.orbit.GroundStationBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a ground station: the half shaft coming up into its plinth, and its mount and dish pointed at the nearest satellite of its
 * network that is over its horizon. With none in view it sweeps the sky slowly, dish half raised. The dish is driven by the shaft:
 * it slews at a rate that grows with the speed and stays where it is while the station stands still; a station never yet turned is
 * parked, dish looking straight up.
 *
 * <p>The aim is kept here, per station, and stepped by the game time that passed since the last frame, so it slews as smoothly at
 * any frame rate. Directions are taken in the frame the station's blocks are laid out in, so a station on a ship aims right too.
 */
public class GroundStationRenderer extends KineticBlockEntityRenderer<GroundStationBlockEntity> {
    /** The parked pose, and the elevation it sweeps at with nothing in view, degrees. */
    private static final float PARK_ELEVATION = 90, SCAN_ELEVATION = 35;
    /** The lowest it points: below this the dish's rim would touch the turntable. */
    private static final float MIN_ELEVATION = 10;
    /** Slew rate, degrees per tick, at 1 rpm and at most; and how much slower it sweeps than it slews. */
    private static final float SLEW_PER_RPM = 0.05f, MAX_SLEW = 6, SCAN_FRACTION = 0.15f;
    /** Ticks of ease-in: within a few degrees of its target it slows down instead of stopping dead. */
    private static final float EASE_TICKS = 4;

    private static final Map<GroundStationBlockEntity, Aim> AIMS = new WeakHashMap<>();

    public GroundStationRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /** Where a station points: azimuth (degrees clockwise from north) and elevation (degrees above level). */
    private static final class Aim {
        float azimuth, elevation = PARK_ELEVATION;
        double time = Double.NaN;
    }

    @Override
    protected void renderSafe(GroundStationBlockEntity station, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = station.getLevel();
        if (level == null) return;
        BlockState state = station.getBlockState();
        standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN), station, light)
                .renderInto(pose, buffers.getBuffer(RenderType.solid()));

        Aim aim = aim(station, level, level.getGameTime() + partial);
        float[] axis = OrbitPartials.STATION_AXIS, pivot = OrbitPartials.DISH_PIVOT;
        CachedBuffers.partial(OrbitPartials.STATION_MOUNT, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
        CachedBuffers.partial(OrbitPartials.STATION_DISH, state)
                .translate(axis[0], axis[1], axis[2]).rotateYDegrees(-aim.azimuth).translate(-axis[0], -axis[1], -axis[2])
                .translate(pivot[0], pivot[1], pivot[2]).rotateXDegrees(aim.elevation).translate(-pivot[0], -pivot[1], -pivot[2])
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
    }

    /** Steps the station's aim to {@code time} (game ticks) and returns it. */
    private static Aim aim(GroundStationBlockEntity station, Level level, double time) {
        Aim aim = AIMS.computeIfAbsent(station, s -> new Aim());
        double dt = Double.isNaN(aim.time) ? 0 : Mth.clamp(time - aim.time, 0, 20);
        aim.time = time;
        float rpm = Math.abs(station.getSpeed());
        if (rpm == 0 || dt <= 0) return aim;
        float slew = (float) (Math.min(MAX_SLEW, rpm * SLEW_PER_RPM) * dt);
        float[] target = target(station, level, time);
        if (target == null) {
            aim.azimuth = Mth.wrapDegrees(aim.azimuth + slew * SCAN_FRACTION);
            aim.elevation = approach(aim.elevation, SCAN_ELEVATION, slew, dt);
        } else {
            aim.azimuth = Mth.wrapDegrees(aim.azimuth + step(Mth.wrapDegrees(target[0] - aim.azimuth), slew, dt));
            aim.elevation = approach(aim.elevation, target[1], slew, dt);
        }
        return aim;
    }

    private static float approach(float from, float to, float slew, double dt) {
        return from + step(to - from, slew, dt);
    }

    /** How far to move towards a target {@code delta} away: eased when close, never more than {@code slew}. */
    private static float step(float delta, float slew, double dt) {
        float eased = (float) (delta * (1 - Math.exp(-dt / EASE_TICKS)));
        return Mth.clamp(eased, -slew, slew);
    }

    /**
     * Azimuth and elevation of the nearest satellite on the station's code that sees it at {@code time}, or null for none. The
     * elevation drops by the world's curvature ({@code MissileConfig.radarEarthRadius}), so it reaches the horizon where the
     * satellite's footprint ends.
     */
    private static float @Nullable [] target(GroundStationBlockEntity station, Level level, double time) {
        String code = station.code();
        if (code.isEmpty()) return null;
        BlockPos pos = station.getBlockPos();
        Vec3 world = ShipSpace.toWorld(level, pos);
        double radius = MissileConfig.radarEarthRadius;
        Satellite best = null;
        V3 bestAt = null;
        double bestDistance = Double.MAX_VALUE;
        for (Satellite satellite : ClientSatellites.all()) {
            if (!code.equals(satellite.code()) || !satellite.orbit().covers(world.x, world.z, time, radius)) continue;
            V3 at = satellite.orbit().position(time);
            double d = sq(at.x() - world.x) + sq(at.y() - world.y) + sq(at.z() - world.z);
            if (d < bestDistance) {
                best = satellite;
                bestAt = at;
                bestDistance = d;
            }
        }
        if (best == null) return null;
        // Seen from where the station's blocks are laid out: on a ship, its plot.
        Vec3 local = ShipSpace.localView(level, pos, new Vec3(bestAt.x(), bestAt.y(), bestAt.z())).subtract(Vec3.atCenterOf(pos).add(0, 0.5, 0));
        double ground = Math.sqrt(local.x * local.x + local.z * local.z);
        double drop = radius > 0 ? ground * ground / (2 * radius) : 0;
        float azimuth = (float) Math.toDegrees(Math.atan2(local.x, -local.z));
        float elevation = (float) Math.toDegrees(Math.atan2(local.y - drop, ground));
        return new float[]{azimuth, Mth.clamp(elevation, MIN_ELEVATION, 90)};
    }

    private static double sq(double v) {
        return v * v;
    }

    /** The dish swings up to a block and three quarters high and a quarter block past the plinth's sides. */
    @Override
    public AABB getRenderBoundingBox(GroundStationBlockEntity station) {
        return new AABB(station.getBlockPos()).inflate(0.3, 0, 0.3).expandTowards(0, 0.85, 0);
    }
}
