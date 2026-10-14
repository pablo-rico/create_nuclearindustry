package org.papiricoh.create_nuclearindustry.explosive;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.explosive.block.ThermonuclearBombBlock;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.ThermonuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.explosive.gui.ThermonuclearBombMenu;

import java.util.List;
import java.util.function.Supplier;

/**
 * Nuclear weapons and what their detonations leave. The thermonuclear bomb and the parts every warhead
 * is assembled from (explosive lenses, neutron initiators, firing sets, implosion assemblies and casings,
 * with lithium deuteride for a thermonuclear secondary); trinitite (the glass the fireball fuses from the
 * crater's floor), charred logs, the flash's damage type, the jobs that work the crater and its zones
 * through ({@link NuclearBlastManager}) and the packet that shows clients the explosion
 * ({@link DetonationPayload}).
 */
public final class ExplosiveRegistry implements NuclearFeature {
    public static final ExplosiveRegistry INSTANCE = new ExplosiveRegistry();
    /** Burns from the thermal flash of a nuclear fireball; a fire damage, so Fire Protection helps. */
    public static final ResourceKey<DamageType> NUCLEAR_FLASH = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_flash"));

    public static final DeferredHolder<Block, Block> TRINITITE = AllNuclearBlocks.BLOCKS.register("trinitite",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5f, 6.0f)
                    .sound(SoundType.GLASS).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, RotatedPillarBlock> CHARRED_LOG = AllNuclearBlocks.BLOCKS.register("charred_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f)
                    .sound(SoundType.WOOD)));

    public static final DeferredHolder<Block, ThermonuclearBombBlock> THERMONUCLEAR_BOMB = AllNuclearBlocks.BLOCKS.register("thermonuclear_bomb",
            () -> new ThermonuclearBombBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(4.0f, 12.0f).noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> TRINITITE_ITEM = blockItem("trinitite", TRINITITE);
    public static final DeferredHolder<Item, BlockItem> CHARRED_LOG_ITEM = blockItem("charred_log", CHARRED_LOG);
    public static final DeferredHolder<Item, BlockItem> THERMONUCLEAR_BOMB_ITEM = blockItem("thermonuclear_bomb", THERMONUCLEAR_BOMB);

    /** Lithium-6 deuteride: the fusion fuel of a thermonuclear secondary, solid and stable until compressed. */
    public static final DeferredHolder<Item, Item> LITHIUM_DEUTERIDE = item("lithium_deuteride", 64);
    /** A shaped charge that focuses its blast inwards; eight of them crush a core to criticality. */
    public static final DeferredHolder<Item, Item> EXPLOSIVE_LENS = item("explosive_lens", 16);
    /** Beryllium and plutonium: a burst of neutrons the instant the core is crushed, so the chain reaction starts on time. */
    public static final DeferredHolder<Item, Item> NEUTRON_INITIATOR = item("neutron_initiator", 16);
    /** Fires every detonator of the lenses in the same instant. */
    public static final DeferredHolder<Item, Item> FIRING_SET = item("firing_set", 16);
    /** A beryllium tamper wrapped in explosive lenses, ready for a core. */
    public static final DeferredHolder<Item, Item> IMPLOSION_ASSEMBLY = item("implosion_assembly", 4);
    public static final DeferredHolder<Item, Item> BOMB_CASING = item("bomb_casing", 4);
    /** A radiation case: the primary's X-rays fill it and squeeze the secondary. */
    public static final DeferredHolder<Item, Item> THERMONUCLEAR_CASING = item("thermonuclear_casing", 4);

    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_EXPLOSIVE_LENS = incomplete("incomplete_explosive_lens");
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_FIRING_SET = incomplete("incomplete_firing_set");
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_IMPLOSION_ASSEMBLY = incomplete("incomplete_implosion_assembly");
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_NUCLEAR_BOMB = incomplete("incomplete_nuclear_bomb");
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_THERMONUCLEAR_BOMB = incomplete("incomplete_thermonuclear_bomb");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermonuclearBombBlockEntity>> THERMONUCLEAR_BOMB_ENTITY =
            AllNuclearEntities.ENTITIES.register("thermonuclear_bomb", () -> BlockEntityType.Builder.of(ThermonuclearBombBlockEntity::new,
                    THERMONUCLEAR_BOMB.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ThermonuclearBombMenu>> THERMONUCLEAR_BOMB_MENU =
            AllNuclearGUIs.MENUS.register("thermonuclear_bomb", () -> IMenuTypeExtension.create(ThermonuclearBombMenu::fromNetwork));

    /** The parts a thermonuclear cannon shell carries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ThermonuclearLoad>> THERMONUCLEAR_LOAD =
            AllNuclearDataComponents.DATA_COMPONENTS.registerComponentType("thermonuclear_load", builder -> builder
                    .persistent(ThermonuclearLoad.CODEC)
                    .networkSynchronized(ThermonuclearLoad.STREAM_CODEC));

    private ExplosiveRegistry() {}

    private static DeferredHolder<Item, BlockItem> blockItem(String name, Supplier<? extends Block> block) {
        return AllNuclearItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> item(String name, int stack) {
        return AllNuclearItems.ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(stack)));
    }

    private static DeferredHolder<Item, SequencedAssemblyItem> incomplete(String name) {
        return AllNuclearItems.ITEMS.register(name, () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));
    }

    public static List<DeferredHolder<Item, ? extends Item>> items() {
        return List.of(THERMONUCLEAR_BOMB_ITEM, LITHIUM_DEUTERIDE, EXPLOSIVE_LENS, NEUTRON_INITIATOR, FIRING_SET, IMPLOSION_ASSEMBLY,
                BOMB_CASING, THERMONUCLEAR_CASING, TRINITITE_ITEM, CHARRED_LOG_ITEM);
    }

    /** The parts a sequenced assembly passes through, which have no place in the creative tab. */
    public static List<DeferredHolder<Item, SequencedAssemblyItem>> incompleteItems() {
        return List.of(INCOMPLETE_EXPLOSIVE_LENS, INCOMPLETE_FIRING_SET, INCOMPLETE_IMPLOSION_ASSEMBLY, INCOMPLETE_NUCLEAR_BOMB,
                INCOMPLETE_THERMONUCLEAR_BOMB);
    }

    @Override
    public void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(NuclearBlastManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(NuclearBlastManager::onChunkLoad);
        DetonationPayload.register(modEventBus);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        items().forEach(item -> output.accept(item.get()));
    }
}
