package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

/** Fluid tags; every area adds its own through {@link AreaDataGen#fluidTags}. */
public class NuclearFluidTagsProvider extends FluidTagsProvider {

    public NuclearFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        AreaDataGen.ALL.forEach(area -> area.fluidTags(this, provider));
    }

    /** {@link #tag} for the areas' datagen, which lives in other classes. */
    public IntrinsicTagAppender<Fluid> tagFor(TagKey<Fluid> tag) {
        return tag(tag);
    }
}
