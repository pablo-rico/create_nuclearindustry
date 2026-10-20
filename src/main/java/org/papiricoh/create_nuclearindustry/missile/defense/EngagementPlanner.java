package org.papiricoh.create_nuclearindustry.missile.defense;

import java.util.List;
import java.util.UUID;

/**
 * Who fires at what: the missiles with least time left are served first, each gets as many interceptors as the doctrine asks
 * for if none is on its way yet, and each one goes to the loaded launcher that meets it soonest, never the same round twice. Pure:
 * how soon a launcher can meet a missile is given to it.
 */
public final class EngagementPlanner {
    private EngagementPlanner() {
    }

    /**
     * A missile to engage.
     *
     * @param remaining ticks until it comes down
     * @param inFlight  interceptors already flying at it
     * @param reserved  interceptors ordered at it, not yet launched
     */
    public record Threat(UUID id, double remaining, int inFlight, int reserved) {
    }

    /** A launcher, by its index in the caller's list, and the rounds it could fire now. */
    public record Shooter(int index, int ready) {
    }

    /** One interceptor: launcher {@code shooter} at missile {@code threat}, meeting it {@code arrivalTicks} from now. */
    public record Assignment(UUID threat, int shooter, int arrivalTicks) {
    }

    /** How soon a launcher can meet a missile. */
    @FunctionalInterface
    public interface Solutions {
        /** Ticks from now until {@code shooter} meets {@code threat}, or -1 if it cannot. */
        int earliest(Threat threat, Shooter shooter);
    }

    public static List<Assignment> plan(List<Threat> threats, List<Shooter> shooters, Doctrine doctrine, Solutions solutions) {
        return List.of();
    }
}
