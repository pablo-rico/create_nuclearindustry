package org.papiricoh.create_nuclearindustry.civildefense.blastdoor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;

/**
 * Part of a blast door's frame that drives it. Its shaft runs through the wall, across the door; a
 * shaft turning one way opens the door and the other way shuts it, faster the faster it turns.
 * Without rotation, a rising redstone signal opens it and a falling one shuts it. A comparator
 * reads how far open it is.
 */
public class BlastDoorControllerBlock extends HorizontalAxisKineticBlock implements IBE<BlastDoorControllerBlockEntity> {
    public static final MapCodec<BlastDoorControllerBlock> CODEC = simpleCodec(BlastDoorControllerBlock::new);

    public BlastDoorControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BlastDoorControllerBlock> codec() {
        return CODEC;
    }

    /** The shaft goes through the wall the player is looking at, unless a shaft beside it says otherwise. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis preferred = getPreferredHorizontalAxis(context);
        return defaultBlockState().setValue(HORIZONTAL_AXIS,
                preferred != null ? preferred : context.getHorizontalDirection().getAxis());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, BlastDoorControllerBlockEntity::requestValidation);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlastDoorControllerBlockEntity controller = getBlockEntity(level, pos);
        return controller == null ? 0 : controller.comparatorSignal();
    }

    @Override
    public Class<BlastDoorControllerBlockEntity> getBlockEntityClass() {
        return BlastDoorControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BlastDoorControllerBlockEntity> getBlockEntityType() {
        return CivilDefenseRegistry.BLAST_DOOR_CONTROLLER_BE.get();
    }
}
