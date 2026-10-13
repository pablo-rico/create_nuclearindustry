package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the synchrotron ring an injection septum feeds. The septum faces the ring's centre, so for
 * each radius the centre is that many blocks ahead of it; a radius is tried in full only when the
 * three other cardinal points of its circle hold ring elements. Every cell of the ring must hold one,
 * with exactly one extraction septum, whose facing is where the extraction line leaves.
 */
public final class RingValidator {
    private static final String ERR = "create_nuclearindustry.accelerator.error.";

    /** A ring and its extraction line, or the reason there is none. */
    public record Result(RingSection ring, BeamlinePath extraction, String error) {
        public boolean ok() {
            return ring != null;
        }
    }

    private RingValidator() {}

    public static Result find(Level level, BlockPos injection, Direction incoming, LongOpenHashSet visited) {
        BlockState septum = level.getBlockState(injection);
        if (!(septum.getBlock() instanceof BeamElementBlock element)) {
            return error("no_ring", injection);
        }
        Direction inward = element.beamExit(septum);
        if (inward.getAxis().isVertical()) {
            return error("no_ring", injection);
        }
        int maxRadius = Math.max(RingGeometry.MIN_RADIUS, AcceleratorConfig.maxRingRadius);
        for (int r = RingGeometry.MIN_RADIUS; r <= maxRadius; r++) {
            BlockPos center = injection.relative(inward, r);
            if (!probe(level, center, r, inward)) {
                continue;
            }
            return scan(level, center, RingGeometry.of(r), injection, visited);
        }
        return error("no_ring", injection);
    }

    /** The three cardinal points of the circle other than the septum hold ring elements. */
    private static boolean probe(Level level, BlockPos center, int r, Direction inward) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (d == inward.getOpposite()) {
                continue;
            }
            if (ringRole(level, center.relative(d, r)) == null) {
                return false;
            }
        }
        return true;
    }

    private static BeamElementRole ringRole(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BeamElementBlock element) {
            BeamElementRole role = element.beamRole(state);
            return role.ringElement() ? role : null;
        }
        return null;
    }

    private static Result scan(Level level, BlockPos center, RingGeometry geometry, BlockPos injection, LongOpenHashSet visited) {
        Map<BeamElementRole, Integer> counts = new EnumMap<>(BeamElementRole.class);
        int[] sectorQuads = new int[geometry.sectorCount()];
        Map<AttachmentRole, Set<BlockPos>> attachments = new EnumMap<>(AttachmentRole.class);
        BlockPos extraction = null;
        Direction extractionDir = null;
        int injections = 0;
        for (int i = 0; i < geometry.cellCount(); i++) {
            BlockPos pos = center.offset(geometry.dx(i), 0, geometry.dz(i));
            BeamElementRole role = ringRole(level, pos);
            if (role == null) {
                return error("ring_gap", pos, geometry.radius());
            }
            counts.merge(role, 1, Integer::sum);
            if (role == BeamElementRole.QUADRUPOLE) {
                sectorQuads[geometry.sector(i)]++;
            } else if (role == BeamElementRole.INJECTION) {
                injections++;
            } else if (role == BeamElementRole.EXTRACTION) {
                if (extraction != null) {
                    return error("two_extractions", pos);
                }
                extraction = pos;
                BlockState state = level.getBlockState(pos);
                extractionDir = ((BeamElementBlock) state.getBlock()).beamExit(state);
            }
            if (!pos.equals(injection) && !visited.add(pos.asLong())) {
                return error("loop", pos);
            }
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof AttachmentRole.Block machine) {
                    attachments.computeIfAbsent(machine.attachmentRole(state), k -> new LinkedHashSet<>()).add(next.immutable());
                }
            }
        }
        if (injections != 1) {
            return error("two_injections", injection);
        }
        if (extraction == null) {
            return error("no_extraction", injection);
        }
        BlockPos first = extraction.relative(extractionDir);
        if (extractionDir.getAxis().isVertical() || geometry.isRing(first.getX() - center.getX(), first.getZ() - center.getZ())) {
            return error("extraction_inward", extraction);
        }
        BeamlineWalker.Result line = BeamlineWalker.walk(level, first, extractionDir, AcceleratorConfig.maxLineLength, visited);
        if (!line.ok()) {
            return new Result(null, null, line.error());
        }
        Map<AttachmentRole, List<BlockPos>> frozen = new EnumMap<>(AttachmentRole.class);
        attachments.forEach((role, set) -> frozen.put(role, List.copyOf(set)));
        RingSection ring = new RingSection(center.immutable(), geometry, counts, injection.immutable(), extraction.immutable(),
                extractionDir, sectorQuads, frozen);
        return new Result(ring, line.path(), null);
    }

    private static Result error(String key, BlockPos pos, Object... extra) {
        Object[] args = new Object[3 + extra.length];
        args[0] = pos.getX();
        args[1] = pos.getY();
        args[2] = pos.getZ();
        System.arraycopy(extra, 0, args, 3, extra.length);
        return new Result(null, null, MultiblockText.message(ERR + key, args));
    }
}
