package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.CoreMap;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.PlasmaSection;
import org.papiricoh.create_nuclearindustry.controlroom.desk.TrendRing;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * What the control room of the Ponder series (tools/structgen/ponder_control_room.py) holds and
 * shows: every module in every desk, table and wall panel, the readings on its screens, its
 * annunciator windows and its charts. Ponder runs no machines, so the scenes write all of it
 * straight into the block entities as they would arrive synced from a running plant: a fission
 * reactor at 880 of its 1000 MW, a burning tokamak and a turbine near full load.
 *
 * <p>No client classes: the GameTest that checks the schematic reads the same layout.
 */
public final class ControlRoomLayout {
    /** A monitor screen: its origin monitor (bottom, leftmost as the viewer sees it) and its size. */
    public record Screen(BlockPos origin, int width, int height) {}

    /** The big reactor screen in the middle of the back wall, and the tokamak and turbine screens on the east wall. */
    public static final Screen REACTOR_SCREEN = new Screen(new BlockPos(11, 2, 17), 6, 3);
    public static final Screen TOKAMAK_SCREEN = new Screen(new BlockPos(17, 2, 8), 3, 2);
    public static final Screen TURBINE_SCREEN = new Screen(new BlockPos(17, 2, 13), 3, 2);
    public static final List<Screen> SCREENS = List.of(REACTOR_SCREEN, TOKAMAK_SCREEN, TURBINE_SCREEN);

    // Positions the scenes point at, in schematic coordinates.
    public static final BlockPos SCRAM_DESK = new BlockPos(9, 1, 13);
    public static final BlockPos CORE_MAP_DESK = new BlockPos(8, 1, 13);
    public static final BlockPos CONTROLLER_DESK = new BlockPos(7, 1, 13);
    public static final BlockPos SCREEN_DESK = new BlockPos(6, 1, 13);
    public static final BlockPos LED_DESK = new BlockPos(10, 1, 13);
    public static final BlockPos SWITCH_DESK = new BlockPos(11, 1, 13);
    public static final BlockPos BAR_DESK = new BlockPos(12, 1, 13);
    public static final BlockPos PRESSURIZER_DESK = new BlockPos(5, 1, 13);
    public static final BlockPos WEST_CORNER = new BlockPos(4, 1, 13);
    public static final BlockPos EAST_CORNER = new BlockPos(14, 1, 13);
    public static final BlockPos WEST_ARM = new BlockPos(4, 1, 12);
    public static final BlockPos WEST_ARM_END = new BlockPos(4, 1, 11);
    public static final BlockPos EAST_ARM = new BlockPos(14, 1, 12);
    public static final BlockPos EAST_ARM_END = new BlockPos(14, 1, 11);
    public static final BlockPos SUPERVISOR_LEDS = new BlockPos(9, 1, 6);
    public static final BlockPos SUPERVISOR_SWITCHES = new BlockPos(10, 1, 6);
    public static final BlockPos PLASMA_PANEL = new BlockPos(17, 3, 11);
    public static final BlockPos TOKAMAK_CONTROLS = new BlockPos(17, 2, 12);
    public static final BlockPos BACK_CORE_MAP = new BlockPos(13, 3, 17);
    public static final BlockPos BACK_TRENDS = new BlockPos(4, 3, 17);
    public static final BlockPos BACK_BARS = new BlockPos(1, 3, 17);
    public static final BlockPos BACK_LEDS = new BlockPos(9, 1, 17);
    public static final BlockPos ANNUNCIATOR = new BlockPos(8, 5, 17);
    public static final BlockPos CHART_RECORDER = new BlockPos(2, 1, 17);
    public static final BlockPos ALARM = new BlockPos(6, 6, 17);
    public static final BlockPos ALARM_EAST = new BlockPos(11, 6, 17);

    /** The machines the room's devices are linked to; positions only, for the cards and windows. */
    public static final GlobalPos REACTOR = GlobalPos.of(Level.OVERWORLD, new BlockPos(9, 1, -40));
    public static final GlobalPos TOKAMAK = GlobalPos.of(Level.OVERWORLD, new BlockPos(60, 1, 9));
    public static final GlobalPos TURBINE = GlobalPos.of(Level.OVERWORLD, new BlockPos(40, 1, -20));

