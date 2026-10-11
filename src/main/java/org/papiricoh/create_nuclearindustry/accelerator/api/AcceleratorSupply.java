package org.papiricoh.create_nuclearindustry.accelerator.api;

/**
 * What the accelerator's attachments offer this tick, summed over every {@link AcceleratorAttachment}
 * along the beamline.
 *
 * @param rfPower        radio-frequency power from the klystrons, MW
 * @param magnetCapacity magnets the power supplies can feed (superconducting ones count half)
 * @param pumpSpeed      vacuum pumping, 1 = one pump at rated speed
 * @param klystrons      klystrons giving power
 * @param supplies       magnet power supplies running
 * @param pumps          pumps running
 * @param sourceDrive    share of full drive the ion source gets, 0..1
 */
public record AcceleratorSupply(double rfPower, double magnetCapacity, double pumpSpeed, int klystrons, int supplies,
                                int pumps, double sourceDrive) {
    public static final AcceleratorSupply NONE = new AcceleratorSupply(0, 0, 0, 0, 0, 0, 0);

    public static Builder builder() {
        return new Builder();
    }

    /** Collects every attachment's share. */
    public static final class Builder {
        private double rfPower;
        private double magnetCapacity;
        private double pumpSpeed;
        private int klystrons;
        private int supplies;
        private int pumps;
        private double sourceDrive;

        private Builder() {}

        public Builder source(double drive) {
            sourceDrive = Math.max(sourceDrive, Math.max(0, Math.min(1, drive)));
            return this;
        }

        public Builder radioFrequency(double powerMw) {
            if (powerMw > 0) {
                rfPower += powerMw;
                klystrons++;
            }
            return this;
        }

        public Builder magnets(double capacity) {
            if (capacity > 0) {
                magnetCapacity += capacity;
                supplies++;
            }
            return this;
        }

        public Builder pump(double speed) {
            if (speed > 0) {
                pumpSpeed += speed;
                pumps++;
            }
            return this;
        }

        public AcceleratorSupply build() {
            return new AcceleratorSupply(rfPower, magnetCapacity, pumpSpeed, klystrons, supplies, pumps, sourceDrive);
        }
    }
}
