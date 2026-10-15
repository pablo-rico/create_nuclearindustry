package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.block.AcceleratorControllerBlock;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.IonSourceBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.interlock.BeamInterlockBlock;
import org.papiricoh.create_nuclearindustry.data.recipe.AcceleratorRecipes;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

import java.util.concurrent.CompletableFuture;

/** Lang, models, tags and recipes for the particle accelerator and the antimatter machines. */
final class AcceleratorDataGen extends AreaDataGen {
    static final AcceleratorDataGen INSTANCE = new AcceleratorDataGen();
    private static final String A = "create_nuclearindustry.accelerator.";

    private AcceleratorDataGen() {}

    // ------------------------------------------------------------------ lang

    @Override
    void lang() {
        LangEntries.block("particle_accelerator_controller", "Particle Accelerator Controller", "Controlador del acelerador de partículas");
        LangEntries.block("particle_accelerator_ion_source", "Ion Source", "Fuente de iones");
        LangEntries.block("beam_pipe", "Beam Pipe", "Tubo de haz");
        LangEntries.block("beam_rf_cavity", "RF Cavity", "Cavidad de radiofrecuencia");
        LangEntries.block("beam_quadrupole", "Quadrupole Magnet", "Imán cuadrupolar");
        LangEntries.block("beam_bending_magnet", "Bending Magnet", "Imán de curvatura");
        LangEntries.block("beam_position_monitor", "Beam Position Monitor", "Monitor de posición del haz");
        LangEntries.block("beam_dump", "Beam Dump", "Absorbedor del haz");
        LangEntries.block("particle_accelerator_klystron", "Klystron", "Klistrón");
        LangEntries.block("particle_accelerator_magnet_power_supply", "Magnet Power Supply", "Fuente de alimentación de imanes");
        LangEntries.block("particle_accelerator_vacuum_pump", "Beamline Vacuum Pump", "Bomba de vacío de la línea de haz");
        LangEntries.fluid("hydrogen", "Hydrogen", "Hidrógeno");
        LangEntries.block("ring_dipole_magnet", "Ring Dipole Magnet", "Imán dipolar de anillo");
        LangEntries.block("superconducting_ring_dipole", "Superconducting Ring Dipole", "Dipolo superconductor de anillo");
        LangEntries.block("superconducting_rf_cavity", "Superconducting RF Cavity", "Cavidad de radiofrecuencia superconductora");
        LangEntries.block("injection_septum", "Injection Septum", "Septo de inyección");
        LangEntries.block("extraction_septum", "Extraction Septum", "Septo de extracción");
        LangEntries.block("accelerator_cryo_feed", "Accelerator Cryogenic Feed", "Alimentación criogénica del acelerador");
        LangEntries.block("target_station", "Target Station", "Estación de blancos");
        LangEntries.block("beam_interlock", "Beam Interlock", "Enclavamiento del haz");

        phases();
        trips();
        structure();
        messages();
        goggles();
        screen();
        subtitles();
        jei();
        ring();
    }

