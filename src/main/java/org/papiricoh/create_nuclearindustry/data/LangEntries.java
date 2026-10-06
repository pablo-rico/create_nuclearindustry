package org.papiricoh.create_nuclearindustry.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for every translated string, so en_us and es_es can never drift apart.
 * Entries are emitted by {@link NuclearLangProvider}.
 */
public final class LangEntries {

    public record Entry(String key, String english, String spanish) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        content();
        materials();
        jei();
        ponder();
    }

    private LangEntries() {}

    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    static void add(String key, String english, String spanish) {
        ENTRIES.add(new Entry(key, english, spanish));
    }


    private static void content() {
        add("item.create_nuclearindustry.raw_uranium",
                "Raw Uranium",
                "Uranio en bruto");
        add("item.create_nuclearindustry.uranium",
                "Uranium",
                "Uranio");
        add("item.create_nuclearindustry.uranium.enrichment",
                "Enrichment: %s%%",
                "Enriquecimiento: %s%%");
        add("item.create_nuclearindustry.uranium_reactor_fuel",
                "Uranium Reactor Fuel",
                "Combustible de uranio para reactor");
        add("item.create_nuclearindustry.depleted_uranium_reactor_fuel",
                "Depleted Uranium Reactor Fuel",
                "Combustible de uranio gastado");
        add("item.create_nuclearindustry.enriched_uranium_blend",
                "Enriched Uranium Blend",
                "Mezcla de uranio enriquecido");
        add("item.create_nuclearindustry.weapons_grade_uranium_core",
                "Weapons-Grade Uranium Core",
                "Núcleo de uranio militar");
        add("item.create_nuclearindustry.borax_salt",
                "Borax Salt",
                "Sal de bórax");
        add("item.create_nuclearindustry.boron",
                "Boron",
                "Boro");
        add("block.create_nuclearindustry.uranium_ore",
                "Uranium Ore",
                "Mena de uranio");
        add("block.create_nuclearindustry.control_rod",
                "Control Rod",
                "Barra de control");
        add("block.create_nuclearindustry.uranium_rod",
                "Uranium Rod",
                "Barra de uranio");
        add("block.create_nuclearindustry.heat_exchanger",
                "Heat Exchanger",
                "Intercambiador de calor");
        add("block.create_nuclearindustry.reactor_casing",
                "Reactor Casing",
                "Carcasa de reactor");
        add("block.create_nuclearindustry.reactor_controller",
                "Reactor Controller",
                "Controlador de reactor");
        add("block.create_nuclearindustry.reactor_fluid_port",
                "Reactor Fluid Port",
                "Puerto de fluido de reactor");
        add("block.create_nuclearindustry.reactor_fuel_port",
                "Reactor Fuel Port",
                "Puerto de combustible de reactor");
        add("block.create_nuclearindustry.reactor_temperature_sensor",
                "Temperature Sensor",
                "Sensor de temperatura");
        add("block.create_nuclearindustry.dual_fluid_pipe",
                "Dual Fluid Pipe",
                "Tubería dual de fluidos");
        add("block.create_nuclearindustry.centrifuge",
                "Centrifuge",
                "Centrifugadora");
        add("block.create_nuclearindustry.turbine_casing",
                "Turbine Casing",
                "Carcasa de turbina");
        add("block.create_nuclearindustry.turbine_rotor",
                "Turbine Rotor",
                "Rotor de turbina");
        add("block.create_nuclearindustry.turbine_output",
                "Turbine Output",
                "Salida de turbina");
        add("block.create_nuclearindustry.turbine_fluid_port",
                "Turbine Fluid Port",
                "Puerto de fluido de turbina");
        add("block.create_nuclearindustry.nuclear_bomb",
                "Nuclear Bomb",
                "Bomba nuclear");
        add("block.create_nuclearindustry.launch_pad",
                "Missile Launch Pad",
                "Plataforma de lanzamiento");
        add("block.create_nuclearindustry.nuclear_cannon_shell",
                "Nuclear Cannon Shell",
                "Proyectil nuclear de cañón");
        add("item.create_nuclearindustry.nuclear_cannon_shell",
                "Nuclear Cannon Shell",
                "Proyectil nuclear de cañón");
        add("item.create_nuclearindustry.missile",
                "Nuclear Missile",
                "Misil nuclear");
        add("item.create_nuclearindustry.target_designator",
                "Target Designator",
                "Designador de objetivo");
        add("item.create_nuclearindustry.target_designator.set",
                "Target set to %s, %s, %s",
                "Objetivo fijado en %s, %s, %s");
        add("item.create_nuclearindustry.target_designator.target",
                "Target: %s, %s, %s",
                "Objetivo: %s, %s, %s");
        add("item.create_nuclearindustry.target_designator.empty",
                "No target set (right-click a block)",
                "Sin objetivo (click derecho en un bloque)");
        add("entity.create_nuclearindustry.missile",
                "Nuclear Missile",
                "Misil nuclear");
        add("subtitle.create_nuclearindustry.nuclear_explosion",
                "Nuclear explosion",
                "Explosión nuclear");
        add("block.create_nuclearindustry.borax_ore",
                "Borax Ore",
                "Mena de bórax");
        add("fluid.create_nuclearindustry.steam",
                "Steam",
                "Vapor");
        add("fluid.create_nuclearindustry.heavy_water",
                "Heavy Water",
                "Agua pesada");
        add("fluid.create_nuclearindustry.heavy_steam",
                "Heavy Steam",
                "Vapor pesado");
        add("create_nuclearindustry.display_source.reactor_temperature_sensor",
                "Reactor Sensor",
                "Sensor de reactor");
        add("itemGroup.create_nuclearindustry.nuclear_tab",
                "Create: Nuclear industry",
                "Create: Nuclear industry");
        add("block.create_nuclearindustry.fusion_controller",
                "Fusion Reactor Core",
                "Núcleo del Reactor de Fusión");
        add("block.create_nuclearindustry.fusion_accelerator_segment",
                "Accelerator Segment",
                "Segmento Acelerador");
        add("block.create_nuclearindustry.fusion_accelerator_corner",
                "Accelerator Corner",
                "Esquina Aceleradora");
        add("block.create_nuclearindustry.fusion_electromagnet",
                "Confinement Electromagnet",
                "Electroimán de Confinamiento");
        add("block.create_nuclearindustry.fusion_cryostat_casing",
                "Cryostat Casing",
                "Carcasa Criostato");
        add("block.create_nuclearindustry.fusion_magnet_input",
                "Confinement Magnet Drive",
                "Toma de Imán de Confinamiento");
        add("block.create_nuclearindustry.fusion_fluid_port",
                "Fusion Fluid Port",
                "Puerto de Fluido de Fusión");
        add("block.create_nuclearindustry.fusion_fuel_injector",
                "Fusion Fuel Injector",
                "Inyector de Combustible de Fusión");
        add("block.create_nuclearindustry.fusion_turbine_casing",
                "Fusion Turbine Casing",
                "Carcasa de Turbina de Fusión");
        add("block.create_nuclearindustry.fusion_turbine_rotor",
                "Fusion Turbine Rotor",
                "Rotor de Turbina de Fusión");
        add("block.create_nuclearindustry.fusion_turbine_fluid_port",
                "Fusion Turbine Fluid Port",
                "Puerto de Turbina de Fusión");
        add("block.create_nuclearindustry.fusion_plasma_turbine",
                "Plasma Turbine",
                "Turbina de Plasma");
        add("item.create_nuclearindustry.lithium",
                "Lithium",
                "Litio");
        add("item.create_nuclearindustry.lithium_6",
                "Lithium-6",
                "Litio-6");
        add("item.create_nuclearindustry.beryllium",
                "Beryllium",
                "Berilio");
        add("item.create_nuclearindustry.superconductor_ingot",
                "Superconductor Ingot",
                "Lingote Superconductor");
        add("item.create_nuclearindustry.lithium_6_breeder_assembly",
                "Lithium-6 Breeder Assembly",
                "Ensamblaje Criador de Litio-6");
        add("item.create_nuclearindustry.deuterium_cell",
                "Deuterium Cell",
                "Célula de Deuterio");
        add("item.create_nuclearindustry.tritium_cell",
                "Tritium Cell",
                "Célula de Tritio");
        add("item.create_nuclearindustry.dt_fuel_pellet",
                "D-T Fuel Pellet",
                "Pellet de Combustible D-T");
        add("item.create_nuclearindustry.spent_dt_pellet",
                "Spent D-T Pellet",
                "Pellet D-T Gastado");
        add("fluid.create_nuclearindustry.deuterium",
                "Deuterium",
                "Deuterio");
        add("fluid.create_nuclearindustry.tritium",
                "Tritium",
                "Tritio");
        add("fluid.create_nuclearindustry.plasma_steam",
                "Plasma Steam",
                "Vapor de Plasma");
    }

    private static void materials() {
        item("crushed_raw_uranium", "Crushed Raw Uranium", "Uranio en bruto triturado");
        item("uranium_concentrate", "Uranium Concentrate", "Concentrado de uranio");
        item("yellowcake", "Yellowcake", "Torta amarilla");
        item("uranium_dioxide", "Uranium Dioxide", "Dióxido de uranio");
        item("uranium_fuel_pellet", "Uranium Fuel Pellet", "Pastilla de combustible de uranio");
        item("incomplete_uranium_reactor_fuel", "Incomplete Uranium Reactor Fuel", "Combustible de uranio incompleto");
        item("radioactive_waste", "Radioactive Waste", "Residuos radiactivos");
        item("fluorite", "Fluorite", "Fluorita");
        item("raw_spodumene", "Raw Spodumene", "Espodumena en bruto");
        item("crushed_spodumene", "Crushed Spodumene", "Espodumena triturada");
        item("spodumene_concentrate", "Spodumene Concentrate", "Concentrado de espodumena");
        item("lithium_carbonate", "Lithium Carbonate", "Carbonato de litio");
        item("empty_gas_cell", "Empty Gas Cell", "Célula de gas vacía");
        block("spodumene_ore", "Spodumene Ore", "Mena de espodumena");
        block("electrolyzer", "Electrolyzer", "Electrolizador");
        block("radioactive_waste_barrel", "Radioactive Waste Barrel", "Barril de residuos radiactivos");

        add("gui.create_nuclearindustry.machine.input", "Input", "Entrada");
        add("gui.create_nuclearindustry.machine.output", "Output", "Salida");
        add("gui.create_nuclearindustry.machine.feed", "Feed", "Alimentación");
        add("gui.create_nuclearindustry.machine.product", "Product", "Producto");
        add("gui.create_nuclearindustry.machine.tails", "Tails", "Colas");
        add("gui.create_nuclearindustry.machine.status.too_slow", "Needs at least %s RPM", "Necesita al menos %s RPM");
        add("gui.create_nuclearindustry.machine.status.overstressed", "Overstressed", "Sobrecargada");
        add("gui.create_nuclearindustry.machine.status.output_full", "Output full", "Salida llena");
        add("gui.create_nuclearindustry.machine.status.idle", "Idle", "En espera");
        add("gui.create_nuclearindustry.machine.status.working", "Working", "Procesando");

        add("gui.create_nuclearindustry.reactor.title", "Reactor Control Panel", "Panel de control del reactor");
        add("gui.create_nuclearindustry.reactor.missing", "Reactor not found", "Reactor no encontrado");
        add("gui.create_nuclearindustry.reactor.unformed", "Reactor not formed", "Reactor sin formar");
        add("gui.create_nuclearindustry.reactor.temperature", "Temperature: %s °C", "Temperatura: %s °C");
        add("gui.create_nuclearindustry.reactor.neutrons", "Neutrons: %s / %s", "Neutrones: %s / %s");
        add("gui.create_nuclearindustry.reactor.fuel", "Fuel: %s / %s units (%s%%)", "Combustible: %s / %s unidades (%s%%)");
        add("gui.create_nuclearindustry.reactor.power", "Power: %s MW  Steam: %s mB/t", "Potencia: %s MW  Vapor: %s mB/t");
        add("gui.create_nuclearindustry.reactor.state", "State: %s", "Estado: %s");
        add("gui.create_nuclearindustry.reactor.stress", "Thermal stress: %s%%", "Estrés térmico: %s%%");
        add("gui.create_nuclearindustry.reactor.excursion", "EXCURSION", "EXCURSIÓN");
        add("gui.create_nuclearindustry.reactor.rods", "Rods: %s/%s inserted (%s%%)", "Barras: %s/%s insertadas (%s%%)");
        add("gui.create_nuclearindustry.reactor.scram_engaged", "Manual SCRAM: all rods held in", "SCRAM manual: todas las barras dentro");
        add("gui.create_nuclearindustry.reactor.fuel_in", "Fuel in", "Combustible");
        add("gui.create_nuclearindustry.reactor.spent", "Spent", "Gastado");
        add("gui.create_nuclearindustry.reactor.scram", "SCRAM", "SCRAM");
        add("gui.create_nuclearindustry.reactor.reset", "Reset", "Rearmar");
        add("gui.create_nuclearindustry.reactor.scram.tooltip", "Emergency shutdown: holds every control rod fully inserted until reset.",
                "Parada de emergencia: mantiene todas las barras de control insertadas hasta rearmar.");

        add("jei.create_nuclearindustry.category.electrolysis", "Electrolysis", "Electrólisis");
        add("jei.create_nuclearindustry.chance", "%s%% chance", "%s%% de probabilidad");
        add("jei.create_nuclearindustry.product", "Product: front face", "Producto: cara frontal");
        add("jei.create_nuclearindustry.tails", "Tails: bottom face", "Colas: cara inferior");
        add("jei.create_nuclearindustry.processing_time", "%ss at %s RPM, faster when spun faster", "%ss a %s RPM, más rápido a más velocidad");

        fluid("sulfuric_acid", "Sulfuric Acid", "Ácido sulfúrico");
        fluid("hydrofluoric_acid", "Hydrofluoric Acid", "Ácido fluorhídrico");
        fluid("uranium_hexafluoride", "Uranium Hexafluoride", "Hexafluoruro de uranio");
        fluid("depleted_uranium_hexafluoride", "Depleted Uranium Hexafluoride", "Hexafluoruro de uranio empobrecido");
        fluid("low_enriched_uranium_hexafluoride", "Low-Enriched Uranium Hexafluoride", "Hexafluoruro de uranio poco enriquecido");
        fluid("high_enriched_uranium_hexafluoride", "High-Enriched Uranium Hexafluoride", "Hexafluoruro de uranio altamente enriquecido");
        fluid("weapons_grade_uranium_hexafluoride", "Weapons-Grade Uranium Hexafluoride", "Hexafluoruro de uranio de grado militar");
    }

    private static void item(String id, String english, String spanish) {
        add("item.create_nuclearindustry." + id, english, spanish);
    }

    private static void block(String id, String english, String spanish) {
        add("block.create_nuclearindustry." + id, english, spanish);
    }

    private static void fluid(String id, String english, String spanish) {
        add("fluid.create_nuclearindustry." + id, english, spanish);
    }

    private static void jei() {
        add("jei.create_nuclearindustry.category.centrifuging",
                "Centrifuging",
                "Centrifugado");
    }

    private static void ponder() {
        add("create_nuclearindustry.ponder.centrifuge.header",
                "Processing uranium with a Centrifuge",
                "Procesando uranio con una centrifugadora");
        add("create_nuclearindustry.ponder.centrifuge.text_1",
                "The Centrifuge is a kinetic machine for refining uranium.",
                "La centrifugadora es una maquina cinetica para refinar uranio.");
        add("create_nuclearindustry.ponder.centrifuge.text_2",
                "It accepts rotational power through the rear shaft.",
                "Acepta potencia rotacional por el eje trasero.");
        add("create_nuclearindustry.ponder.centrifuge.text_3",
                "Right-click with Raw Uranium or Uranium to insert one item.",
                "Haz clic derecho con uranio bruto o uranio para insertar un objeto.");
        add("create_nuclearindustry.ponder.centrifuge.text_4",
                "At 16 RPM or faster, uranium is enriched continuously until it reaches the target set on the scroll dial.",
                "A 16 RPM o mas, el uranio se enriquece de forma continua hasta alcanzar el objetivo fijado en el dial.");
        add("create_nuclearindustry.ponder.centrifuge.text_5",
                "Extract finished uranium from the bottom, or right-click with an empty hand.",
                "Extrae el uranio terminado por abajo, o con clic derecho usando la mano vacia.");
        add("create_nuclearindustry.ponder.reactor_controller.header",
                "Assembling a Nuclear Reactor",
                "Ensamblando un reactor nuclear");
        add("create_nuclearindustry.ponder.reactor_controller.text_1",
                "The reactor is a multiblock: raise a hollow shell of Reactor Casing.",
                "El reactor es un multibloque: levanta una carcasa hueca de Carcasa de Reactor.");
        add("create_nuclearindustry.ponder.reactor_controller.text_2",
                "The width must be odd (3-9) and the height 4-12.",
                "La anchura debe ser impar (3-9) y la altura 4-12.");
        add("create_nuclearindustry.ponder.reactor_controller.text_3",
                "Inside, raise columns of Uranium Rods: the fuel.",
                "Dentro, levanta columnas de Barras de Uranio: el combustible.");
        add("create_nuclearindustry.ponder.reactor_controller.text_4",
                "A central column of Heat Exchangers carries the heat into the coolant.",
                "Una columna central de Intercambiadores de Calor lleva el calor al refrigerante.");
        add("create_nuclearindustry.ponder.reactor_controller.text_5",
                "Control Rods on the corners absorb neutrons: they are the reactor's brakes.",
                "Las Barras de Control de las esquinas absorben neutrones: son los frenos del reactor.");
        add("create_nuclearindustry.ponder.reactor_controller.text_6",
                "Swap wall casing for a Reactor Fluid Port set to INPUT for coolant...",
                "Sustituye carcasa de la pared por un Puerto de Fluido en modo ENTRADA para el refrigerante...");
        add("create_nuclearindustry.ponder.reactor_controller.text_7",
                "...and another set to OUTPUT for steam.",
                "...y otro en modo SALIDA para el vapor.");
        add("create_nuclearindustry.ponder.reactor_controller.text_8",
                "A Reactor Fuel Port loads uranium fuel and ejects depleted fuel.",
                "Un Puerto de Combustible carga uranio y expulsa el combustible gastado.");
        add("create_nuclearindustry.ponder.reactor_controller.text_9",
                "Cap the reactor and place the Reactor Controller at the centre of the top face.",
                "Corona el reactor y coloca el Controlador de Reactor en el centro de la cara superior.");
        add("create_nuclearindustry.ponder.reactor_controller.text_10",
                "Leave the casing OPEN above every Control Rod column: rods are moved from up here.",
                "Deja ABIERTA la carcasa sobre cada columna de Barras de Control: las barras se mueven desde arriba.");
        add("create_nuclearindustry.ponder.fusion_controller.header",
                "Assembling a Fusion Reactor",
                "Ensamblando un reactor de fusion");
        add("create_nuclearindustry.ponder.fusion_controller.text_1",
                "Place the Fusion Reactor Core at the centre.",
                "Coloca el Nucleo del Reactor de Fusion en el centro.");
        add("create_nuclearindustry.ponder.fusion_controller.text_2",
                "Use Accelerator Corners on all four corners.",
                "Usa Esquinas Aceleradoras en las cuatro esquinas.");
        add("create_nuclearindustry.ponder.fusion_controller.text_3",
                "Build an odd square ring: 5x5 to 11x11.",
                "Construye un anillo cuadrado impar: 5x5 a 11x11.");
        add("create_nuclearindustry.ponder.fusion_controller.text_4",
                "Fill the chamber between ring and core with Cryostat Casing.",
                "Rellena la camara entre el anillo y el nucleo con Carcasa de Criostato.");
        add("create_nuclearindustry.ponder.fusion_controller.text_5",
                "More Electromagnets improve field stability.",
                "Mas Electroimanes mejoran la estabilidad del campo.");
        add("create_nuclearindustry.ponder.fusion_controller.text_6",
                "Power a Magnet Drive with Create rotation.",
                "Alimenta una Toma de Iman con rotacion de Create.");
        add("create_nuclearindustry.ponder.fusion_controller.text_7",
                "Add an INPUT Fusion Fluid Port for coolant.",
                "Anade un Puerto de Fluido en ENTRADA para refrigerante.");
        add("create_nuclearindustry.ponder.fusion_controller.text_8",
                "Add an OUTPUT Fusion Fluid Port for plasma steam.",
                "Anade otro en SALIDA para vapor de plasma.");
        add("create_nuclearindustry.ponder.fusion_controller.text_9",
                "The Fuel Injector loads D-T Fuel Pellets.",
                "El Inyector carga Pellets de Combustible D-T.");
        add("create_nuclearindustry.ponder.fusion_controller.text_10",
                "Right-click the core to form the reactor.",
                "Haz clic derecho en el nucleo para formar el reactor.");
        add("create_nuclearindustry.ponder.turbine.header",
                "Assembling a Steam Turbine",
                "Ensamblando una turbina de vapor");
        add("create_nuclearindustry.ponder.turbine.text_1",
                "The Steam Turbine turns steam into rotational power. Start with a Turbine Casing.",
                "La turbina de vapor convierte el vapor en potencia rotacional. Empieza con una Carcasa de Turbina.");
        add("create_nuclearindustry.ponder.turbine.text_2",
                "Place a Turbine Rotor in front of the casing, its axis along the line.",
                "Coloca un Rotor de Turbina delante de la carcasa, con su eje en linea.");
        add("create_nuclearindustry.ponder.turbine.text_3",
                "Cap the line with a Turbine Output facing outward: it generates the rotation for your machines.",
                "Remata la linea con una Salida de Turbina mirando hacia afuera: genera la rotacion para tus maquinas.");
        add("create_nuclearindustry.ponder.turbine.text_4",
                "Feed steam through a Turbine Fluid Port set to INPUT...",
                "Alimenta vapor por un Puerto de Fluido de Turbina en modo ENTRADA...");
        add("create_nuclearindustry.ponder.turbine.text_5",
                "...and drain the condensate through another set to OUTPUT.",
                "...y drena el condensado por otro en modo SALIDA.");
        add("create_nuclearindustry.ponder.turbine.text_6",
                "With steam flowing, the turbine forms on its own and the output shaft powers your contraptions.",
                "Con el vapor fluyendo, la turbina se forma sola y el eje de salida impulsa tus mecanismos.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.header",
                "Generating power with a Plasma Turbine",
                "Generando potencia con una turbina de plasma");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_1",
                "The Plasma Turbine is a multiblock: start with a Fusion Turbine Casing.",
                "La Turbina de Plasma es un multibloque: empieza con una Carcasa de Turbina de Fusion.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_2",
                "Place a Fusion Turbine Rotor in line with the casing.",
                "Coloca un Rotor de Turbina de Fusion en linea con la carcasa.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_3",
                "Cap the line with the Plasma Turbine, facing outward toward the shaft.",
                "Remata la linea con la Turbina de Plasma mirando hacia el eje.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_4",
                "Feed plasma steam through a Fusion Turbine Fluid Port set to INPUT.",
                "Alimenta vapor de plasma por un Puerto de Turbina de Fusion en modo ENTRADA.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_5",
                "Drain condensate through another Fusion Turbine Fluid Port set to OUTPUT.",
                "Drena condensado por otro Puerto de Turbina de Fusion en modo SALIDA.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_6",
                "Each input/output pair can consume 160 mB/t and generate up to 4096 SU at 64 RPM.",
                "Cada par entrada/salida consume 160 mB/t y genera hasta 4096 SU a 64 RPM.");
        add("create_nuclearindustry.ponder.reactor_controller.text_11",
                "Right-click the controller to form the reactor.",
                "Haz clic derecho en el controlador para formar el reactor.");
        add("create_nuclearindustry.ponder.reactor_controller.text_12",
                "A Mechanical Piston above an open channel raises and lowers the Control Rods.",
                "Un Piston Mecanico sobre un canal abierto sube y baja las Barras de Control.");
        add("create_nuclearindustry.ponder.reactor_controller.text_13",
                "Super Glue the rods to each other and to the piston head so they move as one.",
                "Pega las barras entre si y al cabezal del piston con Super Glue para que se muevan como una sola.");
        add("create_nuclearindustry.ponder.reactor_controller.text_14",
                "Withdrawn rods absorb fewer neutrons: the chain reaction and the heat ramp up.",
                "Con las barras fuera se absorben menos neutrones: la reaccion en cadena y el calor aumentan.");
        add("create_nuclearindustry.ponder.reactor_controller.text_15",
                "Inserted rods soak up neutrons: the deeper they sit, the more the reaction is throttled. Fully inserted means shutdown.",
                "Las barras insertadas absorben neutrones: cuanto mas dentro esten, mas se frena la reaccion. Totalmente insertadas, el reactor se apaga.");
    }
}
