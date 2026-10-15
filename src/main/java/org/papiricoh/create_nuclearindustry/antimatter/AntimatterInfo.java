package org.papiricoh.create_nuclearindustry.antimatter;

import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import java.util.List;

/**
 * JEI information pages of antimatter containment and of the creative energy source that tests
 * it. Free of JEI classes, like {@link PlantJeiInfo}, which lists them with its own.
 */
public final class AntimatterInfo {
    public static final List<String> IDS = List.of("penning_traps", "antimatter_vial", "antimatter_conduit", "annihilation_generator",
            "antiproton_injector", "creative_rf_generator");

    private AntimatterInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        return List.of(
                page("penning_traps", new ItemStack(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get()),
                        new ItemStack(AntimatterRegistry.PENNING_TRAP_ITEM.get()), new ItemStack(AntimatterRegistry.LARGE_PENNING_TRAP_ITEM.get())),
                page("antimatter_vial", new ItemStack(AntimatterRegistry.ANTIMATTER_VIAL.get())),
                page("antimatter_conduit", new ItemStack(AntimatterRegistry.ANTIMATTER_CONDUIT_ITEM.get())),
                page("annihilation_generator", new ItemStack(AntimatterRegistry.ANNIHILATION_GENERATOR_ITEM.get())),
                page("antiproton_injector", new ItemStack(AntimatterRegistry.ANTIPROTON_INJECTOR_ITEM.get())),
                page("creative_rf_generator", new ItemStack(EnergyRegistry.CREATIVE_RF_GENERATOR_ITEM.get())));
    }

    private static PlantJeiInfo.Page page(String id, ItemStack... stacks) {
        return new PlantJeiInfo.Page(id, List.of(stacks), List.of());
    }
}
