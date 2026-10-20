package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Line;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText.Scene;

import java.util.List;

/**
 * Text and wiring of the guide's Ponder scenes: civil defense, weapons, the fuel cycle from ore to
 * fuel and the two fan treatments. Like {@link PlantPonderText}, pure Java: the scenes take their
 * lines from here in the order they show them, the datagen writes the lang keys from the same
 * lists, and JUnit checks that both agree. A component without a namespace is an item of this mod;
 * one with a namespace ({@code create:encased_fan}) is another mod's item the scene is shown on too.
 */
public final class GuidePonderText {
    private static final String FAN = "create:encased_fan";

    // ------------------------------------------------------------------ civil defense

    public static final Scene SIRENS = new Scene("civil_defense_sirens", "civil_defense_sirens",
            line("Sirens", "Sirenas"),
            List.of(
                    line("The Air Raid Siren sounds a steady alert while it gets a redstone signal.",
                            "La Sirena antiaérea da un aviso continuo mientras recibe una señal de redstone."),
                    line("Bomb countdowns and plant accidents trigger automatic warnings. Missile alerts require a powered radar connected by redstone.",
                            "Las cuentas atrás de bombas y los accidentes activan avisos automáticos. Para alertas de misiles conecta un radar operativo mediante redstone."),
                    line("Goggles on it tell how long until the impact. It is heard up to 128 blocks away.",
                            "Con las gafas se ve cuánto falta para el impacto. Se oye hasta a 128 bloques."),
                    line("The Hand-Crank Siren has no power of its own: it sounds while the shaft under it turns at 32 RPM or more...",
                            "La Sirena de manivela no tiene energía propia: suena mientras el eje de debajo gira a 32 RPM o más..."),
                    line("...and wails higher the faster it turns. A comparator reads 15 from it while it sounds.",
                            "...y aúlla más agudo cuanto más rápido gira. Un comparador lee 15 de ella mientras suena.")),
            List.of("air_raid_siren", "hand_crank_siren"));

    public static final Scene BLAST_DOOR = new Scene("civil_defense_blast_door", "civil_defense_blast_door",
            line("Blast Doors", "Puertas blindadas"),
            List.of(
                    line("A blast door is a rectangle of steel or lead Blast Door leaves, 2x2 to 4x4, with a ring of Blast Door Frame around it.",
                            "Una puerta blindada es un rectángulo de hojas de puerta blindada, de acero o de plomo, de 2x2 a 4x4, con un anillo de Marco de puerta blindada alrededor."),
                    line("One block of the ring is the Blast Door Controller. Its shaft runs through the wall, across the door.",
                            "Un bloque del anillo es el Controlador de puerta blindada. Su eje atraviesa la pared, perpendicular a la puerta."),
                    line("A rising redstone signal opens it: the panel slides sideways into the wall, towards the controller...",
                            "Una señal de redstone que sube la abre: el panel se desliza de lado dentro de la pared, hacia el controlador..."),
                    line("...and a falling one shuts it. A shaft turning one way opens it and the other way shuts it, faster the faster it turns.",
                            "...y una que baja la cierra. Un eje que gira en un sentido la abre y en el otro la cierra, más deprisa cuanto más rápido gira."),
                    line("Shut, it holds back blast waves and radiation; open, the doorway lets both through. A comparator reads how far open it is.",
                            "Cerrada, detiene ondas expansivas y radiación; abierta, el hueco deja pasar ambas. Un comparador lee cuánto está abierta."),
                    line("If a leaf or a frame block is broken, the door slams shut.",
                            "Si se rompe una hoja o un bloque del marco, la puerta se cierra de golpe.")),
            List.of("blast_door_controller", "blast_door_frame", "steel_blast_door", "lead_blast_door"));

    public static final Scene SHELTER = new Scene("civil_defense_shelter", "civil_defense_shelter",
            line("Fallout Shelters", "Refugios antinucleares"),
            List.of(
                    line("A Shelter Filtration Unit certifies the sealed room it stands in as a shelter.",
                            "Una Unidad de filtración de refugio certifica como refugio la sala estanca en la que está."),
                    line("Every wall block the air touches must hold back blasts and radiation well enough: glass will not do. Goggles on the unit name the weakest wall.",
                            "Cada bloque de pared que toca el aire debe aguantar explosiones y radiación lo bastante: el vidrio no sirve. Con las gafas la unidad señala la pared más débil."),
                    line("Inside a certified shelter a blast wave keeps only a tenth of its force. A shut blast door counts as wall.",
                            "Dentro de un refugio certificado una onda expansiva conserva solo una décima parte de su fuerza. Una puerta blindada cerrada cuenta como pared."),
                    line("While the unit turns at 16 RPM or more it filters the air: fallout and contaminated ground give those inside a twentieth of their dose.",
                            "Mientras la unidad gira a 16 RPM o más filtra el aire: la lluvia radiactiva y el suelo contaminado dan a quien está dentro una vigésima parte de su dosis."),
                    line("A Seismic Detector feels every nuclear detonation in its dimension. Goggles show the distance, bearing and strength of the last eight.",
                            "Un Detector sísmico siente cada detonación nuclear de su dimensión. Las gafas muestran la distancia, el rumbo y la fuerza de las ocho últimas."),
                    line("A comparator reads it: the signal jumps with a blast and fades back to 0 over five minutes.",
                            "Un comparador lo lee: la señal salta con una explosión y vuelve a 0 en cinco minutos.")),
            List.of("shelter_filtration_unit", "seismic_detector"));

    // ------------------------------------------------------------------ weapons

    public static final Scene BOMBS = new Scene("weapons_bombs", "weapons_bombs",
            line("Nuclear and Dirty Bombs", "Bombas nucleares y sucias"),
            List.of(
                    line("The Nuclear Bomb takes weapons-grade cores in its three slots, two to a slot: each is eight uranium at 90%% or more, or four plutonium.",
                            "La Bomba nuclear lleva núcleos de grado militar en sus tres huecos, dos por hueco: cada uno son ocho de uranio al 90%% o más, o cuatro de plutonio."),
                    line("The more cores it holds, the further it wrecks: six reach 250 blocks out, round a crater a third as wide.",
                            "Cuantos más núcleos lleva, más lejos arrasa: seis llegan a 250 bloques, alrededor de un cráter de un tercio de ese ancho."),
                    line("Power it with redstone: after a ten second countdown it goes off. Sirens in reach sound the attack warning meanwhile.",
                            "Dale una señal de redstone: tras una cuenta atrás de diez segundos estalla. Mientras, las sirenas cercanas dan el aviso de ataque."),
                    line("Its flash burns whoever sees it, a blast wave rolls outwards throwing whoever stands in the open, and fallout drifts downwind.",
                            "Su destello quema a quien lo ve, una onda expansiva avanza lanzando a quien esté al descubierto y la lluvia radiactiva se desplaza con el viento."),
                    line("The Dirty Bomb takes anything radioactive that is not weapons-grade: waste, spent fuel, fragments, RTG pellets.",
                            "La Bomba sucia lleva cualquier cosa radiactiva que no sea de grado militar: residuos, combustible gastado, fragmentos, pastillas de RTG."),
                    line("Its blast is small and leaves no crater, but it scatters its load as fallout, wider the more radioactive the load.",
                            "Su explosión es pequeña y no deja cráter, pero esparce su carga como lluvia radiactiva, más lejos cuanto más radiactiva sea.")),
            List.of("nuclear_bomb", "dirty_bomb"));

