package org.papiricoh.create_nuclearindustry.wastetransport.cistern;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Mounts a cistern on a contraption: only its controller carries the fluid, as with Create's tanks. */
public class WasteCisternMountedStorageType extends MountedFluidStorageType<WasteCisternMountedStorage> {

    public WasteCisternMountedStorageType() {
        super(WasteCisternMountedStorage.CODEC);
    }

    @Nullable
    @Override
    public WasteCisternMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof WasteCisternBlockEntity cistern && cistern.isController()) {
            return WasteCisternMountedStorage.fromCistern(cistern);
        }
        return null;
    }
}
