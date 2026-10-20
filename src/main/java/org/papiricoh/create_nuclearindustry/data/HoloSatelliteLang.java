package org.papiricoh.create_nuclearindustry.data;

/** Satellite table commands, shared with the orbital picture. */
final class HoloSatelliteLang {
    private HoloSatelliteLang() { }

    static void add() {
        LangEntries.add("holo_table.create_nuclearindustry.screen.satellites", "SATELLITES", "SATÉLITES");
        String command = "holo_table.create_nuclearindustry.command.";
        LangEntries.add(command + "no_station", "No working ground station linked", "Ninguna estación terrestre operativa enlazada");
        LangEntries.add(command + "no_satellite", "No such satellite", "No existe ese satélite");
        LangEntries.add(command + "no_orbit", "That rocket cannot reach that orbit", "Ese cohete no alcanza esa órbita");
        LangEntries.add(command + "orbit_full", "Too many satellites on this network", "Demasiados satélites en esta red");
        LangEntries.add(command + "out_of_bounds", "Beyond the world border", "Fuera del borde del mundo");
    }
}
