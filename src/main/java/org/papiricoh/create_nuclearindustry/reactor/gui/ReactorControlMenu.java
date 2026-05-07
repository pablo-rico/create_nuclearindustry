package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

        System.out.println("[ReactorControlMenu] Constructor: reactor=" + (reactor != null ? "not null" : "null") + ", reactorPos=" + reactorPos);

        // Don't add slots - reactor doesn't have inventory
        // Data will be fetched live from the BlockEntity
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
        // No inventory to move items in
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // Always valid for now
        return true;
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
            System.out.println("[ReactorControlMenu.getLiveReactor] Got reactor from field: isFormed=" + reactor.isFormed());
            return reactor;
        }

        // Try to get from level as fallback
        if (reactorPos != null && net.minecraft.client.Minecraft.getInstance().level != null) {
            var be = net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(reactorPos);
            if (be instanceof ReactorBlockEntity reactorBE) {
                System.out.println("[ReactorControlMenu.getLiveReactor] Got reactor from level: isFormed=" + reactorBE.isFormed());
                return reactorBE;
            } else {
                System.out.println("[ReactorControlMenu.getLiveReactor] BlockEntity at " + reactorPos + " is: " + (be != null ? be.getClass().getSimpleName() : "null"));
            }
        } else {
            System.out.println("[ReactorControlMenu.getLiveReactor] reactorPos=" + reactorPos + ", level=" + (net.minecraft.client.Minecraft.getInstance().level != null ? "not null" : "null"));
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
}
