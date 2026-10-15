package org.papiricoh.create_nuclearindustry.antimatter;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.stress.BlockStressValues;
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
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlock;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlock;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorDisplaySource;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlock;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.AntimatterTrapDisplaySource;
import org.papiricoh.create_nuclearindustry.antimatter.trap.LargePenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.MediumPenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.trap.SmallPenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;

import java.util.List;
import java.util.function.Function;

/**
 * Antimatter containment and use: the Antimatter Vial, the three Penning traps (small, medium and
 * large, each a different colour), the magnetic Antimatter Conduit, the Annihilation Generator and
 * the tokamak's Antiproton Injector. All of them hold antimatter only with Forge Energy; see
 * {@link PenningTrapBlockEntity} for what happens without it.
 */
public final class AntimatterRegistry implements NuclearFeature {
    public static final AntimatterRegistry INSTANCE = new AntimatterRegistry();

    public static final DeferredHolder<Block, SmallPenningTrapBlock> SMALL_PENNING_TRAP = block("small_penning_trap",
            SmallPenningTrapBlock::new, trap(MapColor.COLOR_CYAN));
    public static final DeferredHolder<Block, MediumPenningTrapBlock> PENNING_TRAP = block("penning_trap",
            MediumPenningTrapBlock::new, trap(MapColor.COLOR_PURPLE));
    public static final DeferredHolder<Block, LargePenningTrapBlock> LARGE_PENNING_TRAP = block("large_penning_trap",
            LargePenningTrapBlock::new, trap(MapColor.COLOR_ORANGE));
    public static final DeferredHolder<Block, AntimatterConduitBlock> ANTIMATTER_CONDUIT = block("antimatter_conduit",
            AntimatterConduitBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion().isViewBlocking((s, l, p) -> false)
                    .lightLevel(state -> 3));