    public static final Scene THERMONUCLEAR = new Scene("weapons_thermonuclear", "weapons_thermonuclear",
            line("Thermonuclear Bombs", "Bombas termonucleares"),
            List.of(
                    line("The Thermonuclear Bomb is a fission primary that lights a fusion secondary. Its first slot takes one weapons-grade core, the primary: without it, the bomb cannot go off.",
                            "La Bomba termonuclear es un primario de fisión que enciende un secundario de fusión. Su primer hueco lleva un núcleo de grado militar, el primario: sin él, la bomba no puede estallar."),
                    line("Up to sixteen Lithium-6 Deuteride make its secondary, and each one multiplies the yield. With none, only the primary goes off.",
                            "Hasta dieciséis de Deuteruro de litio-6 forman su secundario, y cada uno multiplica la potencia. Sin ninguno, solo estalla el primario."),
                    line("Up to four Depleted Uranium Blocks make its tamper: a bigger blast, and far more fallout, with a hot spot that lasts for days.",
                            "Hasta cuatro Bloques de uranio empobrecido forman su camisa: una explosión mayor y mucha más lluvia radiactiva, con un foco que dura días."),
                    line("Power it with redstone: after a fifteen second countdown it wrecks what stands up to 750 blocks away, three times an atomic bomb.",
                            "Dale una señal de redstone: tras una cuenta atrás de quince segundos arrasa lo que se alza hasta 750 bloques, tres veces una bomba atómica."),
                    line("Its fireball burns bigger, whiter and longer, and the cloud it raises is near-black soot.",
                            "Su bola de fuego es mayor, más blanca y dura más, y la nube que levanta es de hollín casi negro.")),
            List.of("thermonuclear_bomb", "lithium_deuteride"));

    public static final Scene MISSILE = new Scene("weapons_missile", "weapons_missile",
            line("Building a Modular Rocket", "Montaje de un cohete modular"),
            List.of(
                    line("A 3×3 Launch Base is all a rocket needs. Put the Launch Controller beside any side; a Silo Concrete shell is optional, and only changes the look.",
                            "Un 3×3 de Base de lanzamiento es todo lo que necesita un cohete. Pon el Controlador de lanzamiento junto a cualquier lado; una carcasa de Hormigón de silo es opcional y solo cambia el aspecto."),
                    line("Stack one to three stages at the centre: an engine and one to four tanks per stage. Every new engine starts the next stage; finish with guidance and one warhead (4–17 blocks).",
                            "Apila de una a tres etapas en el centro: un motor y de uno a cuatro depósitos por etapa. Cada motor nuevo inicia la siguiente etapa; termina con guiado y una ojiva (4–17 bloques)."),
                    line("Fill each tank with a propellant cartridge, or pipe Rocket Fuel and Liquid Oxygen into a Silo Fuel Port touching the base: liquid stages push harder and longer. A stage cannot mix both.",
                            "Llena cada depósito con un cartucho, o lleva por tubería Combustible de cohete y Oxígeno líquido a un Puerto de combustible de silo que toque la base: las etapas líquidas empujan más y más tiempo. Una etapa no puede mezclar ambos."),
                    line("The guidance sets the accuracy: empty it is basic, with a Precision Mechanism inertial, with a Star Tracker stellar. The controller shows the expected miss.",
                            "El guiado decide la precisión: vacío es básico, con un Mecanismo de precisión inercial, con un Rastreador estelar estelar. El controlador muestra el error esperado."),
                    line("Open the Launch Controller to load the warhead in its console, set coordinates or use a Target Designator. A Tactical Linker can give a Holographic Command Table control of it.",
                            "Abre el Controlador de lanzamiento para cargar la ojiva en su consola, introducir coordenadas o usar un Designador de objetivo. Un Enlazador táctico puede dar a una Mesa holográfica de mando control sobre él.")),
            List.of("silo_controller", "launch_base", "silo_wall", "silo_fuel_port", "rocket_engine", "rocket_tank", "rocket_guidance", "rocket_warhead", "propellant_cartridge",
                    "star_tracker", "target_designator", "tactical_linker"));

    public static final Scene SILO_LAUNCH = new Scene("silo_launch", "weapons_missile",
            line("Arming and Launching", "Armado y lanzamiento"),
            List.of(
                    line("Supply at least 32 RPM to the Launch Controller. Its default stress impact is 8 SU per RPM. Load the warhead in its console, then arm the prepared rocket on the Launch tab.",
                            "Suministra al menos 32 RPM al Controlador de lanzamiento. Su impacto de estrés inicial es 8 SU por RPM. Carga la ojiva en su consola y arma el cohete preparado en la pestaña Lanzamiento."),
                    line("A Launch Base works in the open; Silo Concrete is only an optional shell. Apply a new redstone rising edge to start the ten-second countdown. A continuously powered wire cannot launch the next rocket.",
                            "Una Base de lanzamiento funciona al aire libre; el Hormigón de silo es solo una carcasa opcional. Aplica un nuevo flanco de redstone para iniciar los diez segundos de cuenta atrás. Una señal permanente no lanza el siguiente cohete."),
                    line("Losing rotation, modifying the rocket or blocking the exit cancels the countdown. Loading the world also disarms it: arm again before retrying.",
                            "Perder rotación, modificar el cohete u obstruir la salida cancela la cuenta. Recargar el mundo también lo desarma: vuelve a armar antes de reintentar."),
                    line("The rocket lifts off whole and drops its spent stages as falling debris. Missiles warn no one by themselves: radars do.",
                            "El cohete despega entero y suelta sus etapas agotadas, que caen como restos. Los misiles no avisan a nadie por sí mismos: lo hacen los radares.")),
            List.of("silo_controller", "rocket_guidance", "target_designator"));

    public static final Scene RADAR = new Scene("missile_radar", "weapons_missile",
            line("Radar and Missile Alerts", "Radar y alertas de misiles"),
            List.of(
                    line("Turn the radar at 32 RPM or more. It sees a rocket body about 3 km away, further when it turns faster and much further while a booster burns. Its dish turns with the sweep.",
                            "Haz girar el radar a 32 RPM o más. Ve un cuerpo de cohete a unos 3 km, más lejos si gira más rápido y mucho más mientras arde un propulsor. Su antena gira con el barrido."),
                    line("The Earth curves: far, low flights hide below the horizon, so build radars high. Radars that share a four-character link code share what they see.",
                            "La Tierra es curva: los vuelos lejanos y bajos quedan bajo el horizonte, así que pon los radares altos. Los radares con el mismo código de cuatro caracteres comparten lo que ven."),
                    line("Set a defended zone on its screen. Its redstone output and klaxon come on for flights predicted to land in it; acknowledging silences only the klaxon.",
                            "Fija una zona defendida en su pantalla. Su salida de redstone y su claxon se encienden con los vuelos que caerán en ella; reconocer solo calla el claxon."),
                    line("Link a Radar Scope or a Holographic Command Table with a Tactical Linker for a live scope and coordinated launches. Display Links and control room monitors read it too.",
                            "Enlaza una Pantalla de radar o una Mesa holográfica de mando con un Enlazador táctico para ver el barrido en directo y coordinar lanzamientos. También lo leen los Display Link y los monitores.")),
            List.of("missile_radar", "radar_scope", "air_raid_siren", "tactical_linker"));

    public static final Scene HOLO_TABLE = new Scene("holo_table", "weapons_holo_table",
            line("The Holographic Command Table", "La mesa holográfica de mando"),
            List.of(
                    line("Place nine Holographic Command Table pieces in a 3×3: they fuse into one table, with its projector under the middle.",
                            "Coloca nueve piezas de Mesa holográfica de mando en un 3×3: se funden en una sola mesa, con el proyector bajo el centro."),
                    line("With a Tactical Linker, remember a radar or Launch Controller, then use the table to link it. The table takes four radars and up to sixteen launch controllers.",
                            "Con un Enlazador táctico, guarda un radar o un Controlador de lanzamiento y úsalo después sobre la mesa para enlazarlo. La mesa admite cuatro radares y hasta dieciséis controladores de lanzamiento."),
                    line("Choose a linked launcher on the Silos tab, designate its target, and arm it there. Tick several silos to arm a salvo with coordinated delays, or disarm and abort your own flight.",
                            "Elige un lanzador enlazado en la pestaña Silos, fija su blanco y ármalo allí. Marca varios silos para armar una salva con retardos coordinados, o desarma y aborta tu propio vuelo."),
                    line("The table never fires a missile: every armed Launch Controller still needs its own new redstone signal. Look at tracks to label or pin them, and right-click bare glass for the screen.",
                            "La mesa nunca dispara un misil: cada Controlador de lanzamiento armado sigue necesitando su propia señal nueva de redstone. Mira las trazas para ver su etiqueta o fijarlas, y haz clic derecho en el cristal vacío para abrir la pantalla.")),
            List.of("holo_table", "tactical_linker"));

