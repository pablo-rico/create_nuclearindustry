package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;
import java.util.List;

public class RadarMenu extends AbstractContainerMenu {
    final RadarBlockEntity radar;
    public final BlockPos pos;
    private final Player player;
    private long sentAt = Long.MIN_VALUE;
    public List<RadarContacts.Contact> contacts = List.of();
    public int status;
    public RadarMenu(int id, Inventory inventory, RadarBlockEntity radar, BlockPos pos) {
        super(RadarRegistry.MENU.get(), id); this.radar = radar; this.pos = pos; player = inventory.player;
    }
    public static RadarMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new RadarMenu(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof RadarBlockEntity r ? r : null, pos);
    }
    @Override public boolean stillValid(Player p) { return ServerPayloadGuard.stillValid(radar, p) && ServerPayloadGuard.check(p, pos); }
    @Override public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer server) || !stillValid(player)) return;
        long now = player.level().getGameTime();
        int nextStatus = !radar.stationary() ? 2 : radar.operational() ? 1 : 0;
        if (sentAt != Long.MIN_VALUE && now - sentAt < 20 && nextStatus == status) return;
        sentAt = now; status = nextStatus;
        List<RadarContacts.Contact> snapshot = nextStatus == 1 ? RadarContacts.get(server.serverLevel()) : List.of();
        // Bounded packets keep arbitrarily large flight lists below the network payload limit.
        int pages = Math.max(1, (snapshot.size() + 127) / 128);
        for (int page = 0; page < pages; page++) {
            PacketDistributor.sendToPlayer(server, new RadarPayload(containerId, pos, now, status, page == 0,
                    snapshot.subList(page * 128, Math.min(snapshot.size(), (page + 1) * 128))));
        }
    }
}
