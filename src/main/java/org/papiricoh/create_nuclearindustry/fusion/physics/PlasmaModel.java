package org.papiricoh.create_nuclearindustry.fusion.physics;

/**
 * Zero-dimensional plasma of a tokamak, in the mod's "degrees" (20..6000) and millibuckets.
 *
 * <ul>
 *   <li>The plasma holds inventories of deuterium, tritium and helium ash, in mB. Their sum over the
 *       plasma volume is the density; its limit (the Greenwald density) grows with the field, and
 *       above it the plasma grows unstable.</li>
 *   <li>Fusion: {@code burn ∝ n²·(1-fHe)²·<σv>(T)·V}, with a reactivity that peaks near 4000°.
 *       A D-T mix burns at full power; deuterium alone runs the weaker D-D branch (0.3 of the power
 *       per mB), which breeds a little tritium directly.</li>
 *   <li>Losses: conduction {@code W/τE} with {@code τE ∝ R^0.8·confinement·n^0.4}, faster when hot,
 *       plus radiation {@code ∝ n²·√T·(1+5·fHe)}, so ash both dilutes and cools the fuel.</li>
 *   <li>Heating: the central solenoid (ohmic, strong while cold, fades with temperature and lasts one
 *       volt-second budget per shot), neutral beams (raw power) and radio frequency (raw power times a
 *       coupling that needs density). Auxiliary heating cuts back above {@link #HEATING_CUTBACK_TEMP}.</li>
 *   <li>20% of the fusion power heats the plasma; 80% leaves through the coolant as plasma steam.
 *       Whatever the coolant cannot carry stays in the plasma, so a dry tokamak overheats.</li>
 *   <li>Fuel lost from the plasma is pumped out and recycled to the tanks; ash is pumped out as helium.</li>
 *   <li>Overheating, poor confinement while hot or density above the limit build instability; at 100
 *       the plasma disrupts.</li>
 * </ul>
 * Kept free of game classes and config so it can be unit tested; the controller copies the config
 * into {@link Params}.
 */
public final class PlasmaModel {
    public static final double AMBIENT_TEMP = 20.0;
    /** Temperature a breakdown leaves the new plasma at. */
    public static final double BREAKDOWN_TEMP = 300.0;
    /** Above this the plasma is burning: the startup sequence goes from HEATING to BURN. */
    public static final double IGNITION_TEMP = 1500.0;
    /** Auxiliary heating starts to cut back here, and is off at {@link #DANGER_TEMP}. */
    public static final double HEATING_CUTBACK_TEMP = 4500.0;
    /** Above this the plasma grows unstable. */
    public static final double DANGER_TEMP = 5000.0;
    public static final double MAX_TEMP = 6000.0;
    /** Reactivity peaks here. */
    public static final double PEAK_REACTIVITY_TEMP = 4000.0;
    /** A hot plasma with less confinement than this grows unstable. */
    public static final double STABLE_CONFINEMENT = 0.55;
    /** Greenwald fraction above which the plasma grows unstable. */
    public static final double DENSITY_LIMIT = 1.0;
    /** Greenwald fraction the monitors warn at. */
    public static final double DENSITY_WARNING = 0.9;
    /** Instability the monitors warn at. */
    public static final double INSTABILITY_WARNING = 60.0;
    public static final double DISRUPTION_INSTABILITY = 100.0;
    /** MW of fusion power per mB/t of D-T fuel burnt (per mB of each isotope). */
    public static final double POWER_PER_MB = 1000.0;
    /** Share of the fusion power the alpha particles leave in the plasma. */
    public static final double PLASMA_HEATING_SHARE = 0.2;
    /** mB/t of plasma steam per MW of fusion power taken out by the coolant. */
    public static final double STEAM_PER_MW = 1.6;
    public static final double DD_POWER_FACTOR = 0.3;
    /** T made directly per mB of D burnt in the D-D branch. */
    public static final double DD_DIRECT_TRITIUM = 0.25;
    public static final double MAX_TBR = 1.6;
    /** Below this many mB the plasma is gone. */
    public static final double MIN_INVENTORY = 0.5;

    private static final double CONDUCTION_REFERENCE_TEMP = 3000.0;
    private static final double REFERENCE_DENSITY_FRACTION = 0.8;
    private static final double MIN_HEAT_CAPACITY_SHARE = 0.25;
    private static final double TERMINATION_TIME = 10.0;

