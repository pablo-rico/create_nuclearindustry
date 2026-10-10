package org.papiricoh.create_nuclearindustry.data;

/** Lang, models, tags, data maps, loot and recipes for the tokamak core: plasma physics, startup sequence, controller and its fluids. */
final class FusionDataGen extends AreaDataGen {
    static final FusionDataGen INSTANCE = new FusionDataGen();

    private static final String T = "create_nuclearindustry.tokamak.";
    private static final String GUI = "gui.create_nuclearindustry.tokamak.";

    private FusionDataGen() {}

    @Override
    void lang() {
        LangEntries.fluid("helium", "Helium", "Helio");
        LangEntries.fluid("liquid_helium", "Liquid Helium", "Helio líquido");
        LangEntries.fluid("liquid_air", "Liquid Air", "Aire líquido");
        LangEntries.fluid("liquid_nitrogen", "Liquid Nitrogen", "Nitrógeno líquido");

        LangEntries.add(T + "phase.offline", "Offline", "Apagado");
        LangEntries.add(T + "phase.pump_down", "Pumping down", "Haciendo vacío");
        LangEntries.add(T + "phase.cool_down", "Cooling magnets", "Enfriando imanes");
        LangEntries.add(T + "phase.energize", "Energizing field", "Energizando campo");
        LangEntries.add(T + "phase.breakdown", "Breakdown", "Ruptura del gas");
        LangEntries.add(T + "phase.heating", "Heating", "Calentando");
        LangEntries.add(T + "phase.burn", "Burning", "En combustión");
        LangEntries.add(T + "phase.ramp_down", "Ramping down", "Bajando en rampa");

        LangEntries.add(T + "trip.none", "None", "Ninguno");
        LangEntries.add(T + "trip.operator", "Operator shutdown", "Parada del operador");
        LangEntries.add(T + "trip.vacuum_lost", "Vacuum lost", "Vacío perdido");
        LangEntries.add(T + "trip.magnets_warm", "Magnets too warm", "Imanes demasiado calientes");
        LangEntries.add(T + "trip.no_field", "Too little magnetic field", "Campo magnético insuficiente");
        LangEntries.add(T + "trip.no_fuel", "No fuel", "Sin combustible");
        LangEntries.add(T + "trip.structure", "Structure changed", "Estructura alterada");
        LangEntries.add(T + "trip.disruption", "Plasma disruption", "Disrupción del plasma");

        LangEntries.add("subtitle.create_nuclearindustry.magnet_hum", "Magnets hum", "Zumban los imanes");
        LangEntries.add("subtitle.create_nuclearindustry.plasma_rumble", "Plasma roars", "Ruge el plasma");
        LangEntries.add("subtitle.create_nuclearindustry.plasma_breakdown", "Plasma strikes", "Se enciende el plasma");
        LangEntries.add("subtitle.create_nuclearindustry.cryoplant_hum", "Cryogenic plant runs", "Funciona la planta criogénica");

        structure();
        messages();
        goggles();
        screen();
    }

