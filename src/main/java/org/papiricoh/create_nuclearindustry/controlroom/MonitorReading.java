package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReleasePath;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineDisplay;

import java.util.Arrays;

/**
 * One sample of what a monitor shows, taken on the server and synced to clients. {@code values}
 * holds the readings of the target kind at the indices below; {@code seriesA} and {@code seriesB}
 * are the two 0-1 values the history graph plots. {@code status} is a translation key when it starts
 * with the mod id, otherwise literal text.
 */
public record MonitorReading(TargetKind kind, String status, float[] values, float seriesA, float seriesB) {
    public static final MonitorReading NONE = new MonitorReading(TargetKind.NONE, LinkedTarget.NO_LINK, new float[0], 0, 0);

    // Reactor
    public static final int R_TEMPERATURE = 0, R_POWER = 1, R_NEUTRONS = 2, R_REACTIVITY = 3, R_XENON = 4, R_DECAY_HEAT = 5,
            R_RODS = 6, R_ROD_TARGET = 7, R_COOLANT = 8, R_STEAM = 9, R_FUEL = 10, R_SCRAM = 11, R_FORMED = 12, R_STRESS = 13;
    // Reactor, pressurized water loop and containment (see PlantReadings); all 0 for a reactor that boils directly
    public static final int R_PRESSURE = 14, R_T_HOT = 15, R_T_COLD = 16, R_SUBCOOLING = 17, R_PZR_LEVEL = 18, R_FLOW = 19,
            R_VESSEL_LEVEL = 20, R_CONT_PRESSURE = 21, R_H2 = 22, R_PWR = 23;
    // Reactor: the pressurizer's relief valve is open (1) or shut (0)
    public static final int R_RELIEF = 24;
    private static final int REACTOR_VALUES = 25;
    // Steam generator: water level 0-1, steam made mB/t, primary temperature drop °C, formed flag, heat moved MW
    public static final int S_LEVEL = 0, S_STEAM = 1, S_PRIMARY_DT = 2, S_FORMED = 3, S_HEAT = 4;
    // Containment: pressure bar, hydrogen fraction 0-1, sealed flag, vent open flag, airborne activity, free volume m³
    public static final int N_PRESSURE = 0, N_H2 = 1, N_SEALED = 2, N_VENT = 3, N_ACTIVITY = 4, N_VOLUME = 5;
    // Turbine
    public static final int T_SPEED = 0, T_USE = 1, T_LIMIT = 2, T_STEAM = 3, T_EXHAUST = 4, T_FORMED = 5, T_CAPACITY = 6;
    // Cooling tower
    public static final int C_USE = 0, C_CAPACITY = 1, C_WATER = 2, C_STEAM = 3, C_FANS = 4, C_FORMED = 5;
    // Area
    public static final int A_GROUND = 0, A_EXPOSURE = 1;
    // Tokamak
    public static final int K_TEMPERATURE = 0, K_POWER = 1, K_CONFINEMENT = 2, K_INSTABILITY = 3, K_DEUTERIUM = 4,
            K_TRITIUM = 5, K_FORMED = 6, K_FUEL = 7;

    /** Graph ceiling for area exposure, in rad/s. */
    public static final float AREA_GRAPH_MAX = 10.0f;
    /** Graph ceiling for tokamak power, in MW. */
    public static final float TOKAMAK_GRAPH_POWER = 1000.0f;
    /**
     * Full scale of gauges, pens and alarm thresholds for the pressurized water loop and containment:
     * pressure up to where the vessel fails (pwr.accidents.vesselFailurePressure), leg temperatures up to
     * the critical point (no liquid water is hotter), subcooling of a cold loop, the flow of two Mechanical
     * Pumps at full speed (128 mB/t each), and pressure up to the strongest containment wall (blast doors,
     * 10 bar) with hydrogen to 10%, past certain ignition at 8%.
     */
    public static final float PRESSURE_MAX = 250.0f, LEG_TEMPERATURE_MAX = 375.0f, SUBCOOLING_MAX = 50.0f,
            FLOW_MAX = 256.0f, CONTAINMENT_PRESSURE_MAX = 10.0f, HYDROGEN_MAX = 0.1f;
    /**
     * Full scale of steam generator gauges: steam made mB/t, heat moved MW, primary temperature drop °C.
     * One generator fed by two full-speed pumps makes about 250 mB/t of steam from about 280 MW; the
     * primary cools by the core's temperature rise (pwr.coreTemperatureRise, 35 °C by default).
     */
    public static final float SG_STEAM_MAX = 256.0f, SG_HEAT_MAX = 300.0f, SG_DT_MAX = 50.0f;
    /** Containment activity gauges are logarithmic: this many decades below a whole core's release read 0. */
    public static final int ACTIVITY_DECADES = 5;
    private static final String STATUS = "create_nuclearindustry.control_room.status.";