    /**
     * @param burnRatePerCell       mB of D (and of T) burnt per plasma cell per tick at the reference density, peak reactivity and no ash
     * @param greenwaldDensity      density limit at full field, mB per plasma cell
     * @param referenceDensity      density the burn rate refers to, mB per plasma cell
     * @param heatCapacity          MW·tick per degree per plasma cell at the reference density
     * @param confinementTime       energy confinement time of an R=4 tokamak with full confinement, ticks
     * @param particleTime          time fuel stays in the plasma before it is pumped out (and recycled), ticks
     * @param ashTime               time helium ash stays in the plasma, ticks
     * @param radiation             radiation loss coefficient, MW per cell per √degree at the reference density
     * @param ohmicPowerPerCell     solenoid heating of a cold plasma, MW per cell
     * @param ohmicReferenceTemp    temperature up to which ohmic heating is at full power; it fades as T^-1.5 above
     * @param solenoidPulseTicks    how long the solenoid's volt-second budget lasts with a plasma
     * @param solenoidRechargeTicks how long a spent solenoid takes to recharge while offline
     * @param rfMinDensity          Greenwald fraction below which radio-frequency heating does not couple
     * @param rfFullDensity         Greenwald fraction above which it couples fully
     * @param beamFuelling          share of the neutral beams' deuterium that ends up in the plasma
     * @param densityInstability    instability per tick per unit of Greenwald fraction above the limit
     */
    public record Params(double burnRatePerCell, double greenwaldDensity, double referenceDensity, double heatCapacity,
                         double confinementTime, double particleTime, double ashTime, double radiation,
                         double ohmicPowerPerCell, double ohmicReferenceTemp, double solenoidPulseTicks,
                         double solenoidRechargeTicks, double rfMinDensity, double rfFullDensity, double beamFuelling,
                         double densityInstability) {
        public static final Params DEFAULT = new Params(0.0011, 1.0, 0.7, 0.0062, 150.0, 40.0, 100.0, 0.0008,
                0.08, 400.0, 600.0, 600.0, 0.15, 0.35, 0.25, 40.0);

        public Params withBurnRate(double burnRatePerCell) {
            return new Params(burnRatePerCell, greenwaldDensity, referenceDensity, heatCapacity, confinementTime,
                    particleTime, ashTime, radiation, ohmicPowerPerCell, ohmicReferenceTemp, solenoidPulseTicks,
                    solenoidRechargeTicks, rfMinDensity, rfFullDensity, beamFuelling, densityInstability);
        }
    }

    /**
     * One tick's conditions.
     *
     * @param volume          plasma cells
     * @param majorRadius     tokamak major radius
     * @param coilCoverage    toroidal coil coverage, 0..1
     * @param field           toroidal field, 0..1 of rated
     * @param blanketFraction share of the shell taken by breeding blankets
     * @param deuteriumFeed   deuterium injected this tick, mB (gas puff and pellets)
     * @param tritiumFeed     tritium injected this tick, mB
     * @param ohmic           the solenoid drives the plasma current
     * @param beamPower       neutral beam power reaching the plasma, MW
     * @param beamDeuterium   deuterium the beams carry this tick, mB
     * @param rfPower         radio-frequency power sent in, MW, before coupling
     * @param coolant         coolant available, mB
     * @param steamCapacity   plasma steam that can be taken out this tick, mB
     * @param terminate       ramping down: no instability builds and the plasma is pumped out fast
     */
    public record Inputs(int volume, int majorRadius, double coilCoverage, double field, double blanketFraction,
                         double deuteriumFeed, double tritiumFeed, boolean ohmic, double beamPower, double beamDeuterium,
                         double rfPower, double coolant, double steamCapacity, boolean terminate) {}

    /**
     * What the controller must move between its tanks after the tick.
     *
     * @param deuteriumBurnt    D burnt, mB
     * @param tritiumBurnt      T burnt, mB
     * @param tritiumBred       T made by the D-D branch and the breeding blankets, mB
     * @param steamProduced     plasma steam made from coolant, mB
     * @param heliumRemoved     helium ash pumped out, mB
     * @param deuteriumRecycled D pumped out of the plasma unburnt, mB
     * @param tritiumRecycled   T pumped out of the plasma unburnt, mB
     */
    public record Outputs(double deuteriumBurnt, double tritiumBurnt, double tritiumBred, double steamProduced,
                          double heliumRemoved, double deuteriumRecycled, double tritiumRecycled) {
        public static final Outputs NONE = new Outputs(0, 0, 0, 0, 0, 0, 0);
    }

    public enum Mode { COLD, DT, DD }

