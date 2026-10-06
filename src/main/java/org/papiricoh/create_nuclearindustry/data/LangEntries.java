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
        commands();
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
        item("spent_fuel_fragments", "Spent Fuel Fragments", "Fragmentos de combustible gastado");
        item("uranyl_nitrate", "Uranyl Nitrate", "Nitrato de uranilo");
        item("plutonium_dioxide", "Plutonium Dioxide", "Dióxido de plutonio");
        item("plutonium", "Plutonium", "Plutonio");
        add("block.create_nuclearindustry.rtg", "Radioisotope Generator", "Generador de radioisótopos");
        item("plutonium_rtg_pellet", "Plutonium RTG Pellet", "Pastilla RTG de plutonio");
        item("waste_rtg_pellet", "Waste RTG Pellet", "Pastilla RTG de residuos");
        item("depleted_rtg_pellet", "Spent RTG Pellet", "Pastilla RTG agotada");
        add("item.create_nuclearindustry.rtg_pellet.charge", "Charge: %s%%", "Carga: %s%%");
        item("mox_fuel_pellet", "MOX Fuel Pellet", "Pastilla de combustible MOX");
        item("incomplete_mox_reactor_fuel", "Incomplete MOX Fuel Assembly", "Elemento combustible MOX incompleto");
        item("mox_reactor_fuel", "MOX Fuel Assembly", "Elemento combustible MOX");
        item("depleted_mox_reactor_fuel", "Spent MOX Fuel Assembly", "Elemento combustible MOX gastado");
        item("weapons_grade_plutonium_core", "Weapons-Grade Plutonium Core", "Núcleo de plutonio de grado militar");
        add("block.create_nuclearindustry.depleted_uranium_block", "Block of Depleted Uranium", "Bloque de uranio empobrecido");
        add("item.create_nuclearindustry.depleted_uranium_helmet", "Depleted Uranium Helmet", "Casco de uranio empobrecido");
        add("item.create_nuclearindustry.depleted_uranium_chestplate", "Depleted Uranium Chestplate", "Peto de uranio empobrecido");
        add("item.create_nuclearindustry.depleted_uranium_leggings", "Depleted Uranium Leggings", "Grebas de uranio empobrecido");
        add("item.create_nuclearindustry.depleted_uranium_boots", "Depleted Uranium Boots", "Botas de uranio empobrecido");
        add("block.create_nuclearindustry.turbine_exhaust_port", "Turbine Exhaust Port", "Puerto de escape de turbina");
        add("block.create_nuclearindustry.cooling_tower_casing", "Cooling Tower Casing", "Carcasa de torre de refrigeración");
        add("block.create_nuclearindustry.cooling_tower_controller", "Cooling Tower Controller", "Controlador de torre de refrigeración");
        add("block.create_nuclearindustry.cooling_tower_fluid_port", "Cooling Tower Fluid Port", "Puerto de fluidos de torre de refrigeración");
        add("block.create_nuclearindustry.cooling_tower_fan", "Cooling Tower Fan", "Ventilador de torre de refrigeración");
        add("create_nuclearindustry.display_source.cooling_tower", "Steam Condensed", "Vapor condensado");
        add("block.create_nuclearindustry.reactor_redstone_port",
                "Reactor Redstone Port",
                "Puerto de redstone de reactor");
        add("create_nuclearindustry.redstone_port.mode", "Mode: %s", "Modo: %s");
        add("create_nuclearindustry.redstone_port.mode.temperature", "Temperature", "Temperatura");
        add("create_nuclearindustry.redstone_port.mode.power", "Power", "Potencia");
        add("create_nuclearindustry.redstone_port.mode.fuel", "Fuel", "Combustible");
        add("create_nuclearindustry.redstone_port.mode.coolant", "Coolant", "Refrigerante");
        add("create_nuclearindustry.redstone_port.mode.steam", "Steam", "Vapor");
        add("create_nuclearindustry.redstone_port.mode.scram", "SCRAM signal", "Señal de SCRAM");
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
        add("block.create_nuclearindustry.depleted_uranium_ap_shot", "Depleted Uranium AP Shot", "Proyectil perforante de uranio empobrecido");
        add("item.create_nuclearindustry.depleted_uranium_ap_shot", "Depleted Uranium AP Shot", "Proyectil perforante de uranio empobrecido");
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
        add("create_nuclearindustry.display_source.reactor_temperature", "Reactor Temperature", "Temperatura del reactor");
        add("create_nuclearindustry.display_source.reactor_power", "Reactor Power", "Potencia del reactor");
        add("create_nuclearindustry.display_source.reactor_fuel", "Reactor Fuel", "Combustible del reactor");
        add("create_nuclearindustry.display_source.reactor_control_rods", "Control Rod Insertion", "Inserción de las barras de control");
        add("create_nuclearindustry.display_source.reactor_coolant", "Reactor Coolant", "Refrigerante del reactor");
        add("create_nuclearindustry.display_source.reactor_steam", "Reactor Steam", "Vapor del reactor");
        add("create_nuclearindustry.display_source.reactor_status", "Reactor Status", "Estado del reactor");
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
        block("reinforced_turbine_glass", "Reinforced Turbine Glass", "Vidrio reforzado de turbina");
        block("fusion_central_solenoid", "Central Solenoid", "Solenoide central");
        item("geiger_counter", "Geiger Counter", "Contador Geiger");
        add("item.create_nuclearindustry.geiger_counter.reading", "☢ %s rad/s   dose %s rad", "☢ %s rad/s   dosis %s rad");
        item("hazmat_helmet", "Hazmat Hood", "Capucha antirradiación");
        item("hazmat_chestplate", "Hazmat Suit", "Traje antirradiación");
        item("hazmat_leggings", "Hazmat Trousers", "Pantalones antirradiación");
        item("hazmat_boots", "Hazmat Boots", "Botas antirradiación");
        add("effect.create_nuclearindustry.radiation_sickness", "Radiation Sickness", "Síndrome de irradiación");
        add("death.attack.create_nuclearindustry.radiation", "%1$s died of radiation sickness", "%1$s murió por irradiación");
        add("death.attack.create_nuclearindustry.plutonium_burn", "%1$s was burnt by the plutonium they carried",
                "%1$s se quemó con el plutonio que llevaba");
        add("death.attack.create_nuclearindustry.plutonium_burn.player", "%1$s was burnt by plutonium while fighting %2$s",
                "%1$s se quemó con plutonio mientras luchaba contra %2$s");
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
        add("gui.create_nuclearindustry.reactor.scram_engaged", "SCRAM signal on: Redstone Ports in SCRAM mode emit",
                "Señal de SCRAM activa: los Puertos de redstone en modo SCRAM emiten");
        add("gui.create_nuclearindustry.reactor.fuel_in", "Fuel in", "Combustible");
        add("gui.create_nuclearindustry.reactor.auto_scram", "Auto SCRAM", "SCRAM auto.");
        add("gui.create_nuclearindustry.reactor.auto_scram.threshold", "%s°C", "%s°C");
        add("gui.create_nuclearindustry.reactor.auto_scram.off", "OFF", "NO");
        add("gui.create_nuclearindustry.reactor.auto_scram.tripped", "AUTO SCRAM tripped at %s °C: SCRAM signal on",
                "SCRAM AUTOMÁTICO disparado a %s °C: señal de SCRAM activa");
        add("gui.create_nuclearindustry.reactor.auto_scram.tooltip",
                "Automatic SCRAM: when the core reaches this temperature the SCRAM signal turns on and stays on until reset, which only works 200 °C below it. Click to turn it on or off.",
                "SCRAM automático: cuando el núcleo llega a esta temperatura se enciende la señal de SCRAM y se queda encendida hasta rearmar, lo que solo funciona 200 °C por debajo. Haz clic para activarlo o desactivarlo.");
        add("gui.create_nuclearindustry.reactor.auto_scram.step.tooltip", "Change the threshold by 100 °C (Shift: 500 °C)",
                "Cambia el umbral en 100 °C (Mayús: 500 °C)");
        add("gui.create_nuclearindustry.reactor.spent", "Spent", "Gastado");
        add("gui.create_nuclearindustry.reactor.scram", "SCRAM", "SCRAM");
        add("gui.create_nuclearindustry.reactor.reset", "Reset", "Rearmar");
        add("gui.create_nuclearindustry.reactor.scram.tooltip",
                "Emergency shutdown: turns on the SCRAM signal of every Redstone Port in SCRAM mode until reset. Wire it to the pistons that drive your control rods.",
                "Parada de emergencia: enciende la señal de SCRAM de todos los Puertos de redstone en modo SCRAM hasta rearmar. Conéctala a los pistones que mueven tus barras de control.");

        add("jei.create_nuclearindustry.info.reactor_fuel",
                "Fuel assemblies are pressed from uranium dioxide pellets. Each Fuel Channel in a reactor holds one assembly. Richer fuel lasts longer and is more reactive; spent fuel keeps its enrichment and can be reprocessed for plutonium.",
                "Los elementos combustibles se montan con pastillas de dióxido de uranio. Cada Canal de combustible de un reactor aloja uno. El combustible más enriquecido dura más y es más reactivo; el gastado conserva su enriquecimiento y se puede reprocesar para sacar plutonio.");
        add("jei.create_nuclearindustry.info.tokamak",
                "A ring-shaped fusion reactor around a Central Solenoid. Feed it deuterium and tritium, coolant and rotation for its Magnet Drives; it makes plasma steam for a Plasma Turbine. See its Ponder scene.",
                "Un reactor de fusión en forma de anillo alrededor de un Solenoide central. Necesita deuterio y tritio, refrigerante y rotación para sus Tomas de imanes; produce vapor de plasma para una Turbina de plasma. Consulta su escena de Ponder.");
        add("jei.create_nuclearindustry.info.radiation",
                "Enriched uranium, spent fuel and radioactive waste irradiate whoever carries them. A Geiger counter shows the dose; a full hazmat suit stops 95% of it.",
                "El uranio enriquecido, el combustible gastado y los residuos irradian a quien los lleva. Un contador Geiger muestra la dosis; un traje antirradiación completo detiene el 95%.");
        add("jei.create_nuclearindustry.info.turbine",
                "A box multiblock 3-7 blocks wide and high and 3-16 long, with a rotor column on its axis and this block at the centre of one end. Each rotor expands 80 mB/t of steam (40 of plasma steam); each input/output port pair passes 320 (160). Reinforced Turbine Glass shows the rotor and steam inside.",
                "Un multibloque en forma de caja de 3 a 7 bloques de ancho y alto y de 3 a 16 de largo, con una columna de rotores en su eje y este bloque en el centro de un extremo. Cada rotor expande 80 mB/t de vapor (40 de vapor de plasma); cada par de puertos entrada/salida deja pasar 320 (160). El Vidrio reforzado de turbina deja ver el rotor y el vapor.");
        add("jei.create_nuclearindustry.info.reactor_redstone_port",
                "Part of a reactor's shell. It puts out a 0-15 signal on its panel side: temperature, power, fuel, coolant or steam, or the SCRAM signal, which turns on when SCRAM is pressed or the automatic SCRAM trips. Control rods only move with pistons, so wire the SCRAM signal to them. Wrench to change the mode, sneak-wrench to turn it.",
                "Forma parte de la carcasa de un reactor. Da una señal de 0 a 15 por la cara del panel: temperatura, potencia, combustible, refrigerante o vapor, o la señal de SCRAM, que se enciende al pulsar SCRAM o al dispararse el SCRAM automático. Las barras de control solo se mueven con pistones, así que conecta a ellos la señal de SCRAM. Llave para cambiar el modo; agachado con la llave para girarlo.");
        add("jei.create_nuclearindustry.info.cooling_tower",
                "A hyperboloid of Cooling Tower Casing that turns steam or turbine exhaust back into water (heavy into heavy water). A turbine with an Exhaust Port sends its spent steam here and makes twice the power. Base radius 4 to 16, on a full floor; the walls narrow to a waist and flare out at the open top. It condenses its base radius times its height in mB/t, and Fans in the bottom wall layers add up to double that. Ports go in the floor or the two wall layers above it; the controller sits in the bottom wall layer facing out. See its Ponder scene.",
                "Un hiperboloide de Carcasa de torre de refrigeración que vuelve a convertir en agua el vapor o el escape de las turbinas (el pesado, en agua pesada). Una turbina con Puerto de escape le manda su vapor gastado y genera el doble de potencia. Radio de base de 4 a 16, sobre un suelo completo; las paredes se estrechan hasta una cintura y se abren en la cima, que queda abierta. Condensa su radio de base por su altura en mB/t, y los Ventiladores de las capas bajas de la pared añaden hasta el doble. Los puertos van en el suelo o en las dos capas de pared sobre él; el controlador va en la capa más baja de la pared, mirando hacia fuera. Consulta su escena de Ponder.");
        add("jei.create_nuclearindustry.info.depleted_uranium",
                "Depleted uranium (under 0.7%) is dense: a block of it lets only 10% of radiation through, the best shield there is. Its heavy plate armour protects better than diamond and, as a full set, stops 98% of radiation, but slows you by about a sixth. A crusher turns the block back into metal.",
                "El uranio empobrecido (menos del 0,7%) es muy denso: un bloque deja pasar solo el 10% de la radiación, el mejor blindaje que hay. Su armadura de placas protege más que el diamante y, completa, detiene el 98% de la radiación, pero te ralentiza más o menos una sexta parte. Una trituradora devuelve el bloque a metal.");
        add("jei.create_nuclearindustry.info.reprocessing",
                "Spent fuel is reprocessed for plutonium: crush it into fragments, dissolve them in nitric acid in a heated mixer (uranium comes back as uranyl nitrate one tier lower), separate the solution in a centrifuge into plutonium nitrate, recovered acid and waste, then make plutonium dioxide and reduce it to metal. Leaner fuel breeds more plutonium. Plutonium is far more radioactive than uranium and burns whoever carries it without a hazmat suit.",
                "El combustible gastado se reprocesa para sacar plutonio: tritúralo en fragmentos, disuélvelos en ácido nítrico en una mezcladora calentada (el uranio vuelve como nitrato de uranilo un nivel por debajo), separa la disolución en una centrifugadora en nitrato de plutonio, ácido recuperado y residuos, luego obtén dióxido de plutonio y redúcelo a metal. El combustible menos enriquecido cría más plutonio. El plutonio es mucho más radiactivo que el uranio y quema a quien lo lleve sin traje antirradiación.");
        add("jei.create_nuclearindustry.info.mox",
                "MOX fuel mixes plutonium dioxide into depleted uranium dioxide. In a reactor it burns like 12% uranium, a little more reactive and hotter, and control rods hold it back less, so it needs more of them. Spent MOX is reprocessed like spent fuel but gives back less plutonium than went in.",
                "El combustible MOX mezcla dióxido de plutonio con dióxido de uranio empobrecido. En un reactor arde como uranio al 12%, algo más reactivo y más caliente, y las barras de control lo frenan menos, así que necesita más. El MOX gastado se reprocesa como el combustible gastado, pero devuelve menos plutonio del que llevaba.");
        add("jei.create_nuclearindustry.info.rtg",
                "A small generator that turns decay heat into a steady 16 RPM, with no fuel to pump. Load it with RTG pellets by hand or by hopper: plutonium pellets give 256 stress capacity per RPM and halve in strength every 10 in-game days, waste pellets 64 and every 2 days. Below a quarter of their charge they come out spent. It irradiates its surroundings; wall it in with depleted uranium.",
                "Un generador pequeño que convierte el calor de desintegración en 16 RPM constantes, sin combustible que bombear. Se carga con pastillas RTG a mano o por tolva: las de plutonio dan 256 de capacidad de stress por RPM y pierden la mitad de fuerza cada 10 días de juego; las de residuos, 64 y cada 2 días. Por debajo de una cuarta parte de su carga salen agotadas. Irradia a su alrededor: rodéalo de uranio empobrecido.");
        add("jei.create_nuclearindustry.info.turbine_exhaust_port",
                "Goes on a turbine's face like a fluid port. A turbine with one stops condensing its steam inside: it lets it out here as exhaust steam and makes twice the power from the same steam. Only a Cooling Tower turns exhaust steam back into water; if it backs up, the turbine stops. Works on steam and plasma turbines.",
                "Va en una cara de la turbina, como un puerto de fluidos. Una turbina con uno deja de condensar el vapor dentro: lo saca por aquí como vapor de escape y genera el doble de potencia con el mismo vapor. Solo una Torre de refrigeración vuelve a convertir el vapor de escape en agua; si se acumula, la turbina se para. Funciona en turbinas de vapor y de plasma.");
        add("jei.create_nuclearindustry.category.electrolysis", "Electrolysis", "Electrólisis");
        add("jei.create_nuclearindustry.chance", "%s%% chance", "%s%% de probabilidad");
        add("jei.create_nuclearindustry.product", "Product: front face", "Producto: cara frontal");
        add("jei.create_nuclearindustry.tails", "Tails: bottom face", "Colas: cara inferior");
        add("jei.create_nuclearindustry.processing_time", "%ss at %s RPM, faster when spun faster", "%ss a %s RPM, más rápido a más velocidad");

        fluid("sulfuric_acid", "Sulfuric Acid", "Ácido sulfúrico");
        fluid("hydrofluoric_acid", "Hydrofluoric Acid", "Ácido fluorhídrico");
        fluid("nitric_acid", "Nitric Acid", "Ácido nítrico");
        fluid("exhaust_steam", "Exhaust Steam", "Vapor de escape");
        fluid("heavy_exhaust_steam", "Heavy Exhaust Steam", "Vapor de escape pesado");
        fluid("spent_fuel_solution", "Dissolved Spent Fuel", "Combustible gastado disuelto");
        fluid("plutonium_nitrate", "Plutonium Nitrate", "Nitrato de plutonio");
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
        add("create_nuclearindustry.ponder.reactor_redstone_port.header",
                "Reactor Redstone Ports and SCRAM",
                "Puertos de redstone del reactor y SCRAM");
        add("create_nuclearindustry.ponder.reactor_redstone_port.text_1",
                "A Reactor Redstone Port takes the place of a casing block in the shell and puts out a 0-15 signal on its panel side.",
                "Un Puerto de redstone de reactor ocupa el lugar de un bloque de carcasa y da una señal de 0 a 15 por la cara del panel.");
        add("create_nuclearindustry.ponder.reactor_redstone_port.text_2",
                "Use a Wrench to pick what it reads: temperature, power, fuel, coolant or steam. Like a comparator, any content gives at least 1.",
                "Con una Llave eliges qué lee: temperatura, potencia, combustible, refrigerante o vapor. Como un comparador, cualquier contenido da al menos 1.");
        add("create_nuclearindustry.ponder.reactor_redstone_port.text_3",
                "Its last mode is SCRAM. It lights up when SCRAM is pressed on the control panel or the automatic SCRAM trips, and stays on until reset.",
                "Su último modo es SCRAM. Se enciende al pulsar SCRAM en el panel de control o cuando salta el SCRAM automático, y sigue encendido hasta rearmar.");
        add("create_nuclearindustry.ponder.reactor_redstone_port.text_4",
                "Control Rods only move with pistons. Carry the signal to whatever drives them, here a Redstone Link pair and a Gearshift on the piston's shaft.",
                "Las Barras de control solo se mueven con pistones. Lleva la señal a lo que las mueva; aquí, un par de Enlaces de redstone y una Caja de cambios en el eje del pistón.");
        add("create_nuclearindustry.ponder.reactor_redstone_port.text_5",
                "On SCRAM the Gearshift reverses the piston and the rods go all the way in. Without such wiring, SCRAM only lights the port.",
                "Con el SCRAM, la Caja de cambios invierte el pistón y las barras entran del todo. Sin ese cableado, el SCRAM solo enciende el puerto.");
        add("create_nuclearindustry.ponder.rtg.header",
                "Radioisotope Generators",
                "Generadores de radioisótopos");
        add("create_nuclearindustry.ponder.rtg.text_1",
                "A Radioisotope Generator turns the decay heat of a pellet into a steady 16 RPM on its front shaft. Right-click with a pellet, or feed it by hopper.",
                "Un Generador de radioisótopos convierte el calor de desintegración de una pastilla en 16 RPM constantes en su eje frontal. Haz clic derecho con una pastilla o aliméntalo con una tolva.");
        add("create_nuclearindustry.ponder.rtg.text_2",
                "Plutonium pellets give 256 stress capacity per RPM and lose half of it every 10 in-game days; waste pellets give 64 and halve every 2. Below a quarter they come out spent.",
                "Las pastillas de plutonio dan 256 de capacidad de stress por RPM y pierden la mitad cada 10 días de juego; las de residuos dan 64 y se reducen a la mitad cada 2. Por debajo de una cuarta parte salen agotadas.");
        add("create_nuclearindustry.ponder.rtg.text_3",
                "It irradiates whoever stands near it...",
                "Irradia a quien esté cerca...");
        add("create_nuclearindustry.ponder.rtg.text_4",
                "...so wall it in. A Block of Depleted Uranium lets only a tenth of the radiation through.",
                "...así que rodéalo. Un Bloque de uranio empobrecido solo deja pasar una décima parte de la radiación.");
        add("create_nuclearindustry.ponder.waste_storage.header",
                "Geological Waste Storage",
                "Almacenamiento geológico de residuos");
        add("create_nuclearindustry.ponder.waste_storage.text_1",
                "At the surface a Waste Barrel irradiates whoever comes close, and slowly leaks into the ground around it.",
                "En la superficie, un Barril de residuos irradia a quien se acerca y va filtrándose poco a poco en el suelo de alrededor.");
        add("create_nuclearindustry.ponder.waste_storage.text_2",
                "Waste stored at Y=0 or lower counts as geologically stored.",
                "Los residuos guardados en Y=0 o más abajo cuentan como almacenados geológicamente.");
        add("create_nuclearindustry.ponder.waste_storage.text_3",
                "Down there barrels neither irradiate nor leak. Dig a deep gallery, fill it and seal it off: only the depth matters.",
                "Ahí abajo los barriles ni irradian ni se filtran. Excava una galería profunda, llénala y ciérrala: solo cuenta la profundidad.");
        add("create_nuclearindustry.ponder.cooling_tower.header",
                "Cooling Towers",
                "Torres de refrigeración");
        add("create_nuclearindustry.ponder.cooling_tower.text_1",
                "A Cooling Tower stands on a full round floor of Cooling Tower Casing, 4 to 16 blocks in radius.",
                "Una torre de refrigeración se levanta sobre un suelo redondo y completo de Carcasa de torre de refrigeración, de 4 a 16 bloques de radio.");
        add("create_nuclearindustry.ponder.cooling_tower.text_2",
                "Ring by ring, the wall narrows to a waist about three quarters of the way up, then flares out again. The top stays open.",
                "Anillo a anillo, la pared se estrecha hasta una cintura a unos tres cuartos de la altura y luego vuelve a abrirse. La cima queda abierta.");
        add("create_nuclearindustry.ponder.cooling_tower.text_3",
                "Its height may be from twice the radius plus 4 up to five times the radius. A bigger tower condenses more.",
                "Su altura puede ir del doble del radio más 4 hasta cinco veces el radio. Una torre más grande condensa más.");
        add("create_nuclearindustry.ponder.cooling_tower.text_4",
                "The inside must stay empty.",
                "El interior debe quedar vacío.");
        add("create_nuclearindustry.ponder.cooling_tower.text_5",
                "The Cooling Tower Controller goes in the bottom wall layer, centred on a side and facing out. Right-click it to form the tower.",
                "El Controlador de la torre va en la capa más baja de la pared, centrado en un lado y mirando hacia fuera. Haz clic derecho para formar la torre.");
        add("create_nuclearindustry.ponder.cooling_tower.text_6",
                "Fluid Ports go in the floor or the two wall layers above it. One on INPUT takes steam or heavy steam...",
                "Los Puertos de fluidos van en el suelo o en las dos capas de pared sobre él. Uno en ENTRADA recibe vapor o vapor pesado...");
        add("create_nuclearindustry.ponder.cooling_tower.text_7",
                "...and one on OUTPUT gives back the same amount of water or heavy water.",
                "...y otro en SALIDA devuelve la misma cantidad de agua o agua pesada.");
        add("create_nuclearindustry.ponder.cooling_tower.text_8",
                "Fans in the three bottom wall layers, facing out, add up to a quarter more each at 256 RPM, at most doubling the tower.",
                "Los Ventiladores en las tres capas bajas de la pared, mirando hacia fuera, añaden hasta una cuarta parte más cada uno a 256 RPM, como mucho el doble de la torre.");
        add("create_nuclearindustry.ponder.cooling_tower.text_9",
                "It condenses its base radius times its height in mB/t, 48 here. A turbine with an Exhaust Port sends it its spent steam instead of condensing it, and makes twice the power.",
                "Condensa su radio de base por su altura en mB/t, 48 aquí. Una turbina con Puerto de escape le manda su vapor gastado en lugar de condensarlo, y genera el doble de potencia.");
        add("create_nuclearindustry.ponder.turbine.header",
                "Building a Steam Turbine",
                "Construir una turbina de vapor");
        add("create_nuclearindustry.ponder.turbine.text_1",
                "A turbine is a box of casing: 3 to 7 blocks wide and high, and 3 to 16 blocks long.",
                "Una turbina es una caja de carcasa: de 3 a 7 bloques de ancho y alto, y de 3 a 16 de largo.");
        add("create_nuclearindustry.ponder.turbine.text_2",
                "A column of rotors runs down its axis. Every rotor stage expands 80 mB/t of steam.",
                "Una columna de rotores recorre su eje. Cada etapa de rotor expande 80 mB/t de vapor.");
        add("create_nuclearindustry.ponder.turbine.text_3",
                "Reinforced Turbine Glass can replace casing anywhere on a face, but not on edges or corners.",
                "El Vidrio reforzado de turbina puede sustituir la carcasa en cualquier punto de una cara, pero no en aristas ni esquinas.");
        add("create_nuclearindustry.ponder.turbine.text_4",
                "Fluid Ports go on the faces. One set to INPUT takes the steam...",
                "Los Puertos de fluido van en las caras. Uno en ENTRADA recibe el vapor...");
        add("create_nuclearindustry.ponder.turbine.text_5",
                "...and one set to OUTPUT returns the condensed water. Each pair passes 320 mB/t.",
                "...y otro en SALIDA devuelve el agua condensada. Cada par deja pasar 320 mB/t.");
        add("create_nuclearindustry.ponder.turbine.text_6",
                "The Turbine Output sits at the centre of one end, its shaft pointing out. Right-click it to form the turbine.",
                "La Salida de turbina va en el centro de un extremo, con el eje hacia fuera. Haz clic derecho para formar la turbina.");
        add("create_nuclearindustry.ponder.turbine.text_7",
                "Once formed, the rotor spins up while steam flows, and the glass shows how full the turbine is.",
                "Una vez formada, el rotor gira mientras fluye el vapor, y el vidrio muestra lo llena que está la turbina.");
        add("create_nuclearindustry.ponder.turbine.text_8",
                "Rotors and port pairs both cap the flow; the smaller limit wins. Longer turbines with more ports make more power.",
                "Los rotores y los pares de puertos limitan el caudal; manda el límite menor. Las turbinas más largas y con más puertos dan más potencia.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.header",
                "Building a Plasma Turbine",
                "Construir una turbina de plasma");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_1",
                "A turbine is a box of casing: 3 to 7 blocks wide and high, and 3 to 16 blocks long.",
                "Una turbina es una caja de carcasa: de 3 a 7 bloques de ancho y alto, y de 3 a 16 de largo.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_2",
                "A column of rotors runs down its axis. Every rotor stage expands 40 mB/t of plasma steam.",
                "Una columna de rotores recorre su eje. Cada etapa de rotor expande 40 mB/t de vapor de plasma.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_3",
                "Reinforced Turbine Glass can replace casing anywhere on a face, but not on edges or corners.",
                "El Vidrio reforzado de turbina puede sustituir la carcasa en cualquier punto de una cara, pero no en aristas ni esquinas.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_4",
                "Fluid Ports go on the faces. One set to INPUT takes the plasma steam...",
                "Los Puertos de fluido van en las caras. Uno en ENTRADA recibe el vapor de plasma...");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_5",
                "...and one set to OUTPUT returns the condensed water. Each pair passes 160 mB/t.",
                "...y otro en SALIDA devuelve el agua condensada. Cada par deja pasar 160 mB/t.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_6",
                "The Plasma Turbine sits at the centre of one end, its shaft pointing out. Right-click it to form the turbine.",
                "La Turbina de plasma va en el centro de un extremo, con el eje hacia fuera. Haz clic derecho para formar la turbina.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_7",
                "Once formed, the rotor spins up while plasma steam flows, and the glass shows how full the turbine is.",
                "Una vez formada, el rotor gira mientras fluye el vapor de plasma, y el vidrio muestra lo llena que está la turbina.");
        add("create_nuclearindustry.ponder.fusion_plasma_turbine.text_8",
                "Rotors and port pairs both cap the flow; the smaller limit wins. Longer turbines with more ports make more power.",
                "Los rotores y los pares de puertos limitan el caudal; manda el límite menor. Las turbinas más largas y con más puertos dan más potencia.");
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

    private static void commands() {
        String p = "commands.create_nuclearindustry.";
        add(p + "contamination.query", "Chunk %s, %s: %s rad/s, severity %s/%s",
                "Chunk %s, %s: %s rad/s, gravedad %s/%s");
        add(p + "contamination.add", "Added up to %s rad/s of contamination to %s loaded chunks",
                "Añadidos hasta %s rad/s de contaminación a %s chunks cargados");
        add(p + "contamination.set", "Chunk %s, %s set to %s rad/s",
                "Chunk %s, %s fijado a %s rad/s");
        add(p + "contamination.clear", "Cleared %s chunks",
                "Limpiados %s chunks");
        add(p + "contamination.clear_all", "Cleared %s loaded chunks; unloaded chunks will be clean when they load",
                "Limpiados %s chunks cargados; los no cargados estarán limpios al cargarse");
        add(p + "contamination.expand", "Spread contamination over %s chunks in %s steps",
                "Contaminación extendida por %s chunks en %s pasos");
        add(p + "contamination.fallout", "Fallout of radius %s blocks and %s rad/s at the centre",
                "Lluvia radiactiva de radio %s bloques y %s rad/s en el centro");
        add(p + "dose.get", "%s: dose %s rad, rate %s rad/s, lifetime total %s rad",
                "%s: dosis %s rad, tasa %s rad/s, total acumulado %s rad");
        add(p + "dose.set", "%s: dose set to %s rad",
                "%s: dosis fijada a %s rad");
        add(p + "dose.reset.current", "%s: dose reset",
                "%s: dosis a cero");
        add(p + "dose.reset.total", "%s: lifetime total reset",
                "%s: total acumulado a cero");
        add(p + "dose.reset.all", "%s: dose and lifetime total reset",
                "%s: dosis y total acumulado a cero");
        String h = "hud.create_nuclearindustry.";
        add(h + "rate", "☢ %s rad/s", "☢ %s rad/s");
        add(h + "dose", "Dose %s rad", "Dosis %s rad");
        add(h + "total", "Total %s rad", "Total %s rad");
    }
}
