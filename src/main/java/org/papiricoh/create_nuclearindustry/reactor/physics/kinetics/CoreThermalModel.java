package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * The heat of a fission core: a fuel node and a coolant node, both per fuel rod, so a big core and
 * a small one behave alike and only the rated power scales with size.
 *
 * <ul>
 *   <li>The fuel takes the fission, decay and oxidation heat and passes it to the coolant through
 *       its cladding: {@code Q = UA (T_fuel - T_coolant)}, with UA set so a rod at rated power runs
 *       {@code fuelRiseAtRated} above its coolant. An uncovered or damaged core passes less. Above
 *       the critical heat flux the cladding is blanketed in steam and only a tenth of the excess gets
 *       through.</li>
 *   <li>Boiling water holds at its saturation temperature while there is water: the heat above it
 *       boils water into steam, up to what the heat exchangers, steam ports and steam tank can take.
 *       Heat that cannot leave warms the water above saturation, gradually, through its heat
 *       capacity.</li>
 *   <li>A pressurized loop holds the coolant at the loop's temperature and takes the heat it can.</li>
 *   <li>A dry core keeps only its structure's heat capacity and radiates.</li>
 * </ul>
 * Pure arithmetic: powers in MW, heat capacities in MJ per kelvin, temperatures in Celsius.
 */
public final class CoreThermalModel {
    public static final double AMBIENT = 20.0;

    /**
     * @param ratedPowerPerRod      thermal power of one rod at full rated power, MW
     * @param fuelRiseAtRated       fuel temperature above the coolant at rated power, K
     * @param fuelHeatCapacity      per rod, MJ/K
     * @param coolantHeatCapacity   water round one rod when the core is full, MJ/K
     * @param structureHeatCapacity steel and channel round one rod, MJ/K
     * @param boilingTemperature    saturation temperature of the core's water, Celsius
     * @param ratedVoid             steam void in a core boiling off its rated power, 0..1
     * @param passiveLoss           losses from the vessel per rod, MW per kelvin above ambient
     * @param radiation             radiation from hot fuel, MW per K^4 per rod
     * @param chfMargin             critical heat flux over the heat exchangers' rated removal
     * @param maxFuelTemperature    the fuel never gets hotter, Celsius
     */
    public record Params(double ratedPowerPerRod, double fuelRiseAtRated, double fuelHeatCapacity, double coolantHeatCapacity,
                         double structureHeatCapacity, double boilingTemperature, double ratedVoid, double passiveLoss,
                         double radiation, double chfMargin, double maxFuelTemperature) {
        public static final Params DEFAULT = new Params(50, 600, 0.5, 1.5, 0.2, 285, 0.30, 0.0005, 2.0e-14, 1.25, 4500);

        /** Fuel to coolant conductance of one healthy, covered rod, MW/K. */
        public double conductance() {
            return ratedPowerPerRod / Math.max(1, fuelRiseAtRated);
        }
    }

    /** What the core's coolant is doing. */
    public sealed interface Boundary permits Boiling, Pressurized, Dry {}

    /**
     * Water boiling in the core.
     *
     * @param coverage   share of the core under water, 0..1
     * @param maxRemoval steam the exchangers, ports and tank can take away, MW for the whole core
     * @param chf        critical heat flux of the whole core, MW
     */
    public record Boiling(double coverage, double maxRemoval, double chf) implements Boundary {}

    /**
     * A pressurized primary loop.
     *
     * @param coolantTemperature the loop's average temperature in the core, Celsius
     * @param voidFraction       steam in the core, 0..1
     * @param coverage           share of the core under water, 0..1
     * @param maxRemoval         heat the loop can take, MW for the whole core
     */
    public record Pressurized(double coolantTemperature, double voidFraction, double coverage, double maxRemoval) implements Boundary {}

    /** No coolant at all. */
    public record Dry() implements Boundary {}

    /**
     * @param removed      heat that left the core with its coolant, MW for the whole core
     * @param boiled       of it, what boiled water into steam, MW
     * @param voidFraction steam void in the core, 0..1
     */
    public record Result(double removed, double boiled, double voidFraction) {
        public static final Result NONE = new Result(0, 0, 0);
    }

    private double fuelTemperature = AMBIENT;
    private double coolantTemperature = AMBIENT;

