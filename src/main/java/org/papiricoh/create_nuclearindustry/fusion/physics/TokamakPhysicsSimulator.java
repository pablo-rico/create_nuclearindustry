package org.papiricoh.create_nuclearindustry.fusion.physics;

import net.minecraft.nbt.CompoundTag;

/**
 * The controller's plasma: a {@link PlasmaModel} plus its saved form. Keeps the getters and
 * constants other code reads, and loads controllers saved before the plasma model had an inventory
 * (those report {@link #wasLegacyBurning()} so the controller can shut them down cleanly).
 */
public class TokamakPhysicsSimulator {
    public static final double AMBIENT_TEMP = PlasmaModel.AMBIENT_TEMP;
    public static final double IGNITION_TEMP = PlasmaModel.IGNITION_TEMP;
    public static final double DANGER_TEMP = PlasmaModel.DANGER_TEMP;
    public static final double MAX_TEMP = PlasmaModel.MAX_TEMP;
    public static final double STABLE_CONFINEMENT = PlasmaModel.STABLE_CONFINEMENT;
    /**
     * Confinement the monitors warn below. Burning no longer needs a fixed confinement (the power
     * balance decides), but below {@link #STABLE_CONFINEMENT} a hot plasma grows unstable.
     */
    public static final double MIN_BURN_CONFINEMENT = 0.65;
    public static final double DISRUPTION_INSTABILITY = PlasmaModel.DISRUPTION_INSTABILITY;

    private final PlasmaModel model = new PlasmaModel();
    private boolean legacyBurning;

    public PlasmaModel model() {
        return model;
    }

    public void quench() {
        model.quench();
    }

    public boolean isDisrupting() {
        return model.isDisrupting();
    }

    public boolean isBurning() {
        return model.isBurning();
    }

    public double getPlasmaTemperature() {
        return model.getTemperature();
    }

    public double getInstability() {
        return model.getInstability();
    }

    public double getConfinement() {
        return model.getConfinement();
    }

    public double getFusionPower() {
        return model.getFusionPower();
    }

    public double getBurnRate() {
        return model.getBurnRate();
    }

    public double getTritiumBreedingRatio() {
        return model.getTritiumBreedingRatio();
    }

    public PlasmaModel.Mode getMode() {
        return model.getMode();
    }

    /** The saved controller predates the startup sequence and had a hot or burning plasma. */
    public boolean wasLegacyBurning() {
        return legacyBurning;
    }

    public void clearLegacy() {
        legacyBurning = false;
    }

    public void serializeNBT(CompoundTag tag) {
        tag.putDouble("plasmaTemperature", model.getTemperature());
        tag.putDouble("instability", model.getInstability());
        tag.putDouble("confinement", model.getConfinement());
        tag.putDouble("fusionPower", model.getFusionPower());
        tag.putDouble("heatingPower", model.getHeatingPower());
        tag.putDouble("burnRate", model.getBurnRate());
        tag.putDouble("density", model.getDensity());
        tag.putDouble("tbr", model.getTritiumBreedingRatio());
        tag.putString("mode", model.getMode().name());
        tag.putDouble("plasmaDeuterium", model.getDeuterium());
        tag.putDouble("plasmaTritium", model.getTritium());
        tag.putDouble("plasmaAsh", model.getAsh());
        tag.putDouble("solenoidFlux", model.getSolenoidFlux());
    }

    public void deserializeNBT(CompoundTag tag) {
        PlasmaModel.Mode mode;
        try {
            mode = PlasmaModel.Mode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = PlasmaModel.Mode.COLD;
        }
        double temperature = tag.contains("plasmaTemperature") ? tag.getDouble("plasmaTemperature") : AMBIENT_TEMP;
        boolean legacy = !tag.contains("plasmaDeuterium");
        if (legacy) {
            // Saved by the old model: no fuel inventory, so the plasma cannot carry on. Keep the
            // readings for one look and let the controller ramp it down.
            legacyBurning = mode != PlasmaModel.Mode.COLD || temperature > AMBIENT_TEMP + 1;
            model.restore(AMBIENT_TEMP, 0, 0, 0, 0, 1.0);
            model.restoreResults(0, 0, 0, 0, tag.getDouble("confinement"), tag.getDouble("tbr"), PlasmaModel.Mode.COLD);
            return;
        }
        model.restore(temperature, tag.getDouble("plasmaDeuterium"), tag.getDouble("plasmaTritium"),
                tag.getDouble("plasmaAsh"), tag.getDouble("instability"),
                tag.contains("solenoidFlux") ? tag.getDouble("solenoidFlux") : 1.0);
        model.restoreResults(tag.getDouble("fusionPower"), tag.getDouble("heatingPower"), tag.getDouble("burnRate"),
                tag.getDouble("density"), tag.getDouble("confinement"), tag.getDouble("tbr"), mode);
    }
}
