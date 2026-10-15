package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.data.recipe.IsotopeRecipes;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.channel.ChannelStatus;
import org.papiricoh.create_nuclearindustry.isotope.channel.IrradiationChannelBlock;
import org.papiricoh.create_nuclearindustry.isotope.smoke.SmokeDetectorBlock;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Lang, blockstates, item models, tags, radioactivity and recipes for isotopes and nuclear medicine.
 * The three blocks have hand-made models (tools/modelgen): their blockstates only point at them,
 * and their items keep their own models.
 */
final class IsotopeDataGen extends AreaDataGen {
    static final IsotopeDataGen INSTANCE = new IsotopeDataGen();
    private static final String I = "item.create_nuclearindustry.";

    private IsotopeDataGen() {}

    // ------------------------------------------------------------------ lang

    @Override
    void lang() {
        LangEntries.item("cobalt_nugget", "Cobalt Nugget", "Pepita de cobalto");
        LangEntries.item("cobalt_ingot", "Cobalt Ingot", "Lingote de cobalto");
        LangEntries.item("cobalt_60", "Cobalt-60 Source", "Fuente de cobalto-60");
        LangEntries.item("strontium_90", "Strontium-90", "Estroncio-90");
        LangEntries.item("americium_241", "Americium-241 Foil", "Lámina de americio-241");
        LangEntries.item("technetium_99m", "Technetium-99m Dose", "Dosis de tecnecio-99m");
        LangEntries.item("technetium_generator", "Technetium-99m Generator", "Generador de tecnecio-99m");
        LangEntries.item("strontium_rtg_pellet", "Strontium-90 RTG Pellet", "Pastilla RTG de estroncio-90");
        LangEntries.item("radiography_camera", "Radiography Camera", "Cámara de radiografía");
        LangEntries.item("medical_isotope_kit", "Medical Isotope Kit", "Kit de isótopos médicos");
        LangEntries.block("gamma_irradiator", "Gamma Irradiator", "Irradiador gamma");
        LangEntries.block("smoke_detector", "Smoke Detector", "Detector de humo");
        LangEntries.block("reactor_irradiation_channel", "Reactor Irradiation Channel", "Canal de irradiación del reactor");
        LangEntries.add("effect.create_nuclearindustry.isotope_tracer", "Isotope Tracer", "Trazador isotópico");
        String k = I + "isotope.";
        LangEntries.add(k + "activity", "Activity: %s%%", "Actividad: %s %%");
        LangEntries.add(k + "half_life", "Half-life: %s", "Periodo de semidesintegración: %s");
        LangEntries.add(k + "decayed", "Decayed: too weak to use", "Desintegrado: demasiado débil para usarlo");
        LangEntries.add(k + "hours", "%s h", "%s h");
        LangEntries.add(k + "days", "%s days", "%s días");
        LangEntries.add(I + "sterile_food", "Sterilised by gamma rays: no harmful effects", "Esterilizado con rayos gamma: sin efectos dañinos");
        cow();
        kit();
        radiography();
        machines();
        jei();
    }

    private static void cow() {
        String k = I + "technetium_generator.";
        LangEntries.add(k + "parent", "Molybdenum-99: %s%%", "Molibdeno-99: %s %%");
        LangEntries.add(k + "daughter", "Ready to milk: %s doses of Tc-99m", "Listo para ordeñar: %s dosis de Tc-99m");
        LangEntries.add(k + "hint", "Use to milk its technetium-99m; it peaks about a day after milking",
                "Úsalo para ordeñar su tecnecio-99m; llega al máximo un día después de ordeñarlo");
        LangEntries.add(k + "exhausted", "Exhausted: its molybdenum-99 has decayed", "Agotado: su molibdeno-99 se ha desintegrado");
        LangEntries.add(k + "too_soon", "Not enough technetium-99m yet: wait a few hours", "Aún no hay tecnecio-99m suficiente: espera unas horas");
        LangEntries.add(k + "eluted", "Milked %s doses of technetium-99m", "Ordeñadas %s dosis de tecnecio-99m");
    }

