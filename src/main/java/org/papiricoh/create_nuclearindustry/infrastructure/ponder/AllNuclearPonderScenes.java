package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CentrifugeScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CoolingTowerScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorModerationScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorRedstoneScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.RtgScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ScrubberScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.TokamakScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.TurbineScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.WasteStorageScenes;

public class AllNuclearPonderScenes {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(id("reactor_redstone_port"), "reactor_controller", ReactorRedstoneScenes::redstonePort);
        helper.addStoryBoard(id("reactor_controller"), "reactor_controller", ReactorRedstoneScenes::redstonePort);
        helper.addStoryBoard(id("radioactive_waste_barrel"), "waste_storage", WasteStorageScenes::storage);
        helper.addStoryBoard(id("radiation_scrubber"), "centrifuge", ScrubberScenes::scrubber);
        for (String part : new String[]{"rtg", "plutonium_rtg_pellet", "waste_rtg_pellet"}) {
            helper.addStoryBoard(id(part), "centrifuge", RtgScenes::rtg);
        }
        for (String part : new String[]{"cooling_tower_controller", "cooling_tower_casing", "cooling_tower_fluid_port", "cooling_tower_fan"}) {
            helper.addStoryBoard(id(part), "cooling_tower", CoolingTowerScenes::tower);
        }
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
        for (String part : new String[]{"graphite_moderator", "beryllium_reflector", "reactor_controller"}) {
            helper.addStoryBoard(id(part), "reactor_controller", ReactorModerationScenes::moderation);
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