    private static final String STATUS = "create_nuclearindustry.control_room.status.";

    private ControlRoomLayout() {}

    // ------------------------------------------------------------------ modules

    /**
     * A module as the room shows it: its setup, and what a running plant would have synced to it
     * (the needle, lamp or lever position as {@code shown}, an LED's value, a screen's picture).
     */
    public record Face(ControlModule module, float shown, float value, byte[] display, boolean coverOpen, float demand) {
        public ModuleType type() {
            return module.type();
        }

        public Face withShown(float newShown) {
            return new Face(module, newShown, value, display, coverOpen, demand);
        }

        public Face withCover(boolean open) {
            return new Face(module, shown, value, display, open, demand);
        }

        CompoundTag write(HolderLookup.Provider registries) {
            CompoundTag tag = module.write(registries);
            tag.putBoolean("Active", true);
            tag.putFloat("Shown", shown);
            tag.putInt("Signal", Math.round(shown * 15));
            if (module.function() == ModuleFunction.READING) {
                tag.putFloat("Value", value);
            }
            if (coverOpen) {
                tag.putInt("Cover", ControlModule.COVER_TICKS);
            }
            if (type() == ModuleType.TREND_RECORDER) {
                tag.putByteArray("Trend", display);
            } else if (display.length > 0) {
                tag.putByteArray("Display", display);
            }
            if (module.isController()) {
                tag.putFloat("Demand", demand);
            }
            return tag;
        }
    }

    /** A desk, table or wall panel and the module in each of its slots (null for an empty slot). */
    public record Fit(BlockPos pos, Face[] faces) {
        /** The block entity's module list, as its NBT holds it. */
        public ListTag write(HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (int slot = 0; slot < faces.length && slot < ControlDeskBlockEntity.SLOTS; slot++) {
                if (faces[slot] != null) {
                    CompoundTag entry = faces[slot].write(registries);
                    entry.putInt("Slot", slot);
                    list.add(entry);
                }
            }
            return list;
        }
    }

    private static final byte[] NONE = new byte[0];

    private static Face face(ModuleType type, ModuleFunction function, float shown) {
        return new Face(new ControlModule(type).withFunction(function), shown, Float.NaN, NONE, false, 0);
    }

    static Face lamp(ModuleFunction function, boolean lit) {
        return face(ModuleType.LAMP, function, lit ? 1 : 0);
    }

    static Face lever(ModuleFunction function, boolean on) {
        return face(ModuleType.LEVER, function, on ? 1 : 0);
    }

    static Face key(boolean on) {
        return face(ModuleType.KEY_SWITCH, ModuleFunction.TOGGLE, on ? 1 : 0);
    }

    static Face button(ModuleFunction function) {
        return face(ModuleType.PUSH_BUTTON, function, 0);
    }

    static Face guarded(ModuleFunction function) {
        return face(ModuleType.GUARDED_BUTTON, function, 0);
    }

    static Face dial(ModuleFunction function, float position) {
        return face(ModuleType.DIAL, function, position);
    }

    static Face controller(ModuleFunction function, int setpoint, float demand) {
        ControlModule module = new ControlModule(ModuleType.CONTROLLER).withFunction(function).withSetting(setpoint);
        return new Face(module, setpoint / 100.0f, Float.NaN, NONE, false, demand);
    }

    private static Face reading(ModuleType type, GaugeReading reading, String label, float fraction, float value, byte[] display) {
        ControlModule module = new ControlModule(type).withFunction(ModuleFunction.READING).withReading(reading).withLabel(label);
        return new Face(module, fraction, value, display, false, 0);
    }

    static Face gauge(GaugeReading reading, float fraction) {
        return reading(ModuleType.GAUGE, reading, "", fraction, Float.NaN, NONE);
    }

    static Face led(GaugeReading reading, String label, float value) {
        return reading(ModuleType.LED_DISPLAY, reading, label, 0.5f, value, NONE);
    }

    static Face bar(GaugeReading reading, float fraction) {
        return reading(ModuleType.BAR_GRAPH, reading, "", fraction, Float.NaN, NONE);
    }

