package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

public class NuclearPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Create_NuclearIndustry.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        AllNuclearPonderScenes.register(helper);
    }
}
