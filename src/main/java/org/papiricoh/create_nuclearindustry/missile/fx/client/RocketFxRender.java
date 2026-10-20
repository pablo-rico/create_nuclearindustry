package org.papiricoh.create_nuclearindustry.missile.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketFxSource;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlight;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;
import org.papiricoh.create_nuclearindustry.missile.fx.FxCurves;
import org.papiricoh.create_nuclearindustry.missile.fx.PlumeModel;
import org.papiricoh.create_nuclearindustry.missile.fx.TrailModel;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.Iterator;

/**
 * Draws the rocket effects once a frame, after the weather: plumes and their shock diamonds, the glow of a rocket seen from
 * afar, its outline where no puppet stands in for it, the smoke trails, the plasma of a warhead coming back in, and the
 * flashes and dust of launches, separations and impacts. Smoke is blended far to near; light is added on top. What lies past
 * the far plane is pulled in to it ({@link FxQuads}), so a trail and a glow show kilometres off.
 */
final class RocketFxRender {
    private static final FxQuads SMOKE = new FxQuads();
    private static final FxQuads GLOW = new FxQuads();

    // Plume colours, RGB: solid motors burn orange, liquid engines blue-white.
    private static final int[] SOLID = {0xFF6A1E, 0xFFAA4C, 0xFFF0C8, 0xFFE2A8, 0xFFB868, 0xFF8A2A};
    private static final int[] LIQUID = {0x5C78FF, 0xA8BEFF, 0xF2F6FF, 0xFFC8E4, 0xD8E2FF, 0x9DB6FF};
    private static final int OUTER = 0, MID = 1, CORE = 2, DIAMOND = 3, FLARE = 4, PAINT = 5;

    private RocketFxRender() {
    }

