package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.data.recipe.ControlRoom2Recipes;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.add;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.item;

/** Lang, models, tags, data maps, loot and recipes for the chart recorder, the annunciator panel and automatic rod control. */
final class ControlRoom2DataGen extends AreaDataGen {
    static final ControlRoom2DataGen INSTANCE = new ControlRoom2DataGen();

    private ControlRoom2DataGen() {}

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ResourceLocation housing = provider.modLoc("block/control_monitor_side");
        ModelFile recorder = provider.models().orientable("chart_recorder", housing,
                provider.modLoc("block/chart_recorder_front"), provider.modLoc("block/chart_recorder_top"));
        provider.horizontalBlock(ControlRoom2Registry.CHART_RECORDER.get(), recorder);
        provider.simpleBlockItem(ControlRoom2Registry.CHART_RECORDER.get(), recorder);
        ModelFile panel = provider.models().orientable("annunciator_panel", housing,
                provider.modLoc("block/annunciator_panel_front"), housing);
        provider.horizontalBlock(ControlRoom2Registry.ANNUNCIATOR_PANEL.get(), panel);
        provider.simpleBlockItem(ControlRoom2Registry.ANNUNCIATOR_PANEL.get(), panel);
    }

    /** The controller module's item model is a hand-written composite, like the other modules'. */
    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(ControlRoom2Registry.CHART_ROLL.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new ControlRoom2Recipes(output, lookup));
    }

    @Override
    void lang() {
        chartRecorder();
        annunciator();
        controller();
        plantReadings();
        tokamak();
    }

    private static void chartRecorder() {
        block("chart_recorder", "Chart Recorder", "Registrador gráfico");
        item("chart_roll", "Chart Roll", "Rollo de gráfica");
        String c = "create_nuclearindustry.chart_recorder.";
        add(c + "blank", "Nothing drawn yet", "Todavía no ha dibujado nada");
        add(c + "status", "%s sheets of paper left, one sample every %s ticks",
                "Quedan %s hojas de papel, una muestra cada %s ticks");
        add(c + "no_paper", "Out of paper: click it with paper or feed it with a hopper",
                "Sin papel: haz clic con papel o aliméntalo con una tolva");
        add(c + "no_paper_short", "NO PAPER", "SIN PAPEL");
        add(c + "pen", "Pen %s: %s", "Plumilla %s: %s");
        add(c + "pen.off", "Off", "Apagada");
        add(c + "speed", "Paper speed: every %s ticks", "Velocidad del papel: cada %s ticks");
        String roll = "item.create_nuclearindustry.chart_roll.";
        add(roll + "blank", "Blank", "En blanco");
        add(roll + "source", "%s, last %s s", "%s, últimos %s s");
        add(roll + "hint", "Use it to read the chart", "Úsalo para leer la gráfica");
        String screen = "create_nuclearindustry.chart_roll.";
        add(screen + "title", "Chart: %s", "Gráfica: %s");
        add(screen + "span", "-%s s", "-%s s");
        add(screen + "now", "now", "ahora");
        add(screen + "blank", "The chart is blank", "La gráfica está en blanco");
        add(screen + "last", "last %s", "último %s");
    }

    private static void annunciator() {
        block("annunciator_panel", "Annunciator Panel", "Panel anunciador");
        String a = "create_nuclearindustry.annunciator.";
        add(a + "unlinked", "Window %s is not linked: click it with a Control Room Linker",
                "La ventana %s no está enlazada: haz clic en ella con un Enlazador de sala de control");
        add(a + "describe", "Window %s: %s %s, %s", "Ventana %s: %s %s, %s");
        add(a + "state.normal", "normal", "normal");
        add(a + "state.alert", "ALERT, acknowledge it", "ALERTA, reconócela");
        add(a + "state.acknowledged", "acknowledged", "reconocida");
        add(a + "state.ringback", "cleared, reset it", "despejada, rearma");
        add(a + "ack", "ACK", "RECON.");
        add(a + "reset", "RESET", "REARME");
        add(a + "label", "Label", "Etiqueta");
        add(a + "label.hint", "Label (optional)", "Etiqueta (opcional)");
        add(a + "unlink", "Unlink", "Desenlazar");
        add(a + "screen.unlinked", "Not linked: click this window with a linker", "Sin enlazar: haz clic en esta ventana con un enlazador");
        add(a + "screen.linked", "%s at %s %s %s", "%s en %s %s %s");
        add(a + "screen.state", "State: %s", "Estado: %s");
        add(a + "screen.first_out", "%s (first out)", "%s (primera alarma)");
        String s = a + "short.";
        add(s + "temperature", "CORE TEMP HI", "TEMP NÚCLEO ALTA");
        add(s + "power", "POWER HI", "POTENCIA ALTA");
        add(s + "xenon", "XENON HI", "XENÓN ALTO");
        add(s + "coolant_low", "COOLANT LO", "REFRIG. BAJO");
        add(s + "scram", "SCRAM", "SCRAM");
        add(s + "turbine_load", "TURBINE LOAD", "CARGA TURBINA");
        add(s + "tower_load", "TOWER LOAD", "CARGA TORRE");
        add(s + "area_radiation", "RADIATION", "RADIACIÓN");
        add(s + "plasma_temperature", "PLASMA TEMP", "TEMP PLASMA");
        add(s + "plasma_instability", "PLASMA UNSTABLE", "PLASMA INESTABLE");
        add(s + "pressure_high", "PRESS HI", "PRESIÓN ALTA");
        add(s + "pressure_low", "PRESS LO", "PRESIÓN BAJA");
        add(s + "subcooling_low", "SUBCOOLING LO", "SUBENFR. BAJO");
        add(s + "pzr_level", "PZR LEVEL LO", "NIVEL PZR BAJO");
        add(s + "loss_of_flow", "LOSS OF FLOW", "PÉRDIDA CAUDAL");
        add(s + "sg_level_low", "SG LEVEL LO", "NIVEL GV BAJO");
        add(s + "containment_pressure", "CONT PRESS HI", "PRES. CONT. ALTA");
        add(s + "hydrogen", "HYDROGEN HI", "HIDRÓGENO ALTO");
        String m = "create_nuclearindustry.alarm.metric.";
        add(m + "pressure_high", "Primary pressure above", "Presión del primario por encima de");
        add(m + "pressure_low", "Primary pressure below", "Presión del primario por debajo de");
        add(m + "subcooling_low", "Subcooling margin below", "Margen de subenfriamiento por debajo de");
        add(m + "pzr_level", "Pressurizer level below", "Nivel del presionador por debajo de");
        add(m + "loss_of_flow", "Primary flow below", "Caudal del primario por debajo de");
        add(m + "sg_level_low", "Steam generator level below", "Nivel del generador de vapor por debajo de");
        add(m + "containment_pressure", "Containment pressure above", "Presión de la contención por encima de");
        add(m + "hydrogen", "Containment hydrogen above", "Hidrógeno en la contención por encima de");
        add(s + "relief_open", "RELIEF OPEN", "ALIVIO ABIERTA");
        add(s + "sg_level_high", "SG LEVEL HI", "NIVEL GV ALTO");
        add(s + "sg_steam_low", "STEAM FLOW LO", "VAPOR BAJO");
        add(s + "sg_primary_dt_low", "PRIMARY ΔT LO", "ΔT PRIMARIO BAJO");
        add(s + "containment_open", "CONT OPEN", "CONT. ABIERTA");
        add(s + "containment_activity", "ACTIVITY HI", "ACTIVIDAD ALTA");
        add(m + "relief_open", "Pressurizer relief valve open", "Válvula de alivio del presionador abierta");
        add(m + "sg_level_high", "Steam generator level above", "Nivel del generador de vapor por encima de");
        add(m + "sg_steam_low", "Steam generator output below", "Vapor del generador de vapor por debajo de");
        add(m + "sg_primary_dt_low", "Steam generator primary ΔT below", "ΔT primario del generador de vapor por debajo de");
        add(m + "containment_open", "Containment not sealed", "Contención no estanca");
        add(m + "containment_activity", "Containment airborne activity above", "Actividad en el aire de la contención por encima de");
        presets(a);
    }

    /** Annunciator presets: their names and the labels they give the six windows. */
    private static void presets(String a) {
        String p = a + "preset.";
        add(p + "apply", "OK", "OK");
        add(p + "hint", "Preset for all six windows: %s. Click to choose another (sneak to go back), OK to apply it to every window. Links stay as they are.",
                "Plantilla para las seis ventanas: %s. Haz clic para elegir otra (agachado para volver) y OK para aplicarla a todas. Los enlaces no cambian.");
        preset(p + "reactor_core", "Reactor core", "Núcleo",
                new String[]{"CORE TEMP HI", "CORE TEMP HI-HI", "POWER HI", "XENON HI", "COOLANT LO", "SCRAM"},
                new String[]{"TEMP NÚCLEO ALTA", "T NÚCLEO M. ALTA", "POTENCIA ALTA", "XENÓN ALTO", "REFRIG. BAJO", "SCRAM"});
        preset(p + "pwr_primary", "PWR primary", "Primario PWR",
                new String[]{"PRESS HI", "PRESS LO", "RELIEF OPEN", "SUBCOOL LO", "PZR LEVEL LO", "LOSS OF FLOW"},
                new String[]{"PRESIÓN ALTA", "PRESIÓN BAJA", "ALIVIO ABIERTA", "SUBENFR. BAJO", "NIVEL PZR BAJO", "PÉRDIDA CAUDAL"});
        preset(p + "steam_generator", "Steam gen.", "Gen. vapor",
                new String[]{"SG LEVEL LO", "SG LEVEL LO-LO", "SG DRYOUT", "SG LEVEL HI", "STEAM FLOW LO", "PRIMARY ΔT LO"},
                new String[]{"NIVEL GV BAJO", "NIVEL GV M. BAJO", "GV SECO", "NIVEL GV ALTO", "VAPOR BAJO", "ΔT PRIMARIO BAJO"});
        preset(p + "containment", "Containment", "Contención",
                new String[]{"CONT PRESS HI", "CONT PRESS HI-HI", "H2 HI", "H2 HI-HI", "ACTIVITY HI", "CONT OPEN"},
                new String[]{"PRES. CONT. ALTA", "P. CONT. M. ALTA", "H2 ALTO", "H2 MUY ALTO", "ACTIVIDAD ALTA", "CONT. ABIERTA"});
    }

    private static void preset(String key, String english, String spanish, String[] labelsEn, String[] labelsEs) {
        add(key, english, spanish);
        for (int i = 0; i < labelsEn.length; i++) {
            add(key + "." + (i + 1), labelsEn[i], labelsEs[i]);
        }
    }

    private static void controller() {
        item("controller_module", "Controller Module", "Módulo controlador");
        String m = "create_nuclearindustry.control_module.";
        add(m + "type.controller", "Controller", "Controlador");
        add(m + "function.auto_power", "Automatic power", "Potencia automática");
        add(m + "function.auto_temperature", "Automatic temperature", "Temperatura automática");
        add(m + "mode.auto", "Mode: automatic", "Modo: automático");
        add(m + "mode.standby", "Mode: standby", "Modo: en espera");
        add(m + "standby", "standby", "en espera");
        add(m + "controller_state", "%s, %s", "%s, %s");
        add(m + "controller_demand", "rods to %s%%", "barras al %s%%");
        add(m + "setpoint", "Setpoint %s, %s", "Consigna %s, %s");
        pressurizerControl(m);
    }

    /** Levers, lamp and controller for a pressurizer: backup heaters, spray, relief valve and automatic pressure. */
    private static void pressurizerControl(String m) {
        add(m + "function.pzr_heaters", "Pressurizer backup heaters", "Calentadores de respaldo del presionador");
        add(m + "function.pzr_spray", "Pressurizer spray", "Rociado del presionador");
        add(m + "function.auto_pressure", "Automatic pressure", "Presión automática");
        add(m + "function.lamp_relief_open", "Relief valve open", "Válvula de alivio abierta");
        add(m + "pressure_heaters", "heaters %s%%", "calentadores al %s%%");
        add(m + "pressure_spray", "spray %s%%", "rociado al %s%%");
    }

    /**
     * The tokamak's startup and plasma control in the control room: monitor rows, alarms, the annunciator
     * preset, desk functions, gauges and pens, and the Display Link lines.
     */
    private static void tokamak() {
        String monitor = "create_nuclearindustry.monitor.";
        add(monitor + "trip", "Trip", "Disparo");
        add(monitor + "gain", "Gain", "Ganancia");
        add(monitor + "density", "Density", "Densidad");
        add(monitor + "field", "Field", "Campo");
        add(monitor + "magnets", "Magnets", "Imanes");
        add(monitor + "vacuum", "Vacuum", "Vacío");
        add(monitor + "heating", "Heating", "Calentam.");
        add(monitor + "off", "Off", "Apagado");
        add(monitor + "fuel_rate", "Fuelling", "Inyección");
        add(monitor + "helium", "Liquid He", "He líquido");
        add(monitor + "ash", "He ash", "Cenizas");
        add("create_nuclearindustry.control_room.status.tripped", "Tripped", "Disparado");

        String metric = "create_nuclearindustry.alarm.metric.";
        add(metric + "vacuum_poor", "Tokamak chamber pressure above", "Presión de la cámara del tokamak por encima de");
        add(metric + "magnet_temperature", "Tokamak magnet temperature above", "Temperatura de los imanes del tokamak por encima de");
        add(metric + "fusion_gain_low", "Fusion gain Q below", "Ganancia de fusión Q por debajo de");
        add(metric + "plasma_density", "Plasma density, share of its limit, above",
                "Densidad del plasma, fracción de su límite, por encima de");
        add(metric + "tritium_low", "Tokamak tritium below", "Tritio del tokamak por debajo de");
        add(metric + "tokamak_trip", "Tokamak tripped", "Disparo del tokamak");
        String a = "create_nuclearindustry.annunciator.";
        String s = a + "short.";
        add(s + "vacuum_poor", "VACUUM POOR", "VACÍO POBRE");
        add(s + "magnet_temperature", "MAGNET TEMP HI", "TEMP IMANES ALTA");
        add(s + "fusion_gain_low", "Q LOW", "Q BAJO");
        add(s + "plasma_density", "DENSITY HI", "DENSIDAD ALTA");
        add(s + "tritium_low", "TRITIUM LO", "TRITIO BAJO");
        add(s + "tokamak_trip", "TOKAMAK TRIP", "DISPARO TOKAMAK");
        preset(a + "preset.tokamak", "Tokamak", "Tokamak",
                new String[]{"INSTABILITY HI", "DENSITY HI", "VACUUM POOR", "MAGNET TEMP HI", "Q LOW", "TOKAMAK TRIP"},
                new String[]{"INESTAB. ALTA", "DENSIDAD ALTA", "VACÍO POBRE", "TEMP IMANES ALTA", "Q BAJO", "DISPARO TOKAMAK"});

        String m = "create_nuclearindustry.control_module.";
        add(m + "function.tokamak_start", "Tokamak start", "Arranque del tokamak");
        add(m + "function.tokamak_shutdown", "Tokamak shutdown", "Parada del tokamak");
        add(m + "function.tokamak_heating", "Tokamak heating", "Calentamiento del tokamak");
        add(m + "function.auto_fusion_power", "Automatic fusion power", "Potencia de fusión automática");
        add(m + "fusion_demand", "fuel %s%%, heating %s%%", "inyección %s%%, calentamiento %s%%");
        add(m + "tokamak.start", "Tokamak starting up", "El tokamak arranca");
        add(m + "tokamak.refused", "Start refused: %s", "Arranque rechazado: %s");
        add(m + "tokamak.shutdown", "Tokamak shutting down", "El tokamak se apaga");
        add(m + "tokamak.already_offline", "The tokamak is already offline", "El tokamak ya está apagado");
        String r = m + "reading.";
        add(r + "fusion_gain", "Fusion gain (Q)", "Ganancia de fusión (Q)");
        add(r + "plasma_density", "Plasma density", "Densidad del plasma");
        add(r + "helium_ash", "Helium ash", "Cenizas de helio");
        add(r + "magnetic_field", "Magnetic field", "Campo magnético");
        add(r + "magnet_temperature", "Magnet temperature", "Temperatura de los imanes");
        add(r + "chamber_pressure", "Chamber pressure (log)", "Presión de la cámara (log.)");
        add(r + "heating_power", "Heating power", "Potencia de calentamiento");
        add(r + "fuel_rate", "Fuelling rate", "Ritmo de inyección");
        add(r + "liquid_helium", "Liquid helium", "Helio líquido");
        add(r + "tritium", "Tokamak tritium", "Tritio del tokamak");

        String d = "create_nuclearindustry.display_source.tokamak.";
        add(d + "none", "No tokamak", "Sin tokamak");
        add(d + "unformed", "Not formed", "Sin formar");
        add(d + "tripped", "Tripped: %s", "Disparo: %s");
        add(d + "phase", "Phase: %s", "Fase: %s");
        add(d + "plasma", "Plasma: %s° %s", "Plasma: %s° %s");
        add(d + "mode.dt", "D-T", "D-T");
        add(d + "mode.dd", "D-D", "D-D");
        add(d + "mode.cold", "cold", "frío");
        add(d + "power", "Fusion: %s MW, %s", "Fusión: %s MW, %s");
        add(d + "ignited", "ignited", "en ignición");
        add(d + "density", "Density: %s%%, ash %s%%", "Densidad: %s%%, cenizas %s%%");
        add(d + "magnets", "Field: %s%%, magnets %s K", "Campo: %s%%, imanes %s K");
        add(d + "vacuum", "Vacuum: %s", "Vacío: %s");
        add(d + "heating", "Heating: %s MW (%s%%)", "Calentamiento: %s MW (%s%%)");
        add(d + "heating_off", "Heating: off", "Calentamiento: apagado");
        add(d + "fuel", "Fuel: %s mB/t, D %s mB, T %s mB", "Combustible: %s mB/t, D %s mB, T %s mB");
        add(d + "helium", "Helium: %s mB liquid, %s mB warm", "Helio: %s mB líquido, %s mB caliente");
        add(d + "stability", "Instability %s%%, confinement %s%%", "Inestabilidad %s%%, confinamiento %s%%");
        add(d + "tbr", "TBR: %s", "TBR: %s");
    }

    /** Monitor rows, gauges and target names for the pressurized water loop, steam generators and containments. */
    private static void plantReadings() {
        String kind = "create_nuclearindustry.control_room.kind.";
        add(kind + "steam_generator", "Steam Generator", "Generador de vapor");
        add(kind + "containment", "Containment", "Contención");
        String monitor = "create_nuclearindustry.monitor.";
        add(monitor + "pressure", "Pressure", "Presión");
        add(monitor + "subcooling", "Subcooling", "Subenfr.");
        add(monitor + "pzr_level", "Pressurizer", "Presionador");
        add(monitor + "flow", "Flow", "Caudal");
        add(monitor + "legs", "Hot/cold leg", "Rama cal./fría");
        add(monitor + "containment", "Containment", "Contención");
        add(monitor + "hydrogen", "Hydrogen", "Hidrógeno");
        add(monitor + "vessel", "Vessel level", "Nivel vasija");
        add(monitor + "level", "Level", "Nivel");
        add(monitor + "steam_made", "Steam", "Vapor");
        add(monitor + "heat", "Heat", "Calor");
        add(monitor + "primary_dt", "Primary ΔT", "ΔT primario");
        add(monitor + "formed", "Formed", "Formado");
        add(monitor + "sealed", "Sealed", "Estanca");
        add(monitor + "vent", "Vent", "Venteo");
        add(monitor + "open", "Open", "Abierto");
        add(monitor + "closed", "Closed", "Cerrado");
        add(monitor + "activity", "Airborne", "En el aire");
        add(monitor + "volume", "Volume", "Volumen");
        add(monitor + "relief", "Relief valve", "Válv. alivio");
        String r = "create_nuclearindustry.control_module.reading.";
        add(r + "reactor_pressure", "Primary pressure", "Presión del primario");
        add(r + "reactor_hot_leg", "Hot leg temperature", "Temperatura de la rama caliente");
        add(r + "reactor_cold_leg", "Cold leg temperature", "Temperatura de la rama fría");
        add(r + "reactor_subcooling", "Subcooling margin", "Margen de subenfriamiento");
        add(r + "pressurizer_level", "Pressurizer level", "Nivel del presionador");
        add(r + "primary_flow", "Primary flow", "Caudal del primario");
        add(r + "vessel_level", "Vessel water level", "Nivel de agua en la vasija");
        add(r + "reactor_containment_pressure", "Containment pressure (reactor)", "Presión de la contención (reactor)");
        add(r + "sg_level", "Steam generator level", "Nivel del generador de vapor");
        add(r + "sg_steam", "Steam generator output", "Vapor del generador de vapor");
        add(r + "sg_heat", "Steam generator heat", "Calor del generador de vapor");
        add(r + "sg_primary_dt", "Steam generator primary ΔT", "ΔT primario del generador de vapor");
        add(r + "containment_pressure", "Containment pressure", "Presión de la contención");
        add(r + "containment_hydrogen", "Containment hydrogen", "Hidrógeno en la contención");
        add(r + "containment_activity", "Containment airborne activity (log)", "Actividad en el aire de la contención (log.)");
    }
}
