package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.SpentFuelRecipes;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.block.RackLoad;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlock;

import java.util.concurrent.CompletableFuture;

/** Lang, models, tags, data maps, loot and recipes for spent fuel cooling: pool racks and dry casks. */
final class SpentFuelDataGen extends AreaDataGen {
    static final SpentFuelDataGen INSTANCE = new SpentFuelDataGen();
    private static final String P = "create_nuclearindustry.spent_fuel.";

    private SpentFuelDataGen() {}

    @Override
    void lang() {
        LangEntries.block("spent_fuel_rack", "Spent Fuel Rack", "Bastidor de combustible gastado");
        LangEntries.block("pool_cooler", "Pool Cooler", "Refrigerador de piscina");
        LangEntries.block("dry_cask", "Dry Storage Cask", "Contenedor de almacenamiento en seco");

        LangEntries.add(P + "grade.hot", "Hot", "Caliente");
        LangEntries.add(P + "grade.warm", "Warm", "Templado");
        LangEntries.add(P + "grade.cooled", "Cooled", "Enfriado");
        LangEntries.add(P + "grade.aged", "Aged", "Envejecido");
        LangEntries.add(P + "tooltip.grade", "Cooling: %s", "Enfriamiento: %s");
        LangEntries.add(P + "tooltip.days", "Discharged %s days ago", "Descargado hace %s días");
        LangEntries.add(P + "refused", "%s fuel is too hot for a dry cask: cool it in a pool first",
                "El combustible %s está demasiado caliente para un contenedor seco: enfríalo antes en una piscina");
        LangEntries.add(P + "full", "No free cell left", "No queda ninguna celda libre");

        LangEntries.add(P + "goggles.assemblies", "Assemblies: %s / %s", "Elementos: %s / %s");
        LangEntries.add(P + "goggles.hottest", "Hottest: %s", "El más caliente: %s");
        LangEntries.add(P + "goggles.water_above", "Water above: %s blocks", "Agua encima: %s bloques");
        LangEntries.add(P + "goggles.uncovered", "Not covered by water!", "¡Sin agua encima!");
        LangEntries.add(P + "goggles.radiation_out", "Radiation let out: %s%%", "Radiación que escapa: %s%%");
        LangEntries.add(P + "goggles.decay_heat", "Decay heat: %s kW", "Calor residual: %s kW");
        LangEntries.add(P + "goggles.pool_cooling", "Pool cooling: %s kW", "Refrigeración de la piscina: %s kW");
        LangEntries.add(P + "goggles.boiling", "The pool is boiling off!", "¡El agua de la piscina se evapora!");
        LangEntries.add(P + "goggles.cask_rule", "Takes %s fuel or cooler", "Admite combustible %s o más frío");
        LangEntries.add(P + "goggles.cooler", "Pool cooling: %s kW", "Refrigeración de la piscina: %s kW");
        LangEntries.add(P + "goggles.cooler_dry", "Only works under water", "Solo funciona bajo el agua");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        var models = provider.models();
        ResourceLocation rackSide = provider.modLoc("block/spent_fuel_rack_side");
        ResourceLocation rackBottom = provider.modLoc("block/spent_fuel_rack_bottom");
        ModelFile empty = models.cubeBottomTop("spent_fuel_rack", rackSide, rackBottom, provider.modLoc("block/spent_fuel_rack_top"));
        ModelFile loaded = models.cubeBottomTop("spent_fuel_rack_loaded", rackSide, rackBottom,
                provider.modLoc("block/spent_fuel_rack_top_loaded"));
        ModelFile glowing = models.cubeBottomTop("spent_fuel_rack_glowing", rackSide, rackBottom,
                provider.modLoc("block/spent_fuel_rack_top_glowing"));
        provider.getVariantBuilder(SpentFuelRegistry.SPENT_FUEL_RACK.get()).forAllStatesExcept(state -> {
            RackLoad load = state.getValue(SpentFuelRackBlock.LOAD);
            ModelFile model = load == RackLoad.EMPTY ? empty : load == RackLoad.LOADED ? loaded : glowing;
            return ConfiguredModel.builder().modelFile(model).build();
        }, SpentFuelRackBlock.WATERLOGGED);
        provider.simpleBlockItem(SpentFuelRegistry.SPENT_FUEL_RACK.get(), loaded);

        ModelFile cooler = models.cubeBottomTop("pool_cooler", provider.modLoc("block/pool_cooler_side"),
                provider.modLoc("block/pool_cooler_bottom"), provider.modLoc("block/pool_cooler_top"));
        provider.getVariantBuilder(SpentFuelRegistry.POOL_COOLER.get())
                .forAllStates(state -> ConfiguredModel.builder().modelFile(cooler).build());
        provider.simpleBlockItem(SpentFuelRegistry.POOL_COOLER.get(), cooler);

        ResourceLocation caskTop = provider.modLoc("block/dry_cask_top");
        ModelFile cask = models.cubeBottomTop("dry_cask", provider.modLoc("block/dry_cask_side"), caskTop, caskTop);
        provider.simpleBlockWithItem(SpentFuelRegistry.DRY_CASK.get(), cask);
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // The cask's steel and concrete; its own contents are held in by spentFuel.caskTransmission.
        provider.builderFor(AllNuclearRadiation.TRANSMISSION)
                .add(SpentFuelRegistry.DRY_CASK.get().builtInRegistryHolder(), new RadiationTransmission(0.02f), false);
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), named("Spent fuel crafting recipes", new SpentFuelRecipes(output, lookup)));
    }

    /** Vanilla recipe providers all call themselves "Recipes", and the generator refuses two providers with one name. */
    private static DataProvider named(String name, DataProvider provider) {
        return new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return provider.run(cache);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