    private static void structure() {
        LangEntries.add(T + "error.not_formed", "Not formed", "Sin formar");
        LangEntries.add(T + "error.not_found",
                "No tokamak found: place the controller on the outer equator, facing a 3-block plasma chamber, with the central solenoid at the axis",
                "No se encuentra el tokamak: pon el controlador en el ecuador exterior, mirando a una cámara de plasma de 3 bloques, con el solenoide central en el eje");
        LangEntries.add(T + "error.legacy_ring", "This is an old flat fusion ring: rebuild it as a tokamak (see the Ponder scene)",
                "Esto es un antiguo anillo de fusión plano: reconstrúyelo como tokamak (mira la escena de Ponder)");
        LangEntries.add(T + "error.solenoid", "Central solenoid incomplete at %s", "Solenoide central incompleto en %s");
        LangEntries.add(T + "error.chamber_blocked", "Plasma chamber blocked at %s", "Cámara de plasma obstruida en %s");
        LangEntries.add(T + "error.invalid_shell", "Invalid shell block at %s", "Bloque de carcasa no válido en %s");
        LangEntries.add(T + "error.no_magnet_drive", "Missing magnet drive", "Falta una toma de imanes");
        LangEntries.add(T + "error.no_fuel_injector", "Missing fuel injector set to INPUT", "Falta un inyector de combustible en ENTRADA");
        LangEntries.add(T + "error.no_coolant_port", "Missing coolant input port", "Falta un puerto de entrada de refrigerante");
        LangEntries.add(T + "error.no_steam_port", "Missing plasma steam output port", "Falta un puerto de salida de vapor de plasma");
        LangEntries.add(T + "warning.unstable",
                "Too few electromagnets (%s%% coverage): a hot plasma needs %s%% or it grows unstable",
                "Faltan electroimanes (%s%% de cobertura): un plasma caliente necesita un %s%% o se vuelve inestable");
        LangEntries.add(T + "warning.no_burn",
                "With %s%% coil coverage even %s MW of heating cannot bring the plasma to a burn",
                "Con un %s%% de cobertura de bobinas ni %s MW de calentamiento llevan el plasma a la combustión");
        LangEntries.add(T + "warning.no_blankets", "No breeding blankets: tritium will not be replaced",
                "Sin mantos regeneradores: el tritio no se repondrá");
    }

    private static void messages() {
        LangEntries.add(T + "message.formed", "Tokamak formed [%s]", "Tokamak formado [%s]");
        LangEntries.add(T + "message.broken", "Tokamak broken: %s", "Tokamak roto: %s");
        LangEntries.add(T + "message.incomplete", "Tokamak incomplete: %s", "Tokamak incompleto: %s");
        LangEntries.add(T + "message.starting", "Tokamak starting: pumping down the chamber", "Arranque del tokamak: haciendo vacío en la cámara");
        LangEntries.add(T + "message.start_blocked", "The tokamak cannot start: %s", "El tokamak no puede arrancar: %s");
        LangEntries.add(T + "message.trip", "Tokamak trip: %s. The plasma is shut down safely",
                "Disparo del tokamak: %s. El plasma se apaga de forma segura");
        LangEntries.add(T + "message.shutdown", "Tokamak shutting down", "El tokamak se está apagando");
        LangEntries.add(T + "message.offline", "Tokamak offline", "Tokamak apagado");
        LangEntries.add(T + "message.burning", "The tokamak plasma is burning", "El plasma del tokamak está en combustión");
        LangEntries.add(T + "message.disruption", "Plasma disruption at %s!", "¡Disrupción del plasma en %s!");
        LangEntries.add(T + "message.upgraded",
                "This tokamak was running in an older version and has been shut down. It now needs vacuum pumps, cryogenic ports with liquid helium and a Start (screen, redstone pulse or desk lever).",
                "Este tokamak funcionaba en una versión anterior y se ha apagado. Ahora necesita bombas de vacío, puertos criogénicos con helio líquido y un arranque (pantalla, pulso de redstone o palanca del pupitre).");
    }

