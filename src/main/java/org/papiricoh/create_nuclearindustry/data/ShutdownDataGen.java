package org.papiricoh.create_nuclearindustry.data;

import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.add;

/**
 * Lang for the safe shutdown of a fission reactor: boric acid (soluble boron) and the water bled from
 * a shut-down core, the shutdown states, and the readings, alarms, goggles and guides that show them.
 * The Ponder scenes take their lines from {@code PlantPonderText}; the numbers quoted are the config
 * defaults.
 */
final class ShutdownDataGen extends AreaDataGen {
    static final ShutdownDataGen INSTANCE = new ShutdownDataGen();

    private ShutdownDataGen() {}

    @Override
    void lang() {
        LangEntries.fluid("boric_acid", "Boric Acid", "Ácido bórico");
        LangEntries.fluid("hot_water", "Hot Water", "Agua caliente");
        LangEntries.fluid("hot_heavy_water", "Hot Heavy Water", "Agua pesada caliente");
        states();
        controlRoom();
        displaySources();
        goggles();
        jei();
    }

    private static void states() {
        String s = "create_nuclearindustry.reactor.shutdown.";
        add(s + "operating", "Operating", "En operación");
        add(s + "subcritical", "Subcritical", "Subcrítico");
        add(s + "hot_shutdown", "Hot shutdown", "Parada en caliente");
        add(s + "cold_shutdown", "Cold shutdown", "Parada en frío");
    }

    private static void controlRoom() {
        String m = "create_nuclearindustry.alarm.metric.";
        add(m + "shutdown_margin_low", "Cold shutdown margin below", "Margen de parada en frío por debajo de");
        add(m + "cold_shutdown", "Reactor in a cold shutdown", "Reactor en parada en frío");
        add(m + "core_level_low", "Core under water below", "Núcleo cubierto de agua por debajo de");
        add(m + "decay_heat_unremoved", "Decay heat carried away below", "Calor residual evacuado por debajo de");
        String a = "create_nuclearindustry.annunciator.";
        add(a + "short.shutdown_margin_low", "SDM LO", "MARGEN BAJO");
        add(a + "short.cold_shutdown", "COLD SHUTDOWN", "PARADA EN FRÍO");
        add(a + "short.core_level_low", "CORE LEVEL LO", "NIVEL NÚC. BAJO");
        add(a + "short.decay_heat_unremoved", "DECAY HEAT", "CALOR RESIDUAL");
        String p = a + "preset.shutdown";
        add(p, "Shutdown", "Parada");
        String[] en = {"SCRAM", "SDM LO", "DECAY HEAT", "CORE LEVEL LO", "H2 HI", "COLD SHUTDOWN"};
        String[] es = {"SCRAM", "MARGEN BAJO", "CALOR RESIDUAL", "NIVEL NÚC. BAJO", "H2 ALTO", "PARADA EN FRÍO"};
        for (int n = 0; n < en.length; n++) {
            add(p + "." + (n + 1), en[n], es[n]);
        }

        String monitor = "create_nuclearindustry.monitor.";
        add(monitor + "shutdown_margin", "Cold margin", "Margen frío");
        add(monitor + "boron", "Boron", "Boro");

        String c = "create_nuclearindustry.control_module.";
        add(c + "reading.reactor_shutdown_margin", "Cold shutdown margin", "Margen de parada en frío");
        add(c + "reading.reactor_boron", "Dissolved boron", "Boro disuelto");
        add(c + "function.lamp_cold_shutdown", "Cold shutdown", "Parada en frío");
        add(c + "function.lamp_cold_shutdown.desc",
                "Lights while the reactor is in a cold shutdown: held off with margin, under 100 °C and its core under water.",
                "Se enciende mientras el reactor está en parada en frío: subcrítico con margen, por debajo de 100 °C y con el núcleo cubierto.");
    }

    private static void displaySources() {
        String d = "create_nuclearindustry.display_source.";
        add(d + "reactor_shutdown_state", "Reactor Shutdown State", "Estado de parada del reactor");
        add(d + "reactor_cold_margin", "Reactor Cold Shutdown Margin (pcm)", "Margen de parada en frío del reactor (pcm)");
        add(d + "reactor_boron", "Reactor Boron (ppm)", "Boro del reactor (ppm)");
        add(d + "reactor_core_level", "Reactor Core Under Water", "Núcleo del reactor cubierto de agua");
        add(d + "reactor_heat_removal", "Reactor Heat Removed / Decay Heat", "Calor evacuado / residual del reactor");
        add(d + "reactor_rods_inserted", "Reactor Rods In / Fuel Rods", "Barras dentro / barras de combustible del reactor");
        add(d + "reactor_building_hydrogen", "Reactor Building Hydrogen", "Hidrógeno en el edificio del reactor");
    }

