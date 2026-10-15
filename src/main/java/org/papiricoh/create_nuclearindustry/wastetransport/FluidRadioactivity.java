package org.papiricoh.create_nuclearindustry.wastetransport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

/**
 * How strongly a bucket of a fluid irradiates, in rad/s, from the {@code fluid_radioactivity}
 * data map. Fluids without an entry fall back to the {@code radioactivity} of their bucket.
 */
public record FluidRadioactivity(float perBucket) {
    public static final Codec<FluidRadioactivity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("per_bucket").forGetter(FluidRadioactivity::perBucket)
    ).apply(instance, FluidRadioactivity::new));

    /** Synced so goggles can work out a cistern's activity on the client. */
    public static final DataMapType<Fluid, FluidRadioactivity> DATA_MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "fluid_radioactivity"), Registries.FLUID, CODEC)
            .synced(CODEC, false).build();

    /** Dose rate of one bucket of {@code fluid}; 0 for empty or unknown fluids. */
    public static double perBucket(Fluid fluid) {
        @SuppressWarnings("deprecation")
        FluidRadioactivity entry = fluid.builtInRegistryHolder().getData(DATA_MAP);
        if (entry != null) {
            return entry.perBucket();
        }
        Item bucket = fluid.getBucket();
        Radioactivity radioactivity = bucket == null ? null : bucket.builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        return radioactivity == null ? 0 : radioactivity.perSecond();
    }

    /** Bare activity of {@code stack}: its buckets times the dose rate of one. */
    public static double of(FluidStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return stack.getAmount() / 1000.0 * perBucket(stack.getFluid());
    }
}
