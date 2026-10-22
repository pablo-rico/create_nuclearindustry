package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CentrifugeScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ContainmentDoorScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CoolingTowerScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CryoplantScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.PlantScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorModerationScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorRedstoneScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.RtgScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ScrubberScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.SprinklerScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.TokamakScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ReactorScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.TurbineScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.WasteStorageScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.WasteTransportScenes;

public class AllNuclearPonderScenes {
    /** The ponder tag of the "Nuclear power plant" series. */
    public static final ResourceLocation PLANT_TAG = id(PlantPonderText.TAG_ID);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(id("reactor_redstone_port"), "reactor_controller", ReactorRedstoneScenes::redstonePort);
        helper.addStoryBoard(id("reactor_controller"), "reactor_controller", ReactorRedstoneScenes::redstonePort);
        helper.addStoryBoard(id("radioactive_waste_barrel"), "waste_storage", WasteStorageScenes::storage);
        for (String container : new String[]{"radioactive_waste_cistern", "shielded_waste_cask"}) {
            helper.addStoryBoard(id(container), "waste_transport", WasteTransportScenes::transport);
        }
        helper.addStoryBoard(id("radiation_scrubber"), "centrifuge", ScrubberScenes::scrubber);
        for (String part : new String[]{"decontamination_sprinkler", "irradiated_sunflower", "irradiated_biomass"}) {
            helper.addStoryBoard(id(part), "centrifuge", SprinklerScenes::sprinkler);
        }
        for (String part : new String[]{"rtg", "plutonium_rtg_pellet", "waste_rtg_pellet"}) {
            helper.addStoryBoard(id(part), "centrifuge", RtgScenes::rtg);
        }
        for (String part : new String[]{"cooling_tower_controller", "cooling_tower_casing", "cooling_tower_fluid_port", "cooling_tower_fan"}) {
            helper.addStoryBoard(id(part), "cooling_tower", CoolingTowerScenes::tower);
        }
        helper.addStoryBoard(id("centrifuge"), "centrifuge_cascade", CentrifugeScenes::processing);
        helper.addStoryBoard(id("centrifuge"), "centrifuge_cascade", CentrifugeScenes::cascade);
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
        for (String part : new String[]{"fusion_controller", "fusion_magnet_input", "fusion_vacuum_pump", "fusion_cryo_port",
                "fusion_neutral_beam_injector", "fusion_rf_antenna", "fusion_viewport"}) {
            helper.addStoryBoard(id(part), "tokamak_startup", TokamakScenes::startup);
        }
        for (String part : new String[]{"cryoplant_controller", "cryoplant_casing", "cryoplant_port", "cryoplant_compressor",
                "fusion_cryo_port"}) {
            helper.addStoryBoard(id(part), "cryoplant", CryoplantScenes::plant);
        }
        for (String part : new String[]{"graphite_moderator", "beryllium_reflector", "reactor_controller"}) {
            helper.addStoryBoard(id(part), "reactor_controller", ReactorModerationScenes::moderation);
        }
        helper.addStoryBoard(id("electrolyzer"), "electrolyzer", CentrifugeScenes::electrolysis);
        // Turbine storyboards are reachable from every block they are built from.
        for (String part : new String[]{"turbine_output", "turbine_rotor", "turbine_casing", "turbine_fluid_port", "reinforced_turbine_glass"}) {
            helper.addStoryBoard(id(part), "turbine", TurbineScenes::steam);
        }
        for (String part : new String[]{"fusion_plasma_turbine", "fusion_turbine_rotor", "fusion_turbine_casing",
                "fusion_turbine_fluid_port", "reinforced_turbine_glass"}) {
            helper.addStoryBoard(id(part), "fusion_turbine", TurbineScenes::plasma);
        }
        registerPlantSeries(helper);
        // After the series, so a containment part shows the plant scenes first.
        for (String part : new String[]{"containment_door", "containment_concrete", "steel_liner"}) {
            helper.addStoryBoard(id(part), "containment_door", ContainmentDoorScenes::door);
        }
        GuidePonderScenes.register(helper);
    }

    /**
     * The "Nuclear power plant" series. Ponder chains the scenes of one item in the order they are
     * registered, so registering scene by scene keeps the series in order on every item that has more
     * than one of them; the hubs (reactor and steam generator controllers) show all seven, the overview first.
     */
    private static void registerPlantSeries(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        storyBoard(helper, PlantPonderText.OVERVIEW, PlantScenes::overview);
        storyBoard(helper, PlantPonderText.HEAT_LOOP, PlantScenes::heatLoop);
        storyBoard(helper, PlantPonderText.GENERATION, PlantScenes::generation);
        storyBoard(helper, PlantPonderText.CONTROL_SAFETY, PlantScenes::controlSafety);
        storyBoard(helper, PlantPonderText.WASTE, PlantScenes::waste);
        storyBoard(helper, PlantPonderText.SAFE_SHUTDOWN, PlantScenes::safeShutdown);
        storyBoard(helper, PlantPonderText.LOSS_OF_COOLING, PlantScenes::lossOfCooling);
    }

    private static void storyBoard(PonderSceneRegistrationHelper<ResourceLocation> helper, PlantPonderText.Scene scene,
                                   PonderStoryBoard board) {
        for (String component : PlantPonderText.componentsOf(scene)) {
            helper.addStoryBoard(id(component), scene.schematic(), board, PLANT_TAG);
        }
    }

    /** Puts every part of the plant under the series' tag, with the reactor controller as its icon. */
    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(PLANT_TAG)
                .title(PlantPonderText.TAG_TITLE.english())
                .description(PlantPonderText.TAG_DESCRIPTION.english())
                .item(AllNuclearBlocks.REACTOR_CONTROLLER.get(), true, false)
                .addToIndex()
                .register();
        var tag = helper.addToTag(PLANT_TAG);
        for (String component : PlantPonderText.tagComponents()) {
            tag.add(id(component));
        }
        GuidePonderScenes.registerTags(helper);
    }
}
