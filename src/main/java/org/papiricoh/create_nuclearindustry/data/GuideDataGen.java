package org.papiricoh.create_nuclearindustry.data;

import org.papiricoh.create_nuclearindustry.infrastructure.ponder.GuidePonderText;
import org.papiricoh.create_nuclearindustry.infrastructure.ponder.PlantPonderText;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

/**
 * Lang for the guide: the Ponder scenes of civil defense, weapons, the fuel cycle and the fan
 * treatments, and the JEI pages that were missing. The advancement tabs write their own text from
 * {@link NuclearAdvancementProvider#ENTRIES}.
 */
final class GuideDataGen extends AreaDataGen {
    static final GuideDataGen INSTANCE = new GuideDataGen();

    private GuideDataGen() {}

    @Override
    void lang() {
        ponder();
        jei();
    }

    /** The scenes' lines come from {@link GuidePonderText}, the lists the scenes read in order. */
    private static void ponder() {
        LangEntries.add(GuidePonderText.CONTROL_ROOM_TAG_KEY, GuidePonderText.CONTROL_ROOM_TAG_TITLE.english(),
                GuidePonderText.CONTROL_ROOM_TAG_TITLE.spanish());
        LangEntries.add(GuidePonderText.CONTROL_ROOM_TAG_KEY + ".description", GuidePonderText.CONTROL_ROOM_TAG_DESCRIPTION.english(),
                GuidePonderText.CONTROL_ROOM_TAG_DESCRIPTION.spanish());
        for (PlantPonderText.Scene scene : GuidePonderText.SCENES) {
            LangEntries.add(scene.headerKey(), scene.title().english(), scene.title().spanish());
            for (int i = 0; i < scene.lines().size(); i++) {
                PlantPonderText.Line line = scene.lines().get(i);
                LangEntries.add(scene.textKey(i + 1), line.english(), line.spanish());
            }
        }
    }

    private static void page(String id, String english, String spanish) {
        LangEntries.add(PlantJeiInfo.PREFIX + id, english, spanish);
    }

