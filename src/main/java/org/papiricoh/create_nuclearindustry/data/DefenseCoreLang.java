package org.papiricoh.create_nuclearindustry.data;

/** Interception messages: what a kill or a miss tells players (defense.create_nuclearindustry.*). */
final class DefenseCoreLang {
    private DefenseCoreLang() {
    }

    static void add() {
        LangEntries.add("defense.create_nuclearindustry.intercept", "Missile intercepted", "Misil interceptado");
        LangEntries.add("defense.create_nuclearindustry.interceptor_miss", "Interceptor missed", "El interceptor ha fallado");
    }
}
