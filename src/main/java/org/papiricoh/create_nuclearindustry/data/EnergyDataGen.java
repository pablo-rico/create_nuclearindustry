package org.papiricoh.create_nuclearindustry.data;

import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;

/** Lang and models for the shared Forge Energy blocks: the Creative RF Generator (no recipe, on purpose). */
final class EnergyDataGen extends AreaDataGen {
    static final EnergyDataGen INSTANCE = new EnergyDataGen();

    private EnergyDataGen() {}

    @Override
    void lang() {
        LangEntries.block("creative_rf_generator", "Creative RF Generator", "Generador de RF creativo");
        String k = "create_nuclearindustry.creative_rf_generator.";
        LangEntries.add(k + "rate", "Output per side", "Salida por cara");
        LangEntries.add(k + "row.fine", "x1,000 FE/t", "x1.000 FE/t");
        LangEntries.add(k + "row.coarse", "x10,000 FE/t", "x10.000 FE/t");
        LangEntries.add(k + "goggles.rate", "Offers %s FE/t to every side", "Ofrece %s FE/t a cada cara");
        LangEntries.add(k + "goggles.hint", "Endless: creative only", "Inagotable: solo en creativo");
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        ModelFile model = provider.models().getExistingFile(provider.modLoc("block/creative_rf_generator"));
        provider.simpleBlock(EnergyRegistry.CREATIVE_RF_GENERATOR.get(), model);
        provider.simpleBlockItem(EnergyRegistry.CREATIVE_RF_GENERATOR.get(), model);
    }
}