    public static final Scene LAUNCH_BASE = new Scene("launch_base", "weapons_launch_base",
            line("Launch Platform and Redstone Launch", "Plataforma de lanzamiento y lanzamiento por redstone"),
            List.of(
                    line("Start with a 3×3 square of Launch Base blocks. It is the complete launch site floor; Silo Concrete walls are optional decoration.",
                            "Empieza con un cuadrado de 3×3 de bloques de Base de lanzamiento. Es el suelo completo del sitio de lanzamiento; las paredes de Hormigón de silo son decoración opcional."),
                    line("Place the Launch Controller beside any side of the base, drive it with shafts from a Creative Motor or cogs, and put a Silo Fuel Port touching the base with a pipe.",
                            "Coloca el Controlador de lanzamiento junto a cualquier lado de la base, muévelo con ejes desde un Motor creativo o engranajes y pon un Puerto de combustible de silo tocando la base con una tubería."),
                    line("Build upward: engine and tanks make stage 1; the next engine starts stage 2. Finish with guidance and a warhead—every engine begins a new stage.",
                            "Construye hacia arriba: motor y depósitos forman la etapa 1; el siguiente motor inicia la etapa 2. Termina con guiado y una ojiva: cada motor empieza una nueva etapa."),
                    line("Right-click the controller. Its console cutaway shows the stages; load the warhead there with cores, TNT, or a complete bomb.",
                            "Haz clic derecho en el controlador. El corte de su consola muestra las etapas; carga allí la ojiva con núcleos, TNT o una bomba completa."),
                    line("On the Launch tab, arm it. Then flip a lever or press a button beside the controller: only a new redstone signal starts the countdown; one already on does nothing.",
                            "En la pestaña Lanzamiento, ármalo. Después acciona una palanca o pulsa un botón junto al controlador: solo una señal nueva de redstone inicia la cuenta atrás; una ya encendida no hace nada."),
                    line("The rocket lifts off in smoke and the Launch Base remains. Build a Silo Concrete shell only when you want the look of a silo.",
                            "El cohete despega entre humo y la Base de lanzamiento permanece. Construye una carcasa de Hormigón de silo solo si quieres el aspecto de un silo.")),
            List.of("launch_base", "silo_controller", "silo_fuel_port", "rocket_engine", "rocket_tank", "rocket_guidance", "rocket_warhead", "propellant_cartridge"));

    // ------------------------------------------------------------------ fuel cycle and fan treatments

    public static final Scene FUEL_CYCLE = new Scene("fuel_cycle", "fuel_cycle",
            line("From Ore to Fuel", "Del mineral al combustible"),
            List.of(
                    line("Uranium goes from the ground to a reactor in a line of Create machines. First, Crushing Wheels grind Raw Uranium into Crushed Raw Uranium.",
                            "El uranio va del suelo al reactor por una línea de máquinas de Create. Primero, las Ruedas trituradoras muelen el Uranio en bruto hasta Uranio en bruto triturado."),
                    line("Washing it with a fan through water gives Uranium Concentrate.",
                            "Lavarlo con un ventilador a través de agua da Concentrado de uranio."),
                    line("A Mechanical Mixer over a heated Basin leaches the concentrate with Sulfuric Acid into Yellowcake...",
                            "Una Mezcladora mecánica sobre una Cuenca calentada lixivia el concentrado con Ácido sulfúrico hasta obtener Torta amarilla..."),
                    line("...and superheated, with hydrofluoric acid, turns yellowcake into uranium hexafluoride gas.",
                            "...y sobrecalentada, con ácido fluorhídrico, convierte la torta amarilla en gas de hexafluoruro de uranio."),
                    line("Centrifuges enrich the gas. Chain them into a cascade and pipe each stage's tails back to the stage before.",
                            "Las Centrifugadoras enriquecen el gas. Encadénalas en cascada y devuelve las colas de cada etapa a la anterior."),
                    line("Mixed with water, the enriched gas gives Uranium Dioxide, and a Mechanical Press makes pellets of it.",
                            "Mezclado con agua, el gas enriquecido da Dióxido de uranio, y una Prensa mecánica hace pastillas con él."),
                    line("Sequenced assembly on an iron sheet, two pellets and a press at a time, finishes the Uranium Reactor Fuel.",
                            "Un ensamblaje secuenciado sobre una lámina de hierro, dos pastillas y una prensa cada vez, termina el Combustible de uranio para reactor."),
                    line("The Electrolyzer makes what fusion needs: heavy water and deuterium from water, lithium from lithium carbonate.",
                            "El Electrolizador hace lo que necesita la fusión: agua pesada y deuterio a partir de agua, y litio a partir de carbonato de litio.")),
            List.of("raw_uranium", "crushed_raw_uranium", "uranium_concentrate", "yellowcake", "uranium_dioxide", "uranium_fuel_pellet",
                    "uranium_reactor_fuel", "centrifuge", "electrolyzer"));

    public static final Scene ACID_LEACHING = new Scene("fan_acid_leaching", "fan_acid_leaching",
            line("Bulk Acid Leaching", "Lixiviación ácida en masa"),
            List.of(
                    line("Acid Leaching is a fan treatment, like washing: an Encased Fan's air current takes it up passing through Sulfuric Acid.",
                            "La lixiviación ácida es un tratamiento con ventilador, como el lavado: la corriente de aire de un Ventilador revestido la recoge al atravesar Ácido sulfúrico."),
                    line("A bucket places the acid as a source block. Wall it in at the sides so it does not run off.",
                            "Un cubo coloca el ácido como bloque fuente. Ciérralo por los lados para que no se derrame."),
                    line("Items on a depot, on a belt or flying in the air current past the acid are leached.",
                            "Los ítems en un depósito, en una cinta o en vuelo en la corriente de aire después del ácido se lixivian."),
                    line("The acid dissolves what is worth keeping out of rock and sand. JEI lists every recipe of the treatment.",
                            "El ácido disuelve lo que vale la pena de la roca y la arena. JEI muestra todas las recetas del tratamiento.")),
            List.of("sulfuric_acid_bucket", "heavy_mineral_sand", "granite_fines", "tuff_fines", "deepslate_fines", FAN));

    public static final Scene IRRADIATION = new Scene("fan_irradiation", "fan_irradiation",
            line("Bulk Irradiation", "Irradiación en masa"),
            List.of(
                    line("Irradiation is a fan treatment: an Encased Fan's air current takes it up passing a strongly radioactive block.",
                            "La irradiación es un tratamiento con ventilador: la corriente de aire de un Ventilador revestido la recoge al pasar por un bloque muy radiactivo."),
                    line("Use an Irradiation Source: its open cage lets the air through, where a full radioactive block would stop the current.",
                            "Usa una Fuente de irradiación: su jaula abierta deja pasar el aire, mientras que un bloque radiactivo macizo cortaría la corriente."),
                    line("Items in the air current past it are irradiated: Thorium Dioxide breeds into Irradiated Thorium, the first step to uranium-233.",
                            "Los ítems en la corriente de aire después de él se irradian: el Dióxido de torio se convierte en Torio irradiado, el primer paso hacia el uranio-233."),
                    line("The block irradiates you too: shield it, and keep clear while it works.",
                            "El bloque también te irradia a ti: blíndalo y mantente lejos mientras trabaja.")),
            List.of("irradiation_source", "thorium_dioxide", "irradiated_thorium", "irradiated_depleted_uranium", FAN));

