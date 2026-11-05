package org.papiricoh.create_nuclearindustry.explosive;

import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalBlastPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The detonations of a level whose cloud or smoke still stands, so a player who logs in, comes through a portal or walks into
 * range while it does still sees it, at its true age: each payload is sent again to every player in range who has not had it.
 * Saved with the level, so a cloud outlives a restart. A mushroom cloud is kept {@link #NUCLEAR_TICKS}, a conventional blast's
 * smoke {@link #CONVENTIONAL_TICKS}.
 */
public final class ActiveDetonations extends SavedData {
    private static final String NAME = "create_nuclearindustry_active_detonations";
    /** How long a mushroom cloud stands, ticks. */
    public static final long NUCLEAR_TICKS = 6 * 60 * 20;
    /** How long a conventional blast's smoke hangs, ticks. */
    public static final long CONVENTIONAL_TICKS = 2 * 60 * 20;
    /** How long an antimatter flash or the like is worth telling a latecomer about, ticks. */
    public static final long FLASH_TICKS = 30 * 20;
    /** How often players are checked against the detonations, ticks. */
    private static final int CHECK_TICKS = 100;

    private final List<Entry> entries = new ArrayList<>();

    private static final class Entry {
        @Nullable
        final DetonationPayload nuclear;
        @Nullable
        final ConventionalBlastPayload conventional;
        /** Who has it already; not saved, so everyone is told again after a restart. */
        final Set<UUID> told = new HashSet<>();

        Entry(@Nullable DetonationPayload nuclear, @Nullable ConventionalBlastPayload conventional) {
            this.nuclear = nuclear;
            this.conventional = conventional;
        }

        Vec3 centre() {
            return nuclear != null ? nuclear.centre() : conventional.centre();
        }

        long startTick() {
            return nuclear != null ? nuclear.startTick() : conventional.startTick();
        }

        long lifetime() {
            if (nuclear == null) {
                return CONVENTIONAL_TICKS;
            }
            return nuclear.dirty() ? FLASH_TICKS : NUCLEAR_TICKS;
        }

        CustomPacketPayload payload() {
            return nuclear != null ? nuclear : conventional;
        }
    }

    public static ActiveDetonations get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ActiveDetonations::new, ActiveDetonations::load, null), NAME);
    }

    @Nullable
    public static ActiveDetonations getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(new Factory<>(ActiveDetonations::new, ActiveDetonations::load, null), NAME);
    }

    /** Detonations still standing in {@code level}. */
    public static int count(ServerLevel level) {
        ActiveDetonations data = getIfPresent(level);
        return data == null ? 0 : data.entries.size();
    }

    /** Sends {@code payload} to everyone in range now, and remembers it for whoever comes later. */
    public static void send(ServerLevel level, CustomPacketPayload payload) {
        Entry entry = payload instanceof DetonationPayload nuclear ? new Entry(nuclear, null)
                : payload instanceof ConventionalBlastPayload conventional ? new Entry(null, conventional) : null;
        if (entry == null) {
            return;
        }
        ActiveDetonations data = get(level);
        data.entries.add(entry);
        data.setDirty();
        data.tell(level, entry);
    }

    private void tell(ServerLevel level, Entry entry) {
        double range = ExplosiveConfig.broadcastRange();
        for (ServerPlayer player : level.players()) {
            if (!entry.told.contains(player.getUUID()) && player.position().distanceToSqr(entry.centre()) <= range * range) {
                entry.told.add(player.getUUID());
                PacketDistributor.sendToPlayer(player, entry.payload());
            }
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % CHECK_TICKS != 0) {
            return;
        }
        ActiveDetonations data = getIfPresent(level);
        if (data == null || data.entries.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Entry> iterator = data.entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (now - entry.startTick() > entry.lifetime() || now < entry.startTick()) {
                iterator.remove();
                data.setDirty();
                continue;
            }
            data.tell(level, entry);
        }
    }

    /** A player arriving in a level (logging in, respawning, through a portal) has seen none of its clouds yet. */
    public static void onPlayerArrives(PlayerEvent event) {
        if (event instanceof PlayerEvent.PlayerLoggedOutEvent) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            ActiveDetonations data = getIfPresent(level);
            if (data != null) {
                for (Entry entry : data.entries) {
                    entry.told.remove(player.getUUID());
                }
                for (Entry entry : data.entries) {
                    data.tell(level, entry);
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
            try {
                CompoundTag item = new CompoundTag();
                if (entry.nuclear != null) {
                    DetonationPayload.STREAM_CODEC.encode(buf, entry.nuclear);
                    item.putString("Kind", "nuclear");
                } else {
                    ConventionalBlastPayload.STREAM_CODEC.encode(buf, entry.conventional);
                    item.putString("Kind", "conventional");
                }
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                item.putByteArray("Payload", bytes);
                list.add(item);
            } finally {
                buf.release();
            }
        }
        tag.put("Detonations", list);
        return tag;
    }

    private static ActiveDetonations load(CompoundTag tag, HolderLookup.Provider registries) {
        ActiveDetonations data = new ActiveDetonations();
        for (Tag element : tag.getList("Detonations", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) element;
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(item.getByteArray("Payload")),
                    net.minecraft.core.RegistryAccess.EMPTY);
            try {
                if ("nuclear".equals(item.getString("Kind"))) {
                    data.entries.add(new Entry(DetonationPayload.STREAM_CODEC.decode(buf), null));
                } else {
                    data.entries.add(new Entry(null, ConventionalBlastPayload.STREAM_CODEC.decode(buf)));
                }
            } catch (RuntimeException e) {
                // A payload saved by an older version: its cloud is lost, nothing else.
            } finally {
                buf.release();
            }
        }
        return data;
    }
}
