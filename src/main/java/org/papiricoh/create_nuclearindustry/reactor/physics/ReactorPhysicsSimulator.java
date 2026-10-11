package org.papiricoh.create_nuclearindustry.reactor.physics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Handles all nuclear physics calculations for the reactor.
 * Simulates neutron production/consumption, temperature changes, and fuel depletion.
 */
public class ReactorPhysicsSimulator {

    // Physics constants
    private static final double BASE_FISSION_RATE = 4.0;            // neutrons per uranium rod per tick
    private static final double HEAT_GENERATION_RATE = 0.035;       // °C per neutron
    private static final double PASSIVE_HEAT_DISSIPATION = 0.8;     // °C per tick (baseline)
    private static final double FUEL_CONSUMPTION_RATE = 0.002;      // fuel units consumed per neutron
    private static final double STEAM_TEMPERATURE_FACTOR = 0.025;   // mB/t from temperature above boiling
    private static final double STEAM_NEUTRON_FACTOR = 0.055;       // mB/t from neutron activity
    private static final double STEAM_PER_ROD_MULTIPLIER = 0.5;     // steam output scales with uranium rod count

    // Temperature thresholds
    private static final double SAFE_TEMP = 500.0;                  // Below this = safe operation
    private static final double WARNING_TEMP = 2000.0;              // Yellow warning zone
    private static final double DANGER_TEMP = 3500.0;               // Red danger zone
    private static final double MELTDOWN_TEMP = 4000.0;             // Absolute limit

    // Limits
    private static final double MAX_NEUTRON_LEVEL = 2000.0;
    private static final double MIN_NEUTRON_LEVEL = 0.0;
    /** Units per assembly before the enrichment-based fuel model; used for old saves. */
    private static final double LEGACY_FUEL_PER_ASSEMBLY = 100.0;
    private static final float LEGACY_FUEL_ENRICHMENT = 20.0f;
    private static final double MAX_THERMAL_STRESS = 100.0;
    private static final double STRESS_START_TEMP = 2500.0;
    private static final double STRESS_MAX_TEMP = 3000.0;
    private static final double STRESS_LOG_CURVE = 9.0;

    // State variables
    private double coreTemperature;                                 // Celsius (0-4000)
    private double neutronLevel;                                    // 0-MAX_NEUTRON_LEVEL
    private final Deque<FuelAssembly> assemblies = new ArrayDeque<>(); // loaded fuel, burnt front to back
    private final List<SpentAssembly> spentAssemblies = new ArrayList<>(); // assemblies burnt out since last drain
    private float controlRodInsertion;                              // 0.0 = withdrawn, 1.0 = fully inserted
    private int uraniumRodCount;                                    // Number of fuel rods in reactor
    private int controlRodCount;                                    // Number of control rods (PHYSICAL blocks)
    private double moderation;                                      // 0-1, graphite beside the fuel columns
    private double reflection;                                      // 0-1, beryllium lining the core's inner ring
    private int meltdownTickCounter;                                // Ticks spent above meltdown temp
    private double thermalStress;                                   // 0-100, logarithmic heat stress
    private boolean thermalExcursionActive;                         // True while stress is forcing a power excursion
    private final XenonModel poisons = new XenonModel();            // iodine-135 / xenon-135
    private final DecayHeatModel decayHeat = new DecayHeatModel();  // heat of the fission products
    private double coolantFeedback = 1.0;                           // moderation by a pressurized loop's water

    // Derived values for display
    private double powerOutput;                                     // MW equivalent
    private double steamGenerationRate;                             // mB per tick

    public ReactorPhysicsSimulator(int uraniumRodCount) {
        this(uraniumRodCount, 0);
    }

    public ReactorPhysicsSimulator(int uraniumRodCount, int controlRodCount) {
        this(uraniumRodCount, controlRodCount, 0.0, 0.0);
    }

