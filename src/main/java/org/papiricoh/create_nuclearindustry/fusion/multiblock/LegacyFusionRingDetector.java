package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;

/**
 * Recognises the flat square fusion ring from before the tokamak, so its owner gets told to
 * rebuild it instead of a generic "structure incomplete".
 */
public final class LegacyFusionRingDetector {

    private LegacyFusionRingDetector() {}

    /** True if accelerator corners sit on all four diagonals of the controller, as the old ring had them. */
    public static boolean isLegacyRing(Level level, BlockPos controllerPos) {
        for (int half = 2; half <= 5; half++) {
            if (corner(level, controllerPos.offset(half, 0, half))
                    && corner(level, controllerPos.offset(-half, 0, half))
                    && corner(level, controllerPos.offset(half, 0, -half))
                    && corner(level, controllerPos.offset(-half, 0, -half))) {
                return true;
            }
        }
        return false;
    }

    private static boolean corner(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get());
    }
}
