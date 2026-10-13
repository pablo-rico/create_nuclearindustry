package org.papiricoh.create_nuclearindustry.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Recognises the three-block turbine line from before box turbines (output, rotor, casing in
 * a row) so its owner is told to rebuild it.
 */
public final class LegacyTurbineLineDetector {

    private LegacyTurbineLineDetector() {}

    public static boolean isLegacyLine(Level level, BlockPos controllerPos, TurbineTier tier) {
        BlockState controller = level.getBlockState(controllerPos);
        if (!tier.isController(controller)) {
            return false;
        }
        Direction inward = controller.getValue(BlockStateProperties.FACING).getOpposite();
        BlockState rotor = level.getBlockState(controllerPos.relative(inward));
        BlockState casing = level.getBlockState(controllerPos.relative(inward, 2));
        return tier.isRotor(rotor) && rotor.getValue(RotatedPillarBlock.AXIS) == inward.getAxis() && tier.isCasing(casing);
    }
}
