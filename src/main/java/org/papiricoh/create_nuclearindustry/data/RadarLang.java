package org.papiricoh.create_nuclearindustry.data;

/** Radar vocabulary shared by its screen, goggles, Display Link and the control room. */
public final class RadarLang {
    public static void add() {
        LangEntries.block("missile_radar", "Missile Radar", "Radar de misiles");
        entry("online", "Online · tracking", "Operativo · en seguimiento");
        entry("offline", "Offline · rotation required", "Apagado · necesita rotación");
        entry("static_only", "Static installation required", "Requiere instalación estática");
        entry("contacts", "Tracks: %s", "Trazas: %s");
        entry("threats", "Threats: %s · first impact in %s s", "Amenazas: %s · primer impacto en %s s");
        entry("network", "Network %s", "Red %s");
        entry("no_network", "No network code", "Sin código de red");
        entry("clear", "No tracks", "Sin trazas");
        entry("track_line", "%s %s s %s %s", "%s %s s %s %s");
        entry("impact", "Impact: %s s", "Impacto: %s s");
        entry("origin", "Origin: %s", "Origen: %s");
        entry("unknown", "Unknown", "Desconocido");
        entry("position", "Position X/Y/Z: %s", "Posición X/Y/Z: %s");
        entry("velocity", "Velocity X/Y/Z: %s", "Velocidad X/Y/Z: %s");
        entry("target", "Predicted impact: %s", "Impacto previsto: %s");
        entry("phase.boost", "BOOST", "IMPULSO");
        entry("phase.midcourse", "MIDCOURSE", "CRUCERO");
        entry("phase.reentry", "REENTRY", "REENTRADA");
        entry("phase.terminal", "TERMINAL", "TERMINAL");
        entry("phase.impact", "IMPACT", "IMPACTO");
        String source = "create_nuclearindustry.display_source.";
        LangEntries.add(source + "missile_radar", "Radar Summary", "Resumen del radar");
        LangEntries.add(source + "missile_radar_tracks", "Radar Tracks", "Trazas del radar");
        LangEntries.add(source + "missile_radar_threats", "Radar Threats", "Amenazas del radar");
        // Control room: the radar as a target, its alarms, monitor rows and console gauges.
        LangEntries.add("create_nuclearindustry.control_room.kind.radar", "Missile radar", "Radar de misiles");
        LangEntries.add("create_nuclearindustry.alarm.metric.radar_track", "Radar track", "Traza de radar");
        LangEntries.add("create_nuclearindustry.alarm.metric.radar_threat", "Radar threat to zone", "Amenaza de radar a la zona");
        String monitor = "create_nuclearindustry.monitor.";
        LangEntries.add(monitor + "radar_tracks", "Tracks", "Trazas");
        LangEntries.add(monitor + "radar_threats", "Threats", "Amenazas");
        LangEntries.add(monitor + "radar_eta", "First impact", "Primer impacto");
        LangEntries.add(monitor + "radar_range", "Reach", "Alcance");
        LangEntries.add(monitor + "radar_offline", "offline", "apagado");
        LangEntries.add(monitor + "radar_all", "whole world", "todo el mundo");
        String reading = "create_nuclearindustry.control_module.reading.";
        LangEntries.add(reading + "radar_tracks", "Radar tracks", "Trazas de radar");
        LangEntries.add(reading + "radar_threats", "Radar threats", "Amenazas de radar");
    }
    private static void entry(String key, String en, String es) { LangEntries.add("radar.create_nuclearindustry." + key, en, es); }
    private RadarLang() {}
}
