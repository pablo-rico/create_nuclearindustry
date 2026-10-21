package org.papiricoh.create_nuclearindustry.turbine.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineRotorBlock;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;

import java.util.Map;

/**
 * The look of a formed turbine. The rotor blocks hide (the renderer draws one big rotor), the
 * front slice hides behind the controller's bezel and fan partials, and every other wall block
 * shrinks to thin plates on its outer faces, copycat style.
 */
public final class TurbineFormedLook {
    /** Controller and rotor: drawn by the controller's renderer while formed. */
    public static final BooleanProperty FORMED = TurbineRotorBlock.FORMED;
    /** Front-slice casing, covered by the bezel partial. */
    public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");
    /** One thin plate per outer face; none set means the plain unformed block. */
    public static final Map<Direction, BooleanProperty> PANELS = PipeBlock.PROPERTY_BY_DIRECTION;

    private TurbineFormedLook() {}

    public static void addPanels(StateDefinition.Builder<Block, BlockState> builder) {
        PANELS.values().forEach(builder::add);
    }

    public static BlockState withoutPanels(BlockState state) {
        for (BooleanProperty panel : PANELS.values()) {
            state = state.setValue(panel, false);
        }
        return state;
    }

    /** Whether a wall block wears its formed look (hidden or panelled). */
    public static boolean isShaped(BlockState state) {
        if (state.hasProperty(HIDDEN) && state.getValue(HIDDEN)) {
            return true;
        }
        for (BooleanProperty panel : PANELS.values()) {
            if (state.hasProperty(panel) && state.getValue(panel)) {
                return true;
            }
        }
        return false;
    }

    public static void apply(Level level, TurbineStructure structure, boolean formed) {
        if (level == null || level.isClientSide) {
            return;
        }
        for (BlockPos pos : structure.rotors) {
            set(level, pos, FORMED, formed);
        }
        set(level, structure.controllerPos, FORMED, formed);
        for (int a = 0; a < structure.length; a++) {
            for (int i = -structure.hu; i <= structure.hu; i++) {
                for (int j = -structure.hv; j <= structure.hv; j++) {
                    if (a == 0 || a == structure.length - 1 || Math.abs(i) == structure.hu || Math.abs(j) == structure.hv) {
                        shapeWall(level, structure, a, i, j, formed);
                    }
                }
            }
        }
    }

    private static void shapeWall(Level level, TurbineStructure s, int a, int i, int j, boolean formed) {
        BlockPos pos = s.at(a, i, j);
        if (pos.equals(s.controllerPos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        BlockState next = shaped(s, a, i, j, state, formed);
        if (next != state) {
            // Clients only: no neighbour updates, so the multiblock does not revalidate itself.
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    /** The look of the wall block at ({@code a}, {@code i}, {@code j}) of {@code s}; also used by Ponder. */
    public static BlockState shaped(TurbineStructure s, int a, int i, int j, BlockState state, boolean formed) {
        BlockState next = state;
        if (state.hasProperty(HIDDEN)) {
            next = next.setValue(HIDDEN, formed && a == 0);
        }
        if (state.hasProperty(PANELS.get(Direction.NORTH))) {
            next = withoutPanels(next);
            if (formed && a > 0) {
                next = panel(next, a == s.length - 1, s.inward);
                next = panel(next, i == s.hu, s.u);
                next = panel(next, i == -s.hu, s.u.getOpposite());
                next = panel(next, j == s.hv, s.v);
                next = panel(next, j == -s.hv, s.v.getOpposite());
            }
        }
        return next;
    }

    private static BlockState panel(BlockState state, boolean outer, Direction face) {
        return outer ? state.setValue(PANELS.get(face), true) : state;
    }

    private static void set(Level level, BlockPos pos, BooleanProperty property, boolean value) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(property) && state.getValue(property) != value) {
            level.setBlock(pos, state.setValue(property, value), Block.UPDATE_CLIENTS);
        }
    }

    /** Safety net for wall blocks left shaped (turbine gone while unloaded, moved by a contraption). */
    public static void releaseIfOrphaned(BlockState state, ServerLevel level, BlockPos pos) {
        boolean claimed = MultiblockRegistry.affecting(level, pos, AbstractTurbineControllerBlockEntity.class).stream()
                .anyMatch(controller -> controller.getStructure().map(s -> s.bounds().isInside(pos)).orElse(false));
        if (!claimed) {
            BlockState released = withoutPanels(state);
            if (released.hasProperty(HIDDEN)) {
                released = released.setValue(HIDDEN, false);
            }
            level.setBlock(pos, released, Block.UPDATE_CLIENTS);
        }
    }
}
