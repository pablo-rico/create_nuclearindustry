package org.papiricoh.create_nuclearindustry.data;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.item;

/**
 * Names in missile defence shared by its pieces: the battery, its rounds, the kill vehicle, the Tactical Linker's new kind and the
 * command table's new command results. Each piece's own vocabulary lives in its own class.
 */
final class DefenseLang {
    private DefenseLang() {
    }

    static void add() {
        block("interceptor_battery", "Interceptor Battery", "Batería de interceptores");
        block("interceptor_battery_part", "Interceptor Battery", "Batería de interceptores");
        item("interceptor", "Interceptor Missile", "Misil interceptor");
        item("incomplete_interceptor", "Incomplete Interceptor Missile", "Misil interceptor incompleto");
        block("rocket_kill_vehicle", "Rocket Kill Vehicle", "Vehículo de intercepción");
        LangEntries.add("item.create_nuclearindustry.tactical_linker.kind.interceptor_battery", "Interceptor Battery", "Batería de interceptores");
        String command = "holo_table.create_nuclearindustry.command.";
        LangEntries.add(command + "no_shooter", "No working interceptor launcher", "Ningún lanzador de interceptores operativo");
        LangEntries.add(command + "not_tracked", "No linked radar tracks that missile", "Ningún radar enlazado sigue ese misil");
        LangEntries.add(command + "no_solution", "No launcher can reach that missile in time", "Ningún lanzador alcanza ese misil a tiempo");
        LangEntries.add(command + "friendly", "That missile is one of ours", "Ese misil es nuestro");
        LangEntries.add(command + "no_ammo", "No interceptors loaded", "No hay interceptores cargados");
        LangEntries.add(command + "already_engaged", "Interceptors are already on their way", "Ya hay interceptores en camino");
    }
}
