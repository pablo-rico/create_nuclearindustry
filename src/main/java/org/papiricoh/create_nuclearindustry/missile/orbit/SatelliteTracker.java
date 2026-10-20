package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every satellite in orbit over a level, and which foreign networks know of each: a satellite is always known to its own code,
 * and to another once a radar on that code has seen it pass ({@link #catalogue}). Ticked once a second: low orbits decay and burn
 * up, radars catalogue what flies over them, and clients get the orbits to fly on their own ({@link OrbitSyncPayload}).
 */
public final class SatelliteTracker extends SavedData {
    public static final String NAME = "create_nuclearindustry_satellites";

    /** Why a satellite left orbit. */
    public enum Removal { DEORBITED, DECAYED, DESTROYED }

    private final Map<UUID, Satellite> satellites = new LinkedHashMap<>();
    /** Foreign codes that know each satellite; its own code is never stored here. */
    private final Map<UUID, Set<String>> catalogued = new LinkedHashMap<>();
    private int nextNumber = 1;

    public static SatelliteTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(SatelliteTracker::new, SatelliteTracker::load, null), NAME);
    }

    /** The level's tracker if it ever had one, else null; never creates it. */
    @Nullable
    public static SatelliteTracker getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(SatelliteTracker::new, SatelliteTracker::load, null), NAME);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SatelliteTracker tracker = getIfPresent(level);
            if (tracker != null && level.getGameTime() % 20 == 0) tracker.tick(level);
        }
    }

    public Collection<Satellite> all() {
        return Collections.unmodifiableCollection(satellites.values());
    }

    @Nullable
    public Satellite get(UUID id) {
        return satellites.get(id);
    }

    /** The satellites on network {@code code}. */
    public List<Satellite> ofCode(String code) {
        return satellites.values().stream().filter(s -> s.code().equals(code)).toList();
    }

    /** What network {@code code} knows of: its own satellites, then the foreign ones it has catalogued. */
    public List<Satellite> knownTo(String code) {
        List<Satellite> own = new ArrayList<>(ofCode(code));
        for (Satellite s : satellites.values()) if (!s.code().equals(code) && known(s.id(), code)) own.add(s);
        return own;
    }

    /** Whether network {@code code} knows of satellite {@code id}. */
    public boolean known(UUID id, String code) {
        Satellite s = satellites.get(id);
        return s != null && (s.code().equals(code) || catalogued.getOrDefault(id, Set.of()).contains(code));
    }

    /** Network {@code code} has seen satellite {@code id}; true if that is news. */
    public boolean catalogue(UUID id, String code) {
        Satellite s = satellites.get(id);
        if (s == null || s.code().equals(code)) return false;
        boolean added = catalogued.computeIfAbsent(id, k -> new HashSet<>()).add(code);
        if (added) setDirty();
        return added;
    }

    /** A short name for the next satellite of {@code kind}: {@code EW-1}, {@code CM-2}... */
    public String designate(SatelliteKind kind) {
        String prefix = kind == SatelliteKind.EARLY_WARNING ? "EW-" : "CM-";
        setDirty();
        return prefix + nextNumber++;
    }

    /** Puts {@code satellite} in orbit. */
    public void add(ServerLevel level, Satellite satellite) {
        satellites.put(satellite.id(), satellite);
        setDirty();
        sync(level);
    }

    /** Takes satellite {@code id} out of orbit for {@code why}; false if there was none. */
    public boolean remove(ServerLevel level, UUID id, Removal why) {
        Satellite gone = satellites.remove(id);
        catalogued.remove(id);
        if (gone == null) return false;
        setDirty();
        sync(level);
        return true;
    }

    /** Replaces a satellite's orbit, as decay does. */
    void update(Satellite satellite) {
        if (satellites.containsKey(satellite.id())) {
            satellites.put(satellite.id(), satellite);
            setDirty();
        }
    }

    /** Once a second: decay, cataloguing and the clients' copy. */
    public void tick(ServerLevel level) {
        // Filled in by the orbit stream.
    }

    /** Sends every player in {@code level} the satellites over it. */
    public void sync(ServerLevel level) {
        OrbitSyncPayload.send(level, List.copyOf(satellites.values()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Satellite s : satellites.values()) {
            CompoundTag entry = s.save();
            ListTag codes = new ListTag();
            catalogued.getOrDefault(s.id(), Set.of()).forEach(code -> codes.add(StringTag.valueOf(code)));
            entry.put("Catalogued", codes);
            list.add(entry);
        }
        tag.put("Satellites", list);
        tag.putInt("NextNumber", nextNumber);
        return tag;
    }

    /** Public for tests: a tracker as it would load from {@code tag}. */
    public static SatelliteTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        SatelliteTracker tracker = new SatelliteTracker();
        for (Tag t : tag.getList("Satellites", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            Satellite s = Satellite.load(entry);
            tracker.satellites.put(s.id(), s);
            Set<String> codes = new HashSet<>();
            for (Tag code : entry.getList("Catalogued", Tag.TAG_STRING)) codes.add(code.getAsString());
            if (!codes.isEmpty()) tracker.catalogued.put(s.id(), codes);
        }
        tracker.nextNumber = Math.max(1, tag.getInt("NextNumber"));
        return tracker;
    }
}
