package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * The magnetic conduit: a thin evacuated glass tube with a ring of magnets every few pixels that
 * steers antiprotons from one holder to another. It holds nothing in transit, so a conduit that loses
 * power simply stops moving antimatter and cannot explode. Every segment needs {@code fePerBlock}
 * FE per tick; one dark segment stops the whole network.
 *
 * <p>Two ways to move antimatter on a network: from sources (a target station) to sinks (a
 * generator, an injector, or traps) with {@link #plan}, and between traps towards an equal fill with
 * {@link #balance}. Both move at most {@code ratePerTick} per tick and conserve what they move.
 * Pure arithmetic.
 */
public final class ConduitTransfer {
    /** Transfers smaller than this are not worth making, ng. */
    public static final double MIN_TRANSFER = 1.0e-7;

    /**
     * @param fePerBlock  FE per tick each segment needs
     * @param ratePerTick most antimatter a network moves per tick, ng
     * @param maxSegments longest network that still works
     */
    public record Params(int fePerBlock, double ratePerTick, int maxSegments) {
        /** 4 FE/t per block, 0.5 ng/t (a full small trap in about 3 s). */
        public static final Params DEFAULT = new Params(4, 0.5, 256);
    }

    /**
     * @param extract what to take from each source, ng
     * @param insert  what to give each sink, ng
     * @param total   what moves in all, ng
     */
    public record Plan(double[] extract, double[] insert, double total) {}

    private ConduitTransfer() {}

    /** FE per tick a network of this many segments needs. */
    public static long demand(Params p, int segments) {
        return (long) Math.max(0, segments) * Math.max(0, p.fePerBlock());
    }

    /** Whether a network of this many segments, all of them powered or not, may move antimatter. */
    public static boolean flows(Params p, int segments, boolean allPowered) {
        return allPowered && segments > 0 && segments <= Math.max(1, p.maxSegments());
    }

    /** What moves between one holder and another this tick, ng: the least of the rate, what there is and the room. */
    public static double move(double rate, double available, double space) {
        double moved = Math.max(0, Math.min(rate, Math.min(available, space)));
        return moved < MIN_TRANSFER ? 0 : moved;
    }

    /**
     * Sources to sinks: the total is the least of the rate, everything available and all the room.
     * Each source gives in proportion to what it has, each sink takes in proportion to its room.
     */
    public static Plan plan(double rate, double[] available, double[] space) {
        double have = sumPositive(available);
        double room = sumPositive(space);
        double total = move(rate, have, room);
        double[] extract = new double[available.length];
        double[] insert = new double[space.length];
        if (total > 0) {
            for (int i = 0; i < available.length; i++) {
                extract[i] = Math.max(0, available[i]) * total / have;
            }
            for (int i = 0; i < space.length; i++) {
                insert[i] = Math.max(0, space[i]) * total / room;
            }
        }
        return new Plan(extract, insert, total);
    }

    /**
     * Between traps: moves antimatter from the fuller (by share of capacity) to the emptier, towards
     * the network's mean fill, at most {@code rate} per tick. Returns each holder's change, ng:
     * negative gives, positive takes; the changes sum to zero.
     */
    public static double[] balance(double rate, double[] stored, double[] capacity) {
        int n = Math.min(stored.length, capacity.length);
        double[] delta = new double[n];
        double totalStored = 0;
        double totalCapacity = 0;
        for (int i = 0; i < n; i++) {
            totalStored += Math.max(0, stored[i]);
            totalCapacity += Math.max(0, capacity[i]);
        }
        if (totalCapacity <= 0) {
            return delta;
        }
        double mean = totalStored / totalCapacity;
        double[] surplus = new double[n];
        double give = 0;
        double take = 0;
        for (int i = 0; i < n; i++) {
            surplus[i] = Math.max(0, stored[i]) - mean * Math.max(0, capacity[i]);
            if (surplus[i] > 0) {
                give += surplus[i];
            } else {
                take -= surplus[i];
            }
        }
        double total = move(rate, give, take);
        if (total <= 0) {
            return delta;
        }
        for (int i = 0; i < n; i++) {
            delta[i] = surplus[i] > 0 ? -surplus[i] * total / give : -surplus[i] * total / take;
        }
        return delta;
    }

    private static double sumPositive(double[] values) {
        double sum = 0;
        for (double v : values) {
            sum += Math.max(0, v);
        }
        return sum;
    }
}