    /** A trend recorder full of a wavering line around {@code level}, rising by {@code slope} over its window. */
    static Face trend(GaugeReading reading, float level, float slope, int seed) {
        byte[] samples = new byte[TrendRing.CAPACITY];
        for (int i = 0; i < samples.length; i++) {
            float t = i / (float) (samples.length - 1);
            float wave = 0.03f * (float) Math.sin(i * 0.55 + seed) + 0.015f * (float) Math.sin(i * 1.7 + 2 * seed);
            samples[i] = TrendRing.quantize(Math.max(0, Math.min(1, level + slope * (t - 1) + wave)));
        }
        return reading(ModuleType.TREND_RECORDER, reading, "", level, Float.NaN, samples);
    }

    /** One of four core maps making one big map of the room's 7x7 reactor core. */
    static Face coreMap() {
        return new Face(new ControlModule(ModuleType.CORE_MAP).withFunction(ModuleFunction.CORE_VIEW), 0, Float.NaN, core(), false, 0);
    }

    /** One of four plasma sections making one big screen of the tokamak burning. */
    static Face plasma() {
        byte[] picture = PlasmaSection.encode(TokamakPhase.BURN, 0.78f, 0.7f, 0.12f, 0.95f, 0.92f, false);
        return new Face(new ControlModule(ModuleType.PLASMA_SECTION).withFunction(ModuleFunction.PLASMA_VIEW), 0, Float.NaN,
                picture, false, 0);
    }

    /**
     * A 7x7 core: a reflector ring round a 5x5 of fuel with graphite between, and control channels on
     * a cross, their rods a third in (the middle one further), the fuel burnt more towards the centre.
     */
    private static byte[] core() {
        byte[] data = CoreMap.blank(7);
        for (int row = 0; row < 7; row++) {
            for (int column = 0; column < 7; column++) {
                int dx = Math.abs(column - 3);
                int dz = Math.abs(row - 3);
                if (dx == 3 || dz == 3) {
                    CoreMap.set(data, column, row, (dx == 3 && dz == 3) ? CoreMap.Kind.EMPTY : CoreMap.Kind.REFLECTOR, 0);
                } else if ((dx == 0 || dz == 0) && (dx + dz) % 2 == 0) {
                    CoreMap.set(data, column, row, CoreMap.Kind.CONTROL, dx + dz == 0 ? 0.6f : 0.32f);
                } else if ((dx + dz) % 2 == 1 && dx != 0 && dz != 0) {
                    CoreMap.set(data, column, row, CoreMap.Kind.MODERATOR, 0);
                } else {
                    CoreMap.set(data, column, row, CoreMap.Kind.FUEL, 0.55f - 0.12f * Math.max(dx, dz));
                }
            }
        }
        return data;
    }

    /** Every module of the room, block by block. */
    public static List<Fit> fits() {
        List<Fit> fits = new ArrayList<>();
        operatorsConsole(fits);
        supervisorTables(fits);
        backWall(fits);
        eastWall(fits);
        return fits;
    }

    private static void fit(List<Fit> fits, BlockPos pos, Face... faces) {
        fits.add(new Fit(pos, faces));
    }

    private static void fit(List<Fit> fits, int x, int y, int z, Face... faces) {
        fit(fits, new BlockPos(x, y, z), faces);
    }

