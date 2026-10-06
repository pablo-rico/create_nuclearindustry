package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Block states and models for blocks added by the rewrite; older blocks keep hand-written ones. */
public class NuclearBlockStateProvider extends BlockStateProvider {

    public NuclearBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Create_NuclearIndustry.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(AllNuclearBlocks.SPODUMENE_ORE.get(), cubeAll(AllNuclearBlocks.SPODUMENE_ORE.get()));
    }
}
