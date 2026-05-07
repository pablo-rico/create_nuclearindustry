package org.papiricoh.create_nuclearindustry.enrichment;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

@EventBusSubscriber(modid = Create_NuclearIndustry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CentrifugeCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AllNuclearEntities.CENTRIFUGE.get(),
                (centrifuge, direction) -> centrifuge.getItemHandler(direction)
        );
    }
}
