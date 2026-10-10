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
        add(c + "speed", "Paper speed: one sample every %s ticks", "Velocidad del papel: una muestra cada %s ticks");
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
    }

    private static void controller() {
        item("controller_module", "Rod Controller Module", "Módulo controlador de barras");
        String m = "create_nuclearindustry.control_module.";
        add(m + "type.controller", "Rod Controller", "Controlador de barras");
        add(m + "function.auto_power", "Automatic power", "Potencia automática");
        add(m + "function.auto_temperature", "Automatic temperature", "Temperatura automática");
        add(m + "mode.auto", "Mode: automatic", "Modo: automático");
        add(m + "mode.standby", "Mode: standby", "Modo: en espera");
        add(m + "standby", "standby", "en espera");
        add(m + "controller_state", "%s, rods %s", "%s, barras %s");
        add(m + "controller_demand", "rods to %s%%", "barras al %s%%");
        add(m + "setpoint", "Setpoint %s, %s", "Consigna %s, %s");
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
    }
}
