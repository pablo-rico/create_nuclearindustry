package org.papiricoh.create_nuclearindustry.isotope.smoke;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeConfig;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.isotope.physics.SmokeDetectorModel;
import org.papiricoh.create_nuclearindustry.isotope.physics.SmokeDetectorModel.SmokeSource;

/**
 * An ionisation smoke detector: a speck of americium-241 keeps the air in its chamber ionised and a
 * small current flowing; smoke cuts the current and the alarm sounds. It looks for fire, lit
 * campfires, lava, lit furnaces and burning creatures around it twice a second and gives a redstone
 * signal that grows with the smoke, strongest into the block it is fixed to, like a button.
 * Americium-241 lasts centuries, so the detector never wears out.
 */
public class SmokeDetectorBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<SmokeDetectorBlock> CODEC = simpleCodec(SmokeDetectorBlock::new);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    private static final int SCAN_INTERVAL = 10;

    private static final VoxelShape FLOOR = Block.box(3, 0, 3, 13, 3, 13);
    private static final VoxelShape CEILING = Block.box(3, 13, 3, 13, 16, 13);
    private static final VoxelShape NORTH = Block.box(3, 3, 13, 13, 13, 16);
    private static final VoxelShape SOUTH = Block.box(3, 3, 0, 13, 13, 3);
    private static final VoxelShape EAST = Block.box(0, 3, 3, 3, 13, 13);
    private static final VoxelShape WEST = Block.box(13, 3, 3, 16, 13, 13);

    public SmokeDetectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.CEILING).setValue(POWER, 0));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, POWER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
                default -> NORTH;
            };
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmokeDetectorBlockEntity(pos, state);
    }

    /** Scans twice a second on the server, each detector on its own beat. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != IsotopeRegistry.SMOKE_DETECTOR_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, blockState, be) -> {
            if (Math.floorMod(lvl.getGameTime() + pos.asLong(), SCAN_INTERVAL) == 0 && lvl instanceof ServerLevel server) {
                scan(blockState, server, pos);
            }
        };
    }

    private void scan(BlockState state, ServerLevel level, BlockPos pos) {
        int signal = SmokeDetectorModel.signal(IsotopeConfig.smoke(), smoke(level, pos), 1.0);
        if (signal != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, signal), Block.UPDATE_ALL);
            updateNeighbours(state, level, pos);
            if (signal > 0 && state.getValue(POWER) == 0) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.0f, 2.0f);
            }
        } else if (signal > 0 && level.getGameTime() % 20 < SCAN_INTERVAL) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.0f, 2.0f);
        }
    }

    /** The smoke reaching the detector at {@code pos}: every source in range, weighted and fading with distance. */
    public static double smoke(Level level, BlockPos pos) {
        SmokeDetectorModel.Params p = IsotopeConfig.smoke();
        int r = Math.max(1, p.radius());
        double smoke = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    SmokeSource source = sourceAt(level, cursor);
                    if (source != null) {
                        smoke += SmokeDetectorModel.contribution(p, source, Math.sqrt(dx * dx + dy * dy + dz * dz));
                    }
                }
            }
        }
        for (Entity entity : level.getEntities((Entity) null, new AABB(pos).inflate(r), Entity::isOnFire)) {
            smoke += SmokeDetectorModel.contribution(p, SmokeSource.BURNING, Math.sqrt(entity.blockPosition().distSqr(pos)));
        }
        return smoke;
    }

    private static SmokeSource sourceAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        if (state.getBlock() instanceof BaseFireBlock) {
            return SmokeSource.FIRE;
        }
        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            return SmokeSource.CAMPFIRE;
        }
        if (state.getBlock() instanceof AbstractFurnaceBlock && state.getValue(AbstractFurnaceBlock.LIT)) {
            return SmokeSource.BURNING;
        }
        FluidState fluid = state.getFluidState();
        if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) {
            return SmokeSource.LAVA;
        }
        return null;
    }

    private void updateNeighbours(BlockState state, Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), this);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!moving && !state.is(newState.getBlock())) {
            if (state.getValue(POWER) > 0) {
                updateNeighbours(state, level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getConnectedDirection(state) == direction ? state.getValue(POWER) : 0;
    }
}