    private static void kit() {
        String k = I + "medical_isotope_kit.";
        LangEntries.add(k + "hint", "Use on yourself or another player with a Tc-99m dose in your inventory",
                "Úsalo contigo o con otro jugador con una dosis de Tc-99m en el inventario");
        LangEntries.add(k + "no_dose", "No technetium-99m dose in your inventory", "No tienes ninguna dosis de tecnecio-99m en el inventario");
        LangEntries.add(k + "decayed", "Your technetium-99m has decayed", "Tu tecnecio-99m se ha desintegrado");
        LangEntries.add(k + "header", "Nuclear medicine scan of %s", "Gammagrafía de %s");
        LangEntries.add(k + "dose", "Dose held: %s", "Dosis acumulada: %s");
        LangEntries.add(k + "rate", "Taking now: %s per second", "Recibiendo ahora: %s por segundo");
        LangEntries.add(k + "carried", "From carried items: %s per second", "De lo que lleva encima: %s por segundo");
        LangEntries.add(k + "ground", "From contaminated ground: %s per second", "Del suelo contaminado: %s por segundo");
        LangEntries.add(k + "total", "Lifetime total: %s", "Total de por vida: %s");
        LangEntries.add(k + "iodide_on", "Potassium iodide: active", "Yoduro potásico: activo");
        LangEntries.add(k + "iodide_off", "Potassium iodide: none", "Yoduro potásico: ninguno");
        LangEntries.add(k + "purge_on", "Prussian blue: active", "Azul de Prusia: activo");
        LangEntries.add(k + "purge_off", "Prussian blue: none", "Azul de Prusia: ninguno");
    }

    private static void radiography() {
        String k = I + "radiography_camera.";
        LangEntries.add(k + "hint", "Use on a machine with a cobalt-60 source in your inventory", "Úsala en una máquina con una fuente de cobalto-60 en el inventario");
        LangEntries.add(k + "no_source", "No cobalt-60 source in your inventory", "No tienes ninguna fuente de cobalto-60 en el inventario");
        LangEntries.add(k + "weak_source", "Your cobalt-60 source is too weak", "Tu fuente de cobalto-60 es demasiado débil");
        LangEntries.add(k + "nothing", "Nothing to radiograph here", "Aquí no hay nada que radiografiar");
        LangEntries.add(k + "header", "Radiograph (sees %s details deep):", "Radiografía (ve %s detalles de profundidad):");
        LangEntries.add(k + "too_deep", "  ... too deep for this source", "  ... demasiado profundo para esta fuente");
        LangEntries.add(k + "sound", "No hidden damage", "Sin daños ocultos");
        String r = "create_nuclearindustry.radiograph.reactor.";
        LangEntries.add(r + "core_damage", "Core damage: %s%%", "Daño del núcleo: %s %%");
        LangEntries.add(r + "cladding", "Fuel cladding oxidised: %s", "Vaina del combustible oxidada: %s");
        LangEntries.add(r + "vessel", "Pressure vessel: %s", "Vasija de presión: %s");
        LangEntries.add(r + "vessel_sound", "sound", "intacta");
        LangEntries.add(r + "vessel_breached", "BREACHED", "ROTA");
        LangEntries.add(r + "stress", "Thermal stress: %s%%", "Tensión térmica: %s %%");
        LangEntries.add(r + "fuel", "Fuel left: %s%%", "Combustible restante: %s %%");
        LangEntries.add(r + "rods", "Control rods in place: %s / %s segments", "Barras de control en su sitio: %s / %s segmentos");
    }

    private static void machines() {
        String g = "create_nuclearindustry.gamma_irradiator.goggles.";
        LangEntries.add(g + "sources", "Cobalt-60 sources: %s / %s (%s%% in all)", "Fuentes de cobalto-60: %s / %s (%s %% en total)");
        LangEntries.add(g + "empty", "Right-click with cobalt-60 to load it", "Haz clic derecho con cobalto-60 para cargarlo");
        LangEntries.add(g + "too_weak", "Too weak to sterilise: food passes", "Demasiado débil para esterilizar: la comida pasa");
        LangEntries.add(g + "time", "Sterilises harmful food in %s s", "Esteriliza la comida dañina en %s s");
        LangEntries.add(g + "service", "About %s days before it is too weak", "Unos %s días hasta que sea demasiado débil");
        LangEntries.add(g + "open", "Shutter open: irradiating", "Obturador abierto: irradiando");
        LangEntries.add(g + "closed", "Shutter closed", "Obturador cerrado");
        String c = "create_nuclearindustry.irradiation_channel.goggles.";
        LangEntries.add(c + "linked", "In a reactor's wall", "En la pared de un reactor");
        LangEntries.add(c + "not_linked", "Not part of a formed reactor", "No forma parte de un reactor formado");
        LangEntries.add(c + "ready", "%s cobalt-60 sources ready", "%s fuentes de cobalto-60 listas");
        LangEntries.add(c + "empty", "Empty: load cobalt ingots", "Vacío: carga lingotes de cobalto");
        LangEntries.add(c + "batch", "%s cobalt ingots, %s%% irradiated", "%s lingotes de cobalto, %s %% irradiados");
        LangEntries.add(c + "no_neutrons", "The reactor gives no neutrons", "El reactor no da neutrones");
        LangEntries.add(c + "time_left", "Done in %s s at this power", "Listos en %s s a esta potencia");
        String t = "create_nuclearindustry.accelerator.target.goggles.";
        LangEntries.add(t + "target", "Target: %s (%s%%), waiting for a beam below the antiproton threshold",
                "Blanco: %s (%s %%), esperando un haz por debajo del umbral de antiprotones");
        LangEntries.add(t + "irradiating", "Irradiating %s: %s%%", "Irradiando %s: %s %%");
    }

