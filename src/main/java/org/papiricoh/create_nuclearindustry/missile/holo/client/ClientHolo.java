package org.papiricoh.create_nuclearindustry.missile.holo.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloDetonationPayload;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTracksPayload;
import org.papiricoh.create_nuclearindustry.missile.radar.client.RadarSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The client's side of the command tables: the latest picture of each table it was sent, the detonations to flash, and a soft blip
 * when a new track appears on a table nearby.
 */
public final class ClientHolo {
    /** How long a detonation flashes on the tables, milliseconds. */
    public static final long FLASH_MILLIS = 6000;

    /** A detonation as the tables show it. */
    public record Flash(double x, double y, double z, int radius, long at) {
    }

    private static final Map<BlockPos, HoloTracksPayload> pictures = new HashMap<>();
    private static final Map<BlockPos, Set<UUID>> seen = new HashMap<>();
    private static final List<Flash> flashes = new ArrayList<>();
    @Nullable
    private static ClientLevel level;

    private ClientHolo() {
    }

    public static void init() {
        HoloRegistry.clientTracks = ClientHolo::accept;
        HoloRegistry.clientDetonation = ClientHolo::flash;
    }

    private static void checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            pictures.clear();
            seen.clear();
            flashes.clear();
            level = current;
        }
    }

    static void accept(HoloTracksPayload picture) {
        checkLevel();
        pictures.put(picture.centre(), picture);
        boolean first = !seen.containsKey(picture.centre());
        Set<UUID> known = seen.computeIfAbsent(picture.centre(), c -> new HashSet<>());
        boolean fresh = false;
        Set<UUID> now = new HashSet<>();
        for (HoloTracksPayload.Track track : picture.tracks()) {
            now.add(track.id());
            fresh |= !known.contains(track.id());
        }
        if (fresh && !first) RadarSounds.holoBlip();
        known.clear();
        known.addAll(now);
    }

    static void flash(HoloDetonationPayload payload) {
        checkLevel();
        flashes.add(new Flash(payload.x(), payload.y(), payload.z(), payload.radius(), Util.getMillis()));
    }

    /** The latest picture of the table at {@code centre}, or null. */
    @Nullable
    public static HoloTracksPayload picture(BlockPos centre) {
        checkLevel();
        return pictures.get(centre);
    }

    /** Detonations still flashing. */
    public static List<Flash> flashes() {
        checkLevel();
        long now = Util.getMillis();
        flashes.removeIf(f -> now - f.at() > FLASH_MILLIS);
        return flashes;
    }

    /** The view (blocks across) a table shows, as its board says. */
    public static int view(HoloTracksPayload picture) {
        return picture.board().view();
    }
}
