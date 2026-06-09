package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * Menu for the reactor control GUI.
 * Handles data synchronization between server and client.
 */
public class ReactorControlMenu extends AbstractContainerMenu {

    private final ReactorBlockEntity reactor;
    private final BlockPos reactorPos;

    // Don't cache - fetch live from BlockEntity every time

    public ReactorControlMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, ReactorBlockEntity reactor) {
        this(containerId, playerInventory, reactor, reactor != null ? reactor.getBlockPos() : null);
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, ReactorBlockEntity reactor, BlockPos reactorPos) {
        super(AllNuclearGUIs.REACTOR_MENU.get(), containerId);

        // If reactor is null on client, try to get it from the stored position
        if (reactor == null && reactorPos == null && net.minecraft.client.Minecraft.getInstance().level != null) {
            var storedPos = org.papiricoh.create_nuclearindustry.reactor.block.NuclearReactorControllerBlock.lastReactorMenuPos;
            if (storedPos != null) {
                reactorPos = storedPos;
                var be = net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(storedPos);
                if (be instanceof ReactorBlockEntity reactorBE) {
                    reactor = reactorBE;
                }
            }
        }

        this.reactor = reactor;
        this.reactorPos = reactorPos;

        if (this.reactor != null) {
            addReactorFuelSlots(this.reactor);
        }
        addPlayerInventorySlots(playerInventory);
    }

    private void addReactorFuelSlots(ReactorBlockEntity reactor) {
        for (int slot = 0; slot < ReactorBlockEntity.FUEL_INPUT_SLOTS; slot++) {
            addSlot(new FuelSlot(reactor, slot, 20 + slot * 18, 180, true));
        }
        for (int slot = 0; slot < ReactorBlockEntity.FUEL_OUTPUT_SLOTS; slot++) {
            addSlot(new FuelSlot(reactor, ReactorBlockEntity.FUEL_INPUT_SLOTS + slot, 164 + slot * 18, 180, false));
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startY = 222;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 48 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 48 + col * 18, startY + 58));
        }
    }

    /**
     * Gets the reactor BlockEntity, fetching from level if null.
     */
    private ReactorBlockEntity getReactorEntity(Player player) {
        if (reactor != null) return reactor;
        if (reactorPos == null) return null;

        Level level = player.level();
        if (level == null) return null;

        var be = level.getBlockEntity(reactorPos);
        if (be instanceof ReactorBlockEntity reactorBE) {
            return reactorBE;
        }
        return null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int reactorSlots = reactor != null ? ReactorBlockEntity.FUEL_SLOT_COUNT : 0;

        if (index < reactorSlots) {
            if (!moveItemStackTo(stack, reactorSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (reactor != null && reactor.isFreshFuel(stack)) {
            if (!moveItemStackTo(stack, 0, ReactorBlockEntity.FUEL_INPUT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return reactorPos != null && player.blockPosition().closerThan(reactorPos, 8.0);
    }

    /**
     * Gets the reactor being controlled by this menu.
     */
    public ReactorBlockEntity getReactor() {
        return reactor;
    }

    /**
     * Gets the position of the reactor.
     */
    public BlockPos getReactorPos() {
        return reactorPos;
    }

    /**
     * Gets reactor temperature for display.
     */
    public double getCoreTemperature() {
        return reactor != null ? reactor.getCoreTemperature() : 0.0;
    }

    /**
     * Gets neutron level for display.
     */
    public double getNeutronLevel() {
        return reactor != null ? reactor.getNeutronLevel() : 0.0;
    }

    /**
     * Gets fuel remaining percentage.
     */
    public double getFuelRemaining() {
        return reactor != null ? reactor.getFuelRemaining() : 0.0;
    }

    /**
     * Gets control rod position (0-1).
     */
    public float getControlRodPosition() {
        return reactor != null ? reactor.getControlRodPosition() : 1.0f;
    }

    /**
     * Gets power output for display.
     */
    public double getPowerOutput() {
        return reactor != null ? reactor.getPowerOutput() : 0.0;
    }

    /**
     * Gets steam generation rate.
     */
    public double getSteamGenerationRate() {
        return reactor != null ? reactor.getSteamGenerationRate() : 0.0;
    }

    /**
     * Gets reactor state for display.
     */
    public String getReactorState() {
        if (reactor == null) return "OFFLINE";
        if (!reactor.isFormed()) return "INVALID";
        return reactor.getReactorState().getDisplayName();
    }

    /**
     * Sets control rod position from GUI slider.
     */
    public void setControlRodPosition(float position) {
        if (reactor != null) {
            reactor.setControlRodPosition(position);
        }
    }

    /**
     * Gets whether the reactor is formed (live from BlockEntity).
     */
    public boolean isReactorFormed() {
        return getCachedIsFormed();
    }

    private ReactorBlockEntity getLiveReactor() {
        if (reactor != null) {
            return reactor;
        }

        // Try to get from level as fallback
        if (reactorPos != null && net.minecraft.client.Minecraft.getInstance().level != null) {
            var be = net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(reactorPos);
            if (be instanceof ReactorBlockEntity reactorBE) {
                return reactorBE;
            }
        }
        return null;
    }

    /**
     * Gets reactor data live from BlockEntity (for synced display).
     */
    public boolean getCachedIsFormed() {
        var liveReactor = getLiveReactor();
        return liveReactor != null && liveReactor.isFormed();
    }

    public double getCachedTemperature() {
        var liveReactor = getLiveReactor();
        return liveReactor != null ? liveReactor.getCoreTemperature() : 20.0;
    }

    public double getCachedNeutrons() {
        var liveReactor = getLiveReactor();
        return liveReactor != null ? liveReactor.getNeutronLevel() : 0.0;
    }

    public double getCachedFuel() {
        var liveReactor = getLiveReactor();
        return liveReactor != null ? liveReactor.getFuelRemaining() : 0.0;
    }

    public float getCachedRodPosition() {
        var liveReactor = getLiveReactor();
        return liveReactor != null ? liveReactor.getControlRodPosition() : 0.0f;
    }

    private static class FuelSlot extends SlotItemHandler {
        private final ReactorBlockEntity reactor;
        private final boolean input;

        private FuelSlot(ReactorBlockEntity reactor, int slot, int x, int y, boolean input) {
            super(reactor.getFuelInventory(), slot, x, y);
            this.reactor = reactor;
            this.input = input;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return input && reactor.isFreshFuel(stack);
        }
    }
}
