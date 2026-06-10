package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
            linkedController = findLinkedReactor().map(FusionReactorBlockEntity::getBlockPos);
        }
    }

    public IItemHandler getItemHandler(Direction direction) {
        return handler;
    }

    public void setLinkedController(BlockPos controllerPos) {
        linkedController = Optional.of(controllerPos);
        setChanged();
    }

    public void clearLinkedController(BlockPos controllerPos) {
        if (linkedController.map(controllerPos::equals).orElse(false)) {
            linkedController = Optional.empty();
            setChanged();
        }
    }

    private Optional<FusionReactorBlockEntity> findLinkedReactor() {
        if (level == null) {
            return Optional.empty();
        }
        if (linkedController.isPresent()
                && level.getBlockEntity(linkedController.get()) instanceof FusionReactorBlockEntity reactor
                && reactor.getStructure().map(s -> s.containsFuelInjector(getBlockPos())).orElse(false)) {
            return Optional.of(reactor);
        }
        linkedController = Optional.empty();
        return FusionReactorManager.getReactorsInRange(level, getBlockPos(), FusionStructureValidator.MAX_SIZE)
                .stream()
                .filter(reactor -> reactor.getStructure().map(s -> s.containsFuelInjector(getBlockPos())).orElse(false))
                .sorted(Comparator.comparingLong(reactor -> reactor.getBlockPos().asLong()))
                .findFirst();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("Fusion Fuel Injector").withStyle(ChatFormatting.AQUA));
        boolean linked = findLinkedReactor().isPresent();
        tooltip.add(Component.literal("  Status: " + (linked ? "Linked" : "No reactor"))
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.literal("  Accepts: D-T Fuel Pellet").withStyle(ChatFormatting.GRAY));
        return true;
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