    /**
     * @param moderation how well graphite surrounds the fuel columns, 0-1
     * @param reflection how much of the core's inner ring is beryllium reflector, 0-1
     */
    public ReactorPhysicsSimulator(int uraniumRodCount, int controlRodCount, double moderation, double reflection) {
        this.coreTemperature = 20.0;                                // Start at room temperature
        this.neutronLevel = 0.0;
        this.controlRodInsertion = 1.0f;                            // Start fully inserted (safe state)
        this.uraniumRodCount = uraniumRodCount;
        this.controlRodCount = controlRodCount;
        this.moderation = clamp(moderation, 0.0, 1.0);
        this.reflection = clamp(reflection, 0.0, 1.0);
        this.meltdownTickCounter = 0;
        this.thermalStress = 0.0;
        this.thermalExcursionActive = false;
        this.powerOutput = 0.0;
        this.steamGenerationRate = 0.0;
    }

    /**
     * Simulates one tick of reactor operation.
     * @return true if reactor is functioning normally, false if melted down
     */
    public boolean tick() {
        if (!hasFuel()) {
            // No fuel, shut down; what the old fuel left behind still decays and heats
            neutronLevel = Math.max(0, neutronLevel - 8.0);
            coreTemperature = Math.max(20.0, coreTemperature - PASSIVE_HEAT_DISSIPATION * 2.5 + decayHeat.heat());
            updateFissionProducts(0.0);
            thermalExcursionActive = false;
            thermalStress = calculateThermalStress();
            powerOutput = 0.0;
            steamGenerationRate = 0.0;
            return true;
        }

        // Calculate neutron production from fission
        double neutronProduction = calculateNeutronProduction();

        // Calculate neutron consumption from fuel; a reflector bounces part of the leakage back
        double neutronConsumption = uraniumRodCount * FUEL_CONSUMPTION_RATE * neutronLevel * getLeakageFactor();

        // Update neutron level
        neutronLevel += (neutronProduction - neutronConsumption);
        neutronLevel = Math.max(MIN_NEUTRON_LEVEL, Math.min(MAX_NEUTRON_LEVEL, neutronLevel));

        // Calculate heat changes (MOX runs hotter); fission products keep heating after fission stops
        double fissionHeat = neutronLevel * HEAT_GENERATION_RATE * getFuelHeatFactor();
        double heatGeneration = fissionHeat + decayHeat.heat();
        double heatDissipation = calculateHeatDissipation();
        updateFissionProducts(fissionHeat);

        // Update temperature
        coreTemperature += (heatGeneration - heatDissipation);
        coreTemperature = Math.max(0, coreTemperature);

        updateThermalStress();
        applyThermalExcursion();

        // Consume fuel based on operation
        if (neutronLevel > 0) {
            burn(Config.uraniumBurnPerNeutron * neutronLevel);
        }

        // Calculate power output
        powerOutput = neutronLevel * 0.5; // 0.5 MW per neutron level unit

        steamGenerationRate = calculateSteamGenerationRate();

        // Check for meltdown
        if (coreTemperature > MELTDOWN_TEMP) {
            meltdownTickCounter++;
            if (meltdownTickCounter > 200) {
                // Meltdown occurs
                return false;
            }
        } else {
            meltdownTickCounter = 0;
        }

        return true;
    }

