package org.papiricoh.create_nuclearindustry.radiation.contamination;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * What unloaded chunks still have to catch up on when they load: the fallout events since
 * they were last seen, and the epoch of the last global clear. Loaded chunks get both at once,
 * so this never forces a chunk to load.
 */
public class ContaminationData extends SavedData {
    private static final String NAME = "create_nuclearindustry_contamination";
    /** Oldest events are dropped beyond this; chunks that never loaded since then miss them. */
    static final int MAX_EVENTS = 256;

    private final List<FalloutEvent> events = new ArrayList<>();
    private int nextEventId = 1;
    private int clearEpoch;

    /**
     * Fallout centred on a block column: {@code intensity} rad/s at the centre falling linearly
     * to nothing at {@code radius} blocks, as it was at {@code gameTime}.
     */
    public record FalloutEvent(int id, int x, int z, int radius, float intensity, long gameTime) {
        /** Contamination this event leaves on the chunk whose centre is at ({@code cx}, {@code cz}). */
        public float at(double cx, double cz) {
            double distance = Math.sqrt((cx - x) * (cx - x) + (cz - z) * (cz - z));
            return distance >= radius ? 0 : (float) (intensity * (1.0 - distance / radius));
        }
    }

    public static ContaminationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ContaminationData::new, ContaminationData::load, null), NAME);
    }

    public List<FalloutEvent> events() {
        return events;
    }

    public int clearEpoch() {
        return clearEpoch;
    }

    FalloutEvent addEvent(int x, int z, int radius, float intensity, long gameTime) {
        FalloutEvent event = new FalloutEvent(nextEventId++, x, z, radius, intensity, gameTime);
        events.add(event);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
        setDirty();
        return event;
    }

    /** Forgets every pending event and makes every chunk that loads from now on start clean. */
    int clearAll() {
        events.clear();
        clearEpoch++;
        setDirty();
        return clearEpoch;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FalloutEvent event : events) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("id", event.id());
            entry.putInt("x", event.x());
            entry.putInt("z", event.z());
            entry.putInt("radius", event.radius());
            entry.putFloat("intensity", event.intensity());
            entry.putLong("time", event.gameTime());
            list.add(entry);
        }
        tag.put("events", list);
        tag.putInt("nextEvent", nextEventId);
        tag.putInt("clearEpoch", clearEpoch);
        return tag;
    }

    private static ContaminationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ContaminationData data = new ContaminationData();
        for (Tag element : tag.getList("events", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            data.events.add(new FalloutEvent(entry.getInt("id"), entry.getInt("x"), entry.getInt("z"),
                    entry.getInt("radius"), entry.getFloat("intensity"), entry.getLong("time")));
        }
        data.nextEventId = Math.max(1, tag.getInt("nextEvent"));
        data.clearEpoch = tag.getInt("clearEpoch");
        return data;
    }
}
