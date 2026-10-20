package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;

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
    }
}
