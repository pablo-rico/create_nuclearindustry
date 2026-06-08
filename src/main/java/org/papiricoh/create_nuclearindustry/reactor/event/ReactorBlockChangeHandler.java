package org.papiricoh.create_nuclearindustry.reactor.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * Handles block change events near reactor controllers.
 * When a block in a reactor structure is changed, triggers revalidation.
 */
public class ReactorBlockChangeHandler {

    /**
     * Called when a block breaks. Check if it affects any nearby reactors.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) {
            return;
        }

        Player player = event.getPlayer();
        BlockPos brokenPos = event.getPos();
        checkNearbyReactors(level, brokenPos, player);
    }

    /**
     * Called when a block is placed. Check if it affects any nearby reactors.
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) {
            return;
        }

        // Try to get player from entity
        Player player = null;
        if (event.getEntity() instanceof Player p) {
            player = p;
        }

        BlockPos placedPos = event.getPos();
        checkNearbyReactors(level, placedPos, player);
    }

    /**
     * Checks all reactor controllers within a reasonable distance of the changed block.
     * If a block change could affect a reactor structure, triggers revalidation.
     */
    private static void checkNearbyReactors(Level level, BlockPos changedPos, Player player) {
        // Search radius: max reactor size is 11×11×15, add some buffer
        final int SEARCH_RADIUS = 15;

        // Use ReactorManager to get only nearby reactors (O(n) where n = registered reactors nearby, not O(27000))
        for (ReactorBlockEntity reactor : ReactorManager.getReactorsInRange(level, changedPos, SEARCH_RADIUS)) {
            if (reactor.shouldReactToBlockChange(changedPos)) {
                reactor.requestStructureRevalidation(player);
            }
        }
    }
}
