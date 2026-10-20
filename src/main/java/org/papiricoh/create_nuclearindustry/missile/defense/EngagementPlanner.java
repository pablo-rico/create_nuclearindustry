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
        java.util.Map<Integer, Integer> used = new java.util.HashMap<>();
        java.util.ArrayList<Assignment> result = new java.util.ArrayList<>();
        java.util.Set<UUID> served = new java.util.HashSet<>();
        for (Threat threat : threats.stream().sorted(java.util.Comparator.comparingDouble(Threat::remaining)).toList()) {
            if (!served.add(threat.id()) || threat.inFlight() + threat.reserved() != 0) continue;
            for (int slot = 0; slot < doctrine.volley(); slot++) {
                Shooter best = null;
                int earliest = Integer.MAX_VALUE;
                for (Shooter shooter : shooters) {
                    if (used.getOrDefault(shooter.index(), 0) >= shooter.ready()) continue;
                    int arrival = solutions.earliest(threat, shooter);
                    if (arrival >= 0 && arrival < earliest) {
                        best = shooter;
                        earliest = arrival;
                    }
                }
                if (best == null) break;
                result.add(new Assignment(threat.id(), best.index(), earliest));
                used.merge(best.index(), 1, Integer::sum);
            }
        }
        return List.copyOf(result);
    }
}
