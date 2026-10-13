package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Which LED display modules show as one display. A row of slots runs left to right as the operator
 * sees it, through both columns of a block and on into the next block of the same kind joined to
 * its right (not round a desk's corner). LED modules side by side in a row are one display, cut
 * every {@link #MAX_CELLS} modules (see {@link LedRows}); its leftmost module, the head, holds the
 * setup the whole display shows. Worked out from block states and module types that clients have
 * too, so the server and the renderer agree.
 */
public final class LedGroups {
    /** At most this many modules (four blocks) make one display. */
    public static final int MAX_CELLS = 8;
    /** A row is not followed further than this, so a ring of blocks cannot loop forever. */
    private static final int MAX_WALK = ControlDeskBlockEntity.MAX_CONSOLE * 2;

    private LedGroups() {}

    /** One slot of one block. */
    public record Cell(BlockPos pos, int slot) {}

    /** A display, left to right: the first cell is its head. */
    public record Group(List<Cell> cells) {
        public Cell head() {
            return cells.get(0);
        }

        public int length() {
            return cells.size();
        }

        /** How many of its cells lie in the head's block. */
        public int cellsInHeadBlock() {
            int count = 0;
            for (Cell cell : cells) {
                if (cell.pos().equals(head().pos())) {
                    count++;
                }
            }
            return count;
        }
    }

    /** The display the LED module at {@code pos} and {@code slot} is part of, or null when there is none there. */
    @Nullable
    public static Group groupOf(BlockGetter level, BlockPos pos, int slot) {
        Cell cell = new Cell(pos.immutable(), slot);
        if (!isLed(level, cell)) {
            return null;
        }
        Cell start = cell;
        for (int i = 0; i < MAX_WALK; i++) {
            Cell previous = step(level, start, false);
            if (previous == null || !isLed(level, previous) || previous.equals(cell)) {
                break;
            }
            start = previous;
        }
        List<Cell> run = new ArrayList<>();
        run.add(start);
        for (Cell next = step(level, start, true); next != null && isLed(level, next) && run.size() < MAX_WALK
                && !next.equals(start); next = step(level, next, true)) {
            run.add(next);
        }
        LedRows.Span piece = LedRows.pieceOf(run.indexOf(cell), run.size(), MAX_CELLS);
        return new Group(List.copyOf(run.subList(piece.start(), piece.start() + piece.length())));
    }

    /** The displays whose head is in the block at {@code pos}. */
    public static List<Group> headedAt(BlockGetter level, BlockPos pos) {
        List<Group> groups = new ArrayList<>();
        for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
            Group group = groupOf(level, pos, slot);
            if (group != null && group.head().pos().equals(pos) && group.head().slot() == slot) {
                groups.add(group);
            }
        }
        return groups;
    }

    /** The head of the display a module belongs to: itself when it is no LED display or stands alone. */
    public static Cell headOf(BlockGetter level, BlockPos pos, int slot) {
        Group group = groupOf(level, pos, slot);
        return group == null ? new Cell(pos.immutable(), slot) : group.head();
    }

    private static boolean isLed(BlockGetter level, Cell cell) {
        BlockState state = level.getBlockState(cell.pos());
        ModuleSurface surface = ModuleSurface.of(state);
        if (surface == null || !surface.hasSlot(state, cell.slot())
                || !(level.getBlockEntity(cell.pos()) instanceof ControlDeskBlockEntity desk)) {
            return false;
        }
        ControlModule module = desk.module(cell.slot());
        return module != null && module.type() == ModuleType.LED_DISPLAY;
    }

    /** The next cell along the row to the operator's right (or left), or null where the row ends. */
    @Nullable
    private static Cell step(BlockGetter level, Cell cell, boolean right) {
        int row = ModuleSurface.row(cell.slot());
        int column = ModuleSurface.column(cell.slot());
        if (right ? column == 0 : column == 1) {
            return new Cell(cell.pos(), ModuleSurface.slot(row, right ? 1 : 0));
        }
        BlockState state = level.getBlockState(cell.pos());
        ModuleSurface surface = ModuleSurface.of(state);
        if (surface == null || !surface.straightRows(state)
                || !state.getValue(right ? ModuleSurfaceBlock.RIGHT : ModuleSurfaceBlock.LEFT)) {
            return null;
        }
        BlockPos next = cell.pos().relative(right ? ModuleSurface.rightOf(state) : ModuleSurface.leftOf(state));
        BlockState nextState = level.getBlockState(next);
        if (nextState.getBlock() != state.getBlock() || !surface.straightRows(nextState)
                || nextState.getValue(ModuleSurfaceBlock.FACING) != state.getValue(ModuleSurfaceBlock.FACING)) {
            return null;
        }
        return new Cell(next.immutable(), ModuleSurface.slot(row, right ? 0 : 1));
    }
}
