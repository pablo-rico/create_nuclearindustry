package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * Setup screen of one control module. It holds no items of its own: the player inventory is there
 * to pick up frequency items, and every change goes to the server as a {@link ConfigureModulePayload}.
 * It opens with a {@link ConsoleSummary} of the console, so the screen can offer what works on the
 * linked machine; while it is open the console's first desk keeps resending its sample (see
 * {@link ControlDeskBlockEntity#addViewer}).
 */
public class ControlModuleMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 48;
    public static final int INVENTORY_Y = 158;

    private final BlockPos deskPos;
    private final int slot;
    private final Player player;
    private final ConsoleSummary opening;
    private final long openedAt;
    /** Server side: the desk counting this screen as a viewer, or null once let go (or on the client). */
    @Nullable
    private BlockPos viewedConsole;

    public ControlModuleMenu(int containerId, Inventory inventory, BlockPos deskPos, int slot, ConsoleSummary opening) {
        super(AllNuclearGUIs.CONTROL_MODULE_MENU.get(), containerId);
        this.deskPos = deskPos;
        this.slot = slot;
        this.player = inventory.player;
        this.opening = opening;
        this.openedAt = inventory.player.level().getGameTime();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    /** The client's side, from what {@link #writeOpening} put in the open packet. */
    public static ControlModuleMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int slot = buf.readVarInt();
        return new ControlModuleMenu(containerId, inventory, pos, slot, ConsoleSummary.read(buf));
    }

    /** What the client needs to open the screen: the desk, the slot and the console's summary. */
    public static void writeOpening(FriendlyByteBuf buf, BlockPos pos, int slot, ConsoleSummary summary) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(slot);
        summary.write(buf);
    }

    public static void open(ServerPlayer player, BlockPos pos, int slot) {
        if (!(player.level().getBlockEntity(pos) instanceof ControlDeskBlockEntity desk)) {
            return;
        }
        ConsoleSummary summary = ConsoleSummary.of(desk);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
            ControlModuleMenu menu = new ControlModuleMenu(id, inventory, pos, slot, summary);
            menu.watch(desk);
            return menu;
        }, Component.translatable("create_nuclearindustry.control_module.title")), buf -> writeOpening(buf, pos, slot, summary));
    }

    /** Counts this screen on the console's first desk, so it resends live values until the screen closes. */
    public void watch(ControlDeskBlockEntity desk) {
        ControlDeskBlockEntity console = desk.origin().orElse(desk);
        console.addViewer();
        viewedConsole = console.getBlockPos();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (viewedConsole != null && !player.level().isClientSide
                && player.level().getBlockEntity(viewedConsole) instanceof ControlDeskBlockEntity console) {
            console.removeViewer();
        }
        viewedConsole = null;
    }

    public BlockPos deskPos() {
        return deskPos;
    }

    public int slot() {
        return slot;
    }

    /** The console as it was when the screen opened. */
    public ConsoleSummary openingSummary() {
        return opening;
    }

    /**
     * The console as last heard of: the desk's live sync on the client while it comes, otherwise what the
     * screen opened with.
     */
    public ConsoleSummary consoleSummary() {
        if (player.level().isClientSide && player.level().getBlockEntity(deskPos) instanceof ControlDeskBlockEntity desk) {
            ConsoleSummary synced = desk.syncedSummary(openedAt);
            if (synced != null) {
                return synced;
            }
        }
        return opening;
    }

    public TargetKind consoleKind() {
        return consoleSummary().kind();
    }

    public String consoleStatus() {
        return consoleSummary().status();
    }

    public MonitorReading consoleSample() {
        return consoleSummary().sample();
    }

    @Nullable
    public ControlModule module() {
        return player.level().getBlockEntity(deskPos) instanceof ControlDeskBlockEntity desk ? desk.module(slot) : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(deskPos) instanceof ControlDeskBlockEntity desk
                && ServerPayloadGuard.stillValid(desk, player) && desk.module(slot) != null;
    }
}
