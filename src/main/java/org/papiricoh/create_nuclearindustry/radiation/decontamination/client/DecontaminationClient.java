package org.papiricoh.create_nuclearindustry.radiation.decontamination.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationRegistry;

/** Client setup for the Decontamination Sprinkler: its spinning head. */
public final class DecontaminationClient implements NuclearFeatureClient {
    public static final DecontaminationClient INSTANCE = new DecontaminationClient();
    /** The spray head with its four nozzle arms, turning about the block's vertical axis. */
    public static final PartialModel SPRINKLER_HEAD = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/decontamination_sprinkler/head"));

    private DecontaminationClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        // Touching the class creates the partial before models load.
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DecontaminationRegistry.SPRINKLER_ENTITY.get(), SprinklerRenderer::new);
    }
}
