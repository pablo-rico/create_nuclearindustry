package org.papiricoh.create_nuclearindustry.reactor.pwr.containment;

/**
 * The atmosphere of a sealed containment building, as an ideal gas mixture of air (nitrogen and
 * oxygen), steam and hydrogen at one temperature, plus the radioactivity carried in it.
 * <p>
 * Steam from a leak heats and pressurizes the room, then condenses on the walls (more wall, faster)
 * and the gas cools back towards the walls' temperature. Hydrogen from hot cladding builds up until
 * passive recombiners take it out, or it burns: a deflagration turns it into steam and the heat of
 * the burn spikes the pressure. Steam above about 55% inerts the mixture so it cannot burn. A
 * filtered vent lets gas, and the activity in it, out while the pressure is above the outside's.
 * One block is one cubic metre; pressures are absolute, in bar. Kept free of game classes so it can
 * be unit tested.
 */
public final class ContainmentModel {
    /** Gas constant, J/(mol·K). */
    public static final double R = 8.314;
    /** Outside air pressure, bar. */
    public static final double ATMOSPHERE = 1.01325;
    /** Temperature of the walls and of the air the containment starts with, K. */
    public static final double WALL_TEMPERATURE = 293.15;
    /** Lowest hydrogen fraction that can burn. */
    public static final double LOWER_FLAMMABILITY = 0.04;
    /** Hydrogen fraction from which any spark burns it. */
    public static final double CERTAIN_IGNITION = 0.08;
    /** Steam fraction that inerts the mixture. */
    public static final double STEAM_INERTING = 0.55;
    /** Oxygen fraction a burn needs. */
    public static final double MIN_OXYGEN = 0.05;
    /** Hydrogen fraction below which recombiners stop working. */
    public static final double RECOMBINER_START = 0.005;
    /** Heat of hydrogen burning to steam, J/mol. */
    public static final double BURN_HEAT = 242_000.0;
    /** Molar heat capacity of the mixture at constant volume, J/(mol·K). */
    public static final double CV = 22.0;
    /** Steam pressure at the walls' temperature, bar: steam above it condenses. */
    private static final double WALL_STEAM_PRESSURE = 0.023;
    private static final double OXYGEN_IN_AIR = 0.21;
    /** Share of a recombiner's heat that stays in the gas; the rest goes into its plates and the walls. */
    private static final double RECOMBINER_HEAT_SHARE = 0.1;
    /** Share of the shortfall below the outside pressure that seeps back in as air each tick: no building is perfectly tight. */
    private static final double IN_LEAKAGE = 0.002;
    private static final double MAX_TEMPERATURE = 3_000.0;

    /**
     * @param condensation  steam condensed per tick, mol per m² of wall per bar of steam above the walls' saturation
     * @param cooling       share of the gas's excess temperature lost per tick per m² of wall per m³ of volume
     * @param recombinerRate hydrogen one recombiner removes per tick at a hydrogen fraction of 1, mol
     * @param ventFlow      share of the gas one open vent lets out per tick per bar above the outside
     * @param deposition    share of the airborne activity that settles on the walls each tick
     */
    public record Params(double condensation, double cooling, double recombinerRate, double ventFlow, double deposition) {}

    /**
     * What changed in one tick.
     *
     * @param ventedActivity activity that left through the vents (before any filter)
     * @param recombined     hydrogen the recombiners removed, mol
     * @param condensed      steam that condensed on the walls, mol
     */
    public record Tick(double ventedActivity, double recombined, double condensed) {}

    private double volume = 1.0;
    private double wallArea;
    private double inert;
    private double oxygen;
    private double steam;
    private double hydrogen;
    private double temperature = WALL_TEMPERATURE;
    private double activity;

    /** A room of {@code volume} m³ with {@code wallArea} m² of wall, full of air at the outside pressure. */
    public static ContainmentModel atmospheric(double volume, double wallArea) {
        ContainmentModel model = new ContainmentModel();
        model.reset(volume, wallArea);
        return model;
    }

