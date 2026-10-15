package org.papiricoh.create_nuclearindustry.civildefense.shelter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The certified shelters of each level, by the filtration unit that certified them. Kept in memory
 * only: units certify their room again on their first survey after loading. Radiation and blast
 * waves ask it whether a point is inside one.
 */
public final class ShelterRegistry {
    private static final Map<Level, Map<BlockPos, Shelter>> SHELTERS = new WeakHashMap<>();

    /** A certified room; {@code filtering} while its unit turns fast enough. */
    public record Shelter(BlockPos unit, SealedVolume room, boolean filtering) {
        public boolean contains(BlockPos pos) {
            return pos.getX() >= room.minX() && pos.getX() <= room.maxX()
                    && pos.getY() >= room.minY() && pos.getY() <= room.maxY()
                    && pos.getZ() >= room.minZ() && pos.getZ() <= room.maxZ()
                    && room.contains(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private ShelterRegistry() {}

    /** Records, or updates, the room the unit at {@code unit} certified. */
    public static void certify(Level level, BlockPos unit, SealedVolume room, boolean filtering) {
        if (level.isClientSide) {
            return;
        }
        SHELTERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(unit.immutable(), new Shelter(unit.immutable(), room, filtering));
    }

    /** The unit at {@code unit} no longer certifies anything: gone, unloaded, or its room failed. */
    public static void revoke(Level level, BlockPos unit) {
        Map<BlockPos, Shelter> shelters = SHELTERS.get(level);
        if (shelters != null) {
            shelters.remove(unit);
        }
    }

    /** The certified shelter around {@code pos}, preferring one that is filtering, or {@code null}. */
    @Nullable
    public static Shelter at(Level level, BlockPos pos) {
        Map<BlockPos, Shelter> shelters = SHELTERS.get(level);
        if (shelters == null || shelters.isEmpty()) {
            return null;
        }
        Shelter found = null;
        for (Shelter shelter : shelters.values()) {
            if (shelter.contains(pos)) {
                if (shelter.filtering()) {
                    return shelter;
                }
                found = shelter;
            }
        }
        return found;
    }

    public static boolean isSheltered(Level level, Vec3 point) {
        return around(level, point) != null;
    }

    /** The shelter around a world point: on the ground, or in the plot of a ship the point is in. */
    @Nullable
    private static Shelter around(Level level, Vec3 point) {
        Shelter shelter = at(level, BlockPos.containing(point));
        if (shelter != null || !org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.enabled()) {
            return shelter;
        }
        for (org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.Ship ship : org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.shipsIn(level, new net.minecraft.world.phys.AABB(point, point).inflate(0.5))) {
            shelter = at(level, BlockPos.containing(ship.toLocal(point)));
            if (shelter != null) {
                return shelter;
            }
        }
        return null;
    }

    /** Fraction of fallout and ground dose that reaches {@code body}: the filter's inside a running shelter, else all. */
    public static double doseFactor(Level level, Vec3 body) {
        Shelter shelter = around(level, body);
        return shelter != null && shelter.filtering() ? CivilDefenseConfig.shelterFilterFactor : 1.0;
    }

    /** Fraction of a blast wave's overpressure that reaches {@code body}: less inside a certified shelter. */
    public static double blastFactor(Level level, Vec3 body) {
        return isSheltered(level, body) ? CivilDefenseConfig.shelterBlastFactor : 1.0;
    }

    /** Forgets every shelter of a level; for tests. */
    public static void clear(Level level) {
        SHELTERS.remove(level);
    }
}
