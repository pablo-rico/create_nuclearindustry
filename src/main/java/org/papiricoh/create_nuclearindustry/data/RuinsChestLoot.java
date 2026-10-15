package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.ruins.RuinsLoot;

import java.util.function.BiConsumer;

/**
 * Chest loot of the abandoned sites: early and mid-game materials, worn safety gear, medicine and
 * spare parts that help the player along. Never anything enriched or weapons-grade: no enriched
 * uranium, no plutonium, no cores, lenses, firing sets or bomb parts (a GameTest rolls every table).
 */
public final class RuinsChestLoot implements LootTableSubProvider {

    public RuinsChestLoot(HolderLookup.Provider registries) {}

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(RuinsLoot.PLANT_REACTOR, table(3, 6,
                stack(mod("graphite"), 10, 2, 8), stack(mod("boron"), 8, 1, 4), stack(mod("beryllium"), 5, 1, 3),
                stack(mod("control_rod"), 4, 1, 2), stack(mod("heat_exchanger"), 4, 1, 2), stack(mod("dual_fluid_pipe"), 6, 2, 6),
                stack(mod("uranium_concentrate"), 5, 1, 4), worn(mod("hazmat_helmet"), 3), worn(mod("hazmat_boots"), 3),
                stack(mod("potassium_iodide"), 4, 1, 2), stack(create("iron_sheet"), 6, 2, 6)));
        output.accept(RuinsLoot.PLANT_TURBINE, table(3, 6,
                stack(create("iron_sheet"), 10, 2, 8), stack(create("copper_sheet"), 8, 2, 6), stack(create("andesite_alloy"), 8, 2, 8),
                stack(mod("dual_fluid_pipe"), 6, 2, 6), stack(mod("heat_exchanger"), 3, 1, 2), worn(mod("maintenance_kit"), 3),
                stack(Items.COPPER_INGOT, 6, 2, 6), stack(create("precision_mechanism"), 1, 1, 1)));
        output.accept(RuinsLoot.PLANT_CONTROL, table(3, 5,
                stack(mod("gauge_module"), 5, 1, 2), stack(mod("dial_module"), 5, 1, 2), stack(mod("lamp_module"), 6, 1, 3),
                stack(mod("push_button_module"), 5, 1, 2), stack(mod("lever_module"), 5, 1, 2), stack(mod("bar_graph_module"), 3, 1, 1),
                stack(mod("chart_roll"), 6, 1, 3), stack(mod("geiger_counter"), 2, 1, 1), stack(mod("potassium_iodide"), 4, 1, 2),
                stack(Items.PAPER, 6, 2, 8)));
        output.accept(RuinsLoot.PLANT_WASTE, table(2, 5,
                stack(mod("yellowcake"), 8, 1, 4), stack(mod("uranium_concentrate"), 6, 1, 3), stack(mod("borax_salt"), 6, 2, 6),
                worn(mod("hazmat_chestplate"), 3), worn(mod("hazmat_leggings"), 3), stack(mod("prussian_blue"), 4, 1, 2),
                stack(mod("potassium_iodide"), 4, 1, 2)));

        output.accept(RuinsLoot.SHELTER_DORMITORY, table(2, 5,
                ration(Items.BREAD, 8, 1, 4), ration(Items.BAKED_POTATO, 6, 1, 4), stack(mod("potassium_iodide"), 6, 1, 3),
                stack(mod("prussian_blue"), 3, 1, 2), stack(mod("geiger_counter"), 2, 1, 1), worn(mod("hazmat_helmet"), 2),
                stack(Items.TORCH, 6, 2, 8), stack(Items.CANDLE, 4, 1, 3)));
        output.accept(RuinsLoot.SHELTER_SUPPLIES, table(4, 8,
                ration(Items.BREAD, 10, 2, 6), ration(Items.BAKED_POTATO, 8, 2, 6), ration(Items.DRIED_KELP, 8, 3, 10),
                ration(Items.COOKED_BEEF, 5, 1, 4), stack(mod("potassium_iodide"), 6, 1, 4), stack(mod("prussian_blue"), 4, 1, 2),
                worn(mod("maintenance_kit"), 2), stack(create("iron_sheet"), 4, 1, 4), stack(Items.GLASS_BOTTLE, 4, 1, 4)));

