package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * Damage to the fuel, 0 (intact) to 1 (a molten core). Cladding starts to fail above
 * {@code claddingFailure} and the damage grows faster the hotter it gets; above {@code fuelMelting}
 * the fuel itself melts and slumps, much faster. Damage never heals: only fresh fuel replaces it.
 * Once it passes {@code meltdown} the core has melted down. Pure arithmetic, rates per second.
 *
 * @see org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator
 */
public final class CoreDamageModel {

    /**
     * @param claddingFailure    fuel temperature where cladding damage starts, Celsius
     * @param fuelMelting        fuel temperature where the fuel melts, Celsius
     * @param claddingRate       damage per second at 800 K above cladding failure (grows with the square, up to 4x)
     * @param meltingRate        damage per second as the fuel starts to melt
     * @param meltingRateMax     damage per second 1200 K above melting
     * @param meltdown           damage at which the core has melted down
     */
    public record Params(double claddingFailure, double fuelMelting, double claddingRate, double meltingRate,
                         double meltingRateMax, double meltdown) {
        public static final Params DEFAULT = new Params(2000, 2800, 0.004, 0.01, 0.1, 0.75);
    }

    private double damage;

    public double damage() {
        return damage;
    }

    /** Damage per second at this fuel temperature. */
    public static double rate(Params p, double fuelTemperature) {
        double rate = 0;
        if (fuelTemperature > p.claddingFailure()) {
            double x = (fuelTemperature - p.claddingFailure()) / 800.0;
            rate += p.claddingRate() * Math.min(4, x * x);
        }
        if (fuelTemperature > p.fuelMelting()) {
            double x = Math.min(1, (fuelTemperature - p.fuelMelting()) / 1200.0);
            rate += p.meltingRate() + (p.meltingRateMax() - p.meltingRate()) * x;
        }
        return rate;
    }

    /** Advances by {@code seconds}; returns the damage added. */
    public double step(Params p, double fuelTemperature, double seconds) {
        double added = Math.min(1 - damage, rate(p, fuelTemperature) * Math.max(0, seconds));
        damage += added;
        return added;
    }

    public boolean meltedDown(Params p) {
        return damage >= p.meltdown();
    }

    public boolean fuelMelting(Params p, double fuelTemperature) {
        return fuelTemperature >= p.fuelMelting();
    }

    /** New fuel replaces this share of the core; its damage goes with the old fuel. */
    public void refuel(double freshShare) {
        damage *= 1 - Math.max(0, Math.min(1, freshShare));
    }

    public void restore(double damage) {
        this.damage = Double.isFinite(damage) ? Math.max(0, Math.min(1, damage)) : 0;
    }
}
