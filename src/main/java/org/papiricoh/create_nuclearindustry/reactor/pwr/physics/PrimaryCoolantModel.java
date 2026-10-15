package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

/**
 * The water of a pressurized reactor's vessel. The core's heat goes into the water; circulation
 * carries it out by turning cold coolant into hot coolant, a fixed amount of heat per mB, as fast as
 * the pumps (plus a little natural circulation) move water through the core and while there is cold
 * coolant left and room for hot. Heat the flow cannot carry stays in the vessel: it warms the water
 * of the core up to the boiling point at the loop's pressure, then boils it into steam voids.
 * Kept free of game classes so it can be unit tested.
 */
public final class PrimaryCoolantModel {
    /** Share of the water covering the core that sits in the core itself and heats when the flow stops. */
    public static final double CORE_WATER_FRACTION = 0.25;
    private static final double RISE_SMOOTHING = 0.2;

    /**
     * @param coldLegTemperature water returned by the steam generators, °C
     * @param enthalpyPerMb      heat one mB carries from the cold leg to the hot leg, heat units
     * @param nominalRise        temperature rise that heat makes, °C
     * @param latentPerMb        heat that boils one mB, heat units
     * @param vaporExpansion     volume of steam one mB of water makes at loop pressure, mB
     * @param naturalCirculation flow through the core with every pump stopped, mB/t
     * @param heatLoss           share of the stored heat the vessel loses to its surroundings each tick
     */
    public record Params(double coldLegTemperature, double enthalpyPerMb, double nominalRise, double latentPerMb,
                         double vaporExpansion, double naturalCirculation, double heatLoss) {
        /** Heat that warms one mB by one degree. */
        public double heatCapacityPerMb() {
            return enthalpyPerMb / Math.max(1.0e-9, nominalRise);
        }
    }

    /**
     * One tick of the vessel.
     *
     * @param heat       heat units the core gives the water this tick
     * @param cold       cold coolant in the vessel, mB
     * @param hotSpace   room left for hot coolant, mB
     * @param pumpFlow   what the pumps move through the loop, mB/t
     * @param inventory  all the coolant in the vessel, mB
     * @param coreVolume coolant that just covers the core, mB
     * @param saturation boiling point at the loop's pressure, °C
     */
    public record Conditions(double heat, int cold, int hotSpace, double pumpFlow, double inventory, double coreVolume,
                             double saturation) {}

    private double storedHeat;
    private double flowRise;
    private double coreFlow;
    private double coldLeg;
    private double hotLeg;
    private double vaporVolume;
    private double voidFraction;
    private double coverage = 1.0;
    private double saturation;
    private double inventory;
    private double coreVolume;
    /** Temperature cold makeup water lowered the vessel to, °C; infinity while it is at the cold leg temperature. */
    private double baseline = Double.POSITIVE_INFINITY;

    /**
     * Advances one tick and returns how many mB of cold coolant left the core hot; the caller moves
     * that much from the cold pool to the hot pool.
     */
    public int step(Conditions c, Params p) {
        double perMb = Math.max(1.0e-9, p.enthalpyPerMb());
        storedHeat += reheatBaseline(Math.max(0.0, c.heat()), c.inventory(), p);
        double circulation = Math.max(0.0, c.pumpFlow()) + Math.max(0.0, p.naturalCirculation());
        int available = Math.max(0, Math.min(c.cold(), c.hotSpace()));
        int moved = (int) Math.min(available, Math.floor(Math.min(circulation, storedHeat / perMb)));
        moved = Math.max(0, moved);
        storedHeat -= moved * perMb;
        storedHeat = Math.max(0.0, storedHeat * (1.0 - Math.max(0.0, p.heatLoss())));

        coreFlow = Math.max(Math.max(0.0, c.pumpFlow()), moved);
        double rise = coreFlow > 1.0e-6 ? p.nominalRise() * moved / coreFlow : 0.0;
        flowRise += (rise - flowRise) * RISE_SMOOTHING;
        update(c.inventory(), c.coreVolume(), c.saturation(), p);
        return moved;
    }

    /** Heat brought back into the vessel, such as hot coolant pumped into the cold leg. */
    public void addHeat(double heat) {
        storedHeat += Math.max(0.0, heat);
    }

    /**
     * Cold makeup water: {@code mb} at {@code temperature} °C mixed into the vessel's
     * {@code inventory} mB lowers the water's baseline, the temperature the cold leg returns to
     * without heat. The core's heat warms the baseline back to the loop's cold leg temperature before
     * it stores any (see {@link #step}), so a vessel fed enough cold water cools below boiling.
     */
    public void mixCold(double mb, double temperature, double inventory, Params p) {
        if (mb <= 0) {
            return;
        }
        double base = baseline(p);
        double mass = Math.max(1.0, inventory);
        double mixed = (base * mass + Math.min(temperature, base) * mb) / (mass + mb);
        // Stored heat warms the cold water first: the vessel's sensible heat and the makeup share it.
        double cp = p.heatCapacityPerMb();
        double needed = (base - mixed) * mass * cp;
        double fromStore = Math.min(storedHeat, needed);
        storedHeat -= fromStore;
        baseline = mixed + fromStore / (mass * cp);
    }

