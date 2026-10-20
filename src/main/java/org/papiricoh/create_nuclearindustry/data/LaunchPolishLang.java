package org.papiricoh.create_nuclearindustry.data;

/** Final launch site, controller and rocket tooltip and Display Link text. */
final class LaunchPolishLang {
    private LaunchPolishLang() {}

    static void add() {
        LangEntries.add("item.create_nuclearindustry.rocket_section.engine", "Starts a new stage: put one to four tanks above it.",
                "Inicia una etapa: coloca de uno a cuatro depósitos encima.");
        LangEntries.add("item.create_nuclearindustry.rocket_section.tank", "Holds propellant. Fill with a cartridge or pump Rocket Fuel and Liquid Oxygen through a fuel port.",
                "Contiene propelente. Llénalo con un cartucho o bombea combustible de cohete y oxígeno líquido por un puerto de combustible.");
        LangEntries.add("item.create_nuclearindustry.rocket_section.guidance", "Ends the stages; put a warhead above it.",
                "Termina las etapas; coloca una ojiva encima.");
        LangEntries.add("item.create_nuclearindustry.rocket_section.warhead", "Top section. Load cores, TNT or a whole bomb from the launch controller's console.",
                "Sección superior. Carga núcleos, TNT o una bomba entera desde la consola del controlador de lanzamiento.");
        LangEntries.add("item.create_nuclearindustry.launch_base.tooltip", "A 3×3 base under the rocket. Walls are optional; legacy silos still work.",
                "Una base de 3×3 bajo el cohete. Las paredes son opcionales; los silos antiguos siguen funcionando.");
        LangEntries.add("item.create_nuclearindustry.silo_fuel_port.tooltip", "Pipes Rocket Fuel and Liquid Oxygen into a rocket tank beside the base or silo wall.",
                "Bombea combustible de cohete y oxígeno líquido a un depósito junto a la base o a la pared del silo.");
        LangEntries.add("item.create_nuclearindustry.tactical_linker.tooltip", "Link a launch controller to the tactical command table for remote commands and salvos.",
                "Enlaza un controlador de lanzamiento con la mesa de mando táctico para enviar órdenes remotas y salvas.");

        LangEntries.add("silo.create_nuclearindustry.goggles.reach", "Reach: %s blocks", "Alcance: %s bloques");
        LangEntries.add("silo.create_nuclearindustry.goggles.salvo_delay", "Salvo delay: %s s", "Retardo de salva: %s s");
        LangEntries.add("silo.create_nuclearindustry.goggles.site", "Site: %s", "Emplazamiento: %s");
        LangEntries.add("silo.create_nuclearindustry.goggles.open_platform", "open platform", "plataforma abierta");
        LangEntries.add("silo.create_nuclearindustry.goggles.silo", "silo", "silo");

        LangEntries.add("silo.create_nuclearindustry.display.state", "State: %s", "Estado: %s");
        LangEntries.add("silo.create_nuclearindustry.display.countdown", "Countdown: %s s (salvo delay %s s)", "Cuenta atrás: %s s (retardo de salva %s s)");
        LangEntries.add("silo.create_nuclearindustry.display.idle", "Countdown: —", "Cuenta atrás: —");
        LangEntries.add("silo.create_nuclearindustry.display.reach", "Reach: %s blocks", "Alcance: %s bloques");
        LangEntries.add("silo.create_nuclearindustry.display.target", "Target: %s, %s, %s", "Objetivo: %s, %s, %s");
        LangEntries.add("silo.create_nuclearindustry.display.no_target", "Target: none", "Objetivo: ninguno");
        LangEntries.add("silo.create_nuclearindustry.display.offline", "Launch controller offline", "Controlador de lanzamiento desconectado");
    }
}