    /** Airborne activity as a 0-1 gauge position, one fifth per decade up to a whole core's release. */
    public static float activityFraction(float activity) {
        if (!(activity > 0)) {
            return 0.0f;
        }
        double decades = Math.log10(activity / ReleasePath.CORE_REFERENCE) + ACTIVITY_DECADES;
        return clamp01((float) (decades / ACTIVITY_DECADES));
    }

    public float value(int index) {
        return index >= 0 && index < values.length ? values[index] : 0.0f;
    }

    public boolean flag(int index) {
        return value(index) != 0.0f;
    }

    /** 0-15 for a comparator reading the monitor. */
    public int comparatorSignal() {
        if (kind == TargetKind.NONE || kind == TargetKind.NO_SIGNAL) {
            return 0;
        }
        return Mth.clamp(Math.round(seriesA * 15.0f), 0, 15);
    }

    public static MonitorReading sample(LinkedTarget.Resolved target) {
        return switch (target) {
            case LinkedTarget.Reactor reactor -> reactor(reactor.reactor());
            case LinkedTarget.Turbine turbine -> turbine(turbine.turbine());
            case LinkedTarget.Tower tower -> tower(tower.tower());
            case LinkedTarget.Tokamak tokamak -> tokamak(tokamak.tokamak());
            case LinkedTarget.Plant plant -> plant.machine().plantSample();
            case LinkedTarget.Area area -> area(area);
            case LinkedTarget.NoSignal noSignal -> new MonitorReading(noSignal.kind(), noSignal.reason(), new float[0], 0, 0);
        };
    }

    private static MonitorReading reactor(ReactorBlockEntity reactor) {
        float[] v = new float[REACTOR_VALUES];
        v[R_TEMPERATURE] = (float) reactor.getCoreTemperature();
        v[R_POWER] = (float) reactor.getPowerOutput();
        v[R_NEUTRONS] = (float) reactor.getNeutronLevel();
        v[R_REACTIVITY] = (float) reactor.getReactivity();
        v[R_XENON] = (float) reactor.getXenonPenalty();
        v[R_DECAY_HEAT] = (float) reactor.getDecayHeatPower();
        v[R_RODS] = reactor.getControlRodInsertionRatio();
        v[R_ROD_TARGET] = reactor.getEffectiveRodTarget();
        v[R_COOLANT] = fraction(reactor.getCoolantAmount(), reactor.getTankCapacity());
        v[R_STEAM] = fraction(reactor.getSteamAmount(), reactor.getTankCapacity());
        v[R_FUEL] = fraction(reactor.getFuelRemaining(), reactor.getFuelCapacity());
        v[R_SCRAM] = reactor.isScramRequested() ? 1 : 0;
        v[R_FORMED] = reactor.isFormed() ? 1 : 0;
        v[R_STRESS] = (float) reactor.getThermalStress();
        PlantReadings plant = reactor.plantReadings();
        v[R_PRESSURE] = plant.pressure();
        v[R_T_HOT] = plant.hotLeg();
        v[R_T_COLD] = plant.coldLeg();
        v[R_SUBCOOLING] = plant.subcooling();
        v[R_PZR_LEVEL] = plant.pressurizerLevel();
        v[R_FLOW] = plant.flow();
        v[R_VESSEL_LEVEL] = plant.vesselLevel();
        v[R_CONT_PRESSURE] = plant.containmentPressure();
        v[R_H2] = plant.hydrogen();
        v[R_PWR] = plant.pwr() ? 1 : 0;
        v[R_RELIEF] = plant.pwr() && reactor.getPrimaryLoop().reliefOpen() ? 1 : 0;
        String status = ReactorReadout.STATUS.read(reactor).getString();
        return new MonitorReading(TargetKind.REACTOR, status, v,
                clamp01(v[R_TEMPERATURE] / 4000.0f), clamp01(v[R_POWER] / 1000.0f));
    }

    private static MonitorReading turbine(AbstractTurbineControllerBlockEntity turbine) {
        TurbineDisplay display = turbine.getDisplay();
        float[] v = new float[7];
        float speed = Math.abs(turbine.getSpeed());
        v[T_SPEED] = speed;
        v[T_USE] = turbine.getCurrentUse();
        v[T_LIMIT] = display.limit();
        v[T_STEAM] = fraction(turbine.getSteamTank().getFluidAmount(), turbine.getSteamTank().getCapacity());
        v[T_EXHAUST] = display.exhausting() ? 1 : 0;
        v[T_FORMED] = display.formed() ? 1 : 0;
        v[T_CAPACITY] = turbine.calculateAddedStressCapacity() * speed;
        String status = !display.formed() ? STATUS + "unformed"
                : turbine.isSuspended() ? STATUS + "suspended"
                : turbine.getCurrentUse() > 0 ? STATUS + "running" : STATUS + "idle";
        return new MonitorReading(TargetKind.TURBINE, status, v, display.load(), clamp01(speed / 256.0f));
    }

