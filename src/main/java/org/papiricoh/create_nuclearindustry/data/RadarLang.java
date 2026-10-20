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
        entry("scope.unlinked", "Link a radar", "Enlaza un radar");
        entry("scope.no_signal", "No signal", "Sin señal");
        entry("phase_short.boost", "BST", "IMP");
        entry("phase_short.midcourse", "MID", "CRU");
        entry("phase_short.reentry", "REN", "REE");
        entry("phase_short.terminal", "TRM", "TER");
        entry("phase_short.impact", "IMP", "IMP");
        screen("online", "ONLINE · %s TRK · %s THR", "OPERATIVO · %s TRZ · %s AMZ");
        screen("offline", "NO ROTATION", "SIN ROTACIÓN");
        screen("static", "STATIC ONLY", "SOLO ESTÁTICO");
        screen("range", "R %s", "R %s");
        screen("zone", "ZONE %s", "ZONA %s");
        screen("zone_any", "ZONE: ANY", "ZONA: TODO");
        screen("zone_edit", "SET ZONE", "FIJAR ZONA");
        screen("head.track", "TRK", "TRZ");
        screen("head.phase", "PH", "FS");
        screen("head.range", "RNG", "DIST");
        screen("head.eta", "ETA", "ETA");
        screen("filter.all", "ALL", "TODO");
        screen("filter.threat", "THREAT", "AMENAZA");
        screen("filter.boost", "BOOST", "IMPULSO");
        screen("ack", "ACK", "ENTERADO");
        screen("net", "NET", "RED");
        screen("no_tracks", "NO TRACKS", "SIN TRAZAS");
        screen("no_track", "NO TRACK SELECTED", "NINGUNA TRAZA");
        screen("threat", "THREAT", "AMENAZA");
        screen("alt", "ALT %s", "ALT %s");
        screen("spd", "SPD %s", "VEL %s");
        screen("apo", "APO %s", "APO %s");
        screen("eta", "ETA %s", "ETA %s");
        screen("impact", "IMPACT %s %s", "IMPACTO %s %s");
        screen("bearing", "BRG %s° RNG %s", "RUMBO %s° DIST %s");
        screen("sensors", "SENSORS %s · NET %s", "SENSORES %s · RED %s");
        screen("tip.zoom_out", "Zoom out (or scroll over the scope)", "Alejar (o rueda sobre la pantalla)");
        screen("tip.zoom_in", "Zoom in (or scroll over the scope)", "Acercar (o rueda sobre la pantalla)");
        screen("tip.centre", "Centre the scope on the radar again. Drag the scope to look elsewhere.",
                "Vuelve a centrar la pantalla en el radar. Arrastra la pantalla para mirar a otro sitio.");
        screen("tip.zone", "Defended zone: switch on, click the scope to centre it and scroll to size it. Its redstone output only answers tracks that come down in it; with no zone, every track.",
                "Zona defendida: actívala, haz clic en la pantalla para centrarla y usa la rueda para su tamaño. Su salida de redstone solo responde a trazas que caen en ella; sin zona, a todas.");
        screen("tip.filter", "Show every track, only threats to the zone, or only rockets still burning",
                "Muestra todas las trazas, solo las amenazas a la zona o solo los cohetes aún encendidos");
        screen("tip.ack", "Acknowledge: silences the klaxon. The redstone output stays on while the threat lasts.",
                "Enterado: silencia la sirena. La salida de redstone sigue activa mientras dure la amenaza.");
        screen("tip.code", "Network code: radars with the same four letters or digits share what they see",
                "Código de red: los radares con las mismas cuatro letras o cifras comparten lo que ven");
        screen("tip.sort", "Sort by name, range or time to impact", "Ordena por nombre, distancia o tiempo hasta el impacto");
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
    private static void screen(String key, String en, String es) { entry("screen." + key, en, es); }
    private static void entry(String key, String en, String es) { LangEntries.add("radar.create_nuclearindustry." + key, en, es); }
    private RadarLang() {}
}
