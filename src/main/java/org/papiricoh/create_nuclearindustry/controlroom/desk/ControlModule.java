package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * One control module in a desk slot: how the player set it up, plus what it currently shows.
 * The setup comes from the configuration screen and is sanitised on the way in; the shown state is
 * worked out on the server from the console's target and synced to clients for rendering.
 */
public final class ControlModule {
    public static final int LABEL_MAX = 16;
    public static final int PULSE_TICKS = 10;
    public static final float ROD_STEP = 0.05f;
    public static final int THRESHOLD_STEP = 100;
    public static final int MIN_THRESHOLD = 500;
    public static final int MAX_THRESHOLD = 3900;
    /** Setpoint step of a controller clicked like a dial, out of 100. */
    public static final int SETPOINT_STEP = 5;
    /** Full scale of a controller's setpoint, so 0-100 maps onto it: MW for power, °C for temperature. */
    public static final float POWER_SCALE = 1000.0f, TEMPERATURE_SCALE = 4000.0f;

    private final ModuleType type;
    private ModuleFunction function;
    private String label = "";
    /**
     * Signal level of a SIGNAL_LEVEL dial (0-15), the 0-100 threshold setting of an alarm lamp, or the
     * 0-100 setpoint of a controller.
     */
    private int setting;
    /** A controller in automatic: it drives the rod target. In standby it leaves the rods alone. */
    private boolean engaged = true;
    private AlarmMetric metric = AlarmMetric.TEMPERATURE;
    private GaugeReading reading = GaugeReading.REACTOR_TEMPERATURE;
    private OutputMode output = OutputMode.NONE;
    private ItemStack frequencyFirst = ItemStack.EMPTY;
    private ItemStack frequencyLast = ItemStack.EMPTY;

    /** State a TOGGLE lever or key switch holds by itself. */
    private boolean on;
    private int pulse;
    /** A controller's loop and the rod target it last asked for, 0-1, or below 0 before it has. */
    private final PidController pid = new PidController();
    private float demand = -1.0f;

    // Shown state, worked out by evaluate().
    private boolean active;
    private float shown;
    private int signal;

    public ControlModule(ModuleType type) {
        this.type = type;
        this.function = type.defaultFunction();
        if (type == ModuleType.CONTROLLER) {
            this.setting = 50;
        }
    }

    // ------------------------------------------------------------------ setup

    public ModuleType type() {
        return type;
    }

    public ModuleFunction function() {
        return function;
    }

    public String label() {
        return label;
    }

    public int setting() {
        return setting;
    }

    public AlarmMetric metric() {
        return metric;
    }

    public GaugeReading reading() {
        return reading;
    }

    public OutputMode output() {
        return output;
    }

    public boolean engaged() {
        return engaged;
    }

    /** What a controller last set the rod target to, 0-1, or below 0 before it has. */
    public float demand() {
        return demand;
    }

    public boolean isController() {
        return function == ModuleFunction.AUTO_POWER || function == ModuleFunction.AUTO_TEMPERATURE;
    }

    /** A controller's setpoint in its own units: MW or °C. */
    public float setpoint() {
        return setting / 100.0f * (function == ModuleFunction.AUTO_POWER ? POWER_SCALE : TEMPERATURE_SCALE);
    }

    /** "500 MW" or "2000 °C". */
    public String formatSetpoint() {
        return String.format(java.util.Locale.ROOT, function == ModuleFunction.AUTO_POWER ? "%.0f MW" : "%.0f °C", setpoint());
    }

    public ItemStack frequencyFirst() {
        return frequencyFirst;
    }

    public ItemStack frequencyLast() {
        return frequencyLast;
    }

    public boolean hasFrequency() {
        return !frequencyFirst.isEmpty() || !frequencyLast.isEmpty();
    }

    public ControlModule withFunction(ModuleFunction function) {
        if (type.allows(function)) {
            if (this.function != function) {
                pid.reset();
            }
            this.function = function;
            this.setting = Mth.clamp(setting, 0, maxSetting());
        }
        return this;
    }

    public ControlModule withLabel(String label) {
        this.label = sanitizeLabel(label);
        return this;
    }

