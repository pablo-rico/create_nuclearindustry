package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.MaintenanceRecipes;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;

import java.util.concurrent.CompletableFuture;

/** Lang, the kit's model and recipe for plant operation and maintenance. */
final class MaintenanceDataGen extends AreaDataGen {
    static final MaintenanceDataGen INSTANCE = new MaintenanceDataGen();
    private static final String M = "create_nuclearindustry.maintenance.";

    private MaintenanceDataGen() {}

    @Override
    void lang() {
        LangEntries.item("maintenance_kit", "Maintenance Kit", "Kit de mantenimiento");
        String k = "item.create_nuclearindustry.maintenance_kit.";
        LangEntries.add(k + "hint", "Use on a reactor in a refuelling outage, or on a stopped steam generator or turbine",
                "Úsalo en un reactor en parada de recarga, o en un generador de vapor o una turbina parados");
        LangEntries.add(k + "mended", "Parts serviced", "Piezas revisadas");
        LangEntries.add(k + "nothing_to_do", "Nothing needs servicing", "No hay nada que revisar");
        LangEntries.add(k + "not_stopped", "Stop it first: a reactor needs a refuelling outage (cold, every rod in)",
                "Detenlo antes: un reactor necesita una parada de recarga (en frío, con todas las barras dentro)");
        LangEntries.add(k + "no_machine", "Not part of a reactor, steam generator or turbine",
                "No forma parte de un reactor, un generador de vapor o una turbina");
        parts();
        incidents();
        controlRoom();
        jei();
    }

    private static void parts() {
        LangEntries.add(M + "part", "%s: %s (%s%%)", "%s: %s (%s %%)");
        LangEntries.add(M + "part.rod_drives", "Control rod drives", "Mecanismos de las barras");
        LangEntries.add(M + "part.relief_valve", "Relief valve", "Válvula de alivio");
        LangEntries.add(M + "part.sg_tubes", "Steam generator tubes", "Tubos del generador de vapor");
        LangEntries.add(M + "part.turbine_blades", "Turbine blades", "Álabes de la turbina");
        LangEntries.add(M + "condition.good", "good", "bien");
        LangEntries.add(M + "condition.worn", "worn", "desgastado");
        LangEntries.add(M + "condition.poor", "poor: service it", "mal: revísalo");
        LangEntries.add(M + "condition.failed", "FAILED", "AVERIADO");
        LangEntries.add(M + "stuck_rod", "Control rod drive %s jammed at the SCRAM: its rods stay at %s%%",
                "El mecanismo de barras %s se ha atascado en el SCRAM: sus barras se quedan al %s %%");
        LangEntries.add(M + "stuck_channel", "Rod channel %s stuck at %s%% until serviced", "Canal de barras %s atascado al %s %% hasta revisarlo");
        LangEntries.add(M + "earthquake", "Earthquake! (%s)", "¡Terremoto! (%s)");
        LangEntries.add("create_nuclearindustry.reactor.message.seismic_scram", "Seismic trip: SCRAM signal on",
                "Disparo sísmico: señal de SCRAM activada");
        LangEntries.add("create_nuclearindustry.radiograph.reactor.outage", "In a refuelling outage: fuel and repairs may go in",
                "En parada de recarga: se puede cargar combustible y reparar");
        LangEntries.add("create_nuclearindustry.steam_generator.goggles.rupture", "TUBE RUPTURE: primary leaking into the secondary",
                "ROTURA DE TUBO: el primario se fuga al secundario");
        LangEntries.add("create_nuclearindustry.turbine.goggles.tripped", "Tripped on vibration: service the blades",
                "Disparada por vibración: revisa los álabes");
    }

