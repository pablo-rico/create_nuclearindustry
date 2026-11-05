package org.papiricoh.create_nuclearindustry.dyson.client.fx;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.dyson.client.ClientSwarm;
import org.papiricoh.create_nuclearindustry.dyson.swarm.OrbitShape;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SunPath;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SwarmGeometry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SwarmSyncPayload;
import org.papiricoh.create_nuclearindustry.missile.fx.client.RocketRenderTypes;
import org.papiricoh.create_nuclearindustry.missile.orbit.SkyView;

/**
 * The Dyson swarm in the Overworld's sky: its sails as golden points round the sun, each orbit a faint ring of them, going round as
 * {@link SwarmGeometry} says. They must read by day next to the sun, so each is a solid bright dot (paint) in an added halo (light);
 * at night the halos carry them. Up to {@link ClientConfig#dysonMaxPoints} points are shared among the orbits by their sails, each
 * standing for as many sails as it must and the brighter for it. Points behind the sun's disc are hidden and those near the horizon
 * fade into its haze. Drawn on a dome inside the far plane after the weather ({@link #stage}), depth-tested, so terrain and clouds
 * hide them; a shader pack fogs the distance more, so its dome is smaller ({@link ClientConfig#dysonShaderDomeScale}).
 * <p>
 * Allocation-free per frame: every point goes through the same scratch arrays and builders.
 */
public final class DysonSkyRender {
    private static final DysonQuads HALO = new DysonQuads(), RING = new DysonQuads(), CORE = new DysonQuads();
    private static final double[] SUN = new double[3], OUT = new double[4], PREV = new double[4];
    private static final int[] POINTS = new int[OrbitShape.COUNT];
    /** Each orbit's tint: all gold, a little apart so they tell from one another. */
    private static final int[] TINT = {0xFFD36B, 0xFFC05A, 0xFFE28E, 0xFFB872};
    /** A point's core and halo radius as shares of the dome's. */
    private static final double CORE_SIZE = 0.0021, HALO_SIZE = 0.0068;
    /** Ring segments per orbit, and its half-width as a share of the dome. */
    private static final int RING_SEGMENTS = 72;
    private static final double RING_WIDTH = 0.0011;
    /** Sine of the elevation below which points fade into the horizon's haze. */
    private static final double HAZE = 0.12;
    private static boolean initialised;
    /** The sail whose place lies nearest its orbit's true ring: tracing its path round draws the ring. */
    private static int ringSail;

    private DysonSkyRender() {
    }

