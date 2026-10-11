package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.ReactorVisualsRecipes;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

import java.util.concurrent.CompletableFuture;

/** Lang, models and tags for the visible and audible fission reactor: the viewport, the fuel channels' looks and the sounds. */
final class ReactorVisualsDataGen extends AreaDataGen {
    static final ReactorVisualsDataGen INSTANCE = new ReactorVisualsDataGen();

    /** Lead glass lets a little less through than the casing it replaces. */
    static final float VIEWPORT_TRANSMISSION = 0.3f;

    private ReactorVisualsDataGen() {}

    @Override
    void lang() {
        LangEntries.block("reactor_viewport", "Reactor Viewport", "Mirilla del reactor");

        String s = "subtitle.create_nuclearindustry.";
        LangEntries.add(s + "reactor_hum", "Reactor hums", "Zumba el reactor");
        LangEntries.add(s + "reactor_scram", "Control rods drop", "Caen las barras de control");
        LangEntries.add(s + "reactor_klaxon", "Reactor klaxon sounds", "Suena la bocina del reactor");
        LangEntries.add(s + "reactor_overtemp_alarm", "Over-temperature alarm", "Alarma de sobretemperatura");
        LangEntries.add(s + "steam_hiss", "Steam hisses", "Silba el vapor");
        LangEntries.add(s + "coolant_bubbling", "Coolant bubbles", "Burbujea el refrigerante");
        LangEntries.add(s + "relief_valve_lift", "Relief valve lifts", "Salta la válvula de alivio");
        LangEntries.add(s + "centrifuge_whine", "Centrifuge whines", "Silba la centrifugadora");
        LangEntries.add(s + "electrolyzer_buzz", "Electrolyzer buzzes", "Zumba el electrolizador");
        LangEntries.add(s + "melter_roar", "Melter roars", "Ruge el fundidor");
        LangEntries.add(s + "rtg_hum", "Radioisotope generator hums", "Zumba el generador de radioisótopos");

        reactorMessages();
        reactorValidation();
        reactorGoggles();
        reactorPorts();
        plantMessages();
    }

