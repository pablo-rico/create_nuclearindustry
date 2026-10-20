package org.papiricoh.create_nuclearindustry.data;

/** The command table's defence tab and in-world defence picture. */
final class HoloDefenseLang {
    private HoloDefenseLang() {
    }

    static void add() {
        String key = "holo_table.create_nuclearindustry.screen.";
        String[] states = {"unpowered", "idle", "tracking", "slewing", "reloading", "empty", "hold", "on_ship"};
        String[] en = {"Unpowered", "Idle", "Tracking", "Slewing", "Reloading", "Empty", "Hold", "On ship"};
        String[] es = {"Sin energía", "En espera", "Siguiendo", "Girando", "Recargando", "Vacía", "Retenida", "En nave"};
        for (int i = 0; i < states.length; i++) LangEntries.add(key + "defense.status." + states[i], en[i], es[i]);
        LangEntries.add(key + "defense", "DEFENSE", "DEFENSA");
        LangEntries.add(key + "column.shooter", "SHOOTER", "TIRADOR");
        LangEntries.add(key + "defense.pin", "Pin a radar track", "Fija una pista de radar");
        LangEntries.add(key + "defense.no_estimate", "Engage a tracked missile to see Pk / TTI", "Ataca un misil seguido para ver Pk / TTI");
        LangEntries.add(key + "defense.in_flight", "Interceptor in flight", "Interceptor en vuelo");
        LangEntries.add(key + "defense.estimate", "%s · Pk %s%% · TTI %s s", "%s · Pk %s%% · TTI %s s");
        LangEntries.add(key + "defense.pk", "Pk %s%%", "Pk %s%%");
        LangEntries.add(key + "defense.shoot_look", "SHOOT / LOOK", "DISPARAR / MIRAR");
        LangEntries.add(key + "defense.salvo_two", "SALVO OF 2", "SALVA DE 2");
        LangEntries.add(key + "defense.default", "DEFAULT", "PREDEFINIDO");
        LangEntries.add(key + "defense.engage", "ENGAGE", "ATACAR");
        LangEntries.add(key + "defense.auto", "AUTO", "AUTO");
        LangEntries.add(key + "defense.cease", "CEASE", "CESAR");
    }
}