    /** What the core's heat does first: warm a lowered baseline back up. Returns the heat left over. */
    private double reheatBaseline(double heat, double inventory, Params p) {
        double base = baseline(p);
        if (base >= p.coldLegTemperature() || heat <= 0) {
            return heat;
        }
        double capacity = Math.max(1.0, inventory) * p.heatCapacityPerMb();
        double needed = (p.coldLegTemperature() - base) * capacity;
        if (heat >= needed) {
            baseline = Double.POSITIVE_INFINITY;
            return heat - needed;
        }
        baseline = base + heat / capacity;
        return 0.0;
    }

    /** Temperature the vessel water returns to with no heat stored, °C: the cold leg's, unless cold makeup lowered it. */
    public double baseline(Params p) {
        return Math.min(baseline, p.coldLegTemperature());
    }

    /** The lowered baseline, or infinity when it sits at the cold leg temperature (saved). */
    public double savedBaseline() {
        return baseline;
    }

    public void restoreBaseline(double baseline) {
        this.baseline = Double.isFinite(baseline) ? Math.max(0.0, baseline) : Double.POSITIVE_INFINITY;
    }

    /**
     * A relief valve lets steam out: up to {@code maxHeat} of the boiling heat leaves with it.
     * Returns the mB of coolant that left as steam.
     */
    public double ventSteam(double maxHeat, Params p) {
        double vaporHeat = vaporHeat(p);
        double removed = Math.min(vaporHeat, Math.max(0.0, maxHeat));
        storedHeat -= removed;
        return removed / Math.max(1.0e-9, p.latentPerMb());
    }

    /** Recomputes the readings for the current state, as {@link #step} does at its end. */
    public void update(double inventory, double coreVolume, double saturation, Params p) {
        double cp = p.heatCapacityPerMb();
        double mass = coreWater(inventory, coreVolume);
        double base = baseline(p);
        double sensibleCap = mass * cp * Math.max(0.0, saturation - base);
        double sensible = Math.min(storedHeat, sensibleCap);
        double vaporHeat = storedHeat - sensible;
        coldLeg = Math.min(saturation, base + sensible / (mass * cp));
        hotLeg = Math.max(coldLeg, Math.min(saturation, coldLeg + flowRise));
        vaporVolume = vaporHeat / Math.max(1.0e-9, p.latentPerMb()) * Math.max(0.0, p.vaporExpansion());
        voidFraction = vaporVolume / (mass + vaporVolume);
        coverage = coreVolume <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, inventory / coreVolume));
        this.saturation = saturation;
        this.inventory = inventory;
        this.coreVolume = coreVolume;
    }

    private double vaporHeat(Params p) {
        double cp = p.heatCapacityPerMb();
        double mass = coreWater(inventory, coreVolume);
        return Math.max(0.0, storedHeat - mass * cp * Math.max(0.0, saturation - baseline(p)));
    }

    private static double coreWater(double inventory, double coreVolume) {
        return Math.max(1.0, Math.min(Math.max(0.0, inventory), coreVolume) * CORE_WATER_FRACTION);
    }

    /** Heat units the vessel water holds above the cold-leg condition: warmer water first, then steam. */
    public double storedHeat() {
        return storedHeat;
    }

    /** Temperature rise across the core from the circulation alone, °C, smoothed. */
    public double flowRise() {
        return flowRise;
    }

    /** Water moved through the core last tick, mB/t. */
    public double coreFlow() {
        return coreFlow;
    }

    public double coldLeg() {
        return coldLeg;
    }

    public double hotLeg() {
        return hotLeg;
    }

    public double averageTemperature() {
        return (coldLeg + hotLeg) / 2.0;
    }

    /** Volume of the steam in the core, mB; it pushes the same volume of water into the pressurizer. */
    public double vaporVolume() {
        return vaporVolume;
    }

    /** Share of the core's volume taken by steam, 0-1. */
    public double voidFraction() {
        return voidFraction;
    }

    /** How much of the core the water covers, 0-1. */
    public double coverage() {
        return coverage;
    }

    /** Restores a saved state; call {@link #update} afterwards for the readings. */
    public void restore(double storedHeat, double flowRise) {
        this.storedHeat = Math.max(0.0, storedHeat);
        this.flowRise = Math.max(0.0, flowRise);
    }
}
