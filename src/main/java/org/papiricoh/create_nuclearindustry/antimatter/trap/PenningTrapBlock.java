package org.papiricoh.create_nuclearindustry.antimatter.trap;

import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.multiblock.MasterProxy;

import java.util.List;
import java.util.Optional;

/**
 * A Penning trap of one {@link TrapTier}: one block (small), two (medium) or twelve (large), placed
 * and broken as a unit. Only the master block has the {@link PenningTrapBlockEntity}; every other
 * part answers through it ({@link MasterProxy}), for capabilities, goggles, comparators and clicks.
 * Breaking any part removes the whole trap: a loaded trap annihilates, an empty one drops itself.
 * Right-click with a vial to put it in the port, with an empty hand to take it out; the wrench
 * switches the port between filling and emptying the vial.
 */
public abstract class PenningTrapBlock extends Block implements IBE<PenningTrapBlockEntity>, MasterProxy, IWrenchable,
        IProxyHoveringInformation {
    /** Set while one part removes the others, so they do not each try again. */
    private static boolean removingParts;

    protected PenningTrapBlock(Properties properties) {
        super(properties);
    }

    public abstract TrapTier tier();

    /** Whether this part is the one with the block entity. */
    public abstract boolean isMaster(BlockState state);

    /** Every block of the trap {@code state} at {@code pos} belongs to, master first. */
    public abstract List<BlockPos> parts(BlockState state, BlockPos pos);

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMaster(state) ? IBE.super.newBlockEntity(pos, state) : null;
    }

    /** The master's block entity, from any part. */
    public Optional<PenningTrapBlockEntity> master(BlockGetter level, BlockState state, BlockPos pos) {
        return level.getBlockEntity(masterPos(state, pos)) instanceof PenningTrapBlockEntity trap ? Optional.of(trap) : Optional.empty();
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return masterPos(state, pos);
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof AntimatterVialItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            master(level, state, pos).ifPresent(trap -> trap.insertVial(player, hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            master(level, state, pos).ifPresent(trap -> trap.takeVial(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The wrench switches the vial port between filling and emptying the vial; it never turns a trap. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            master(level, state, context.getClickedPos()).ifPresent(trap -> trap.cycleMode(context.getPlayer()));
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ removal

    /**
     * Breaking any part takes the rest of the trap with it, without drops: only the part that was
     * broken drops the trap. The master's block entity annihilates whatever it held.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // A ship carries every part along at once: none takes the others with it.
        if (!state.is(newState.getBlock()) && !removingParts && !movedByPiston
                && !org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly.isMoving()) {
            removingParts = true;
            try {
                for (BlockPos part : parts(state, pos)) {
                    if (!part.equals(pos) && level.getBlockState(part).is(this)) {
                        level.setBlock(part, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                removingParts = false;
            }
        }
        IBE.onRemove(state, level, pos, newState);
    }

    // ------------------------------------------------------------------ comparators and looks

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return master(level, state, pos).map(PenningTrapBlockEntity::comparatorSignal).orElse(0);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public Class<PenningTrapBlockEntity> getBlockEntityClass() {
        return PenningTrapBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PenningTrapBlockEntity> getBlockEntityType() {
        return AntimatterRegistry.PENNING_TRAP_ENTITY.get();
    }
}
