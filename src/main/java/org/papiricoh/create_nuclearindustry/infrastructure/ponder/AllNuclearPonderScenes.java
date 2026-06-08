package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CentrifugeScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.TurbineScenes;

public class AllNuclearPonderScenes {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(
                id("centrifuge"),
                "centrifuge",
                CentrifugeScenes::processing
        );
        helper.addStoryBoard(
                id("reactor_controller"),
                "reactor_controller",
                ReactorScenes::assembly
        );
        // La turbina se ancla a todos sus componentes para que el ponder aparezca desde cualquiera de ellos.
        helper.addStoryBoard(id("turbine_output"), "turbine", TurbineScenes::assembly);
        helper.addStoryBoard(id("turbine_rotor"), "turbine", TurbineScenes::assembly);
        helper.addStoryBoard(id("turbine_casing"), "turbine", TurbineScenes::assembly);
        helper.addStoryBoard(id("turbine_fluid_port"), "turbine", TurbineScenes::assembly);
    }
}
