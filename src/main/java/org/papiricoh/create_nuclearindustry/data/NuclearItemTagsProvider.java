package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

public class NuclearItemTagsProvider extends ItemTagsProvider {

    public NuclearItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                   CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES).add(
                AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get(), AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get());
        copy(AllNuclearTags.Blocks.C_ORES_URANIUM, itemTag(AllNuclearTags.Blocks.C_ORES_URANIUM));
        copy(AllNuclearTags.Blocks.C_ORES_LITHIUM, itemTag(AllNuclearTags.Blocks.C_ORES_LITHIUM));
        copy(AllNuclearTags.Blocks.C_ORES_BORAX, itemTag(AllNuclearTags.Blocks.C_ORES_BORAX));
        copy(AllNuclearTags.Blocks.C_ORES, itemTag(AllNuclearTags.Blocks.C_ORES));

        tag(AllNuclearTags.Items.C_RAW_URANIUM).add(AllNuclearItems.RAW_URANIUM.get());
        tag(AllNuclearTags.Items.C_RAW_LITHIUM).add(AllNuclearItems.RAW_SPODUMENE.get());
        tag(AllNuclearTags.Items.C_RAW_MATERIALS).addTags(AllNuclearTags.Items.C_RAW_URANIUM, AllNuclearTags.Items.C_RAW_LITHIUM);
        tag(AllNuclearTags.Items.C_GEMS_FLUORITE).add(AllNuclearItems.FLUORITE.get());

        tag(AllNuclearTags.Items.RAW_URANIUM)
                .add(AllNuclearItems.RAW_URANIUM.get())
                .addOptionalTag(AllNuclearTags.Items.C_RAW_URANIUM)
                .addOptionalTag(common("raw_ores/uranium"))
                .addOptionalTag(forge("raw_materials/uranium"))
                .addOptionalTag(forge("raw_ores/uranium"));
        tag(AllNuclearTags.Items.URANIUM_INGOTS)
                .add(AllNuclearItems.URANIUM.get())
                .addOptionalTag(AllNuclearTags.Items.C_INGOTS_URANIUM)
                .addOptionalTag(forge("ingots/uranium"));
        tag(TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "uranium")))
                .add(AllNuclearItems.URANIUM.get());
        AreaDataGen.ALL.forEach(area -> area.itemTags(this, provider));
    }

    /** {@link #tag} for the areas' datagen, which lives in other classes. */
    public IntrinsicTagAppender<Item> tagFor(TagKey<Item> tag) {
        return tag(tag);
    }

    /** {@link #copy} for the areas' datagen. */
    public void copyTag(TagKey<Block> blockTag, TagKey<Item> itemTag) {
        copy(blockTag, itemTag);
    }

    private static TagKey<Item> itemTag(TagKey<Block> blockTag) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, blockTag.location());
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> forge(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }
}
