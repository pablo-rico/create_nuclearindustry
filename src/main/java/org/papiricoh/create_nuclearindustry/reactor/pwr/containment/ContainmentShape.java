package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * A containment building as its monitor last measured it: the sealed air inside, how much wall
 * the air touches, and its weakest point.
 * <p>
 * Internal obstacles (the reactor, machines) are walls of the flood fill too, so only lines that
 * lead outside count for the building's strength. "Outside" is the open air around the building:
 * a second flood fill from the faces of a box {@link #MARGIN} blocks around it, so pockets of air
 * sealed inside a machine never count. Looking from the building's air straight through a wall,
 * a line leads outside if it reaches that open air within {@link #MAX_LINE} blocks without
 * meeting the building's air again. Every solid block along it shares the load, so the strongest
 * of them is what has to give way; lines through things standing inside, into the ground or
 * through very thick walls never decide anything.
 */
public final class ContainmentShape {
    /** Blocks around the building that the search for open air covers. */
    public static final int MARGIN = 4;
    /** Longest line through a wall, pockets included. */
    public static final int MAX_LINE = 12;
    /** Vents and recombiners beyond these counts are ignored. */
    private static final int MAX_DEVICES = 64;

    private final SealedVolume volume;
    private final BoundingBox bounds;
    private final int faces;
    private final double weakestRating;
    private final List<BlockPos> weakestWall;
    private final List<BlockPos> recombiners;
    private final List<BlockPos> vents;

    private ContainmentShape(SealedVolume volume, BoundingBox bounds, int faces, double weakestRating, List<BlockPos> weakestWall,
                             List<BlockPos> recombiners, List<BlockPos> vents) {
        this.volume = volume;
        this.bounds = bounds;
        this.faces = faces;
        this.weakestRating = weakestRating;
        this.weakestWall = weakestWall;
        this.recombiners = recombiners;
        this.vents = vents;
    }

    /** Measures a fill that came back {@link SealedVolume.State#SEALED}. */
    public static ContainmentShape analyze(Level level, SealedVolume volume) {
        BoundingBox bounds = new BoundingBox(volume.minX(), volume.minY(), volume.minZ(), volume.maxX(), volume.maxY(), volume.maxZ());
        BoundingBox around = bounds.inflatedBy(MARGIN);
        LongOpenHashSet outside = openAir(level, volume, around);
        int faces = 0;
        double weakest = Double.POSITIVE_INFINITY;
        List<BlockPos> weakestWall = List.of();
        List<BlockPos> recombiners = new ArrayList<>();
        List<BlockPos> vents = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (long packed : volume.wallCells()) {
            int x = SealedVolume.unpackX(packed), y = SealedVolume.unpackY(packed), z = SealedVolume.unpackZ(packed);
            BlockPos wall = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(wall);
            if (state.is(PwrRegistry.RECOMBINER.get()) && recombiners.size() < MAX_DEVICES) {
                recombiners.add(wall);
            } else if (state.is(PwrRegistry.FILTERED_VENT.get()) && vents.size() < MAX_DEVICES) {
                vents.add(wall);
            }
            for (Direction direction : Direction.values()) {
                int dx = direction.getStepX(), dy = direction.getStepY(), dz = direction.getStepZ();
                if (!volume.contains(x - dx, y - dy, z - dz)) {
                    continue;
                }
                faces++;
                double strength = 0.0;
                List<BlockPos> line = new ArrayList<>();
                boolean leadsOut = false;
                for (int step = 0; step < MAX_LINE; step++) {
                    int cx = x + dx * step, cy = y + dy * step, cz = z + dz * step;
                    if (!around.isInside(cx, cy, cz) || outside.contains(SealedVolume.pack(cx, cy, cz))) {
                        leadsOut = true;
                        break;
                    }
                    if (volume.contains(cx, cy, cz)) {
                        break;
                    }
                    cursor.set(cx, cy, cz);
                    if (level.isOutsideBuildHeight(cursor) || !level.isLoaded(cursor)) {
                        break;
                    }
                    BlockState layer = level.getBlockState(cursor);
                    if (ContainmentSurvey.isWall(layer, level, cursor)) {
                        strength = Math.max(strength, ContainmentRating.of(layer, level, cursor));
                        line.add(cursor.immutable());
                    }
                }
                if (leadsOut && strength < weakest) {
                    weakest = strength;
                    weakestWall = List.copyOf(line);
                }
            }
        }
        return new ContainmentShape(volume, bounds, faces, weakest, weakestWall, List.copyOf(recombiners), List.copyOf(vents));
    }

    /**
     * The air around the building that is open to the world: everything gas reaches inside
     * {@code around} from its faces without entering the building. Unloaded and out-of-world cells
     * count as open.
     */
    private static LongOpenHashSet openAir(Level level, SealedVolume volume, BoundingBox around) {
        LongOpenHashSet open = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = around.minX(); x <= around.maxX(); x++) {
            for (int y = around.minY(); y <= around.maxY(); y++) {
                boolean side = x == around.minX() || x == around.maxX() || y == around.minY() || y == around.maxY();
                if (side) {
                    for (int z = around.minZ(); z <= around.maxZ(); z++) {
                        seed(level, volume, cursor, open, queue, x, y, z);
                    }
                } else {
                    seed(level, volume, cursor, open, queue, x, y, around.minZ());
                    seed(level, volume, cursor, open, queue, x, y, around.maxZ());
                }
            }
        }
        while (!queue.isEmpty()) {
            long cell = queue.dequeueLong();
            int x = SealedVolume.unpackX(cell), y = SealedVolume.unpackY(cell), z = SealedVolume.unpackZ(cell);
            for (Direction direction : Direction.values()) {
                int nx = x + direction.getStepX(), ny = y + direction.getStepY(), nz = z + direction.getStepZ();
                if (around.isInside(nx, ny, nz)) {
                    seed(level, volume, cursor, open, queue, nx, ny, nz);
                }
            }
        }
        return open;
    }

    /** Adds the cell to the open air if gas passes it, and queues it to spread from. */
    private static void seed(Level level, SealedVolume volume, BlockPos.MutableBlockPos cursor, LongOpenHashSet open,
                             LongArrayFIFOQueue queue, int x, int y, int z) {
        long key = SealedVolume.pack(x, y, z);
        if (open.contains(key) || volume.contains(x, y, z)) {
            return;
        }
        cursor.set(x, y, z);
        if (level.isOutsideBuildHeight(cursor) || !level.isLoaded(cursor)) {
            open.add(key);
            return;
        }
        if (!ContainmentSurvey.isWall(level.getBlockState(cursor), level, cursor)) {
            open.add(key);
            queue.enqueue(key);
        }
    }

    /** Blocks of air inside, m³. */
    public int volume() {
        return volume.volume();
    }

    /** Faces of solid blocks the air touches, m²: what steam condenses on. */
    public int wallArea() {
        return faces;
    }

    /** The air inside, without the walls. */
    public BoundingBox bounds() {
        return bounds;
    }

    /** Absolute pressure the weakest wall holds, bar. */
    public double weakestRating() {
        return weakestRating;
    }

    /** The wall blocks that give way first, from the inside out. */
    public List<BlockPos> weakestWall() {
        return weakestWall;
    }

    public List<BlockPos> recombiners() {
        return recombiners;
    }

    public List<BlockPos> vents() {
        return vents;
    }

    /** Whether {@code pos} is air inside the building. */
    public boolean contains(BlockPos pos) {
        return volume.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Whether {@code pos} is a solid block the air inside touches: the walls, and anything standing inside. */
    public boolean isWall(BlockPos pos) {
        return volume.isWall(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Whether a structure in {@code box} stands inside the building: the box lies within the air's
     * bounds and air touches it from at least one side.
     */
    public boolean encloses(BoundingBox box) {
        if (box.minX() < bounds.minX() || box.minY() < bounds.minY() || box.minZ() < bounds.minZ()
                || box.maxX() > bounds.maxX() || box.maxY() > bounds.maxY() || box.maxZ() > bounds.maxZ()) {
            return false;
        }
        int cx = (box.minX() + box.maxX()) / 2, cy = (box.minY() + box.maxY()) / 2, cz = (box.minZ() + box.maxZ()) / 2;
        return volume.contains(box.minX() - 1, cy, cz) || volume.contains(box.maxX() + 1, cy, cz)
                || volume.contains(cx, box.minY() - 1, cz) || volume.contains(cx, box.maxY() + 1, cz)
                || volume.contains(cx, cy, box.minZ() - 1) || volume.contains(cx, cy, box.maxZ() + 1);
    }
}
