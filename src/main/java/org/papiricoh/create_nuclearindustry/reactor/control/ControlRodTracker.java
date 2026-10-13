package org.papiricoh.create_nuclearindustry.reactor.control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.multiblock.ReactorStructureValidator;

public final class ControlRodTracker {
    private ControlRodTracker() {}

    /**
     * Counts the channel cells a rod fills, standing or carried by a contraption. Each cell counts
     * once; the channels are indexed by column first, so the cost grows with the cells looked at
     * and the rods found, not with channels times rods.
     */
    public static ControlRodScan scan(Level level, ReactorStructureValidator.ReactorStructure structure) {
        if (level == null || structure.controlChannels.isEmpty()) {
            return ControlRodScan.empty();
        }

        int channelCount = structure.controlChannels.size();
        int[] channelX = new int[channelCount];
        int[] channelZ = new int[channelCount];
        int i = 0;
        // The structure's channel order is the set's iteration order: the per-channel split keeps it.
        for (BlockPos channel : structure.controlChannels) {
            channelX[i] = channel.getX();
            channelZ[i++] = channel.getZ();
        }
        ControlRodGrid grid = new ControlRodGrid(channelX, channelZ, structure.bottomY + 1, structure.topY - structure.bottomY - 1);
        int staticSegments = scanStatic(level, grid, channelX, channelZ);
        int movingSegments = scanContraptions(level, structure, grid);
        int expectedSegments = channelCount * structure.innerHeight();
        int inserted = grid.filledCells();
        float insertion = expectedSegments <= 0 ? 0.0f : Math.min(1.0f, inserted / (float) expectedSegments);
        // Each channel on its own: a rod is worth most in the middle of the core, so where they are matters.
        return new ControlRodScan(insertion, staticSegments, movingSegments, inserted, expectedSegments, grid.channelInsertions());
    }

    private static int scanStatic(Level level, ControlRodGrid grid, int[] channelX, int[] channelZ) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int c = 0; c < channelX.length; c++) {
            int column = grid.column(channelX[c], channelZ[c]);
            for (int y = grid.firstY(); y <= grid.lastY(); y++) {
                cursor.set(channelX[c], y, channelZ[c]);
                if (level.getBlockState(cursor).is(AllNuclearBlocks.CONTROL_ROD.get()) && grid.fill(column, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Only the cells under each rod's bounds are tested, each looked up by its column. */
    private static int scanContraptions(Level level, ReactorStructureValidator.ReactorStructure structure, ControlRodGrid grid) {
        int count = 0;
        for (AABB rod : ControlRodGeometry.movingControlRods(level, structure)) {
            int minY = Math.max(grid.firstY(), ControlRodGrid.firstCell(rod.minY));
            int maxY = Math.min(grid.lastY(), ControlRodGrid.lastCell(rod.maxY));
            if (minY > maxY) {
                continue;
            }
            for (int x = ControlRodGrid.firstCell(rod.minX); x <= ControlRodGrid.lastCell(rod.maxX); x++) {
                for (int z = ControlRodGrid.firstCell(rod.minZ); z <= ControlRodGrid.lastCell(rod.maxZ); z++) {
                    int column = grid.column(x, z);
                    if (column < 0) {
                        continue;
                    }
                    for (int y = minY; y <= maxY; y++) {
                        if (ControlRodGeometry.overlapsControlCell(rod, x, y, z) && grid.fill(column, y)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * @param channelInsertions how far each control channel is filled, 0..1, in the structure's channel order
     */
    public record ControlRodScan(float insertionRatio, int staticSegments, int movingSegments, int insertedSegments, int expectedSegments,
                                 double[] channelInsertions) {
        /** A scan known only by its totals (synced to clients): every channel at the average. */
        public ControlRodScan(float insertionRatio, int staticSegments, int movingSegments, int insertedSegments, int expectedSegments) {
            this(insertionRatio, staticSegments, movingSegments, insertedSegments, expectedSegments, new double[]{insertionRatio});
        }

        public static ControlRodScan empty() {
            return new ControlRodScan(0.0f, 0, 0, 0, 0);
        }
    }
}
