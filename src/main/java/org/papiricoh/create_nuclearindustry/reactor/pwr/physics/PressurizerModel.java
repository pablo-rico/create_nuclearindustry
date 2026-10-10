package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

/**
 * The pressurizer: a tall tank on the reactor lid, part water and part steam, whose steam bubble sets
 * the pressure of the whole primary loop. Heaters boil a little more water to raise it, spray from the
 * cold leg condenses steam to lower it, and water surging in from the loop (heat-up, boiling in the
 * core) compresses the bubble while water draining out (cool-down, a leak) lets it flash. Once the
 * pressurizer fills with water there is no bubble to compress and the pressure climbs fast, which is
 * what the relief valve on top is for: it opens at one pressure, closes at a lower one, and may stick
 * open when it should close. Kept free of game classes so it can be unit tested.
 */
public final class PressurizerModel {
    /** Highest pressure the model reports, bar; past this the vessel has long failed. */
    public static final double MAX_PRESSURE = 400.0;

    /**
     * @param setpoint          pressure the automatic heaters and spray hold, bar
     * @param heaterBand        automatic heaters switch on this far below the setpoint and off at it, bar
     * @param sprayBand         spray starts this far above the setpoint, bar
     * @param heaterRate        °C per tick one heater adds to the steam bubble
     * @param sprayRate         share of the bubble's excess over the cold leg the spray removes each tick
     * @param heatLoss          share of the bubble's excess over the hot leg lost each tick
     * @param surgeGain         °C the bubble gains when the level rises by the whole pressurizer
     * @param solidStiffness    bar the pressure climbs per pressurizer volume of water pushed into a full one
     * @param reliefOpen        pressure the relief valve opens at, bar
     * @param reliefClose       pressure it closes at, bar
     * @param reliefCooling     °C per tick an open relief valve takes from the bubble
     * @param minHeaterLevel    heaters trip below this level, so they never run dry
     */
    public record Params(double setpoint, double heaterBand, double sprayBand, double heaterRate, double sprayRate,
                         double heatLoss, double surgeGain, double solidStiffness, double reliefOpen, double reliefClose,
                         double reliefCooling, double minHeaterLevel) {}

    /**
     * One tick's conditions.
     *
     * @param levelDemand    water the loop pushes into the pressurizer, in pressurizer volumes; above 1 it is full
     * @param autoHeaters    heater banks under automatic control
     * @param manualHeaters  heater banks switched on by hand
     * @param sprayAvailable the cold leg flows, so the spray has water
     * @param hotLeg         hot leg temperature, °C
     * @param coldLeg        cold leg temperature, °C
     * @param reliefIsolated the block valve under the relief valve is shut
     * @param stickRoll      a uniform random number in [0, 1) for the relief valve sticking
     * @param stickChance    chance the relief valve sticks open when it should close
     * @param demand         what an operator or a pressure controller asks of the heaters and the spray
     */
    public record Inputs(double levelDemand, int autoHeaters, int manualHeaters, boolean sprayAvailable, double hotLeg,
                         double coldLeg, boolean reliefIsolated, double stickRoll, double stickChance, Demand demand) {
        public Inputs(double levelDemand, int autoHeaters, int manualHeaters, boolean sprayAvailable, double hotLeg,
                      double coldLeg, boolean reliefIsolated, double stickRoll, double stickChance) {
            this(levelDemand, autoHeaters, manualHeaters, sprayAvailable, hotLeg, coldLeg, reliefIsolated, stickRoll,
                    stickChance, Demand.NONE);
        }
    }

    /**
     * Control from outside the pressurizer's own setpoint logic. A pressure controller ({@code controlled})
     * replaces the automatic on/off heaters and spray: it runs the automatic heater banks at a share of
     * their power, as proportional heaters, and opens the spray valve part way. A forced spray opens it
     * fully whoever is in charge. The spray still needs the cold leg to flow.
     *
     * @param controlled a pressure controller drives the automatic heaters and the spray
     * @param heaters    share of the automatic heater banks' power it asks for, 0-1
     * @param spray      spray valve opening asked for, 0-1, by the controller or by hand
     */
    public record Demand(boolean controlled, double heaters, double spray) {
        public static final Demand NONE = new Demand(false, 0.0, 0.0);

        public Demand {
            heaters = clamp(heaters, 0.0, 1.0);
            spray = clamp(spray, 0.0, 1.0);
        }
    }

    private double bubbleTemperature;
    private double level;
    private double solidPressure;
    private boolean autoHeatersOn;
    private int heatersRunning;
    /** Heater banks' worth of heat going into the bubble this tick: proportional heaters run part way. */
    private double heaterPower;
    private boolean spraying;
    private boolean reliefOpen;
    private boolean reliefStuck;

