package org.papiricoh.create_nuclearindustry.reactor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One rotor stage. Inside a formed turbine it turns invisible ({@link #FORMED}) and the
 * controller renders the whole rotor spinning.
 */
public class TurbineRotorBlock extends RotatedPillarBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 2, 2, 14, 14, 14),
            Block.box(0, 6, 6, 16, 10, 10),
            Block.box(6, 0, 6, 10, 16, 10),
            Block.box(6, 6, 0, 10, 10, 16)
    ).optimize();

    public TurbineRotorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(FORMED);
    }

    /** Safety net: a rotor left marked formed (turbine gone while unloaded, contraption) shows again. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean claimed = MultiblockRegistry.affecting(level, pos, AbstractTurbineControllerBlockEntity.class).stream()
                .anyMatch(controller -> controller.getStructure().map(s -> s.rotors.contains(pos)).orElse(false));
        if (!claimed) {
            level.setBlock(pos, state.setValue(FORMED, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        return defaultBlockState().setValue(AXIS, clickedFace.getAxis()).setValue(FORMED, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
