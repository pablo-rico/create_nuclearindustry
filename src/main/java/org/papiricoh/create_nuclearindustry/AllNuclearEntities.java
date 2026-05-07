package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.blockentity.DualFluidPipeBlockEntity;
import org.papiricoh.create_nuclearindustry.blockentity.ReactorBlockEntity;

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

    public static void init() {}
}