    /** Back to fresh air at the outside pressure and the walls' temperature. */
    public void reset(double volume, double wallArea) {
        this.volume = Math.max(1.0, volume);
        this.wallArea = Math.max(0.0, wallArea);
        double air = ATMOSPHERE * 1.0e5 * this.volume / (R * WALL_TEMPERATURE);
        inert = air * (1.0 - OXYGEN_IN_AIR);
        oxygen = air * OXYGEN_IN_AIR;
        steam = 0.0;
        hydrogen = 0.0;
        temperature = WALL_TEMPERATURE;
        activity = 0.0;
    }

    /** The room changed shape but stayed sealed: the same gas now fills the new volume. */
    public void resize(double volume, double wallArea) {
        this.volume = Math.max(1.0, volume);
        this.wallArea = Math.max(0.0, wallArea);
    }

    /** Steam let in at {@code temperatureK}, mixing its heat into the gas. */
    public void addSteam(double moles, double temperatureK) {
        if (moles <= 0 || !Double.isFinite(moles)) {
            return;
        }
        mix(moles, temperatureK);
        steam += moles;
    }

    /** Hydrogen let in at {@code temperatureK}. */
    public void addHydrogen(double moles, double temperatureK) {
        if (moles <= 0 || !Double.isFinite(moles)) {
            return;
        }
        mix(moles, temperatureK);
        hydrogen += moles;
    }

    /** Radioactivity carried into the atmosphere. */
    public void addActivity(double amount) {
        if (amount > 0 && Double.isFinite(amount)) {
            activity += amount;
        }
    }

    private void mix(double moles, double temperatureK) {
        double total = totalMoles();
        double t = Double.isFinite(temperatureK) ? Math.max(1.0, temperatureK) : temperature;
        temperature = Math.min(MAX_TEMPERATURE, (total * temperature + moles * t) / (total + moles));
    }

    /** One tick: condensation, cooling, recombiners, vents and settling activity. */
    public Tick tick(int recombiners, int openVents, Params p) {
        double condensed = 0.0;
        double steamPressure = partialPressure(steam);
        if (steamPressure > WALL_STEAM_PRESSURE) {
            double saturated = WALL_STEAM_PRESSURE * 1.0e5 * volume / (R * temperature);
            condensed = Math.min(Math.max(0.0, steam - saturated), p.condensation() * wallArea * (steamPressure - WALL_STEAM_PRESSURE));
            steam -= condensed;
        }
        double coolShare = Math.min(1.0, p.cooling() * wallArea / volume);
        temperature += (WALL_TEMPERATURE - temperature) * coolShare;

        double recombined = 0.0;
        double fraction = hydrogenFraction();
        if (recombiners > 0 && fraction > RECOMBINER_START && oxygen > 0) {
            recombined = Math.min(Math.min(hydrogen, 2.0 * oxygen), recombiners * p.recombinerRate() * fraction);
            hydrogen -= recombined;
            oxygen -= recombined / 2.0;
            steam += recombined;
            temperature = Math.min(MAX_TEMPERATURE,
                    temperature + RECOMBINER_HEAT_SHARE * recombined * BURN_HEAT / (Math.max(1.0, totalMoles()) * CV));
        }

        double ventedActivity = 0.0;
        double pressure = pressure();
        if (openVents > 0 && pressure > ATMOSPHERE) {
            double share = Math.min(0.5, openVents * p.ventFlow() * (pressure - ATMOSPHERE));
            // never vent below the outside pressure
            share = Math.min(share, 1.0 - ATMOSPHERE / pressure);
            inert *= 1.0 - share;
            oxygen *= 1.0 - share;
            steam *= 1.0 - share;
            hydrogen *= 1.0 - share;
            ventedActivity = activity * share;
            activity -= ventedActivity;
        }
        activity *= 1.0 - Math.max(0.0, Math.min(1.0, p.deposition()));

        pressure = pressure();
        if (pressure < ATMOSPHERE) {
            double air = (ATMOSPHERE - pressure) * IN_LEAKAGE * 1.0e5 * volume / (R * temperature);
            mix(air, WALL_TEMPERATURE);
            inert += air * (1.0 - OXYGEN_IN_AIR);
            oxygen += air * OXYGEN_IN_AIR;
        }
        return new Tick(ventedActivity, recombined, condensed);
    }