    private static void goggles() {
        String g = T + "goggles.";
        LangEntries.add(g + "title", "Tokamak", "Tokamak");
        LangEntries.add(g + "structure", "R=%s, %s coils (%s%% coverage), %s blankets", "R=%s, %s bobinas (%s%% de cobertura), %s mantos");
        LangEntries.add(g + "phase", "Phase: %s", "Fase: %s");
        LangEntries.add(g + "trip", "Last trip: %s", "Último disparo: %s");
        LangEntries.add(g + "suspended", "Paused: part of the torus is unloaded", "En pausa: parte del toro no está cargada");
        LangEntries.add(g + "plasma", "Plasma: %s°, density %s%%, ash %s%%", "Plasma: %s°, densidad %s%%, cenizas %s%%");
        LangEntries.add(g + "power", "Fusion %s MW, heating %s MW, Q %s", "Fusión %s MW, calentamiento %s MW, Q %s");
        LangEntries.add(g + "stability", "Confinement %s%%, instability %s%%", "Confinamiento %s%%, inestabilidad %s%%");
        LangEntries.add(g + "magnets", "Field %s%%, coils %s K, vacuum %s", "Campo %s%%, bobinas %s K, vacío %s");
        LangEntries.add(g + "fuel", "D: %s mB  T: %s mB  TBR %s", "D: %s mB  T: %s mB  TBR %s");
        LangEntries.add(g + "coolant", "Water: %s mB  Plasma steam: %s mB", "Agua: %s mB  Vapor de plasma: %s mB");
        LangEntries.add(g + "helium", "Liquid helium: %s mB  Warm helium: %s mB", "Helio líquido: %s mB  Helio caliente: %s mB");
        LangEntries.add(g + "tritium_overflow", "Tritium tank full: breeding stopped. Set an injector to OUTPUT",
                "Tanque de tritio lleno: la regeneración se detiene. Pon un inyector en SALIDA");

        LangEntries.add(T + "port.coolant_input", "Coolant input (water only)", "Entrada de refrigerante (solo agua)");
        LangEntries.add(T + "port.steam_output", "Plasma steam output", "Salida de vapor de plasma");
        LangEntries.add(T + "port.fuel_input", "Fuel input (deuterium, tritium)", "Entrada de combustible (deuterio, tritio)");
        LangEntries.add(T + "port.tritium_output", "Bred tritium output (keeps %s mB)", "Salida del tritio regenerado (guarda %s mB)");
        LangEntries.add(T + "port.hint", "Connect pipes or tanks to any side; wrench toggles the mode",
                "Conecta tuberías o tanques por cualquier lado; la llave inglesa cambia el modo");
        LangEntries.add(T + "port.linked", "Status: linked", "Estado: enlazado");
        LangEntries.add(T + "port.unlinked", "Status: no tokamak", "Estado: sin tokamak");

        LangEntries.add(T + "drive.spin_up", "Status: spin up (needs %s RPM)", "Estado: acelera (necesita %s RPM)");
        LangEntries.add(T + "drive.field", "Field: %s%%", "Campo: %s%%");
        LangEntries.add(T + "drive.speed", "Speed: %s RPM", "Velocidad: %s RPM");
    }

