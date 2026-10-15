package org.papiricoh.create_nuclearindustry.propulsion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.AntimatterEngineBlockEntity;

/**
 * The antimatter engine's controller: the annihilation chamber. An antimatter conduit or a vial in its
 * port feeds it; right-click with a vial to put it in, with an empty hand to take it back or read the
 * chamber.
 */
public class AntimatterEngineBlock extends RocketEngineBlock<AntimatterEngineBlockEntity> {
    public static final MapCodec<AntimatterEngineBlock> CODEC = simpleCodec(AntimatterEngineBlock::new);

    public AntimatterEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof AntimatterVialItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, engine -> engine.insertVial(player, hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, engine -> engine.takeVial(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Class<AntimatterEngineBlockEntity> getBlockEntityClass() {
        return AntimatterEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AntimatterEngineBlockEntity> getBlockEntityType() {
        return PropulsionRegistry.ANTIMATTER_ENGINE_ENTITY.get();
    }
}
