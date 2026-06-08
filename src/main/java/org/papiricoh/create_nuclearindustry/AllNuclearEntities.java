package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.fluids.blockentity.DualFluidPipeBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.TurbineOutputBlockEntity;

public class AllNuclearEntities {

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBlockEntity>> REACTOR =
            ENTITIES.register("reactor_controller", () -> BlockEntityType.Builder.of(
                    ReactorBlockEntity::new,
                    AllNuclearBlocks.REACTOR_CONTROLLER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DualFluidPipeBlockEntity>> DUAL_PIPE =
            ENTITIES.register("dual_fluid_pipe", () -> BlockEntityType.Builder.of(
                    DualFluidPipeBlockEntity::new,
                    AllNuclearBlocks.DUAL_FLUID_PIPE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE =
            ENTITIES.register("centrifuge", () -> BlockEntityType.Builder.of(
                    CentrifugeBlockEntity::new,
                    AllNuclearBlocks.CENTRIFUGE.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineOutputBlockEntity>> TURBINE_OUTPUT =
            ENTITIES.register("turbine_output", () -> BlockEntityType.Builder.of(
                    TurbineOutputBlockEntity::new,
                    AllNuclearBlocks.TURBINE_OUTPUT.get()
            ).build(null));

    public static void init() {}
}