    private static void incidents() {
        String i = "create_nuclearindustry.incident.";
        LangEntries.add(i + "ines", "INES %s", "INES %s");
        LangEntries.add(i + "off_scale", "—", "—");
        LangEntries.add(i + "line", "Day %s: [%s] %s at %s", "Día %s: [%s] %s en %s");
        LangEntries.add(i + "none", "No incidents logged", "Sin incidentes registrados");
        String[][] types = {
                {"scram", "Automatic SCRAM", "SCRAM automático"},
                {"seismic_scram", "Seismic SCRAM", "SCRAM sísmico"},
                {"turbine_trip", "Turbine trip", "Disparo de turbina"},
                {"stuck_rod", "Stuck control rod", "Barra de control atascada"},
                {"relief_valve_stuck", "Relief valve stuck open", "Válvula de alivio atascada abierta"},
                {"sg_tube_rupture", "Steam generator tube rupture", "Rotura de tubo del generador de vapor"},
                {"core_damage", "Fuel damage", "Daño del combustible"},
                {"vessel_breach", "Pressure vessel breach", "Rotura de la vasija de presión"},
                {"meltdown", "Core meltdown", "Fusión del núcleo"},
                {"earthquake", "Earthquake", "Terremoto"},
                {"detonation", "Detonation", "Detonación"},
                {"drill", "Emergency drill", "Simulacro de emergencia"}};
        if (types.length != IncidentType.values().length) {
            throw new IllegalStateException("every incident type needs a name");
        }
        for (String[] type : types) {
            LangEntries.add(i + "type." + type[0], type[1], type[2]);
        }
        String c = "commands.create_nuclearindustry.incident.";
        LangEntries.add(c + "list", "Incident log, newest %s of %s:", "Registro de incidentes, los %s más recientes de %s:");
        LangEntries.add(c + "drill", "Drill: plant alarm sounded at %s", "Simulacro: alarma de la central activada en %s");
        LangEntries.add(c + "earthquake", "Magnitude %s earthquake at %s; %s reactors scrammed",
                "Terremoto de magnitud %s en %s; %s reactores en SCRAM");
        LangEntries.add(c + "no_machine", "No machine there for a %s", "No hay ninguna máquina ahí para %s");
        LangEntries.add(c + "triggered", "%s set off at %s", "%s provocado en %s");
        LangEntries.add(c + "kind.stuck_rod", "stuck control rod", "barra de control atascada");
        LangEntries.add(c + "kind.sg_tube_rupture", "steam generator tube rupture", "rotura de tubo del generador de vapor");
        LangEntries.add(c + "kind.turbine_trip", "turbine trip", "disparo de turbina");
        LangEntries.add("create_nuclearindustry.display_source.incident_log", "Incident Log", "Registro de incidentes");
    }

    private static void controlRoom() {
        String metric = "create_nuclearindustry.alarm.metric.";
        LangEntries.add(metric + "component_wear", "Reactor parts' health below", "Salud de las piezas del reactor por debajo de");
        LangEntries.add(metric + "blade_wear", "Turbine blades' health below", "Salud de los álabes de la turbina por debajo de");
        LangEntries.add(metric + "refuelling_outage", "Reactor in a refuelling outage", "Reactor en parada de recarga");
        String a = "create_nuclearindustry.annunciator.";
        LangEntries.add(a + "short.component_wear", "COMP WORN", "PIEZA GASTADA");
        LangEntries.add(a + "short.blade_wear", "BLADES WORN", "ÁLABES GASTADOS");
        LangEntries.add(a + "short.refuelling_outage", "OUTAGE", "PARADA");
        String p = a + "preset.maintenance";
        LangEntries.add(p, "Maintenance", "Mantenimiento");
        String[] en = {"MAINT DUE", "COMP WORN", "COMP FAILED", "OUTAGE", "SCRAM", "CORE DAMAGE"};
        String[] es = {"REVISIÓN", "PIEZA GASTADA", "PIEZA AVERIADA", "PARADA", "SCRAM", "DAÑO NÚCLEO"};
        for (int n = 0; n < en.length; n++) {
            LangEntries.add(p + "." + (n + 1), en[n], es[n]);
        }
    }

