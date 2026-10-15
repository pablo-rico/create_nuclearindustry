package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.PropulsionRecipes;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.block.RocketNozzleBlock;

import java.util.concurrent.CompletableFuture;

/**
 * Lang, blockstates, tags and recipes of the nuclear engines for Create Aeronautics ships. The nozzles
 * share one hand-made bell ({@code models/block/rocket_nozzle}); the rest are cubes. The recipes only
 * load with Sable installed.
 */
final class PropulsionDataGen extends AreaDataGen {
    static final PropulsionDataGen INSTANCE = new PropulsionDataGen();
    private static final String TH = "create_nuclearindustry.thermal_engine.";
    private static final String AM = "create_nuclearindustry.antimatter_engine.";

    private PropulsionDataGen() {}

    @Override
    void lang() {
        LangEntries.block("nuclear_thermal_engine", "Nuclear Thermal Engine", "Motor térmico nuclear");
        LangEntries.block("thermal_engine_exchanger", "Thermal Engine Heat Exchanger", "Intercambiador del motor térmico");
        LangEntries.block("thermal_rocket_nozzle", "Thermal Rocket Nozzle", "Tobera del motor térmico");
        LangEntries.block("antimatter_engine", "Antimatter Engine", "Motor de antimateria");
        LangEntries.block("magnetic_coil", "Magnetic Coil", "Bobina magnética");
        LangEntries.block("magnetic_nozzle", "Magnetic Nozzle", "Tobera magnética");
        LangEntries.add("itemGroup.create_nuclearindustry.section.propulsion", "Propulsion", "Propulsión");

        LangEntries.add(TH + "status.not_formed", "Not built: exchangers then a nozzle behind it", "Sin construir: intercambiadores y una tobera detrás");
        LangEntries.add(TH + "status.idle", "Idle", "En reposo");
        LangEntries.add(TH + "status.firing", "Firing", "Encendido");
        LangEntries.add(TH + "status.overheating", "Nozzle overheating!", "¡Tobera sobrecalentada!");
        LangEntries.add(TH + "status.failed", "Nozzle failed: mend it with a Maintenance Kit", "Tobera rota: repárala con un kit de mantenimiento");
        LangEntries.add(TH + "none", "none", "ninguno");
        String g = TH + "goggles.";
        LangEntries.add(g + "segments", "Heat exchanger segments: %s", "Segmentos de intercambiador: %s");
        LangEntries.add(g + "thrust", "Thrust: %s", "Empuje: %s");
        LangEntries.add(g + "throttle", "Throttle: %s%%", "Acelerador: %s%%");
        LangEntries.add(g + "flow", "Propellant flow: %s mB/t", "Caudal de propelente: %s mB/t");
        LangEntries.add(g + "nozzle", "Nozzle: %s °C (wears above %s °C)", "Tobera: %s °C (se desgasta por encima de %s °C)");
        LangEntries.add(g + "wear", "Nozzle wear: %s%%", "Desgaste de la tobera: %s%%");
        LangEntries.add(g + "hot_primary", "Hot primary coolant: %s / %s mB", "Refrigerante primario caliente: %s / %s mB");
        LangEntries.add(g + "propellant", "Propellant: %s, %s mB", "Propelente: %s, %s mB");

        LangEntries.add(AM + "status.not_formed", "Not built: magnetic coils then a magnetic nozzle behind it",
                "Sin construir: bobinas magnéticas y una tobera magnética detrás");
        LangEntries.add(AM + "status.idle", "Field up, idle", "Campo activo, en reposo");
        LangEntries.add(AM + "status.firing", "Firing", "Encendido");
        LangEntries.add(AM + "status.no_field", "No field: the coils need energy", "Sin campo: las bobinas necesitan energía");
        LangEntries.add(AM + "status.overheating", "Coils overheating!", "¡Bobinas sobrecalentadas!");
        LangEntries.add(AM + "status.quench", "CONFINEMENT LOST: annihilation imminent", "CONFINAMIENTO PERDIDO: aniquilación inminente");
        LangEntries.add(AM + "status.failed", "Engine destroyed", "Motor destruido");
        LangEntries.add(AM + "status_line", "Chamber %s ng, %s, thrust %s", "Cámara %s ng, %s, empuje %s");
        g = AM + "goggles.";
        LangEntries.add(g + "coils", "Magnetic coils: %s (%s%% of the energy into thrust)", "Bobinas magnéticas: %s (%s%% de la energía en empuje)");
        LangEntries.add(g + "thrust", "Thrust: %s", "Empuje: %s");
        LangEntries.add(g + "throttle", "Throttle: %s%%", "Acelerador: %s%%");
        LangEntries.add(g + "chamber", "Chamber: %s / %s ng", "Cámara: %s / %s ng");
        LangEntries.add(g + "burn", "Burn: %s ng/min", "Consumo: %s ng/min");
        LangEntries.add(g + "energy", "Field buffer: %s / %s FE", "Reserva del campo: %s / %s FE");
        LangEntries.add(g + "coil", "Coils: %s °C (wear above %s °C)", "Bobinas: %s °C (se desgastan por encima de %s °C)");
        LangEntries.add(g + "wear", "Coil wear: %s%%", "Desgaste de las bobinas: %s%%");
        LangEntries.add(g + "quench", "Annihilation in %s s!", "¡Aniquilación en %s s!");

        LangEntries.add("create_nuclearindustry.display_source.rocket_engine", "Nuclear Engine", "Motor nuclear");
        LangEntries.add("create_nuclearindustry.display_source.rocket_engine.line", "%s, thrust %s (%s%%)", "%s, empuje %s (%s%%)");

        String kind = "create_nuclearindustry.control_room.kind.";
        LangEntries.add(kind + "thermal_engine", "Nuclear Thermal Engine", "Motor térmico nuclear");
        LangEntries.add(kind + "antimatter_engine", "Antimatter Engine", "Motor de antimateria");
        String m = "create_nuclearindustry.monitor.";
        LangEntries.add(m + "eng_thrust", "Thrust", "Empuje");
        LangEntries.add(m + "eng_throttle", "Throttle", "Acelerador");
        LangEntries.add(m + "eng_nozzle", "Nozzle", "Tobera");
        LangEntries.add(m + "eng_coil", "Coils", "Bobinas");
        LangEntries.add(m + "eng_wear", "Wear", "Desgaste");
        LangEntries.add(m + "eng_propellant", "Propellant", "Propelente");
        LangEntries.add(m + "eng_flow", "Flow", "Caudal");
        LangEntries.add(m + "eng_burn", "Burn", "Consumo");
        LangEntries.add(m + "eng_field", "Field", "Campo");
        LangEntries.add(m + "eng_failed", "FAILED", "AVERIADO");
        String r = "create_nuclearindustry.control_module.reading.";
        LangEntries.add(r + "thermal_throttle", "Thermal engine throttle", "Acelerador del motor térmico");
        LangEntries.add(r + "thermal_nozzle", "Thermal engine nozzle temperature", "Temperatura de la tobera del motor térmico");
        LangEntries.add(r + "thermal_propellant", "Thermal engine propellant", "Propelente del motor térmico");
        LangEntries.add(r + "antimatter_throttle", "Antimatter engine throttle", "Acelerador del motor de antimateria");
        LangEntries.add(r + "antimatter_coil", "Antimatter engine coil temperature", "Temperatura de las bobinas del motor de antimateria");
        LangEntries.add(r + "antimatter_chamber", "Antimatter engine chamber", "Cámara del motor de antimateria");

        String p = org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo.PREFIX;
        LangEntries.add(p + "nuclear_thermal_engine",
                "A rocket for Create Aeronautics ships, heated by a reactor. Build it in a straight line: the controller, one to eight heat exchanger segments laid along the engine, and a Thermal Rocket Nozzle at the end facing the way the exhaust leaves. Pipe hot primary coolant from a pressurized reactor into the controller and propellant beside it (water, deuterium or hydrogen), and take the cooled primary back from it. Each segment passes 5 mB/t of propellant at full throttle; the lighter the gas the harder it pushes, hydrogen three times as hard as water. A redstone signal sets the throttle. The nozzle heats with the flow, water most of all: above 1400 °C it wears, and worn through it bursts, spilling the primary's activity around it. Mend it cold with a Maintenance Kit on the controller. The exhaust carries a trace of activity to wherever the jet lands. Aboard a ship it pushes the ship opposite to its nozzle; on the ground it only fires.",
                "Un cohete para naves de Create Aeronautics calentado por un reactor. Constrúyelo en línea recta: el controlador, de uno a ocho segmentos de intercambiador a lo largo del motor y una tobera del motor térmico al final, mirando hacia donde sale el escape. Mete por tubería refrigerante primario caliente de un reactor de agua a presión en el controlador y propelente a su lado (agua, deuterio o hidrógeno), y saca de él el primario ya frío. Cada segmento deja pasar 5 mB/t de propelente con el acelerador a fondo; cuanto más ligero el gas, más empuja: el hidrógeno el triple que el agua. Una señal de redstone fija el acelerador. La tobera se calienta con el caudal, sobre todo con agua: por encima de 1400 °C se desgasta y, gastada del todo, revienta y esparce la actividad del primario alrededor. Repárala en frío con un kit de mantenimiento sobre el controlador. El escape lleva un rastro de actividad allí donde cae el chorro. En una nave la empuja en sentido contrario a su tobera; en tierra solo se enciende.");
        LangEntries.add(p + "antimatter_engine",
                "The strongest engine there is, for Create Aeronautics ships. Build it in a straight line: the controller (the annihilation chamber), one to six Magnetic Coils along the engine and a Magnetic Nozzle at the end facing the way the exhaust leaves. Feed antimatter with an Antimatter Conduit or a vial in its port (the chamber holds 16 ng) and Forge Energy for the coils: 64 FE/t each to hold the field, and much more while it burns. At full throttle it annihilates 12 ng a minute. More coils turn more of it into thrust, two half and six nearly all; what they miss heats them and streams out behind the nozzle as gamma rays that dose anything in the jet. Too few coils at full throttle overheat and wear through. Without energy, or with worn-out coils, the field fails: a quench alarm sounds and the chamber's antimatter annihilates where the engine is.",
                "El motor más potente que existe, para naves de Create Aeronautics. Constrúyelo en línea recta: el controlador (la cámara de aniquilación), de una a seis bobinas magnéticas a lo largo del motor y una tobera magnética al final, mirando hacia donde sale el escape. Aliméntalo con antimateria por un conducto de antimateria o con un vial en su puerto (la cámara guarda 16 ng) y con Forge Energy para las bobinas: 64 FE/t cada una para mantener el campo, y mucho más mientras quema. Con el acelerador a fondo aniquila 12 ng por minuto. Cuantas más bobinas, más se convierte en empuje: con dos, la mitad, y con seis, casi todo; lo que se escapa las calienta y sale detrás de la tobera como rayos gamma que irradian todo lo que hay en el chorro. Pocas bobinas a fondo se sobrecalientan y se desgastan. Sin energía, o con las bobinas gastadas, el campo cae: suena una alarma de quench y la antimateria de la cámara se aniquila donde está el motor.");

        String a = "advancement.create_nuclearindustry.";
        LangEntries.add(a + "nuclear_flight.title", "Atomic Wings", "Alas atómicas");
        LangEntries.add(a + "nuclear_flight.description", "Push a Create Aeronautics ship with a Nuclear Thermal Engine",
                "Impulsa una nave de Create Aeronautics con un motor térmico nuclear");
        LangEntries.add(a + "antimatter_thrust.title", "Warp Speed... Almost", "Velocidad warp... casi");
        LangEntries.add(a + "antimatter_thrust.description", "Push a Create Aeronautics ship with an Antimatter Engine",
                "Impulsa una nave de Create Aeronautics con un motor de antimateria");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        controller(provider, PropulsionRegistry.THERMAL_ENGINE.get(), "nuclear_thermal_engine");
        controller(provider, PropulsionRegistry.ANTIMATTER_ENGINE.get(), "antimatter_engine");
        provider.axisBlock(PropulsionRegistry.THERMAL_EXCHANGER.get(), provider.modLoc("block/thermal_engine_exchanger_side"),
                provider.modLoc("block/thermal_engine_exchanger_end"));
        provider.simpleBlockItem(PropulsionRegistry.THERMAL_EXCHANGER.get(),
                provider.models().getExistingFile(provider.modLoc("block/thermal_engine_exchanger")));
        provider.axisBlock(PropulsionRegistry.MAGNETIC_COIL.get(), provider.modLoc("block/magnetic_coil_side"),
                provider.modLoc("block/magnetic_coil_end"));
        provider.simpleBlockItem(PropulsionRegistry.MAGNETIC_COIL.get(),
                provider.models().getExistingFile(provider.modLoc("block/magnetic_coil")));
        nozzle(provider, PropulsionRegistry.THERMAL_NOZZLE.get(), "thermal_rocket_nozzle");
        nozzle(provider, PropulsionRegistry.MAGNETIC_NOZZLE.get(), "magnetic_nozzle");
    }

