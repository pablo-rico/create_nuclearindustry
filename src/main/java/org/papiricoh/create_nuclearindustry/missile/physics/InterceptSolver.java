package org.papiricoh.create_nuclearindustry.missile.physics;

import java.util.Optional;

/**
 * Finds the earliest meeting an interceptor can make with a burnt-out missile or a satellite: a point on the target's path it
 * reaches on the same tick, within its envelope. Pure: no game classes, no config.
 */
public final class InterceptSolver {
    private InterceptSolver() {
    }

    /**
     * The earliest way an interceptor launched from {@code launch} {@code delayTicks} from now meets {@code arc}, or empty if it
     * cannot.
     */
    public static Optional<InterceptSolution> solve(TargetPath arc, V3 launch, InterceptorEnvelope envelope, BallisticModel.Params params,
                                                    int delayTicks) {
        return solve(arc, launch, envelope, params, delayTicks, 4);
    }

    /** Configurable sweep spacing; all guidance verification remains independent of server config. */
    public static Optional<InterceptSolution> solve(TargetPath arc, V3 launch, InterceptorEnvelope envelope,
                                                    BallisticModel.Params params, int delayTicks, int stepTicks) {
        int delay = Math.max(0, delayTicks);
        int limit = (int) Math.min(Math.floor(arc.remaining() - 1 - delay), envelope.maxFlightTicks());
        int step = Math.max(1, stepTicks);
        for (int n = Math.min(step, limit); n > 0 && n <= limit;) {
            InterceptSolution found = candidate(arc, launch, envelope, params, delay, n);
            if (found != null) {
                // Feasibility need not be monotonic across altitude bands or stage transitions.
                for (int earlier = 1; earlier < n; earlier++) {
                    InterceptSolution refined = candidate(arc, launch, envelope, params, delay, earlier);
                    if (refined != null) return Optional.of(refined);
                }
                return Optional.of(found);
            }
            if (n == limit) break;
            n = Math.min(limit, n + step);
        }
        // A narrow valid window can lie entirely between sweep samples.
        for (int n = 1; n <= limit; n++) {
            if (n % step == 0 || n == limit) continue;
            InterceptSolution found = candidate(arc, launch, envelope, params, delay, n);
            if (found != null) return Optional.of(found);
        }
        return Optional.empty();
    }

    private static InterceptSolution candidate(TargetPath arc, V3 launch, InterceptorEnvelope env,
                                                BallisticModel.Params params, int delay, int n) {
        V3 point = arc.at(n + delay);
        V3 offset = point.sub(launch);
        double range = offset.horizontalLength();
        if (point.y() < env.minY() || point.y() > env.maxY() || range > env.maxRange()
                || env.exoatmospheric() != (point.y() > params.atmosphereTop())
                || offset.length() > (env.ejectSpeed() + env.design().deltaV()) * n) return null;
        double pitch = Math.max(Math.toRadians(env.minPitch()), Math.min(Math.toRadians(85), Math.atan2(offset.y(), range)));
        V3 horizontal = new V3(offset.x(), 0, offset.z()).normalize();
        if (range < 1e-12) horizontal = new V3(1, 0, 0);
        V3 eject = horizontal.scale(Math.cos(pitch)).add(V3.UP.scale(Math.sin(pitch))).scale(env.ejectSpeed());
        MissileFlight flight = MissileFlight.timed(env.design(), launch, point, eject, n, env.rise());
        MissileFlight.Prediction prediction = flight.predict(params);
        if (!prediction.onTarget() || prediction.impact().distance(point) > 1) return null;
        while (!flight.impacted()) flight.step(params);
        return new InterceptSolution(n, delay, point, eject, flight.velocity(), arc.velocityAt(n + delay), range,
                arc.remaining() - (n + delay) <= params.terminalTicks());
    }
}
