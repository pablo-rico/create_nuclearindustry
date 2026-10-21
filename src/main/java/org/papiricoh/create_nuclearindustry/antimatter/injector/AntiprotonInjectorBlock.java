package org.papiricoh.create_nuclearindustry.antimatter.injector;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakFormedLook;

/**
 * Antiproton injector: a tokamak shell block. {@code FACING} is its outer face, with the vial port
 * and the lamps; the beam nozzle on the opposite face looks into the plasma. It takes Forge Energy
 * and antimatter (from a trap, a conduit or a vial) on any face. Right-click with a vial to put it
 * in the port, with an empty hand to take it back (or read the injector); the wrench steps through
 * automatic, redstone and safe arming. Comparators read the doses it holds.
 */
public class AntiprotonInjectorBlock extends Block implements IBE<AntiprotonInjectorBlockEntity>, TokamakShellBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AntiprotonInjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TokamakFormedLook.FORMED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TokamakFormedLook.FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public TokamakShellRole tokamakRole(BlockState state) {
        return TokamakShellRole.ANTIPROTON_INJECTOR;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof AntimatterVialItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, injector -> injector.insertVial(player, hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, injector -> injector.takeVial(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The wrench steps through the arming settings; it never turns an injector built into a shell. */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            withBlockEntityDo(level, context.getClickedPos(), injector -> injector.cycleArming(context.getPlayer()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(AntiprotonInjectorBlockEntity::comparatorSignal).orElse(0);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public Class<AntiprotonInjectorBlockEntity> getBlockEntityClass() {
        return AntiprotonInjectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AntiprotonInjectorBlockEntity> getBlockEntityType() {
        return AntimatterRegistry.INJECTOR_ENTITY.get();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return TokamakFormedLook.isShaped(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TokamakFormedLook.releaseIfOrphaned(state, level, pos);
    }
}
