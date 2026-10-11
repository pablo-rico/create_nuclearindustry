package org.papiricoh.create_nuclearindustry.infrastructure.ponder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Text and wiring of the "Nuclear power plant" Ponder series: the ponder tag, the chained scenes
 * (an overview of the whole plant, then four on its parts) with every line in English and
 * Spanish, and the items each scene is reached from.
 *
 * <p>Pure Java on purpose: the scenes ({@code PlantScenes}) take their lines from here in the
 * order they show them, the datagen writes the lang keys from the same lists, and JUnit checks
 * that both agree, so a key can never drift from the text box it belongs to.
 */
public final class PlantPonderText {

    public record Line(String english, String spanish) {}

    /**
     * One scene: its Ponder id (lang keys {@code create_nuclearindustry.ponder.<id>.header} and
     * {@code .text_N}), the schematic it is built on and the items whose Ponder page shows it.
     */
    public record Scene(String id, String schematic, Line title, List<Line> lines, List<String> components) {
        public String headerKey() {
            return KEY_PREFIX + id + ".header";
        }

        /** Key of the {@code n}th text box, counting from 1 as Ponder does. */
        public String textKey(int n) {
            return KEY_PREFIX + id + ".text_" + n;
        }

        public Script script() {
            return new Script(this);
        }
    }

    /**
     * Hands a scene its lines in order, one per text box. It never throws, so a scene with one
     * text box too many still loads (a test keeps the counts equal).
     */
    public static final class Script {
        private final Scene scene;
        private int next;

        private Script(Scene scene) {
            this.scene = scene;
        }

        public String next() {
            return next < scene.lines().size() ? scene.lines().get(next++).english() : "";
        }
    }

    private static final String KEY_PREFIX = "create_nuclearindustry.ponder.";

    public static final String TAG_ID = "nuclear_power_plant";
    public static final String TAG_KEY = KEY_PREFIX + "tag." + TAG_ID;
    public static final Line TAG_TITLE = line("Nuclear Power Plant", "Central nuclear");
    public static final Line TAG_DESCRIPTION = line(
            "Everything a nuclear power plant is made of: the reactor and its loops, the turbine hall, the control room, the containment and the spent fuel",
            "Todo lo que forma una central nuclear: el reactor y sus circuitos, la sala de turbinas, la sala de control, la contención y el combustible gastado");

    /** Items that open every scene of the series, in order. */
    public static final List<String> HUBS = List.of("reactor_controller", "steam_generator_controller");

