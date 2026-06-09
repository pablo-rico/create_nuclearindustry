package org.papiricoh.create_nuclearindustry.reactor.control;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ControlRodTracker {
    private ControlRodTracker() {}

    public static ControlRodScan scan(Level level, ReactorStructureValidator.ReactorStructure structure) {
        if (level == null || structure.controlChannels.isEmpty()) {
            return ControlRodScan.empty();
        }

        Set<BlockPos> insertedPositions = new HashSet<>();
        int staticSegments = scanStatic(level, structure, insertedPositions);
        int movingSegments = scanContraptions(level, structure, insertedPositions);
        int expectedSegments = structure.controlChannels.size() * structure.innerHeight();
        float insertion = expectedSegments <= 0 ? 0.0f : Math.min(1.0f, insertedPositions.size() / (float) expectedSegments);
        return new ControlRodScan(insertion, staticSegments, movingSegments, insertedPositions.size(), expectedSegments);
    }

    private static int scanStatic(Level level, ReactorStructureValidator.ReactorStructure structure, Set<BlockPos> insertedPositions) {
        int count = 0;
        for (BlockPos channel : structure.controlChannels) {
            for (int y = structure.bottomY + 1; y < structure.topY; y++) {
                BlockPos pos = new BlockPos(channel.getX(), y, channel.getZ());
                if (level.getBlockState(pos).is(AllNuclearBlocks.CONTROL_ROD.get()) && insertedPositions.add(pos)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int scanContraptions(Level level, ReactorStructureValidator.ReactorStructure structure, Set<BlockPos> insertedPositions) {
        int count = 0;
        AABB searchBox = createSearchBox(structure);
        for (AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, searchBox)) {
            Contraption contraption = entity.getContraption();
            if (contraption == null) {
                continue;
            }

            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
                StructureTemplate.StructureBlockInfo info = entry.getValue();
                if (!info.state().is(AllNuclearBlocks.CONTROL_ROD.get())) {
                    continue;
                }

                AABB rodBounds = ControlRodGeometry.movingBlockBounds(entity, entry.getKey());
                for (BlockPos channel : structure.controlChannels) {
                    for (int y = structure.bottomY + 1; y < structure.topY; y++) {
                        BlockPos cell = new BlockPos(channel.getX(), y, channel.getZ());
                        if (ControlRodGeometry.overlapsControlCell(rodBounds, cell) && insertedPositions.add(cell)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static AABB createSearchBox(ReactorStructureValidator.ReactorStructure structure) {
        return ControlRodGeometry.createControlRodSearchBox(structure);
    }

    public record ControlRodScan(float insertionRatio, int staticSegments, int movingSegments, int insertedSegments, int expectedSegments) {
        public static ControlRodScan empty() {
            return new ControlRodScan(0.0f, 0, 0, 0, 0);
        }
    }
}
