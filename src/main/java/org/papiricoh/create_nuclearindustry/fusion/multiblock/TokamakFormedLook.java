package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.turbine.block.TurbineFormedLook;

/**
 * The look of a formed tokamak. The plain hull (coils, blankets, vessel, viewports) hides under the
 * faceted skin the controller's renderer draws, and the parts with a job (controller, ports,
 * injectors, magnet drive) shrink to a nozzle poking out of that skin. Leftover corner blocks of a
 * ring built before the octagonal section hide too.
 */
public final class TokamakFormedLook {
    /** Hull block covered by the skin. */
    public static final BooleanProperty HIDDEN = TurbineFormedLook.HIDDEN;
    /** Functional part drawn as a nozzle. */
    public static final BooleanProperty FORMED = TurbineFormedLook.FORMED;

    private TokamakFormedLook() {}

    /** Whether the block wears its formed look. */
    public static boolean isShaped(BlockState state) {
        return state.hasProperty(HIDDEN) && state.getValue(HIDDEN)
                || state.hasProperty(FORMED) && state.getValue(FORMED);
    }

    public static void apply(Level level, TokamakStructure structure, boolean formed) {
        if (level == null || level.isClientSide) {
            return;
        }
        TokamakGeometry geometry = structure.geometry;
        BlockPos center = structure.center;
        for (int i = 0; i < geometry.shellCount(); i++) {
            shape(level, center.offset(geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i)), formed);
        }
        for (int i = 0; i < geometry.cornerCount(); i++) {
            shape(level, center.offset(geometry.cornerX(i), geometry.cornerY(i), geometry.cornerZ(i)), formed);
        }
        shape(level, structure.controllerPos, formed);
    }

    private static void shape(Level level, BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        BlockState next = shaped(state, formed);
        if (next != state) {
            // Clients only: no neighbour updates, so the multiblock does not revalidate itself.
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    /** The formed (or plain) look of {@code state}; also used by Ponder. */
    public static BlockState shaped(BlockState state, boolean formed) {
        if (state.hasProperty(HIDDEN)) {
            state = state.setValue(HIDDEN, formed);
        }
        if (state.hasProperty(FORMED)) {
            state = state.setValue(FORMED, formed);
        }
        return state;
    }

    /** Safety net for blocks left shaped (tokamak gone while unloaded, moved by a contraption). */
    public static void releaseIfOrphaned(BlockState state, ServerLevel level, BlockPos pos) {
        boolean claimed = MultiblockRegistry.affecting(level, pos, FusionReactorBlockEntity.class).stream()
                .anyMatch(tokamak -> tokamak.getStructure().map(s -> s.bounds().isInside(pos)).orElse(false));
        if (!claimed) {
            level.setBlock(pos, shaped(state, false), Block.UPDATE_CLIENTS);
        }
    }
}
