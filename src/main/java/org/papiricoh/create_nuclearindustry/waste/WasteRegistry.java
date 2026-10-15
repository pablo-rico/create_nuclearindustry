package org.papiricoh.create_nuclearindustry.waste;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryInspectorBlock;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryInspectorBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.repository.RepositoryInspectorDisplaySource;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationMelterBlock;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationMelterBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.vitrification.WasteRecipeTypes;

/**
 * Content for waste classes, treatments and the geological repository: blocks, items and block entities register here.
 * Waste comes in three classes (low, intermediate, high level), each with its treatment into a
 * {@link WastePackage}: compacted drums, cemented drums and vitrified canisters.
 */
public final class WasteRegistry implements NuclearFeature {
    public static final WasteRegistry INSTANCE = new WasteRegistry();

    // Waste classes. Intermediate-level waste is the old RADIOACTIVE_WASTE item, which keeps its id.
    public static final DeferredHolder<Item, Item> LOW_LEVEL_WASTE = AllNuclearItems.ITEMS.register("low_level_waste",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HIGH_LEVEL_WASTE = AllNuclearItems.ITEMS.register("high_level_waste",
            () -> new Item(new Item.Properties().stacksTo(16)));

    // Waste packages.
    public static final DeferredHolder<Block, WastePackageBlock> COMPACTED_WASTE_DRUM = AllNuclearBlocks.BLOCKS.register("compacted_waste_drum",
            () -> new WastePackageBlock(drum(MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredHolder<Block, WastePackageBlock> CEMENTED_WASTE_DRUM = AllNuclearBlocks.BLOCKS.register("cemented_waste_drum",
            () -> new WastePackageBlock(drum(MapColor.STONE)));
    public static final DeferredHolder<Block, WastePackageBlock> VITRIFIED_WASTE_CANISTER = AllNuclearBlocks.BLOCKS.register("vitrified_waste_canister",
            () -> new WastePackageBlock(drum(MapColor.METAL).strength(6.0f, 18.0f)));

    // Repository.
    public static final DeferredHolder<Block, Block> BENTONITE = AllNuclearBlocks.BLOCKS.register("bentonite",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.8f).sound(SoundType.GRAVEL)));
    public static final DeferredHolder<Block, RepositoryInspectorBlock> REPOSITORY_INSPECTOR = AllNuclearBlocks.BLOCKS.register("repository_inspector",
            () -> new RepositoryInspectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops()));

    // Treatment.
    public static final DeferredHolder<Block, VitrificationMelterBlock> VITRIFICATION_MELTER = AllNuclearBlocks.BLOCKS.register("vitrification_melter",
            () -> new VitrificationMelterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 9.0f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Item, BlockItem> COMPACTED_WASTE_DRUM_ITEM = blockItem("compacted_waste_drum", COMPACTED_WASTE_DRUM, 64);
    public static final DeferredHolder<Item, BlockItem> CEMENTED_WASTE_DRUM_ITEM = blockItem("cemented_waste_drum", CEMENTED_WASTE_DRUM, 64);
    public static final DeferredHolder<Item, BlockItem> VITRIFIED_WASTE_CANISTER_ITEM = blockItem("vitrified_waste_canister", VITRIFIED_WASTE_CANISTER, 16);
    public static final DeferredHolder<Item, BlockItem> BENTONITE_ITEM = blockItem("bentonite", BENTONITE, 64);
    public static final DeferredHolder<Item, BlockItem> REPOSITORY_INSPECTOR_ITEM = blockItem("repository_inspector", REPOSITORY_INSPECTOR, 64);
    public static final DeferredHolder<Item, BlockItem> VITRIFICATION_MELTER_ITEM = blockItem("vitrification_melter", VITRIFICATION_MELTER, 64);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VitrificationMelterBlockEntity>> VITRIFICATION_MELTER_ENTITY =
            AllNuclearEntities.ENTITIES.register("vitrification_melter", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new VitrificationMelterBlockEntity(WasteRegistry.VITRIFICATION_MELTER_ENTITY.get(), pos, state),
                    VITRIFICATION_MELTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RepositoryInspectorBlockEntity>> REPOSITORY_INSPECTOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("repository_inspector", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RepositoryInspectorBlockEntity(WasteRegistry.REPOSITORY_INSPECTOR_ENTITY.get(), pos, state),
                    REPOSITORY_INSPECTOR.get()).build(null));

    public static final DeferredHolder<DisplaySource, RepositoryInspectorDisplaySource> REPOSITORY_INSPECTOR_SOURCE =
            AllNuclearDisplaySources.DISPLAY_SOURCES.register("repository_inspector", RepositoryInspectorDisplaySource::new);

    private WasteRegistry() {}

    private static BlockBehaviour.Properties drum(MapColor colour) {
        return BlockBehaviour.Properties.of().mapColor(colour).strength(4.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Item, BlockItem> blockItem(String id, DeferredHolder<Block, ? extends Block> block, int stack) {
        return AllNuclearItems.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(stack)));
    }

    @Override
    public void register(IEventBus modEventBus) {
        WasteRecipeTypes.register(modEventBus);
        modEventBus.addListener(WasteRegistry::registerDataMaps);
        modEventBus.addListener(WasteRegistry::commonSetup);
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(WastePackage.DATA_MAP);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DisplaySource.BY_BLOCK_ENTITY.add(REPOSITORY_INSPECTOR_ENTITY.get(), REPOSITORY_INSPECTOR_SOURCE.get()));
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, VITRIFICATION_MELTER_ENTITY.get(),
                (melter, direction) -> melter.getAutomationHandler());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.SPENT_FUEL);
        output.accept(LOW_LEVEL_WASTE.get());
        output.accept(HIGH_LEVEL_WASTE.get());
        output.accept(COMPACTED_WASTE_DRUM_ITEM.get());
        output.accept(CEMENTED_WASTE_DRUM_ITEM.get());
        output.accept(VITRIFICATION_MELTER_ITEM.get());
        output.accept(VITRIFIED_WASTE_CANISTER_ITEM.get());
        output.accept(BENTONITE_ITEM.get());
        output.accept(REPOSITORY_INSPECTOR_ITEM.get());
    }
}
