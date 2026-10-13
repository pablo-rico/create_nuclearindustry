package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;

/**
 * A synchrotron ring found while validating the accelerator.
 *
 * @param center        the ring's centre (at beam height)
 * @param geometry      its cells
 * @param counts        elements of each role in the ring
 * @param injection     the injection septum
 * @param extraction    the extraction septum
 * @param extractionDir where the extracted beam leaves
 * @param sectorQuadrupoles quadrupoles in each sector
 * @param attachments   machines beside the ring, by role
 */
public record RingSection(BlockPos center, RingGeometry geometry, Map<BeamElementRole, Integer> counts, BlockPos injection,
                          BlockPos extraction, Direction extractionDir, int[] sectorQuadrupoles,
                          Map<AttachmentRole, List<BlockPos>> attachments) {

    public int radius() {
        return geometry.radius();
    }

    public int cells() {
        return geometry.cellCount();
    }

    public int count(BeamElementRole role) {
        return counts.getOrDefault(role, 0);
    }

    public int dipoles() {
        return count(BeamElementRole.DIPOLE) + count(BeamElementRole.SC_DIPOLE);
    }

    /** Share of the ring that bends the beam. */
    public double dipoleFraction() {
        return cells() == 0 ? 0 : (double) dipoles() / cells();
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

    public int cavities() {
        return count(BeamElementRole.RF_CAVITY) + count(BeamElementRole.SC_RF_CAVITY);
    }

    public int superconducting() {
        return count(BeamElementRole.SC_RF_CAVITY) + count(BeamElementRole.SC_DIPOLE);
    }

    /** Sectors with no quadrupole: where the beam is lost. */
    public int unfocusedSectors() {
        int count = 0;
        for (int quads : sectorQuadrupoles) {
            if (quads == 0) {
                count++;
            }
        }
        return count;
    }
}
