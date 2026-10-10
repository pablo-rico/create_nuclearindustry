package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

/**
 * A U-tube steam generator: hot primary coolant flows through a bundle of tubes standing in
 * feedwater, which boils into steam for the turbines. Heat crosses the tube walls at UA times the
 * gap between the primary and the boiling point of the secondary, so more tubes move more heat, and
 * steam left in the generator raises the secondary pressure and its boiling point until the gap, and
 * the heat, close. Without feedwater the tubes dry out and stop cooling the primary.
 * Every mB of primary handed back cold has given up its heat; every mB of steam took the same heat
 * a reactor boiling its water directly spends on it. Kept free of game classes so it can be unit tested.
 */
public final class SteamGeneratorModel {

    /**
     * @param uaPerTube           heat transfer of one tube bundle block, heat units per tick per °C
     * @param primaryTemperature  temperature of the hot primary coming in, °C
     * @param enthalpyPerMb       heat one mB of hot primary gives up to come out cold, heat units
     * @param steamHeatPerMb      heat that boils one mB of feedwater into steam, heat units
     * @param minPressure         secondary pressure with an empty steam space, bar
     * @param maxPressure         secondary pressure with a full steam space, bar
     * @param wetInventoryPerTube feedwater each tube bundle block needs around it to be fully wet, mB
     */
    public record Params(double uaPerTube, double primaryTemperature, double enthalpyPerMb, double steamHeatPerMb,
                         double minPressure, double maxPressure, double wetInventoryPerTube) {}

    /**
     * What one tick did.
     *
     * @param primaryCooled hot primary handed back cold, mB
     * @param steamMade     feedwater boiled into steam, mB
     * @param heat          heat moved from the primary to the secondary, heat units
     * @param pressure      secondary pressure, bar
     * @param wetFraction   share of the tubes under water, 0-1
     */
    public record Result(int primaryCooled, int steamMade, double heat, double pressure, double wetFraction) {
        public static final Result IDLE = new Result(0, 0, 0, 0, 0);
    }

    private double primaryCarry;
    private double steamCarry;

    /**
     * Advances one tick.
     *
     * @param hotPrimary hot primary waiting in the tubes, mB
     * @param coldSpace  room for cooled primary, mB
     * @param feedwater  water on the secondary side, mB
     * @param steamSpace room for steam, mB
     * @param steamFill  how full the steam space is, 0-1
     * @param tubes      tube bundle blocks
     */
    public Result tick(int hotPrimary, int coldSpace, int feedwater, int steamSpace, double steamFill, int tubes, Params p) {
        double pressure = secondaryPressure(steamFill, p);
        double gap = Math.max(0.0, p.primaryTemperature() - SaturationTable.temperature(pressure));
        double wet = wetFraction(feedwater, tubes, p);
        double capacity = p.uaPerTube() * Math.max(0, tubes) * gap * wet;
        double perPrimary = Math.max(1.0e-9, p.enthalpyPerMb());
        double perSteam = Math.max(1.0e-9, p.steamHeatPerMb());
        double heat = Math.min(capacity, Math.min(Math.max(0, Math.min(hotPrimary, coldSpace)) * perPrimary,
                Math.max(0, Math.min(feedwater, steamSpace)) * perSteam));
        if (heat <= 0.0) {
            return new Result(0, 0, 0.0, pressure, wet);
        }
        primaryCarry += heat / perPrimary;
        steamCarry += heat / perSteam;
        int cooled = (int) Math.min(Math.floor(primaryCarry), Math.min(hotPrimary, coldSpace));
        int steam = (int) Math.min(Math.floor(steamCarry), Math.min(feedwater, steamSpace));
        // What rounding held back carries to the next tick, never more than a mB.
        primaryCarry = Math.min(1.0, primaryCarry - cooled);
        steamCarry = Math.min(1.0, steamCarry - steam);
        return new Result(cooled, steam, heat, pressure, wet);
    }

    /** Secondary pressure: steam piling up in the generator raises it from the minimum to the maximum. */
    public static double secondaryPressure(double steamFill, Params p) {
        double fill = Math.max(0.0, Math.min(1.0, steamFill));
        return p.minPressure() + (p.maxPressure() - p.minPressure()) * fill;
    }

    /** Share of the tubes under water: they dry out as the feedwater drops below what covers them. */
    public static double wetFraction(int feedwater, int tubes, Params p) {
        double needed = Math.max(1.0, tubes * p.wetInventoryPerTube());
        return Math.max(0.0, Math.min(1.0, feedwater / needed));
    }

    public void restore(double primaryCarry, double steamCarry) {
        this.primaryCarry = Math.max(0.0, Math.min(1.0, primaryCarry));
        this.steamCarry = Math.max(0.0, Math.min(1.0, steamCarry));
    }

    public double primaryCarry() {
        return primaryCarry;
    }

    public double steamCarry() {
        return steamCarry;
    }
}
