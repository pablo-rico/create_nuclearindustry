package org.papiricoh.create_nuclearindustry.data;

/** Text of the satellites' effects: the subtitles of their sounds. */
final class OrbitFxLang {
    private static final String SUBTITLE = "subtitle.create_nuclearindustry.";

    private OrbitFxLang() {
    }

    static void add() {
        LangEntries.add(SUBTITLE + "fairing_separation", "Satellite fairing separates", "Se separa la cofia del satélite");
        LangEntries.add(SUBTITLE + "satellite_breakup", "Satellite breaks up", "Un satélite se desintegra");
    }
}
