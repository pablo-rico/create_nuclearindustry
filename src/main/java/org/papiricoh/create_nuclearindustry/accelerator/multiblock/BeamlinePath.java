package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One beamline the walker followed: its cells in beam order (the terminal last), how many of each
 * element it has, where it turns, and the attachments beside it.
 *
 * @param cells       every block of the line, terminal included, in beam order
 * @param waypoints   where the beam enters the line, every bend, and the terminal, in order
 * @param counts      elements of each role on the line
 * @param terminal    the block the line ends in
 * @param terminalRole what it is
 * @param exit        the direction the beam travels into the terminal
 * @param attachments machines beside the line, by role
 */
public record BeamlinePath(List<BlockPos> cells, List<BlockPos> waypoints, Map<BeamElementRole, Integer> counts,
                           BlockPos terminal, BeamElementRole terminalRole, Direction exit,
                           Map<AttachmentRole, List<BlockPos>> attachments) {

    public int count(BeamElementRole role) {
        return counts.getOrDefault(role, 0);
    }

    /** Elements the beam travels through, the terminal not counted. */
    public int elements() {
        return Math.max(0, cells.size() - 1);
    }

    public int magnets() {
        int magnets = 0;
        for (Map.Entry<BeamElementRole, Integer> entry : counts.entrySet()) {
            if (entry.getKey().magnet()) {
                magnets += entry.getValue();
            }
        }
        return magnets;
    }

    public int superconducting() {
        return count(BeamElementRole.SC_RF_CAVITY) + count(BeamElementRole.SC_DIPOLE);
    }

    static Map<BeamElementRole, Integer> emptyCounts() {
        return new EnumMap<>(BeamElementRole.class);
    }
}
