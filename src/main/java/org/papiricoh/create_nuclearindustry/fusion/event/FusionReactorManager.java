package org.papiricoh.create_nuclearindustry.fusion.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

import java.util.List;

/** Lookup of fusion controllers whose structure area contains a position. */
public final class FusionReactorManager {

    private FusionReactorManager() {}

    public static List<FusionReactorBlockEntity> getReactorsInRange(Level level, BlockPos pos, int ignoredRange) {
        return MultiblockRegistry.affecting(level, pos, FusionReactorBlockEntity.class);
    }
}
