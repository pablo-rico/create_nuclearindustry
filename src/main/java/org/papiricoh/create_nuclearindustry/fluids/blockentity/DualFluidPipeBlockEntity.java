package org.papiricoh.create_nuclearindustry.fluids.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fluids.block.DualFluidPipeBlock;

public class DualFluidPipeBlockEntity extends BlockEntity {

    private static final int CHANNEL_CAPACITY = 8_000;
    private static final int PUSH_PER_TICK = 40;

    private int steam;
    private int heavyWater;

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

    public int fillSteam(int amount) {
        int accepted = Math.max(0, Math.min(amount, CHANNEL_CAPACITY - steam));
        if (accepted > 0) {
            steam += accepted;
            setChanged();
        }
        return accepted;
    }

    public int fillHeavyWater(int amount) {
        int accepted = Math.max(0, Math.min(amount, CHANNEL_CAPACITY - heavyWater));
        if (accepted > 0) {
            heavyWater += accepted;
            setChanged();
        }
        return accepted;
    }

    public int drainSteam(int amount) {
        int extracted = Math.max(0, Math.min(amount, steam));
        if (extracted > 0) {
            steam -= extracted;
            setChanged();
        }
        return extracted;
    }

    public int drainHeavyWater(int amount) {
        int extracted = Math.max(0, Math.min(amount, heavyWater));
        if (extracted > 0) {
            heavyWater -= extracted;
            setChanged();
        }
        return extracted;
    }

    public int getSteam() {
        return steam;
    }

    public int getHeavyWater() {
        return heavyWater;
    }

    private void pushToNeighbors(Level level, BlockPos pos) {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);

            if (!(neighborEntity instanceof DualFluidPipeBlockEntity neighborPipe)) {
                continue;
            }

            int steamSent = Math.min(PUSH_PER_TICK, steam);
            if (steamSent > 0) {
                steamSent = neighborPipe.fillSteam(steamSent);
                if (steamSent > 0) {
                    steam -= steamSent;
                    changed = true;
                }
            }

            int heavySent = Math.min(PUSH_PER_TICK, heavyWater);
            if (heavySent > 0) {
                heavySent = neighborPipe.fillHeavyWater(heavySent);
                if (heavySent > 0) {
                    heavyWater -= heavySent;
                    changed = true;
                }
            }
        }

        if (changed) {
            setChanged();
        }
    }

    private void updateVisualState(Level level, BlockPos pos, BlockState state) {
        DualFluidPipeBlock.VisualFluid newFluid = DualFluidPipeBlock.VisualFluid.NONE;
        if (steam > 0 && heavyWater > 0) {
            newFluid = DualFluidPipeBlock.VisualFluid.MIXED;
        } else if (steam > 0) {
            newFluid = DualFluidPipeBlock.VisualFluid.STEAM;
        } else if (heavyWater > 0) {
            newFluid = DualFluidPipeBlock.VisualFluid.HEAVY_WATER;
        }

        if (state.getValue(DualFluidPipeBlock.VISUAL_FLUID) != newFluid) {
            level.setBlock(pos, state.setValue(DualFluidPipeBlock.VISUAL_FLUID, newFluid), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("steam", steam);
        tag.putInt("heavy_water", heavyWater);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        steam = tag.getInt("steam");
        heavyWater = tag.getInt("heavy_water");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.putInt("steam", steam);
        tag.putInt("heavy_water", heavyWater);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        steam = tag.getInt("steam");
        heavyWater = tag.getInt("heavy_water");
    }
}

