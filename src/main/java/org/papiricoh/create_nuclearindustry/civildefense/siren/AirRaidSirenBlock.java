package org.papiricoh.create_nuclearindustry.civildefense.siren;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;

/**
 * An electric air raid siren: sounds while powered by redstone, or on its own while a missile, bomb or
 * launch countdown is aimed within its reach ({@code civilDefense.siren.threatRadius}). A comparator
 * reads 15 from it while it sounds.
 */
public class AirRaidSirenBlock extends Block implements IBE<AirRaidSirenBlockEntity> {
    public static final BooleanProperty SOUNDING = BooleanProperty.create("sounding");
    /** Pole, rotor housing, rain cap and finial. */
    static final VoxelShape SHAPE = Shapes.or(box(6, 0, 6, 10, 4, 10), box(3, 4, 3, 13, 12, 13),
            box(2, 12, 2, 14, 14, 14), box(6, 14, 6, 10, 16, 10));

    public AirRaidSirenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOUNDING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOUNDING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, AirRaidSirenBlockEntity::evaluate);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(SOUNDING) ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<AirRaidSirenBlockEntity> getBlockEntityClass() {
        return AirRaidSirenBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirRaidSirenBlockEntity> getBlockEntityType() {
        return CivilDefenseRegistry.AIR_RAID_SIREN_BE.get();
    }
}
