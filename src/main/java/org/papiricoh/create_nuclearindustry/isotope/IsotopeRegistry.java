package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearIngredients;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.creativetab.NuclearTabSection;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.isotope.channel.ChannelStatus;
import org.papiricoh.create_nuclearindustry.isotope.channel.IrradiationChannelBlock;
import org.papiricoh.create_nuclearindustry.isotope.channel.IrradiationChannelBlockEntity;
import org.papiricoh.create_nuclearindustry.isotope.irradiator.GammaIrradiatorBlock;
import org.papiricoh.create_nuclearindustry.isotope.irradiator.GammaIrradiatorBlockEntity;
import org.papiricoh.create_nuclearindustry.isotope.physics.Isotope;
import org.papiricoh.create_nuclearindustry.isotope.smoke.SmokeDetectorBlock;
import org.papiricoh.create_nuclearindustry.isotope.smoke.SmokeDetectorBlockEntity;
import org.papiricoh.create_nuclearindustry.radiation.RadiationMedicineEffect;
import org.papiricoh.create_nuclearindustry.rtg.RtgFuel;
import org.papiricoh.create_nuclearindustry.rtg.RtgPelletItem;

/**
 * Isotopes and nuclear medicine: cobalt and its cobalt-60 sources (from the reactor's irradiation
 * channel), strontium-90 (from PUREX), americium-241 (from aged plutonium), the technetium-99m
 * generator (from a Target Station), and what they are for: the Gamma Irradiator that sterilises
 * food, the Radiography Camera, the Smoke Detector, the strontium RTG pellet and the Medical
 * Isotope Kit. Every isotope decays on its own clock ({@link Isotopes}).
 */
