package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEventTrigger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The progression tree, from the first raw uranium through the fuel cycle, reactor operation,
 * fusion, radiation safety and weapons. {@link #ENTRIES} also feeds the titles and descriptions
 * in {@link LangEntries}, so both stay in step.
 */
public class NuclearAdvancementProvider extends AdvancementProvider {

    public NuclearAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                      ExistingFileHelper existingFiles) {
        super(output, registries, existingFiles, List.of(NuclearAdvancementProvider::generate));
    }

    /** One advancement: where it hangs, what it shows and what earns it, with its English and Spanish text. */
    public record Entry(String id, String parent, Supplier<? extends ItemLike> icon, AdvancementType type, boolean hidden,
                        Supplier<Map<String, Criterion<?>>> criteria, boolean anyOf,
                        String title, String titleEs, String description, String descriptionEs) {
        public String titleKey() {
            return "advancement." + Create_NuclearIndustry.MODID + "." + id + ".title";
        }

        public String descriptionKey() {
            return "advancement." + Create_NuclearIndustry.MODID + "." + id + ".description";
        }
    }

    private static Supplier<Map<String, Criterion<?>>> has(Supplier<? extends ItemLike> item) {
        return () -> Map.of("has", InventoryChangeTrigger.TriggerInstance.hasItems(item.get()));
    }

    private static Supplier<Map<String, Criterion<?>>> event(NuclearEvent event) {
        return () -> Map.of(event.id(), NuclearEventTrigger.TriggerInstance.of(event));
    }

    private static Entry task(String id, String parent, Supplier<? extends ItemLike> icon, Supplier<Map<String, Criterion<?>>> criteria,
                              String title, String titleEs, String description, String descriptionEs) {
        return new Entry(id, parent, icon, AdvancementType.TASK, false, criteria, false, title, titleEs, description, descriptionEs);
    }

    private static Entry with(Entry entry, AdvancementType type, boolean hidden) {
        return new Entry(entry.id(), entry.parent(), entry.icon(), type, hidden, entry.criteria(), entry.anyOf(),
                entry.title(), entry.titleEs(), entry.description(), entry.descriptionEs());
    }

    public static final List<Entry> ENTRIES = List.of(
            task("root", null, AllNuclearItems.RAW_URANIUM, has(AllNuclearItems.RAW_URANIUM),
                    "Create: Nuclear Industry", "Create: Nuclear Industry",
                    "Dig up raw uranium", "Extrae uranio en bruto"),
            // ---- fuel cycle
            task("yellowcake", "root", AllNuclearItems.YELLOWCAKE, has(AllNuclearItems.YELLOWCAKE),
                    "Not a Dessert", "No es un postre",
                    "Leach uranium concentrate into yellowcake", "Lixivia concentrado de uranio hasta obtener torta amarilla"),
            task("reactor_fuel", "yellowcake", AllNuclearItems.URANIUM_REACTOR_FUEL, has(AllNuclearItems.URANIUM_REACTOR_FUEL),
                    "Fuel Assembly", "Elemento combustible",
                    "Assemble enriched uranium fuel", "Monta combustible de uranio enriquecido"),
            task("plutonium", "reactor_fuel", AllNuclearItems.PLUTONIUM, has(AllNuclearItems.PLUTONIUM),
                    "Bred, Not Mined", "Criado, no extraído",
                    "Reprocess spent fuel into plutonium", "Reprocesa combustible gastado para obtener plutonio"),
            task("mox", "plutonium", AllNuclearItems.MOX_REACTOR_FUEL, has(AllNuclearItems.MOX_REACTOR_FUEL),
                    "Closing the Cycle", "Cerrando el ciclo",
                    "Assemble MOX fuel from plutonium and depleted uranium", "Monta combustible MOX con plutonio y uranio empobrecido"),
            // ---- reactor operation
            task("reactor_formed", "reactor_fuel", AllNuclearItems.REACTOR_CONTROLLER, event(NuclearEvent.REACTOR_FORMED),
                    "Containment", "Contención",
                    "Form a fission reactor", "Forma un reactor de fisión"),
            task("reactor_running", "reactor_formed", AllNuclearItems.URANIUM_ROD, event(NuclearEvent.REACTOR_RUNNING),
                    "Criticality", "Criticidad",
                    "Start a chain reaction", "Inicia una reacción en cadena"),
            task("moderated_core", "reactor_formed", AllNuclearItems.GRAPHITE_MODERATOR, event(NuclearEvent.MODERATED_CORE),
                    "Slow Down, Split More", "Más despacio, más fisiones",
                    "Form a reactor with graphite or beryllium in its core", "Forma un reactor con grafito o berilio en su núcleo"),
            task("scram", "reactor_running", AllNuclearItems.CONTROL_ROD, event(NuclearEvent.SCRAM),
                    "Big Red Button", "El gran botón rojo",
                    "SCRAM a reactor", "Haz SCRAM a un reactor"),
            with(task("iodine_pit", "scram", AllNuclearItems.REACTOR_TEMPERATURE_SENSOR, event(NuclearEvent.IODINE_PIT),
                    "The Iodine Pit", "El pozo de yodo",
                    "Shut a hard-working reactor down and watch xenon hold it back",
                    "Para un reactor que trabajaba a fondo y mira cómo el xenón lo frena"), AdvancementType.GOAL, false),
            task("control_room", "reactor_running", AllNuclearItems.CONTROL_MONITOR, event(NuclearEvent.CONTROL_ROOM_LINKED),
                    "Control Room", "Sala de control",
                    "Link a control room device to a machine", "Enlaza un dispositivo de sala de control a una máquina"),
            task("turbine", "reactor_running", AllNuclearItems.TURBINE_OUTPUT, event(NuclearEvent.TURBINE_FORMED),
                    "Spin Cycle", "Centrifugado",
                    "Form a turbine", "Forma una turbina"),
            task("cooling_tower", "turbine", AllNuclearItems.COOLING_TOWER_CONTROLLER, event(NuclearEvent.COOLING_TOWER_FORMED),
                    "Iconic Silhouette", "Silueta icónica",
                    "Form a cooling tower", "Forma una torre de refrigeración"),
            with(task("meltdown", "reactor_running", AllNuclearItems.REACTOR_CASING, event(NuclearEvent.MELTDOWN),
                    "Not Great, Not Terrible", "Ni bien ni terrible",
                    "Witness a reactor meltdown", "Presencia la fusión de un reactor"), AdvancementType.CHALLENGE, true),
            // ---- fusion
            task("lithium_6", "root", AllNuclearItems.LITHIUM_6, has(AllNuclearItems.LITHIUM_6),
                    "Lighter Than Light", "Más ligero que la luz",
                    "Separate lithium-6", "Separa litio-6"),
            task("deuterium", "lithium_6", AllNuclearItems.DEUTERIUM_CELL, has(AllNuclearItems.DEUTERIUM_CELL),
                    "Heavy Hydrogen", "Hidrógeno pesado",
                    "Fill a cell with deuterium", "Llena una célula de deuterio"),
            with(task("tokamak_ignition", "deuterium", AllNuclearItems.FUSION_CONTROLLER, event(NuclearEvent.TOKAMAK_IGNITION),
                    "A Star in a Bottle", "Una estrella en una botella",
                    "Ignite the plasma of a tokamak", "Enciende el plasma de un tokamak"), AdvancementType.GOAL, false),
            // ---- radiation safety
            task("geiger", "root", AllNuclearItems.GEIGER_COUNTER, has(AllNuclearItems.GEIGER_COUNTER),
                    "Click, Click, Click", "Clic, clic, clic",
                    "Make a Geiger counter", "Fabrica un contador Geiger"),
            task("hazmat", "geiger", AllNuclearItems.HAZMAT_CHESTPLATE,
                    () -> Map.of("has", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.HAZMAT_HELMET.get(),
                            AllNuclearItems.HAZMAT_CHESTPLATE.get(), AllNuclearItems.HAZMAT_LEGGINGS.get(), AllNuclearItems.HAZMAT_BOOTS.get())),
                    "Suited Up", "Bien protegido",
                    "Have a full hazmat suit", "Ten un traje antirradiación completo"),
            task("radiation_sickness", "geiger", AllNuclearItems.RADIOACTIVE_WASTE, event(NuclearEvent.RADIATION_SICKNESS),
                    "That Glow Isn't Healthy", "Ese brillo no es sano",
                    "Come down with radiation sickness", "Sufre el síndrome de irradiación"),
            task("radiation_purge", "radiation_sickness", AllNuclearItems.PRUSSIAN_BLUE, event(NuclearEvent.RADIATION_PURGE),
                    "Flush It Out", "Fuera de mi cuerpo",
                    "Take Prussian Blue", "Toma Azul de Prusia"),
            // ---- weapons
            new Entry("weapons_core", "plutonium", AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE, AdvancementType.TASK, false,
                    () -> {
                        Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
                        criteria.put("uranium", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get()));
                        criteria.put("plutonium", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()));
                        return criteria;
                    }, true,
                    "Critical Mass", "Masa crítica",
                    "Make a weapons-grade core", "Fabrica un núcleo de grado militar"),
            with(task("nuclear_detonation", "weapons_core", AllNuclearItems.NUCLEAR_BOMB, event(NuclearEvent.NUCLEAR_DETONATION),
                    "Now I Am Become Death", "Ahora me he convertido en la muerte",
                    "Witness a nuclear detonation", "Presencia una detonación nuclear"), AdvancementType.CHALLENGE, false),
            task("dirty_detonation", "geiger", AllNuclearItems.DIRTY_BOMB, event(NuclearEvent.DIRTY_DETONATION),
                    "Spreading the Waste", "Repartiendo los residuos",
                    "Witness a dirty bomb go off", "Presencia la explosión de una bomba sucia")
    );

    private static void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFiles) {
        Map<String, AdvancementHolder> made = new HashMap<>();
        for (Entry entry : ENTRIES) {
            Advancement.Builder builder = Advancement.Builder.advancement();
            if (entry.parent() != null) {
                builder.parent(made.get(entry.parent()));
            }
            boolean root = entry.parent() == null;
            builder.display(new ItemStack(entry.icon().get()), Component.translatable(entry.titleKey()),
                    Component.translatable(entry.descriptionKey()),
                    root ? ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/block/reactor_casing.png") : null,
                    entry.type(), !root, entry.type() != AdvancementType.TASK, entry.hidden());
            entry.criteria().get().forEach(builder::addCriterion);
            if (entry.anyOf()) {
                builder.requirements(AdvancementRequirements.Strategy.OR);
            }
            made.put(entry.id(), builder.save(saver, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, entry.id()), existingFiles));
        }
    }
}
