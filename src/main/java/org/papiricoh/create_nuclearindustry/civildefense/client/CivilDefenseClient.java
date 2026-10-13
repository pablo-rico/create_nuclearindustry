package org.papiricoh.create_nuclearindustry.civildefense.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.siren.SoundingSiren;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for sirens, the seismic detector, blast doors, the blast wave and shelters. */
public final class CivilDefenseClient implements NuclearFeatureClient {
    public static final CivilDefenseClient INSTANCE = new CivilDefenseClient();

    private CivilDefenseClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        SoundingSiren.ClientHooks.install(SirenSounds::tick);
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CivilDefenseRegistry.BLAST_DOOR_CONTROLLER_BE.get(), BlastDoorRenderer::new);
    }
}
