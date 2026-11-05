package org.papiricoh.create_nuclearindustry.dyson.receiver;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;

/**
 * A ray receiver: a plinth with a turning mount and a gold dish that follows the sun (drawn by its renderer), taking the Dyson
 * swarm's light and giving Forge Energy on every side. Its outline is the plinth; the dish overhangs it.
 */
public class RayReceiverBlock extends BaseEntityBlock implements IBE<RayReceiverBlockEntity> {
    public static final MapCodec<RayReceiverBlock> CODEC = simpleCodec(RayReceiverBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);

    public RayReceiverBlock(Properties properties) {
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
    public Class<RayReceiverBlockEntity> getBlockEntityClass() {
        return RayReceiverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RayReceiverBlockEntity> getBlockEntityType() {
        return DysonRegistry.RECEIVER_ENTITY.get();
    }
}