    // ------------------------------------------------------------------ particle accelerator

    public static final Scene ACCELERATOR_LINAC = new Scene("accelerator_linac", "accelerator_linac",
            line("Building a Linac", "Construir un linac"),
            List.of(
                    line("A particle accelerator starts at its controller. The beam leaves it on the face opposite its panel.",
                            "Un acelerador de partículas empieza en su controlador. El haz sale por la cara opuesta a su panel."),
                    line("Right in front of it goes the Ion Source, with its shaft across the beam. It turns hydrogen into protons: fill it by pipe.",
                            "Justo delante va la Fuente de iones, con el eje de través al haz. Convierte hidrógeno en protones: llénala por tubería."),
                    line("From the source the beamline runs on. Each RF Cavity adds energy: a longer linac with more cavities reaches higher energies.",
                            "Desde la fuente sigue la línea de haz. Cada Cavidad de radiofrecuencia añade energía: un linac más largo con más cavidades llega más alto."),
                    line("Quadrupole Magnets keep the beam focused. Put one at least every four blocks, or most of the beam is lost on the walls.",
                            "Los Imanes cuadrupolares mantienen el haz enfocado. Pon uno al menos cada cuatro bloques, o casi todo el haz se pierde contra las paredes."),
                    line("The line ends in a Beam Dump or a target. The dump soaks up the whole beam, and becomes radioactive doing it.",
                            "La línea termina en un Absorbedor del haz o en un blanco. El absorbedor se traga todo el haz, y se vuelve radiactivo al hacerlo."),
                    line("Klystrons, Magnet Power Supplies and Vacuum Pumps beside the line power it, from a shaft or with Forge Energy.",
                            "Los Klistrones, las Fuentes de alimentación de imanes y las Bombas de vacío junto a la línea la alimentan, con un eje o con Forge Energy."),
                    line("Right-click the controller to open its panel and start the beam. It glows through the windows of the Beam Pipes.",
                            "Haz clic derecho en el controlador para abrir su panel y arrancar el haz. Se ve brillar por las ventanas de los Tubos de haz.")),
            List.of("particle_accelerator_controller", "particle_accelerator_ion_source", "beam_pipe", "beam_rf_cavity", "beam_quadrupole",
                    "beam_dump", "particle_accelerator_klystron", "particle_accelerator_magnet_power_supply",
                    "particle_accelerator_vacuum_pump"));

    public static final Scene ACCELERATOR_SYNCHROTRON = new Scene("accelerator_synchrotron", "accelerator_synchrotron",
            line("Building a Synchrotron", "Construir un sincrotrón"),
            List.of(
                    line("A synchrotron ring takes the beam round and round, gaining energy on every turn. The injector line ends in an Injection Septum facing the ring's centre.",
                            "Un anillo sincrotrón hace girar el haz una y otra vez, ganando energía en cada vuelta. La línea de inyección termina en un Septo de inyección mirando al centro del anillo."),
                    line("The ring is a circle of radius 6 or more, one block high, and every block of it must be a ring element. Ring Dipole Magnets bend the beam round it.",
                            "El anillo es un círculo de radio 6 o más, de un bloque de alto, y cada bloque debe ser un elemento de anillo. Los Imanes dipolares de anillo curvan el haz a su alrededor."),
                    line("Put a Quadrupole Magnet in every sector of about eight blocks to keep the beam focused, and RF cavities to speed it up on each turn.",
                            "Pon un Imán cuadrupolar en cada sector de unos ocho bloques para mantener el haz enfocado, y cavidades de radiofrecuencia para acelerarlo en cada vuelta."),
                    line("Superconducting Ring Dipoles and Superconducting RF Cavities are stronger but need liquid helium: an Accelerator Cryo Feed beside the ring takes it in by pipe and gives the warm gas back.",
                            "Los Dipolos de anillo superconductores y las Cavidades de RF superconductoras son más potentes pero necesitan helio líquido: una Toma criogénica junto al anillo lo recibe por tubería y devuelve el gas caliente."),
                    line("Klystrons, Magnet Power Supplies and Vacuum Pumps serve the ring from beside it, as on a linac.",
                            "Los Klistrones, las Fuentes de alimentación de imanes y las Bombas de vacío sirven al anillo desde su lado, como en un linac."),
                    line("Exactly one Extraction Septum lets the beam out of the ring, facing the way it leaves.",
                            "Un único Septo de extracción deja salir el haz del anillo, mirando hacia donde sale."),
                    line("The extraction line ends in a Target Station with its window towards the beam. It soaks up the beam like a dump...",
                            "La línea de extracción termina en una Estación de blancos con la ventana hacia el haz. Se traga el haz como un absorbedor..."),
                    line("...and above 25 GeV its horn collects antiprotons and pushes them into a Penning trap or an Antimatter Conduit beside it. Power the trap before the first antiproton arrives!",
                            "...y por encima de 25 GeV su bocina recoge antiprotones y los empuja a una trampa de Penning o a un Conducto de antimateria a su lado. ¡Da energía a la trampa antes de que llegue el primer antiprotón!"),
                    line("Beam Interlock panels keep people out of the ring. The beam only runs while every interlock beside it is secured: right-click one to search and secure it.",
                            "Los paneles de Enclavamiento del haz mantienen a la gente fuera del anillo. El haz solo funciona mientras todos los enclavamientos junto a él están asegurados: haz clic derecho en uno para inspeccionar y asegurar."),
                    line("Sneak-right-click it, or open a door beside it, and the interlock opens and the beam trips. While the beam runs, the doors beside a panel stay locked.",
                            "Agachado y con clic derecho, o abriendo una puerta a su lado, el enclavamiento se abre y el haz se dispara. Mientras hay haz, las puertas junto a un panel quedan bloqueadas."),
                    line("Right-click the controller and start the beam: it is injected, ramped up round the ring and extracted into the station.",
                            "Haz clic derecho en el controlador y arranca el haz: se inyecta, sube de energía dando vueltas al anillo y se extrae hacia la estación.")),
            List.of("injection_septum", "ring_dipole_magnet", "superconducting_ring_dipole", "superconducting_rf_cavity", "accelerator_cryo_feed",
                    "extraction_septum", "target_station", "beam_interlock", "particle_accelerator_controller"));

    // ------------------------------------------------------------------ antimatter

