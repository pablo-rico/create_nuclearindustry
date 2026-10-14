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
                    line("Without redstone it watches the sky: a missile, a bomb or a launch countdown aimed within 96 blocks of it, plus its fallout's reach, sets off the faster attack warning.",
                            "Sin redstone vigila el cielo: un misil, una bomba o una cuenta atrás de lanzamiento apuntados a menos de 96 bloques, más el alcance de su lluvia radiactiva, disparan el aviso de ataque, más rápido."),
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
                    line("The Nuclear Bomb takes weapons-grade material in its three slots: uranium enriched to 90%% or more, or plutonium, which counts double.",
                            "La Bomba nuclear lleva material de grado militar en sus tres huecos: uranio enriquecido al 90%% o más, o plutonio, que cuenta doble."),
                    line("The more it holds, and the richer, the further it wrecks: up to 250 blocks out, round a crater a third as wide.",
                            "Cuanto más lleva, y más rico, más lejos arrasa: hasta 250 bloques, alrededor de un cráter de un tercio de ese ancho."),
                    line("Power it with redstone: after a ten second countdown it goes off. Sirens in reach sound the attack warning meanwhile.",
                            "Dale una señal de redstone: tras una cuenta atrás de diez segundos estalla. Mientras, las sirenas cercanas dan el aviso de ataque."),
                    line("Its flash burns whoever sees it, a blast wave rolls outwards throwing whoever stands in the open, and fallout drifts downwind.",
                            "Su destello quema a quien lo ve, una onda expansiva avanza lanzando a quien esté al descubierto y la lluvia radiactiva se desplaza con el viento."),
                    line("The Dirty Bomb takes anything radioactive that is not weapons-grade: waste, spent fuel, fragments, RTG pellets.",
                            "La Bomba sucia lleva cualquier cosa radiactiva que no sea de grado militar: residuos, combustible gastado, fragmentos, pastillas de RTG."),
                    line("Its blast is small and leaves no crater, but it scatters its load as fallout, wider the more radioactive the load.",
                            "Su explosión es pequeña y no deja cráter, pero esparce su carga como lluvia radiactiva, más lejos cuanto más radiactiva sea.")),
            List.of("nuclear_bomb", "dirty_bomb"));

    public static final Scene MISSILE = new Scene("weapons_missile", "weapons_missile",
            line("Missile Launch Pads", "Plataformas de lanzamiento"),
            List.of(
                    line("A Missile Launch Pad fires a Nuclear Missile at a target far away.",
                            "Una Plataforma de lanzamiento dispara un Misil nuclear contra un objetivo lejano."),
                    line("Right-click a block with a Target Designator to aim, then put the designator and a missile in the pad. Sneak-use it in the air for an airburst.",
                            "Haz clic derecho en un bloque con un Designador de objetivo para apuntar, y pon el designador y un misil en la plataforma. Úsalo agachado al aire para una explosión aérea."),
                    line("Its three warhead slots take weapons-grade uranium or plutonium for a nuclear warhead, or other radioactive material for a dirty one. Both together salt the fallout.",
                            "Sus tres huecos de cabeza admiten uranio o plutonio de grado militar para una cabeza nuclear, u otro material radiactivo para una sucia. Ambos a la vez ensucian la lluvia radiactiva."),
                    line("Power the pad with redstone: after a ten second countdown the missile lifts off and flies to its target.",
                            "Dale a la plataforma una señal de redstone: tras una cuenta atrás de diez segundos el misil despega y vuela hasta su objetivo."),
                    line("Air Raid Sirens near the target hear the countdown and sound the attack warning before the missile lands.",
                            "Las Sirenas antiaéreas cerca del objetivo oyen la cuenta atrás y dan el aviso de ataque antes de que caiga el misil.")),
            List.of("launch_pad", "missile", "target_designator"));

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
                    line("Annunciator Panels line the top of the board, six alarm windows each. Link a window with the linker, or the frame for all six, and wrench it to choose its alarm, threshold and label.",
                            "Los Paneles anunciadores recorren lo alto del panel, con seis ventanas de alarma cada uno. Enlaza una ventana con el enlazador, o el marco para las seis, y usa la llave para elegir su alarma, umbral y etiqueta."),
                    line("A window that trips flashes and the horn sounds until ACK; then it stays lit while the alarm lasts, flashes slowly once it clears and goes dark on RESET. The first to trip is marked first out.",
                            "Una ventana que salta parpadea y suena la bocina hasta ACK; luego queda encendida mientras dura la alarma, parpadea despacio al despejarse y se apaga con RESET. La primera en saltar se marca como primera."),
                    line("Redstone pulses into its left and right sides press ACK and RESET. Its back and bottom give a signal while an alarm waits, and a comparator reads how many windows are lit.",
                            "Los pulsos de redstone en sus lados izquierdo y derecho pulsan ACK y RESET. Su parte trasera e inferior dan señal mientras una alarma espera, y un comparador lee cuántas ventanas hay encendidas."),
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

    /** Every guide scene, in the order they are registered. */
    public static final List<Scene> SCENES = List.of(SIRENS, BLAST_DOOR, SHELTER, BOMBS, MISSILE, FUEL_CYCLE, ACID_LEACHING, IRRADIATION,
            ACCELERATOR_LINAC, CONTROL_ROOM, CONTROL_MODULES, CONTROL_INSTRUMENTS, CONTROL_SIGNALS, CONTROL_SERVO);

    private GuidePonderText() {}

    private static Line line(String english, String spanish) {
        return new Line(english, spanish);
    }
}