    private static MonitorReading tower(org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity tower) {
        var display = tower.getDisplay();
        float[] v = new float[6];
        v[C_USE] = tower.getRecentUse();
        v[C_CAPACITY] = tower.getCapacity();
        v[C_WATER] = fraction(tower.getWaterTank().getFluidAmount(), tower.getWaterTank().getCapacity());
        v[C_STEAM] = fraction(tower.getSteamTank().getFluidAmount(), tower.getSteamTank().getCapacity());
        v[C_FANS] = display.fans();
        v[C_FORMED] = tower.getStructure().isPresent() ? 1 : 0;
        String status = tower.getStructure().isEmpty() ? STATUS + "unformed"
                : v[C_USE] > 0 ? STATUS + "running" : STATUS + "idle";
        return new MonitorReading(TargetKind.COOLING_TOWER, status, v, fraction(v[C_USE], v[C_CAPACITY]), v[C_WATER]);
    }

    private static MonitorReading tokamak(org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity tokamak) {
        var physics = tokamak.getPhysics();
        float[] v = new float[8];
        v[K_TEMPERATURE] = (float) physics.getPlasmaTemperature();
        v[K_POWER] = (float) physics.getFusionPower();
        v[K_CONFINEMENT] = (float) physics.getConfinement();
        v[K_INSTABILITY] = (float) physics.getInstability();
        v[K_DEUTERIUM] = fraction(tokamak.getDeuteriumTank().getFluidAmount(), tokamak.getDeuteriumTank().getCapacity());
        v[K_TRITIUM] = fraction(tokamak.getTritiumTank().getFluidAmount(), tokamak.getTritiumTank().getCapacity());
        v[K_FORMED] = tokamak.isFormed() ? 1 : 0;
        v[K_FUEL] = tokamak.isFuelEnabled() ? 1 : 0;
        String status = !tokamak.isFormed() ? STATUS + "unformed"
                : tokamak.getStatus() == org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity.Status.SUSPENDED
                ? STATUS + "suspended"
                : physics.isBurning() ? STATUS + "burning" : STATUS + "cold";
        return new MonitorReading(TargetKind.TOKAMAK, status, v,
                clamp01(v[K_TEMPERATURE] / (float) org.papiricoh.create_nuclearindustry.fusion.physics.TokamakPhysicsSimulator.MAX_TEMP),
                clamp01(v[K_POWER] / TOKAMAK_GRAPH_POWER));
    }

    private static MonitorReading area(LinkedTarget.Area area) {
        Vec3 body = Vec3.atCenterOf(area.pos()).add(0, 0.5, 0);
        float[] v = new float[2];
        v[A_GROUND] = Config.contaminationEnabled
                ? Contamination.get(area.level(), new net.minecraft.world.level.ChunkPos(area.pos()))
                : 0;
        v[A_EXPOSURE] = (float) RadiationHandler.ambientRate(area.level(), body);
        String status = v[A_EXPOSURE] >= Config.radiationBackgroundRate ? STATUS + "radiation" : STATUS + "clear";
        float groundMax = (float) Math.max(1.0e-3, Config.contaminationMax);
        return new MonitorReading(TargetKind.AREA, status, v, clamp01(v[A_EXPOSURE] / AREA_GRAPH_MAX), clamp01(v[A_GROUND] / groundMax));
    }

    private static float fraction(double amount, double capacity) {
        return capacity <= 0 ? 0 : clamp01((float) (amount / capacity));
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0.0f, 1.0f);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putString("Status", status);
        int[] bits = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            bits[i] = Float.floatToIntBits(values[i]);
        }
        tag.putIntArray("Values", bits);
        tag.putFloat("A", seriesA);
        tag.putFloat("B", seriesB);
        return tag;
    }

    public static MonitorReading read(CompoundTag tag) {
        if (!tag.contains("Kind")) {
            return NONE;
        }
        int[] bits = tag.getIntArray("Values");
        float[] values = new float[bits.length];
        for (int i = 0; i < bits.length; i++) {
            values[i] = Float.intBitsToFloat(bits[i]);
        }
        return new MonitorReading(TargetKind.byName(tag.getString("Kind")), tag.getString("Status"), values,
                tag.getFloat("A"), tag.getFloat("B"));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MonitorReading reading && kind == reading.kind && status.equals(reading.status)
                && Arrays.equals(values, reading.values) && seriesA == reading.seriesA && seriesB == reading.seriesB;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * kind.hashCode() + status.hashCode()) + Arrays.hashCode(values);
    }
}