    /**
     * Calculates neutron production based on fission reactions.
     * Depends on the RATIO of control rods to uranium rods.
     */
    private double calculateNeutronProduction() {
        if (uraniumRodCount == 0) {
            return 0;
        }

        // If no control rods are present, there's nothing to absorb neutrons - MELTDOWN RISK
        if (controlRodCount == 0) {
            // With no physical control rods, neutron absorption is 0 regardless of position
            return uraniumRodCount * BASE_FISSION_RATE * getModerationFactor() * (1.0 - getXenonPenalty()) * coolantFeedback;
        }

        // The ratio of control rods to uranium rods determines absorption capacity
        // 1.0 ratio = balanced (1 control per 1 uranium)
        // > 1.0 = overcontrolled (can stop reaction)
        // < 1.0 = undercontrolled (reaction hard to control)
        double actualAbsorption = getMaxAbsorption() * controlRodInsertion;
        double absorptionFactor = 1.0 - actualAbsorption;

        // Base fission multiplied by number of uranium rods and absorption factor
        return uraniumRodCount * BASE_FISSION_RATE * absorptionFactor * getFuelReactivity() * getModerationFactor()
                * (1.0 - getXenonPenalty()) * coolantFeedback;
    }

    /**
     * Multiplier on neutron production from the water of a pressurized loop: steam voids, an uncovered
     * core and hot water moderate less. 1 for a reactor that boils its water directly, which is the default.
     */
    public void setCoolantFeedback(double feedback) {
        this.coolantFeedback = Double.isFinite(feedback) ? Math.max(0.0, feedback) : 1.0;
    }

    public double getCoolantFeedback() {
        return coolantFeedback;
    }

    /** Advances iodine, xenon and decay heat by one tick, given this tick's fission heat. */
    private void updateFissionProducts(double fissionHeat) {
        poisons.tick(neutronLevel / MAX_NEUTRON_LEVEL, Config.iodineHalfLifeTicks, Config.xenonHalfLifeTicks);
        decayHeat.tick(fissionHeat, Config.decayHeatFraction, Config.decayHeatTimeTicks);
    }

    /** Fraction of neutron production xenon-135 currently swallows. */
    public double getXenonPenalty() {
        return Config.xenonPoisoning ? poisons.penalty(Config.xenonWorth) : 0.0;
    }

    /** Xenon-135 in the core, normalised to the flux (about 0.25 at full power, more after a shutdown). */
    public double getXenon() {
        return poisons.xenon();
    }

    public double getIodine() {
        return poisons.iodine();
    }

    /** Decay heat, in degrees per tick. */
    public double getDecayHeat() {
        return decayHeat.heat();
    }

    /** Decay heat in MW, on the same scale as {@link #getPowerOutput()}. */
    public double getDecayHeatPower() {
        return decayHeat.heat() / HEAT_GENERATION_RATE * 0.5;
    }

    /**
     * The core's multiplication relative to a bare, clean core of low-enriched fuel, regardless of
     * the rods: fuel enrichment, graphite and beryllium raise it, xenon and a voided pressurized loop lower it.
     */
    public double getReactivity() {
        return getFuelReactivity() * getModerationFactor() * (1.0 - getXenonPenalty()) * coolantFeedback / getLeakageFactor();
    }

    /** Sets the fission products directly, for tests and commands. */
    public void setFissionProducts(double iodine, double xenon, double decayHeatPerTick) {
        poisons.set(iodine, xenon);
        decayHeat.set(decayHeatPerTick);
    }

    /** Multiplier on neutron production from graphite beside the fuel columns. */
    public double getModerationFactor() {
        return 1.0 + Config.graphiteModeratorBonus * moderation;
    }

    /** Multiplier on neutron losses: below 1 when beryllium lines the core. */
    public double getLeakageFactor() {
        return 1.0 - Config.berylliumReflectorBonus * reflection;
    }

    /** How well graphite surrounds the fuel columns, 0-1. */
    public double getModeration() {
        return moderation;
    }

    /** How much of the core's inner ring is beryllium reflector, 0-1. */
    public double getReflection() {
        return reflection;
    }

    /**
     * Multiplier on neutron production from the enrichment of the loaded fuel, weighted
     * by the units each assembly still holds. Low-enriched fuel is the 1.0 reference.
     */
    public double getFuelReactivity() {
        double units = 0.0;
        double weighted = 0.0;
        for (FuelAssembly assembly : assemblies) {
            units += assembly.units;
            weighted += assembly.units * assembly.reactivity;
        }
        return units <= 0.0 ? 1.0 : weighted / units;
    }

