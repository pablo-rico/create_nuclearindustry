package org.papiricoh.create_nuclearindustry.data;

/** The interceptor battery's screen, goggles and messages (battery.create_nuclearindustry.*). */
final class BatteryLang {
    private static final String KEY = "battery.create_nuclearindustry.";

    private BatteryLang() {
    }

    private static void add(String key, String english, String spanish) {
        LangEntries.add(KEY + key, english, spanish);
    }

    static void add() {
        // What it is doing, by Status
        add("unpowered", "Not turning", "Sin giro");
        add("idle", "Standing by", "En espera");
        add("tracking", "Engaging", "Atacando");
        add("slewing", "Turning to target", "Girando hacia el blanco");
        add("reloading", "Reloading", "Recargando");
        add("empty", "No rounds", "Sin munición");
        add("hold", "Held by redstone", "Retenida por redstone");
        add("on_ship", "Cannot work on a ship", "No funciona en una nave");
        add("doctrine.shoot_look_shoot", "1 at a time", "De 1 en 1");
        add("doctrine.salvo_two", "2 at once", "De 2 en 2");

        // Goggles
        add("rounds", "%s ready, %s reloading", "%s listas, %s recargando");
        add("network", "Radar network %s", "Red de radares %s");
        add("no_network", "No radar link code", "Sin código de enlace de radar");
        add("goggles.auto", "Automatic, %s", "Automática, %s");
        add("goggles.manual", "Manual: fires only on a command table's order (%s)", "Manual: solo dispara por orden de una mesa (%s)");
        add("goggles.zone_default", "Defends up to %s blocks round itself", "Defiende hasta %s bloques a su alrededor");
        add("goggles.zone", "Defends %s blocks round %s, %s", "Defiende %s bloques alrededor de %s, %s");

        // Screen
        add("screen.code", "CODE", "CÓDIGO");
        add("screen.code.tooltip", "The four-character link code of the radars it takes its picture from. Without one it only fires on a linked command table's orders.",
                "El código de enlace de cuatro caracteres de los radares de los que toma la imagen. Sin código solo dispara por orden de una mesa de mando enlazada.");
        add("screen.auto.on", "AUTO", "AUTO");
        add("screen.auto.off", "MANUAL", "MANUAL");
        add("screen.auto.tooltip", "Automatic: engages on its own every missile its radars see heading for its zone, unless redstone holds it. Manual: only a command table's orders.",
                "Automática: ataca por sí sola todo misil que sus radares vean ir hacia su zona, salvo que la retenga una señal de redstone. Manual: solo órdenes de una mesa de mando.");
        add("screen.doctrine.tooltip", "How many interceptors go up at a missile: one, and another only if it misses with time left, or two at once.",
                "Cuántos interceptores salen contra un misil: uno, y otro solo si falla y queda tiempo, o dos a la vez.");
        add("screen.zone.tooltip", "The defended zone's radius: it engages only missiles predicted to come down within it. Default: its own reach.",
                "El radio de la zona defendida: solo ataca misiles que vayan a caer dentro. Por defecto: su propio alcance.");
        add("screen.zone_here.tooltip", "Centre the defended zone on the battery.", "Centra la zona defendida en la batería.");
        add("screen.zone.default", "%s (own)", "%s (propio)");
        add("screen.zone.blocks", "%s", "%s");
        add("screen.tube.empty", "empty", "vacío");
        add("screen.tube.loading", "loading %ss", "cargando %ss");
        add("screen.tube.ready", "ready", "listo");
        add("screen.needs_rpm", "Needs rotation from a shaft below", "Necesita rotación de un eje por debajo");
        add("screen.held", "Automatic fire held", "Fuego automático retenido");
        add("screen.no_code", "No radar code: table orders only", "Sin código: solo órdenes de mesa");
        add("screen.on_network", "Radar network %s", "Red de radares %s");
    }
}
