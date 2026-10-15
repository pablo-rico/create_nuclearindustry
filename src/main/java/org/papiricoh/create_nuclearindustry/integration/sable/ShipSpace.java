package org.papiricoh.create_nuclearindustry.integration.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Where things really are when some blocks ride a ship. Sable keeps a ship's blocks in the real
 * level, in a far-away plot, and draws and collides them somewhere else: a block position on a ship
 * is a plot position, while players and entities stay in world space. Anything that measures a
 * block against a player, an entity or the terrain goes through here first.
 *
 * <p>Without Sable every position is already a world position: {@link #toWorld} is the identity,
 * distances are plain and there are no ships.
 */
public final class ShipSpace {
    /** One ship, as seen by the rest of the mod. Its local space is the plot. */
    public interface Ship {
        UUID id();

        Level level();

        /** Where the ship is in the world, a loose box around all its blocks. */
        AABB worldBounds();

        /** The block box its blocks take in the plot. */
        BoundingBox localBounds();

        Vec3 toWorld(Vec3 local);

        Vec3 toLocal(Vec3 world);

        Vec3 toWorldDirection(Vec3 local);

        Vec3 toLocalDirection(Vec3 world);

        /** The ship's velocity at a world point, blocks per second. */
        Vec3 velocity(Vec3 worldPoint);

        /** The ship's mass, in Sable's units (a plain block weighs 1). */
        double mass();

        /** Pushes the ship with a world-space impulse applied at a world point; it spins if off its centre. */
        void impulse(Vec3 worldPoint, Vec3 worldImpulse);

        /** This mod's data kept with the ship, saved and loaded with it. Empty if none. */
        CompoundTag data(String key);

        void setData(String key, CompoundTag tag);

        boolean isRemoved();

        /** The world's "down" in the ship's axes, as the nearest block face. */
        default Direction localDown() {
            Vec3 down = toLocalDirection(new Vec3(0, -1, 0));
            return Direction.getNearest(down.x, down.y, down.z);
        }
    }

    /** What a Sable install answers; {@link #NONE} without one. */
    interface Backend {
        @Nullable Ship shipAt(Level level, Vec3 position);

        List<Ship> shipsIn(Level level, AABB worldBox);

        @Nullable Ship riding(Entity entity);

        @Nullable Ship byId(Level level, UUID id);

        List<Ship> all(Level level);

        boolean inPlotGrid(Level level, Vec3 position);
    }

    static final Backend NONE = new Backend() {
        @Override
        public Ship shipAt(Level level, Vec3 position) {
            return null;
        }

        @Override
        public List<Ship> shipsIn(Level level, AABB worldBox) {
            return List.of();
        }

        @Override
        public Ship riding(Entity entity) {
            return null;
        }

        @Override
        public Ship byId(Level level, UUID id) {
            return null;
        }

        @Override
        public List<Ship> all(Level level) {
            return List.of();
        }

        @Override
        public boolean inPlotGrid(Level level, Vec3 position) {
            return false;
        }
    };

    private static volatile Backend backend = NONE;

    private ShipSpace() {}

    static void install(Backend installed) {
        backend = installed;
    }

    /** True when ships can exist at all. */
    public static boolean enabled() {
        return backend != NONE;
    }

    /** The ship whose plot holds this position, if any. */
    public static @Nullable Ship shipAt(Level level, Vec3 position) {
        return backend == NONE ? null : backend.shipAt(level, position);
    }

    public static @Nullable Ship shipAt(Level level, BlockPos pos) {
        return backend == NONE ? null : backend.shipAt(level, Vec3.atCenterOf(pos));
    }

    public static boolean onShip(Level level, BlockPos pos) {
        return backend != NONE && backend.inPlotGrid(level, Vec3.atCenterOf(pos)) && shipAt(level, pos) != null;
    }

    /** The ships whose world box meets this one. */
    public static List<Ship> shipsIn(Level level, AABB worldBox) {
        return backend == NONE ? List.of() : backend.shipsIn(level, worldBox);
    }

    /** The ship an entity stands on or rides, if any. */
    public static @Nullable Ship riding(Entity entity) {
        return backend == NONE ? null : backend.riding(entity);
    }

    public static @Nullable Ship byId(Level level, UUID id) {
        return backend == NONE ? null : backend.byId(level, id);
    }

    /** Every loaded ship of a level. */
    public static List<Ship> all(Level level) {
        return backend == NONE ? List.of() : backend.all(level);
    }

    /** Where a position really is: a plot position goes to the world, anything else stays. */
    public static Vec3 toWorld(Level level, Vec3 position) {
        if (backend == NONE) {
            return position;
        }
        Ship ship = backend.shipAt(level, position);
        return ship == null ? position : ship.toWorld(position);
    }

    /** The world position of a block's centre. */
    public static Vec3 toWorld(Level level, BlockPos pos) {
        return toWorld(level, Vec3.atCenterOf(pos));
    }

    /** The world block of a block, for code that files things by world block or chunk. */
    public static BlockPos worldBlock(Level level, BlockPos pos) {
        return backend == NONE ? pos : BlockPos.containing(toWorld(level, pos));
    }

    /** Squared distance between two points, each taken where it really is. */
    public static double distanceSqr(Level level, Vec3 a, Vec3 b) {
        return backend == NONE ? a.distanceToSqr(b) : toWorld(level, a).distanceToSqr(toWorld(level, b));
    }

    public static boolean closerThan(Level level, BlockPos a, BlockPos b, double distance) {
        if (backend == NONE) {
            return a.closerThan(b, distance);
        }
        return distanceSqr(level, Vec3.atCenterOf(a), Vec3.atCenterOf(b)) < distance * distance;
    }

    /**
     * A world point seen from where a block's neighbours are laid out: on a ship, its plot. For
     * comparing a player or the camera with block positions, e.g. how far a machine is.
     */
    public static Vec3 localView(Level level, BlockPos anchor, Vec3 world) {
        Ship ship = shipAt(level, anchor);
        return ship == null ? world : ship.toLocal(world);
    }

    /** The world's "down" at a block, in the axes its neighbours are laid out in. */
    public static Direction localDown(Level level, BlockPos pos) {
        Ship ship = shipAt(level, pos);
        return ship == null ? Direction.DOWN : ship.localDown();
    }
}
