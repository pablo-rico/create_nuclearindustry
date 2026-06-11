package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.event.FusionReactorManager;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.FusionStructureValidator;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FusionFluidPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int LINK_SCAN_INTERVAL = 20;

    private final IFluidHandler inputHandler = new PortFluidHandler(ReactorFluidPortMode.INPUT);
    private final IFluidHandler outputHandler = new PortFluidHandler(ReactorFluidPortMode.OUTPUT);
    private Optional<BlockPos> linkedController = Optional.empty();
    private boolean linked;
    private int linkScanCounter;

    public FusionFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_FLUID_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++linkScanCounter >= LINK_SCAN_INTERVAL) {
            linkScanCounter = 0;
            rescanLinkedReactor();
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        // Flat ring geometry: pipes connect from any horizontal side, so expose the handler on
        // every face (do NOT gate on FACING like the vertical fission ports do).
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
    }

    public boolean hasLinkedReactor() {
        return findLinkedReactor().isPresent();
    }

    /** Right-click with a bucket: fills coolant (INPUT) or drains the active tank, via the linked reactor. */
    public boolean tryBucketInteraction(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) {
            return false;
        }
        IFluidHandler handler = getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
        return net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, handler);
    }

    public void notifyLinkedReactorChanged(Player player) {
        Optional<FusionReactorBlockEntity> reactor = findLinkedReactor();
        linkedController = reactor.map(FusionReactorBlockEntity::getBlockPos);
        linked = reactor.isPresent();
        reactor.ifPresent(r -> r.requestStructureRevalidation(player));
        markDirtyAndSync();
    }

    public void setLinkedController(BlockPos controllerPos) {
        linkedController = Optional.of(controllerPos);
        linked = true;
        markDirtyAndSync();
    }

    public void clearLinkedController(BlockPos controllerPos) {
        if (linkedController.map(controllerPos::equals).orElse(false)) {
            linkedController = Optional.empty();
            linked = false;
            markDirtyAndSync();
        }
    }

    private ReactorFluidPortMode getMode() {
        return getBlockState().getValue(FusionFluidPortBlock.MODE);
    }

    private Optional<FusionReactorBlockEntity> findLinkedReactor() {
        if (level == null) {
            return Optional.empty();
        }
        if (level.isClientSide) {
            return Optional.empty();
        }
        Optional<FusionReactorBlockEntity> cached = getCachedLinkedReactor();
        if (cached.isPresent()) {
            linked = true;
            return cached;
        }
        Optional<FusionReactorBlockEntity> discovered = FusionReactorManager.getReactorsInRange(level, getBlockPos(), FusionStructureValidator.MAX_SIZE + 2)
                .stream()
                .filter(reactor -> reactor.getStructure().map(s -> s.containsFluidPort(getBlockPos())).orElse(false))
                .sorted(Comparator.comparingLong(reactor -> reactor.getBlockPos().asLong()))
                .findFirst();
        if (discovered.isEmpty()) {
            discovered = scanNearbyControllers();
        }
        if (discovered.isPresent()) {
            linkedController = discovered.map(FusionReactorBlockEntity::getBlockPos);
            linked = true;
        } else {
            linked = false;
        }
        return discovered;
    }

    private Optional<FusionReactorBlockEntity> getCachedLinkedReactor() {
        if (level == null || linkedController.isEmpty()) {
            return Optional.empty();
        }
        if (level.getBlockEntity(linkedController.get()) instanceof FusionReactorBlockEntity reactor
                && reactor.getStructure().map(s -> s.containsFluidPort(getBlockPos())).orElse(false)) {
            return Optional.of(reactor);
        }
        return Optional.empty();
    }

    private Optional<FusionReactorBlockEntity> scanNearbyControllers() {
        if (level == null || level.isClientSide) {
            return Optional.empty();
        }
        int range = FusionStructureValidator.MAX_SIZE + 2;
        BlockPos min = getBlockPos().offset(-range, -range, -range);
        BlockPos max = getBlockPos().offset(range, range, range);
        FusionReactorBlockEntity best = null;
        for (BlockPos candidate : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(candidate) instanceof FusionReactorBlockEntity reactor
                    && reactor.getStructure().map(s -> s.containsFluidPort(getBlockPos())).orElse(false)
                    && (best == null || reactor.getBlockPos().asLong() < best.getBlockPos().asLong())) {
                best = reactor;
            }
        }
        return Optional.ofNullable(best);
    }

    private void rescanLinkedReactor() {
        Optional<BlockPos> previousController = linkedController;
        boolean wasLinked = linked;
        Optional<FusionReactorBlockEntity> reactor = findLinkedReactor();
        if (reactor.isPresent()) {
            linkedController = reactor.map(FusionReactorBlockEntity::getBlockPos);
            linked = true;
        } else {
            linked = false;
        }
        if (!previousController.equals(linkedController) || wasLinked != linked) {
            markDirtyAndSync();
        }
    }

    private String linkStatus(boolean activeLink) {
        if (activeLink) {
            return "Linked";
        }
        if (linkedController.isPresent()) {
            return "Structure incomplete";
        }
        return "No reactor";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.literal("Fusion Fluid Port").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Mode: " + (mode == ReactorFluidPortMode.INPUT ? "Coolant input" : "Plasma steam output"))
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Connect pipes to any side")
                .withStyle(ChatFormatting.GRAY));
        boolean activeLink = level != null && level.isClientSide ? linked : findLinkedReactor().isPresent();
        tooltip.add(Component.literal("  Status: " + linkStatus(activeLink))
                .withStyle(activeLink ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(getBlockPos());
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        linkedController.ifPresent(controllerPos -> tag.putLong("linkedController", controllerPos.asLong()));
        tag.putBoolean("linked", linked);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("linkedController")) {
            linkedController = Optional.of(BlockPos.of(tag.getLong("linkedController")));
            linked = tag.getBoolean("linked") || linkedController.isPresent();
        } else {
            linkedController = Optional.empty();
            linked = false;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        linkedController.ifPresent(controllerPos -> tag.putLong("linkedController", controllerPos.asLong()));
        tag.putBoolean("linked", linked);
        tag.putString("mode", getMode().name());
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
        if (tag.contains("linkedController")) {
            linkedController = Optional.of(BlockPos.of(tag.getLong("linkedController")));
        } else {
            linkedController = Optional.empty();
        }
        linked = tag.getBoolean("linked");
    }

    private class PortFluidHandler implements IFluidHandler {
        private final ReactorFluidPortMode handlerMode;

        private PortFluidHandler(ReactorFluidPortMode handlerMode) {
            this.handlerMode = handlerMode;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return findLinkedReactor()
                    .map(reactor -> handlerMode == ReactorFluidPortMode.INPUT ? reactor.getCoolantFluid() : reactor.getPlasmaSteamFluid())
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return findLinkedReactor().map(FusionReactorBlockEntity::getTankCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return handlerMode == ReactorFluidPortMode.INPUT
                    ? NuclearFluidHelper.isCoolant(stack)
                    : NuclearFluidHelper.isPlasmaSteam(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.INPUT || !NuclearFluidHelper.isCoolant(resource)) {
                return 0;
            }
            return findLinkedReactor().map(reactor -> reactor.fillCoolant(resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT || !NuclearFluidHelper.isPlasmaSteam(resource)) {
                return FluidStack.EMPTY;
            }
            return findLinkedReactor().map(reactor -> reactor.drainPlasmaSteam(resource, action)).orElse(FluidStack.EMPTY);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT) {
                return FluidStack.EMPTY;
            }
            return findLinkedReactor().map(reactor -> reactor.drainPlasmaSteam(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
