package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A radar's screen. The server sends the radar's picture once a second, in pages of {@link RadarPayload#PAGE} tracks, each
 * track's whole history only the first time; the client keeps each track's history growing from there. Nothing here loads
 * chunks or reads terrain.
 */
public class RadarMenu extends AbstractContainerMenu {
    final RadarBlockEntity radar;
    public final BlockPos pos;
    private final Player player;
    private long sentAt = Long.MIN_VALUE;
    private int sentStatus = -1;
    /** Tracks whose history this screen has been sent (server). */
    private final Set<UUID> known = new HashSet<>();

    /** What the screen shows (client): each track's latest snapshot with all the history heard so far. */
    private Map<UUID, RadarTrack.Snapshot> tracks = new LinkedHashMap<>();
    private Map<UUID, RadarTrack.Snapshot> incoming = new LinkedHashMap<>();
    /** When the picture last changed, game time (client). */
    public long sample;
    public int status;

    public RadarMenu(int id, Inventory inventory, RadarBlockEntity radar, BlockPos pos) {
        super(RadarRegistry.MENU.get(), id);
        this.radar = radar;
        this.pos = pos;
        this.player = inventory.player;
    }

    public static RadarMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new RadarMenu(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof RadarBlockEntity r ? r : null, pos);
    }

    public RadarBlockEntity radar() {
        return radar;
    }

    @Override public boolean stillValid(Player p) { return ServerPayloadGuard.stillValid(radar, p) && ServerPayloadGuard.check(p, pos); }

    @Override public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer server) || radar == null || !stillValid(player)) return;
        long now = player.level().getGameTime();
        int status = radar.status();
        if (sentAt != Long.MIN_VALUE && now - sentAt < 20 && status == sentStatus) return;
        sentAt = now;
        sentStatus = status;
        List<RadarTrack> tracks = status == 1 ? radar.tracks() : List.of();
        Set<UUID> present = new HashSet<>();
        List<RadarTrack.Snapshot> snapshots = new ArrayList<>(tracks.size());
        for (RadarTrack track : tracks) {
            present.add(track.id());
            snapshots.add(track.snapshot(known.add(track.id())));
        }
        known.retainAll(present);
        // Bounded pages keep any number of flights under the network's payload limit.
        int pages = Math.max(1, (snapshots.size() + RadarPayload.PAGE - 1) / RadarPayload.PAGE);
        for (int page = 0; page < pages; page++) {
            PacketDistributor.sendToPlayer(server, new RadarPayload(containerId, pos, now, status, page == 0, page == pages - 1,
                    snapshots.subList(page * RadarPayload.PAGE, Math.min(snapshots.size(), (page + 1) * RadarPayload.PAGE))));
        }
    }

    /** A page of the picture arrived (client). */
    void accept(RadarPayload payload) {
        if (payload.first()) incoming = new LinkedHashMap<>();
        for (RadarTrack.Snapshot snapshot : payload.tracks()) {
            RadarTrack.Snapshot old = tracks.get(snapshot.id());
            if (snapshot.history().size() <= 1 && old != null) {
                List<V3> history = new ArrayList<>(old.history());
                if (!snapshot.history().isEmpty()) history.addAll(snapshot.history());
                while (history.size() > RadarTrack.HISTORY) history.remove(0);
                snapshot = new RadarTrack.Snapshot(snapshot.id(), snapshot.designation(), snapshot.position(), snapshot.velocity(), snapshot.phase(),
                        snapshot.impact(), snapshot.along(), snapshot.across(), snapshot.heading(), snapshot.eta(), snapshot.firstSeen(),
                        snapshot.apogee(), List.copyOf(history));
            }
            incoming.put(snapshot.id(), snapshot);
        }
        status = payload.status();
        if (payload.last()) {
            tracks = incoming;
            incoming = new LinkedHashMap<>();
            sample = payload.sample();
        }
    }

    /** The tracks the screen shows now (client), soonest impact first. */
    public List<RadarTrack.Snapshot> tracks() {
        return new ArrayList<>(tracks.values());
    }
}
