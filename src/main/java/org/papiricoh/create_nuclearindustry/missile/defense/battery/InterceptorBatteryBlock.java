package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

/**
 * The interceptor battery's master block: the corner of its 2×2×2 footprint with the lowest coordinates, driven by a shaft from
 * below. Its block entity holds the four tubes and does the turning and launching; the other seven blocks are
 * {@link InterceptorBatteryPartBlock}s.
 */
public class InterceptorBatteryBlock extends KineticBlock implements EntityBlock {
    public static final MapCodec<InterceptorBatteryBlock> CODEC = simpleCodec(InterceptorBatteryBlock::new);
    /** The way the battery faced when placed: where its turret rests. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public InterceptorBatteryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends InterceptorBatteryBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }

    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) { return face == Direction.DOWN; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InterceptorBatteryEntity(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DefenseRegistry.BATTERY_ENTITY.get() ? (l, p, s, be) -> ((InterceptorBatteryEntity) be).tick() : null;
    }
}
