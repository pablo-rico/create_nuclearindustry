package org.papiricoh.create_nuclearindustry.accelerator;

import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.world.inventory.MenuType;
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
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.accelerator.block.AcceleratorControllerBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.AcceleratorCryoFeedBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.TargetStationBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.AcceleratorMachineBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.BeamDirectionalBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.BeamDumpBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.BeamLineBlock;
import org.papiricoh.create_nuclearindustry.accelerator.block.IonSourceBlock;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorCryoFeedBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.TargetStationBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorMachineBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.BeamDumpBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.IonSourceBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.gui.AcceleratorControlPayload;
import org.papiricoh.create_nuclearindustry.accelerator.gui.AcceleratorMenu;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.BeamInterlockBlock;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.BeamInterlockBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.PersonnelInterlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Content for the particle accelerator: the beamline and ring blocks, the target stations and the
 * antimatter machines. Blocks, items and block entities go on the shared registers.
 */
public final class AcceleratorRegistry implements NuclearFeature {
    public static final AcceleratorRegistry INSTANCE = new AcceleratorRegistry();

    // ---------------------------------------------------------------- the beamline
    public static final DeferredHolder<Block, AcceleratorControllerBlock> CONTROLLER = block("particle_accelerator_controller",
            AcceleratorControllerBlock::new, steel());
    public static final DeferredHolder<Block, IonSourceBlock> ION_SOURCE = block("particle_accelerator_ion_source",
            IonSourceBlock::new, steel());
    public static final DeferredHolder<Block, BeamLineBlock> BEAM_PIPE = block("beam_pipe",
            p -> new BeamLineBlock(BeamElementRole.DRIFT, p), steel().noOcclusion().isViewBlocking((s, l, pos) -> false));
    public static final DeferredHolder<Block, BeamLineBlock> RF_CAVITY = block("beam_rf_cavity",
            p -> new BeamLineBlock(BeamElementRole.RF_CAVITY, p), copper());
    public static final DeferredHolder<Block, BeamLineBlock> QUADRUPOLE = block("beam_quadrupole",
            p -> new BeamLineBlock(BeamElementRole.QUADRUPOLE, p), steel());
    public static final DeferredHolder<Block, BeamDirectionalBlock> BENDING_MAGNET = block("beam_bending_magnet",
            p -> new BeamDirectionalBlock(BeamElementRole.BEND, p), steel());
    public static final DeferredHolder<Block, BeamLineBlock> POSITION_MONITOR = block("beam_position_monitor",
            p -> new BeamLineBlock(BeamElementRole.DIAGNOSTIC, p), steel());
    public static final DeferredHolder<Block, BeamDumpBlock> BEAM_DUMP = block("beam_dump", BeamDumpBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0f, 12.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK));

    // ---------------------------------------------------------------- the synchrotron ring
    public static final DeferredHolder<Block, BeamLineBlock> RING_DIPOLE = block("ring_dipole_magnet",
            p -> new BeamLineBlock(BeamElementRole.DIPOLE, p), steel().noOcclusion());
    public static final DeferredHolder<Block, BeamLineBlock> SC_RING_DIPOLE = block("superconducting_ring_dipole",
            p -> new BeamLineBlock(BeamElementRole.SC_DIPOLE, p), steel().noOcclusion());
    public static final DeferredHolder<Block, BeamLineBlock> SC_RF_CAVITY = block("superconducting_rf_cavity",
            p -> new BeamLineBlock(BeamElementRole.SC_RF_CAVITY, p), steel().noOcclusion());
    public static final DeferredHolder<Block, BeamDirectionalBlock> INJECTION_SEPTUM = block("injection_septum",
            p -> new BeamDirectionalBlock(BeamElementRole.INJECTION, p), steel());
    public static final DeferredHolder<Block, BeamDirectionalBlock> EXTRACTION_SEPTUM = block("extraction_septum",
            p -> new BeamDirectionalBlock(BeamElementRole.EXTRACTION, p), steel());
    public static final DeferredHolder<Block, TargetStationBlock> TARGET_STATION = block("target_station", TargetStationBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0f, 12.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK));

    // ---------------------------------------------------------------- machines beside the beamline
    public static final DeferredHolder<Block, AcceleratorMachineBlock> KLYSTRON = block("particle_accelerator_klystron",
            p -> new AcceleratorMachineBlock(AttachmentRole.KLYSTRON, () -> AcceleratorRegistry.KLYSTRON_ENTITY.get(), p), copper());
    public static final DeferredHolder<Block, AcceleratorMachineBlock> MAGNET_POWER_SUPPLY = block("particle_accelerator_magnet_power_supply",
            p -> new AcceleratorMachineBlock(AttachmentRole.MAGNET_POWER, () -> AcceleratorRegistry.MAGNET_POWER_SUPPLY_ENTITY.get(), p), steel());
    public static final DeferredHolder<Block, AcceleratorMachineBlock> VACUUM_PUMP = block("particle_accelerator_vacuum_pump",
            p -> new AcceleratorMachineBlock(AttachmentRole.VACUUM_PUMP, () -> AcceleratorRegistry.VACUUM_PUMP_ENTITY.get(), p), steel());

    public static final DeferredHolder<Block, AcceleratorCryoFeedBlock> CRYO_FEED = block("accelerator_cryo_feed",
            AcceleratorCryoFeedBlock::new, steel().noOcclusion());
    public static final DeferredHolder<Block, BeamInterlockBlock> BEAM_INTERLOCK = block("beam_interlock", BeamInterlockBlock::new,
            steel().lightLevel(state -> state.getValue(BeamInterlockBlock.SECURED) ? 4 : 2));

    public static final DeferredHolder<Item, BlockItem> CONTROLLER_ITEM = item(CONTROLLER);
    public static final DeferredHolder<Item, BlockItem> ION_SOURCE_ITEM = item(ION_SOURCE);
    public static final DeferredHolder<Item, BlockItem> BEAM_PIPE_ITEM = item(BEAM_PIPE);
    public static final DeferredHolder<Item, BlockItem> RF_CAVITY_ITEM = item(RF_CAVITY);
    public static final DeferredHolder<Item, BlockItem> QUADRUPOLE_ITEM = item(QUADRUPOLE);
    public static final DeferredHolder<Item, BlockItem> BENDING_MAGNET_ITEM = item(BENDING_MAGNET);
    public static final DeferredHolder<Item, BlockItem> POSITION_MONITOR_ITEM = item(POSITION_MONITOR);
    public static final DeferredHolder<Item, BlockItem> BEAM_DUMP_ITEM = item(BEAM_DUMP);
    public static final DeferredHolder<Item, BlockItem> KLYSTRON_ITEM = item(KLYSTRON);
    public static final DeferredHolder<Item, BlockItem> MAGNET_POWER_SUPPLY_ITEM = item(MAGNET_POWER_SUPPLY);
    public static final DeferredHolder<Item, BlockItem> VACUUM_PUMP_ITEM = item(VACUUM_PUMP);
    public static final DeferredHolder<Item, BlockItem> RING_DIPOLE_ITEM = item(RING_DIPOLE);
    public static final DeferredHolder<Item, BlockItem> SC_RING_DIPOLE_ITEM = item(SC_RING_DIPOLE);
    public static final DeferredHolder<Item, BlockItem> SC_RF_CAVITY_ITEM = item(SC_RF_CAVITY);
    public static final DeferredHolder<Item, BlockItem> INJECTION_SEPTUM_ITEM = item(INJECTION_SEPTUM);
    public static final DeferredHolder<Item, BlockItem> EXTRACTION_SEPTUM_ITEM = item(EXTRACTION_SEPTUM);
    public static final DeferredHolder<Item, BlockItem> TARGET_STATION_ITEM = item(TARGET_STATION);
    public static final DeferredHolder<Item, BlockItem> CRYO_FEED_ITEM = item(CRYO_FEED);
    public static final DeferredHolder<Item, BlockItem> BEAM_INTERLOCK_ITEM = item(BEAM_INTERLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorControllerBlockEntity>> CONTROLLER_ENTITY =
            AllNuclearEntities.ENTITIES.register("particle_accelerator_controller", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AcceleratorControllerBlockEntity(AcceleratorRegistry.CONTROLLER_ENTITY.get(), pos, state),
                    CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IonSourceBlockEntity>> ION_SOURCE_ENTITY =
            AllNuclearEntities.ENTITIES.register("particle_accelerator_ion_source", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new IonSourceBlockEntity(AcceleratorRegistry.ION_SOURCE_ENTITY.get(), pos, state),
                    ION_SOURCE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorMachineBlockEntity>> KLYSTRON_ENTITY =
            machineEntity("particle_accelerator_klystron", AttachmentRole.KLYSTRON, KLYSTRON);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorMachineBlockEntity>> MAGNET_POWER_SUPPLY_ENTITY =
            machineEntity("particle_accelerator_magnet_power_supply", AttachmentRole.MAGNET_POWER, MAGNET_POWER_SUPPLY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorMachineBlockEntity>> VACUUM_PUMP_ENTITY =
            machineEntity("particle_accelerator_vacuum_pump", AttachmentRole.VACUUM_PUMP, VACUUM_PUMP);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeamDumpBlockEntity>> BEAM_DUMP_ENTITY =
            AllNuclearEntities.ENTITIES.register("beam_dump", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new BeamDumpBlockEntity(AcceleratorRegistry.BEAM_DUMP_ENTITY.get(), pos, state),
                    BEAM_DUMP.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TargetStationBlockEntity>> TARGET_STATION_ENTITY =
            AllNuclearEntities.ENTITIES.register("target_station", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new TargetStationBlockEntity(AcceleratorRegistry.TARGET_STATION_ENTITY.get(), pos, state),
                    TARGET_STATION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorCryoFeedBlockEntity>> CRYO_FEED_ENTITY =
            AllNuclearEntities.ENTITIES.register("accelerator_cryo_feed", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AcceleratorCryoFeedBlockEntity(AcceleratorRegistry.CRYO_FEED_ENTITY.get(), pos, state),
                    CRYO_FEED.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeamInterlockBlockEntity>> BEAM_INTERLOCK_ENTITY =
            AllNuclearEntities.ENTITIES.register("beam_interlock", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new BeamInterlockBlockEntity(AcceleratorRegistry.BEAM_INTERLOCK_ENTITY.get(), pos, state),
                    BEAM_INTERLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<AcceleratorMenu>> MENU =
            AllNuclearGUIs.MENUS.register("accelerator_control", () -> IMenuTypeExtension.create(AcceleratorMenu::read));

    /** Every block that can be part of an accelerator: placing one lets an unformed controller look again. */
    public static final List<Supplier<? extends Block>> PARTS = List.of(CONTROLLER, ION_SOURCE, BEAM_PIPE, RF_CAVITY, QUADRUPOLE,
            BENDING_MAGNET, POSITION_MONITOR, BEAM_DUMP, KLYSTRON, MAGNET_POWER_SUPPLY, VACUUM_PUMP, RING_DIPOLE, SC_RING_DIPOLE,
            SC_RF_CAVITY, INJECTION_SEPTUM, EXTRACTION_SEPTUM, TARGET_STATION, CRYO_FEED, BEAM_INTERLOCK);

    private AcceleratorRegistry() {}

    private static BlockBehaviour.Properties steel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6.0f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHERITE_BLOCK);
    }

    private static BlockBehaviour.Properties copper() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6.0f).requiresCorrectToolForDrops()
                .sound(SoundType.COPPER);
    }

    private static <B extends Block> DeferredHolder<Block, B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                    BlockBehaviour.Properties properties) {
        return AllNuclearBlocks.BLOCKS.register(id, () -> factory.apply(properties));
    }

    private static DeferredHolder<Item, BlockItem> item(DeferredHolder<Block, ? extends Block> block) {
        return AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorMachineBlockEntity>> machineEntity(
            String id, AttachmentRole role, DeferredHolder<Block, AcceleratorMachineBlock> block) {
        @SuppressWarnings("unchecked")
        DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorMachineBlockEntity>>[] holder = new DeferredHolder[1];
        holder[0] = AllNuclearEntities.ENTITIES.register(id, () -> BlockEntityType.Builder.of(
                (pos, state) -> new AcceleratorMachineBlockEntity(holder[0].get(), pos, state, role), block.get()).build(null));
        return holder[0];
    }

    @Override
    public void register(IEventBus modEventBus) {
        modEventBus.addListener(AcceleratorRegistry::commonSetup);
        PersonnelInterlock.init();
        AcceleratorControlPayload.register(modEventBus);
    }

    /** Lets Create show the machines' stress impact on their item tooltips and in its goggles. */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(KLYSTRON.get(), () -> AcceleratorConfig.klystronStress);
            BlockStressValues.IMPACTS.register(MAGNET_POWER_SUPPLY.get(), () -> AcceleratorConfig.magnetSupplyStress);
            BlockStressValues.IMPACTS.register(VACUUM_PUMP.get(), () -> AcceleratorConfig.pumpStress);
            BlockStressValues.IMPACTS.register(ION_SOURCE.get(), () -> AcceleratorConfig.ionSourceStress);
        });
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorMachineBlockEntity>> type
                : List.of(KLYSTRON_ENTITY, MAGNET_POWER_SUPPLY_ENTITY, VACUUM_PUMP_ENTITY)) {
            event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type.get(), (be, side) -> be.energy());
        }
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ION_SOURCE_ENTITY.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ION_SOURCE_ENTITY.get(), (be, side) -> be.fluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CRYO_FEED_ENTITY.get(), (be, side) -> be.fluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TARGET_STATION_ENTITY.get(), (be, side) -> be.automation());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.ACCELERATOR);
        for (DeferredHolder<Item, BlockItem> item : List.of(CONTROLLER_ITEM, ION_SOURCE_ITEM, BEAM_PIPE_ITEM, RF_CAVITY_ITEM,
                QUADRUPOLE_ITEM, BENDING_MAGNET_ITEM, POSITION_MONITOR_ITEM, KLYSTRON_ITEM, MAGNET_POWER_SUPPLY_ITEM,
                VACUUM_PUMP_ITEM, BEAM_DUMP_ITEM, RING_DIPOLE_ITEM, SC_RING_DIPOLE_ITEM, SC_RF_CAVITY_ITEM, INJECTION_SEPTUM_ITEM,
                EXTRACTION_SEPTUM_ITEM, CRYO_FEED_ITEM, BEAM_INTERLOCK_ITEM, TARGET_STATION_ITEM)) {
            output.accept(item.get());
        }
    }
}