    public static final Scene OVERVIEW = new Scene("plant_overview", "plant_overview",
            line("The Whole Plant", "La central completa"),
            List.of(
                    line("A nuclear power plant is a chain of machines joined by pipes and shafts. Here it is whole: each step zooms in on one part and how it joins the next.",
                            "Una central nuclear es una cadena de máquinas unidas por tuberías y ejes. Aquí está completa: en cada paso la cámara se acerca a una parte y a cómo se une con la siguiente."),
                    line("The reactor and its steam generator stand inside the containment, a sealed room of Containment Concrete with a Containment Door to walk in. From here on it is shown cut away.",
                            "El reactor y su generador de vapor están dentro de la contención, una sala estanca de Hormigón de contención con una Puerta de contención para entrar. A partir de aquí se muestra cortada."),
                    line("In the primary loop a pump carries the hot Primary Coolant from the reactor's OUTPUT port to the steam generator's primary in...",
                            "En el circuito primario, una bomba lleva el Refrigerante primario caliente del puerto de SALIDA del reactor a la entrada del primario del generador de vapor..."),
                    line("...and a second pump brings it back from primary out to the reactor's INPUT port. The Pressurizer on the lid keeps the loop under pressure.",
                            "...y una segunda bomba lo devuelve desde la salida del primario al puerto de ENTRADA del reactor. El Presionador de la tapa mantiene el circuito a presión."),
                    line("Pipes may only leave the containment through Containment Penetrations: one for the steam line, one for the feedwater.",
                            "Las tuberías solo pueden salir de la contención por Penetraciones de contención: una para la línea de vapor y otra para el agua de alimentación."),
                    line("The steam line runs from the Steam Out port on top of the steam generator, through its penetration and a pump, into the turbine's INPUT port.",
                            "La línea de vapor va desde el puerto de salida de vapor, encima del generador de vapor, por su penetración y una bomba, hasta el puerto de ENTRADA de la turbina."),
                    line("The Turbine Output turns the shaft at the turbine's end: take the rotation from there to your machines.",
                            "La Salida de turbina hace girar el eje del extremo de la turbina: lleva desde ahí la rotación a tus máquinas."),
                    line("The turbine's Exhaust Port sends the spent steam, through another pump, to the Cooling Tower's INPUT port.",
                            "El Puerto de escape de la turbina envía el vapor gastado, con otra bomba, al puerto de ENTRADA de la torre de refrigeración."),
                    line("The tower condenses it into water. From its OUTPUT port a pump sends the water through the other penetration to the steam generator's feedwater port: the loop is closed.",
                            "La torre lo condensa en agua. Desde su puerto de SALIDA, una bomba lleva el agua por la otra penetración hasta el puerto de agua de alimentación del generador de vapor: el circuito se cierra."),
                    line("The control room runs it all from a distance: link its desks, annunciator and chart recorder to the reactor with the Control Room Linker.",
                            "La sala de control lo maneja todo a distancia: enlaza sus pupitres, el anunciador y el registrador gráfico al reactor con el Enlazador de sala de control."),
                    line("Spent fuel taken out of the reactor goes into the racks at the bottom of the pool, under water, with a Pool Cooler to carry off its heat.",
                            "El combustible gastado que sale del reactor va a los bastidores del fondo de la piscina, bajo el agua, con un Refrigerador de piscina que se lleva su calor."),
                    line("Once it has cooled, move it to Dry Storage Casks or send it to reprocessing.",
                            "Cuando se ha enfriado, pásalo a Contenedores de almacenamiento en seco o mándalo a reprocesar."),
                    line("That is the whole plant, running. The next scenes explain each part in detail.",
                            "Esta es la central completa, en marcha. Las siguientes escenas explican cada parte en detalle.")),
            List.of("turbine_output", "cooling_tower_controller", "containment_concrete", "containment_penetration",
                    "spent_fuel_rack", "reactor_control_panel"));

