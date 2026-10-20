package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.AcceleratorScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.AntimatterScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.CivilDefenseScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.ControlRoomScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.DefenseScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.FanTreatmentScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.FuelCycleScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.IsotopeScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.MaintenanceScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.PropulsionScenes;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.WeaponsScenes;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The guide's Ponder scenes: civil defense, weapons, the fuel cycle from ore to fuel, the two fan
 * treatments, the linac, the synchrotron, antimatter containment, the annihilation generator, the antiproton injector and the control room series (under its own tag). Registered by {@link AllNuclearPonderScenes} after its own scenes, each on the
 * items {@link GuidePonderText} lists for it (the fan treatments also on Create's Encased Fan).
 */
public final class GuidePonderScenes {
    private static final ResourceLocation CONTROL_ROOM_TAG = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID,
            GuidePonderText.CONTROL_ROOM_TAG_ID);
    private static final List<PlantPonderText.Scene> CONTROL_ROOM_SCENES = List.of(GuidePonderText.CONTROL_ROOM,
            GuidePonderText.CONTROL_MODULES, GuidePonderText.CONTROL_INSTRUMENTS, GuidePonderText.CONTROL_SIGNALS,
            GuidePonderText.CONTROL_SERVO);

    private GuidePonderScenes() {}

    /** Each scene's storyboard, in {@link GuidePonderText#SCENES} order. */
    private static Map<PlantPonderText.Scene, PonderStoryBoard> storyBoards() {
        Map<PlantPonderText.Scene, PonderStoryBoard> boards = new LinkedHashMap<>();
        boards.put(GuidePonderText.SIRENS, CivilDefenseScenes::sirens);
        boards.put(GuidePonderText.BLAST_DOOR, CivilDefenseScenes::blastDoor);
        boards.put(GuidePonderText.SHELTER, CivilDefenseScenes::shelter);
        boards.put(GuidePonderText.BOMBS, WeaponsScenes::bombs);
        boards.put(GuidePonderText.THERMONUCLEAR, WeaponsScenes::thermonuclear);
        boards.put(GuidePonderText.LAUNCH_BASE, WeaponsScenes::launchBase);
        boards.put(GuidePonderText.MISSILE, WeaponsScenes::missile);
        boards.put(GuidePonderText.SILO_LAUNCH, WeaponsScenes::siloLaunch);
        boards.put(GuidePonderText.RADAR, WeaponsScenes::radar);
        boards.put(GuidePonderText.HOLO_TABLE, WeaponsScenes::holoTable);
        boards.put(GuidePonderText.INTERCEPTOR_BATTERY, DefenseScenes::interceptorBattery);
        boards.put(GuidePonderText.INTERCEPTOR_SILO, DefenseScenes::interceptorSilo);
        boards.put(GuidePonderText.GROUND_STATION, org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.OrbitScenes::groundStation);
        boards.put(GuidePonderText.SATELLITE_LAUNCH, org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes.OrbitScenes::satelliteLaunch);
        boards.put(GuidePonderText.FUEL_CYCLE, FuelCycleScenes::oreToFuel);
        boards.put(GuidePonderText.ACID_LEACHING, FanTreatmentScenes::acidLeaching);
        boards.put(GuidePonderText.IRRADIATION, FanTreatmentScenes::irradiation);
        boards.put(GuidePonderText.ACCELERATOR_LINAC, AcceleratorScenes::linac);
        boards.put(GuidePonderText.ACCELERATOR_SYNCHROTRON, AcceleratorScenes::synchrotron);
        boards.put(GuidePonderText.ANTIMATTER_CONTAINMENT, AntimatterScenes::containment);
        boards.put(GuidePonderText.ANTIMATTER_GENERATOR, AntimatterScenes::generator);
        boards.put(GuidePonderText.ANTIPROTON_INJECTOR, AntimatterScenes::injector);
        boards.put(GuidePonderText.ISOTOPES, IsotopeScenes::isotopes);
        boards.put(GuidePonderText.MAINTENANCE, MaintenanceScenes::maintenance);
        boards.put(GuidePonderText.CONTROL_ROOM, ControlRoomScenes::controlRoom);
        boards.put(GuidePonderText.CONTROL_MODULES, ControlRoomScenes::modules);
        boards.put(GuidePonderText.CONTROL_INSTRUMENTS, ControlRoomScenes::instruments);
        boards.put(GuidePonderText.CONTROL_SIGNALS, ControlRoomScenes::signals);
        boards.put(GuidePonderText.CONTROL_SERVO, ControlRoomScenes::servo);
        boards.put(GuidePonderText.THERMAL_ENGINE, PropulsionScenes::thermal);
        boards.put(GuidePonderText.ANTIMATTER_ENGINE, PropulsionScenes::antimatter);
        return boards;
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        storyBoards().forEach((scene, board) -> {
            for (String component : scene.components()) {
                if (CONTROL_ROOM_SCENES.contains(scene)) {
                    helper.addStoryBoard(item(component), scene.schematic(), board, CONTROL_ROOM_TAG);
                } else {
                    helper.addStoryBoard(item(component), scene.schematic(), board);
                }
            }
        });
    }

    /** The control room series, under its own tag with the monitor as its icon. */
    public static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(CONTROL_ROOM_TAG)
                .title(GuidePonderText.CONTROL_ROOM_TAG_TITLE.english())
                .description(GuidePonderText.CONTROL_ROOM_TAG_DESCRIPTION.english())
                .item(ControlRoom2Registry.WALL_CONTROL_PANEL_ITEM.get(), true, false)
                .addToIndex()
                .register();
        var tag = helper.addToTag(CONTROL_ROOM_TAG);
        Set<String> components = new LinkedHashSet<>();
        CONTROL_ROOM_SCENES.forEach(scene -> components.addAll(scene.components()));
        components.forEach(component -> tag.add(item(component)));
    }

    /** An item of this mod by its path, or another mod's by its full id. */
    static ResourceLocation item(String component) {
        return component.contains(":") ? ResourceLocation.parse(component)
                : ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, component);
    }
}
