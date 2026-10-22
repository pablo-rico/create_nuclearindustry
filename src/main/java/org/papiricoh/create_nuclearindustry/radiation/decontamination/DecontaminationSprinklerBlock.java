package org.papiricoh.create_nuclearindustry.radiation.decontamination;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Washes fallout off the ground around it with piped water. Driven by a shaft from below; the spray head turns on top. */
public class DecontaminationSprinklerBlock extends KineticBlock implements IBE<DecontaminationSprinklerBlockEntity> {
    public static final MapCodec<DecontaminationSprinklerBlock> CODEC = simpleCodec(DecontaminationSprinklerBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 12, 16), Block.box(5, 12, 5, 11, 15, 11));

    public DecontaminationSprinklerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DecontaminationSprinklerBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
    public Class<DecontaminationSprinklerBlockEntity> getBlockEntityClass() {
        return DecontaminationSprinklerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DecontaminationSprinklerBlockEntity> getBlockEntityType() {
        return DecontaminationRegistry.SPRINKLER_ENTITY.get();
    }
}