    // State.
    private double temperature = AMBIENT_TEMP;
    private double deuterium;
    private double tritium;
    private double ash;
    private double instability;
    private double solenoidFlux = 1.0;

    // Last tick's results.
    private double fusionPower;
    private double heatingPower;
    private double ohmicPower;
    private double burnRate;
    private double density;
    private double confinement;
    private double tritiumBreedingRatio;
    private Mode mode = Mode.COLD;

    /** Lights a new plasma: a puff of deuterium ionised by the solenoid. */
    public void breakdown(double deuteriumPuff) {
        deuterium += Math.max(0, deuteriumPuff);
        temperature = Math.max(temperature, BREAKDOWN_TEMP);
        instability = 0;
    }

    public Outputs tick(Params p, Inputs in) {
        int volume = Math.max(1, in.volume());
        confinement = clamp(in.coilCoverage() * in.field(), 0, 1);
        tritiumBreedingRatio = Math.min(MAX_TBR, 1.5 * in.blanketFraction() / 0.4);

        deuterium += Math.max(0, in.deuteriumFeed()) + Math.max(0, in.beamDeuterium()) * p.beamFuelling();
        tritium += Math.max(0, in.tritiumFeed());

        double inventory = deuterium + tritium + ash;
        double perCell = inventory / volume;
        density = perCell / greenwaldLimit(p, in.field());
        double ashFraction = inventory > 0 ? ash / inventory : 0;
        double relative = perCell / p.referenceDensity();

        // Fusion.
        double fuel = deuterium + tritium;
        double xD = fuel > 0 ? deuterium / fuel : 0;
        double xT = fuel > 0 ? tritium / fuel : 0;
        double fuelDensity = relative * (1 - ashFraction);
        double potential = hasPlasma() ? p.burnRatePerCell() * volume * fuelDensity * fuelDensity * reactivity(temperature) : 0;
        double dtBurn = Math.min(potential * 4 * xD * xT, Math.min(deuterium, tritium));
        // D-D only matters when tritium is scarce: in an even mix D-T outruns it many times over.
        double ddBurn = Math.max(0, Math.min(potential * xD * xD * Math.max(0, 1 - 4 * xD * xT), deuterium - dtBurn));
        burnRate = dtBurn + ddBurn;
        fusionPower = dtBurn * POWER_PER_MB + ddBurn * POWER_PER_MB * DD_POWER_FACTOR;
        mode = burnRate <= 0 ? Mode.COLD : dtBurn >= ddBurn ? Mode.DT : Mode.DD;
        double tritiumBred = dtBurn * tritiumBreedingRatio + ddBurn * (DD_DIRECT_TRITIUM + 0.25 * tritiumBreedingRatio);

        double steamWanted = (1 - PLASMA_HEATING_SHARE) * fusionPower * STEAM_PER_MW;
        double steam = Math.max(0, Math.min(steamWanted, Math.min(in.coolant(), in.steamCapacity())));
        double extracted = steamWanted <= 0 ? 1 : steam / steamWanted;
        double alphaHeating = fusionPower * (PLASMA_HEATING_SHARE + (1 - PLASMA_HEATING_SHARE) * (1 - extracted));

        // Heating.
        boolean plasma = hasPlasma();
        ohmicPower = plasma && in.ohmic() && solenoidFlux > 0
                ? p.ohmicPowerPerCell() * volume * Math.min(1, Math.pow(p.ohmicReferenceTemp() / Math.max(temperature, 1), 1.5))
                : 0;
        if (plasma && in.ohmic()) {
            solenoidFlux = Math.max(0, solenoidFlux - 1.0 / Math.max(1, p.solenoidPulseTicks()));
        }
        double cutback = clamp((DANGER_TEMP - temperature) / (DANGER_TEMP - HEATING_CUTBACK_TEMP), 0, 1);
        double auxiliary = plasma && !in.terminate()
                ? (Math.max(0, in.beamPower()) + Math.max(0, in.rfPower()) * rfCoupling(p, density)) * cutback
                : 0;
        heatingPower = ohmicPower + auxiliary;

        // Losses.
        double heatCapacity = heatCapacity(p, volume, relative);
        double tauE = confinementTime(p, in.majorRadius(), confinement, density);
        double conduction = heatCapacity * (temperature - AMBIENT_TEMP) / tauE * Math.sqrt(Math.max(temperature, AMBIENT_TEMP) / CONDUCTION_REFERENCE_TEMP);
        double radiation = plasma ? p.radiation() * volume * relative * relative * Math.sqrt(temperature) * (1 + 5 * ashFraction) : 0;
        temperature = clamp(temperature + (alphaHeating + heatingPower - conduction - radiation) / heatCapacity, AMBIENT_TEMP, MAX_TEMP);

        // Particles.
        double ashMade = dtBurn + 0.5 * ddBurn;
        deuterium = Math.max(0, deuterium - dtBurn - ddBurn);
        tritium = Math.max(0, tritium - dtBurn);
        ash += ashMade;
        double particleTime = in.terminate() ? TERMINATION_TIME : Math.max(1, p.particleTime());
        double ashTime = in.terminate() ? TERMINATION_TIME : Math.max(1, p.ashTime());
        double deuteriumOut = deuterium / particleTime;
        double tritiumOut = tritium / particleTime;
        double ashOut = ash / ashTime;
        deuterium -= deuteriumOut;
        tritium -= tritiumOut;
        ash -= ashOut;
        if (deuterium + tritium + ash < MIN_INVENTORY && in.terminate()) {
            deuteriumOut += deuterium;
            tritiumOut += tritium;
            ashOut += ash;
            deuterium = tritium = ash = 0;
        }
        if (!hasPlasma()) {
            temperature = AMBIENT_TEMP;
        }

        // Stability.
        if (temperature > DANGER_TEMP) {
            instability += (temperature - DANGER_TEMP) / 100.0;
        } else if (!in.terminate() && temperature >= IGNITION_TEMP && confinement < STABLE_CONFINEMENT) {
            instability += (STABLE_CONFINEMENT - confinement) * 4.0;
        } else if (!in.terminate() && hasPlasma() && density > DENSITY_LIMIT) {
            instability += (density - DENSITY_LIMIT) * p.densityInstability();
        } else {
            instability = Math.max(0, instability - 0.5);
        }
        instability = Math.min(DISRUPTION_INSTABILITY, instability);

        return new Outputs(dtBurn + ddBurn, dtBurn, tritiumBred, steam, ashOut, deuteriumOut, tritiumOut);
    }

