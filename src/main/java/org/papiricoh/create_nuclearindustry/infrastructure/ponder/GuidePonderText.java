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
                    line("The more it holds, and the richer, the bigger its crater: up to 250 blocks out from ground zero.",
                            "Cuanto más lleva, y más rico, mayor es su cráter: hasta 250 bloques desde la zona cero."),
                    line("Power it with redstone: after a ten second countdown it goes off. Sirens in reach sound the attack warning meanwhile.",
                            "Dale una señal de redstone: tras una cuenta atrás de diez segundos estalla. Mientras, las sirenas cercanas dan el aviso de ataque."),
                    line("Beyond the crater a blast wave rolls outwards, hurting and throwing whoever stands in the open, and fallout settles on the ground.",
                            "Más allá del cráter avanza una onda expansiva que hiere y lanza a quien esté al descubierto, y la lluvia radiactiva cae sobre el suelo."),
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
                    line("Right-click a block with a Target Designator to aim, then put the designator and a missile in the pad. The designator is kept.",
                            "Haz clic derecho en un bloque con un Designador de objetivo para apuntar, y pon el designador y un misil en la plataforma. El designador se conserva."),
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

    /** Every guide scene, in the order they are registered. */
    public static final List<Scene> SCENES = List.of(SIRENS, BLAST_DOOR, SHELTER, BOMBS, MISSILE, FUEL_CYCLE, ACID_LEACHING, IRRADIATION,
            ACCELERATOR_LINAC);

    private GuidePonderText() {}

    private static Line line(String english, String spanish) {
        return new Line(english, spanish);
    }
}
