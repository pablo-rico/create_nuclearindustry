package org.papiricoh.create_nuclearindustry.radiation.contamination;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Config;

/**
 * Waste deep enough underground counts as geologically stored: it neither irradiates nor
 * leaks. Building a proper repository around it is left to the player; the only rule is depth.
 */
public final class GeologicalStorage {
    private GeologicalStorage() {}

    public static boolean isStored(BlockState state, BlockPos pos) {
        return isDeepEnough(pos) && state.is(AllNuclearTags.Blocks.GEOLOGICALLY_STORABLE);
    }

    public static boolean isDeepEnough(BlockPos pos) {
        return pos.getY() <= Config.geologicalStorageMaxY;
    }
}
