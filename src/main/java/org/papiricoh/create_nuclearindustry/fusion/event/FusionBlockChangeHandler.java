package org.papiricoh.create_nuclearindustry.fusion.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.FusionStructureValidator;

/**
 * Triggers revalidation of nearby fusion reactors when blocks change (mirror of
 * {@code ReactorBlockChangeHandler}).
 */
public class FusionBlockChangeHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) return;
        checkNearby(level, event.getPos(), event.getPlayer());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event instanceof BlockEvent.EntityMultiPlaceEvent) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        Player player = event.getEntity() instanceof Player p ? p : null;
        checkNearby(level, event.getPos(), player);
    }

    @SubscribeEvent
    public static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        Player player = event.getEntity() instanceof Player p ? p : null;
        event.getReplacedBlockSnapshots().forEach(snapshot -> checkNearby(level, snapshot.getPos(), player));
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        if (!isStructuralBlock(event.getState())) return;
        checkNearby(level, event.getPos(), null);
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        checkNearby(level, event.getPos(), null);
        checkNearby(level, event.getLiquidPos(), null);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        event.getAffectedBlocks().forEach(pos -> checkNearby(level, pos, null));
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        FusionReactorManager.unregisterReactorsInChunk(level, event.getChunk().getPos());
    }

    private static void checkNearby(Level level, BlockPos changedPos, Player player) {
        final int searchRadius = FusionStructureValidator.MAX_SIZE + 2;
        for (FusionReactorBlockEntity reactor : FusionReactorManager.getReactorsInRange(level, changedPos, searchRadius)) {
            if (reactor.shouldReactToBlockChange(changedPos)) {
                reactor.requestStructureRevalidation(player);
            }
        }
    }

    private static boolean isStructuralBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT.get())
                || state.is(AllNuclearBlocks.FUSION_ACCELERATOR_CORNER.get())
                || state.is(AllNuclearBlocks.FUSION_ELECTROMAGNET.get())
                || state.is(AllNuclearBlocks.FUSION_CRYOSTAT_CASING.get())
                || state.is(AllNuclearBlocks.FUSION_MAGNET_INPUT.get())
                || state.is(AllNuclearBlocks.FUSION_FLUID_PORT.get())
                || state.is(AllNuclearBlocks.FUSION_FUEL_INJECTOR.get());
    }
}
