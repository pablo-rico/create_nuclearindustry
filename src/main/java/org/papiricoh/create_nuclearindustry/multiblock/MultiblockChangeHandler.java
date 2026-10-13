package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Routes every kind of block change to the multiblock controllers whose structure it may
 * affect. Revalidation itself is deferred to the controller's next tick.
 */
public final class MultiblockChangeHandler {

    private MultiblockChangeHandler() {}

    public static void register(IEventBus bus) {
        bus.addListener(MultiblockChangeHandler::onBlockBreak);
        bus.addListener(MultiblockChangeHandler::onBlockPlace);
        bus.addListener(MultiblockChangeHandler::onBlockMultiPlace);
        bus.addListener(MultiblockChangeHandler::onNeighborNotify);
        bus.addListener(MultiblockChangeHandler::onFluidPlaceBlock);
        bus.addListener(MultiblockChangeHandler::onBlockToolModification);
        bus.addListener(MultiblockChangeHandler::onExplosionDetonate);
        bus.addListener(MultiblockChangeHandler::onChunkUnload);
        bus.addListener(MultiblockChangeHandler::onLevelUnload);
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, event.getPos(), event.getState(), true, event.getPlayer());
        }
    }

    private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event instanceof BlockEvent.EntityMultiPlaceEvent) {
            return;
        }
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            Player player = event.getEntity() instanceof Player p ? p : null;
            notify(level, event.getPos(), event.getPlacedBlock(), false, player);
        }
    }

    private static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            Player player = event.getEntity() instanceof Player p ? p : null;
            event.getReplacedBlockSnapshots().forEach(snapshot ->
                    notify(level, snapshot.getPos(), null, false, player));
        }
    }

    /** Catches changes made without a player: drills, deployers, pistons, commands. */
    private static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, event.getPos(), event.getState(), false, null);
        }
    }

    private static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, event.getPos(), event.getNewState(), false, null);
        }
    }

    private static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isSimulated() && event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, event.getPos(), null, true, event.getPlayer());
        }
    }

    private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            notify(level, pos, null, true, null);
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            MultiblockRegistry.unregisterChunk(level, event.getChunk().getPos());
        }
    }

    /** The level's index goes with it: nothing must keep its block entities (and the level) alive. */
    public static void onLevelUnload(LevelEvent.Unload event) {
        MultiblockRegistry.forget(event.getLevel());
    }

    /**
     * Tells the controllers around {@code pos} of a change. Most changes happen far from any
     * multiblock: those cost one chunk lookup, and the change itself (and the state, when it is
     * read from the level) is only made once some controller's bounds hold the position.
     *
     * @param state the state of the change, or null to read it from the level
     */
    private static void notify(Level level, BlockPos pos, @Nullable BlockState state, boolean removed, @Nullable Player player) {
        List<MultiblockController> affected = MultiblockRegistry.affecting(level, pos);
        if (affected.isEmpty()) {
            return;
        }
        BlockChange change = new BlockChange(pos, state != null ? state : level.getBlockState(pos), removed);
        for (MultiblockController controller : affected) {
            if (controller.shouldReactToBlockChange(change)) {
                controller.requestStructureRevalidation(player);
            }
        }
    }
}