    /** The synchrotron's blocks, the target station, the cryogenic feed and the personnel interlock. */
    private static void ring() {
        String i = A + "interlock.";
        LangEntries.add(i + "secured", "Area searched and secured: the beam may run", "Zona revisada y asegurada: el haz puede funcionar");
        LangEntries.add(i + "already_secured", "Already secured. Sneak-click to open the interlock",
                "Ya está asegurado. Haz clic agachado para abrir el enclavamiento");
        LangEntries.add(i + "opened", "Interlock open: the beam cannot run", "Enclavamiento abierto: el haz no puede funcionar");
        LangEntries.add(i + "close_doors", "Close the doors beside the panel first", "Cierra antes las puertas junto al panel");
        LangEntries.add(i + "door_locked", "Locked by the beam interlock: the beam is on", "Bloqueada por el enclavamiento: el haz está en marcha");
        LangEntries.add(i + "goggles.secured", "Secured", "Asegurado");
        LangEntries.add(i + "goggles.open", "Open: the beam cannot run", "Abierto: el haz no puede funcionar");
        LangEntries.add(i + "goggles.linked", "Guarding an accelerator: %s", "Protege un acelerador: %s");
        LangEntries.add(i + "goggles.not_linked", "Not beside a formed accelerator's beamline", "No está junto a la línea de un acelerador formado");
        LangEntries.add(i + "goggles.doors_locked", "Doors beside it are locked", "Las puertas de al lado están bloqueadas");
        LangEntries.add(i + "goggles.hint", "Click to secure, sneak-click to open", "Clic para asegurar, clic agachado para abrir");
        String c = A + "cryo_feed.goggles.";
        LangEntries.add(c + "linked", "Feeding the accelerator's helium loop", "Alimenta el circuito de helio del acelerador");
        LangEntries.add(c + "not_linked", "Not beside a formed accelerator's beamline", "No está junto a la línea de un acelerador formado");
        LangEntries.add(c + "helium", "Liquid helium: %s / %s mB", "Helio líquido: %s / %s mB");
        LangEntries.add(c + "hint", "Pipe liquid helium in; warm helium comes back out", "Mete helio líquido por tubería; el helio caliente vuelve a salir");
        String t = A + "target.goggles.";
        LangEntries.add(t + "antiprotons", "Collecting %s ng of antiprotons a minute", "Recoge %s ng de antiprotones por minuto");
        LangEntries.add(t + "threshold", "Makes antiprotons only above %s GeV", "Solo produce antiprotones por encima de %s GeV");
        String p = PlantJeiInfo.PREFIX;
        LangEntries.add(p + "synchrotron",
                "A synchrotron ring raises the beam far beyond what a linac reaches. End the linac in an Injection Septum facing the centre of a round ring, one block wide, of radius 6 or more, and fill every cell of the ring with Ring Dipole Magnets (more dipoles, more energy), Quadrupole Magnets (at least one every eight blocks), RF Cavities and exactly one Extraction Septum facing out, from which an extraction line runs on to a Target Station or a Beam Dump. Superconducting Ring Dipoles reach 8 T instead of 1.8 T and Superconducting RF Cavities add 12 MeV for little power, but both need liquid helium through an Accelerator Cryogenic Feed. The top energy is 0.3 x field x radius x share of dipoles, in GeV.",
                "Un anillo sincrotrón lleva el haz mucho más allá de lo que alcanza un linac. Termina el linac en un septo de inyección que mire al centro de un anillo redondo, de un bloque de ancho y radio 6 o más, y llena cada celda del anillo con imanes dipolares de anillo (más dipolos, más energía), imanes cuadrupolares (al menos uno cada ocho bloques), cavidades de radiofrecuencia y exactamente un septo de extracción mirando hacia fuera, del que sale una línea de extracción hasta una estación de blancos o un absorbedor. Los dipolos superconductores llegan a 8 T en vez de 1,8 T y las cavidades superconductoras añaden 12 MeV con poca potencia, pero ambos necesitan helio líquido a través de una alimentación criogénica del acelerador. La energía máxima es 0,3 x campo x radio x fracción de dipolos, en GeV.");
        LangEntries.add(p + "target_station",
                "The end of an extraction line that does more than a beam dump. It takes the whole beam and gets activated like a dump, but above 25 GeV its magnetic horn also collects antiprotons and pushes them into the Penning traps beside it, or into an Antimatter Conduit. The higher the energy and the power of the beam, the more it collects. Its beam window faces the incoming beam. Putting antimatter into a trap without power annihilates it at once: power the traps first.",
                "El final de una línea de extracción que hace más que un absorbedor. Recibe todo el haz y se activa como un absorbedor, pero por encima de 25 GeV su cuerno magnético también recoge antiprotones y los empuja a las trampas de Penning de al lado, o a un conducto de antimateria. Cuanta más energía y potencia tenga el haz, más recoge. Su ventana mira al haz que llega. Meter antimateria en una trampa sin energía la aniquila al instante: alimenta antes las trampas.");
        LangEntries.add(p + "accelerator_cryo_feed",
                "Beside the beamline, it passes liquid helium from pipes (a cryogenic plant, a helium well or Liquid Helium Cells emptied into a tank) to the accelerator's superconducting dipoles and cavities, and gives the warm helium back out for the plant to liquefy again. Without it, superconducting parts never cool down and the accelerator refuses to start.",
                "Junto a la línea de haz, pasa el helio líquido de las tuberías (una planta criogénica, un pozo de helio o celdas de helio líquido vaciadas en un tanque) a los dipolos y cavidades superconductores del acelerador, y devuelve el helio caliente para que la planta lo vuelva a licuar. Sin ella, las piezas superconductoras nunca se enfrían y el acelerador no arranca.");
        LangEntries.add(p + "beam_interlock",
                "A personnel interlock panel. Place it beside the beamline, next to the door into the beam area: once you have checked nobody is inside, right-click it to secure the area. While any interlock of an accelerator is open its beam cannot start, and a running beam trips with a buzzer. Doors beside a panel stay locked while the beam is on, and a door opened anyway opens the interlock. A secured panel gives a redstone signal out of its back, for a warning light. The server can also refuse to run the beam with a player inside the machine.",
                "Un panel de enclavamiento de personal. Colócalo junto a la línea de haz, al lado de la puerta de la zona del haz: cuando hayas comprobado que no queda nadie dentro, haz clic derecho para asegurar la zona. Mientras cualquier enclavamiento de un acelerador esté abierto el haz no puede arrancar, y un haz en marcha se dispara con un zumbador. Las puertas junto a un panel quedan bloqueadas mientras hay haz, y una puerta abierta de todos modos abre el enclavamiento. Un panel asegurado da señal de redstone por detrás, para una luz de aviso. El servidor también puede negarse a dar haz con un jugador dentro de la máquina.");
    }

