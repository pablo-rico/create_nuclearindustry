package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.integration.cbc.CBCNuclearIntegration;

public class AllCreativeTabs {
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Create_NuclearIndustry.MODID);


    public static final DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> NUCLEAR_TAB = CREATIVE_MODE_TABS.register("nuclear_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_nuclearindustry.nuclear_tab"))
            .icon(() -> AllNuclearItems.RAW_URANIUM.value().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(AllNuclearItems.RAW_URANIUM.value());
                output.accept(AllNuclearItems.URANIUM.value());
                ItemStack weaponsGradeUranium = new ItemStack(AllNuclearItems.URANIUM.value());
                UraniumItem.setEnrichment(weaponsGradeUranium, 90.0f);
                output.accept(weaponsGradeUranium);
                output.accept(AllNuclearItems.URANIUM_ORE.value());
                output.accept(AllNuclearItems.URANIUM_REACTOR_FUEL.value());
                output.accept(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.value());
                output.accept(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.value());
                output.accept(AllNuclearItems.URANIUM_ROD.value());
                output.accept(AllNuclearItems.BORAX_ORE.value());
                output.accept(AllNuclearItems.BORAX_SALT.value());
                output.accept(AllNuclearItems.BORON.value());
                output.accept(AllNuclearItems.CONTROL_ROD.value());
                output.accept(AllNuclearItems.HEAT_EXCHANGER.value());
                output.accept(AllNuclearItems.REACTOR_CASING.value());
                output.accept(AllNuclearItems.REACTOR_CONTROLLER.value());
                output.accept(AllNuclearItems.REACTOR_FLUID_PORT.value());
                output.accept(AllNuclearItems.REACTOR_FUEL_PORT.value());
                output.accept(AllNuclearItems.REACTOR_TEMPERATURE_SENSOR.value());
                output.accept(AllNuclearItems.DUAL_FLUID_PIPE.value());
                output.accept(AllNuclearItems.CENTRIFUGE.value());
                output.accept(AllNuclearItems.TURBINE_CASING.value());
                output.accept(AllNuclearItems.TURBINE_ROTOR.value());
                output.accept(AllNuclearItems.TURBINE_OUTPUT.value());
                output.accept(AllNuclearItems.TURBINE_FLUID_PORT.value());
                output.accept(AllNuclearItems.NUCLEAR_BOMB.value());
                output.accept(AllNuclearItems.LAUNCH_PAD.value());
                output.accept(AllNuclearItems.MISSILE.value());
                output.accept(AllNuclearItems.TARGET_DESIGNATOR.value());
                // Fusion reactor
                output.accept(AllNuclearItems.FUSION_CONTROLLER.value());
                output.accept(AllNuclearItems.FUSION_ACCELERATOR_SEGMENT.value());
                output.accept(AllNuclearItems.FUSION_ACCELERATOR_CORNER.value());
                output.accept(AllNuclearItems.FUSION_ELECTROMAGNET.value());
                output.accept(AllNuclearItems.FUSION_CRYOSTAT_CASING.value());
                output.accept(AllNuclearItems.FUSION_MAGNET_INPUT.value());
                output.accept(AllNuclearItems.FUSION_FLUID_PORT.value());
                output.accept(AllNuclearItems.FUSION_FUEL_INJECTOR.value());
                output.accept(AllNuclearItems.FUSION_TURBINE_CASING.value());
                output.accept(AllNuclearItems.FUSION_TURBINE_ROTOR.value());
                output.accept(AllNuclearItems.FUSION_TURBINE_FLUID_PORT.value());
                output.accept(AllNuclearItems.FUSION_PLASMA_TURBINE.value());
                output.accept(AllNuclearItems.LITHIUM.value());
                output.accept(AllNuclearItems.LITHIUM_6.value());
                output.accept(AllNuclearItems.BERYLLIUM.value());
                output.accept(AllNuclearItems.SUPERCONDUCTOR_INGOT.value());
                output.accept(AllNuclearItems.LITHIUM_6_BREEDER_ASSEMBLY.value());
                output.accept(AllNuclearItems.DEUTERIUM_CELL.value());
                output.accept(AllNuclearItems.TRITIUM_CELL.value());
                output.accept(AllNuclearItems.DT_FUEL_PELLET.value());
                output.accept(AllNuclearItems.SPENT_DT_PELLET.value());
                CBCNuclearIntegration.addCreativeItems(output);
            }).build());




    public static void init() {}
}