    /** Recharges the solenoid's volt-second budget; call while there is no plasma. */
    public void rechargeSolenoid(Params p) {
        solenoidFlux = Math.min(1.0, solenoidFlux + 1.0 / Math.max(1, p.solenoidRechargeTicks()));
    }

    /** The plasma is lost: everything resets to cold and empty. */
    public void quench() {
        temperature = AMBIENT_TEMP;
        deuterium = 0;
        tritium = 0;
        ash = 0;
        instability = 0;
        fusionPower = 0;
        heatingPower = 0;
        ohmicPower = 0;
        burnRate = 0;
        density = 0;
        mode = Mode.COLD;
    }

    // ------------------------------------------------------------------ pure helpers

    /** Normalised fusion reactivity, 1 at {@link #PEAK_REACTIVITY_TEMP}. */
    public static double reactivity(double temperature) {
        if (temperature <= AMBIENT_TEMP) {
            return 0;
        }
        double x = temperature / PEAK_REACTIVITY_TEMP;
        return x * x * x * Math.exp(3 * (1 - x));
    }

    /** Share of the radio-frequency power the plasma absorbs at this Greenwald fraction. */
    public static double rfCoupling(Params p, double density) {
        if (p.rfFullDensity() <= p.rfMinDensity()) {
            return density >= p.rfMinDensity() ? 1 : 0;
        }
        return clamp((density - p.rfMinDensity()) / (p.rfFullDensity() - p.rfMinDensity()), 0, 1);
    }

    /** Density limit, mB per cell, at this field. */
    public static double greenwaldLimit(Params p, double field) {
        return p.greenwaldDensity() * Math.max(0.05, field);
    }

    /** Energy confinement time, ticks. */
    public static double confinementTime(Params p, int majorRadius, double confinement, double density) {
        double densityFactor = Math.pow(clamp(density / REFERENCE_DENSITY_FRACTION, 0.25, 1.5), 0.4);
        return Math.max(1, p.confinementTime() * Math.pow(Math.max(1, majorRadius) / 4.0, 0.8) * Math.max(0.02, confinement) * densityFactor);
    }

    private static double heatCapacity(Params p, int volume, double relativeDensity) {
        return p.heatCapacity() * volume * Math.max(MIN_HEAT_CAPACITY_SHARE, relativeDensity);
    }

