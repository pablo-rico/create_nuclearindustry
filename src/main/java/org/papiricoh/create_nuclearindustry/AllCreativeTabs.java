package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AllCreativeTabs {
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Create_NuclearIndustry.MODID);


    public static final DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> NUCLEAR_TAB = CREATIVE_MODE_TABS.register("nuclear_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> AllNuclearItems.RAW_URANIUM.value().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(AllNuclearItems.RAW_URANIUM.value());
                output.accept(AllNuclearItems.URANIUM_ORE.value());
                output.accept(AllNuclearItems.URANIUM_238.value());
                output.accept(AllNuclearItems.URANIUM_235.value());
                output.accept(AllNuclearItems.URANIUM_ROD.value());
                output.accept(AllNuclearItems.BORAX_ORE.value());
                output.accept(AllNuclearItems.BORAX_SALT.value());
                output.accept(AllNuclearItems.BORON.value());
                output.accept(AllNuclearItems.CONTROL_ROD.value());
                output.accept(AllNuclearItems.HEAT_EXCHANGER.value());
                output.accept(AllNuclearItems.REACTOR_CASING.value());
                output.accept(AllNuclearItems.REACTOR_CONTROLLER.value());
            }).build());




    public static void init() {}
}
