package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AllNuclearParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Create_NuclearIndustry.MODID);

    /** Green dust drifting off contaminated ground. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RADIOACTIVE_DUST =
            PARTICLE_TYPES.register("radioactive_dust", () -> new SimpleParticleType(false));

    /** Pale wisps of hydrogen gathering under the roof of a reactor building or a containment. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HYDROGEN_GAS =
            PARTICLE_TYPES.register("hydrogen_gas", () -> new SimpleParticleType(false));

    private AllNuclearParticles() {}

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
