package org.papiricoh.create_nuclearindustry.fusion.cryo;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantCompressorBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantCompressorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionCryoPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionCryoPortBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionVacuumPumpBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionVacuumPumpBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.cryo.well.HeliumWellBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.well.HeliumWellBlockEntity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Content for the helium well, the cryogenic plant, the tokamak's vacuum pumps and cryogenic ports: blocks, items and block entities register here. */
public final class CryoRegistry implements NuclearFeature {
    public static final CryoRegistry INSTANCE = new CryoRegistry();

    // ---------------------------------------------------------------- cryogenic plant
    public static final DeferredHolder<Block, CryoplantControllerBlock> CRYOPLANT_CONTROLLER = block("cryoplant_controller",
            CryoplantControllerBlock::new, steel().lightLevel(state -> state.getValue(CryoplantControllerBlock.RUNNING) ? 6 : 0));
    public static final DeferredHolder<Block, Block> CRYOPLANT_CASING = block("cryoplant_casing", Block::new, steel());
    public static final DeferredHolder<Block, CryoplantPortBlock> CRYOPLANT_PORT = block("cryoplant_port", CryoplantPortBlock::new, steel());
    public static final DeferredHolder<Block, CryoplantCompressorBlock> CRYOPLANT_COMPRESSOR = block("cryoplant_compressor",
            CryoplantCompressorBlock::new, steel().noOcclusion());

    // ---------------------------------------------------------------- helium well (the chain's first helium)
    public static final DeferredHolder<Block, HeliumWellBlock> HELIUM_WELL = block("helium_well", HeliumWellBlock::new,
            steel().noOcclusion());

    // ---------------------------------------------------------------- tokamak shell parts
    public static final DeferredHolder<Block, FusionCryoPortBlock> FUSION_CRYO_PORT = block("fusion_cryo_port", FusionCryoPortBlock::new,
            fusion());
    public static final DeferredHolder<Block, FusionVacuumPumpBlock> FUSION_VACUUM_PUMP = block("fusion_vacuum_pump",
            FusionVacuumPumpBlock::new, fusion().noOcclusion());

    public static final DeferredHolder<Item, BlockItem> CRYOPLANT_CONTROLLER_ITEM = item(CRYOPLANT_CONTROLLER);
    public static final DeferredHolder<Item, BlockItem> CRYOPLANT_CASING_ITEM = item(CRYOPLANT_CASING);
    public static final DeferredHolder<Item, BlockItem> CRYOPLANT_PORT_ITEM = item(CRYOPLANT_PORT);
    public static final DeferredHolder<Item, BlockItem> CRYOPLANT_COMPRESSOR_ITEM = item(CRYOPLANT_COMPRESSOR);
    public static final DeferredHolder<Item, BlockItem> FUSION_CRYO_PORT_ITEM = item(FUSION_CRYO_PORT);
    public static final DeferredHolder<Item, BlockItem> FUSION_VACUUM_PUMP_ITEM = item(FUSION_VACUUM_PUMP);
    public static final DeferredHolder<Item, BlockItem> HELIUM_WELL_ITEM = item(HELIUM_WELL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoplantControllerBlockEntity>> CRYOPLANT_CONTROLLER_ENTITY =
            AllNuclearEntities.ENTITIES.register("cryoplant_controller", () -> BlockEntityType.Builder.of(
                    CryoplantControllerBlockEntity::new, CRYOPLANT_CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoplantPortBlockEntity>> CRYOPLANT_PORT_ENTITY =
            AllNuclearEntities.ENTITIES.register("cryoplant_port", () -> BlockEntityType.Builder.of(
                    CryoplantPortBlockEntity::new, CRYOPLANT_PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CryoplantCompressorBlockEntity>> CRYOPLANT_COMPRESSOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("cryoplant_compressor", () -> BlockEntityType.Builder.of(
                    CryoplantCompressorBlockEntity::new, CRYOPLANT_COMPRESSOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionCryoPortBlockEntity>> FUSION_CRYO_PORT_ENTITY =
            AllNuclearEntities.ENTITIES.register("fusion_cryo_port", () -> BlockEntityType.Builder.of(
                    FusionCryoPortBlockEntity::new, FUSION_CRYO_PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionVacuumPumpBlockEntity>> FUSION_VACUUM_PUMP_ENTITY =
            AllNuclearEntities.ENTITIES.register("fusion_vacuum_pump", () -> BlockEntityType.Builder.of(
                    FusionVacuumPumpBlockEntity::new, FUSION_VACUUM_PUMP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeliumWellBlockEntity>> HELIUM_WELL_ENTITY =
            AllNuclearEntities.ENTITIES.register("helium_well", () -> BlockEntityType.Builder.of(
                    HeliumWellBlockEntity::new, HELIUM_WELL.get()).build(null));

    /** Every block of the cryogenic plant: they all re-form it when placed. */
    public static final List<Supplier<? extends Block>> PLANT_PARTS = List.of(CRYOPLANT_CONTROLLER, CRYOPLANT_CASING,
            CRYOPLANT_PORT, CRYOPLANT_COMPRESSOR);
    /** The tokamak shell parts of this area: they re-form the tokamak when placed. */
    public static final List<Supplier<? extends Block>> TOKAMAK_PARTS = List.of(FUSION_CRYO_PORT, FUSION_VACUUM_PUMP);

    private CryoRegistry() {}

    private static BlockBehaviour.Properties steel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHERITE_BLOCK);
    }

    private static BlockBehaviour.Properties fusion() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0f, 12.0f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHERITE_BLOCK);
    }

    private static <B extends Block> DeferredHolder<Block, B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                    BlockBehaviour.Properties properties) {
        return AllNuclearBlocks.BLOCKS.register(id, () -> factory.apply(properties));
    }

    private static DeferredHolder<Item, BlockItem> item(DeferredHolder<Block, ? extends Block> block) {
        return AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CRYOPLANT_PORT_ENTITY.get(),
                (port, direction) -> port.getFluidHandler(direction));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUSION_CRYO_PORT_ENTITY.get(),
                (port, direction) -> port.getFluidHandler(direction));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HELIUM_WELL_ENTITY.get(),
                (well, direction) -> well.getFluidHandler(direction));
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.CRYOGENICS);
        output.accept(HELIUM_WELL_ITEM.get());
        output.accept(CRYOPLANT_CONTROLLER_ITEM.get());
        output.accept(CRYOPLANT_CASING_ITEM.get());
        output.accept(CRYOPLANT_PORT_ITEM.get());
        output.accept(CRYOPLANT_COMPRESSOR_ITEM.get());
        output.accept(FUSION_CRYO_PORT_ITEM.get());
        output.accept(FUSION_VACUUM_PUMP_ITEM.get());
    }
}
