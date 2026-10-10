package org.papiricoh.create_nuclearindustry.controlroom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
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
    private static final String STATUS = "create_nuclearindustry.control_room.status.";

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
            case LinkedTarget.Area area -> area(area);
            case LinkedTarget.NoSignal noSignal -> new MonitorReading(noSignal.kind(), noSignal.reason(), new float[0], 0, 0);
        };
    }

    private static MonitorReading reactor(ReactorBlockEntity reactor) {
        float[] v = new float[14];
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