    private static void reactorMessages() {
        String m = "create_nuclearindustry.reactor.message.";
        LangEntries.add(m + "scrammed_hot_load", "Reactor scrammed hot: meltdown protection active after load",
                "Reactor parado en caliente: protección contra la fusión activa tras la carga");
        LangEntries.add(m + "formed", "Reactor formed %s", "Reactor formado %s");
        LangEntries.add(m + "valid_scrammed_hot", "Reactor structure valid, scrammed hot after load",
                "Estructura del reactor válida, parado en caliente tras la carga");
        LangEntries.add(m + "formed_rods_moving", "Reactor formed; control rod channel is occupied by moving rods",
                "Reactor formado; el canal de las barras de control está ocupado por barras en movimiento");
        LangEntries.add(m + "scrammed", "Reactor scrammed: %s", "Reactor parado (SCRAM): %s");
        LangEntries.add(m + "fuel_damage", "The reactor's fuel cladding is failing: the core is damaged",
                "Las vainas del combustible del reactor están fallando: el núcleo está dañado");
        LangEntries.add(m + "fuel_melting", "The reactor's fuel is melting!", "¡El combustible del reactor se está fundiendo!");
        LangEntries.add(m + "structure_scram", "Reactor structure broken while hot: the reactor is scrammed",
                "Se rompió la estructura del reactor en caliente: el reactor está parado (SCRAM)");
        LangEntries.add(m + "incomplete", "Reactor incomplete: %s", "Reactor incompleto: %s");
        LangEntries.add(m + "auto_scram", "Automatic SCRAM at %s °C: SCRAM signal on",
                "SCRAM automático a %s °C: señal de SCRAM activada");
        LangEntries.add(m + "reset_too_hot", "Automatic SCRAM can only be reset below %s °C",
                "El SCRAM automático solo se puede rearmar por debajo de %s °C");
        LangEntries.add(m + "scram_reset", "SCRAM reset", "SCRAM rearmado");
        LangEntries.add(m + "manual_scram", "Manual SCRAM: signal sent to the Redstone Ports",
                "SCRAM manual: señal enviada a los puertos de redstone");
        LangEntries.add(m + "manual_scram_released", "Manual SCRAM released", "SCRAM manual liberado");
        LangEntries.add(m + "meltdown", "Nuclear meltdown at %s", "Fusión del núcleo en %s");
        LangEntries.add(m + "meltdown_contained", "Core meltdown at %s: the containment holds the release",
                "Fusión del núcleo en %s: la contención retiene la emisión");
        LangEntries.add(m + "meltdown_release", "Core meltdown at %s: radioactive release",
                "Fusión del núcleo en %s: emisión radiactiva");
        LangEntries.add(m + "steam_explosion", "Steam explosion: water reached the molten core at %s",
                "Explosión de vapor: el agua llegó al núcleo fundido en %s");
        LangEntries.add(m + "vessel_breached", "Reactor vessel breached at %s: the primary is open",
                "Vasija del reactor rota en %s: el primario está abierto");

        String st = "create_nuclearindustry.reactor.status.";
        LangEntries.add(st + "incomplete", "Incomplete", "Incompleto");
        LangEntries.add(st + "formed", "Formed", "Formado");
        LangEntries.add(st + "scrammed", "Scrammed", "Parado (SCRAM)");
        LangEntries.add(st + "scrammed_hot", "Scrammed Hot", "Parado en caliente");
        LangEntries.add(st + "meltdown", "Meltdown", "Fusión del núcleo");
        LangEntries.add(st + "syncing", "Syncing", "Sincronizando");
        LangEntries.add(st + "waiting_for_sync", "Waiting for server sync", "Esperando datos del servidor");

        String state = "create_nuclearindustry.reactor.state.";
        LangEntries.add(state + "idle", "Idle", "En reposo");
        LangEntries.add(state + "fueling", "Fuel queued", "Combustible en cola");
        LangEntries.add(state + "suppressed", "Suppressed", "Suprimido");
        LangEntries.add(state + "running", "Running", "En marcha");
        LangEntries.add(state + "warning", "Warning", "Aviso");
        LangEntries.add(state + "critical", "Critical", "Crítico");
        LangEntries.add(state + "melting", "MELTING", "FUNDIÉNDOSE");

        String d = "create_nuclearindustry.reactor.display.";
        LangEntries.add(d + "unformed", "Unformed", "Sin formar");
        LangEntries.add(d + "auto_scram", "AUTO SCRAM", "SCRAM AUTO");
        LangEntries.add(d + "scram", "SCRAM", "SCRAM");
        LangEntries.add(d + "no_reactor", "No reactor", "Sin reactor");
        LangEntries.add(d + "no_sensor", "No sensor", "Sin sensor");
        LangEntries.add(d + "sensor.state", "State: %s", "Estado: %s");
        LangEntries.add(d + "sensor.missing", "Missing: %s", "Falta: %s");
        LangEntries.add(d + "sensor.temperature", "Temp: %s C", "Temp.: %s C");
        LangEntries.add(d + "sensor.coolant", "Coolant: %s/%s mB", "Refrigerante: %s/%s mB");
        LangEntries.add(d + "sensor.steam", "Steam: %s/%s mB", "Vapor: %s/%s mB");
        LangEntries.add(d + "sensor.stress", "Stress: %s%%", "Estrés: %s%%");
        LangEntries.add(d + "sensor.rods", "Rods: %s/%s", "Barras: %s/%s");
    }

    private static void reactorValidation() {
        String v = "create_nuclearindustry.reactor.validation.";
        LangEntries.add(v + "not_formed", "Not formed", "Sin formar");
        LangEntries.add(v + "no_level", "Level unavailable", "Mundo no disponible");
        LangEntries.add(v + "no_controller", "Controller block missing", "Falta el bloque controlador");
        LangEntries.add(v + "needs_pressurizer", "Primary coolant needs a pressurizer on the lid: the loop is not pressurized",
                "El refrigerante primario necesita un presurizador sobre la tapa: el circuito no está presurizado");
        LangEntries.add(v + "no_floor", "Missing casing floor below controller", "Falta el suelo de carcasa bajo el controlador");
        LangEntries.add(v + "height", "Height must be %s-%s blocks, got %s", "La altura debe ser de %s a %s bloques, tiene %s");
        LangEntries.add(v + "width", "Width must be odd and between %s and %s, got %s",
                "La anchura debe ser impar y estar entre %s y %s, tiene %s");
        LangEntries.add(v + "controller_centre", "Controller must be centered on the top face",
                "El controlador debe estar en el centro de la cara superior");
        LangEntries.add(v + "casing_missing", "Missing casing at %s", "Falta carcasa en %s");
        LangEntries.add(v + "more_casing", "More casing errors omitted", "Se omiten más errores de carcasa");
        LangEntries.add(v + "mixed_column", "Mixed interior column at %s", "Columna interior mezclada en %s");
        LangEntries.add(v + "invalid_interior", "Invalid interior block at %s", "Bloque interior no válido en %s");
        LangEntries.add(v + "not_homogeneous", "Interior columns must be vertical and homogeneous at %s",
                "Las columnas interiores deben ser verticales y homogéneas en %s");
        LangEntries.add(v + "no_fuel", "Missing uranium rod column", "Falta una columna de barras de uranio");
        LangEntries.add(v + "no_exchanger", "Missing heat exchanger column", "Falta una columna de intercambiadores de calor");
        LangEntries.add(v + "no_coolant_port", "Missing coolant input port", "Falta un puerto de entrada de refrigerante");
        LangEntries.add(v + "no_steam_port", "Missing steam output port", "Falta un puerto de salida de vapor");
        LangEntries.add(v + "no_control_rods", "No control rod columns; reactor will be difficult to control",
                "Sin columnas de barras de control: el reactor será difícil de controlar");
        LangEntries.add(v + "channel_blocked", "Control rod channel blocked at %s", "Canal de barras de control bloqueado en %s");
        LangEntries.add(v + "extra_pressurizer", "A reactor takes one pressurizer; the one at %s is ignored",
                "Un reactor admite un solo presurizador; se ignora el de %s");
    }

