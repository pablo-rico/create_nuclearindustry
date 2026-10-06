package org.papiricoh.create_nuclearindustry.turbine;

import net.minecraft.nbt.CompoundTag;

/**
 * What clients (renderer, goggles, Ponder) need to know about a turbine, synced from the
 * server-side {@link TurbineStructure}.
 */
public record TurbineDisplay(boolean formed, int hu, int hv, int length, int rotors, int inputPorts, int outputPorts,
                             int limit, int currentUse, int tankCapacity, String message, int exhaustPorts) {

    public static final TurbineDisplay EMPTY = new TurbineDisplay(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, "Not formed", 0);

    public TurbineDisplay(boolean formed, int hu, int hv, int length, int rotors, int inputPorts, int outputPorts,
                          int limit, int currentUse, int tankCapacity, String message) {
        this(formed, hu, hv, length, rotors, inputPorts, outputPorts, limit, currentUse, tankCapacity, message, 0);
    }

    public static TurbineDisplay of(TurbineStructure s, int currentUse, int tankCapacity) {
        return new TurbineDisplay(true, s.hu, s.hv, s.length, s.rotorCount(), s.inputPorts, s.outputPorts,
                s.throughputLimit(), currentUse, tankCapacity, "", s.exhaustPorts);
    }

    public static TurbineDisplay unformed(String message, int tankCapacity) {
        return new TurbineDisplay(false, 0, 0, 0, 0, 0, 0, 0, 0, tankCapacity, message, 0);
    }

    public boolean exhausting() {
        return exhaustPorts > 0;
    }

    public float load() {
        return limit <= 0 ? 0 : Math.min(1, currentUse / (float) limit);
    }

    public String dimensionsText(boolean verticalFirst) {
        int a = 2 * hu + 1;
        int b = 2 * hv + 1;
        return verticalFirst ? b + "x" + a + "x" + length : a + "x" + b + "x" + length;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Formed", formed);
        tag.putInt("HalfU", hu);
        tag.putInt("HalfV", hv);
        tag.putInt("Length", length);
        tag.putInt("Rotors", rotors);
        tag.putInt("InputPorts", inputPorts);
        tag.putInt("OutputPorts", outputPorts);
        tag.putInt("Limit", limit);
        tag.putInt("CurrentUse", currentUse);
        tag.putInt("TankCapacity", tankCapacity);
        tag.putString("Message", message);
        tag.putInt("ExhaustPorts", exhaustPorts);
        return tag;
    }

    public static TurbineDisplay read(CompoundTag tag) {
        return new TurbineDisplay(tag.getBoolean("Formed"), tag.getInt("HalfU"), tag.getInt("HalfV"), tag.getInt("Length"),
                tag.getInt("Rotors"), tag.getInt("InputPorts"), tag.getInt("OutputPorts"), tag.getInt("Limit"),
                tag.getInt("CurrentUse"), tag.getInt("TankCapacity"), tag.getString("Message"), tag.getInt("ExhaustPorts"));
    }
}
