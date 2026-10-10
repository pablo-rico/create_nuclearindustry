package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.CryoCentrifugingRecipes;
import org.papiricoh.create_nuclearindustry.data.recipe.CryoCraftingRecipes;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantPortMode;
import org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak.FusionCryoPortBlock;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Lang, models, tags, data maps, loot and recipes for the cryogenic plant, the tokamak's vacuum pumps and cryogenic ports. */
final class CryoDataGen extends AreaDataGen {
    static final CryoDataGen INSTANCE = new CryoDataGen();

    private CryoDataGen() {}

    @Override
    void lang() {
        LangEntries.block("cryoplant_controller", "Cryogenic Plant Controller", "Controlador de la planta criogénica");
        LangEntries.block("cryoplant_casing", "Cryogenic Plant Casing", "Carcasa de la planta criogénica");
        LangEntries.block("cryoplant_port", "Cryogenic Plant Port", "Puerto de la planta criogénica");
        LangEntries.block("cryoplant_compressor", "Helium Compressor", "Compresor de helio");
        LangEntries.block("fusion_cryo_port", "Tokamak Cryogenic Port", "Puerto criogénico del tokamak");
        LangEntries.block("fusion_vacuum_pump", "Tokamak Vacuum Pump", "Bomba de vacío del tokamak");

        String plant = "create_nuclearindustry.cryoplant.";
        LangEntries.add(plant + "formed", "Cryogenic plant formed [3x3x%s, %s compressors]",
                "Planta criogénica formada [3x3x%s, %s compresores]");
        LangEntries.add(plant + "broken", "Cryogenic plant broken: %s", "Planta criogénica rota: %s");
        LangEntries.add(plant + "incomplete", "Cryogenic plant incomplete: %s", "Planta criogénica incompleta: %s");
        LangEntries.add(plant + "port.helium_in", "Warm helium in", "Entrada de helio caliente");
        LangEntries.add(plant + "port.liquid_helium_out", "Liquid helium out", "Salida de helio líquido");
        LangEntries.add(plant + "port.nitrogen_in", "Liquid nitrogen in (pre-cooling)", "Entrada de nitrógeno líquido (preenfriamiento)");
        LangEntries.add(plant + "port.liquid_air_out", "Air intake: liquid air out", "Toma de aire: salida de aire líquido");
        LangEntries.add(plant + "port_mode", "Port: %s", "Puerto: %s");
        LangEntries.add(plant + "port_hint", "Pipes or tanks on any side; the wrench changes what it carries",
                "Tuberías o tanques por cualquier lado; la llave inglesa cambia lo que lleva");
        LangEntries.add(plant + "linked", "Linked", "Enlazado");
        LangEntries.add(plant + "unlinked", "No cryogenic plant", "Sin planta criogénica");

        String compressor = plant + "compressor.";
        LangEntries.add(compressor + "speed", "Compressing: %s%% of rated speed", "Comprimiendo: %s%% de la velocidad nominal");
        LangEntries.add(compressor + "spin_up", "Spin up: needs %s RPM", "Hazlo girar: necesita %s RPM");
        LangEntries.add(compressor + "share", "Drives %s layers of the plant", "Mueve %s capas de la planta");
        LangEntries.add(compressor + "unlinked", "Not part of a formed cryogenic plant", "No forma parte de una planta criogénica formada");

        String goggles = plant + "goggles.";
        LangEntries.add(goggles + "size", "3x3x%s, %s compressors", "3x3x%s, %s compresores");
        LangEntries.add(goggles + "compressors", "Compressors: %s%% of rated speed", "Compresores: %s%% de la velocidad nominal");
        LangEntries.add(goggles + "helium_mode", "Liquefying helium", "Licuando helio");
        LangEntries.add(goggles + "air_mode", "Liquefying air", "Licuando aire");
        LangEntries.add(goggles + "making_helium", "Liquid helium: %s mB/t (up to %s)", "Helio líquido: %s mB/t (hasta %s)");
        LangEntries.add(goggles + "making_air", "Liquid air: %s mB/t (up to %s)", "Aire líquido: %s mB/t (hasta %s)");
        LangEntries.add(goggles + "helium_tanks", "Warm helium %s mB, liquid %s / %s mB", "Helio caliente %s mB, líquido %s / %s mB");
        LangEntries.add(goggles + "air_tank", "Liquid air: %s / %s mB", "Aire líquido: %s / %s mB");
        LangEntries.add(goggles + "nitrogen", "Nitrogen pre-cooling: %s mB, +%s%% output", "Preenfriamiento con nitrógeno: %s mB, +%s%% de producción");
        LangEntries.add(goggles + "no_helium", "No warm helium to liquefy", "No hay helio caliente que licuar");
        LangEntries.add(goggles + "full", "Liquid helium tank full", "Tanque de helio líquido lleno");

        String error = plant + "error.";
        LangEntries.add(error + "not_formed", "Not formed", "Sin formar");
        LangEntries.add(error + "no_controller", "Controller missing", "Falta el controlador");
        LangEntries.add(error + "no_casing", "The controller must face out of a side wall, with the plant behind it",
                "El controlador debe mirar hacia fuera desde una pared lateral, con la planta detrás");
        LangEntries.add(error + "height", "Height must be %s to %s blocks, found %s", "La altura debe ser de %s a %s bloques; hay %s");
        LangEntries.add(error + "casing", "Plant block missing at %s %s %s", "Falta un bloque de la planta en %s %s %s");
        LangEntries.add(error + "cold_box", "The centre of the inner layers must be casing: %s %s %s",
                "El centro de las capas interiores debe ser carcasa: %s %s %s");
        LangEntries.add(error + "compressor_inward", "Compressor facing into the plant at %s %s %s: its shaft goes outside",
                "Compresor mirando hacia dentro en %s %s %s: su eje va por fuera");
        LangEntries.add(error + "two_controllers", "Second controller at %s %s %s", "Segundo controlador en %s %s %s");
        LangEntries.add(error + "no_compressor", "Needs at least one Helium Compressor", "Necesita al menos un Compresor de helio");
        LangEntries.add(error + "missing_port", "Needs a port set to %s", "Necesita un puerto en modo %s");

        String port = "create_nuclearindustry.cryo_port.";
        LangEntries.add(port + "input", "Liquid helium in, to cool the coils", "Entrada de helio líquido, para enfriar las bobinas");
        LangEntries.add(port + "output", "Warm helium out, back to the cryogenic plant",
                "Salida de helio caliente, de vuelta a la planta criogénica");
        LangEntries.add(port + "mode", "Cryogenic port: %s", "Puerto criogénico: %s");
        LangEntries.add(port + "hint", "Pipes or tanks on any side; the wrench toggles in and out",
                "Tuberías o tanques por cualquier lado; la llave inglesa alterna entrada y salida");
        LangEntries.add(port + "linked", "Linked", "Enlazado");
        LangEntries.add(port + "unlinked", "No tokamak", "Sin tokamak");

        String pump = "create_nuclearindustry.vacuum_pump.";
        LangEntries.add(pump + "pumping", "Pumping: %s%% of a pump at rated speed", "Bombeando: %s%% de una bomba a velocidad nominal");
        LangEntries.add(pump + "spin_up", "Spin up: needs %s RPM", "Hazla girar: necesita %s RPM");
        LangEntries.add(pump + "chamber", "Chamber pressure: %s Pa", "Presión de la cámara: %s Pa");
        LangEntries.add(pump + "unlinked", "Not part of a formed tokamak", "No forma parte de un tokamak formado");

        jei();
    }

