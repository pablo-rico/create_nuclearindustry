package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;

/** Writes {@link TokamakReadings} into the controller's update packet and reads them back on the client. */
final class TokamakReadingsNbt {
    private TokamakReadingsNbt() {}

    static CompoundTag write(TokamakReadings r) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", r.formed());
        tag.putBoolean("suspended", r.suspended());
        tag.putInt("radius", r.majorRadius());
        tag.putString("phase", r.phase().name());
        tag.putString("trip", r.tripCause().name());
        tag.putFloat("temperature", (float) r.temperature());
        tag.putFloat("instability", (float) r.instability());
        tag.putFloat("confinement", (float) r.confinement());
        tag.putFloat("density", (float) r.density());
        tag.putFloat("ash", (float) r.ashFraction());
        tag.putFloat("fusionPower", (float) r.fusionPower());
        tag.putFloat("heatingPower", (float) r.heatingPower());
        // Infinite gain (ignition) is saved as -1.
        tag.putFloat("gain", Double.isInfinite(r.gain()) ? -1f : (float) r.gain());
        tag.putFloat("burnRate", (float) r.burnRate());
        tag.putFloat("tbr", (float) r.tritiumBreeding());
        tag.putBoolean("dt", r.dtMode());
        tag.putFloat("field", (float) r.field());
        tag.putFloat("fieldTarget", (float) r.fieldTarget());
        tag.putFloat("magnetTemperature", (float) r.magnetTemperature());
        tag.putFloat("vacuum", (float) r.vacuum());
        tag.putFloat("fuelRate", (float) r.fuelRate());
        tag.putFloat("dtRatio", (float) r.dtRatio());
        tag.putBoolean("heatingEnabled", r.heatingEnabled());
        tag.putFloat("heatingLevel", (float) r.heatingLevel());
        tag.putInt("deuterium", r.deuterium());
        tag.putInt("tritium", r.tritium());
        tag.putInt("coolant", r.coolant());
        tag.putInt("plasmaSteam", r.plasmaSteam());
        tag.putInt("liquidHelium", r.liquidHelium());
        tag.putInt("warmHelium", r.warmHelium());
        tag.putBoolean("tritiumOverflow", r.tritiumOverflow());
        return tag;
    }

    static TokamakReadings read(CompoundTag tag) {
        if (!tag.getBoolean("formed")) {
            return TokamakReadings.NONE;
        }
        float gain = tag.getFloat("gain");
        return new TokamakReadings(true, tag.getBoolean("suspended"), tag.getInt("radius"),
                phase(tag.getString("phase")), trip(tag.getString("trip")),
                tag.getFloat("temperature"), tag.getFloat("instability"), tag.getFloat("confinement"),
                tag.getFloat("density"), tag.getFloat("ash"), tag.getFloat("fusionPower"), tag.getFloat("heatingPower"),
                gain < 0 ? Double.POSITIVE_INFINITY : gain, tag.getFloat("burnRate"), tag.getFloat("tbr"),
                tag.getBoolean("dt"), tag.getFloat("field"), tag.getFloat("fieldTarget"),
                tag.getFloat("magnetTemperature"), tag.getFloat("vacuum"), tag.getFloat("fuelRate"),
                tag.getFloat("dtRatio"), tag.getBoolean("heatingEnabled"), tag.getFloat("heatingLevel"),
                tag.getInt("deuterium"), tag.getInt("tritium"), tag.getInt("coolant"), tag.getInt("plasmaSteam"),
                tag.getInt("liquidHelium"), tag.getInt("warmHelium"), tag.getBoolean("tritiumOverflow"));
    }

    static TokamakPhase phase(String name) {
        try {
            return TokamakPhase.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return TokamakPhase.OFFLINE;
        }
    }

    static TripCause trip(String name) {
        try {
            return TripCause.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return TripCause.NONE;
        }
    }
}