    public static final Scene HEAT_LOOP = new Scene("plant_heat_loop", "plant_heat",
            line("The Pressurized Primary Loop", "El circuito primario a presión"),
            List.of(
                    line("A pressurized water reactor keeps its coolant liquid under pressure. Fill a fission reactor with Primary Coolant: water and boron from a mixer.",
                            "Un reactor de agua a presión mantiene líquido su refrigerante gracias a la presión. Llena un reactor de fisión con Refrigerante primario: agua y boro de una mezcladora."),
                    line("Stand a Pressurizer on the lid, away from the controller and the rod channels: 2 to 4 Pressurizer or Heater Port blocks under a Relief Valve.",
                            "Coloca un Presionador sobre la tapa, lejos del controlador y de los canales de las barras: de 2 a 4 bloques de Presionador o Puerto calefactor bajo una Válvula de alivio."),
                    line("Its steam bubble holds the loop at 155 bar. Heater Ports raise the pressure, on their own or by redstone after a wrench click; spray from the cold leg lowers it.",
                            "Su burbuja de vapor mantiene el circuito a 155 bar. Los Puertos calefactores suben la presión, solos o con redstone tras un clic con la llave inglesa; el rociado desde la rama fría la baja."),
                    line("The Relief Valve lifts at 170 bar and reseats at 160. It can stick open: a redstone signal on it shuts its block valve.",
                            "La Válvula de alivio se abre a 170 bar y vuelve a cerrar a 160. Puede quedarse abierta: una señal de redstone sobre ella cierra su válvula de aislamiento."),
                    line("A port set to OUTPUT is the hot leg, one set to INPUT the cold leg. Without a pressurizer they refuse primary coolant.",
                            "Un puerto en SALIDA es la rama caliente; uno en ENTRADA, la rama fría. Sin presionador rechazan el refrigerante primario."),
                    line("The Steam Generator is a 3x3 column, 4 to 8 high, of casing around a core of Tube Bundles. More tubes carry more heat.",
                            "El Generador de vapor es una columna de 3x3 y de 4 a 8 de alto: carcasa alrededor de un núcleo de Haces de tubos. Más tubos transmiten más calor."),
                    line("Its controller goes in a side wall, facing out, with a Tube Bundle right behind it.",
                            "Su controlador va en una pared lateral, mirando hacia fuera, con un Haz de tubos justo detrás."),
                    line("Each Steam Generator Port carries one thing, picked with the wrench: primary in, primary out, feedwater in or steam out.",
                            "Cada Puerto del generador de vapor lleva una sola cosa, que se elige con la llave inglesa: entrada del primario, salida del primario, entrada de agua de alimentación o salida de vapor."),
                    line("Create pumps drive the loop. The hot leg carries the hot primary coolant from the reactor to the primary inlet...",
                            "Las bombas de Create mueven el circuito. La rama caliente lleva el refrigerante primario caliente del reactor a la entrada del primario..."),
                    line("...and the cold leg brings it back, cooled, to the reactor. The flow is whatever the pumps move, and a pressurized core needs plenty.",
                            "...y la rama fría lo devuelve enfriado al reactor. El caudal es el que muevan las bombas, y un núcleo a presión necesita mucho."),
                    line("In the tubes its heat boils the feedwater on the other side into steam. The two never mix.",
                            "En los tubos, su calor hace hervir el agua de alimentación del otro lado y la convierte en vapor. Nunca se mezclan."),
                    line("If a pump stops, the core heats up and boils, the pressure climbs and the relief valve lifts.",
                            "Si una bomba se para, el núcleo se calienta y hierve, la presión sube y salta la válvula de alivio."),
                    line("A broken pipe drains the loop until the core uncovers and overheats. Primary coolant is radioactive: a leak irradiates.",
                            "Una tubería rota vacía el circuito hasta que el núcleo queda al descubierto y se sobrecalienta. El refrigerante primario es radiactivo: una fuga irradia.")),
            List.of("reactor_fluid_port", "pressurizer", "pressurizer_heater_port", "relief_valve",
                    "steam_generator_casing", "steam_generator_tube_bundle", "steam_generator_port"));

    public static final Scene GENERATION = new Scene("plant_generation", "plant_generation",
            line("Steam, Turbine and Cooling Tower", "Vapor, turbina y torre de refrigeración"),
            List.of(
                    line("The secondary loop turns the reactor's heat into rotation. It starts at the steam generator, heated by the primary loop.",
                            "El circuito secundario convierte el calor del reactor en rotación. Empieza en el generador de vapor, calentado por el circuito primario."),
                    line("A pump draws the steam from the Steam Out port into the turbine's INPUT port...",
                            "Una bomba saca el vapor del puerto de salida de vapor y lo lleva al puerto de ENTRADA de la turbina..."),
                    line("...where every rotor stage expands it and the Turbine Output turns the shaft.",
                            "...donde cada etapa de rotor lo expande y la Salida de turbina hace girar el eje."),
                    line("With an Exhaust Port the turbine stops condensing inside: it lets the spent steam out and makes twice the power from the same steam.",
                            "Con un Puerto de escape la turbina deja de condensar dentro: suelta el vapor gastado y genera el doble de potencia con el mismo vapor."),
                    line("Only a Cooling Tower turns that exhaust back into water. Pump it into a tower port set to INPUT.",
                            "Solo una Torre de refrigeración vuelve a convertir ese escape en agua. Bombéalo a un puerto de la torre en ENTRADA."),
                    line("This tower, radius 4 and 12 high, condenses 48 mB/t. If the exhaust backs up the turbine stops, so size the tower for its turbines.",
                            "Esta torre, de radio 4 y 12 de alto, condensa 48 mB/t. Si el escape se acumula la turbina se para, así que dimensiona la torre para sus turbinas."),
                    line("The water leaves through a port set to OUTPUT, and a pump sends it back to the steam generator's feedwater port.",
                            "El agua sale por un puerto en SALIDA y una bomba la devuelve al puerto de agua de alimentación del generador de vapor."),
                    line("Keep the feedwater coming: without it the tubes dry out and stop taking heat, and the reactor loses its heat sink.",
                            "Que no falte agua de alimentación: sin ella los tubos se secan y dejan de absorber calor, y el reactor se queda sin sumidero de calor."),
                    line("Steam left in the generator raises its pressure until it stops taking heat, so the turbine must keep drawing it.",
                            "El vapor que se queda en el generador sube su presión hasta que deja de absorber calor, así que la turbina debe seguir tirando de él."),
                    line("Two loops that never mix: the radioactive primary stays around the reactor, while clean water and steam drive the turbine.",
                            "Dos circuitos que nunca se mezclan: el primario radiactivo se queda junto al reactor, y el agua y el vapor limpios mueven la turbina.")),
            List.of("steam_generator_port", "turbine_output", "turbine_exhaust_port", "turbine_fluid_port",
                    "cooling_tower_controller", "cooling_tower_fluid_port"));

