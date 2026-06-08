package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;

import java.util.List;
import java.util.Optional;

public class TurbineFluidPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int LINK_SCAN_INTERVAL = 20;
    private static final int SEARCH_RADIUS = 4;

    private final IFluidHandler inputHandler = new PortFluidHandler(ReactorFluidPortMode.INPUT);
    private final IFluidHandler outputHandler = new PortFluidHandler(ReactorFluidPortMode.OUTPUT);
    private Optional<BlockPos> linkedOutput = Optional.empty();
    private int linkScanCounter;

    public TurbineFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.TURBINE_FLUID_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++linkScanCounter >= LINK_SCAN_INTERVAL) {
            linkScanCounter = 0;
            refreshLink();
        }
    }

    public void refreshLink() {
        linkedOutput = findLinkedTurbine().map(TurbineOutputBlockEntity::getBlockPos);
        setChanged();
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        if (direction != getBlockState().getValue(TurbineFluidPortBlock.FACING)) {
            return null;
        }
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
    }

    private ReactorFluidPortMode getMode() {
        return getBlockState().getValue(TurbineFluidPortBlock.MODE);
    }

    private Optional<TurbineOutputBlockEntity> findLinkedTurbine() {
        if (level == null) {
            return Optional.empty();
        }
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos candidate = getBlockPos().offset(x, y, z);
                    if (level.getBlockEntity(candidate) instanceof TurbineOutputBlockEntity turbine
                            && turbine.isPortAttached(getBlockPos())) {
                        return Optional.of(turbine);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private String linkStatus() {
        if (findLinkedTurbine().isPresent()) {
            return "Linked";
        }
        if (linkedOutput.isPresent()) {
            return "Structure incomplete";
        }
        return "No turbine";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.literal("Turbine Fluid Port").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Mode: " + (mode == ReactorFluidPortMode.INPUT ? "Steam Input" : "Condensate Output"))
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.YELLOW : ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Connect pump on: " + getBlockState().getValue(TurbineFluidPortBlock.FACING).getName().toUpperCase())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Status: " + linkStatus())
                .withStyle(findLinkedTurbine().isPresent() ? ChatFormatting.GREEN : ChatFormatting.RED));
        findLinkedTurbine().ifPresent(turbine -> {
            tooltip.add(Component.literal("  Steam: " + turbine.getSteamAmount() + "/" + turbine.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("  Condensate: " + turbine.getCondensateAmount() + "/" + turbine.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
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
            return findLinkedTurbine()
                    .map(turbine -> handlerMode == ReactorFluidPortMode.INPUT ? turbine.getSteamFluid() : turbine.getCondensateFluid())
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return findLinkedTurbine().map(TurbineOutputBlockEntity::getTankCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return handlerMode == ReactorFluidPortMode.INPUT && NuclearFluidHelper.isTurbineSteam(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.INPUT || !NuclearFluidHelper.isTurbineSteam(resource)) {
                return 0;
            }
            return findLinkedTurbine().map(turbine -> turbine.fillSteam(resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT || !NuclearFluidHelper.isCoolant(resource)) {
                return FluidStack.EMPTY;
            }
            return findLinkedTurbine().map(turbine -> turbine.drainCondensate(resource, action)).orElse(FluidStack.EMPTY);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT) {
                return FluidStack.EMPTY;
            }
            return findLinkedTurbine().map(turbine -> turbine.drainCondensate(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
