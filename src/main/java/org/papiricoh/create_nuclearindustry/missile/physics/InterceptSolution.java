package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * How an interceptor meets a target: launched {@code delayTicks} from now, it flies {@code arrivalTicks} and is at {@code point}
 * on the same tick the target is.
 *
 * @param arrivalTicks   ticks of its own flight, launch to meeting
 * @param delayTicks     ticks from now until launch (turning, counting down)
 * @param point          where they meet
 * @param ejectVelocity  its velocity leaving the launcher
 * @param arrivalVelocity its velocity at the meeting
 * @param targetVelocity the target's velocity at the meeting
 * @param groundRange    blocks over the ground from launcher to meeting
 * @param terminal       whether the target is in its terminal phase by then
 */
public record InterceptSolution(int arrivalTicks, int delayTicks, V3 point, V3 ejectVelocity, V3 arrivalVelocity, V3 targetVelocity,
                                double groundRange, boolean terminal) {
}
