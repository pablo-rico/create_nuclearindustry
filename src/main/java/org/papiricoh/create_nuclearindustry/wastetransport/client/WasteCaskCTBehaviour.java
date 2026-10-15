package org.papiricoh.create_nuclearindustry.wastetransport.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.wastetransport.cask.WasteCaskBlock;

/**
 * Connected textures of the Shielded Waste Cask, ported from Create's Item Vault: the ends show the
 * front sheet, the long sides run along the cask's axis, and only blocks of the same cask join.
 */
public class WasteCaskCTBehaviour extends ConnectedTextureBehaviour.Base {

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        Direction.Axis axis = WasteCaskBlock.getCaskAxis(state);
        if (axis == null) {
            return null;
        }
        if (direction.getAxis() == axis) {
            return WasteTransportSpriteShifts.CASK_FRONT;
        }
        if (direction == Direction.UP) {
            return WasteTransportSpriteShifts.CASK_TOP;
        }
        return direction == Direction.DOWN ? WasteTransportSpriteShifts.CASK_BOTTOM : WasteTransportSpriteShifts.CASK_SIDE;
    }

    @Override
    protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis axis = WasteCaskBlock.getCaskAxis(state);
        boolean alongX = axis == Direction.Axis.X;
        if (face.getAxis().isVertical() && alongX) {
            return super.getUpDirection(reader, pos, state, face).getClockWise();
        }
        if (face.getAxis() == axis || face.getAxis().isVertical()) {
            return super.getUpDirection(reader, pos, state, face);
        }
        return Direction.fromAxisAndDirection(axis, alongX ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    @Override
    protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis axis = WasteCaskBlock.getCaskAxis(state);
        if (face.getAxis().isVertical() && axis == Direction.Axis.X) {
            return super.getRightDirection(reader, pos, state, face).getClockWise();
        }
        if (face.getAxis() == axis || face.getAxis().isVertical()) {
            return super.getRightDirection(reader, pos, state, face);
        }
        return Direction.fromAxisAndDirection(Direction.Axis.Y, face.getAxisDirection());
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        return state == other && ConnectivityHandler.isConnected(reader, pos, otherPos);
    }
}
