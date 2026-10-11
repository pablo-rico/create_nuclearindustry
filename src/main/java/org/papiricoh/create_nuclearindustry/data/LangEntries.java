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
    private static final java.util.Set<String> KEYS = new java.util.HashSet<>();

    static {
        content();
        materials();
        moderation();
        controlRoom();
        medicine();
        dirtyBomb();
        advancements();
        jei();
        ponder();
        commands();
        AreaDataGen.ALL.forEach(AreaDataGen::lang);
    }

    private LangEntries() {}

    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    static void add(String key, String english, String spanish) {
        if (!KEYS.add(key)) {
            throw new IllegalStateException("Duplicate lang key " + key);
        }
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
        add("create_nuclearindustry.redstone_port.mode.rod_insert", "Insert rods", "Insertar barras");
        add("create_nuclearindustry.redstone_port.mode.rod_withdraw", "Withdraw rods", "Extraer barras");
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
        add("create_nuclearindustry.display_source.reactor_xenon", "Reactor Xenon Poisoning", "Envenenamiento por xenón del reactor");
        add("create_nuclearindustry.display_source.reactor_decay_heat", "Reactor Decay Heat", "Calor residual del reactor");
        add("create_nuclearindustry.display_source.reactor_reactivity", "Reactor Reactivity", "Reactividad del reactor");
        add("create_nuclearindustry.display_source.reactor_power_level", "Reactor Power (% of rated)", "Potencia del reactor (% nominal)");
        add("create_nuclearindustry.display_source.reactor_net_reactivity", "Reactor Net Reactivity (pcm)", "Reactividad neta del reactor (pcm)");
        add("create_nuclearindustry.display_source.reactor_coolant_temperature", "Reactor Coolant Temperature",
                "Temperatura del refrigerante del reactor");
        add("create_nuclearindustry.display_source.reactor_core_damage", "Reactor Fuel Damage", "Daño del combustible del reactor");
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
        item("radioactive_waste", "Intermediate-Level Waste", "Residuo de media actividad");
        item("fluorite", "Fluorite", "Fluorita");
        item("raw_spodumene", "Raw Spodumene", "Espodumena en bruto");
        item("crushed_spodumene", "Crushed Spodumene", "Espodumena triturada");
        item("spodumene_concentrate", "Spodumene Concentrate", "Concentrado de espodumena");
        item("lithium_carbonate", "Lithium Carbonate", "Carbonato de litio");
        item("empty_gas_cell", "Empty Gas Cell", "Célula de gas vacía");
        block("spodumene_ore", "Spodumene Ore", "Mena de espodumena");
        block("electrolyzer", "Electrolyzer", "Electrolizador");
        block("radioactive_waste_barrel", "Radioactive Waste Barrel", "Barril de residuos radiactivos");
        block("radiation_scrubber", "Radiation Scrubber", "Absorbedor de radiación");
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
        // The rest of the tokamak screen, goggles and messages: data/FusionDataGen.

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
        add("gui.create_nuclearindustry.reactor.temperature", "Fuel: %s °C  Coolant: %s °C", "Combustible: %s °C  Refrig.: %s °C");
        add("gui.create_nuclearindustry.reactor.power_level", "Power: %s%% of %s MW rated", "Potencia: %s%% de %s MW nominales");
        add("gui.create_nuclearindustry.reactor.fuel", "Fuel: %s / %s units (%s%%)", "Combustible: %s / %s unidades (%s%%)");
        add("gui.create_nuclearindustry.reactor.power", "Fission: %s MW  Steam: %s mB/t", "Fisión: %s MW  Vapor: %s mB/t");
        add("gui.create_nuclearindustry.reactor.state", "State: %s", "Estado: %s");
        add("gui.create_nuclearindustry.reactor.stress", "Fuel stress: %s%%  Damage: %s%%", "Tensión del comb.: %s%%  Daño: %s%%");
        add("gui.create_nuclearindustry.reactor.core", "Reactivity %s pcm  Xe -%s%%  Decay %s MW", "Reactividad %s pcm  Xe -%s%%  Residual %s MW");
        add("gui.create_nuclearindustry.reactor.excursion", "MELTING", "FUNDIÉNDOSE");
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
                "Fuel assemblies are pressed from uranium dioxide pellets. Each Fuel Channel in a reactor holds one assembly. Richer fuel lasts longer and is more reactive. Each fuel rod block is rated 50 MW of heat, and the reactor's power is shown as a share of that rating; spent fuel keeps its enrichment and can be reprocessed for plutonium.",
                "Los elementos combustibles se montan con pastillas de dióxido de uranio. Cada Canal de combustible de un reactor aloja uno. El combustible más enriquecido dura más y es más reactivo. Cada bloque de barra de combustible tiene una potencia nominal de 50 MW de calor, y la potencia del reactor se muestra como fracción de ella; el gastado conserva su enriquecimiento y se puede reprocesar para sacar plutonio.");
        add("jei.create_nuclearindustry.info.tokamak",
                "A ring-shaped fusion reactor around a Central Solenoid. Feed it deuterium and tritium, water as coolant, rotation for its Magnet Drives, vacuum pumps and liquid helium for its coils, then press Start: it pumps down, cools its coils, ramps the field, lights the plasma and heats it to a burn, making plasma steam for a Plasma Turbine. See its Ponder scene.",
                "Un reactor de fusión en forma de anillo alrededor de un Solenoide central. Necesita deuterio y tritio, agua como refrigerante, rotación para sus Tomas de imanes, bombas de vacío y helio líquido para sus bobinas; después pulsa Arrancar: hace el vacío, enfría las bobinas, sube el campo, enciende el plasma y lo calienta hasta la combustión, y produce vapor de plasma para una Turbina de plasma. Consulta su escena de Ponder.");
        add("jei.create_nuclearindustry.info.radiation",
                "Enriched uranium, spent fuel and radioactive waste irradiate whoever carries them. A Geiger counter shows the dose; a full hazmat suit stops 95% of it.",
                "El uranio enriquecido, el combustible gastado y los residuos irradian a quien los lleva. Un contador Geiger muestra la dosis; un traje antirradiación completo detiene el 95%.");
        add("jei.create_nuclearindustry.info.radiation_scrubber",
                "Draws ground contamination out of its chunk and the chunks around it, the worst first. Drive it with a shaft from below; it cleans faster up to 256 RPM. What it absorbs comes out as Low-Level Waste for a funnel or hopper; when full it stops. Contamination comes from fallout, meltdowns and waste stored outside a proper repository.",
                "Saca la contaminación del suelo de su chunk y de los de alrededor, empezando por el peor. Acciónalo con un eje desde abajo; limpia más rápido hasta 256 RPM. Lo que absorbe sale como Residuos de baja actividad para un embudo o una tolva; lleno se detiene. La contaminación viene de la lluvia radiactiva, las fusiones del núcleo y los residuos guardados fuera de un buen repositorio.");
        add("jei.create_nuclearindustry.info.waste_storage",
                "A placed waste package (barrel, drum or canister) irradiates whoever comes close and slowly leaks into the ground of its chunk unless it sits in a good geological repository. Its score adds up four barriers: depth below the surface (full at 64 blocks), host rock such as deepslate, tuff or basalt within 3 blocks, backfill covering its faces (Bentonite or other packages) and the waste form (a raw barrel counts least, a vitrified canister most). A fluid source within 4 blocks takes a large share off. From 80% it counts as stored and neither irradiates nor leaks; below that it does less the higher the score. A Repository Inspector shows the packages within 8 blocks and what the weakest one lacks, by goggles, Display Link or comparator. waste.repository.legacyDepthRule in the config brings back the old rule: stored at Y=0 or lower. See its Ponder scene.",
                "Un bulto de residuos colocado (barril, bidón o contenedor) irradia a quien se acerca y se filtra poco a poco en el suelo de su chunk, salvo que esté en un buen repositorio geológico. Su puntuación suma cuatro barreras: la profundidad bajo la superficie (completa a 64 bloques), la roca huésped como pizarra profunda, toba o basalto a menos de 3 bloques, el relleno que cubre sus caras (Bentonita u otros bultos) y la forma del residuo (un barril sin tratar cuenta lo mínimo y un contenedor vitrificado lo máximo). Una fuente de fluido a menos de 4 bloques le quita buena parte. Desde el 80% cuenta como almacenado y ni irradia ni se filtra; por debajo, lo hace menos cuanto más alta es la puntuación. Un Inspector de repositorio muestra los bultos a menos de 8 bloques y qué le falta al peor, con las gafas, un Display Link o un comparador. waste.repository.legacyDepthRule en la configuración recupera la regla antigua: almacenado en Y=0 o más abajo. Mira su escena de Ponder.");
        add("jei.create_nuclearindustry.info.contaminated_water",
                "Heavy fallout turns the surface of lakes and rivers into Contaminated Water. It irradiates; a centrifuge separates it into clean water and low-level waste.",
                "La lluvia radiactiva intensa convierte la superficie de lagos y ríos en Agua contaminada. Irradia; una centrífuga la separa en agua limpia y residuos de baja actividad.");
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
                "Spent fuel is reprocessed for plutonium: crush it into fragments, dissolve them in nitric acid in a heated mixer (uranium comes back as uranyl nitrate one tier lower), separate the solution in a centrifuge into plutonium nitrate, recovered acid and high-level waste, then make plutonium dioxide and reduce it to metal. Leaner fuel breeds more plutonium. Plutonium is far more radioactive than uranium and burns whoever carries it without a hazmat suit.",
                "El combustible gastado se reprocesa para sacar plutonio: tritúralo en fragmentos, disuélvelos en ácido nítrico en una mezcladora calentada (el uranio vuelve como nitrato de uranilo un nivel por debajo), separa la disolución en una centrifugadora en nitrato de plutonio, ácido recuperado y residuos de alta actividad, luego obtén dióxido de plutonio y redúcelo a metal. El combustible menos enriquecido cría más plutonio. El plutonio es mucho más radiactivo que el uranio y quema a quien lo lleve sin traje antirradiación.");
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
        fluid("contaminated_water", "Contaminated Water", "Agua contaminada");
        block("contaminated_water", "Contaminated Water", "Agua contaminada");
        item("contaminated_water_bucket", "Contaminated Water Bucket", "Cubo de agua contaminada");
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

    private static void advancements() {
        for (NuclearAdvancementProvider.Entry entry : NuclearAdvancementProvider.ENTRIES) {
            add(entry.titleKey(), entry.title(), entry.titleEs());
            add(entry.descriptionKey(), entry.description(), entry.descriptionEs());
        }
    }

    private static void dirtyBomb() {
        block("dirty_bomb", "Dirty Bomb", "Bomba sucia");
        add("gui.create_nuclearindustry.dirty_bomb.load", "Radioactive load: %s rad/s", "Carga radiactiva: %s rad/s");
        add("gui.create_nuclearindustry.dirty_bomb.radius", "Fallout radius: %s", "Radio de lluvia radiactiva: %s");
        add("gui.create_nuclearindustry.dirty_bomb.intensity", "Ground zero: %s rad/s", "Zona cero: %s rad/s");
        add("jei.create_nuclearindustry.info.dirty_bomb",
                "A radiological weapon. Load it with anything radioactive that is not weapons-grade (waste, spent fuel, fragments, RTG pellets) and power it with redstone: after 10 seconds a small blast scatters the load. No crater, but the more radioactive the load, the wider the fallout (up to 128 blocks) and the hotter the spot left at ground zero. A launch pad also takes radioactive material: on its own it makes a dirty warhead, packed with fissile material it salts a nuclear one for dirtier fallout.",
                "Un arma radiológica. Cárgala con cualquier cosa radiactiva que no sea de grado militar (residuos, combustible gastado, fragmentos, pastillas RTG) y dale señal de redstone: a los 10 segundos una pequeña explosión dispersa la carga. No deja cráter, pero cuanto más radiactiva es la carga, más lejos llega la lluvia radiactiva (hasta 128 bloques) y más caliente queda la zona cero. La plataforma de lanzamiento también acepta material radiactivo: solo, forma una cabeza sucia; junto a material fisible, \"sala\" una cabeza nuclear y su lluvia radiactiva es más sucia.");
    }

    private static void medicine() {
        item("potassium_iodide", "Potassium Iodide", "Yoduro potásico");
        item("prussian_blue", "Prussian Blue", "Azul de Prusia");
        add("item.create_nuclearindustry.potassium_iodide.tooltip", "Halves the radiation you take up for 3 minutes",
                "Reduce a la mitad la radiación que absorbes durante 3 minutos");
        add("item.create_nuclearindustry.prussian_blue.tooltip", "Flushes 15 rad of dose a second for 30 seconds",
                "Elimina 15 rad de dosis por segundo durante 30 segundos");
        add("effect.create_nuclearindustry.radioprotection", "Radioprotection", "Radioprotección");
        add("effect.create_nuclearindustry.radiation_purge", "Radiation Purge", "Purga de radiación");
        add("jei.create_nuclearindustry.info.medicine",
                "Potassium Iodide (dried kelp and bone meal, mixed hot in water) halves the radiation you take up for 3 minutes: take it before going near a hot core. Prussian Blue (iron nuggets and blue dye in sulfuric acid) flushes 15 rad of dose a second for 30 seconds, curing radiation sickness. Neither lowers the lifetime total. Both are eaten whatever your hunger, then wait 5 seconds.",
                "El Yoduro potásico (algas secas y polvo de hueso, mezclados en caliente con agua) reduce a la mitad la radiación que absorbes durante 3 minutos: tómalo antes de acercarte a un núcleo caliente. El Azul de Prusia (pepitas de hierro y tinte azul en ácido sulfúrico) elimina 15 rad de dosis por segundo durante 30 segundos y cura el síndrome de irradiación. Ninguno baja el total acumulado. Ambos se toman aunque no tengas hambre; después hay que esperar 5 segundos.");
    }

    private static void controlModules() {
        item("push_button_module", "Push Button Module", "Módulo de pulsador");
        item("lever_module", "Lever Module", "Módulo de palanca");
        item("dial_module", "Dial Module", "Módulo de rueda");
        item("lamp_module", "Indicator Lamp Module", "Módulo de piloto");
        item("gauge_module", "Gauge Module", "Módulo de medidor");
        item("key_switch_module", "Key Switch Module", "Módulo de interruptor de llave");
        String m = "create_nuclearindustry.control_module.";
        add(m + "title", "Control Module", "Módulo de control");
        add(m + "tooltip", "Fits a Control Desk slot. Wrench it to set it up.",
                "Va en un hueco de un Pupitre de control. Configúralo con la llave inglesa.");
        add(m + "inactive", "This module does nothing with what the console is linked to",
                "Este módulo no hace nada con lo que tiene enlazado la consola");
        add(m + "inactive_short", "Not for this target", "No sirve para este objetivo");
        add(m + "state", "%s%%  signal %s", "%s%%  señal %s");
        add(m + "label", "Label", "Etiqueta");
        add(m + "label.hint", "Label (optional)", "Etiqueta (opcional)");
        add(m + "function", "Function: %s", "Función: %s");
        add(m + "output", "Redstone: %s", "Redstone: %s");
        add(m + "frequency", "Link frequency", "Frecuencia de enlace");
        add(m + "type.push_button", "Push Button", "Pulsador");
        add(m + "type.lever", "Lever", "Palanca");
        add(m + "type.dial", "Dial", "Rueda");
        add(m + "type.lamp", "Indicator Lamp", "Piloto");
        add(m + "type.gauge", "Gauge", "Medidor");
        add(m + "type.key_switch", "Key Switch", "Interruptor de llave");
        add(m + "function.scram", "SCRAM / reset", "SCRAM / rearmar");
        add(m + "function.pulse", "Redstone pulse", "Pulso de redstone");
        add(m + "function.auto_scram", "Automatic SCRAM", "SCRAM automático");
        add(m + "function.tokamak_fuel", "Tokamak on/off", "Tokamak encendido/apagado");
        add(m + "function.toggle", "Redstone on/off", "Redstone sí/no");
        add(m + "function.rod_target", "Control rod target", "Objetivo de las barras");
        add(m + "function.scram_threshold", "Auto SCRAM threshold", "Umbral del SCRAM automático");
        add(m + "function.signal_level", "Redstone level", "Nivel de redstone");
        add(m + "function.lamp_scram", "SCRAM signal", "Señal de SCRAM");
        add(m + "function.lamp_running", "Running", "En marcha");
        add(m + "function.lamp_no_signal", "No signal", "Sin señal");
        add(m + "function.lamp_alarm", "Alarm", "Alarma");
        add(m + "function.reading", "Reading", "Lectura");
        add(m + "output.none", "none", "ninguna");
        add(m + "output.below", "below the desk", "bajo el pupitre");
        add(m + "output.link", "Redstone Link", "Redstone Link");
        String r = m + "reading.";
        add(r + "reactor_temperature", "Reactor temperature", "Temperatura del reactor");
        add(r + "reactor_power", "Reactor power", "Potencia del reactor");
        add(r + "reactor_rods", "Control rod insertion", "Inserción de las barras");
        add(r + "reactor_xenon", "Xenon poisoning", "Envenenamiento por xenón");
        add(r + "reactor_coolant", "Reactor coolant", "Refrigerante del reactor");
        add(r + "reactor_steam", "Reactor steam", "Vapor del reactor");
        add(r + "reactor_fuel", "Reactor fuel", "Combustible del reactor");
        add(r + "turbine_speed", "Turbine speed", "Velocidad de la turbina");
        add(r + "turbine_load", "Turbine load", "Carga de la turbina");
        add(r + "tower_load", "Cooling tower load", "Carga de la torre");
        add(r + "tower_water", "Cooling tower water", "Agua de la torre");
        add(r + "plasma_temperature", "Plasma temperature", "Temperatura del plasma");
        add(r + "fusion_power", "Fusion power", "Potencia de fusión");
        add(r + "confinement", "Plasma confinement", "Confinamiento del plasma");
        add(r + "instability", "Plasma instability", "Inestabilidad del plasma");
        add(r + "area_exposure", "Radiation exposure", "Exposición a la radiación");
        add(r + "area_ground", "Ground contamination", "Contaminación del suelo");
        add("jei.create_nuclearindustry.info.control_desk",
                "Control Desks placed side by side, or meeting at a corner, join into one console; link any of them with the Control Room Linker to link the whole console. Each straight desk holds four control modules and each corner two, one on each arm (high up on an inner corner, low down on an outer one): click a slot with a module to fit it, wrench the module to choose what it does and where its redstone goes, sneak-wrench to take it out, and click it with an empty hand to work it (sneak to turn a dial down). Push buttons SCRAM and reset the reactor, send a pulse or start and shut down a tokamak; levers switch the automatic SCRAM, a tokamak's start, shutdown or heating or a redstone output; dials set the rod target, the SCRAM threshold or a redstone level; lamps light on SCRAM, running, no signal or an alarm; gauges show any reading. Any module can put its state out as redstone below the desk or on a Create Redstone Link frequency.",
                "Los Pupitres de control colocados uno junto a otro, o en esquina, se unen en una consola; enlaza cualquiera de ellos con el Enlazador de sala de control para enlazar la consola entera. Cada pupitre recto lleva cuatro módulos de control y cada esquina dos, uno en cada brazo (arriba en una esquina interior, abajo en una exterior): haz clic en un hueco con un módulo para colocarlo, usa la llave inglesa sobre él para elegir qué hace y adónde va su redstone, agachado con la llave para quitarlo, y haz clic con la mano vacía para accionarlo (agachado baja una rueda). Los pulsadores hacen SCRAM y rearman el reactor, mandan un pulso o arrancan y paran un tokamak; las palancas activan el SCRAM automático, el arranque, la parada o el calentamiento de un tokamak o una salida de redstone; las ruedas fijan el objetivo de barras, el umbral del SCRAM o un nivel de redstone; los pilotos se encienden con SCRAM, en marcha, sin señal o una alarma; los medidores muestran cualquier lectura. Cualquier módulo puede sacar su estado como redstone bajo el pupitre o por una frecuencia de Redstone Link de Create.");
    }

    private static void controlRoom() {
        block("control_monitor", "Control Room Monitor", "Monitor de sala de control");
        block("reactor_control_panel", "Control Desk", "Pupitre de control");
        block("nuclear_alarm", "Nuclear Alarm", "Alarma nuclear");
        item("control_room_linker", "Control Room Linker", "Enlazador de sala de control");
        String linker = "item.create_nuclearindustry.control_room_linker.";
        add(linker + "stored", "Linker set to %s", "Enlazador apuntando a %s");
        add(linker + "applied", "Linked to %s", "Enlazado a %s");
        add(linker + "unlinked", "Device unlinked", "Dispositivo desenlazado");
        add(linker + "cleared", "Linker cleared", "Enlazador borrado");
        add(linker + "empty", "Click a reactor, turbine, cooling tower or any block first",
                "Primero haz clic en un reactor, turbina, torre de refrigeración o cualquier bloque");
        add(linker + "out_of_range", "Too far: devices reach %s blocks, in the same dimension",
                "Demasiado lejos: los dispositivos llegan a %s bloques, en la misma dimensión");
        add(linker + "describe", "%s at %s %s %s", "%s en %s %s %s");
        add(linker + "target", "Target: %s %s %s", "Objetivo: %s %s %s");
        add(linker + "none", "No target: click a machine or any block", "Sin objetivo: haz clic en una máquina o en cualquier bloque");
        String kind = "create_nuclearindustry.control_room.kind.";
        add(kind + "none", "Unlinked", "Sin enlazar");
        add(kind + "no_signal", "No signal", "Sin señal");
        add(kind + "reactor", "Reactor", "Reactor");
        add(kind + "turbine", "Turbine", "Turbina");
        add(kind + "cooling_tower", "Cooling Tower", "Torre de refrigeración");
        add(kind + "area", "Radiation", "Radiación");
        String status = "create_nuclearindustry.control_room.status.";
        add(status + "unformed", "Unformed", "Sin formar");
        add(status + "suspended", "Paused", "En pausa");
        add(status + "running", "Running", "En marcha");
        add(status + "idle", "Idle", "En espera");
        add(status + "radiation", "RADIATION", "RADIACIÓN");
        add(status + "clear", "Clear", "Limpio");
        String monitor = "create_nuclearindustry.monitor.";
        add(monitor + "unlinked", "Unlinked", "Sin enlazar");
        add(monitor + "unlinked.hint", "Use a Control Room Linker", "Usa un Enlazador de sala de control");
        add(monitor + "no_signal", "NO SIGNAL", "SIN SEÑAL");
        add(monitor + "no_signal.range", "Target out of range", "Objetivo fuera de alcance");
        add(monitor + "no_signal.unloaded", "Target not loaded", "Objetivo sin cargar");
        add(monitor + "no_signal.dimension", "Target in another dimension", "Objetivo en otra dimensión");
        add(monitor + "temperature", "Temp.", "Temp.");
        add(monitor + "power", "Power", "Potencia");
        add(monitor + "rods", "Rods", "Barras");
        add(monitor + "xenon", "Xenon", "Xenón");
        add(monitor + "coolant", "Coolant", "Refrig.");
        add(monitor + "reactivity", "Reactivity", "Reactividad");
        add(monitor + "decay_heat", "Decay heat", "Calor resid.");
        add(monitor + "steam", "Steam", "Vapor");
        add(monitor + "fuel", "Fuel", "Combust.");
        add(monitor + "neutrons", "Neutrons", "Neutrones");
        add(monitor + "stress", "Fuel stress", "Tensión comb.");
        add(monitor + "damage", "Fuel damage", "Daño comb.");
        add(monitor + "coolant_temperature", "Coolant T", "T refrig.");
        add(monitor + "speed", "Speed", "Velocidad");
        add(monitor + "steam_use", "Steam use", "Consumo");
        add(monitor + "capacity", "Capacity", "Capacidad");
        add(monitor + "steam_tank", "Steam tank", "Depósito");
        add(monitor + "exhaust", "Exhaust", "Escape");
        add(monitor + "yes", "Yes", "Sí");
        add(monitor + "no", "No", "No");
        add(monitor + "condensing", "Condensing", "Condensa");
        add(monitor + "water", "Water", "Agua");
        add(monitor + "fans", "Fans", "Ventiladores");
        add(monitor + "exposure", "Exposure", "Exposición");
        add(monitor + "ground", "Ground", "Suelo");
        controlModules();
        String alarm = "create_nuclearindustry.alarm.";
        add(alarm + "metric", "Watch", "Vigilar");
        add(alarm + "threshold", "Threshold", "Umbral");
        add(alarm + "metric.temperature", "Reactor temperature above", "Temperatura del reactor por encima de");
        add(alarm + "metric.power", "Reactor power, share of rated, above", "Potencia del reactor, fracción de la nominal, por encima de");
        add(alarm + "metric.xenon", "Xenon poisoning above", "Envenenamiento por xenón por encima de");
        add(alarm + "metric.coolant_low", "Reactor coolant below", "Refrigerante del reactor por debajo de");
        add(alarm + "metric.scram", "Reactor SCRAM signal", "Señal de SCRAM del reactor");
        add(alarm + "metric.turbine_load", "Turbine load above", "Carga de la turbina por encima de");
        add(alarm + "metric.tower_load", "Cooling tower load above", "Carga de la torre por encima de");
        add(alarm + "metric.area_radiation", "Radiation above", "Radiación por encima de");
        add(kind + "tokamak", "Tokamak", "Tokamak");
        add(status + "burning", "Burning", "En fusión");
        add(status + "cold", "Cold", "Frío");
        add(monitor + "plasma", "Plasma", "Plasma");
        add(monitor + "confinement", "Confinement", "Confinamiento");
        add(monitor + "instability", "Instability", "Inestabilidad");
        add(monitor + "deuterium", "Deuterium", "Deuterio");
        add(monitor + "tritium", "Tritium", "Tritio");
        add(alarm + "metric.plasma_temperature", "Plasma temperature above", "Temperatura del plasma por encima de");
        add(alarm + "metric.plasma_instability", "Plasma instability above", "Inestabilidad del plasma por encima de");
        add("create_nuclearindustry.display_source.turbine", "Turbine Status", "Estado de la turbina");
        add("create_nuclearindustry.ponder.control_room.header", "Building a Control Room", "Montar una sala de control");
        add("create_nuclearindustry.ponder.control_room.text_1",
                "A control room watches and drives machines from afar. Click a reactor, turbine or cooling tower (any of its blocks) with the Control Room Linker.",
                "Una sala de control vigila y maneja máquinas a distancia. Haz clic con el Enlazador de sala de control en un reactor, turbina o torre de refrigeración (en cualquiera de sus bloques).");
        add("create_nuclearindustry.ponder.control_room.text_2",
                "Monitors side by side join into one screen up to 8x8. Click one with the linker to show the machine: readings and gauges, and on a big screen a two-minute graph.",
                "Los monitores juntos forman una pantalla de hasta 8x8. Haz clic en uno con el enlazador para mostrar la máquina: lecturas e indicadores y, en una pantalla grande, una gráfica de dos minutos.");
        add("create_nuclearindustry.ponder.control_room.text_3",
                "Click a screen to change page. A comparator reads its main value; Display Links read a linked reactor too.",
                "Haz clic en una pantalla para cambiar de página. Un comparador lee su valor principal; los Display Links también leen un reactor enlazado.");
        add("create_nuclearindustry.ponder.control_room.text_4",
                "Control Desks side by side join into one console. Fit control modules into their slots and set each up with a wrench: buttons, levers and dials SCRAM the reactor, switch its automatic SCRAM and set a target for the control rods.",
                "Los Pupitres de control juntos forman una consola. Coloca módulos de control en sus huecos y configura cada uno con la llave inglesa: pulsadores, palancas y ruedas hacen SCRAM al reactor, activan su SCRAM automático y fijan un objetivo para las barras de control.");
        add("create_nuclearindustry.ponder.control_room.text_5",
                "Rods still only move with pistons: Redstone Ports set to Insert rods or Withdraw rods light up until the rods reach the target. Wire them to the clutch and gearshift of the rod pistons.",
                "Las barras solo se mueven con pistones: los Puertos de redstone en Insertar barras o Extraer barras se encienden hasta que las barras llegan al objetivo. Conéctalos al embrague y la caja de cambios de los pistones de las barras.");
        add("create_nuclearindustry.ponder.control_room.text_6",
                "A Nuclear Alarm rings and powers every side once a reading passes its threshold. Pick the reading on its top and the threshold on its sides.",
                "Una Alarma nuclear suena y da señal por todos sus lados cuando una lectura pasa su umbral. Elige la lectura en su cara superior y el umbral en sus laterales.");
        add("jei.create_nuclearindustry.info.control_room",
                "Build a control room. Click a reactor, turbine or cooling tower (any of their blocks) with the Control Room Linker, or any other block to watch the radiation there, then click a Monitor, Control Desk or Nuclear Alarm to link it; sneak-click a device to unlink it. Monitors side by side join into one screen up to 8x8 that shows the readings and two minutes of history; click it to change page. Control Desks carry modules that SCRAM the reactor, switch its automatic SCRAM and set a rod target: Redstone Ports in Insert rods or Withdraw rods mode light up until the rods reach it, to drive the clutch and gearshift of the pistons. The alarm rings and powers every side past the threshold set on its sides, for the reading chosen on its top. Devices reach 128 blocks.",
                "Monta una sala de control. Haz clic con el Enlazador de sala de control en un reactor, turbina o torre de refrigeración (en cualquiera de sus bloques), o en cualquier otro bloque para vigilar la radiación allí, y luego en un Monitor, Pupitre de control o Alarma nuclear para enlazarlo; agachado sobre un dispositivo lo desenlaza. Los monitores juntos forman una pantalla de hasta 8x8 que muestra las lecturas y dos minutos de historial; haz clic para cambiar de página. Los Pupitres de control llevan módulos que hacen SCRAM al reactor, activan su SCRAM automático y fijan un objetivo de barras: los Puertos de redstone en modo Insertar barras o Extraer barras se encienden hasta que las barras llegan a él, para mover el embrague y la caja de cambios de los pistones. La alarma suena y da señal por todos sus lados al pasar el umbral fijado en sus caras, para la lectura elegida en su cara superior. Los dispositivos llegan a 128 bloques.");
    }

    private static void moderation() {
        item("graphite", "Graphite", "Grafito");
        block("graphite_moderator", "Graphite Moderator", "Moderador de grafito");
        block("beryllium_reflector", "Beryllium Reflector", "Reflector de berilio");
        add("create_nuclearindustry.ponder.reactor_moderation.header",
                "Moderating and Reflecting the Core",
                "Moderar y reflejar el núcleo");
        add("create_nuclearindustry.ponder.reactor_moderation.text_1",
                "Graphite Moderator fills whole interior columns. It slows neutrons, so the Fuel Channels sharing a face with it split more often.",
                "El Moderador de grafito llena columnas interiores enteras. Frena los neutrones, así que los Canales de combustible pegados a sus caras se fisionan más.");
        add("create_nuclearindustry.ponder.reactor_moderation.text_2",
                "A fuel column with two graphite neighbours is fully moderated; one with a single neighbour, half. A fully moderated core is 35%% more reactive.",
                "Una columna de combustible con dos vecinas de grafito está totalmente moderada; con una sola, a medias. Un núcleo totalmente moderado es un 35%% más reactivo.");
        add("create_nuclearindustry.ponder.reactor_moderation.text_3",
                "Beryllium Reflector belongs in the columns that touch the shell: it bounces escaping neutrons back into the core.",
                "El Reflector de berilio va en las columnas que tocan la carcasa: devuelve al núcleo los neutrones que escapan.");
        add("create_nuclearindustry.ponder.reactor_moderation.text_4",
                "A ring lined all round adds reactivity too. Both make the same fuel more reactive, so leave room for enough Control Rods.",
                "Un anillo revestido por completo también suma reactividad. Ambos hacen más reactivo el mismo combustible, así que deja sitio para suficientes Barras de control.");
        add("jei.create_nuclearindustry.info.reactor_poisons",
                "A running core makes iodine, which decays into xenon, a neutron poison. At steady power it holds the reactor back a little; after power drops it keeps building for a few minutes before it decays, so a reactor just shut down needs more rods out to restart. Fission products also keep heating the core after a SCRAM, about 7% of the fission heat, fading over a few minutes: keep the coolant flowing. The control panel, goggles and Display Links show reactivity, xenon and decay heat.",
                "Un núcleo en marcha produce yodo, que se desintegra en xenón, un veneno de neutrones. A potencia constante frena un poco el reactor; cuando la potencia baja sigue acumulándose unos minutos antes de desaparecer, así que un reactor recién parado necesita sacar más barras para volver a arrancar. Los productos de fisión también siguen calentando el núcleo tras un SCRAM, cerca del 7% del calor de fisión, que se apaga en unos minutos: no cortes el refrigerante. El panel de control, las gafas y los Display Links muestran la reactividad, el xenón y el calor residual.");
        add("jei.create_nuclearindustry.info.moderation",
                "Graphite Moderator and Beryllium Reflector fill whole interior columns of a fission reactor, like Fuel Channels. Graphite slows neutrons so the fuel beside it splits more: a Fuel Channel with two graphite columns sharing a face with it is fully moderated, and a fully moderated core gains about 1500 pcm of reactivity, but graphite keeps moderating when the water boils, so steam in its core adds reactivity instead of taking it away. Beryllium lining the columns that touch the shell bounces escaping neutrons back: a complete ring adds about 1000 pcm. Both make the same fuel more reactive, so they also need more control rods.",
                "El Moderador de grafito y el Reflector de berilio llenan columnas interiores enteras de un reactor de fisión, como los Canales de combustible. El grafito frena los neutrones para que el combustible de al lado se fisione más: un Canal de combustible con dos columnas de grafito pegadas a sus caras está totalmente moderado, y un núcleo totalmente moderado gana unos 1500 pcm de reactividad, pero el grafito sigue moderando cuando el agua hierve, así que el vapor del núcleo le suma reactividad en vez de quitársela. El berilio que reviste las columnas que tocan la carcasa devuelve los neutrones que escapan: un anillo completo suma unos 1000 pcm. Ambos hacen más reactivo el mismo combustible, así que también necesitan más barras de control.");
    }

    static void item(String id, String english, String spanish) {
        add("item.create_nuclearindustry." + id, english, spanish);
    }

    static void block(String id, String english, String spanish) {
        add("block.create_nuclearindustry." + id, english, spanish);
    }

    static void fluid(String id, String english, String spanish) {
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
        add("create_nuclearindustry.ponder.radiation_scrubber.header",
                "Radiation Scrubbers",
                "Absorbedores de radiación");
        add("create_nuclearindustry.ponder.radiation_scrubber.text_1",
                "Fallout leaves the ground contaminated: green dust rises from it, and standing on it irradiates.",
                "La lluvia radiactiva deja el suelo contaminado: de él sube un polvo verde y estar encima irradia.");
        add("create_nuclearindustry.ponder.radiation_scrubber.text_2",
                "A Radiation Scrubber draws it out of the ground of its chunk and the eight around it, the worst first. Drive it from below: it cleans faster up to 256 RPM.",
                "Un Absorbedor de radiación la saca del suelo de su chunk y de los ocho de alrededor, empezando por el peor. Acciónalo desde abajo: limpia más rápido hasta 256 RPM.");
        add("create_nuclearindustry.ponder.radiation_scrubber.text_3",
                "What it absorbs comes out as Low-Level Waste, for a Compacted Waste Drum. Take it out with a funnel or hopper: full, it stops, and the waste inside irradiates.",
                "Lo que absorbe sale como Residuo de baja actividad, para un Bidón de residuos compactados. Sácalo con un embudo o una tolva: lleno se detiene, y los residuos de dentro irradian.");
        add("create_nuclearindustry.ponder.waste_storage.header",
                "Geological Waste Storage",
                "Almacenamiento geológico de residuos");
        add("create_nuclearindustry.ponder.waste_storage.text_1",
                "At the surface a Waste Barrel irradiates whoever comes close, and slowly leaks into the ground around it.",
                "En la superficie, un Barril de residuos irradia a quien se acerca y va filtrándose poco a poco en el suelo de alrededor.");
        add("create_nuclearindustry.ponder.waste_storage.text_2",
                "Underground, a repository score decides: depth below the surface, host rock such as deepslate around it, Bentonite backfill on its faces and the waste form. Water within 4 blocks spoils it.",
                "Bajo tierra decide una puntuación de repositorio: la profundidad bajo la superficie, la roca que lo rodea, como la pizarra profunda, el relleno de Bentonita en sus caras y la forma del residuo. El agua a menos de 4 bloques la arruina.");
        add("create_nuclearindustry.ponder.waste_storage.text_3",
                "From a score of 80%% a package neither irradiates nor leaks. Raw barrels score lowest: bury them deep in host rock, pack them in Bentonite, or treat the waste first. The old Y=0 rule is a config option.",
                "Desde una puntuación del 80%% un bulto ni irradia ni se filtra. Los barriles sin tratar puntúan lo más bajo: entiérralos hondo en roca firme, rodéalos de Bentonita o trata antes los residuos. La antigua regla de Y=0 es una opción de configuración.");
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
                "A Fluid Port on INPUT takes water as coolant...",
                "Un Puerto de fluidos en ENTRADA recibe agua como refrigerante...");
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
                "Withdrawn rods absorb fewer neutrons: the power rises until the hotter fuel and water hold it at a new level.",
                "Con las barras fuera se absorben menos neutrones: la potencia sube hasta que el combustible y el agua, más calientes, la frenan en un nivel nuevo.");
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
