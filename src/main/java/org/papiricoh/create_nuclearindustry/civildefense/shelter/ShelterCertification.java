package org.papiricoh.create_nuclearindustry.civildefense.shelter;

import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;

/**
 * Whether a surveyed room is a shelter: closed off, not too big, and with no wall block weaker
 * than the thresholds against blasts (explosion resistance) or radiation (transmission). The
 * first failing reason wins, in the order a player would fix them. No game classes.
 */
public final class ShelterCertification {

    public enum Status {
        /** The survey is still going. */
        SCANNING,
        /** No air next to the unit to start from. */
        NO_ROOM,
        /** The air reaches somewhere the survey cannot follow: outside, or an unloaded chunk. */
        OPEN,
        /** More air than the cap: an open space, or a room too big to keep clean. */
        TOO_BIG,
        /** A wall block gives way too easily to blasts. */
        WEAK_WALL,
        /** A wall block lets too much radiation through. */
        LEAKY_WALL,
        CERTIFIED;

        public boolean certified() {
            return this == CERTIFIED;
        }
    }

    /** Slack for transmissions read back from a float data map. */
    private static final double EPSILON = 1.0e-6;

    private ShelterCertification() {}

    /**
     * @param minResistance   lowest explosion resistance among the wall blocks
     * @param maxTransmission highest radiation transmission among the wall blocks
     */
    public static Status evaluate(SealedVolume.State state, int volume, int maxVolume, double minResistance,
                                  double maxTransmission, double requiredResistance, double allowedTransmission) {
        return switch (state) {
            case FILLING -> Status.SCANNING;
            case LEAKED -> volume > maxVolume ? Status.TOO_BIG : Status.OPEN;
            case SEALED -> {
                if (minResistance + EPSILON < requiredResistance) {
                    yield Status.WEAK_WALL;
                }
                if (maxTransmission > allowedTransmission + EPSILON) {
                    yield Status.LEAKY_WALL;
                }
                yield Status.CERTIFIED;
            }
        };
    }
}