    /**
     * The desks, slot by slot. A desk's slots run near left, near right, far left, far right as the
     * operator sees them; facing north the operator's left is east.
     */
    private static void operatorsConsole(List<Fit> fits) {
        fit(fits, PRESSURIZER_DESK, gauge(GaugeReading.REACTOR_PRESSURE, 0.62f), gauge(GaugeReading.PRESSURIZER_LEVEL, 0.55f),
                lever(ModuleFunction.PZR_HEATERS, true), lever(ModuleFunction.PZR_SPRAY, false));
        fit(fits, SCREEN_DESK, trend(GaugeReading.REACTOR_POWER, 0.88f, 0.35f, 1), trend(GaugeReading.REACTOR_TEMPERATURE, 0.6f, 0.2f, 2),
                bar(GaugeReading.PRIMARY_FLOW, 0.94f), bar(GaugeReading.REACTOR_COOLANT, 0.8f));
        fit(fits, CONTROLLER_DESK, controller(ModuleFunction.AUTO_POWER, 88, 0.35f), lamp(ModuleFunction.LAMP_RUNNING, true),
                dial(ModuleFunction.ROD_TARGET, 0.35f), gauge(GaugeReading.REACTOR_RODS, 0.35f));
        fit(fits, CORE_MAP_DESK, coreMap(), coreMap(), coreMap(), coreMap());
        fit(fits, SCRAM_DESK, guarded(ModuleFunction.SCRAM), lamp(ModuleFunction.LAMP_SCRAM, false),
                button(ModuleFunction.SCRAM), lever(ModuleFunction.AUTO_SCRAM, true));
        fit(fits, LED_DESK, led(GaugeReading.REACTOR_POWER, "POWER", 880), led(GaugeReading.REACTOR_POWER, "POWER", 880),
                led(GaugeReading.REACTOR_TEMPERATURE, "CORE TEMP", 870), led(GaugeReading.REACTOR_TEMPERATURE, "CORE TEMP", 870));
        fit(fits, SWITCH_DESK, dial(ModuleFunction.SCRAM_THRESHOLD, 0.35f), key(true),
                lamp(ModuleFunction.LAMP_ALARM, false), gauge(GaugeReading.REACTOR_TEMPERATURE, 0.22f));
        fit(fits, BAR_DESK, bar(GaugeReading.REACTOR_TEMPERATURE, 0.22f), bar(GaugeReading.REACTOR_PRESSURE, 0.62f),
                bar(GaugeReading.VESSEL_LEVEL, 1.0f), bar(GaugeReading.PRESSURIZER_LEVEL, 0.55f));
        fit(fits, 13, 1, 13, button(ModuleFunction.PULSE), lamp(ModuleFunction.LAMP_RELIEF_OPEN, false),
                gauge(GaugeReading.REACTOR_HOT_LEG, 0.87f), gauge(GaugeReading.REACTOR_COLD_LEG, 0.77f));
        // Corners hold their two modules in the back row.
        fit(fits, WEST_CORNER, null, null, lamp(ModuleFunction.LAMP_NO_SIGNAL, false), lamp(ModuleFunction.LAMP_RUNNING, true));
        fit(fits, EAST_CORNER, null, null, key(false), lamp(ModuleFunction.LAMP_ALARM, false));
        fit(fits, WEST_ARM, led(GaugeReading.REACTOR_SUBCOOLING, "SUBCOOL", 20.4f), led(GaugeReading.REACTOR_SUBCOOLING, "SUBCOOL", 20.4f),
                gauge(GaugeReading.PRIMARY_FLOW, 0.94f), gauge(GaugeReading.REACTOR_STEAM, 0.4f));
        fit(fits, WEST_ARM_END, bar(GaugeReading.REACTOR_HOT_LEG, 0.87f), bar(GaugeReading.REACTOR_COLD_LEG, 0.77f),
                lamp(ModuleFunction.LAMP_RUNNING, true), button(ModuleFunction.PULSE));
        fit(fits, EAST_ARM, led(GaugeReading.REACTOR_FUEL, "FUEL", 72), led(GaugeReading.REACTOR_FUEL, "FUEL", 72),
                gauge(GaugeReading.REACTOR_XENON, 0.24f), dial(ModuleFunction.SIGNAL_LEVEL, 0.6f));
        fit(fits, EAST_ARM_END, trend(GaugeReading.REACTOR_XENON, 0.24f, 0.1f, 3), gauge(GaugeReading.REACTOR_FUEL, 0.72f),
                key(true), lamp(ModuleFunction.LAMP_ALARM, false));
    }

    /** The shift supervisor's tables: an eight-digit overview in the middle, lamps and switches either side. */
    private static void supervisorTables(List<Fit> fits) {
        fit(fits, 7, 1, 6, lamp(ModuleFunction.LAMP_SCRAM, false), lamp(ModuleFunction.LAMP_ALARM, false),
                lamp(ModuleFunction.LAMP_RUNNING, true), lamp(ModuleFunction.LAMP_NO_SIGNAL, false));
        Face power = led(GaugeReading.REACTOR_POWER, "UNIT POWER", 880);
        Face temperature = led(GaugeReading.REACTOR_PRESSURE, "PRIMARY", 155.2f);
        fit(fits, 8, 1, 6, power, power, temperature, temperature);
        fit(fits, SUPERVISOR_LEDS, power, power, temperature, temperature);
        fit(fits, SUPERVISOR_SWITCHES, key(true), key(false), gauge(GaugeReading.REACTOR_POWER, 0.88f), gauge(GaugeReading.REACTOR_TEMPERATURE, 0.22f));
        fit(fits, 11, 1, 6, guarded(ModuleFunction.SCRAM), trend(GaugeReading.REACTOR_POWER, 0.88f, 0.25f, 4),
                lamp(ModuleFunction.LAMP_RELIEF_OPEN, false), gauge(GaugeReading.REACTOR_PRESSURE, 0.62f));
    }