    /** The light the effects see this frame: day or night, the low sun on high smoke, and the haze. */
    record Sky(float day, float twilight, float fogR, float fogG, float fogB) {
        static Sky of(ClientLevel level, float partial) {
            float day = Math.max(0, Math.min(1, (level.getSkyDarken(partial) - 0.2f) / 0.8f));
            double sun = Math.cos(level.getTimeOfDay(partial) * Math.PI * 2);
            // Strongest with the sun just under the horizon, fading by full night or day.
            float twilight = (float) Math.max(0, 1 - Math.abs(sun + 0.08) / 0.3);
            float[] fog = RenderSystem.getShaderFogColor();
            return new Sky(day, twilight, fog[0], fog[1], fog[2]);
        }
    }

    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || !ClientConfig.rocketEffects) return;
        RocketFxManager fx = RocketFxManager.INSTANCE;
        OrbitFx orbit = OrbitFx.INSTANCE;
        if (fx.flights.isEmpty() && fx.bursts.isEmpty() && !fx.debrisNear && orbit.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        // Well inside the far plane, still beyond all vanilla terrain.
        double limit = mc.gameRenderer.getDepthFar() * 0.9;
        boolean shaded = RocketRenderTypes.shaded();
        SMOKE.begin(eye, limit, shaded, camera.getLeftVector(), camera.getUpVector());
        GLOW.begin(eye, limit, shaded, camera.getLeftVector(), camera.getUpVector());
        Sky sky = Sky.of(level, partial);
        long now = level.getGameTime();
        double time = now + partial;
        // The fairing halves and satellites as models first: the light and smoke drawn over them test against their depth.
        orbit.renderModels(eye, partial, now, limit);

        for (FlightFx flight : fx.flights.values()) {
            drawFlight(flight, partial, time, now, eye, sky);
        }
        if (fx.debrisNear) {
            for (Entity entity : level.entitiesForRendering()) {
                if (entity instanceof RocketFxSource source) drawDebris(source, partial, time, eye);
            }
        }
        for (Burst burst : fx.bursts) {
            drawBurst(burst, now, partial, eye, sky);
        }
        orbit.draw(GLOW, SMOKE, partial, now, eye, sky);
        SMOKE.sortFarToNear();
        SMOKE.draw(RocketRenderTypes.smoke(shaded));
        GLOW.draw(RocketRenderTypes.glow(shaded));
    }

    // ------------------------------------------------------------------ one flight

    private static void drawFlight(FlightFx fx, float partial, double time, long now, Vec3 eye, Sky sky) {
        ClientFlight flight = fx.flight;
        Vec3 pivot = flight != null ? flight.position(partial) : fx.position;
        Quaternionf attitude = flight != null ? flight.attitude(partial) : fx.attitude;
        Vec3 axis = RocketFxManager.rotate(attitude, 0, 1, 0);
        double distance = eye.distanceTo(pivot);
        FxBudget.Lod lod = FxBudget.lod(distance);
        if (lod == FxBudget.Lod.FAR && !ClientConfig.rocketFarTrails) return;

        Vec3 head = null;
        if (!fx.ended() && fx.plume > 0.05f) {
            Vec3 nozzle = flight != null ? centre(flight, fx, partial) : RocketFxManager.nozzleCentre(fx);
            PlumeModel.Plume plume = RocketFxManager.plumeOf(fx, nozzle.y, 1);
            double clip = RocketFxManager.clipLength(fx, nozzle, axis);
            head = nozzle.subtract(axis.scale(Math.min(plume.length() * 0.55, Math.max(0.5, clip))));
        }
        if (fx.trailed) trail(fx, head, time, eye, sky);
        if (!fx.ended()) {
            if (fx.plume > 0.01f) plumes(fx, flight, pivot, axis, partial, time, distance, lod, sky);
            if (!fx.has(org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState.PUPPET)) {
                impostor(fx, pivot, axis, distance, sky);
            }
            sheath(fx, pivot, axis, distance, 1, sky);
            if (fx.has(org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState.SATELLITE) && lod != FxBudget.Lod.NEAR) {
                climbGlint(fx, pivot, axis, distance, sky, time);
            }
        } else if (fx.nuclearEnd && now - fx.endedTick < 10) {
            // The detonation takes over: the warhead's glow only goes out.
            sheath(fx, pivot, axis, distance, 1 - (now - fx.endedTick + partial) / 10.0, sky);
        }
        streak(fx, pivot, axis, now, partial);
    }

    private static Vec3 centre(ClientFlight flight, FlightFx fx, float partial) {
        RocketShape.Stage stage = fx.shape.stage(fx.stage);
        if (stage == null || stage.nozzles().isEmpty()) return flight.position(partial);
        Vec3 sum = Vec3.ZERO;
        for (int i = 0; i < stage.nozzles().size(); i++) sum = sum.add(flight.nozzleWorld(fx.stage, i, partial));
        return sum.scale(1.0 / stage.nozzles().size());
    }

    // ------------------------------------------------------------------ plumes

    private static void plumes(FlightFx fx, ClientFlight flight, Vec3 pivot, Vec3 axis, float partial, double time, double distance,
                               FxBudget.Lod lod, Sky sky) {
        RocketShape.Stage stage = fx.shape.stage(fx.stage);
        if (stage == null) return;
        boolean liquid = stage.propellant() == RocketShape.Propellant.LIQUID;
        Vec3 dir = axis.scale(-1);
        // A dying engine sputters.
        double level = fx.plume * (fx.burnoutTick >= 0 || !fx.burning() ? 0.6 + 0.4 * Math.abs(Math.sin(time * 1.7)) : 1);
        for (int i = 0; i < stage.nozzles().size(); i++) {
            Vec3 nozzle = flight != null ? flight.nozzleWorld(fx.stage, i, partial)
                    : pivot.add(RocketFxManager.rotate(fx.attitude, stage.nozzles().get(i).x(), stage.nozzles().get(i).y(), stage.nozzles().get(i).z()));
            PlumeModel.Plume plume = RocketFxManager.plumeOf(fx, nozzle.y, 1);
            double clip = RocketFxManager.clipLength(fx, nozzle, axis);
            plume(nozzle, dir, plume, liquid, level, time, fx.seed + i * 31, lod, clip, distance, sky, !liquid);
        }
    }

    /**
     * One nozzle's flame: an outer envelope, a brighter body and a white-hot core, the train of shock diamonds near the
     * ground, a glow at the nozzle and a flare that keeps it a bright point from afar. A flame that meets the ground or the
     * silo's floor splashes there.
     */
    static void plume(Vec3 nozzle, Vec3 dir, PlumeModel.Plume p, boolean liquid, double level, double time, int seed, FxBudget.Lod lod,
                      double clip, double distance, Sky sky, boolean smoky) {
        int[] c = liquid ? LIQUID : SOLID;
        double flick = PlumeModel.flicker(time, seed) * level;
        double length = Math.min(p.length() * (0.92 + 0.08 * PlumeModel.flicker(time * 1.3, seed + 7)), Math.max(0.3, clip));
        double haze = haze(distance);
        double fade = 1 - 0.55 * haze;
        if (lod != FxBudget.Lod.FAR) {
            double[] ts = {0, 0.1, 0.3, 0.6, 1};
            // The envelope.
            if (sky != null && sky.day() > 0.05) {
                // Light added onto a bright sky only whitens it: in daylight the flame's body is painted on as well.
                cone(SMOKE, nozzle, dir, length * 0.7, ts, p.exitRadius() * 1.05, p.endRadius() * 0.7, c[PAINT],
                        (liquid ? 0.3 : 0.6) * flick * sky.day() * fade, 1.4);
            }
            cone(GLOW, nozzle, dir, length, ts, p.exitRadius() * 1.15, p.endRadius(), c[OUTER], (liquid ? 0.22 : 0.42) * flick * fade, 1.3);
            if (lod == FxBudget.Lod.NEAR && ClientConfig.rocketDetail >= 0.5) {
                cone(GLOW, nozzle, dir, length * 0.62, ts, p.exitRadius() * 0.95, p.endRadius() * 0.55, c[MID], 0.55 * flick * fade, 1.6);
            }
            double core = Math.min(p.coreLength(), length);
            cone(GLOW, nozzle, dir, core, new double[]{0, 0.25, 1}, p.coreRadius(), p.coreRadius() * 0.35, c[CORE], 0.95 * flick, 1.0);
            if (lod == FxBudget.Lod.NEAR && p.diamonds() > 0) {
                for (int k = 1; k <= p.diamonds(); k++) {
                    double along = p.diamondSpacing() * k * (1 + 0.06 * k);
                    if (along > length * 0.9) break;
                    Vec3 at = nozzle.add(dir.scale(along));
                    double a = p.diamondStrength() * flick * (1 - (k - 1.0) / (p.diamonds() + 1)) * 0.95;
                    double across = p.coreRadius() * (0.75 - 0.07 * k);
                    GLOW.ellipse(at.x, at.y, at.z, p.coreRadius() * 1.1, Math.max(0.05, across), dir, argb(a, c[DIAMOND]));
                }
            }
            // Seen end on, a strip shows nothing: fill the flame in with discs along it.
            Vec3 toEye = GLOW.eye().subtract(nozzle).normalize();
            double endOn = Math.abs(dir.dot(toEye));
            if (endOn > 0.5) {
                double w = (endOn - 0.5) * 2;
                for (double t : new double[]{0.12, 0.4, 0.75}) {
                    Vec3 at = nozzle.add(dir.scale(length * t));
                    double r = p.exitRadius() + (p.endRadius() - p.exitRadius()) * Math.sqrt(t);
                    GLOW.disc(at.x, at.y, at.z, r * 1.3, argb(0.35 * w * flick * (1 - t), c[t < 0.3 ? MID : OUTER]), seed + t);
                }
            }
            if (clip < p.length()) {
                // Splashing over the ground or the shaft's floor.
                Vec3 at = nozzle.add(dir.scale(clip));
                GLOW.disc(at.x, at.y + 0.2, at.z, p.endRadius() * 2.4, argb(0.5 * flick, c[MID]), seed);
            }
            if (smoky && lod == FxBudget.Lod.NEAR) {
                // Thick smoke boiling off the end of a solid motor's flame.
                Vec3 from = nozzle.add(dir.scale(length * 0.55));
                Vec3 to = nozzle.add(dir.scale(Math.min(clip, length * 1.7)));
                if (clip > length * 0.6) {
                    int grey = lit(0xD6D3CE, sky, 0.9);
                    SMOKE.strip(from.x, from.y, from.z, to.x, to.y, to.z, p.endRadius() * 0.9, p.endRadius() * 1.9,
                            argb(0.0, grey), argb(0.55 * p.smoke() * level, grey), (float) (time * 0.0), (float) (length * 0.4));
                }
            }
        }
        // The nozzle's glow and the flare that keeps it a point of light far away.
        Vec3 at = nozzle.add(dir.scale(Math.min(p.coreLength() * 0.4, Math.max(0.1, clip))));
        double near = lod == FxBudget.Lod.NEAR ? 1 : 0;
        // Near, a modest glow that leaves the rocket in sight; far, a point of light that never shrinks under a few pixels.
        // Against the dark a burning engine stands out far more than against a bright sky.
        double night = sky == null ? 0 : 1 - sky.day();
        GLOW.disc(at.x, at.y, at.z, Math.max(p.exitRadius() * (near > 0 ? 2.2 : 3.2), distance * 0.0065),
                argb((0.85 - 0.25 * near) * flick * fade, c[FLARE]), seed);
        GLOW.disc(at.x, at.y, at.z, Math.max(p.exitRadius() * 8, distance * 0.026),
                argb((0.2 - 0.12 * near + 0.3 * night * (1 - near)) * flick * fade, c[FLARE]), seed + 1);
    }

    /**
     * A flame cone along {@code dir} from {@code base}: strips between the fractions {@code ts} of its length, opening from
     * {@code r0} to {@code r1} and fading out along it (by {@code power}).
     */
    private static void cone(FxQuads quads, Vec3 base, Vec3 dir, double length, double[] ts, double r0, double r1, int rgb, double alpha,
                             double power) {
        if (length <= 0.05 || alpha <= 0.003) return;
        for (int i = 0; i + 1 < ts.length; i++) {
            double t0 = ts[i], t1 = ts[i + 1];
            Vec3 a = base.add(dir.scale(length * t0)), b = base.add(dir.scale(length * t1));
            double ra = r0 + (r1 - r0) * Math.pow(t0, 0.6), rb = r0 + (r1 - r0) * Math.pow(t1, 0.6);
            // Rises over the first tenth so its root is not a hard line at the nozzle.
            double aa = alpha * Math.pow(1 - t0, power) * (t0 < 0.1 ? 0.7 + 3 * t0 : 1);
            double ab = alpha * Math.pow(1 - t1, power);
            quads.strip(a.x, a.y, a.z, b.x, b.y, b.z, ra, rb, argb(aa, rgb), argb(ab, rgb), (float) (length * t0), (float) (length * t1));
        }
    }

    // ------------------------------------------------------------------ the rocket from afar

    /**
     * The rocket's outline where no puppet entity draws it (beyond the distance entities are shown at): its body stage by stage
     * and its nose, never thinner than a pixel or two so it does not flicker out.
     */
    private static void impostor(FlightFx fx, Vec3 pivot, Vec3 axis, double distance, Sky sky) {
        RocketShape shape = fx.shape;
        int bottom = shape.attachedBottom(fx.stage, fx.has(org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState.BURNED_OUT));
        double radius = Math.max(shape.radius(), distance * 0.0016);
        double light = 0.3 + 0.7 * sky.day();
        for (int s = 0; s < shape.stages().size(); s++) {
            RocketShape.Stage stage = shape.stages().get(s);
            int from = Math.max(bottom, stage.bottom());
            int to = Math.min(stage.top() + 1, shape.payloadBottom());
            if (to <= from) continue;
            body(pivot, axis, from, to, radius, s % 2 == 0 ? 0xE6E6E2 : 0xC4C7CC, light, distance);
        }
        int payloadFrom = Math.max(bottom, shape.payloadBottom());
        int noseFrom = Math.max(payloadFrom, shape.height() - 1);
        if (noseFrom > payloadFrom) body(pivot, axis, payloadFrom, noseFrom, radius, 0x9EA3A8, light, distance);
        // The nose cone.
        Vec3 a = pivot.add(axis.scale(noseFrom)), b = pivot.add(axis.scale(shape.height() + 0.4));
        int colour = argb(1, shade(0x7A7F86, light));
        SMOKE.solidStrip(a.x, a.y, a.z, b.x, b.y, b.z, radius, Math.max(0.04, radius * 0.12), colour, colour);
    }

    private static void body(Vec3 pivot, Vec3 axis, double from, double to, double radius, int rgb, double light, double distance) {
        Vec3 a = pivot.add(axis.scale(from)), b = pivot.add(axis.scale(to));
        int colour = argb(1, shade(rgb, light));
        SMOKE.solidStrip(a.x, a.y, a.z, b.x, b.y, b.z, radius, radius, colour, colour);
    }

    /**
     * A launch vehicle high up catches the sun: a steady point of light on it, so it can be followed from the pad through the
     * long coast to orbit with its engines off, brighter the darker the sky.
     */
    private static void climbGlint(FlightFx fx, Vec3 pivot, Vec3 axis, double distance, Sky sky, double time) {
        Vec3 at = pivot.add(axis.scale(fx.shape.height() * 0.7));
        double high = FxCurves.smoothstep(150, 500, at.y - fx.launchY);
        double a = high * (0.3 + 0.5 * (1 - sky.day()) + 0.3 * sky.twilight()) * (fx.plume > 0.3f ? 0.4 : 1);
        if (a <= 0.01) return;
        double twinkle = 0.9 + 0.1 * Math.sin(time * 0.37 + fx.seed);
        GLOW.disc(at.x, at.y, at.z, Math.max(fx.shape.radius() * 1.5, distance * 0.0035), argb(a * twinkle, 0xF2F5FF), fx.seed + 5);
        GLOW.disc(at.x, at.y, at.z, Math.max(fx.shape.radius() * 4, distance * 0.011), argb(0.18 * a, 0xD8E4FF), fx.seed + 6);
    }

    // ------------------------------------------------------------------ re-entry

    /**
     * The air glowing round a warhead coming back in: a cap of plasma on its nose, brighter and whiter the faster and the
     * deeper it goes, and a short wake of glowing gas behind it.
     */
    private static void sheath(FlightFx fx, Vec3 pivot, Vec3 axis, double distance, double fade, Sky sky) {
        if (!fx.descending() || fade <= 0) return;
        double speed = fx.velocity.length();
        double heat = FxCurves.reentryHeat(speed, FxCurves.airDepth(pivot.y, MissileConfig.atmosphereTop)) * fade;
        if (heat < 0.01) return;
        Vec3 vdir = speed > 1.0e-6 ? fx.velocity.scale(1 / speed) : axis;
        double r = fx.shape.radius();
        Vec3 nose = pivot.add(axis.scale(fx.shape.height()));
        int hot = mix(0xFF6A28, 0xFFE8C8, heat);
        GLOW.ellipse(nose.x + vdir.x * 0.2, nose.y + vdir.y * 0.2, nose.z + vdir.z * 0.2, r * (1.4 + heat), r * (2.2 + 2.5 * heat), vdir,
                argb(0.9 * heat, hot));
        Vec3 end = nose.subtract(vdir.scale(3 + speed * 3.5 * heat));
        if (sky.day() > 0.05) {
            // Against a bright sky the glowing wake is painted on too, or it would only whiten the blue.
            SMOKE.strip(nose.x, nose.y, nose.z, end.x, end.y, end.z, r * (1.3 + heat), r * 0.3, argb(0.6 * heat * sky.day(), 0xFF7A30),
                    argb(0, 0xFF5A20), 0, (float) (speed * 4));
        }
        GLOW.strip(nose.x, nose.y, nose.z, end.x, end.y, end.z, r * (1.6 + heat), r * 0.4, argb(0.75 * heat, 0xFF7A38), argb(0, 0xFF4A20), 0,
                (float) (speed * 4));
        GLOW.disc(nose.x, nose.y, nose.z, Math.max(r * 3, distance * 0.004), argb(0.7 * Math.sqrt(heat), hot), fx.seed);
        GLOW.disc(nose.x, nose.y, nose.z, Math.max(r * 8, distance * 0.012), argb(0.18 * heat, 0xFF8A40), fx.seed + 3);
    }

    /** The glowing path a re-entering warhead leaves for a second. */
    private static void streak(FlightFx fx, Vec3 pivot, Vec3 axis, long now, float partial) {
        if (fx.streak.size() < 2) return;
        double heat = FxCurves.reentryHeat(fx.velocity.length(), FxCurves.airDepth(pivot.y, MissileConfig.atmosphereTop));
        if (fx.ended()) heat *= Math.max(0, 1 - (now - fx.endedTick + partial) / 20.0);
        if (heat < 0.01) return;
        double r = fx.shape.radius();
        int n = fx.streak.size();
        Iterator<Vec3> it = fx.streak.iterator();
        Vec3 previous = it.next();
        int i = 0;
        while (it.hasNext()) {
            Vec3 next = it.next();
            double t0 = (double) i / n, t1 = (double) (i + 1) / n;
            GLOW.strip(previous.x, previous.y, previous.z, next.x, next.y, next.z, r * (2.2 - 1.4 * t0), r * (2.2 - 1.4 * t1),
                    argb(0.55 * heat * t0 * t0, 0xFF5A24), argb(0.55 * heat * t1 * t1, 0xFF7A3A), i * 3f, (i + 1) * 3f);
            previous = next;
            i++;
        }
    }

    // ------------------------------------------------------------------ the trail

    /**
     * The smoke trail: a ribbon through the points the rocket left, each spread out and drifted with the wind by its age, up
     * to the plume's tail where it is still being made ({@code head}, or null once the engines are off). Lit by the sun, dark
     * at night but for the stretch the flame still lights, and catching the sunset high up after it has gone dark below.
     */
    private static void trail(FlightFx fx, Vec3 head, double time, Vec3 eye, Sky sky) {
        TrailModel trail = fx.trail;
        int n = trail.size();
        if (n == 0) return;
        double life = fx.trailTicks();
        double dayTicks = Minecraft.getInstance().level.getDayTime();
        boolean liquid = RocketFxManager.liquid(fx);
        int base = liquid ? 0xF2F4F8 : 0xDCD9D3;
        double px = 0, py = 0, pz = 0, pw = 0, pa = 0;
        int pc = 0;
        float pv = 0;
        for (int i = 0; i <= n; i++) {
            double x, y, z, w, a;
            float v;
            int colour;
            if (i < n) {
                TrailModel.Point point = trail.get(i);
                double age = time - point.tick();
                V3 wind = TrailModel.wind(point.y(), dayTicks);
                V3 at = TrailModel.drifted(point, age, wind);
                x = at.x();
                y = at.y();
                z = at.z();
                double seen = eye.distanceTo(new Vec3(x, y, z));
                // Never thinner than a couple of pixels, so a trail kilometres off still shows as a line.
                w = Math.max(TrailModel.width(age, point.width()), seen * 0.0028);
                // Fresh smoke near the rocket is the particles' job; the ribbon thickens in behind them.
                double young = fx.lod == FxBudget.Lod.FAR ? 1 : FxCurves.smoothstep(0, 30, age);
                a = Math.min(0.9, point.density() * TrailModel.alpha(age, life) * (0.5 + 0.4 * young) * (seen > 400 ? 1.25 : 1));
                v = point.tick() * 0.07f;
                colour = smokeColour(base, y, age, fx.plume, sky, seen);
            } else {
                if (head == null) break;
                x = head.x;
                y = head.y;
                z = head.z;
                TrailModel.Point newest = trail.newest();
                w = newest.width();
                a = fx.lod == FxBudget.Lod.FAR ? 0.6 * newest.density() : 0.12;
                v = (float) (time * 0.07);
                colour = smokeColour(base, y, 0, fx.plume, sky, eye.distanceTo(head));
            }
            if (i > 0 && (pa > 0.003 || a > 0.003)) {
                SMOKE.strip(px, py, pz, x, y, z, pw * 0.5, w * 0.5, argb(pa, pc), argb(a, colour), pv, v);
            }
            px = x;
            py = y;
            pz = z;
            pw = w;
            pa = a;
            pc = colour;
            pv = v;
        }
    }

    /** Smoke's colour at height {@code y}, {@code age} ticks old, {@code distance} blocks off. */
    private static int smokeColour(int base, double y, double age, float plume, Sky sky, double distance) {
        double r = ((base >> 16) & 0xFF) / 255.0, g = ((base >> 8) & 0xFF) / 255.0, b = (base & 0xFF) / 255.0;
        double light = 0.12 + 0.88 * sky.day();
        r *= light;
        g *= light;
        b *= light;
        // The low sun still lights smoke high up, warm, after dusk below.
        double high = FxCurves.clamp01((y - 110) / 260);
        double glow = sky.twilight() * high * (1 - 0.4 * sky.day());
        r += 0.95 * glow;
        g += 0.48 * glow;
        b += 0.4 * glow;
        // Fresh smoke lit from inside by the flame.
        double fire = plume * Math.max(0, 1 - age / 40) * (1 - 0.7 * sky.day());
        r += 0.9 * fire;
        g += 0.45 * fire;
        b += 0.15 * fire;
        double h = haze(distance);
        r += (sky.fogR() - r) * h;
        g += (sky.fogG() - g) * h;
        b += (sky.fogB() - b) * h;
        return rgb(r, g, b);
    }

    /** How much of the air's haze lies between here and something {@code distance} blocks off, 0 to about 0.6. */
    private static double haze(double distance) {
        return 0.4 * (1 - Math.exp(-distance / 2500));
    }

    // ------------------------------------------------------------------ falling stages

    private static void drawDebris(RocketFxSource source, float partial, double time, Vec3 eye) {
        if (!source.fxBurning()) return;
        Vec3 pos = source.fxPosition(partial);
        double distance = eye.distanceTo(pos);
        FxBudget.Lod lod = FxBudget.lod(distance);
        if (lod == FxBudget.Lod.FAR && !ClientConfig.rocketFarTrails) return;
        Quaternionf attitude = source.fxAttitude(partial);
        RocketShape.Stage stage = source.fxStage();
        boolean liquid = stage.propellant() == RocketShape.Propellant.LIQUID;
        Vec3 dir = RocketFxManager.rotate(attitude, 0, -1, 0);
        int nozzles = Math.max(1, stage.nozzles().size());
        for (int i = 0; i < stage.nozzles().size(); i++) {
            RocketShape.Nozzle n = stage.nozzles().get(i);
            Vec3 at = pos.add(RocketFxManager.rotate(attitude, n.x(), n.y() - stage.bottom(), n.z()));
            PlumeModel.Plume plume = PlumeModel.of(liquid, 0.4 / Math.sqrt(nozzles), 1000, at.y, MissileConfig.atmosphereTop, 0.5);
            plume(at, dir, plume, liquid, 0.7, time, System.identityHashCode(source) + i, lod, Double.MAX_VALUE, distance, null, false);
        }
    }

    // ------------------------------------------------------------------ moments

    private static void drawBurst(Burst burst, long now, float partial, Vec3 eye, Sky sky) {
        double seconds = burst.seconds(now, partial);
        // Some are set to go off a moment later (a fairing's bolts after the engine cuts off).
        if (seconds < 0) return;
        double length = burst.ticks() / 20.0;
        double t = Math.min(1, seconds / length);
        Vec3 at = burst.at();
        double distance = eye.distanceTo(at);
        if (distance > FxBudget.MID && !ClientConfig.rocketFarTrails) return;
        switch (burst.kind()) {
            case FLASH -> {
                double a = FxCurves.flash(seconds, length);
                double r = burst.size() * (0.45 + 0.55 * Math.sqrt(t));
                GLOW.disc(at.x, at.y, at.z, Math.max(r, distance * 0.006), argb(a, burst.colour()), burst.start());
                GLOW.disc(at.x, at.y, at.z, Math.max(r * 2.6, distance * 0.018), argb(0.35 * a, burst.colour()), burst.start() + 1);
            }
            case RING -> {
                double a = (1 - t) * (1 - t);
                double r = burst.size() * (1 - Math.pow(1 - t, 2.2));
                Vec3 axis = burst.axis() == null ? new Vec3(0, 1, 0) : burst.axis();
                ringAcross(GLOW, at, axis, r, 0.25 + 0.06 * r, argb(0.7 * a, burst.colour()));
                ringAcross(SMOKE, at, axis, r * 0.85, 0.4 + 0.12 * r, argb(0.4 * a, lit(0xF0F0EE, sky, 1)));
            }
            case SHAFT -> {
                double a = FxCurves.flash(seconds, length);
                GLOW.strip(at.x, at.y, at.z, at.x, at.y + burst.height(), at.z, burst.size(), burst.size() * 1.3, argb(0.9 * a, burst.colour()),
                        argb(0.6 * a, burst.colour()), 0, (float) burst.height());
                Vec3 mouth = at.add(0, burst.height() - 2, 0);
                GLOW.disc(mouth.x, mouth.y, mouth.z, 3.5 + 3 * t, argb(0.7 * a, burst.colour()), burst.start());
            }
            case CLOUD -> {
                double a = (1 - t) * (1 - t) * Math.min(1, seconds * 8);
                if (a <= 0.01) return;
                double r = Math.max(burst.size() * (0.25 + 0.75 * Math.sqrt(t)), distance * 0.006);
                // High enough to stay in sunlight a while after it has set below.
                double sunlit = Math.min(1, sky.day() + sky.twilight() * 0.8 + 0.15);
                int colour = shade(burst.colour(), sunlit);
                SMOKE.disc(at.x, at.y, at.z, r, argb(0.5 * a, colour), burst.start());
                SMOKE.disc(at.x, at.y + r * 0.15, at.z, r * 0.7, argb(0.35 * a, colour), burst.start() + 2);
            }
            case DUST_RING -> {
                double a = FxCurves.dustAlpha(seconds);
                if (a <= 0.01) return;
                double r = FxCurves.dustRadius(seconds) * burst.size() / FxCurves.DUST_END;
                int colour = lit(burst.colour(), sky, 1);
                // A rolling wall of dust: soft puffs round the ring, swelling as it spreads, low and thin at its top.
                int puffs = Math.max(16, (int) (r * 2.2));
                double size = 1.0 + 0.11 * r;
                java.util.Random random = new java.util.Random(burst.start());
                for (int k = 0; k < puffs; k++) {
                    double angle = (k + random.nextDouble() * 0.6) * 2 * Math.PI / puffs;
                    double s = size * (0.7 + 0.6 * random.nextDouble());
                    double rr = r * (0.94 + 0.08 * random.nextDouble());
                    double x = at.x + Math.cos(angle) * rr, z = at.z + Math.sin(angle) * rr;
                    SMOKE.disc(x, burst.height() + s * 0.55, z, s, argb(0.55 * a, colour), angle * 7 + k);
                    if (k % 2 == 0) {
                        SMOKE.disc(x, burst.height() + s * 1.25, z, s * 0.8, argb(0.3 * a, colour), angle * 5 + k);
                    }
                }
            }
        }
    }

    /** A ring round {@code centre} lying across {@code axis}, of strips facing the camera. */
    private static void ringAcross(FxQuads quads, Vec3 centre, Vec3 axis, double radius, double thickness, int colour) {
        if (radius < 0.05) return;
        Vec3 u = RocketFxManager.perpendicular(axis);
        Vec3 w = axis.normalize().cross(u);
        int segments = 28;
        Vec3 previous = centre.add(u.scale(radius));
        for (int k = 1; k <= segments; k++) {
            double a = k * 2 * Math.PI / segments;
            Vec3 next = centre.add(u.scale(Math.cos(a) * radius)).add(w.scale(Math.sin(a) * radius));
            quads.strip(previous.x, previous.y, previous.z, next.x, next.y, next.z, thickness, thickness, colour, colour,
                    (float) (radius * (k - 1) * 0.2), (float) (radius * k * 0.2));
            previous = next;
        }
    }

    // ------------------------------------------------------------------ colours

    /** {@code rgb} as smoke lit by the sky. */
    static int lit(int rgb, Sky sky, double strength) {
        if (sky == null) return rgb;
        return shade(rgb, 1 - strength + strength * (0.15 + 0.85 * sky.day()));
    }

    static int shade(int rgb, double light) {
        return rgb(((rgb >> 16) & 0xFF) / 255.0 * light, ((rgb >> 8) & 0xFF) / 255.0 * light, (rgb & 0xFF) / 255.0 * light);
    }

    static int mix(int a, int b, double t) {
        double s = 1 - t;
        return rgb((((a >> 16) & 0xFF) * s + ((b >> 16) & 0xFF) * t) / 255.0, (((a >> 8) & 0xFF) * s + ((b >> 8) & 0xFF) * t) / 255.0,
                ((a & 0xFF) * s + (b & 0xFF) * t) / 255.0);
    }

    private static int rgb(double r, double g, double b) {
        return clamp255(r) << 16 | clamp255(g) << 8 | clamp255(b);
    }

    static int argb(double alpha, int rgb) {
        return clamp255(alpha) << 24 | (rgb & 0xFFFFFF);
    }

    private static int clamp255(double v) {
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 255);
    }
}
