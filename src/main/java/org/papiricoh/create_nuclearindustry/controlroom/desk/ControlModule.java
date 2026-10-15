package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakControl;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ShutdownState;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PrimaryLoop;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

/**
 * One control module in a desk slot: how the player set it up, plus what it currently shows.
 * The setup comes from the configuration screen and is sanitised on the way in; the shown state is
 * worked out on the server from the console's target and synced to clients for rendering.
 */
public final class ControlModule {
    public static final int LABEL_MAX = 16;
    public static final int PULSE_TICKS = 10;
    /** A guarded button's cover falls shut by itself this long after it was lifted or the button pressed. */
    public static final int COVER_TICKS = 100;
    public static final float ROD_STEP = 0.05f;
    public static final int THRESHOLD_STEP = 100;
    public static final int MIN_THRESHOLD = 500;
    public static final int MAX_THRESHOLD = 3900;
    /** Setpoint step of a controller clicked like a dial, out of 100. */
    public static final int SETPOINT_STEP = 5;
    /**
     * Full scale of a controller's setpoint, so 0-100 maps onto it: MW for power, °C for temperature, bar for
     * pressure, MW for a tokamak's fusion power.
     */
    public static final float TEMPERATURE_SCALE = 4000.0f, PRESSURE_SCALE = MonitorReading.PRESSURE_MAX,
            FUSION_POWER_SCALE = TokamakScales.POWER_MAX;
    /** Where an automatic fusion power controller starts: about what a tokamak of radius 4 makes. */
    public static final int FUSION_POWER_DEFAULT_SETTING = 25;
    /** 22% of the 4000 C scale: 880 C, about the fuel at rated power. */
    private static final int TEMPERATURE_DEFAULT_SETTING = 22;
    private static final String KEY = "create_nuclearindustry.control_module.";

    private final ModuleType type;
    private ModuleFunction function;
    private String label = "";
    /**
     * Signal level of a SIGNAL_LEVEL dial (0-15), the 0-100 threshold setting of an alarm lamp, or the
     * 0-100 setpoint of a controller.
     */
    private int setting;
    /** A controller in automatic: it drives the rod target or the pressurizer. In standby it leaves them alone. */
    private boolean engaged = true;
    private AlarmMetric metric = AlarmMetric.TEMPERATURE;
    private GaugeReading reading = GaugeReading.REACTOR_TEMPERATURE;
    private OutputMode output = OutputMode.NONE;
    private ItemStack frequencyFirst = ItemStack.EMPTY;
    /** The Advanced Redstone Link code it sends on with {@link OutputMode#ADVANCED_LINK}. */
    private String linkCode = "";
    private ItemStack frequencyLast = ItemStack.EMPTY;

    /** State a TOGGLE or pressurizer lever, or a key switch, holds by itself. */
    private boolean on;
    private int pulse;
    /**
     * A controller's loop and the output it last asked for, 0-1, or below 0 before it has: the rod
     * target, for {@link ModuleFunction#AUTO_PRESSURE} a {@link PressureDemand} output, and for
     * {@link ModuleFunction#AUTO_FUSION_POWER} a {@link FusionPowerDemand} output.
     */
    private final PidController pid = new PidController();
    private float demand = -1.0f;
    /** The heating level the operator left when a fusion power controller took over, or below 0 before it has. */
    private double heatingFloor = -1.0;
    /** What the last click did, for the player who clicked; not saved. */
    @Nullable
    private Component feedback;

    // Shown state, worked out by evaluate().
    private boolean active;
    private float shown;
    private int signal;
    /** A reading's value in its units (gauges and LED displays), or NaN when there is none. */
    private float value = Float.NaN;
    /** Ticks a guarded button's cover stays up; 0 while it is shut. */
    private int coverTicks;
    /** A trend recorder's trace, and the game time of its next sample. */
    @Nullable
    private final TrendRing trend;
    private long nextTrendSample;
    /** The picture a screen module syncs: a trend's samples, a core map or a plasma section; empty for none. */
    private byte[] display = NO_DISPLAY;
    private static final byte[] NO_DISPLAY = new byte[0];