    private static void screen() {
        LangEntries.add(GUI + "start", "Start", "Arrancar");
        LangEntries.add(GUI + "start.tooltip",
                "Starts the sequence: pump down, cool the coils, ramp the field, light the plasma and heat it to a burn. A redstone pulse on the controller does the same.",
                "Inicia la secuencia: vacío, enfriado de las bobinas, rampa del campo, encendido del plasma y calentamiento hasta la combustión. Un pulso de redstone en el controlador hace lo mismo.");
        LangEntries.add(GUI + "shutdown", "Shutdown", "Apagar");
        LangEntries.add(GUI + "shutdown.tooltip",
                "Cuts fuel and heating, pumps the plasma out and ramps the field down, safely.",
                "Corta el combustible y el calentamiento, extrae el plasma y baja el campo en rampa, con seguridad.");
        LangEntries.add(GUI + "heating_on", "Heating: ON", "Calentamiento: SÍ");
        LangEntries.add(GUI + "heating_off", "Heating: OFF", "Calentamiento: NO");
        LangEntries.add(GUI + "heating.tooltip",
                "Neutral beam injectors and RF antennas in the shell. An ignited plasma burns without them; it cuts back by itself above 4500°.",
                "Inyectores de haces neutros y antenas de RF de la carcasa. Un plasma en ignición arde sin ellos; se reduce solo por encima de 4500°.");
        LangEntries.add(GUI + "phase", "Phase: %s", "Fase: %s");
        LangEntries.add(GUI + "trip", "Trip: %s", "Disparo: %s");
        LangEntries.add(GUI + "suspended", "Paused: part of the torus is unloaded", "En pausa: parte del toro sin cargar");
        LangEntries.add(GUI + "detail.offline", "Ready: press Start", "Listo: pulsa Arrancar");
        LangEntries.add(GUI + "detail.pump_down", "Pumping down: %s (needs %s)", "Haciendo vacío: %s (hasta %s)");
        LangEntries.add(GUI + "detail.cool_down", "Cooling coils: %s K (needs %s K)", "Enfriando bobinas: %s K (hasta %s K)");
        LangEntries.add(GUI + "detail.energize", "Field %s of %s", "Campo %s de %s");
        LangEntries.add(GUI + "detail.breakdown", "Solenoid pulse: lighting the plasma", "Pulso del solenoide: encendido");
        LangEntries.add(GUI + "detail.heating", "Heating to %s°", "Calentando hasta %s°");
        LangEntries.add(GUI + "detail.burn", "Burning with auxiliary heating", "En combustión con calentamiento");
        LangEntries.add(GUI + "detail.ignited", "Ignited: burning on its own", "En ignición: arde por sí solo");
        LangEntries.add(GUI + "detail.ramp_down", "Pumping out plasma, then the field", "Extrayendo plasma, luego el campo");
        LangEntries.add(GUI + "temperature", "Plasma %s°", "Plasma %s°");
        LangEntries.add(GUI + "density", "Density %s", "Densidad %s");
        LangEntries.add(GUI + "instability", "Instability %s%%", "Inestabilidad %s%%");
        LangEntries.add(GUI + "ash_gain", "Ash %s   Q %s", "Cenizas %s   Q %s");
        LangEntries.add(GUI + "fusion", "Fusion %s MW, %s", "Fusión %s MW, %s");
        LangEntries.add(GUI + "mode.cold", "cold", "frío");
        LangEntries.add(GUI + "mode.dt", "D-T", "D-T");
        LangEntries.add(GUI + "mode.dd", "D-D", "D-D");
        LangEntries.add(GUI + "heating", "Heating %s MW", "Calentamiento %s MW");
        LangEntries.add(GUI + "field", "Field %s (drives %s)", "Campo %s (tomas %s)");
        LangEntries.add(GUI + "magnets", "Coils %s K", "Bobinas %s K");
        LangEntries.add(GUI + "vacuum", "Vacuum %s", "Vacío %s");
        LangEntries.add(GUI + "burn", "Burn %s mB/t, TBR %s", "Quema %s mB/t, TBR %s");
        LangEntries.add(GUI + "tritium_overflow", "Tritium full: no breeding", "Tritio lleno: no se regenera");
        LangEntries.add(GUI + "fuel_rate", "Fuel mB/t", "Comb. mB/t");
        LangEntries.add(GUI + "dt_ratio", "D:T mix", "Mezcla D:T");
        LangEntries.add(GUI + "heating_level", "Heating", "Calentam.");
        LangEntries.add(GUI + "tank.deuterium", "Deuterium", "Deuterio");
        LangEntries.add(GUI + "tank.tritium", "Tritium", "Tritio");
        LangEntries.add(GUI + "tank.coolant", "Water (coolant)", "Agua (refrigerante)");
        LangEntries.add(GUI + "tank.plasma_steam", "Plasma steam", "Vapor de plasma");
        LangEntries.add(GUI + "tank.liquid_helium", "Liquid helium", "Helio líquido");
        LangEntries.add(GUI + "tank.warm_helium", "Warm helium (back to the plant)", "Helio caliente (vuelve a la planta)");
        LangEntries.add(GUI + "tank.amount", "%s / %s mB", "%s / %s mB");
    }
}
