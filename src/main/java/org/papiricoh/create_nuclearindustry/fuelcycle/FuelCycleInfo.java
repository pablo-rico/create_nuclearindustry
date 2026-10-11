package org.papiricoh.create_nuclearindustry.fuelcycle;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The renewable fuel cycle's JEI information pages. Free of JEI classes, like {@link PlantJeiInfo},
 * which lists these pages with its own. Every item of {@link FuelCycleRegistry#ITEMS} is on one.
 */
public final class FuelCycleInfo {

    private FuelCycleInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        List<PlantJeiInfo.Page> pages = new ArrayList<>();
        pages.add(new PlantJeiInfo.Page("acid_leaching", stacks(AllNuclearFluids.SULFURIC_ACID_BUCKET),
                fluids(AllNuclearFluids.SULFURIC_ACID)));
        pages.add(new PlantJeiInfo.Page("irradiation", stacks(FuelCycleRegistry.IRRADIATION_SOURCE_ITEM,
                FuelCycleRegistry.IRRADIATED_THORIUM, FuelCycleRegistry.IRRADIATED_DEPLETED_URANIUM), List.of()));
        pages.add(new PlantJeiInfo.Page("rock_fines", stacks(FuelCycleRegistry.GRANITE_FINES, FuelCycleRegistry.TUFF_FINES,
                FuelCycleRegistry.DEEPSLATE_FINES), List.of()));
        pages.add(new PlantJeiInfo.Page("heavy_mineral_sand", stacks(FuelCycleRegistry.HEAVY_MINERAL_SAND,
                FuelCycleRegistry.THORIUM_DIOXIDE), List.of()));
        pages.add(new PlantJeiInfo.Page("seawater", stacks(FuelCycleRegistry.AMIDOXIME_ADSORBENT, FuelCycleRegistry.LOADED_ADSORBENT),
                fluids(AllNuclearFluids.BRINE)));
        pages.add(new PlantJeiInfo.Page("decontamination", stacks(FuelCycleRegistry.RADIOACTIVE_SLUDGE), List.of()));
        pages.add(new PlantJeiInfo.Page("thorium_fuel", stacks(FuelCycleRegistry.THORIUM_FUEL_PELLET,
                FuelCycleRegistry.THORIUM_REACTOR_FUEL, FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL,
                FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS, FuelCycleRegistry.URANIUM_233_DIOXIDE),
                fluids(AllNuclearFluids.THORIUM_FUEL_SOLUTION, AllNuclearFluids.URANIUM_233_NITRATE)));
        return pages;
    }

    @SafeVarargs
    private static List<ItemStack> stacks(Supplier<? extends Item>... items) {
        return Stream.of(items).map(item -> new ItemStack(item.get())).toList();
    }

    @SafeVarargs
    private static List<FluidStack> fluids(Supplier<? extends Fluid>... fluids) {
        return Stream.of(fluids).map(fluid -> new FluidStack(fluid.get(), 1000)).toList();
    }
}
