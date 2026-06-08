package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
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
    private static final int MAX_COOLANT_PER_TICK = 80;
    private static final int MAX_STEAM_PER_TICK = 80;
    private static final int CONTROL_ROD_SCAN_INTERVAL = 5;
    private static final int LOAD_MELTDOWN_GRACE_TICKS = 200;
    private static final int MELTDOWN_CONFIRM_TICKS = 200;
    private static final float MELTDOWN_EXPLOSION_POWER = 80.0f;
    public static final int FUEL_INPUT_SLOTS = 4;
    public static final int FUEL_OUTPUT_SLOTS = 2;
    public static final int FUEL_SLOT_COUNT = FUEL_INPUT_SLOTS + FUEL_OUTPUT_SLOTS;

    private ReactorStatus status = ReactorStatus.UNFORMED;
    private Optional<ReactorStructureValidator.ReactorStructure> currentStructure = Optional.empty();
    private ReactorStructureValidator.ReactorValidationResult lastValidation =
            ReactorStructureValidator.ReactorValidationResult.invalid(List.of("Not formed"), List.of());
    private ReactorPhysicsSimulator physicsSimulator = new ReactorPhysicsSimulator(0);
    private boolean pendingRevalidation = true;
    private int syncCounter;
    private int controlRodScanCounter;
    private int ticksSinceLoad;
    private int pendingDepletedFuel;
    private ControlRodTracker.ControlRodScan lastControlRodScan = ControlRodTracker.ControlRodScan.empty();
    private Optional<UUID> ownerUUID = Optional.empty();
    private Player ownerPlayer;

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
            if (!stillRunning) {
                if (entity.canTriggerMeltdown()) {
                    entity.handleMeltdown();
                } else {
                    entity.status = ReactorStatus.SCRAMMED_HOT;
                    entity.physicsSimulator.forceScramForLoad();
                    entity.sendPlayerMessage("§cReactor scrammed hot: meltdown protection active after load");
                    entity.markDirtyAndSync();
                }
                return;
            }
        } else if (entity.status == ReactorStatus.SCRAMMED || entity.status == ReactorStatus.SCRAMMED_HOT) {
            entity.physicsSimulator.scramTick();
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
            if (status == ReactorStatus.FORMED || status == ReactorStatus.MELTDOWN) {
                status = ReactorStatus.SCRAMMED_HOT;
                physicsSimulator.forceScramForLoad();
            }
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
        ReactorStructureValidator.ReactorValidationResult result = ReactorStructureValidator.validate(level, getBlockPos());
        lastValidation = result;

        if (result.formed() && result.structure().isPresent()) {
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
            if (playerInitiated || previousStatus != ReactorStatus.FORMED) {
                sendPlayerMessage(status == ReactorStatus.FORMED
                        ? "§2Reactor formed §8[" + newStructure.dimensionsText() + "]"
                        : "§eReactor structure valid, scrammed hot after load");
            }
            markDirtyAndSync();
            return;
        }

        if (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED || previousStatus == ReactorStatus.SCRAMMED_HOT) {
            status = ReactorStatus.SCRAMMED;
            physicsSimulator.setControlRodInsertion(1.0f);
            sendPlayerMessage("§cReactor scrammed: " + result.primaryMessage());
        } else {
            status = ReactorStatus.UNFORMED;
            currentStructure = Optional.empty();
            if (playerInitiated) {
                sendPlayerMessage("§cReactor incomplete: " + result.primaryMessage());
            }
        }

        markDirtyAndSync();
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
                && oldStructure.fuelPorts.equals(newStructure.fuelPorts);
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

        boolean heavy = NuclearFluidHelper.isHeavyWater(coolant);
        double heatFactor = Math.max(0.0, physicsSimulator.getCoreTemperature() - 100.0) / 4000.0;
        int coolantToConsume = Math.max(1, Math.min(MAX_COOLANT_PER_TICK, (int) Math.ceil(MAX_COOLANT_PER_TICK * heatFactor)));
        int steamToProduce = Math.max(1, Math.min(MAX_STEAM_PER_TICK, Math.max(coolantToConsume, (int) Math.ceil(physicsSimulator.getSteamGenerationRate()))));

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
        double cooling = drained.getAmount() * (heavy ? 1.25 : 0.85);
        double moderation = heavy ? drained.getAmount() * 0.015 : drained.getAmount() * 0.004;
        physicsSimulator.applyExternalCooling(cooling, moderation);
    }

    private void tryLoadFuelAssembly() {
        if (physicsSimulator.hasFuel() || pendingDepletedFuel > 0) {
            return;
        }
        for (int slot = 0; slot < FUEL_INPUT_SLOTS; slot++) {
            ItemStack extracted = fuelInventory.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                physicsSimulator.loadFuelAssembly();
                return;
            }
        }
    }

    private void recordSpentFuel(double previousFuel) {
        if (previousFuel > 0.0 && physicsSimulator.getFuelRemaining() <= 0.0) {
            pendingDepletedFuel++;
            tryStorePendingDepletedFuel();
        }
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

    private void markDirtyAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void sendPlayerMessage(String message) {
        if (getLevel() == null) {
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

    private void handleMeltdown() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos reactorPos = getBlockPos();
        LOGGER.warn("Nuclear meltdown at {}", reactorPos);

        status = ReactorStatus.MELTDOWN;
        currentStructure = Optional.empty();
        physicsSimulator = new ReactorPhysicsSimulator(0);
        markDirtyAndSync();
        sendPlayerMessage("§4Nuclear meltdown at " + getBlockPos());
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
        return status == ReactorStatus.FORMED;
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

    public float getControlRodPosition() {
        return status == ReactorStatus.FORMED ? physicsSimulator.getControlRodPosition() : 0.0f;
    }

    public double getPowerOutput() {
        return status == ReactorStatus.FORMED ? physicsSimulator.getPowerOutput() : 0.0;
    }

    public double getSteamGenerationRate() {
        return status == ReactorStatus.FORMED ? physicsSimulator.getSteamGenerationRate() : 0.0;
    }

    public ReactorPhysicsSimulator.ReactorState getReactorState() {
        if (status == ReactorStatus.SCRAMMED) {
            return ReactorPhysicsSimulator.ReactorState.CRITICAL;
        }
        return status == ReactorStatus.FORMED ? physicsSimulator.getState() : ReactorPhysicsSimulator.ReactorState.IDLE;
    }

    public Optional<ReactorStructureValidator.ReactorStructure> getStructure() {
        return currentStructure;
    }

    public ReactorPhysicsSimulator getPhysicsSimulator() {
        return physicsSimulator;
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
        return stack.is(AllNuclearItems.URANIUM_REACTOR_FUEL.get());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("Nuclear Reactor").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Structure: " + status.displayName)
                .withStyle(status == ReactorStatus.FORMED ? ChatFormatting.GREEN : ChatFormatting.RED));
        currentStructure.ifPresent(structure ->
                tooltip.add(Component.literal("  Dimensions: " + structure.dimensionsText()).withStyle(ChatFormatting.GRAY)));

        if (!lastValidation.errors().isEmpty()) {
            tooltip.add(Component.literal("  Missing: " + lastValidation.errors().get(0)).withStyle(ChatFormatting.RED));
        }
        for (String warning : lastValidation.warnings()) {
            tooltip.add(Component.literal("  Warning: " + warning).withStyle(ChatFormatting.YELLOW));
        }

        currentStructure.ifPresent(structure -> {
            tooltip.add(Component.literal("  Uranium columns: " + structure.uraniumColumnCount).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Heat exchanger columns: " + structure.heatExchangerColumnCount).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Control rod columns: " + structure.controlRodColumnCount).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Coolant input ports: " + structure.coolantInputPortCount).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Steam output ports: " + structure.steamOutputPortCount).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Fuel ports: " + structure.fuelPortCount).withStyle(ChatFormatting.GRAY));
        });
        tooltip.add(Component.literal(String.format("  Control rod insertion: %.0f%%", lastControlRodScan.insertionRatio() * 100.0f)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Static rods: " + lastControlRodScan.staticSegments()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Moving rods: " + lastControlRodScan.movingSegments()).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.literal("  Coolant: " + coolantTank.getFluidAmount() + "/" + coolantTank.getCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Steam: " + steamTank.getFluidAmount() + "/" + steamTank.getCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        if (status != ReactorStatus.UNFORMED) {
            tooltip.add(Component.literal(String.format("  Temperature: %.0f C", physicsSimulator.getCoreTemperature())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(String.format("  Neutrons: %.0f", physicsSimulator.getNeutronLevel())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(String.format("  Fuel: %.1f%%", physicsSimulator.getFuelRemaining())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Fuel input: " + countInputFuel() + " assemblies").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Depleted fuel: " + countOutputFuel() + " assemblies").withStyle(ChatFormatting.GRAY));
            if (pendingDepletedFuel > 0) {
                tooltip.add(Component.literal("  Depleted output blocked: " + pendingDepletedFuel).withStyle(ChatFormatting.RED));
            }
        }
        return true;
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
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
    }

    private CompoundTag writePhysicsTag() {
        CompoundTag physicsTag = new CompoundTag();
        physicsSimulator.serializeNBT(physicsTag);
        return physicsTag;
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
            count += fuelInventory.getStackInSlot(slot).getCount();
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
