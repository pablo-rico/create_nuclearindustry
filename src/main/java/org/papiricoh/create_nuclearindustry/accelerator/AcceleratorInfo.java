package org.papiricoh.create_nuclearindustry.accelerator;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The accelerator's JEI information pages. Free of JEI classes, like {@link PlantJeiInfo}, which lists
 * these pages with its own; the datagen writes a text for every id in {@link #IDS}.
 */
public final class AcceleratorInfo {
    public static final List<String> IDS = List.of("particle_accelerator", "beamline", "accelerator_machines", "beam_dump");

    private AcceleratorInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        List<PlantJeiInfo.Page> pages = new ArrayList<>();
        pages.add(new PlantJeiInfo.Page("particle_accelerator", stacks(AcceleratorRegistry.CONTROLLER_ITEM, AcceleratorRegistry.ION_SOURCE_ITEM),
                List.of(new FluidStack(AllNuclearFluids.HYDROGEN.get(), 1000))));
        pages.add(page("beamline", AcceleratorRegistry.BEAM_PIPE_ITEM, AcceleratorRegistry.RF_CAVITY_ITEM, AcceleratorRegistry.QUADRUPOLE_ITEM,
                AcceleratorRegistry.BENDING_MAGNET_ITEM, AcceleratorRegistry.POSITION_MONITOR_ITEM));
        pages.add(page("accelerator_machines", AcceleratorRegistry.KLYSTRON_ITEM, AcceleratorRegistry.MAGNET_POWER_SUPPLY_ITEM,
                AcceleratorRegistry.VACUUM_PUMP_ITEM));
        pages.add(page("beam_dump", AcceleratorRegistry.BEAM_DUMP_ITEM));
        return pages;
    }

    @SafeVarargs
    private static PlantJeiInfo.Page page(String id, Supplier<? extends Item>... items) {
        return new PlantJeiInfo.Page(id, stacks(items), List.of());
    }

    @SafeVarargs
    private static List<ItemStack> stacks(Supplier<? extends Item>... items) {
        return Stream.of(items).map(item -> new ItemStack(item.get())).toList();
    }
}
