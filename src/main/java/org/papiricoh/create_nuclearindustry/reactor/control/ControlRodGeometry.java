package org.papiricoh.create_nuclearindustry.reactor.control;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ControlRodGeometry {
    private static final double MIN_OVERLAP = 0.01;

    private ControlRodGeometry() {}

    public static AABB movingBlockBounds(AbstractContraptionEntity entity, BlockPos localPos) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    Vec3 corner = entity.toGlobalVector(new Vec3(
                            localPos.getX() + dx,
                            localPos.getY() + dy,
                            localPos.getZ() + dz
                    ), 1.0f);
                    minX = Math.min(minX, corner.x);
                    minY = Math.min(minY, corner.y);
                    minZ = Math.min(minZ, corner.z);
                    maxX = Math.max(maxX, corner.x);
                    maxY = Math.max(maxY, corner.y);
                    maxZ = Math.max(maxZ, corner.z);
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean overlapsControlCell(AABB rodBounds, BlockPos cellPos) {
        return overlapsControlCell(rodBounds, cellPos.getX(), cellPos.getY(), cellPos.getZ());
    }

    /** {@link #overlapsControlCell(AABB, BlockPos)} without allocating the cell, for loops over many cells. */
    public static boolean overlapsControlCell(AABB rodBounds, int x, int y, int z) {
        return overlapLength(rodBounds.minX, rodBounds.maxX, x, x + 1.0) >= MIN_OVERLAP
                && overlapLength(rodBounds.minY, rodBounds.maxY, y, y + 1.0) >= MIN_OVERLAP
                && overlapLength(rodBounds.minZ, rodBounds.maxZ, z, z + 1.0) >= MIN_OVERLAP;
    }

    /**
     * World bounds of every control rod block carried by a contraption near the reactor. One entity
     * query per call, so callers that test many cells or columns share the result.
     */
    public static List<AABB> movingControlRods(Level level, ReactorStructureValidator.ReactorStructure structure) {
        List<AABB> rods = new ArrayList<>();
        for (AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, createControlRodSearchBox(structure))) {
            Contraption contraption = entity.getContraption();
            if (contraption == null) {
                continue;
            }
            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
                if (entry.getValue().state().is(AllNuclearBlocks.CONTROL_ROD.get())) {
                    rods.add(movingBlockBounds(entity, entry.getKey()));
                }
            }
        }
        return rods;
    }

    public static AABB createControlRodSearchBox(ReactorStructureValidator.ReactorStructure structure) {
        int halfWidth = structure.width / 2;
        BlockPos min = new BlockPos(
                structure.controllerPos.getX() - halfWidth - 2,
                structure.bottomY,
                structure.controllerPos.getZ() - halfWidth - 2
        );
        BlockPos max = new BlockPos(
                structure.controllerPos.getX() + halfWidth + 2,
                structure.topY + structure.innerHeight() + 4,
                structure.controllerPos.getZ() + halfWidth + 2
        );
        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0
        );
    }

    private static double overlapLength(double minA, double maxA, double minB, double maxB) {
        return Math.max(0.0, Math.min(maxA, maxB) - Math.max(minA, minB));
    }
}
