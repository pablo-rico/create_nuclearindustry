package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.EnrichedUraniumBlendRecipe;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.block.NuclearReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.control.ControlRodTracker;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorManager;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ReactorBlockEntity extends BlockEntity implements IHaveGoggleInformation, Clearable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TANK_CAPACITY = 16_000;
    private static final int HEAT_EXCHANGER_STEAM_PER_TICK = 40;
    private static final int FLUID_PORT_STEAM_PER_TICK = 160;
    // Balance parameters for external coolant heat removal.
    private static final double WATER_COOLING_PER_MB = 0.225;
    private static final double HEAVY_WATER_COOLING_PER_MB = 0.35;
    private static final double WATER_MODERATION_PER_MB = 0.001;
    private static final double HEAVY_WATER_MODERATION_PER_MB = 0.003;
    private static final int CONTROL_ROD_SCAN_INTERVAL = 5;
    private static final int SCRAM_RECOVERY_SCAN_INTERVAL = 20;
    private static final int AUTOMATIC_MESSAGE_COOLDOWN_TICKS = 100;
    private static final int LOAD_MELTDOWN_GRACE_TICKS = 200;
    private static final int MELTDOWN_CONFIRM_TICKS = 200;
    private static final float MELTDOWN_EXPLOSION_POWER = 80.0f;
    public static final int FUEL_INPUT_SLOTS = 4;
    public static final int FUEL_OUTPUT_SLOTS = 2;
    public static final int FUEL_SLOT_COUNT = FUEL_INPUT_SLOTS + FUEL_OUTPUT_SLOTS;
    public static final float MIN_FUEL_ENRICHMENT = 3.0f;

    private ReactorStatus status = ReactorStatus.UNFORMED;
    private Optional<ReactorStructureValidator.ReactorStructure> currentStructure = Optional.empty();
    private ReactorStructureValidator.ReactorValidationResult lastValidation =
            ReactorStructureValidator.ReactorValidationResult.invalid(List.of("Not formed"), List.of());
    private ReactorPhysicsSimulator physicsSimulator = new ReactorPhysicsSimulator(0);
    private boolean pendingRevalidation = true;
    private int syncCounter;
    private int controlRodScanCounter;
    private int scramRecoveryScanCounter;
    private int ticksSinceLoad;
    private int pendingDepletedFuel;
    private ControlRodTracker.ControlRodScan lastControlRodScan = ControlRodTracker.ControlRodScan.empty();
    private ReactorDisplaySnapshot displaySnapshot = ReactorDisplaySnapshot.empty();
    private boolean hasDisplaySnapshot;
    private Optional<UUID> ownerUUID = Optional.empty();
    private Player ownerPlayer;
    private String lastAutomaticMessage = "";
    private long lastAutomaticMessageGameTime = Long.MIN_VALUE;

    private final FluidTank coolantTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isCoolant) {
        @Override
        protected void onContentsChanged() {
            markDirtyAndSync();
        }
    };
    private final FluidTank steamTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isTurbineSteam) {
        @Override
        protected void onContentsChanged() {
            markDirtyAndSync();
        }
    };
    private final ItemStackHandler fuelInventory = new ItemStackHandler(FUEL_SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < FUEL_INPUT_SLOTS) {
                return isFreshFuel(stack);
            }
            return stack.is(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndSync();
        }
    };
    private final IItemHandler fuelInputHandler = new FuelSlotView(true);
    private final IItemHandler fuelOutputHandler = new FuelSlotView(false);

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReactorBlockEntity entity) {
        if (level == null || level.isClientSide) {
            return;
        }

        if (entity.pendingRevalidation) {
            entity.revalidate(false, null);
        }
        entity.ticksSinceLoad++;

        if (entity.status == ReactorStatus.FORMED) {
            entity.updateControlRodInsertion(false);
            entity.tryStorePendingDepletedFuel();
            entity.tryLoadFuelAssembly();
            double previousFuel = entity.physicsSimulator.getFuelRemaining();
            boolean stillRunning = entity.physicsSimulator.tick();
            entity.recordSpentFuel(previousFuel);
            entity.processCoolantLoop();
            entity.updateControllerRunningState();
            if (!stillRunning) {
                if (entity.canTriggerMeltdown()) {
                    entity.handleMeltdown();
                } else {
                    entity.status = ReactorStatus.SCRAMMED_HOT;
                    entity.physicsSimulator.forceScramForLoad();
                    entity.sendPlayerMessage("§cReactor scrammed hot: meltdown protection active after load", false);
                    entity.markDirtyAndSync();
                }
                return;
            }
        } else if (entity.status == ReactorStatus.SCRAMMED || entity.status == ReactorStatus.SCRAMMED_HOT) {
            entity.physicsSimulator.scramTick();
            if (entity.currentStructure.isPresent() && entity.status == ReactorStatus.SCRAMMED_HOT && !entity.physicsSimulator.isMeltingDown()) {
                entity.status = ReactorStatus.FORMED;
                entity.markDirtyAndSync();
            } else if (entity.currentStructure.isEmpty() && ++entity.scramRecoveryScanCounter >= SCRAM_RECOVERY_SCAN_INTERVAL) {
                entity.scramRecoveryScanCounter = 0;
                entity.revalidate(false, null);
            }
            entity.updateControllerRunningState();
        }

        if (++entity.syncCounter >= 20) {
            entity.markDirtyAndSync();
            entity.syncCounter = 0;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            ReactorManager.registerReactor(level, getBlockPos(), this);
            ticksSinceLoad = 0;
        }
    }

    @Override
    public void setRemoved() {
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            ReactorManager.unregisterReactor(level, getBlockPos());
        }
        super.setRemoved();
    }

    public void attemptFormation(Player player) {
        revalidate(true, player);
    }

    public void requestStructureRevalidation(Player player) {
        if (player != null) {
            ownerPlayer = player;
            ownerUUID = Optional.of(player.getUUID());
        }
        pendingRevalidation = true;
    }

    public boolean shouldReactToBlockChange(BlockPos changedPos) {
        if (currentStructure.isPresent()) {
            ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
            return structure.contains(changedPos) && !structure.isControlArea(changedPos);
        }
        return changedPos.closerThan(getBlockPos(), ReactorStructureValidator.MAX_WIDTH + 2);
    }

    private void revalidate(boolean playerInitiated, Player player) {
        if (player != null) {
            ownerPlayer = player;
            ownerUUID = Optional.of(player.getUUID());
        }
        pendingRevalidation = false;

        Level level = getLevel();
        if (level == null) {
            return;
        }

        ReactorStatus previousStatus = status;
        ReactorStructureValidator.ReactorValidationResult result = ReactorStructureValidator.validate(level, getBlockPos(), currentStructure);
        lastValidation = result;

        if (result.formed() && result.structure().isPresent()) {
            scramRecoveryScanCounter = 0;
            ReactorStructureValidator.ReactorStructure newStructure = result.structure().get();
            boolean structureChanged = currentStructure.isEmpty() || !sameStructure(currentStructure.get(), newStructure);
            boolean loadedStructureWithoutSnapshot = currentStructure.isEmpty()
                    && (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED || previousStatus == ReactorStatus.SCRAMMED_HOT);
            currentStructure = Optional.of(newStructure);
            status = previousStatus == ReactorStatus.SCRAMMED_HOT && ticksSinceLoad < LOAD_MELTDOWN_GRACE_TICKS
                    ? ReactorStatus.SCRAMMED_HOT
                    : ReactorStatus.FORMED;
            if (structureChanged && !loadedStructureWithoutSnapshot) {
                physicsSimulator = new ReactorPhysicsSimulator(newStructure.getUraniumRodCount(), newStructure.getControlRodCount());
            }
            updateControlRodInsertion(true);
            linkStructureParts(newStructure);
            if (playerInitiated || previousStatus != ReactorStatus.FORMED) {
                sendPlayerMessage(status == ReactorStatus.FORMED
                        ? "§2Reactor formed §8[" + newStructure.dimensionsText() + "]"
                        : "§eReactor structure valid, scrammed hot after load", playerInitiated);
            }
            markDirtyAndSync();
            return;
        }

        if (isDynamicControlChannelFailure(result) && currentStructure.isPresent()) {
            status = previousStatus == ReactorStatus.SCRAMMED_HOT ? ReactorStatus.SCRAMMED_HOT : ReactorStatus.FORMED;
            updateControlRodInsertion(true);
            linkStructureParts(currentStructure.get());
            if (playerInitiated) {
                sendPlayerMessage("§eReactor formed; control rod channel is occupied by moving rods", true);
            }
            markDirtyAndSync();
            return;
        }

        if (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED || previousStatus == ReactorStatus.SCRAMMED_HOT) {
            currentStructure.ifPresent(this::clearStructureParts);
            currentStructure = Optional.empty();
            status = ReactorStatus.SCRAMMED;
            physicsSimulator.setControlRodInsertion(1.0f);
            if (playerInitiated || previousStatus != ReactorStatus.SCRAMMED) {
                sendPlayerMessage("§cReactor scrammed: " + result.primaryMessage(), playerInitiated);
            }
        } else {
            status = ReactorStatus.UNFORMED;
            currentStructure = Optional.empty();
            if (playerInitiated) {
                sendPlayerMessage("§cReactor incomplete: " + result.primaryMessage(), true);
            }
        }

        markDirtyAndSync();
    }

    private boolean isDynamicControlChannelFailure(ReactorStructureValidator.ReactorValidationResult result) {
        return !result.errors().isEmpty()
                && result.errors().stream().allMatch(error -> error.startsWith("Control rod channel blocked at "));
    }

    private boolean sameStructure(ReactorStructureValidator.ReactorStructure oldStructure, ReactorStructureValidator.ReactorStructure newStructure) {
        return oldStructure.width == newStructure.width
                && oldStructure.height == newStructure.height
                && oldStructure.bottomY == newStructure.bottomY
                && oldStructure.uraniumRodCount == newStructure.uraniumRodCount
                && oldStructure.controlRodCount == newStructure.controlRodCount
                && oldStructure.heatExchangerCount == newStructure.heatExchangerCount
                && oldStructure.coolantInputPortCount == newStructure.coolantInputPortCount
                && oldStructure.steamOutputPortCount == newStructure.steamOutputPortCount
                && oldStructure.fuelPortCount == newStructure.fuelPortCount
                && oldStructure.controlChannels.equals(newStructure.controlChannels)
                && oldStructure.fluidPorts.equals(newStructure.fluidPorts)
                && oldStructure.fuelPorts.equals(newStructure.fuelPorts)
                && oldStructure.temperatureSensors.equals(newStructure.temperatureSensors);
    }

    private void linkStructureParts(ReactorStructureValidator.ReactorStructure structure) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos portPos : structure.fluidPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFluidPortBlockEntity port) {
                port.setLinkedController(getBlockPos());
            }
        }
        for (BlockPos portPos : structure.fuelPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFuelPortBlockEntity port) {
                port.setLinkedController(getBlockPos());
            }
        }
        for (BlockPos sensorPos : structure.temperatureSensors) {
            if (level.getBlockEntity(sensorPos) instanceof ReactorTemperatureSensorBlockEntity sensor) {
                sensor.setLinkedController(getBlockPos());
            }
        }
    }

    private void clearStructureParts(ReactorStructureValidator.ReactorStructure structure) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos portPos : structure.fluidPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFluidPortBlockEntity port) {
                port.clearLinkedController(getBlockPos());
            }
        }
        for (BlockPos portPos : structure.fuelPorts) {
            if (level.getBlockEntity(portPos) instanceof ReactorFuelPortBlockEntity port) {
                port.clearLinkedController(getBlockPos());
            }
        }
        for (BlockPos sensorPos : structure.temperatureSensors) {
            if (level.getBlockEntity(sensorPos) instanceof ReactorTemperatureSensorBlockEntity sensor) {
                sensor.clearLinkedController(getBlockPos());
            }
        }
    }

    private void updateControlRodInsertion(boolean force) {
        if (getLevel() == null || currentStructure.isEmpty()) {
            lastControlRodScan = ControlRodTracker.ControlRodScan.empty();
            physicsSimulator.setControlRodInsertion(0.0f);
            return;
        }

        if (!force && controlRodScanCounter++ < CONTROL_ROD_SCAN_INTERVAL) {
            return;
        }
        controlRodScanCounter = 0;
        lastControlRodScan = ControlRodTracker.scan(getLevel(), currentStructure.get());
        physicsSimulator.setControlRodInsertion(lastControlRodScan.insertionRatio());
    }

    private void processCoolantLoop() {
        if (physicsSimulator.getCoreTemperature() <= 100.0 || steamTank.getSpace() <= 0) {
            return;
        }

        FluidStack coolant = coolantTank.getFluid();
        if (coolant.isEmpty()) {
            return;
        }

        int throughput = getSteamThroughputLimit();
        if (throughput <= 0) {
            return;
        }

        boolean heavy = NuclearFluidHelper.isHeavyWater(coolant);
        int steamToProduce = Math.max(0, Math.min(throughput, (int) Math.ceil(physicsSimulator.getSteamGenerationRate())));
        if (steamToProduce <= 0) {
            return;
        }

        FluidStack produced = new FluidStack(heavy ? AllNuclearFluids.HEAVY_STEAM.get() : AllNuclearFluids.STEAM.get(), steamToProduce);
        int acceptedSteam = steamTank.fill(produced, IFluidHandler.FluidAction.SIMULATE);
        if (acceptedSteam <= 0) {
            return;
        }

        int amount = Math.min(Math.min(steamToProduce, acceptedSteam), coolantTank.getFluidAmount());
        FluidStack drained = coolantTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }

        steamTank.fill(new FluidStack(produced.getFluid(), drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        double cooling = drained.getAmount() * (heavy ? HEAVY_WATER_COOLING_PER_MB : WATER_COOLING_PER_MB);
        double moderation = drained.getAmount() * (heavy ? HEAVY_WATER_MODERATION_PER_MB : WATER_MODERATION_PER_MB);
        physicsSimulator.applyExternalCooling(cooling, moderation);
    }

    private int getSteamThroughputLimit() {
        if (currentStructure.isEmpty()) {
            return 0;
        }
        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        int exchangerLimit = structure.heatExchangerCount * HEAT_EXCHANGER_STEAM_PER_TICK;
        int coolantPortLimit = structure.coolantInputPortCount * FLUID_PORT_STEAM_PER_TICK;
        int steamPortLimit = structure.steamOutputPortCount * FLUID_PORT_STEAM_PER_TICK;
        return Math.min(exchangerLimit, Math.min(coolantPortLimit, steamPortLimit));
    }

    private void tryLoadFuelAssembly() {
        if (pendingDepletedFuel > 0) {
            return;
        }
        while (physicsSimulator.canLoadFuelAssembly()) {
            boolean loaded = false;
            for (int slot = 0; slot < FUEL_INPUT_SLOTS; slot++) {
                if (!isFreshFuel(fuelInventory.getStackInSlot(slot))) {
                    continue;
                }
                ItemStack extracted = fuelInventory.extractItem(slot, 1, false);
                if (!extracted.isEmpty()) {
                    if (physicsSimulator.loadFuelAssembly()) {
                        loaded = true;
                    } else {
                        fuelInventory.insertItem(slot, extracted, false);
                    }
                    break;
                }
            }
            if (!loaded) {
                return;
            }
        }
    }

    private void recordSpentFuel(double previousFuel) {
        int previousAssemblies = assembliesRemaining(previousFuel);
        int currentAssemblies = assembliesRemaining(physicsSimulator.getFuelRemaining());
        if (currentAssemblies < previousAssemblies) {
            pendingDepletedFuel += previousAssemblies - currentAssemblies;
            tryStorePendingDepletedFuel();
        }
    }

    private int assembliesRemaining(double fuelAmount) {
        return fuelAmount <= 0.0 ? 0 : (int) Math.ceil(fuelAmount / 100.0);
    }

    private void tryStorePendingDepletedFuel() {
        while (pendingDepletedFuel > 0) {
            ItemStack depleted = new ItemStack(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
            ItemStack remainder = insertIntoOutputSlots(depleted, false);
            if (!remainder.isEmpty()) {
                return;
            }
            pendingDepletedFuel--;
        }
    }

    private ItemStack insertIntoOutputSlots(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack;
        for (int slot = FUEL_INPUT_SLOTS; slot < FUEL_SLOT_COUNT && !remainder.isEmpty(); slot++) {
            remainder = fuelInventory.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private boolean canTriggerMeltdown() {
        return status == ReactorStatus.FORMED
                && ticksSinceLoad >= LOAD_MELTDOWN_GRACE_TICKS
                && physicsSimulator.getMeltdownTickCounter() >= MELTDOWN_CONFIRM_TICKS;
    }

    private void updateControllerRunningState() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(NuclearReactorControllerBlock.ON)) {
            return;
        }
        boolean running = status == ReactorStatus.FORMED && physicsSimulator.isRunning();
        if (state.getValue(NuclearReactorControllerBlock.ON) != running) {
            level.setBlock(getBlockPos(), state.setValue(NuclearReactorControllerBlock.ON, running), Block.UPDATE_CLIENTS);
        }
    }

    private void markDirtyAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void sendPlayerMessage(String message, boolean force) {
        if (getLevel() == null) {
            return;
        }
        if (!force && !shouldSendAutomaticMessage(message)) {
            return;
        }
        if (ownerPlayer != null && ownerPlayer.isAlive()) {
            ownerPlayer.displayClientMessage(Component.literal(message), false);
            return;
        }
        if (ownerUUID.isPresent()) {
            Player player = getLevel().getPlayerByUUID(ownerUUID.get());
            if (player != null && player.isAlive()) {
                ownerPlayer = player;
                player.displayClientMessage(Component.literal(message), false);
                return;
            }
        }
        LOGGER.debug("[Reactor] {}", message);
    }

    private boolean shouldSendAutomaticMessage(String message) {
        long gameTime = getLevel().getGameTime();
        if (message.equals(lastAutomaticMessage)
                && gameTime - lastAutomaticMessageGameTime < AUTOMATIC_MESSAGE_COOLDOWN_TICKS) {
            return false;
        }
        lastAutomaticMessage = message;
        lastAutomaticMessageGameTime = gameTime;
        return true;
    }

    private void handleMeltdown() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos reactorPos = getBlockPos();
        LOGGER.warn("Nuclear meltdown at {}", reactorPos);

        status = ReactorStatus.MELTDOWN;
        currentStructure.ifPresent(this::clearStructureParts);
        currentStructure = Optional.empty();
        physicsSimulator = new ReactorPhysicsSimulator(0);
        markDirtyAndSync();
        sendPlayerMessage("§4Nuclear meltdown at " + getBlockPos(), false);
        level.explode(null, reactorPos.getX() + 0.5, reactorPos.getY() + 0.5, reactorPos.getZ() + 0.5,
                MELTDOWN_EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
    }

    public void setControlRodPosition(float position) {
        physicsSimulator.setControlRodPosition(position);
        markDirtyAndSync();
    }

    public void moveControlRod(float delta) {
        physicsSimulator.moveControlRod(delta);
        markDirtyAndSync();
    }

    public boolean isFormed() {
        return currentStructure.isPresent() && status != ReactorStatus.UNFORMED && status != ReactorStatus.MELTDOWN;
    }

    public double getCoreTemperature() {
        return status == ReactorStatus.UNFORMED ? 20.0 : physicsSimulator.getCoreTemperature();
    }

    public double getNeutronLevel() {
        return status == ReactorStatus.UNFORMED ? 0.0 : physicsSimulator.getNeutronLevel();
    }

    public double getFuelRemaining() {
        return status == ReactorStatus.UNFORMED ? 0.0 : physicsSimulator.getFuelRemaining();
    }

    public double getFuelCapacity() {
        return status == ReactorStatus.UNFORMED ? 0.0 : physicsSimulator.getFuelCapacity();
    }

    public int getInputFuelCount() {
        return countInputFuel();
    }

    public int getOutputFuelCount() {
        return countOutputFuel();
    }

    public int getPendingDepletedFuel() {
        return pendingDepletedFuel;
    }

    public ControlRodTracker.ControlRodScan getControlRodScan() {
        return lastControlRodScan;
    }

    public int getStaticControlRodSegments() {
        return lastControlRodScan.staticSegments();
    }

    public int getMovingControlRodSegments() {
        return lastControlRodScan.movingSegments();
    }

    public int getInsertedControlRodSegments() {
        return lastControlRodScan.insertedSegments();
    }

    public int getExpectedControlRodSegments() {
        return lastControlRodScan.expectedSegments();
    }

    public float getControlRodInsertionRatio() {
        return lastControlRodScan.insertionRatio();
    }

    public float getControlRodPosition() {
        return isFormed() ? physicsSimulator.getControlRodPosition() : 0.0f;
    }

    public double getPowerOutput() {
        return isFormed() ? physicsSimulator.getPowerOutput() : 0.0;
    }

    public double getSteamGenerationRate() {
        return isFormed() ? physicsSimulator.getSteamGenerationRate() : 0.0;
    }

    public double getThermalStress() {
        return isFormed() ? physicsSimulator.getThermalStress() : 0.0;
    }

    public boolean isThermalExcursionActive() {
        return isFormed() && physicsSimulator.isThermalExcursionActive();
    }

    public ReactorPhysicsSimulator.ReactorState getReactorState() {
        if (status == ReactorStatus.SCRAMMED && currentStructure.isEmpty()) {
            return ReactorPhysicsSimulator.ReactorState.CRITICAL;
        }
        if (!isFormed()) {
            return ReactorPhysicsSimulator.ReactorState.IDLE;
        }
        if (physicsSimulator.getFuelRemaining() > 0.0
                && physicsSimulator.getNeutronLevel() <= 1.0
                && lastControlRodScan.insertionRatio() >= 0.99f) {
            return ReactorPhysicsSimulator.ReactorState.SUPPRESSED;
        }
        if (physicsSimulator.getFuelRemaining() <= 0.0 && countInputFuel() > 0) {
            return ReactorPhysicsSimulator.ReactorState.FUELING;
        }
        return physicsSimulator.getState();
    }

    public Optional<ReactorStructureValidator.ReactorStructure> getStructure() {
        return currentStructure;
    }

    public ReactorPhysicsSimulator getPhysicsSimulator() {
        return physicsSimulator;
    }

    public ReactorDisplaySnapshot getDisplaySnapshot() {
        if (getLevel() != null && getLevel().isClientSide) {
            return hasUsableDisplaySnapshot() ? displaySnapshot : ReactorDisplaySnapshot.unsynced();
        }
        return createDisplaySnapshot();
    }

    public int getTankCapacity() {
        return TANK_CAPACITY;
    }

    public int getCoolantAmount() {
        return coolantTank.getFluidAmount();
    }

    public int getSteamAmount() {
        return steamTank.getFluidAmount();
    }

    public FluidStack getCoolantFluid() {
        return coolantTank.getFluid().copy();
    }

    public FluidStack getSteamFluid() {
        return steamTank.getFluid().copy();
    }

    public int fillCoolant(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isCoolant(resource) ? coolantTank.fill(resource, action) : 0;
    }

    public FluidStack drainSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isTurbineSteam(resource) ? steamTank.drain(resource, action) : FluidStack.EMPTY;
    }

    public FluidStack drainSteam(int maxDrain, IFluidHandler.FluidAction action) {
        return steamTank.drain(maxDrain, action);
    }

    public IItemHandler getFuelItemHandler(ReactorFluidPortMode mode) {
        return mode == ReactorFluidPortMode.INPUT ? fuelInputHandler : fuelOutputHandler;
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    public boolean isFreshFuel(ItemStack stack) {
        return (stack.is(AllNuclearItems.URANIUM_REACTOR_FUEL.get()) || stack.is(AllNuclearItems.URANIUM.get()))
                && getFuelEnrichment(stack) > MIN_FUEL_ENRICHMENT;
    }

    public static float getFuelEnrichment(ItemStack stack) {
        if (stack.is(AllNuclearItems.URANIUM.get())) {
            return UraniumItem.getEnrichment(stack);
        }
        if (stack.is(AllNuclearItems.URANIUM_REACTOR_FUEL.get())) {
            return stack.has(AllNuclearDataComponents.ENRICHMENT.get())
                    ? UraniumItem.getEnrichment(stack)
                    : EnrichedUraniumBlendRecipe.REQUIRED_ENRICHMENT;
        }
        return 0.0f;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorDisplaySnapshot snapshot = getDisplaySnapshot();
        tooltip.add(Component.literal("Nuclear Reactor").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Structure: " + (snapshot.formed() ? "Formed" : getStructureDisplayName(snapshot)))
                .withStyle(snapshot.formed() ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("  State: " + snapshot.state().getDisplayName())
                .withStyle(getReactorStateColor(snapshot.state())));
        if (!snapshot.dimensions().isEmpty()) {
            tooltip.add(Component.literal("  Dimensions: " + snapshot.dimensions()).withStyle(ChatFormatting.GRAY));
        }

        if (!snapshot.formed()) {
            tooltip.add(Component.literal("  Missing: " + snapshot.validationMessage()).withStyle(ChatFormatting.RED));
            return true;
        }

        tooltip.add(Component.literal("  Uranium columns: " + snapshot.uraniumColumnCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Heat exchanger columns: " + snapshot.heatExchangerColumnCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Control rod columns: " + snapshot.controlRodColumnCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Control rod segments: " + snapshot.insertedControlRodSegments() + "/" + snapshot.expectedControlRodSegments()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Coolant input ports: " + snapshot.coolantInputPortCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Steam output ports: " + snapshot.steamOutputPortCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Fuel ports: " + snapshot.fuelPortCount()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Control rod insertion: %.0f%%", snapshot.controlRodInsertionRatio() * 100.0f)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Static rods: " + snapshot.staticControlRodSegments()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Moving rods: " + snapshot.movingControlRodSegments()).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.literal("  Coolant: " + snapshot.coolantAmount() + "/" + snapshot.tankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Steam: " + snapshot.steamAmount() + "/" + snapshot.tankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Temperature: %.0f C", snapshot.coreTemperature()))
                .withStyle(getTemperatureColor(snapshot.coreTemperature())));
        tooltip.add(Component.literal(String.format("  Neutrons: %.0f", snapshot.neutronLevel())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Thermal stress: %.0f%%", snapshot.thermalStress()))
                .withStyle(getThermalStressColor(snapshot.thermalStress())));
        if (snapshot.thermalExcursionActive()) {
            tooltip.add(Component.literal("  Thermal excursion: insert control rods").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.literal(String.format("  Fuel: %.1f / %.1f units", snapshot.fuelRemaining(), snapshot.fuelCapacity())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Fuel input: " + snapshot.inputFuelCount() + " assemblies").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Depleted fuel: " + snapshot.outputFuelCount() + " assemblies").withStyle(ChatFormatting.GRAY));
        if (snapshot.fuelRemaining() > 0.0 && snapshot.neutronLevel() <= 1.0 && snapshot.controlRodInsertionRatio() >= 0.99f) {
            tooltip.add(Component.literal("  Status: loaded, control rods suppressing reaction").withStyle(ChatFormatting.YELLOW));
        }
        if (snapshot.pendingDepletedFuel() > 0) {
            tooltip.add(Component.literal("  Depleted output blocked: " + snapshot.pendingDepletedFuel()).withStyle(ChatFormatting.RED));
        }
        return true;
    }

    private String getStructureDisplayName(ReactorDisplaySnapshot snapshot) {
        if (snapshot.formed()) {
            return "Formed";
        }
        if (!snapshot.statusDisplayName().isEmpty()) {
            return snapshot.statusDisplayName();
        }
        return "Incomplete";
    }

    private ChatFormatting getTemperatureColor(double temperature) {
        if (temperature >= 3500.0) {
            return ChatFormatting.RED;
        }
        if (temperature >= 2000.0) {
            return ChatFormatting.GOLD;
        }
        if (temperature >= 500.0) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GREEN;
    }

    private ChatFormatting getThermalStressColor(double stress) {
        if (stress >= 90.0) {
            return ChatFormatting.RED;
        }
        if (stress >= 65.0) {
            return ChatFormatting.GOLD;
        }
        if (stress >= 35.0) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GREEN;
    }

    private ChatFormatting getReactorStateColor(ReactorPhysicsSimulator.ReactorState state) {
        return switch (state) {
            case RUNNING -> ChatFormatting.GREEN;
            case FUELING, SUPPRESSED, WARNING -> ChatFormatting.YELLOW;
            case CRITICAL, MELTING -> ChatFormatting.RED;
            case IDLE -> ChatFormatting.GRAY;
        };
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("status", status.name());
        tag.put("physics", writePhysicsTag());
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("fuelInventory", fuelInventory.serializeNBT(provider));
        tag.putInt("pendingDepletedFuel", pendingDepletedFuel);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        status = readStatus(tag.getString("status"));
        if (tag.contains("physics")) {
            physicsSimulator.deserializeNBT(tag.getCompound("physics"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
        if (tag.contains("steam")) {
            steamTank.readFromNBT(provider, tag.getCompound("steam"));
        }
        if (tag.contains("fuelInventory")) {
            fuelInventory.deserializeNBT(provider, tag.getCompound("fuelInventory"));
        } else {
            physicsSimulator.clearFuel();
        }
        pendingDepletedFuel = Math.max(0, tag.getInt("pendingDepletedFuel"));
        pendingRevalidation = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putString("status", status.name());
        tag.put("physics", writePhysicsTag());
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("fuelInventory", fuelInventory.serializeNBT(provider));
        tag.putInt("pendingDepletedFuel", pendingDepletedFuel);
        currentStructure.ifPresent(structure -> {
            tag.put("structure", writeStructureTag(structure));
            tag.put("controlRodScan", writeControlRodScanTag(lastControlRodScan));
        });
        tag.put("validation", writeValidationTag(lastValidation));
        tag.put("displaySnapshot", writeDisplaySnapshot(createDisplaySnapshot()));
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        status = readStatus(tag.getString("status"));
        if (tag.contains("physics")) {
            physicsSimulator.deserializeNBT(tag.getCompound("physics"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
        if (tag.contains("steam")) {
            steamTank.readFromNBT(provider, tag.getCompound("steam"));
        }
        if (tag.contains("fuelInventory")) {
            fuelInventory.deserializeNBT(provider, tag.getCompound("fuelInventory"));
        }
        pendingDepletedFuel = Math.max(0, tag.getInt("pendingDepletedFuel"));
        currentStructure = tag.contains("structure")
                ? readStructureTag(tag.getCompound("structure"))
                : Optional.empty();
        lastControlRodScan = currentStructure.isPresent() && tag.contains("controlRodScan")
                ? readControlRodScanTag(tag.getCompound("controlRodScan"))
                : ControlRodTracker.ControlRodScan.empty();
        lastValidation = tag.contains("validation")
                ? readValidationTag(tag.getCompound("validation"))
                : ReactorStructureValidator.ReactorValidationResult.invalid(List.of("Not formed"), List.of());
        displaySnapshot = tag.contains("displaySnapshot")
                ? readDisplaySnapshot(tag.getCompound("displaySnapshot"))
                : ReactorDisplaySnapshot.unsynced();
        hasDisplaySnapshot = tag.contains("displaySnapshot");
    }

    private ReactorDisplaySnapshot createDisplaySnapshot() {
        if (!isFormed() || currentStructure.isEmpty()) {
            return ReactorDisplaySnapshot.empty(status.displayName, getReactorState(), lastValidation.primaryMessage());
        }

        ReactorStructureValidator.ReactorStructure structure = currentStructure.get();
        return new ReactorDisplaySnapshot(
                true,
                status.displayName,
                getReactorState(),
                lastValidation.primaryMessage(),
                structure.dimensionsText(),
                structure.uraniumRodCount,
                structure.controlRodCount,
                structure.heatExchangerCount,
                structure.uraniumColumnCount,
                structure.controlRodColumnCount,
                structure.heatExchangerColumnCount,
                structure.coolantInputPortCount,
                structure.steamOutputPortCount,
                structure.fuelPortCount,
                structure.fuelInputPortCount,
                structure.fuelOutputPortCount,
                coolantTank.getFluidAmount(),
                steamTank.getFluidAmount(),
                coolantTank.getCapacity(),
                physicsSimulator.getCoreTemperature(),
                physicsSimulator.getNeutronLevel(),
                physicsSimulator.getFuelRemaining(),
                physicsSimulator.getFuelCapacity(),
                physicsSimulator.getControlRodPosition(),
                physicsSimulator.getPowerOutput(),
                physicsSimulator.getSteamGenerationRate(),
                physicsSimulator.getThermalStress(),
                physicsSimulator.isThermalExcursionActive(),
                lastControlRodScan.insertionRatio(),
                lastControlRodScan.staticSegments(),
                lastControlRodScan.movingSegments(),
                lastControlRodScan.insertedSegments(),
                lastControlRodScan.expectedSegments(),
                countInputFuel(),
                countOutputFuel(),
                pendingDepletedFuel
        );
    }

    private boolean hasUsableDisplaySnapshot() {
        return hasDisplaySnapshot && displaySnapshot != null;
    }

    private CompoundTag writeDisplaySnapshot(ReactorDisplaySnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", snapshot.formed());
        tag.putString("statusDisplayName", snapshot.statusDisplayName());
        tag.putString("state", snapshot.state().name());
        tag.putString("validationMessage", snapshot.validationMessage());
        tag.putString("dimensions", snapshot.dimensions());
        tag.putInt("uraniumRodCount", snapshot.uraniumRodCount());
        tag.putInt("controlRodCount", snapshot.controlRodCount());
        tag.putInt("heatExchangerCount", snapshot.heatExchangerCount());
        tag.putInt("uraniumColumnCount", snapshot.uraniumColumnCount());
        tag.putInt("controlRodColumnCount", snapshot.controlRodColumnCount());
        tag.putInt("heatExchangerColumnCount", snapshot.heatExchangerColumnCount());
        tag.putInt("coolantInputPortCount", snapshot.coolantInputPortCount());
        tag.putInt("steamOutputPortCount", snapshot.steamOutputPortCount());
        tag.putInt("fuelPortCount", snapshot.fuelPortCount());
        tag.putInt("fuelInputPortCount", snapshot.fuelInputPortCount());
        tag.putInt("fuelOutputPortCount", snapshot.fuelOutputPortCount());
        tag.putInt("coolantAmount", snapshot.coolantAmount());
        tag.putInt("steamAmount", snapshot.steamAmount());
        tag.putInt("tankCapacity", snapshot.tankCapacity());
        tag.putDouble("coreTemperature", snapshot.coreTemperature());
        tag.putDouble("neutronLevel", snapshot.neutronLevel());
        tag.putDouble("fuelRemaining", snapshot.fuelRemaining());
        tag.putDouble("fuelCapacity", snapshot.fuelCapacity());
        tag.putFloat("controlRodPosition", snapshot.controlRodPosition());
        tag.putDouble("powerOutput", snapshot.powerOutput());
        tag.putDouble("steamGenerationRate", snapshot.steamGenerationRate());
        tag.putDouble("thermalStress", snapshot.thermalStress());
        tag.putBoolean("thermalExcursionActive", snapshot.thermalExcursionActive());
        tag.putFloat("controlRodInsertionRatio", snapshot.controlRodInsertionRatio());
        tag.putInt("staticControlRodSegments", snapshot.staticControlRodSegments());
        tag.putInt("movingControlRodSegments", snapshot.movingControlRodSegments());
        tag.putInt("insertedControlRodSegments", snapshot.insertedControlRodSegments());
        tag.putInt("expectedControlRodSegments", snapshot.expectedControlRodSegments());
        tag.putInt("inputFuelCount", snapshot.inputFuelCount());
        tag.putInt("outputFuelCount", snapshot.outputFuelCount());
        tag.putInt("pendingDepletedFuel", snapshot.pendingDepletedFuel());
        return tag;
    }

    private ReactorDisplaySnapshot readDisplaySnapshot(CompoundTag tag) {
        return new ReactorDisplaySnapshot(
                tag.getBoolean("formed"),
                tag.getString("statusDisplayName"),
                readReactorState(tag.getString("state")),
                defaultString(tag.getString("validationMessage"), "Not formed"),
                tag.getString("dimensions"),
                Math.max(0, tag.getInt("uraniumRodCount")),
                Math.max(0, tag.getInt("controlRodCount")),
                Math.max(0, tag.getInt("heatExchangerCount")),
                Math.max(0, tag.getInt("uraniumColumnCount")),
                Math.max(0, tag.getInt("controlRodColumnCount")),
                Math.max(0, tag.getInt("heatExchangerColumnCount")),
                Math.max(0, tag.getInt("coolantInputPortCount")),
                Math.max(0, tag.getInt("steamOutputPortCount")),
                Math.max(0, tag.getInt("fuelPortCount")),
                Math.max(0, tag.getInt("fuelInputPortCount")),
                Math.max(0, tag.getInt("fuelOutputPortCount")),
                Math.max(0, tag.getInt("coolantAmount")),
                Math.max(0, tag.getInt("steamAmount")),
                Math.max(1, tag.getInt("tankCapacity")),
                Math.max(20.0, tag.getDouble("coreTemperature")),
                Math.max(0.0, tag.getDouble("neutronLevel")),
                Math.max(0.0, tag.getDouble("fuelRemaining")),
                Math.max(0.0, tag.getDouble("fuelCapacity")),
                Math.max(0.0f, Math.min(1.0f, tag.getFloat("controlRodPosition"))),
                Math.max(0.0, tag.getDouble("powerOutput")),
                Math.max(0.0, tag.getDouble("steamGenerationRate")),
                Math.max(0.0, Math.min(100.0, tag.getDouble("thermalStress"))),
                tag.getBoolean("thermalExcursionActive"),
                Math.max(0.0f, Math.min(1.0f, tag.getFloat("controlRodInsertionRatio"))),
                Math.max(0, tag.getInt("staticControlRodSegments")),
                Math.max(0, tag.getInt("movingControlRodSegments")),
                Math.max(0, tag.getInt("insertedControlRodSegments")),
                Math.max(0, tag.getInt("expectedControlRodSegments")),
                Math.max(0, tag.getInt("inputFuelCount")),
                Math.max(0, tag.getInt("outputFuelCount")),
                Math.max(0, tag.getInt("pendingDepletedFuel"))
        );
    }

    private CompoundTag writePhysicsTag() {
        CompoundTag physicsTag = new CompoundTag();
        physicsSimulator.serializeNBT(physicsTag);
        return physicsTag;
    }

    private CompoundTag writeStructureTag(ReactorStructureValidator.ReactorStructure structure) {
        CompoundTag structureTag = new CompoundTag();
        structureTag.putInt("width", structure.width);
        structureTag.putInt("height", structure.height);
        structureTag.putInt("bottomY", structure.bottomY);
        structureTag.putInt("uraniumRodCount", structure.uraniumRodCount);
        structureTag.putInt("controlRodCount", structure.controlRodCount);
        structureTag.putInt("heatExchangerCount", structure.heatExchangerCount);
        structureTag.putInt("uraniumColumnCount", structure.uraniumColumnCount);
        structureTag.putInt("controlRodColumnCount", structure.controlRodColumnCount);
        structureTag.putInt("heatExchangerColumnCount", structure.heatExchangerColumnCount);
        structureTag.putInt("coolantInputPortCount", structure.coolantInputPortCount);
        structureTag.putInt("steamOutputPortCount", structure.steamOutputPortCount);
        structureTag.putInt("fuelPortCount", structure.fuelPortCount);
        structureTag.putInt("fuelInputPortCount", structure.fuelInputPortCount);
        structureTag.putInt("fuelOutputPortCount", structure.fuelOutputPortCount);
        structureTag.putLongArray("controlChannels", writePositions(structure.controlChannels));
        structureTag.putLongArray("fluidPorts", writePositions(structure.fluidPorts));
        structureTag.putLongArray("fuelPorts", writePositions(structure.fuelPorts));
        structureTag.putLongArray("temperatureSensors", writePositions(structure.temperatureSensors));
        return structureTag;
    }

    private Optional<ReactorStructureValidator.ReactorStructure> readStructureTag(CompoundTag structureTag) {
        int width = structureTag.getInt("width");
        int height = structureTag.getInt("height");
        if (width < ReactorStructureValidator.MIN_WIDTH || width > ReactorStructureValidator.MAX_WIDTH
                || height < ReactorStructureValidator.MIN_HEIGHT || height > ReactorStructureValidator.MAX_HEIGHT
                || width % 2 == 0) {
            return Optional.empty();
        }

        ReactorStructureValidator.ReactorStructure structure =
                new ReactorStructureValidator.ReactorStructure(getBlockPos(), width, height, structureTag.getInt("bottomY"));
        structure.uraniumRodCount = Math.max(0, structureTag.getInt("uraniumRodCount"));
        structure.controlRodCount = Math.max(0, structureTag.getInt("controlRodCount"));
        structure.heatExchangerCount = Math.max(0, structureTag.getInt("heatExchangerCount"));
        structure.uraniumColumnCount = Math.max(0, structureTag.getInt("uraniumColumnCount"));
        structure.controlRodColumnCount = Math.max(0, structureTag.getInt("controlRodColumnCount"));
        structure.heatExchangerColumnCount = Math.max(0, structureTag.getInt("heatExchangerColumnCount"));
        structure.coolantInputPortCount = Math.max(0, structureTag.getInt("coolantInputPortCount"));
        structure.steamOutputPortCount = Math.max(0, structureTag.getInt("steamOutputPortCount"));
        structure.fuelPortCount = Math.max(0, structureTag.getInt("fuelPortCount"));
        structure.fuelInputPortCount = Math.max(0, structureTag.getInt("fuelInputPortCount"));
        structure.fuelOutputPortCount = Math.max(0, structureTag.getInt("fuelOutputPortCount"));
        readPositions(structureTag.getLongArray("controlChannels"), structure.controlChannels);
        readPositions(structureTag.getLongArray("fluidPorts"), structure.fluidPorts);
        readPositions(structureTag.getLongArray("fuelPorts"), structure.fuelPorts);
        readPositions(structureTag.getLongArray("temperatureSensors"), structure.temperatureSensors);
        return Optional.of(structure);
    }

    private long[] writePositions(Set<BlockPos> positions) {
        long[] packed = new long[positions.size()];
        int index = 0;
        for (BlockPos pos : positions) {
            packed[index++] = pos.asLong();
        }
        return packed;
    }

    private void readPositions(long[] packed, Set<BlockPos> positions) {
        positions.clear();
        for (long value : packed) {
            positions.add(BlockPos.of(value));
        }
    }

    private CompoundTag writeControlRodScanTag(ControlRodTracker.ControlRodScan scan) {
        CompoundTag scanTag = new CompoundTag();
        scanTag.putFloat("insertionRatio", scan.insertionRatio());
        scanTag.putInt("staticSegments", scan.staticSegments());
        scanTag.putInt("movingSegments", scan.movingSegments());
        scanTag.putInt("insertedSegments", scan.insertedSegments());
        scanTag.putInt("expectedSegments", scan.expectedSegments());
        return scanTag;
    }

    private ControlRodTracker.ControlRodScan readControlRodScanTag(CompoundTag scanTag) {
        int expectedSegments = Math.max(0, scanTag.getInt("expectedSegments"));
        int insertedSegments = Math.max(0, scanTag.getInt("insertedSegments"));
        int staticSegments = Math.max(0, scanTag.getInt("staticSegments"));
        int movingSegments = Math.max(0, scanTag.getInt("movingSegments"));
        float insertionRatio = Math.max(0.0f, Math.min(1.0f, scanTag.getFloat("insertionRatio")));
        return new ControlRodTracker.ControlRodScan(insertionRatio, staticSegments, movingSegments, insertedSegments, expectedSegments);
    }

    private CompoundTag writeValidationTag(ReactorStructureValidator.ReactorValidationResult validation) {
        CompoundTag validationTag = new CompoundTag();
        validationTag.putBoolean("formed", validation.formed());
        validationTag.putString("errors", String.join("\n", validation.errors()));
        validationTag.putString("warnings", String.join("\n", validation.warnings()));
        return validationTag;
    }

    private ReactorStructureValidator.ReactorValidationResult readValidationTag(CompoundTag validationTag) {
        List<String> errors = readStringList(validationTag.getString("errors"));
        List<String> warnings = readStringList(validationTag.getString("warnings"));
        if (validationTag.getBoolean("formed") && currentStructure.isPresent()) {
            return ReactorStructureValidator.ReactorValidationResult.valid(currentStructure.get(), warnings);
        }
        return ReactorStructureValidator.ReactorValidationResult.invalid(
                errors.isEmpty() ? List.of("Not formed") : errors,
                warnings);
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return List.of(value.split("\n"));
    }

    private ReactorPhysicsSimulator.ReactorState readReactorState(String name) {
        try {
            return ReactorPhysicsSimulator.ReactorState.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ReactorPhysicsSimulator.ReactorState.IDLE;
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private ReactorStatus readStatus(String name) {
        if ("INVALID_HOT".equals(name)) {
            return ReactorStatus.SCRAMMED;
        }
        try {
            return ReactorStatus.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ReactorStatus.UNFORMED;
        }
    }

    public record ReactorDisplaySnapshot(
            boolean formed,
            String statusDisplayName,
            ReactorPhysicsSimulator.ReactorState state,
            String validationMessage,
            String dimensions,
            int uraniumRodCount,
            int controlRodCount,
            int heatExchangerCount,
            int uraniumColumnCount,
            int controlRodColumnCount,
            int heatExchangerColumnCount,
            int coolantInputPortCount,
            int steamOutputPortCount,
            int fuelPortCount,
            int fuelInputPortCount,
            int fuelOutputPortCount,
            int coolantAmount,
            int steamAmount,
            int tankCapacity,
            double coreTemperature,
            double neutronLevel,
            double fuelRemaining,
            double fuelCapacity,
            float controlRodPosition,
            double powerOutput,
            double steamGenerationRate,
            double thermalStress,
            boolean thermalExcursionActive,
            float controlRodInsertionRatio,
            int staticControlRodSegments,
            int movingControlRodSegments,
            int insertedControlRodSegments,
            int expectedControlRodSegments,
            int inputFuelCount,
            int outputFuelCount,
            int pendingDepletedFuel
    ) {
        public static ReactorDisplaySnapshot empty() {
            return empty("Incomplete", ReactorPhysicsSimulator.ReactorState.IDLE, "Not formed");
        }

        public static ReactorDisplaySnapshot unsynced() {
            return empty("Syncing", ReactorPhysicsSimulator.ReactorState.IDLE, "Waiting for server sync");
        }

        public static ReactorDisplaySnapshot empty(String statusDisplayName, ReactorPhysicsSimulator.ReactorState state, String validationMessage) {
            return new ReactorDisplaySnapshot(
                    false,
                    statusDisplayName == null || statusDisplayName.isEmpty() ? "Incomplete" : statusDisplayName,
                    state == null ? ReactorPhysicsSimulator.ReactorState.IDLE : state,
                    validationMessage == null || validationMessage.isEmpty() ? "Not formed" : validationMessage,
                    "",
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    TANK_CAPACITY,
                    20.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0f,
                    0.0,
                    0.0,
                    0.0,
                    false,
                    0.0f,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
    }

    private enum ReactorStatus {
        UNFORMED("Incomplete"),
        FORMED("Formed"),
        SCRAMMED("Scrammed"),
        SCRAMMED_HOT("Scrammed Hot"),
        MELTDOWN("Meltdown");

        private final String displayName;

        ReactorStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    private int countInputFuel() {
        int count = 0;
        for (int slot = 0; slot < FUEL_INPUT_SLOTS; slot++) {
            ItemStack stack = fuelInventory.getStackInSlot(slot);
            if (isFreshFuel(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countOutputFuel() {
        int count = pendingDepletedFuel;
        for (int slot = FUEL_INPUT_SLOTS; slot < FUEL_SLOT_COUNT; slot++) {
            count += fuelInventory.getStackInSlot(slot).getCount();
        }
        return count;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            fuelInventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private class FuelSlotView implements IItemHandlerModifiable {
        private final boolean input;

        private FuelSlotView(boolean input) {
            this.input = input;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            int actual = actualSlot(slot);
            if (actual >= 0 && isValidForView(actual, stack)) {
                fuelInventory.setStackInSlot(actual, stack);
            }
        }

        @Override
        public int getSlots() {
            return input ? FUEL_INPUT_SLOTS : FUEL_OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            int actual = actualSlot(slot);
            return actual >= 0 ? fuelInventory.getStackInSlot(actual) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int actual = actualSlot(slot);
            if (actual < 0 || !input || !isFreshFuel(stack)) {
                return stack;
            }
            return fuelInventory.insertItem(actual, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int actual = actualSlot(slot);
            if (actual < 0 || input) {
                return ItemStack.EMPTY;
            }
            return fuelInventory.extractItem(actual, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int actual = actualSlot(slot);
            return actual >= 0 && isValidForView(actual, stack);
        }

        private int actualSlot(int slot) {
            if (slot < 0 || slot >= getSlots()) {
                return -1;
            }
            return input ? slot : FUEL_INPUT_SLOTS + slot;
        }

        private boolean isValidForView(int actual, ItemStack stack) {
            return input ? isFreshFuel(stack) : actual >= FUEL_INPUT_SLOTS && stack.is(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get());
        }
    }

}
