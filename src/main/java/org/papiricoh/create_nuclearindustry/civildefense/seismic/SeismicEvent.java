package org.papiricoh.create_nuclearindustry.civildefense.seismic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * One detonation as a seismic detector felt it.
 *
 * @param distance blocks from the detector to the blast
 * @param power    blast power reported by the detonation
 * @param bearing  direction of the blast from the detector, degrees clockwise from north
 * @param gameTime when it was felt
 * @param dirty    a dirty bomb rather than a nuclear one
 */
public record SeismicEvent(double distance, float power, float bearing, long gameTime, boolean dirty) {

    public double groundMotion(double falloff) {
        return SeismicModel.groundMotion(power, distance, falloff);
    }

    /** "N", "NE"... translated. */
    public MutableComponent compassPoint() {
        return Component.translatable("create_nuclearindustry.compass." + SeismicModel.COMPASS[SeismicModel.compassPoint(bearing)]);
    }

    /** "Power 120, 340 m NE": what the goggles and display links show. */
    public MutableComponent describe() {
        return Component.translatable("create_nuclearindustry.seismic.event", Math.round(power), Math.round(distance), compassPoint());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Distance", distance);
        tag.putFloat("Power", power);
        tag.putFloat("Bearing", bearing);
        tag.putLong("Time", gameTime);
        tag.putBoolean("Dirty", dirty);
        return tag;
    }

    public static SeismicEvent load(CompoundTag tag) {
        return new SeismicEvent(tag.getDouble("Distance"), tag.getFloat("Power"), tag.getFloat("Bearing"),
                tag.getLong("Time"), tag.getBoolean("Dirty"));
    }
}
