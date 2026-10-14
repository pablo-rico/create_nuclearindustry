package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlock;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlock;
import org.papiricoh.create_nuclearindustry.data.recipe.ControlRoom2Recipes;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.add;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.item;

/**
 * Lang, models, tags, data maps, loot and recipes for the control table and wall control panel, the
 * chart recorder, the annunciator panel and automatic rod control.
 */
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
        surfaceStates(provider, ControlRoom2Registry.CONTROL_TABLE.get(), "control_table");
        surfaceStates(provider, ControlRoom2Registry.WALL_CONTROL_PANEL.get(), "wall_control_panel");
        advancedLinkStates(provider);
        servoStates(provider);
    }

    /** The servo's hand-made model, its sides' arrows lit by MOTION, turned to its shaft like Create's gearshift. */
    private static void servoStates(NuclearBlockStateProvider provider) {
        provider.getVariantBuilder(ControlRoom2Registry.SERVO_GEARBOX.get()).forAllStates(state -> {
            ServoGearboxBlock.Motion motion = state.getValue(ServoGearboxBlock.MOTION);
            Direction.Axis axis = state.getValue(ServoGearboxBlock.AXIS);
            String model = motion == ServoGearboxBlock.Motion.IDLE ? "block" : motion.getSerializedName();
            return ConfiguredModel.builder()
                    .modelFile(provider.models().getExistingFile(provider.modLoc("block/servo_gearbox/" + model)))
                    .rotationX(axis == Direction.Axis.Y ? 0 : 90)
                    .rotationY(axis == Direction.Axis.X ? 90 : axis == Direction.Axis.Z ? 180 : 0)
                    .build();
        });
    }

    /** The link's four hand-made models (transmitter or receiver, lit or not), built facing up and turned like an end rod. */
    private static void advancedLinkStates(NuclearBlockStateProvider provider) {
        provider.getVariantBuilder(ControlRoom2Registry.ADVANCED_REDSTONE_LINK.get()).forAllStates(state -> {
            Direction facing = state.getValue(AdvancedRedstoneLinkBlock.FACING);
            String variant = (state.getValue(AdvancedRedstoneLinkBlock.RECEIVER) ? "receiver" : "transmitter")
                    + (state.getValue(AdvancedRedstoneLinkBlock.POWERED) ? "_powered" : "");
            return ConfiguredModel.builder()
                    .modelFile(provider.models().getExistingFile(provider.modLoc("block/advanced_redstone_link/" + variant)))
                    .rotationX(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
                    .rotationY(facing.getAxis().isVertical() ? 0 : ((int) facing.toYRot() + 180) % 360)
                    .build();
        });
    }

    /**
     * A table or wall panel: its body, plus the end panel on each side no block of its kind carries
     * on from. The models are hand-made in models/block/{@code folder}, with the operator to the north;
     * the item models are hand-made composites.
     */
    private static void surfaceStates(NuclearBlockStateProvider provider, Block block, String folder) {
        ModelFile body = provider.models().getExistingFile(provider.modLoc("block/" + folder + "/body"));
        ModelFile capLeft = provider.models().getExistingFile(provider.modLoc("block/" + folder + "/cap_left"));
        ModelFile capRight = provider.models().getExistingFile(provider.modLoc("block/" + folder + "/cap_right"));
        var builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int yaw = ((int) facing.toYRot() + 180) % 360;
            builder.part().modelFile(body).rotationY(yaw).addModel()
                    .condition(BlockStateProperties.HORIZONTAL_FACING, facing).end();
            builder.part().modelFile(capLeft).rotationY(yaw).addModel()
                    .condition(BlockStateProperties.HORIZONTAL_FACING, facing).condition(ModuleSurfaceBlock.LEFT, false).end();
            builder.part().modelFile(capRight).rotationY(yaw).addModel()
                    .condition(BlockStateProperties.HORIZONTAL_FACING, facing).condition(ModuleSurfaceBlock.RIGHT, false).end();
        }
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
        moduleSurfaces();
        newModules();
        advancedLink();
        servo();
        panel();
        plantReadings();
        tokamak();
        monitorSetup();
        moduleScreen();
    }

    /** The wrench setup of a Control Room Monitor's screen. */
    private static void monitorSetup() {
        String s = "create_nuclearindustry.monitor.setup.";
        add(s + "title", "Title", "Título");
        add(s + "layout", "Layout", "Disposición");
        add(s + "layout.auto", "Automatic", "Automática");
        add(s + "layout.list", "Readings only", "Solo lecturas");
        add(s + "layout.chart", "Graph only", "Solo gráfica");
        add(s + "layout.both", "Readings and graph", "Lecturas y gráfica");
        add(s + "series_a", "Graph A", "Gráfica A");
        add(s + "series_b", "Graph B", "Gráfica B");
        add(s + "series.default", "Default", "Por defecto");
        add(s + "rows", "Readings", "Lecturas");
        add(s + "rows.unlinked", "Link the screen first", "Enlaza antes la pantalla");
        add(s + "rows.up", "Move up", "Subir");
        add(s + "rows.down", "Move down", "Bajar");
        add(s + "rows.default", "Default: usual order, all shown", "Por defecto: orden habitual, todas visibles");
        add(s + "rows.shown", "Shown", "Visible");
        add(s + "rows.hidden", "Hidden", "Oculta");
        add(s + "rows.show", "Show this reading", "Mostrar esta lectura");
        add(s + "rows.hide", "Hide this reading", "Ocultar esta lectura");
        add("create_nuclearindustry.monitor.no_rows", "No readings shown", "Ninguna lectura visible");
        add("jei.create_nuclearindustry.info.control_monitor",
                "Right-click a Monitor with a wrench to set up its screen, from any of its blocks: a title of your own, the layout (readings, graph, both, or the automatic one that switches on click), what the graph's two lines plot, and which readings it lists in which order. Click a reading in the list to show or hide it or move it up or down. A screen that grows keeps its setup.",
                "Haz clic derecho en un Monitor con una llave inglesa para configurar su pantalla, desde cualquiera de sus bloques: un título propio, la disposición (lecturas, gráfica, ambas o la automática que cambia al hacer clic), qué dibujan las dos líneas de la gráfica y qué lecturas lista y en qué orden. Haz clic en una lectura de la lista para mostrarla, ocultarla, subirla o bajarla. Una pantalla que crece conserva su configuración.");
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
        add(m + "controller_demand", "rods to %s%%", "barras al %s%%");
        add(m + "setpoint", "Setpoint %s, %s", "Consigna %s, %s");
        pressurizerControl(m);
    }

    /** The control table, the wall control panel and the LED display module. */
    private static void moduleSurfaces() {
        block("control_table", "Control Table", "Mesa de control");
        block("wall_control_panel", "Wall Control Panel", "Panel de control de pared");
        item("led_display_module", "LED Display Module", "Módulo de display LED");
        String m = "create_nuclearindustry.control_module.";
        add(m + "type.led_display", "LED Display", "Display LED");
    }

    /** The guarded button and the screen modules: bar graph, trend recorder, core map and plasma section. */
    private static void newModules() {
        String m = "create_nuclearindustry.control_module.";
        item("guarded_button_module", "Guarded Button Module", "Módulo de pulsador con tapa");
        item("bar_graph_module", "Bar Graph Module", "Módulo de barra gráfica");
        item("trend_recorder_module", "Trend Recorder Module", "Módulo de registrador de tendencia");
        item("core_map_module", "Core Map Module", "Módulo de mapa del núcleo");
        item("plasma_section_module", "Plasma Section Module", "Módulo de sección del plasma");
        add(m + "type.guarded_button", "Guarded Button", "Pulsador con tapa");
        add(m + "type.bar_graph", "Bar Graph", "Barra gráfica");
        add(m + "type.trend_recorder", "Trend Recorder", "Registrador de tendencia");
        add(m + "type.core_map", "Core Map", "Mapa del núcleo");
        add(m + "type.plasma_section", "Plasma Section", "Sección del plasma");
        add(m + "function.core_view", "Core map", "Mapa del núcleo");
        add(m + "function.plasma_view", "Plasma section", "Sección del plasma");
        add(m + "function.core_view.desc",
                "Shows the reactor's columns from above, north at the top: fuel green to red as it burns, control channels blue as their rods go in, moderator, reflector and heat exchangers. Four in one block make one big map.",
                "Muestra las columnas del reactor desde arriba, con el norte arriba: el combustible de verde a rojo según se quema, los canales de control en azul según entran las barras, moderador, reflector e intercambiadores. Cuatro en un bloque forman un mapa grande.");
        add(m + "function.plasma_view.desc",
                "A cut through the tokamak with its plasma: brighter the hotter it is, bigger the denser, shaking as it grows unstable, with a field line for each step of the magnetic field. Four in one block make one big screen.",
                "Un corte del tokamak con su plasma: más brillante cuanto más caliente, más grande cuanto más denso, temblando al volverse inestable, con una línea de campo por cada escalón del campo magnético. Cuatro en un bloque forman una pantalla grande.");
        add(m + "cover.open", "Cover lifted: click again to press, sneak-click to close it",
                "Tapa levantada: haz clic otra vez para pulsar, agachado para cerrarla");
        add(m + "cover.closed", "Cover closed", "Tapa cerrada");
        String s = m + "screen.";
        add(s + "alarm_level", "Red from", "Rojo desde");
        add(s + "trend_speed", "Each sample", "Cada muestra");
        String p = "create_nuclearindustry.panel.";
        add(p + "alarm_level", "Red from", "Rojo desde");
        add(p + "trend_span", "Shows", "Muestra");
        add(p + "trend_span_value", "last %s s", "últimos %s s");
        add(p + "cover", "Cover", "Tapa");
        add(p + "cover.open", "lifted", "levantada");
        add(p + "cover.closed", "closed", "cerrada");
        add(p + "core.fuel", "Fuel", "Combustible");
        add(p + "core.channels", "Channels", "Canales");
        add(p + "core.moderator", "Moderator", "Moderador");
        add(p + "core.reflector", "Reflector", "Reflector");
        add(p + "core.burnup", "Burnup", "Quemado");
        add(p + "plasma.phase", "Phase", "Fase");
        add(p + "plasma.temperature", "Temperature", "Temperatura");
        add(p + "plasma.instability", "Instability", "Inestabilidad");
        add(p + "plasma.field_lines", "Field lines", "Líneas de campo");
    }

    /** The Advanced Redstone Link and control modules sending on its codes. */
    private static void advancedLink() {
        block("advanced_redstone_link", "Advanced Redstone Link", "Enlace de redstone avanzado");
        String a = "create_nuclearindustry.advanced_link.";
        add(a + "code_box", "Code", "Código");
        add(a + "hint", "Four letters or digits, as on the other links", "Cuatro letras o cifras, como en los otros enlaces");
        String m = "create_nuclearindustry.control_module.";
        add(m + "output.advanced_link", "Advanced Redstone Link", "Enlace de redstone avanzado");
        add(m + "screen.output_hint.advanced_link", "Sends the signal on a code of four letters or digits, to Advanced Redstone Links set to it",
                "Manda la señal por un código de cuatro letras o cifras, a los Enlaces de redstone avanzados con ese código");
        add(m + "link_code", "Link code", "Código de enlace");
        add(m + "screen.link_code_hint", "Four letters or digits; Advanced Redstone Links with this code receive the signal",
                "Cuatro letras o cifras; los Enlaces de redstone avanzados con este código reciben la señal");
        add(m + "screen.link_code_off", "Only with the Advanced Redstone Link output", "Solo con la salida por Enlace de redstone avanzado");
    }

    /** The Servo Gearbox: its name, the settings on its sides and its card. */
    private static void servo() {
        block("servo_gearbox", "Servo Gearbox", "Caja servo");
        String s = "create_nuclearindustry.servo.";
        add(s + "mode", "Stronger signal", "Con más señal");
        add(s + "mode.extend_with_signal", "Extends further", "Extiende más");
        add(s + "mode.retract_with_signal", "Retracts further", "Recoge más");
        add(s + "stroke", "Stroke length", "Longitud de carrera");
        String p = "create_nuclearindustry.panel.servo.";
        add(p + "kicker", "Kinetics", "Cinética");
        add(p + "status.no_rotation", "No rotation", "Sin giro");
        add(p + "status.no_actuator", "No piston or pulley", "Sin pistón ni polea");
        add(p + "status.holding", "Holding", "En posición");
        add(p + "status.extending", "Extending", "Extendiendo");
        add(p + "status.retracting", "Retracting", "Recogiendo");
        add(p + "status.stalled", "Blocked", "Bloqueado");
        add(p + "position", "Position", "Posición");
        add(p + "actuators", "Actuators", "Actuadores");
        add(p + "stroke", "Stroke", "Carrera");
        add(p + "stroke_reach", "%s blocks (reaches %s)", "%s bloques (llega a %s)");
        add(p + "hint.sides", "Scroll its sides: mode and stroke", "Rueda en sus lados: modo y carrera");
        add(p + "footer", "Comparator reads %s", "Un comparador lee %s");
    }

    /** The panel on the right of the screen (see client.panel.PanelHud): control room cards, shared units and hints. */
    private static void panel() {
        String p = "create_nuclearindustry.panel.";
        add(p + "status.online", "Online", "En línea");
        add(p + "status.unlinked", "No link", "Sin enlace");
        add(p + "status.linked", "Linked", "Enlazada");
        add(p + "status.wrong_target", "Not for this target", "No sirve para este objetivo");
        add(p + "status.no_code", "No code", "Sin código");
        add(p + "status.transmitting", "Transmitting", "Transmitiendo");
        add(p + "status.receiving", "Receiving", "Recibiendo");
        add(p + "status.idle", "Idle", "En reposo");
        add(p + "slot", "Slot %s", "Hueco %s");
        add(p + "reading", "Reading", "Lectura");
        add(p + "scale", "Scale", "Escala");
        add(p + "mode", "Mode", "Modo");
        add(p + "demand", "Asks for", "Pide");
        add(p + "setpoint", "Setpoint", "Consigna");
        add(p + "rods_in", "Rods in", "Barras dentro");
        add(p + "threshold", "Threshold", "Umbral");
        add(p + "level", "Level", "Nivel");
        add(p + "alarm", "Alarm", "Alarma");
        add(p + "on", "ON", "ENC.");
        add(p + "off", "OFF", "APAG.");
        add(p + "redstone", "Redstone output", "Salida de redstone");
        add(p + "output", "Output", "Salida");
        add(p + "signal", "Signal", "Señal");
        add(p + "console", "Console", "Consola");
        add(p + "target", "Target", "Objetivo");
        add(p + "console_blocks", "Console blocks", "Bloques de la consola");
        add(p + "modules", "Modules here", "Módulos aquí");
        add(p + "slots_used", "Slots used", "Huecos ocupados");
        add(p + "hint.link", "Link it with a Control Room Linker", "Enlázala con un Enlazador de sala de control");
        add(p + "hint.module", "Look at a module for its details", "Mira un módulo para ver sus detalles");
        add(p + "transmitter", "Transmitter", "Transmisor");
        add(p + "receiver", "Receiver", "Receptor");
        add(p + "code", "Code", "Código");
        add(p + "range", "Range", "Alcance");
        add(p + "blocks", "%s blocks", "%s bloques");
        add(p + "hint.code", "Click: type the code", "Clic: escribir el código");
        add(p + "hint.to_receiver", "Sneak-click: switch to receiver", "Clic agachado: pasar a receptor");
        add(p + "hint.to_transmitter", "Sneak-click: switch to transmitter", "Clic agachado: pasar a transmisor");
        add(p + "footer.linked", "Link %s %s %s", "Enlace %s %s %s");
        add(p + "footer.unlinked", "No link", "Sin enlace");
        add(p + "footer.console", "console of %s", "consola de %s");
        add(p + "status.not_formed", "Not formed", "Sin formar");
        add(p + "hint.sneak", "Sneak for details", "Agáchate para ver los detalles");
        add(p + "hint.release", "Details · stand up for the summary", "Detalles · levántate para el resumen");
        add(p + "unit.percent", "%s%%", "%s%%");
        add(p + "unit.pcm", "%s pcm", "%s pcm");
        add(p + "unit.celsius", "%s °C", "%s °C");
        add(p + "unit.kelvin", "%s K", "%s K");
        add(p + "unit.mw", "%s MW", "%s MW");
        add(p + "unit.bar", "%s bar", "%s bar");
        add(p + "unit.mb", "%s mB", "%s mB");
        add(p + "unit.mbt", "%s mB/t", "%s mB/t");
        add(p + "unit.of", "%s / %s", "%s / %s");
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
        add(s + "core_damage", "FUEL DAMAGE", "DAÑO COMBUSTIBLE");
        add(metric + "core_damage", "Fuel damage above", "Daño del combustible por encima de");
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

    /**
     * The setup screen of a desk module: header, captions, what each function does, what it needs, the
     * setting input and the output, frequency and signal hints.
     */
    private static void moduleScreen() {
        String m = "create_nuclearindustry.control_module.";
        String s = m + "screen.";
        add(s + "console", "Console: %s", "Consola: %s");
        add(s + "unlinked", "Not linked: every function is listed", "Sin enlazar: se muestran todas las funciones");
        add(s + "works", "Works on this console", "Funciona con esta consola");
        add(s + "warning.machine", "Does nothing here: needs %s", "Aquí no hace nada: necesita %s");
        add(s + "warning.pressurizer", "Does nothing here: the reactor has no pressurizer",
                "Aquí no hace nada: el reactor no tiene presionador");
        add(s + "warning.running", "Does nothing here: needs a reactor, turbine, tower or tokamak",
                "Aquí no hace nada: necesita un reactor, turbina, torre o tokamak");
        add(s + "needs", "Needs: %s", "Necesita: %s");
        add(s + "needs_pressurizer", "Needs a reactor with a pressurizer", "Necesita un reactor con presionador");
        add(s + "needs_machine", "Needs a reactor, turbine, cooling tower or tokamak",
                "Necesita un reactor, turbina, torre de refrigeración o tokamak");
        add(s + "functions", "Function", "Función");
        add(s + "metrics", "Alarm", "Alarma");
        add(s + "readings", "Reading", "Lectura");
        add(s + "controller", "Controller", "Controlador");
        add(s + "about", "What it does", "Qué hace");
        add(s + "no_metrics", "No alarms for this machine", "No hay alarmas para esta máquina");
        add(s + "no_readings", "No readings for this machine", "No hay lecturas para esta máquina");
        add(s + "flag_on", "ON", "SÍ");
        add(s + "flag_off", "off", "no");
        add(s + "threshold", "Threshold", "Umbral");
        add(s + "setpoint", "Setpoint", "Consigna");
        add(s + "level", "Level", "Nivel");
        add(s + "no_setting", "No setting", "Sin ajuste");
        add(s + "scroll", "Scroll to change it", "Usa la rueda para cambiarlo");
        add(s + "scroll_fine", "Hold Shift for steps of 1", "Mantén Mayús para pasos de 1");
        add(s + "mode_hint", "Click to switch between automatic and standby", "Haz clic para cambiar entre automático y en espera");
        add(s + "output_hint.none", "The module keeps its signal to itself", "El módulo no saca su señal");
        add(s + "output_hint.below", "Powers the block below the desk and the one behind it",
                "Alimenta el bloque bajo el pupitre y el de detrás");
        add(s + "output_hint.link", "Sends its signal on a Redstone Link frequency: set it in the two slots on the left",
                "Manda su señal por una frecuencia de Redstone Link: fíjala en los dos huecos de la izquierda");
        add(s + "frequency_hint", "Click with an item to set it, with an empty hand to clear it",
                "Haz clic con un objeto para fijarla, con la mano vacía para borrarla");
        add(s + "frequency_off", "Choose the Redstone Link output to use it", "Elige la salida por Redstone Link para usarla");
        add(s + "signal", "Redstone signal: %s", "Señal de redstone: %s");
        add(s + "signal_hint", "What the module puts out now, 0-15", "Lo que saca ahora el módulo, de 0 a 15");
        String f = m + "function.";
        add(f + "scram.desc", "Shuts the reactor down at once (SCRAM); press it again to reset.",
                "Para el reactor de golpe (SCRAM); púlsalo otra vez para rearmarlo.");
        add(f + "pulse.desc", "Sends a short redstone pulse.", "Manda un pulso corto de redstone.");
        add(f + "auto_scram.desc", "Turns the reactor's automatic SCRAM on or off.",
                "Activa o desactiva el SCRAM automático del reactor.");
        add(f + "tokamak_fuel.desc", "Turns the tokamak on or off: up starts it, down shuts it down safely.",
                "Enciende o apaga el tokamak: arriba lo arranca, abajo lo para de forma segura.");
        add(f + "toggle.desc", "Switches a redstone output on or off.", "Enciende o apaga una salida de redstone.");
        add(f + "rod_target.desc", "Sets the control rod target, 0-100%. Click to raise it, sneak-click to lower it.",
                "Fija el objetivo de las barras de control, 0-100 %. Clic para subirlo, agachado para bajarlo.");
        add(f + "scram_threshold.desc", "Sets the core temperature at which the automatic SCRAM trips.",
                "Fija la temperatura del núcleo a la que salta el SCRAM automático.");
        add(f + "signal_level.desc", "Puts out a redstone level from 0 to 15, set here or by clicking the dial.",
                "Saca un nivel de redstone de 0 a 15, fijado aquí o con clics en la rueda.");
        add(f + "lamp_scram.desc", "Lights while the reactor's SCRAM signal is on.",
                "Se enciende mientras la señal de SCRAM del reactor está activa.");
        add(f + "lamp_running.desc", "Lights while the machine runs: chain reaction, turbine or tower working, plasma burning.",
                "Se enciende mientras la máquina funciona: reacción en cadena, turbina o torre trabajando, plasma ardiendo.");
        add(f + "lamp_no_signal.desc", "Lights while the console gets no signal from its machine.",
                "Se enciende mientras la consola no recibe señal de su máquina.");
        add(f + "lamp_alarm.desc", "Lights while the chosen alarm passes its threshold.",
                "Se enciende mientras la alarma elegida pasa de su umbral.");
        add(f + "reading.desc", "A needle showing the chosen reading, full scale at the top.",
                "Una aguja que muestra la lectura elegida, con el fondo de escala arriba.");
        add(f + "auto_power.desc", "Moves the rod target to hold the reactor's power at the setpoint, in % of its rated power.",
                "Mueve el objetivo de las barras para mantener la potencia del reactor en la consigna, en % de la nominal.");
        add(f + "auto_temperature.desc", "Moves the rod target to hold the core temperature at the setpoint.",
                "Mueve el objetivo de las barras para mantener la temperatura del núcleo en la consigna.");
        add(f + "pzr_heaters.desc", "Runs every pressurizer heater as a backup, whatever its own mode.",
                "Enciende todos los calentadores del presionador como respaldo, sea cual sea su modo.");
        add(f + "pzr_spray.desc", "Opens the pressurizer spray fully, as long as the cold leg flows.",
                "Abre del todo el rociado del presionador, mientras circule la rama fría.");
        add(f + "auto_pressure.desc", "Runs the pressurizer heaters and spray to hold the primary pressure at the setpoint.",
                "Maneja los calentadores y el rociado del presionador para mantener la presión del primario en la consigna.");
        add(f + "lamp_relief_open.desc", "Lights while the pressurizer's relief valve is open.",
                "Se enciende mientras la válvula de alivio del presionador está abierta.");
        add(f + "tokamak_start.desc", "Starts the tokamak's startup sequence; if it refuses, it says why.",
                "Inicia la secuencia de arranque del tokamak; si se niega, dice por qué.");
        add(f + "tokamak_shutdown.desc", "Shuts the tokamak down safely.", "Para el tokamak de forma segura.");
        add(f + "tokamak_heating.desc", "Turns the tokamak's auxiliary heating on or off, at the level it was set to.",
                "Enciende o apaga el calentamiento auxiliar del tokamak, al nivel que tenía.");
        add(f + "auto_fusion_power.desc", "Sets the tokamak's fuelling, then its heating, to hold the fusion power at the setpoint.",
                "Ajusta el combustible del tokamak, y después su calentamiento, para mantener la potencia de fusión en la consigna.");
    }
}
