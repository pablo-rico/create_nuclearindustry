package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBlockEntity;

/** A thermonuclear cannon shell standing as a block or loaded in a cannon: its fuze, and the parts it was made with. */
public class ThermonuclearCannonShellBlockEntity extends FuzedBlockEntity {
    private ThermonuclearLoad load = ThermonuclearLoad.SHELL_FULL;

    public ThermonuclearCannonShellBlockEntity(BlockPos pos, BlockState state) {
        super(CBCNuclearContent.THERMONUCLEAR_CANNON_SHELL_BLOCK_ENTITY.get(), pos, state);
    }

    public ThermonuclearLoad load() {
        return load;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        load.write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load = ThermonuclearLoad.read(tag);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        load = components.getOrDefault(ExplosiveRegistry.THERMONUCLEAR_LOAD.get(), ThermonuclearLoad.SHELL_FULL);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ExplosiveRegistry.THERMONUCLEAR_LOAD.get(), load);
    }
}
