package org.papiricoh.create_nuclearindustry.civildefense;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicDetectorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicDetectorBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicNetwork;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlock;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.siren.HandCrankSirenBlock;
import org.papiricoh.create_nuclearindustry.civildefense.siren.HandCrankSirenBlockEntity;

import java.util.List;
import java.util.function.Supplier;

/** Content for sirens, the seismic detector, blast doors, the blast wave and shelters: blocks, items and block entities register here. */
public final class CivilDefenseRegistry implements NuclearFeature {
    public static final CivilDefenseRegistry INSTANCE = new CivilDefenseRegistry();
    /** Explosion resistance of blast door parts: above the 1200 a nuclear crater spares. */
    public static final float BLAST_RESISTANCE = 1500.0f;

    public static final DeferredHolder<Block, AirRaidSirenBlock> AIR_RAID_SIREN = AllNuclearBlocks.BLOCKS.register("air_raid_siren",
            () -> new AirRaidSirenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Block, HandCrankSirenBlock> HAND_CRANK_SIREN = AllNuclearBlocks.BLOCKS.register("hand_crank_siren",
            () -> new HandCrankSirenBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredHolder<Block, SeismicDetectorBlock> SEISMIC_DETECTOR = AllNuclearBlocks.BLOCKS.register("seismic_detector",
            () -> new SeismicDetectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> BLAST_DOOR_FRAME = AllNuclearBlocks.BLOCKS.register("blast_door_frame",
            () -> new Block(blastProof(MapColor.COLOR_GRAY)));
    public static final DeferredHolder<Block, BlastDoorBlock> STEEL_BLAST_DOOR = AllNuclearBlocks.BLOCKS.register("steel_blast_door",
            () -> new BlastDoorBlock(leaf(MapColor.COLOR_YELLOW)));
    public static final DeferredHolder<Block, BlastDoorBlock> LEAD_BLAST_DOOR = AllNuclearBlocks.BLOCKS.register("lead_blast_door",
            () -> new BlastDoorBlock(leaf(MapColor.COLOR_BLUE)));
    public static final DeferredHolder<Block, BlastDoorControllerBlock> BLAST_DOOR_CONTROLLER = AllNuclearBlocks.BLOCKS.register("blast_door_controller",
            () -> new BlastDoorControllerBlock(blastProof(MapColor.COLOR_GRAY)));

    public static final DeferredHolder<Item, BlockItem> AIR_RAID_SIREN_ITEM = blockItem("air_raid_siren", AIR_RAID_SIREN);
    public static final DeferredHolder<Item, BlockItem> HAND_CRANK_SIREN_ITEM = blockItem("hand_crank_siren", HAND_CRANK_SIREN);
    public static final DeferredHolder<Item, BlockItem> SEISMIC_DETECTOR_ITEM = blockItem("seismic_detector", SEISMIC_DETECTOR);
    public static final DeferredHolder<Item, BlockItem> BLAST_DOOR_FRAME_ITEM = blockItem("blast_door_frame", BLAST_DOOR_FRAME);
    public static final DeferredHolder<Item, BlockItem> STEEL_BLAST_DOOR_ITEM = blockItem("steel_blast_door", STEEL_BLAST_DOOR);
    public static final DeferredHolder<Item, BlockItem> LEAD_BLAST_DOOR_ITEM = blockItem("lead_blast_door", LEAD_BLAST_DOOR);
    public static final DeferredHolder<Item, BlockItem> BLAST_DOOR_CONTROLLER_ITEM = blockItem("blast_door_controller", BLAST_DOOR_CONTROLLER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirRaidSirenBlockEntity>> AIR_RAID_SIREN_BE =
            AllNuclearEntities.ENTITIES.register("air_raid_siren", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new AirRaidSirenBlockEntity(CivilDefenseRegistry.AIR_RAID_SIREN_BE.get(), pos, state),
                    AIR_RAID_SIREN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankSirenBlockEntity>> HAND_CRANK_SIREN_BE =
            AllNuclearEntities.ENTITIES.register("hand_crank_siren", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new HandCrankSirenBlockEntity(CivilDefenseRegistry.HAND_CRANK_SIREN_BE.get(), pos, state),
                    HAND_CRANK_SIREN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SeismicDetectorBlockEntity>> SEISMIC_DETECTOR_BE =
            AllNuclearEntities.ENTITIES.register("seismic_detector", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new SeismicDetectorBlockEntity(CivilDefenseRegistry.SEISMIC_DETECTOR_BE.get(), pos, state),
                    SEISMIC_DETECTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastDoorControllerBlockEntity>> BLAST_DOOR_CONTROLLER_BE =
            AllNuclearEntities.ENTITIES.register("blast_door_controller", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new BlastDoorControllerBlockEntity(CivilDefenseRegistry.BLAST_DOOR_CONTROLLER_BE.get(), pos, state),
                    BLAST_DOOR_CONTROLLER.get()).build(null));

    private CivilDefenseRegistry() {}

    /** Reinforced steel: slow to mine, immovable, and spared by nuclear craters. */
    private static BlockBehaviour.Properties blastProof(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(12.0f, BLAST_RESISTANCE).sound(SoundType.NETHERITE_BLOCK)
                .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK);
    }

    /**
     * A door leaf counts as solid even when open, so water does not wash away the retracted panel
     * (fluids replace non-solid blocks); its collision still follows its state.
     */
    private static BlockBehaviour.Properties leaf(MapColor color) {
        return blastProof(color).forceSolidOn();
    }

    private static DeferredHolder<Item, BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return AllNuclearItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    /** Every block of this area, for datagen and the creative tab. */
    public static List<DeferredHolder<Item, BlockItem>> items() {
        return List.of(AIR_RAID_SIREN_ITEM, HAND_CRANK_SIREN_ITEM, SEISMIC_DETECTOR_ITEM, BLAST_DOOR_FRAME_ITEM,
                STEEL_BLAST_DOOR_ITEM, LEAD_BLAST_DOOR_ITEM, BLAST_DOOR_CONTROLLER_ITEM);
    }

    @Override
    public void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(SeismicNetwork::onDetonation);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        items().forEach(item -> output.accept(item.get()));
    }
}
