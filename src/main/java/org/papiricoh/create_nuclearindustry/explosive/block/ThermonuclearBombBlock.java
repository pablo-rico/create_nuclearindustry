package org.papiricoh.create_nuclearindustry.explosive.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.ThermonuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.explosive.gui.ThermonuclearBombMenu;

/** A Thermonuclear Bomb: powered like a nuclear one, loaded with a primary, a secondary and a tamper. */
public class ThermonuclearBombBlock extends NuclearBombBlock {
    public static final MapCodec<ThermonuclearBombBlock> CODEC = simpleCodec(ThermonuclearBombBlock::new);

    public ThermonuclearBombBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected MenuConstructor menu(BlockPos pos) {
        return (id, inventory, player) -> new ThermonuclearBombMenu(id, inventory,
                player.level().getBlockEntity(pos) instanceof ThermonuclearBombBlockEntity bomb ? bomb : null, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermonuclearBombBlockEntity(pos, state);
    }
}
