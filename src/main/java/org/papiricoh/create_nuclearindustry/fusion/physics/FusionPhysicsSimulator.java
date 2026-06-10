package org.papiricoh.create_nuclearindustry.fusion.physics;

import net.minecraft.nbt.CompoundTag;

/**
 * Plasma physics for the fusion reactor. Mirrors the structure of
 * {@code ReactorPhysicsSimulator} but models magnetic-confinement fusion instead of fission.
 *
 * <p>Behaviour, in short: external rotational power (Create stress) drives the confinement
 * field. Confinement heats the plasma; once it crosses the ignition threshold (with enough
 * confinement and D-T fuel) fusion ignites and becomes self-heating, producing fusion power
 * that is turned into plasma steam by the coolant loop. If confinement collapses while hot,
 * instability climbs and — if sustained — containment is lost (the controller then triggers a
 * moderate breach).
 */
public class FusionPhysicsSimulator {
    public static final double AMBIENT_TEMP = 20.0;
    public static final double IGNITION_TEMP = 1500.0;
    public static final double OPERATING_TEMP = 2000.0;
    public static final double MAX_PLASMA_TEMP = 4000.0;
    public static final double FUEL_PER_PELLET = 100.0;
    public static final double FUEL_CAPACITY = 500.0;

    private static final double CONFINEMENT_HEAT_GAIN = 6.0;
    private static final double RADIATIVE_LOSS_RATE = 0.9;
    private static final double FUSION_HEAT_GAIN = 0.05;
    private static final double FUEL_BURN_RATE = 0.0015;
    private static final double POWER_PER_TEMP = 0.4;
    private static final double MIN_CONFINEMENT_FOR_IGNITION = 0.65;
    private static final double INSTABILITY_START_CONF = 0.55;
    private static final double INSTABILITY_MAX = 100.0;
    private static final double STEAM_POWER_FACTOR = 0.06;
    public static final double MAX_PLASMA_STEAM_PER_TICK = 160.0;
    private static final int CONTAINMENT_LOSS_CONFIRM_TICKS = 100;

    private final int electromagnetCount;
    private final int ringSize;

    private double plasmaTemperature = AMBIENT_TEMP;
    private double confinementInput;
    private double confinementField;
    private double fuelRemaining;
    private double instability;
    private boolean ignited;
    private double fusionPower;
    private double plasmaSteamRate;
    private int containmentLossTickCounter;

    public FusionPhysicsSimulator(int electromagnetCount, int ringSize) {
        this.electromagnetCount = Math.max(0, electromagnetCount);
        this.ringSize = Math.max(1, ringSize);
    }

    /** Raw 0..1 confinement demand from the magnet input(s); applied on the next tick. */
    public void setConfinementInput(double raw) {
        this.confinementInput = clamp(raw, 0.0, 1.0);
    }

    /**
     * Advances the simulation one tick.
     *
     * @return {@code true} while contained; {@code false} when containment is lost (breach).
     */
    public boolean tick() {
        double magnetFactor = clamp(electromagnetCount / (double) ringSize, 0.0, 1.0);
        confinementField = confinementInput * magnetFactor;

        plasmaTemperature += CONFINEMENT_HEAT_GAIN * confinementField;
        if (ignited) {
            plasmaTemperature += FUSION_HEAT_GAIN * fusionPower;
        }
        plasmaTemperature -= RADIATIVE_LOSS_RATE * (plasmaTemperature / 1000.0);
        if (plasmaTemperature < AMBIENT_TEMP) {
            plasmaTemperature = AMBIENT_TEMP;
        }

        if (!ignited
                && plasmaTemperature >= IGNITION_TEMP
                && confinementField >= MIN_CONFINEMENT_FOR_IGNITION
                && fuelRemaining > 0.0) {
            ignited = true;
        }

        if (ignited) {
            fusionPower = Math.max(0.0, (plasmaTemperature - IGNITION_TEMP) * POWER_PER_TEMP);
            fuelRemaining -= FUEL_BURN_RATE * fusionPower;
            if (fuelRemaining <= 0.0) {
                fuelRemaining = 0.0;
                ignited = false;
                fusionPower = 0.0;
            }
        } else {
            fusionPower = 0.0;
        }

        plasmaSteamRate = Math.min(MAX_PLASMA_STEAM_PER_TICK, fusionPower * STEAM_POWER_FACTOR);

        if (plasmaTemperature > IGNITION_TEMP && confinementField < INSTABILITY_START_CONF) {
            instability += (INSTABILITY_START_CONF - confinementField) * 4.0;
        } else {
            instability -= 2.0;
        }
        instability = clamp(instability, 0.0, INSTABILITY_MAX);

        boolean disruption = instability >= INSTABILITY_MAX || plasmaTemperature > MAX_PLASMA_TEMP;
        if (disruption) {
            containmentLossTickCounter++;
        } else {
            containmentLossTickCounter = Math.max(0, containmentLossTickCounter - 2);
        }
        return containmentLossTickCounter < CONTAINMENT_LOSS_CONFIRM_TICKS;
    }