    public ControlModule withSetting(int setting) {
        this.setting = Mth.clamp(setting, 0, maxSetting());
        return this;
    }

    public ControlModule withMetric(AlarmMetric metric) {
        this.metric = metric;
        return this;
    }

    public ControlModule withReading(GaugeReading reading) {
        this.reading = reading;
        return this;
    }

    public ControlModule withEngaged(boolean engaged) {
        if (this.engaged != engaged) {
            this.engaged = engaged;
            pid.reset();
        }
        return this;
    }

    public ControlModule withOutput(OutputMode output) {
        this.output = output;
        return this;
    }

    public ControlModule withFrequencies(ItemStack first, ItemStack last) {
        this.frequencyFirst = first.isEmpty() ? ItemStack.EMPTY : first.copyWithCount(1);
        this.frequencyLast = last.isEmpty() ? ItemStack.EMPTY : last.copyWithCount(1);
        return this;
    }

    /** The largest {@link #setting()} the function takes. */
    public int maxSetting() {
        return switch (function) {
            case SIGNAL_LEVEL -> 15;
            case LAMP_ALARM, AUTO_POWER, AUTO_TEMPERATURE -> 100;
            default -> 0;
        };
    }

    /** Printable text only, at most {@link #LABEL_MAX} characters, no formatting codes. */
    public static String sanitizeLabel(String label) {
        if (label == null) {
            return "";
        }
        String clean = StringUtil.filterText(label).replace("§", "").strip();
        return clean.length() > LABEL_MAX ? clean.substring(0, LABEL_MAX) : clean;
    }

    // ------------------------------------------------------------------ shown state

    public boolean active() {
        return active;
    }

    /** 0-1: lamp lit, lever or switch on, button engaged, dial position or needle position. */
    public float shown() {
        return shown;
    }

    /** The redstone level the module puts out, 0-15. */
    public int signal() {
        return signal;
    }

    public boolean pulsing() {
        return pulse > 0;
    }

    /** Counts a pulse down; true when it just ended. */
    boolean tickPulse() {
        return pulse > 0 && --pulse == 0;
    }

    /**
     * Works out what the module shows for a console linked to {@code target}, sampled as
     * {@code sample}. Returns true when anything a client or a redstone listener sees changed.
     */
    public boolean evaluate(LinkedTarget.Resolved target, MonitorReading sample) {
        TargetKind kind = target.kind();
        boolean nowActive = isActiveFor(kind, target);
        float nowShown = nowActive ? computeShown(target, sample) : 0.0f;
        int nowSignal = nowActive ? computeSignal(nowShown) : 0;
        boolean changed = nowActive != active || Math.abs(nowShown - shown) > 1.0e-3f || nowSignal != signal;
        active = nowActive;
        shown = nowShown;
        signal = nowSignal;
        return changed;
    }

    private boolean isActiveFor(TargetKind kind, LinkedTarget.Resolved target) {
        return switch (function) {
            case LAMP_RUNNING -> kind == TargetKind.REACTOR || kind == TargetKind.TURBINE || kind == TargetKind.COOLING_TOWER
                    || kind == TargetKind.TOKAMAK;
            case LAMP_ALARM -> metric.watches(kind);
            case READING -> reading.kind() == kind;
            default -> function.worksOn(kind);
        };
    }