    /**
     * Fuelling, mB/t of D plus T, that holds the plasma near the given Greenwald fraction at full
     * field: what pumping takes out each tick. The burn itself is small next to it.
     */
    public static double fuelRateFor(Params p, int volume, double densityFraction) {
        return densityFraction * p.greenwaldDensity() * volume / Math.max(1, p.particleTime());
    }

    /** Whether a tokamak can burn at all, before anyone tries. */
    public enum Outlook {
        /** It can reach burning temperature and stay stable there. */
        CAN_BURN,
        /** Too little coil coverage: a hot plasma would grow unstable and disrupt. */
        UNSTABLE,
        /** Stable, but the reference heating cannot bring the plasma to burning temperature. */
        TOO_COLD
    }

    /**
     * Whether this tokamak can reach burning temperature and stay stable there, with full field, the
     * fuelling for {@code densityFraction} and {@code heating} MW of auxiliary heating. Runs the model
     * itself from a breakdown, so the structure warning answers the same question as the game does.
     */
    public static Outlook outlook(Params p, int volume, int majorRadius, double coilCoverage, double heating,
                                  double densityFraction) {
        if (coilCoverage < STABLE_CONFINEMENT) {
            return Outlook.UNSTABLE;
        }
        PlasmaModel model = new PlasmaModel();
        model.breakdown(10);
        double fuel = fuelRateFor(p, volume, densityFraction);
        boolean burnt = false;
        for (int tick = 0; tick < 2400; tick++) {
            model.tick(p, new Inputs(volume, majorRadius, coilCoverage, 1.0, 0, fuel / 2, fuel / 2, true, heating, 0,
                    0, 1e9, 1e9, false));
            if (model.isDisrupting() || model.instability > 0 && model.temperature >= IGNITION_TEMP) {
                return Outlook.UNSTABLE;
            }
            burnt |= model.temperature >= IGNITION_TEMP && model.fusionPower > 0;
        }
        return burnt ? Outlook.CAN_BURN : Outlook.TOO_COLD;
    }

    // ------------------------------------------------------------------ state

    public boolean hasPlasma() {
        return deuterium + tritium + ash >= MIN_INVENTORY;
    }

    public boolean isDisrupting() {
        return instability >= DISRUPTION_INSTABILITY;
    }

    public boolean isBurning() {
        return mode != Mode.COLD;
    }

    /** Q = fusion power / heating power; infinite when burning with no heating, 0 without fusion. */
    public double gain() {
        if (fusionPower <= 0) {
            return 0;
        }
        return heatingPower <= 1.0e-6 ? Double.POSITIVE_INFINITY : fusionPower / heatingPower;
    }

    public double ashFraction() {
        double inventory = deuterium + tritium + ash;
        return inventory > 0 ? ash / inventory : 0;
    }

    public double getTemperature() { return temperature; }
    public double getDeuterium() { return deuterium; }
    public double getTritium() { return tritium; }
    public double getAsh() { return ash; }
    public double getInstability() { return instability; }
    public double getSolenoidFlux() { return solenoidFlux; }
    public double getFusionPower() { return fusionPower; }
    public double getHeatingPower() { return heatingPower; }
    public double getOhmicPower() { return ohmicPower; }
    public double getBurnRate() { return burnRate; }
    public double getDensity() { return density; }
    public double getConfinement() { return confinement; }
    public double getTritiumBreedingRatio() { return tritiumBreedingRatio; }
    public Mode getMode() { return mode; }

    /** Restores saved state; values are clamped to sane ranges. */
    public void restore(double temperature, double deuterium, double tritium, double ash, double instability, double solenoidFlux) {
        this.temperature = clamp(temperature, AMBIENT_TEMP, MAX_TEMP);
        this.deuterium = Math.max(0, deuterium);
        this.tritium = Math.max(0, tritium);
        this.ash = Math.max(0, ash);
        this.instability = clamp(instability, 0, DISRUPTION_INSTABILITY - 1);
        this.solenoidFlux = clamp(solenoidFlux, 0, 1);
    }

    /** Restores the last tick's results, so the readings survive a reload. */
    public void restoreResults(double fusionPower, double heatingPower, double burnRate, double density, double confinement,
                               double tritiumBreedingRatio, Mode mode) {
        this.fusionPower = Math.max(0, fusionPower);
        this.heatingPower = Math.max(0, heatingPower);
        this.burnRate = Math.max(0, burnRate);
        this.density = Math.max(0, density);
        this.confinement = clamp(confinement, 0, 1);
        this.tritiumBreedingRatio = Math.max(0, tritiumBreedingRatio);
        this.mode = mode == null ? Mode.COLD : mode;
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
