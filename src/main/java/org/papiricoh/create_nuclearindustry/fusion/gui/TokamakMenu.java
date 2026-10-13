package org.papiricoh.create_nuclearindustry.fusion.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/**
 * Tokamak control panel. It holds no slots: the screen reads the controller's synced
 * {@link org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings} and sends its buttons as
 * {@link TokamakControlPayload}s, which the server validates.
 */
public class TokamakMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public TokamakMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(AllNuclearGUIs.TOKAMAK_MENU.get(), containerId);
        this.pos = pos;
    }

    public TokamakMenu(int containerId, Inventory inventory, FusionReactorBlockEntity tokamak) {
        this(containerId, inventory, tokamak.getBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    /** The controller this panel works, if it is still there. */
    @Nullable
    public FusionReactorBlockEntity tokamak(Level level) {
        return level.getBlockEntity(pos) instanceof FusionReactorBlockEntity tokamak ? tokamak : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return ServerPayloadGuard.stillValid(tokamak(player.level()), player);
    }

    public static void writeInitialData(RegistryFriendlyByteBuf buf, FusionReactorBlockEntity tokamak) {
        buf.writeBlockPos(tokamak.getBlockPos());
    }

    public static TokamakMenu read(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new TokamakMenu(containerId, inventory, buf.readBlockPos());
    }
}