    private static void reactorGoggles() {
        String g = "create_nuclearindustry.reactor.goggles.";
        LangEntries.add(g + "title", "Nuclear Reactor", "Reactor nuclear");
        LangEntries.add(g + "structure", "Structure: %s", "Estructura: %s");
        LangEntries.add(g + "state", "State: %s", "Estado: %s");
        LangEntries.add(g + "dimensions", "Dimensions: %s", "Dimensiones: %s");
        LangEntries.add(g + "missing", "Missing: %s", "Falta: %s");
        LangEntries.add(g + "uranium_columns", "Uranium columns: %s", "Columnas de uranio: %s");
        LangEntries.add(g + "exchanger_columns", "Heat exchanger columns: %s", "Columnas de intercambiadores: %s");
        LangEntries.add(g + "control_columns", "Control rod columns: %s", "Columnas de barras de control: %s");
        LangEntries.add(g + "graphite_columns", "Graphite columns: %s (moderation %s%%)", "Columnas de grafito: %s (moderación %s%%)");
        LangEntries.add(g + "reflector_columns", "Reflector columns: %s (ring %s%%)", "Columnas de reflector: %s (anillo %s%%)");
        LangEntries.add(g + "rod_segments", "Control rod segments: %s/%s", "Segmentos de barras de control: %s/%s");
        LangEntries.add(g + "coolant_ports", "Coolant input ports: %s", "Puertos de entrada de refrigerante: %s");
        LangEntries.add(g + "steam_ports", "Steam output ports: %s", "Puertos de salida de vapor: %s");
        LangEntries.add(g + "fuel_ports", "Fuel ports: %s", "Puertos de combustible: %s");
        LangEntries.add(g + "rod_insertion", "Control rod insertion: %s%%", "Inserción de las barras de control: %s%%");
        LangEntries.add(g + "rod_target", "Panel rod target: %s%%", "Objetivo de barras del panel: %s%%");
        LangEntries.add(g + "static_rods", "Static rods: %s", "Barras fijas: %s");
        LangEntries.add(g + "moving_rods", "Moving rods: %s", "Barras en movimiento: %s");
        LangEntries.add(g + "coolant", "Coolant: %s/%s mB", "Refrigerante: %s/%s mB");
        LangEntries.add(g + "steam", "Steam: %s/%s mB", "Vapor: %s/%s mB");
        LangEntries.add(g + "temperature", "Fuel temperature: %s °C", "Temperatura del combustible: %s °C");
        LangEntries.add(g + "coolant_temperature", "Coolant in the core: %s °C", "Refrigerante en el núcleo: %s °C");
        LangEntries.add(g + "power_level", "Fission power: %s%% of %s MW rated", "Potencia de fisión: %s%% de %s MW nominales");
        LangEntries.add(g + "reactivity", "Reactivity: %s pcm (fuel x%s)", "Reactividad: %s pcm (combustible x%s)");
        LangEntries.add(g + "reactivity_parts", "  fuel %s · rods %s · heat %s · steam %s · xenon %s",
                "  combustible %s · barras %s · calor %s · vapor %s · xenón %s");
        LangEntries.add(g + "xenon", "Xenon poisoning: -%s%%", "Envenenamiento por xenón: -%s%%");
        LangEntries.add(g + "decay_heat", "Decay heat: %s MW", "Calor residual: %s MW");
        LangEntries.add(g + "thermal_stress", "Fuel stress: %s%% of the way to melting", "Tensión del combustible: %s%% hasta fundirse");
        LangEntries.add(g + "core_damage", "Fuel damage: %s%% (permanent until refuelled)", "Daño del combustible: %s%% (permanente hasta recargar)");
        LangEntries.add(g + "excursion", "Fuel melting: insert control rods and cool the core",
                "Combustible fundiéndose: inserta las barras y enfría el núcleo");
        LangEntries.add(g + "fuel", "Fuel: %s / %s units", "Combustible: %s / %s unidades");
        LangEntries.add(g + "enrichment", "Fuel enrichment: %s%%", "Enriquecimiento del combustible: %s%%");
        LangEntries.add(g + "auto_scram_tripped", "Automatic SCRAM tripped at %s C", "SCRAM automático disparado a %s C");
        LangEntries.add(g + "manual_scram", "Manual SCRAM signal on", "Señal de SCRAM manual activada");
        LangEntries.add(g + "fuel_input", "Fuel input: %s assemblies", "Combustible de entrada: %s elementos");
        LangEntries.add(g + "depleted", "Depleted fuel: %s assemblies", "Combustible gastado: %s elementos");
        LangEntries.add(g + "suppressed", "Status: loaded, control rods suppressing reaction",
                "Estado: cargado, las barras de control frenan la reacción");
        LangEntries.add(g + "output_blocked", "Depleted output blocked: %s", "Salida de combustible gastado bloqueada: %s");
    }