    public double fuelTemperature() {
        return fuelTemperature;
    }

    public double coolantTemperature() {
        return coolantTemperature;
    }

    public void restore(double fuel, double coolant) {
        fuelTemperature = finite(fuel, AMBIENT);
        coolantTemperature = finite(coolant, Math.min(fuelTemperature, 285));
    }

    /** Heats (or cools, negative) the fuel by {@code kelvin} at once; the coolant too when the core is dry. */
    public void addToFuel(double kelvin, boolean dry) {
        fuelTemperature = Math.max(AMBIENT, fuelTemperature + kelvin);
        if (dry) {
            coolantTemperature = Math.max(AMBIENT, coolantTemperature + kelvin);
        }
    }

    public void coolBoth(double kelvin) {
        fuelTemperature = Math.max(AMBIENT, fuelTemperature - Math.max(0, kelvin));
        coolantTemperature = Math.max(AMBIENT, coolantTemperature - Math.max(0, kelvin));
    }

    /**
     * Advances by {@code seconds}.
     *
     * @param rods          fuel rods in the core
     * @param heatPerRod    fission, decay and oxidation heat of one rod, MW
     * @param damage        core damage, 0..1: damaged fuel passes its heat on less well
     */
    public Result step(Params p, Boundary boundary, int rods, double heatPerRod, double damage, double seconds) {
        int n = Math.max(1, rods);
        double dt = Math.max(0, seconds);
        double coverage = switch (boundary) {
            case Boiling b -> clamp(b.coverage());
            case Pressurized pr -> clamp(pr.coverage());
            case Dry d -> 0;
        };
        double ua = p.conductance() * (0.1 + 0.9 * coverage) * (1 - 0.6 * clamp(damage));
        double transfer = ua * (fuelTemperature - coolantTemperature);
        double chfPerRod = switch (boundary) {
            case Boiling b -> b.chf() / n;
            case Pressurized pr -> pr.maxRemoval() / n;
            case Dry d -> Double.POSITIVE_INFINITY;
        };
        if (transfer > chfPerRod) {
            // Film boiling: a steam blanket on the cladding lets little more through.
            transfer = chfPerRod + 0.1 * (transfer - chfPerRod);
        }
        double radiated = p.radiation() * Math.pow(fuelTemperature + 273.15, 4);
        fuelTemperature += dt * (heatPerRod - transfer - radiated) / p.fuelHeatCapacity();
        fuelTemperature = Math.max(AMBIENT, Math.min(p.maxFuelTemperature(), fuelTemperature));

        double passive = p.passiveLoss() * (coolantTemperature - AMBIENT);
        return switch (boundary) {
            case Pressurized pr -> {
                coolantTemperature = pr.coolantTemperature();
                double removed = Math.max(0, transfer) * n;
                yield new Result(Math.min(removed, Math.max(0, pr.maxRemoval())), 0, clamp(pr.voidFraction()));
            }
            case Boiling b -> {
                double capacity = p.coolantHeatCapacity() * coverage + p.structureHeatCapacity();
                double next = coolantTemperature + dt * (transfer - passive) / capacity;
                double boiledPerRod = 0;
                if (coverage > 0 && next > p.boilingTemperature()) {
                    double surplus = (next - p.boilingTemperature()) * capacity / Math.max(1.0e-9, dt);
                    boiledPerRod = Math.min(surplus, Math.max(0, b.maxRemoval()) / n);
                    next -= boiledPerRod * dt / capacity;
                }
                coolantTemperature = Math.max(AMBIENT, next);
                double voids = (1 - coverage) + coverage * (p.ratedVoid() * Math.min(1, boiledPerRod / Math.max(1.0e-9, p.ratedPowerPerRod()))
                        + Math.max(0, coolantTemperature - p.boilingTemperature() - 5) / 200.0);
                yield new Result(boiledPerRod * n, boiledPerRod * n, clamp(voids));
            }
            case Dry d -> {
                coolantTemperature = Math.max(AMBIENT, coolantTemperature + dt * (transfer - passive) / p.structureHeatCapacity());
                yield new Result(0, 0, 1);
            }
        };
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double finite(double v, double fallback) {
        return Double.isFinite(v) ? Math.max(AMBIENT, v) : fallback;
    }
}
