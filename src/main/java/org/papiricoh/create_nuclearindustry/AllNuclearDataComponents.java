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

    /** A target designator set to airburst: the missile goes off above its target. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AIRBURST =
            DATA_COMPONENTS.registerComponentType("airburst", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));
    /** The fissile load of a nuclear cannon shell: what its warhead is worth when it goes off. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<org.papiricoh.create_nuclearindustry.explosive.WarheadLoad>> WARHEAD_LOAD =
            DATA_COMPONENTS.registerComponentType("warhead_load", builder -> builder
                    .persistent(org.papiricoh.create_nuclearindustry.explosive.WarheadLoad.CODEC)
                    .networkSynchronized(org.papiricoh.create_nuclearindustry.explosive.WarheadLoad.STREAM_CODEC));
    // Coordenadas de destino guardadas por el item designador del misil.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> TARGET_POS =
            DATA_COMPONENTS.registerComponentType("target_pos", builder -> builder
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC));

    /** What a Control Room Linker points at: a machine's controller, or any block for its radiation. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> LINKED_TARGET =
            DATA_COMPONENTS.registerComponentType("linked_target", builder -> builder
                    .persistent(net.minecraft.core.GlobalPos.CODEC)
                    .networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));

    /** Fraction of an RTG pellet's heat left, 1 when fresh. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> RTG_CHARGE =
            DATA_COMPONENTS.registerComponentType("rtg_charge", builder -> builder
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT));

    /** Antimatter in a vial, in nanograms; absent on an empty one, so empty vials stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> ANTIMATTER =
            DATA_COMPONENTS.registerComponentType("antimatter", builder -> builder
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE));

    /** Ticks of field a vial's battery has left; absent on an empty vial. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VIAL_CHARGE =
            DATA_COMPONENTS.registerComponentType("vial_charge", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static void init() {}
}
