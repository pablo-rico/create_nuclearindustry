package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class AllCreativeTabs {
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Create_NuclearIndustry.MODID);


    public static final RegistryObject<CreativeModeTab> NUCLEAR_TAB = CREATIVE_MODE_TABS.register("nuclear_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> AllNuclearItems.RAW_URANIUM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(AllNuclearItems.RAW_URANIUM.get());
                output.accept(AllNuclearItems.URANIUM_ORE.get());
                output.accept(AllNuclearItems.URANIUM_238.get());
                output.accept(AllNuclearItems.URANIUM_235.get());
                output.accept(AllNuclearItems.URANIUM_ROD.get());
                output.accept(AllNuclearItems.BORAX_ORE.get());
                output.accept(AllNuclearItems.BORAX_SALT.get());
                output.accept(AllNuclearItems.BORON.get());
                output.accept(AllNuclearItems.CONTROL_ROD.get());
                output.accept(AllNuclearItems.HEAT_EXCHANGER.get());
                output.accept(AllNuclearItems.REACTOR_CASING.get());
                output.accept(AllNuclearItems.REACTOR_CONTROLLER.get());
            }).build());




    public static void init() {}
}