    /** A pressurizer at the setpoint with its level where the loop puts it. */
    public static PressurizerModel atSetpoint(double setpoint, double levelDemand) {
        PressurizerModel model = new PressurizerModel();
        model.bubbleTemperature = SaturationTable.temperature(setpoint);
        model.level = clamp(levelDemand, 0.0, 1.0);
        return model;
    }

    /**
     * Where the loop puts the pressurizer's water level, in pressurizer volumes: the coolant beyond what
     * covers the core, the swell of warmer water and the volume of the steam in the core.
     */
    public static double levelDemand(double inventory, double coreVolume, double capacity, double averageTemperature,
                                     double referenceTemperature, double expansionPerDegree, double vaporVolume) {
        double volume = Math.max(1.0, capacity);
        return (inventory - coreVolume) / volume
                + expansionPerDegree * (averageTemperature - referenceTemperature)
                + Math.max(0.0, vaporVolume) / volume;
    }

    /** Advances one tick. Returns whether the relief valve let steam out. */
    public boolean tick(Inputs in, Params p) {
        double newLevel = clamp(in.levelDemand(), 0.0, 1.0);
        bubbleTemperature += p.surgeGain() * (newLevel - level);
        level = newLevel;
        solidPressure = Math.max(0.0, in.levelDemand() - 1.0) * p.solidStiffness();

        double pressure = pressure();
        Demand demand = in.demand() == null ? Demand.NONE : in.demand();
        double autoShare;
        double sprayShare = demand.spray();
        if (demand.controlled()) {
            autoHeatersOn = false;
            autoShare = demand.heaters();
        } else {
            if (pressure < p.setpoint() - p.heaterBand()) {
                autoHeatersOn = true;
            } else if (pressure > p.setpoint()) {
                autoHeatersOn = false;
            }
            autoShare = autoHeatersOn ? 1.0 : 0.0;
            if (pressure > p.setpoint() + p.sprayBand()) {
                sprayShare = 1.0;
            }
        }
        double power = Math.max(0, in.manualHeaters()) + autoShare * Math.max(0, in.autoHeaters());
        heaterPower = level >= p.minHeaterLevel() ? power : 0.0;
        heatersRunning = (int) Math.ceil(heaterPower - 1.0e-9);
        bubbleTemperature += p.heaterRate() * heaterPower;
        bubbleTemperature -= p.heatLoss() * (bubbleTemperature - in.hotLeg());

        spraying = in.sprayAvailable() && sprayShare > 0.0;
        if (spraying) {
            bubbleTemperature -= p.sprayRate() * sprayShare * Math.max(0.0, bubbleTemperature - in.coldLeg());
        }

        pressure = pressure();
        if (in.reliefIsolated()) {
            reliefStuck = false;
        } else if (!reliefOpen) {
            reliefOpen = pressure >= p.reliefOpen();
        } else if (pressure <= p.reliefClose() && !reliefStuck) {
            if (in.stickRoll() < in.stickChance()) {
                reliefStuck = true;
            } else {
                reliefOpen = false;
            }
        }
        boolean venting = reliefOpen && !in.reliefIsolated();
        if (venting) {
            bubbleTemperature -= p.reliefCooling();
            solidPressure *= 0.5;
        }
        if (in.reliefIsolated()) {
            reliefOpen = false;
        }
        bubbleTemperature = clamp(bubbleTemperature, SaturationTable.MIN_TEMPERATURE, SaturationTable.MAX_TEMPERATURE);
        return venting;
    }

    /** Pressure of the primary loop, bar. */
    public double pressure() {
        return Math.min(MAX_PRESSURE, SaturationTable.pressure(bubbleTemperature) + solidPressure);
    }

    public double bubbleTemperature() {
        return bubbleTemperature;
    }

    /** Water level, 0-1. */
    public double level() {
        return level;
    }

    /** True while the pressurizer is full of water and the pressure is no longer cushioned. */
    public boolean solid() {
        return solidPressure > 0.0;
    }

    /** Heater banks switched on, counting a part-powered bank as one. */
    public int heatersRunning() {
        return heatersRunning;
    }

    /** Heater banks' worth of heat the heaters gave the bubble last tick. */
    public double heaterPower() {
        return heaterPower;
    }

    public boolean spraying() {
        return spraying;
    }

    public boolean reliefOpen() {
        return reliefOpen;
    }

    public boolean reliefStuck() {
        return reliefStuck;
    }

    public boolean autoHeatersOn() {
        return autoHeatersOn;
    }

    /** Restores a saved state. */
    public void restore(double bubbleTemperature, double level, boolean autoHeatersOn, boolean reliefOpen, boolean reliefStuck) {
        this.bubbleTemperature = clamp(bubbleTemperature, SaturationTable.MIN_TEMPERATURE, SaturationTable.MAX_TEMPERATURE);
        this.level = clamp(level, 0.0, 1.0);
        this.autoHeatersOn = autoHeatersOn;
        this.reliefOpen = reliefOpen;
        this.reliefStuck = reliefStuck && reliefOpen;
        this.solidPressure = 0.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
