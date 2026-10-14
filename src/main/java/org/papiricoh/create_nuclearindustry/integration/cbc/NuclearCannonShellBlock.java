package org.papiricoh.create_nuclearindustry.integration.cbc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBlockEntity;
import rbasamoyai.createbigcannons.munitions.big_cannon.SimpleShellBlock;

import java.util.List;

/**
 * A nuclear cannon shell as a block. Its warhead load ({@link WarheadLoad}) goes with it everywhere Create
 * Big Cannons moves it: from the item to the block, into the cannon, back out as an item, and into the
 * projectile when it is fired.
 */
public class NuclearCannonShellBlock extends SimpleShellBlock<NuclearCannonShellProjectile> {
    public static final MapCodec<NuclearCannonShellBlock> CODEC = simpleCodec(NuclearCannonShellBlock::new);

    public NuclearCannonShellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends NuclearCannonShellBlock> codec() {
        return CODEC;
    }

    @Override
    public EntityType<? extends NuclearCannonShellProjectile> getAssociatedEntityType() {
        return CBCNuclearContent.NUCLEAR_CANNON_SHELL_PROJECTILE.get();
    }

    @Override
    public BlockEntityType<? extends FuzedBlockEntity> getBlockEntityType() {
        return CBCNuclearContent.NUCLEAR_CANNON_SHELL_BLOCK_ENTITY.get();
    }

    @Override
    public boolean isBaseFuze() {
        return false;
    }

    @Override
    public AbstractBigCannonProjectile getProjectile(Level level, List<StructureTemplate.StructureBlockInfo> blocks) {
        AbstractBigCannonProjectile projectile = super.getProjectile(level, blocks);
        if (projectile instanceof NuclearCannonShellProjectile shell && !blocks.isEmpty() && blocks.get(0).nbt() != null) {
            shell.setLoad(WarheadLoad.read(blocks.get(0).nbt()));
        }
        return projectile;
    }

    @Override
    public AbstractBigCannonProjectile getProjectile(Level level, ItemStack stack) {
        AbstractBigCannonProjectile projectile = super.getProjectile(level, stack);
        if (projectile instanceof NuclearCannonShellProjectile shell) {
            shell.setLoad(stack.getOrDefault(AllNuclearDataComponents.WARHEAD_LOAD.get(), WarheadLoad.FULL));
        }
        return projectile;
    }

    @Override
    public AbstractBigCannonProjectile getProjectile(Level level, BlockPos pos, BlockState state) {
        AbstractBigCannonProjectile projectile = super.getProjectile(level, pos, state);
        if (projectile instanceof NuclearCannonShellProjectile shell
                && level.getBlockEntity(pos) instanceof NuclearCannonShellBlockEntity entity) {
            shell.setLoad(entity.load());
        }
        return projectile;
    }

    @Override
    public StructureTemplate.StructureBlockInfo getHandloadingInfo(ItemStack stack, BlockPos pos, Direction direction,
                                                                   HolderLookup.Provider registries) {
        StructureTemplate.StructureBlockInfo info = super.getHandloadingInfo(stack, pos, direction, registries);
        CompoundTag nbt = info.nbt() == null ? new CompoundTag() : info.nbt().copy();
        stack.getOrDefault(AllNuclearDataComponents.WARHEAD_LOAD.get(), WarheadLoad.FULL).write(nbt);
        return new StructureTemplate.StructureBlockInfo(info.pos(), info.state(), nbt);
    }

    @Override
    public ItemStack getExtractedItem(StructureTemplate.StructureBlockInfo info, HolderLookup.Provider registries) {
        ItemStack stack = super.getExtractedItem(info, registries);
        if (info.nbt() != null) {
            stack.set(AllNuclearDataComponents.WARHEAD_LOAD.get(), WarheadLoad.read(info.nbt()));
        }
        return stack;
    }
}
