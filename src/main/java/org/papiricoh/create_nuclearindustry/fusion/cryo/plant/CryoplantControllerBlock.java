package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;

/** Sits in the middle of a side wall of the cryogenic plant, facing out. Right-click to form it; it lights up while running. */
public class CryoplantControllerBlock extends HorizontalDirectionalBlock implements IBE<CryoplantControllerBlockEntity> {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    public static final MapCodec<CryoplantControllerBlock> CODEC = simpleCodec(CryoplantControllerBlock::new);

    public CryoplantControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    /** Faces the player, who places it from outside. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, controller -> controller.attemptFormation(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<CryoplantControllerBlockEntity> getBlockEntityClass() {
        return CryoplantControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CryoplantControllerBlockEntity> getBlockEntityType() {
        return CryoRegistry.CRYOPLANT_CONTROLLER_ENTITY.get();
    }
}
