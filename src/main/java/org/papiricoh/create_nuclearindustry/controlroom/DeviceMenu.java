package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

import java.util.function.Supplier;

/**
 * Setup screen of a chart recorder or annunciator panel. It holds no items: the screen reads the
 * block entity it was opened on and sends every change as a {@link ConfigureDevicePayload}.
 * {@code part} is what the screen opens on, such as the window the player clicked.
 */
public class DeviceMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final int part;
    private final Player player;

    public DeviceMenu(MenuType<?> type, int containerId, Inventory inventory, BlockPos pos, int part) {
        super(type, containerId);
        this.pos = pos;
        this.part = part;
        this.player = inventory.player;
    }

    public static void open(ServerPlayer player, Supplier<? extends MenuType<DeviceMenu>> type, BlockPos pos, int part, Component title) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new DeviceMenu(type.get(), id, inventory, pos, part), title), buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(part);
        });
    }

    public BlockPos pos() {
        return pos;
    }

    public int part() {
        return part;
    }

    @Nullable
    public BlockEntity blockEntity() {
        return player.level().getBlockEntity(pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockEntity device = player.level().getBlockEntity(pos);
        return device instanceof ConfigurableDevice && ServerPayloadGuard.stillValid(device, player);
    }
}
