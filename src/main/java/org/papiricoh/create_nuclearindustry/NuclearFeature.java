package org.papiricoh.create_nuclearindustry;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * One area of the mod that registers its own content (PWR, civil defense, waste...), so each area
 * lives in its own files and the shared registries only loop over {@link NuclearFeatures#ALL}.
 * Blocks, items and block entities go on the shared {@code DeferredRegister}s from {@link #register}.
 * Client-only hooks live in {@link org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient}.
 */
public interface NuclearFeature {

    /** Called from the mod constructor, before the shared registers fire. */
    default void register(IEventBus modEventBus) {}

    default void registerCapabilities(RegisterCapabilitiesEvent event) {}

    /** Appends this area's items to the end of the creative tab. */
    default void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {}
}
