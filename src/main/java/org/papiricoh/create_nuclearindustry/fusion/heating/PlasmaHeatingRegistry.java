package org.papiricoh.create_nuclearindustry.fusion.heating;

import com.simibubi.create.api.stress.BlockStressValues;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/** Content for the tokamak's neutral beam injectors, radio-frequency antennas, viewports, plasma rendering and sounds: blocks, items and block entities register here. */
public final class PlasmaHeatingRegistry implements NuclearFeature {
    public static final PlasmaHeatingRegistry INSTANCE = new PlasmaHeatingRegistry();

    public static final DeferredHolder<Block, NeutralBeamInjectorBlock> NEUTRAL_BEAM_INJECTOR = block("fusion_neutral_beam_injector",
            NeutralBeamInjectorBlock::new, titanium());
    public static final DeferredHolder<Block, RfAntennaBlock> RF_ANTENNA = block("fusion_rf_antenna", RfAntennaBlock::new, titanium());
    public static final DeferredHolder<Block, FusionViewportBlock> VIEWPORT = block("fusion_viewport", FusionViewportBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f, 9.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).noOcclusion().lightLevel(FusionViewportBlock::lightLevel)
                    .isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));

    public static final DeferredHolder<Item, BlockItem> NEUTRAL_BEAM_INJECTOR_ITEM = item(NEUTRAL_BEAM_INJECTOR);
    public static final DeferredHolder<Item, BlockItem> RF_ANTENNA_ITEM = item(RF_ANTENNA);
    public static final DeferredHolder<Item, BlockItem> VIEWPORT_ITEM = item(VIEWPORT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NeutralBeamInjectorBlockEntity>> NEUTRAL_BEAM_INJECTOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("fusion_neutral_beam_injector", () -> BlockEntityType.Builder.of(
                    NeutralBeamInjectorBlockEntity::new, NEUTRAL_BEAM_INJECTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RfAntennaBlockEntity>> RF_ANTENNA_ENTITY =
            AllNuclearEntities.ENTITIES.register("fusion_rf_antenna", () -> BlockEntityType.Builder.of(
                    RfAntennaBlockEntity::new, RF_ANTENNA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FusionViewportBlockEntity>> VIEWPORT_ENTITY =
            AllNuclearEntities.ENTITIES.register("fusion_viewport", () -> BlockEntityType.Builder.of(
                    FusionViewportBlockEntity::new, VIEWPORT.get()).build(null));

    /** Every block of this area; all are tokamak shell parts. */
    public static final List<Supplier<? extends Block>> PARTS = List.of(NEUTRAL_BEAM_INJECTOR, RF_ANTENNA, VIEWPORT);

    private PlasmaHeatingRegistry() {}

    private static BlockBehaviour.Properties titanium() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0f, 6.0f)
                .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK);
    }

    private static <B extends Block> DeferredHolder<Block, B> block(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                    BlockBehaviour.Properties properties) {
        return AllNuclearBlocks.BLOCKS.register(id, () -> factory.apply(properties));
    }

    private static DeferredHolder<Item, BlockItem> item(DeferredHolder<Block, ? extends Block> block) {
        return AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    @Override
    public void register(IEventBus modEventBus) {
        modEventBus.addListener(PlasmaHeatingRegistry::commonSetup);
    }

    /** Lets Create show the heaters' stress impact on their item tooltips and in its goggles. */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BlockStressValues.IMPACTS.register(NEUTRAL_BEAM_INJECTOR.get(), () -> PlasmaHeatingConfig.neutralBeamStress);
            BlockStressValues.IMPACTS.register(RF_ANTENNA.get(), () -> PlasmaHeatingConfig.radioFrequencyStress);
        });
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(NEUTRAL_BEAM_INJECTOR_ITEM.get());
        output.accept(RF_ANTENNA_ITEM.get());
        output.accept(VIEWPORT_ITEM.get());
    }
}
