package org.papiricoh.create_nuclearindustry.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.blockentity.ReactorBlockEntity;

/**
 * Handles block change events near reactor controllers.
 * When a block in a reactor structure is changed, triggers revalidation.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID, bus = EventBusSubscriber.Bus.GAME)
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

        // Iterate through potential reactor controller positions
        BlockPos.betweenClosed(
                changedPos.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS),
                changedPos.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
        ).forEach(checkPos -> {
            BlockState blockState = level.getBlockState(checkPos);

            // Check if this position has a reactor controller
            if (blockState.getBlock() == AllNuclearBlocks.REACTOR_CONTROLLER.get()) {
                // Get the block entity and trigger revalidation
                if (level.getBlockEntity(checkPos) instanceof ReactorBlockEntity reactor) {
                    System.out.println("[Reactor Block Change] Block changed at " + changedPos +
                            ", checking reactor at " + checkPos);
                    reactor.requestStructureRevalidation(player);
                }
            }
        });
    }
}
