package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * Where something an interceptor can be sent at will be, known ahead of time: a burnt-out missile's free fall ({@link TargetArc})
 * or a satellite's orbit. All {@link InterceptSolver} needs.
 */
public interface TargetPath {
    /** Where it is {@code ahead} ticks from now. */
    V3 at(double ahead);

    /** How fast and which way it goes {@code ahead} ticks from now, blocks per tick. */
    V3 velocityAt(double ahead);

    /** Ticks from now it stays where it can be met: until a missile comes down; a satellite's look-ahead horizon. */
    double remaining();

    /** Whether it can still be met {@code ahead} ticks from now. */
    default boolean alive(double ahead) {
        return ahead < remaining();
    }
}