    /** One text for every id in {@link org.papiricoh.create_nuclearindustry.integration.jei.GuideJeiInfo#IDS}. */
    private static void jei() {
        // Weapons
        page("nuclear_bomb",
                "A nuclear weapon, assembled on a Create line: deploy an Implosion Assembly, a Neutron Initiator and a Firing Set into a Bomb Casing and press it shut. It comes out empty: its three slots take weapons-grade cores, two to a slot. Each core is worth the metal cast into it, eight uranium at 90% or four plutonium, and six make a full warhead that wrecks what stands up to 250 blocks from ground zero and digs a crater about a third as wide, with a raised rim and a floor of glassy trinitite. Loose metal no longer goes in: cast it into a core first. Power it with redstone and it goes off after a 10 second countdown; taking the signal away stops the countdown. Its flash burns whoever sees the fireball and scorches the land further out still, prompt radiation doses whoever is near, a blast wave hurts and throws whoever is in the open, and fallout drifts downwind. Blast doors and bedrock survive it. Sirens within reach sound the attack warning during the countdown.",
                "Un arma nuclear, que se monta en una línea de Create: despliega un Conjunto de implosión, un Iniciador de neutrones y un Conjunto de disparo en una Carcasa de bomba y préndela con la prensa. Sale vacía: sus tres huecos admiten núcleos de grado militar, dos por hueco. Cada núcleo vale el metal que lleva, ocho de uranio al 90% o cuatro de plutonio, y seis forman una cabeza completa que arrasa lo que se alza hasta 250 bloques desde la zona cero y abre un cráter de un tercio de ese ancho, con el borde levantado y el fondo de trinitita vítrea. El metal suelto ya no entra: fúndelo antes en un núcleo. Dale una señal de redstone y estalla tras una cuenta atrás de 10 segundos; si se quita la señal, la cuenta se detiene. Su destello quema a quien ve la bola de fuego y abrasa el terreno aún más lejos, la radiación inmediata irradia a quien esté cerca, una onda expansiva hiere y lanza a quien esté al descubierto, y la lluvia radiactiva se desplaza con el viento. Las puertas blindadas y la roca madre resisten. Las sirenas cercanas dan el aviso de ataque durante la cuenta atrás.");
        page("thermonuclear_bomb",
                "A hydrogen bomb: a fission primary whose X-rays crush and light a fusion secondary. Assemble it like a Nuclear Bomb in a Thermonuclear Casing, adding a Tritium Cell to boost the primary, so it needs a working tokamak. Load it with one weapons-grade core as its primary (plutonium compresses better than uranium), up to sixteen Lithium-6 Deuteride for its secondary and up to four Depleted Uranium Blocks for its tamper. Without a core it cannot count down; without deuteride only the primary goes off. Each deuteride multiplies the yield, the tamper adds to it and spreads far more fallout, leaving a hot spot that fades over days. A full one wrecks what stands up to 750 blocks away, three times a full atomic bomb, with a crater kept to about 110 blocks. It counts down for 15 seconds once powered. Its explosion is unmistakable: a bigger, whiter fireball that burns for longer and a cloud of near-black soot. Servers can lower its reach in the config.",
                "Una bomba de hidrógeno: un primario de fisión cuyos rayos X comprimen y encienden un secundario de fusión. Se monta como una Bomba nuclear en una Carcasa termonuclear, con una Celda de tritio más para reforzar el primario, así que necesita un tokamak en marcha. Cárgala con un núcleo de grado militar como primario (el plutonio comprime mejor que el uranio), hasta dieciséis de Deuteruro de litio-6 como secundario y hasta cuatro Bloques de uranio empobrecido como camisa. Sin núcleo no puede contar; sin deuteruro solo estalla el primario. Cada deuteruro multiplica la potencia, y la camisa la aumenta y esparce mucha más lluvia radiactiva, dejando un foco que se apaga en días. Una cargada al máximo arrasa lo que se alza hasta 750 bloques, tres veces una bomba atómica completa, con un cráter limitado a unos 110 bloques. Cuenta 15 segundos al recibir señal. Su explosión es inconfundible: una bola de fuego mayor y más blanca que arde más tiempo y una nube de hollín casi negro. Los servidores pueden limitar su alcance en la configuración.");
        page("warhead_parts",
                "Every warhead is built from parts made on a Create line. An Explosive Lens is TNT and gunpowder deployed onto an iron sheet and pressed, twice over; eight lenses deployed round a Beryllium Reflector make an Implosion Assembly. A Firing Set is a precision mechanism fitted with electron tubes, copper sheets and redstone, twice over. A Neutron Initiator is beryllium, plutonium dioxide and a golden sheet mixed hot. Bomb Casings and Thermonuclear Casings are riveted on a Mechanical Crafter, the second lined with depleted uranium. Lithium-6 Deuteride is lithium-6 mixed hot with deuterium. Guidance modules, warhead casings, silo controllers and dirty bombs take a firing set too.",
                "Toda cabeza se monta con piezas hechas en una línea de Create. Una Lente explosiva es TNT y pólvora desplegadas sobre una lámina de hierro y prensadas, dos veces; ocho lentes desplegadas alrededor de un Reflector de berilio forman un Conjunto de implosión. Un Conjunto de disparo es un mecanismo de precisión al que se añaden válvulas electrónicas, láminas de cobre y redstone, dos veces. Un Iniciador de neutrones es berilio, dióxido de plutonio y una lámina de oro mezclados en caliente. Las Carcasas de bomba y las Carcasas termonucleares se remachan en un Ensamblador mecánico, la segunda forrada de uranio empobrecido. El Deuteruro de litio-6 es litio-6 mezclado en caliente con deuterio. Los módulos de guiado, las ojivas, los controladores de silo y las bombas sucias también llevan un conjunto de disparo.");
        page("missile_launch",
                "Build a 5×5 silo with a full floor, perimeter walls and a 3×3 interior. Stack 1–3 rocket stages at its centre: each engine followed by 1–4 tanks, then guidance and one warhead. Total height: 4–17 blocks. Load every tank with a propellant cartridge and configure the warhead; inert flights allow safe testing. Set coordinates or use the Target Designator. Complete the walls up to the tip and clear the vertical exit. Supply at least 32 RPM (8 SU/RPM by default), arm from the controller screen and apply a new redstone rising edge. The ten-second countdown cancels if rotation, construction or exit clearance changes. Reloading disarms the silo. Launch consumes the rocket and its contents, leaving the silo. Range and server restrictions still apply. Missile alerts require a powered radar connected to redstone: there are no automatic siren or chat broadcasts. Static installations only; ships and contraptions are unsupported. Convert legacy launch pads and missiles into controllers and engines in a crafting grid.",
                "Construye un silo de 5×5 con suelo completo, paredes perimetrales e interior de 3×3. Apila 1–3 etapas en el centro: cada motor seguido de 1–4 depósitos, después guiado y una ojiva. Altura total: 4–17 bloques. Carga cada depósito con un cartucho de propelente y configura la ojiva; el vuelo inerte permite probar sin explosión. Introduce coordenadas o usa el Designador. Completa las paredes hasta la punta y despeja la salida vertical. Suministra al menos 32 RPM (8 SU/RPM por defecto), arma en la pantalla y aplica un nuevo flanco de redstone. La cuenta de diez segundos se cancela si cambia la rotación, el montaje o la salida. Recargar desarma el silo. Lanzar consume cohete y contenido, y conserva el silo. Se mantienen el alcance y las restricciones del servidor. Las alertas requieren un radar operativo conectado por redstone: no hay avisos automáticos a sirenas ni chat. Solo instalaciones estáticas; naves y contraptions no son compatibles. Convierte plataformas y misiles antiguos en controladores y motores en la mesa de trabajo.");
        page("nuclear_cannon_shell",
                "A big cannon shell from Create Big Cannons with a nuclear warhead: craft a high explosive shell with a Nuclear Bomb and one to six weapons-grade cores. Six cores are a fully loaded Nuclear Bomb; fewer, a smaller blast. Load and fire it like any other shell; its fuze sets it off, and set off high enough above the ground it is an airburst.",
                "Un proyectil de cañón grande de Create Big Cannons con una cabeza nuclear: fabrícalo con un proyectil explosivo, una Bomba nuclear y de uno a seis núcleos de grado militar. Seis núcleos equivalen a una Bomba nuclear cargada al máximo; menos, a una explosión menor. Se carga y se dispara como cualquier otro proyectil; su espoleta lo hace estallar, y si estalla bastante por encima del suelo es una explosión aérea.");
        page("thermonuclear_cannon_shell",
                "A big cannon shell from Create Big Cannons with a thermonuclear warhead: craft a high explosive shell with a Thermonuclear Bomb, one weapons-grade core, one to four Lithium-6 Deuteride and up to two Depleted Uranium Blocks. It has room for a quarter of a bomb's secondary, so its blast reaches about 470 blocks at most, still far beyond any atomic warhead. Load and fire it like any other shell.",
                "Un proyectil de cañón grande de Create Big Cannons con una cabeza termonuclear: fabrícalo con un proyectil explosivo, una Bomba termonuclear, un núcleo de grado militar, de uno a cuatro de Deuteruro de litio-6 y hasta dos Bloques de uranio empobrecido. Solo cabe una cuarta parte del secundario de una bomba, así que su explosión llega como mucho a unos 470 bloques, aun así muy por encima de cualquier cabeza atómica. Se carga y se dispara como cualquier otro proyectil.");

        // Fission reactor shell parts
        page("reactor_fuel_port",
                "Part of a fission reactor's shell. On INPUT it takes fuel assemblies, by hand, hopper, funnel or chute, and loads them into the free Fuel Channels; on OUTPUT it hands out the spent fuel the channels eject. Wrench it to switch the mode, sneak-wrench to turn its face.",
                "Parte de la carcasa de un reactor de fisión. En ENTRADA admite elementos combustibles, a mano, con tolva, embudo o rampa, y los carga en los Canales de combustible libres; en SALIDA entrega el combustible gastado que expulsan los canales. Usa la llave para cambiar de modo, y agachado para girar su cara.");
        page("reactor_fluid_port",
                "Part of a fission reactor's shell, for its coolant. On INPUT it takes water, or Primary Coolant in a pressurized plant; on OUTPUT it gives the steam, or the hot primary coolant, the core heats. A reactor needs at least one of each. Wrench it to switch the mode, sneak-wrench to turn its face.",
                "Parte de la carcasa de un reactor de fisión, para su refrigerante. En ENTRADA admite agua, o Refrigerante primario en una central a presión; en SALIDA da el vapor, o el refrigerante primario caliente, que calienta el núcleo. Un reactor necesita al menos uno de cada. Usa la llave para cambiar de modo, y agachado para girar su cara.");
        page("control_rod",
                "Stacked in columns inside a reactor, Control Rods soak up neutrons: the deeper they sit in the core, the more they hold back the chain reaction, and fully inserted they shut it down. They only move with a Mechanical Piston above an open column of the lid, glued to each other and to the piston head. Wire the reactor's SCRAM signal to the piston to drive them in when it trips.",
                "Apiladas en columnas dentro de un reactor, las Barras de control absorben neutrones: cuanto más dentro del núcleo, más frenan la reacción en cadena, y metidas del todo la detienen. Solo se mueven con un Pistón mecánico encima de una columna abierta de la tapa, pegadas entre sí y a la cabeza del pistón. Lleva la señal de SCRAM del reactor al pistón para que las meta cuando salte.");
        page("reactor_temperature_sensor",
                "Part of a fission reactor's shell. It gives a redstone signal on the face it points at, rising with the core temperature: 0 at 20 °C up to 15 at 3500 °C. Wrench it to turn its face. Goggles show the exact temperature, and a Display Link reads it too.",
                "Parte de la carcasa de un reactor de fisión. Da una señal de redstone por la cara hacia la que apunta, que sube con la temperatura del núcleo: 0 a 20 °C y hasta 15 a 3500 °C. Usa la llave para girar su cara. Las gafas muestran la temperatura exacta, y un Enlace de pantalla también la lee.");
        page("reactor_viewport",
                "A lead glass window that takes the place of a Reactor Casing block in a fission reactor's walls. Through it you watch the fuel channels: how much fuel they hold, how brightly they glow with the power, and whether coolant flows past them. Viewports side by side join into one pane.",
                "Una ventana de vidrio plomado que ocupa el lugar de un bloque de Carcasa de reactor en las paredes de un reactor de fisión. A través de ella ves los canales de combustible: cuánto combustible tienen, cuánto brillan con la potencia y si pasa refrigerante. Varias mirillas juntas forman un solo cristal.");

        // Tokamak and fusion turbine
        page("tokamak_structure",
                "The blocks a tokamak's shell is made of. The torus has a 5x5 cross-section around an empty 3x3 plasma chamber, 4 to 16 blocks from the Central Solenoid, a column five blocks tall at its axis. Toroidal Field Coils line its walls, Breeding Blankets in its floor and roof turn fusion neutrons into tritium, and the rest is plain shell: Tokamak Vacuum Vessel or Cryostat Casing, which count the same. See the tokamak's Ponder scene.",
                "Los bloques de los que está hecha la carcasa de un tokamak. El toro tiene una sección de 5x5 alrededor de una cámara de plasma vacía de 3x3, a entre 4 y 16 bloques del Solenoide central, una columna de cinco bloques de alto en su eje. Las Bobinas de campo toroidal recubren sus paredes, los Mantos regeneradores del suelo y el techo convierten los neutrones de fusión en tritio, y el resto es carcasa sin más: Cámara de vacío del tokamak o Carcasa criostato, que cuentan igual. Consulta la escena de Ponder del tokamak.");
        page("tokamak_magnet_drive",
                "Part of a tokamak's shell that turns rotation into the field that confines the plasma. A tokamak needs one for every 4 blocks of radius, turning at 64 RPM or more. Short of drives or speed, the field is weaker and holds the plasma less well.",
                "Parte de la carcasa de un tokamak que convierte la rotación en el campo que confina el plasma. Un tokamak necesita una por cada 4 bloques de radio, girando a 64 RPM o más. Si faltan tomas o velocidad, el campo es más débil y sujeta peor el plasma.");
        page("tokamak_fluid_port",
                "Part of a tokamak's shell. On INPUT it takes water as coolant; on OUTPUT it gives the plasma steam a burning plasma boils it into, for a Plasma Turbine. Keep the coolant flowing: a tokamak without it overheats and disrupts.",
                "Parte de la carcasa de un tokamak. En ENTRADA admite agua como refrigerante; en SALIDA da el vapor de plasma en que la convierte un plasma en combustión, para una Turbina de plasma. Mantén el refrigerante: un tokamak sin él se recalienta y sufre una disrupción.");
        page("tokamak_fuel_injector",
                "Part of a tokamak's shell that fuels the plasma with deuterium and tritium, by pipe or by hand with gas cells. On deuterium alone the plasma burns slowly and breeds its first tritium in the blankets. Switched to OUTPUT it hands out the bred tritium above the reserve the tokamak keeps.",
                "Parte de la carcasa de un tokamak que alimenta el plasma con deuterio y tritio, por tubería o a mano con células de gas. Solo con deuterio el plasma arde despacio y cría su primer tritio en los mantos. En modo SALIDA entrega el tritio criado por encima de la reserva que guarda el tokamak.");
        page("fusion_turbine",
                "The fusion turbine is built like the steam turbine, from these blocks, around a Plasma Turbine at the centre of one end: a box 3-7 blocks wide and high and 3-16 long, with a column of Fusion Turbine Rotors on its axis and a pair of Fusion Turbine Fluid Ports, one on input and one on output, for every 80 mB/t of plasma steam. Each rotor expands 20 mB/t.",
                "La turbina de fusión se construye como la de vapor, con estos bloques, alrededor de una Turbina de plasma en el centro de un extremo: una caja de 3 a 7 bloques de ancho y alto y de 3 a 16 de largo, con una columna de Rotores de turbina de fusión en su eje y una pareja de Puertos de turbina de fusión, uno de entrada y otro de salida, por cada 80 mB/t de vapor de plasma. Cada rotor expande 20 mB/t.");
    }
}
