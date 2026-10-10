package org.papiricoh.create_nuclearindustry.reactor.pwr;

import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.reactor.pwr.physics.CoolantFeedback;
import org.papiricoh.create_nuclearindustry.reactor.pwr.physics.PressurizerModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.physics.PrimaryCoolantModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.physics.SaturationTable;

/**
 * The pressurized primary loop of one fission reactor: its vessel water and its pressurizer, wired
 * to the config. The reactor's coolant tank is the cold leg and its output tank the hot leg; this
 * decides each tick how much heat leaves the core, how much cold coolant comes out hot, what the
 * relief valve vents and how the water feeds back on the chain reaction. The flows the pumps really
 * move through the reactor's ports are measured here as moving averages.
 */
public final class PrimaryLoop {
    /** Coolant that just covers the core, mB. Less uncovers it. */
    public static final int CORE_VOLUME = 12_000;
    /** Water one pressurizer block holds, mB. */
    public static final int PRESSURIZER_VOLUME_PER_SEGMENT = 2_000;
    /** Cooling a dry core still gets from steam, as a share of a covered one. */
    private static final double UNCOVERED_COOLING = 0.1;
    private static final double LATENT_PER_MB = 0.6;
    private static final double VAPOR_EXPANSION = 7.0;
    private static final double VESSEL_HEAT_LOSS = 0.0005;
    private static final double EXPANSION_PER_DEGREE = 0.005;
    /** Boiling heat an open relief valve lets out with its steam, heat units per tick. */
    private static final double RELIEF_STEAM_HEAT = 150.0;
    /** Water an open relief valve lets out of a full pressurizer, mB per tick. */
    private static final int RELIEF_LIQUID_FLOW = 100;
    private static final double FLOW_SMOOTHING = 0.25;
    /** Cold leg flow the spray needs, mB/t. */
    private static final double SPRAY_MIN_FLOW = 1.0;

    private final PrimaryCoolantModel coolant = new PrimaryCoolantModel();
    private PressurizerModel pressurizer;
    private double flowIn;
    private double flowOut;
    private int inThisTick;
    private int outThisTick;
    private PlantReadings readings = PlantReadings.NONE;
    private boolean heatersRunning;
    private boolean spraying;
    private boolean reliefStuck;

    /**
     * What the reactor knows this tick.
     *
     * @param coreTemperature core temperature, °C
     * @param rodMultiplier   the simulator's size factor: core cooling times this is heat units
     * @param maxHeat         most heat units the heat exchangers pass per tick
     * @param cold            cold coolant in the vessel, mB
     * @param hot             hot coolant in the vessel, mB
     * @param hotCapacity     room for hot coolant, mB
     * @param stack           the pressurizer on the lid
     * @param autoHeaters     heater ports under automatic control
     * @param manualHeaters   heater ports in manual mode with a redstone signal
     * @param reliefIsolated  the relief valve gets a redstone signal
     * @param stickRoll       a uniform random number in [0, 1)
     */
    public record Context(double coreTemperature, double rodMultiplier, double maxHeat, int cold, int hot, int hotCapacity,
                          PressurizerStack stack, int autoHeaters, int manualHeaters, boolean reliefIsolated, double stickRoll) {}

    /**
     * What the reactor applies.
     *
     * @param coreCooling °C per tick taken from the core
     * @param moved       cold coolant to turn into hot coolant, mB
     * @param vented      coolant the relief valve let out, mB
     * @param feedback    multiplier on neutron production
     */
    public record Result(double coreCooling, int moved, int vented, double feedback) {}

    /** Coolant pumped into the cold leg; hot coolant brings its heat back with it. */
    public void recordInflow(int amount, boolean hot) {
        inThisTick += Math.max(0, amount);
        if (hot) {
            coolant.addHeat(Math.max(0, amount) * PwrConfig.enthalpyPerMb);
        }
    }

    /** Hot coolant pumped out of the hot leg. */
    public void recordOutflow(int amount) {
        outThisTick += Math.max(0, amount);
    }

