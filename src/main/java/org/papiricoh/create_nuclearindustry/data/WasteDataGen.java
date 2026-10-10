package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.data.recipe.WasteTreatmentRecipes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;
import org.papiricoh.create_nuclearindustry.waste.WasteForm;
import org.papiricoh.create_nuclearindustry.waste.WastePackage;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteTags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Lang, models, tags, data maps, loot and recipes for waste classes, treatments and the geological repository. */
final class WasteDataGen extends AreaDataGen {
    static final WasteDataGen INSTANCE = new WasteDataGen();

    private WasteDataGen() {}

    @Override
    void lang() {
        LangEntries.item("low_level_waste", "Low-Level Waste", "Residuo de baja actividad");
        LangEntries.item("high_level_waste", "High-Level Waste", "Residuo de alta actividad");
        LangEntries.block("compacted_waste_drum", "Compacted Waste Drum", "Bidón de residuos compactados");
        LangEntries.block("cemented_waste_drum", "Cemented Waste Drum", "Bidón de residuos cementados");
        LangEntries.block("vitrified_waste_canister", "Vitrified Waste Canister", "Contenedor de residuos vitrificados");
        LangEntries.block("vitrification_melter", "Vitrification Melter", "Fundidor de vitrificación");
        LangEntries.block("bentonite", "Bentonite", "Bentonita");
        LangEntries.block("repository_inspector", "Repository Inspector", "Inspector de repositorio");

        String melter = "create_nuclearindustry.goggles.vitrification_melter.";
        LangEntries.add(melter + "no_heat", "No heat: needs a lit Blaze Burner below",
                "Sin calor: necesita un quemador de blaze encendido debajo");
        LangEntries.add(melter + "heated", "Heated", "Calentado");
        LangEntries.add(melter + "superheated", "Superheated: melting twice as fast", "Sobrecalentado: funde el doble de rápido");
        LangEntries.add(melter + "melting", "Melting: %s%%", "Fundiendo: %s%%");
        LangEntries.add(melter + "idle", "Waiting for waste, sand and borax", "Esperando residuo, arena y bórax");

        String inspector = "create_nuclearindustry.repository_inspector.";
        LangEntries.add(inspector + "none", "No waste packages within %s blocks", "No hay bultos de residuos a menos de %s bloques");
        LangEntries.add(inspector + "stored", "Stored: %s of %s packages", "Almacenados: %s de %s bultos");
        LangEntries.add(inspector + "legacy", "Legacy depth rule is on", "La regla antigua de profundidad está activa");
        LangEntries.add(inspector + "weakest", "Weakest: %s at %s %s %s", "Más débil: %s en %s %s %s");
        LangEntries.add(inspector + "score", "Score: %s%% (stored from %s%%)", "Puntuación: %s%% (almacenado desde %s%%)");
        LangEntries.add(inspector + "depth", "Depth: %s of %s", "Profundidad: %s de %s");
        LangEntries.add(inspector + "host_rock", "Host rock: %s of %s", "Roca huésped: %s de %s");
        LangEntries.add(inspector + "backfill", "Backfill: %s of %s", "Relleno: %s de %s");
        LangEntries.add(inspector + "form", "Waste form: %s of %s", "Forma del residuo: %s de %s");
        LangEntries.add(inspector + "fluid", "Groundwater nearby: -%s", "Agua subterránea cerca: -%s");
        LangEntries.add(inspector + "summary", "%s/%s stored, weakest %s%%", "%s/%s almacenados, el peor %s%%");
        LangEntries.add(inspector + "summary_none", "No packages", "Sin bultos");
        LangEntries.add("create_nuclearindustry.display_source.repository_inspector", "Repository Status", "Estado del repositorio");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        drum(provider, WasteRegistry.COMPACTED_WASTE_DRUM.get(), "compacted_waste_drum");
        drum(provider, WasteRegistry.CEMENTED_WASTE_DRUM.get(), "cemented_waste_drum");
        provider.simpleBlockWithItem(WasteRegistry.VITRIFIED_WASTE_CANISTER.get(), provider.models().cubeColumn("vitrified_waste_canister",
                provider.modLoc("block/vitrified_waste_canister_side"), provider.modLoc("block/vitrified_waste_canister_top")));
        provider.simpleBlockWithItem(WasteRegistry.BENTONITE.get(), provider.cubeAll(WasteRegistry.BENTONITE.get()));
        provider.simpleBlockWithItem(WasteRegistry.VITRIFICATION_MELTER.get(), provider.models().cubeBottomTop("vitrification_melter",
                provider.modLoc("block/vitrification_melter_side"), provider.modLoc("block/vitrification_melter_bottom"),
                provider.modLoc("block/vitrification_melter_top")));
        ModelFile inspector = provider.models().orientable("repository_inspector", provider.modLoc("block/repository_inspector_side"),
                provider.modLoc("block/repository_inspector_front"), provider.modLoc("block/repository_inspector_top"));
        provider.horizontalBlock(WasteRegistry.REPOSITORY_INSPECTOR.get(), inspector);
        provider.simpleBlockItem(WasteRegistry.REPOSITORY_INSPECTOR.get(), inspector);
    }

