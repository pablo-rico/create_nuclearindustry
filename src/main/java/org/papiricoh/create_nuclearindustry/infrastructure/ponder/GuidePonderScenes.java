package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CivilDefenseScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.FanTreatmentScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.FuelCycleScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.WeaponsScenes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The guide's Ponder scenes: civil defense, weapons, the fuel cycle from ore to fuel and the two
 * fan treatments. Registered by {@link AllNuclearPonderScenes} after its own scenes, each on the
 * items {@link GuidePonderText} lists for it (the fan treatments also on Create's Encased Fan).
 */
public final class GuidePonderScenes {

    private GuidePonderScenes() {}

    /** Each scene's storyboard, in {@link GuidePonderText#SCENES} order. */
    private static Map<PlantPonderText.Scene, PonderStoryBoard> storyBoards() {
        Map<PlantPonderText.Scene, PonderStoryBoard> boards = new LinkedHashMap<>();
        boards.put(GuidePonderText.SIRENS, CivilDefenseScenes::sirens);
        boards.put(GuidePonderText.BLAST_DOOR, CivilDefenseScenes::blastDoor);
        boards.put(GuidePonderText.SHELTER, CivilDefenseScenes::shelter);
        boards.put(GuidePonderText.BOMBS, WeaponsScenes::bombs);
        boards.put(GuidePonderText.MISSILE, WeaponsScenes::missile);
        boards.put(GuidePonderText.FUEL_CYCLE, FuelCycleScenes::oreToFuel);
        boards.put(GuidePonderText.ACID_LEACHING, FanTreatmentScenes::acidLeaching);
        boards.put(GuidePonderText.IRRADIATION, FanTreatmentScenes::irradiation);
        return boards;
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        storyBoards().forEach((scene, board) -> {
            for (String component : scene.components()) {
                helper.addStoryBoard(item(component), scene.schematic(), board);
            }
        });
    }

    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {}

    /** An item of this mod by its path, or another mod's by its full id. */
    static ResourceLocation item(String component) {
        return component.contains(":") ? ResourceLocation.parse(component)
                : ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, component);
    }
}
