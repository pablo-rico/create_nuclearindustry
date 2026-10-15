package org.papiricoh.create_nuclearindustry.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.accelerator.client.AcceleratorClient;
import org.papiricoh.create_nuclearindustry.antimatter.client.AntimatterClient;
import org.papiricoh.create_nuclearindustry.civildefense.client.CivilDefenseClient;
import org.papiricoh.create_nuclearindustry.controlroom.client.ControlRoom2Client;
import org.papiricoh.create_nuclearindustry.fuelcycle.client.FuelCycleClient;
import org.papiricoh.create_nuclearindustry.fusion.client.FusionClient;
import org.papiricoh.create_nuclearindustry.fusion.cryo.client.CryoClient;
import org.papiricoh.create_nuclearindustry.fusion.heating.client.PlasmaHeatingClient;
import org.papiricoh.create_nuclearindustry.reactor.pwr.client.PwrClient;
import org.papiricoh.create_nuclearindustry.reactor.visual.client.ReactorVisualsClient;
import org.papiricoh.create_nuclearindustry.spentfuel.client.SpentFuelClient;
import org.papiricoh.create_nuclearindustry.waste.client.WasteClient;

import java.util.List;

/** Every {@link NuclearFeatureClient}; only touched on the client. */
public final class NuclearFeaturesClient {
    public static final List<NuclearFeatureClient> ALL = List.of(
            PwrClient.INSTANCE,
            SpentFuelClient.INSTANCE,
            WasteClient.INSTANCE,
            ControlRoom2Client.INSTANCE,
            CivilDefenseClient.INSTANCE,
            FusionClient.INSTANCE,
            CryoClient.INSTANCE,
            PlasmaHeatingClient.INSTANCE,
            ReactorVisualsClient.INSTANCE,
            FuelCycleClient.INSTANCE,
            AcceleratorClient.INSTANCE,
            AntimatterClient.INSTANCE,
            org.papiricoh.create_nuclearindustry.isotope.client.IsotopeClient.INSTANCE,
            org.papiricoh.create_nuclearindustry.explosive.client.ExplosiveClient.INSTANCE);

    private NuclearFeaturesClient() {}

    public static void init(IEventBus modEventBus) {
        ALL.forEach(feature -> feature.init(modEventBus));
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ALL.forEach(feature -> feature.registerRenderers(event));
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        ALL.forEach(feature -> feature.registerScreens(event));
    }

    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        ALL.forEach(feature -> feature.registerGeometryLoaders(event));
    }
}