    private static void phases() {
        String[][] phases = {
                {"offline", "Offline", "Apagado"},
                {"pump_down", "Pumping down", "Haciendo vacío"},
                {"cool_down", "Cooling down", "Enfriando"},
                {"energize", "Energizing magnets", "Excitando imanes"},
                {"rf_on", "RF on", "Radiofrecuencia encendida"},
                {"injection", "Injecting", "Inyectando"},
                {"ramp", "Ramping", "Acelerando"},
                {"beam_on", "Beam on", "Haz en marcha"},
                {"beam_dump", "Dumping beam", "Descargando el haz"},
                {"ramp_down", "Ramping down", "Bajando imanes"}};
        for (String[] p : phases) {
            LangEntries.add(A + "phase." + p[0], p[1], p[2]);
        }
    }

    private static void trips() {
        String[][] trips = {
                {"none", "None", "Ninguna"},
                {"operator", "Stopped by the operator", "Parado por el operador"},
                {"structure", "Structure broken or unloaded", "Estructura rota o descargada"},
                {"vacuum_lost", "Beam pipe vacuum lost, or too few pumps", "Se perdió el vacío del tubo, o faltan bombas"},
                {"magnets_warm", "Superconducting parts warm and no liquid helium", "Piezas superconductoras calientes y sin helio líquido"},
                {"magnet_quench", "Magnet quench", "Quench de un imán"},
                {"no_magnet_power", "Too little power for the magnets", "Poca potencia para los imanes"},
                {"no_rf_power", "Too little radio-frequency power", "Poca potencia de radiofrecuencia"},
                {"no_ion_gas", "No hydrogen in the ion source", "No hay hidrógeno en la fuente de iones"},
                {"no_source_power", "The ion source has no drive: turn its shaft", "La fuente de iones no tiene accionamiento: haz girar su eje"},
                {"interlock", "Personnel interlock open", "Enclavamiento de personal abierto"},
                {"injection_energy_low", "The linac is too weak for the ring", "El linac es demasiado débil para el anillo"},
                {"beam_loss", "Too much of the beam was lost", "Se perdió demasiado haz"}};
        for (String[] t : trips) {
            LangEntries.add(A + "trip." + t[0], t[1], t[2]);
        }
    }

