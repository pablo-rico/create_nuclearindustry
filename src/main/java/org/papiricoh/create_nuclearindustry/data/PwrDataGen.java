package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.data.recipe.PwrCraftingRecipes;
import org.papiricoh.create_nuclearindustry.data.recipe.PwrMixingRecipes;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.PressurizerHeaterPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.ReliefValveBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortRole;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Lang, models, tags, data maps, loot and recipes for the pressurized water loop, steam generator, containment and the accident chain. */
final class PwrDataGen extends AreaDataGen {
    static final PwrDataGen INSTANCE = new PwrDataGen();

    private PwrDataGen() {}

    @Override
    void lang() {
        LangEntries.block("pressurizer", "Pressurizer", "Presionador");
        LangEntries.block("pressurizer_heater_port", "Pressurizer Heater Port", "Puerto calefactor del presionador");
        LangEntries.block("relief_valve", "Relief Valve", "Válvula de alivio");
        LangEntries.block("steam_generator_controller", "Steam Generator Controller", "Controlador del generador de vapor");
        LangEntries.block("steam_generator_casing", "Steam Generator Casing", "Carcasa del generador de vapor");
        LangEntries.block("steam_generator_tube_bundle", "Steam Generator Tube Bundle", "Haz de tubos del generador de vapor");
        LangEntries.block("steam_generator_port", "Steam Generator Port", "Puerto del generador de vapor");
        LangEntries.fluid("primary_coolant", "Primary Coolant", "Refrigerante primario");
        LangEntries.block("primary_coolant", "Primary Coolant", "Refrigerante primario");
        LangEntries.item("primary_coolant_bucket", "Primary Coolant Bucket", "Cubo de refrigerante primario");
        LangEntries.fluid("hot_primary_coolant", "Hot Primary Coolant", "Refrigerante primario caliente");

        String source = "create_nuclearindustry.display_source.";
        LangEntries.add(source + "reactor_pressure", "Primary Pressure", "Presión del primario");
        LangEntries.add(source + "reactor_hot_leg", "Hot Leg Temperature", "Temperatura de la rama caliente");
        LangEntries.add(source + "reactor_cold_leg", "Cold Leg Temperature", "Temperatura de la rama fría");
        LangEntries.add(source + "reactor_subcooling", "Subcooling Margin", "Margen de subenfriamiento");
        LangEntries.add(source + "reactor_pressurizer_level", "Pressurizer Level", "Nivel del presionador");
        LangEntries.add("create_nuclearindustry.control_room.kind.steam_generator", "Steam Generator", "Generador de vapor");

        String pwr = "create_nuclearindustry.pwr.";
        LangEntries.add(pwr + "heaters.automatic", "Heaters: automatic, they hold the pressure setpoint",
                "Calentadores: automáticos, mantienen la presión de consigna");
        LangEntries.add(pwr + "heaters.manual", "Heaters: manual, on while powered by redstone",
                "Calentadores: manuales, encendidos con señal de redstone");
        LangEntries.add(pwr + "port.cold_leg", "Cold leg: primary coolant in", "Rama fría: entra refrigerante primario");
        LangEntries.add(pwr + "port.hot_leg", "Hot leg: hot primary coolant out", "Rama caliente: sale refrigerante primario caliente");
        String goggles = pwr + "goggles.";
        LangEntries.add(goggles + "header", "Pressurized loop:", "Circuito a presión:");
        LangEntries.add(goggles + "pressure", " Pressure: %s bar, subcooling %s °C", " Presión: %s bar, subenfriamiento %s °C");
        LangEntries.add(goggles + "legs", " Hot leg %s °C, cold leg %s °C", " Rama caliente %s °C, rama fría %s °C");
        LangEntries.add(goggles + "pressurizer", " Pressurizer: %s%% full, heaters %s", " Presionador: %s%% lleno, calentadores %s");
        LangEntries.add(goggles + "heaters_on", "on", "encendidos");
        LangEntries.add(goggles + "heaters_off", "off", "apagados");
        LangEntries.add(goggles + "flow", " Core flow: %s mB/t, core covered %s%%", " Caudal en el núcleo: %s mB/t, núcleo cubierto %s%%");
        LangEntries.add(goggles + "relief_open", " Relief valve OPEN", " Válvula de alivio ABIERTA");
        LangEntries.add(goggles + "relief_stuck", " Relief valve STUCK OPEN: a redstone signal on it shuts its block valve",
                " Válvula de alivio ATASCADA ABIERTA: una señal de redstone cierra su válvula de bloqueo");
        LangEntries.add(goggles + "no_pressurizer", "Primary coolant without a pressurizer on the lid: the loop is not running",
                "Refrigerante primario sin presionador en la tapa: el circuito no funciona");

        String sg = "create_nuclearindustry.steam_generator.";
        LangEntries.add(sg + "role.primary_in", "Primary in", "Entrada del primario");
        LangEntries.add(sg + "role.primary_out", "Primary out", "Salida del primario");
        LangEntries.add(sg + "role.feed_in", "Feedwater in", "Entrada de agua de alimentación");
        LangEntries.add(sg + "role.steam_out", "Steam out", "Salida de vapor");
        LangEntries.add(sg + "port_role", "Port: %s", "Puerto: %s");
        LangEntries.add(sg + "port_hint", "Pipes on any side; the wrench changes what it carries",
                "Tuberías por cualquier lado; la llave inglesa cambia lo que lleva");
        LangEntries.add(sg + "linked", "Linked", "Enlazado");
        LangEntries.add(sg + "unlinked", "No steam generator", "Sin generador de vapor");
        LangEntries.add(sg + "status.dry", "DRY", "SECO");
        String sgGoggles = sg + "goggles.";
        LangEntries.add(sgGoggles + "size", "3x3x%s, %s tube bundles", "3x3x%s, %s haces de tubos");
        LangEntries.add(sgGoggles + "steam", "Steam: %s mB/t at %s bar", "Vapor: %s mB/t a %s bar");
        LangEntries.add(sgGoggles + "heat", "Heat: %s MW, primary cooled %s °C", "Calor: %s MW, primario enfriado %s °C");
        LangEntries.add(sgGoggles + "primary", "Primary: %s mB hot, %s mB cooled", "Primario: %s mB caliente, %s mB enfriado");
        LangEntries.add(sgGoggles + "feed", "Feedwater: %s / %s mB, steam %s mB", "Agua de alimentación: %s / %s mB, vapor %s mB");
        LangEntries.add(sgGoggles + "drying", "Tubes drying out: %s%% wet", "Tubos secándose: %s%% mojados");
        String error = sg + "error.";
        LangEntries.add(error + "not_formed", "Not formed", "Sin formar");
        LangEntries.add(error + "no_controller", "Controller missing", "Falta el controlador");
        LangEntries.add(error + "no_tubes", "The controller must face out with a Tube Bundle behind it",
                "El controlador debe mirar hacia fuera con un Haz de tubos detrás");
        LangEntries.add(error + "height", "Height must be %s to %s blocks, found %s", "La altura debe ser de %s a %s bloques; hay %s");
        LangEntries.add(error + "tube", "Tube Bundle missing at %s %s %s", "Falta un Haz de tubos en %s %s %s");
        LangEntries.add(error + "casing", "Casing missing at %s %s %s", "Falta carcasa en %s %s %s");
        LangEntries.add(error + "two_controllers", "Second controller at %s %s %s", "Segundo controlador en %s %s %s");
        LangEntries.add(error + "missing_port", "Needs a port set to %s", "Necesita un puerto en modo %s");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        // Fluid blocks render from their fluid type; the model only needs a particle texture.
        provider.simpleBlock(AllNuclearFluids.PRIMARY_COOLANT_BLOCK.get(),
                provider.models().getBuilder("primary_coolant").texture("particle", provider.mcLoc("block/water_still")));

        ResourceLocation pressurizerTop = provider.modLoc("block/pressurizer_top");
        ModelFile pressurizer = provider.models().cubeColumn("pressurizer", provider.modLoc("block/pressurizer_side"), pressurizerTop);
        provider.simpleBlockWithItem(PwrRegistry.PRESSURIZER.get(), pressurizer);

        ModelFile heaterAuto = provider.models().cubeColumn("pressurizer_heater_port",
                provider.modLoc("block/pressurizer_heater_port"), pressurizerTop);
        ModelFile heaterManual = provider.models().cubeColumn("pressurizer_heater_port_manual",
                provider.modLoc("block/pressurizer_heater_port_manual"), pressurizerTop);
        provider.getVariantBuilder(PwrRegistry.PRESSURIZER_HEATER_PORT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(PressurizerHeaterPortBlock.AUTOMATIC) ? heaterAuto : heaterManual).build());
        provider.simpleBlockItem(PwrRegistry.PRESSURIZER_HEATER_PORT.get(), heaterAuto);

