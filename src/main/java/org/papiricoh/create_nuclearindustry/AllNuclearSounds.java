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

    private static DeferredHolder<SoundEvent, SoundEvent> variableRange(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name)));
    }

    public static void init() {}
}