    private static void structure() {
        String e = A + "error.";
        LangEntries.add(e + "not_formed", "Not checked yet: right-click the controller", "Aún sin comprobar: haz clic derecho en el controlador");
        LangEntries.add(e + "no_source", "Needs an Ion Source right in front of the controller",
                "Necesita una fuente de iones justo delante del controlador");
        LangEntries.add(e + "source_shaft", "The Ion Source's shaft must come in across the beam, not along it",
                "El eje de la fuente de iones debe entrar de través al haz, no a lo largo");
        LangEntries.add(e + "gap", "The beamline is broken at %s %s %s: it needs a beamline block there",
                "La línea de haz se corta en %s %s %s: necesita ahí un bloque de línea de haz");
        LangEntries.add(e + "not_line_element", "This block cannot sit on a straight beamline: %s %s %s",
                "Este bloque no puede ir en una línea de haz recta: %s %s %s");
        LangEntries.add(e + "misaligned", "Turned the wrong way at %s %s %s: the beam must run along it (wrench to turn it)",
                "Mal girado en %s %s %s: el haz debe pasar a lo largo (gíralo con la llave)");
        LangEntries.add(e + "bend_aligned", "The bending magnet at %s %s %s must send the beam to one side",
                "El imán de curvatura de %s %s %s debe desviar el haz hacia un lado");
        LangEntries.add(e + "loop", "The beamline runs into itself at %s %s %s", "La línea de haz se cruza consigo misma en %s %s %s");
        LangEntries.add(e + "too_long", "The beamline has no end within %4$s blocks (at %s %s %s): end it in a Beam Dump or a target",
                "La línea de haz no acaba en %4$s bloques (en %s %s %s): termínala en un absorbedor o un blanco");
        LangEntries.add(e + "extraction_into_ring", "The extraction line must not lead back into a ring",
                "La línea de extracción no puede volver a un anillo");
        LangEntries.add(e + "no_ring", "The injection septum at %s %s %s must face the centre of a complete ring",
                "El septo de inyección de %s %s %s debe mirar al centro de un anillo completo");
        LangEntries.add(e + "ring_gap", "The ring of radius %4$s is broken at %s %s %s", "El anillo de radio %4$s se corta en %s %s %s");
        LangEntries.add(e + "two_extractions", "A ring may have only one extraction septum (another at %s %s %s)",
                "Un anillo solo puede tener un septo de extracción (otro en %s %s %s)");
        LangEntries.add(e + "two_injections", "A ring needs exactly one injection septum", "Un anillo necesita exactamente un septo de inyección");
        LangEntries.add(e + "no_extraction", "The ring needs an extraction septum", "El anillo necesita un septo de extracción");
        LangEntries.add(e + "extraction_inward", "The extraction septum at %s %s %s must face out of the ring",
                "El septo de extracción de %s %s %s debe mirar hacia fuera del anillo");
        String w = A + "warning.";
        LangEntries.add(w + "no_cavities", "No RF cavities: the beam stays at the source's energy",
                "Sin cavidades de radiofrecuencia: el haz se queda con la energía de la fuente");
        LangEntries.add(w + "no_klystrons", "No klystron beside the beamline: the cavities get no power",
                "Ningún klistrón junto a la línea: las cavidades no reciben potencia");
        LangEntries.add(w + "no_magnet_supplies", "No magnet power supply beside the beamline", "Ninguna fuente de alimentación de imanes junto a la línea");
        LangEntries.add(w + "no_pumps", "No vacuum pump beside the beamline", "Ninguna bomba de vacío junto a la línea");
        LangEntries.add(w + "no_cryo", "Superconducting parts but no Accelerator Cryogenic Feed beside the beamline",
                "Piezas superconductoras pero ninguna alimentación criogénica junto a la línea");
    }

    private static void messages() {
        String m = A + "message.";
        LangEntries.add(m + "formed", "Particle accelerator formed: %s blocks, up to %s", "Acelerador de partículas formado: %s bloques, hasta %s");
        LangEntries.add(m + "broken", "Particle accelerator broken: %s", "Acelerador de partículas roto: %s");
        LangEntries.add(m + "incomplete", "Particle accelerator incomplete: %s", "Acelerador de partículas incompleto: %s");
        LangEntries.add(m + "starting", "Accelerator starting: pumping down the beam pipe", "Arranca el acelerador: haciendo vacío en el tubo");
        LangEntries.add(m + "cannot_start", "The accelerator cannot start: %s", "El acelerador no puede arrancar: %s");
        LangEntries.add(m + "beam_on", "Beam on target at %s", "Haz en el blanco a %s");
        LangEntries.add(m + "stopped", "Beam dumped; the accelerator is ramping down", "Haz descargado; el acelerador se apaga");
        LangEntries.add(m + "trip", "Beam tripped: %s", "El haz se disparó: %s");
    }

