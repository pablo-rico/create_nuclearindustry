package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionReactorControllerBlock;
import org.papiricoh.create_nuclearindustry.fusion.event.FusionReactorManager;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.FusionStructureValidator;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.FusionStructureValidator.FusionStructure;
import org.papiricoh.create_nuclearindustry.fusion.physics.FusionPhysicsSimulator;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FusionReactorBlockEntity extends BlockEntity implements IHaveGoggleInformation, Clearable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TANK_CAPACITY = 16_000;
    private static final int MAX_PLASMA_STEAM_TANK = 16_000;
    private static final double COOLING_PER_MB = 0.5;
    public static final int FUEL_INPUT_SLOT = 0;
    public static final int FUEL_OUTPUT_SLOT = 1;

    private FusionStatus status = FusionStatus.UNFORMED;
    private Optional<FusionStructure> currentStructure = Optional.empty();
    private FusionStructureValidator.FusionValidationResult lastValidation =
            FusionStructureValidator.FusionValidationResult.invalid(List.of("Not formed"), List.of());
    private FusionPhysicsSimulator physics = new FusionPhysicsSimulator(0, 1);
    private boolean pendingRevalidation = true;
    private int syncCounter;
    private Optional<UUID> ownerUUID = Optional.empty();
    private Player ownerPlayer;

    // Synced display summary (so goggles show data client-side without rebuilding the structure).
    private boolean dispFormed;
    private String dispDimensions = "";
    private int dispElectromagnets;
    private int dispMagnetInputs;
    private int dispCoolantPorts;
    private int dispPlasmaPorts;
    private int dispFuelInjectors;
    private String dispValidationMessage = "Not formed";

    private final FluidTank coolantTank = new FluidTank(TANK_CAPACITY, NuclearFluidHelper::isCoolant) {
        @Override
        protected void onContentsChanged() {
            markDirtyAndSync();
        }
    };
    private final FluidTank plasmaSteamTank = new FluidTank(MAX_PLASMA_STEAM_TANK, NuclearFluidHelper::isPlasmaSteam) {
        @Override
        protected void onContentsChanged() {
            markDirtyAndSync();
        }
    };
    private final ItemStackHandler fuelInventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == FUEL_INPUT_SLOT) {
                return stack.is(AllNuclearItems.DT_FUEL_PELLET.get());
            }
            return stack.is(AllNuclearItems.SPENT_DT_PELLET.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndSync();
        }
    };

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionReactorBlockEntity entity) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (entity.pendingRevalidation) {
            entity.revalidate(false, null);
        }

        if (entity.status == FusionStatus.FORMED || entity.status == FusionStatus.IGNITED) {
            entity.physics.setConfinementInput(entity.readConfinementInput());
            entity.tryLoadFuel();
            boolean contained = entity.physics.tick();
            entity.processCoolantLoop();
            entity.updateBlockStateFlags();
            if (!contained) {
                entity.handleContainmentBreach();
                return;
            }
            entity.status = entity.physics.isIgnited() ? FusionStatus.IGNITED : FusionStatus.FORMED;
        }

        if (++entity.syncCounter >= 20) {
            entity.syncCounter = 0;
            entity.markDirtyAndSync();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            FusionReactorManager.registerReactor(level, getBlockPos(), this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            FusionReactorManager.unregisterReactor(level, getBlockPos());
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
            return currentStructure.get().contains(changedPos);
        }
        return changedPos.closerThan(getBlockPos(), FusionStructureValidator.MAX_SIZE + 2);
    }

    private double readConfinementInput() {
        if (level == null || currentStructure.isEmpty()) {
            return 0.0;
        }
        double field = 0.0;
        for (BlockPos magnetPos : currentStructure.get().magnetInputs) {
            if (level.getBlockEntity(magnetPos) instanceof FusionMagnetInputBlockEntity magnet) {
                field = Math.max(field, magnet.getConfinementContribution());
            }
        }
        return field;
    }

    private void revalidate(boolean playerInitiated, Player player) {
        if (player != null) {
            ownerPlayer = player;
            ownerUUID = Optional.of(player.getUUID());
        }
        pendingRevalidation = false;
        if (level == null) {
            return;
        }

        FusionStatus previousStatus = status;
        FusionStructureValidator.FusionValidationResult result =
                FusionStructureValidator.validate(level, getBlockPos(), currentStructure);
        lastValidation = result;

        if (result.formed() && result.structure().isPresent()) {
            FusionStructure newStructure = result.structure().get();
            boolean changed = currentStructure.isEmpty() || !sameStructure(currentStructure.get(), newStructure);
            currentStructure = Optional.of(newStructure);
            if (changed) {
                physics = new FusionPhysicsSimulator(newStructure.electromagnetCount(), newStructure.size);
                status = FusionStatus.FORMED;
            } else if (status == FusionStatus.UNFORMED || status == FusionStatus.DISRUPTED) {
                status = FusionStatus.FORMED;
            }
            linkStructureParts(newStructure);
            if (playerInitiated || previousStatus == FusionStatus.UNFORMED) {
                sendPlayerMessage("§bFusion reactor formed §8[" + newStructure.dimensionsText() + "]");
            }
            markDirtyAndSync();
            return;
        }

        if (previousStatus == FusionStatus.FORMED || previousStatus == FusionStatus.IGNITED) {
            currentStructure.ifPresent(this::clearStructureParts);
            if (playerInitiated) {
                sendPlayerMessage("§cFusion reactor disrupted: " + result.primaryMessage());
            }
        } else if (playerInitiated) {
            sendPlayerMessage("§cFusion reactor incomplete: " + result.primaryMessage());
        }
        currentStructure = Optional.empty();
        status = FusionStatus.UNFORMED;
        physics = new FusionPhysicsSimulator(0, 1);
        markDirtyAndSync();
    }

    private boolean sameStructure(FusionStructure a, FusionStructure b) {
        return a.size == b.size
                && a.ringY == b.ringY
                && a.electromagnetCount() == b.electromagnetCount()
                && a.magnetInputs.equals(b.magnetInputs)
                && a.fluidPorts.equals(b.fluidPorts)
                && a.fuelInjectors.equals(b.fuelInjectors);
    }

    private void linkStructureParts(FusionStructure structure) {
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos portPos : structure.fluidPorts) {
            if (level.getBlockEntity(portPos) instanceof FusionFluidPortBlockEntity port) {
                port.setLinkedController(getBlockPos());
            }
        }
        for (BlockPos injectorPos : structure.fuelInjectors) {
            if (level.getBlockEntity(injectorPos) instanceof FusionFuelInjectorBlockEntity injector) {
                injector.setLinkedController(getBlockPos());
            }
        }
    }

    private void clearStructureParts(FusionStructure structure) {
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos portPos : structure.fluidPorts) {
            if (level.getBlockEntity(portPos) instanceof FusionFluidPortBlockEntity port) {
                port.clearLinkedController(getBlockPos());
            }
        }
        for (BlockPos injectorPos : structure.fuelInjectors) {
            if (level.getBlockEntity(injectorPos) instanceof FusionFuelInjectorBlockEntity injector) {
                injector.clearLinkedController(getBlockPos());
            }
        }
    }

    private void tryLoadFuel() {
        if (!physics.canLoadFuel()) {
            return;
        }
        ItemStack input = fuelInventory.getStackInSlot(FUEL_INPUT_SLOT);
        if (!input.is(AllNuclearItems.DT_FUEL_PELLET.get())) {
            return;
        }
        ItemStack spent = new ItemStack(AllNuclearItems.SPENT_DT_PELLET.get());
        if (!fuelInventory.insertItem(FUEL_OUTPUT_SLOT, spent, true).isEmpty()) {
            return; // output blocked
        }
        ItemStack extracted = fuelInventory.extractItem(FUEL_INPUT_SLOT, 1, false);
        if (extracted.isEmpty()) {
            return;
        }
        if (physics.loadFuel()) {
            fuelInventory.insertItem(FUEL_OUTPUT_SLOT, spent, false);
        } else {
            fuelInventory.insertItem(FUEL_INPUT_SLOT, extracted, false);
        }
    }

    private void processCoolantLoop() {
        double rate = physics.getPlasmaSteamRate();
        if (rate <= 0.0 || plasmaSteamTank.getSpace() <= 0) {
            return;
        }
        FluidStack coolant = coolantTank.getFluid();
        if (coolant.isEmpty()) {
            return;
        }
        int steamToProduce = Math.max(0, (int) Math.ceil(rate));
        FluidStack produced = new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), steamToProduce);
        int acceptedSteam = plasmaSteamTank.fill(produced, IFluidHandler.FluidAction.SIMULATE);
        if (acceptedSteam <= 0) {
            return;
        }
        int amount = Math.min(Math.min(steamToProduce, acceptedSteam), coolantTank.getFluidAmount());
        FluidStack drained = coolantTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        plasmaSteamTank.fill(new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), drained.getAmount()), IFluidHandler.FluidAction.EXECUTE);
        physics.applyExternalCooling(drained.getAmount() * COOLING_PER_MB);
    }

    private void updateBlockStateFlags() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(FusionReactorControllerBlock.ON)) {
            return;
        }
        boolean on = status == FusionStatus.FORMED || status == FusionStatus.IGNITED;
        boolean ignited = physics.isIgnited();
        if (state.getValue(FusionReactorControllerBlock.ON) != on
                || state.getValue(FusionReactorControllerBlock.IGNITED) != ignited) {
            level.setBlock(getBlockPos(),
                    state.setValue(FusionReactorControllerBlock.ON, on).setValue(FusionReactorControllerBlock.IGNITED, ignited),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void handleContainmentBreach() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = getBlockPos();
        LOGGER.warn("Fusion containment breach at {}", pos);
        int size = currentStructure.map(s -> s.size).orElse(FusionStructureValidator.MIN_SIZE);
        status = FusionStatus.DISRUPTED;
        currentStructure.ifPresent(this::clearStructureParts);
        currentStructure = Optional.empty();
        physics = new FusionPhysicsSimulator(0, 1);
        markDirtyAndSync();
        sendPlayerMessage("§4Plasma containment breach at " + pos);

        // Moderate, localized blast — reuses the crater system at a fraction of the nuke's scale.
        NuclearBlastManager.start(serverLevel, pos, size, Math.max(1, size / 2), 0.3f);
        serverLevel.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12.0f, Level.ExplosionInteraction.BLOCK);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void sendPlayerMessage(String message) {
        if (level == null) {
            return;
        }
        if (ownerPlayer != null && ownerPlayer.isAlive()) {
            ownerPlayer.displayClientMessage(Component.literal(message), false);
            return;
        }
        if (ownerUUID.isPresent()) {
            Player player = level.getPlayerByUUID(ownerUUID.get());
            if (player != null && player.isAlive()) {
                ownerPlayer = player;
                player.displayClientMessage(Component.literal(message), false);
                return;
            }
        }
        LOGGER.debug("[Fusion] {}", message);
    }

    // ---- accessors used by ports ----

    public int getTankCapacity() {
        return TANK_CAPACITY;
    }

    public int getCoolantAmount() {
        return coolantTank.getFluidAmount();
    }

    public int getPlasmaSteamAmount() {
        return plasmaSteamTank.getFluidAmount();
    }

    public FluidStack getCoolantFluid() {
        return coolantTank.getFluid().copy();
    }

    public FluidStack getPlasmaSteamFluid() {
        return plasmaSteamTank.getFluid().copy();
    }

    public int fillCoolant(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isCoolant(resource) ? coolantTank.fill(resource, action) : 0;
    }

    public FluidStack drainPlasmaSteam(FluidStack resource, IFluidHandler.FluidAction action) {
        return NuclearFluidHelper.isPlasmaSteam(resource) ? plasmaSteamTank.drain(resource, action) : FluidStack.EMPTY;
    }

    public FluidStack drainPlasmaSteam(int maxDrain, IFluidHandler.FluidAction action) {
        return plasmaSteamTank.drain(maxDrain, action);
    }

    public ItemStackHandler getFuelInventory() {
        return fuelInventory;
    }

    public Optional<FusionStructure> getStructure() {
        return currentStructure;
    }

    // ---- goggles ----

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean formed = level != null && level.isClientSide ? dispFormed : currentStructure.isPresent();
        tooltip.add(Component.literal("Fusion Reactor").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Structure: " + (formed ? "Formed" : "Incomplete"))
                .withStyle(formed ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (!formed) {
            tooltip.add(Component.literal("  " + dispValidationMessage).withStyle(ChatFormatting.RED));
            return true;
        }
        tooltip.add(Component.literal("  Ring: " + dispDimensions).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Electromagnets: " + dispElectromagnets + " (drives: " + dispMagnetInputs + ")")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Coolant ports: " + dispCoolantPorts + "  Plasma ports: " + dispPlasmaPorts)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Fuel injectors: " + dispFuelInjectors).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  State: " + physics.getState().getDisplayName())
                .withStyle(physics.isIgnited() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Plasma temp: %.0f / %.0f", physics.getPlasmaTemperature(), FusionPhysicsSimulator.MAX_PLASMA_TEMP))
                .withStyle(getTempColor(physics.getPlasmaTemperature())));
        tooltip.add(Component.literal(String.format("  Confinement: %.0f%%", physics.getConfinementField() * 100.0))
                .withStyle(physics.getConfinementField() >= 0.65 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        tooltip.add(Component.literal(String.format("  Instability: %.0f%%", physics.getInstability()))
                .withStyle(physics.getInstability() >= 60.0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  Fusion power: %.0f MW", physics.getFusionPower())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("  D-T fuel: %.0f / %.0f", physics.getFuelRemaining(), physics.getFuelCapacity())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Coolant: " + coolantTank.getFluidAmount() + "/" + TANK_CAPACITY + " mB").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Plasma steam: " + plasmaSteamTank.getFluidAmount() + "/" + MAX_PLASMA_STEAM_TANK + " mB").withStyle(ChatFormatting.GRAY));
        return true;
    }

    private ChatFormatting getTempColor(double temp) {
        if (temp >= FusionPhysicsSimulator.MAX_PLASMA_TEMP * 0.9) return ChatFormatting.RED;
        if (temp >= FusionPhysicsSimulator.IGNITION_TEMP) return ChatFormatting.GOLD;
        if (temp >= FusionPhysicsSimulator.AMBIENT_TEMP + 50) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }

    // ---- NBT ----

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("status", status.name());
        CompoundTag physicsTag = new CompoundTag();
        physics.serializeNBT(physicsTag);
        tag.put("physics", physicsTag);
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("plasmaSteam", plasmaSteamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("fuelInventory", fuelInventory.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        status = readStatus(tag.getString("status"));
        if (tag.contains("physics")) {
            physics.deserializeNBT(tag.getCompound("physics"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
        if (tag.contains("plasmaSteam")) {
            plasmaSteamTank.readFromNBT(provider, tag.getCompound("plasmaSteam"));
        }
        if (tag.contains("fuelInventory")) {
            fuelInventory.deserializeNBT(provider, tag.getCompound("fuelInventory"));
        }
        pendingRevalidation = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putString("status", status.name());
        CompoundTag physicsTag = new CompoundTag();
        physics.serializeNBT(physicsTag);
        tag.put("physics", physicsTag);
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        tag.put("plasmaSteam", plasmaSteamTank.writeToNBT(provider, new CompoundTag()));
        tag.putBoolean("dispFormed", currentStructure.isPresent());
        tag.putString("dispDimensions", currentStructure.map(FusionStructure::dimensionsText).orElse(""));
        tag.putInt("dispElectromagnets", currentStructure.map(FusionStructure::electromagnetCount).orElse(0));
        tag.putInt("dispMagnetInputs", currentStructure.map(s -> s.magnetInputCount).orElse(0));
        tag.putInt("dispCoolantPorts", currentStructure.map(s -> s.coolantPortCount).orElse(0));
        tag.putInt("dispPlasmaPorts", currentStructure.map(s -> s.plasmaPortCount).orElse(0));
        tag.putInt("dispFuelInjectors", currentStructure.map(s -> s.fuelInjectorCount).orElse(0));
        tag.putString("dispValidationMessage", lastValidation.primaryMessage());
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
            physics.deserializeNBT(tag.getCompound("physics"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
        if (tag.contains("plasmaSteam")) {
            plasmaSteamTank.readFromNBT(provider, tag.getCompound("plasmaSteam"));
        }
        dispFormed = tag.getBoolean("dispFormed");
        dispDimensions = tag.getString("dispDimensions");
        dispElectromagnets = tag.getInt("dispElectromagnets");
        dispMagnetInputs = tag.getInt("dispMagnetInputs");
        dispCoolantPorts = tag.getInt("dispCoolantPorts");
        dispPlasmaPorts = tag.getInt("dispPlasmaPorts");
        dispFuelInjectors = tag.getInt("dispFuelInjectors");
        dispValidationMessage = tag.getString("dispValidationMessage");
        if (dispValidationMessage.isEmpty()) {
            dispValidationMessage = "Not formed";
        }
    }

    @Override
    public void clearContent() {
        fuelInventory.setStackInSlot(FUEL_INPUT_SLOT, ItemStack.EMPTY);
        fuelInventory.setStackInSlot(FUEL_OUTPUT_SLOT, ItemStack.EMPTY);
    }

    private FusionStatus readStatus(String name) {
        try {
            return FusionStatus.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return FusionStatus.UNFORMED;
        }
    }

    private enum FusionStatus {
        UNFORMED,
        FORMED,
        IGNITED,
        DISRUPTED
    }
}
