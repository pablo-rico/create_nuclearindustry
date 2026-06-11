package org.papiricoh.create_nuclearindustry.fusion.blockentity;

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
import org.papiricoh.create_nuclearindustry.fusion.block.FusionTurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.List;
import java.util.Optional;

public class FusionTurbineFluidPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final int LINK_SCAN_INTERVAL = 20;
    private static final int SEARCH_RADIUS = 4;

    private final IFluidHandler inputHandler = new PortFluidHandler(ReactorFluidPortMode.INPUT);
    private final IFluidHandler outputHandler = new PortFluidHandler(ReactorFluidPortMode.OUTPUT);
    private Optional<BlockPos> linkedTurbine = Optional.empty();
    private int linkScanCounter;

    public FusionTurbineFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_TURBINE_FLUID_PORT.get(), pos, state);
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
        Optional<BlockPos> previous = linkedTurbine;
        linkedTurbine = findLinkedTurbine().map(FusionPlasmaTurbineBlockEntity::getBlockPos);
        setChanged();
        if (level != null && !level.isClientSide && !previous.equals(linkedTurbine)) {
            level.invalidateCapabilities(getBlockPos());
        }
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
    }

    private ReactorFluidPortMode getMode() {
        return getBlockState().getValue(FusionTurbineFluidPortBlock.MODE);
    }

    private Optional<FusionPlasmaTurbineBlockEntity> findLinkedTurbine() {
        if (level == null) {
            return Optional.empty();
        }
        if (linkedTurbine.isPresent()
                && level.getBlockEntity(linkedTurbine.get()) instanceof FusionPlasmaTurbineBlockEntity turbine
                && turbine.isPortAttached(getBlockPos())) {
            return Optional.of(turbine);
        }
        linkedTurbine = Optional.empty();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos candidate = getBlockPos().offset(x, y, z);
                    if (level.getBlockEntity(candidate) instanceof FusionPlasmaTurbineBlockEntity turbine
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
        if (linkedTurbine.isPresent()) {
            return "Structure incomplete";
        }
        return "No turbine";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.literal("Fusion Turbine Fluid Port").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Mode: " + (mode == ReactorFluidPortMode.INPUT ? "Plasma steam input" : "Condensate output"))
                .withStyle(mode == ReactorFluidPortMode.INPUT ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Connect pipes to any side")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  Status: " + linkStatus())
                .withStyle(findLinkedTurbine().isPresent() ? ChatFormatting.GREEN : ChatFormatting.RED));
        findLinkedTurbine().ifPresent(turbine -> {
            tooltip.add(Component.literal("  Plasma steam: " + turbine.getPlasmaSteamAmount() + "/" + turbine.getTankCapacity() + " mB").withStyle(ChatFormatting.GRAY));
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
                    .map(turbine -> handlerMode == ReactorFluidPortMode.INPUT ? turbine.getPlasmaSteamFluid() : turbine.getCondensateFluid())
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return findLinkedTurbine().map(FusionPlasmaTurbineBlockEntity::getTankCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return handlerMode == ReactorFluidPortMode.INPUT && NuclearFluidHelper.isPlasmaTurbineFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.INPUT || !NuclearFluidHelper.isPlasmaTurbineFluid(resource)) {
                return 0;
            }
            return findLinkedTurbine().map(turbine -> turbine.fillPlasmaSteam(resource, action)).orElse(0);
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
