package org.papiricoh.create_nuclearindustry.wastetransport.cask;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteCargo;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.breach.BreachEffects;

/**
 * A Shielded Waste Cask block: lies along a horizontal axis and joins neighbours on the same axis
 * into one cask, like Create's Item Vault. Broken, it drops what it held and the waste it held
 * contaminates the ground around it.
 */
public class WasteCaskBlock extends Block implements IWrenchable, IBE<WasteCaskBlockEntity> {
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public WasteCaskBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_AXIS);
        super.createBlockStateDefinition(builder);
    }

    public static boolean isCask(BlockState state) {
        return state.getBlock() instanceof WasteCaskBlock;
    }

    @Nullable
    public static Direction.Axis getCaskAxis(BlockState state) {
        return isCask(state) ? state.getValue(HORIZONTAL_AXIS) : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            // Placed against another cask, it lies the same way so the two connect.
            BlockState placedOn = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
            Direction.Axis preferred = getCaskAxis(placedOn);
            if (preferred != null) {
                return defaultBlockState().setValue(HORIZONTAL_AXIS, preferred);
            }
        }
        return defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (oldState.getBlock() != state.getBlock() && !isMoving) {
            withBlockEntityDo(level, pos, WasteCaskBlockEntity::updateConnectivity);
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getClickedFace().getAxis().isVertical()) {
            if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof WasteCaskBlockEntity cask) {
                ConnectivityHandler.splitMulti(cask);
                cask.removeController(true);
            }
        }
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            if (!(level.getBlockEntity(pos) instanceof WasteCaskBlockEntity cask)) {
                return;
            }
            if (!ShipAssembly.isMoving()) {
                // Carried to or from a ship the copy at the new place keeps everything; otherwise it bursts.
                if (!isMoving && level instanceof ServerLevel serverLevel) {
                    BreachEffects.spill(serverLevel, pos, WasteCargo.activity(cask.getInventoryOfBlock()));
                }
                ItemHelper.dropContents(level, pos, cask.getInventoryOfBlock());
            }
            level.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(cask);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction.Axis axis = state.getValue(HORIZONTAL_AXIS);
        return state.setValue(HORIZONTAL_AXIS, rotation.rotate(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)).getAxis());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        WasteCaskBlockEntity cask = getBlockEntity(level, pos);
        IItemHandler handler = cask == null ? null : cask.itemHandler();
        return handler == null ? 0 : ItemHelper.calcRedstoneFromInventory(handler);
    }

    @Override
    public Class<WasteCaskBlockEntity> getBlockEntityClass() {
        return WasteCaskBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WasteCaskBlockEntity> getBlockEntityType() {
        return WasteTransportRegistry.CASK_ENTITY.get();
    }
}