        ModelFile valveShut = provider.models().cubeBottomTop("relief_valve", provider.modLoc("block/relief_valve_side"),
                pressurizerTop, provider.modLoc("block/relief_valve_top"));
        ModelFile valveOpen = provider.models().cubeBottomTop("relief_valve_open", provider.modLoc("block/relief_valve_side_open"),
                pressurizerTop, provider.modLoc("block/relief_valve_top"));
        provider.getVariantBuilder(PwrRegistry.RELIEF_VALVE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ReliefValveBlock.OPEN) ? valveOpen : valveShut).build());
        provider.simpleBlockItem(PwrRegistry.RELIEF_VALVE.get(), valveShut);

        ResourceLocation casing = provider.modLoc("block/steam_generator_casing");
        provider.simpleBlockWithItem(PwrRegistry.STEAM_GENERATOR_CASING.get(), provider.models().cubeAll("steam_generator_casing", casing));
        provider.simpleBlockWithItem(PwrRegistry.STEAM_GENERATOR_TUBE_BUNDLE.get(), provider.models().cubeColumn("steam_generator_tube_bundle",
                provider.modLoc("block/steam_generator_tube_bundle"), provider.modLoc("block/steam_generator_tube_bundle_top")));
        ModelFile controller = provider.models().orientable("steam_generator_controller", casing,
                provider.modLoc("block/steam_generator_controller"), casing);
        provider.horizontalBlock(PwrRegistry.STEAM_GENERATOR_CONTROLLER.get(), controller);
        provider.simpleBlockItem(PwrRegistry.STEAM_GENERATOR_CONTROLLER.get(), controller);

        Map<SteamGeneratorPortRole, ModelFile> ports = new EnumMap<>(SteamGeneratorPortRole.class);
        for (SteamGeneratorPortRole role : SteamGeneratorPortRole.values()) {
            String name = "steam_generator_port_" + role.getSerializedName();
            ports.put(role, provider.models().cubeAll(name, provider.modLoc("block/" + name)));
        }
        provider.getVariantBuilder(PwrRegistry.STEAM_GENERATOR_PORT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(ports.get(state.getValue(SteamGeneratorPortBlock.ROLE))).build());
        provider.simpleBlockItem(PwrRegistry.STEAM_GENERATOR_PORT.get(), ports.get(SteamGeneratorPortRole.PRIMARY_IN));
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.withExistingParent("primary_coolant_bucket", ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(AllNuclearFluids.PRIMARY_COOLANT.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        PwrRegistry.PARTS.forEach(block -> parts.add(block.get()));
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // Activated corrosion products and boron: a bucket, or a puddle from a leak, irradiates.
        provider.builderFor(AllNuclearRadiation.RADIOACTIVITY)
                .add(AllNuclearFluids.PRIMARY_COOLANT_BUCKET.get().builtInRegistryHolder(), new Radioactivity(0.5f, false), false);
        var transmission = provider.builderFor(AllNuclearRadiation.TRANSMISSION);
        transmission.add(AllNuclearFluids.PRIMARY_COOLANT_BLOCK.get().builtInRegistryHolder(), new RadiationTransmission(0.85f), false);
        for (var block : PwrRegistry.PARTS) {
            transmission.add(((Block) block.get()).builtInRegistryHolder(), new RadiationTransmission(0.5f), false);
        }
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Pressurized water reactor crafting recipes",
                new PwrCraftingRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Pressurized water reactor mixing recipes",
                new PwrMixingRecipes(output, lookup)));
    }

    /** Providers need unique names, and the shared recipe providers already hold the default ones. */
    private record Named(String name, DataProvider provider) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return provider.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
