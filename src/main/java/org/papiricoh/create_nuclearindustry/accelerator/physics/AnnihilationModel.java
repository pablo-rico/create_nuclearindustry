package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * The annihilation generator: antiprotons meet matter in a magnetic chamber and the energy comes out
 * as rotation, Forge Energy or both. At full throttle it burns {@code ratedBurn} nanograms per tick
 * for {@code ratedCapacity} SU per RPM at {@code ratedSpeed}, or {@code ratedEnergy} FE per tick. A
 * generator with fuel keeps a small standby burn to hold its own chamber's field. Pure arithmetic.
 */
public final class AnnihilationModel {

    /** Where the generator's power goes. Saved by name. */
    public enum Mode {
        ROTATION, ELECTRIC, SPLIT;

        public double rotationShare() {
            return switch (this) {
                case ROTATION -> 1.0;
                case ELECTRIC -> 0.0;
                case SPLIT -> 0.5;
            };
        }
    }

    /**
     * @param ratedBurn     antiprotons burnt per tick at full throttle, ng
     * @param ratedSpeed    shaft speed the generator turns at, RPM
     * @param ratedCapacity stress capacity at full throttle, SU per RPM
     * @param ratedEnergy   Forge Energy at full throttle, FE per tick
     * @param standbyShare  share of the rated burn a fuelled generator always uses
     */
    public record Params(double ratedBurn, double ratedSpeed, double ratedCapacity, double ratedEnergy,
                         double standbyShare) {
        /** 6 ng a minute for 256 RPM x 256 SU/RPM, or 16384 FE/t. */
        public static final Params DEFAULT = new Params(6.0 / 1200.0, 256, 256, 16384, 0.02);
    }

    /**
     * One tick of the generator.
     *
     * @param burnt    antiprotons used, ng
     * @param capacity stress capacity offered, SU per RPM
     * @param energy   Forge Energy made, FE per tick
     */
    public record Output(double burnt, double capacity, double energy) {
        public static final Output NONE = new Output(0, 0, 0);
    }

    private AnnihilationModel() {}

    /**
     * @param throttle  wanted output, 0..1
     * @param available antiprotons in the generator, ng
     */
    public static Output tick(Params p, double throttle, double available, Mode mode) {
        if (available <= 0) {
            return Output.NONE;
        }
        double wanted = clamp(throttle, 0, 1);
        // At zero throttle the standby burn only holds the field and gives nothing.
        double demand = p.ratedBurn() * Math.max(p.standbyShare(), wanted);
        double burnt = Math.min(demand, available);
        double fraction = demand <= 0 ? 0 : wanted * burnt / demand;
        double rotation = (mode == null ? Mode.ROTATION : mode).rotationShare();
        return new Output(burnt, fraction * rotation * p.ratedCapacity(), fraction * (1 - rotation) * p.ratedEnergy());
    }

    /** Minutes a generator at this throttle runs on this many nanograms. */
    public static double minutesOn(Params p, double nanograms, double throttle) {
        double perTick = p.ratedBurn() * Math.max(p.standbyShare(), clamp(throttle, 0, 1));
        return perTick <= 0 ? Double.POSITIVE_INFINITY : nanograms / perTick / 1200.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