    /**
     * Fraction of neutrons the control rods absorb when fully inserted: the ratio of control rods
     * to fuel channels times their boron, capped at 100%. MOX lowers the rods' worth inside the
     * cap, so it needs more rods while a full SCRAM can still shut it down.
     */
    public double getMaxAbsorption() {
        if (uraniumRodCount == 0 || controlRodCount == 0) {
            return 0.0;
        }
        double rodRatio = (double) controlRodCount / uraniumRodCount;
        return Math.min(1.0, rodRatio * Config.boronAbsorption * getControlWorth());
    }

    /** Heat per neutron relative to uranium, weighted by the units each assembly still holds. */
    public double getFuelHeatFactor() {
        return weightedByUnits(assembly -> assembly.kind.heatFactor());
    }

    /** Control rod worth relative to uranium, weighted by the units each assembly still holds. */
    public double getControlWorth() {
        return weightedByUnits(assembly -> assembly.kind.controlWorth());
    }

    private double weightedByUnits(java.util.function.ToDoubleFunction<FuelAssembly> value) {
        double units = 0.0;
        double weighted = 0.0;
        for (FuelAssembly assembly : assemblies) {
            units += assembly.units;
            weighted += assembly.units * value.applyAsDouble(assembly);
        }
        return units <= 0.0 ? 1.0 : weighted / units;
    }

    /** Units of burn a fresh assembly of this enrichment provides. */
    public static double fuelUnitsFor(float enrichment) {
        return Config.fuelUnitsPerAssembly * Math.pow(Math.max(EnrichmentTier.LOW.min(), enrichment) / EnrichmentTier.LOW.nominal(), 0.66);
    }

    public static double reactivityFor(float enrichment) {
        double ratio = Math.max(EnrichmentTier.LOW.min(), enrichment) / EnrichmentTier.LOW.nominal();
        return Math.max(0.25, 1.0 + Config.enrichmentReactivity * (Math.log(ratio) / Math.log(2.0)));
    }

    private void burn(double amount) {
        while (amount > 0.0 && !assemblies.isEmpty()) {
            FuelAssembly head = assemblies.peekFirst();
            double used = Math.min(amount, head.units);
            head.units -= used;
            amount -= used;
            if (head.units <= 1.0e-9) {
                assemblies.pollFirst();
                spentAssemblies.add(new SpentAssembly(head.kind, head.enrichment));
            }
        }
    }

    /**
     * Calculates heat dissipation from the reactor.
     */
    private double calculateHeatDissipation() {
        // Passive dissipation based on temperature difference from environment
        double tempDifference = coreTemperature - 20.0; // Assume 20°C environment
        double passiveDissipation = PASSIVE_HEAT_DISSIPATION * (tempDifference / 100.0);

        // Heat dissipation increases with temperature (radiation loss)
        double radiationLoss = Math.pow(coreTemperature / 3600.0, 2) * 0.1;

        // At high temperatures, additional dissipation is reduced (thermodynamics)
        if (coreTemperature > WARNING_TEMP) {
            return passiveDissipation + radiationLoss;
        }

        return passiveDissipation;
    }

    private void updateThermalStress() {
        thermalStress = calculateThermalStress();
        if (thermalStress >= MAX_THERMAL_STRESS) {
            thermalExcursionActive = true;
        }
    }

    private double calculateThermalStress() {
        if (coreTemperature <= STRESS_START_TEMP) {
            return 0.0;
        }
        if (coreTemperature > STRESS_MAX_TEMP) {
            return MAX_THERMAL_STRESS;
        }

        double tempFactor = Math.min(
                (coreTemperature - STRESS_START_TEMP) / (STRESS_MAX_TEMP - STRESS_START_TEMP),
                Math.nextDown(1.0)
        );
        double logarithmicFactor = Math.log1p(tempFactor * STRESS_LOG_CURVE) / Math.log1p(STRESS_LOG_CURVE);
        return clamp(logarithmicFactor * MAX_THERMAL_STRESS, 0.0, MAX_THERMAL_STRESS);
    }

