package org.papiricoh.create_nuclearindustry.wastetransport.cask;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Mounts every cask block on a contraption with its own slots, as Create does with vaults. */
public class WasteCaskMountedStorageType extends MountedItemStorageType<WasteCaskMountedStorage> {

    public WasteCaskMountedStorageType() {
        super(WasteCaskMountedStorage.CODEC);
    }

    @Nullable
    @Override
    public WasteCaskMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        return be instanceof WasteCaskBlockEntity cask ? WasteCaskMountedStorage.fromCask(cask) : null;
    }
}