    public static final Scene ANTIMATTER_CONTAINMENT = new Scene("antimatter_containment", "antimatter_containment",
            line("Antimatter Containment", "Contención de antimateria"),
            List.of(
                    line("Antimatter is kept in Penning traps: a magnetic field between two magnet heads holds a cloud of antiprotons away from the glass. Each size has its own colour.",
                            "La antimateria se guarda en trampas de Penning: un campo magnético entre dos cabezales sostiene una nube de antiprotones lejos del cristal. Cada tamaño tiene su color."),
                    line("The Small Penning Trap holds 32 ng, the Penning Trap, two blocks tall, 256 ng, and the Large Penning Trap, 2x2 and three high, 2048 ng.",
                            "La trampa de Penning pequeña guarda 32 ng, la trampa de Penning, de dos bloques de alto, 256 ng, y la trampa de Penning grande, de 2x2 y tres de alto, 2048 ng."),
                    line("Every trap needs Forge Energy all the time, more the fuller it is. A Creative RF Generator gives what its value board says; in survival, bring FE by cable.",
                            "Cada trampa necesita Forge Energy todo el tiempo, más cuanto más llena está. Un Generador de RF creativo da lo que marca su panel de valor; en supervivencia, trae FE por cable."),
                    line("The light on the front tells how the field is doing: green when stable, amber when its buffer runs low.",
                            "La luz del frente dice cómo va el campo: verde si es estable, ámbar cuando su reserva se agota."),
                    line("Right-click a powered trap with an Antimatter Vial to put it in the port. The wrench switches the port between filling the vial and emptying it into the trap.",
                            "Haz clic derecho en una trampa con energía con un Vial de antimateria para meterlo en el puerto. La llave cambia el puerto entre llenar el vial y vaciarlo en la trampa."),
                    line("A loaded vial's battery holds its field for thirty seconds away from a powered port. When it runs out, the vial annihilates wherever it is.",
                            "La batería de un vial cargado mantiene su campo treinta segundos lejos de un puerto con energía. Cuando se agota, el vial se aniquila donde esté."),
                    line("An Antimatter Conduit moves antimatter between the traps it joins. Every block of it needs a little FE; without it, nothing moves.",
                            "Un Conducto de antimateria mueve antimateria entre las trampas que une. Cada bloque necesita un poco de FE; sin ella, no se mueve nada."),
                    line("If a loaded trap loses its power, its buffer lasts about ten seconds. Then it quenches: red lights, sparks and an alarm for three seconds...",
                            "Si una trampa cargada pierde la energía, su reserva aguanta unos diez segundos. Después entra en quench: luces rojas, chispas y una alarma durante tres segundos..."),
                    line("...and the antimatter annihilates: a blast that grows with the charge, a flash and a pulse of gamma rays. It leaves no fallout.",
                            "...y la antimateria se aniquila: una explosión que crece con la carga, un destello y un pulso de rayos gamma. No deja lluvia radiactiva."),
                    line("Putting antimatter into an unpowered trap, or breaking a loaded one, annihilates it at once. The blast breaks the traps beside it, and they go off in turn: keep them apart!",
                            "Meter antimateria en una trampa sin energía, o romper una cargada, la aniquila al instante. La explosión rompe las trampas de al lado, que estallan a su vez: ¡sepáralas!")),
            List.of("small_penning_trap", "penning_trap", "large_penning_trap", "antimatter_vial", "antimatter_conduit", "creative_rf_generator"));

    public static final Scene ANTIMATTER_GENERATOR = new Scene("antimatter_generator", "antimatter_generator",
            line("Annihilation Generator", "Generador de aniquilación"),
            List.of(
                    line("The Annihilation Generator burns antiprotons in a small chamber of its own, 1 ng, behind the porthole on its back.",
                            "El Generador de aniquilación quema antiprotones en una pequeña cámara propia, de 1 ng, tras el ojo de buey de su parte trasera."),
                    line("It draws antimatter from a Penning trap or an Antimatter Conduit beside it, or from a vial put in the socket on its top.",
                            "Toma antimateria de una trampa de Penning o de un Conducto de antimateria a su lado, o de un vial metido en el zócalo de arriba."),
                    line("To start, its chamber needs FE for its field: without it the generator takes no antimatter in, and a loaded vial in its socket runs its battery flat.",
                            "Para arrancar, su cámara necesita FE para su campo: sin ella el generador no admite antimateria, y un vial cargado en su zócalo agota su batería."),
                    line("A redstone signal sets the throttle. With no signal it keeps a small standby burn that holds its own field, and the mode lamp blinks.",
                            "Una señal de redstone fija la potencia. Sin señal mantiene una pequeña combustión de reserva que sostiene su propio campo, y la luz del modo parpadea."),
                    line("The panel on top, or the wrench on that face, picks the mode. Rotation turns the shaft at 256 RPM with a great deal of stress capacity...",
                            "El panel de arriba, o la llave en esa cara, elige el modo. Rotación gira el eje a 256 RPM con muchísima capacidad de estrés..."),
                    line("...Electric pushes FE out of its terminals into every block beside it, and Split gives half of each.",
                            "...Eléctrico empuja FE por sus bornes a todos los bloques de al lado, y Mixto da la mitad de cada cosa."),
                    line("In Electric or Split mode it powers the trap that feeds it and its own chamber: once running, it no longer needs any other source.",
                            "En modo Eléctrico o Mixto alimenta la trampa que lo abastece y su propia cámara: una vez en marcha, ya no necesita otra fuente."),
                    line("The core glows brighter the harder it burns and flickers when the supply cannot keep up. A comparator reads its output.",
                            "El núcleo brilla más cuanto más fuerte quema y titila cuando el suministro no da abasto. Un comparador lee su potencia.")),
            List.of("annihilation_generator"));

    public static final Scene ANTIPROTON_INJECTOR = new Scene("antiproton_injector", "antiproton_injector",
            line("Antiproton Injector", "Inyector de antiprotones"),
            List.of(
                    line("The Antiproton Injector is a block of a tokamak's shell. Its port faces out; its nozzle looks into the plasma.",
                            "El Inyector de antiprotones es un bloque de la carcasa de un tokamak. Su puerto mira hacia fuera; su tobera, hacia el plasma."),
                    line("It needs FE and antimatter: from a trap or a conduit beside it, or a vial in its port. Its little chamber holds four doses.",
                            "Necesita FE y antimateria: de una trampa o un conducto a su lado, o de un vial en su puerto. Su pequeña cámara guarda cuatro dosis."),
                    line("The wrench sets it to fire on its own, only with a redstone signal, or never. The amber lamp shows it armed.",
                            "La llave lo pone a disparar solo, solo con señal de redstone o nunca. La luz ámbar indica que está armado."),
                    line("The green lamp means it is ready: armed, powered and loaded with a dose. Only the plasma is missing.",
                            "La luz verde indica que está listo: armado, con energía y cargado con una dosis. Solo falta el plasma."),
                    line("While the tokamak heats, once its plasma is well confined and dense enough, the injector fires one dose of antiprotons into it...",
                            "Mientras el tokamak calienta, en cuanto su plasma está bien confinado y es lo bastante denso, el inyector le dispara una dosis de antiprotones..."),
                    line("...and their annihilation ignites the plasma at once, without heating it all the way. Then the injector cools down for ten seconds.",
                            "...y su aniquilación enciende el plasma al momento, sin calentarlo del todo. Después el inyector se enfría durante diez segundos.")),
            List.of("antiproton_injector"));

    public static final Scene ISOTOPES = new Scene("isotopes", "isotopes",
            line("Isotopes and Nuclear Medicine", "Isótopos y medicina nuclear"),
            List.of(
                    line("Cobalt-60 is made in a fission reactor: build a Reactor Irradiation Channel into its wall in place of a casing block.",
                            "El cobalto-60 se fabrica en un reactor de fisión: construye un Canal de irradiación del reactor en su pared en lugar de un bloque de carcasa."),
                    line("Load it with cobalt ingots. While the reactor runs, its neutrons activate the whole batch, which comes out as cobalt-60 sources.",
                            "Cárgalo con lingotes de cobalto. Mientras el reactor funciona, sus neutrones activan todo el lote, que sale como fuentes de cobalto-60."),
                    line("Every isotope decays on its own clock, even in a chest: the bar under the item shows the activity left, and the Geiger counter follows it.",
                            "Cada isótopo se desintegra con su propio reloj, incluso en un cofre: la barra bajo el objeto muestra la actividad que queda, y el contador Geiger la sigue."),
                    line("A Gamma Irradiator holds up to four cobalt-60 sources and works two blocks above a depot or a belt, like a press.",
                            "Un Irradiador gamma guarda hasta cuatro fuentes de cobalto-60 y trabaja dos bloques por encima de un depósito o una cinta, como una prensa."),
                    line("Harmful food stops under it while the shutter opens, and comes out sterilised: no harmful effects. Stay clear while it is open!",
                            "La comida dañina se detiene debajo mientras se abre el obturador, y sale esterilizada: sin efectos dañinos. ¡Apártate mientras está abierto!"),
                    line("A Smoke Detector, made with americium-241, gives a redstone signal when fire, a lit campfire or lava is close.",
                            "Un Detector de humo, hecho con americio-241, da una señal de redstone cuando hay fuego, una fogata encendida o lava cerca.")),
            List.of("reactor_irradiation_channel", "gamma_irradiator", "smoke_detector"));

