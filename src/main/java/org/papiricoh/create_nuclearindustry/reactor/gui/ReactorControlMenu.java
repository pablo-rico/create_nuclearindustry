package org.papiricoh.create_nuclearindustry.reactor.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearGUIs;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * Menu for the reactor control GUI.
 * Handles data synchronization between server and client.
 */
public class ReactorControlMenu extends AbstractContainerMenu {
    private static final int DATA_FORMED = 0;
    private static final int DATA_STATE = 1;
    private static final int DATA_TEMPERATURE = 2;
    private static final int DATA_NEUTRONS = 3;
    private static final int DATA_FUEL_X10 = 4;
    private static final int DATA_FUEL_CAPACITY_X10 = 5;
    private static final int DATA_ROD_POSITION_PERMILLE = 6;
    private static final int DATA_POWER_X10 = 7;
    private static final int DATA_STEAM_RATE_X10 = 8;
    private static final int DATA_THERMAL_STRESS = 9;
    private static final int DATA_THERMAL_EXCURSION = 10;
    private static final int DATA_ROD_INSERTION_PERMILLE = 11;
    private static final int DATA_STATIC_RODS = 12;
    private static final int DATA_MOVING_RODS = 13;
    private static final int DATA_INSERTED_RODS = 14;
    private static final int DATA_EXPECTED_RODS = 15;
    private static final int DATA_COOLANT = 16;
    private static final int DATA_STEAM_TANK = 17;
    private static final int DATA_FUEL_INPUT = 18;
    private static final int DATA_FUEL_OUTPUT = 19;
    private static final int DATA_PENDING_DEPLETED = 20;
    private static final int DATA_COUNT = 21;

    private final ReactorBlockEntity reactor;
    private final BlockPos reactorPos;
    private final Level menuLevel;
    private final ContainerData reactorData;

    // The screen reads these synced values instead of racing the client-side BlockEntity update.

