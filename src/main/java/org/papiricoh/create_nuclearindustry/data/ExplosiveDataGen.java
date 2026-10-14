package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;

import static org.papiricoh.create_nuclearindustry.data.LangEntries.add;
import static org.papiricoh.create_nuclearindustry.data.LangEntries.block;

/**
 * Lang, models, tags and data maps for what a nuclear detonation leaves (trinitite, charred logs), its
 * flash's damage type, the target designator's airburst and the cannon shell's cores.
 */
final class ExplosiveDataGen extends AreaDataGen {
    static final ExplosiveDataGen INSTANCE = new ExplosiveDataGen();

    private ExplosiveDataGen() {}

    @Override
    void lang() {
        block("trinitite", "Trinitite", "Trinitita");
        block("charred_log", "Charred Log", "Tronco carbonizado");
        add("death.attack.create_nuclearindustry.nuclear_flash", "%1$s was burnt by a nuclear flash",
                "%1$s ardió con el destello de una explosión nuclear");
        add("death.attack.create_nuclearindustry.nuclear_flash.player", "%1$s was burnt by a nuclear flash while fighting %2$s",
                "%1$s ardió con el destello de una explosión nuclear mientras luchaba contra %2$s");
        String designator = "item.create_nuclearindustry.target_designator.";
        add(designator + "ground_burst", "Ground burst: goes off on the target", "Explosión en superficie: estalla sobre el objetivo");
        add(designator + "airburst", "Airburst: goes off above the target", "Explosión aérea: estalla por encima del objetivo");
        add(designator + "mode_hint", "Sneak-use in the air to switch", "Úsalo agachado al aire para cambiar");
        add("item.create_nuclearindustry.nuclear_cannon_shell.cores", "Warhead: %s cores, reaching %s blocks",
                "Cabeza: %s núcleos, alcance de %s bloques");
        add(PlantJeiInfo.PREFIX + "nuclear_aftermath",
                "What a nuclear crater leaves. Trinitite is the sand and rock of the crater's floor fused into green glass by the fireball, among basalt, obsidian and, at the very centre, magma; it is faintly radioactive. Charred Logs are what the flash leaves of trees within its reach.",
                "Lo que deja un cráter nuclear. La trinitita es la arena y la roca del fondo del cráter fundidas en vidrio verde por la bola de fuego, entre basalto, obsidiana y, en el mismo centro, magma; es ligeramente radiactiva. Los Troncos carbonizados son lo que deja el destello de los árboles a su alcance.");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        provider.simpleBlockWithItem(ExplosiveRegistry.TRINITITE.get(), provider.models().cubeAll("trinitite", provider.modLoc("block/trinitite")));
        provider.axisBlock(ExplosiveRegistry.CHARRED_LOG.get(), provider.modLoc("block/charred_log"), provider.modLoc("block/charred_log_top"));
        provider.simpleBlockItem(ExplosiveRegistry.CHARRED_LOG.get(), new ModelFile.UncheckedModelFile(provider.modLoc("block/charred_log")));
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(ExplosiveRegistry.TRINITITE.get());
        provider.tagFor(BlockTags.MINEABLE_WITH_AXE).add(ExplosiveRegistry.CHARRED_LOG.get());
    }

    @Override
    void dataMaps(NuclearDataMapProvider provider) {
        // Fused with the fission products of the fireball: barely, but measurably, active.
        NuclearDataMapProvider.add(provider.builderFor(AllNuclearRadiation.RADIOACTIVITY), ExplosiveRegistry.TRINITITE_ITEM, 0.03f, false);
    }
}