    /**
     * The back wall, one console with the reactor: columns of bar graphs and gauges like the
     * vertical boards of a real plant, trend recorders, a big core map, and a row of LED displays
     * under the screen.
     */
    private static void backWall(List<Fit> fits) {
        GaugeReading[] loop = {GaugeReading.REACTOR_HOT_LEG, GaugeReading.REACTOR_COLD_LEG, GaugeReading.PRIMARY_FLOW,
                GaugeReading.REACTOR_PRESSURE, GaugeReading.PRESSURIZER_LEVEL, GaugeReading.REACTOR_SUBCOOLING};
        float[] loopLevels = {0.87f, 0.77f, 0.94f, 0.62f, 0.55f, 0.4f};
        for (int x = 1; x <= 16; x++) {
            for (int y = 1; y <= 4; y++) {
                if (x >= 6 && x <= 11 && y >= 2 || y == 1 && (x <= 2 || x >= 15)) {
                    continue;
                }
                int i = x * 7 + y * 3;
                BlockPos pos = new BlockPos(x, y, 17);
                if (pos.equals(BACK_CORE_MAP)) {
                    fit(fits, pos, coreMap(), coreMap(), coreMap(), coreMap());
                } else if (y == 1 && x >= 6 && x <= 11) {
                    backLeds(fits, x);
                } else if (x == 1 || x == 2 || x == 16 || x == 12 && y >= 2) {
                    // Columns of bar graphs at the ends of the board.
                    fit(fits, pos, bar(loop[i % 6], loopLevels[i % 6]), bar(loop[(i + 1) % 6], loopLevels[(i + 1) % 6]),
                            bar(loop[(i + 2) % 6], loopLevels[(i + 2) % 6]), bar(loop[(i + 3) % 6], loopLevels[(i + 3) % 6]));
                } else if ((x == 4 || x == 14) && y >= 2) {
                    fit(fits, pos, trend(loop[i % 6], loopLevels[i % 6], 0.15f, i), trend(loop[(i + 2) % 6], loopLevels[(i + 2) % 6], 0.1f, i + 1),
                            gauge(loop[(i + 1) % 6], loopLevels[(i + 1) % 6]), gauge(loop[(i + 3) % 6], loopLevels[(i + 3) % 6]));
                } else if (y == 1) {
                    fit(fits, pos, lamp(ModuleFunction.LAMP_RUNNING, i % 3 != 0), key(i % 2 == 0),
                            lever(ModuleFunction.TOGGLE, i % 4 == 1), button(ModuleFunction.PULSE));
                } else {
                    fit(fits, pos, gauge(loop[i % 6], loopLevels[i % 6]), gauge(loop[(i + 1) % 6], loopLevels[(i + 1) % 6]),
                            lamp(i % 5 == 0 ? ModuleFunction.LAMP_ALARM : ModuleFunction.LAMP_RUNNING, i % 5 != 0 && i % 2 == 0),
                            lamp(ModuleFunction.LAMP_RUNNING, i % 3 == 1));
                }
            }
        }
    }

    /**
     * Under the screen, three LED displays of four digits each, one per pair of panels. LED displays
     * in a row join up to eight, so the middle one sits in the other row, with lamps between.
     */
    private static void backLeds(List<Fit> fits, int x) {
        if (x == 8 || x == 9) {
            Face temperature = led(GaugeReading.REACTOR_TEMPERATURE, "CORE TEMP", 870);
            fit(fits, x, 1, 17, lamp(ModuleFunction.LAMP_RUNNING, true), lamp(ModuleFunction.LAMP_SCRAM, false), temperature, temperature);
            return;
        }
        Face top = x >= 10 ? led(GaugeReading.REACTOR_POWER, "POWER", 880) : led(GaugeReading.REACTOR_PRESSURE, "PRIMARY", 155.2f);
        fit(fits, x, 1, 17, top, top, lamp(ModuleFunction.LAMP_RUNNING, true), lamp(ModuleFunction.LAMP_SCRAM, false));
    }

