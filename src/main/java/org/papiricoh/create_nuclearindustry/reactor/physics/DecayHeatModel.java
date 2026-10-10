package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * Heat from the decay of fission products. It follows the fission heat with a lag: a core that ran
 * hot keeps heating for minutes after its chain reaction stops, so it still needs coolant after a
 * SCRAM. Kept free of game classes so it can be unit tested.
 */
public final class DecayHeatModel {
    private double heat;

    /**
     * Advances one tick.
     *
     * @param fissionHeat heat the chain reaction makes this tick, in °C per tick
     * @param fraction    share of the fission heat the decay heat settles at
     * @param lifetime    ticks for the decay heat to fall to about a third once fission stops
     */
    public void tick(double fissionHeat, double fraction, double lifetime) {
        double target = Math.max(0.0, fissionHeat) * Math.max(0.0, fraction);
        heat += (target - heat) / Math.max(1.0, lifetime);
        heat = Math.max(0.0, heat);
    }

    /** Decay heat, in °C per tick. */
    public double heat() {
        return heat;
    }

    public void set(double heat) {
        this.heat = Math.max(0.0, heat);
    }
}
