package org.papiricoh.create_nuclearindustry.isotope.irradiator;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;

/**
 * The Gamma Irradiator's lead head on four legs. It works two blocks above a belt or a depot, like
 * Create's press. Right-click with a cobalt-60 source to load it; sneak with an empty hand to take
 * the weakest source back.
 */
public class GammaIrradiatorBlock extends BaseEntityBlock implements IBE<GammaIrradiatorBlockEntity> {
    public static final MapCodec<GammaIrradiatorBlock> CODEC = simpleCodec(GammaIrradiatorBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 13, 0, 16, 16, 16), Block.box(3, 1, 3, 13, 13, 13),
            Block.box(1, 0, 1, 3, 13, 3), Block.box(13, 0, 1, 15, 13, 3), Block.box(1, 0, 13, 3, 13, 15), Block.box(13, 0, 13, 15, 13, 15));

    public GammaIrradiatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(IsotopeRegistry.COBALT_60.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, irradiator -> irradiator.insertSource(player, hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, irradiator -> irradiator.takeSource(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, GammaIrradiatorBlockEntity::dropSources);
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(GammaIrradiatorBlockEntity::comparatorSignal).orElse(0);
    }

    @Override
    public Class<GammaIrradiatorBlockEntity> getBlockEntityClass() {
        return GammaIrradiatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GammaIrradiatorBlockEntity> getBlockEntityType() {
        return IsotopeRegistry.GAMMA_IRRADIATOR_ENTITY.get();
    }
}
