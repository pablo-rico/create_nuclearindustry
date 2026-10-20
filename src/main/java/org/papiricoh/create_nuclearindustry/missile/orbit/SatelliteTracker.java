package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxBroadcaster;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarModel;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarNetwork;

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
    private int seconds;
    private boolean unsavedDecay;

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

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncPlayer(player);
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncPlayer(player);
    }

    private static void syncPlayer(ServerPlayer player) {
        SatelliteTracker tracker = getIfPresent(player.serverLevel());
        OrbitSyncPayload.send(player, tracker == null ? List.of() : List.copyOf(tracker.all()));
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

    /** How many satellites belong to network {@code code}. */
    public int count(String code) {
        return (int) satellites.values().stream().filter(s -> s.code().equals(code)).count();
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
            unsavedDecay = true;
        }
    }

    /** Once a second: decay, cataloguing and the clients' copy. */
    public void tick(ServerLevel level) {
        seconds++;
        long now = level.getGameTime();
        double decay = OrbitConfig.leoDecayPerDay * 20 / 24000;
        for (Satellite satellite : List.copyOf(satellites.values())) {
            Orbit orbit = satellite.orbit();
            if (orbit.geostationary()) continue;
            Satellite lower = satellite.withOrbit(orbit.withAltitude(orbit.altitude() - decay));
            if (lower.orbit().altitude() < OrbitConfig.leoMinAltitude) {
                remove(level, satellite.id(), Removal.DECAYED);
                FlightFxBroadcaster.event(level, satellite.id(), FlightFxEvent.SATELLITE_REENTRY, 0,
                        Flight.vec(lower.orbit().position(now)), Flight.vec(lower.orbit().velocity(now)));
            } else if (decay > 0) update(lower);
        }
        for (var radar : RadarNetwork.radars(level)) {
            if (!radar.operational() || !LinkCode.complete(radar.code())) continue;
            double reach = radar.reach();
            reach = reach <= 0 ? Double.POSITIVE_INFINITY
                    : reach * Math.pow(RadarModel.signature(MissileFlight.Phase.MIDCOURSE), 0.25);
            for (Satellite satellite : satellites.values()) {
                if (!known(satellite.id(), radar.code()) && RadarModel.detects(radar.antenna(), satellite.orbit().position(now),
                        reach, MissileConfig.radarEarthRadius)) catalogue(satellite.id(), radar.code());
            }
        }
        if (unsavedDecay && seconds % 30 == 0) {
            setDirty();
            unsavedDecay = false;
        }
        if (seconds % Math.max(1, OrbitConfig.syncSeconds) == 0) sync(level);
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
