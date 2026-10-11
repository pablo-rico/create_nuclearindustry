package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.BeamDumpBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementBlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;

/** The beam dump at the end of a beamline; takes the beam from any side. */
public class BeamDumpBlock extends Block implements IBE<BeamDumpBlockEntity>, BeamElementBlock, IWrenchable {

    public BeamDumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BeamElementRole beamRole(BlockState state) {
        return BeamElementRole.DUMP;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<BeamDumpBlockEntity> getBlockEntityClass() {
        return BeamDumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BeamDumpBlockEntity> getBlockEntityType() {
        return AcceleratorRegistry.BEAM_DUMP_ENTITY.get();
    }
}
