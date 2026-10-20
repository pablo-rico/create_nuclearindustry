package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloPart;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTableBlock;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

import java.util.List;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.item;

/**
 * Lang and tags for the modular missiles: the silo and the rocket assembled in it, the radar that tracks flights and the
 * command table that shows them. Each piece's vocabulary lives in its own lang class ({@link SiloLang}, {@link RadarLang}).
 */
final class MissileDataGen extends AreaDataGen {
    static final MissileDataGen INSTANCE = new MissileDataGen();

    private MissileDataGen() {}

    @Override
    void lang() {
        RadarLang.add();
        SiloLang.add();
        block("rocket_engine", "Rocket Engine", "Motor de cohete");
        block("rocket_tank", "Rocket Propellant Tank", "Depósito de propelente");
        block("rocket_guidance", "Rocket Guidance Module", "Módulo de guiado");
        block("rocket_warhead", "Rocket Warhead", "Ojiva de cohete");
        block("silo_controller", "Silo Controller", "Controlador de silo");
        block("silo_wall", "Silo Wall", "Pared de silo");
        block("silo_fuel_port", "Silo Fuel Port", "Puerto de combustible de silo");
        LangEntries.fluid("rocket_fuel", "Rocket Fuel", "Combustible de cohete");
        LangEntries.fluid("liquid_oxygen", "Liquid Oxygen", "Oxígeno líquido");
        item("propellant_cartridge", "Propellant Cartridge", "Cartucho de propelente");
        item("star_tracker", "Star Tracker", "Rastreador estelar");
        block("radar_scope", "Radar Scope", "Pantalla de radar");
        HoloLang.add();
    }

    /** The command table's pieces: a whole table alone, and once formed a corner, an edge or the centre turned to its place. */
    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        net.neoforged.neoforge.client.model.generators.ModelFile single = provider.models().getExistingFile(provider.modLoc("block/holo_table"));
        net.neoforged.neoforge.client.model.generators.ModelFile corner = provider.models().getExistingFile(provider.modLoc("block/holo_table_corner"));
        net.neoforged.neoforge.client.model.generators.ModelFile edge = provider.models().getExistingFile(provider.modLoc("block/holo_table_edge"));
        net.neoforged.neoforge.client.model.generators.ModelFile centre = provider.models().getExistingFile(provider.modLoc("block/holo_table_center"));
        provider.getVariantBuilder(HoloRegistry.TABLE.get()).forAllStates(state -> {
            HoloPart part = state.getValue(HoloTableBlock.PART);
            var builder = net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder();
            return switch (part) {
                case SINGLE -> builder.modelFile(single).build();
                case CENTER -> builder.modelFile(centre).build();
                case NW -> builder.modelFile(corner).build();
                case NE -> builder.modelFile(corner).rotationY(90).build();
                case SE -> builder.modelFile(corner).rotationY(180).build();
                case SW -> builder.modelFile(corner).rotationY(270).build();
                case N -> builder.modelFile(edge).build();
                case E -> builder.modelFile(edge).rotationY(90).build();
                case S -> builder.modelFile(edge).rotationY(180).build();
                case W -> builder.modelFile(edge).rotationY(270).build();
            };
        });
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(SiloRegistry.STAR_TRACKER.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var pickaxe = provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE);
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        pickaxe.add(RadarRegistry.RADAR.get(), RadarRegistry.SCOPE.get(), org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry.TABLE.get());
        for (var block : List.of(SiloRegistry.ENGINE, SiloRegistry.TANK, SiloRegistry.GUIDANCE, SiloRegistry.WARHEAD,
                SiloRegistry.CONTROLLER, SiloRegistry.WALL, SiloRegistry.FUEL_PORT)) {
            pickaxe.add(block.get());
            parts.add(block.get());
        }
        provider.tagFor(SiloRegistry.SILO_WALLS).add(SiloRegistry.WALL.get(), SiloRegistry.FUEL_PORT.get());
    }
}
