package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * What one interceptor can do, for the {@link InterceptSolver}: its rocket, how it leaves its launcher and where it may meet a
 * target.
 *
 * @param design         its rocket
 * @param rise           blocks it climbs straight up before it turns (0 for a canister that already points it)
 * @param ejectSpeed     blocks per tick it leaves its tube at, along the launch direction, before its motor lights
 * @param maxRange       furthest the meeting point may lie from the launcher over the ground, blocks
 * @param minY           lowest the meeting point may be, absolute height
 * @param maxY           highest the meeting point may be, absolute height
 * @param exoatmospheric whether it meets targets above the air ({@code > atmosphereTop}) rather than within it
 * @param basePk         its chance to kill in the best case
 * @param minPitch       degrees above the horizon it can be launched at, at least
 * @param maxFlightTicks longest it may fly before it must have met its target
 */
public record InterceptorEnvelope(MissileDesign design, double rise, double ejectSpeed, double maxRange, double minY, double maxY,
                                  boolean exoatmospheric, double basePk, double minPitch, int maxFlightTicks) {
}
