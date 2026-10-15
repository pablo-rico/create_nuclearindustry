package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * The antimatter vial: a tiny Penning trap with two micro magnet heads and its own battery. The
 * battery holds the field for {@code batteryTicks} (30 s by default) and runs down only while the
 * vial holds antimatter outside a powered trap; a powered trap's vial slot recharges it. When it runs
 * out with antimatter inside, the vial annihilates wherever it is. An empty vial is harmless.
 * Pure arithmetic: charge is in ticks of field left.
 */
public final class VialBattery {

    /**
     * @param capacity        most antimatter a vial holds, ng
     * @param batteryTicks    ticks a full battery holds the field
     * @param rechargePerTick battery ticks a powered trap's slot puts back per tick
     * @param lowShare        battery share below which the vial's glow flickers, 0..1
     * @param transferPerTick antimatter moved per tick between a vial and a trap's slot, ng
     */
    public record Params(double capacity, int batteryTicks, int rechargePerTick, double lowShare, double transferPerTick) {
        /** 4 ng, 30 s, recharged in 3 s, filled in 2 s. */
        public static final Params DEFAULT = new Params(4.0, 600, 10, 0.25, 0.1);
    }

    private VialBattery() {}

    public static boolean loaded(double antimatter) {
        return antimatter > PenningTrapModel.EMPTY;
    }

    /** Battery charge one tick later. */
    public static int tick(Params p, int charge, double antimatter, boolean inPoweredTrap) {
        int full = Math.max(1, p.batteryTicks());
        int c = Math.max(0, Math.min(full, charge));
        if (inPoweredTrap) {
            return Math.min(full, c + Math.max(0, p.rechargePerTick()));
        }
        return loaded(antimatter) ? Math.max(0, c - 1) : c;
    }

    /** Whether a vial in this state annihilates now. */
    public static boolean annihilates(int charge, double antimatter) {
        return loaded(antimatter) && charge <= 0;
    }

    /** Charge as a share of a full battery, 0..1. */
    public static double chargeShare(Params p, int charge) {
        return Math.max(0, Math.min(1, charge / (double) Math.max(1, p.batteryTicks())));
    }

    /** A loaded vial running out of battery: its glow flickers. */
    public static boolean lowBattery(Params p, int charge, double antimatter) {
        return loaded(antimatter) && chargeShare(p, charge) < p.lowShare();
    }

    /** Seconds of field left. */
    public static double secondsLeft(int charge) {
        return Math.max(0, charge) / 20.0;
    }

    /** Fill stage for the item model: 0 empty, 1 up to half, 2 more than half, 3 full. */
    public static int fillStage(Params p, double antimatter) {
        if (!loaded(antimatter)) {
            return 0;
        }
        if (antimatter >= p.capacity() - 1.0e-6) {
            return 3;
        }
        return antimatter <= p.capacity() / 2 ? 1 : 2;
    }

    /** Item property value for {@link #fillStage}: 0, 1/3, 2/3 or 1. */
    public static float fillProperty(Params p, double antimatter) {
        return fillStage(p, antimatter) / 3f;
    }

    /** Antimatter a trap's slot moves into this vial this tick, ng. */
    public static double fillStep(Params p, double vial, double trapStored) {
        return ConduitTransfer.move(p.transferPerTick(), trapStored, Math.max(0, p.capacity() - Math.max(0, vial)));
    }

    /** Antimatter a trap's slot takes out of this vial this tick, ng. */
    public static double drainStep(Params p, double vial, double trapSpace) {
        return ConduitTransfer.move(p.transferPerTick(), vial, trapSpace);
    }
}