    /** The east wall, one console with the tokamak: its plasma, its controls and its readings. */
    private static void eastWall(List<Fit> fits) {
        fit(fits, PLASMA_PANEL, plasma(), plasma(), plasma(), plasma());
        fit(fits, TOKAMAK_CONTROLS, button(ModuleFunction.TOKAMAK_START), guarded(ModuleFunction.TOKAMAK_SHUTDOWN),
                lever(ModuleFunction.TOKAMAK_FUEL, true), lever(ModuleFunction.TOKAMAK_HEATING, true));
        fit(fits, 17, 2, 11, bar(GaugeReading.CONFINEMENT, 0.92f), bar(GaugeReading.INSTABILITY, 0.12f),
                bar(GaugeReading.PLASMA_DENSITY, 0.7f), bar(GaugeReading.MAGNETIC_FIELD, 0.95f));
        fit(fits, 17, 3, 12, led(GaugeReading.FUSION_POWER, "FUSION MW", 610), led(GaugeReading.FUSION_POWER, "FUSION MW", 610),
                led(GaugeReading.FUSION_GAIN, "GAIN", 12.4f), led(GaugeReading.FUSION_GAIN, "GAIN", 12.4f));
        for (int z : new int[]{7, 16}) {
            fit(fits, 17, 3, z, trend(GaugeReading.PLASMA_TEMPERATURE, 0.78f, 0.3f, z), trend(GaugeReading.FUSION_POWER, 0.6f, 0.35f, z + 1),
                    gauge(GaugeReading.MAGNET_TEMPERATURE, 0.3f), gauge(GaugeReading.LIQUID_HELIUM, 0.8f));
            fit(fits, 17, 2, z, gauge(GaugeReading.PLASMA_DENSITY, 0.7f), gauge(GaugeReading.HEATING_POWER, 0.45f),
                    lamp(ModuleFunction.LAMP_RUNNING, true), lamp(ModuleFunction.LAMP_ALARM, z == 16));
        }
        for (int z = 7; z <= 16; z++) {
            fit(fits, 17, 1, z, gauge(z % 2 == 0 ? GaugeReading.TRITIUM : GaugeReading.FUEL_RATE, z % 2 == 0 ? 0.66f : 0.5f),
                    lamp(ModuleFunction.LAMP_RUNNING, z % 3 != 0), dial(ModuleFunction.SIGNAL_LEVEL, 0.2f + 0.07f * z), key(z % 2 == 1));
        }
    }

    // ------------------------------------------------------------------ screens, windows and charts

    /** The reactor as its screen shows it: 880 of 1000 MW, a pressurized water loop at 155 bar. */
    public static MonitorReading reactorReading() {
        float[] v = new float[MonitorReading.R_REACTIVITY_VOIDS + 1];
        v[MonitorReading.R_TEMPERATURE] = 870;
        v[MonitorReading.R_POWER] = 880;
        v[MonitorReading.R_NEUTRONS] = 0.88f;
        v[MonitorReading.R_REACTIVITY] = 0.0f;
        v[MonitorReading.R_XENON] = 0.12f;
        v[MonitorReading.R_DECAY_HEAT] = 55;
        v[MonitorReading.R_RODS] = 0.35f;
        v[MonitorReading.R_ROD_TARGET] = 0.35f;
        v[MonitorReading.R_COOLANT] = 0.8f;
        v[MonitorReading.R_STEAM] = 0.4f;
        v[MonitorReading.R_FUEL] = 0.72f;
        v[MonitorReading.R_FORMED] = 1;
        v[MonitorReading.R_STRESS] = 0.2f;
        v[MonitorReading.R_PRESSURE] = 155.2f;
        v[MonitorReading.R_T_HOT] = 325;
        v[MonitorReading.R_T_COLD] = 290;
        v[MonitorReading.R_SUBCOOLING] = 20.4f;
        v[MonitorReading.R_PZR_LEVEL] = 0.55f;
        v[MonitorReading.R_FLOW] = 240;
        v[MonitorReading.R_VESSEL_LEVEL] = 1;
        v[MonitorReading.R_CONT_PRESSURE] = 1.01f;
        v[MonitorReading.R_PWR] = 1;
        v[MonitorReading.R_RATED_POWER] = 1000;
        v[MonitorReading.R_NET_REACTIVITY] = 2;
        v[MonitorReading.R_COOLANT_TEMP] = 307;
        v[MonitorReading.R_PERIOD] = 0;
        v[MonitorReading.R_SHUTDOWN_MARGIN] = 4200;
        return new MonitorReading(TargetKind.REACTOR, STATUS + "running", v, 870 / 4000.0f, 0.88f);
    }

