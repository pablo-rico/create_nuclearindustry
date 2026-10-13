package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

/**
 * Whether a pressurized reactor vessel still holds. Held above its failure pressure the steel
 * creeps; after long enough it gives way. Time spent below the failure pressure lets the count
 * fall back slowly, so a short spike does no harm. Once breached it stays breached until repaired.
 * Kept free of game classes so it can be unit tested.
 */
public final class VesselIntegrity {
    private int overpressureTicks;
    private boolean breached;

    /** Advances one tick at {@code pressure} bar. Returns true on the tick the vessel fails. */
    public boolean tick(double pressure, double failurePressure, int failureTicks) {
        if (breached) {
            return false;
        }
        if (pressure > failurePressure) {
            overpressureTicks++;
            if (overpressureTicks >= Math.max(1, failureTicks)) {
                breached = true;
                return true;
            }
        } else if (overpressureTicks > 0) {
            overpressureTicks--;
        }
        return false;
    }

    public boolean breached() {
        return breached;
    }

    public int overpressureTicks() {
        return overpressureTicks;
    }

    /** The vessel was rebuilt. */
    public void repair() {
        breached = false;
        overpressureTicks = 0;
    }

    public void restore(boolean breached, int overpressureTicks) {
        this.breached = breached;
        this.overpressureTicks = Math.max(0, overpressureTicks);
    }
}
