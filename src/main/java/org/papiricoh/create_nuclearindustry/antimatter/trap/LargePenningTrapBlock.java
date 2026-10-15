package org.papiricoh.create_nuclearindustry.antimatter.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;

import java.util.ArrayList;
import java.util.List;

/**
 * The large Penning trap: 2x2 blocks and three high, placed as a unit ahead of and to the right of
 * the player, like a blast door is built as one. The master is the bottom north-west block (smallest
 * x, y and z). It has no facing: vial ports on its north and south sides, the cable entry east.
 */
public class LargePenningTrapBlock extends PenningTrapBlock {
    public static final EnumProperty<TrapCorner> CORNER = EnumProperty.create("corner", TrapCorner.class);
    public static final EnumProperty<TrapLevel> LEVEL = EnumProperty.create("level", TrapLevel.class);
    public static final int HEIGHT = 3;

    public LargePenningTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CORNER, TrapCorner.NW).setValue(LEVEL, TrapLevel.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORNER, LEVEL);
    }

    /** The master position of a trap placed at {@code clicked} by someone looking {@code facing}. */
    public static BlockPos originFor(BlockPos clicked, Direction facing) {
        BlockPos ahead = clicked.relative(facing);
        BlockPos right = clicked.relative(facing.getClockWise());
        BlockPos diagonal = ahead.relative(facing.getClockWise());
        int minX = Math.min(Math.min(clicked.getX(), ahead.getX()), Math.min(right.getX(), diagonal.getX()));
        int minZ = Math.min(Math.min(clicked.getZ(), ahead.getZ()), Math.min(right.getZ(), diagonal.getZ()));
        return new BlockPos(minX, clicked.getY(), minZ);
    }

    /** Every block of the trap whose master is at {@code origin}, master first. */
    public static List<BlockPos> partsFrom(BlockPos origin) {
        List<BlockPos> parts = new ArrayList<>(12);
        for (int dy = 0; dy < HEIGHT; dy++) {
            for (TrapCorner corner : TrapCorner.values()) {
                parts.add(origin.offset(corner.dx(), dy, corner.dz()));
            }
        }
        return parts;
    }

    /** The state of the part at {@code part} of the trap whose master is at {@code origin}. */
    public BlockState partState(BlockPos origin, BlockPos part) {
        return defaultBlockState().setValue(CORNER, TrapCorner.of(part.getX() - origin.getX(), part.getZ() - origin.getZ()))
                .setValue(LEVEL, TrapLevel.of(part.getY() - origin.getY()));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos origin = originFor(clicked, context.getHorizontalDirection());
        if (origin.getY() + HEIGHT > level.getMaxBuildHeight()) {
            return null;
        }
        for (BlockPos part : partsFrom(origin)) {
            if (!part.equals(clicked) && !level.getBlockState(part).canBeReplaced(context)) {
                return null;
            }
            if (!level.getWorldBorder().isWithinBounds(part)) {
                return null;
            }
        }
        return partState(origin, clicked);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockPos origin = masterPos(state, pos);
        for (BlockPos part : partsFrom(origin)) {
            if (!part.equals(pos)) {
                level.setBlock(part, partState(origin, part), Block.UPDATE_ALL);
            }
        }
    }

    /** Builds all twelve blocks with the master at {@code origin} (game tests, schematics). */
    public void place(Level level, BlockPos origin) {
        for (BlockPos part : partsFrom(origin)) {
            level.setBlock(part, partState(origin, part), Block.UPDATE_ALL);
        }
    }

    @Override
    public TrapTier tier() {
        return TrapTier.LARGE;
    }

    @Override
    public boolean isMaster(BlockState state) {
        return state.getValue(CORNER) == TrapCorner.NW && state.getValue(LEVEL) == TrapLevel.BOTTOM;
    }

    @Override
    public BlockPos masterPos(BlockState state, BlockPos pos) {
        if (!state.hasProperty(CORNER)) {
            return pos;
        }
        TrapCorner corner = state.getValue(CORNER);
        return pos.offset(-corner.dx(), -state.getValue(LEVEL).dy(), -corner.dz());
    }

    @Override
    public List<BlockPos> parts(BlockState state, BlockPos pos) {
        return partsFrom(masterPos(state, pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