        output.accept(RuinsLoot.BUNKER_GUARD, table(2, 4,
                ration(Items.BREAD, 6, 1, 3), stack(mod("potassium_iodide"), 5, 1, 2), stack(mod("geiger_counter"), 2, 1, 1),
                stack(Items.ARROW, 5, 4, 12), stack(Items.IRON_NUGGET, 5, 3, 9), stack(Items.TORCH, 4, 2, 6)));
        output.accept(RuinsLoot.BUNKER_COMMAND, table(3, 5,
                stack(mod("gauge_module"), 5, 1, 2), stack(mod("dial_module"), 4, 1, 2), stack(mod("lamp_module"), 5, 1, 3),
                stack(mod("push_button_module"), 4, 1, 2), stack(mod("chart_roll"), 6, 1, 3), stack(mod("geiger_counter"), 3, 1, 1),
                stack(mod("potassium_iodide"), 4, 1, 2), stack(Items.PAPER, 6, 2, 8), stack(Items.COMPASS, 2, 1, 1)));
        output.accept(RuinsLoot.BUNKER_DORMITORY, table(2, 4,
                ration(Items.BREAD, 8, 1, 4), ration(Items.DRIED_KELP, 6, 2, 8), stack(mod("potassium_iodide"), 6, 1, 3),
                stack(mod("prussian_blue"), 3, 1, 2), stack(Items.CANDLE, 4, 1, 3), worn(mod("hazmat_boots"), 2)));
        output.accept(RuinsLoot.BUNKER_ARMORY, table(3, 6,
                worn(mod("depleted_uranium_helmet"), 3), worn(mod("depleted_uranium_chestplate"), 2),
                worn(mod("depleted_uranium_leggings"), 2), worn(mod("depleted_uranium_boots"), 3), worn(Items.CROSSBOW, 3),
                stack(Items.ARROW, 6, 6, 16), stack(Items.IRON_INGOT, 6, 2, 6), stack(create("iron_sheet"), 5, 2, 6),
                worn(mod("hazmat_chestplate"), 2)));
        output.accept(RuinsLoot.BUNKER_LAB, table(3, 6,
                stack(mod("cobalt_nugget"), 6, 2, 8), stack(mod("cobalt_ingot"), 3, 1, 2), stack(mod("lithium"), 6, 1, 4),
                stack(mod("beryllium"), 5, 1, 3), stack(mod("boron"), 6, 1, 4), stack(mod("lithium_carbonate"), 5, 1, 4),
                stack(mod("geiger_counter"), 2, 1, 1), stack(mod("prussian_blue"), 4, 1, 2), stack(Items.GLASS_BOTTLE, 5, 1, 4),
                stack(create("precision_mechanism"), 1, 1, 1)));
        output.accept(RuinsLoot.BUNKER_WASTE, table(2, 4,
                stack(mod("yellowcake"), 8, 1, 4), stack(mod("uranium_concentrate"), 6, 1, 3), worn(mod("hazmat_chestplate"), 3),
                worn(mod("hazmat_leggings"), 3), worn(mod("maintenance_kit"), 2), stack(mod("prussian_blue"), 5, 1, 2)));
    }

    private static LootTable.Builder table(int minRolls, int maxRolls, LootPoolSingletonContainer.Builder<?>... entries) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(UniformGenerator.between(minRolls, maxRolls));
        for (LootPoolSingletonContainer.Builder<?> entry : entries) {
            pool.add(entry);
        }
        return LootTable.lootTable().withPool(pool);
    }

    private static LootPoolSingletonContainer.Builder<?> stack(ItemLike item, int weight, int min, int max) {
        LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(item).setWeight(weight);
        return min == max && min == 1 ? entry : entry.apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }

    /** Gear left behind for years: a third to four fifths worn. */
    private static LootPoolSingletonContainer.Builder<?> worn(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.2f, 0.65f)));
    }

    /** Shelter rations, sterilised by gamma rays so they kept. */
    private static LootPoolSingletonContainer.Builder<?> ration(ItemLike item, int weight, int min, int max) {
        return stack(item, weight, min, max).apply(SetComponentsFunction.setComponent(IsotopeRegistry.IRRADIATED.get(), Unit.INSTANCE));
    }

    private static Item mod(String id) {
        return item(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, id));
    }

    private static Item create(String id) {
        return item(ResourceLocation.fromNamespaceAndPath("create", id));
    }

    private static Item item(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("ruin loot names a missing item: " + id);
        }
        return item;
    }
}