    private void applyThermalExcursion() {
        if (!thermalExcursionActive || !hasFuel()) {
            return;
        }

        if (controlRodInsertion >= 0.95f) {
            thermalExcursionActive = false;
            return;
        }

        double withdrawn = 1.0 - controlRodInsertion;
        neutronLevel = Math.min(MAX_NEUTRON_LEVEL, neutronLevel + 12.0 + withdrawn * 38.0);
        coreTemperature += 8.0 + withdrawn * 42.0;
    }

    private double calculateSteamGenerationRate() {
        if (coreTemperature <= 100.0) {
            return 0.0;
        }

        double temperatureSteam = (coreTemperature - 100.0) * STEAM_TEMPERATURE_FACTOR;
        double neutronSteam = neutronLevel * STEAM_NEUTRON_FACTOR;
        return (temperatureSteam + neutronSteam) * getSteamRodMultiplier();
    }

    /**
     * Steam output scales with reactor size. Cooling from the coolant loop is
     * normalized by this same factor so core thermodynamics stay size-independent.
     */
    public double getSteamRodMultiplier() {
        return Math.max(1.0, uraniumRodCount * STEAM_PER_ROD_MULTIPLIER);
    }

    /**
     * Sets the control rod position.
     * @param position 0.0 = fully inserted (maximum neutron absorption), 1.0 = fully withdrawn
     */
    public void setControlRodPosition(float position) {
        setControlRodInsertion(1.0f - position);
    }

    public void setControlRodInsertion(float insertion) {
        this.controlRodInsertion = Math.max(0.0f, Math.min(1.0f, insertion));
    }

    /**
     * Changes control rod position by a given amount.
     * Used when moving the rod with Create contraptions.
     */
    public void moveControlRod(float delta) {
        setControlRodPosition(getControlRodPosition() + delta);
    }

    public void scramTick() {
        controlRodInsertion = 1.0f;
        neutronLevel = Math.max(0.0, neutronLevel - 60.0);
        coreTemperature = Math.max(20.0, coreTemperature - PASSIVE_HEAT_DISSIPATION * 4.0 + decayHeat.heat());
        updateFissionProducts(0.0);
        thermalExcursionActive = false;
        thermalStress = calculateThermalStress();
        powerOutput = 0.0;
        steamGenerationRate = 0.0;
    }

    public void applyExternalCooling(double cooling, double neutronModeration) {
        coreTemperature = Math.max(20.0, coreTemperature - Math.max(0.0, cooling));
        neutronLevel = Math.max(MIN_NEUTRON_LEVEL, neutronLevel - Math.max(0.0, neutronModeration));
    }

    public void applyExternalHeat(double heat) {
        coreTemperature = Math.max(20.0, coreTemperature + Math.max(0.0, heat));
    }

    public boolean hasFuel() {
        return getFuelRemaining() > 0.0;
    }

    /** Each fuel channel block holds one assembly. */
    public boolean canLoadFuelAssembly() {
        return assemblies.size() < uraniumRodCount;
    }

    public boolean loadFuelAssembly(float enrichment) {
        return loadFuelAssembly(FuelKind.URANIUM, enrichment);
    }

    /** Loads one fresh assembly; MOX is loaded at its equivalent enrichment. */
    public boolean loadFuelAssembly(FuelKind kind, float enrichment) {
        if (!canLoadFuelAssembly()) {
            return false;
        }
        double units = fuelUnitsFor(enrichment);
        assemblies.addLast(new FuelAssembly(kind, enrichment, units, units, reactivityFor(enrichment) * kind.reactivityBonus()));
        return true;
    }

