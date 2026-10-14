package org.papiricoh.create_nuclearindustry.radiation.contamination;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.papiricoh.create_nuclearindustry.explosive.blast.FalloutPlume;

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
     * to nothing at {@code radius} blocks, as it was at {@code gameTime}. A wind blowing towards
     * {@code wind} radians stretches it into a plume by {@code stretch} (0 keeps a circle; see
     * {@link FalloutPlume}).
     */
    public record FalloutEvent(int id, int x, int z, int radius, float intensity, long gameTime, float wind, float stretch) {
        public FalloutEvent(int id, int x, int z, int radius, float intensity, long gameTime) {
            this(id, x, z, radius, intensity, gameTime, 0, 0);
        }

        /** Contamination this event leaves on the chunk whose centre is at ({@code cx}, {@code cz}). */
        public float at(double cx, double cz) {
            return (float) (intensity * FalloutPlume.share(cx - x, cz - z, radius, wind, stretch));
        }

        /** The furthest from its centre it reaches. */
        public double reach() {
            return FalloutPlume.reach(radius, stretch);
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

    FalloutEvent addEvent(int x, int z, int radius, float intensity, long gameTime, float wind, float stretch) {
        FalloutEvent event = new FalloutEvent(nextEventId++, x, z, radius, intensity, gameTime, wind, stretch);
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
            if (event.stretch() > 0) {
                entry.putFloat("wind", event.wind());
                entry.putFloat("stretch", event.stretch());
            }
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
                    entry.getInt("radius"), entry.getFloat("intensity"), entry.getLong("time"), entry.getFloat("wind"),
                    entry.getFloat("stretch")));
        }
        data.nextEventId = Math.max(1, tag.getInt("nextEvent"));
        data.clearEpoch = tag.getInt("clearEpoch");
        return data;
    }
}
