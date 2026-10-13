package org.papiricoh.create_nuclearindustry.coolingtower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A validated cooling tower: its floor centre, shape, controller, ports and fans. */
public final class CoolingTowerStructure {
    public final BlockPos center;
    public final BlockPos controller;
    public final Direction outward;
    public final CoolingTowerGeometry geometry;
    public final Set<Long> ports = new HashSet<>();
    public final List<BlockPos> fans = new ArrayList<>();
    public int inputPorts;
    public int outputPorts;

    public CoolingTowerStructure(BlockPos center, BlockPos controller, Direction outward, CoolingTowerGeometry geometry) {
        this.center = center;
        this.controller = controller;
        this.outward = outward;
        this.geometry = geometry;
    }

    public int baseRadius() {
        return geometry.baseRadius();
    }

    public int height() {
        return geometry.height();
    }

    public BlockPos at(int dx, int y, int dz) {
        return center.offset(dx, y, dz);
    }

    public BoundingBox bounds() {
        int r = geometry.baseRadius();
        return new BoundingBox(center.getX() - r, center.getY(), center.getZ() - r,
                center.getX() + r, center.getY() + geometry.height() - 1, center.getZ() + r);
    }

    /** Floor, wall and interior: every position whose change can break the tower. */
    public List<BlockPos> allPositions() {
        List<BlockPos> positions = new ArrayList<>();
        int r = geometry.baseRadius();
        for (int y = 0; y < geometry.height(); y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (geometry.classify(dx, y, dz) != CoolingTowerGeometry.Cell.OUTSIDE) {
                        positions.add(at(dx, y, dz));
                    }
                }
            }
        }
        return positions;
    }

    public boolean ownsPort(BlockPos pos) {
        return ports.contains(pos.asLong());
    }

    public boolean ownsFan(BlockPos pos) {
        return fans.contains(pos);
    }

    public int tankCapacity() {
        return 16000 + 1000 * geometry.baseRadius();
    }

    public String dimensionsText() {
        return "base radius " + geometry.baseRadius() + ", " + geometry.height() + " high";
    }
}
