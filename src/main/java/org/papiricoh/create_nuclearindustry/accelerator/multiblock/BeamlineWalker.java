package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Follows a beamline from its first block until it ends in a terminal: every block on the way must be
 * a line element along the walking axis; a bend turns the walk to its exit, which must be across the
 * incoming beam. Machines beside each block (not ahead or behind) are collected as attachments.
 * Beamlines are horizontal.
 */
public final class BeamlineWalker {
    private static final String ERR = "create_nuclearindustry.accelerator.error.";

    /** A walked line, or the reason it is not one. */
    public record Result(BeamlinePath path, String error) {
        public boolean ok() {
            return path != null;
        }
    }

    private BeamlineWalker() {}

    /**
     * @param start   first block after the beam's source (ion source or extraction septum)
     * @param dir     direction the beam travels
     * @param maxLength most blocks the line may have
     * @param visited blocks already part of the structure; walking into one is a loop
     */
    public static Result walk(Level level, BlockPos start, Direction dir, int maxLength, LongOpenHashSet visited) {
        List<BlockPos> cells = new ArrayList<>();
        List<BlockPos> waypoints = new ArrayList<>();
        Map<BeamElementRole, Integer> counts = new EnumMap<>(BeamElementRole.class);
        Map<AttachmentRole, Set<BlockPos>> attachments = new EnumMap<>(AttachmentRole.class);
        waypoints.add(start.immutable());
        BlockPos pos = start;
        Direction d = dir;
        for (int i = 0; i < maxLength; i++) {
            if (!visited.add(pos.asLong())) {
                return error("loop", pos);
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BeamElementBlock element)) {
                return error("gap", pos);
            }
            BeamElementRole role = element.beamRole(state);
            if (!role.lineElement() && !role.terminal()) {
                return error("not_line_element", pos);
            }
            Direction.Axis axis = element.beamAxis(state);
            if (axis != null && axis != d.getAxis()) {
                return error("misaligned", pos);
            }
            cells.add(pos.immutable());
            counts.merge(role, 1, Integer::sum);
            if (role.terminal()) {
                waypoints.add(pos.immutable());
                return new Result(new BeamlinePath(List.copyOf(cells), List.copyOf(waypoints), counts, pos.immutable(), role, d,
                        freeze(attachments)), null);
            }
            Direction incoming = d;
            if (role == BeamElementRole.BEND) {
                Direction exit = element.beamExit(state);
                if (exit.getAxis() == d.getAxis() || exit.getAxis().isVertical()) {
                    return error("bend_aligned", pos);
                }
                waypoints.add(pos.immutable());
                d = exit;
            }
            collectAttachments(level, pos, incoming, d, attachments);
            pos = pos.relative(d);
        }
        return error("too_long", pos, maxLength);
    }

    private static void collectAttachments(Level level, BlockPos pos, Direction incoming, Direction outgoing,
                                           Map<AttachmentRole, Set<BlockPos>> attachments) {
        for (Direction side : Direction.values()) {
            if (side == outgoing || side == incoming.getOpposite()) {
                continue;
            }
            BlockPos next = pos.relative(side);
            BlockState state = level.getBlockState(next);
            if (state.getBlock() instanceof AttachmentRole.Block machine) {
                attachments.computeIfAbsent(machine.attachmentRole(state), r -> new LinkedHashSet<>()).add(next.immutable());
            }
        }
    }

    /** Machines beside a block that is not walked (the ion source). */
    public static void collectAround(Level level, BlockPos pos, Direction beam, Map<AttachmentRole, Set<BlockPos>> into) {
        collectAttachments(level, pos, beam, beam, into);
    }

    private static Map<AttachmentRole, List<BlockPos>> freeze(Map<AttachmentRole, Set<BlockPos>> attachments) {
        Map<AttachmentRole, List<BlockPos>> out = new EnumMap<>(AttachmentRole.class);
        attachments.forEach((role, set) -> out.put(role, List.copyOf(set)));
        return out;
    }

    private static Result error(String key, BlockPos pos, Object... extra) {
        Object[] args = new Object[3 + extra.length];
        args[0] = pos.getX();
        args[1] = pos.getY();
        args[2] = pos.getZ();
        System.arraycopy(extra, 0, args, 3, extra.length);
        return new Result(null, MultiblockText.message(ERR + key, args));
    }
}