    public static final Scene MAINTENANCE = new Scene("maintenance", "maintenance",
            line("Plant Maintenance", "Mantenimiento de la central"),
            List.of(
                    line("A plant's parts wear with use: the reactor's control rod drives and relief valve, a steam generator's tubes, a turbine's blades. Worn out, they fail.",
                            "Las piezas de una central se desgastan con el uso: los mecanismos de las barras y la válvula de alivio del reactor, los tubos de un generador de vapor, los álabes de una turbina. Gastadas, fallan."),
                    line("A reactor shut down cold with every rod in is in a refuelling outage: only then does its fuel port take fresh fuel.",
                            "Un reactor parado en frío y con todas las barras dentro está en parada de recarga: solo entonces su puerto de combustible admite combustible nuevo."),
                    line("In the outage, a Maintenance Kit used on any block of the reactor services its parts and frees a stuck rod. Steam generators and turbines just need to be stopped.",
                            "En la parada, un kit de mantenimiento usado en cualquier bloque del reactor revisa sus piezas y libera una barra atascada. Los generadores de vapor y las turbinas solo tienen que estar parados."),
                    line("Goggles and the Radiography Camera show how worn each part is, so it can be serviced before it fails.",
                            "Las gafas y la cámara de radiografía muestran lo gastada que está cada pieza, para revisarla antes de que falle."),
                    line("Now and then the ground shakes: a reactor past its seismic trip scrams, and every incident goes into the server's log on the INES scale.",
                            "De vez en cuando tiembla la tierra: un reactor que pasa su disparo sísmico entra en SCRAM, y cada incidente queda en el registro del servidor con la escala INES.")),
            List.of("maintenance_kit"));

    // ------------------------------------------------------------------ control room

    public static final String CONTROL_ROOM_TAG_ID = "control_room";
    public static final String CONTROL_ROOM_TAG_KEY = "create_nuclearindustry.ponder.tag." + CONTROL_ROOM_TAG_ID;
    public static final Line CONTROL_ROOM_TAG_TITLE = line("Control Room", "Sala de control");
    public static final Line CONTROL_ROOM_TAG_DESCRIPTION = line(
            "Everything to watch and run a plant from one room: screens, consoles, modules, annunciators, recorders and alarms",
            "Todo para vigilar y manejar una central desde una sala: pantallas, consolas, módulos, anunciadores, registradores y alarmas");

    public static final Scene CONTROL_ROOM = new Scene("control_room", "control_room",
            line("The Control Room", "La sala de control"),
            List.of(
                    line("A control room runs a whole plant from one place. This one is laid out like a real plant's: tall instrument boards on the back wall, and the operators' desks set back in front of them.",
                            "Una sala de control maneja toda una central desde un solo sitio. Esta se distribuye como la de una central real: paneles altos de instrumentos en la pared del fondo y los pupitres de los operadores retirados frente a ellos."),
                    line("Every device starts unlinked. Click any block of a machine with the Control Room Linker, then click the device: reactors, steam generators, containments, turbines, cooling towers and tokamaks.",
                            "Todo dispositivo empieza sin enlazar. Haz clic con el Enlazador de sala de control en cualquier bloque de una máquina y luego en el dispositivo: reactores, generadores de vapor, contenciones, turbinas, torres y tokamaks."),
                    line("Linked to any other block, a device watches the radiation there. Devices reach 128 blocks; sneak-click one with the linker to unlink it.",
                            "Enlazado a cualquier otro bloque, un dispositivo vigila la radiación allí. Los dispositivos llegan a 128 bloques; agachado, el enlazador desenlaza uno."),
                    line("Desks, tables and wall panels touching side by side, and wall panels above one another, make one console with one link: link any block of it and the whole console follows.",
                            "Los pupitres, mesas y paneles de pared que se tocan de lado, y los paneles de pared uno encima de otro, forman una consola con un solo enlace: enlaza cualquier bloque y toda la consola lo sigue."),
                    line("So the whole back board is one console on the reactor. Wall Control Panels hang on the face of a solid block and hold four modules upright.",
                            "Así, todo el panel del fondo es una consola enlazada al reactor. Los Paneles de control de pared se cuelgan de la cara de un bloque sólido y llevan cuatro módulos en vertical."),
                    line("Monitors side by side join into one screen of up to 8x8. A big screen lists the machine's readings beside a two-minute graph; a lone monitor shows one big reading per page.",
                            "Los monitores juntos forman una pantalla de hasta 8x8. Una pantalla grande lista las lecturas de la máquina junto a una gráfica de dos minutos; un monitor solo muestra una lectura grande por página."),
                    line("Click a screen to change page, and wrench it to choose its title, layout, style, rows and up to three graph lines. A comparator reads its main value.",
                            "Haz clic en una pantalla para cambiar de página, y usa la llave para elegir su título, distribución, estilo, filas y hasta tres líneas de gráfica. Un comparador lee su valor principal."),
                    line("Control Desks join side by side and turn corners like stairs, so a console can wrap round its operators. A corner desk holds one module on each arm.",
                            "Los Pupitres de control se unen de lado y doblan esquinas como las escaleras, así una consola puede rodear a sus operadores. Un pupitre de esquina lleva un módulo en cada brazo."),
                    line("Consoles on different walls are separate: this one runs the tokamak. Each monitor screen links on its own, here to the tokamak and to a turbine, each in a style of its own.",
                            "Las consolas de paredes distintas van por separado: esta maneja el tokamak. Cada pantalla de monitores se enlaza por su cuenta, aquí al tokamak y a una turbina, cada una con su propio estilo."),
                    line("Control Tables have a flat top, level with a desk's rail: here, the shift supervisor's at the back. Tables apart from the desks make a console of their own.",
                            "Las Mesas de control tienen la tapa plana, a la altura del borde de un pupitre: aquí, la del jefe de turno al fondo. Las mesas apartadas de los pupitres forman su propia consola."),
                    line("Look at a module or a console to see its card on the right of the screen: what it is set to, what it shows, where its redstone goes and what the console is linked to.",
                            "Mira un módulo o una consola para ver su ficha a la derecha de la pantalla: cómo está configurado, qué muestra, a dónde va su redstone y a qué está enlazada la consola.")),
            List.of("control_room_linker", "control_monitor", "reactor_control_panel", "control_table", "wall_control_panel"));