    private static void jei() {
        String info = "jei.create_nuclearindustry.info.";
        LangEntries.add(info + "cryoplant",
                "The Cryogenic Plant liquefies the helium that keeps a tokamak's coils superconducting. Build a 3x3 block of Cryogenic Plant Casing 3 to 5 high, with the controller in the middle of a side wall facing out, at least one Helium Compressor in the walls with its shaft side facing out, and Cryogenic Plant Ports (the wrench sets what each one carries). Warm helium goes in through a Warm helium in port and leaves a Liquid helium out port as liquid: 2 mB/t per layer of height at 128 RPM, less when slower and nothing below 32 RPM. The plant loads the network with 10240 SU/RPM per layer, shared among its compressors. About 1% of the helium leaks out on every pass. Liquid nitrogen fed through a nitrogen port pre-cools it for 50% more output, boiling off 1 mB of nitrogen per mB of liquid helium. A plant with a Liquid Air port liquefies air instead, 5 mB/t per layer: centrifuge the liquid air into liquid nitrogen and the helium for a first charge.",
                "La Planta criogénica licúa el helio que mantiene superconductoras las bobinas de un tokamak. Construye un bloque de 3x3 de Carcasa de la planta criogénica de 3 a 5 de alto, con el controlador en el centro de una pared lateral mirando hacia fuera, al menos un Compresor de helio en las paredes con el lado del eje hacia fuera y Puertos de la planta criogénica (la llave inglesa elige lo que lleva cada uno). El helio caliente entra por un puerto de Entrada de helio caliente y sale líquido por uno de Salida de helio líquido: 2 mB/t por capa de altura a 128 RPM, menos si gira más despacio y nada por debajo de 32 RPM. La planta carga la red con 10240 SU/RPM por capa, repartidas entre sus compresores. Cerca del 1% del helio se escapa en cada paso. El nitrógeno líquido que entra por un puerto de nitrógeno la preenfría y produce un 50% más, gastando 1 mB de nitrógeno por cada mB de helio líquido. Una planta con un puerto de Aire líquido licúa aire en su lugar, 5 mB/t por capa: centrifuga el aire líquido para sacar nitrógeno líquido y el helio de la primera carga.");
        LangEntries.add(info + "tokamak_cryo_port",
                "Tokamak Cryogenic Ports go in the tokamak shell. In input mode they take liquid helium in to cool the superconducting coils; in output mode they give back the warm helium gas the coils boil off, for a cryogenic plant to liquefy again. The wrench toggles the mode. They connect to pipes, and also push or pull on their own with a tank or Cryogenic Plant Port right next to them, so the plant can stand against the shell. A tokamak cannot start until liquid helium has cooled its coils.",
                "Los Puertos criogénicos del tokamak van en la carcasa del tokamak. En modo entrada reciben helio líquido para enfriar las bobinas superconductoras; en modo salida devuelven el helio gaseoso caliente que hierve en las bobinas, para que una planta criogénica lo vuelva a licuar. La llave inglesa cambia el modo. Se conectan a tuberías y además empujan o tiran por sí solos de un tanque o un Puerto de la planta criogénica que tengan justo al lado, así que la planta puede ir pegada a la carcasa. Un tokamak no puede arrancar hasta que el helio líquido haya enfriado sus bobinas.");
        LangEntries.add(info + "tokamak_vacuum_pump",
                "The Tokamak Vacuum Pump goes in the tokamak shell with its shaft side facing out, and holds the vacuum the plasma needs. One pump at 128 RPM (2048 SU/RPM) is enough for a tokamak of major radius 4; every 4 blocks more of radius needs another. Slower pumps count for less, and below 32 RPM they do not pump at all. Goggles show the chamber pressure.",
                "La Bomba de vacío del tokamak va en la carcasa del tokamak con el lado del eje hacia fuera y mantiene el vacío que necesita el plasma. Una bomba a 128 RPM (2048 SU/RPM) basta para un tokamak de radio mayor 4; cada 4 bloques más de radio necesitan otra. Las bombas más lentas cuentan menos y por debajo de 32 RPM no bombean nada. Las gafas muestran la presión de la cámara.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ResourceLocation casing = provider.modLoc("block/cryoplant_casing");
        ResourceLocation casingTop = provider.modLoc("block/cryoplant_casing_top");
        provider.simpleBlockWithItem(CryoRegistry.CRYOPLANT_CASING.get(),
                provider.models().cubeColumn("cryoplant_casing", casing, casingTop));

        ModelFile idle = provider.models().orientable("cryoplant_controller", casing,
                provider.modLoc("block/cryoplant_controller"), casingTop);
        ModelFile running = provider.models().orientable("cryoplant_controller_running", casing,
                provider.modLoc("block/cryoplant_controller_running"), casingTop);
        provider.horizontalBlock(CryoRegistry.CRYOPLANT_CONTROLLER.get(),
                state -> state.getValue(CryoplantControllerBlock.RUNNING) ? running : idle);
        provider.simpleBlockItem(CryoRegistry.CRYOPLANT_CONTROLLER.get(), idle);

        Map<CryoplantPortMode, ModelFile> ports = new EnumMap<>(CryoplantPortMode.class);
        for (CryoplantPortMode mode : CryoplantPortMode.values()) {
            String name = "cryoplant_port_" + mode.getSerializedName();
            ports.put(mode, provider.models().cubeAll(name, provider.modLoc("block/" + name)));
        }
        provider.getVariantBuilder(CryoRegistry.CRYOPLANT_PORT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(ports.get(state.getValue(CryoplantPortBlock.MODE))).build());
        provider.simpleBlockItem(CryoRegistry.CRYOPLANT_PORT.get(), ports.get(CryoplantPortMode.HELIUM_IN));

        // Hand-written: a recessed front behind which the renderer turns the impeller.
        facing(provider, CryoRegistry.CRYOPLANT_COMPRESSOR.get(),
                provider.models().getExistingFile(provider.modLoc("block/cryoplant_compressor")));
        facing(provider, CryoRegistry.FUSION_VACUUM_PUMP.get(),
                provider.models().getExistingFile(provider.modLoc("block/fusion_vacuum_pump")));

        Map<ReactorFluidPortMode, ModelFile> cryoPorts = new EnumMap<>(ReactorFluidPortMode.class);
        for (ReactorFluidPortMode mode : ReactorFluidPortMode.values()) {
            String name = "fusion_cryo_port_" + mode.getSerializedName();
            cryoPorts.put(mode, provider.models().cubeAll(name, provider.modLoc("block/" + name)));
        }
        provider.getVariantBuilder(CryoRegistry.FUSION_CRYO_PORT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(cryoPorts.get(state.getValue(FusionCryoPortBlock.MODE))).build());
        provider.simpleBlockItem(CryoRegistry.FUSION_CRYO_PORT.get(), cryoPorts.get(ReactorFluidPortMode.INPUT));
    }

    /** A block whose model faces north, turned to its {@code facing}; the item shows it facing north. */
    private static void facing(NuclearBlockStateProvider provider, Block block, ModelFile model) {
        provider.getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isVertical() ? 0 : ((int) facing.toYRot() + 180) % 360;
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
        provider.simpleBlockItem(block, model);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        CryoRegistry.PLANT_PARTS.forEach(block -> parts.add(block.get()));
        CryoRegistry.TOKAMAK_PARTS.forEach(block -> parts.add(block.get()));
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Cryogenic plant crafting recipes",
                new CryoCraftingRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Cryogenic plant centrifuging recipes",
                new CryoCentrifugingRecipes(output, lookup)));
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
