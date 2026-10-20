package org.papiricoh.create_nuclearindustry.data;

/** Lang of the Tactical Linker and the command table's commands (stream S4). */
final class TacticalLang {
    private TacticalLang() {}

    static void add() {
        // The item name is supplied by MissileDataGen; these keys belong to the linker workflow.
        linker("empty", "Click a radar or launch controller first", "Haz clic primero en un radar o controlador de lanzamiento");
        linker("stored", "Missile device stored; click a command table or radar scope", "Dispositivo guardado; haz clic en una mesa de mando o pantalla de radar");
        linker("cleared", "Stored missile link cleared", "Enlace de misil guardado borrado");
        linker("unlinked", "All device links cleared", "Todos los enlaces del dispositivo borrados");
        linker("added", "Missile device linked", "Dispositivo de misiles enlazado");
        linker("removed", "Missile device unlinked", "Dispositivo de misiles desenlazado");
        linker("full", "No free links: unlink a device first", "Sin enlaces libres: desenlaza primero un dispositivo");
        linker("unsupported", "Not a compatible missile device", "No es un dispositivo de misiles compatible");
        linker("out_of_range", "Missile device is too far away or in another dimension", "El dispositivo está demasiado lejos o en otra dimensión");
        linker("target", "%s at %s, %s, %s · %s", "%s en %s, %s, %s · %s");
        linker("kind.radar", "Radar", "Radar");
        linker("kind.launch_controller", "Launch Controller", "Controlador de lanzamiento");
        LangEntries.add("item.create_nuclearindustry.control_room_linker.use_tactical", "Use a Tactical Linker for missile devices", "Usa un enlazador táctico para los dispositivos de misiles");
        command("ok", "Command accepted", "Orden aceptada");
        command("too_far", "Stand at the command table", "Ponte junto a la mesa de mando");
        command("broken", "The command table is incomplete", "La mesa de mando está incompleta");
        command("not_linked", "Device is not linked to this table", "El dispositivo no está enlazado a esta mesa");
        command("unloaded", "Launch controller is missing or unloaded", "El controlador falta o no está cargado");
        command("refused", "Command refused: check the selected silo's error", "Orden rechazada: revisa el error del silo seleccionado");
        command("no_target", "Select a target before arming", "Selecciona un blanco antes de armar");
        command("too_long", "Salvo needs more launch delay than the server allows", "La salva necesita más retardo de lanzamiento del permitido");
        command("not_own", "This flight does not belong to a linked launch controller", "Este vuelo no pertenece a un controlador enlazado");
        command("gone", "The flight has already ended", "El vuelo ya ha terminado");
        screen("silos", "SILOS", "SILOS");
        screen("radars", "RADARS", "RADARES");
        screen("silo_header", "SALVO / SILO / STATE / DELAY", "SALVA / SILO / ESTADO / RETARDO");
        screen("radar_header", "RADAR / NETWORK", "RADAR / RED");
        screen("track_header", "TRACK / ETA", "TRAZA / LLEGADA");
        screen("unloaded", "Unloaded", "Sin cargar");
        screen("online", "Online", "En servicio");
        screen("time_on_target", "SAME IMPACT", "SIMULTÁNEA");
        screen("sequence", "SEQUENCE", "SECUENCIA");
        screen("arm", "ARM", "ARMAR");
        screen("disarm", "DISARM", "SEGURO");
        screen("salvo", "SALVO", "SALVA");
        screen("abort", "ABORT", "ABORTAR");
        screen("flight_delay", "Flight %s s · delay %s s", "Vuelo %s s · retardo %s s");
        screen("target", "Target %s, %s", "Blanco %s, %s");
        screen("track_detail", "%s · altitude %s m", "%s · altitud %s m");
        screen("own", "Own flight: may self-destruct", "Vuelo propio: se puede autodestruir");
        screen("foreign", "Foreign flight", "Vuelo ajeno");
        screen("redstone", "The table only arms and sets launch delays. Each controller needs a new redstone signal to launch. Sequence order follows the order you tick the silos.",
                "La mesa solo arma y fija retardos. Cada controlador necesita una señal nueva de redstone para lanzar. La secuencia sigue el orden en que marcas los silos.");
    }

    private static void linker(String key, String en, String es) { LangEntries.add("item.create_nuclearindustry.tactical_linker." + key, en, es); }
    private static void command(String key, String en, String es) { LangEntries.add("holo_table.create_nuclearindustry.command." + key, en, es); }
    private static void screen(String key, String en, String es) { LangEntries.add("holo_table.create_nuclearindustry.screen." + key, en, es); }
}
