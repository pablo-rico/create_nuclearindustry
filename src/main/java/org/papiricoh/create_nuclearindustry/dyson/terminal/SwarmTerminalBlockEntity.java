package org.papiricoh.create_nuclearindustry.dyson.terminal;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;

import java.util.List;

/**
 * A swarm terminal holds nothing of its own: the swarm is the world's ({@code DysonSwarm}). It is a block entity so Display Links
 * can read the swarm from it and its screen can find it.
 */
public class SwarmTerminalBlockEntity extends SmartBlockEntity {
    public SwarmTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(DysonRegistry.TERMINAL_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }
}
