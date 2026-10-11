package org.papiricoh.create_nuclearindustry.integration.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The guide's JEI information pages: the parts of the mod that had none (weapons, reactor ports,
 * tokamak structure...), the reactor viewport and the two fan treatments. Free of JEI classes, like
 * {@link PlantJeiInfo}, which lists these pages with its own.
 */
public final class GuideJeiInfo {
    /** The nuclear cannon shell exists only with Create Big Cannons; looked up by id so its classes never load without it. */
    public static final ResourceLocation NUCLEAR_CANNON_SHELL = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_cannon_shell");
    private static final ResourceLocation ENCASED_FAN = ResourceLocation.fromNamespaceAndPath("create", "encased_fan");

    /** Every page id, also those shown only when their item is there; the datagen writes a text for each. */
    public static final List<String> IDS = List.of("nuclear_bomb", "missile_launch", "nuclear_cannon_shell", "reactor_fuel_port",
            "reactor_fluid_port", "control_rod", "reactor_temperature_sensor", "tokamak_structure", "tokamak_magnet_drive",
            "tokamak_fluid_port", "tokamak_fuel_injector", "fusion_turbine", "reactor_viewport", "acid_leaching", "irradiation");

    private GuideJeiInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        List<PlantJeiInfo.Page> pages = new ArrayList<>();
        // Weapons (the dirty bomb keeps the page it already had)
        pages.add(items("nuclear_bomb", AllNuclearItems.NUCLEAR_BOMB));
        pages.add(items("missile_launch", AllNuclearItems.LAUNCH_PAD, AllNuclearItems.MISSILE, AllNuclearItems.TARGET_DESIGNATOR));
        item(NUCLEAR_CANNON_SHELL).ifPresent(shell ->
                pages.add(new PlantJeiInfo.Page("nuclear_cannon_shell", List.of(new ItemStack(shell)), List.of())));

        // The fission reactor's shell parts (the redstone port keeps the page it already had)
        pages.add(items("reactor_fuel_port", AllNuclearItems.REACTOR_FUEL_PORT));
        pages.add(items("reactor_fluid_port", AllNuclearItems.REACTOR_FLUID_PORT));
        pages.add(items("control_rod", AllNuclearItems.CONTROL_ROD));
        pages.add(items("reactor_temperature_sensor", AllNuclearItems.REACTOR_TEMPERATURE_SENSOR));
        pages.add(items("reactor_viewport", ReactorVisualsRegistry.REACTOR_VIEWPORT_ITEM));

        // The tokamak's structure and the fusion turbine
        pages.add(items("tokamak_structure", AllNuclearItems.FUSION_ACCELERATOR_SEGMENT, AllNuclearItems.FUSION_ELECTROMAGNET,
                AllNuclearItems.FUSION_CENTRAL_SOLENOID, AllNuclearItems.FUSION_CRYOSTAT_CASING, AllNuclearItems.FUSION_BREEDING_BLANKET));
        pages.add(items("tokamak_magnet_drive", AllNuclearItems.FUSION_MAGNET_INPUT));
        pages.add(items("tokamak_fluid_port", AllNuclearItems.FUSION_FLUID_PORT));
        pages.add(items("tokamak_fuel_injector", AllNuclearItems.FUSION_FUEL_INJECTOR));
        pages.add(items("fusion_turbine", AllNuclearItems.FUSION_TURBINE_CASING, AllNuclearItems.FUSION_TURBINE_ROTOR,
                AllNuclearItems.FUSION_TURBINE_FLUID_PORT));

        // The two fan treatments, on what makes them and on the fan
        List<ItemStack> acid = new ArrayList<>(List.of(new ItemStack(AllNuclearFluids.SULFURIC_ACID_BUCKET.get())));
        List<ItemStack> irradiation = new ArrayList<>(List.of(new ItemStack(WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM.get()),
                new ItemStack(FuelCycleRegistry.THORIUM_DIOXIDE.get()), new ItemStack(FuelCycleRegistry.IRRADIATED_THORIUM.get())));
        item(ENCASED_FAN).ifPresent(fan -> {
            acid.add(new ItemStack(fan));
            irradiation.add(new ItemStack(fan));
        });
        pages.add(new PlantJeiInfo.Page("acid_leaching", acid, List.of(new FluidStack(AllNuclearFluids.SULFURIC_ACID.get(), 1000))));
        pages.add(new PlantJeiInfo.Page("irradiation", irradiation, List.of()));
        return pages;
    }

    private static Optional<Item> item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR);
    }

    @SafeVarargs
    private static PlantJeiInfo.Page items(String id, Supplier<? extends Item>... items) {
        return new PlantJeiInfo.Page(id, Stream.of(items).map(item -> new ItemStack(item.get())).toList(), List.of());
    }
}
