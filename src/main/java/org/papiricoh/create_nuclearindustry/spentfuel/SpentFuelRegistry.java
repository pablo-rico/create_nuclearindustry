package org.papiricoh.create_nuclearindustry.spentfuel;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearIngredients;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.spentfuel.block.DryCaskBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.block.DryCaskBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.block.PoolCoolerBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.block.PoolCoolerBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlock;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlockEntity;

/** Content for spent fuel cooling: pool racks and dry casks: blocks, items and block entities register here. */
public final class SpentFuelRegistry implements NuclearFeature {
    public static final SpentFuelRegistry INSTANCE = new SpentFuelRegistry();

    /** Cooling history of a spent assembly. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpentFuelData>> SPENT_FUEL =
            AllNuclearDataComponents.DATA_COMPONENTS.registerComponentType("spent_fuel", builder -> builder
                    .persistent(SpentFuelData.CODEC)
                    .networkSynchronized(SpentFuelData.STREAM_CODEC));

    public static final DeferredHolder<IngredientType<?>, IngredientType<SpentFuelGradeIngredient>> GRADE_INGREDIENT =
            AllNuclearIngredients.INGREDIENT_TYPES.register("spent_fuel_grade", () -> new IngredientType<>(SpentFuelGradeIngredient.CODEC));

    public static final DeferredHolder<Block, SpentFuelRackBlock> SPENT_FUEL_RACK = AllNuclearBlocks.BLOCKS.register("spent_fuel_rack",
            () -> new SpentFuelRackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(SpentFuelRackBlock::lightLevel)));
    public static final DeferredHolder<Block, PoolCoolerBlock> POOL_COOLER = AllNuclearBlocks.BLOCKS.register("pool_cooler",
            () -> new PoolCoolerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, DryCaskBlock> DRY_CASK = AllNuclearBlocks.BLOCKS.register("dry_cask",
            () -> new DryCaskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0f, 1200.0f)
                    .sound(SoundType.STONE).requiresCorrectToolForDrops()));

    public static final DeferredHolder<Item, BlockItem> SPENT_FUEL_RACK_ITEM = AllNuclearItems.ITEMS.register("spent_fuel_rack",
            () -> new BlockItem(SPENT_FUEL_RACK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> POOL_COOLER_ITEM = AllNuclearItems.ITEMS.register("pool_cooler",
            () -> new BlockItem(POOL_COOLER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DRY_CASK_ITEM = AllNuclearItems.ITEMS.register("dry_cask",
            () -> new BlockItem(DRY_CASK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpentFuelRackBlockEntity>> SPENT_FUEL_RACK_ENTITY =
            AllNuclearEntities.ENTITIES.register("spent_fuel_rack", () -> BlockEntityType.Builder.of(
                    SpentFuelRackBlockEntity::new, SPENT_FUEL_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoolCoolerBlockEntity>> POOL_COOLER_ENTITY =
            AllNuclearEntities.ENTITIES.register("pool_cooler", () -> BlockEntityType.Builder.of(
                    PoolCoolerBlockEntity::new, POOL_COOLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryCaskBlockEntity>> DRY_CASK_ENTITY =
            AllNuclearEntities.ENTITIES.register("dry_cask", () -> BlockEntityType.Builder.of(
                    DryCaskBlockEntity::new, DRY_CASK.get()).build(null));

    private SpentFuelRegistry() {}

    @Override
    public void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(SpentFuelHooks::onPlayerTick);
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SPENT_FUEL_RACK_ENTITY.get(), (rack, side) -> rack.getInventory());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DRY_CASK_ENTITY.get(), (cask, side) -> cask.getInventory());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(SPENT_FUEL_RACK_ITEM.get());
        output.accept(POOL_COOLER_ITEM.get());
        output.accept(DRY_CASK_ITEM.get());
    }
}
