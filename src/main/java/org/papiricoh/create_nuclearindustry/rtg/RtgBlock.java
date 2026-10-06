package org.papiricoh.create_nuclearindustry.rtg;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;

/**
 * Radioisotope thermoelectric generator: a small, steady kinetic source fed with decaying
 * pellets. Its shaft comes out of the front face. Right-click with a pellet to load it;
 * right-click empty-handed to take out spent pellets.
 */
public class RtgBlock extends DirectionalKineticBlock implements IBE<RtgBlockEntity> {
    public static final MapCodec<RtgBlock> CODEC = simpleCodec(RtgBlock::new);

    public RtgBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RtgBlock> codec() {
        return CODEC;
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof RtgPelletItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, rtg -> player.setItemInHand(hand, rtg.getInventory().insertItem(RtgBlockEntity.INPUT_SLOT, stack, false)));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, rtg -> {
                ItemStack spent = rtg.getInventory().extractItem(RtgBlockEntity.OUTPUT_SLOT, 64, false);
                if (!spent.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(spent);
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Class<RtgBlockEntity> getBlockEntityClass() {
        return RtgBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RtgBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.RTG.get();
    }
}
