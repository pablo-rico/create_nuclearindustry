package org.papiricoh.create_nuclearindustry.fusion.api;

/**
 * What the tokamak's auxiliary systems offer the plasma this tick, summed over every
 * {@link TokamakAuxiliary} in the shell.
 *
 * @param nbiPower           raw neutral beam heating power, in MW, before any deuterium shortage
 * @param rfPower            raw radio-frequency power, in MW, before plasma coupling
 * @param pumpSpeed          vacuum pumping speed, in the vacuum model's units (1 = one pump at rated speed)
 * @param nbiDeuteriumDemand deuterium the beams want this tick, in mB; the controller scales
 *                           {@code nbiPower} down by whatever share of it the tank cannot give
 * @param nbiCount           beam injectors that are running
 * @param rfCount            antennas that are running
 * @param pumpCount          vacuum pumps that are running
 */
public record TokamakSupply(double nbiPower, double rfPower, double pumpSpeed, double nbiDeuteriumDemand,
                            int nbiCount, int rfCount, int pumpCount) {
    public static final TokamakSupply NONE = new TokamakSupply(0, 0, 0, 0, 0, 0, 0);

    public static Builder builder() {
        return new Builder();
    }

    /** Collects every auxiliary's share. */
    public static final class Builder {
        private double nbiPower;
        private double rfPower;
        private double pumpSpeed;
        private double nbiDeuteriumDemand;
        private int nbiCount;
        private int rfCount;
        private int pumpCount;

        private Builder() {}

        public Builder neutralBeam(double powerMw, double deuteriumMb) {
            if (powerMw > 0) {
                nbiPower += powerMw;
                nbiDeuteriumDemand += Math.max(0, deuteriumMb);
                nbiCount++;
            }
            return this;
        }

        public Builder radioFrequency(double powerMw) {
            if (powerMw > 0) {
                rfPower += powerMw;
                rfCount++;
            }
            return this;
        }

        public Builder pump(double speed) {
            if (speed > 0) {
                pumpSpeed += speed;
                pumpCount++;
            }
            return this;
        }

        public TokamakSupply build() {
            return new TokamakSupply(nbiPower, rfPower, pumpSpeed, nbiDeuteriumDemand, nbiCount, rfCount, pumpCount);
        }
    }
}
