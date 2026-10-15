package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fusion.FusionRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;

/** Every {@link NuclearFeature}, in creative tab order within each section. */
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
            AcceleratorRegistry.INSTANCE,
            AntimatterRegistry.INSTANCE,
            EnergyRegistry.INSTANCE,
            org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry.INSTANCE,
            org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry.INSTANCE,
            org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry.INSTANCE,
            org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry.INSTANCE);

    private NuclearFeatures() {}

    static void register(IEventBus modEventBus) {
        ALL.forEach(feature -> feature.register(modEventBus));
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ALL.forEach(feature -> feature.registerCapabilities(event));
    }

    public static void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        ALL.forEach(feature -> {
            output.clearSection();
            feature.fillCreativeTab(parameters, output);
        });
        output.clearSection();
    }
}