    /** Whether the mixture can burn: enough hydrogen and oxygen, and not inerted by steam. */
    public boolean flammable() {
        return hydrogenFraction() >= LOWER_FLAMMABILITY && steamFraction() < STEAM_INERTING && oxygenFraction() >= MIN_OXYGEN;
    }

    /**
     * Chance a flammable mixture lights within a second: none below the lower flammability limit,
     * {@code chancePerSecond} up to {@link #CERTAIN_IGNITION}, and certain from there.
     */
    public static double ignitionChance(double hydrogenFraction, double chancePerSecond) {
        if (hydrogenFraction < LOWER_FLAMMABILITY) {
            return 0.0;
        }
        return hydrogenFraction >= CERTAIN_IGNITION ? 1.0 : Math.max(0.0, Math.min(1.0, chancePerSecond));
    }

    /**
     * Burns the hydrogen, as far as the oxygen allows, into steam; the heat raises the temperature at
     * constant volume. Returns the pressure rise, bar.
     */
    public double burn() {
        double before = pressure();
        double burnt = Math.min(hydrogen, 2.0 * oxygen);
        if (burnt <= 0) {
            return 0.0;
        }
        hydrogen -= burnt;
        oxygen -= burnt / 2.0;
        steam += burnt;
        temperature = Math.min(MAX_TEMPERATURE, temperature + burnt * BURN_HEAT / (Math.max(1.0, totalMoles()) * CV));
        return pressure() - before;
    }

    /**
     * The room opened to the outside: the gas expands out until the pressure is the outside's and the
     * airborne activity goes with it. Returns the activity released.
     */
    public double depressurize() {
        double pressure = pressure();
        if (pressure > ATMOSPHERE) {
            double keep = ATMOSPHERE / pressure;
            inert *= keep;
            oxygen *= keep;
            steam *= keep;
            hydrogen *= keep;
        }
        double released = activity;
        activity = 0.0;
        return released;
    }

    /** Absolute pressure, bar. */
    public double pressure() {
        return totalMoles() * R * temperature / volume / 1.0e5;
    }

    private double partialPressure(double moles) {
        return moles * R * temperature / volume / 1.0e5;
    }

    public double totalMoles() {
        return inert + oxygen + steam + hydrogen;
    }

    public double hydrogenFraction() {
        double total = totalMoles();
        return total <= 0 ? 0.0 : hydrogen / total;
    }

    public double steamFraction() {
        double total = totalMoles();
        return total <= 0 ? 0.0 : steam / total;
    }

    public double oxygenFraction() {
        double total = totalMoles();
        return total <= 0 ? 0.0 : oxygen / total;
    }

    /** Gas temperature, K. */
    public double temperature() {
        return temperature;
    }

    public double activity() {
        return activity;
    }

    public double volume() {
        return volume;
    }

    public double wallArea() {
        return wallArea;
    }

    public double inert() {
        return inert;
    }

    public double oxygen() {
        return oxygen;
    }

    public double steam() {
        return steam;
    }

    public double hydrogen() {
        return hydrogen;
    }

    /** Restores a saved state. */
    public void restore(double volume, double wallArea, double inert, double oxygen, double steam, double hydrogen,
                        double temperature, double activity) {
        this.volume = Math.max(1.0, volume);
        this.wallArea = Math.max(0.0, wallArea);
        this.inert = Math.max(0.0, inert);
        this.oxygen = Math.max(0.0, oxygen);
        this.steam = Math.max(0.0, steam);
        this.hydrogen = Math.max(0.0, hydrogen);
        this.temperature = Double.isFinite(temperature) && temperature > 1.0 ? Math.min(MAX_TEMPERATURE, temperature) : WALL_TEMPERATURE;
        this.activity = Math.max(0.0, activity);
    }
}
