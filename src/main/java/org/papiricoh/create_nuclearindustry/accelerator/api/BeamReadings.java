package org.papiricoh.create_nuclearindustry.accelerator.api;

import net.minecraft.nbt.CompoundTag;

/**
 * Everything the accelerator shows: one record for the screen, goggles, Display Link, control room,
 * attachments and the beam renderer. Built by the controller on the server and synced to clients.
 *
 * @param formed          the structure is formed
 * @param suspended       part of it is in unloaded chunks: everything waits
 * @param phase           startup phase
 * @param tripCause       why it last stopped or refuses to start
 * @param energy          beam energy reaching the end of the line, MeV
 * @param maxEnergy       energy the machine reaches at full drive (ring top energy for rings), MeV
 * @param current         beam current reaching the end of the line, uA
 * @param sourceCurrent   current the ion source makes, uA
 * @param power           beam power on target, kW
 * @param lossPower       beam power lost along the way, kW
 * @param lossFraction    share of the source current lost, 0..1
 * @param vacuum          beam pipe pressure, log10 Pa
 * @param field           magnet field, fraction of rated
 * @param magnetPower     share of the magnets the power supplies feed, 0..1
 * @param rfDrive         share of the cavities' radio-frequency power that is there, 0..1
 * @param rfPower         radio-frequency power from the klystrons, MW
 * @param rfDemand        radio-frequency power the cavities need, MW
 * @param hydrogen        hydrogen in the ion source, mB
 * @param energySetpoint  operator's energy setpoint, 0..1
 * @param currentSetpoint operator's current setpoint, 0..1
 * @param ringRadius      ring radius, 0 without a ring
 * @param liquidHelium    liquid helium in the controller, mB
 * @param magnetTemperature temperature of the superconducting parts, K (ambient without any)
 * @param interlocksClear every personnel interlock is closed
 * @param activity        activation of the terminal, dose per second
 * @param cells           beamline blocks, ring included
 */
public record BeamReadings(boolean formed, boolean suspended, BeamPhase phase, BeamTripCause tripCause, double energy,
                           double maxEnergy, double current, double sourceCurrent, double power, double lossPower,
                           double lossFraction, double vacuum, double field, double magnetPower, double rfDrive,
                           double rfPower, double rfDemand, int hydrogen, double energySetpoint, double currentSetpoint,
                           int ringRadius, int liquidHelium, double magnetTemperature, boolean interlocksClear,
                           double activity, int cells) {
    public static final BeamReadings NONE = new BeamReadings(false, false, BeamPhase.OFFLINE, BeamTripCause.NONE, 0, 0, 0, 0, 0,
            0, 0, 5, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 293, true, 0, 0);

    /** Beam is on its way to the target. */
    public boolean beamOn() {
        return formed && !suspended && phase == BeamPhase.BEAM_ON && current > 0;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", formed);
        tag.putBoolean("suspended", suspended);
        tag.putString("phase", phase.name());
        tag.putString("trip", tripCause.name());
        tag.putDouble("energy", energy);
        tag.putDouble("maxEnergy", maxEnergy);
        tag.putDouble("current", current);
        tag.putDouble("sourceCurrent", sourceCurrent);
        tag.putDouble("power", power);
        tag.putDouble("lossPower", lossPower);
        tag.putDouble("lossFraction", lossFraction);
        tag.putDouble("vacuum", vacuum);
        tag.putDouble("field", field);
        tag.putDouble("magnetPower", magnetPower);
        tag.putDouble("rfDrive", rfDrive);
        tag.putDouble("rfPower", rfPower);
        tag.putDouble("rfDemand", rfDemand);
        tag.putInt("hydrogen", hydrogen);
        tag.putDouble("energySetpoint", energySetpoint);
        tag.putDouble("currentSetpoint", currentSetpoint);
        tag.putInt("ringRadius", ringRadius);
        tag.putInt("liquidHelium", liquidHelium);
        tag.putDouble("magnetTemperature", magnetTemperature);
        tag.putBoolean("interlocksClear", interlocksClear);
        tag.putDouble("activity", activity);
        tag.putInt("cells", cells);
        return tag;
    }

    public static BeamReadings read(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return NONE;
        }
        return new BeamReadings(tag.getBoolean("formed"), tag.getBoolean("suspended"), phase(tag.getString("phase")),
                trip(tag.getString("trip")), tag.getDouble("energy"), tag.getDouble("maxEnergy"), tag.getDouble("current"),
                tag.getDouble("sourceCurrent"), tag.getDouble("power"), tag.getDouble("lossPower"), tag.getDouble("lossFraction"),
                tag.getDouble("vacuum"), tag.getDouble("field"), tag.getDouble("magnetPower"), tag.getDouble("rfDrive"),
                tag.getDouble("rfPower"), tag.getDouble("rfDemand"), tag.getInt("hydrogen"), tag.getDouble("energySetpoint"),
                tag.getDouble("currentSetpoint"), tag.getInt("ringRadius"), tag.getInt("liquidHelium"),
                tag.getDouble("magnetTemperature"), tag.getBoolean("interlocksClear"), tag.getDouble("activity"),
                tag.getInt("cells"));
    }

    public static BeamPhase phase(String name) {
        try {
            return BeamPhase.valueOf(name);
        } catch (IllegalArgumentException e) {
            return BeamPhase.OFFLINE;
        }
    }

    public static BeamTripCause trip(String name) {
        try {
            return BeamTripCause.valueOf(name);
        } catch (IllegalArgumentException e) {
            return BeamTripCause.NONE;
        }
    }
}