    public static final Scene CONTROL_SAFETY = new Scene("plant_control_safety", "plant_control",
            line("Control Room and Containment", "Sala de control y contención"),
            List.of(
                    line("The control room runs the plant from afar. Fit control modules into Control Desks and link them to the reactor with the Control Room Linker.",
                            "La sala de control maneja la central a distancia. Coloca módulos de control en los Pupitres de control y enlázalos al reactor con el Enlazador de sala de control."),
                    line("A dial set to Rod target chooses how deep the control rods should sit.",
                            "Una rueda en Objetivo de las barras elige a qué profundidad deben quedar las barras de control."),
                    line("A Controller Module moves that target for you: on Automatic power or Automatic temperature it holds the power or the core temperature at its setpoint. On Automatic pressure it runs the pressurizer instead.",
                            "Un Módulo controlador mueve ese objetivo por ti: en Potencia automática o Temperatura automática mantiene la potencia o la temperatura del núcleo en su consigna. En Presión automática maneja el presionador."),
                    line("Rods only move with pistons. Redstone Ports set to Withdraw rods or Insert rods light up until the rods reach the target...",
                            "Las barras solo se mueven con pistones. Los Puertos de redstone en Extraer barras o Insertar barras se encienden hasta que las barras llegan al objetivo..."),
                    line("...so wire them to a Clutch and a Gearshift on the piston's shaft: the rods move while a port is lit, the way it asks.",
                            "...así que conéctalos a un Embrague y una Caja de cambios en el eje del pistón: las barras se mueven mientras un puerto está encendido, en el sentido que pide."),
                    line("A SCRAM button calls for full insertion and beats every other setting until it is reset.",
                            "Un pulsador de SCRAM pide la inserción completa y se impone a cualquier otro ajuste hasta que se rearma."),
                    line("Link each window of an Annunciator Panel to an alarm. It flashes fast with a horn until acknowledged, stays lit while the alarm lasts and flashes slowly once it clears.",
                            "Enlaza cada ventana de un Panel anunciador a una alarma. Parpadea rápido con bocina hasta que se reconoce, sigue encendida mientras dura y parpadea despacio cuando se despeja."),
                    line("A Chart Recorder draws up to three readings on paper. Keep it fed with paper, and sneak-click it to tear off the chart.",
                            "Un Registrador gráfico dibuja hasta tres lecturas en papel. Mantenlo con papel y haz clic agachado para arrancar la gráfica."),
                    line("Close the reactor inside a containment: a room of full blocks with no gaps. Containment Concrete holds 6 bar, Steel Liner 8.",
                            "Encierra el reactor en una contención: una sala de bloques completos sin huecos. El Hormigón de contención aguanta 6 bar y el Revestimiento de acero, 8."),
                    line("Containment Penetrations carry the pipes through the wall without unsealing it.",
                            "Las Penetraciones de contención llevan las tuberías a través de la pared sin romper la estanqueidad."),
                    line("The weakest wall sets the limit: this window holds only 1.05 bar. If the pressure beats it, the wall bursts there and everything airborne escapes.",
                            "La pared más débil marca el límite: esta ventana solo aguanta 1,05 bar. Si la presión la supera, la pared revienta ahí y todo lo que hay en el aire escapa."),
                    line("A Containment Monitor inside measures the sealed air: pressure, temperature, steam, hydrogen and airborne activity.",
                            "Un Monitor de contención en el interior mide el aire estanco: presión, temperatura, vapor, hidrógeno y actividad en el aire."),
                    line("Overheated cladding gives off hydrogen. Above 4%% it may burn and above 8%% it always does: Recombiners remove it without a flame.",
                            "Las vainas sobrecalentadas desprenden hidrógeno. Por encima del 4%% puede arder y por encima del 8%% siempre arde: los Recombinadores lo eliminan sin llama."),
                    line("A Filtered Vent opened by redstone bleeds off the overpressure and lets out only 1%% of the airborne activity.",
                            "Un Venteo filtrado abierto con redstone alivia la sobrepresión y solo deja salir el 1%% de la actividad en el aire."),
                    line("In a meltdown the fuel turns into Corium, which melts its way down...",
                            "En una fusión del núcleo el combustible se convierte en Corium, que se abre paso hacia abajo fundiéndolo todo..."),
                    line("...until a Core Catcher floor stops it: it spreads out and freezes into Solidified Corium, still radioactive but contained.",
                            "...hasta que un suelo de Recogedor del núcleo lo detiene: se extiende y se solidifica en Corium solidificado, aún radiactivo pero contenido.")),
            List.of("reactor_control_panel", "controller_module", "dial_module", "push_button_module", "reactor_redstone_port",
                    "annunciator_panel", "chart_recorder", "control_room_linker", "containment_concrete", "steel_liner",
                    "containment_monitor", "containment_penetration", "recombiner", "filtered_vent", "core_catcher"));