    public static final DeferredHolder<Block, AnnihilationGeneratorBlock> ANNIHILATION_GENERATOR = block("annihilation_generator",
            AnnihilationGeneratorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 4));
    public static final DeferredHolder<Block, AntiprotonInjectorBlock> ANTIPROTON_INJECTOR = block("antiproton_injector",
            AntiprotonInjectorBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 3));

    public static final DeferredHolder<Item, BlockItem> SMALL_PENNING_TRAP_ITEM = item(SMALL_PENNING_TRAP, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, BlockItem> PENNING_TRAP_ITEM = item(PENNING_TRAP, Rarity.RARE);
    public static final DeferredHolder<Item, BlockItem> LARGE_PENNING_TRAP_ITEM = item(LARGE_PENNING_TRAP, Rarity.EPIC);
    public static final DeferredHolder<Item, BlockItem> ANTIMATTER_CONDUIT_ITEM = item(ANTIMATTER_CONDUIT, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, BlockItem> ANNIHILATION_GENERATOR_ITEM = item(ANNIHILATION_GENERATOR, Rarity.EPIC);
    public static final DeferredHolder<Item, BlockItem> ANTIPROTON_INJECTOR_ITEM = item(ANTIPROTON_INJECTOR, Rarity.RARE);
    public static final DeferredHolder<Item, AntimatterVialItem> ANTIMATTER_VIAL = AllNuclearItems.ITEMS.register("antimatter_vial",
            () -> new AntimatterVialItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PenningTrapBlockEntity>> PENNING_TRAP_ENTITY =
            AllNuclearEntities.ENTITIES.register("penning_trap", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new PenningTrapBlockEntity(AntimatterRegistry.PENNING_TRAP_ENTITY.get(), pos, state),
                    SMALL_PENNING_TRAP.get(), PENNING_TRAP.get(), LARGE_PENNING_TRAP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntimatterConduitBlockEntity>> CONDUIT_ENTITY =
            AllNuclearEntities.ENTITIES.register("antimatter_conduit", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AntimatterConduitBlockEntity(AntimatterRegistry.CONDUIT_ENTITY.get(), pos, state),
                    ANTIMATTER_CONDUIT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnnihilationGeneratorBlockEntity>> GENERATOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("annihilation_generator", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AnnihilationGeneratorBlockEntity(AntimatterRegistry.GENERATOR_ENTITY.get(), pos, state),
                    ANNIHILATION_GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntiprotonInjectorBlockEntity>> INJECTOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("antiproton_injector", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AntiprotonInjectorBlockEntity(AntimatterRegistry.INJECTOR_ENTITY.get(), pos, state),
                    ANTIPROTON_INJECTOR.get()).build(null));

    public static final DeferredHolder<DisplaySource, AntimatterTrapDisplaySource> TRAP_DISPLAY =
            AllNuclearDisplaySources.DISPLAY_SOURCES.register("antimatter_trap", AntimatterTrapDisplaySource::new);
    public static final DeferredHolder<DisplaySource, AnnihilationGeneratorDisplaySource> GENERATOR_DISPLAY =
            AllNuclearDisplaySources.DISPLAY_SOURCES.register("annihilation_generator", AnnihilationGeneratorDisplaySource::new);

    private AntimatterRegistry() {}

    /** Steel heads round a glass cylinder; pistons cannot move a trap, and it lets light through. */
    private static BlockBehaviour.Properties trap(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(4.0f, 6.0f).requiresCorrectToolForDrops()
                .sound(SoundType.NETHERITE_BLOCK).noOcclusion().isViewBlocking((s, l, p) -> false)
                .pushReaction(PushReaction.BLOCK).lightLevel(state -> 5);
    }

    private static <B extends Block> DeferredHolder<Block, B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                    BlockBehaviour.Properties properties) {
        return AllNuclearBlocks.BLOCKS.register(id, () -> factory.apply(properties));
    }

    private static DeferredHolder<Item, BlockItem> item(DeferredHolder<Block, ? extends Block> block, Rarity rarity) {
        return AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties().rarity(rarity)));
    }

    /** The three trap blocks. */
    public static List<PenningTrapBlock> traps() {
        return List.of(SMALL_PENNING_TRAP.get(), PENNING_TRAP.get(), LARGE_PENNING_TRAP.get());
    }

    @Override
    public void register(IEventBus modEventBus) {
        AntimatterAnnihilation.init();
        modEventBus.addListener(AntimatterRegistry::commonSetup);
    }

    /**
     * Display Links read a trap from any of its blocks, and the generator; Create's tooltips and
     * goggles learn the generator's speed and capacity.
     */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            traps().forEach(trap -> DisplaySource.BY_BLOCK.add(trap, TRAP_DISPLAY.get()));
            DisplaySource.BY_BLOCK.add(ANNIHILATION_GENERATOR.get(), GENERATOR_DISPLAY.get());
            BlockStressValues.CAPACITIES.register(ANNIHILATION_GENERATOR.get(), () -> AcceleratorConfig.generatorCapacity);
            BlockStressValues.RPM.register(ANNIHILATION_GENERATOR.get(),
                    new BlockStressValues.GeneratedRpm((int) AcceleratorConfig.generator().ratedSpeed(), true));
        });
    }

    /** Every part of a trap answers through its master, so a cable or funnel may touch any of them. */
    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (PenningTrapBlock trap : traps()) {
            event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) ->
                    trap.master(level, state, pos).map(PenningTrapBlockEntity::energy).orElse(null), trap);
            event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) ->
                    trap.master(level, state, pos).map(PenningTrapBlockEntity::port).orElse(null), trap);
        }
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CONDUIT_ENTITY.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GENERATOR_ENTITY.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GENERATOR_ENTITY.get(), (be, side) -> be.port());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INJECTOR_ENTITY.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INJECTOR_ENTITY.get(), (be, side) -> be.port());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.ANTIMATTER);
        output.accept(ANTIMATTER_VIAL.get());
        output.accept(SMALL_PENNING_TRAP_ITEM.get());
        output.accept(PENNING_TRAP_ITEM.get());
        output.accept(LARGE_PENNING_TRAP_ITEM.get());
        output.accept(ANTIMATTER_CONDUIT_ITEM.get());
        output.accept(ANNIHILATION_GENERATOR_ITEM.get());
        output.accept(ANTIPROTON_INJECTOR_ITEM.get());
    }
}
