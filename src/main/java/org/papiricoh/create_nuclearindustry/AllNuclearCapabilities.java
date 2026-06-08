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
                AllNuclearEntities.REACTOR.get(),
                (reactor, direction) -> reactor.getFluidHandler(direction)
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                AllNuclearEntities.TURBINE_OUTPUT.get(),
                (turbine, direction) -> turbine.getFluidHandler(direction)
        );
    }
}