    public static MonitorReading tokamakReading() {
        float[] v = new float[MonitorReading.K_TRIPPED + 1];
        v[MonitorReading.K_TEMPERATURE] = 150;
        v[MonitorReading.K_POWER] = 610;
        v[MonitorReading.K_CONFINEMENT] = 0.92f;
        v[MonitorReading.K_INSTABILITY] = 12;
        v[MonitorReading.K_DEUTERIUM] = 0.7f;
        v[MonitorReading.K_TRITIUM] = 0.66f;
        v[MonitorReading.K_FORMED] = 1;
        v[MonitorReading.K_FUEL] = 1;
        v[MonitorReading.K_PHASE] = TokamakPhase.BURN.ordinal();
        v[MonitorReading.K_DENSITY] = 0.7f;
        v[MonitorReading.K_GAIN] = 12.4f;
        v[MonitorReading.K_FIELD] = 0.95f;
        v[MonitorReading.K_MAGNET_TEMPERATURE] = 4.5f;
        v[MonitorReading.K_VACUUM] = -4;
        v[MonitorReading.K_HEATING] = 30;
        v[MonitorReading.K_LIQUID_HELIUM] = 0.8f;
        v[MonitorReading.K_HEATING_ON] = 1;
        v[MonitorReading.K_HEATING_LEVEL] = 0.45f;
        return new MonitorReading(TargetKind.TOKAMAK, STATUS + "burning", v, 0.78f, 0.6f);
    }

    public static MonitorReading turbineReading() {
        float[] v = new float[MonitorReading.T_CAPACITY + 1];
        v[MonitorReading.T_SPEED] = 230;
        v[MonitorReading.T_USE] = 1;
        v[MonitorReading.T_LIMIT] = 1;
        v[MonitorReading.T_STEAM] = 0.65f;
        v[MonitorReading.T_FORMED] = 1;
        v[MonitorReading.T_CAPACITY] = 2048;
        return new MonitorReading(TargetKind.TURBINE, STATUS + "running", v, 0.78f, 0.9f);
    }

    /**
     * A screen's origin monitor: its size, its reading and two minutes of history climbing to it
     * (the first series to {@code a}, the second to {@code b}).
     */
    static void screen(CompoundTag nbt, Screen screen, GlobalPos target, MonitorReading reading) {
        target(nbt, target);
        nbt.putLong("Origin", screen.origin().asLong());
        nbt.putInt("Width", screen.width());
        nbt.putInt("Height", screen.height());
        nbt.put("Reading", reading.write());
        int samples = 120;
        int[] a = new int[samples];
        int[] b = new int[samples];
        for (int i = 0; i < samples; i++) {
            float t = i / (float) (samples - 1);
            float rise = 1 - (float) Math.exp(-4 * t);
            a[i] = Float.floatToIntBits(reading.seriesA() * (0.55f + 0.45f * rise) + 0.01f * (float) Math.sin(i * 0.4));
            b[i] = Float.floatToIntBits(reading.seriesB() * (0.3f + 0.7f * rise) + 0.015f * (float) Math.sin(i * 0.23));
        }
        nbt.putIntArray("HistoryA", a);
        nbt.putIntArray("HistoryB", b);
        nbt.putLong("HistorySeq", samples);
    }

    /** Any other monitor of a screen: only where its origin is. */
    static void screenMember(CompoundTag nbt, Screen screen) {
        nbt.putLong("Origin", screen.origin().asLong());
        nbt.putInt("Width", screen.width());
        nbt.putInt("Height", screen.height());
    }

