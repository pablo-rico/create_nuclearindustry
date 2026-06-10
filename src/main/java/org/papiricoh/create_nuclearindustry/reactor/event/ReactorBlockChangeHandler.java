package org.papiricoh.create_nuclearindustry.reactor.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
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
        if (event instanceof BlockEvent.EntityMultiPlaceEvent) {
            return;
        }
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
     * Called for multi-block placements such as beds or modded placement helpers.
     */
    @SubscribeEvent
    public static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }

        Player player = event.getEntity() instanceof Player p ? p : null;
        event.getReplacedBlockSnapshots().forEach(snapshot -> checkNearbyReactors(level, snapshot.getPos(), player));
    }

    /**
     * Called when neighbor notifications happen around state changes.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        if (!isStructuralNeighborUpdate(event.getState())) {
            return;
        }
        checkNearbyReactors(level, event.getPos(), null);
    }

    /**
     * Called when fluids place or replace blocks.
     */
    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        checkNearbyReactors(level, event.getPos(), null);
        checkNearbyReactors(level, event.getLiquidPos(), null);
    }

    /**
     * Called for vanilla/modded tool transformations that bypass normal placement paths.
     */
    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || !(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        checkNearbyReactors(level, event.getPos(), event.getPlayer());
    }

    /**
     * Called once an explosion has calculated affected blocks.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        event.getAffectedBlocks().forEach(pos -> checkNearbyReactors(level, pos, null));
    }

    /**
     * Remove registered controllers when their chunk unloads.
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        ReactorManager.unregisterReactorsInChunk(level, event.getChunk().getPos());
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

    private static boolean isStructuralNeighborUpdate(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(AllNuclearBlocks.REACTOR_CASING.get())
                || state.is(AllNuclearBlocks.REACTOR_FLUID_PORT.get())
                || state.is(AllNuclearBlocks.REACTOR_FUEL_PORT.get())
                || state.is(AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get())
                || state.is(AllNuclearBlocks.URANIUM_ROD.get())
                || state.is(AllNuclearBlocks.HEAT_EXCHANGER.get());
    }
}
