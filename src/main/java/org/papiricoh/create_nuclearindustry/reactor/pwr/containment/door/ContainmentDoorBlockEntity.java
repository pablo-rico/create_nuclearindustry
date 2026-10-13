package org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The lower half of a containment door: it carries the opening animation (a progress from 0 to 1
 * chasing the door's OPEN state, as Create's sliding doors do) and puts the block model back once
 * the door has shut again.
 */
public class ContainmentDoorBlockEntity extends SmartBlockEntity {
    final LerpedFloat animation;
    /** Set when placed, so the first neighbour update does not read the placing click as redstone. */
    boolean deferUpdate;
    /** Client: a couple of ticks of overlap so the animated door and the block model never both vanish. */
    int bridgeTicks;

    public ContainmentDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        animation = LerpedFloat.linear().startWithValue(isOpen(state) ? 1 : 0);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        if (deferUpdate && level != null && !level.isClientSide) {
            deferUpdate = false;
            getBlockState().handleNeighborChanged(level, worldPosition, Blocks.AIR, worldPosition, false);
        }
        super.tick();
        BlockState state = getBlockState();
        boolean open = isOpen(state);
        boolean wasSettled = animation.settled();
        animation.chase(open ? 1 : 0, ContainmentDoorMotion.speed(), LerpedFloat.Chaser.LINEAR);
        animation.tickChaser();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (bridgeTicks < 2 && open) {
                bridgeTicks++;
            } else if (bridgeTicks > 0 && !open && isVisible(state)) {
                bridgeTicks--;
            }
            return;
        }
        if (!open && !wasSettled && animation.settled() && !isVisible(state)) {
            level.setBlock(worldPosition, state.setValue(ContainmentDoorBlock.VISIBLE, true), Block.UPDATE_ALL);
            // The door seats in its frame with a heavy thud.
            level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.5f);
        }
    }

    /** Opening progress, 0 shut to 1 open, between ticks. */
    public float progress(float partialTicks) {
        return animation.getValue(partialTicks);
    }

    /** Whether the renderer must draw the door instead of the block model. */
    public boolean shouldRenderAnimated() {
        return !isVisible(getBlockState()) || bridgeTicks != 0;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(2);
    }

    private static boolean isVisible(BlockState state) {
        return state.getOptionalValue(ContainmentDoorBlock.VISIBLE).orElse(true);
    }

    private static boolean isOpen(BlockState state) {
        return state.getOptionalValue(DoorBlock.OPEN).orElse(false);
    }
}
