package org.papiricoh.create_nuclearindustry.integration.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The guide's JEI information pages: the parts of the mod that had none (weapons, reactor ports,
 * tokamak structure...) and the reactor viewport. The fan treatments' pages are the fuel cycle's
 * ({@link org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleInfo}). Free of JEI classes, like
 * {@link PlantJeiInfo}, which lists these pages with its own.
 */
public final class GuideJeiInfo {
    /** The nuclear cannon shell exists only with Create Big Cannons; looked up by id so its classes never load without it. */
    public static final ResourceLocation NUCLEAR_CANNON_SHELL = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_cannon_shell");
    public static final ResourceLocation THERMONUCLEAR_CANNON_SHELL =
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "thermonuclear_cannon_shell");

    /** Every page id, also those shown only when their item is there; the datagen writes a text for each. */
    public static final List<String> IDS = List.of("nuclear_bomb", "thermonuclear_bomb", "warhead_parts", "missile_launch", "nuclear_cannon_shell",
            "thermonuclear_cannon_shell", "reactor_fuel_port",
            "reactor_fluid_port", "control_rod", "reactor_temperature_sensor", "tokamak_structure", "tokamak_magnet_drive",
            "tokamak_fluid_port", "tokamak_fuel_injector", "fusion_turbine", "reactor_viewport");

    private GuideJeiInfo() {}

    public static List<PlantJeiInfo.Page> pages() {
        List<PlantJeiInfo.Page> pages = new ArrayList<>();
        // Weapons (the dirty bomb keeps the page it already had)
        pages.add(items("nuclear_bomb", AllNuclearItems.NUCLEAR_BOMB));
        pages.add(items("thermonuclear_bomb", ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM, ExplosiveRegistry.LITHIUM_DEUTERIDE));
        pages.add(items("warhead_parts", ExplosiveRegistry.EXPLOSIVE_LENS, ExplosiveRegistry.IMPLOSION_ASSEMBLY,
                ExplosiveRegistry.FIRING_SET, ExplosiveRegistry.NEUTRON_INITIATOR, ExplosiveRegistry.BOMB_CASING,
                ExplosiveRegistry.THERMONUCLEAR_CASING));
        pages.add(new PlantJeiInfo.Page("missile_launch", Stream.of("silo_controller", "silo_wall", "rocket_engine", "rocket_tank", "rocket_guidance", "rocket_warhead", "propellant_cartridge", "silo_fuel_port", "star_tracker", "target_designator", "missile_radar", "radar_scope", "holo_table")
                .map(path -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path)))
                .map(ItemStack::new).toList(), List.of()));
        item(NUCLEAR_CANNON_SHELL).ifPresent(shell ->
                pages.add(new PlantJeiInfo.Page("nuclear_cannon_shell", List.of(new ItemStack(shell)), List.of())));
        item(THERMONUCLEAR_CANNON_SHELL).ifPresent(shell ->
                pages.add(new PlantJeiInfo.Page("thermonuclear_cannon_shell", List.of(new ItemStack(shell)), List.of())));

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
