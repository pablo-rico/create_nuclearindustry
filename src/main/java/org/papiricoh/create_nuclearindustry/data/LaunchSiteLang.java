package org.papiricoh.create_nuclearindustry.data;

/** Lang of the launch site: base, clearance and fuel ports (stream S1). */
final class LaunchSiteLang {
    private LaunchSiteLang() {}

    static void add() {
        LangEntries.add("silo.create_nuclearindustry.error.base", "Launch base incomplete: place launch base blocks in a 3×3 beside the controller (%s, %s, %s)", "Base de lanzamiento incompleta: coloca bloques de base en un 3×3 junto al controlador (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.clearance", "Rocket clearance obstructed (%s, %s, %s)", "Espacio libre del cohete obstruido (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.site.instructions", "Place one controller per base. Walls are optional; fuel ports may touch any side or sit underneath.", "Coloca un controlador por base. Las paredes son opcionales; los puertos de combustible pueden ir en cualquier lado o debajo.");
    }
}
