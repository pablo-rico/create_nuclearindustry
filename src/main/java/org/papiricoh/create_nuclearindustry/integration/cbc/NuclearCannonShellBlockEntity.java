package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBlockEntity;

/** A nuclear cannon shell standing as a block or loaded in a cannon: its fuze, and the warhead load it was made with. */
public class NuclearCannonShellBlockEntity extends FuzedBlockEntity {
    private WarheadLoad load = WarheadLoad.FULL;

    public NuclearCannonShellBlockEntity(BlockPos pos, BlockState state) {
        super(CBCNuclearContent.NUCLEAR_CANNON_SHELL_BLOCK_ENTITY.get(), pos, state);
    }

    public WarheadLoad load() {
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
        load = WarheadLoad.read(tag);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        load = components.getOrDefault(AllNuclearDataComponents.WARHEAD_LOAD.get(), WarheadLoad.FULL);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(AllNuclearDataComponents.WARHEAD_LOAD.get(), load);
    }
}