    private static void jei() {
        String p = PlantJeiInfo.PREFIX;
        LangEntries.add(p + "isotope_decay",
                "Radioisotopes decay on their own clock, in your inventory, in a chest or in a machine: technetium-99m halves in six hours of game time, molybdenum-99 in under three days, cobalt-60 in 60 days, strontium-90 in 330 and americium-241 practically never. The bar under the item shows the activity left; the Geiger counter and the dose you take follow it. A decayed source is too weak to use.",
                "Los radioisótopos se desintegran con su propio reloj, en el inventario, en un cofre o en una máquina: el tecnecio-99m se reduce a la mitad en seis horas de juego, el molibdeno-99 en menos de tres días, el cobalto-60 en 60 días, el estroncio-90 en 330 y el americio-241 prácticamente nunca. La barra bajo el objeto muestra la actividad que queda; el contador Geiger y la dosis que recibes la siguen. Una fuente desintegrada es demasiado débil para usarla.");
        LangEntries.add(p + "cobalt",
                "Cobalt hides in iron and copper ores: heated mixing of crushed raw iron with crushed raw copper frees two cobalt nuggets. Nine make an ingot, the target for the Reactor Irradiation Channel.",
                "El cobalto se esconde en las menas de hierro y de cobre: mezclar en caliente hierro bruto triturado con cobre bruto triturado libera dos pepitas de cobalto. Nueve forman un lingote, el blanco del canal de irradiación del reactor.");
        LangEntries.add(p + "reactor_irradiation_channel",
                "A sample tube through a fission reactor's wall: build it in place of any casing block. Load up to sixteen cobalt ingots; every tick the running reactor adds its neutron level to the batch, and at rated power (1000) about five minutes turn the whole batch into cobalt-60 sources. Cobalt added later shares the batch's progress. Pipes and funnels load it and take the sources out; comparators read the progress, 15 when it is ready.",
                "Un tubo de muestras a través de la pared de un reactor de fisión: constrúyelo en lugar de cualquier bloque de carcasa. Carga hasta dieciséis lingotes de cobalto; en cada tick el reactor en marcha suma su nivel de neutrones al lote, y a potencia nominal (1000) unos cinco minutos convierten todo el lote en fuentes de cobalto-60. El cobalto que se añade después comparte el progreso del lote. Las tuberías y embudos lo cargan y sacan las fuentes; los comparadores leen el progreso, 15 cuando está listo.");
        LangEntries.add(p + "gamma_irradiator",
                "Holds up to four cobalt-60 sources in a lead head and works two blocks above a belt or depot, like a press. Food with a harmful effect (raw chicken, rotten flesh, spider eyes, pufferfish, poisonous potatoes) stops under it while the shutter opens and comes out sterilised: same nourishment, no harmful effects, and not radioactive. One fresh source takes five seconds; four take a quarter of that. As the cobalt decays it gets slower, and below a twelfth of a fresh source it lets food pass. Open, it irradiates whoever stands by the belt. Sneak with an empty hand to take the weakest source out.",
                "Guarda hasta cuatro fuentes de cobalto-60 en un cabezal de plomo y trabaja dos bloques por encima de una cinta o un depósito, como una prensa. La comida con un efecto dañino (pollo crudo, carne podrida, ojos de araña, pez globo, patatas venenosas) se detiene debajo mientras se abre el obturador y sale esterilizada: el mismo alimento, sin efectos dañinos y sin radiactividad. Una fuente nueva tarda cinco segundos; cuatro, la cuarta parte. Según se desintegra el cobalto, va más lento, y por debajo de una doceava parte de una fuente nueva deja pasar la comida. Abierto, irradia a quien esté junto a la cinta. Agáchate con la mano vacía para sacar la fuente más débil.");
        LangEntries.add(p + "smoke_detector",
                "A speck of americium-241 keeps the air in its chamber ionised; smoke stops the current. Fixed to a floor, wall or ceiling, it gives a redstone signal when there is fire, a lit campfire, a lit furnace, lava close by or a burning creature within five blocks, stronger the more smoke reaches it, and beeps. The americium lasts centuries.",
                "Una pizca de americio-241 mantiene ionizado el aire de su cámara; el humo corta la corriente. Fijado a un suelo, una pared o un techo, da una señal de redstone cuando hay fuego, una fogata o un horno encendidos, lava cerca o una criatura ardiendo a menos de cinco bloques, más fuerte cuanto más humo le llega, y pita. El americio dura siglos.");
        LangEntries.add(p + "strontium_90",
                "Strontium-90 is extracted from PUREX high-level waste with nitric acid (heated mixing). Two compacted into an iron capsule make a Strontium-90 RTG Pellet: less power than plutonium, but it lasts twice as long.",
                "El estroncio-90 se extrae de los residuos de alta actividad del PUREX con ácido nítrico (mezcla en caliente). Dos compactados en una cápsula de hierro forman una pastilla RTG de estroncio-90: menos potencia que el plutonio, pero dura el doble.");
        LangEntries.add(p + "americium_241",
                "Plutonium grows americium-241 as its plutonium-241 decays. Plutonium starts ageing when a player first carries it; after about a week of game time it holds enough americium to separate it with nitric acid (heated mixing), which gives the plutonium back.",
                "El plutonio genera americio-241 según se desintegra su plutonio-241. El plutonio empieza a envejecer cuando un jugador lo lleva encima por primera vez; tras una semana de juego aproximadamente tiene americio suficiente para separarlo con ácido nítrico (mezcla en caliente), que devuelve el plutonio.");
        LangEntries.add(p + "technetium_generator",
                "Made at an accelerator's Target Station: a depleted uranium ingot in its slot, hit by a beam of 1 GeV or more but below the antiproton threshold, becomes a column of molybdenum-99 in a minute per megawatt. Its molybdenum decays into technetium-99m, which you milk off by using the generator: up to four doses about a day after the last milking. It is worn out in a little over a week.",
                "Se fabrica en la estación de blancos de un acelerador: un lingote de uranio empobrecido en su ranura, bajo un haz de 1 GeV o más pero por debajo del umbral de antiprotones, se convierte en una columna de molibdeno-99 en un minuto por megavatio. Su molibdeno se desintegra en tecnecio-99m, que se ordeña usando el generador: hasta cuatro dosis un día después del último ordeño. Se agota en poco más de una semana.");
        LangEntries.add(p + "medical_isotope_kit",
                "Nuclear medicine: with a technetium-99m dose in your inventory, use it on yourself or on another player for a full scan: dose held, what is adding to it from carried items and from the ground, the lifetime total and the medicines at work. The tracer left in the body makes potassium iodide and Prussian blue up to 50% stronger for two minutes. Technetium-99m halves every six hours: milk it shortly before use.",
                "Medicina nuclear: con una dosis de tecnecio-99m en el inventario, úsalo contigo o con otro jugador para un estudio completo: dosis acumulada, lo que la aumenta desde lo que lleva encima y desde el suelo, el total de por vida y los medicamentos activos. El trazador que queda en el cuerpo hace el yoduro potásico y el azul de Prusia hasta un 50 % más eficaces durante dos minutos. El tecnecio-99m se reduce a la mitad cada seis horas: ordéñalo poco antes de usarlo.");
        LangEntries.add(p + "radiography_camera",
                "Industrial gamma radiography: with a cobalt-60 source in your inventory, use it on any block of a formed machine to see what it hides, such as core damage, oxidised cladding, a cracked vessel or thermal stress. A weaker source sees fewer details and takes longer per shot. Each shot gives you a small dose.",
                "Radiografía gamma industrial: con una fuente de cobalto-60 en el inventario, úsala en cualquier bloque de una máquina formada para ver lo que oculta, como daño del núcleo, vainas oxidadas, una vasija agrietada o tensión térmica. Una fuente más débil ve menos detalles y tarda más por disparo. Cada disparo te da una pequeña dosis.");
        LangEntries.add("jei.create_nuclearindustry.category.target_irradiation", "Target Irradiation", "Irradiación de blancos");
        LangEntries.add("jei.create_nuclearindustry.target_irradiation.energy", "%s MJ of beam, %s GeV to %s GeV",
                "%s MJ de haz, de %s GeV a %s GeV");
        LangEntries.add("jei.create_nuclearindustry.category.reactor_irradiation", "Reactor Irradiation", "Irradiación en el reactor");
        LangEntries.add("jei.create_nuclearindustry.reactor_irradiation.time", "About %s s at rated power", "Unos %s s a potencia nominal");
        LangEntries.add("jei.create_nuclearindustry.category.gamma_sterilisation", "Gamma Sterilisation", "Esterilización gamma");
        LangEntries.add("jei.create_nuclearindustry.gamma_sterilisation.time", "%s s under one fresh source", "%s s bajo una fuente nueva");
    }

