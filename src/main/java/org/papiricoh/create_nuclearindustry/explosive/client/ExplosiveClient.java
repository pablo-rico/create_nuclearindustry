package org.papiricoh.create_nuclearindustry.explosive.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.integration.dh.DistantHorizonsCompat;

/** Client side of {@link org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry}: how a nuclear detonation looks. */
public final class ExplosiveClient implements NuclearFeatureClient {
    public static final ExplosiveClient INSTANCE = new ExplosiveClient();

    private ExplosiveClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(NuclearExplosionEffects::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(NuclearExplosionEffects::onFogColor);
        NeoForge.EVENT_BUS.addListener(NuclearExplosionEffects::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(NuclearExplosionEffects::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(NuclearExplosionEffects::onRenderFrame);
        DistantHorizonsCompat.initClient(modEventBus);
        modEventBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_flash"), NuclearExplosionEffects::renderOverlay));
    }
}
