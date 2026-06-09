package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public final class CBCNuclearIntegration {
    public static final String MODID = "createbigcannons";

    private CBCNuclearIntegration() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    public static void init(IEventBus modEventBus) {
        if (isLoaded()) {
            CBCNuclearContent.init(modEventBus);
        }
    }

    public static void addCreativeItems(CreativeModeTab.Output output) {
        if (isLoaded()) {
            CBCNuclearContent.addCreativeItems(output);
        }
    }
}
