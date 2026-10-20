package org.papiricoh.create_nuclearindustry.missile.physics;

import java.util.Optional;

/**
 * Finds the earliest meeting an interceptor can make with a burnt-out missile: a point on the target's arc it reaches on the
 * same tick, within its envelope. Pure: no game classes, no config.
 */
public final class InterceptSolver {
    private InterceptSolver() {
    }

    /**
     * The earliest way an interceptor launched from {@code launch} {@code delayTicks} from now meets {@code arc}, or empty if it
     * cannot.
     */
    public static Optional<InterceptSolution> solve(TargetArc arc, V3 launch, InterceptorEnvelope envelope, BallisticModel.Params params,
                                                    int delayTicks) {
        return Optional.empty();
    }
}
