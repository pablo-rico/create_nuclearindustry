package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.DecontaminationRecipes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Lang, models, data maps, loot and recipes for the Decontamination Sprinkler and phytoremediation.
 * The sprinkler's and the irradiated sunflower's block and item models are written by hand.
 */
final class DecontaminationDataGen extends AreaDataGen {
    static final DecontaminationDataGen INSTANCE = new DecontaminationDataGen();
    private static final String SPRINKLER = "create_nuclearindustry.decontamination_sprinkler.";

    private DecontaminationDataGen() {}

    @Override
    void lang() {
        LangEntries.block("decontamination_sprinkler", "Decontamination Sprinkler", "Rociador de descontaminación");
        LangEntries.block("irradiated_sunflower", "Irradiated Sunflower", "Girasol irradiado");
        LangEntries.item("irradiated_biomass", "Irradiated Biomass", "Biomasa irradiada");

        LangEntries.add(SPRINKLER + "spraying", "Spraying: washing off %s rad/s each second",
                "Rociando: lava %s rad/s cada segundo");
        LangEntries.add(SPRINKLER + "no_rotation", "Needs rotation", "Necesita rotación");
        LangEntries.add(SPRINKLER + "no_water", "No water: pipe some in", "Sin agua: mete agua por tubería");
        LangEntries.add(SPRINKLER + "full", "Contaminated water tank full: pipe it out",
                "Depósito de agua contaminada lleno: sácala por tubería");
        LangEntries.add(SPRINKLER + "water", "Water: %s / %s mB", "Agua: %s / %s mB");
        LangEntries.add(SPRINKLER + "contaminated", "Contaminated water: %s / %s mB", "Agua contaminada: %s / %s mB");

        LangEntries.add("jei.create_nuclearindustry.info.decontamination_sprinkler",
                "Washes fallout off the ground of its chunk and the eight around it, much faster than a Radiation Scrubber. Pipe water into it and drive it with a shaft from below; it works faster up to 256 RPM. What it washes off collects as Contaminated Water: pipe it out to a centrifuge, or the sprinkler stops when its tank is full. Standing in its spray washes your own dose off.",
                "Lava la lluvia radiactiva del suelo de su chunk y de los ocho de alrededor, mucho más rápido que un Absorbedor de radiación. Mete agua por tubería y acciónalo con un eje desde abajo; va más rápido hasta 256 RPM. Lo que lava se acumula como Agua contaminada: sácala por tubería hacia una centrífuga, o el rociador se para cuando su depósito se llena. Ponerte bajo su chorro te lava la dosis.");
        LangEntries.add("jei.create_nuclearindustry.info.irradiated_sunflower",
                "Sunflowers planted on contaminated ground draw the fallout out of their chunk, a little at a time, until they wilt into Irradiated Sunflowers. Break those for Irradiated Biomass and press four into Low-Level Waste.",
                "Los girasoles plantados en suelo contaminado sacan poco a poco la lluvia radiactiva de su chunk, hasta que se marchitan en Girasoles irradiados. Rómpelos para obtener Biomasa irradiada y prensa cuatro para hacer un Residuo de baja actividad.");

        String ponder = "create_nuclearindustry.ponder.decontamination_sprinkler.";
        LangEntries.add(ponder + "header", "Decontamination Sprinklers", "Rociadores de descontaminación");
        LangEntries.add(ponder + "text_1",
                "A Decontamination Sprinkler washes fallout off the ground of its chunk and the eight around it. Drive it from below: it works faster up to 256 RPM.",
                "Un Rociador de descontaminación lava la lluvia radiactiva del suelo de su chunk y de los ocho de alrededor. Acciónalo desde abajo: va más rápido hasta 256 RPM.");
        LangEntries.add(ponder + "text_2",
                "It sprays water: pipe it in from any side.",
                "Rocía agua: métela por tubería por cualquier lado.");
        LangEntries.add(ponder + "text_3",
                "What it washes off collects as Contaminated Water. Pipe it out to a centrifuge: with its tank full, the sprinkler stops.",
                "Lo que lava se acumula como Agua contaminada. Sácala por tubería hacia una centrífuga: con su depósito lleno, el rociador se para.");
        LangEntries.add(ponder + "text_4",
                "Stand in its spray to wash your own dose off.",
                "Ponte bajo su chorro para lavarte la dosis.");
        LangEntries.add(ponder + "text_5",
                "Sunflowers on contaminated ground do it slowly by themselves, until they wilt into Irradiated Sunflowers: their biomass is low-level waste.",
                "Los girasoles en suelo contaminado lo hacen solos y despacio, hasta que se marchitan en Girasoles irradiados: su biomasa es residuo de baja actividad.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile sprinkler = provider.models().getExistingFile(provider.modLoc("block/decontamination_sprinkler/block"));
        provider.simpleBlock(DecontaminationRegistry.SPRINKLER.get(), sprinkler);

        ModelFile bottom = provider.models().getExistingFile(provider.modLoc("block/irradiated_sunflower_bottom"));
        ModelFile top = provider.models().getExistingFile(provider.modLoc("block/irradiated_sunflower_top"));
        provider.getVariantBuilder(DecontaminationRegistry.IRRADIATED_SUNFLOWER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? bottom : top).build());
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(DecontaminationRegistry.IRRADIATED_BIOMASS.get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        provider.builderFor(AllNuclearRadiation.RADIOACTIVITY).add(DecontaminationRegistry.IRRADIATED_BIOMASS.get().builtInRegistryHolder(),
                new Radioactivity(0.1f, false), false);
    }

    /** Only the lower half drops, one or two handfuls of biomass. */
    @Override
    void blockLoot(NuclearBlockLoot loot) {
        var sunflower = DecontaminationRegistry.IRRADIATED_SUNFLOWER.get();
        loot.addTable(sunflower, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(DecontaminationRegistry.IRRADIATED_BIOMASS.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(sunflower)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)))));
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new DecontaminationRecipes(output, lookup));
    }
}