    public ControlModule(ModuleType type) {
        this.type = type;
        this.function = type.defaultFunction();
        if (type == ModuleType.CONTROLLER) {
            this.setting = 50;
        } else if (type == ModuleType.BAR_GRAPH) {
            this.setting = BarGraph.DEFAULT_ALARM;
        }
        this.trend = type == ModuleType.TREND_RECORDER ? new TrendRing() : null;
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

    /** What a controller last asked for (rod target or pressure demand output), 0-1, or below 0 before it has. */
    public float demand() {
        return demand;
    }

    public boolean isController() {
        return function == ModuleFunction.AUTO_POWER || function == ModuleFunction.AUTO_TEMPERATURE
                || function == ModuleFunction.AUTO_PRESSURE || function == ModuleFunction.AUTO_FUSION_POWER;
    }

    /** A controller's setpoint in its own units: % of the reactor's rated power, MW, °C or bar. */
    public float setpoint() {
        return setting / 100.0f * switch (function) {
            case AUTO_POWER -> 100.0f;
            case AUTO_PRESSURE -> PRESSURE_SCALE;
            case AUTO_FUSION_POWER -> FUSION_POWER_SCALE;
            default -> TEMPERATURE_SCALE;
        };
    }

    /** The setpoint's number alone: whole units, or one decimal when it has one (152.5 bar). */
    public String formatSetpointValue() {
        float value = setpoint();
        return String.format(java.util.Locale.ROOT, Math.abs(value - Math.round(value)) < 0.05f ? "%.0f" : "%.1f", value);
    }

    /** "50%" of the rated power, "250 MW", "2000 °C" or "155 bar". */
    public String formatSetpoint() {
        return formatSetpointValue() + switch (function) {
            case AUTO_POWER -> "%";
            case AUTO_FUSION_POWER -> " MW";
            case AUTO_PRESSURE -> " bar";
            default -> " °C";
        };
    }

    /**
     * What a controller is asking for right now: "rods to 40%", "heaters 30%", "spray 20%" or
     * "fuel 60%, heating 40%", or "standby" while it drives nothing.
     */
    public Component describeDemand() {
        if (!engaged || demand < 0) {
            return Component.translatable(KEY + "standby");
        }
        if (function == ModuleFunction.AUTO_PRESSURE) {
            PressureDemand split = PressureDemand.of(demand);
            return split.spray() > 0
                    ? Component.translatable(KEY + "pressure_spray", Math.round(split.spray() * 100))
                    : Component.translatable(KEY + "pressure_heaters", Math.round(split.heaters() * 100));
        }
        if (function == ModuleFunction.AUTO_FUSION_POWER) {
            FusionPowerDemand split = FusionPowerDemand.of(demand, Math.max(0.0, heatingFloor));
            return Component.translatable(KEY + "fusion_demand", Math.round(split.fuel() * 100), Math.round(split.heating() * 100));
        }
        return Component.translatable(KEY + "controller_demand", Math.round(demand * 100));
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

    public String linkCode() {
        return linkCode;
    }

    public ControlModule withFunction(ModuleFunction function) {
        if (type.allows(function)) {
            if (this.function != function) {
                pid.reset();
                demand = -1.0f;
                heatingFloor = -1.0;
                if (function == ModuleFunction.AUTO_PRESSURE) {
                    // Start from the pressure the pressurizer holds on its own.
                    this.setting = Math.round((float) (PwrConfig.pressureSetpoint / PRESSURE_SCALE * 100.0));
                } else if (function == ModuleFunction.AUTO_FUSION_POWER) {
                    this.setting = FUSION_POWER_DEFAULT_SETTING;
                } else if (function == ModuleFunction.AUTO_TEMPERATURE) {
                    // Rated power runs the fuel near 900 C; 2000 C (the old half scale) is past the cladding failure.
                    this.setting = TEMPERATURE_DEFAULT_SETTING;
                }
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
            heatingFloor = -1.0;
        }
        return this;
    }

    public ControlModule withOutput(OutputMode output) {
        this.output = output;
        return this;
    }

    /** Keeps only the letters and digits an Advanced Redstone Link code takes. */
    public ControlModule withLinkCode(String code) {
        this.linkCode = LinkCode.sanitize(code);
        return this;
    }

    public ControlModule withFrequencies(ItemStack first, ItemStack last) {
        this.frequencyFirst = first.isEmpty() ? ItemStack.EMPTY : first.copyWithCount(1);
        this.frequencyLast = last.isEmpty() ? ItemStack.EMPTY : last.copyWithCount(1);
        return this;
    }

    /**
     * The largest {@link #setting()} the function takes: a bar graph's alarm level is 0-100 %, a trend
     * recorder's speed one of {@link TrendRing#SECONDS}.
     */
    public int maxSetting() {
        return switch (function) {
            case SIGNAL_LEVEL -> 15;
            case LAMP_ALARM, AUTO_POWER, AUTO_TEMPERATURE, AUTO_PRESSURE, AUTO_FUSION_POWER -> 100;
            case READING -> type == ModuleType.BAR_GRAPH ? 100 : type == ModuleType.TREND_RECORDER ? TrendRing.SECONDS.length - 1 : 0;
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

    /** A reading's value in its units (see {@link ReadingValue}), or NaN when there is none. */
    public float value() {
        return value;
    }

    /** The redstone level the module puts out, 0-15. */
    public int signal() {
        return signal;
    }

    public boolean pulsing() {
        return pulse > 0;
    }

    /** Whether a guarded button's cover is up, so the next click presses the button. */
    public boolean coverOpen() {
        return coverTicks > 0;
    }

    /**
     * What a screen module draws: a trend recorder's samples oldest first (see {@link TrendRing}), a core
     * map ({@link CoreMap}) or a plasma section ({@link PlasmaSection}); empty while there is none.
     */
    public byte[] display() {
        return display;
    }

    /** What the last click did, for the player who clicked, once; null when it has nothing to say. */
    @Nullable
    public Component takeFeedback() {
        Component said = feedback;
        feedback = null;
        return said;
    }

    /** Counts a pulse down; true when it just ended. */
    boolean tickPulse() {
        return pulse > 0 && --pulse == 0;
    }

    /** Counts a guarded button's cover down; true when it just fell shut. */
    boolean tickCover() {
        return coverTicks > 0 && --coverTicks == 0;
    }

    /**
     * Works out what the module shows for a console linked to {@code target}, sampled as
     * {@code sample}. Returns true when anything a client or a redstone listener sees changed.
     */
    public boolean evaluate(LinkedTarget.Resolved target, MonitorReading sample) {
        return evaluate(target, sample, Long.MIN_VALUE, true);
    }

    /**
     * As {@link #evaluate(LinkedTarget.Resolved, MonitorReading)}, at game time {@code gameTime} (when a
     * trend recorder takes its samples; {@link Long#MIN_VALUE} takes none). {@code picture} is false for
     * a module whose picture another draws (a merged screen's other three), which then keeps none.
     */
    public boolean evaluate(LinkedTarget.Resolved target, MonitorReading sample, long gameTime, boolean picture) {
        TargetKind kind = target.kind();
        boolean nowActive = isActiveFor(kind, target);
        float nowShown = nowActive ? computeShown(target, sample) : 0.0f;
        int nowSignal = nowActive ? computeSignal(nowShown) : 0;
        float nowValue = nowActive && function == ModuleFunction.READING ? (float) ReadingValue.value(reading, sample) : Float.NaN;
        if (trend != null && gameTime != Long.MIN_VALUE && gameTime >= nextTrendSample) {
            trend.push(nowActive ? nowShown : Float.NaN);
            nextTrendSample = gameTime + TrendRing.secondsFor(setting) * 20L;
        }
        byte[] nowDisplay = picture ? computeDisplay(target, sample, nowActive) : NO_DISPLAY;
        boolean changed = nowActive != active || Math.abs(nowShown - shown) > 1.0e-3f || nowSignal != signal
                || valueChanged(value, nowValue) || !java.util.Arrays.equals(display, nowDisplay);
        active = nowActive;
        shown = nowShown;
        signal = nowSignal;
        value = nowValue;
        display = nowDisplay;
        return changed;
    }

    private byte[] computeDisplay(LinkedTarget.Resolved target, MonitorReading sample, boolean nowActive) {
        if (trend != null) {
            return trend.ordered();
        }
        if (!nowActive) {
            return NO_DISPLAY;
        }
        return switch (function) {
            case CORE_VIEW -> target instanceof LinkedTarget.Reactor linked ? ModuleDisplays.coreMap(linked.reactor()) : NO_DISPLAY;
            case PLASMA_VIEW -> ModuleDisplays.plasma(sample);
            default -> NO_DISPLAY;
        };
    }

    /** Whether an LED display would show the change: one part in ten thousand is past its last digit. */
    private static boolean valueChanged(float before, float after) {
        if (Float.isNaN(before) || Float.isNaN(after)) {
            return Float.isNaN(before) != Float.isNaN(after);
        }
        return Math.abs(after - before) > Math.max(1.0e-4f, 1.0e-4f * Math.abs(after));
    }

    private boolean isActiveFor(TargetKind kind, LinkedTarget.Resolved target) {
        return switch (function) {
            case LAMP_RUNNING -> kind == TargetKind.REACTOR || kind == TargetKind.TURBINE || kind == TargetKind.COOLING_TOWER
                    || kind == TargetKind.TOKAMAK;
            case LAMP_ALARM -> metric.watches(kind);
            case READING -> reading.kind() == kind;
            default -> function.worksOn(kind) && (!function.needsPressurizer() || pressurizerOf(target) != null);
        };
    }

    /** The primary loop of a linked reactor with a pressurizer on its lid, or null. */
    @Nullable
    private static PrimaryLoop pressurizerOf(LinkedTarget.Resolved target) {
        return target instanceof LinkedTarget.Reactor linked && linked.reactor().hasPressurizer()
                ? linked.reactor().getPrimaryLoop() : null;
    }

    private float computeShown(LinkedTarget.Resolved target, MonitorReading sample) {
        ReactorBlockEntity reactor = target instanceof LinkedTarget.Reactor linked ? linked.reactor() : null;
        FusionReactorBlockEntity tokamak = target instanceof LinkedTarget.Tokamak linked ? linked.tokamak() : null;
        return switch (function) {
            case SCRAM, LAMP_SCRAM -> reactor != null && reactor.isScramRequested() ? 1 : 0;
            case PULSE -> pulse > 0 ? 1 : 0;
            case AUTO_SCRAM -> reactor != null && reactor.getAutoScram().isEnabled() ? 1 : 0;
            case TOKAMAK_FUEL -> tokamak != null && tokamak.isFuelEnabled() ? 1 : 0;
            // A button shows its press; a lever stays where it was put.
            case TOKAMAK_START, TOKAMAK_SHUTDOWN -> (type.isButton() ? pulse > 0 : on) ? 1 : 0;
            // Read live, as the fuel lever is: the console's sample may predate the click.
            case TOKAMAK_HEATING -> tokamak != null && tokamak.readings().heatingEnabled() ? 1 : 0;
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
            case AUTO_POWER, AUTO_TEMPERATURE, AUTO_PRESSURE, AUTO_FUSION_POWER -> setting / 100.0f;
            case PZR_HEATERS, PZR_SPRAY -> on ? 1 : 0;
            case LAMP_RELIEF_OPEN -> reactor != null && reactor.getPrimaryLoop().reliefOpen() ? 1 : 0;
            case CORE_VIEW -> reactor != null && reactor.isFormed() ? 1 : 0;
            case PLASMA_VIEW -> sample.kind() == TargetKind.TOKAMAK && sample.flag(MonitorReading.K_FORMED) ? 1 : 0;
            case LAMP_COLD_SHUTDOWN -> reactor != null && reactor.getShutdownState() == ShutdownState.COLD_SHUTDOWN ? 1 : 0;
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
            case AUTO_POWER, AUTO_TEMPERATURE, AUTO_PRESSURE, AUTO_FUSION_POWER ->
                    engaged && demand >= 0 ? Mth.clamp(Math.round(demand * 15), 0, 15) : 0;
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
        if (type == ModuleType.GUARDED_BUTTON) {
            // The cover first: lifted by a click, let down by a sneaking one; only under it is the button.
            if (back || coverTicks == 0) {
                coverTicks = back ? 0 : COVER_TICKS;
                feedback = Component.translatable(KEY + (back ? "cover.closed" : "cover.open"));
                return true;
            }
            coverTicks = COVER_TICKS;
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
            case TOKAMAK_START, TOKAMAK_SHUTDOWN -> pressStartStop(tokamak);
            case TOKAMAK_HEATING -> {
                TokamakReadings readings = tokamak.readings();
                tokamak.setHeating(!readings.heatingEnabled(), readings.heatingLevel());
            }
            case TOGGLE, PZR_HEATERS, PZR_SPRAY -> on = !on;
            case ROD_TARGET -> reactor.adjustRodTarget(back ? -ROD_STEP : ROD_STEP);
            case SCRAM_THRESHOLD -> reactor.adjustAutoScramThreshold(back ? -THRESHOLD_STEP : THRESHOLD_STEP);
            case SIGNAL_LEVEL -> setting = Mth.clamp(setting + (back ? -1 : 1), 0, 15);
            case AUTO_POWER, AUTO_TEMPERATURE, AUTO_PRESSURE, AUTO_FUSION_POWER ->
                    setting = Mth.clamp(setting + (back ? -SETPOINT_STEP : SETPOINT_STEP), 0, 100);
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * A start or shutdown button, or a lever thrown up: asks the tokamak, and tells the player what it
     * answered. A lever thrown down does nothing; a lever whose start is refused springs back down.
     */
    private void pressStartStop(TokamakControl tokamak) {
        if (type.isButton()) {
            pulse = PULSE_TICKS;
        } else {
            on = !on;
            if (!on) {
                return;
            }
        }
        if (function == ModuleFunction.TOKAMAK_SHUTDOWN) {
            boolean wasRunning = tokamak.phase() != TokamakPhase.OFFLINE;
            tokamak.requestShutdown();
            feedback = Component.translatable(KEY + (wasRunning ? "tokamak.shutdown" : "tokamak.already_offline"));
            return;
        }
        TripCause refused = tokamak.requestStart();
        if (refused == TripCause.NONE) {
            feedback = Component.translatable(KEY + "tokamak.start");
        } else {
            feedback = Component.translatable(KEY + "tokamak.refused", Component.translatable(refused.langKey()));
            on = false;
        }
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
        if (function == ModuleFunction.AUTO_PRESSURE) {
            return controlPressure(target, sample, dt, tuning);
        }
        if (function == ModuleFunction.AUTO_FUSION_POWER) {
            return controlFusionPower(target, sample, dt, tuning);
        }
        ReactorBlockEntity reactor = target instanceof LinkedTarget.Reactor linked ? linked.reactor() : null;
        if (!engaged || reactor == null || !reactor.isFormed() || reactor.isScramRequested() || sample.kind() != TargetKind.REACTOR) {
            pid.reset();
            boolean changed = demand >= 0;
            demand = -1.0f;
            return changed;
        }
        double measurement = function == ModuleFunction.AUTO_POWER
                ? sample.value(MonitorReading.R_POWER) / Math.max(1.0f, sample.value(MonitorReading.R_RATED_POWER))
                : sample.value(MonitorReading.R_TEMPERATURE) / TEMPERATURE_SCALE;
        float current = reactor.getRodTarget() >= 0 ? reactor.getRodTarget() : reactor.getControlRodInsertionRatio();
        int steps = reactor.getStructure().map(structure -> structure.innerHeight()).orElse(0);
        // A core with more rod blocks than it needs has rods worth more than one bank: each step of insertion
        // moves more reactivity, so the gains shrink by the same factor to keep the loop as stable as before.
        PidController.Tuning rodTuning = tuning.scaled(1.0 / Math.max(1.0, reactor.getPhysicsSimulator().getMaxAbsorption()));
        float next = (float) pid.step(setting / 100.0, measurement, dt, current, steps, rodTuning);
        reactor.setRodTarget(next);
        boolean changed = Math.abs(next - demand) > 1.0e-4f;
        demand = next;
        return changed;
    }

    /**
     * One step of a pressure controller: reads the primary pressure and sets the pressurizer's heaters
     * and spray through a {@link PressureDemand}. It lets go in standby, without a pressurizer or while
     * the loop is not running, and the pressurizer's own automatic control takes over again.
     */
    private boolean controlPressure(LinkedTarget.Resolved target, MonitorReading sample, double dt, PidController.Tuning tuning) {
        PrimaryLoop loop = pressurizerOf(target);
        if (!engaged || loop == null || sample.kind() != TargetKind.REACTOR || !sample.flag(MonitorReading.R_PWR)) {
            pid.reset();
            boolean changed = demand >= 0;
            demand = -1.0f;
            return changed;
        }
        double measurement = sample.value(MonitorReading.R_PRESSURE) / PRESSURE_SCALE;
        float next = (float) pid.step(setting / 100.0, measurement, dt, PressureDemand.NEUTRAL, 0, tuning);
        boolean changed = Math.abs(next - demand) > 1.0e-4f;
        demand = next;
        holdOrders(target);
        return changed;
    }

    /**
     * One step of a fusion power controller: sets the tokamak's fuelling and, once that is saturated,
     * raises its heating above the operator's level (see {@link FusionPowerDemand}). It acts on a
     * burning plasma only: the startup sequence and a shutdown are the operator's, and in standby or
     * without a burn it lets go, leaving the fuelling and heating where they are.
     */
    private boolean controlFusionPower(LinkedTarget.Resolved target, MonitorReading sample, double dt, PidController.Tuning tuning) {
        TokamakControl tokamak = target instanceof LinkedTarget.Tokamak linked ? linked.control() : null;
        if (!engaged || tokamak == null || sample.kind() != TargetKind.TOKAMAK || !sample.flag(MonitorReading.K_FORMED)
                || TokamakScales.phase(sample.value(MonitorReading.K_PHASE)) != TokamakPhase.BURN) {
            pid.reset();
            heatingFloor = -1.0;
            boolean changed = demand >= 0;
            demand = -1.0f;
            return changed;
        }
        double maxFuel = tokamak.maxFuelRate();
        double fuel = maxFuel > 0 ? sample.value(MonitorReading.K_FUEL_RATE) / maxFuel : 0.0;
        double heating = sample.flag(MonitorReading.K_HEATING_ON) ? sample.value(MonitorReading.K_HEATING_LEVEL) : 0.0;
        if (!pid.initialized() || heatingFloor < 0) {
            // Take over from where the operator left it: that heating stays the least the plasma gets.
            heatingFloor = heating;
        }
        double current = FusionPowerDemand.outputFor(fuel, heating, heatingFloor);
        FusionPowerDemand next = FusionPowerDemand.step(pid, setting / 100.0, sample.value(MonitorReading.K_POWER) / FUSION_POWER_SCALE,
                dt, current, heatingFloor, tuning);
        tokamak.setFuelRate(next.fuel() * maxFuel);
        if (Math.abs(next.heating() - heating) > 1.0e-4) {
            tokamak.setHeating(next.heating() > 0, next.heating());
        }
        float output = (float) pid.output();
        boolean changed = Math.abs(output - demand) > 1.0e-4f;
        demand = output;
        return changed;
    }

    /**
     * Repeats the orders a pressurizer forgets unless a desk keeps giving them: a pressurizer lever
     * that is on, and a pressure controller's demand. The desk calls this every time it updates its
     * modules, so a desk that is broken or unloaded lets go within {@link PrimaryLoop#DESK_HOLD_TICKS}.
     */
    public void holdOrders(LinkedTarget.Resolved target) {
        PrimaryLoop loop = function.needsPressurizer() ? pressurizerOf(target) : null;
        if (loop == null) {
            return;
        }
        switch (function) {
            case PZR_HEATERS -> {
                if (on) {
                    loop.holdBackupHeaters();
                }
            }
            case PZR_SPRAY -> {
                if (on) {
                    loop.holdSpray();
                }
            }
            case AUTO_PRESSURE -> {
                if (engaged && demand >= 0) {
                    PressureDemand split = PressureDemand.of(demand);
                    loop.holdPressureDemand(split.heaters(), split.spray());
                }
            }
            default -> {
            }
        }
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
        tag.putString("LinkCode", linkCode);
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
        if (function == ModuleFunction.READING) {
            tag.putFloat("Value", value);
        }
        if (coverTicks > 0) {
            tag.putInt("Cover", coverTicks);
        }
        if (trend != null) {
            tag.putByteArray("Trend", trend.ordered());
            tag.putLong("TrendNext", nextTrendSample);
        }
        if (display.length > 0 && trend == null) {
            tag.putByteArray("Display", display);
        }
        if (isController()) {
            CompoundTag loop = new CompoundTag();
            loop.putBoolean("Initialized", pid.initialized());
            loop.putDouble("Integral", pid.integral());
            loop.putDouble("Output", pid.output());
            loop.putDouble("Previous", pid.previousMeasurement());
            loop.putDouble("HeatingFloor", heatingFloor);
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
        module.value = tag.contains("Value") ? tag.getFloat("Value") : Float.NaN;
        module.coverTicks = Mth.clamp(tag.getInt("Cover"), 0, COVER_TICKS);
        if (module.trend != null) {
            module.trend.load(tag.getByteArray("Trend"));
            module.nextTrendSample = tag.getLong("TrendNext");
            module.display = module.trend.ordered();
        } else {
            module.display = tag.getByteArray("Display");
        }
        if (tag.contains("Pid")) {
            CompoundTag loop = tag.getCompound("Pid");
            module.pid.load(loop.getBoolean("Initialized"), loop.getDouble("Integral"), loop.getDouble("Output"),
                    loop.getDouble("Previous"));
            module.demand = Mth.clamp(tag.getFloat("Demand"), -1, 1);
            module.heatingFloor = loop.contains("HeatingFloor") ? Math.min(1.0, loop.getDouble("HeatingFloor")) : -1.0;
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
        withLinkCode(tag.getString("LinkCode"));
        withEngaged(!tag.contains("Engaged") || tag.getBoolean("Engaged"));
        withFrequencies(ItemStack.parseOptional(registries, tag.getCompound("FrequencyFirst")),
                ItemStack.parseOptional(registries, tag.getCompound("FrequencyLast")));
    }
}