    private static void goggles() {
        String g = A + "goggles.";
        LangEntries.add(g + "title", "Particle Accelerator", "Acelerador de partículas");
        LangEntries.add(g + "structure", "%s beamline blocks, up to %s", "%s bloques de línea, hasta %s");
        LangEntries.add(g + "ring", "Synchrotron ring of radius %s", "Anillo sincrotrón de radio %s");
        LangEntries.add(g + "phase", "Phase: %s", "Fase: %s");
        LangEntries.add(g + "trip", "Last trip: %s", "Último disparo: %s");
        LangEntries.add(g + "suspended", "Part of it is in unloaded chunks: waiting", "Parte está en chunks sin cargar: esperando");
        LangEntries.add(g + "beam", "Beam: %s, %s µA, %s", "Haz: %s, %s µA, %s");
        LangEntries.add(g + "losses", "Losses: %s%% (%s)", "Pérdidas: %s%% (%s)");
        LangEntries.add(g + "systems", "Vacuum %s, magnets %s%%, RF %s%%", "Vacío %s, imanes %s%%, RF %s%%");
        LangEntries.add(g + "cryo", "Superconductors at %s K, liquid helium %s mB", "Superconductores a %s K, helio líquido %s mB");
        LangEntries.add(g + "hydrogen", "Hydrogen in the source: %s mB", "Hidrógeno en la fuente: %s mB");
        String k = A + "machine.goggles.";
        LangEntries.add(k + "speed", "Speed: %s RPM (full drive at %s RPM)", "Velocidad: %s RPM (al máximo a %s RPM)");
        LangEntries.add(k + "energy", "Forge Energy: %s / %s FE (full drive takes %s FE/t)",
                "Forge Energy: %s / %s FE (al máximo consume %s FE/t)");
        LangEntries.add(k + "drive", "Drive: %s%%", "Rendimiento: %s%%");
        LangEntries.add(k + "klystron", "Radio-frequency power: %s / %s MW", "Potencia de radiofrecuencia: %s / %s MW");
        LangEntries.add(k + "magnets", "Feeds %s of %s magnets", "Alimenta %s de %s imanes");
        LangEntries.add(k + "pump", "Pumping at %s%%", "Bombeando al %s%%");
        LangEntries.add(k + "linked", "Serving an accelerator: %s", "Sirve a un acelerador: %s");
        LangEntries.add(k + "not_linked", "Not beside a formed accelerator's beamline", "No está junto a la línea de un acelerador formado");
        LangEntries.add(A + "ion_source.goggles.hydrogen", "Hydrogen: %s / %s mB (%s mB/s with beam)",
                "Hidrógeno: %s / %s mB (%s mB/s con haz)");
        LangEntries.add(A + "dump.goggles.beam", "Taking %s kW of beam", "Recibe %s kW de haz");
        LangEntries.add(A + "dump.goggles.activity", "Activation: %s rad/s", "Activación: %s rad/s");
    }

    private static void screen() {
        String s = "gui.create_nuclearindustry.accelerator.";
        LangEntries.add(s + "title", "Particle Accelerator", "Acelerador de partículas");
        LangEntries.add(s + "start", "Start beam", "Arrancar el haz");
        LangEntries.add(s + "start.tooltip", "Pumps down, energizes the magnets, fills the cavities and sends the beam",
                "Hace vacío, excita los imanes, llena las cavidades y envía el haz");
        LangEntries.add(s + "stop", "Stop beam", "Parar el haz");
        LangEntries.add(s + "stop.tooltip", "Kicks the beam into the dump and ramps the magnets down",
                "Lleva el haz al absorbedor y baja los imanes");
        LangEntries.add(s + "not_formed", "Not formed", "Sin formar");
        LangEntries.add(s + "phase", "Phase: %s", "Fase: %s");
        LangEntries.add(s + "suspended", "Waiting for unloaded chunks", "Esperando a chunks sin cargar");
        LangEntries.add(s + "energy", "Energy", "Energía");
        LangEntries.add(s + "current", "Current", "Corriente");
        LangEntries.add(s + "power", "Power", "Potencia");
        LangEntries.add(s + "losses", "Losses", "Pérdidas");
        LangEntries.add(s + "vacuum", "Vacuum", "Vacío");
        LangEntries.add(s + "magnets", "Magnets", "Imanes");
        LangEntries.add(s + "rf", "RF", "RF");
        LangEntries.add(s + "hydrogen", "Hydrogen", "Hidrógeno");
        LangEntries.add(s + "energy_setpoint", "Energy setpoint", "Consigna de energía");
        LangEntries.add(s + "current_setpoint", "Current setpoint", "Consigna de corriente");
    }

