package org.papiricoh.create_nuclearindustry.data;

final class SiloLang {
    static void add() {
        LangEntries.add("silo.create_nuclearindustry.arm", "Arm", "Armar");
        LangEntries.add("silo.create_nuclearindustry.disarm", "Disarm", "Desarmar");
        LangEntries.add("silo.create_nuclearindustry.target", "Set target", "Fijar objetivo");
        LangEntries.add("silo.create_nuclearindustry.timing", "Countdown / flight:", "Cuenta / vuelo:");
        LangEntries.add("silo.create_nuclearindustry.section", "%s: %s", "%s: %s");
        LangEntries.add("silo.create_nuclearindustry.goggles.countdown", "Lift-off in %s s", "Despegue en %s s");
        LangEntries.add("silo.create_nuclearindustry.guidance", "Guidance: %s · CEP %s m", "Guiado: %s · CEP %s m");
        LangEntries.add("silo.create_nuclearindustry.guidance.basic", "Dead reckoning", "Navegación a estima");
        LangEntries.add("silo.create_nuclearindustry.guidance.inertial", "Inertial", "Inercial");
        LangEntries.add("silo.create_nuclearindustry.guidance.stellar", "Stellar-inertial", "Astroinercial");
        LangEntries.add("silo.create_nuclearindustry.tank.liquid", "%s mB fuel, %s mB LOX", "%s mB combustible, %s mB LOX");
        LangEntries.add("silo.create_nuclearindustry.tank.solid", "Solid propellant cartridge", "Cartucho de propelente sólido");
        LangEntries.add("silo.create_nuclearindustry.tank.fuel", "Rocket fuel: %s / %s mB", "Combustible: %s / %s mB");
        LangEntries.add("silo.create_nuclearindustry.tank.oxidizer", "Liquid oxygen: %s / %s mB", "Oxígeno líquido: %s / %s mB");
        LangEntries.add("silo.create_nuclearindustry.tank.fill", "Burnable: %s%%", "Utilizable: %s%%");
        LangEntries.add("silo.create_nuclearindustry.error.mixed_propellant", "Stage mixes solid and liquid propellant (%s, %s, %s)", "La etapa mezcla propelente sólido y líquido (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.assembly", "Rocket cannot be assembled (%s, %s, %s)", "El cohete no se puede ensamblar (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.assembly_reason", "Rocket cannot be assembled: %s", "El cohete no se puede ensamblar: %s");
        LangEntries.add("silo.create_nuclearindustry.error.oversize", "Rocket sticks out of the shaft (%s, %s, %s)", "El cohete sobresale del pozo (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.glued_to_silo", "Rocket glued to the silo (%s, %s, %s)", "Cohete pegado al silo (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.cargo_not_empty", "Attached block holds items or fluids (%s, %s, %s)", "Un bloque acoplado guarda objetos o fluidos (%s, %s, %s)");
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
        LangEntries.add("silo.create_nuclearindustry.error.range_detail", "Target out of reach: %s blocks away, this rocket reaches %s",
                "Objetivo fuera de alcance: está a %s bloques y este cohete llega a %s");
        LangEntries.add("silo.create_nuclearindustry.error.range_none_detail",
                "This rocket reaches no target: it burns its propellant climbing out of the silo. Add tanks or a stage",
                "Este cohete no alcanza ningún objetivo: gasta el propelente en salir del silo. Añade depósitos o una etapa");
        LangEntries.add("silo.create_nuclearindustry.error.speed_detail", "Needs %s RPM through the shaft on its outer face (turning at %s RPM)",
                "Necesita %s RPM por el eje de su cara exterior (gira a %s RPM)");
        LangEntries.add("silo.create_nuclearindustry.error.overstressed_detail", "Kinetic network overstressed: the silo needs %s SU",
                "Red cinética sobrecargada: el silo necesita %s SU");
    }
    private SiloLang() {}
}
