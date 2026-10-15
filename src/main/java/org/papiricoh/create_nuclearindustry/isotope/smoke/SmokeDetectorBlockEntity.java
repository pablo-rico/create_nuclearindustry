package org.papiricoh.create_nuclearindustry.isotope.smoke;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;

/**
 * Keeps a smoke detector scanning however it was placed (by hand, a schematic, a structure): a
 * scheduled tick would be lost with blocks placed without {@code onPlace}. Holds no data.
 */
public class SmokeDetectorBlockEntity extends BlockEntity {

    public SmokeDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(IsotopeRegistry.SMOKE_DETECTOR_ENTITY.get(), pos, state);
    }
}
