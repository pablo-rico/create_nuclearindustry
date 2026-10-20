package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;
import org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteKind;
import org.papiricoh.create_nuclearindustry.missile.orbit.SkyView;
import org.papiricoh.create_nuclearindustry.missile.orbit.client.ClientSatellites;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.List;

/**
 * The satellites over the level as points of light in the night sky, like moving stars: each drawn in the direction it lies in,
 * on a dome just inside the far plane rather than at its real distance (kilometres up). Low ones cross the sky in seconds and
 * flare now and then; geostationary ones stand still, fainter. Only as the stars come out, under a clear sky, and over the
 * horizon ({@link SkyView}). Drawn after the weather, depth-tested, so terrain and clouds hide them; costs nothing with no
 * satellites up.
 */
public final class SatelliteSkyRender {
    private static final FxQuads QUADS = new FxQuads();
    /** The dome's radius as a share of the far plane's distance. */
    private static final double DOME = 0.8;
    /** A point's radius, as a share of the dome's: moving and parked. */
    private static final double LOW_SIZE = 0.0042, HIGH_SIZE = 0.0032;
    private static boolean initialised;

    private SatelliteSkyRender() {
    }

    /** Hooks the frame; call once from the client side's init. */
    public static void init() {
        if (initialised) return;
        initialised = true;
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, SatelliteSkyRender::onRenderLevel);
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || !ClientConfig.rocketSatellites) return;
        List<Satellite> satellites = ClientSatellites.all();
        if (satellites.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL) return;
        Camera camera = event.getCamera();
        if (camera.getFluidInCamera() != FogType.NONE) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double dark = SkyView.skyDarkness(level.getStarBrightness(partial), level.getRainLevel(partial));
        if (dark < 0.02) return;

        Vec3 eye = camera.getPosition();
        double far = mc.gameRenderer.getDepthFar();
        double radius = far * DOME;
        boolean shaded = RocketRenderTypes.shaded();
        QUADS.begin(eye, far * 0.9, shaded, camera.getLeftVector(), camera.getUpVector());
        double time = level.getGameTime() + partial;
        V3 from = new V3(eye.x, eye.y, eye.z);
        for (Satellite satellite : satellites) {
            V3 dir = SkyView.direction(MissileConfig.radarEarthRadius, from, satellite.orbit().position(time));
            if (dir == null) continue;
            double haze = SkyView.haze(dir);
            if (haze <= 0.01) continue;
            boolean moving = !satellite.orbit().geostationary();
            int seed = satellite.id().hashCode();
            double flare = SkyView.flare(time, seed, moving, 2.5);
            double alpha = dark * haze * (moving ? 0.9 : 0.55);
            int colour = satellite.kind() == SatelliteKind.COMMS ? 0xE6EEFF : 0xFFF0D2;
            double x = eye.x + dir.x() * radius, y = eye.y + dir.y() * radius, z = eye.z + dir.z() * radius;
            double size = radius * (moving ? LOW_SIZE : HIGH_SIZE) * Math.sqrt(flare);
            QUADS.disc(x, y, z, size, RocketFxRender.argb(Math.min(1, alpha * flare), colour), seed);
            QUADS.disc(x, y, z, size * 2.8, RocketFxRender.argb(0.14 * alpha * flare, colour), seed + 1);
        }
        QUADS.draw(RocketRenderTypes.glow(shaded));
    }
}
