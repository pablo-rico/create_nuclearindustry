package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementBlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;

/**
 * A straight beamline block (beam pipe, cavity, quadrupole, monitor, ring dipole): the beam runs
 * along its {@code AXIS}, which is always horizontal. Placed along the way the player looks; the
 * wrench turns it a quarter round.
 */
public class BeamLineBlock extends RotatedPillarBlock implements BeamElementBlock, IWrenchable {
    private final BeamElementRole role;

    public BeamLineBlock(BeamElementRole role, Properties properties) {
        super(properties);
        this.role = role;
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction.Axis axis = face.getAxis().isHorizontal() ? face.getAxis() : context.getHorizontalDirection().getAxis();
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    @Override
    public BeamElementRole beamRole(BlockState state) {
        return role;
    }

    @Override
    public Direction.Axis beamAxis(BlockState state) {
        return state.getValue(AXIS);
    }
}
