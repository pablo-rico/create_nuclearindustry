package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * Boron dissolved in a reactor's water, kept as a mass (ppm times mB) rather than a concentration,
 * so the concentration follows the water: boiling leaves the boron behind and concentrates it,
 * plain water fed in dilutes it, and only water bled out of the vessel takes boron with it. Past
 * {@code maxPpm} the extra boron precipitates and redissolves once there is water for it again; it
 * never goes away by itself. Kept free of game classes so it can be unit tested.
 */
public final class SolubleBoron {
    /** ppm times mB of boron in the vessel. */
    private double mass;

    /** Adds {@code mb} of water carrying {@code ppm} of boron. */
    public void add(double mb, double ppm) {
        if (mb > 0 && ppm > 0 && Double.isFinite(mb * ppm)) {
            mass += mb * ppm;
        }
    }

    /**
     * Concentration in {@code inventory} mB of water, ppm, at most {@code maxPpm} (the rest lies
     * precipitated). No water, no dissolved boron.
     */
    public double ppm(double inventory, double maxPpm) {
        if (inventory <= 0 || mass <= 0) {
            return 0;
        }
        return Math.min(Math.max(0, maxPpm), mass / inventory);
    }

    /**
     * {@code drained} mB of the vessel's {@code inventory} left it: the boron dissolved in them goes
     * too. Returns the ppm times mB removed.
     */
    public double removeWith(double drained, double inventory, double maxPpm) {
        if (drained <= 0 || inventory <= 0 || mass <= 0) {
            return 0;
        }
        double removed = Math.min(mass, Math.min(drained, inventory) * ppm(inventory, maxPpm));
        mass -= removed;
        if (mass < 1.0e-6) {
            mass = 0;
        }
        return removed;
    }

    /** All of it gone (a meltdown). */
    public void clear() {
        mass = 0;
    }

    /** ppm times mB of boron in the vessel, dissolved or not (saved). */
    public double mass() {
        return mass;
    }

    public void restore(double mass) {
        this.mass = Double.isFinite(mass) ? Math.max(0, mass) : 0;
    }
}
