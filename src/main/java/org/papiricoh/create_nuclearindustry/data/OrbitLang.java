package org.papiricoh.create_nuclearindustry.data;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.item;

/**
 * Names shared by satellites, their fairing, ground stations and the Tactical Linker.
 * Table commands live in HoloSatelliteLang.
 */
final class OrbitLang {
    private OrbitLang() {
    }

    static void add() {
        block("ground_station", "Satellite Ground Station", "Estación terrestre de satélites");
        block("rocket_satellite_fairing", "Rocket Satellite Fairing", "Cofia de satélite");
        item("early_warning_satellite", "Early Warning Satellite", "Satélite de alerta temprana");
        item("comms_satellite", "Communications Satellite", "Satélite de comunicaciones");
        item("incomplete_satellite", "Incomplete Satellite", "Satélite incompleto");
        LangEntries.add("item.create_nuclearindustry.tactical_linker.kind.ground_station", "Ground Station", "Estación terrestre");
        LangEntries.add("silo.create_nuclearindustry.section.satellite", "Satellite Fairing", "Cofia de satélite");
        LangEntries.add("silo.create_nuclearindustry.screen.section.satellite", "Satellite Fairing", "Cofia de satélite");
        LangEntries.add("silo.create_nuclearindustry.error.satellite", "The fairing holds no satellite (%s, %s, %s)", "La cofia no lleva satélite (%s, %s, %s)");
        LangEntries.add("satellite.create_nuclearindustry.kind.early_warning", "Early warning", "Alerta temprana");
        LangEntries.add("satellite.create_nuclearindustry.kind.comms", "Communications", "Comunicaciones");
        LangEntries.add("satellite.create_nuclearindustry.orbit.leo", "Low orbit", "Órbita baja");
        LangEntries.add("satellite.create_nuclearindustry.orbit.geo", "Geostationary", "Geoestacionaria");
    }
}
