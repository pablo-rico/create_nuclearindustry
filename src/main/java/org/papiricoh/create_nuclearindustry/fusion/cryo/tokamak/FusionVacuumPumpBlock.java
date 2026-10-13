package org.papiricoh.create_nuclearindustry.fusion.cryo.tokamak;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;

/**
 * Turbomolecular vacuum pump in the tokamak shell: a shaft on its outward face ({@link #FACING})
 * spins the rotor that holds the plasma chamber's vacuum.
 */
public class FusionVacuumPumpBlock extends KineticBlock implements IBE<FusionVacuumPumpBlockEntity>, TokamakShellBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final MapCodec<FusionVacuumPumpBlock> CODEC = simpleCodec(FusionVacuumPumpBlock::new);

    public FusionVacuumPumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FusionVacuumPumpBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    /** The shaft side faces the player, who builds the shell from outside. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Class<FusionVacuumPumpBlockEntity> getBlockEntityClass() {
        return FusionVacuumPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FusionVacuumPumpBlockEntity> getBlockEntityType() {
        return CryoRegistry.FUSION_VACUUM_PUMP_ENTITY.get();
    }

    @Override
    public TokamakShellRole tokamakRole(BlockState state) {
        return TokamakShellRole.VACUUM_PUMP;
    }
}
