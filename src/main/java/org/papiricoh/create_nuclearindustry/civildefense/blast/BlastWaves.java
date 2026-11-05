package org.papiricoh.create_nuclearindustry.civildefense.blast;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The blast waves still travelling through a level ({@link BlastWaveManager}), saved with it. */
public final class BlastWaves extends SavedData {
    private static final String NAME = "create_nuclearindustry_blast_waves";

    private final List<BlastWaveManager.Wave> waves = new ArrayList<>();

    public static BlastWaves get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(BlastWaves::new, BlastWaves::load, null), NAME);
    }

    /** The level's waves if it has ever had any, else null; never creates the data. */
    @Nullable
    public static BlastWaves getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(BlastWaves::new, BlastWaves::load, null), NAME);
    }

    public List<BlastWaveManager.Wave> waves() {
        return waves;
    }

    void add(BlastWaveManager.Wave wave) {
        waves.add(wave);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (BlastWaveManager.Wave wave : waves) {
            list.add(wave.save());
        }
        tag.put("Waves", list);
        return tag;
    }

    /** The waves saved in {@code tag}, as a level loading them gets them; for tests. */
    public static BlastWaves fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        return load(tag, registries);
    }

    private static BlastWaves load(CompoundTag tag, HolderLookup.Provider registries) {
        BlastWaves data = new BlastWaves();
        for (Tag element : tag.getList("Waves", Tag.TAG_COMPOUND)) {
            data.waves.add(BlastWaveManager.Wave.load((CompoundTag) element));
        }
        return data;
    }
}