    // ------------------------------------------------------------------ models

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        provider.simpleBlock(IsotopeRegistry.GAMMA_IRRADIATOR.get(), existing(provider, "gamma_irradiator"));
        ModelFile quiet = existing(provider, "smoke_detector");
        ModelFile alarm = existing(provider, "smoke_detector_alarm");
        provider.getVariantBuilder(IsotopeRegistry.SMOKE_DETECTOR.get()).forAllStates(state -> {
            AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
            Direction facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
            // The model faces north; vanilla button rotations.
            int yaw = ((int) facing.toYRot() + 180) % 360;
            int x = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
            int y = face == AttachFace.CEILING ? (yaw + 180) % 360 : yaw;
            return ConfiguredModel.builder().modelFile(state.getValue(SmokeDetectorBlock.POWER) > 0 ? alarm : quiet)
                    .rotationX(x).rotationY(y).uvLock(false).build();
        });
        provider.getVariantBuilder(IsotopeRegistry.IRRADIATION_CHANNEL.get()).forAllStates(state -> {
            ChannelStatus status = state.getValue(IrradiationChannelBlock.STATUS);
            String name = status == ChannelStatus.EMPTY ? "reactor_irradiation_channel" : "reactor_irradiation_channel_" + status.getSerializedName();
            Direction facing = state.getValue(IrradiationChannelBlock.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            return ConfiguredModel.builder().modelFile(existing(provider, name)).rotationX(x).rotationY(y).build();
        });
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        for (Supplier<? extends Item> item : List.<Supplier<? extends Item>>of(IsotopeRegistry.COBALT_NUGGET, IsotopeRegistry.COBALT_INGOT,
                IsotopeRegistry.COBALT_60, IsotopeRegistry.STRONTIUM_90, IsotopeRegistry.AMERICIUM_241, IsotopeRegistry.TECHNETIUM_99M,
                IsotopeRegistry.TECHNETIUM_GENERATOR, IsotopeRegistry.STRONTIUM_RTG_PELLET, IsotopeRegistry.RADIOGRAPHY_CAMERA,
                IsotopeRegistry.MEDICAL_ISOTOPE_KIT)) {
            provider.basicItem(item.get());
        }
    }

