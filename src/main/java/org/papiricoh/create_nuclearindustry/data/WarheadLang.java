package org.papiricoh.create_nuclearindustry.data;

/** Lang of warhead slots, refusals and charges (stream S2). */
final class WarheadLang {
    private WarheadLang() {}

    static void add() {
        LangEntries.add("silo.create_nuclearindustry.error.charge_bomb_and_loose", "Use either a whole bomb or loose charges (%s, %s, %s)", "Usa una bomba entera o cargas sueltas (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_mixed_conventional", "TNT cannot mix with other charges (%s, %s, %s)", "El TNT no se puede mezclar con otras cargas (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_thermo_primary", "A secondary needs exactly one core and no other charge (%s, %s, %s)", "El secundario necesita exactamente un núcleo y ninguna otra carga (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_tamper_alone", "A tamper needs a secondary (%s, %s, %s)", "La camisa necesita un secundario (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_invalid", "Warhead item or stack exceeds its slot limits (%s, %s, %s)", "Objeto de ojiva o cantidad incompatible con el hueco (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.refuse.not_for_slot", "That item does not belong in this slot", "Ese objeto no corresponde a este hueco");
        LangEntries.add("silo.create_nuclearindustry.refuse.wrong_section", "This section has no slot for that item", "Esta sección no tiene un hueco para ese objeto");
        LangEntries.add("silo.create_nuclearindustry.refuse.full", "The matching slots are full", "Los huecos compatibles están llenos");
        LangEntries.add("silo.create_nuclearindustry.refuse.liquid", "Drain both liquids before inserting a cartridge", "Vacía ambos líquidos antes de insertar un cartucho");
        LangEntries.add("silo.create_nuclearindustry.bomb.contents", "Contents: %s items · %s", "Contenido: %s objetos · %s");
        LangEntries.add("silo.create_nuclearindustry.warhead.inert", "Inert", "Inerte");
        LangEntries.add("silo.create_nuclearindustry.warhead.fission", "Fission", "Fisión");
        LangEntries.add("silo.create_nuclearindustry.warhead.dirty", "Radiological", "Radiológica");
        LangEntries.add("silo.create_nuclearindustry.warhead.thermonuclear", "Thermonuclear", "Termonuclear");
        LangEntries.add("silo.create_nuclearindustry.warhead.conventional", "Conventional", "Convencional");
        LangEntries.add("silo.create_nuclearindustry.section.engine", "Engine", "Motor");
        LangEntries.add("silo.create_nuclearindustry.section.tank", "Tank", "Depósito");
        LangEntries.add("silo.create_nuclearindustry.section.guidance", "Guidance", "Guiado");
        LangEntries.add("silo.create_nuclearindustry.section.warhead", "Warhead", "Ojiva");
        LangEntries.add("silo.create_nuclearindustry.advice.replace", "Replace this section with one of the indicated types", "Sustituye esta sección por uno de los tipos indicados");
        LangEntries.add("silo.create_nuclearindustry.advice.remove_extra", "Remove sections above the warhead", "Retira las secciones sobre la ojiva");
        LangEntries.add("silo.create_nuclearindustry.advice.engine", "Start with an engine", "Empieza con un motor");
        LangEntries.add("silo.create_nuclearindustry.advice.tank", "Add a tank above the engine", "Añade un depósito sobre el motor");
        LangEntries.add("silo.create_nuclearindustry.advice.warhead", "Add a warhead above guidance", "Añade una ojiva sobre el guiado");
        LangEntries.add("silo.create_nuclearindustry.advice.next", "Add a tank, guidance, or an engine for another stage", "Añade un depósito, guiado o un motor para otra etapa");
        LangEntries.add("silo.create_nuclearindustry.advice.add_stage", "Add an engine above stage %s’s tanks to start the next stage", "Añade un motor sobre los depósitos de la etapa %s para empezar la siguiente");
    }
}