    /**
     * Moves loaded fuel over from the simulator of a previous structure. Assemblies that no
     * longer fit (fewer fuel channels) are reported as spent rather than lost.
     */
    public void inheritFuel(ReactorPhysicsSimulator previous) {
        for (FuelAssembly assembly : previous.assemblies) {
            if (assemblies.size() < uraniumRodCount) {
                assemblies.addLast(assembly);
            } else {
                spentAssemblies.add(new SpentAssembly(assembly.kind, assembly.enrichment));
            }
        }
        spentAssemblies.addAll(previous.spentAssemblies);
        previous.assemblies.clear();
        previous.spentAssemblies.clear();
    }

    /**
     * Carries the core's heat and activity over when the structure is rebuilt, so swapping a
     * shell block cannot cool the core down instantly.
     */
    public void inheritThermalState(ReactorPhysicsSimulator previous) {
        coreTemperature = previous.coreTemperature;
        neutronLevel = previous.neutronLevel;
        controlRodInsertion = previous.controlRodInsertion;
        meltdownTickCounter = previous.meltdownTickCounter;
        thermalStress = previous.thermalStress;
        thermalExcursionActive = previous.thermalExcursionActive;
        poisons.set(previous.poisons.iodine(), previous.poisons.xenon());
        decayHeat.set(previous.decayHeat.heat());
        coolantFeedback = previous.coolantFeedback;
    }

    /** Every assembly burnt out since the last call. */
    public List<SpentAssembly> drainSpent() {
        if (spentAssemblies.isEmpty()) {
            return List.of();
        }
        List<SpentAssembly> spent = List.copyOf(spentAssemblies);
        spentAssemblies.clear();
        return spent;
    }

    /** Enrichment of every assembly burnt out since the last call. */
    public List<Float> drainSpentAssemblies() {
        return drainSpent().stream().map(SpentAssembly::enrichment).toList();
    }

    /**
     * Every loaded assembly, in burn order (the first is the one burning), with how much of it is
     * burnt: 0 fresh, 1 spent. Read-only; for showing the fuel in the channels.
     */
    public List<AssemblyView> assemblyViews() {
        List<AssemblyView> views = new ArrayList<>(assemblies.size());
        for (FuelAssembly assembly : assemblies) {
            double burnt = assembly.initialUnits > 0 ? 1.0 - assembly.units / assembly.initialUnits : 1.0;
            views.add(new AssemblyView(assembly.kind, clamp(burnt, 0.0, 1.0)));
        }
        return views;
    }

    /** A loaded assembly as {@link #assemblyViews()} shows it. */
    public record AssemblyView(FuelKind kind, double burnFraction) {}

    public int getLoadedAssemblyCount() {
        return assemblies.size();
    }

    /** Enrichment averaged over the units still in the core, or 0 when empty. */
    public float getAverageFuelEnrichment() {
        double units = 0.0;
        double weighted = 0.0;
        for (FuelAssembly assembly : assemblies) {
            units += assembly.units;
            weighted += assembly.units * assembly.enrichment;
        }
        return units <= 0.0 ? 0.0f : (float) (weighted / units);
    }

    public void clearFuel() {
        assemblies.clear();
    }

    /** Loaded assemblies at their initial size, plus empty channels at the reference size. */
    public double getFuelCapacity() {
        double capacity = 0.0;
        for (FuelAssembly assembly : assemblies) {
            capacity += assembly.initialUnits;
        }
        return capacity + Math.max(0, uraniumRodCount - assemblies.size()) * Config.fuelUnitsPerAssembly;
    }

    public void forceScramForLoad() {
        controlRodInsertion = 1.0f;
        neutronLevel = Math.min(neutronLevel, 50.0);
        coreTemperature = Math.min(coreTemperature, DANGER_TEMP);
        meltdownTickCounter = 0;
        thermalExcursionActive = false;
        thermalStress = calculateThermalStress();
        powerOutput = 0.0;
        steamGenerationRate = 0.0;
    }

