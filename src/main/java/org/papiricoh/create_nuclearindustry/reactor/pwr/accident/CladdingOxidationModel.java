package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

/**
 * The zirconium cladding of the fuel rods reacting with steam once the core is hot enough:
 * Zr + 2 H₂O → ZrO₂ + 2 H₂. The reaction gives off heat, which speeds up the heat-up of an
 * uncovered core, and hydrogen, which ends up in the containment. It runs faster the hotter the
 * core, needs steam, and stops once the cladding is used up: each rod holds a fixed amount of
 * zirconium, and fresh fuel brings fresh cladding. Kept free of game classes so it can be unit tested.
 */
public final class CladdingOxidationModel {
    /** Degrees above the threshold at which the rate reaches {@link Params#ratePerRod()}. */
    public static final double REFERENCE_SPAN = 300.0;
    /** The rate stops growing this many reference spans above the threshold. */
    private static final double MAX_SPEEDUP = 9.0;

    /**
     * @param threshold   core temperature where oxidation starts, °C
     * @param ratePerRod  zirconium oxidised per rod per tick {@link #REFERENCE_SPAN} above the threshold, mol
     * @param zrPerRod    zirconium in the cladding of one rod, mol
     * @param heatPerMol  core heating per mol oxidised per rod, °C
     */
    public record Params(double threshold, double ratePerRod, double zrPerRod, double heatPerMol) {}

    /**
     * One tick's result.
     *
     * @param hydrogen mol of hydrogen made
     * @param heat     °C the core gains
     */
    public record Step(double hydrogen, double heat) {
        public static final Step NONE = new Step(0.0, 0.0);
    }

    private double oxidized;

    /**
     * Advances one tick.
     *
     * @param coreTemperature core temperature, °C
     * @param rods            fuel rods in the core
     * @param steam           how much steam reaches the cladding, 0 (dry) to 1
     */
    public Step step(double coreTemperature, int rods, double steam, Params p) {
        if (rods <= 0 || coreTemperature <= p.threshold() || steam <= 0) {
            return Step.NONE;
        }
        double available = rods * p.zrPerRod() - oxidized;
        if (available <= 0) {
            return Step.NONE;
        }
        double over = (coreTemperature - p.threshold()) / REFERENCE_SPAN;
        double speed = Math.min(MAX_SPEEDUP, over * over);
        double reacted = Math.min(available, rods * p.ratePerRod() * speed * Math.min(1.0, steam));
        oxidized += reacted;
        return new Step(2.0 * reacted, p.heatPerMol() * reacted / rods);
    }

    /** Fresh fuel assemblies loaded: their cladding replaces oxidised cladding. */
    public void refuel(int assemblies, double zrPerRod) {
        oxidized = Math.max(0.0, oxidized - Math.max(0, assemblies) * zrPerRod);
    }

    /** Share of the cladding already oxidised, 0-1: how damaged the fuel is. */
    public double damage(int rods, double zrPerRod) {
        double total = rods * zrPerRod;
        return total <= 0 ? 0.0 : Math.min(1.0, oxidized / total);
    }

    /** Zirconium oxidised so far, mol. */
    public double oxidized() {
        return oxidized;
    }

    public void restore(double oxidized) {
        this.oxidized = Double.isFinite(oxidized) ? Math.max(0.0, oxidized) : 0.0;
    }
}
