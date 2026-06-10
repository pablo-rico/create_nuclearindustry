package org.papiricoh.create_nuclearindustry.fusion.blockentity;

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
            linkedController = findLinkedReactor().map(FusionReactorBlockEntity::getBlockPos);
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        if (direction != getBlockState().getValue(FusionFluidPortBlock.FACING)) {
            return null;
        }
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
    }

    public void notifyLinkedReactorChanged(Player player) {
        Optional<FusionReactorBlockEntity> reactor = findLinkedReactor();
        linkedController = reactor.map(FusionReactorBlockEntity::getBlockPos);
        reactor.ifPresent(r -> r.requestStructureRevalidation(player));
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
        return getBlockState().getValue(FusionFluidPortBlock.MODE);
    }

    private Optional<FusionReactorBlockEntity> findLinkedReactor() {
        if (level == null) {
            return Optional.empty();
        }
        if (linkedController.isPresent()
                && level.getBlockEntity(linkedController.get()) instanceof FusionReactorBlockEntity reactor
                && reactor.getStructure().map(s -> s.containsFluidPort(getBlockPos())).orElse(false)) {
            return Optional.of(reactor);
        }
        linkedController = Optional.empty();
        return FusionReactorManager.getReactorsInRange(level, getBlockPos(), FusionStructureValidator.MAX_SIZE)
                .stream()
                .filter(reactor -> reactor.getStructure().map(s -> s.containsFluidPort(getBlockPos())).orElse(false))
                .sorted(Comparator.comparingLong(reactor -> reactor.getBlockPos().asLong()))
                .findFirst();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.literal("Fusion Fluid Port").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Mode: " + (mode == ReactorFluidPortMode.INPUT ? "Coolant input" : "Plasma steam output"))
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Connect on: " + getBlockState().getValue(FusionFluidPortBlock.FACING).getName().toUpperCase())
                .withStyle(ChatFormatting.GRAY));
        boolean linked = findLinkedReactor().isPresent();
        tooltip.add(Component.literal("  Status: " + (linked ? "Linked" : "No reactor"))
                .withStyle(linked ? ChatFormatting.GREEN : ChatFormatting.RED));
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
                    .map(reactor -> handlerMode == ReactorFluidPortMode.INPUT ? reactor.getCoolantFluid() : reactor.getPlasmaSteamFluid())
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return findLinkedReactor().map(FusionReactorBlockEntity::getTankCapacity).orElse(0);
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