    // Getters for display and monitoring
    public double getCoreTemperature() {
        return coreTemperature;
    }

    public double getNeutronLevel() {
        return neutronLevel;
    }

    public static double getMaxNeutronLevel() {
        return MAX_NEUTRON_LEVEL;
    }

    public double getFuelRemaining() {
        double units = 0.0;
        for (FuelAssembly assembly : assemblies) {
            units += assembly.units;
        }
        return units;
    }

    public float getControlRodPosition() {
        return 1.0f - controlRodInsertion;
    }

    public float getControlRodInsertion() {
        return controlRodInsertion;
    }

    public int getControlRodCount() {
        return controlRodCount;
    }

    public int getUraniumRodCount() {
        return uraniumRodCount;
    }

    public double getPowerOutput() {
        return powerOutput;
    }

    public double getSteamGenerationRate() {
        return steamGenerationRate;
    }

    public int getMeltdownTickCounter() {
        return meltdownTickCounter;
    }

    public double getThermalStress() {
        return thermalStress;
    }

    public boolean isThermalExcursionActive() {
        return thermalExcursionActive;
    }

    public boolean isMeltingDown() {
        return coreTemperature > MELTDOWN_TEMP;
    }

    public boolean isCritical() {
        return coreTemperature > WARNING_TEMP;
    }

    public boolean isRunning() {
        return neutronLevel > 1.0 && hasFuel();
    }

    /**
     * Gets the reactor state as a string for debugging.
     */
    public ReactorState getState() {
        if (coreTemperature > MELTDOWN_TEMP) {
            return ReactorState.MELTING;
        } else if (coreTemperature > DANGER_TEMP) {
            return ReactorState.CRITICAL;
        } else if (coreTemperature > WARNING_TEMP) {
            return ReactorState.WARNING;
        } else if (neutronLevel > 1.0) {
            return ReactorState.RUNNING;
        } else {
            return ReactorState.IDLE;
        }
    }

    // NBT Serialization for saving state
    public void serializeNBT(CompoundTag tag) {
        tag.putDouble("coreTemperature", coreTemperature);
        tag.putDouble("neutronLevel", neutronLevel);
        ListTag loaded = new ListTag();
        for (FuelAssembly assembly : assemblies) {
            CompoundTag entry = new CompoundTag();
            entry.putFloat("enrichment", assembly.enrichment);
            entry.putDouble("units", assembly.units);
            entry.putDouble("initialUnits", assembly.initialUnits);
            entry.putDouble("reactivity", assembly.reactivity);
            entry.putString("type", assembly.kind.id());
            loaded.add(entry);
        }
        tag.put("assemblies", loaded);
        ListTag spent = new ListTag();
        for (SpentAssembly assembly : spentAssemblies) {
            CompoundTag entry = new CompoundTag();
            entry.putFloat("enrichment", assembly.enrichment());
            entry.putString("type", assembly.kind().id());
            spent.add(entry);
        }
        tag.put("spentAssemblies", spent);
        tag.putFloat("controlRodInsertion", controlRodInsertion);
        tag.putInt("uraniumRodCount", uraniumRodCount);
        tag.putInt("controlRodCount", controlRodCount);
        tag.putDouble("moderation", moderation);
        tag.putDouble("reflection", reflection);
        tag.putDouble("iodine", poisons.iodine());
        tag.putDouble("xenon", poisons.xenon());
        tag.putDouble("decayHeat", decayHeat.heat());
        tag.putInt("meltdownTickCounter", meltdownTickCounter);
        tag.putDouble("thermalStress", thermalStress);
        tag.putBoolean("thermalExcursionActive", thermalExcursionActive);
    }

