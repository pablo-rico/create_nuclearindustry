package org.papiricoh.create_nuclearindustry.fuelcycle;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The renewable fuel cycle's JEI information pages. Free of JEI classes, like {@link PlantJeiInfo},
 * which lists these pages with its own.
 */
public final class FuelCycleInfo {

    private FuelCycleInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        List<PlantJeiInfo.Page> pages = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        FuelCycleRegistry.ITEMS.forEach(item -> items.add(new ItemStack(item.get())));
        items.add(new ItemStack(AllNuclearFluids.SULFURIC_ACID_BUCKET.get()));
        pages.add(new PlantJeiInfo.Page("renewable_fuel_cycle", items,
                List.of(new FluidStack(AllNuclearFluids.SULFURIC_ACID.get(), 1000), new FluidStack(AllNuclearFluids.BRINE.get(), 1000))));
        return pages;
    }
}
