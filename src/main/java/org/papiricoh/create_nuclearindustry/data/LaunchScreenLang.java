package org.papiricoh.create_nuclearindustry.data;

/** Labels and explanations belonging to the launch console, in both supported languages. */
final class LaunchScreenLang {
    private LaunchScreenLang() {}

    private static void entry(String key, String en, String es) {
        LangEntries.add("silo.create_nuclearindustry.screen." + key, en, es);
    }

    static void add() {
        entry("tab.rocket", "Rocket", "Cohete");
        entry("tab.launch", "Launch", "Lanzar");
        entry("waiting", "Waiting for controller…", "Esperando al controlador…");
        entry("diagram", "CUTAWAY", "SECCIÓN");
        entry("section.engine", "Engine", "Motor");
        entry("section.tank", "Propellant tank", "Depósito de propelente");
        entry("section.guidance", "Guidance module", "Módulo de guiado");
        entry("section.warhead", "Warhead", "Ojiva");
        entry("section.foreign", "Unexpected block", "Bloque inesperado");
        entry("selected", "#%s · %s", "#%s · %s");
        entry("select", "Select a section in the cutaway to load it", "Selecciona una sección del cohete para cargarla");
        entry("cell.hint", "Click to inspect and load this section", "Haz clic para inspeccionar y cargar esta sección");
        entry("stage.number", "Stage %s", "Etapa %s");
        entry("stages.empty", "Build an engine and tanks to see stage figures", "Coloca un motor y depósitos para ver sus datos");
        entry("table.stage", "Stg.", "Et.");
        entry("table.tanks", "Tanks", "Dep.");
        entry("table.twr", "TWR", "TWR");
        entry("table.dv", "Δv", "Δv");
        entry("table.burn", "Burn s", "Motor s");
        entry("table.reach", "Reach m", "Alc. m");
        entry("table.hint", "Tanks / fill · orange: solid, blue: liquid. TWR at ignition; Δv in blocks/tick; reach includes preceding stages.",
                "Depósitos / carga · naranja: sólido, azul: líquido. TWR al encender; Δv en bloques/tick; alcance con etapas anteriores.");
        entry("stage.stats", "TWR %s · Δv %s · burn %s s · reach %s m", "TWR %s · Δv %s · motor %s s · alcance %s m");
        entry("solid", "Solid propellant", "Propelente sólido");
        entry("liquid", "Liquid fuel / LOX", "Combustible / oxígeno líquido");
        entry("liquid.short", "Liquid / LOX", "Líquido / LOX");
        entry("fill", "Fill: %s%%", "Carga: %s%%");
        entry("warhead.inert", "Inert", "Inerte");
        entry("warhead.fission", "Fission", "Fisión");
        entry("warhead.dirty", "Radiological", "Radiológica");
        entry("warhead.thermonuclear", "Thermonuclear", "Termonuclear");
        entry("warhead.conventional", "Conventional", "Convencional");
        entry("power", "Power: %s", "Potencia: %s");
        entry("radius", "Blast: %s m", "Explosión: %s m");
        entry("fallout", "Fallout: %s m", "Lluvia rad.: %s m");
        entry("mass", "Mass: %s", "Masa: %s");
        entry("cep", "CEP: %s m", "CEP: %s m");
        entry("engine.hint", "Feed the tanks above this engine", "Carga los depósitos sobre este motor");
        entry("advice.ready", "Select a section to inspect its contents", "Selecciona una sección para ver su contenido");
        entry("locked", "Loading locked while armed or launching", "Carga bloqueada al armar o lanzar");
        entry("slot.charge", "Charge: fissile cores (2), radiological material (64), or TNT (16)", "Carga: núcleos fisibles (2), material radiológico (64) o TNT (16)");
        entry("slot.secondary", "Secondary: lithium deuteride (4 per slot)", "Secundario: deuteruro de litio (4 por casilla)");
        entry("slot.tamper", "Tamper: thermonuclear tamper material (2 per slot)", "Reflector: material de reflector termonuclear (2 por casilla)");
        entry("slot.bomb", "One complete nuclear, dirty or thermonuclear bomb; use no loose charge", "Una bomba nuclear, sucia o termonuclear completa; sin carga suelta");
        entry("slot.propellant", "One solid propellant cartridge; drain liquid before inserting", "Un cartucho de propelente sólido; vacía el líquido antes de insertarlo");
        entry("slot.guidance", "One precision mechanism or star tracker", "Un mecanismo de precisión o rastreador estelar");
        entry("slot.full", "This slot cannot accept the carried item or is full", "Esta casilla no admite el objeto o está llena");
        entry("slot.short.charge", "C", "C");
        entry("slot.short.secondary", "S", "S");
        entry("slot.short.tamper", "T", "R");
        entry("slot.short.bomb", "B", "B");
        entry("slot.short.propellant", "P", "P");
        entry("slot.short.guidance", "G", "G");
        entry("site.0", "No launch base detected", "No se detecta base de lanzamiento");
        entry("site.1", "Open launch platform", "Plataforma de lanzamiento abierta");
        entry("site.2", "Walled launch silo", "Silo de lanzamiento con paredes");
        entry("where.2", "BASE", "BASE");
        entry("where.3", "CLEARANCE", "ESPACIO LIBRE");
        entry("where.4", "EXIT", "SALIDA");
        entry("where.5", "CONTROLLER", "CONTROLADOR");
        entry("target", "TARGET", "OBJETIVO");
        entry("coordinate", "Target %s coordinate", "Coordenada %s del objetivo");
        entry("set", "Set target", "Fijar objetivo");
        entry("set.hint", "Apply X, Y and Z. Editing coordinates alone does not change the target.", "Aplica X, Y y Z. Editar las coordenadas por sí solo no cambia el objetivo.");
        entry("airburst.off", "Airburst: off", "Aérea: no");
        entry("airburst.on", "Airburst: on", "Aérea: sí");
        entry("arm", "Arm", "Armar");
        entry("armed", "ARMED", "ARMADO");
        entry("arm.hint", "Arm when ready, then apply a NEW redstone signal to launch", "Arma cuando esté listo y aplica una señal NUEVA de redstone para lanzar");
        entry("disarm", "Disarm", "Desarmar");
        entry("reach", "Reach: %s m", "Alcance: %s m");
        entry("guidance", "%s · CEP %s m", "%s · CEP %s m");
        entry("flight", "Flight time: %s s", "Tiempo de vuelo: %s s");
        entry("target.none", "Target: not set", "Objetivo: sin fijar");
        entry("target.xyz", "Target: %s / %s / %s", "Objetivo: %s / %s / %s");
        entry("delay", "Salvo delay: %s s", "Retardo de salva: %s s");
        entry("countdown", "LIFT-OFF IN", "DESPEGUE EN");
        entry("seconds", "%s s", "%s s");
        entry("standby", "—", "—");
        entry("redstone", "A new redstone signal (lever, button) launches it once armed; a signal already on does not.",
                "Una señal nueva de redstone (palanca, botón) lo lanza una vez armado; una señal ya encendida no.");
    }
}
