package org.papiricoh.create_nuclearindustry.data;

final class SiloLang {
    static void add() {
        LangEntries.add("silo.create_nuclearindustry.arm", "Arm", "Armar");
        LangEntries.add("silo.create_nuclearindustry.disarm", "Disarm", "Desarmar");
        LangEntries.add("silo.create_nuclearindustry.target", "Set target", "Fijar objetivo");
        LangEntries.add("silo.create_nuclearindustry.timing", "Countdown / flight:", "Cuenta / vuelo:");
        LangEntries.add("silo.create_nuclearindustry.section", "%s: %s", "%s: %s");
        LangEntries.add("silo.create_nuclearindustry.goggles.countdown", "Lift-off in %s s", "Despegue en %s s");
        LangEntries.add("silo.create_nuclearindustry.goggles.target", "Target: %s %s %s", "Objetivo: %s %s %s");
        LangEntries.add("silo.create_nuclearindustry.state.incomplete", "Incomplete", "Incompleto");
        LangEntries.add("silo.create_nuclearindustry.state.unprepared", "Unprepared", "Sin preparar");
        LangEntries.add("silo.create_nuclearindustry.state.ready", "Ready", "Listo");
        LangEntries.add("silo.create_nuclearindustry.state.armed", "Armed", "Armado");
        LangEntries.add("silo.create_nuclearindustry.state.countdown", "Counting down", "Cuenta atrás");
        LangEntries.add("silo.create_nuclearindustry.state.launched", "Launched", "Lanzado");
        LangEntries.add("silo.create_nuclearindustry.error.unloaded", "Structure chunk unloaded (%s, %s, %s)", "Chunk de la estructura descargado (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.fuel", "Empty propellant tank (%s, %s, %s)", "Depósito de propelente vacío (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge", "Invalid warhead contents (%s, %s, %s)", "Contenido de ojiva inválido (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.height", "Rocket must be 4–17 sections tall (%s, %s, %s)", "El cohete debe medir 4–17 bloques (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.stages", "At most three stages (%s, %s, %s)", "Máximo de tres etapas (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.tanks", "Each engine needs 1–4 tanks (%s, %s, %s)", "Cada motor requiere 1–4 depósitos (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.engine", "Expected rocket engine (%s, %s, %s)", "Falta motor de cohete (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.guidance", "Expected guidance module (%s, %s, %s)", "Falta módulo de guiado (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.warhead", "Expected one warhead (%s, %s, %s)", "Falta una ojiva (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.extra", "Unexpected section above warhead (%s, %s, %s)", "Sección sobrante sobre la ojiva (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.wall", "Missing silo wall or floor (%s, %s, %s)", "Falta pared o suelo de silo (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.interior", "Silo interior obstructed (%s, %s, %s)", "Interior del silo obstruido (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.exit", "Launch exit obstructed (%s, %s, %s)", "Salida de lanzamiento obstruida (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.dimension", "Launch forbidden in this dimension (%s, %s, %s)", "Lanzamiento prohibido en esta dimensión (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.out_of_range", "Target exceeds server range limit (%s, %s, %s)", "Objetivo fuera del alcance administrativo (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.spawn_protected", "Target inside protected spawn (%s, %s, %s)", "Objetivo dentro del spawn protegido (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.no_permission", "Insufficient launch permission (%s, %s, %s)", "Sin permiso de lanzamiento (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.cooldown", "Launch cooldown active (%s, %s, %s)", "Enfriamiento de lanzamiento activo (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.static", "Static installation required (%s, %s, %s)", "Se requiere una instalación estática (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.target", "Set a target first (%s, %s, %s)", "Fija primero un objetivo (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.speed", "Insufficient rotation (%s, %s, %s)", "Rotación insuficiente (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.range", "Target unreachable with this rocket (%s, %s, %s)", "Objetivo inalcanzable con este cohete (%s, %s, %s)");
    }
    private SiloLang() {}
}