    public ReactorControlMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null, emptyInitialData());
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, BlockPos reactorPos) {
        this(containerId, playerInventory, reactorPos, emptyInitialData());
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, BlockPos reactorPos, int[] initialData) {
        this(containerId, playerInventory, getReactorAt(playerInventory.player.level(), reactorPos), reactorPos, initialData);
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, ReactorBlockEntity reactor) {
        this(containerId, playerInventory, reactor, reactor != null ? reactor.getBlockPos() : null, emptyInitialData());
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, ReactorBlockEntity reactor, BlockPos reactorPos) {
        this(containerId, playerInventory, reactor, reactorPos, emptyInitialData());
    }

    public ReactorControlMenu(int containerId, Inventory playerInventory, ReactorBlockEntity reactor, BlockPos reactorPos, int[] initialData) {
        super(AllNuclearGUIs.REACTOR_MENU.get(), containerId);

        this.menuLevel = playerInventory.player.level();
        this.reactor = reactor;
        this.reactorPos = reactorPos;
        this.reactorData = reactor != null && !this.menuLevel.isClientSide
                ? new LiveReactorData(reactor)
                : createClientData(initialData);
        addDataSlots(this.reactorData);

        if (this.reactor != null || this.reactorPos != null) {
            addReactorFuelSlots(this.reactor);
        }
        addPlayerInventorySlots(playerInventory);
    }

    private void addReactorFuelSlots(ReactorBlockEntity reactor) {
        IItemHandler fuelInventory = reactor != null
                ? reactor.getFuelInventory()
                : new ItemStackHandler(ReactorBlockEntity.FUEL_SLOT_COUNT);
        for (int slot = 0; slot < ReactorBlockEntity.FUEL_INPUT_SLOTS; slot++) {
            addSlot(new FuelSlot(fuelInventory, reactor, slot, 20 + slot * 18, 180, true));
        }
        for (int slot = 0; slot < ReactorBlockEntity.FUEL_OUTPUT_SLOTS; slot++) {
            addSlot(new FuelSlot(fuelInventory, reactor, ReactorBlockEntity.FUEL_INPUT_SLOTS + slot, 164 + slot * 18, 180, false));
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

        return getReactorAt(player.level(), reactorPos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        ReactorBlockEntity liveReactor = getReactorEntity(player);
        int reactorSlots = liveReactor != null ? ReactorBlockEntity.FUEL_SLOT_COUNT : 0;

        if (index < reactorSlots) {
            if (!moveItemStackTo(stack, reactorSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (liveReactor != null && liveReactor.isFreshFuel(stack)) {
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
        return reactorData.get(DATA_TEMPERATURE);
    }

    /**
     * Gets neutron level for display.
     */
    public double getNeutronLevel() {
        return reactorData.get(DATA_NEUTRONS);
    }

    /**
     * Gets fuel remaining percentage.
     */
    public double getFuelRemaining() {
        return reactorData.get(DATA_FUEL_X10) / 10.0;
    }

    /**
     * Gets control rod position (0-1).
     */
    public float getControlRodPosition() {
        return reactorData.get(DATA_ROD_POSITION_PERMILLE) / 1000.0f;
    }

    /**
     * Gets power output for display.
     */
    public double getPowerOutput() {
        return reactorData.get(DATA_POWER_X10) / 10.0;
    }

    /**
     * Gets steam generation rate.
     */
    public double getSteamGenerationRate() {
        return reactorData.get(DATA_STEAM_RATE_X10) / 10.0;
    }

    /**
     * Gets reactor state for display.
     */
    public String getReactorState() {
        if (reactorPos == null) return "OFFLINE";
        if (!isReactorFormed()) return "INVALID";
        return getSyncedReactorStateName();
    }

    /**
     * Sets control rod position from GUI slider.
     */
    public void setControlRodPosition(float position) {
        ReactorBlockEntity liveReactor = getLiveReactor();
        if (liveReactor != null) {
            liveReactor.setControlRodPosition(position);
        }
    }

    /**
     * Gets whether the reactor is formed (live from BlockEntity).
     */
    public boolean isReactorFormed() {
        return reactorData.get(DATA_FORMED) != 0;
    }

    private ReactorBlockEntity getLiveReactor() {
        if (reactor != null) {
            return reactor;
        }
        return getReactorAt(menuLevel, reactorPos);
    }

    private static ReactorBlockEntity getReactorAt(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        var be = level.getBlockEntity(pos);
        return be instanceof ReactorBlockEntity reactorBE ? reactorBE : null;
    }

    /**
     * Gets reactor data live from BlockEntity (for synced display).
     */
    public boolean getCachedIsFormed() {
        return isReactorFormed();
    }

    public double getCachedTemperature() {
        return getCoreTemperature();
    }

    public double getCachedNeutrons() {
        return getNeutronLevel();
    }

    public double getCachedFuel() {
        return getFuelRemaining();
    }

    public float getCachedRodPosition() {
        return getControlRodPosition();
    }

    public double getFuelCapacity() {
        return reactorData.get(DATA_FUEL_CAPACITY_X10) / 10.0;
    }

    public double getThermalStress() {
        return reactorData.get(DATA_THERMAL_STRESS);
    }

    public boolean isThermalExcursionActive() {
        return reactorData.get(DATA_THERMAL_EXCURSION) != 0;
    }

    public float getControlRodInsertionRatio() {
        return reactorData.get(DATA_ROD_INSERTION_PERMILLE) / 1000.0f;
    }

    public int getStaticControlRodSegments() {
        return reactorData.get(DATA_STATIC_RODS);
    }

    public int getMovingControlRodSegments() {
        return reactorData.get(DATA_MOVING_RODS);
    }

    public int getInsertedControlRodSegments() {
        return reactorData.get(DATA_INSERTED_RODS);
    }

    public int getExpectedControlRodSegments() {
        return reactorData.get(DATA_EXPECTED_RODS);
    }

    public int getCoolantAmount() {
        return reactorData.get(DATA_COOLANT);
    }

    public int getSteamAmount() {
        return reactorData.get(DATA_STEAM_TANK);
    }

    public int getInputFuelCount() {
        return reactorData.get(DATA_FUEL_INPUT);
    }

    public int getOutputFuelCount() {
        return reactorData.get(DATA_FUEL_OUTPUT);
    }

    public int getPendingDepletedFuel() {
        return reactorData.get(DATA_PENDING_DEPLETED);
    }

    public String getSyncedReactorStateName() {
        int ordinal = reactorData.get(DATA_STATE);
        var states = org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator.ReactorState.values();
        if (ordinal < 0 || ordinal >= states.length) {
            return "Offline";
        }
        return states[ordinal].getDisplayName();
    }

    public static void writeInitialData(RegistryFriendlyByteBuf buf, ReactorBlockEntity reactor) {
        int[] data = snapshot(reactor);
        buf.writeVarInt(data.length);
        for (int value : data) {
            buf.writeVarInt(value);
        }
    }

    public static int[] readInitialData(RegistryFriendlyByteBuf buf) {
        int length = Math.max(0, Math.min(DATA_COUNT, buf.readVarInt()));
        int[] data = emptyInitialData();
        for (int index = 0; index < length; index++) {
            data[index] = buf.readVarInt();
        }
        return data;
    }

    private static ContainerData createClientData(int[] initialData) {
        SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
        int[] normalized = normalizeInitialData(initialData);
        for (int index = 0; index < DATA_COUNT; index++) {
            data.set(index, normalized[index]);
        }
        return data;
    }

    private static int[] normalizeInitialData(int[] initialData) {
        int[] data = emptyInitialData();
        if (initialData != null) {
            System.arraycopy(initialData, 0, data, 0, Math.min(initialData.length, DATA_COUNT));
        }
        return data;
    }

    private static int[] emptyInitialData() {
        int[] data = new int[DATA_COUNT];
        data[DATA_STATE] = org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator.ReactorState.IDLE.ordinal();
        data[DATA_TEMPERATURE] = 20;
        return data;
    }

    private static int[] snapshot(ReactorBlockEntity reactor) {
        int[] data = emptyInitialData();
        if (reactor == null) {
            return data;
        }
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = reactor.getDisplaySnapshot();
        data[DATA_FORMED] = snapshot.formed() ? 1 : 0;
        data[DATA_STATE] = snapshot.state().ordinal();
        data[DATA_TEMPERATURE] = clampInt(Math.round((float) snapshot.coreTemperature()), 0, 32767);
        data[DATA_NEUTRONS] = clampInt(Math.round((float) snapshot.neutronLevel()), 0, 32767);
        data[DATA_FUEL_X10] = scaled(snapshot.fuelRemaining(), 10.0);
        data[DATA_FUEL_CAPACITY_X10] = scaled(snapshot.fuelCapacity(), 10.0);
        data[DATA_ROD_POSITION_PERMILLE] = scaled(snapshot.controlRodPosition(), 1000.0);
        data[DATA_POWER_X10] = scaled(snapshot.powerOutput(), 10.0);
        data[DATA_STEAM_RATE_X10] = scaled(snapshot.steamGenerationRate(), 10.0);
        data[DATA_THERMAL_STRESS] = clampInt(Math.round((float) snapshot.thermalStress()), 0, 100);
        data[DATA_THERMAL_EXCURSION] = snapshot.thermalExcursionActive() ? 1 : 0;
        data[DATA_ROD_INSERTION_PERMILLE] = scaled(snapshot.controlRodInsertionRatio(), 1000.0);
        data[DATA_STATIC_RODS] = clampInt(snapshot.staticControlRodSegments(), 0, 32767);
        data[DATA_MOVING_RODS] = clampInt(snapshot.movingControlRodSegments(), 0, 32767);
        data[DATA_INSERTED_RODS] = clampInt(snapshot.insertedControlRodSegments(), 0, 32767);
        data[DATA_EXPECTED_RODS] = clampInt(snapshot.expectedControlRodSegments(), 0, 32767);
        data[DATA_COOLANT] = clampInt(snapshot.coolantAmount(), 0, 32767);
        data[DATA_STEAM_TANK] = clampInt(snapshot.steamAmount(), 0, 32767);
        data[DATA_FUEL_INPUT] = clampInt(snapshot.inputFuelCount(), 0, 32767);
        data[DATA_FUEL_OUTPUT] = clampInt(snapshot.outputFuelCount(), 0, 32767);
        data[DATA_PENDING_DEPLETED] = clampInt(snapshot.pendingDepletedFuel(), 0, 32767);
        return data;
    }

    private static int scaled(double value, double scale) {
        return clampInt((int) Math.round(value * scale), 0, 32767);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class LiveReactorData implements ContainerData {
        private final ReactorBlockEntity reactor;

        private LiveReactorData(ReactorBlockEntity reactor) {
            this.reactor = reactor;
        }

        @Override
        public int get(int index) {
            if (index < 0 || index >= DATA_COUNT) {
                return 0;
            }
            return snapshot(reactor)[index];
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }

    private static class FuelSlot extends SlotItemHandler {
        private final ReactorBlockEntity reactor;
        private final boolean input;

        private FuelSlot(IItemHandler fuelInventory, ReactorBlockEntity reactor, int slot, int x, int y, boolean input) {
            super(fuelInventory, slot, x, y);
            this.reactor = reactor;
            this.input = input;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return input && (reactor == null || reactor.isFreshFuel(stack));
        }
    }
}
