package org.papiricoh.create_nuclearindustry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.Nullable;

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
    }

    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, BlockChange.removed(event.getPos(), event.getState()), event.getPlayer());
        }
    }

    private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event instanceof BlockEvent.EntityMultiPlaceEvent) {
            return;
        }
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            Player player = event.getEntity() instanceof Player p ? p : null;
            notify(level, BlockChange.placed(event.getPos(), event.getPlacedBlock()), player);
        }
    }

    private static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            Player player = event.getEntity() instanceof Player p ? p : null;
            event.getReplacedBlockSnapshots().forEach(snapshot ->
                    notify(level, BlockChange.placed(snapshot.getPos(), level.getBlockState(snapshot.getPos())), player));
        }
    }

    /** Catches changes made without a player: drills, deployers, pistons, commands. */
    private static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, BlockChange.placed(event.getPos(), event.getState()), null);
        }
    }

    private static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            notify(level, BlockChange.placed(event.getPos(), event.getNewState()), null);
        }
    }

    private static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isSimulated() && event.getLevel() instanceof Level level && !level.isClientSide) {
            BlockPos pos = event.getPos();
            notify(level, BlockChange.removed(pos, level.getBlockState(pos)), event.getPlayer());
        }
    }

    private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            notify(level, BlockChange.removed(pos, level.getBlockState(pos)), null);
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            MultiblockRegistry.unregisterChunk(level, event.getChunk().getPos());
        }
    }

    private static void notify(Level level, BlockChange change, @Nullable Player player) {
        for (MultiblockController controller : MultiblockRegistry.affecting(level, change.pos())) {
            if (controller.shouldReactToBlockChange(change)) {
                controller.requestStructureRevalidation(player);
            }
        }
    }
}
