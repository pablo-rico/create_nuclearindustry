package org.papiricoh.create_nuclearindustry.fusion.cryo.physics;

/**
 * The helium well: a shaft-driven wellhead that draws the traces of helium seeping out of the rock,
 * the first step of the cryogenic chain (helium gas, then the cryogenic plant, then liquid helium).
 * Its output is a few hundredths of a millibucket a tick, so it builds up in a fractional balance
 * and only whole millibuckets reach the tank. While the tank is full the well draws nothing, so the
 * balance never grows past one millibucket. Kept free of game classes so it can be unit tested.
 */
public final class HeliumWellModel {

    /**
     * One tick's result.
     *
     * @param balance  fractional helium still owed to the tank, mB (0..1)
     * @param released whole millibuckets that go into the tank this tick
     */
    public record Step(double balance, int released) {}

    private HeliumWellModel() {}

    /** Helium drawn per tick at {@code rate} mB/t for a speed factor of 0..1 (see {@link CryoplantModel#speedFactor}). */
    public static double output(double rate, double speedFactor) {
        if (!(rate > 0) || !(speedFactor > 0)) {
            return 0.0;
        }
        return rate * Math.min(1.0, speedFactor);
    }

    /**
     * Adds {@code perTick} to the balance and releases the whole millibuckets that fit in
     * {@code space}. With no room (or no output) nothing accrues; the balance is capped at one
     * millibucket so a full, unattended well does not pile up a debt.
     */
    public static Step tick(double balance, double perTick, int space) {
        double owed = Double.isFinite(balance) ? Math.max(0.0, Math.min(1.0, balance)) : 0.0;
        if (space <= 0 || !(perTick > 0) || !Double.isFinite(perTick)) {
            return new Step(owed, 0);
        }
        owed += perTick;
        int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(owed));
        int released = Math.min(whole, space);
        owed -= released;
        return new Step(Math.min(1.0, owed), released);
    }

    /** A per-tick rate written as millibuckets per minute. */
    public static double perMinute(double perTick) {
        return perTick * 20.0 * 60.0;
    }
}
