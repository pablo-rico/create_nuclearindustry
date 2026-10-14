package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * The detonations a level is still working through ({@link NuclearBlastManager}), saved with it so a
 * world closed half way through a crater finishes it when it opens again: each job's centre, its zones,
 * where its sweep got to and the chunks it is waiting to load.
 */
public final class BlastJobs extends SavedData {
    private static final String NAME = "create_nuclearindustry_blast_jobs";

    private final List<NuclearBlastManager.BlastJob> jobs = new ArrayList<>();
    private int nextId = 1;
    /** Whose turn it is to work first this tick. */
    private int turn;

    public static BlastJobs get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(BlastJobs::new, BlastJobs::load, null), NAME);
    }

    /** The level's jobs if it has ever had any saved or started, else null; never creates the data. */
    @Nullable
    public static BlastJobs getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(BlastJobs::new, BlastJobs::load, null), NAME);
    }

    public List<NuclearBlastManager.BlastJob> jobs() {
        return jobs;
    }

    int nextId() {
        return nextId++;
    }

    void add(NuclearBlastManager.BlastJob job) {
        jobs.add(job);
        setDirty();
    }

    void clear() {
        jobs.clear();
        setDirty();
    }

    /** The job to start with this tick, moving on by one each tick. */
    int turn(int count) {
        turn = (turn + 1) % Math.max(1, count);
        return turn;
    }

    void removeFinished() {
        if (jobs.removeIf(NuclearBlastManager.BlastJob::finished)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (NuclearBlastManager.BlastJob job : jobs) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Id", job.id);
            entry.putLong("Centre", job.centre.asLong());
            entry.putInt("GroundZero", job.groundZero);
            entry.putBoolean("PlainHole", job.plainHole);
            double[] profile = job.profile.encode();
            ListTag numbers = new ListTag();
            for (double value : profile) {
                numbers.add(net.minecraft.nbt.DoubleTag.valueOf(value));
            }
            entry.put("Profile", numbers);
            entry.putInt("Ring", job.ring);
            entry.putInt("Index", job.idx);
            entry.putInt("Fallen", job.fallen);
            entry.putInt("Fires", job.fires);
            entry.put("Deferred", new LongArrayTag(job.deferred.toLongArray()));
            list.add(entry);
        }
        tag.put("Jobs", list);
        tag.putInt("NextId", nextId);
        return tag;
    }

    /** The jobs saved in {@code tag}, as a level loading them gets them; for tests. */
    public static BlastJobs fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        return load(tag, registries);
    }

    private static BlastJobs load(CompoundTag tag, HolderLookup.Provider registries) {
        BlastJobs data = new BlastJobs();
        for (Tag element : tag.getList("Jobs", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            ListTag numbers = entry.getList("Profile", Tag.TAG_DOUBLE);
            double[] values = new double[numbers.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = numbers.getDouble(i);
            }
            DetonationProfile profile = DetonationProfile.decode(values);
            if (profile == null) {
                continue;
            }
            NuclearBlastManager.BlastJob job = new NuclearBlastManager.BlastJob(entry.getInt("Id"), BlockPos.of(entry.getLong("Centre")),
                    profile, entry.getInt("GroundZero"), entry.getBoolean("PlainHole"));
            job.ring = entry.getInt("Ring");
            job.idx = entry.getInt("Index");
            job.fallen = entry.getInt("Fallen");
            job.fires = entry.getInt("Fires");
            for (long chunk : entry.getLongArray("Deferred")) {
                job.deferred.add(chunk);
                // Chunks that load with the level fire their load events before this is read: look again.
                job.loaded.add(chunk);
            }
            data.jobs.add(job);
        }
        data.nextId = Math.max(1, tag.getInt("NextId"));
        return data;
    }
}
