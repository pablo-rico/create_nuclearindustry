package org.papiricoh.create_nuclearindustry.data;

/**
 * The satellites' screens: the command table's SATELLITES tab and its network panel, the hologram's satellite labels, the ground
 * station's screen and the launch controller's satellite fairing readouts.
 */
final class HoloSatelliteScreenLang {
    private HoloSatelliteScreenLang() {
    }

    static void add() {
        table();
        station();
        controller();
    }

    private static void table() {
        String key = "holo_table.create_nuclearindustry.screen.sat.";
        // The list's header band and sections.
        LangEntries.add(key + "no_station", "No ground station", "Sin estación terrestre");
        LangEntries.add(key + "station_no_code", "Station has no code", "Estación sin código");
        LangEntries.add(key + "station_offline", "Station not turning", "Estación parada");
        LangEntries.add(key + "network", "NET %s", "RED %s");
        LangEntries.add(key + "station", "GS%s", "ET%s");
        LangEntries.add(key + "station.online", "Ground station %s: online", "Estación terrestre %s: operativa");
        LangEntries.add(key + "station.offline", "Ground station %s: offline", "Estación terrestre %s: desconectada");
        LangEntries.add(key + "station.where", "At %s, %s, %s; click the cross to unlink it", "En %s, %s, %s; pulsa la cruz para desenlazarla");
        LangEntries.add(key + "own", "OWN SATELLITES (%s)", "SATÉLITES PROPIOS (%s)");
        LangEntries.add(key + "foreign", "FOREIGN, CATALOGUED (%s)", "AJENOS, CATALOGADOS (%s)");
        LangEntries.add(key + "launchers", "SATELLITE LAUNCHERS (%s)", "LANZADORES DE SATÉLITES (%s)");
        LangEntries.add(key + "none", "none", "ninguno");
        // Row cells.
        LangEntries.add(key + "kind_short.early_warning", "EW", "AT");
        LangEntries.add(key + "kind_short.comms", "COMMS", "COMS");
        LangEntries.add(key + "orbit_short.leo", "LEO", "LEO");
        LangEntries.add(key + "orbit_short.geo", "GEO", "GEO");
        LangEntries.add(key + "altitude", "%s %s m", "%s %s m");
        LangEntries.add(key + "over", "OVER", "ENCIMA");
        LangEntries.add(key + "forever", "∞", "∞");
        LangEntries.add(key + "empty", "empty", "vacía");
        LangEntries.add(key + "launcher", "S%s", "S%s");
        // Detail strip.
        LangEntries.add(key + "pick", "Pick a satellite or a launcher, in the list or on the map", "Elige un satélite o un lanzador, en la lista o en el mapa");
        LangEntries.add(key + "pick.hint", "Own satellites deorbit; foreign ones can be engaged", "Los propios se desorbitan; a los ajenos se les puede atacar");
        LangEntries.add(key + "detail", "%s · %s · %s · %s m", "%s · %s · %s · %s m");
        LangEntries.add(key + "over_now", "Over the table now", "Sobre la mesa ahora");
        LangEntries.add(key + "next_pass", "Next pass over the table in %s", "Próximo paso sobre la mesa en %s");
        LangEntries.add(key + "no_pass", "No pass over the table soon", "Sin paso sobre la mesa en breve");
        LangEntries.add(key + "decays", "decays in %s", "decae en %s");
        LangEntries.add(key + "stays", "stays up", "no decae");
        LangEntries.add(key + "pk", "interceptor Pk %s%%", "Pk del interceptor %s%%");
        LangEntries.add(key + "launcher_detail", "S%s · %s · reaches %s · %s", "S%s · %s · alcanza %s · %s");
        LangEntries.add(key + "reach_none", "no orbit", "ninguna órbita");
        LangEntries.add(key + "anchor_hint", "ANCHOR, then click the map over where the orbit starts", "ANCLAJE y haz clic en el mapa donde empieza la órbita");
        LangEntries.add(key + "anchor", "Anchor %s, %s", "Anclaje %s, %s");
        LangEntries.add(key + "heading_hint", "Anchor %s, %s · click again towards its heading", "Anclaje %s, %s · haz clic otra vez hacia su rumbo");
        LangEntries.add(key + "heading", "Anchor %s, %s · heading %s°", "Anclaje %s, %s · rumbo %s°");
        LangEntries.add(key + "parked", "Parked for good over %s, %s", "Fijo para siempre sobre %s, %s");
        LangEntries.add(key + "unreachable", "This rocket cannot reach that orbit", "Este cohete no alcanza esa órbita");
        // Keys.
        LangEntries.add(key + "anchor_button", "ANCHOR", "ANCLAJE");
        LangEntries.add(key + "anchor_on", "CLICK MAP", "CLIC EN MAPA");
        LangEntries.add(key + "orbit_leo", "ORBIT: LEO", "ÓRBITA: LEO");
        LangEntries.add(key + "orbit_geo", "ORBIT: GEO", "ÓRBITA: GEO");
        LangEntries.add(key + "heading_none", "HDG —", "RUMBO —");
        LangEntries.add(key + "heading_value", "HDG %s°", "RUMBO %s°");
        LangEntries.add(key + "launch", "LAUNCH", "LANZAR");
        LangEntries.add(key + "engage", "ENGAGE", "ATACAR");
        LangEntries.add(key + "clear", "CLEAR", "BORRAR");
        LangEntries.add(key + "deorbit", "DEORBIT", "DESORBITAR");
        LangEntries.add(key + "launch.tooltip", "Launches the selected launcher's satellite into the chosen orbit over the anchor. Needs a working ground station.",
                "Lanza el satélite del lanzador elegido a la órbita indicada sobre el anclaje. Necesita una estación terrestre operativa.");
        LangEntries.add(key + "engage.tooltip", "Sends an interceptor at the foreign satellite. Geostationary ones are out of reach.",
                "Lanza un interceptor contra el satélite ajeno. Los geoestacionarios están fuera de alcance.");
        LangEntries.add(key + "deorbit.tooltip", "Brings the table's own satellite down: it burns up.", "Hace caer el satélite propio: se desintegra.");
        LangEntries.add(key + "heading.tooltip", "A low orbit's heading: the second click on the map, or these keys in steps of 15°.",
                "Rumbo de una órbita baja: el segundo clic en el mapa, o estas teclas en pasos de 15°.");
        // The network panel.
        LangEntries.add(key + "uplink", "UPLINK", "ENLACE");
        LangEntries.add(key + "count", "%s SAT", "%s SAT");
        LangEntries.add(key + "network.tooltip", "The table's satellite network: its ground stations' link code; the lamp is lit while one works.",
                "La red de satélites de la mesa: el código de sus estaciones terrestres; la luz se enciende mientras una funciona.");
    }