    private static void goggles() {
        String f = "create_nuclearindustry.panel.fission.";
        add(f + "shutdown", "Shutdown", "Parada");
        add(f + "shutdown.state", "State", "Estado");
        add(f + "shutdown.margin", "Cold margin", "Margen en frío");
        add(f + "shutdown.boron", "Boron", "Boro");
        add(f + "shutdown.rods", "Rods", "Barras");
        add(f + "shutdown.heat", "Heat out / decay", "Calor evacuado / residual");
        add(f + "shutdown.heat_value", "%s / %s MW", "%s / %s MW");
        add(f + "term.boron", "Boron", "Boro");
        add(f + "core_uncovered", "Core uncovered: only %s%% under water", "Núcleo descubierto: solo el %s %% bajo el agua");
        add(f + "decay_unremoved", "Decay heat not carried away", "No se evacua el calor residual");
        add(f + "bleeding", "Bleeding hot water from the core", "Purgando agua caliente del núcleo");
        add(f + "bleeding_contaminated", "Bleeding contaminated water: treat it as waste",
                "Purgando agua contaminada: trátala como residuo");
        add(f + "building_hydrogen", "Hydrogen in the building: %s%%", "Hidrógeno en el edificio: %s %%");
        add("create_nuclearindustry.panel.unit.ppm", "%s ppm", "%s ppm");

        String port = "create_nuclearindustry.reactor.port.";
        add(port + "bleed", "Bleed: %s", "Purga: %s");
        add(port + "bleed.disabled", "turned off", "desactivada");
        add(port + "bleed.active", "bleeding hot water", "purgando agua caliente");
        add(port + "bleed.contaminated", "bleeding contaminated water", "purgando agua contaminada");
        add(port + "bleed.running", "only once the chain reaction is off", "solo con la reacción en cadena parada");
        add(port + "bleed.ready", "ready, once the steam runs out", "lista, cuando se acabe el vapor");
    }

    private static void jei() {
        String p = PlantJeiInfo.PREFIX;
        add(p + "safe_shutdown",
                "A scrammed core keeps making decay heat, a few percent of its power, for a long while. To shut it down for good, hold it subcritical and carry that heat away. Rods first: with at least as many inserted control rod blocks as fuel rod blocks, any core stays subcritical even cold, and rod-rich cores get stronger rods. Short of that, pour Boric Acid (Create mixing: water, borax salt and sulfuric acid) in through an input port: its boron holds the core down from the water (5 pcm per ppm) and stays when the water boils; only bleeding takes it out. Boiling never cools the water below 100 °C: once the chain reaction is off, feed cold water through an input port with a pump of its own and bleed the hot water out of an output port, which never bleeds the core dry. Send the Hot Water to a Cooling Tower, never straight back in; a damaged core bleeds Contaminated Water. Goggles, Display Links, desk lamps and the annunciator's Shutdown preset show the state: Subcritical (it will restart as it cools), Hot shutdown, then Cold shutdown, with 1000 pcm of margin, fuel and water under 100 °C and the core under water.",
                "Un núcleo en SCRAM sigue produciendo calor residual, un pequeño porcentaje de su potencia, durante bastante tiempo. Para apagarlo del todo hay que mantenerlo subcrítico y llevarse ese calor. Primero las barras: con al menos tantos bloques de barras de control insertados como bloques de barras de combustible, cualquier núcleo sigue subcrítico incluso en frío, y los núcleos con muchas barras tienen barras más fuertes. Si no llegan, echa Ácido bórico (mezcla de Create: agua, sal de bórax y ácido sulfúrico) por un puerto de entrada: su boro frena el núcleo desde el agua (5 pcm por ppm) y se queda aunque el agua hierva; solo la purga lo saca. Hervir nunca enfría el agua por debajo de 100 °C: con la reacción en cadena parada, alimenta agua fría por un puerto de entrada con una bomba propia y purga el agua caliente por un puerto de salida, que nunca deja el núcleo en seco. Manda el Agua caliente a una Torre de refrigeración, nunca directamente de vuelta; un núcleo dañado purga Agua contaminada. Las gafas, los Display Links, las lámparas del pupitre y el preajuste Parada del anunciador muestran el estado: Subcrítico (volverá a arrancar al enfriarse), Parada en caliente y por fin Parada en frío, con 1000 pcm de margen, combustible y agua por debajo de 100 °C y el núcleo cubierto.");
        add(p + "reactor_building_hydrogen",
                "Cladding hotter than 1200 °C reacts with steam and gives off hydrogen. A sealed containment around the reactor holds it, and its recombiners remove it. Without one, the hydrogen gathers in whatever closed room surrounds the reactor (up to 4096 blocks of air); it slowly leaks out of an ordinary building, and in open air it simply disperses. From 4% it may ignite and from 8% it always does, and a meltdown sets it off: ordinary walls hold only 1.5 bar, so the blast blows the roof off, burns whoever is inside, lets some activity of the damaged fuel fall out around the plant, sounds the sirens and is logged as an INES 3 incident (5 with damaged cladding), as at Fukushima. You can see it: pale wisps gather under the roof, thicker the closer it is to burning. Recombiners in the room help, and steam from a leak dilutes it too much to burn. Servers can turn it off (pwr.building.hydrogen).",
                "Las vainas a más de 1200 °C reaccionan con el vapor y desprenden hidrógeno. Una contención estanca alrededor del reactor lo retiene, y sus recombinadores lo eliminan. Sin ella, el hidrógeno se acumula en cualquier sala cerrada que rodee el reactor (hasta 4096 bloques de aire); se escapa poco a poco de un edificio normal, y al aire libre simplemente se dispersa. Desde el 4% puede prender, desde el 8% prende siempre, y una fusión del núcleo lo hace estallar: las paredes normales solo aguantan 1,5 bar, así que la explosión vuela el tejado, quema a quien esté dentro, esparce parte de la actividad del combustible dañado alrededor de la central, hace sonar las sirenas y queda registrada como incidente INES 3 (5 con las vainas dañadas), como en Fukushima. Se ve: volutas pálidas se acumulan bajo el techo, más espesas cuanto más cerca está de arder. Los recombinadores en la sala ayudan, y el vapor de una fuga lo diluye demasiado para arder. Los servidores pueden desactivarlo (pwr.building.hydrogen).");
    }
}