    /** Removes heat (used by the coolant loop, which turns that heat into plasma steam). */
    public void applyExternalCooling(double cooling) {
        plasmaTemperature = Math.max(AMBIENT_TEMP, plasmaTemperature - cooling);
    }

    public boolean canLoadFuel() {
        return fuelRemaining + FUEL_PER_PELLET <= FUEL_CAPACITY;
    }

    public boolean loadFuel() {
        if (!canLoadFuel()) {
            return false;
        }
        fuelRemaining = Math.min(FUEL_CAPACITY, fuelRemaining + FUEL_PER_PELLET);
        return true;
    }

    public double getPlasmaTemperature() {
        return plasmaTemperature;
    }

    public double getConfinementField() {
        return confinementField;
    }

    public double getFuelRemaining() {
        return fuelRemaining;
    }

    public double getFuelCapacity() {
        return FUEL_CAPACITY;
    }

    public double getInstability() {
        return instability;
    }

    public boolean isIgnited() {
        return ignited;
    }

    public double getFusionPower() {
        return fusionPower;
    }

    public double getPlasmaSteamRate() {
        return plasmaSteamRate;
    }

    public FusionState getState() {
        if (instability >= 60.0) {
            return FusionState.UNSTABLE;
        }
        if (ignited) {
            return FusionState.IGNITED;
        }
        if (plasmaTemperature > AMBIENT_TEMP + 5.0) {
            return FusionState.CHARGING;
        }
        return FusionState.IDLE;
    }

    public void serializeNBT(CompoundTag tag) {
        tag.putDouble("plasmaTemperature", plasmaTemperature);
        tag.putDouble("confinementField", confinementField);
        tag.putDouble("fuelRemaining", fuelRemaining);
        tag.putDouble("instability", instability);
        tag.putBoolean("ignited", ignited);
        tag.putDouble("fusionPower", fusionPower);
        tag.putDouble("plasmaSteamRate", plasmaSteamRate);
        tag.putInt("containmentLossTickCounter", containmentLossTickCounter);
    }

    public void deserializeNBT(CompoundTag tag) {
        plasmaTemperature = Math.max(AMBIENT_TEMP, tag.getDouble("plasmaTemperature"));
        confinementField = clamp(tag.getDouble("confinementField"), 0.0, 1.0);
        fuelRemaining = Math.max(0.0, tag.getDouble("fuelRemaining"));
        instability = clamp(tag.getDouble("instability"), 0.0, INSTABILITY_MAX);
        ignited = tag.getBoolean("ignited");
        fusionPower = Math.max(0.0, tag.getDouble("fusionPower"));
        plasmaSteamRate = Math.max(0.0, tag.getDouble("plasmaSteamRate"));
        containmentLossTickCounter = Math.max(0, tag.getInt("containmentLossTickCounter"));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum FusionState {
        IDLE("Idle"),
        CHARGING("Charging"),
        IGNITED("Ignited"),
        UNSTABLE("Unstable"),
        BREACH("Breach");

        private final String displayName;

        FusionState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