    private static void jei() {
        String p = PlantJeiInfo.PREFIX;
        LangEntries.add(p + "maintenance_kit",
                "Plant parts wear with use: a fission reactor's control rod drives (with its power and at every SCRAM) and relief valve (at every lift), a steam generator's tubes (while they move heat, four times faster boiling dry) and a turbine's blades (with the steam they expand). Worn, they fail: a rod jams at a SCRAM and stays where it was, the relief valve sticks open more often, a tube bursts and leaks primary coolant into the secondary side (which then irradiates its surroundings), the turbine trips on vibration. Goggles and the Radiography Camera show their state. The Maintenance Kit mends them: on a reactor in a refuelling outage, or on a steam generator or turbine that is stopped. A Deployer can use it.",
                "Las piezas de una central se desgastan con el uso: los mecanismos de las barras de control de un reactor de fisión (con su potencia y en cada SCRAM) y su válvula de alivio (en cada apertura), los tubos de un generador de vapor (mientras transfieren calor, cuatro veces más rápido en seco) y los álabes de una turbina (con el vapor que expanden). Desgastadas, fallan: una barra se atasca en un SCRAM y se queda donde estaba, la válvula de alivio se queda abierta más a menudo, un tubo revienta y fuga refrigerante primario al secundario (que entonces irradia lo que lo rodea), la turbina se dispara por vibración. Las gafas y la cámara de radiografía muestran su estado. El kit de mantenimiento las repara: en un reactor en parada de recarga, o en un generador de vapor o una turbina parados. Un Deployer también puede usarlo.");
        LangEntries.add(p + "refuelling_outage",
                "A fission reactor takes fresh fuel only in a refuelling outage: formed, shut down cold (core under 100 °C) with every control rod in. Then the fuel ports take new assemblies and its parts can be serviced. The radiograph and the control room's Refuelling outage alarm show it. Servers can turn this off (maintenance.requireRefuellingOutage).",
                "Un reactor de fisión solo admite combustible nuevo en una parada de recarga: formado, parado en frío (núcleo por debajo de 100 °C) y con todas las barras de control dentro. Entonces los puertos de combustible aceptan elementos nuevos y se pueden revisar sus piezas. La radiografía y la alarma de parada de recarga de la sala de control lo muestran. Los servidores pueden desactivarlo (maintenance.requireRefuellingOutage).");
        LangEntries.add(p + "incident_log",
                "The server keeps a log of plant incidents rated on the INES scale: automatic and seismic SCRAMs, turbine trips (0), stuck rods and relief valves (1), tube ruptures (2), fuel damage (4), meltdowns (5, up to 7 with no containment), plus earthquakes, detonations and drills. Players nearby hear of anything from INES 1 up. A Display Link on a reactor controller or a seismic detector shows the newest entries; operators read it with /nuclear incident list. Natural earthquakes now and then shake the ground: seismic detectors record them, and a reactor shaken past its seismic trip scrams.",
                "El servidor lleva un registro de incidentes de las centrales clasificados en la escala INES: SCRAM automáticos y sísmicos, disparos de turbina (0), barras y válvulas de alivio atascadas (1), roturas de tubo (2), daño del combustible (4), fusiones del núcleo (5, hasta 7 sin contención), además de terremotos, detonaciones y simulacros. Los jugadores cercanos se enteran de todo lo que sea INES 1 o más. Un Display Link en un controlador de reactor o en un detector sísmico muestra las entradas más recientes; los operadores lo leen con /nuclear incident list. De vez en cuando la tierra tiembla: los detectores sísmicos registran los terremotos, y un reactor sacudido por encima de su disparo sísmico entra en SCRAM.");
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(MaintenanceRegistry.MAINTENANCE_KIT.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Maintenance crafting recipes", new MaintenanceRecipes(output, lookup)));
    }

    /** Providers need unique names, and the shared recipe providers already hold the default ones. */
    private record Named(String name, DataProvider provider) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput output) {
            return provider.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
