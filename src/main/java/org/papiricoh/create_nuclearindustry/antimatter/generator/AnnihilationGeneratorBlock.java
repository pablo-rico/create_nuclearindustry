package org.papiricoh.create_nuclearindustry.antimatter.generator;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;

/**
 * Annihilation generator: a heavy chamber that burns antiprotons for 256 RPM, Forge Energy or both.
 * {@code FACING} is the shaft side. Right-click with a vial to put it in the port, with an empty hand
 * to take it back (or read the chamber); the mode is set on the panel on top, or with the wrench on
 * the top face (the wrench on any other face turns the generator, as on any kinetic block).
 * Redstone sets the throttle; comparators read the output.
 */
public class AnnihilationGeneratorBlock extends HorizontalKineticBlock implements IBE<AnnihilationGeneratorBlockEntity> {
    public static final MapCodec<AnnihilationGeneratorBlock> CODEC = simpleCodec(AnnihilationGeneratorBlock::new);

    public AnnihilationGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AnnihilationGeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof AntimatterVialItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, generator -> generator.insertVial(player, hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, generator -> generator.takeVial(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** On the top face the wrench steps through the modes; elsewhere it turns the generator. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return super.onWrenched(state, context);
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            withBlockEntityDo(level, context.getClickedPos(), generator -> generator.cycleMode(context.getPlayer()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(AnnihilationGeneratorBlockEntity::comparatorSignal).orElse(0);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public Class<AnnihilationGeneratorBlockEntity> getBlockEntityClass() {
        return AnnihilationGeneratorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AnnihilationGeneratorBlockEntity> getBlockEntityType() {
        return AntimatterRegistry.GENERATOR_ENTITY.get();
    }
}
