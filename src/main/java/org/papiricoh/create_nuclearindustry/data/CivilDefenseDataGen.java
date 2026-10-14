package org.papiricoh.create_nuclearindustry.data;

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
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.Tags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.data.recipe.CivilDefenseRecipes;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationTransmission;

import java.util.concurrent.CompletableFuture;

/** Lang, models, tags, data maps, loot and recipes for sirens, the seismic detector, blast doors, the blast wave and shelters. */
final class CivilDefenseDataGen extends AreaDataGen {
    static final CivilDefenseDataGen INSTANCE = new CivilDefenseDataGen();
    /** Create's tag for blocks contraptions must leave alone; a door moved away from its controller would break. */
    private static final TagKey<Block> NON_MOVABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "non_movable"));

    private CivilDefenseDataGen() {}

    @Override
    void lang() {
        LangEntries.block("air_raid_siren", "Air Raid Siren", "Sirena antiaérea");
        LangEntries.block("hand_crank_siren", "Hand-Crank Siren", "Sirena de manivela");
        LangEntries.block("seismic_detector", "Seismic Detector", "Detector sísmico");
        LangEntries.block("blast_door_frame", "Blast Door Frame", "Marco de puerta blindada");
        LangEntries.block("steel_blast_door", "Steel Blast Door", "Puerta blindada de acero");
        LangEntries.block("lead_blast_door", "Lead Blast Door", "Puerta blindada de plomo");
        LangEntries.block("blast_door_controller", "Blast Door Controller", "Controlador de puerta blindada");
        LangEntries.add("subtitle.create_nuclearindustry.air_raid_siren", "Siren wails", "Sirena aúlla");
        LangEntries.add("create_nuclearindustry.display_source.seismic_detector", "Latest Detonation", "Última detonación");

        LangEntries.add("create_nuclearindustry.siren.silent", "Silent", "En silencio");
        LangEntries.add("create_nuclearindustry.siren.alert", "Sounding: alert", "Sonando: alerta");
        LangEntries.add("create_nuclearindustry.siren.attack", "Sounding: attack warning", "Sonando: aviso de ataque");
        LangEntries.add("create_nuclearindustry.siren.impact_in", "Impact in %s s", "Impacto en %s s");
        LangEntries.add("create_nuclearindustry.siren.hazard", "Hazard under way nearby", "Peligro en curso cerca");
        LangEntries.add("create_nuclearindustry.siren.reach", "Listens for threats %s blocks around",
                "Vigila amenazas a %s bloques");
        LangEntries.add("create_nuclearindustry.siren.needs_speed", "Silent: turn it at %s RPM or more",
                "En silencio: gírala a %s RPM o más");

        String[][] compass = {{"n", "N", "N"}, {"ne", "NE", "NE"}, {"e", "E", "E"}, {"se", "SE", "SE"},
                {"s", "S", "S"}, {"sw", "SW", "SO"}, {"w", "W", "O"}, {"nw", "NW", "NO"}};
        for (String[] point : compass) {
            LangEntries.add("create_nuclearindustry.compass." + point[0], point[1], point[2]);
        }
        LangEntries.add("create_nuclearindustry.seismic.event", "Power %s, %s m %s", "Potencia %s, %s m %s");
        LangEntries.add("create_nuclearindustry.seismic.quiet", "No detonations felt", "Sin detonaciones");
        LangEntries.add("create_nuclearindustry.seismic.ago", "%s s ago", "hace %s s");
        LangEntries.add("create_nuclearindustry.seismic.dirty_ago", "Dirty bomb, %s s ago", "Bomba sucia, hace %s s");
        LangEntries.add("create_nuclearindustry.seismic.more", "%s more: sneak to see them", "%s más: agáchate para verlas");

        LangEntries.add("create_nuclearindustry.blast_door.no_door", "No door", "Sin puerta");
        LangEntries.add("create_nuclearindustry.blast_door.no_door_hint",
                "Frame a 2x2 to 4x4 opening filled with blast door leaves",
                "Rodea de marco un hueco de 2x2 a 4x4 lleno de hojas de puerta blindada");
        LangEntries.add("create_nuclearindustry.blast_door.size", "Door %sx%s", "Puerta de %sx%s");
        LangEntries.add("create_nuclearindustry.blast_door.opening", "Opening: %s%%", "Abriendo: %s%%");
        LangEntries.add("create_nuclearindustry.blast_door.closing", "Closing: %s%%", "Cerrando: %s%%");
        LangEntries.add("create_nuclearindustry.blast_door.shut", "Shut", "Cerrada");
        LangEntries.add("create_nuclearindustry.blast_door.open", "Open", "Abierta");
        LangEntries.add("create_nuclearindustry.blast_door.stopped", "Stopped at %s%% open", "Detenida al %s%% abierta");

        LangEntries.block("shelter_filtration_unit", "Shelter Filtration Unit", "Unidad de filtración de refugio");
        LangEntries.add("death.attack.create_nuclearindustry.blast_wave", "%1$s was flattened by a blast wave",
                "%1$s fue aplastado por una onda expansiva");
        LangEntries.add("death.attack.create_nuclearindustry.blast_wave.player", "%1$s was flattened by a blast wave while fighting %2$s",
                "%1$s fue aplastado por una onda expansiva mientras luchaba contra %2$s");
        LangEntries.add("create_nuclearindustry.shelter.scanning", "Surveying the room...", "Inspeccionando la sala...");
        LangEntries.add("create_nuclearindustry.shelter.no_room", "No air next to it: place it inside the shelter",
                "Sin aire al lado: colócala dentro del refugio");
        LangEntries.add("create_nuclearindustry.shelter.open", "Not sealed: the air gets out",
                "Sin sellar: el aire se escapa");
        LangEntries.add("create_nuclearindustry.shelter.too_big", "Not sealed: open, or over %s blocks of air",
                "Sin sellar: abierta o con más de %s bloques de aire");
        LangEntries.add("create_nuclearindustry.shelter.weak_wall", "Weak wall at %s: blast resistance %s, needs %s",
                "Pared débil en %s: resistencia %s, necesita %s");
        LangEntries.add("create_nuclearindustry.shelter.leaky_wall", "Wall at %s lets %s%% of radiation through, at most %s%%",
                "La pared en %s deja pasar el %s%% de la radiación, máximo %s%%");
        LangEntries.add("create_nuclearindustry.shelter.certified", "Certified shelter: %s blocks of air",
                "Refugio certificado: %s bloques de aire");
        LangEntries.add("create_nuclearindustry.shelter.filtering", "Filtering: %s%% of fallout and ground dose gets in",
                "Filtrando: entra el %s%% de la dosis de lluvia radiactiva y del suelo");
        LangEntries.add("create_nuclearindustry.shelter.not_filtering", "Not filtering: turn it at %s RPM or more",
                "Sin filtrar: gírala a %s RPM o más");
        LangEntries.add("create_nuclearindustry.shelter.blast", "Blast waves inside: %s%% of the overpressure",
                "Ondas expansivas dentro: %s%% de la sobrepresión");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        sirens(provider);
        ModelFile seismic = provider.models().cubeBottomTop("seismic_detector", provider.modLoc("block/seismic_detector_side"),
                provider.modLoc("block/blast_door_frame"), provider.modLoc("block/seismic_detector_top"));
        provider.simpleBlockWithItem(CivilDefenseRegistry.SEISMIC_DETECTOR.get(), seismic);

        provider.simpleBlockWithItem(CivilDefenseRegistry.BLAST_DOOR_FRAME.get(),
                provider.models().cubeAll("blast_door_frame", provider.modLoc("block/blast_door_frame")));
        leaf(provider, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), "steel_blast_door");
        leaf(provider, CivilDefenseRegistry.LEAD_BLAST_DOOR.get(), "lead_blast_door");

        // The shaft faces are north and south; the X-axis variant turns them east and west.
        ResourceLocation side = provider.modLoc("block/blast_door_controller");
        ResourceLocation shaft = provider.modLoc("block/blast_door_controller_shaft");
        ModelFile controller = provider.models().cube("blast_door_controller", side, side, shaft, shaft, side, side)
                .texture("particle", side);
        provider.getVariantBuilder(CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(controller)
                .rotationY(state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) == Direction.Axis.X ? 90 : 0)
                .build());
        provider.simpleBlockItem(CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get(), controller);

        // Shaft sockets on the ends, filter grilles around; lying down for the horizontal axes.
        ModelFile unit = provider.models().cubeColumn("shelter_filtration_unit", provider.modLoc("block/shelter_filtration_unit_side"),
                provider.modLoc("block/shelter_filtration_unit_end"));
        provider.getVariantBuilder(CivilDefenseRegistry.SHELTER_FILTRATION_UNIT.get()).forAllStates(state -> {
            Direction.Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
            return ConfiguredModel.builder().modelFile(unit)
                    .rotationX(axis == Direction.Axis.Y ? 0 : 90)
                    .rotationY(axis == Direction.Axis.X ? 90 : 0)
                    .build();
        });
        provider.simpleBlockItem(CivilDefenseRegistry.SHELTER_FILTRATION_UNIT.get(), unit);
    }

    /** A leaf looks the same in every state; moving and open leaves are not drawn at all. */
    private static void leaf(NuclearBlockStateProvider provider, Block block, String name) {
        ModelFile model = provider.models().cubeAll(name, provider.modLoc("block/" + name));
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
        provider.simpleBlockItem(block, model);
    }

    /** Pole (or gearbox for the hand-crank one), slotted rotor housing, rain cap and finial. */
    private static void sirens(NuclearBlockStateProvider provider) {
        ModelFile electric = siren(provider, "air_raid_siren", "block/air_raid_siren_base", 6, 0, 6, 10, 4, 10);
        ModelFile crank = siren(provider, "hand_crank_siren", "block/hand_crank_siren_base", 3, 0, 3, 13, 4, 13);
        for (Block block : new Block[]{CivilDefenseRegistry.AIR_RAID_SIREN.get(), CivilDefenseRegistry.HAND_CRANK_SIREN.get()}) {
            ModelFile model = block == CivilDefenseRegistry.AIR_RAID_SIREN.get() ? electric : crank;
            provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
            provider.simpleBlockItem(block, model);
        }
    }

    private static ModelFile siren(NuclearBlockStateProvider provider, String name, String base,
                                   float bx0, float by0, float bz0, float bx1, float by1, float bz1) {
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/air_raid_siren_housing"))
                .texture("base", provider.modLoc(base))
                .texture("housing", provider.modLoc("block/air_raid_siren_housing"))
                .texture("cap", provider.modLoc("block/air_raid_siren_cap"));
        model.element().from(bx0, by0, bz0).to(bx1, by1, bz1)
                .allFaces((direction, face) -> face.texture("#base").cullface(direction == Direction.DOWN ? Direction.DOWN : null)).end();
        model.element().from(3, 4, 3).to(13, 12, 13)
                .allFaces((direction, face) -> face.texture(direction.getAxis() == Direction.Axis.Y ? "#cap" : "#housing")).end();
        model.element().from(2, 12, 2).to(14, 14, 14).allFaces((direction, face) -> face.texture("#cap")).end();
        model.element().from(6, 14, 6).to(10, 16, 10)
                .allFaces((direction, face) -> face.texture("#cap").cullface(direction == Direction.UP ? Direction.UP : null)).end();
        return model;
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        Block[] door = {CivilDefenseRegistry.BLAST_DOOR_FRAME.get(), CivilDefenseRegistry.STEEL_BLAST_DOOR.get(),
                CivilDefenseRegistry.LEAD_BLAST_DOOR.get(), CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get()};
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(door);
        provider.tagFor(NON_MOVABLE).add(door);
        provider.tagFor(BlockTags.NEEDS_IRON_TOOL).add(door);
        provider.tagFor(CivilDefenseRegistry.BLAST_FRAGILE).addTags(BlockTags.LEAVES, Tags.Blocks.GLASS_BLOCKS, Tags.Blocks.GLASS_PANES);
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        var transmission = provider.builderFor(AllNuclearRadiation.TRANSMISSION);
        transmission.add(CivilDefenseRegistry.LEAD_BLAST_DOOR.get().builtInRegistryHolder(), new RadiationTransmission(0.02f), false);
        transmission.add(CivilDefenseRegistry.STEEL_BLAST_DOOR.get().builtInRegistryHolder(), new RadiationTransmission(0.15f), false);
        transmission.add(CivilDefenseRegistry.BLAST_DOOR_FRAME.get().builtInRegistryHolder(), new RadiationTransmission(0.15f), false);
        transmission.add(CivilDefenseRegistry.BLAST_DOOR_CONTROLLER.get().builtInRegistryHolder(), new RadiationTransmission(0.35f), false);
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output,
                      CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Renamed("Civil defense recipes", new CivilDefenseRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new DamageTypeTags(output, lookup, existingFiles));
    }

    /** The blast wave counts as an explosion, so Blast Protection softens it. */
    private static final class DamageTypeTags extends TagsProvider<DamageType> {
        private DamageTypeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
            super(output, Registries.DAMAGE_TYPE, lookup, Create_NuclearIndustry.MODID, existingFiles);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION).add(CivilDefenseRegistry.BLAST_WAVE_DAMAGE);
            // The flash of a nuclear fireball burns: Fire Protection helps, and what is immune to fire takes none.
            tag(net.minecraft.tags.DamageTypeTags.IS_FIRE).add(org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry.NUCLEAR_FLASH);
        }

        @Override
        public String getName() {
            return "Civil defense damage type tags";
        }
    }

    /** Data providers need unique names, and every vanilla recipe provider is called "Recipes" (final). */
    private record Renamed(String name, DataProvider provider) implements DataProvider {
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
