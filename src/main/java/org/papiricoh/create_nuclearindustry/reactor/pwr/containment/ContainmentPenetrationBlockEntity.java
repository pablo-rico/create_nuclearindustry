package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.List;

/**
 * The two buffers of a containment penetration. Each end fills the buffer that empties at the other
 * end, so fluid only ever crosses the wall; each tick a buffer also pushes what it holds into
 * whatever takes fluid on its outlet side, so a single pump can drive a line through the wall.
 */
public class ContainmentPenetrationBlockEntity extends SmartBlockEntity {
    public ContainmentPenetrationBlockEntity(BlockPos pos, BlockState state) {
        super(PwrRegistry.CONTAINMENT_PENETRATION_ENTITY.get(), pos, state);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour(this));
    }
}
