package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;

public class NuclearParticleDescriptionProvider extends ParticleDescriptionProvider {
    public NuclearParticleDescriptionProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        spriteSet(AllNuclearParticles.RADIOACTIVE_DUST.get(),
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "radioactive_dust"), 3, false);
        spriteSet(AllNuclearParticles.HYDROGEN_GAS.get(),
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "hydrogen_gas"), 3, false);
        spriteSet(RocketFxRegistry.ROCKET_SMOKE.get(),
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "rocket_smoke"), 4, false);
        spriteSet(RocketFxRegistry.ROCKET_SMOKE_DENSE.get(),
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "rocket_smoke_dense"), 4, false);
        spriteSet(RocketFxRegistry.EMBER.get(),
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "ember"), 2, false);
        explosion(ExplosionFxRegistry.NUKE_ASH.get(), "nuke_ash", 4);
        explosion(ExplosionFxRegistry.BLACK_RAIN.get(), "black_rain", 2);
        explosion(ExplosionFxRegistry.FIRE_SMOKE.get(), "fire_smoke", 4);
        explosion(ExplosionFxRegistry.FIREBALL_FLAME.get(), "fireball_flame", 4);
        explosion(ExplosionFxRegistry.SHRAPNEL_SPARK.get(), "shrapnel_spark", 2);
        explosion(ExplosionFxRegistry.CONDENSATION_MIST.get(), "condensation_mist", 2);
        explosion(ExplosionFxRegistry.BLAST_DUST.get(), "blast_dust", 4);
        explosion(ExplosionFxRegistry.DEBRIS_CHUNK.get(), "debris_chunk", 4);
    }

    private void explosion(net.minecraft.core.particles.ParticleType<?> type, String name, int sprites) {
        spriteSet(type, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name), sprites, false);
    }
}