    private static void station() {
        String key = "ground_station.create_nuclearindustry.screen.";
        LangEntries.add(key + "rpm", "%s RPM", "%s RPM");
        LangEntries.add(key + "rpm.tooltip", "The dish turns at %s RPM; it needs %s RPM to track.", "La antena gira a %s RPM; necesita %s RPM para seguir.");
        LangEntries.add(key + "needs_rpm", "Needs %s RPM to track", "Necesita %s RPM para seguir");
        LangEntries.add(key + "needs_code", "Set a complete link code", "Pon un código de enlace completo");
        LangEntries.add(key + "tracking", "Tracking its network", "Siguiendo a su red");
        LangEntries.add(key + "pass_now", "A satellite is overhead now", "Un satélite está encima ahora");
        LangEntries.add(key + "next_pass", "Soonest pass in %s", "Próximo paso en %s");
        LangEntries.add(key + "code.tooltip", "The network's link code: four characters, shared with its satellites, radars and command tables.",
                "El código de enlace de la red: cuatro caracteres, compartido con sus satélites, radares y mesas de mando.");
        LangEntries.add(key + "sky.tooltip", "The sky over the station: its network's satellites by bearing and distance, north up, the ring their horizon.",
                "El cielo sobre la estación: los satélites de su red por rumbo y distancia, el norte arriba, el anillo su horizonte.");
    }

    private static void controller() {
        String key = "silo.create_nuclearindustry.screen.satellite.";
        LangEntries.add(key + "fairing", "Fairing: %s", "Cofia: %s");
        LangEntries.add(key + "empty", "empty", "vacía");
        LangEntries.add(key + "reach", "Highest orbit: %s", "Órbita más alta: %s");
        LangEntries.add(key + "reach_none", "none, too little delta-v", "ninguna, poco delta-v");
        LangEntries.add(key + "launch_hint", "Launched from a command table's SATELLITES tab: anchor and orbit are chosen there.",
                "Se lanza desde la pestaña SATÉLITES de una mesa de mando: allí se eligen el anclaje y la órbita.");
        LangEntries.add(key + "gps", "GPS", "GPS");
    }
}
