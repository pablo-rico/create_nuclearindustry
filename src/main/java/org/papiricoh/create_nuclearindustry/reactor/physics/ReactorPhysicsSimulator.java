package org.papiricoh.create_nuclearindustry.reactor.physics;

import net.minecraft.nbt.CompoundTag;

/**
 * Handles all nuclear physics calculations for the reactor.
 * Simulates neutron production/consumption, temperature changes, and fuel depletion.
 */
public class ReactorPhysicsSimulator {

    // Physics constants
    private static final double BASE_FISSION_RATE = 10.0;           // neutrons per uranium rod per tick
    private static final double HEAT_GENERATION_RATE = 0.15;        // °C per neutron
    private static final double PASSIVE_HEAT_DISSIPATION = 0.5;     // °C per tick (baseline)
    private static final double CONTROL_ROD_EFFECTIVENESS = 0.85;   // neutron absorption rate (0-1)
    private static final double FUEL_CONSUMPTION_RATE = 0.01;       // fuel units consumed per neutron

    // Temperature thresholds
    private static final double SAFE_TEMP = 500.0;                  // Below this = safe operation
    private static final double WARNING_TEMP = 2000.0;              // Yellow warning zone
    private static final double DANGER_TEMP = 3500.0;               // Red danger zone
    private static final double MELTDOWN_TEMP = 4000.0;             // Absolute limit

    // Limits
    private static final double MAX_NEUTRON_LEVEL = 1000.0;
    private static final double MIN_NEUTRON_LEVEL = 0.0;

    // State variables
    private double coreTemperature;                                 // Celsius (0-4000)
    private double neutronLevel;                                    // 0-1000
    private double fuelRemaining;                                   // Fuel units (0-100)
    private float controlRodInsertion;                              // 0.0 = withdrawn, 1.0 = fully inserted
    private int uraniumRodCount;                                    // Number of fuel rods in reactor
    private int controlRodCount;                                    // Number of control rods (PHYSICAL blocks)
    private int meltdownTickCounter;                                // Ticks spent above meltdown temp

    // Derived values for display
    private double powerOutput;                                     // MW equivalent
    private double steamGenerationRate;                             // mB per tick

    public ReactorPhysicsSimulator(int uraniumRodCount) {
        this(uraniumRodCount, 0);
    }

    public ReactorPhysicsSimulator(int uraniumRodCount, int controlRodCount) {
        this.coreTemperature = 20.0;                                // Start at room temperature
        this.neutronLevel = 0.0;
        this.fuelRemaining = 100.0;                                 // Start with 100% fuel
        this.controlRodInsertion = 1.0f;                            // Start fully inserted (safe state)
        this.uraniumRodCount = uraniumRodCount;
        this.controlRodCount = controlRodCount;
        this.meltdownTickCounter = 0;
        this.powerOutput = 0.0;
        this.steamGenerationRate = 0.0;
    }

    /**
     * Simulates one tick of reactor operation.
     * @return true if reactor is functioning normally, false if melted down
     */
    public boolean tick() {
        if (fuelRemaining <= 0) {
            // No fuel, shut down
            neutronLevel = Math.max(0, neutronLevel - 5.0);
            coreTemperature = Math.max(20.0, coreTemperature - PASSIVE_HEAT_DISSIPATION * 2);
            return true;
        }

        // Calculate neutron production from fission
        double neutronProduction = calculateNeutronProduction();

        // Calculate neutron consumption from fuel
        double neutronConsumption = uraniumRodCount * FUEL_CONSUMPTION_RATE * neutronLevel;

        // Update neutron level
        neutronLevel += (neutronProduction - neutronConsumption);
        neutronLevel = Math.max(MIN_NEUTRON_LEVEL, Math.min(MAX_NEUTRON_LEVEL, neutronLevel));

        // Calculate heat changes
        double heatGeneration = neutronLevel * HEAT_GENERATION_RATE;
        double heatDissipation = calculateHeatDissipation();

        // Update temperature
        coreTemperature += (heatGeneration - heatDissipation);
        coreTemperature = Math.max(0, coreTemperature);

        // Consume fuel based on operation
        if (neutronLevel > 0) {
            fuelRemaining -= FUEL_CONSUMPTION_RATE * (neutronLevel / 100.0);
            fuelRemaining = Math.max(0, fuelRemaining);
        }

        // Calculate power output
        powerOutput = neutronLevel * 0.5; // 0.5 MW per neutron level unit

        // Calculate steam generation (temperature must be above 373K/100°C)
        if (coreTemperature > 100.0) {
            steamGenerationRate = (coreTemperature - 100.0) * 0.1; // mB per tick
        } else {
            steamGenerationRate = 0.0;
        }

        // Check for meltdown
        if (coreTemperature > MELTDOWN_TEMP) {
            meltdownTickCounter++;
            if (meltdownTickCounter > 200) {
                // Meltdown occurs
                return false;
            }
        } else {
            meltdownTickCounter = 0;
        }

        return true;
    }

    /**
     * Calculates neutron production based on fission reactions.
     * Depends on the RATIO of control rods to uranium rods.
     */
    private double calculateNeutronProduction() {
        if (uraniumRodCount == 0) {
            return 0;
        }

        // If no control rods are present, there's nothing to absorb neutrons - MELTDOWN RISK
        if (controlRodCount == 0) {
            // With no physical control rods, neutron absorption is 0 regardless of position
            return uraniumRodCount * BASE_FISSION_RATE;
        }

        // The ratio of control rods to uranium rods determines absorption capacity
        // 1.0 ratio = balanced (1 control per 1 uranium)
        // > 1.0 = overcontrolled (can stop reaction)
        // < 1.0 = undercontrolled (reaction hard to control)
        double rodRatio = (double) controlRodCount / uraniumRodCount;

        // Maximum absorption capacity based on ratio (capped at 100%)
        double maxAbsorption = Math.min(1.0, rodRatio * CONTROL_ROD_EFFECTIVENESS);

        double actualAbsorption = maxAbsorption * controlRodInsertion;
        double absorptionFactor = 1.0 - actualAbsorption;

        // Base fission multiplied by number of uranium rods and absorption factor
        return uraniumRodCount * BASE_FISSION_RATE * absorptionFactor;
    }

