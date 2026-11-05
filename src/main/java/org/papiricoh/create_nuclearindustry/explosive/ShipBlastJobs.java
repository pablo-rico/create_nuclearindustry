package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The ships a level's detonations are still working through ({@link ShipBlastManager}), saved with it. */
public final class ShipBlastJobs extends SavedData {
    private static final String NAME = "create_nuclearindustry_ship_blast_jobs";

    private final List<ShipBlastManager.Job> jobs = new ArrayList<>();

    public static ShipBlastJobs get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ShipBlastJobs::new, ShipBlastJobs::load, null), NAME);
    }

    /** The level's jobs if it has ever had any, else null; never creates the data. */
    @Nullable
    public static ShipBlastJobs getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(ShipBlastJobs::new, ShipBlastJobs::load, null), NAME);
    }

    public List<ShipBlastManager.Job> jobs() {
        return jobs;
    }

    void add(ShipBlastManager.Job job) {
        jobs.add(job);
        setDirty();
    }

    /** For tests: forgets every queued job. */
    public void clear() {
        jobs.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ShipBlastManager.Job job : jobs) {
            list.add(job.save());
        }
        tag.put("Jobs", list);
        return tag;
    }

    private static ShipBlastJobs load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipBlastJobs data = new ShipBlastJobs();
        for (Tag element : tag.getList("Jobs", Tag.TAG_COMPOUND)) {
            ShipBlastManager.Job job = ShipBlastManager.Job.load((CompoundTag) element);
            if (job != null) {
                data.jobs.add(job);
            }
        }
        return data;
    }
}
