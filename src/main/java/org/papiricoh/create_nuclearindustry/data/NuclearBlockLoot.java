package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;

import java.util.Set;

public class NuclearBlockLoot extends BlockLootSubProvider {

    public NuclearBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(AllNuclearBlocks.URANIUM_ORE.get(), block -> createOreDrop(block, AllNuclearItems.RAW_URANIUM.get()));
        add(AllNuclearBlocks.SPODUMENE_ORE.get(), block -> createOreDrop(block, AllNuclearItems.RAW_SPODUMENE.get()));
        add(AllNuclearBlocks.BORAX_ORE.get(), block -> createMultipleOreDrops(block, AllNuclearItems.BORAX_SALT.get(), 2, 3));


        for (Block block : getKnownBlocks()) {
            if (!map.containsKey(block.getLootTable())) {
                dropSelf(block);
            }
        }
    }

    /** Like copper ore: a few items per block, multiplied by Fortune, the block itself with Silk Touch. */
    private LootTable.Builder createMultipleOreDrops(Block block, Item item, float min, float max) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        return createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return AllNuclearBlocks.BLOCKS.getEntries().stream().map(Holder::value).map(Block.class::cast).toList();
    }
}
