package org.papiricoh.create_nuclearindustry.data;

/** Text of the rocket effects: sound subtitles ({@code subtitle.create_nuclearindustry.*}) and their own keys. */
public final class RocketFxLang {
    private static final String SUBTITLE = "subtitle.create_nuclearindustry.";

    private RocketFxLang() {
    }

    public static void add() {
        subtitle("rocket_ignition", "Rocket engines ignite", "Se encienden los motores del cohete");
        subtitle("rocket_roar", "Rocket engines roar", "Rugen los motores del cohete");
        subtitle("rocket_roar_distant", "Distant rocket rumbles", "Retumba un cohete a lo lejos");
        subtitle("stage_separation", "Rocket stage separates", "Se separa una etapa del cohete");
        subtitle("sonic_boom", "Sonic boom", "Estampido sónico");
        subtitle("reentry_hiss", "Warhead screams through the air", "Una ojiva silba al atravesar el aire");
        subtitle("debris_impact", "Wreckage crashes down", "Caen restos con estrépito");
        subtitle("silo_vent", "Silo vents steam", "El silo expulsa vapor");
        subtitle("radar_ping", "Radar pings", "Pitido del radar");
        subtitle("radar_new_track", "Radar acquires a track", "El radar detecta una traza");
        subtitle("radar_klaxon", "Radar threat alarm", "Alarma de amenaza del radar");
        subtitle("radar_ack", "Radar alarm acknowledged", "Alarma del radar reconocida");
        subtitle("holo_blip", "Holographic table blips", "Pitido de la mesa holográfica");
    }

    private static void subtitle(String sound, String english, String spanish) {
        LangEntries.add(SUBTITLE + sound, english, spanish);
    }
}