    private static void subtitles() {
        String s = "subtitle.create_nuclearindustry.";
        LangEntries.add(s + "accelerator_rf_hum", "Klystrons hum", "Zumban los klistrones");
        LangEntries.add(s + "magnet_ramp", "Magnets ramp", "Suben los imanes");
        LangEntries.add(s + "magnet_quench", "Magnet quenches", "Quench de un imán");
        LangEntries.add(s + "beam_dump", "Beam dumped", "Haz descargado");
        LangEntries.add(s + "interlock_buzzer", "Interlock buzzes", "Suena el enclavamiento");
        LangEntries.add(s + "confinement_hum", "Confinement field hums", "Zumba el campo de confinamiento");
        LangEntries.add(s + "annihilation_drone", "Annihilation generator drones", "Ronronea el generador de aniquilación");
    }

    private static void jei() {
        String p = PlantJeiInfo.PREFIX;
        LangEntries.add(p + "particle_accelerator",
                "A particle accelerator starts at its controller. Put an Ion Source right in front of it, shaft across the beam, and fill it with hydrogen (electrolyse brine). From the source a beamline runs on: Beam Pipes, RF Cavities that add energy, Quadrupole Magnets that keep the beam focused (one per four blocks), Bending Magnets to turn it and Beam Position Monitors to read it, until it ends in a Beam Dump or a target. Klystrons, Magnet Power Supplies and Vacuum Pumps beside the line power it. Right-click the controller to check the structure and open its panel; a redstone signal starts the beam and taking it away stops it. A longer linac with more cavities reaches higher energies.",
                "Un acelerador de partículas empieza en su controlador. Pon justo delante una fuente de iones, con el eje de través al haz, y llénala de hidrógeno (electroliza salmuera). Desde la fuente sigue una línea de haz: tubos de haz, cavidades de radiofrecuencia que añaden energía, imanes cuadrupolares que mantienen el haz enfocado (uno cada cuatro bloques), imanes de curvatura para girarlo y monitores de posición para leerlo, hasta terminar en un absorbedor del haz o en un blanco. Los klistrones, las fuentes de alimentación de imanes y las bombas de vacío junto a la línea la alimentan. Haz clic derecho en el controlador para comprobar la estructura y abrir su panel; una señal de redstone arranca el haz y quitarla lo para. Un linac más largo con más cavidades llega a más energía.");
        LangEntries.add(p + "beamline",
                "Beamline blocks run along the beam: place them looking along the line, or turn them with a wrench. Each RF Cavity adds 2.5 MeV with full radio-frequency power. Without a Quadrupole Magnet every four blocks the beam spreads and is lost: half the quadrupoles keep only a quarter of it. A Bending Magnet sends the beam to the side it faces. Beam Pipes have windows to watch the beam; a Beam Position Monitor gives a comparator signal with the beam current.",
                "Los bloques de la línea de haz van a lo largo del haz: colócalos mirando a lo largo de la línea, o gíralos con la llave. Cada cavidad de radiofrecuencia añade 2,5 MeV con toda su potencia de radiofrecuencia. Sin un imán cuadrupolar cada cuatro bloques el haz se abre y se pierde: con la mitad de cuadrupolos solo queda una cuarta parte. Un imán de curvatura desvía el haz hacia el lado al que mira. Los tubos de haz tienen ventanas para ver el haz; un monitor de posición del haz da una señal de comparador con la corriente.");
        LangEntries.add(p + "accelerator_machines",
                "Machines that power the beamline from beside it, with a shaft in their front face, or with Forge Energy from another mod if there is one. A Klystron gives 2 MW of radio-frequency power at 128 RPM; each copper cavity needs 0.25 MW. A Magnet Power Supply feeds 32 magnets at 64 RPM. A Beamline Vacuum Pump keeps 24 beamline blocks at beam vacuum at 64 RPM. Goggles show what each one gives.",
                "Máquinas que alimentan la línea de haz desde un lado, con un eje en su cara frontal, o con Forge Energy de otro mod si lo hay. Un klistrón da 2 MW de radiofrecuencia a 128 RPM; cada cavidad de cobre necesita 0,25 MW. Una fuente de alimentación de imanes alimenta 32 imanes a 64 RPM. Una bomba de vacío de la línea mantiene 24 bloques de línea en vacío de haz a 64 RPM. Las gafas muestran lo que da cada una.");
        LangEntries.add(p + "beam_dump",
                "Where a beamline ends when there is nothing to hit. It takes the whole beam and gives off radiation while it does; it also becomes radioactive itself, and keeps radiating after the beam stops, fading with time. Breaking an activated dump leaves radioactive waste and contaminates the ground: let it cool first. Shield it with concrete or lead.",
                "Donde termina una línea de haz si no hay nada a lo que dar. Recibe todo el haz y emite radiación mientras tanto; además se vuelve radiactivo y sigue radiando después de parar el haz, cada vez menos. Romper un absorbedor activado deja residuos radiactivos y contamina el suelo: deja que se enfríe antes. Blíndalo con hormigón o plomo.");
    }

