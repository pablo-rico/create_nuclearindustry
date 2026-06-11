package org.papiricoh.create_nuclearindustry.fluids.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fluids.block.DualFluidPipeBlock;

public class DualFluidPipeBlockEntity extends BlockEntity {

    private static final int CHANNEL_CAPACITY = 8_000;
    private static final int PUSH_PER_TICK = 40;

    private final FluidTank steamTank = new FluidTank(CHANNEL_CAPACITY, DualFluidPipeBlockEntity::isSteamChannelFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank coolantTank = new FluidTank(CHANNEL_CAPACITY, NuclearFluidHelper::isCoolant) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new DualPipeFluidHandler();

    public DualFluidPipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(AllNuclearEntities.DUAL_PIPE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DualFluidPipeBlockEntity entity) {
        if (level == null || level.isClientSide) {
            return;
        }

        entity.pushToNeighbors(level, pos);
        entity.updateVisualState(level, pos, state);
    }

    public IFluidHandler getFluidHandler(Direction direction) {
        return fluidHandler;
    }

    private static boolean isSteamChannelFluid(FluidStack stack) {
        return NuclearFluidHelper.isTurbineSteam(stack) || NuclearFluidHelper.isPlasmaSteam(stack);
    }

    private void pushToNeighbors(Level level, BlockPos pos) {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            IFluidHandler target = getNeighborFluidHandler(level, neighborPos, direction.getOpposite());
            if (target == null) {
                continue;
            }

            changed |= pushTank(steamTank, target);
            changed |= pushTank(coolantTank, target);
        }

        if (changed) {
            setChanged();
        }
    }

    private IFluidHandler getNeighborFluidHandler(Level level, BlockPos neighborPos, Direction side) {
        BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
        if (neighborEntity instanceof DualFluidPipeBlockEntity neighborPipe) {
            return neighborPipe.fluidHandler;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, side);
    }

    private boolean pushTank(FluidTank tank, IFluidHandler target) {
        FluidStack available = tank.drain(PUSH_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }

        int accepted = target.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return false;
        }

        accepted = Math.min(accepted, available.getAmount());
        FluidStack transfer = available.copyWithAmount(accepted);
        int inserted = target.fill(transfer, IFluidHandler.FluidAction.EXECUTE);
        if (inserted <= 0) {
            return false;
        }

        inserted = Math.min(inserted, accepted);
        tank.drain(inserted, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void updateVisualState(Level level, BlockPos pos, BlockState state) {
        boolean hasSteam = !steamTank.isEmpty();
        boolean hasCoolant = !coolantTank.isEmpty();

        DualFluidPipeBlock.VisualFluid newFluid = DualFluidPipeBlock.VisualFluid.NONE;
        if (hasSteam && hasCoolant) {
            newFluid = DualFluidPipeBlock.VisualFluid.MIXED;
        } else if (hasSteam) {
            newFluid = DualFluidPipeBlock.VisualFluid.STEAM;
        } else if (hasCoolant) {
            newFluid = NuclearFluidHelper.isHeavyWater(coolantTank.getFluid())
                    ? DualFluidPipeBlock.VisualFluid.HEAVY_WATER
                    : DualFluidPipeBlock.VisualFluid.WATER;
        }

        if (state.getValue(DualFluidPipeBlock.VISUAL_FLUID) != newFluid) {
            level.setBlock(pos, state.setValue(DualFluidPipeBlock.VISUAL_FLUID, newFluid), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("steam")) {
            steamTank.readFromNBT(provider, tag.getCompound("steam"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("steam", steamTank.writeToNBT(provider, new CompoundTag()));
        tag.put("coolant", coolantTank.writeToNBT(provider, new CompoundTag()));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        if (tag.contains("steam")) {
            steamTank.readFromNBT(provider, tag.getCompound("steam"));
        }
        if (tag.contains("coolant")) {
            coolantTank.readFromNBT(provider, tag.getCompound("coolant"));
        }
    }

    private class DualPipeFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return getTank(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return getTank(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return getTank(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (isSteamChannelFluid(resource)) {
                return steamTank.fill(resource, action);
            }
            if (NuclearFluidHelper.isCoolant(resource)) {
                return coolantTank.fill(resource, action);
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (isSteamChannelFluid(resource)) {
                return steamTank.drain(resource, action);
            }
            if (NuclearFluidHelper.isCoolant(resource)) {
                return coolantTank.drain(resource, action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = steamTank.drain(maxDrain, action);
            return drained.isEmpty() ? coolantTank.drain(maxDrain, action) : drained;
        }

        private FluidTank getTank(int tank) {
            return tank == 0 ? steamTank : coolantTank;
        }
    }
}
