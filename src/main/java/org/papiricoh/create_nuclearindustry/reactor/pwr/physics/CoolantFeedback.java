package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

/**
 * How the water of a pressurized reactor steers its chain reaction. The water is the moderator: where
 * it boils into steam or drains away, fewer neutrons slow down enough to split uranium, and warmer
 * water is thinner and moderates less. Both effects push back against a power rise, which is what
 * makes a pressurized water reactor stable. Cooler water than normal does the opposite.
 * Kept free of game classes so it can be unit tested.
 */
public final class CoolantFeedback {
    public static final double MIN = 0.05;
    public static final double MAX = 1.2;

    private CoolantFeedback() {}

    /**
     * Multiplier on neutron production.
     *
     * @param voidFraction           share of the core's water boiled into steam, 0-1
     * @param coverage               share of the core the water covers, 0-1
     * @param averageTemperature     average coolant temperature in the core, °C
     * @param referenceTemperature   average temperature at which the multiplier is 1, °C
     * @param voidWorth              reactivity lost when the core holds no water at all
     * @param temperatureCoefficient reactivity lost per °C above the reference
     */
    public static double multiplier(double voidFraction, double coverage, double averageTemperature,
                                    double referenceTemperature, double voidWorth, double temperatureCoefficient) {
        double water = (1.0 - clamp(voidFraction)) * clamp(coverage);
        double value = 1.0 - voidWorth * (1.0 - water) - temperatureCoefficient * (averageTemperature - referenceTemperature);
        return Math.max(MIN, Math.min(MAX, value));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
