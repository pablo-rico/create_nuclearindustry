package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CentrifugeScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.TokamakScenes;
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
        // The tokamak storyboard is reachable from every block it is built from.
        for (String part : new String[]{"fusion_controller", "fusion_central_solenoid", "fusion_accelerator_segment",
                "fusion_electromagnet", "fusion_breeding_blanket", "fusion_cryostat_casing", "fusion_magnet_input",
                "fusion_fluid_port", "fusion_fuel_injector"}) {
            helper.addStoryBoard(id(part), "tokamak", TokamakScenes::assembly);
        }
        helper.addStoryBoard(id("electrolyzer"), "centrifuge", CentrifugeScenes::electrolysis);
        // Turbine storyboards are reachable from every block they are built from.
        for (String part : new String[]{"turbine_output", "turbine_rotor", "turbine_casing", "turbine_fluid_port", "reinforced_turbine_glass"}) {
            helper.addStoryBoard(id(part), "turbine", TurbineScenes::steam);
        }
        for (String part : new String[]{"fusion_plasma_turbine", "fusion_turbine_rotor", "fusion_turbine_casing",
                "fusion_turbine_fluid_port", "reinforced_turbine_glass"}) {
            helper.addStoryBoard(id(part), "fusion_turbine", TurbineScenes::plasma);
        }
    }
}
