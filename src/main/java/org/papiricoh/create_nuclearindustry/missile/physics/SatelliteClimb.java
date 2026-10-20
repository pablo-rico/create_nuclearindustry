package org.papiricoh.create_nuclearindustry.missile.physics;

/** A launch vehicle's climb to an insertion point. Pure: heights and flight parameters come from the caller. */
public final class SatelliteClimb {
    private SatelliteClimb() {
    }

    /** First clear the launcher vertically, then lean downrange; the timed coast ends at the insertion point. */
    public static MissileFlight flight(MissileDesign design, V3 start, double heading, double insertionY,
                                       double downrange, BallisticModel.Params params) {
        V3 aim = new V3(start.x() + Math.sin(heading) * downrange, insertionY, start.z() + Math.cos(heading) * downrange);
        // Allow all stages to burn, followed by a coast on the scale of the climb's free-fall time.
        int arrival = (int) Math.ceil(design.totalBurnTicks()
                + Math.sqrt(2 * Math.max(1, insertionY - start.y()) / Math.max(1e-6, params.gravity())));
        return MissileFlight.timed(design, start, aim, V3.ZERO, Math.max(1, arrival), params.riseHeight());
    }
}
