package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalBlastPayload;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CameraShake;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;

/**
 * Client side of {@link ExplosionFxRegistry}: where the server's detonation packets land, and where every explosion effect is
 * hooked to the game. A nuclear detonation goes to {@link NuclearFx} (the cloud, its sounds and particles), a conventional one to
 * {@link ConventionalFx}; both schedule their sounds and shaking by the shock front's arrival. Around them: the screen's flash and
 * flash blindness ({@link ScreenEffects}), deafness ({@link Deafness}), the sky's tint and the dimmed sun ({@link SkyTint}), the
 * world lit by the flash ({@link TerrainGlow}) and the heat haze ({@link HeatHaze}).
 */
public final class ExplosionFxClient implements NuclearFeatureClient {
    public static final ExplosionFxClient INSTANCE = new ExplosionFxClient();
    private static final double[] SHAKE = new double[3];
    private static long frame;
    @org.jetbrains.annotations.Nullable
    private static net.minecraft.client.multiplayer.ClientLevel levelSeen;

    private ExplosionFxClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterShadersEvent.class, NukeRenderTypes::registerShaders);
        modEventBus.addListener(RegisterParticleProvidersEvent.class, ExplosionParticles::registerProviders);
        modEventBus.addListener(RegisterGuiLayersEvent.class, ScreenEffects::registerLayers);
        modEventBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> HeatHaze.reload()));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, event -> {
            frame++;
            NuclearFx.onFrame();
            SkyTint.update(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        });
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ExplosionFxClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, NuclearFx::fog);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, ExplosionFxClient::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> clear());
    }

    /** A nuclear detonation (or an antimatter flash) went off, or one still standing came into range. */
    public static void onNuclear(DetonationPayload payload) {
        syncLevel();
        NuclearFx.add(payload);
    }

    /** A conventional warhead, bomblet or dirty bomb went off, or one still smoking came into range. */
    public static void onConventional(ConventionalBlastPayload payload) {
        syncLevel();
        ConventionalFx.add(payload);
    }

    /**
     * Forgets what was shown in another world or dimension: it belonged to the old one, and the server sends what stands in the
     * new one. Checked before a packet is taken too, which may come in before the first tick in the new level.
     */
    private static void syncLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != levelSeen) {
            levelSeen = mc.level;
            NuclearFx.clear();
            ConventionalFx.clear();
            ExplosionParticles.clear();
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        syncLevel();
        ExplosionParticles.prune();
        NuclearFx.tick(mc);
        ConventionalFx.tick(mc);
        TerrainGlow.tick(mc);
        Deafness.tick(mc);
        if (CBCNuclearIntegration.isLoaded()) {
            org.papiricoh.create_nuclearindustry.integration.cbc.client.NuclearShellFx.tick(mc);
        }
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            ScreenEffects.capture(event);
            FxProfiler.begin();
            HeatHaze.render(event);
            FxProfiler.end(frame);
            return;
        }
        if (event.getStage() != NukeRenderTypes.stage()) return;
        FxProfiler.begin();
        NuclearFx.render(event);
        ConventionalFx.render(event);
        FxProfiler.end(frame);
    }

    /** Every explosion's shaking together, within what a view can take. */
    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientConfig.explosionShake) return;
        SHAKE[0] = SHAKE[1] = SHAKE[2] = 0;
        float partial = (float) event.getPartialTick();
        NuclearFx.shake(event.getCamera().getPosition(), partial, SHAKE);
        ConventionalFx.shake(event.getCamera().getPosition(), partial, SHAKE);
        if (SHAKE[0] == 0 && SHAKE[1] == 0 && SHAKE[2] == 0) return;
        double max = CameraShake.MAX_DEGREES;
        event.setYaw(event.getYaw() + (float) Math.max(-max, Math.min(max, SHAKE[0])));
        event.setPitch(event.getPitch() + (float) Math.max(-max, Math.min(max, SHAKE[1])));
        event.setRoll(event.getRoll() + (float) Math.max(-max, Math.min(max, SHAKE[2])));
    }

    private static void clear() {
        NuclearFx.clear();
        ConventionalFx.clear();
        ExplosionParticles.clear();
        ScreenEffects.reset();
        Deafness.reset();
        TerrainGlow.reset();
    }
}