    /**
     * Calculates heat dissipation from the reactor.
     */
    private double calculateHeatDissipation() {
        // Passive dissipation based on temperature difference from environment
        double tempDifference = coreTemperature - 20.0; // Assume 20°C environment
        double passiveDissipation = PASSIVE_HEAT_DISSIPATION * (tempDifference / 100.0);

        // Heat dissipation increases with temperature (radiation loss)
        double radiationLoss = Math.pow(coreTemperature / 3600.0, 2) * 0.1;

        // At high temperatures, additional dissipation is reduced (thermodynamics)
        if (coreTemperature > WARNING_TEMP) {
            return passiveDissipation + radiationLoss;
        }

        return passiveDissipation;
    }

    /**
     * Sets the control rod position.
     * @param position 0.0 = fully inserted (maximum neutron absorption), 1.0 = fully withdrawn
     */
    public void setControlRodPosition(float position) {
        setControlRodInsertion(1.0f - position);
    }

    public void setControlRodInsertion(float insertion) {
        this.controlRodInsertion = Math.max(0.0f, Math.min(1.0f, insertion));
    }

    /**
     * Changes control rod position by a given amount.
     * Used when moving the rod with Create contraptions.
     */
    public void moveControlRod(float delta) {
        setControlRodPosition(getControlRodPosition() + delta);
    }

    public void scramTick() {
        controlRodInsertion = 1.0f;
        neutronLevel = Math.max(0.0, neutronLevel - 40.0);
        coreTemperature = Math.max(20.0, coreTemperature - PASSIVE_HEAT_DISSIPATION * 3.0);
        powerOutput = 0.0;
        steamGenerationRate = 0.0;
    }

    public void applyExternalCooling(double cooling, double neutronModeration) {
        coreTemperature = Math.max(20.0, coreTemperature - Math.max(0.0, cooling));
        neutronLevel = Math.max(MIN_NEUTRON_LEVEL, neutronLevel - Math.max(0.0, neutronModeration));
    }

    public void applyExternalHeat(double heat) {
        coreTemperature = Math.max(20.0, coreTemperature + Math.max(0.0, heat));
    }

    // Getters for display and monitoring
    public double getCoreTemperature() {
        return coreTemperature;
    }

    public double getNeutronLevel() {
        return neutronLevel;
    }

    public double getFuelRemaining() {
        return fuelRemaining;
    }

    public float getControlRodPosition() {
        return 1.0f - controlRodInsertion;
    }

    public float getControlRodInsertion() {
        return controlRodInsertion;
    }

    public int getControlRodCount() {
        return controlRodCount;
    }

    public double getPowerOutput() {
        return powerOutput;
    }

    public double getSteamGenerationRate() {
        return steamGenerationRate;
    }

    public int getMeltdownTickCounter() {
        return meltdownTickCounter;
    }

    public boolean isMeltingDown() {
        return coreTemperature > MELTDOWN_TEMP;
    }

    public boolean isCritical() {
        return coreTemperature > WARNING_TEMP;
    }

    public boolean isRunning() {
        return neutronLevel > 1.0 && fuelRemaining > 0;
    }

    /**
     * Gets the reactor state as a string for debugging.
     */
    public ReactorState getState() {
        if (coreTemperature > MELTDOWN_TEMP) {
            return ReactorState.MELTING;
        } else if (coreTemperature > DANGER_TEMP) {
            return ReactorState.CRITICAL;
        } else if (coreTemperature > WARNING_TEMP) {
            return ReactorState.WARNING;
        } else if (neutronLevel > 1.0) {
            return ReactorState.RUNNING;
        } else {
            return ReactorState.IDLE;
        }
    }

    // NBT Serialization for saving state
    public void serializeNBT(CompoundTag tag) {
        tag.putDouble("coreTemperature", coreTemperature);
        tag.putDouble("neutronLevel", neutronLevel);
        tag.putDouble("fuelRemaining", fuelRemaining);
        tag.putFloat("controlRodInsertion", controlRodInsertion);
        tag.putInt("uraniumRodCount", uraniumRodCount);
        tag.putInt("controlRodCount", controlRodCount);
        tag.putInt("meltdownTickCounter", meltdownTickCounter);
    }

    public void deserializeNBT(CompoundTag tag) {
        this.coreTemperature = tag.getDouble("coreTemperature");
        this.neutronLevel = tag.getDouble("neutronLevel");
        this.fuelRemaining = tag.getDouble("fuelRemaining");
        this.controlRodInsertion = tag.contains("controlRodInsertion")
                ? tag.getFloat("controlRodInsertion")
                : 1.0f - tag.getFloat("controlRodPosition");
        this.uraniumRodCount = tag.getInt("uraniumRodCount");
        this.controlRodCount = tag.getInt("controlRodCount");
        this.meltdownTickCounter = tag.getInt("meltdownTickCounter");
    }

    /**
     * Enum representing reactor operational states.
     */
    public enum ReactorState {
        IDLE("Idle"),
        RUNNING("Running"),
        WARNING("Warning"),
        CRITICAL("Critical"),
        MELTING("MELTING");

        private final String displayName;

        ReactorState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
