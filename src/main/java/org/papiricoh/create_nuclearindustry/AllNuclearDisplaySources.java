package org.papiricoh.create_nuclearindustry;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorSensorDisplaySource;

public class AllNuclearDisplaySources {
    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
            DeferredRegister.create(CreateBuiltInRegistries.DISPLAY_SOURCE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<DisplaySource, ReactorSensorDisplaySource> REACTOR_TEMPERATURE_SENSOR =
            DISPLAY_SOURCES.register("reactor_temperature_sensor", ReactorSensorDisplaySource::new);

    public static void register(IEventBus modEventBus) {
        DISPLAY_SOURCES.register(modEventBus);
        modEventBus.addListener(AllNuclearDisplaySources::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                DisplaySource.BY_BLOCK_ENTITY.add(AllNuclearEntities.REACTOR_TEMPERATURE_SENSOR.get(), REACTOR_TEMPERATURE_SENSOR.get()));
    }
}
