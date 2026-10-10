package org.papiricoh.create_nuclearindustry.civildefense.siren;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;

/**
 * A mechanical siren with no power of its own: it sounds while its shaft (from below) turns at
 * {@code civilDefense.siren.crankMinSpeed} RPM or more, from a hand crank or any other source, and
 * rises in pitch the faster it turns. A comparator reads 15 from it while it sounds.
 */
public class HandCrankSirenBlock extends KineticBlock implements IBE<HandCrankSirenBlockEntity> {
    public static final MapCodec<HandCrankSirenBlock> CODEC = simpleCodec(HandCrankSirenBlock::new);
    /** Gearbox housing, rotor housing, rain cap and finial. */
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 4, 13), box(3, 4, 3, 13, 12, 13),
            box(2, 12, 2, 14, 14, 14), box(6, 14, 6, 10, 16, 10));

    public HandCrankSirenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AirRaidSirenBlock.SOUNDING, false));
    }

    @Override
    protected MapCodec<? extends HandCrankSirenBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AirRaidSirenBlock.SOUNDING);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(AirRaidSirenBlock.SOUNDING) ? 15 : 0;
    }

    @Override
    public Class<HandCrankSirenBlockEntity> getBlockEntityClass() {
        return HandCrankSirenBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HandCrankSirenBlockEntity> getBlockEntityType() {
        return CivilDefenseRegistry.HAND_CRANK_SIREN_BE.get();
    }
}