    private static ModelFile existing(NuclearBlockStateProvider provider, String name) {
        return provider.models().getExistingFile(provider.modLoc("block/" + name));
    }

    // ------------------------------------------------------------------ tags, radioactivity, recipes

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.NEEDS_IRON_TOOL).add(IsotopeRegistry.GAMMA_IRRADIATOR.get());
        // The channel is a reactor wall part: changing it makes the reactor look again.
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(IsotopeRegistry.IRRADIATION_CHANNEL.get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // A fresh source; the dose then follows its activity (RadioactivityModifiers).
        var radioactivity = provider.builderFor(AllNuclearRadiation.RADIOACTIVITY);
        add(radioactivity, IsotopeRegistry.COBALT_60, 3.0f);
        add(radioactivity, IsotopeRegistry.STRONTIUM_90, 1.0f);
        add(radioactivity, IsotopeRegistry.AMERICIUM_241, 0.1f);
        add(radioactivity, IsotopeRegistry.TECHNETIUM_99M, 0.4f);
        add(radioactivity, IsotopeRegistry.TECHNETIUM_GENERATOR, 1.0f);
        add(radioactivity, IsotopeRegistry.STRONTIUM_RTG_PELLET, 2.5f);
    }

    private static void add(NuclearDataMapProvider.Builder<Radioactivity, Item> builder, Supplier<? extends Item> item, float perSecond) {
        builder.add(item.get().builtInRegistryHolder(), new Radioactivity(perSecond, false), false);
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new IsotopeRecipes(output, lookup));
    }
}