    private static void drum(NuclearBlockStateProvider provider, Block block, String name) {
        ResourceLocation top = provider.modLoc("block/" + name + "_top");
        provider.simpleBlockWithItem(block, provider.models().cubeBottomTop(name, provider.modLoc("block/" + name + "_side"), top, top));
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(WasteRegistry.LOW_LEVEL_WASTE.get());
        provider.basicItem(WasteRegistry.HIGH_LEVEL_WASTE.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(AllNuclearTags.Blocks.GEOLOGICALLY_STORABLE).add(WasteRegistry.COMPACTED_WASTE_DRUM.get(),
                WasteRegistry.CEMENTED_WASTE_DRUM.get(), WasteRegistry.VITRIFIED_WASTE_CANISTER.get());
        provider.tagFor(WasteTags.REPOSITORY_BACKFILL).add(WasteRegistry.BENTONITE.get());
        provider.tagFor(BlockTags.MINEABLE_WITH_SHOVEL).add(WasteRegistry.BENTONITE.get());
        provider.tagFor(WasteTags.REPOSITORY_HOST_ROCK)
                .addTag(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                .add(Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
                        Blocks.CHISELED_DEEPSLATE, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_TILES,
                        Blocks.POLISHED_TUFF, Blocks.TUFF_BRICKS, Blocks.CHISELED_TUFF, Blocks.CHISELED_TUFF_BRICKS,
                        Blocks.SMOOTH_BASALT, Blocks.BASALT, Blocks.POLISHED_BASALT, Blocks.OBSIDIAN, Blocks.BEDROCK);
    }

    @Override
    void itemTags(NuclearItemTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(WasteTags.BARREL_WASTE).add(AllNuclearItems.RADIOACTIVE_WASTE.get(), WasteRegistry.HIGH_LEVEL_WASTE.get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // Low-level waste is mildly active; high-level waste (fission products) is fierce and warm.
        var radioactivity = provider.builderFor(AllNuclearRadiation.RADIOACTIVITY);
        radioactivity.add(item(WasteRegistry.LOW_LEVEL_WASTE), new Radioactivity(0.5f, false), false);
        radioactivity.add(item(WasteRegistry.HIGH_LEVEL_WASTE), new Radioactivity(15.0f, false, 0.5f), false);
        radioactivity.add(item(WasteRegistry.COMPACTED_WASTE_DRUM_ITEM), new Radioactivity(0.4f, false), false);
        radioactivity.add(item(WasteRegistry.CEMENTED_WASTE_DRUM_ITEM), new Radioactivity(1.5f, false), false);
        radioactivity.add(item(WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM), new Radioactivity(8.0f, false, 1.0f), false);

        // How leaky and how shielded each package is once placed, and the form the repository score counts.
        var packages = provider.builderFor(WastePackage.DATA_MAP);
        packages.add(block(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL), new WastePackage(1.0f, 1.0f, WasteForm.RAW), false);
        packages.add(block(WasteRegistry.COMPACTED_WASTE_DRUM), new WastePackage(0.5f, 1.0f, WasteForm.COMPACTED), false);
        packages.add(block(WasteRegistry.CEMENTED_WASTE_DRUM), new WastePackage(0.25f, 0.6f, WasteForm.CEMENTED), false);
        packages.add(block(WasteRegistry.VITRIFIED_WASTE_CANISTER), new WastePackage(0.05f, 0.5f, WasteForm.VITRIFIED), false);
    }

    private static net.minecraft.core.Holder<Item> item(Supplier<? extends Item> item) {
        return item.get().builtInRegistryHolder();
    }

    private static net.minecraft.core.Holder<Block> block(Supplier<? extends Block> block) {
        return block.get().builtInRegistryHolder();
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new WasteTreatmentRecipes(output, lookup));
    }
}
