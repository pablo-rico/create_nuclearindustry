package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Which way a placed port, sensor or controller faces. Its front turns to the player, whatever face
 * was clicked: a part set into a wall from outside faces out without a block to click behind it.
 * Sneaking turns it the other way, as Create does for its directional blocks.
 */
public final class PartPlacement {

    private PartPlacement() {}

    public static Direction facingPlayer(BlockPlaceContext context) {
        Direction looking = context.getNearestLookingDirection();
        return context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? looking : looking.getOpposite();
    }
}
