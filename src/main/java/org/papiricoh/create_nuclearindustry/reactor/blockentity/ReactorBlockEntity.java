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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.control.ControlRodTracker;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorManager;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ReactorBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TANK_CAPACITY = 16_000;
    private static final int MAX_COOLANT_PER_TICK = 80;
    private static final int MAX_STEAM_PER_TICK = 120;
    private static final int CONTROL_ROD_SCAN_INTERVAL = 5;

    private ReactorStatus status = ReactorStatus.UNFORMED;
    private Optional<ReactorStructureValidator.ReactorStructure> currentStructure = Optional.empty();
    private ReactorStructureValidator.ReactorValidationResult lastValidation =
            ReactorStructureValidator.ReactorValidationResult.invalid(List.of("Not formed"), List.of());
    private ReactorPhysicsSimulator physicsSimulator = new ReactorPhysicsSimulator(0);
    private boolean pendingRevalidation = true;
    private int syncCounter;
    private int controlRodScanCounter;
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
    private final IFluidHandler fluidHandler = new ReactorFluidHandler();

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

        if (entity.status == ReactorStatus.FORMED) {
            entity.updateControlRodInsertion(false);
            boolean stillRunning = entity.physicsSimulator.tick();
            entity.processCoolantLoop();
            if (!stillRunning) {
                entity.handleMeltdown();
                return;
            }
        } else if (entity.status == ReactorStatus.SCRAMMED) {
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

    public IFluidHandler getFluidHandler(Direction direction) {
        return fluidHandler;
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
                    && (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED);
            currentStructure = Optional.of(newStructure);
            status = ReactorStatus.FORMED;
            if (structureChanged && !loadedStructureWithoutSnapshot) {
                physicsSimulator = new ReactorPhysicsSimulator(newStructure.getUraniumRodCount(), newStructure.getControlRodCount());
            }
            updateControlRodInsertion(true);
            if (playerInitiated || previousStatus != ReactorStatus.FORMED) {
                sendPlayerMessage("§2Reactor formed §8[" + newStructure.dimensionsText() + "]");
            }
            markDirtyAndSync();
            return;
        }

        if (previousStatus == ReactorStatus.FORMED || previousStatus == ReactorStatus.SCRAMMED) {
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
                && oldStructure.controlChannels.equals(newStructure.controlChannels);
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
        int steamToProduce = Math.max(1, Math.min(MAX_STEAM_PER_TICK, coolantToConsume + (int) Math.ceil(physicsSimulator.getSteamGenerationRate())));

        FluidStack produced = new FluidStack(heavy ? AllNuclearFluids.HEAVY_STEAM.get() : AllNuclearFluids.STEAM.get(), steamToProduce);
        int acceptedSteam = steamTank.fill(produced, IFluidHandler.FluidAction.SIMULATE);
        if (acceptedSteam <= 0) {
            return;
        }

        int steamProduced = Math.min(Math.min(steamToProduce, acceptedSteam), coolantTank.getFluidAmount() * 2);
        int coolantNeeded = Math.max(1, Math.min(coolantToConsume, (int) Math.ceil(steamProduced / 2.0)));
        FluidStack drained = coolantTank.drain(coolantNeeded, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }

        steamTank.fill(new FluidStack(produced.getFluid(), steamProduced), IFluidHandler.FluidAction.EXECUTE);
        double cooling = drained.getAmount() * (heavy ? 1.25 : 0.85);
        double moderation = heavy ? drained.getAmount() * 0.015 : drained.getAmount() * 0.004;
        physicsSimulator.applyExternalCooling(cooling, moderation);
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

        Set<BlockPos> structureBlocks = currentStructure
                .map(structure -> structure.allBlocks)
                .orElse(Set.of(reactorPos));
        for (BlockPos pos : structureBlocks) {
            level.destroyBlock(pos, true);
        }

        int explosionRadius = 20;
        int centerX = reactorPos.getX();
        int centerY = reactorPos.getY();
        int centerZ = reactorPos.getZ();
        for (int x = centerX - explosionRadius; x <= centerX + explosionRadius; x++) {
            for (int y = centerY - explosionRadius; y <= centerY + explosionRadius; y++) {
                for (int z = centerZ - explosionRadius; z <= centerZ + explosionRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    double distance = pos.distToCenterSqr(centerX + 0.5, centerY + 0.5, centerZ + 0.5);
                    double maxDistSq = explosionRadius * explosionRadius;
                    if (distance <= maxDistSq && Math.random() < 1.0 - (distance / maxDistSq) * 0.7) {
                        BlockState blockState = level.getBlockState(pos);
                        if (!blockState.isAir()) {
                            level.destroyBlock(pos, Math.random() < 0.5);
                        }
                    }
                }
            }
        }

        status = ReactorStatus.MELTDOWN;
        currentStructure = Optional.empty();
        physicsSimulator = new ReactorPhysicsSimulator(0);
        markDirtyAndSync();
        sendPlayerMessage("§4Nuclear meltdown at " + getBlockPos());
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
        pendingRevalidation = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putString("status", status.name());
        tag.put("physics", writePhysicsTag());
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
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
        MELTDOWN("Meltdown");

        private final String displayName;

        ReactorStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    private class ReactorFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? coolantTank.getFluid() : steamTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? coolantTank.getCapacity() : steamTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && coolantTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return NuclearFluidHelper.isCoolant(resource) ? coolantTank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return NuclearFluidHelper.isTurbineSteam(resource) ? steamTank.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return steamTank.drain(maxDrain, action);
        }
    }
}
