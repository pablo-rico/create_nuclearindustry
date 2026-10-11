package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.data.recipe.ContainmentCraftingRecipes;
import org.papiricoh.create_nuclearindustry.data.recipe.PwrCraftingRecipes;
import org.papiricoh.create_nuclearindustry.data.recipe.PwrMixingRecipes;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrTags;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.CoriumBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentRating;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.FilteredVentBlock;
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

        containmentLang();

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

    private static void containmentLang() {
        LangEntries.block("containment_concrete", "Containment Concrete", "Hormigón de contención");
        LangEntries.block("steel_liner", "Steel Liner", "Revestimiento de acero");
        LangEntries.block("containment_monitor", "Containment Monitor", "Monitor de contención");
        LangEntries.block("containment_penetration", "Containment Penetration", "Penetración de contención");
        LangEntries.block("recombiner", "Passive Autocatalytic Recombiner", "Recombinador autocatalítico pasivo");
        LangEntries.block("filtered_vent", "Filtered Containment Vent", "Venteo filtrado de contención");
        LangEntries.block("corium", "Corium", "Corium");
        LangEntries.block("solidified_corium", "Solidified Corium", "Corium solidificado");
        LangEntries.block("core_catcher", "Core Catcher", "Recogedor del núcleo");
        LangEntries.block("containment_door", "Containment Door", "Puerta de contención");
        LangEntries.add("create_nuclearindustry.ponder.containment_door.header", "The Containment Door", "La puerta de contención");
        LangEntries.add("create_nuclearindustry.ponder.containment_door.text_1",
                "A Containment Door is the way into a containment. Shut, it seals like the wall around it and holds 8 bar.",
                "Una Puerta de contención es la entrada a una contención. Cerrada, sella como la pared que la rodea y aguanta 8 bar.");
        LangEntries.add("create_nuclearindustry.ponder.containment_door.text_2",
                "Click it or give it a redstone signal: it pulls out of its frame and slides sideways along the wall.",
                "Haz clic en ella o dale una señal de redstone: sale de su marco y se desliza hacia un lado, pegada a la pared.");
        LangEntries.add("create_nuclearindustry.ponder.containment_door.text_3",
                "Open, the doorway is a hole: the containment is no longer sealed until the door shuts again.",
                "Abierta, el hueco es un agujero: la contención deja de estar sellada hasta que la puerta se vuelva a cerrar.");
        LangEntries.add("create_nuclearindustry.ponder.containment_door.text_4",
                "While the containment behind it is more than 0.2 bar above the outside, the door is pressure-locked: neither a click nor redstone opens it.",
                "Mientras la contención tras ella esté a más de 0,2 bar por encima del exterior, la puerta queda bloqueada por presión: no la abre ni un clic ni la redstone.");
        LangEntries.add("create_nuclearindustry.ponder.containment_door.text_5",
                "Two doors side by side, hinged on opposite sides, open together and slide apart.",
                "Dos puertas juntas, con las bisagras en lados opuestos, se abren a la vez y se separan.");
        LangEntries.add("create_nuclearindustry.containment_door.locked",
                "Pressure-locked: the containment behind this door is at %s bar",
                "Bloqueada por presión: la contención tras esta puerta está a %s bar");

        String source = "create_nuclearindustry.display_source.";
        LangEntries.add(source + "reactor_containment_pressure", "Containment Pressure", "Presión de la contención");
        LangEntries.add(source + "reactor_hydrogen", "Containment Hydrogen", "Hidrógeno en la contención");

        String c = "create_nuclearindustry.containment.";
        LangEntries.add(c + "status.sealed", "SEALED", "ESTANCA");
        LangEntries.add(c + "status.ruptured", "RUPTURED", "ROTA");
        LangEntries.add(c + "status.hydrogen", "HYDROGEN", "HIDRÓGENO");
        LangEntries.add(c + "status.venting", "VENTING", "VENTEANDO");
        LangEntries.add(c + "goggles.surveying", "Measuring the building...", "Midiendo el edificio...");
        LangEntries.add(c + "goggles.open", "Not sealed: the air inside reaches the outside",
                "Sin sellar: el aire de dentro llega al exterior");
        LangEntries.add(c + "goggles.volume", "Sealed: %s m³, weakest wall holds %s bar",
                "Estanca: %s m³, la pared más débil aguanta %s bar");
        LangEntries.add(c + "goggles.pressure", "Pressure: %s bar at %s °C", "Presión: %s bar a %s °C");
        LangEntries.add(c + "goggles.gas", "Hydrogen: %s%%, steam %s%%", "Hidrógeno: %s%%, vapor %s%%");
        LangEntries.add(c + "goggles.devices", "Recombiners: %s, vents: %s (%s open)",
                "Recombinadores: %s, venteos: %s (%s abiertos)");
        LangEntries.add(c + "goggles.activity", "Airborne activity: %s", "Actividad en el aire: %s");
        LangEntries.add(c + "goggles.burn", "HYDROGEN BURN", "COMBUSTIÓN DE HIDRÓGENO");
        LangEntries.add(c + "penetration.buffer", "Toward %s: %s, %s / %s mB", "Hacia %s: %s, %s / %s mB");
        LangEntries.add(c + "penetration.empty", "empty", "vacío");
        String[][] directions = {{"down", "below", "abajo"}, {"up", "above", "arriba"}, {"north", "north", "el norte"},
                {"south", "south", "el sur"}, {"west", "west", "el oeste"}, {"east", "east", "el este"}};
        for (String[] direction : directions) {
            LangEntries.add(c + "direction." + direction[0], direction[1], direction[2]);
        }

        String a = "create_nuclearindustry.accident.goggles.";
        LangEntries.add(a + "containment", " Containment: %s bar, hydrogen %s%%", " Contención: %s bar, hidrógeno %s%%");
        LangEntries.add(a + "cladding", " Fuel cladding oxidised: %s%%", " Vainas del combustible oxidadas: %s%%");
        LangEntries.add(a + "breached", " VESSEL BREACHED: rebuild and form it again", " VASIJA ROTA: reconstrúyela y vuelve a formarla");
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

        containmentStates(provider);
    }

    private static void containmentStates(NuclearBlockStateProvider provider) {
        ResourceLocation concrete = provider.modLoc("block/containment_concrete");
        provider.simpleBlockWithItem(PwrRegistry.CONTAINMENT_CONCRETE.get(), provider.models().cubeAll("containment_concrete", concrete));
        ResourceLocation liner = provider.modLoc("block/steel_liner");
        provider.simpleBlockWithItem(PwrRegistry.STEEL_LINER.get(), provider.models().cubeAll("steel_liner", liner));

        ModelFile monitor = provider.models().orientable("containment_monitor", liner, provider.modLoc("block/containment_monitor"), liner);
        provider.horizontalBlock(PwrRegistry.CONTAINMENT_MONITOR.get(), monitor);
        provider.simpleBlockItem(PwrRegistry.CONTAINMENT_MONITOR.get(), monitor);

        provider.axisBlock(PwrRegistry.CONTAINMENT_PENETRATION.get(), provider.modLoc("block/containment_penetration_side"),
                provider.modLoc("block/containment_penetration_end"));
        provider.simpleBlockItem(PwrRegistry.CONTAINMENT_PENETRATION.get(),
                provider.models().getExistingFile(provider.modLoc("block/containment_penetration")));

        provider.simpleBlockWithItem(PwrRegistry.RECOMBINER.get(), provider.models().cubeColumn("recombiner",
                provider.modLoc("block/recombiner_side"), provider.modLoc("block/recombiner_top")));

        ModelFile ventShut = provider.models().cubeColumn("filtered_vent", provider.modLoc("block/filtered_vent"), concrete);
        ModelFile ventOpen = provider.models().cubeColumn("filtered_vent_open", provider.modLoc("block/filtered_vent_open"), concrete);
        provider.getVariantBuilder(PwrRegistry.FILTERED_VENT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(FilteredVentBlock.OPEN) ? ventOpen : ventShut).build());
        provider.simpleBlockItem(PwrRegistry.FILTERED_VENT.get(), ventShut);

        ModelFile coriumHot = provider.models().cubeAll("corium", provider.modLoc("block/corium"));
        ModelFile coriumCooling = provider.models().cubeAll("corium_cooling", provider.modLoc("block/corium_cooling"));
        provider.getVariantBuilder(PwrRegistry.CORIUM.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(CoriumBlock.HEAT) >= 3 ? coriumHot : coriumCooling).build());
        provider.simpleBlockItem(PwrRegistry.CORIUM.get(), coriumHot);
        provider.simpleBlockWithItem(PwrRegistry.SOLIDIFIED_CORIUM.get(),
                provider.models().cubeAll("solidified_corium", provider.modLoc("block/solidified_corium")));
        provider.simpleBlockWithItem(PwrRegistry.CORE_CATCHER.get(), provider.models().cubeColumn("core_catcher",
                provider.modLoc("block/core_catcher_side"), provider.modLoc("block/core_catcher_top")));

        // Hand-written full-block halves (door faces north and south); turned for doors facing east or west.
        // The open states are never drawn as a model: an open door is hidden and its block entity draws it.
        ModelFile doorBottom = provider.models().getExistingFile(provider.modLoc("block/containment_door_bottom"));
        ModelFile doorTop = provider.models().getExistingFile(provider.modLoc("block/containment_door_top"));
        provider.getVariantBuilder(PwrRegistry.CONTAINMENT_DOOR.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? doorBottom : doorTop)
                .rotationY(state.getValue(DoorBlock.FACING).getAxis() == Direction.Axis.X ? 90 : 0)
                .build(), DoorBlock.POWERED);
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.withExistingParent("primary_coolant_bucket", ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(AllNuclearFluids.PRIMARY_COOLANT.get());
        provider.basicItem(PwrRegistry.CONTAINMENT_DOOR_ITEM.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        PwrRegistry.PARTS.forEach(block -> parts.add(block.get()));
        provider.tagFor(PwrTags.CORIUM_RESISTANT).add(PwrRegistry.CORE_CATCHER.get());
        provider.tagFor(BlockTags.NEEDS_IRON_TOOL).add(PwrRegistry.SOLIDIFIED_CORIUM.get(), PwrRegistry.CORE_CATCHER.get(),
                PwrRegistry.CONTAINMENT_DOOR.get());
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
        // Thick concrete and steel shield well; the slag of a melted core is as dense as it gets.
        transmission.add(PwrRegistry.CONTAINMENT_CONCRETE.get().builtInRegistryHolder(), new RadiationTransmission(0.3f), false);
        transmission.add(PwrRegistry.STEEL_LINER.get().builtInRegistryHolder(), new RadiationTransmission(0.4f), false);
        transmission.add(PwrRegistry.CONTAINMENT_PENETRATION.get().builtInRegistryHolder(), new RadiationTransmission(0.35f), false);
        transmission.add(PwrRegistry.FILTERED_VENT.get().builtInRegistryHolder(), new RadiationTransmission(0.35f), false);
        transmission.add(PwrRegistry.CORE_CATCHER.get().builtInRegistryHolder(), new RadiationTransmission(0.3f), false);
        transmission.add(PwrRegistry.CONTAINMENT_DOOR.get().builtInRegistryHolder(), new RadiationTransmission(0.4f), false);
        transmission.add(PwrRegistry.CORIUM.get().builtInRegistryHolder(), new RadiationTransmission(0.2f), false);
        transmission.add(PwrRegistry.SOLIDIFIED_CORIUM.get().builtInRegistryHolder(), new RadiationTransmission(0.2f), false);

        // Molten and frozen core: by far the most radioactive blocks there are.
        var radioactivity = provider.builderFor(AllNuclearRadiation.RADIOACTIVITY);
        radioactivity.add(PwrRegistry.CORIUM_ITEM.get().builtInRegistryHolder(), new Radioactivity(20.0f, false, 2.0f), false);
        radioactivity.add(PwrRegistry.SOLIDIFIED_CORIUM_ITEM.get().builtInRegistryHolder(), new Radioactivity(8.0f, false, 0.5f), false);

        // Absolute pressure a block holds in a containment wall, bar. Unlisted full blocks hold 1.5.
        var rating = provider.builderFor(ContainmentRating.TYPE);
        rating.add(PwrRegistry.CONTAINMENT_CONCRETE.get().builtInRegistryHolder(), new ContainmentRating(6.0f), false);
        rating.add(PwrRegistry.STEEL_LINER.get().builtInRegistryHolder(), new ContainmentRating(8.0f), false);
        rating.add(PwrRegistry.CONTAINMENT_PENETRATION.get().builtInRegistryHolder(), new ContainmentRating(6.0f), false);
        rating.add(PwrRegistry.FILTERED_VENT.get().builtInRegistryHolder(), new ContainmentRating(6.0f), false);
        rating.add(PwrRegistry.CORE_CATCHER.get().builtInRegistryHolder(), new ContainmentRating(8.0f), false);
        // A shut containment door is a steel plug as strong as the liner; open, it has no collision and the room is open.
        rating.add(PwrRegistry.CONTAINMENT_DOOR.get().builtInRegistryHolder(), new ContainmentRating(8.0f), false);
        // Fittings that stand inside: never the weak point.
        rating.add(PwrRegistry.CONTAINMENT_MONITOR.get().builtInRegistryHolder(), new ContainmentRating(10.0f), false);
        rating.add(PwrRegistry.RECOMBINER.get().builtInRegistryHolder(), new ContainmentRating(10.0f), false);
        rating.add(Tags.Blocks.GLASS_BLOCKS, new ContainmentRating(1.05f), false);
        rating.add(Blocks.OBSIDIAN.builtInRegistryHolder(), new ContainmentRating(6.0f), false);
        rating.add(Blocks.IRON_BLOCK.builtInRegistryHolder(), new ContainmentRating(4.0f), false);
        // A shut blast door and its ring are the strongest wall there is; an open leaf has no
        // collision, so the room is simply open.
        rating.add(CivilDefenseRegistry.STEEL_BLAST_DOOR.get().builtInRegistryHolder(), new ContainmentRating(10.0f), false);
        rating.add(CivilDefenseRegistry.LEAD_BLAST_DOOR.get().builtInRegistryHolder(), new ContainmentRating(10.0f), false);
        rating.add(CivilDefenseRegistry.BLAST_DOOR_FRAME.get().builtInRegistryHolder(), new ContainmentRating(10.0f), false);
        rating.add(CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get().builtInRegistryHolder(), new ContainmentRating(10.0f), false);
    }

    @Override
    void blockLoot(NuclearBlockLoot loot) {
        // Molten fuel cannot be picked up; it has to freeze first.
        loot.addTable(PwrRegistry.CORIUM.get(), LootTable.lootTable());
        // Like any door: one item, from the lower half.
        loot.addDoorTable(PwrRegistry.CONTAINMENT_DOOR.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Pressurized water reactor crafting recipes",
                new PwrCraftingRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Pressurized water reactor mixing recipes",
                new PwrMixingRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Containment crafting recipes",
                new ContainmentCraftingRecipes(output, lookup)));
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
