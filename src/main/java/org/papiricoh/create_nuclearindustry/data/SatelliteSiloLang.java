package org.papiricoh.create_nuclearindustry.data;

/** Satellite controller and ground station readouts. */
final class SatelliteSiloLang {
    private SatelliteSiloLang() {}
    static void add() {
        LangEntries.add("item.create_nuclearindustry.rocket_section.satellite", "Holds one satellite; launch it from a command table.", "Contiene un satélite; lánzalo desde una mesa de mando.");
        LangEntries.add("silo.create_nuclearindustry.screen.slot.satellite", "Satellite", "Satélite");
        LangEntries.add("silo.create_nuclearindustry.screen.slot.short.satellite", "S", "S");
        String silo = "silo.create_nuclearindustry.satellite.";
        LangEntries.add(silo + "leo", "Satellite ready: reaches low orbit", "Satélite listo: alcanza órbita baja");
        LangEntries.add(silo + "geo", "Satellite ready: reaches geostationary orbit", "Satélite listo: alcanza órbita geoestacionaria");
        LangEntries.add(silo + "no_orbit", "Satellite ready: insufficient orbital delta-v", "Satélite listo: delta-v orbital insuficiente");
        LangEntries.add(silo + "countdown", "Launching satellite T-%s", "Lanzando satélite T-%s");
        LangEntries.add(silo + "controlled", "Controlled by a command table", "Controlado por una mesa de mando");
        LangEntries.add(silo + "gps", "Satellite GPS guidance active", "Guiado GPS por satélite activo");
        String station = "ground_station.create_nuclearindustry.";
        LangEntries.add(station + "online", "Ground station online", "Estación terrestre operativa");
        LangEntries.add(station + "offline", "Ground station offline", "Estación terrestre desconectada");
        LangEntries.add(station + "code", "Network: %s", "Red: %s");
        LangEntries.add(station + "code_label", "Network code", "Código de red");
        LangEntries.add(station + "satellites", "Satellites: %s", "Satélites: %s");
        LangEntries.add(station + "no_pass", "No pass in the next day", "Sin paso durante el próximo día");
        LangEntries.add(station + "next_pass", "Next pass in %s", "Próximo paso en %s");
        LangEntries.add(station + "overhead", "Satellite overhead", "Satélite a la vista");
    }
}