public final class IsotopeRegistry implements NuclearFeature {
    public static final IsotopeRegistry INSTANCE = new IsotopeRegistry();

    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Create_NuclearIndustry.MODID);

    /** The decay clock of an isotope item, and plutonium's age. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<IsotopeData>> ISOTOPE =
            AllNuclearDataComponents.DATA_COMPONENTS.registerComponentType("isotope", builder -> builder
                    .persistent(IsotopeData.CODEC)
                    .networkSynchronized(IsotopeData.STREAM_CODEC));
    /** The Mo-99 and Tc-99m in a technetium generator. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TechnetiumGeneratorItem.CowData>> COW =
            AllNuclearDataComponents.DATA_COMPONENTS.registerComponentType("technetium_generator", builder -> builder
                    .persistent(TechnetiumGeneratorItem.CowData.CODEC)
                    .networkSynchronized(TechnetiumGeneratorItem.CowData.STREAM_CODEC));
    /** Food sterilised by the Gamma Irradiator. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> IRRADIATED =
            AllNuclearDataComponents.DATA_COMPONENTS.registerComponentType("irradiated", builder -> builder
                    .persistent(Unit.CODEC)
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    public static final DeferredHolder<IngredientType<?>, IngredientType<AgedPlutoniumIngredient>> AGED_PLUTONIUM =
            AllNuclearIngredients.INGREDIENT_TYPES.register("aged_plutonium", () -> new IngredientType<>(AgedPlutoniumIngredient.CODEC));

    /** Technetium-99m tracer from a medical kit: boosts potassium iodide and Prussian blue. */
    public static final DeferredHolder<MobEffect, RadiationMedicineEffect> TRACER = EFFECTS.register("isotope_tracer",
            () -> new RadiationMedicineEffect(0x6BE3C8));

    public static final DeferredHolder<Block, GammaIrradiatorBlock> GAMMA_IRRADIATOR = AllNuclearBlocks.BLOCKS.register("gamma_irradiator",
            () -> new GammaIrradiatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredHolder<Block, SmokeDetectorBlock> SMOKE_DETECTOR = AllNuclearBlocks.BLOCKS.register("smoke_detector",
            () -> new SmokeDetectorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5f)
                    .sound(SoundType.STONE).noCollission().pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<Block, IrradiationChannelBlock> IRRADIATION_CHANNEL = AllNuclearBlocks.BLOCKS.register(
            "reactor_irradiation_channel", () -> new IrradiationChannelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f).lightLevel(state -> state.getValue(IrradiationChannelBlock.STATUS)
                            == ChannelStatus.IRRADIATING ? 6 : 0)));

    public static final DeferredHolder<Item, BlockItem> GAMMA_IRRADIATOR_ITEM = blockItem(GAMMA_IRRADIATOR, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, BlockItem> SMOKE_DETECTOR_ITEM = blockItem(SMOKE_DETECTOR, Rarity.COMMON);
    public static final DeferredHolder<Item, BlockItem> IRRADIATION_CHANNEL_ITEM = blockItem(IRRADIATION_CHANNEL, Rarity.COMMON);

    public static final DeferredHolder<Item, Item> COBALT_NUGGET = AllNuclearItems.ITEMS.register("cobalt_nugget",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COBALT_INGOT = AllNuclearItems.ITEMS.register("cobalt_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, IsotopeItem> COBALT_60 = isotope("cobalt_60", Isotope.COBALT_60, Rarity.UNCOMMON, 16);
    public static final DeferredHolder<Item, IsotopeItem> STRONTIUM_90 = isotope("strontium_90", Isotope.STRONTIUM_90, Rarity.UNCOMMON, 64);
    public static final DeferredHolder<Item, IsotopeItem> AMERICIUM_241 = isotope("americium_241", Isotope.AMERICIUM_241, Rarity.UNCOMMON, 64);
    public static final DeferredHolder<Item, IsotopeItem> TECHNETIUM_99M = isotope("technetium_99m", Isotope.TECHNETIUM_99M, Rarity.RARE, 16);
    public static final DeferredHolder<Item, TechnetiumGeneratorItem> TECHNETIUM_GENERATOR = AllNuclearItems.ITEMS.register(
            "technetium_generator", () -> new TechnetiumGeneratorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, RtgPelletItem> STRONTIUM_RTG_PELLET = AllNuclearItems.ITEMS.register("strontium_rtg_pellet",
            () -> new RtgPelletItem(RtgFuel.STRONTIUM, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, RadiographyCameraItem> RADIOGRAPHY_CAMERA = AllNuclearItems.ITEMS.register("radiography_camera",
            () -> new RadiographyCameraItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, MedicalIsotopeKitItem> MEDICAL_ISOTOPE_KIT = AllNuclearItems.ITEMS.register("medical_isotope_kit",
            () -> new MedicalIsotopeKitItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GammaIrradiatorBlockEntity>> GAMMA_IRRADIATOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("gamma_irradiator", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new GammaIrradiatorBlockEntity(IsotopeRegistry.GAMMA_IRRADIATOR_ENTITY.get(), pos, state),
                    GAMMA_IRRADIATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IrradiationChannelBlockEntity>> IRRADIATION_CHANNEL_ENTITY =
            AllNuclearEntities.ENTITIES.register("reactor_irradiation_channel", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new IrradiationChannelBlockEntity(IsotopeRegistry.IRRADIATION_CHANNEL_ENTITY.get(), pos, state),
                    IRRADIATION_CHANNEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeDetectorBlockEntity>> SMOKE_DETECTOR_ENTITY =
            AllNuclearEntities.ENTITIES.register("smoke_detector", () -> BlockEntityType.Builder.of(
                    SmokeDetectorBlockEntity::new, SMOKE_DETECTOR.get()).build(null));

    private IsotopeRegistry() {}

    private static DeferredHolder<Item, IsotopeItem> isotope(String id, Isotope isotope, Rarity rarity, int stack) {
        return AllNuclearItems.ITEMS.register(id, () -> new IsotopeItem(isotope, new Item.Properties().rarity(rarity).stacksTo(stack)));
    }

    private static DeferredHolder<Item, BlockItem> blockItem(DeferredHolder<Block, ? extends Block> block, Rarity rarity) {
        return AllNuclearItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties().rarity(rarity)));
    }

    @Override
    public void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(Isotopes::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SterileFood::onTooltip);
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GAMMA_IRRADIATOR_ENTITY.get(), (be, side) -> be.sources());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IRRADIATION_CHANNEL_ENTITY.get(), (be, side) -> be.automation());
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, SectionedOutput output) {
        output.section(NuclearTabSection.MATERIALS);
        output.accept(COBALT_NUGGET.get());
        output.accept(COBALT_INGOT.get());
        output.section(NuclearTabSection.ISOTOPES);
        output.accept(COBALT_60.get());
        output.accept(STRONTIUM_90.get());
        output.accept(AMERICIUM_241.get());
        output.accept(TECHNETIUM_GENERATOR.get());
        output.accept(TECHNETIUM_99M.get());
        output.accept(STRONTIUM_RTG_PELLET.get());
        output.accept(IRRADIATION_CHANNEL_ITEM.get());
        output.accept(GAMMA_IRRADIATOR_ITEM.get());
        output.accept(SMOKE_DETECTOR_ITEM.get());
        output.accept(RADIOGRAPHY_CAMERA.get());
        output.accept(MEDICAL_ISOTOPE_KIT.get());
    }
}
