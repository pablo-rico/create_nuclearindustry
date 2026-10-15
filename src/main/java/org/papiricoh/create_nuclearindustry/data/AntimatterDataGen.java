package org.papiricoh.create_nuclearindustry.data;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlock;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlock;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.antimatter.trap.LargePenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.MediumPenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.SmallPenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.TrapCorner;
import org.papiricoh.create_nuclearindustry.antimatter.trap.TrapLevel;
import org.papiricoh.create_nuclearindustry.data.recipe.AntimatterRecipes;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lang, blockstates, tags and recipes for antimatter containment. Every model is hand-made (see
 * tools/modelgen): the blockstates only point at them, and the items keep their own models.
 */
final class AntimatterDataGen extends AreaDataGen {
    static final AntimatterDataGen INSTANCE = new AntimatterDataGen();
    private static final String T = "create_nuclearindustry.penning_trap.";
    private static final TagKey<Block> NON_MOVABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "non_movable"));

    private AntimatterDataGen() {}

    // ------------------------------------------------------------------ lang

    @Override
    void lang() {
        LangEntries.block("small_penning_trap", "Small Penning Trap", "Trampa de Penning pequeña");
        LangEntries.block("penning_trap", "Penning Trap", "Trampa de Penning");
        LangEntries.block("large_penning_trap", "Large Penning Trap", "Trampa de Penning grande");
        LangEntries.block("antimatter_conduit", "Antimatter Conduit", "Conducto de antimateria");
        LangEntries.item("antimatter_vial", "Antimatter Vial", "Vial de antimateria");
        LangEntries.block("annihilation_generator", "Annihilation Generator", "Generador de aniquilación");
        LangEntries.block("antiproton_injector", "Antiproton Injector", "Inyector de antiprotones");
        vial();
        trap();
        conduit();
        generator();
        injector();
        controlRoom();
        generatorControlRoom();
        String s = "subtitle.create_nuclearindustry.";
        LangEntries.add(s + "trap_hum", "Penning trap hums", "Zumba una trampa de Penning");
        LangEntries.add(s + "trap_quench", "Penning trap quench alarm", "Alarma de quench de una trampa de Penning");
        LangEntries.add(s + "annihilation_blast", "Antimatter annihilates", "Se aniquila antimateria");
        LangEntries.add(s + "antiproton_shot", "Antiproton injector fires", "Dispara el inyector de antiprotones");
        jei();
    }

    private static void vial() {
        String v = "item.create_nuclearindustry.antimatter_vial.";
        LangEntries.add(v + "empty", "Empty: holds up to %s ng of antimatter", "Vacío: guarda hasta %s ng de antimateria");
        LangEntries.add(v + "charge", "Antimatter: %s / %s ng", "Antimateria: %s / %s ng");
        LangEntries.add(v + "battery", "Battery: %s s of field left", "Batería: quedan %s s de campo");
        LangEntries.add(v + "warning", "Annihilates when its battery runs out: keep it in a powered trap",
                "Se aniquila cuando se agota la batería: guárdalo en una trampa con energía");
    }

    private static void trap() {
        LangEntries.add(T + "state.offline", "Offline", "Apagada");
        LangEntries.add(T + "state.stable", "Stable", "Estable");
        LangEntries.add(T + "state.low_power", "Low power", "Poca energía");
        LangEntries.add(T + "state.quenching", "QUENCHING", "QUENCH");
        LangEntries.add(T + "mode.fill", "fill the vial", "llenar el vial");
        LangEntries.add(T + "mode.drain", "empty the vial", "vaciar el vial");
        LangEntries.add(T + "mode", "Vial port: %s", "Puerto de viales: %s");
        LangEntries.add(T + "port_full", "There is a vial in the port already", "Ya hay un vial en el puerto");
        LangEntries.add(T + "vial_in", "Vial in the port, set to %s", "Vial en el puerto, para %s");
        LangEntries.add(T + "status", "%s / %s ng, %s", "%s / %s ng, %s");
        String g = T + "goggles.";
        LangEntries.add(g + "stored", "Antimatter: %s / %s ng (%s%%)", "Antimateria: %s / %s ng (%s%%)");
        LangEntries.add(g + "state", "Field: %s", "Campo: %s");
        LangEntries.add(g + "field", "Field strength %s%%, needs %s FE/t", "Intensidad del campo %s%%, necesita %s FE/t");
        LangEntries.add(g + "energy", "Energy buffer: %s / %s FE", "Reserva de energía: %s / %s FE");
        LangEntries.add(g + "quench", "Annihilation in %s s!", "¡Aniquilación en %s s!");
        LangEntries.add(g + "time_left", "Without more energy: annihilates in %s s", "Sin más energía: se aniquila en %s s");
        LangEntries.add(g + "port_empty", "Vial port empty (%s)", "Puerto de viales vacío (%s)");
        LangEntries.add(g + "port", "Vial in the port: %s ng (%s)", "Vial en el puerto: %s ng (%s)");
        LangEntries.add("create_nuclearindustry.display_source.antimatter_trap", "Antimatter Trap", "Trampa de antimateria");
        LangEntries.add("create_nuclearindustry.display_source.antimatter_trap.line", "%s / %s ng, %s", "%s / %s ng, %s");
    }

    private static void conduit() {
        String g = "create_nuclearindustry.antimatter_conduit.goggles.";
        LangEntries.add(g + "powered", "Magnets powered", "Imanes con energía");
        LangEntries.add(g + "unpowered", "Not enough energy: nothing moves", "Energía insuficiente: no se mueve nada");
        LangEntries.add(g + "flowing", "Moving antimatter, up to %s ng/s", "Moviendo antimateria, hasta %s ng/s");
        LangEntries.add(g + "idle", "Idle, moves up to %s ng/s", "En reposo, mueve hasta %s ng/s");
        LangEntries.add(g + "energy", "Energy: %s / %s FE (%s FE/t per block)", "Energía: %s / %s FE (%s FE/t por bloque)");
    }

    private static void generator() {
        String k = "create_nuclearindustry.annihilation_generator.";
        LangEntries.add(k + "mode", "Mode", "Modo");
        LangEntries.add(k + "mode.rotation", "Rotation", "Rotación");
        LangEntries.add(k + "mode.electric", "Electric", "Eléctrico");
        LangEntries.add(k + "mode.split", "Split", "Mixto");
        LangEntries.add(k + "mode_set", "Mode: %s", "Modo: %s");
        LangEntries.add(k + "status.running", "Running", "En marcha");
        LangEntries.add(k + "status.standby", "Standby", "En espera");
        LangEntries.add(k + "status.short", "Short of antimatter", "Falta antimateria");
        LangEntries.add(k + "status.cold", "Cold", "Frío");
        LangEntries.add(k + "status.no_field", "No field: antimatter without energy!", "Sin campo: ¡antimateria sin energía!");
        LangEntries.add(k + "status.quenching", "QUENCHING", "QUENCH");
        LangEntries.add(k + "status_line", "Chamber %s ng, %s, output %s%%", "Cámara %s ng, %s, salida %s%%");
        String g = k + "goggles.";
        LangEntries.add(g + "status", "Status: %s", "Estado: %s");
        LangEntries.add(g + "mode", "Mode: %s", "Modo: %s");
        LangEntries.add(g + "fe_disabled", "Forge Energy is disabled in the config: rotation only",
                "La Forge Energy está desactivada en la configuración: solo rotación");
        LangEntries.add(g + "throttle", "Throttle %s%%, delivering %s%%", "Acelerador %s%%, entrega %s%%");
        LangEntries.add(g + "chamber", "Chamber: %s / %s ng", "Cámara: %s / %s ng");
        LangEntries.add(g + "field", "Chamber field %s%%", "Campo de la cámara %s%%");
        LangEntries.add(g + "field_lit", "Field held by the burn (%s%%)", "Campo mantenido por la combustión (%s%%)");
        LangEntries.add(g + "buffer", "Field buffer: %s / %s FE", "Reserva del campo: %s / %s FE");
        LangEntries.add(g + "quench", "Annihilation in %s s!", "¡Aniquilación en %s s!");
        LangEntries.add(g + "energy", "Making %s FE/t", "Produce %s FE/t");
        LangEntries.add(g + "short", "The supply cannot keep up with the throttle", "El suministro no da para el acelerador");
        LangEntries.add(g + "port_empty", "Vial port empty", "Puerto de viales vacío");
        LangEntries.add(g + "port", "Vial in the port: %s ng", "Vial en el puerto: %s ng");
        LangEntries.add("create_nuclearindustry.display_source.annihilation_generator", "Annihilation Generator", "Generador de aniquilación");
        LangEntries.add("create_nuclearindustry.display_source.annihilation_generator.line", "%s, %s%% (%s), %s ng", "%s, %s%% (%s), %s ng");
    }

    private static void injector() {
        String k = "create_nuclearindustry.antiproton_injector.";
        LangEntries.add(k + "arming.automatic", "automatic", "automático");
        LangEntries.add(k + "arming.redstone", "by redstone", "por redstone");
        LangEntries.add(k + "arming.safe", "safe", "seguro");
        LangEntries.add(k + "arming_set", "Arming: %s", "Armado: %s");
        LangEntries.add(k + "status_line", "%s doses, %s (%s)", "%s dosis, %s (%s)");
        String v = k + "verdict.";
        LangEntries.add(v + "ready", "Ready: firing", "Listo: dispara");
        LangEntries.add(v + "safe", "Disarmed", "Desarmado");
        LangEntries.add(v + "cooldown", "Recharging after the last shot", "Recargando tras el último disparo");
        LangEntries.add(v + "no_dose", "Less than one dose in the chamber", "Menos de una dosis en la cámara");
        LangEntries.add(v + "no_power", "Not enough energy for a shot", "Energía insuficiente para un disparo");
        LangEntries.add(v + "waiting", "Waiting for a heating plasma", "Esperando a un plasma en calentamiento");
        LangEntries.add(v + "ignited", "The plasma is already ignited", "El plasma ya está en ignición");
        LangEntries.add(v + "low_confinement", "Plasma confinement too low", "Confinamiento del plasma insuficiente");
        LangEntries.add(v + "low_density", "Plasma too thin", "Plasma demasiado tenue");
        String g = k + "goggles.";
        LangEntries.add(g + "arming", "Arming: %s (%s)", "Armado: %s (%s)");
        LangEntries.add(g + "armed", "armed", "armado");
        LangEntries.add(g + "disarmed", "disarmed", "desarmado");
        LangEntries.add(g + "dose", "Antiprotons: %s / %s ng (%s doses of %s ng)", "Antiprotones: %s / %s ng (%s dosis de %s ng)");
        LangEntries.add(g + "energy", "Energy: %s / %s FE (%s FE a shot)", "Energía: %s / %s FE (%s FE por disparo)");
        LangEntries.add(g + "field", "Chamber field: %s", "Campo de la cámara: %s");
        LangEntries.add(g + "quench", "Annihilation in %s s!", "¡Aniquilación en %s s!");
        LangEntries.add(g + "cooldown", "Ready again in %s s", "Listo de nuevo en %s s");
        LangEntries.add(g + "no_tokamak", "Not part of a formed tokamak", "No forma parte de un tokamak formado");
        LangEntries.add(g + "port", "Vial in the port: %s ng", "Vial en el puerto: %s ng");
    }

    /** The generator as a control room target: its kind, monitor rows and gauges, and the traps' gauges. */
    private static void generatorControlRoom() {
        LangEntries.add("create_nuclearindustry.control_room.kind.annihilation_generator", "Annihilation Generator", "Generador de aniquilación");
        String m = "create_nuclearindustry.monitor.";
        LangEntries.add(m + "gen_output", "Output", "Salida");
        LangEntries.add(m + "gen_status", "Generator", "Generador");
        LangEntries.add(m + "gen_mode", "Mode", "Modo");
        LangEntries.add(m + "gen_throttle", "Throttle", "Acelerador");
        LangEntries.add(m + "gen_energy", "Energy", "Energía");
        LangEntries.add(m + "gen_stress", "Capacity", "Capacidad");
        LangEntries.add(m + "gen_burn", "Burn", "Consumo");
        LangEntries.add(m + "gen_running", "Running", "En marcha");
        LangEntries.add(m + "gen_standby", "Standby", "En espera");
        LangEntries.add(m + "gen_cold", "Cold", "Frío");
        LangEntries.add(m + "gen_no_field", "NO FIELD", "SIN CAMPO");
        LangEntries.add(m + "gen_quenching", "QUENCH", "QUENCH");
        LangEntries.add(m + "gen_mode_rotation", "Rotation", "Rotación");
        LangEntries.add(m + "gen_mode_electric", "Electric", "Eléctrico");
        LangEntries.add(m + "gen_mode_split", "Split", "Mixto");
        String r = "create_nuclearindustry.control_module.reading.";
        LangEntries.add(r + "trap_antimatter", "Penning trap antimatter", "Antimateria de la trampa de Penning");
        LangEntries.add(r + "trap_field", "Penning trap field", "Campo de la trampa de Penning");
        LangEntries.add(r + "trap_buffer", "Penning trap energy buffer", "Reserva de energía de la trampa de Penning");
        LangEntries.add(r + "generator_output", "Annihilation generator output", "Salida del generador de aniquilación");
        LangEntries.add(r + "generator_throttle", "Annihilation generator throttle", "Acelerador del generador de aniquilación");
        LangEntries.add(r + "generator_chamber", "Annihilation generator chamber", "Cámara del generador de aniquilación");
        LangEntries.add(r + "generator_energy", "Annihilation generator energy", "Energía del generador de aniquilación");
    }

    /** The trap as a control room target: its kind, monitor rows, alarm metrics and annunciator preset. */
    private static void controlRoom() {
        LangEntries.add("create_nuclearindustry.control_room.kind.antimatter_trap", "Penning Trap", "Trampa de Penning");
        String m = "create_nuclearindustry.monitor.";
        LangEntries.add(m + "antimatter", "Antimatter", "Antimateria");
        LangEntries.add(m + "trap_state", "Trap", "Trampa");
        LangEntries.add(m + "trap_buffer", "Buffer", "Reserva");
        LangEntries.add(m + "trap_demand", "Demand", "Consumo");
        LangEntries.add(m + "trap_offline", "Offline", "Apagada");
        LangEntries.add(m + "trap_stable", "Stable", "Estable");
        LangEntries.add(m + "trap_low_power", "Low power", "Poca energía");
        LangEntries.add(m + "trap_quenching", "QUENCH", "QUENCH");
        String metric = "create_nuclearindustry.alarm.metric.";
        LangEntries.add(metric + "antimatter_stored", "Penning trap fill above", "Llenado de la trampa de Penning por encima de");
        LangEntries.add(metric + "trap_power", "Penning trap energy buffer below", "Reserva de energía de la trampa de Penning por debajo de");
        String a = "create_nuclearindustry.annunciator.";
        LangEntries.add(a + "short.antimatter_stored", "AM STORED", "AM ALMACENADA");
        LangEntries.add(a + "short.trap_power", "TRAP PWR LO", "TRAMPA SIN ENERG.");
        String p = a + "preset.antimatter";
        LangEntries.add(p, "Antimatter", "Antimateria");
        String[] en = {"AM LOADED", "AM STORE HI", "AM STORE FULL", "TRAP PWR LO", "TRAP PWR LO-LO", "QUENCH IMMINENT"};
        String[] es = {"AM CARGADA", "AM ALTA", "TRAMPA LLENA", "ENERGÍA BAJA", "ENERGÍA M. BAJA", "QUENCH INMINENTE"};
        for (int i = 0; i < en.length; i++) {
            LangEntries.add(p + "." + (i + 1), en[i], es[i]);
        }
    }

    private static void jei() {
        String p = PlantJeiInfo.PREFIX;
        LangEntries.add(p + "penning_traps",
                "Penning traps hold antiprotons in a magnetic field between two magnet heads, and need Forge Energy every tick to do it: a Small Penning Trap holds 32 ng for about 20 FE/t, a Penning Trap (two blocks tall) 256 ng for 80 FE/t and a Large Penning Trap (2x2, three high) 2048 ng for 250 FE/t; the fuller, the more they draw. Their own buffer lasts ten seconds if the power is cut; then the trap quenches, sounds an alarm for three seconds and annihilates everything it holds. Anything put into a trap without power annihilates at once, and so does breaking a loaded trap, which can set off the traps beside it. An empty trap is safe to move. Right-click with a vial to put it in the port; the wrench switches the port between filling and emptying the vial. Comparators read the fill.",
                "Las trampas de Penning guardan antiprotones en un campo magnético entre dos cabezales y necesitan Forge Energy en cada tick: una trampa de Penning pequeña guarda 32 ng con unos 20 FE/t, una trampa de Penning (dos bloques de alto) 256 ng con 80 FE/t y una trampa de Penning grande (2x2, tres de alto) 2048 ng con 250 FE/t; cuanto más llenas, más consumen. Su propia reserva dura diez segundos si se corta la energía; después la trampa entra en quench, suena una alarma durante tres segundos y aniquila todo lo que guarda. Cualquier cosa que entre en una trampa sin energía se aniquila al instante, igual que al romper una trampa cargada, que puede hacer estallar las de al lado. Una trampa vacía se puede mover sin peligro. Haz clic derecho con un vial para meterlo en el puerto; la llave cambia el puerto entre llenar y vaciar el vial. Los comparadores leen el llenado.");
        LangEntries.add(p + "antimatter_vial",
                "A pocket trap for 4 ng of antimatter, with its own battery. Fill it in a powered Penning trap's port, which also recharges its battery. Away from the port the battery of a loaded vial lasts thirty seconds, in your inventory or on the ground; when it runs out, the vial annihilates where it is. Empty vials are harmless and stack.",
                "Una trampa de bolsillo para 4 ng de antimateria, con su propia batería. Llénalo en el puerto de una trampa de Penning con energía, que también le recarga la batería. Fuera del puerto la batería de un vial cargado dura treinta segundos, en tu inventario o en el suelo; cuando se agota, el vial se aniquila donde esté. Los viales vacíos son inofensivos y se apilan.");
        LangEntries.add(p + "antimatter_conduit",
                "A magnetic tube that moves antimatter between the holders it joins: from a Target Station into traps, and between traps towards the same fill, up to 0.5 ng per tick. Every block of it needs 4 FE/t, fed by a cable on any block of the network; short of energy it moves nothing. It holds nothing itself, so it never explodes, but it delivers into traps without power too, which annihilates them.",
                "Un tubo magnético que mueve antimateria entre los contenedores que une: de una estación de blancos a las trampas, y entre trampas hasta igualar su llenado, hasta 0,5 ng por tick. Cada bloque necesita 4 FE/t, que llegan por un cable en cualquier bloque de la red; sin energía suficiente no mueve nada. No guarda nada, así que nunca explota, pero también entrega en trampas sin energía, lo que las aniquila.");
        LangEntries.add(p + "annihilation_generator",
                "Burns antiprotons from the traps, target stations and conduits beside it, or from a vial in its port, in a 1 ng chamber. At full throttle it uses 6 ng a minute for 256 RPM with 256 SU per RPM (Rotation), 16,384 FE/t (Electric) or half of each (Split); set the mode on the panel on top or with the wrench on the top face. A redstone signal sets the throttle: 0 keeps only a standby burn (2%) that holds the chamber's field, 15 is full output; short of antimatter the output drops with it. A cold chamber holds antimatter only with Forge Energy in its field buffer (12 FE/t), like a trap: give it energy once to start it. Once lit, the burn holds the field itself, and in Electric or Split it keeps its own buffer full and pushes the rest of its energy into every block beside it, so it can power its own traps. A cold chamber without energy takes no antimatter in, and a loaded vial left in its port annihilates when its battery runs flat; breaking it loaded annihilates it too. Comparators read the output.",
                "Quema antiprotones de las trampas, estaciones de blancos y conductos de al lado, o de un vial en su puerto, en una cámara de 1 ng. A pleno rendimiento gasta 6 ng por minuto para dar 256 RPM con 256 SU por RPM (Rotación), 16.384 FE/t (Eléctrico) o la mitad de cada cosa (Mixto); el modo se elige en el panel de arriba o con la llave en la cara superior. Una señal de redstone fija el acelerador: con 0 solo mantiene una combustión de espera (2 %) que sostiene el campo de la cámara, con 15 da todo; si falta antimateria, la salida baja con ella. Una cámara fría solo guarda antimateria con Forge Energy en la reserva del campo (12 FE/t), como una trampa: dale energía una vez para arrancarlo. Una vez encendido, la combustión sostiene el campo por sí sola, y en Eléctrico o Mixto mantiene llena su propia reserva y empuja el resto de su energía a todos los bloques de al lado, así que puede alimentar sus propias trampas. Una cámara fría sin energía no admite antimateria, y un vial cargado que se quede en su puerto se aniquila cuando se le agota la batería; romperlo cargado también lo aniquila. Los comparadores leen la salida.");
        LangEntries.add(p + "antiproton_injector",
                "A part of a tokamak's shell with a small Penning trap (8 ng) and a beam nozzle into the chamber. It takes Forge Energy and antimatter (from a trap, a conduit or a vial in its port) on any face. While the tokamak is heating a plasma held at 55% confinement or better and at least 5% dense, it fires one dose of 2 ng and 20,000 FE, which annihilates in the plasma and lifts it straight past ignition; then it waits ten seconds. The wrench switches it between automatic, by redstone and safe. Its chamber needs energy like any trap: without it, it annihilates in the tokamak's wall. Comparators read the doses it holds.",
                "Una pieza de la carcasa de un tokamak con una pequeña trampa de Penning (8 ng) y una tobera de haz hacia la cámara. Toma Forge Energy y antimateria (de una trampa, un conducto o un vial en su puerto) por cualquier cara. Mientras el tokamak calienta un plasma con un confinamiento del 55 % o más y al menos un 5 % de densidad, dispara una dosis de 2 ng y 20.000 FE, que se aniquila en el plasma y lo lleva directamente más allá de la ignición; luego espera diez segundos. La llave lo cambia entre automático, por redstone y seguro. Su cámara necesita energía como cualquier trampa: sin ella se aniquila en la pared del tokamak. Los comparadores leen las dosis que guarda.");
        LangEntries.add("jei.create_nuclearindustry.category.target_station", "Antiproton Production", "Producción de antiprotones");
        String t = "jei.create_nuclearindustry.target_station.";
        LangEntries.add(t + "threshold", "Beam of %s GeV or more", "Haz de %s GeV o más");
        LangEntries.add(t + "saturation", "Best yield from %s GeV", "Rendimiento máximo desde %s GeV");
        LangEntries.add(t + "yield", "%s ng/min per MW at %s GeV", "%s ng/min por MW a %s GeV");
        LangEntries.add(t + "curve", "Antiprotons per minute and MW of beam, by beam energy", "Antiprotones por minuto y MW de haz, según la energía del haz");
        LangEntries.add(t + "output", "Into a powered trap or conduit beside the station", "A una trampa o conducto con energía junto a la estación");
        LangEntries.add(p + "creative_rf_generator",
                "Creative only: pushes up to a million FE/t into every block beside it, and gives cables as much as they ask. Set the rate on its value panel. It has no recipe and cannot be broken in survival.",
                "Solo en creativo: empuja hasta un millón de FE/t a cada bloque de al lado y da a los cables todo lo que pidan. Ajusta el ritmo en su panel de valor. No tiene receta y no se puede romper en supervivencia.");
    }

    // ------------------------------------------------------------------ models

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile small = existing(provider, "small_penning_trap");
        provider.getVariantBuilder(AntimatterRegistry.SMALL_PENNING_TRAP.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(small).rotationY(yaw(state.getValue(SmallPenningTrapBlock.FACING))).build());
        ModelFile lower = existing(provider, "penning_trap/lower");
        ModelFile upper = existing(provider, "penning_trap/upper");
        provider.getVariantBuilder(AntimatterRegistry.PENNING_TRAP.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(MediumPenningTrapBlock.HALF) == DoubleBlockHalf.LOWER ? lower : upper)
                .rotationY(yaw(state.getValue(MediumPenningTrapBlock.FACING))).build());
        provider.getVariantBuilder(AntimatterRegistry.LARGE_PENNING_TRAP.get()).forAllStates(state -> {
            TrapLevel level = state.getValue(LargePenningTrapBlock.LEVEL);
            TrapCorner corner = state.getValue(LargePenningTrapBlock.CORNER);
            return ConfiguredModel.builder().modelFile(existing(provider, "large_penning_trap/" + level.getSerializedName() + "_"
                    + corner.getSerializedName())).build();
        });
        conduit(provider);
        ModelFile generator = existing(provider, "annihilation_generator");
        provider.getVariantBuilder(AntimatterRegistry.ANNIHILATION_GENERATOR.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(generator).rotationY(yaw(state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING))).build());
        ModelFile injector = existing(provider, "antiproton_injector");
        provider.getVariantBuilder(AntimatterRegistry.ANTIPROTON_INJECTOR.get()).forAllStates(state -> {
            Direction facing = state.getValue(AntiprotonInjectorBlock.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? yaw(facing) : 0;
            return ConfiguredModel.builder().modelFile(injector).rotationX(x).rotationY(y).build();
        });
    }

    /** The conduit's core, with an arm towards every side it joins. */
    private static void conduit(NuclearBlockStateProvider provider) {
        ModelFile core = existing(provider, "antimatter_conduit/core");
        ModelFile arm = existing(provider, "antimatter_conduit/arm");
        var builder = provider.getMultipartBuilder(AntimatterRegistry.ANTIMATTER_CONDUIT.get());
        builder.part().modelFile(core).addModel().end();
        for (Direction side : Direction.values()) {
            int x = side == Direction.UP ? 270 : side == Direction.DOWN ? 90 : 0;
            int y = side.getAxis().isHorizontal() ? ((int) side.toYRot() + 180) % 360 : 0;
            builder.part().modelFile(arm).rotationX(x).rotationY(y).addModel()
                    .condition(AntimatterConduitBlock.SIDES.get(side), true).end();
        }
    }

    private static int yaw(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    private static ModelFile existing(NuclearBlockStateProvider provider, String name) {
        return provider.models().getExistingFile(provider.modLoc("block/" + name));
    }

    // ------------------------------------------------------------------ tags and recipes

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        List<Block> sturdy = List.of(AntimatterRegistry.SMALL_PENNING_TRAP.get(), AntimatterRegistry.PENNING_TRAP.get(),
                AntimatterRegistry.LARGE_PENNING_TRAP.get(), AntimatterRegistry.ANTIMATTER_CONDUIT.get(),
                AcceleratorRegistry.RING_DIPOLE.get(), AcceleratorRegistry.SC_RING_DIPOLE.get(), AcceleratorRegistry.SC_RF_CAVITY.get(),
                AcceleratorRegistry.INJECTION_SEPTUM.get(), AcceleratorRegistry.EXTRACTION_SEPTUM.get(),
                AcceleratorRegistry.TARGET_STATION.get(), AcceleratorRegistry.CRYO_FEED.get(), AcceleratorRegistry.BEAM_INTERLOCK.get(),
                AntimatterRegistry.ANNIHILATION_GENERATOR.get(), AntimatterRegistry.ANTIPROTON_INJECTOR.get());
        provider.tagFor(BlockTags.NEEDS_IRON_TOOL).add(sturdy.toArray(Block[]::new));
        // The injector is a tokamak shell part: changing it makes the tokamak look again.
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(AntimatterRegistry.ANTIPROTON_INJECTOR.get());
        // Antimatter holders keep their field only in place: contraptions cannot carry them off unpowered.
        provider.tagFor(NON_MOVABLE).add(AntimatterRegistry.SMALL_PENNING_TRAP.get(), AntimatterRegistry.PENNING_TRAP.get(),
                AntimatterRegistry.LARGE_PENNING_TRAP.get(), AntimatterRegistry.ANNIHILATION_GENERATOR.get(),
                AntimatterRegistry.ANTIPROTON_INJECTOR.get());
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Antimatter crafting recipes", new AntimatterRecipes(output, lookup)));
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
