package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/**
 * Client setup for antimatter containment: the trap renderer (cloud, lights, gauge) with its
 * partials, the conduit's flow glow, the trap's hum and sparks, the vial's model switches, the
 * annihilation generator (shaft, mode lamps, core, drone) and the antiproton injector (lamps, dose,
 * shot).
 */
public final class AntimatterClient implements NuclearFeatureClient {
    public static final AntimatterClient INSTANCE = new AntimatterClient();

    private AntimatterClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        AntimatterPartials.init();
        PenningTrapBlockEntity.clientTick = PenningTrapBlockEntity.clientTick.andThen(TrapClientEffects::tick);
        AnnihilationGeneratorBlockEntity.clientTick = AnnihilationGeneratorBlockEntity.clientTick.andThen(GeneratorClientEffects::tick);
        AntiprotonInjectorBlockEntity.clientTick = AntiprotonInjectorBlockEntity.clientTick.andThen(InjectorClientEffects::tick);
        modEventBus.addListener(AntimatterRenderTypes::registerShaders);
        modEventBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(VialItemProperties::register));
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AntimatterRegistry.PENNING_TRAP_ENTITY.get(), PenningTrapRenderer::new);
        event.registerBlockEntityRenderer(AntimatterRegistry.CONDUIT_ENTITY.get(), AntimatterConduitRenderer::new);
        event.registerBlockEntityRenderer(AntimatterRegistry.GENERATOR_ENTITY.get(), AnnihilationGeneratorRenderer::new);
        event.registerBlockEntityRenderer(AntimatterRegistry.INJECTOR_ENTITY.get(), AntiprotonInjectorRenderer::new);
    }
}
