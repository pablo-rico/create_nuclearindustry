package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * Absolute pressure a block holds as part of a containment wall, in bar. Blocks without an entry
 * hold {@link #DEFAULT_SOLID} if they are a full block; blocks that cannot be broken never give way.
 */
public record ContainmentRating(float bar) {
    public static final Codec<ContainmentRating> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(1.0f, 10_000.0f).fieldOf("bar").forGetter(ContainmentRating::bar)
    ).apply(instance, ContainmentRating::new));

    /** Data map {@code create_nuclearindustry:containment_rating} on blocks. */
    public static final DataMapType<net.minecraft.world.level.block.Block, ContainmentRating> TYPE = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "containment_rating"), Registries.BLOCK, CODEC).build();

    /** An ordinary full block: stone, planks, a machine. */
    public static final double DEFAULT_SOLID = 1.5;

    public static double of(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getDestroySpeed(level, pos) < 0) {
            return Double.POSITIVE_INFINITY;
        }
        ContainmentRating entry = state.getBlockHolder().getData(TYPE);
        if (entry != null) {
            return entry.bar();
        }
        return DEFAULT_SOLID;
    }
}
