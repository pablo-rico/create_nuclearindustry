package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEventTrigger;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The progression tree, in six tabs: the fuel cycle from the first raw uranium to thorium, fission
 * and the pressurized water plant, waste, civil defense (with radiation safety), fusion and
 * weapons. Every tab has its own root and background; ids from before the tabs are kept, only their
 * parents moved, so players keep their progress. {@link #ENTRIES} also feeds the titles and
 * descriptions in {@link LangEntries}, so both stay in step.
 */
public class NuclearAdvancementProvider extends AdvancementProvider {
    public static final String FUEL_CYCLE = "root";
    public static final String FISSION = "fission";
    public static final String WASTE = "waste";
    public static final String CIVIL_DEFENSE = "civil_defense";
    public static final String FUSION = "fusion";
    public static final String WEAPONS = "weapons";
    public static final String ACCELERATOR = "accelerator";

    /** The background of each tab, by the id of its root. */
    public static final Map<String, ResourceLocation> BACKGROUNDS = Map.of(
            FUEL_CYCLE, ResourceLocation.withDefaultNamespace("textures/block/deepslate.png"),
            FISSION, texture("reactor_casing"),
            WASTE, texture("bentonite"),
            CIVIL_DEFENSE, texture("blast_door_frame"),
            FUSION, texture("fusion_cryostat_casing"),
            WEAPONS, texture("launch_pad_base"),
            ACCELERATOR, texture("beam_quadrupole_side"));

    public NuclearAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                      ExistingFileHelper existingFiles) {
        super(output, registries, existingFiles, List.of(NuclearAdvancementProvider::generate));
    }

    /**
     * One advancement: its tab (the id of the tab's root), where it hangs, what it shows and what
     * earns it, with its English and Spanish text.
     */
    public record Entry(String id, String parent, Supplier<? extends ItemLike> icon, AdvancementType type, boolean hidden,
                        Supplier<Map<String, Criterion<?>>> criteria, boolean anyOf,
                        String title, String titleEs, String description, String descriptionEs, String tab) {
        public String titleKey() {
            return "advancement." + Create_NuclearIndustry.MODID + "." + id + ".title";
        }

        public String descriptionKey() {
            return "advancement." + Create_NuclearIndustry.MODID + "." + id + ".description";
        }

        public boolean isRoot() {
            return parent == null;
        }

        Entry withTab(String tab) {
            return new Entry(id, parent, icon, type, hidden, criteria, anyOf, title, titleEs, description, descriptionEs, tab);
        }
    }

    private static ResourceLocation texture(String block) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/block/" + block + ".png");
    }

    private static Supplier<Map<String, Criterion<?>>> has(Supplier<? extends ItemLike> item) {
        return () -> Map.of("has", InventoryChangeTrigger.TriggerInstance.hasItems(item.get()));
    }

    /** One criterion per item, named after it; earned by any of them when the entry is {@code anyOf}. */
    @SafeVarargs
    private static Supplier<Map<String, Criterion<?>>> hasEach(Supplier<? extends ItemLike>... items) {
        return () -> {
            Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
            for (Supplier<? extends ItemLike> item : items) {
                criteria.put(BuiltInRegistries.ITEM.getKey(item.get().asItem()).getPath(),
                        InventoryChangeTrigger.TriggerInstance.hasItems(item.get()));
            }
            return criteria;
        };
    }

    private static Supplier<Map<String, Criterion<?>>> event(NuclearEvent event) {
        return () -> Map.of(event.id(), NuclearEventTrigger.TriggerInstance.of(event));
    }

    private static Entry task(String id, String parent, Supplier<? extends ItemLike> icon, Supplier<Map<String, Criterion<?>>> criteria,
                              String title, String titleEs, String description, String descriptionEs) {
        return new Entry(id, parent, icon, AdvancementType.TASK, false, criteria, false, title, titleEs, description, descriptionEs, "");
    }

    /** Earned by any one of its criteria. */
    private static Entry any(Entry entry) {
        return new Entry(entry.id(), entry.parent(), entry.icon(), entry.type(), entry.hidden(), entry.criteria(), true,
                entry.title(), entry.titleEs(), entry.description(), entry.descriptionEs(), entry.tab());
    }

    private static Entry with(Entry entry, AdvancementType type, boolean hidden) {
        return new Entry(entry.id(), entry.parent(), entry.icon(), type, hidden, entry.criteria(), entry.anyOf(),
                entry.title(), entry.titleEs(), entry.description(), entry.descriptionEs(), entry.tab());
    }

    /** A secret challenge: hidden until earned. */
    private static Entry secret(Entry entry) {
        return with(entry, AdvancementType.CHALLENGE, true);
    }

    private static List<Entry> tab(String tab, Entry... entries) {
        List<Entry> stamped = new ArrayList<>();
        for (Entry entry : entries) {
            stamped.add(entry.withTab(tab));
        }
        return stamped;
    }

    @SafeVarargs
    private static List<Entry> tabs(List<Entry>... tabs) {
        List<Entry> all = new ArrayList<>();
        for (List<Entry> tab : tabs) {
            all.addAll(tab);
        }
        return List.copyOf(all);
    }

    public static final List<Entry> ENTRIES = tabs(
            tab(FUEL_CYCLE,
                    task("root", null, AllNuclearItems.RAW_URANIUM, has(AllNuclearItems.RAW_URANIUM),
                            "Create: Nuclear Industry", "Create: Nuclear Industry",
                            "Dig up raw uranium", "Extrae uranio en bruto"),
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
                    any(task("fines", "root", FuelCycleRegistry.GRANITE_FINES,
                            hasEach(FuelCycleRegistry.GRANITE_FINES, FuelCycleRegistry.TUFF_FINES, FuelCycleRegistry.DEEPSLATE_FINES),
                            "Rock Flour", "Harina de roca",
                            "Grind granite, tuff or deepslate into fines", "Muele granito, toba o pizarra profunda hasta obtener finos")),
                    task("sulfuric_acid", "fines", AllNuclearFluids.SULFURIC_ACID_BUCKET, has(AllNuclearFluids.SULFURIC_ACID_BUCKET),
                            "Handle With Care", "Manéjese con cuidado",
                            "Fill a bucket with sulfuric acid", "Llena un cubo de ácido sulfúrico"),
                    task("heavy_mineral_sand", "fines", FuelCycleRegistry.HEAVY_MINERAL_SAND, has(FuelCycleRegistry.HEAVY_MINERAL_SAND),
                            "Black Sand", "Arena negra",
                            "Get heavy mineral sand", "Consigue arena de minerales pesados"),
                    task("thorium_dioxide", "heavy_mineral_sand", FuelCycleRegistry.THORIUM_DIOXIDE, has(FuelCycleRegistry.THORIUM_DIOXIDE),
                            "Element 90", "Elemento 90",
                            "Extract thorium dioxide", "Extrae dióxido de torio"),
                    task("uranium_233", "thorium_dioxide", FuelCycleRegistry.URANIUM_233_DIOXIDE, has(FuelCycleRegistry.URANIUM_233_DIOXIDE),
                            "Bred From Thorium", "Criado a partir del torio",
                            "Turn irradiated thorium into uranium-233", "Convierte torio irradiado en uranio-233"),
                    task("thorium_fuel", "uranium_233", FuelCycleRegistry.THORIUM_REACTOR_FUEL, has(FuelCycleRegistry.THORIUM_REACTOR_FUEL),
                            "The Other Fuel", "El otro combustible",
                            "Assemble thorium reactor fuel", "Monta combustible de torio para reactor"),
                    secret(task("thorium_cycle", "thorium_fuel", FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL,
                            has(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL),
                            "The Whole Thorium Cycle", "El ciclo del torio completo",
                            "Burn thorium fuel in a reactor until it is spent", "Quema combustible de torio en un reactor hasta gastarlo"))),
            tab(FISSION,
                    task("fission", null, AllNuclearItems.REACTOR_CONTROLLER, has(AllNuclearItems.REACTOR_CASING),
                            "Nuclear Fission", "Fisión nuclear",
                            "Make Reactor Casing", "Fabrica carcasa de reactor"),
                    task("reactor_formed", "fission", AllNuclearItems.REACTOR_CONTROLLER, event(NuclearEvent.REACTOR_FORMED),
                            "Containment", "Contención",
                            "Form a fission reactor", "Forma un reactor de fisión"),
                    task("reactor_viewport", "reactor_formed", ReactorVisualsRegistry.REACTOR_VIEWPORT_ITEM,
                            has(ReactorVisualsRegistry.REACTOR_VIEWPORT_ITEM),
                            "A Window Into the Core", "Una ventana al núcleo",
                            "Make a Reactor Viewport to watch the fuel channels", "Fabrica una mirilla del reactor para ver los canales de combustible"),
                    task("reactor_running", "reactor_formed", AllNuclearItems.URANIUM_ROD, event(NuclearEvent.REACTOR_RUNNING),
                            "Criticality", "Criticidad",
                            "Start a chain reaction", "Inicia una reacción en cadena"),
                    task("moderated_core", "reactor_formed", AllNuclearItems.GRAPHITE_MODERATOR, event(NuclearEvent.MODERATED_CORE),
                            "Slow Down, Split More", "Más despacio, más fisiones",
                            "Form a reactor with graphite or beryllium in its core", "Forma un reactor con grafito o berilio en su núcleo"),
                    task("full_power", "reactor_running", AllNuclearItems.REACTOR_TEMPERATURE_SENSOR, event(NuclearEvent.REACTOR_FULL_POWER),
                            "Full Power", "A plena potencia",
                            "Run a fission reactor at full power", "Haz funcionar un reactor de fisión a plena potencia"),
                    task("scram", "reactor_running", AllNuclearItems.CONTROL_ROD, event(NuclearEvent.SCRAM),
                            "Big Red Button", "El gran botón rojo",
                            "SCRAM a reactor", "Haz SCRAM a un reactor"),
                    task("cold_shutdown", "scram", () -> Items.PACKED_ICE, event(NuclearEvent.COLD_SHUTDOWN),
                            "Cold Shutdown", "Parada en frío",
                            "Bring a reactor down to a cold shutdown", "Lleva un reactor a parada en frío"),
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
                    task("steam_generator", "reactor_running", PwrRegistry.STEAM_GENERATOR_CONTROLLER_ITEM,
                            event(NuclearEvent.STEAM_GENERATOR_FORMED),
                            "Two Loops", "Dos circuitos",
                            "Form a steam generator", "Forma un generador de vapor"),
                    task("relief_valve", "steam_generator", PwrRegistry.RELIEF_VALVE_ITEM, event(NuclearEvent.RELIEF_VALVE_LIFTED),
                            "Letting Off Steam", "Soltando vapor",
                            "Have a relief valve lift", "Haz que salte una válvula de alivio"),
                    secret(task("containment_held", "steam_generator", PwrRegistry.CONTAINMENT_MONITOR_ITEM,
                            event(NuclearEvent.CONTAINMENT_HELD),
                            "It Held", "Ha aguantado",
                            "Keep a loss of coolant inside a sealed containment past half its rated pressure",
                            "Retén una pérdida de refrigerante en una contención estanca por encima de la mitad de su presión nominal")),
                    with(task("meltdown", "reactor_running", AllNuclearItems.REACTOR_CASING, event(NuclearEvent.MELTDOWN),
                            "Not Great, Not Terrible", "Ni bien ni terrible",
                            "Witness a reactor meltdown", "Presencia la fusión de un reactor"), AdvancementType.CHALLENGE, true),
                    secret(task("corium_caught", "meltdown", PwrRegistry.CORE_CATCHER_ITEM, event(NuclearEvent.CORIUM_CAUGHT),
                            "Caught It", "Atrapado",
                            "Let a core catcher freeze the corium of a melted core",
                            "Deja que un recogedor del núcleo solidifique el corium de un núcleo fundido"))),
            tab(WASTE,
                    task("waste", null, AllNuclearItems.RADIOACTIVE_WASTE_BARREL, has(AllNuclearItems.RADIOACTIVE_WASTE),
                            "Nuclear Waste", "Residuos nucleares",
                            "Get your hands on radioactive waste", "Hazte con residuos radiactivos"),
                    task("dry_cask", "waste", SpentFuelRegistry.DRY_CASK_ITEM, has(SpentFuelRegistry.DRY_CASK_ITEM),
                            "Dry Storage", "Almacenamiento en seco",
                            "Make a Dry Cask for cooled spent fuel", "Fabrica un contenedor de almacenamiento en seco para el combustible gastado ya frío"),
                    task("vitrified", "waste", WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM, has(WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM),
                            "Locked in Glass", "Encerrado en vidrio",
                            "Vitrify high-level waste into a canister", "Vitrifica residuos de alta actividad en un contenedor"),
                    secret(task("repository_sealed", "vitrified", WasteRegistry.REPOSITORY_INSPECTOR_ITEM,
                            event(NuclearEvent.REPOSITORY_SEALED),
                            "Sealed for Ten Thousand Years", "Sellado por diez mil años",
                            "Have a Repository Inspector find eight or more waste packages, every one of them stored for good",
                            "Haz que un inspector de repositorio encuentre ocho o más bultos de residuos, todos almacenados para siempre"))),
            tab(CIVIL_DEFENSE,
                    any(task("civil_defense", null, CivilDefenseRegistry.AIR_RAID_SIREN_ITEM,
                            hasEach(AllNuclearItems.GEIGER_COUNTER, AllNuclearItems.HAZMAT_HELMET, CivilDefenseRegistry.AIR_RAID_SIREN_ITEM,
                                    CivilDefenseRegistry.HAND_CRANK_SIREN_ITEM, CivilDefenseRegistry.SEISMIC_DETECTOR_ITEM,
                                    CivilDefenseRegistry.BLAST_DOOR_CONTROLLER_ITEM, CivilDefenseRegistry.SHELTER_FILTRATION_UNIT_ITEM),
                            "Civil Defense", "Defensa civil",
                            "Make something that protects people from radiation or a blast",
                            "Fabrica algo que proteja a la gente de la radiación o de una explosión")),
                    task("geiger", "civil_defense", AllNuclearItems.GEIGER_COUNTER, has(AllNuclearItems.GEIGER_COUNTER),
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
                    task("siren", "civil_defense", CivilDefenseRegistry.HAND_CRANK_SIREN_ITEM, event(NuclearEvent.SIREN_SOUNDED),
                            "Air Raid Warning", "Alarma antiaérea",
                            "Sound a siren", "Haz sonar una sirena"),
                    task("seismic", "siren", CivilDefenseRegistry.SEISMIC_DETECTOR_ITEM, event(NuclearEvent.SEISMIC_DETECTED),
                            "The Ground Shook", "Tembló el suelo",
                            "Have a Seismic Detector pick up a detonation", "Haz que un detector sísmico registre una detonación"),
                    task("blast_door", "civil_defense", CivilDefenseRegistry.STEEL_BLAST_DOOR_ITEM, event(NuclearEvent.BLAST_DOOR_SHUT),
                            "Shut Tight", "Bien cerrada",
                            "Shut a blast door", "Cierra una puerta blindada"),
                    task("shelter", "blast_door", CivilDefenseRegistry.SHELTER_FILTRATION_UNIT_ITEM,
                            has(CivilDefenseRegistry.SHELTER_FILTRATION_UNIT_ITEM),
                            "Down to the Bunker", "Al búnker",
                            "Make a Shelter Filtration Unit", "Fabrica una unidad de filtración de refugio"),
                    secret(task("sheltered_from_blast", "shelter", CivilDefenseRegistry.BLAST_DOOR_FRAME_ITEM,
                            event(NuclearEvent.SHELTERED_FROM_BLAST),
                            "Duck and Cover", "Agáchate y cúbrete",
                            "Ride out a blast wave unharmed inside a certified shelter",
                            "Aguanta ileso una onda expansiva dentro de un refugio certificado"))),
            tab(FUSION,
                    any(task("fusion", null, AllNuclearItems.FUSION_CONTROLLER,
                            hasEach(AllNuclearItems.LITHIUM, AllNuclearItems.DEUTERIUM_CELL, AllNuclearItems.SUPERCONDUCTOR_INGOT,
                                    AllNuclearItems.FUSION_CONTROLLER),
                            "Nuclear Fusion", "Fusión nuclear",
                            "Make lithium, a deuterium cell, a superconductor or a Tokamak Controller",
                            "Fabrica litio, una célula de deuterio, un superconductor o un controlador del tokamak")),
                    task("lithium_6", "fusion", AllNuclearItems.LITHIUM_6, has(AllNuclearItems.LITHIUM_6),
                            "Lighter Than Light", "Más ligero que la luz",
                            "Separate lithium-6", "Separa litio-6"),
                    task("deuterium", "fusion", AllNuclearItems.DEUTERIUM_CELL, has(AllNuclearItems.DEUTERIUM_CELL),
                            "Heavy Hydrogen", "Hidrógeno pesado",
                            "Fill a cell with deuterium", "Llena una célula de deuterio"),
                    task("tokamak_formed", "fusion", AllNuclearItems.FUSION_CENTRAL_SOLENOID, event(NuclearEvent.TOKAMAK_FORMED),
                            "Magnetic Bottle", "Botella magnética",
                            "Form a tokamak", "Forma un tokamak"),
                    task("cryoplant", "tokamak_formed", CryoRegistry.CRYOPLANT_CONTROLLER_ITEM, event(NuclearEvent.CRYOPLANT_FORMED),
                            "Colder Than Space", "Más frío que el espacio",
                            "Form a cryogenic plant", "Forma una planta criogénica"),
                    with(task("tokamak_ignition", "tokamak_formed", AllNuclearItems.FUSION_CONTROLLER, event(NuclearEvent.TOKAMAK_IGNITION),
                            "A Star in a Bottle", "Una estrella en una botella",
                            "Ignite the plasma of a tokamak", "Enciende el plasma de un tokamak"), AdvancementType.GOAL, false),
                    task("plasma_turbine", "tokamak_ignition", AllNuclearItems.FUSION_PLASMA_TURBINE, has(AllNuclearItems.FUSION_PLASMA_TURBINE),
                            "Steam From a Star", "Vapor de una estrella",
                            "Make a Plasma Turbine", "Fabrica una turbina de plasma"),
                    secret(task("fusion_breakeven", "tokamak_ignition", AllNuclearItems.DT_FUEL_PELLET, event(NuclearEvent.FUSION_BREAKEVEN),
                            "Q Greater Than One", "Q mayor que uno",
                            "Get more fusion power out of a burning plasma than its heaters put in",
                            "Saca de un plasma en combustión más potencia de fusión de la que meten sus calentadores")),
                    with(task("disruption", "tokamak_ignition", AllNuclearItems.FUSION_ELECTROMAGNET, event(NuclearEvent.DISRUPTION),
                            "Disruption", "Disrupción",
                            "Lose a burning plasma to a disruption", "Pierde un plasma en combustión por una disrupción"),
                            AdvancementType.GOAL, true)),
            tab(WEAPONS,
                    any(task("weapons", null, AllNuclearItems.NUCLEAR_BOMB,
                            hasEach(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE, AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE,
                                    AllNuclearItems.NUCLEAR_BOMB, AllNuclearItems.DIRTY_BOMB, AllNuclearItems.MISSILE,
                                    AllNuclearItems.LAUNCH_PAD, AllNuclearItems.TARGET_DESIGNATOR, ExplosiveRegistry.FIRING_SET,
                                    ExplosiveRegistry.EXPLOSIVE_LENS),
                            "Weapons of Mass Destruction", "Armas de destrucción masiva",
                            "Make a bomb, a missile or what goes into one", "Fabrica una bomba, un misil o lo que va dentro")),
                    new Entry("weapons_core", "weapons", AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE, AdvancementType.TASK, false,
                            () -> {
                                Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
                                criteria.put("uranium", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get()));
                                criteria.put("plutonium", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get()));
                                return criteria;
                            }, true,
                            "Critical Mass", "Masa crítica",
                            "Make a weapons-grade core", "Fabrica un núcleo de grado militar", ""),
                    with(task("nuclear_detonation", "weapons_core", AllNuclearItems.NUCLEAR_BOMB, event(NuclearEvent.NUCLEAR_DETONATION),
                            "Now I Am Become Death", "Ahora me he convertido en la muerte",
                            "Witness a nuclear detonation", "Presencia una detonación nuclear"), AdvancementType.CHALLENGE, false),
                    task("teller_ulam", "weapons_core", ExplosiveRegistry.LITHIUM_DEUTERIDE, has(ExplosiveRegistry.LITHIUM_DEUTERIDE),
                            "Teller-Ulam", "Teller-Ulam",
                            "Make lithium-6 deuteride, the fuel of a thermonuclear secondary",
                            "Fabrica deuteruro de litio-6, el combustible de un secundario termonuclear"),
                    with(task("thermonuclear_detonation", "nuclear_detonation", ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM,
                            event(NuclearEvent.THERMONUCLEAR_DETONATION),
                            "Brighter Than a Thousand Suns", "Más brillante que mil soles",
                            "Witness a thermonuclear detonation", "Presencia una detonación termonuclear"), AdvancementType.CHALLENGE, false),
                    secret(task("nuclear_shell_fired", "weapons_core", () -> Items.FIRE_CHARGE, event(NuclearEvent.NUCLEAR_SHELL_FIRED),
                            "Artillery of Last Resort", "Artillería de último recurso",
                            "See a nuclear cannon shell go off", "Presencia la explosión de un proyectil nuclear de cañón")),
                    task("missile_launched", "weapons", AllNuclearItems.MISSILE, event(NuclearEvent.MISSILE_LAUNCHED),
                            "Liftoff", "Despegue",
                            "Launch a missile from a launch pad", "Lanza un misil desde una plataforma de lanzamiento"),
                    task("dirty_detonation", "weapons", AllNuclearItems.DIRTY_BOMB, event(NuclearEvent.DIRTY_DETONATION),
                            "Spreading the Waste", "Repartiendo los residuos",
                            "Witness a dirty bomb go off", "Presencia la explosión de una bomba sucia")),
            tab(ACCELERATOR,
                    task("accelerator", null, AcceleratorRegistry.CONTROLLER_ITEM, has(AcceleratorRegistry.CONTROLLER_ITEM),
                            "Particle Physics", "Física de partículas",
                            "Make a Particle Accelerator Controller", "Fabrica un controlador de acelerador de partículas"),
                    task("accelerator_formed", "accelerator", AcceleratorRegistry.ION_SOURCE_ITEM, event(NuclearEvent.ACCELERATOR_FORMED),
                            "Line of Sight", "En línea recta",
                            "Form a particle accelerator: an ion source, a beamline and a dump or target at its end",
                            "Forma un acelerador de partículas: una fuente de iones, una línea de haz y un absorbedor o un blanco al final"),
                    task("first_beam", "accelerator_formed", AcceleratorRegistry.RF_CAVITY_ITEM, event(NuclearEvent.FIRST_BEAM),
                            "First Beam", "Primer haz",
                            "Send a beam down the line", "Haz llegar un haz al final de la línea"),
                    task("synchrotron", "first_beam", AcceleratorRegistry.BENDING_MAGNET_ITEM, event(NuclearEvent.RING_FORMED),
                            "Going in Circles", "Dando vueltas",
                            "Feed a synchrotron ring from your linac", "Alimenta un anillo sincrotrón con tu linac"),
                    with(task("gev_beam", "synchrotron", AcceleratorRegistry.QUADRUPOLE_ITEM, event(NuclearEvent.GEV_BEAM),
                            "Giga Electron Volts", "Gigaelectronvoltios",
                            "Accelerate a beam to 1 GeV or more", "Acelera un haz a 1 GeV o más"), AdvancementType.GOAL, false))
    );

    /** The tab roots, in the order the tabs appear. */
    public static List<Entry> roots() {
        return ENTRIES.stream().filter(Entry::isRoot).toList();
    }

    private static void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFiles) {
        Map<String, AdvancementHolder> made = new HashMap<>();
        for (Entry entry : ENTRIES) {
            Advancement.Builder builder = Advancement.Builder.advancement();
            if (entry.parent() != null) {
                builder.parent(made.get(entry.parent()));
            }
            boolean root = entry.isRoot();
            builder.display(new ItemStack(entry.icon().get()), Component.translatable(entry.titleKey()),
                    Component.translatable(entry.descriptionKey()),
                    root ? BACKGROUNDS.get(entry.id()) : null,
                    entry.type(), !root, entry.type() != AdvancementType.TASK, entry.hidden());
            entry.criteria().get().forEach(builder::addCriterion);
            if (entry.anyOf()) {
                builder.requirements(AdvancementRequirements.Strategy.OR);
            }
            made.put(entry.id(), builder.save(saver, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, entry.id()), existingFiles));
        }
    }
}
