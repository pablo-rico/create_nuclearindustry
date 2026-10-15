package org.papiricoh.create_nuclearindustry.propulsion;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.integration.sable.SableCompat;
import org.papiricoh.create_nuclearindustry.propulsion.block.AntimatterEngineBlock;
import org.papiricoh.create_nuclearindustry.propulsion.block.EngineSegmentBlock;
import org.papiricoh.create_nuclearindustry.propulsion.block.RocketNozzleBlock;
import org.papiricoh.create_nuclearindustry.propulsion.block.ThermalEngineBlock;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.AntimatterEngineBlockEntity;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.ThermalEngineBlockEntity;

import java.util.function.Function;

/**
 * Nuclear engines for Create Aeronautics ships: the nuclear thermal engine (controller, heat exchanger
 * segments, nozzle) and the antimatter engine (annihilation chamber, magnetic coils, magnetic nozzle).
 * They are registered always, so worlds open the same with and without Aeronautics, but only crafted
 * and listed with Sable installed: without a ship to push they would be test stands only.
 */
public final class PropulsionRegistry implements NuclearFeature {
    public static final PropulsionRegistry INSTANCE = new PropulsionRegistry();

    public static final DeferredHolder<Block, ThermalEngineBlock> THERMAL_ENGINE = block("nuclear_thermal_engine",
            ThermalEngineBlock::new, heavy(MapColor.METAL));
    public static final DeferredHolder<Block, EngineSegmentBlock> THERMAL_EXCHANGER = block("thermal_engine_exchanger",
            EngineSegmentBlock::new, heavy(MapColor.METAL));
    public static final DeferredHolder<Block, RocketNozzleBlock> THERMAL_NOZZLE = block("thermal_rocket_nozzle",
            p -> new RocketNozzleBlock(p, RocketNozzleBlock.Jet.THERMAL), heavy(MapColor.COLOR_GRAY).noOcclusion()
                    .lightLevel(state -> state.getValue(RocketNozzleBlock.LIT) ? 12 : 0));
    public static final DeferredHolder<Block, AntimatterEngineBlock> ANTIMATTER_ENGINE = block("antimatter_engine",
            AntimatterEngineBlock::new, heavy(MapColor.COLOR_PURPLE).lightLevel(state -> 4));
    public static final DeferredHolder<Block, EngineSegmentBlock> MAGNETIC_COIL = block("magnetic_coil",
            EngineSegmentBlock::new, heavy(MapColor.COLOR_ORANGE));
    public static final DeferredHolder<Block, RocketNozzleBlock> MAGNETIC_NOZZLE = block("magnetic_nozzle",
            p -> new RocketNozzleBlock(p, RocketNozzleBlock.Jet.MAGNETIC), heavy(MapColor.COLOR_PURPLE).noOcclusion()
                    .lightLevel(state -> state.getValue(RocketNozzleBlock.LIT) ? 15 : 2));

    public static final DeferredHolder<Item, BlockItem> THERMAL_ENGINE_ITEM = item(THERMAL_ENGINE, Rarity.RARE);
    public static final DeferredHolder<Item, BlockItem> THERMAL_EXCHANGER_ITEM = item(THERMAL_EXCHANGER, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, BlockItem> THERMAL_NOZZLE_ITEM = item(THERMAL_NOZZLE, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, BlockItem> ANTIMATTER_ENGINE_ITEM = item(ANTIMATTER_ENGINE, Rarity.EPIC);
    public static final DeferredHolder<Item, BlockItem> MAGNETIC_COIL_ITEM = item(MAGNETIC_COIL, Rarity.RARE);
    public static final DeferredHolder<Item, BlockItem> MAGNETIC_NOZZLE_ITEM = item(MAGNETIC_NOZZLE, Rarity.RARE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermalEngineBlockEntity>> THERMAL_ENGINE_ENTITY =
            AllNuclearEntities.ENTITIES.register("nuclear_thermal_engine", () -> BlockEntityType.Builder.of(
                    ThermalEngineBlockEntity::new, THERMAL_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntimatterEngineBlockEntity>> ANTIMATTER_ENGINE_ENTITY =
            AllNuclearEntities.ENTITIES.register("antimatter_engine", () -> BlockEntityType.Builder.of(
                    AntimatterEngineBlockEntity::new, ANTIMATTER_ENGINE.get()).build(null));

    public static final DeferredHolder<DisplaySource, EngineDisplaySource> ENGINE_DISPLAY =
            AllNuclearDisplaySources.DISPLAY_SOURCES.register("rocket_engine", EngineDisplaySource::new);

    private PropulsionRegistry() {}

    /** Engine plating: heavy, pistons cannot move it. */
    private static BlockBehaviour.Properties heavy(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(5.0f, 10.0f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK);
    }

    private static <B extends Block> DeferredHolder<Block, B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                    BlockBehaviour.Properties properties) {
        return AllNuclearBlocks.BLOCKS.register(id, () -> factory.apply(properties));
    }

    private static DeferredHolder<Item, BlockItem> item(DeferredHolder<Block, ? extends Block> block, Rarity rarity) {
        return AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties().rarity(rarity)));
    }

    @Override
    public void register(IEventBus modEventBus) {
        modEventBus.addListener(PropulsionRegistry::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DisplaySource.BY_BLOCK.add(THERMAL_ENGINE.get(), ENGINE_DISPLAY.get());
            DisplaySource.BY_BLOCK.add(ANTIMATTER_ENGINE.get(), ENGINE_DISPLAY.get());
        });
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, THERMAL_ENGINE_ENTITY.get(), (be, side) -> be.fluids());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ANTIMATTER_ENGINE_ENTITY.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ANTIMATTER_ENGINE_ENTITY.get(), (be, side) -> be.port());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        if (!SableCompat.isLoaded()) {
            return;
        }
        output.section(NuclearTabSection.PROPULSION);
        output.accept(THERMAL_ENGINE_ITEM.get());
        output.accept(THERMAL_EXCHANGER_ITEM.get());
        output.accept(THERMAL_NOZZLE_ITEM.get());
        output.accept(ANTIMATTER_ENGINE_ITEM.get());
        output.accept(MAGNETIC_COIL_ITEM.get());
        output.accept(MAGNETIC_NOZZLE_ITEM.get());
    }
}