    public Result tick(Context c) {
        flowIn += (inThisTick - flowIn) * FLOW_SMOOTHING;
        flowOut += (outThisTick - flowOut) * FLOW_SMOOTHING;
        inThisTick = 0;
        outThisTick = 0;

        PrimaryCoolantModel.Params coolantParams = coolantParams();
        double inventory = Math.max(0, c.cold()) + Math.max(0, c.hot());
        double capacity = c.stack().segments() * (double) PRESSURIZER_VOLUME_PER_SEGMENT;
        if (pressurizer == null) {
            coolant.update(inventory, CORE_VOLUME, SaturationTable.temperature(PwrConfig.pressureSetpoint), coolantParams);
            pressurizer = PressurizerModel.atSetpoint(PwrConfig.pressureSetpoint, levelDemand(inventory, capacity));
        }
        double saturation = SaturationTable.temperature(pressurizer.pressure());
        coolant.update(inventory, CORE_VOLUME, saturation, coolantParams);

        double coverage = coolant.coverage();
        double rodMultiplier = Math.max(1.0, c.rodMultiplier());
        double coreCooling = PwrConfig.heatTransfer * Math.max(0.0, c.coreTemperature() - coolant.coldLeg())
                * (coverage + UNCOVERED_COOLING * (1.0 - coverage));
        coreCooling = Math.min(coreCooling, Math.max(0.0, c.maxHeat()) / rodMultiplier);
        int moved = coolant.step(new PrimaryCoolantModel.Conditions(coreCooling * rodMultiplier, c.cold(),
                Math.max(0, c.hotCapacity() - c.hot()), Math.max(flowIn, flowOut), inventory, CORE_VOLUME, saturation), coolantParams);

        boolean venting = pressurizer.tick(new PressurizerModel.Inputs(levelDemand(inventory, capacity),
                c.autoHeaters(), c.manualHeaters(), flowIn >= SPRAY_MIN_FLOW, coolant.hotLeg(), coolant.coldLeg(),
                c.reliefIsolated(), c.stickRoll(), PwrConfig.reliefStickChance), pressurizerParams());
        int vented = 0;
        if (venting) {
            vented = (int) Math.round(coolant.ventSteam(RELIEF_STEAM_HEAT, coolantParams));
            if (pressurizer.solid()) {
                vented += RELIEF_LIQUID_FLOW;
            }
        }
        double pressure = pressurizer.pressure();
        coolant.update(inventory, CORE_VOLUME, SaturationTable.temperature(pressure), coolantParams);
        double feedback = CoolantFeedback.multiplier(coolant.voidFraction(), coolant.coverage(), coolant.averageTemperature(),
                referenceAverage(), PwrConfig.voidWorth, PwrConfig.temperatureCoefficient);

        heatersRunning = pressurizer.heatersRunning() > 0;
        spraying = pressurizer.spraying();
        reliefStuck = pressurizer.reliefStuck();
        readings = new PlantReadings(true, (float) pressure, (float) coolant.hotLeg(), (float) coolant.coldLeg(),
                (float) (SaturationTable.temperature(pressure) - coolant.hotLeg()), (float) pressurizer.level(),
                (float) coolant.coreFlow(), (float) coolant.coverage(), 0.0f, 0.0f);
        return new Result(coreCooling, moved, vented, feedback);
    }

    private double levelDemand(double inventory, double capacity) {
        return PressurizerModel.levelDemand(inventory, CORE_VOLUME, capacity, coolant.averageTemperature(), referenceAverage(),
                EXPANSION_PER_DEGREE, coolant.vaporVolume());
    }

    /** Average core coolant temperature at full flow, where the feedback is neutral. */
    private static double referenceAverage() {
        return PwrConfig.coldLegTemperature + PwrConfig.coreTemperatureRise / 2.0;
    }

    private static PrimaryCoolantModel.Params coolantParams() {
        return new PrimaryCoolantModel.Params(PwrConfig.coldLegTemperature, PwrConfig.enthalpyPerMb, PwrConfig.coreTemperatureRise,
                LATENT_PER_MB, VAPOR_EXPANSION, PwrConfig.naturalCirculation, VESSEL_HEAT_LOSS);
    }

