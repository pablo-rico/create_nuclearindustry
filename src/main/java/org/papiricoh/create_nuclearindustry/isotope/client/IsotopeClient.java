package org.papiricoh.create_nuclearindustry.isotope.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.Isotopes;

/** Client setup for isotopes: the Gamma Irradiator's renderer and partials, and the clock tooltips read. */
public final class IsotopeClient implements NuclearFeatureClient {
    public static final IsotopeClient INSTANCE = new IsotopeClient();

    private IsotopeClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        IsotopePartials.init();
        Isotopes.clientClock = () -> {
            var level = Minecraft.getInstance().level;
            return level == null ? -1 : level.getGameTime();
        };
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(IsotopeRegistry.GAMMA_IRRADIATOR_ENTITY.get(), GammaIrradiatorRenderer::new);
    }
}
