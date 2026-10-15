package org.papiricoh.create_nuclearindustry.wastetransport;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.ExplosionBreaches;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.TrainBreachMonitor;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.WasteContainerMovementBehaviour;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlock;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlockEntity;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskMountedStorageType;
import org.papiricoh.create_nuclearindustry.wastetransport.cistern.WasteCisternBlock;
import org.papiricoh.create_nuclearindustry.wastetransport.cistern.WasteCisternBlockEntity;
import org.papiricoh.create_nuclearindustry.wastetransport.cistern.WasteCisternMountedStorageType;

/**
 * Radioactive waste transport: a cistern for radioactive fluids and a shielded cask for solid
 * waste, both multiblocks in the style of Create's tank and vault that ride trains and other
 * contraptions with their cargo, shielded until a derailment or an explosion breaches them.
 */
public final class WasteTransportRegistry implements NuclearFeature {
    public static final WasteTransportRegistry INSTANCE = new WasteTransportRegistry();

    private static final DeferredRegister<MountedItemStorageType<?>> ITEM_STORAGE_TYPES =
            DeferredRegister.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, Create_NuclearIndustry.MODID);
    private static final DeferredRegister<MountedFluidStorageType<?>> FLUID_STORAGE_TYPES =
            DeferredRegister.create(CreateRegistries.MOUNTED_FLUID_STORAGE_TYPE, Create_NuclearIndustry.MODID);

    public static final DeferredHolder<Block, WasteCisternBlock> RADIOACTIVE_WASTE_CISTERN = AllNuclearBlocks.BLOCKS.register(
            "radioactive_waste_cistern", () -> new WasteCisternBlock(shell(MapColor.COLOR_GRAY)));
    public static final DeferredHolder<Block, WasteCaskBlock> SHIELDED_WASTE_CASK = AllNuclearBlocks.BLOCKS.register(
            "shielded_waste_cask", () -> new WasteCaskBlock(shell(MapColor.STONE)));

    public static final DeferredHolder<Item, BlockItem> RADIOACTIVE_WASTE_CISTERN_ITEM = AllNuclearItems.ITEMS.register(
            "radioactive_waste_cistern", () -> new BlockItem(RADIOACTIVE_WASTE_CISTERN.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> SHIELDED_WASTE_CASK_ITEM = AllNuclearItems.ITEMS.register(
            "shielded_waste_cask", () -> new BlockItem(SHIELDED_WASTE_CASK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteCisternBlockEntity>> CISTERN_ENTITY =
            AllNuclearEntities.ENTITIES.register("radioactive_waste_cistern", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new WasteCisternBlockEntity(WasteTransportRegistry.CISTERN_ENTITY.get(), pos, state),
                    RADIOACTIVE_WASTE_CISTERN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteCaskBlockEntity>> CASK_ENTITY =
            AllNuclearEntities.ENTITIES.register("shielded_waste_cask", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new WasteCaskBlockEntity(WasteTransportRegistry.CASK_ENTITY.get(), pos, state),
                    SHIELDED_WASTE_CASK.get()).build(null));

    // Built inside the supplier: an item storage type takes an intrusive holder when constructed.
    public static final DeferredHolder<MountedItemStorageType<?>, WasteCaskMountedStorageType> CASK_STORAGE =
            ITEM_STORAGE_TYPES.register("shielded_waste_cask", WasteCaskMountedStorageType::new);
    public static final DeferredHolder<MountedFluidStorageType<?>, WasteCisternMountedStorageType> CISTERN_STORAGE =
            FLUID_STORAGE_TYPES.register("radioactive_waste_cistern", WasteCisternMountedStorageType::new);

    private static final WasteContainerMovementBehaviour MOVEMENT = new WasteContainerMovementBehaviour();

    private WasteTransportRegistry() {}

    private static BlockBehaviour.Properties shell(MapColor colour) {
        return BlockBehaviour.Properties.of().mapColor(colour).strength(5.0f, 12.0f).sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops();
    }

    @Override
    public void register(IEventBus modEventBus) {
        ITEM_STORAGE_TYPES.register(modEventBus);
        FLUID_STORAGE_TYPES.register(modEventBus);
        modEventBus.addListener(WasteTransportRegistry::registerDataMaps);
        modEventBus.addListener(WasteTransportRegistry::commonSetup);
        NeoForge.EVENT_BUS.addListener(TrainBreachMonitor::onServerTick);
        NeoForge.EVENT_BUS.addListener(TrainBreachMonitor::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ExplosionBreaches::onDetonate);
        NeoForge.EVENT_BUS.addListener(ExplosionBreaches::onNuclearDetonation);
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(FluidRadioactivity.DATA_MAP);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MountedFluidStorageType.REGISTRY.register(RADIOACTIVE_WASTE_CISTERN.get(), CISTERN_STORAGE.get());
            MountedItemStorageType.REGISTRY.register(SHIELDED_WASTE_CASK.get(), CASK_STORAGE.get());
            MovementBehaviour.REGISTRY.register(RADIOACTIVE_WASTE_CISTERN.get(), MOVEMENT);
            MovementBehaviour.REGISTRY.register(SHIELDED_WASTE_CASK.get(), MOVEMENT);
            // A cask moves whole, as Create's vaults do (cisterns already do as tanks).
            BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> state.getBlock() instanceof WasteCaskBlock
                    ? BlockMovementChecks.CheckResult.of(ConnectivityHandler.isConnected(level, pos, pos.relative(direction)))
                    : BlockMovementChecks.CheckResult.PASS);
        });
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CISTERN_ENTITY.get(), (cistern, side) -> cistern.fluidHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CASK_ENTITY.get(), (cask, side) -> cask.itemHandler());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.SPENT_FUEL);
        output.accept(RADIOACTIVE_WASTE_CISTERN_ITEM.get());
        output.accept(SHIELDED_WASTE_CASK_ITEM.get());
    }
}
