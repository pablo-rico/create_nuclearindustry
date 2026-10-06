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
                "Fuel Channel",
                "Canal de combustible");
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
                "Tokamak Controller",
                "Controlador del tokamak");
        add("block.create_nuclearindustry.fusion_accelerator_segment",
                "Tokamak Vacuum Vessel",
                "Cámara de vacío del tokamak");
        add("block.create_nuclearindustry.fusion_accelerator_corner",
                "Vacuum Vessel Corner (Legacy)",
                "Esquina de cámara de vacío (antigua)");
        add("block.create_nuclearindustry.fusion_electromagnet",
                "Toroidal Field Coil",
                "Bobina de campo toroidal");
        add("block.create_nuclearindustry.fusion_cryostat_casing",
                "Cryostat Casing",
                "Carcasa Criostato");
        add("block.create_nuclearindustry.fusion_magnet_input",
                "Magnet Drive",
                "Toma de imanes");
        add("block.create_nuclearindustry.fusion_fluid_port",
                "Tokamak Fluid Port",
                "Puerto de fluidos del tokamak");
        add("block.create_nuclearindustry.fusion_fuel_injector",
                "Tokamak Fuel Injector",
                "Inyector de combustible del tokamak");
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
                "D-T Fuel Pellet (Legacy)",
                "Pellet de combustible D-T (antiguo)");
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
        block("fusion_central_solenoid", "Central Solenoid", "Solenoide central");
        item("geiger_counter", "Geiger Counter", "Contador Geiger");
        add("item.create_nuclearindustry.geiger_counter.reading", "☢ %s rad/s   dose %s rad", "☢ %s rad/s   dosis %s rad");
        item("hazmat_helmet", "Hazmat Hood", "Capucha antirradiación");
        item("hazmat_chestplate", "Hazmat Suit", "Traje antirradiación");
        item("hazmat_leggings", "Hazmat Trousers", "Pantalones antirradiación");
        item("hazmat_boots", "Hazmat Boots", "Botas antirradiación");
        add("effect.create_nuclearindustry.radiation_sickness", "Radiation Sickness", "Síndrome de irradiación");
        add("death.attack.create_nuclearindustry.radiation", "%1$s died of radiation sickness", "%1$s murió por irradiación");
        add("death.attack.create_nuclearindustry.radiation.player", "%1$s died of radiation sickness while fighting %2$s",
                "%1$s murió por irradiación mientras luchaba contra %2$s");
        block("fusion_breeding_blanket", "Breeding Blanket", "Manto regenerador");

        add("create_nuclearindustry.display_source.tokamak", "Tokamak Status", "Estado del tokamak");
        add("gui.create_nuclearindustry.tokamak.title", "Tokamak Control", "Control del tokamak");
        add("gui.create_nuclearindustry.tokamak.unformed", "Tokamak not formed", "Tokamak sin formar");
        add("gui.create_nuclearindustry.tokamak.temperature", "Plasma: %s °", "Plasma: %s °");
        add("gui.create_nuclearindustry.tokamak.confinement", "Conf. %s (coils %s, drive %s)", "Conf. %s (bobinas %s, toma %s)");
        add("gui.create_nuclearindustry.tokamak.instability", "Instability: %s%%", "Inestabilidad: %s%%");
        add("gui.create_nuclearindustry.tokamak.power", "%s MW, %s", "%s MW, %s");
        add("gui.create_nuclearindustry.tokamak.mode.cold", "cold", "frío");
        add("gui.create_nuclearindustry.tokamak.mode.dt", "D-T burn", "fusión D-T");
        add("gui.create_nuclearindustry.tokamak.mode.dd", "D-D burn", "fusión D-D");
        add("gui.create_nuclearindustry.tokamak.burn", "Burn %s mB/t, TBR %s", "Quema %s mB/t, TBR %s");
        add("gui.create_nuclearindustry.tokamak.structure", "R=%s, %s coils, %s blankets", "R=%s, %s bobinas, %s mantos");
        add("gui.create_nuclearindustry.tokamak.suspended", "Paused: part of the torus is unloaded", "En pausa: parte del toro no está cargada");
        add("gui.create_nuclearindustry.tokamak.fuel_on", "Fuel: ON", "Combustible: SÍ");
        add("gui.create_nuclearindustry.tokamak.fuel_off", "Fuel: OFF", "Combustible: NO");
        add("gui.create_nuclearindustry.tokamak.fuel.tooltip",
                "Stops or resumes fuel injection. Without fuel the plasma cools down safely while the magnets hold it.",
                "Detiene o reanuda la inyección de combustible. Sin combustible el plasma se enfría con seguridad mientras los imanes lo sujetan.");

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

        add("jei.create_nuclearindustry.info.reactor_fuel",
                "Fuel assemblies are pressed from uranium dioxide pellets. Each Fuel Channel in a reactor holds one assembly. Richer fuel lasts longer and is more reactive; spent fuel keeps its enrichment and can be reprocessed.",
                "Los elementos combustibles se montan con pastillas de dióxido de uranio. Cada Canal de combustible de un reactor aloja uno. El combustible más enriquecido dura más y es más reactivo; el gastado conserva su enriquecimiento y se puede reprocesar.");
        add("jei.create_nuclearindustry.info.tokamak",
                "A ring-shaped fusion reactor around a Central Solenoid. Feed it deuterium and tritium, coolant and rotation for its Magnet Drives; it makes plasma steam for a Plasma Turbine. See its Ponder scene.",
                "Un reactor de fusión en forma de anillo alrededor de un Solenoide central. Necesita deuterio y tritio, refrigerante y rotación para sus Tomas de imanes; produce vapor de plasma para una Turbina de plasma. Consulta su escena de Ponder.");
        add("jei.create_nuclearindustry.info.radiation",
                "Enriched uranium, spent fuel and radioactive waste irradiate whoever carries them. A Geiger counter shows the dose; a full hazmat suit stops 95% of it.",
                "El uranio enriquecido, el combustible gastado y los residuos irradian a quien los lleva. Un contador Geiger muestra la dosis; un traje antirradiación completo detiene el 95%.");
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
        add("create_nuclearindustry.ponder.tokamak.header",
                "Building a Tokamak",
                "Construir un tokamak");
        add("create_nuclearindustry.ponder.tokamak.text_1",
                "A tokamak holds a ring of plasma with magnets. Start with a Central Solenoid column, five blocks tall, at its axis.",
                "Un tokamak sujeta un anillo de plasma con imanes. Empieza por una columna de Solenoide central, de cinco bloques de alto, en su eje.");
        add("create_nuclearindustry.ponder.tokamak.text_2",
                "The torus has a 5x5 cross-section. Its floor and roof can hold Breeding Blankets, which turn fusion neutrons into tritium.",
                "El toro tiene una sección de 5x5. Su suelo y su techo pueden llevar Mantos regeneradores, que convierten los neutrones de fusión en tritio.");
        add("create_nuclearindustry.ponder.tokamak.text_3",
                "Toroidal Field Coils line the walls. The 3x3 core of the ring must stay empty: it is the plasma chamber.",
                "Las Bobinas de campo toroidal forran las paredes. El núcleo de 3x3 del anillo debe quedar vacío: es la cámara de plasma.");
        add("create_nuclearindustry.ponder.tokamak.text_4",
                "The ring may be 4 to 16 blocks from the solenoid to the plasma. A bigger ring holds more plasma and makes more power.",
                "El anillo puede estar a entre 4 y 16 bloques del solenoide al plasma. Un anillo mayor contiene más plasma y da más potencia.");
        add("create_nuclearindustry.ponder.tokamak.text_5",
                "The Tokamak Controller sits on the outer equator. Right-click it to form the tokamak and open its control panel.",
                "El Controlador del tokamak va en el ecuador exterior. Haz clic derecho para formar el tokamak y abrir su panel de control.");
        add("create_nuclearindustry.ponder.tokamak.text_6",
                "A Fluid Port on INPUT takes water or heavy water as coolant...",
                "Un Puerto de fluidos en ENTRADA recibe agua o agua pesada como refrigerante...");
        add("create_nuclearindustry.ponder.tokamak.text_7",
                "...and one on OUTPUT gives plasma steam for a Plasma Turbine.",
                "...y otro en SALIDA entrega vapor de plasma para una Turbina de plasma.");
        add("create_nuclearindustry.ponder.tokamak.text_8",
                "The Fuel Injector takes deuterium and tritium, by pipe or by hand with gas cells. On deuterium alone the plasma runs slowly and breeds its first tritium.",
                "El Inyector de combustible recibe deuterio y tritio, por tubería o a mano con células de gas. Solo con deuterio el plasma funciona despacio y cría su primer tritio.");
        add("create_nuclearindustry.ponder.tokamak.text_9",
                "Magnet Drives turn rotation into the confining field: one for every 4 blocks of radius, at 64 RPM or more.",
                "Las Tomas de imanes convierten la rotación en el campo de confinamiento: una por cada 4 bloques de radio, a 64 RPM o más.");
        add("create_nuclearindustry.ponder.tokamak.text_10",
                "Keep the coolant flowing. A tokamak without coolant overheats and disrupts, blowing holes in its vessel.",
                "Mantén el refrigerante fluyendo. Un tokamak sin refrigerante se sobrecalienta y sufre una disrupción que agujerea su cámara.");
        add("create_nuclearindustry.ponder.centrifuge.header",
                "Enriching uranium with Centrifuges",
                "Enriquecer uranio con centrifugadoras");
        add("create_nuclearindustry.ponder.centrifuge.text_1",
                "The Centrifuge separates uranium hexafluoride by enrichment. It turns through its back, at 32 RPM or more.",
                "La centrifugadora separa el hexafluoruro de uranio por enriquecimiento. Gira por su parte trasera, a 32 RPM o más.");
        add("create_nuclearindustry.ponder.centrifuge.text_2",
                "Pipe UF6 into the top or the sides.",
                "Introduce el UF6 por arriba o por los lados.");
        add("create_nuclearindustry.ponder.centrifuge.text_3",
                "The enriched product leaves through the front...",
                "El producto enriquecido sale por el frente...");
        add("create_nuclearindustry.ponder.centrifuge.text_4",
                "...and the depleted tails through the bottom.",
                "...y las colas empobrecidas por abajo.");
        add("create_nuclearindustry.ponder.centrifuge.text_5",
                "Chain centrifuges into a cascade: natural to LEU, LEU to HEU, HEU to weapons grade. Pipe each stage's tails back into the stage before it.",
                "Encadena centrifugadoras en cascada: natural a LEU, LEU a HEU, HEU a grado militar. Devuelve las colas de cada etapa a la etapa anterior.");
        add("create_nuclearindustry.ponder.centrifuge.text_6",
                "It also separates Lithium-6 from lithium.",
                "También separa litio-6 del litio.");
        add("create_nuclearindustry.ponder.electrolyzer.header",
                "Electrolysis",
                "Electrólisis");
        add("create_nuclearindustry.ponder.electrolyzer.text_1",
                "The Electrolyzer uses rotation in place of electric current, at 32 RPM or more.",
                "El electrolizador usa la rotación en lugar de corriente eléctrica, a 32 RPM o más.");
        add("create_nuclearindustry.ponder.electrolyzer.text_2",
                "Fed water, it slowly concentrates heavy water. Heavy water then splits into deuterium gas.",
                "Con agua, concentra lentamente agua pesada. Luego el agua pesada se descompone en gas deuterio.");
        add("create_nuclearindustry.ponder.electrolyzer.text_3",
                "It also reduces lithium carbonate to lithium metal.",
                "También reduce el carbonato de litio a litio metálico.");
        add("create_nuclearindustry.ponder.electrolyzer.text_4",
                "Fluids leave through the front, items through the bottom.",
                "Los fluidos salen por el frente y los objetos por abajo.");
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
                "Inside, raise columns of Fuel Channels: each block holds one fuel assembly.",
                "Dentro, levanta columnas de Canales de combustible: cada bloque aloja un elemento combustible.");
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
                "A Reactor Fuel Port loads fuel assemblies and ejects spent fuel.",
                "Un Puerto de combustible carga elementos combustibles y expulsa el combustible gastado.");
        add("create_nuclearindustry.ponder.reactor_controller.text_9",
                "Cap the reactor and place the Reactor Controller at the centre of the top face.",
                "Corona el reactor y coloca el Controlador de Reactor en el centro de la cara superior.");
        add("create_nuclearindustry.ponder.reactor_controller.text_10",
                "Leave the casing OPEN above every Control Rod column: rods are moved from up here.",
                "Deja ABIERTA la carcasa sobre cada columna de Barras de Control: las barras se mueven desde arriba.");
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
