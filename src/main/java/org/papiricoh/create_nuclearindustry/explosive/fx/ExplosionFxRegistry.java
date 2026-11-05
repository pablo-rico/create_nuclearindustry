package org.papiricoh.create_nuclearindustry.explosive.fx;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;

/**
 * The particles, sounds and effects every explosion shares, nuclear or not: the fireball's flames and smoke, the dust and
 * debris the blast throws up, ash and black rain falling from a cloud, and the shock's sounds layered by distance. Each sound is
 * a file made by {@code tools/soundgen}, listed in {@code sounds.json}; each particle's sprites are made by
 * {@code tools/texturegen}.
 */
public final class ExplosionFxRegistry implements NuclearFeature {
    public static final ExplosionFxRegistry INSTANCE = new ExplosionFxRegistry();

    /** The volume slider every explosion plays under, as vanilla TNT does. */
    public static final SoundSource CATEGORY = SoundSource.BLOCKS;
    /** The ringing in a deafened player's ears. */
    public static final SoundSource TINNITUS_CATEGORY = SoundSource.PLAYERS;

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Create_NuclearIndustry.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Create_NuclearIndustry.MODID);

    /** A flake of ash falling from a cloud's fallout. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUKE_ASH = simple("nuke_ash");
    /** A drop of black, sooty rain falling under a cloud. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLACK_RAIN = simple("black_rain");
    /** Dark smoke rising from a fire the blast or the flash lit. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_SMOKE = simple("fire_smoke");
    /** A tongue of the fireball's flame. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREBALL_FLAME = simple("fireball_flame");
    /** A hot fragment of casing flying out of a blast. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHRAPNEL_SPARK = simple("shrapnel_spark");
    /** A wisp of the condensation cloud the shock leaves in damp air. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CONDENSATION_MIST = simple("condensation_mist");
    /** Dust the shock wave raises off the ground, tinted by the block it rose from. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<BlockParticleOption>> BLAST_DUST = block("blast_dust");
    /** A chunk of the ground thrown out of a crater, tinted by the block it came from. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<BlockParticleOption>> DEBRIS_CHUNK = block("debris_chunk");

    /** The first sharp crack of a nuclear shock front close by. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_CRACK = sound("nuke_crack");
    /** The deep boom of a nuclear shock front. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_BOOM = sound("nuke_boom");
    /** The long rolling rumble after a nuclear boom. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_RUMBLE = sound("nuke_rumble");
    /** A nuclear detonation heard from far off: a low, long roll with no crack. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_RUMBLE_FAR = sound("nuke_rumble_far");
    /** The wind rushing back in towards the rising fireball. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUKE_RETURN_WIND = sound("nuke_return_wind");
    /** The ground's tremor, felt before the shock arrives. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEISMIC_RUMBLE = sound("seismic_rumble");
    /** Lightning crackling inside a young mushroom cloud. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOUD_LIGHTNING = sound("cloud_lightning");
    /** A small conventional blast, up to about 8 kg. */
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_SMALL = sound("explosion_small");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_MEDIUM = sound("explosion_medium");
    /** A large conventional blast, from about 60 kg. */
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_LARGE = sound("explosion_large");
    /** A conventional blast heard from far off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_DISTANT = sound("explosion_distant");
    /** Debris pattering down after a blast. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DEBRIS_RAIN = sound("debris_rain");
    /** A thermobaric cloud igniting: a long pressure whump after the air rushes in. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THERMOBARIC_BLAST = sound("thermobaric_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> THERMOBARIC_DISTANT = sound("thermobaric_distant");
    /** A cluster warhead's casing blown open to free its bomblets. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLUSTER_OPEN = sound("cluster_open");
    /** A cluster warhead's bomblets going off one after another. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CLUSTER_CRACKLE = sound("cluster_crackle");
    /** The ringing in a deafened player's ears, looped. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TINNITUS = sound("tinnitus");

    /** Dazzled by a fireball: the screen washes out and an afterimage lingers. */
    public static final DeferredHolder<MobEffect, BlastSenseEffect> FLASH_BLINDED =
            EFFECTS.register("flash_blinded", () -> new BlastSenseEffect(0xFFF2C8));
    /** Deafened by a shock wave: the world goes muffled and the ears ring. */
    public static final DeferredHolder<MobEffect, BlastSenseEffect> DEAFENED =
            EFFECTS.register("deafened", () -> new BlastSenseEffect(0x6F6A64));

    private ExplosionFxRegistry() {}

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> simple(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(true));
    }

    private static DeferredHolder<ParticleType<?>, ParticleType<BlockParticleOption>> block(String name) {
        return PARTICLES.register(name, () -> new ParticleType<BlockParticleOption>(true) {
            @Override
            public MapCodec<BlockParticleOption> codec() {
                return BlockParticleOption.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() {
                return BlockParticleOption.streamCodec(this);
            }
        });
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name)));
    }

    @Override
    public void register(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
        SOUNDS.register(modEventBus);
        EFFECTS.register(modEventBus);
    }
}
