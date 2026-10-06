package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

public class NuclearParticleDescriptionProvider extends ParticleDescriptionProvider {
    public NuclearParticleDescriptionProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, fileHelper);
    }

    @Override
    protected void addDescriptions() {
        spriteSet(AllNuclearParticles.RADIOACTIVE_DUST.get(),
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "radioactive_dust"), 3, false);
    }
}