    public void deserializeNBT(CompoundTag tag) {
        this.coreTemperature = clamp(tag.getDouble("coreTemperature"), 20.0, MELTDOWN_TEMP - 1.0);
        this.neutronLevel = clamp(tag.getDouble("neutronLevel"), MIN_NEUTRON_LEVEL, MAX_NEUTRON_LEVEL);
        this.controlRodInsertion = tag.contains("controlRodInsertion")
                ? tag.getFloat("controlRodInsertion")
                : 1.0f - tag.getFloat("controlRodPosition");
        this.controlRodInsertion = Math.max(0.0f, Math.min(1.0f, controlRodInsertion));
        this.uraniumRodCount = Math.max(0, tag.getInt("uraniumRodCount"));
        this.controlRodCount = Math.max(0, tag.getInt("controlRodCount"));
        this.moderation = clamp(tag.getDouble("moderation"), 0.0, 1.0);
        this.reflection = clamp(tag.getDouble("reflection"), 0.0, 1.0);
        poisons.set(tag.getDouble("iodine"), tag.getDouble("xenon"));
        decayHeat.set(tag.getDouble("decayHeat"));
        readFuel(tag);
        this.meltdownTickCounter = Math.max(0, Math.min(100, tag.getInt("meltdownTickCounter")));
        this.thermalStress = calculateThermalStress();
        this.thermalExcursionActive = tag.getBoolean("thermalExcursionActive") && thermalStress >= MAX_THERMAL_STRESS;
    }

    private void readFuel(CompoundTag tag) {
        assemblies.clear();
        spentAssemblies.clear();
        if (tag.contains("assemblies", Tag.TAG_LIST)) {
            for (Tag element : tag.getList("assemblies", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) element;
                double units = Math.max(0.0, entry.getDouble("units"));
                if (units > 0.0 && assemblies.size() < uraniumRodCount) {
                    double initial = Math.max(units, entry.getDouble("initialUnits"));
                    double reactivity = entry.contains("reactivity") ? entry.getDouble("reactivity") : 1.0;
                    assemblies.addLast(new FuelAssembly(FuelKind.fromId(entry.getString("type")), entry.getFloat("enrichment"),
                            units, initial, reactivity));
                }
            }
            for (Tag element : tag.getList("spentAssemblies", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) element;
                spentAssemblies.add(new SpentAssembly(FuelKind.fromId(entry.getString("type")), entry.getFloat("enrichment")));
            }
            return;
        }
        // Saves from before enrichment mattered held a single fuel counter. Split it back into
        // 100-unit assemblies of the old 20% fuel, at neutral reactivity so nothing heats up on load.
        double legacy = clamp(tag.getDouble("fuelRemaining"), 0.0, uraniumRodCount * LEGACY_FUEL_PER_ASSEMBLY);
        while (legacy > 1.0e-9 && assemblies.size() < uraniumRodCount) {
            double units = Math.min(LEGACY_FUEL_PER_ASSEMBLY, legacy);
            assemblies.addLast(new FuelAssembly(FuelKind.URANIUM, LEGACY_FUEL_ENRICHMENT, units, LEGACY_FUEL_PER_ASSEMBLY, 1.0));
            legacy -= units;
        }
    }

    /** One fuel assembly loaded in a channel. */
    private static final class FuelAssembly {
        private final FuelKind kind;
        private final float enrichment;
        private final double initialUnits;
        private final double reactivity;
        private double units;

        private FuelAssembly(FuelKind kind, float enrichment, double units, double initialUnits, double reactivity) {
            this.kind = kind;
            this.enrichment = enrichment;
            this.units = units;
            this.initialUnits = initialUnits;
            this.reactivity = reactivity;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Enum representing reactor operational states.
     */
    public enum ReactorState {
        IDLE("Idle"),
        FUELING("Fuel queued"),
        SUPPRESSED("Suppressed"),
        RUNNING("Running"),
        WARNING("Warning"),
        CRITICAL("Critical"),
        MELTING("MELTING");

        private final String displayName;

        ReactorState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
