package org.papiricoh.create_nuclearindustry.client;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;

/** Item colours; only touched on the client. */
public final class NuclearItemColors {

    private NuclearItemColors() {}

    /**
     * Tints the fluid inside the buckets: their model draws the fluid's still texture, which stays
     * grey until it gets the fluid's colour.
     */
    public static void register(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), AllNuclearFluids.buckets().toArray(Item[]::new));
    }
}
