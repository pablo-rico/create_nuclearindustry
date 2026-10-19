package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
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
        item("propellant_cartridge", "Propellant Cartridge", "Cartucho de propelente");
        item("star_tracker", "Star Tracker", "Rastreador estelar");
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(SiloRegistry.STAR_TRACKER.get());
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        var pickaxe = provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE);
        var parts = provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS);
        pickaxe.add(RadarRegistry.RADAR.get());
        for (var block : List.of(SiloRegistry.ENGINE, SiloRegistry.TANK, SiloRegistry.GUIDANCE, SiloRegistry.WARHEAD,
                SiloRegistry.CONTROLLER, SiloRegistry.WALL)) {
            pickaxe.add(block.get());
            parts.add(block.get());
        }
    }
}
