package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

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

    public static void addCreativeItems(SectionedOutput output) {
        if (isLoaded()) {
            CBCNuclearContent.addCreativeItems(output);
        }
    }
}
