package org.papiricoh.create_nuclearindustry.reactor.pwr;

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
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.PressurizerHeaterPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.ReliefValveBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortBlockEntity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Content for the pressurized water loop, steam generator, containment and the accident chain: blocks, items and block entities register here. */
public final class PwrRegistry implements NuclearFeature {
    public static final PwrRegistry INSTANCE = new PwrRegistry();

    // ---------------------------------------------------------------- pressurizer, on the reactor lid
    public static final DeferredHolder<Block, Block> PRESSURIZER = block("pressurizer", Block::new, steel());
    public static final DeferredHolder<Block, PressurizerHeaterPortBlock> PRESSURIZER_HEATER_PORT =
            block("pressurizer_heater_port", PressurizerHeaterPortBlock::new, steel());
    public static final DeferredHolder<Block, ReliefValveBlock> RELIEF_VALVE = block("relief_valve", ReliefValveBlock::new, steel());

    // ---------------------------------------------------------------- steam generator
    public static final DeferredHolder<Block, Block> STEAM_GENERATOR_CASING = block("steam_generator_casing", Block::new, steel());
    public static final DeferredHolder<Block, Block> STEAM_GENERATOR_TUBE_BUNDLE = block("steam_generator_tube_bundle", Block::new,
            steel().sound(SoundType.COPPER));
    public static final DeferredHolder<Block, SteamGeneratorControllerBlock> STEAM_GENERATOR_CONTROLLER =
            block("steam_generator_controller", SteamGeneratorControllerBlock::new, steel());
    public static final DeferredHolder<Block, SteamGeneratorPortBlock> STEAM_GENERATOR_PORT =
            block("steam_generator_port", SteamGeneratorPortBlock::new, steel());

    public static final DeferredHolder<Item, BlockItem> PRESSURIZER_ITEM = item(PRESSURIZER);
    public static final DeferredHolder<Item, BlockItem> PRESSURIZER_HEATER_PORT_ITEM = item(PRESSURIZER_HEATER_PORT);
    public static final DeferredHolder<Item, BlockItem> RELIEF_VALVE_ITEM = item(RELIEF_VALVE);
    public static final DeferredHolder<Item, BlockItem> STEAM_GENERATOR_CASING_ITEM = item(STEAM_GENERATOR_CASING);
    public static final DeferredHolder<Item, BlockItem> STEAM_GENERATOR_TUBE_BUNDLE_ITEM = item(STEAM_GENERATOR_TUBE_BUNDLE);
    public static final DeferredHolder<Item, BlockItem> STEAM_GENERATOR_CONTROLLER_ITEM = item(STEAM_GENERATOR_CONTROLLER);
    public static final DeferredHolder<Item, BlockItem> STEAM_GENERATOR_PORT_ITEM = item(STEAM_GENERATOR_PORT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamGeneratorControllerBlockEntity>> STEAM_GENERATOR_CONTROLLER_ENTITY =
            AllNuclearEntities.ENTITIES.register("steam_generator_controller", () -> BlockEntityType.Builder.of(
                    SteamGeneratorControllerBlockEntity::new, STEAM_GENERATOR_CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamGeneratorPortBlockEntity>> STEAM_GENERATOR_PORT_ENTITY =
            AllNuclearEntities.ENTITIES.register("steam_generator_port", () -> BlockEntityType.Builder.of(
                    SteamGeneratorPortBlockEntity::new, STEAM_GENERATOR_PORT.get()).build(null));

    /** Every block of the pressurizer and the steam generator: multiblock parts, all mined with a pickaxe. */
    public static final List<Supplier<? extends Block>> PARTS = List.of(PRESSURIZER, PRESSURIZER_HEATER_PORT, RELIEF_VALVE,
            STEAM_GENERATOR_CONTROLLER, STEAM_GENERATOR_CASING, STEAM_GENERATOR_TUBE_BUNDLE, STEAM_GENERATOR_PORT);

    private PwrRegistry() {}

    private static BlockBehaviour.Properties steel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f).requiresCorrectToolForDrops();
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
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STEAM_GENERATOR_PORT_ENTITY.get(),
                (port, direction) -> port.getFluidHandler(direction));
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(AllNuclearFluids.PRIMARY_COOLANT_BUCKET.get());
        output.accept(PRESSURIZER_ITEM.get());
        output.accept(PRESSURIZER_HEATER_PORT_ITEM.get());
        output.accept(RELIEF_VALVE_ITEM.get());
        output.accept(STEAM_GENERATOR_CONTROLLER_ITEM.get());
        output.accept(STEAM_GENERATOR_CASING_ITEM.get());
        output.accept(STEAM_GENERATOR_TUBE_BUNDLE_ITEM.get());
        output.accept(STEAM_GENERATOR_PORT_ITEM.get());
    }
}
