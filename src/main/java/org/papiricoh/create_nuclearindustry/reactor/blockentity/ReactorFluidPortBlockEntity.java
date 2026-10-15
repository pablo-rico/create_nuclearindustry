package org.papiricoh.create_nuclearindustry.reactor.blockentity;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
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
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ShutdownState;

import java.util.List;
import java.util.Optional;

public class ReactorFluidPortBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final IFluidHandler inputHandler = new PortFluidHandler(ReactorFluidPortMode.INPUT);
    private final IFluidHandler outputHandler = new PortFluidHandler(ReactorFluidPortMode.OUTPUT);
    private final ControllerLink<ReactorBlockEntity> link = new ControllerLink<>(this, ReactorBlockEntity.class,
            (reactor, pos) -> reactor.getStructure().map(structure -> structure.containsFluidPort(pos)).orElse(false));

    public ReactorFluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.REACTOR_FLUID_PORT.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        Optional<BlockPos> before = link.linkedPos();
        if (link.tick() && !before.equals(link.linkedPos())) {
            syncLink();
        }
    }

    /**
     * Sends the linked controller to clients: their goggles find the reactor through it (the multiblock
     * index only lives on the server), to show its tanks and whether the port bleeds.
     */
    private void syncLink() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        link.linkedPos().ifPresent(pos -> tag.putLong("Controller", pos.asLong()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Controller")) {
            link.link(BlockPos.of(tag.getLong("Controller")));
        } else {
            link.linkedPos().ifPresent(link::unlink);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        if (direction != getBlockState().getValue(ReactorFluidPortBlock.FACING)) {
            return null;
        }
        return getMode() == ReactorFluidPortMode.INPUT ? inputHandler : outputHandler;
    }

    public void notifyLinkedReactorChanged(Player player) {
        link.refresh().ifPresent(reactor -> reactor.requestStructureRevalidation(player));
        setChanged();
    }

    public void setLinkedController(BlockPos controllerPos) {
        if (link.link(controllerPos)) {
            syncLink();
        }
    }

    public void clearLinkedController(BlockPos controllerPos) {
        if (link.unlink(controllerPos)) {
            syncLink();
        }
    }

    private ReactorFluidPortMode getMode() {
        return getBlockState().getValue(ReactorFluidPortBlock.MODE);
    }

    private Optional<ReactorBlockEntity> findLinkedReactor() {
        return link.find();
    }

    private Component linkStatus() {
        return ReactorBlockEntity.portLinkStatus(findLinkedReactor().isPresent(), link.linkedPos().isPresent());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ReactorFluidPortMode mode = getMode();
        tooltip.add(Component.translatable("block.create_nuclearindustry.reactor_fluid_port").withStyle(ChatFormatting.GOLD));
        tooltip.add(ReactorBlockEntity.portLine("mode", mode == ReactorFluidPortMode.INPUT ? ChatFormatting.AQUA : ChatFormatting.YELLOW,
                ReactorBlockEntity.portMode(mode)));
        tooltip.add(ReactorBlockEntity.portLine("connect_pump", ChatFormatting.GRAY,
                ReactorBlockEntity.portSide(getBlockState().getValue(ReactorFluidPortBlock.FACING))));
        tooltip.add(ReactorBlockEntity.portLine("status", findLinkedReactor().isPresent() ? ChatFormatting.GREEN : ChatFormatting.RED,
                linkStatus()));
        findLinkedReactor().ifPresent(reactor -> {
            tooltip.add(ReactorBlockEntity.goggleLine("coolant", ChatFormatting.GRAY, reactor.getCoolantAmount(), reactor.getTankCapacity()));
            tooltip.add(ReactorBlockEntity.goggleLine("steam", ChatFormatting.GRAY, reactor.getSteamAmount(), reactor.getTankCapacity()));
            if (mode == ReactorFluidPortMode.OUTPUT) {
                bleedLine(tooltip, reactor);
            }
            if (reactor.plantReadings().pwr()) {
                tooltip.add(Component.literal("  ").append(Component.translatable(mode == ReactorFluidPortMode.INPUT
                        ? "create_nuclearindustry.pwr.port.cold_leg" : "create_nuclearindustry.pwr.port.hot_leg")).withStyle(ChatFormatting.AQUA));
            }
        });
        return true;
    }

    /**
     * Whether an output port bleeds the coolant tank: bleeding now (contaminated water from a damaged
     * core), ready to once the steam runs out, held while the chain reaction runs, or turned off.
     */
    private static void bleedLine(List<Component> tooltip, ReactorBlockEntity reactor) {
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = reactor.getDisplaySnapshot();
        String state;
        ChatFormatting color;
        if (!FissionConfig.bleed) {
            state = "disabled";
            color = ChatFormatting.DARK_GRAY;
        } else if (snapshot.bleeding()) {
            boolean contaminated = FissionConfig.contaminatedDamage > 0 && snapshot.coreDamage() >= FissionConfig.contaminatedDamage;
            state = contaminated ? "contaminated" : "active";
            color = contaminated ? ChatFormatting.RED : ChatFormatting.AQUA;
        } else if (snapshot.shutdownState() == ShutdownState.OPERATING) {
            state = "running";
            color = ChatFormatting.GRAY;
        } else {
            state = "ready";
            color = ChatFormatting.GREEN;
        }
        tooltip.add(ReactorBlockEntity.portLine("bleed", color, Component.translatable(ReactorBlockEntity.PORT + "bleed." + state)));
    }

    private class PortFluidHandler implements IFluidHandler {
        private final ReactorFluidPortMode handlerMode;

        private PortFluidHandler(ReactorFluidPortMode handlerMode) {
            this.handlerMode = handlerMode;
        }

        /**
         * An output port shows two tanks: what it gives first (steam, or the bled water once there is no
         * steam) and, while there is steam, the water it could bleed, so a pipe already full of bled
         * water finds it and keeps bleeding.
         */
        @Override
        public int getTanks() {
            return handlerMode == ReactorFluidPortMode.INPUT ? 1 : 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return findLinkedReactor().map(reactor -> {
                if (handlerMode == ReactorFluidPortMode.INPUT) {
                    return tank == 0 ? reactor.getCoolantFluid() : FluidStack.EMPTY;
                }
                if (tank == 0) {
                    return reactor.getOutputFluid();
                }
                return tank == 1 && reactor.getSteamAmount() > 0 ? reactor.getBleedFluid() : FluidStack.EMPTY;
            }).orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return findLinkedReactor().map(ReactorBlockEntity::getTankCapacity).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return handlerMode == ReactorFluidPortMode.INPUT
                    && findLinkedReactor().map(reactor -> reactor.acceptsCoolant(stack)).orElse(NuclearFluidHelper.isCoolant(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Water bled hot from a reactor is never taken back: it has to cool in a tower first.
            if (handlerMode != ReactorFluidPortMode.INPUT || !NuclearFluidHelper.isReactorCoolant(resource)
                    && !NuclearFluidHelper.isHotPrimary(resource) && !NuclearFluidHelper.isBoricAcid(resource)) {
                return 0;
            }
            return findLinkedReactor().map(reactor -> reactor.fillCoolant(resource, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT
                    || !NuclearFluidHelper.isReactorOutput(resource) && !NuclearFluidHelper.isReactorBleed(resource)) {
                return FluidStack.EMPTY;
            }
            return findLinkedReactor().map(reactor -> reactor.drainOutput(resource, action)).orElse(FluidStack.EMPTY);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (handlerMode != ReactorFluidPortMode.OUTPUT) {
                return FluidStack.EMPTY;
            }
            return findLinkedReactor().map(reactor -> reactor.drainOutput(maxDrain, action)).orElse(FluidStack.EMPTY);
        }
    }
}
