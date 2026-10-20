package org.papiricoh.create_nuclearindustry.data;

/** Lang of warhead slots, refusals and charges (stream S2). */
final class WarheadLang {
    private WarheadLang() {}

    static void add() {
        LangEntries.add("silo.create_nuclearindustry.error.charge_bomb_and_loose", "Use either a whole bomb or loose charges (%s, %s, %s)", "Usa una bomba entera o cargas sueltas (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_mixed_conventional", "TNT cannot mix with other charges (%s, %s, %s)", "El TNT no se puede mezclar con otras cargas (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_thermo_primary", "A thermonuclear warhead needs exactly 1 core as its primary (%s, %s, %s)", "La ojiva termonuclear necesita exactamente 1 núcleo como primario (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_tamper_alone", "A tamper needs a secondary (%s, %s, %s)", "La camisa necesita un secundario (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.error.charge_invalid", "Warhead item or stack exceeds its slot limits (%s, %s, %s)", "Objeto de ojiva o cantidad incompatible con el hueco (%s, %s, %s)");
        LangEntries.add("silo.create_nuclearindustry.refuse.not_for_slot", "That item does not belong in this slot", "Ese objeto no corresponde a este hueco");
        LangEntries.add("silo.create_nuclearindustry.refuse.wrong_section", "This section has no slot for that item", "Esta sección no tiene un hueco para ese objeto");
        LangEntries.add("silo.create_nuclearindustry.refuse.thermo_primary", "A thermonuclear warhead takes a single core as its primary", "Una ojiva termonuclear lleva un solo núcleo como primario");
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
        LangEntries.add("silo.create_nuclearindustry.advice.charge_thermo_primary", "Thermonuclear: leave just 1 core in Charge (now %s), no TNT or radiological", "Termonuclear: deja solo 1 núcleo en Carga (hay %s), sin TNT ni radiológico");
        LangEntries.add("silo.create_nuclearindustry.advice.charge_tamper_alone", "Add lithium deuteride to the Secondary slots, or take the tamper out", "Añade deuteruro de litio en Secundario o saca la camisa");
        LangEntries.add("silo.create_nuclearindustry.advice.charge_mixed_conventional", "TNT goes alone: take out the cores, lithium and radiological material", "El TNT va solo: saca los núcleos, el litio y el material radiológico");
        LangEntries.add("silo.create_nuclearindustry.advice.charge_bomb_and_loose", "Take out either the whole bomb or the loose charges", "Saca la bomba entera o las cargas sueltas");
        LangEntries.add("silo.create_nuclearindustry.advice.charge_invalid", "Take out the item that does not fit its slot", "Saca el objeto que no cabe en su hueco");
    }
}