    // ------------------------------------------------------------------ models

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        controller(provider);
        machine(provider, AcceleratorRegistry.ION_SOURCE.get(), "particle_accelerator_ion_source");
        machine(provider, AcceleratorRegistry.KLYSTRON.get(), "particle_accelerator_klystron");
        machine(provider, AcceleratorRegistry.MAGNET_POWER_SUPPLY.get(), "particle_accelerator_magnet_power_supply");
        machine(provider, AcceleratorRegistry.VACUUM_PUMP.get(), "particle_accelerator_vacuum_pump");
        pillar(provider, AcceleratorRegistry.BEAM_PIPE.get(), "beam_pipe", true);
        pillar(provider, AcceleratorRegistry.RF_CAVITY.get(), "beam_rf_cavity", false);
        pillar(provider, AcceleratorRegistry.QUADRUPOLE.get(), "beam_quadrupole", false);
        pillar(provider, AcceleratorRegistry.POSITION_MONITOR.get(), "beam_position_monitor", false);
        directional(provider, AcceleratorRegistry.BENDING_MAGNET.get(), "beam_bending_magnet");
        ModelFile dump = provider.models().cubeBottomTop("beam_dump", tex(provider, "beam_dump_side"), tex(provider, "beam_dump_bottom"),
                tex(provider, "beam_dump_top"));
        provider.simpleBlock(AcceleratorRegistry.BEAM_DUMP.get(), dump);
        provider.simpleBlockItem(AcceleratorRegistry.BEAM_DUMP.get(), dump);
        ringElement(provider, AcceleratorRegistry.RING_DIPOLE.get(), "ring_dipole_magnet");
        ringElement(provider, AcceleratorRegistry.SC_RING_DIPOLE.get(), "superconducting_ring_dipole");
        ringElement(provider, AcceleratorRegistry.SC_RF_CAVITY.get(), "superconducting_rf_cavity");
        facingExisting(provider, AcceleratorRegistry.INJECTION_SEPTUM.get(), existing(provider, "injection_septum"));
        facingExisting(provider, AcceleratorRegistry.EXTRACTION_SEPTUM.get(), existing(provider, "extraction_septum"));
        facingExisting(provider, AcceleratorRegistry.TARGET_STATION.get(), existing(provider, "target_station"));
        ModelFile cryo = existing(provider, "accelerator_cryo_feed");
        provider.getVariantBuilder(AcceleratorRegistry.CRYO_FEED.get()).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            return ConfiguredModel.builder().modelFile(cryo).rotationX(x).rotationY(y).build();
        });
        provider.simpleBlockItem(AcceleratorRegistry.CRYO_FEED.get(), cryo);
        ModelFile open = existing(provider, "beam_interlock");
        ModelFile secured = existing(provider, "beam_interlock_secured");
        provider.getVariantBuilder(AcceleratorRegistry.BEAM_INTERLOCK.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(BeamInterlockBlock.SECURED) ? secured : open)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360).build());
        provider.simpleBlockItem(AcceleratorRegistry.BEAM_INTERLOCK.get(), open);
    }

    private static ModelFile existing(NuclearBlockStateProvider provider, String name) {
        return provider.models().getExistingFile(provider.modLoc("block/" + name));
    }

    /** A ring element on its hand-made model: the beam runs along the model's Z axis. */
    private static void ringElement(NuclearBlockStateProvider provider, Block block, String id) {
        ModelFile model = existing(provider, id);
        provider.getVariantBuilder(block).forAllStates(state -> {
            Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
            return ConfiguredModel.builder().modelFile(model).rotationX(axis == Direction.Axis.Y ? 90 : 0)
                    .rotationY(axis == Direction.Axis.X ? 90 : 0).build();
        });
        provider.simpleBlockItem(block, model);
    }

    /** A hand-made model whose north face is the block's horizontal {@code FACING}. */
    private static void facingExisting(NuclearBlockStateProvider provider, Block block, ModelFile model) {
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360).build());
        provider.simpleBlockItem(block, model);
    }

    private static ResourceLocation tex(NuclearBlockStateProvider provider, String name) {
        return provider.modLoc("block/" + name);
    }

    /** The controller: beam port on its {@code FACING} face, panel (lit while running) on the back. */
    private static void controller(NuclearBlockStateProvider provider) {
        String id = "particle_accelerator_controller";
        ModelFile off = provider.models().cube(id, tex(provider, id + "_bottom"), tex(provider, id + "_top"), tex(provider, id + "_beam"),
                tex(provider, id + "_front"), tex(provider, id + "_side"), tex(provider, id + "_side")).texture("particle", tex(provider, id + "_side"));
        ModelFile on = provider.models().cube(id + "_on", tex(provider, id + "_bottom"), tex(provider, id + "_top"), tex(provider, id + "_beam"),
                tex(provider, id + "_front_on"), tex(provider, id + "_side"), tex(provider, id + "_side")).texture("particle", tex(provider, id + "_side"));
        provider.getVariantBuilder(AcceleratorRegistry.CONTROLLER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(AcceleratorControllerBlock.ON) ? on : off)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360).build());
        provider.simpleBlockItem(AcceleratorRegistry.CONTROLLER.get(), off);
    }

    /** A shaft-driven machine on the shared recessed model, turned so its front faces {@code FACING}. */
    private static void machine(NuclearBlockStateProvider provider, Block block, String id) {
        ModelFile file = provider.models().withExistingParent(id, provider.modLoc("block/accelerator_machine"))
                .texture("front", tex(provider, id + "_front")).texture("side", tex(provider, id + "_side"))
                .texture("back", tex(provider, id + "_back")).texture("particle", tex(provider, id + "_side"));
        provider.getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            return ConfiguredModel.builder().modelFile(file).rotationX(x).rotationY(y).build();
        });
        provider.simpleBlockItem(block, file);
    }

    /** A straight beamline block: its ends face along the beam. */
    private static void pillar(NuclearBlockStateProvider provider, Block block, String id, boolean cutout) {
        var vertical = provider.models().cubeColumn(id, tex(provider, id + "_side"), tex(provider, id + "_end"));
        var horizontal = provider.models().cubeColumnHorizontal(id + "_horizontal", tex(provider, id + "_side"), tex(provider, id + "_end"));
        if (cutout) {
            vertical.renderType("cutout");
            horizontal.renderType("cutout");
        }
        provider.axisBlock((RotatedPillarBlock) block, vertical, horizontal);
        provider.simpleBlockItem(block, horizontal);
    }

    /** A beamline block with a horizontal facing: the front (north in the model) is where the beam goes. */
    private static void directional(NuclearBlockStateProvider provider, Block block, String id) {
        ModelFile file = provider.models().cube(id, tex(provider, id + "_top"), tex(provider, id + "_top"), tex(provider, id + "_front"),
                tex(provider, id + "_back"), tex(provider, id + "_side"), tex(provider, id + "_side")).texture("particle", tex(provider, id + "_side"));
        provider.horizontalBlock(block, file);
        provider.simpleBlockItem(block, file);
    }

    // ------------------------------------------------------------------ tags and recipes

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        AcceleratorRegistry.PARTS.forEach(block -> parts.add(block.get()));
    }

    @Override
    void fluidTags(NuclearFluidTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(IonSourceBlockEntity.HYDROGEN).add(AllNuclearFluids.HYDROGEN.get(), AllNuclearFluids.HYDROGEN_FLUID.flowing().get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Accelerator crafting recipes", new AcceleratorRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Accelerator electrolysis recipes", new AcceleratorRecipes.Electrolysis(output, lookup)));
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
