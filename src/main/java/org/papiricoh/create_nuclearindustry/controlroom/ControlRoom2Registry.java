package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlock;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartData;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlock;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRollItem;

/**
 * Content for the chart recorder, the annunciator panel and automatic rod control. The controller
 * module item comes with {@link org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType#CONTROLLER},
 * like every other module.
 */
public final class ControlRoom2Registry implements NuclearFeature {
    public static final ControlRoom2Registry INSTANCE = new ControlRoom2Registry();

    public static final DeferredHolder<Block, ChartRecorderBlock> CHART_RECORDER = AllNuclearBlocks.BLOCKS.register("chart_recorder",
            () -> new ChartRecorderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)));
    public static final DeferredHolder<Block, AnnunciatorPanelBlock> ANNUNCIATOR_PANEL = AllNuclearBlocks.BLOCKS.register("annunciator_panel",
            () -> new AnnunciatorPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)
                    .lightLevel(state -> 3)));

    public static final DeferredHolder<Item, BlockItem> CHART_RECORDER_ITEM = AllNuclearItems.ITEMS.register("chart_recorder",
            () -> new BlockItem(CHART_RECORDER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ANNUNCIATOR_PANEL_ITEM = AllNuclearItems.ITEMS.register("annunciator_panel",
            () -> new BlockItem(ANNUNCIATOR_PANEL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, ChartRollItem> CHART_ROLL = AllNuclearItems.ITEMS.register("chart_roll",
            () -> new ChartRollItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChartRecorderBlockEntity>> CHART_RECORDER_ENTITY =
            AllNuclearEntities.ENTITIES.register("chart_recorder", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ChartRecorderBlockEntity(ControlRoom2Registry.CHART_RECORDER_ENTITY.get(), pos, state),
                    CHART_RECORDER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnnunciatorPanelBlockEntity>> ANNUNCIATOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("annunciator_panel", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AnnunciatorPanelBlockEntity(ControlRoom2Registry.ANNUNCIATOR_ENTITY.get(), pos, state),
                    ANNUNCIATOR_PANEL.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<DeviceMenu>> CHART_RECORDER_MENU = AllNuclearGUIs.MENUS.register("chart_recorder",
            () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new DeviceMenu(ControlRoom2Registry.CHART_RECORDER_MENU.get(), id, inventory, buf.readBlockPos(), buf.readVarInt())));
    public static final DeferredHolder<MenuType<?>, MenuType<DeviceMenu>> ANNUNCIATOR_MENU = AllNuclearGUIs.MENUS.register("annunciator_panel",
            () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new DeviceMenu(ControlRoom2Registry.ANNUNCIATOR_MENU.get(), id, inventory, buf.readBlockPos(), buf.readVarInt())));
    /** The wrench setup of a Control Room Monitor's screen, opened on the clicked monitor. */
    public static final DeferredHolder<MenuType<?>, MenuType<DeviceMenu>> MONITOR_MENU = AllNuclearGUIs.MENUS.register("control_monitor",
            () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    new DeviceMenu(ControlRoom2Registry.MONITOR_MENU.get(), id, inventory, buf.readBlockPos(), buf.readVarInt())));

    /** A torn-off chart, on a Chart Roll. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChartData>> CHART_DATA =
            AllNuclearDataComponents.DATA_COMPONENTS.registerComponentType("chart_data", builder -> builder
                    .persistent(ChartData.CODEC)
                    .networkSynchronized(ChartData.STREAM_CODEC));

    private ControlRoom2Registry() {}

    @Override
    public void register(IEventBus modEventBus) {
        ConfigureDevicePayload.register(modEventBus);
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHART_RECORDER_ENTITY.get(), ChartRecorderBlockEntity::paperHandler);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(CHART_RECORDER_ITEM.get());
        output.accept(ANNUNCIATOR_PANEL_ITEM.get());
    }
}
