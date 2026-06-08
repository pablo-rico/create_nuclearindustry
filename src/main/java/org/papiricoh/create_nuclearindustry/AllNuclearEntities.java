package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.fluids.blockentity.DualFluidPipeBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorFuelPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorFluidPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.TurbineFluidPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.TurbineOutputBlockEntity;

public class AllNuclearEntities {

    public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBlockEntity>> REACTOR =
            ENTITIES.register("reactor_controller", () -> BlockEntityType.Builder.of(
                    ReactorBlockEntity::new,
                    AllNuclearBlocks.REACTOR_CONTROLLER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorFluidPortBlockEntity>> REACTOR_FLUID_PORT =
            ENTITIES.register("reactor_fluid_port", () -> BlockEntityType.Builder.of(
                    ReactorFluidPortBlockEntity::new,
                    AllNuclearBlocks.REACTOR_FLUID_PORT.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorFuelPortBlockEntity>> REACTOR_FUEL_PORT =
            ENTITIES.register("reactor_fuel_port", () -> BlockEntityType.Builder.of(
                    ReactorFuelPortBlockEntity::new,
                    AllNuclearBlocks.REACTOR_FUEL_PORT.get()
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineFluidPortBlockEntity>> TURBINE_FLUID_PORT =
            ENTITIES.register("turbine_fluid_port", () -> BlockEntityType.Builder.of(
                    TurbineFluidPortBlockEntity::new,
                    AllNuclearBlocks.TURBINE_FLUID_PORT.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearBombBlockEntity>> NUCLEAR_BOMB =
            ENTITIES.register("nuclear_bomb", () -> BlockEntityType.Builder.of(
                    NuclearBombBlockEntity::new,
                    AllNuclearBlocks.NUCLEAR_BOMB.get()
            ).build(null));

    public static void init() {}
}