    /** The panel faces away from the exhaust (south of the model), the flange towards the segments (north). */
    private static void controller(NuclearBlockStateProvider provider, Block block, String name) {
        ModelFile model = provider.models().cube(name, provider.modLoc("block/" + name + "_side"), provider.modLoc("block/" + name + "_side"),
                        provider.modLoc("block/" + name + "_back"), provider.modLoc("block/" + name + "_front"),
                        provider.modLoc("block/" + name + "_side"), provider.modLoc("block/" + name + "_side"))
                .texture("particle", provider.modLoc("block/" + name + "_side"));
        provider.directionalBlock(block, model);
        provider.simpleBlockItem(block, model);
    }

    /** The bell's mouth faces FACING, the way the exhaust leaves; lit while the engine fires. */
    private static void nozzle(NuclearBlockStateProvider provider, Block block, String name) {
        ModelFile cold = provider.models().withExistingParent(name, provider.modLoc("block/rocket_nozzle"))
                .texture("side", provider.modLoc("block/" + name + "_side"))
                .texture("plate", provider.modLoc("block/" + name + "_plate"))
                .texture("mouth", provider.modLoc("block/" + name + "_mouth"));
        ModelFile lit = provider.models().withExistingParent(name + "_lit", provider.modLoc("block/rocket_nozzle"))
                .texture("side", provider.modLoc("block/" + name + "_side"))
                .texture("plate", provider.modLoc("block/" + name + "_plate"))
                .texture("mouth", provider.modLoc("block/" + name + "_mouth_lit"));
        provider.directionalBlock(block, state -> state.getValue(RocketNozzleBlock.LIT) ? lit : cold);
        provider.simpleBlockItem(block, cold);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL).add(PropulsionRegistry.THERMAL_ENGINE.get(),
                PropulsionRegistry.THERMAL_EXCHANGER.get(), PropulsionRegistry.THERMAL_NOZZLE.get(),
                PropulsionRegistry.ANTIMATTER_ENGINE.get(), PropulsionRegistry.MAGNETIC_COIL.get(), PropulsionRegistry.MAGNETIC_NOZZLE.get());
        provider.tagFor(NuclearBlockTagsProvider.SABLE_HEAVY).add(PropulsionRegistry.THERMAL_ENGINE.get(),
                PropulsionRegistry.THERMAL_EXCHANGER.get(), PropulsionRegistry.ANTIMATTER_ENGINE.get(), PropulsionRegistry.MAGNETIC_COIL.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Propulsion crafting recipes", new PropulsionRecipes(output, lookup)));
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