    static CompoundTag target(CompoundTag tag, GlobalPos target) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().ifPresent(encoded -> tag.put("Target", encoded));
        return tag;
    }

    private static final String[][] REACTOR_WINDOWS = {
            {"TEMPERATURE", "CORE TEMP"}, {"POWER", "POWER HI"}, {"PRESSURE_HIGH", "PRESS HI"}, {"PRESSURE_LOW", "PRESS LO"},
            {"SUBCOOLING_LOW", "SUBCOOL LO"}, {"PZR_LEVEL", "PZR LEVEL"}, {"LOSS_OF_FLOW", "FLOW LO"}, {"SCRAM", "SCRAM"},
            {"XENON", "XENON"}, {"COOLANT_LOW", "COOLANT LO"}, {"RELIEF_OPEN", "RELIEF OPEN"}, {"CORE_DAMAGE", "CORE DAMAGE"}};
    private static final String[][] TOKAMAK_WINDOWS = {
            {"PLASMA_TEMPERATURE", "PLASMA TEMP"}, {"PLASMA_INSTABILITY", "INSTABILITY"}, {"VACUUM_POOR", "VACUUM"},
            {"MAGNET_TEMPERATURE", "MAGNET TEMP"}, {"FUSION_GAIN_LOW", "GAIN LO"}, {"PLASMA_DENSITY", "DENSITY"},
            {"TRITIUM_LOW", "TRITIUM LO"}, {"TOKAMAK_TRIP", "TRIP"}};

    /**
     * Six windows, the {@code index}th panel's share of the reactor's or the tokamak's alarms, all
     * dark but for {@code lit} windows (an acknowledged alarm, then one flashing, then one clearing).
     */
    static void annunciator(CompoundTag nbt, GlobalPos target, boolean tokamak, int index, int lit) {
        String[][] windows = tokamak ? TOKAMAK_WINDOWS : REACTOR_WINDOWS;
        ListTag list = new ListTag();
        int[] states = new int[6];
        for (int w = 0; w < 6; w++) {
            String[] window = windows[(index * 6 + w) % windows.length];
            CompoundTag tag = target(new CompoundTag(), target);
            tag.putString("Kind", tokamak ? "TOKAMAK" : "REACTOR");
            tag.putString("Metric", AlarmMetric.valueOf(window[0]).name());
            tag.putInt("Setting", 80);
            tag.putString("Label", window[1]);
            list.add(tag);
        }
        // Ordinals of AnnunciatorLogic.State (NORMAL, ALERT, ACKNOWLEDGED, RINGBACK); 0x100 marks a window that tripped.
        int[] litStates = {2 | 0x100, 1 | 0x100, 3};
        for (int i = 0; i < lit && i < litStates.length; i++) {
            states[(index * 5 + 2 * i) % 6] = litStates[i];
        }
        nbt.put("Windows", list);
        nbt.put("States", new IntArrayTag(states));
        nbt.putInt("FirstOut", lit > 0 ? (index * 5) % 6 : -1);
    }

    /** Three pens of the reactor: power and temperature settling, pressure steady, and paper in the tray. */
    static void chart(CompoundTag nbt, GlobalPos target, int seed) {
        target(nbt, target);
        nbt.putString("Kind", "REACTOR");
        ListTag pens = new ListTag();
        for (String pen : new String[]{"REACTOR_POWER", "REACTOR_TEMPERATURE", "REACTOR_PRESSURE"}) {
            pens.add(StringTag.valueOf(pen));
        }
        nbt.put("Pens", pens);
        nbt.putInt("Paper", ChartRecorderBlockEntity.MAX_PAPER / 2);
        int samples = ChartRecorderBlockEntity.VISIBLE;
        nbt.putLong("Drawn", samples);
        byte[][] traces = new byte[3][samples];
        for (int i = 0; i < samples; i++) {
            float t = i / (float) (samples - 1);
            traces[0][i] = ChartBuffer.quantize(Math.min(0.88f, 0.2f + t * 1.2f) + 0.02f * (float) Math.sin(i * 0.9 + seed));
            traces[1][i] = ChartBuffer.quantize(0.25f + 0.4f * (1 - (float) Math.exp(-3 * t)));
            traces[2][i] = ChartBuffer.quantize(0.62f + 0.03f * (float) Math.sin(i * 0.35 + seed));
        }
        for (int pen = 0; pen < 3; pen++) {
            nbt.putByteArray("Trace" + pen, traces[pen]);
        }
    }
}
