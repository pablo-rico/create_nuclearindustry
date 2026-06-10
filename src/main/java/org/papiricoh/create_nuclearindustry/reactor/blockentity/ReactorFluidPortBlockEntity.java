package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.event.ReactorManager;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ReactorFluidPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int LINK_SCAN_INTERVAL = 20;

    private final IFluidHandler inputHandler = new PortFluidHandler(ReactorFluidPortMode.INPUT);
    private final IFluidHandler outputHandler = new PortFluidHandler(ReactorFluidPortMode.OUTPUT);
    private Optional<BlockPos> linkedController = Optional.empty();
    private int linkScanCounter;

    public ReactorFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR_FLUID_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++linkScanCounter >= LINK_SCAN_INTERVAL) {
            linkScanCounter = 0;
            linkedController = findLinkedReactor().map(ReactorBlockEntity::getBlockPos);
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        if (direction != getBlockState().getValue(ReactorFluidPortBlock.FACING)) {
            return null;
        }
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
    }

    public void notifyLinkedReactorChanged(Player player) {
        Optional<ReactorBlockEntity> reactor = findLinkedReactor();
        linkedController = reactor.map(ReactorBlockEntity::getBlockPos);
        reactor.ifPresent(linkedReactor -> linkedReactor.requestStructureRevalidation(player));
        setChanged();
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

    private ReactorFluidPortMode getMode() {
        return getBlockState().getValue(ReactorFluidPortBlock.MODE);
    }

    private Optional<ReactorBlockEntity> findLinkedReactor() {
        if (level == null) {
            return Optional.empty();
        }
        Optional<ReactorBlockEntity> cached = getCachedLinkedReactor();
        if (cached.isPresent()) {
            return cached;
        }
        return ReactorManager.getReactorsInRange(level, getBlockPos(), ReactorStructureValidator.MAX_WIDTH + ReactorStructureValidator.MAX_HEIGHT)
                .stream()
                .filter(reactor -> reactor.getStructure().map(structure -> structure.containsFluidPort(getBlockPos())).orElse(false))
                .sorted(Comparator.comparingLong(reactor -> reactor.getBlockPos().asLong()))
                .findFirst();
    }

    private Optional<ReactorBlockEntity> getCachedLinkedReactor() {
        if (level == null || linkedController.isEmpty()) {
            return Optional.empty();
        }
        if (level.getBlockEntity(linkedController.get()) instanceof ReactorBlockEntity reactor
                && reactor.getStructure().map(structure -> structure.containsFluidPort(getBlockPos())).orElse(false)) {
            return Optional.of(reactor);
        }
        linkedController = Optional.empty();
        return Optional.empty();
    }

    private String linkStatus() {
        Optional<ReactorBlockEntity> reactor = findLinkedReactor();
        if (reactor.isPresent()) {
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
        tooltip.add(Component.literal("Reactor Fluid Port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Mode: " + mode.displayName())
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Connect pump on: " + getBlockState().getValue(ReactorFluidPortBlock.FACING).getName().toUpperCase())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Status: " + linkStatus())
                .withStyle(findLinkedReactor().isPresent() ? ChatFormatting.GREEN : ChatFormatting.RED));
        findLinkedReactor().ifPresent(reactor -> {
            tooltip.add(Component.literal("  Coolant: " + reactor.getCoolantAmount() + "/" + reactor.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Steam: " + reactor.getSteamAmount() + "/" + reactor.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
        });
        return true;
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
                    .map(reactor -> handlerMode == ReactorFluidPortMode.INPUT ? reactor.getCoolantFluid() : reactor.getSteamFluid())
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return findLinkedReactor().map(ReactorBlockEntity::getTankCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return handlerMode == ReactorFluidPortMode.INPUT && NuclearFluidHelper.isCoolant(stack);
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
            if (handlerMode != ReactorFluidPortMode.OUTPUT || !NuclearFluidHelper.isTurbineSteam(resource)) {
                return FluidStack.EMPTY;
            }
            return findLinkedReactor().map(reactor -> reactor.drainSteam(resource, action)).orElse(FluidStack.EMPTY);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT) {
                return FluidStack.EMPTY;
            }
            return findLinkedReactor().map(reactor -> reactor.drainSteam(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
