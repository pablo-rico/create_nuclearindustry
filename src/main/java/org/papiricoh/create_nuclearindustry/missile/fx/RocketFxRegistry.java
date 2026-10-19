package org.papiricoh.create_nuclearindustry.missile.fx;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;

/**
 * The rocket effects' own particles and sounds: launch, engines, staging, re-entry and the falling stages, plus the radar's and
 * the command table's cues. Each sound is a file made by {@code tools/soundgen}, listed in {@code sounds.json}.
 */
public final class RocketFxRegistry implements NuclearFeature {
    public static final RocketFxRegistry INSTANCE = new RocketFxRegistry();

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Create_NuclearIndustry.MODID);

    /** The engines lighting at lift-off, heard later the further off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_IGNITION = sound("rocket_ignition");
    /** The engines' roar near the rocket, looped while it burns. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_ROAR = sound("rocket_roar");
    /** The engines' low rumble from afar, looped while it burns. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_ROAR_DISTANT = sound("rocket_roar_distant");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAGE_SEPARATION = sound("stage_separation");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONIC_BOOM = sound("sonic_boom");
    /** The air tearing past a warhead coming back in, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> REENTRY_HISS = sound("reentry_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEBRIS_IMPACT = sound("debris_impact");
    /** Steam venting from a silo in the last seconds of its countdown. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SILO_VENT = sound("silo_vent");
    /** The radar's sweep passing a contact. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_PING = sound("radar_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_NEW_TRACK = sound("radar_new_track");
    /** A threat to the defended zone not yet acknowledged, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_KLAXON = sound("radar_klaxon");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAR_ACK = sound("radar_ack");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLO_BLIP = sound("holo_blip");

    private RocketFxRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name)));
    }

    @Override
    public void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
        SOUNDS.register(modEventBus);
    }
}