    public static final Scene WASTE = new Scene("plant_waste", "plant_waste",
            line("Spent Fuel and Waste", "Combustible gastado y residuos"),
            List.of(
                    line("Fuel leaves the reactor as Hot spent fuel, dated with the day it came out. It goes under water first.",
                            "El combustible sale del reactor como combustible gastado Caliente, fechado con el día en que salió. Primero va bajo el agua."),
                    line("A Spent Fuel Rack holds nine assemblies on the floor of a pool. Each block of water above cuts its radiation: two stop over 97%%.",
                            "Un Bastidor de combustible gastado guarda nueve elementos en el fondo de una piscina. Cada bloque de agua encima reduce su radiación: dos frenan más del 97%%."),
                    line("Spent fuel cools with time: Warm after about 2 days, Cooled after a week and Aged after 3 weeks. Fresh fuel glows blue.",
                            "El combustible gastado se enfría con el tiempo: Templado a los 2 días, Enfriado a la semana y Envejecido a las 3 semanas. El recién descargado brilla en azul."),
                    line("Its decay heat warms the pool, and a pool too small for its racks boils dry. A Pool Cooler turned from above carries the heat away.",
                            "Su calor residual calienta la piscina, y una piscina demasiado pequeña para sus bastidores se queda sin agua. Un Refrigerador de piscina accionado desde arriba se lleva el calor."),
                    line("Once Warm or cooler, fuel can move to a Dry Storage Cask. It lets out only 2%% of the radiation and never leaks.",
                            "Cuando ya está Templado o más frío, el combustible puede pasar a un Contenedor de almacenamiento en seco. Solo deja salir el 2%% de la radiación y nunca tiene fugas."),
                    line("Or reprocess it (PUREX): crush it, dissolve it in nitric acid and centrifuge the solution for uranium and plutonium. Cooled fuel yields more and leaves less waste.",
                            "O reprocésalo (PUREX): tritúralo, disuélvelo en ácido nítrico y centrifuga la disolución para obtener uranio y plutonio. El combustible enfriado rinde más y deja menos residuos."),
                    line("Waste comes in three classes. Low-Level Waste, from scrubbers and contaminated water, is compacted with an iron sheet into a Compacted Waste Drum.",
                            "Los residuos son de tres clases. Los de baja actividad, de los absorbedores y del agua contaminada, se compactan con una lámina de hierro en un Bidón de residuos compactados."),
                    line("Intermediate-Level Waste is cemented in a mixer, with sand, gravel, an iron sheet and water, into a Cemented Waste Drum.",
                            "Los de media actividad se cementan en una mezcladora, con arena, grava, una lámina de hierro y agua, en un Bidón de residuos cementados."),
                    line("High-Level Waste from PUREX melts with sand and borax into a Vitrified Waste Canister, in a Vitrification Melter on a lit Blaze Burner.",
                            "Los de alta actividad del PUREX se funden con arena y bórax en un Contenedor de residuos vitrificados, en un Fundidor de vitrificación sobre un Quemador de Blaze encendido."),
                    line("Packages end up in a geological repository, where each one is scored. Depth below the surface counts most: full marks 64 blocks down.",
                            "Los bultos acaban en un repositorio geológico, donde cada uno recibe una puntuación. Lo que más cuenta es la profundidad bajo la superficie: la nota máxima, a 64 bloques."),
                    line("Then the host rock around it, such as deepslate, tuff or basalt, and the waste form: vitrified is best, a raw barrel worst.",
                            "Después, la roca que lo rodea, como pizarra profunda, toba o basalto, y la forma del residuo: vitrificado es lo mejor y un barril sin tratar, lo peor."),
                    line("Backfill every face with Bentonite, made from clay and borax, or with other packages.",
                            "Rellena todas sus caras con Bentonita, hecha de arcilla y bórax, o con otros bultos."),
                    line("Water within 4 blocks spoils the site: water is what carries waste out.",
                            "El agua a menos de 4 bloques arruina el emplazamiento: es el agua la que se lleva los residuos."),
                    line("From a score of 80%% a package is stored: it neither leaks nor irradiates. A Repository Inspector shows the weakest package within 8 blocks.",
                            "Desde una puntuación del 80%% el bulto queda almacenado: ni se filtra ni irradia. Un Inspector de repositorio muestra el bulto más débil a 8 bloques.")),
            List.of("spent_fuel_rack", "pool_cooler", "dry_cask", "vitrification_melter", "compacted_waste_drum",
                    "cemented_waste_drum", "vitrified_waste_canister", "bentonite", "repository_inspector", "radioactive_waste_barrel"));