    private static PressurizerModel.Params pressurizerParams() {
        return new PressurizerModel.Params(PwrConfig.pressureSetpoint, 2.0, 5.0, PwrConfig.heaterRate, 0.05, 0.002,
                20.0, 60.0, PwrConfig.reliefOpenPressure, PwrConfig.reliefClosePressure, 0.3, 0.1);
    }

    /** The loop stopped running (no coolant, no pressurizer): readings go blank, the state stays for a restart. */
    public void idle() {
        readings = PlantReadings.NONE;
        inThisTick = 0;
        outThisTick = 0;
        flowIn = 0;
        flowOut = 0;
        heatersRunning = false;
        spraying = false;
    }

    public PlantReadings readings() {
        return readings;
    }

    public boolean reliefOpen() {
        return pressurizer != null && pressurizer.reliefOpen();
    }

    public boolean reliefStuck() {
        return reliefStuck;
    }

    public boolean heatersRunning() {
        return heatersRunning;
    }

    public boolean spraying() {
        return spraying;
    }

    /** Smoothed flow pumped into the cold leg, mB/t. */
    public double flowIn() {
        return flowIn;
    }

    /** Smoothed flow pumped out of the hot leg, mB/t. */
    public double flowOut() {
        return flowOut;
    }

    public PrimaryCoolantModel coolant() {
        return coolant;
    }

    /** Full state for saving; also what clients get for goggles and monitors. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("StoredHeat", coolant.storedHeat());
        tag.putDouble("FlowRise", coolant.flowRise());
        tag.putDouble("FlowIn", flowIn);
        tag.putDouble("FlowOut", flowOut);
        if (pressurizer != null) {
            CompoundTag pzr = new CompoundTag();
            pzr.putDouble("Bubble", pressurizer.bubbleTemperature());
            pzr.putDouble("Level", pressurizer.level());
            pzr.putBoolean("AutoHeaters", pressurizer.autoHeatersOn());
            pzr.putBoolean("ReliefOpen", pressurizer.reliefOpen());
            pzr.putBoolean("ReliefStuck", pressurizer.reliefStuck());
            tag.put("Pressurizer", pzr);
        }
        CompoundTag r = new CompoundTag();
        r.putBoolean("Pwr", readings.pwr());
        r.putFloat("Pressure", readings.pressure());
        r.putFloat("HotLeg", readings.hotLeg());
        r.putFloat("ColdLeg", readings.coldLeg());
        r.putFloat("Subcooling", readings.subcooling());
        r.putFloat("PzrLevel", readings.pressurizerLevel());
        r.putFloat("Flow", readings.flow());
        r.putFloat("VesselLevel", readings.vesselLevel());
        r.putBoolean("Heaters", heatersRunning);
        r.putBoolean("Spray", spraying);
        r.putBoolean("Stuck", reliefStuck);
        tag.put("Readings", r);
        return tag;
    }

    public void load(CompoundTag tag) {
        coolant.restore(tag.getDouble("StoredHeat"), tag.getDouble("FlowRise"));
        flowIn = Math.max(0.0, tag.getDouble("FlowIn"));
        flowOut = Math.max(0.0, tag.getDouble("FlowOut"));
        if (tag.contains("Pressurizer")) {
            CompoundTag pzr = tag.getCompound("Pressurizer");
            if (pressurizer == null) {
                pressurizer = new PressurizerModel();
            }
            pressurizer.restore(pzr.getDouble("Bubble"), pzr.getDouble("Level"), pzr.getBoolean("AutoHeaters"),
                    pzr.getBoolean("ReliefOpen"), pzr.getBoolean("ReliefStuck"));
        } else {
            pressurizer = null;
        }
        CompoundTag r = tag.getCompound("Readings");
        readings = r.getBoolean("Pwr")
                ? new PlantReadings(true, r.getFloat("Pressure"), r.getFloat("HotLeg"), r.getFloat("ColdLeg"), r.getFloat("Subcooling"),
                r.getFloat("PzrLevel"), r.getFloat("Flow"), r.getFloat("VesselLevel"), 0.0f, 0.0f)
                : PlantReadings.NONE;
        heatersRunning = r.getBoolean("Heaters");
        spraying = r.getBoolean("Spray");
        reliefStuck = r.getBoolean("Stuck");
    }
}
