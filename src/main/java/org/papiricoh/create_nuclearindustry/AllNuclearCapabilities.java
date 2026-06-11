package org.papiricoh.create_nuclearindustry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class AllNuclearCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AllNuclearEntities.CENTRIFUGE.get(),
                (centrifuge, direction) -> centrifuge.getItemHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllNuclearEntities.DUAL_PIPE.get(),
                (pipe, direction) -> pipe.getFluidHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllNuclearEntities.REACTOR_FLUID_PORT.get(),
                (port, direction) -> port.getFluidHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AllNuclearEntities.REACTOR_FUEL_PORT.get(),
                (port, direction) -> port.getItemHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllNuclearEntities.TURBINE_FLUID_PORT.get(),
                (port, direction) -> port.getFluidHandler(direction)
        );

        // ---- Fusion reactor ----
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllNuclearEntities.FUSION_FLUID_PORT.get(),
                (port, direction) -> port.getFluidHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AllNuclearEntities.FUSION_FUEL_INJECTOR.get(),
                (port, direction) -> port.getItemHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllNuclearEntities.FUSION_TURBINE_FLUID_PORT.get(),
                (port, direction) -> port.getFluidHandler(direction)
        );
    }
}
