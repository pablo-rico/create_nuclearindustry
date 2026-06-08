package org.papiricoh.create_nuclearindustry;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllNuclearDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> ENRICHMENT =
            DATA_COMPONENTS.registerComponentType("enrichment", builder -> builder
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT));

    // Coordenadas de destino guardadas por el item designador del misil.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> TARGET_POS =
            DATA_COMPONENTS.registerComponentType("target_pos", builder -> builder
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    public static void init() {}
}
