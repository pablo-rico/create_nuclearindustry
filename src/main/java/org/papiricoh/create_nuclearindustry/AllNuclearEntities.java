package org.papiricoh.create_nuclearindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.blockentity.LaunchPadBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;
import org.papiricoh.create_nuclearindustry.fluids.blockentity.DualFluidPipeBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionFluidPortBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionFuelInjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionMagnetInputBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionPlasmaTurbineBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorFuelPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorFluidPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorTemperatureSensorBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorTemperatureSensorBlockEntity>> REACTOR_TEMPERATURE_SENSOR =
            ENTITIES.register("reactor_temperature_sensor", () -> BlockEntityType.Builder.of(
                    ReactorTemperatureSensorBlockEntity::new,
                    AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get()
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD =
            ENTITIES.register("launch_pad", () -> BlockEntityType.Builder.of(
                    LaunchPadBlockEntity::new,
                    AllNuclearBlocks.LAUNCH_PAD.get()
            ).build(null));

    // ---- Fusion reactor block entities ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionReactorBlockEntity>> FUSION_CONTROLLER =
            ENTITIES.register("fusion_controller", () -> BlockEntityType.Builder.of(
                    FusionReactorBlockEntity::new,
                    AllNuclearBlocks.FUSION_CONTROLLER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionFluidPortBlockEntity>> FUSION_FLUID_PORT =
            ENTITIES.register("fusion_fluid_port", () -> BlockEntityType.Builder.of(
                    FusionFluidPortBlockEntity::new,
                    AllNuclearBlocks.FUSION_FLUID_PORT.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionFuelInjectorBlockEntity>> FUSION_FUEL_INJECTOR =
            ENTITIES.register("fusion_fuel_injector", () -> BlockEntityType.Builder.of(
                    FusionFuelInjectorBlockEntity::new,
                    AllNuclearBlocks.FUSION_FUEL_INJECTOR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionMagnetInputBlockEntity>> FUSION_MAGNET_INPUT =
            ENTITIES.register("fusion_magnet_input", () -> BlockEntityType.Builder.of(
                    FusionMagnetInputBlockEntity::new,
                    AllNuclearBlocks.FUSION_MAGNET_INPUT.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionPlasmaTurbineBlockEntity>> FUSION_PLASMA_TURBINE =
            ENTITIES.register("fusion_plasma_turbine", () -> BlockEntityType.Builder.of(
                    FusionPlasmaTurbineBlockEntity::new,
                    AllNuclearBlocks.FUSION_PLASMA_TURBINE.get()
            ).build(null));

    // Entidades (no block-entities): el misil ICBM volador.
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MissileEntity>> MISSILE =
            ENTITY_TYPES.register("missile", () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.6f, 2.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("missile"));

    public static void init() {}
}
