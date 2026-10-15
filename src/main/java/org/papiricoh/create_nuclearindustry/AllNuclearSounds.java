package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllNuclearSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION =
            SOUNDS.register("nuclear_explosion", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(
                            Create_NuclearIndustry.MODID,
                            "nuclear_explosion"
                    )));

    /** One steady siren tone, looped; sirens bend its pitch into a wail as it plays. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AIR_RAID_SIREN =
            SOUNDS.register("air_raid_siren", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(
                            Create_NuclearIndustry.MODID,
                            "air_raid_siren"
                    )));

    // ---------------------------------------------------------------- tokamak (sounds made by tools/soundgen)
    /** The coils' hum, looped; the tokamak raises its pitch and volume with the field. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_HUM = variableRange("magnet_hum");
    /** The burning plasma's low roar, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_RUMBLE = variableRange("plasma_rumble");
    /** The crack of the central solenoid pulse that breaks the gas down into plasma. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PLASMA_BREAKDOWN = variableRange("plasma_breakdown");
    /** The cryogenic plant's compressors, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOPLANT_HUM = variableRange("cryoplant_hum");

    // ---------------------------------------------------------------- fission reactor and its plant (sounds made by tools/soundgen)
    /** The running core's hum, looped; the reactor raises its pitch and volume with power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_HUM = variableRange("reactor_hum");
    /** The thud of the control rods dropping into the core on a SCRAM. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_SCRAM = variableRange("reactor_scram");
    /** The control room klaxon after a SCRAM, looped while the core is still hot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_KLAXON = variableRange("reactor_klaxon");
    /** The over-temperature alarm, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_OVERTEMP_ALARM = variableRange("reactor_overtemp_alarm");
    /** Steam rushing through an output port, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_HISS = variableRange("steam_hiss");
    /** Coolant boiling and bubbling in the core, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COOLANT_BUBBLING = variableRange("coolant_bubbling");
    /** The pressurizer's relief valve lifting. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RELIEF_VALVE_LIFT = variableRange("relief_valve_lift");

    // ---------------------------------------------------------------- fuel cycle machines (sounds made by tools/soundgen)
    /** A gas centrifuge spinning, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CENTRIFUGE_WHINE = variableRange("centrifuge_whine");
    /** The electrolyzer's cells, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTROLYZER_BUZZ = variableRange("electrolyzer_buzz");
    /** The vitrification melter's furnace, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MELTER_ROAR = variableRange("melter_roar");
    /** A radioisotope generator's thermocouples, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RTG_HUM = variableRange("rtg_hum");
    /** Acid eating into items in a fan's air current. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_SIZZLE = variableRange("acid_sizzle");
    /** Items crackling in a fan's air current past a radioactive block. */
    public static final DeferredHolder<SoundEvent, SoundEvent> IRRADIATION_CRACKLE = variableRange("irradiation_crackle");

    /** The accelerator's klystrons and cavities filling with radio-frequency power, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ACCELERATOR_RF_HUM = variableRange("accelerator_rf_hum");
    /** The accelerator's magnets ramping their field, looped; its pitch follows the beam energy. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_RAMP = variableRange("magnet_ramp");
    /** A superconducting magnet quenching: a bang and the hiss of venting helium. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGNET_QUENCH = variableRange("magnet_quench");
    /** The beam kicked into the dump. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAM_DUMP = variableRange("beam_dump");
    /** A personnel interlock tripping the beam. */
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERLOCK_BUZZER = variableRange("interlock_buzzer");
    /** A magnetic confinement vessel's field, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CONFINEMENT_HUM = variableRange("confinement_hum");
    /** The annihilation generator running, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATION_DRONE = variableRange("annihilation_drone");
    /** A Penning trap's field, looped; its pitch and volume follow the charge. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TRAP_HUM = variableRange("trap_hum");
    /** A loaded trap losing its field: the three-second quench alarm before it annihilates. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TRAP_QUENCH = variableRange("trap_quench");
    /** Antimatter annihilating all at once. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNIHILATION_BLAST = variableRange("annihilation_blast");
    /** An antiproton injector firing its dose into a tokamak's plasma. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANTIPROTON_SHOT = variableRange("antiproton_shot");

    // ---------------------------------------------------------------- control room (sounds made by tools/soundgen)
    /** An annunciator board's urgent horn, for an alarm window waiting for acknowledgement. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNUNCIATOR_ALARM = variableRange("annunciator_alarm");
    /** Its softer two-tone for a warning window. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNUNCIATOR_WARNING = variableRange("annunciator_warning");
    /** The ringback chime: an acknowledged condition has cleared and waits for a reset. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANNUNCIATOR_RINGBACK = variableRange("annunciator_ringback");

    private static DeferredHolder<SoundEvent, SoundEvent> variableRange(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name)));
    }

    public static void init() {}
}