    private static void reactorPorts() {
        String p = "create_nuclearindustry.reactor.port.";
        LangEntries.add(p + "fuel_not_attached", "Fuel port is not attached to a formed reactor shell",
                "El puerto de combustible no está en la carcasa de un reactor formado");
        LangEntries.add(p + "linked", "Linked", "Enlazado");
        LangEntries.add(p + "structure_incomplete", "Structure incomplete", "Estructura incompleta");
        LangEntries.add(p + "no_reactor", "No reactor", "Sin reactor");
        LangEntries.add(p + "mode", "Mode: %s", "Modo: %s");
        LangEntries.add(p + "mode.input", "Input", "Entrada");
        LangEntries.add(p + "mode.output", "Output", "Salida");
        LangEntries.add(p + "status", "Status: %s", "Estado: %s");
        LangEntries.add(p + "connect_inventory", "Connect inventory on: %s", "Conecta el inventario por: %s");
        LangEntries.add(p + "connect_pump", "Connect pump on: %s", "Conecta la bomba por: %s");
        LangEntries.add(p + "accepts", "Accepts fuel assemblies at %s%% or more", "Acepta elementos combustibles de %s%% o más");
        LangEntries.add(p + "side.north", "NORTH", "NORTE");
        LangEntries.add(p + "side.south", "SOUTH", "SUR");
        LangEntries.add(p + "side.east", "EAST", "ESTE");
        LangEntries.add(p + "side.west", "WEST", "OESTE");
        LangEntries.add(p + "side.up", "UP", "ARRIBA");
        LangEntries.add(p + "side.down", "DOWN", "ABAJO");
    }

