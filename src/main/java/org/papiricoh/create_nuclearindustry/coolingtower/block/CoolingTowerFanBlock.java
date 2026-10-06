package org.papiricoh.create_nuclearindustry.coolingtower.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerFanBlockEntity;

/**
 * Forced-draft fan for the bottom of a cooling tower wall. Takes a shaft on its outward face;
 * the faster it turns, the more steam the tower condenses.
 */
public class CoolingTowerFanBlock extends KineticBlock implements IBE<CoolingTowerFanBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<CoolingTowerFanBlock> CODEC = simpleCodec(CoolingTowerFanBlock::new);

    public CoolingTowerFanBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends CoolingTowerFanBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    /** Faces the player, who places it from outside the tower. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<CoolingTowerFanBlockEntity> getBlockEntityClass() {
        return CoolingTowerFanBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CoolingTowerFanBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.COOLING_TOWER_FAN.get();
    }
}