    /** The series, in the order Ponder chains it. */
    public static final List<Scene> SCENES = List.of(OVERVIEW, HEAT_LOOP, GENERATION, CONTROL_SAFETY, WASTE);

    /** Plant parts with no scene of the series of their own that still belong under the tag. */
    private static final List<String> OTHER_PARTS = List.of("containment_door", "reactor_casing", "reactor_fuel_port", "control_rod", "heat_exchanger",
            "turbine_casing", "turbine_rotor", "reinforced_turbine_glass", "cooling_tower_casing", "cooling_tower_fan",
            "control_monitor", "nuclear_alarm", "lever_module", "lamp_module", "gauge_module", "key_switch_module");

    private PlantPonderText() {}

    /** Items whose Ponder page shows {@code scene}: the hubs first, then its own parts. */
    public static List<String> componentsOf(Scene scene) {
        Set<String> components = new LinkedHashSet<>(HUBS);
        components.addAll(scene.components());
        return List.copyOf(components);
    }

    /** Every item listed under the ponder tag. */
    public static List<String> tagComponents() {
        Set<String> components = new LinkedHashSet<>(HUBS);
        SCENES.forEach(scene -> components.addAll(scene.components()));
        components.addAll(OTHER_PARTS);
        return new ArrayList<>(components);
    }

    private static Line line(String english, String spanish) {
        return new Line(english, spanish);
    }
}
