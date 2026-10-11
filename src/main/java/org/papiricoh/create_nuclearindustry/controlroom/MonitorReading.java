package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ReactivityModel;
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
    /** The fission core's physics, appended: rated power MW, net reactivity pcm, coolant °C, damage 0-1, period s, shutdown margin pcm. */
    public static final int R_RATED_POWER = 25, R_NET_REACTIVITY = 26, R_COOLANT_TEMP = 27, R_CORE_DAMAGE = 28, R_PERIOD = 29,
            R_SHUTDOWN_MARGIN = 30;
    /** The net reactivity split into its parts, pcm: fuel (with graphite, reflector, heavy water), rods, temperature, voids. */
    public static final int R_REACTIVITY_FUEL = 31, R_REACTIVITY_RODS = 32, R_REACTIVITY_TEMPERATURE = 33, R_REACTIVITY_VOIDS = 34;
    private static final int REACTOR_VALUES = 35;
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
    // Tokamak. K_FUEL is the controller's old fuel switch, now an alias of running or shut down.
    public static final int K_TEMPERATURE = 0, K_POWER = 1, K_CONFINEMENT = 2, K_INSTABILITY = 3, K_DEUTERIUM = 4,
            K_TRITIUM = 5, K_FORMED = 6, K_FUEL = 7;
    // Tokamak startup and plasma control (see TokamakReadings): phase and trip cause by ordinal, density as a
    // fraction of the Greenwald limit, ash share, gain (TokamakScales.GAIN_IGNITED when ignited), field 0-1,
    // magnet temperature K, chamber pressure log10 Pa, heating MW, fuelling and its ceiling mB/t, liquid helium
    // tank 0-1, heating switch flag and level 0-1, tripped flag
    public static final int K_PHASE = 8, K_TRIP = 9, K_DENSITY = 10, K_ASH = 11, K_GAIN = 12, K_FIELD = 13,
            K_MAGNET_TEMPERATURE = 14, K_VACUUM = 15, K_HEATING = 16, K_FUEL_RATE = 17, K_FUEL_RATE_MAX = 18,
            K_LIQUID_HELIUM = 19, K_HEATING_ON = 20, K_HEATING_LEVEL = 21, K_TRIPPED = 22;
    private static final int TOKAMAK_VALUES = 23;

    /** Graph ceiling for area exposure, in rad/s. */
    public static final float AREA_GRAPH_MAX = 10.0f;
    /** Graph ceiling for tokamak power, in MW. */
    public static final float TOKAMAK_GRAPH_POWER = TokamakScales.POWER_MAX;
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
        v[R_RATED_POWER] = (float) reactor.getRatedPower();
        v[R_NET_REACTIVITY] = (float) reactor.getNetReactivity();
        v[R_COOLANT_TEMP] = (float) reactor.getCoolantTemperature();
        v[R_CORE_DAMAGE] = (float) reactor.getCoreDamage();
        double period = reactor.getPeriod();
        v[R_PERIOD] = Double.isFinite(period) ? (float) Math.max(-1.0e6, Math.min(1.0e6, period)) : 0.0f;
        v[R_SHUTDOWN_MARGIN] = (float) reactor.getShutdownMargin();
        ReactivityModel.Parts parts = reactor.getReactivityParts();
        v[R_REACTIVITY_FUEL] = (float) parts.fuel();
        v[R_REACTIVITY_RODS] = (float) parts.rods();
        v[R_REACTIVITY_TEMPERATURE] = (float) parts.temperature();
        v[R_REACTIVITY_VOIDS] = (float) parts.voids();
        String status = ReactorReadout.STATUS.read(reactor).getString();
        float rated = Math.max(1.0f, v[R_RATED_POWER]);
        return new MonitorReading(TargetKind.REACTOR, status, v,
                clamp01(v[R_TEMPERATURE] / 4000.0f), clamp01(v[R_POWER] / rated));
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
        TokamakReadings k = tokamak.readings();
        float[] v = new float[TOKAMAK_VALUES];
        v[K_TEMPERATURE] = (float) k.temperature();
        v[K_POWER] = (float) k.fusionPower();
        v[K_CONFINEMENT] = (float) k.confinement();
        v[K_INSTABILITY] = (float) k.instability();
        v[K_DEUTERIUM] = fraction(k.deuterium(), tokamak.getDeuteriumTank().getCapacity());
        v[K_TRITIUM] = fraction(k.tritium(), tokamak.getTritiumTank().getCapacity());
        v[K_FORMED] = k.formed() ? 1 : 0;
        v[K_FUEL] = tokamak.isFuelEnabled() ? 1 : 0;
        v[K_PHASE] = k.phase().ordinal();
        v[K_TRIP] = k.tripCause().ordinal();
        v[K_DENSITY] = (float) k.density();
        v[K_ASH] = (float) k.ashFraction();
        v[K_GAIN] = TokamakScales.storedGain(k.gain());
        v[K_FIELD] = (float) k.field();
        v[K_MAGNET_TEMPERATURE] = (float) k.magnetTemperature();
        v[K_VACUUM] = (float) k.vacuum();
        v[K_HEATING] = (float) k.heatingPower();
        v[K_FUEL_RATE] = (float) k.fuelRate();
        v[K_FUEL_RATE_MAX] = k.formed() ? (float) tokamak.maxFuelRate() : 0;
        v[K_LIQUID_HELIUM] = fraction(k.liquidHelium(), tokamak.cryoTanks().liquidHelium().getCapacity());
        v[K_HEATING_ON] = k.heatingEnabled() ? 1 : 0;
        v[K_HEATING_LEVEL] = (float) k.heatingLevel();
        boolean tripped = k.formed() && TokamakScales.tripped(k.phase(), k.tripCause());
        v[K_TRIPPED] = tripped ? 1 : 0;
        String status = !k.formed() ? STATUS + "unformed"
                : k.suspended() ? STATUS + "suspended"
                : tripped ? STATUS + "tripped"
                : k.phase() == TokamakPhase.BURN ? STATUS + "burning"
                : k.phase() == TokamakPhase.OFFLINE ? STATUS + "cold"
                : k.phase().langKey();
        return new MonitorReading(TargetKind.TOKAMAK, status, v,
                clamp01(v[K_TEMPERATURE] / TokamakScales.MAX_TEMPERATURE),
                clamp01(v[K_POWER] / TOKAMAK_GRAPH_POWER));
    }

    private static MonitorReading area(LinkedTarget.Area area) {
        Vec3 body = Vec3.atCenterOf(area.pos()).add(0, 0.5, 0);
        float[] v = new float[2];
        v[A_GROUND] = Config.contaminationEnabled
                ? Contamination.get(area.level(), new net.minecraft.world.level.ChunkPos(area.pos()))
                : 0;
        v[A_EXPOSURE] = (float) RadiationHandler.cachedAmbientRate(area.level(), body);
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