    public static final Scene CONTROL_MODULES = new Scene("control_room_modules", "control_room",
            line("Control Modules", "Módulos de control"),
            List.of(
                    line("Fit a control module by clicking a free slot with it. Straight desks, tables and wall panels hold four; a corner desk holds one on each arm.",
                            "Coloca un módulo de control haciendo clic con él en un hueco libre. Los pupitres rectos, las mesas y los paneles de pared llevan cuatro; un pupitre de esquina, uno en cada brazo."),
                    line("Wrench a module to set it up: its function, label, reading or alarm, and its redstone output. Functions the linked machine cannot use are greyed out.",
                            "Usa la llave inglesa en un módulo para configurarlo: su función, etiqueta, lectura o alarma, y su salida de redstone. Las funciones que la máquina enlazada no admite salen en gris."),
                    line("Sneak-wrench a module to take just that module out. Sneak-wrench the block anywhere else to pick it up; its modules drop beside it.",
                            "Agachado, la llave quita solo ese módulo. Agachado en cualquier otra parte del bloque, la llave lo recoge y sus módulos caen al lado."),
                    line("Click a module with an empty hand to work it. The Push Button SCRAMs the reactor or resets the SCRAM, sends a pulse, or starts and shuts down a tokamak.",
                            "Haz clic en un módulo con la mano vacía para accionarlo. El Pulsador hace SCRAM al reactor o lo rearma, manda un pulso, o arranca y para un tokamak."),
                    line("The Guarded Button keeps a SCRAM or a shutdown under a hinged cover: the first click lifts the cover, the second presses the button.",
                            "El Pulsador con tapa guarda un SCRAM o una parada bajo una tapa con bisagra: el primer clic levanta la tapa y el segundo pulsa el botón."),
                    line("A sneaking click lets the cover down, and it falls shut by itself five seconds after the last click.",
                            "Un clic agachado baja la tapa, y se cierra sola cinco segundos después del último clic."),
                    line("Levers switch the automatic SCRAM, the pressurizer heaters and spray, a tokamak's fuel, heating, start or shutdown, or a plain signal. The Key Switch turns a signal on and off.",
                            "Las Palancas activan el SCRAM automático, los calentadores y el rociado del presionador, el combustible, el calentamiento, el arranque o la parada de un tokamak, o una señal. El Interruptor de llave enciende y apaga una señal."),
                    line("Dials set the control rods' target, the automatic SCRAM threshold or a redstone level: click to turn one up, sneak-click to turn it down.",
                            "Las Ruedas fijan el objetivo de las barras de control, el umbral del SCRAM automático o un nivel de redstone: clic para subir, clic agachado para bajar."),
                    line("The Controller is an automatic operator: it holds reactor power or temperature through the rod target, primary pressure through the pressurizer, or a tokamak's fusion power.",
                            "El Controlador es un operador automático: mantiene la potencia o la temperatura del reactor con el objetivo de barras, la presión del primario con el presionador o la potencia de fusión de un tokamak."),
                    line("Its lamp is green while it drives. Click it to raise the setpoint and sneak-click to lower it; its setup switches it to standby.",
                            "Su piloto está verde mientras actúa. Haz clic para subir la consigna y clic agachado para bajarla; en su configuración se pasa a reserva."),
                    line("Indicator Lamps light on SCRAM, running, no signal, an open relief valve or any alarm. Gauges show one reading on a dial.",
                            "Los Pilotos se encienden con el SCRAM, en marcha, sin señal, con la válvula de alivio abierta o con cualquier alarma. Los Medidores muestran una lectura en una esfera.")),
            List.of("push_button_module", "guarded_button_module", "lever_module", "key_switch_module", "dial_module", "controller_module",
                    "lamp_module", "gauge_module", "reactor_control_panel", "control_table", "wall_control_panel"));

    public static final Scene CONTROL_INSTRUMENTS = new Scene("control_room_instruments", "control_room",
            line("Instruments and Screens", "Instrumentos y pantallas"),
            List.of(
                    line("LED Displays show a reading in digits with its unit. In a row they join into one wider display of up to eight modules, set up by the leftmost: the wider, the more digits, with a label on top.",
                            "Los Displays LED muestran una lectura en cifras con su unidad. En fila se unen en un display más ancho de hasta ocho módulos, que configura el de la izquierda: cuanto más ancho, más cifras, con una etiqueta encima."),
                    line("Too big a value reads OL; a reading the machine does not have reads dashes.",
                            "Un valor demasiado grande se lee OL; una lectura que la máquina no tiene se lee con guiones."),
                    line("Bar Graphs show a reading as a column of twenty lights: green, amber in the 15%% under the alarm level and red from it. The alarm level is set in the module's screen.",
                            "Las Barras gráficas muestran una lectura como una columna de veinte luces: verde, ámbar en el 15%% bajo el nivel de alarma y roja desde él. El nivel de alarma se fija en la pantalla del módulo."),
                    line("Trend Recorders draw a reading over their last 48 samples, one every 1, 5 or 20 seconds.",
                            "Los Registradores de tendencia dibujan una lectura en sus últimas 48 muestras, una cada 1, 5 o 20 segundos."),
                    line("The Core Map shows a fission core from above, north at the top: fuel green to red as it burns, control channels filling blue as their rods go in. Four in one block make one big map.",
                            "El Mapa del núcleo muestra un núcleo de fisión desde arriba, con el norte arriba: el combustible de verde a rojo al quemarse y los canales llenándose de azul al entrar sus barras. Cuatro en un bloque hacen un mapa grande."),
                    line("The Plasma Section is a cut through a tokamak: its plasma glows with the temperature, swells with the density and shakes as it grows unstable. Four in one block make one big screen too.",
                            "La Sección del plasma es un corte de un tokamak: el plasma brilla con la temperatura, crece con la densidad y tiembla al volverse inestable. Cuatro en un bloque forman también una pantalla grande."),
                    line("Annunciator Panels hang on the wall, six alarm windows each, and side by side make one board. Link a window with the linker, or the plate for all six; wrench it to set its alarm, threshold, severity and label.",
                            "Los Paneles anunciadores cuelgan de la pared, con seis ventanas cada uno, y juntos forman un tablero. Enlaza una ventana con el enlazador, o la placa para las seis; con la llave eliges su alarma, umbral, severidad y etiqueta."),
                    line("A tripped window flashes and the horn sounds until ACK; it stays lit while the alarm lasts, flashes slowly once clear and goes dark on RESET. Alarms are red, warnings amber; the board rings the first to trip.",
                            "Una ventana que salta parpadea y suena la bocina hasta RECON.; sigue encendida mientras dura, parpadea despacio al despejarse y se apaga al rearmar. Alarmas en rojo, avisos en ámbar; la primera en saltar queda rodeada."),
                    line("The board's buttons sit at the right of its bottom strip: TEST lights every lamp, SIL quiets the horn, then ACK and RESET. Redstone presses them: left ACK, right RESET, above SIL, below TEST. A waiting alarm powers the wall.",
                            "Los botones están a la derecha de la franja inferior: PRUEBA enciende las lámparas, SIL. calla la bocina, luego RECON. y REARME. La redstone los pulsa: izquierda RECON., derecha REARME, arriba SIL., abajo PRUEBA."),
                    line("Chart Recorders draw up to three readings on paper. Feed them paper by hand or with a hopper, and wrench them to choose the pens and the speed.",
                            "Los Registradores gráficos dibujan hasta tres lecturas en papel. Dales papel a mano o con una tolva, y usa la llave para elegir las plumas y la velocidad."),
                    line("Sneak-click one with an empty hand to tear the chart off as a Chart Roll, to read anywhere. A comparator reads the paper left.",
                            "Agachado y con la mano vacía, arranca la gráfica como un Rollo de gráfica para leerla donde quieras. Un comparador lee el papel que queda."),
                    line("Nuclear Alarms ring and power every side once their reading passes the threshold. Pick the reading on the top face and the threshold on the sides.",
                            "Las Alarmas nucleares suenan y dan señal por todos sus lados cuando su lectura pasa el umbral. Elige la lectura en la cara superior y el umbral en los laterales.")),
            List.of("led_display_module", "bar_graph_module", "trend_recorder_module", "core_map_module", "plasma_section_module",
                    "annunciator_panel", "chart_recorder", "chart_roll", "nuclear_alarm", "control_monitor"));

