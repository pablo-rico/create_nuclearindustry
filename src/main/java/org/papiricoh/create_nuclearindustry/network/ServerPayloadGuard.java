package org.papiricoh.create_nuclearindustry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The server's checks on a player who asks, from a screen, to change a block: a client can send
 * any position, so the request must come from a living player, not a spectator, in the block's
 * level, within reach of a loaded block, where spawn protection and the world border let them
 * act and their game mode lets them build. Also the shared {@code stillValid} of the mod's block
 * menus. Each payload still checks its own values.
 */
public final class ServerPayloadGuard {
    /**
     * Blocks beyond the player's interaction range a screen keeps working, as for vanilla
     * containers: a step or two back from the block does not close it.
     */
    public static final double SCREEN_REACH = 4.0;

    private ServerPayloadGuard() {}

    /** Whether {@code player} may change the block at {@code pos} from a screen. */
    public static boolean check(Player player, BlockPos pos) {
        return check(player, pos, SCREEN_REACH);
    }

    /** The same, with {@code reach} blocks beyond the player's interaction range. */
    public static boolean check(Player player, BlockPos pos, double reach) {
        return verdict(player, player.level(), pos, reach) == PayloadVerdict.OK;
    }

    /** Why {@code player} may or may not change the block at {@code pos} in {@code level}. */
    public static PayloadVerdict verdict(Player player, Level level, BlockPos pos, double reach) {
        return PayloadVerdict.decide(player.isAlive(), player.isSpectator(), player.level() == level, level.isLoaded(pos),
                () -> player.canInteractWithBlock(pos, reach), () -> level.mayInteract(player, pos), player::mayBuild);
    }

    /**
     * A block menu's {@code stillValid}: the block entity is still the one at its position, in the
     * player's level, and within {@link #SCREEN_REACH} of the player's range, as vanilla containers do.
     */
    public static boolean stillValid(@Nullable BlockEntity blockEntity, Player player) {
        if (blockEntity == null || blockEntity.isRemoved() || !player.isAlive()) {
            return false;
        }
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        return level != null && level == player.level()
                && level.getBlockEntity(pos) == blockEntity && player.canInteractWithBlock(pos, SCREEN_REACH);
    }
}