    private float computeShown(LinkedTarget.Resolved target, MonitorReading sample) {
        ReactorBlockEntity reactor = target instanceof LinkedTarget.Reactor linked ? linked.reactor() : null;
        FusionReactorBlockEntity tokamak = target instanceof LinkedTarget.Tokamak linked ? linked.tokamak() : null;
        return switch (function) {
            case SCRAM, LAMP_SCRAM -> reactor != null && reactor.isScramRequested() ? 1 : 0;
            case PULSE -> pulse > 0 ? 1 : 0;
            case AUTO_SCRAM -> reactor != null && reactor.getAutoScram().isEnabled() ? 1 : 0;
            case TOKAMAK_FUEL -> tokamak != null && tokamak.isFuelEnabled() ? 1 : 0;
            case TOGGLE -> on ? 1 : 0;
            case ROD_TARGET -> reactor == null ? 0
                    : reactor.getRodTarget() >= 0 ? reactor.getRodTarget() : reactor.getControlRodInsertionRatio();
            case SCRAM_THRESHOLD -> reactor == null ? 0
                    : (float) (reactor.getAutoScram().getThreshold() - MIN_THRESHOLD) / (MAX_THRESHOLD - MIN_THRESHOLD);
            case SIGNAL_LEVEL -> setting / 15.0f;
            case LAMP_RUNNING -> running(sample) ? 1 : 0;
            case LAMP_NO_SIGNAL -> sample.kind() == TargetKind.NONE || sample.kind() == TargetKind.NO_SIGNAL ? 1 : 0;
            case LAMP_ALARM -> metric.tripped(sample, setting) ? 1 : 0;
            case READING -> reading.fraction(sample);
            case AUTO_POWER, AUTO_TEMPERATURE -> setting / 100.0f;
        };
    }

    private static boolean running(MonitorReading sample) {
        return switch (sample.kind()) {
            case REACTOR -> sample.value(MonitorReading.R_POWER) > 0.5f;
            case TURBINE -> sample.value(MonitorReading.T_USE) > 0;
            case COOLING_TOWER -> sample.value(MonitorReading.C_USE) > 0;
            case TOKAMAK -> sample.status().endsWith(".burning");
            default -> false;
        };
    }

    private int computeSignal(float value) {
        return switch (function) {
            // A controller puts out the rod insertion it asks for.
            case AUTO_POWER, AUTO_TEMPERATURE -> engaged && demand >= 0 ? Mth.clamp(Math.round(demand * 15), 0, 15) : 0;
            case ROD_TARGET, SCRAM_THRESHOLD, SIGNAL_LEVEL, READING -> Mth.clamp(Math.round(value * 15), 0, 15);
            default -> value > 0 ? 15 : 0;
        };
    }

