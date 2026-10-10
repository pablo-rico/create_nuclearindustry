package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

/**
 * What a block of corium (molten core) does each time it ticks. Its heat is a budget of
 * {@link #MAX_ENERGY} + 1 actions kept in the block state as {@code HEAT * 4 + STEP}: falling through
 * an open block costs one, melting into the block below two, quenching in water eight, and
 * resting where it cannot go further cools it. When the budget runs out the corium freezes into
 * slag where it stands. So a block of corium moves at most {@link #MAX_ENERGY} + 1 blocks down and
 * always solidifies within that many ticks, whatever lies below it. Kept free of game classes so it
 * can be unit tested.
 */
public final class CoriumModel {
    public static final int MAX_HEAT = 7;
    public static final int MAX_STEP = 3;
    /** The budget of fresh corium: HEAT 7, STEP 3. */
    public static final int MAX_ENERGY = MAX_HEAT * (MAX_STEP + 1) + MAX_STEP;
    public static final int FALL_COST = 1;
    public static final int MELT_COST = 2;
    public static final int SPREAD_COST = 2;
    public static final int REST_COST = 2;
    /** Resting on more corium: it is not losing heat to cold ground. */
    public static final int POOL_COST = 1;
    /** Spreading on a core catcher, or stuck on it, cools fast. */
    public static final int CATCHER_COST = 3;
    public static final int QUENCH_COST = 8;

    /** What lies under the corium. */
    public enum Below {
        /** Air, a fluid, anything it falls through. */
        OPEN,
        /** A block it can melt. */
        MELTABLE,
        /** Bedrock, very blast resistant blocks and the corium_resistant tag. */
        RESISTANT,
        /** A core catcher: the corium spreads over it and freezes. */
        CATCHER,
        /** More corium, molten or frozen. */
        CORIUM
    }

    public enum Action {
        /** Moves down one block. */
        FALL,
        /** Melts the block below and takes its place. */
        MELT,
        /** Moves sideways onto free space beside it. */
        SPREAD,
        /** Stays where it is and cools. */
        REST,
        /** Touches water: a steam explosion, and the water boils away. */
        QUENCH
    }

    /**
     * @param action     what the corium does this tick
     * @param energy     budget left after it; below 0 the corium freezes where the action leaves it
     * @param solidifies whether it freezes at the end of this tick
     */
    public record Decision(Action action, int energy, boolean solidifies) {}

    private CoriumModel() {}

    public static int energy(int heat, int step) {
        return clamp(heat, 0, MAX_HEAT) * (MAX_STEP + 1) + clamp(step, 0, MAX_STEP);
    }

    public static int heat(int energy) {
        return clamp(energy, 0, MAX_ENERGY) / (MAX_STEP + 1);
    }

    public static int step(int energy) {
        return clamp(energy, 0, MAX_ENERGY) % (MAX_STEP + 1);
    }

    /**
     * Decides one tick.
     *
     * @param energy           budget left, 0 to {@link #MAX_ENERGY}
     * @param below            what is under it
     * @param waterAdjacent    water touches it
     * @param canSpread        a free, supported space is beside it
     * @param blockDestruction whether it may melt blocks
     */
    public static Decision decide(int energy, Below below, boolean waterAdjacent, boolean canSpread, boolean blockDestruction) {
        Action action;
        int cost;
        if (waterAdjacent) {
            action = Action.QUENCH;
            cost = QUENCH_COST;
        } else {
            switch (below) {
                case OPEN -> {
                    action = Action.FALL;
                    cost = FALL_COST;
                }
                case MELTABLE -> {
                    action = blockDestruction ? Action.MELT : Action.REST;
                    cost = blockDestruction ? MELT_COST : REST_COST;
                }
                case CATCHER -> {
                    action = canSpread ? Action.SPREAD : Action.REST;
                    cost = canSpread ? SPREAD_COST : CATCHER_COST;
                }
                case CORIUM -> {
                    action = Action.REST;
                    cost = POOL_COST;
                }
                default -> {
                    action = Action.REST;
                    cost = REST_COST;
                }
            }
        }
        int left = clamp(energy, 0, MAX_ENERGY) - cost;
        return new Decision(action, left, left < 0);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