    /** Hooks the frame; call once from the client side's init. */
    public static void init() {
        if (initialised) return;
        initialised = true;
        ringSail = ringSail();
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, DysonSkyRender::onRenderLevel);
    }

    /** The sail strayed least from its ring, by the hashes {@code SwarmGeometry.sail} spreads them with. */
    private static int ringSail() {
        int best = 0;
        double bestOff = Double.MAX_VALUE;
        for (int i = 0; i < 4096; i++) {
            double off = Math.abs(SwarmGeometry.hash(i, SwarmGeometry.RADIAL_SALT) - 0.5) + Math.abs(SwarmGeometry.hash(i, SwarmGeometry.NORMAL_SALT) - 0.5);
            if (off < bestOff) {
                bestOff = off;
                best = i;
            }
        }
        return best;
    }

    /**
     * The stage the sails are drawn in. After the weather, normally; but under a shader pack whatever is drawn there goes through
     * the pack's weather program, which may treat it as rain (Complementary samples the rain texture and scales it by the rain's
     * strength, so on a clear day the sails vanish). There they are drawn with the block entities instead, as the beams are.
     */
    private static RenderLevelStageEvent.Stage stage() {
        return RocketRenderTypes.shaderPackInUse() ? RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES : RenderLevelStageEvent.Stage.AFTER_WEATHER;
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != stage() || !ClientConfig.dysonSky || ClientConfig.dysonMaxPoints <= 0) return;
        SwarmSyncPayload swarm = ClientSwarm.get();
        long total = swarm.totalSails();
        if (total <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !ClientSwarm.shows(level) || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL) return;
        Camera camera = event.getCamera();
        if (camera.getFluidInCamera() != FogType.NONE) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        org.papiricoh.create_nuclearindustry.dyson.client.ClientSun.direction(level, partial, SUN);
        // The widest orbit is 40 degrees out: with the sun this far down, none of it is up.
        if (SUN[1] < -0.72) return;
        double weather = 1 - 0.85 * level.getRainLevel(partial);
        if (weather < 0.03) return;
        double night = SkyView.skyDarkness(level.getStarBrightness(partial), 0);

        Vec3 eye = camera.getPosition();
        double far = mc.gameRenderer.getDepthFar();
        double radius = far * (RocketRenderTypes.shaderPackInUse() ? ClientConfig.dysonShaderDomeScale : ClientConfig.dysonDomeScale);
        boolean shaded = RocketRenderTypes.shaded();
        HALO.begin(eye, far * 0.95, shaded, camera.getLeftVector(), camera.getUpVector());
        RING.begin(eye, far * 0.95, false, camera.getLeftVector(), camera.getUpVector());
        CORE.begin(eye, far * 0.95, false, camera.getLeftVector(), camera.getUpVector());
        double time = level.getGameTime() + partial;
        double sx = SUN[0], sy = SUN[1], sz = SUN[2];

        long[] sails = swarm.sails();
        OrbitShape[] shapes = swarm.shapes();
        int budget = ClientConfig.dysonMaxPoints;
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            long n = sails[i] <= 0 ? 0 : Math.max(1, Math.min(sails[i], Math.round(budget * (sails[i] / (double) total))));
            POINTS[i] = (int) Math.min(n, budget);
        }

        double haloAlpha = 0.16 + 0.34 * night;
        for (int i = 0; i < OrbitShape.COUNT; i++) {
            int n = POINTS[i];
            if (n <= 0) continue;
            OrbitShape shape = shapes[i];
            int tint = TINT[i];
            double per = sails[i] / (double) n;
            double bright = Math.max(0.55, Math.min(1, 0.55 + 0.15 * Math.log(per)));
            double size = 0.85 + 0.35 * (bright - 0.55) / 0.45;
            ring(shape, sx, sy, sz, time, radius, eye, tint, weather * (0.07 + 0.08 * night) * Math.min(1, 0.3 + sails[i] / 300.0));
            for (int j = 0; j < n; j++) {
                SwarmGeometry.sail(shape, sx, sy, sz, j, time, OUT);
                if (SwarmGeometry.hiddenBySun(OUT, sx, sy, sz)) continue;
                double haze = haze(OUT[1]);
                if (haze <= 0.01) continue;
                int seed = j * 7919 + i * 104729;
                double glint = SkyView.flare(time, seed, true, 1.6);
                double a = bright * haze * weather;
                double x = eye.x + OUT[0] * radius, y = eye.y + OUT[1] * radius, z = eye.z + OUT[2] * radius;
                double r = radius * CORE_SIZE * size;
                CORE.dot(x, y, z, r, DysonQuads.argb(0.95 * a, 0xFFF4D0), DysonQuads.argb(0.55 * a, tint));
                HALO.disc(x, y, z, radius * HALO_SIZE * size * Math.sqrt(glint), DysonQuads.argb(haloAlpha * a * glint, tint), seed);
            }
        }
        HALO.draw(RocketRenderTypes.glow(shaded));
        RING.draw(RocketRenderTypes.glow(false));
        CORE.draw(DysonRenderTypes.paint());
    }

    /** An orbit's faint ring: the path of its least-strayed sail, traced once round. */
    private static void ring(OrbitShape shape, double sx, double sy, double sz, double time, double radius, Vec3 eye, int tint, double alpha) {
        if (alpha < 0.005) return;
        double period = SwarmGeometry.period(shape.radius());
        double width = radius * RING_WIDTH;
        boolean have = false;
        for (int k = 0; k <= RING_SEGMENTS; k++) {
            SwarmGeometry.sail(shape, sx, sy, sz, ringSail, time + period * k / RING_SEGMENTS, OUT);
            boolean shows = !SwarmGeometry.hiddenBySun(OUT, sx, sy, sz) && OUT[1] > -0.02;
            if (have && shows) {
                double a0 = alpha * haze(PREV[1]), a1 = alpha * haze(OUT[1]);
                if (a0 + a1 > 0.004) {
                    RING.strip(eye.x + PREV[0] * radius, eye.y + PREV[1] * radius, eye.z + PREV[2] * radius,
                            eye.x + OUT[0] * radius, eye.y + OUT[1] * radius, eye.z + OUT[2] * radius, width, width,
                            DysonQuads.argb(a0, tint), DysonQuads.argb(a1, tint), 0, 0);
                }
            }
            have = shows;
            PREV[0] = OUT[0];
            PREV[1] = OUT[1];
            PREV[2] = OUT[2];
        }
    }

    /** How much of a point at sine-of-elevation {@code y} shows through the haze near the horizon, 0 to 1. */
    private static double haze(double y) {
        double t = Math.max(0, Math.min(1, y / HAZE));
        return t * t * (3 - 2 * t);
    }
}
