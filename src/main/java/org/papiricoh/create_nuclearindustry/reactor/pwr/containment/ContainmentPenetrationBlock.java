package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

/**
 * A sealed pipe sleeve through a containment wall. It counts as wall, as strong as concrete, and
 * carries fluid straight through along its axis: whatever is pumped into one end comes out of the
 * other, through a 1000 mB buffer each way, and is pushed into a tank or machine on the far side.
 * Placed like a log, along the face it is put against.
 */
public class ContainmentPenetrationBlock extends RotatedPillarBlock implements IBE<ContainmentPenetrationBlockEntity> {

    public ContainmentPenetrationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<ContainmentPenetrationBlockEntity> getBlockEntityClass() {
        return ContainmentPenetrationBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ContainmentPenetrationBlockEntity> getBlockEntityType() {
        return PwrRegistry.CONTAINMENT_PENETRATION_ENTITY.get();
    }
}
