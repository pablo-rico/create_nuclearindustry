package org.papiricoh.create_nuclearindustry.propulsion;

import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.integration.sable.SableCompat;

import java.util.List;

/**
 * JEI information pages of the nuclear engines, only with Sable installed. Free of JEI classes, like
 * {@link PlantJeiInfo}, which lists them with its own.
 */
public final class PropulsionInfo {
    public static final List<String> IDS = List.of("nuclear_thermal_engine", "antimatter_engine");

    private PropulsionInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        if (!SableCompat.isLoaded()) {
            return List.of();
        }
        return List.of(
                page("nuclear_thermal_engine", new ItemStack(PropulsionRegistry.THERMAL_ENGINE_ITEM.get()),
                        new ItemStack(PropulsionRegistry.THERMAL_EXCHANGER_ITEM.get()), new ItemStack(PropulsionRegistry.THERMAL_NOZZLE_ITEM.get())),
                page("antimatter_engine", new ItemStack(PropulsionRegistry.ANTIMATTER_ENGINE_ITEM.get()),
                        new ItemStack(PropulsionRegistry.MAGNETIC_COIL_ITEM.get()), new ItemStack(PropulsionRegistry.MAGNETIC_NOZZLE_ITEM.get())));
    }

    private static PlantJeiInfo.Page page(String id, ItemStack... stacks) {
        return new PlantJeiInfo.Page(id, List.of(stacks), List.of());
    }
}
