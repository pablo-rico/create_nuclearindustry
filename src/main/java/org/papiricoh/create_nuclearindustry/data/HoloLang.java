package org.papiricoh.create_nuclearindustry.data;

/** The holographic command table's vocabulary: its block, its hologram's labels, its screen and its answers when designating. */
final class HoloLang {
    static void add() {
        LangEntries.block("holo_table", "Holographic Command Table", "Mesa holográfica de mando");
        entry("designate.ok", "Silo targeted at %s, %s", "Silo apuntado a %s, %s");
        entry("designate.too_far", "Stand at the table to designate", "Ponte junto a la mesa para designar");
        entry("designate.broken", "The table is not whole", "La mesa no está completa");
        entry("designate.no_silo", "No silo linked: click one with a Tactical Linker, then the table",
                "Sin silo enlazado: haz clic en uno con un enlazador táctico y luego en la mesa");
        entry("designate.other_dimension", "The silo is in another dimension", "El silo está en otra dimensión");
        entry("designate.unloaded", "The silo is not loaded", "El silo no está cargado");
        entry("designate.out_of_reach", "%s, %s is beyond the silo's rocket", "%s, %s queda fuera del alcance del cohete del silo");
        entry("designate.off_table", "%s, %s is beyond the table's map", "%s, %s queda fuera del mapa de la mesa");
        entry("designate.limits", "%s, %s is outside the server's launch limits", "%s, %s queda fuera de los límites de lanzamiento del servidor");
        entry("marked", "Target marked at %s, %s: sneak and click the mark again to send the silo",
                "Blanco marcado en %s, %s: agáchate y vuelve a hacer clic en la marca para enviárselo al silo");
        entry("zoom", "View: %s km", "Vista: %s km");
        entry("zoom.auto", "View: automatic", "Vista: automática");
        entry("label.speed", "%s m/s · ETA %s s", "%s m/s · ETA %s s");
        entry("label.altitude", "ALT %s m", "ALT %s m");
        entry("label.impact", "Impact %s, %s", "Impacto %s, %s");
        entry("label.pinned", "PINNED", "FIJADA");
        entry("label.silo", "Silo · %s", "Silo · %s");
        entry("label.silo_reach", "Reach %s m · CEP %s m", "Alcance %s m · CEP %s m");
        entry("label.radar", "Radar %s", "Radar %s");
        entry("label.radar_reach", "Coverage %s m", "Cobertura %s m");
        entry("label.radar_off", "Offline", "Fuera de servicio");
        entry("label.no_code", "no code", "sin código");
        entry("label.launch_site", "Launch site of %s", "Lanzamiento de %s");
        entry("label.mark_ok", "Sneak-click to confirm", "Agachado + clic para confirmar");
        screen("auto", "auto", "auto");
        screen("tracks", "TRACKS", "TRAZAS");
        screen("none", "No tracks", "Sin trazas");
        screen("silo", "SILO", "SILO");
        screen("no_silo", "Not linked", "Sin enlazar");
        screen("reach", "Reach %s m", "Alcance %s m");
        screen("cep", "CEP %s m", "CEP %s m");
        screen("designate", "DESIGNATE", "DESIGNAR");
        screen("designate_on", "CLICK THE MAP", "CLIC EN EL MAPA");
        screen("airburst", "AIR BURST", "EN EL AIRE");
        screen("groundburst", "GROUND BURST", "EN SUPERFICIE");
        screen("auto_view", "AUTO VIEW", "VISTA AUTO");
    }

    private static void screen(String key, String en, String es) {
        entry("screen." + key, en, es);
    }

    private static void entry(String key, String en, String es) {
        LangEntries.add("holo_table.create_nuclearindustry." + key, en, es);
    }

    private HoloLang() {
    }
}
