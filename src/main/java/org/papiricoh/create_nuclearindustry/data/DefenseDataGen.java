package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Datagen of missile defence: the interceptor battery ({@link BatteryData}), the launch controller's kill vehicle
 * ({@link InterceptorSiloData}) and their lang, one class per piece so each can grow on its own.
 */
final class DefenseDataGen extends AreaDataGen {
    static final DefenseDataGen INSTANCE = new DefenseDataGen();

    private DefenseDataGen() {
    }

    @Override
    void lang() {
        DefenseLang.add();
        DefenseCoreLang.add();
        BatteryLang.add();
        InterceptorSiloLang.add();
        HoloDefenseLang.add();
        DefenseFxLang.add();
    }

    @Override
    void blockStates(NuclearBlockStateProvider provider) {
        BatteryData.blockStates(provider);
        InterceptorSiloData.blockStates(provider);
    }

    @Override
    void itemModels(NuclearItemModelProvider provider) {
        BatteryData.itemModels(provider);
        InterceptorSiloData.itemModels(provider);
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        BatteryData.blockTags(provider);
        InterceptorSiloData.blockTags(provider);
    }

    @Override
    void itemTags(NuclearItemTagsProvider provider, HolderLookup.Provider lookup) {
        BatteryData.itemTags(provider);
    }

    /** A battery part has no item of its own: breaking one drops nothing, the master drops the battery. */
    @Override
    void blockLoot(NuclearBlockLoot loot) {
        loot.addTable(DefenseRegistry.BATTERY_PART.get(), LootTable.lootTable());
        BatteryData.blockLoot(loot);
    }

    @Override
    void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                      ExistingFileHelper existingFiles) {
        BatteryData.addProviders(event, generator, output, lookup);
        InterceptorSiloData.addProviders(event, generator, output, lookup);
    }
}