    public static final Scene CONTROL_SIGNALS = new Scene("control_room_signals", "control_room",
            line("Signals Out of the Room", "Señales fuera de la sala"),
            List.of(
                    line("Any module can put its state out as redstone, chosen in its setup: a lever or a lamp on or off, a button's pulse, a dial's level or a reading from 0 to 15.",
                            "Cualquier módulo puede sacar su estado como redstone, elegido en su configuración: una palanca o un piloto, el pulso de un pulsador, el nivel de una rueda o una lectura de 0 a 15."),
                    line("Below the desk: out of the bottom and the back of the block it sits in.",
                            "Bajo el pupitre: sale por debajo y por detrás del bloque en el que está."),
                    line("On a Create Redstone Link frequency: pick its two items in the setup, and every receiver on that frequency within range puts the signal out.",
                            "En una frecuencia de Enlace de redstone de Create: elige sus dos objetos en la configuración, y cada receptor en esa frecuencia y a su alcance da la señal."),
                    line("Or on an Advanced Redstone Link code: four letters or digits instead of two items. Click the link to type its code; sneak-click it to switch between sending and receiving.",
                            "O en un código de Enlace de redstone avanzado: cuatro letras o cifras en lugar de dos objetos. Haz clic en el enlace para escribir su código; agachado, cambia entre emitir y recibir."),
                    line("Control rods only move with pistons. Reactor Redstone Ports set to Insert rods or Withdraw rods light up until the rods reach the dialled target: wire them to the clutch and gearshift of the rod pistons.",
                            "Las barras de control solo se mueven con pistones. Los Puertos de redstone del reactor en Insertar barras o Extraer barras se encienden hasta que llegan al objetivo: conéctalos al embrague y la caja de cambios de los pistones."),
                    line("A port in SCRAM mode lights when SCRAM is pressed or the automatic SCRAM trips: wire it to drive every rod in, whatever else the room is doing.",
                            "Un puerto en modo SCRAM se enciende al pulsar SCRAM o al saltar el SCRAM automático: conéctalo para meter todas las barras, haga lo que haga el resto de la sala.")),
            List.of("advanced_redstone_link", "reactor_redstone_port", "reactor_control_panel", "wall_control_panel"));

    public static final Scene CONTROL_SERVO = new Scene("servo_gearbox", "servo_gearbox",
            line("The Servo Gearbox", "La caja servo"),
            List.of(
                    line("The Servo Gearbox puts a Mechanical Piston or a Rope Pulley where a redstone signal says. Either end of its shaft takes the rotation; the other end drives.",
                            "La Caja servo pone un Pistón mecánico o una Polea de cuerda donde dice una señal de redstone. Cualquier extremo de su eje toma el giro; el otro mueve."),
                    line("It finds the actuator through shafts, gearboxes and cogs, and moves it a share of its stroke: no signal keeps it in, a full signal runs it all the way.",
                            "Encuentra el actuador a través de ejes, cajas de engranajes y ruedas dentadas, y lo mueve una parte de su carrera: sin señal queda recogido, con señal máxima llega hasta el final."),
                    line("Scroll the top or bottom to set the stroke in blocks. It is cut to how far the actuator reaches: a piston's poles, a pulley's rope.",
                            "Rueda arriba o abajo para fijar la carrera en bloques. Se limita a lo que llega el actuador: los postes de un pistón, la cuerda de una polea."),
                    line("A weaker signal, a shorter move: 8 of 15 of a 3-block stroke is 1.6, so 2 blocks. It always stops on a whole block.",
                            "Menos señal, menos recorrido: 8 de 15 de una carrera de 3 bloques son 1,6, así que 2 bloques. Siempre se para en un bloque entero."),
                    line("Scroll the other two sides to choose whether a stronger signal extends or retracts.",
                            "Rueda en los otros dos lados para elegir si más señal extiende o recoge."),
                    line("It stops the actuator exactly on the block and lets it set its blocks down. Every actuator turned at the same speed moves alike; if something blocks the way, it waits and tries again.",
                            "Detiene el actuador justo en el bloque y lo deja asentar sus bloques. Todos los actuadores que giran a la misma velocidad se mueven igual; si algo bloquea el paso, espera y lo vuelve a intentar."),
                    line("A comparator reads where the actuator stands, on the same scale as the signal.",
                            "Un comparador lee dónde está el actuador, en la misma escala que la señal."),
                    line("Send it the redstone of a Control Desk dial set to Control rod target, and the rods follow the desk without clutch or gearshift.",
                            "Mándale la redstone de una rueda de un Pupitre de control en Objetivo de las barras, y las barras siguen al pupitre sin embrague ni cambio de marcha.")),
            List.of("servo_gearbox"));

    // ------------------------------------------------------------------ nuclear engines

    public static final Scene THERMAL_ENGINE = new Scene("nuclear_thermal_engine", "nuclear_thermal_engine",
            line("Nuclear Thermal Engine", "Motor térmico nuclear"),
            List.of(
                    line("A rocket for Create Aeronautics ships, heated by a reactor. Build it in a line: the controller, heat exchanger segments along the engine, and a nozzle facing the way the exhaust leaves.",
                            "Un cohete para naves de Create Aeronautics calentado por un reactor. Constrúyelo en línea: el controlador, segmentos de intercambiador a lo largo del motor y una tobera mirando hacia donde sale el escape."),
                    line("Pipe hot primary coolant from a pressurized reactor and a propellant into the controller, and take the cooled primary back from it.",
                            "Mete por tubería en el controlador refrigerante primario caliente de un reactor de agua a presión y un propelente, y saca de él el primario ya frío."),
                    line("Water, deuterium or hydrogen: the lighter the gas, the harder it pushes. Each segment passes more propellant.",
                            "Agua, deuterio o hidrógeno: cuanto más ligero el gas, más empuja. Cada segmento deja pasar más propelente."),
                    line("A redstone signal sets the throttle. Aboard a ship it pushes the ship opposite to its nozzle.",
                            "Una señal de redstone fija el acelerador. En una nave la empuja en sentido contrario a su tobera."),
                    line("The nozzle heats with the flow, water most of all. Run too hot it wears and bursts, spilling the primary's activity; its exhaust leaves a trace of it where the jet lands.",
                            "La tobera se calienta con el caudal, sobre todo con agua. Demasiado caliente se desgasta y revienta, esparciendo la actividad del primario; su escape deja un rastro allí donde cae el chorro.")),
            List.of("nuclear_thermal_engine", "thermal_engine_exchanger", "thermal_rocket_nozzle"));

    public static final Scene ANTIMATTER_ENGINE = new Scene("antimatter_engine", "antimatter_engine",
            line("Antimatter Engine", "Motor de antimateria"),
            List.of(
                    line("The strongest engine there is. The controller is an annihilation chamber; behind it, magnetic coils and a magnetic nozzle.",
                            "El motor más potente que existe. El controlador es una cámara de aniquilación; detrás, bobinas magnéticas y una tobera magnética."),
                    line("Feed it antimatter with a conduit or a vial in its port, and Forge Energy for the coils' field.",
                            "Aliméntalo con antimateria por un conducto o con un vial en su puerto, y con Forge Energy para el campo de las bobinas."),
                    line("More coils turn more of the annihilation into thrust. What they miss heats them, and too few at full throttle wear through.",
                            "Cuantas más bobinas, más aniquilación se convierte en empuje. Lo que se escapa las calienta, y pocas a fondo se desgastan."),
                    line("The gamma rays they miss stream out behind the nozzle: keep out of the jet.",
                            "Los rayos gamma que se escapan salen detrás de la tobera: no te pongas en el chorro."),
                    line("Without energy the field fails: a quench alarm sounds, and the chamber's antimatter annihilates where the engine is.",
                            "Sin energía el campo cae: suena una alarma de quench y la antimateria de la cámara se aniquila donde está el motor.")),
            List.of("antimatter_engine", "magnetic_coil", "magnetic_nozzle"));

    /** Every guide scene, in the order they are registered. */
    public static final List<Scene> SCENES = List.of(SIRENS, BLAST_DOOR, SHELTER, BOMBS, THERMONUCLEAR, MISSILE, SILO_LAUNCH, LAUNCH_BASE, RADAR, HOLO_TABLE, FUEL_CYCLE, ACID_LEACHING, IRRADIATION,
            ACCELERATOR_LINAC, ACCELERATOR_SYNCHROTRON, ANTIMATTER_CONTAINMENT, ANTIMATTER_GENERATOR, ANTIPROTON_INJECTOR,
            ISOTOPES, MAINTENANCE, CONTROL_ROOM, CONTROL_MODULES, CONTROL_INSTRUMENTS, CONTROL_SIGNALS, CONTROL_SERVO,
            THERMAL_ENGINE, ANTIMATTER_ENGINE);

    private GuidePonderText() {}

    private static Line line(String english, String spanish) {
        return new Line(english, spanish);
    }
}
