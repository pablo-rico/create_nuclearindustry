package org.papiricoh.create_nuclearindustry.civildefense.seismic;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;

/**
 * A seismograph: feels every nuclear and dirty detonation in its dimension and remembers the last
 * eight (distance, power, bearing, time). Read it with goggles, a comparator or a Display Link.
 */
public class SeismicDetectorBlock extends Block implements IBE<SeismicDetectorBlockEntity> {

    public SeismicDetectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        SeismicDetectorBlockEntity detector = getBlockEntity(level, pos);
        return detector == null ? 0 : detector.comparatorSignal();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<SeismicDetectorBlockEntity> getBlockEntityClass() {
        return SeismicDetectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SeismicDetectorBlockEntity> getBlockEntityType() {
        return CivilDefenseRegistry.SEISMIC_DETECTOR_BE.get();
    }
}
