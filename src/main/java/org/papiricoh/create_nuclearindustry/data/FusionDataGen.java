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
        ponder();
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
        panel();

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

    /** The tokamak's card on the panel (see fusion.display.TokamakPanel). */
    private static void panel() {
        String t = "create_nuclearindustry.panel.tokamak.";
        LangEntries.add(t + "kicker", "Tokamak", "Tokamak");
        LangEntries.add(t + "kicker_radius", "Tokamak · R %s", "Tokamak · R %s");
        LangEntries.add(t + "title", "Tokamak", "Tokamak");
        LangEntries.add(t + "hint.structure", "Fix the torus and click the controller", "Arregla el toro y haz clic en el controlador");
        LangEntries.add(t + "tag.dt", "D-T", "D-T");
        LangEntries.add(t + "tag.dd", "D-D", "D-D");
        LangEntries.add(t + "status.tripped", "Tripped", "Disparado");
        LangEntries.add(t + "status.suspended", "Paused", "En pausa");
        LangEntries.add(t + "status.burning", "Burning", "En combustión");
        LangEntries.add(t + "status.plasma", "Plasma", "Plasma");
        LangEntries.add(t + "status.standby", "Standby", "En espera");
        LangEntries.add(t + "gain", "Gain Q", "Ganancia Q");
        LangEntries.add(t + "heating", "Heating", "Calentam.");
        LangEntries.add(t + "temperature", "Plasma temperature", "Temperatura del plasma");
        LangEntries.add(t + "density", "Density", "Densidad");
        LangEntries.add(t + "confinement", "Confinement", "Confinamiento");
        LangEntries.add(t + "instability", "Instability", "Inestabilidad");
        LangEntries.add(t + "magnets", "Magnets and vacuum", "Imanes y vacío");
        LangEntries.add(t + "field", "Field", "Campo");
        LangEntries.add(t + "magnet_temperature", "Coils", "Bobinas");
        LangEntries.add(t + "vacuum", "Vacuum", "Vacío");
        LangEntries.add(t + "suspended", "Part of the torus is unloaded", "Parte del toro no está cargada");
        LangEntries.add(t + "tritium_overflow", "Tritium full: set an injector to output", "Tritio lleno: pon un inyector en salida");
        LangEntries.add(t + "structure", "Torus", "Toro");
        LangEntries.add(t + "radius", "Radius", "Radio");
        LangEntries.add(t + "coils", "Coils", "Bobinas");
        LangEntries.add(t + "coverage", "Coverage", "Cobertura");
        LangEntries.add(t + "blankets", "Blankets", "Mantos");
        LangEntries.add(t + "fuel", "Fuel", "Combustible");
        LangEntries.add(t + "deuterium", "Deuterium", "Deuterio");
        LangEntries.add(t + "tritium", "Tritium", "Tritio");
        LangEntries.add(t + "fuel_rate", "Fuelling", "Alimentac.");
        LangEntries.add(t + "breeding", "TBR", "TBR");
        LangEntries.add(t + "ash", "Ash", "Cenizas");
        LangEntries.add(t + "burn_rate", "Burn", "Quemado");
        LangEntries.add(t + "cooling", "Cooling", "Refrigeración");
        LangEntries.add(t + "coolant", "Water", "Agua");
        LangEntries.add(t + "plasma_steam", "Plasma steam", "Vapor plasma");
        LangEntries.add(t + "liquid_helium", "Liq. helium", "Helio líq.");
        LangEntries.add(t + "warm_helium", "Warm helium", "Helio cal.");
        LangEntries.add(t + "heating_switch", "Switch", "Interruptor");
        LangEntries.add(t + "heating_level", "Level", "Nivel");
        LangEntries.add(t + "field_target", "Field target", "Campo obj.");
        LangEntries.add("create_nuclearindustry.panel.unit.degrees", "%s°", "%s°");
        LangEntries.add("create_nuclearindustry.panel.unit.pa", "%s Pa", "%s Pa");
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

    private static void ponder() {
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.header", "Starting a Tokamak", "Arrancar un tokamak");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_1",
                "A tokamak does not start by itself. Before its plasma can burn it needs a vacuum, cold magnets, a field and heating.",
                "Un tokamak no arranca solo. Antes de que su plasma arda necesita vacío, imanes fríos, campo magnético y calentamiento.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_2",
                "Vacuum Pumps in the shell empty the plasma chamber: one for every 4 blocks of radius, turning at 128 RPM.",
                "Las bombas de vacío de la carcasa vacían la cámara de plasma: una por cada 4 bloques de radio, girando a 128 RPM.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_3",
                "Cryogenic Ports feed the coils liquid helium from a Cryogenic Plant: INPUT takes it in, OUTPUT sends the boiled-off helium back.",
                "Los puertos criogénicos llevan a las bobinas helio líquido de una planta criogénica: ENTRADA lo recibe y SALIDA devuelve el helio evaporado.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_4",
                "Superconducting coils must stay below 20 K. If the helium runs out they warm up and the tokamak shuts down safely.",
                "Las bobinas superconductoras deben seguir por debajo de 20 K. Si se acaba el helio se calientan y el tokamak se apaga de forma segura.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_5",
                "Neutral Beam Injectors and RF Antennas heat the plasma. Antennas only couple to a dense enough plasma; beams burn a little deuterium.",
                "Los inyectores de haces neutros y las antenas de RF calientan el plasma. Las antenas solo acoplan con un plasma bastante denso; los haces gastan algo de deuterio.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_6",
                "Heating takes a lot of rotation. Fusion gives back Q times the heating: below a Q of about 3 the plant spends more than it makes.",
                "Calentar cuesta mucha rotación. La fusión devuelve Q veces el calentamiento: con una Q por debajo de 3 la central gasta más de lo que produce.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_7",
                "Press Start in the controller's screen, give it a redstone pulse or throw a desk lever. The screen shows each phase and what is missing.",
                "Pulsa Arrancar en la pantalla del controlador, dale un pulso de redstone o acciona una palanca del pupitre. La pantalla muestra cada fase y lo que falta.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_8",
                "It pumps down, cools the coils and ramps up the field; then a solenoid pulse breaks a puff of deuterium down into plasma.",
                "Hace vacío, enfría las bobinas y sube el campo; después un pulso del solenoide convierte una bocanada de deuterio en plasma.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_9",
                "Viewports let you watch it: the plasma glows from red to white as it heats, and the windows light up the hall.",
                "Las ventanas de visión permiten verlo: el plasma pasa de rojo a blanco al calentarse, y las ventanas iluminan la sala.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_10",
                "Burning, a dense and well confined plasma can ignite: turn the heating down and it keeps itself hot.",
                "En combustión, un plasma denso y bien confinado puede llegar a la ignición: baja el calentamiento y se mantiene caliente solo.");
        LangEntries.add("create_nuclearindustry.ponder.tokamak_startup.text_11",
                "Closed up again, the formed vessel hides the burning plasma: the Viewports are its windows, and the solenoid glows with the field.",
                "Otra vez cerrada, la vasija formada oculta el plasma encendido: las ventanas de visión son sus ventanas, y el solenoide brilla con el campo.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.header", "The Cryogenic Plant", "La planta criogénica");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_1",
                "A Cryogenic Plant liquefies helium for a tokamak's magnets. It is a 3x3 box of Cryogenic Plant Casing, 3 to 5 blocks tall; every layer adds capacity.",
                "Una planta criogénica licúa helio para los imanes de un tokamak. Es una caja de 3x3 de carcasa de planta criogénica, de 3 a 5 bloques de alto; cada capa añade capacidad.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_2",
                "The Controller sits in a wall. Right-click it to form the plant.",
                "El controlador va en una pared. Haz clic derecho en él para formar la planta.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_3",
                "Compressors in the walls take the rotation, at 128 RPM or more. They are hungry: about 10240 SU per RPM for each layer, shared between them.",
                "Los compresores de las paredes reciben la rotación, a 128 RPM o más. Consumen mucho: unas 10240 SU por RPM por cada capa, repartidas entre ellos.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_4",
                "A port on Warm Helium In takes back the helium the tokamak's coils boil off...",
                "Un puerto en Entrada de helio caliente recoge el helio que evaporan las bobinas del tokamak...");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_5",
                "...and one on Liquid Helium Out sends it, cold, to the tokamak's Cryogenic Ports. A wrench changes a port's mode.",
                "...y uno en Salida de helio líquido lo envía, frío, a los puertos criogénicos del tokamak. La llave inglesa cambia el modo de un puerto.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_6",
                "Each pass loses a little helium. The helium ash the plasma makes as it burns tops the loop up again.",
                "En cada vuelta se pierde un poco de helio. Las cenizas de helio que produce el plasma al arder rellenan el circuito.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_7",
                "The first helium comes from the air. With a port on Liquid Air Out the plant liquefies air instead, and a Centrifuge splits liquid air into liquid nitrogen and a trace of helium.",
                "El primer helio sale del aire. Con un puerto en Salida de aire líquido la planta licúa aire, y una centrífuga separa el aire líquido en nitrógeno líquido y una traza de helio.");
        LangEntries.add("create_nuclearindustry.ponder.cryoplant.text_8",
                "Liquid nitrogen poured into a Nitrogen In port precools the plant, which then makes half as much liquid helium again.",
                "El nitrógeno líquido que entra por un puerto de Entrada de nitrógeno preenfría la planta, que entonces da la mitad más de helio líquido.");
    }
}
