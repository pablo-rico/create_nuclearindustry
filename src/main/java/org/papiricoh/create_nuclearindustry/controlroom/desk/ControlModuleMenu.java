package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
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
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * Setup screen of one control module. It holds no items of its own: the player inventory is there
 * to pick up frequency items, and every change goes to the server as a {@link ConfigureModulePayload}.
 */
public class ControlModuleMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 20;
    public static final int INVENTORY_Y = 140;

    private final BlockPos deskPos;
    private final int slot;
    private final Player player;

    public ControlModuleMenu(int containerId, Inventory inventory, BlockPos deskPos, int slot) {
        super(AllNuclearGUIs.CONTROL_MODULE_MENU.get(), containerId);
        this.deskPos = deskPos;
        this.slot = slot;
        this.player = inventory.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    public static void open(ServerPlayer player, BlockPos pos, int slot) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ControlModuleMenu(id, inventory, pos, slot),
                Component.translatable("create_nuclearindustry.control_module.title")), buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(slot);
        });
    }

    public BlockPos deskPos() {
        return deskPos;
    }

    public int slot() {
        return slot;
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
