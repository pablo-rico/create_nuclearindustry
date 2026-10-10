package org.papiricoh.create_nuclearindustry.fusion.cryo.physics;

/**
 * The cryogenic plant: compressors push warm helium gas through heat exchangers and an expansion
 * stage, and what comes out the cold end is liquid helium for the tokamak's coils. Capacity grows
 * with the plant's height (each layer is another exchanger stage) and with the compressors' speed.
 * A little helium leaks out of the loop on every pass. Liquid nitrogen fed in as pre-cooling takes
 * the first stage's load off the compressors, so the plant liquefies more while it lasts; it boils
 * off one mB for each mB of liquid helium made. With its intake open to the air the plant
 * liquefies air instead, which is how the first helium charge is won (centrifuging liquid air).
 * Kept free of game classes so it can be unit tested.
 */
public final class CryoplantModel {

    /** What the plant liquefies. */
    public enum Mode { HELIUM, AIR }

    /**
     * @param heliumPerLayer    liquid helium made per casing layer at rated speed, mB/t
     * @param airPerLayer       liquid air made per casing layer at rated speed, mB/t
     * @param loopLoss          share of the helium passing through that leaks out of the loop, 0..1
     * @param nitrogenBonus     extra liquid helium output while pre-cooled with liquid nitrogen, as a share (0.5 = +50%)
     * @param nitrogenPerHelium liquid nitrogen boiled off per mB of liquid helium made while pre-cooling
     */
    public record Params(double heliumPerLayer, double airPerLayer, double loopLoss, double nitrogenBonus,
                         double nitrogenPerHelium) {}

    /**
     * One tick's conditions. Amounts are in mB and may be fractional.
     *
     * @param mode              helium or air liquefaction
     * @param layers            casing layers of the plant (its height)
     * @param speedFactor       the compressors' share of rated speed, 0..1 (see {@link #speedFactor})
     * @param warmHelium        warm helium waiting in the inlet tank
     * @param liquidHeliumSpace room left in the liquid helium tank
     * @param nitrogen          liquid nitrogen in the pre-cooling tank
     * @param liquidAirSpace    room left in the liquid air tank
     */
    public record Inputs(Mode mode, int layers, double speedFactor, double warmHelium, double liquidHeliumSpace,
                         double nitrogen, double liquidAirSpace) {}

    /**
     * @param heliumTaken      warm helium drawn from the inlet tank, mB
     * @param liquidHeliumMade liquid helium added to the outlet tank, mB
     * @param heliumLost       helium that leaked out of the loop, mB ({@code heliumTaken - liquidHeliumMade})
     * @param nitrogenUsed     liquid nitrogen boiled off, mB
     * @param liquidAirMade    liquid air added to its tank, mB
     * @param capacity         what the plant could make this tick with unlimited input and room, mB/t
     * @param precooling       share of the nitrogen bonus in effect, 0..1
     */
    public record Result(double heliumTaken, double liquidHeliumMade, double heliumLost, double nitrogenUsed,
                         double liquidAirMade, double capacity, double precooling) {
        public static final Result IDLE = new Result(0, 0, 0, 0, 0, 0, 0);

        /** Liquid made this tick, whichever the mode. */
        public double made() {
            return liquidHeliumMade + liquidAirMade;
        }
    }

    private CryoplantModel() {}

    /**
     * Share of rated output a machine gives at {@code rpm}: nothing below {@code minSpeed}, then
     * proportional to the speed up to full output at {@code ratedSpeed} and above.
     */
    public static double speedFactor(double rpm, double minSpeed, double ratedSpeed) {
        double speed = Math.abs(rpm);
        if (speed < minSpeed || speed <= 0 || ratedSpeed <= 0) {
            return 0.0;
        }
        return Math.min(1.0, speed / ratedSpeed);
    }

    /** Liquid helium the plant makes at full input, before any nitrogen pre-cooling, mB/t. */
    public static double heliumCapacity(int layers, double speedFactor, Params params) {
        return Math.max(0, layers) * params.heliumPerLayer() * clamp01(speedFactor);
    }

    /** Liquid air the plant makes with its intake open, mB/t. */
    public static double airCapacity(int layers, double speedFactor, Params params) {
        return Math.max(0, layers) * params.airPerLayer() * clamp01(speedFactor);
    }

    public static Result tick(Inputs in, Params params) {
        if (in.layers() <= 0 || in.speedFactor() <= 0) {
            return Result.IDLE;
        }
        if (in.mode() == Mode.AIR) {
            double capacity = airCapacity(in.layers(), in.speedFactor(), params);
            double made = Math.max(0, Math.min(capacity, in.liquidAirSpace()));
            return new Result(0, 0, 0, 0, made, capacity, 0);
        }
        double base = heliumCapacity(in.layers(), in.speedFactor(), params);
        double bonus = Math.max(0, params.nitrogenBonus());
        // Pre-cooling: the full bonus while the nitrogen covers a whole tick of boosted output,
        // a matching share of it when the nitrogen is about to run out.
        double precooling = 0.0;
        if (bonus > 0 && in.nitrogen() > 0) {
            double needed = base * (1 + bonus) * params.nitrogenPerHelium();
            precooling = needed <= 0 ? 1.0 : Math.min(1.0, in.nitrogen() / needed);
        }
        double capacity = base * (1 + bonus * precooling);
        double kept = 1.0 - clamp01(params.loopLoss());
        if (kept <= 0) {
            return new Result(0, 0, 0, 0, 0, capacity, precooling);
        }
        double made = Math.max(0, Math.min(capacity, Math.min(in.warmHelium() * kept, in.liquidHeliumSpace())));
        double taken = made / kept;
        double nitrogenUsed = precooling > 0 ? Math.min(in.nitrogen(), made * params.nitrogenPerHelium()) : 0.0;
        return new Result(taken, made, taken - made, nitrogenUsed, 0, capacity, precooling);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