    /** Chat and goggles of the steam generator, the turbines, the cooling tower and the RTG. */
    private static void plantMessages() {
        String sg = "create_nuclearindustry.steam_generator.message.";
        LangEntries.add(sg + "formed", "Steam generator formed %s", "Generador de vapor formado %s");
        LangEntries.add(sg + "tubes", "%s tube bundles", "%s haces de tubos");
        LangEntries.add(sg + "broken", "Steam generator broken: %s", "Generador de vapor roto: %s");
        LangEntries.add(sg + "incomplete", "Steam generator incomplete: %s", "Generador de vapor incompleto: %s");

        String ct = "create_nuclearindustry.cooling_tower.";
        LangEntries.add(ct + "message.formed", "Cooling tower formed %s", "Torre de refrigeración formada %s");
        LangEntries.add(ct + "message.broken", "Cooling tower broken: %s", "Torre de refrigeración rota: %s");
        LangEntries.add(ct + "message.incomplete", "Cooling tower incomplete: %s", "Torre de refrigeración incompleta: %s");
        LangEntries.add(ct + "goggles.title", "Cooling Tower", "Torre de refrigeración");
        LangEntries.add(ct + "goggles.size", "Base radius %s, %s high, %s fans", "Radio de la base %s, %s de alto, %s ventiladores");
        LangEntries.add(ct + "goggles.condensing", "Condensing %s / %s mB/t", "Condensando %s / %s mB/t");
        LangEntries.add(ct + "goggles.steam", "Steam: %s / %s mB", "Vapor: %s / %s mB");
        LangEntries.add(ct + "goggles.water", "Water: %s / %s mB", "Agua: %s / %s mB");

        String t = "create_nuclearindustry.turbine.";
        LangEntries.add(t + "message.formed.steam", "Steam turbine formed %s", "Turbina de vapor formada %s");
        LangEntries.add(t + "message.formed.plasma", "Plasma steam turbine formed %s", "Turbina de vapor de plasma formada %s");
        LangEntries.add(t + "message.details", "[%s, %s rotors, up to %s mB/t]", "[%s, %s rotores, hasta %s mB/t]");
        LangEntries.add(t + "message.broken", "Turbine broken: %s", "Turbina rota: %s");
        LangEntries.add(t + "message.incomplete", "Turbine incomplete: %s", "Turbina incompleta: %s");
        LangEntries.add(t + "legacy_line", "This is an old 3-block turbine. Rebuild it as a box (3-7 wide and high, 3-16 long); see the Ponder scene",
                "Es una turbina antigua de 3 bloques. Reconstrúyela como una caja (de 3 a 7 de ancho y alto, de 3 a 16 de largo); mira la escena de Ponder");
        LangEntries.add(t + "title.steam", "Steam Turbine", "Turbina de vapor");
        LangEntries.add(t + "title.plasma", "Plasma Steam Turbine", "Turbina de vapor de plasma");
        LangEntries.add(t + "goggles.size", "%s, %s rotors, ports %s in / %s out", "%s, %s rotores, puertos %s de entrada / %s de salida");
        LangEntries.add(t + "goggles.expanding", "Expanding %s / %s mB/t", "Expandiendo %s / %s mB/t");
        LangEntries.add(t + "goggles.exhausting", "Exhausting to a cooling tower: x2 power", "Escape a una torre de refrigeración: potencia x2");
        LangEntries.add(t + "goggles.exhaust_full", "Exhaust full: send it to a cooling tower", "Escape lleno: llévalo a una torre de refrigeración");
        LangEntries.add(t + "goggles.condensate_full", "Condensate full: pump the water out", "Condensado lleno: bombea el agua fuera");
        LangEntries.add(t + "goggles.tank.steam", "Steam: %s / %s mB", "Vapor: %s / %s mB");
        LangEntries.add(t + "goggles.tank.plasma", "Plasma steam: %s / %s mB", "Vapor de plasma: %s / %s mB");
        LangEntries.add(t + "goggles.exhaust", "Exhaust: %s / %s mB", "Escape: %s / %s mB");
        LangEntries.add(t + "goggles.condensate", "Condensate: %s / %s mB", "Condensado: %s / %s mB");
        LangEntries.add(t + "display.no_turbine", "No turbine", "Sin turbina");
        LangEntries.add(t + "display.not_formed", "Not formed", "Sin formar");
        LangEntries.add(t + "display.speed", "Speed: %s RPM", "Velocidad: %s RPM");
        LangEntries.add(t + "display.steam", "Steam: %s/%s mB/t", "Vapor: %s/%s mB/t");
        LangEntries.add(t + "display.capacity", "Capacity: %s su", "Capacidad: %s su");
        LangEntries.add(t + "display.tank", "Tank: %s mB", "Depósito: %s mB");
        LangEntries.add(t + "display.exhaust_tower", "Exhaust: to cooling tower", "Escape: a la torre de refrigeración");
        LangEntries.add(t + "display.exhaust_condensing", "Exhaust: condensing", "Escape: condensando");

        LangEntries.add("create_nuclearindustry.rtg.goggles.no_pellet", "No pellet loaded", "Sin pastilla cargada");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile viewport = provider.models().cubeAll("reactor_viewport", provider.modLoc("block/reactor_viewport"))
                .renderType("cutout");
        provider.simpleBlockWithItem(ReactorVisualsRegistry.REACTOR_VIEWPORT.get(), viewport);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        // Placing or breaking a viewport in the wall revalidates the reactor, like its casing.
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(ReactorVisualsRegistry.REACTOR_VIEWPORT.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Renamed("Reactor viewport recipes", new ReactorVisualsRecipes(output, lookup)));
    }

    /** Data providers need unique names, and every vanilla recipe provider is called "Recipes" (final). */
    private record Renamed(String name, DataProvider provider) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return provider.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        provider.builderFor(AllNuclearRadiation.TRANSMISSION).add(ReactorVisualsRegistry.REACTOR_VIEWPORT.get().builtInRegistryHolder(),
                new RadiationTransmission(VIEWPORT_TRANSMISSION), false);
    }
}
