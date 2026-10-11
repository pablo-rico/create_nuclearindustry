package org.papiricoh.create_nuclearindustry.turbine;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A validated box turbine. Coordinates: the controller sits at the centre of the front end
 * face; {@code inward} runs along the rotor axis; {@code u} and {@code v} span the
 * cross-section, with half-extents {@code hu} and {@code hv} (outer walls included).
 */
public final class TurbineStructure {
    public final TurbineTier tier;
    public final BlockPos controllerPos;
    public final Direction outward;
    public final Direction inward;
    public final Direction.Axis axis;
    public final Direction u;
    public final Direction v;
    public final int hu;
    public final int hv;
    /** Outer length along the axis, both end walls included. */
    public final int length;
    public final List<BlockPos> rotors = new ArrayList<>();
    /** {@link #rotors} as packed positions, for {@link #ownsRotor}. */
    private LongSet rotorSet;
    public final LongSet ports = new LongOpenHashSet();
    public int inputPorts;
    public int outputPorts;
    /** Exhaust ports send the expanded steam to a cooling tower instead of condensing it inside. */
    public int exhaustPorts;
    public int glassCount;
    private BoundingBox bounds;

    public TurbineStructure(TurbineTier tier, BlockPos controllerPos, Direction outward, int hu, int hv, int length) {
        this.tier = tier;
        this.controllerPos = controllerPos.immutable();
        this.outward = outward;
        this.inward = outward.getOpposite();
        this.axis = outward.getAxis();
        Direction[] cross = crossSection(axis);
        this.u = cross[0];
        this.v = cross[1];
        this.hu = hu;
        this.hv = hv;
        this.length = length;
    }

    /** Two directions perpendicular to {@code axis}, fixed so geometry is reproducible. */
    public static Direction[] crossSection(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Direction[]{Direction.UP, Direction.SOUTH};
            case Y -> new Direction[]{Direction.EAST, Direction.SOUTH};
            case Z -> new Direction[]{Direction.UP, Direction.EAST};
        };
    }

    /** Position {@code a} blocks in from the controller, offset {@code i} along u and {@code j} along v. */
    public BlockPos at(int a, int i, int j) {
        return controllerPos.relative(inward, a).relative(u, i).relative(v, j);
    }

    public int rotorCount() {
        return rotors.size();
    }

    public int interiorVolume() {
        return (2 * hu - 1) * (2 * hv - 1) * (length - 2);
    }

    /** Working fluid the turbine can expand per tick: rotors and port pairs both limit it. */
    public int throughputLimit() {
        int rotorLimit = rotorCount() * tier.perRotor;
        int portLimit = Math.min(inputPorts, outputPorts + exhaustPorts) * tier.perPort;
        return Math.min(rotorLimit, portLimit);
    }

    /** A turbine with any exhaust port exhausts all its steam through them. */
    public boolean exhausting() {
        return exhaustPorts > 0;
    }

    public int tankCapacity() {
        return tier.baseTank + interiorVolume() * tier.tankPerInteriorBlock;
    }

    public boolean ownsPort(BlockPos pos) {
        return ports.contains(pos.asLong());
    }

    /**
     * Whether {@code pos} is one of this turbine's rotor stages: a hash lookup, since every formed
     * rotor asks on its random ticks. The set is built from {@link #rotors} once it is complete.
     */
    public boolean ownsRotor(BlockPos pos) {
        LongSet set = rotorSet;
        if (set == null || set.size() != rotors.size()) {
            set = new LongOpenHashSet(rotors.size());
            for (BlockPos rotor : rotors) {
                set.add(rotor.asLong());
            }
            rotorSet = set;
        }
        return set.contains(pos.asLong());
    }

    /** The geometry never changes, so the box is worked out once. */
    public BoundingBox bounds() {
        if (bounds == null) {
            bounds = BoundingBox.fromCorners(at(0, -hu, -hv), at(length - 1, hu, hv));
        }
        return bounds;
    }

    /** Every block of the box, interior air included, for change tracking. */
    public List<BlockPos> allPositions() {
        List<BlockPos> positions = new ArrayList<>(length * (2 * hu + 1) * (2 * hv + 1));
        for (int a = 0; a < length; a++) {
            for (int i = -hu; i <= hu; i++) {
                for (int j = -hv; j <= hv; j++) {
                    positions.add(at(a, i, j));
                }
            }
        }
        return positions;
    }

    /** Width x height x length, as players count it (height is the vertical side when there is one). */
    public String dimensionsText() {
        int a = 2 * hu + 1;
        int b = 2 * hv + 1;
        return u == Direction.UP ? b + "x" + a + "x" + length : a + "x" + b + "x" + length;
    }
}
