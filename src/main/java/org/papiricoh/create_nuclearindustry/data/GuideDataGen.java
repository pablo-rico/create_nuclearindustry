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
                "A nuclear weapon. Its three slots take weapons-grade material: uranium enriched to 90% or more, or plutonium, which counts double. The more it holds and the richer it is, the more powerful the blast and the wider the crater, up to 250 blocks from ground zero. Power it with redstone and it goes off after a 10 second countdown; taking the signal away stops the countdown. Beyond the crater a blast wave hurts and throws whoever is in the open, shatters glass and leaves, and fallout contaminates the ground. Sirens within reach sound the attack warning during the countdown.",
                "Un arma nuclear. Sus tres huecos admiten material de grado militar: uranio enriquecido al 90% o más, o plutonio, que cuenta doble. Cuanto más lleva y más rico es, más potente es la explosión y más ancho el cráter, hasta 250 bloques desde la zona cero. Dale una señal de redstone y estalla tras una cuenta atrás de 10 segundos; si se quita la señal, la cuenta se detiene. Más allá del cráter, una onda expansiva hiere y lanza a quien esté al descubierto, rompe vidrios y hojas, y la lluvia radiactiva contamina el suelo. Las sirenas cercanas dan el aviso de ataque durante la cuenta atrás.");
        page("missile_launch",
                "The Missile Launch Pad fires a Nuclear Missile at a block far away. Right-click the target with a Target Designator, then put the designator, a missile and the warhead in the pad. Its three warhead slots take weapons-grade uranium or plutonium for a nuclear warhead, or other radioactive material for a dirty one; both together make a salted warhead with dirtier, wider fallout. Power the pad with redstone: after a 10 second countdown the missile lifts off. The missile and the warhead are used up; the designator stays. Sirens near the target sound the attack warning while it flies.",
                "La Plataforma de lanzamiento dispara un Misil nuclear contra un bloque lejano. Haz clic derecho en el objetivo con un Designador de objetivo y pon en la plataforma el designador, un misil y la cabeza. Sus tres huecos de cabeza admiten uranio o plutonio de grado militar para una cabeza nuclear, u otro material radiactivo para una sucia; ambos a la vez dan una cabeza salada con una lluvia radiactiva más sucia y amplia. Dale una señal de redstone: tras una cuenta atrás de 10 segundos el misil despega. El misil y la cabeza se gastan; el designador se queda. Las sirenas cerca del objetivo dan el aviso de ataque mientras vuela.");
        page("nuclear_cannon_shell",
                "A big cannon shell from Create Big Cannons with a full nuclear warhead. Load and fire it like any other shell; its fuze sets it off with the blast, crater, blast wave and fallout of a fully loaded Nuclear Bomb.",
                "Un proyectil de cañón grande de Create Big Cannons con una cabeza nuclear completa. Se carga y se dispara como cualquier otro proyectil; su espoleta lo hace estallar con la explosión, el cráter, la onda expansiva y la lluvia radiactiva de una Bomba nuclear cargada al máximo.");

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
                "The fusion turbine is built like the steam turbine, from these blocks, around a Plasma Turbine at the centre of one end: a box 3-7 blocks wide and high and 3-16 long, with a column of Fusion Turbine Rotors on its axis and a pair of Fusion Turbine Fluid Ports, one on input and one on output, for every 160 mB/t of plasma steam. Each rotor expands 40 mB/t.",
                "La turbina de fusión se construye como la de vapor, con estos bloques, alrededor de una Turbina de plasma en el centro de un extremo: una caja de 3 a 7 bloques de ancho y alto y de 3 a 16 de largo, con una columna de Rotores de turbina de fusión en su eje y una pareja de Puertos de turbina de fusión, uno de entrada y otro de salida, por cada 160 mB/t de vapor de plasma. Cada rotor expande 40 mB/t.");

        // Fan treatments
        page("acid_leaching",
                "Bulk Acid Leaching: an Encased Fan whose air current passes through Sulfuric Acid leaches the items it blows on, on a depot, a belt or in flight, as washing does with water. A bucket places the acid as a source block; wall it in at the sides so it does not run off. JEI lists what each item becomes.",
                "Lixiviación ácida en masa: un Ventilador revestido cuya corriente de aire atraviesa Ácido sulfúrico lixivia los ítems sobre los que sopla, en un depósito, una cinta o en vuelo, como hace el lavado con el agua. Un cubo coloca el ácido como bloque fuente; ciérralo por los lados para que no se derrame. JEI muestra en qué se convierte cada ítem.");
        page("irradiation",
                "Bulk Irradiation: an Encased Fan whose air current passes a strongly radioactive block irradiates the items it blows on. Any block whose item gives off at least 2 rad/s will do (fuelcycle.irradiationMinActivity), such as a Vitrified Waste Canister, as do the blocks in the irradiation catalyst tag. Thorium Dioxide breeds into Irradiated Thorium this way. The block irradiates you too: shield it.",
                "Irradiación en masa: un Ventilador revestido cuya corriente de aire pasa por un bloque muy radiactivo irradia los ítems sobre los que sopla. Sirve cualquier bloque cuyo ítem emita al menos 2 rad/s (fuelcycle.irradiationMinActivity), como un Contenedor de residuos vitrificados, y también los bloques de la etiqueta de catalizadores de irradiación. Así el Dióxido de torio se convierte en Torio irradiado. El bloque también te irradia a ti: blíndalo.");
    }
}
