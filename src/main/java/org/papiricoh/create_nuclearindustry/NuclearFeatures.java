package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fusion.FusionRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;

/** Every {@link NuclearFeature}, in creative tab order. */
public final class NuclearFeatures {
    public static final List<NuclearFeature> ALL = List.of(
            PwrRegistry.INSTANCE,
            SpentFuelRegistry.INSTANCE,
            WasteRegistry.INSTANCE,
            ControlRoom2Registry.INSTANCE,
            CivilDefenseRegistry.INSTANCE,
            FusionRegistry.INSTANCE,
            CryoRegistry.INSTANCE,
            PlasmaHeatingRegistry.INSTANCE,
            ReactorVisualsRegistry.INSTANCE,
            FuelCycleRegistry.INSTANCE,
            AcceleratorRegistry.INSTANCE);

    private NuclearFeatures() {}

    static void register(IEventBus modEventBus) {
        ALL.forEach(feature -> feature.register(modEventBus));
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ALL.forEach(feature -> feature.registerCapabilities(event));
    }

    static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        ALL.forEach(feature -> feature.fillCreativeTab(parameters, output));
    }
}