    /**
     * A click on the module with an empty hand: {@code back} (sneaking) steps dials down instead
     * of up. Returns whether anything happened.
     */
    public boolean press(LinkedTarget.Resolved target, boolean back) {
        if (!isActiveFor(target.kind(), target)) {
            return false;
        }
        ReactorBlockEntity reactor = target instanceof LinkedTarget.Reactor linked ? linked.reactor() : null;
        FusionReactorBlockEntity tokamak = target instanceof LinkedTarget.Tokamak linked ? linked.tokamak() : null;
        switch (function) {
            case SCRAM -> {
                if (reactor.isScramRequested()) {
                    reactor.resetScram();
                } else {
                    reactor.setManualScram(true);
                }
            }
            case PULSE -> pulse = PULSE_TICKS;
            case AUTO_SCRAM -> reactor.toggleAutoScram();
            case TOKAMAK_FUEL -> tokamak.setFuelEnabled(!tokamak.isFuelEnabled());
            case TOGGLE -> on = !on;
            case ROD_TARGET -> reactor.adjustRodTarget(back ? -ROD_STEP : ROD_STEP);
            case SCRAM_THRESHOLD -> reactor.adjustAutoScramThreshold(back ? -THRESHOLD_STEP : THRESHOLD_STEP);
            case SIGNAL_LEVEL -> setting = Mth.clamp(setting + (back ? -1 : 1), 0, 15);
            case AUTO_POWER, AUTO_TEMPERATURE -> setting = Mth.clamp(setting + (back ? -SETPOINT_STEP : SETPOINT_STEP), 0, 100);
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * One step of a controller's loop: reads the reactor, moves its rod target and returns whether the
     * demand changed. A controller in standby, or facing a SCRAM or an unformed core, lets go and will
     * pick up bumplessly from wherever the rods are when it can act again. {@code dt} is in seconds.
     */
    public boolean control(LinkedTarget.Resolved target, MonitorReading sample, double dt, PidController.Tuning tuning) {
        if (!isController()) {
            return false;
        }
        ReactorBlockEntity reactor = target instanceof LinkedTarget.Reactor linked ? linked.reactor() : null;
        if (!engaged || reactor == null || !reactor.isFormed() || reactor.isScramRequested() || sample.kind() != TargetKind.REACTOR) {
            pid.reset();
            boolean changed = demand >= 0;
            demand = -1.0f;
            return changed;
        }
        double measurement = function == ModuleFunction.AUTO_POWER
                ? sample.value(MonitorReading.R_POWER) / POWER_SCALE
                : sample.value(MonitorReading.R_TEMPERATURE) / TEMPERATURE_SCALE;
        float current = reactor.getRodTarget() >= 0 ? reactor.getRodTarget() : reactor.getControlRodInsertionRatio();
        int steps = reactor.getStructure().map(structure -> structure.innerHeight()).orElse(0);
        float next = (float) pid.step(setting / 100.0, measurement, dt, current, steps, tuning);
        reactor.setRodTarget(next);
        boolean changed = Math.abs(next - demand) > 1.0e-4f;
        demand = next;
        return changed;
    }

    // ------------------------------------------------------------------ persistence

    /** The player-set part: what the configuration screen sends. */
    public CompoundTag writeConfig(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putString("Function", function.name());
        tag.putString("Label", label);
        tag.putInt("Setting", setting);
        tag.putString("Metric", metric.name());
        tag.putString("Reading", reading.name());
        tag.putString("Output", output.name());
        tag.putBoolean("Engaged", engaged);
        tag.put("FrequencyFirst", frequencyFirst.saveOptional(registries));
        tag.put("FrequencyLast", frequencyLast.saveOptional(registries));
        return tag;
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag tag = writeConfig(registries);
        tag.putBoolean("On", on);
        tag.putInt("Pulse", pulse);
        tag.putBoolean("Active", active);
        tag.putFloat("Shown", shown);
        tag.putInt("Signal", signal);
        if (isController()) {
            CompoundTag loop = new CompoundTag();
            loop.putBoolean("Initialized", pid.initialized());
            loop.putDouble("Integral", pid.integral());
            loop.putDouble("Output", pid.output());
            loop.putDouble("Previous", pid.previousMeasurement());
            tag.put("Pid", loop);
            tag.putFloat("Demand", demand);
        }
        return tag;
    }

    /** Reads a module, keeping only what its type allows. Null when the tag names no known type. */
    @Nullable
    public static ControlModule read(CompoundTag tag, HolderLookup.Provider registries) {
        ModuleType type = ModuleType.byName(tag.getString("Type"));
        if (type == null) {
            return null;
        }
        ControlModule module = new ControlModule(type);
        module.applyConfig(tag, registries);
        module.on = tag.getBoolean("On");
        module.pulse = Mth.clamp(tag.getInt("Pulse"), 0, PULSE_TICKS);
        module.active = tag.getBoolean("Active");
        module.shown = Mth.clamp(tag.getFloat("Shown"), 0, 1);
        module.signal = Mth.clamp(tag.getInt("Signal"), 0, 15);
        if (tag.contains("Pid")) {
            CompoundTag loop = tag.getCompound("Pid");
            module.pid.load(loop.getBoolean("Initialized"), loop.getDouble("Integral"), loop.getDouble("Output"),
                    loop.getDouble("Previous"));
            module.demand = Mth.clamp(tag.getFloat("Demand"), -1, 1);
        }
        return module;
    }

    /** Takes the setup from a configuration tag, sanitised; the type never changes. */
    public void applyConfig(CompoundTag tag, HolderLookup.Provider registries) {
        withFunction(ModuleFunction.byName(tag.getString("Function"), type.defaultFunction()));
        withLabel(tag.getString("Label"));
        withSetting(tag.getInt("Setting"));
        try {
            withMetric(AlarmMetric.valueOf(tag.getString("Metric")));
        } catch (IllegalArgumentException ignored) {
            withMetric(AlarmMetric.TEMPERATURE);
        }
        withReading(GaugeReading.byName(tag.getString("Reading")));
        withOutput(OutputMode.byName(tag.getString("Output")));
        withEngaged(!tag.contains("Engaged") || tag.getBoolean("Engaged"));
        withFrequencies(ItemStack.parseOptional(registries, tag.getCompound("FrequencyFirst")),
                ItemStack.parseOptional(registries, tag.getCompound("FrequencyLast")));
    }
}
