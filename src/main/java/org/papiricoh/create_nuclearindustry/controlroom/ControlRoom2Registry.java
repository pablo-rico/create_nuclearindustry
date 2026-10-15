package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
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
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlTableBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.WallControlPanelBlock;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedLinkNetwork;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlock;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlock;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlockEntity;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

/**
 * Content for the Advanced Redstone Link, the control table and wall control panel, the chart
 * recorder, the annunciator panel, the Servo Gearbox and automatic rod control. The controller
 * module item comes with {@link org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType#CONTROLLER},
 * like every other module.
 */
public final class ControlRoom2Registry implements NuclearFeature {
    public static final ControlRoom2Registry INSTANCE = new ControlRoom2Registry();

    public static final DeferredHolder<Block, ChartRecorderBlock> CHART_RECORDER = AllNuclearBlocks.BLOCKS.register("chart_recorder",
            () -> new ChartRecorderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)));
    public static final DeferredHolder<Block, AnnunciatorPanelBlock> ANNUNCIATOR_PANEL = AllNuclearBlocks.BLOCKS.register("annunciator_panel",
            () -> new AnnunciatorPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)
                    .lightLevel(state -> 3).noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, ControlTableBlock> CONTROL_TABLE = AllNuclearBlocks.BLOCKS.register("control_table",
            () -> new ControlTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f).noOcclusion()));
    public static final DeferredHolder<Block, WallControlPanelBlock> WALL_CONTROL_PANEL = AllNuclearBlocks.BLOCKS.register("wall_control_panel",
            () -> new WallControlPanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0f, 6.0f)
                    .noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, AdvancedRedstoneLinkBlock> ADVANCED_REDSTONE_LINK = AllNuclearBlocks.BLOCKS.register(
            "advanced_redstone_link", () -> new AdvancedRedstoneLinkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5f).noOcclusion().forceSolidOn().pushReaction(PushReaction.DESTROY)));

    /** Puts a piston or pulley where a redstone signal says (see {@link ServoGearboxBlockEntity}). */
    public static final DeferredHolder<Block, ServoGearboxBlock> SERVO_GEARBOX = AllNuclearBlocks.BLOCKS.register("servo_gearbox",
            () -> new ServoGearboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(2.0f, 6.0f)
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ADVANCED_REDSTONE_LINK_ITEM = AllNuclearItems.ITEMS.register("advanced_redstone_link",
            () -> new BlockItem(ADVANCED_REDSTONE_LINK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CONTROL_TABLE_ITEM = AllNuclearItems.ITEMS.register("control_table",
            () -> new BlockItem(CONTROL_TABLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> WALL_CONTROL_PANEL_ITEM = AllNuclearItems.ITEMS.register("wall_control_panel",
            () -> new BlockItem(WALL_CONTROL_PANEL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CHART_RECORDER_ITEM = AllNuclearItems.ITEMS.register("chart_recorder",
            () -> new BlockItem(CHART_RECORDER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ANNUNCIATOR_PANEL_ITEM = AllNuclearItems.ITEMS.register("annunciator_panel",
            () -> new BlockItem(ANNUNCIATOR_PANEL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> SERVO_GEARBOX_ITEM = AllNuclearItems.ITEMS.register("servo_gearbox",
            () -> new BlockItem(SERVO_GEARBOX.get(), new Item.Properties()));
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

    /** Tables and wall panels work like desks; desks keep their own type, from before. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControlDeskBlockEntity>> MODULE_SURFACE_ENTITY =
            AllNuclearEntities.ENTITIES.register("module_surface", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ControlDeskBlockEntity(ControlRoom2Registry.MODULE_SURFACE_ENTITY.get(), pos, state),
                    CONTROL_TABLE.get(), WALL_CONTROL_PANEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedRedstoneLinkBlockEntity>> ADVANCED_LINK_ENTITY =
            AllNuclearEntities.ENTITIES.register("advanced_redstone_link", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AdvancedRedstoneLinkBlockEntity(ControlRoom2Registry.ADVANCED_LINK_ENTITY.get(), pos, state),
                    ADVANCED_REDSTONE_LINK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServoGearboxBlockEntity>> SERVO_GEARBOX_ENTITY =
            AllNuclearEntities.ENTITIES.register("servo_gearbox", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new ServoGearboxBlockEntity(ControlRoom2Registry.SERVO_GEARBOX_ENTITY.get(), pos, state),
                    SERVO_GEARBOX.get()).build(null));

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
        modEventBus.addListener(ControlRoom2Registry::commonSetup);
        NeoForge.EVENT_BUS.addListener(AdvancedLinkNetwork::onLevelUnload);
    }

    /** Tables and wall panels linked to a reactor pass its readings on to Display Links, as desks do. */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (DeferredHolder<DisplaySource, SingleLineDisplaySource> source : AllNuclearDisplaySources.REACTOR_READOUTS.values()) {
                DisplaySource.BY_BLOCK_ENTITY.add(MODULE_SURFACE_ENTITY.get(), source.get());
            }
        });
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHART_RECORDER_ENTITY.get(), ChartRecorderBlockEntity::paperHandler);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.CONTROL_ROOM);
        output.accept(ADVANCED_REDSTONE_LINK_ITEM.get());
        output.accept(CONTROL_TABLE_ITEM.get());
        output.accept(WALL_CONTROL_PANEL_ITEM.get());
        output.accept(CHART_RECORDER_ITEM.get());
        output.accept(ANNUNCIATOR_PANEL_ITEM.get());
        output.accept(SERVO_GEARBOX_ITEM.get());
    }
}
