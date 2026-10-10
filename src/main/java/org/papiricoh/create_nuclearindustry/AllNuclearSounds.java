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

    public static void init() {}
}
