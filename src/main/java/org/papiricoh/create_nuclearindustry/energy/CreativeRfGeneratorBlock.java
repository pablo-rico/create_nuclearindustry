package org.papiricoh.create_nuclearindustry.energy;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Creative RF Generator: an endless source of Forge Energy for testing machines without
 * another mod. Only in the creative tab, with no recipe, and unbreakable in survival.
 */
public class CreativeRfGeneratorBlock extends Block implements IBE<CreativeRfGeneratorBlockEntity>, IWrenchable {

    public CreativeRfGeneratorBlock(Properties properties) {
        super(properties);
    }

    /** Only a creative player may pick it up with the wrench. */
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            return InteractionResult.PASS;
        }
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<CreativeRfGeneratorBlockEntity> getBlockEntityClass() {
        return CreativeRfGeneratorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeRfGeneratorBlockEntity> getBlockEntityType() {
        return EnergyRegistry.CREATIVE_RF_GENERATOR_ENTITY.get();
    }
}
