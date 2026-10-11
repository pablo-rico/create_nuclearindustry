package org.papiricoh.create_nuclearindustry.fuelcycle;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.fuelcycle.fan.NuclearFanTypes;

import java.util.List;
import java.util.function.Supplier;

/**
 * Content for the renewable fuel cycle: base materials won from rock, sand and water by the acid
 * leaching and irradiation fan treatments ({@link NuclearFanTypes}), and the thorium fuel that
 * breeds uranium-233. Items register here; the fluids live in {@link AllNuclearFluids}.
 */
public final class FuelCycleRegistry implements NuclearFeature {
    public static final FuelCycleRegistry INSTANCE = new FuelCycleRegistry();

    // ---------------------------------------------------------------- rock, sand and sea water
    public static final DeferredHolder<Item, Item> GRANITE_FINES = item("granite_fines");
    public static final DeferredHolder<Item, Item> TUFF_FINES = item("tuff_fines");
    public static final DeferredHolder<Item, Item> DEEPSLATE_FINES = item("deepslate_fines");
    public static final DeferredHolder<Item, Item> HEAVY_MINERAL_SAND = item("heavy_mineral_sand");
    public static final DeferredHolder<Item, Item> AMIDOXIME_ADSORBENT = item("amidoxime_adsorbent");
    public static final DeferredHolder<Item, Item> LOADED_ADSORBENT = item("loaded_adsorbent");
    /** What acid leaching washes off contaminated items: drained into contaminated water. */
    public static final DeferredHolder<Item, Item> RADIOACTIVE_SLUDGE = item("radioactive_sludge");

    // ---------------------------------------------------------------- thorium and uranium-233
    public static final DeferredHolder<Item, Item> THORIUM_DIOXIDE = item("thorium_dioxide");
    public static final DeferredHolder<Item, Item> IRRADIATED_THORIUM = item("irradiated_thorium");
    public static final DeferredHolder<Item, Item> IRRADIATED_DEPLETED_URANIUM = item("irradiated_depleted_uranium");
    public static final DeferredHolder<Item, Item> URANIUM_233_DIOXIDE = item("uranium_233_dioxide");
    public static final DeferredHolder<Item, Item> THORIUM_FUEL_PELLET = item("thorium_fuel_pellet");
    public static final DeferredHolder<Item, Item> THORIUM_REACTOR_FUEL = AllNuclearItems.ITEMS.register("thorium_reactor_fuel",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> DEPLETED_THORIUM_REACTOR_FUEL = AllNuclearItems.ITEMS.register(
            "depleted_thorium_reactor_fuel", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> THORIUM_SPENT_FRAGMENTS = item("thorium_spent_fragments");
    /** A thorium assembly half way through sequenced assembly; not in the creative tab, like the other fuels'. */
    public static final DeferredHolder<Item, SequencedAssemblyItem> INCOMPLETE_THORIUM_REACTOR_FUEL = AllNuclearItems.ITEMS.register(
            "incomplete_thorium_reactor_fuel", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

    // ---------------------------------------------------------------- the irradiation fan treatment's catalyst
    public static final DeferredHolder<Block, IrradiationSourceBlock> IRRADIATION_SOURCE = AllNuclearBlocks.BLOCKS.register(
            "irradiation_source", () -> new IrradiationSourceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(4.0f, 8.0f).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> 6)));
    public static final DeferredHolder<Item, BlockItem> IRRADIATION_SOURCE_ITEM = AllNuclearItems.ITEMS.register(
            "irradiation_source", () -> new BlockItem(IRRADIATION_SOURCE.get(), new Item.Properties()));

    /** Every item of this area, in creative tab order (the irradiation source first: it is a block). */
    public static final List<Supplier<? extends Item>> ITEMS = List.of(IRRADIATION_SOURCE_ITEM, GRANITE_FINES, TUFF_FINES, DEEPSLATE_FINES,
            HEAVY_MINERAL_SAND, AMIDOXIME_ADSORBENT, LOADED_ADSORBENT, RADIOACTIVE_SLUDGE, THORIUM_DIOXIDE, IRRADIATED_THORIUM,
            IRRADIATED_DEPLETED_URANIUM, URANIUM_233_DIOXIDE, THORIUM_FUEL_PELLET, THORIUM_REACTOR_FUEL,
            DEPLETED_THORIUM_REACTOR_FUEL, THORIUM_SPENT_FRAGMENTS);

    private FuelCycleRegistry() {}

    private static DeferredHolder<Item, Item> item(String id) {
        return AllNuclearItems.ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    @Override
    public void register(IEventBus modEventBus) {
        NuclearFanTypes.register(modEventBus);
    }

    @Override
    public void fillCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        ITEMS.forEach(item -> output.accept(item.get()));
        output.accept(AllNuclearFluids.SULFURIC_ACID_BUCKET.get());
    }
}
