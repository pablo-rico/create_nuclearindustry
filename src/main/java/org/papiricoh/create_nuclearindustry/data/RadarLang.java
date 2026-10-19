package org.papiricoh.create_nuclearindustry.data;

/** Radar vocabulary shared by its screen, goggles and Display Link. */
public final class RadarLang {
    public static void add() {
        LangEntries.block("missile_radar", "Missile Radar", "Radar de misiles");
        entry("online", "Online · dimension coverage", "Operativo · cobertura dimensional");
        entry("offline", "Offline · rotation required", "Apagado · necesita rotación");
        entry("static_only", "Static installation required", "Requiere instalación estática");
        entry("contacts", "Contacts: %s", "Contactos: %s");
        entry("impact", "Impact: %s s", "Impacto: %s s");
        entry("origin", "Origin: %s", "Origen: %s");
        entry("unknown", "Unknown", "Desconocido");
        entry("position", "Position X/Y/Z: %s", "Posición X/Y/Z: %s");
        entry("velocity", "Velocity X/Y/Z: %s", "Velocidad X/Y/Z: %s");
        entry("target", "Predicted impact: %s", "Impacto previsto: %s");
    }
    private static void entry(String key, String en, String es) { LangEntries.add("radar.create_nuclearindustry." + key, en, es); }
    private RadarLang() {}
}
