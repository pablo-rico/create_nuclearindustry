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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.fusion.event.FusionReactorManager;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.FusionStructureValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Item input port that feeds D-T fuel pellets to the linked controller's fuel inventory and lets
 * spent pellets be extracted.
 */
public class FusionFuelInjectorBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int LINK_SCAN_INTERVAL = 20;

    private final IItemHandler handler = new InjectorHandler();
    private Optional<BlockPos> linkedController = Optional.empty();
    private boolean linked;
    private int linkScanCounter;

    public FusionFuelInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_FUEL_INJECTOR.get(), pos, state);
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

    public IItemHandler getItemHandler(Direction direction) {
        // Flat ring geometry: funnels/hoppers may sit on any side, so expose on every face.
        return handler;
    }

    public boolean hasLinkedReactor() {
        return findLinkedReactor().isPresent();
    }

    /** Right-click insertion: pushes one held D-T pellet into the linked reactor's fuel input. */
    public boolean insertFuelByHand(ItemStack held) {
        if (level == null || level.isClientSide || !held.is(AllNuclearItems.DT_FUEL_PELLET.get())) {
            return false;
        }
        return findLinkedReactor().map(reactor -> {
            ItemStack remainder = reactor.getFuelInventory()
                    .insertItem(FusionReactorBlockEntity.FUEL_INPUT_SLOT, held.copyWithCount(1), false);
            if (remainder.isEmpty()) {
                held.shrink(1);
                markDirtyAndSync();
                return true;
            }
            return false;
        }).orElse(false);
    }

    /** Right-click (empty hand): pulls spent pellets out of the linked reactor's output slot. */
    public ItemStack extractSpentByHand() {
        if (level == null || level.isClientSide) {
            return ItemStack.EMPTY;
        }
        return findLinkedReactor()
                .map(reactor -> reactor.getFuelInventory().extractItem(FusionReactorBlockEntity.FUEL_OUTPUT_SLOT, 64, false))
                .orElse(ItemStack.EMPTY);
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
                .filter(reactor -> reactor.getStructure().map(s -> s.containsFuelInjector(getBlockPos())).orElse(false))
                .sorted(Comparator.comparingLong(reactor -> reactor.getBlockPos().asLong()))
                .findFirst();
        if (discovered.isEmpty()) {
            discovered = scanNearbyControllers();
        }
        linkedController = discovered.map(FusionReactorBlockEntity::getBlockPos);
        linked = discovered.isPresent();
        return discovered;
    }

    private Optional<FusionReactorBlockEntity> getCachedLinkedReactor() {
        if (level == null || linkedController.isEmpty()) {
            return Optional.empty();
        }
        if (level.getBlockEntity(linkedController.get()) instanceof FusionReactorBlockEntity reactor
                && reactor.getStructure().map(s -> s.containsFuelInjector(getBlockPos())).orElse(false)) {
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
                    && reactor.getStructure().map(s -> s.containsFuelInjector(getBlockPos())).orElse(false)
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
        linkedController = reactor.map(FusionReactorBlockEntity::getBlockPos);
        linked = reactor.isPresent();
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
        tooltip.add(Component.literal("Fusion Fuel Injector").withStyle(ChatFormatting.AQUA));
        boolean activeLink = level != null && level.isClientSide ? linked : findLinkedReactor().isPresent();
        tooltip.add(Component.literal("  Status: " + linkStatus(activeLink))
                .withStyle(activeLink ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("  Accepts: D-T Fuel Pellet").withStyle(ChatFormatting.GRAY));
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

    private class InjectorHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return findLinkedReactor().map(r -> r.getFuelInventory().getStackInSlot(slot)).orElse(ItemStack.EMPTY);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != FusionReactorBlockEntity.FUEL_INPUT_SLOT || !stack.is(AllNuclearItems.DT_FUEL_PELLET.get())) {
                return stack;
            }
            return findLinkedReactor().map(r -> r.getFuelInventory().insertItem(slot, stack, simulate)).orElse(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != FusionReactorBlockEntity.FUEL_OUTPUT_SLOT) {
                return ItemStack.EMPTY;
            }
            return findLinkedReactor().map(r -> r.getFuelInventory().extractItem(slot, amount, simulate)).orElse(ItemStack.EMPTY);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FusionReactorBlockEntity.FUEL_INPUT_SLOT && stack.is(AllNuclearItems.DT_FUEL_PELLET.get());
        }
    }
}
