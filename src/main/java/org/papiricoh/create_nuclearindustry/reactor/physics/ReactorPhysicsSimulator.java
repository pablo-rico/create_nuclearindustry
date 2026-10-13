package org.papiricoh.create_nuclearindustry.reactor.physics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.CoreThermalModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.FissionCore;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ReactivityModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * A fission reactor's core: its fuel assemblies and the physics of the chain reaction, on top of
 * {@link FissionCore} (point kinetics with delayed neutrons, reactivity in pcm with temperature and
 * void feedback, fuel and coolant heat, core damage).
 *
 * <p>The block entity tells it each tick how far each control rod channel is in and what the
 * coolant is doing ({@link #setCoolant}), then reads back the power, the heat the coolant took and
 * the temperatures. Power is relative to the core's rated power, which grows with its fuel rods, so
 * a big core and a small one are equally stable. The "core temperature" everything shows is the
 * fuel's, on the same 0-4000 °C scale as always. The states follow the damage thresholds of the
 * config: Warning where the cladding starts to fail, Melting where the fuel melts
 * ({@link FissionConfig#warningTemperature()} and friends).
 */
public class ReactorPhysicsSimulator {
    /** The neutron level shown is the relative power times this: 1000 at rated power. */
    private static final double NEUTRONS_AT_RATED = 1000.0;
    private static final double MAX_NEUTRON_LEVEL = 2000.0;
    /** Below this relative power the chain reaction counts as stopped. */
    private static final double RUNNING_POWER = 1.0e-3;
    /** Units per assembly before the enrichment-based fuel model; used for old saves. */
    private static final double LEGACY_FUEL_PER_ASSEMBLY = 100.0;
    private static final float LEGACY_FUEL_ENRICHMENT = 20.0f;
    /** Saves before the kinetics model are clamped under the cladding failure on load, so an old hot core cannot damage itself at once. */
    private static double legacyTemperatureCap() {
        return FissionConfig.warningTemperature() - 100.0;
    }
    private static final int KINETICS_VERSION = 2;
    /** What a bare simulator is cooled by until a block entity says otherwise: plenty of boiling water. */
    private static final CoreThermalModel.Boundary IDEAL_COOLING =
            new CoreThermalModel.Boiling(1.0, Double.MAX_VALUE / 4, Double.MAX_VALUE / 4);

    private final FissionCore core = new FissionCore();
    private final Deque<FuelAssembly> assemblies = new ArrayDeque<>(); // loaded fuel, burnt front to back
    private final List<SpentAssembly> spentAssemblies = new ArrayList<>(); // assemblies burnt out since last drain
    private float controlRodInsertion;                              // average, 0 withdrawn .. 1 fully inserted
    private double[] channelInsertion = {1.0};                      // per control channel
    private int uraniumRodCount;                                    // fuel rod blocks
    private int controlRodCount;                                    // control channel capacity, blocks
    private double moderation;                                      // 0-1, graphite beside the fuel columns
    private double reflection;                                      // 0-1, beryllium lining the core's inner ring
    private CoreThermalModel.Boundary boundary = IDEAL_COOLING;
    private ReactivityModel.Coolant coolantKind = ReactivityModel.Coolant.WATER;
    private double oxidationHeat;                                   // MW per rod
    private double coolantFeedback = 1.0;                           // the pressurized loop's reading, for display

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
        this.controlRodInsertion = 1.0f;
        this.uraniumRodCount = uraniumRodCount;
        this.controlRodCount = controlRodCount;
        this.moderation = clamp(moderation, 0.0, 1.0);
        this.reflection = clamp(reflection, 0.0, 1.0);
    }

    /**
     * Simulates one tick of reactor operation.
     *
     * @return true while the core holds together, false once it has melted down
     */
    public boolean tick() {
        FissionCore.Params params = FissionConfig.core();
        core.tick(params, inputs());
        double burn = FissionConfig.fuelBurnPerRod * core.power() * uraniumRodCount;
        if (burn > 0) {
            burn(burn);
        }
        return !core.damageModel().meltedDown(params.damage());
    }

    private FissionCore.Inputs inputs() {
        int loaded = assemblies.size();
        double empty = uraniumRodCount <= 0 ? 1 : Math.max(0, uraniumRodCount - loaded) / (double) uraniumRodCount;
        return new FissionCore.Inputs(uraniumRodCount, hasFuel(), getFuelHeatFactor(), weightedByUnits(a -> a.kind.delayedFraction()),
                getFuelReactivity(), getBurntFraction(), weightedByUnits(a -> a.kind == FuelKind.THORIUM ? 1.0 : 0.0), empty,
                moderation, reflection, coolantKind, getMaxAbsorption(), channelInsertion, boundary, oxidationHeat,
                PwrConfig.voidWorth, PwrConfig.temperatureCoefficient);
    }

    /** What cools the core this tick: boiling water, a pressurized loop, or nothing. */
    public void setCoolant(CoreThermalModel.Boundary boundary, ReactivityModel.Coolant kind) {
        this.boundary = boundary == null ? new CoreThermalModel.Dry() : boundary;
        this.coolantKind = kind == null ? ReactivityModel.Coolant.WATER : kind;
    }

    /** Heat of the cladding oxidising, MW per rod, added to the fuel on the next tick. */
    public void setOxidationHeat(double megawattsPerRod) {
        this.oxidationHeat = Double.isFinite(megawattsPerRod) ? Math.max(0, megawattsPerRod) : 0;
    }

    /** The pressurized loop's reading of how its water moderates, for display; the physics has its own feedback. */
    public void setCoolantFeedback(double feedback) {
        this.coolantFeedback = Double.isFinite(feedback) ? Math.max(0.0, feedback) : 1.0;
    }

    public double getCoolantFeedback() {
        return coolantFeedback;
    }

    // ------------------------------------------------------------------ fission products

    /** Share of the core's excess reactivity xenon-135 holds back (about a quarter at full power). */
    public double getXenonPenalty() {
        return getXenonPenaltyPcm() / Math.max(1, FissionConfig.fuelExcess);
    }

    /** Reactivity xenon-135 holds back, pcm. */
    public double getXenonPenaltyPcm() {
        return Config.xenonPoisoning ? core.xenon().penalty(FissionConfig.xenonWorthPcm) : 0.0;
    }

    /** Xenon-135 in the core, normalised to the flux (about 0.25 at full power, more after a shutdown). */
    public double getXenon() {
        return core.xenon().xenon();
    }

    public double getIodine() {
        return core.xenon().iodine();
    }

    /** Decay heat in degrees per tick of the fuel, the scale the decay heat has always been read in. */
    public double getDecayHeat() {
        return core.decay().heat() * FissionConfig.ratedPowerPerRod * FuelKind.URANIUM.heatFactor() * FissionCore.TICK
                / FissionConfig.fuelHeatCapacity;
    }

    /** Decay heat in MW. */
    public double getDecayHeatPower() {
        return core.decay().heat() * getRatedPower();
    }

    /**
     * The core's reactivity relative to a bare, clean core of low-enriched fuel, regardless of the
     * rods, heat and voids: fuel enrichment, graphite and beryllium raise it, xenon lowers it.
     */
    public double getReactivity() {
        ReactivityModel.Breakdown b = core.breakdown();
        double excess = Math.max(1, FissionConfig.fuelExcess);
        if (b.fuel() == 0 && b.moderation() == 0 && b.reflector() == 0) {
            // Not ticked yet: from the fuel alone.
            return (getFuelReactivity() * excess + FissionConfig.graphiteWorth * moderation + FissionConfig.reflectorWorth * reflection
                    - getXenonPenaltyPcm()) / excess;
        }
        double emptyLoss = FissionConfig.emptyChannelWorth * Math.max(0, uraniumRodCount - assemblies.size())
                / (double) Math.max(1, uraniumRodCount);
        return (b.fuel() + emptyLoss + b.moderation() + b.reflector() + b.heavyWater() + b.xenon()) / excess;
    }

    /** Sets the fission products directly, for tests and commands; decay heat in degrees per tick of the fuel. */
    public void setFissionProducts(double iodine, double xenon, double decayHeatPerTick) {
        core.xenon().set(iodine, xenon);
        core.decay().set(Math.max(0, decayHeatPerTick) * FissionConfig.fuelHeatCapacity / FissionCore.TICK
                / Math.max(1.0e-9, FissionConfig.ratedPowerPerRod));
    }

    /** How well graphite surrounds the fuel columns, 0-1. */
    public double getModeration() {
        return moderation;
    }

    /** How much of the core's inner ring is beryllium reflector, 0-1. */
    public double getReflection() {
        return reflection;
    }

    // ------------------------------------------------------------------ fuel

    /**
     * Reactivity of the loaded fuel relative to fresh low-enriched fuel, weighted by the units each
     * assembly still holds. Low-enriched fuel is the 1.0 reference.
     */
    public double getFuelReactivity() {
        double units = 0.0;
        double weighted = 0.0;
        for (FuelAssembly assembly : assemblies) {
            units += assembly.units;
            weighted += assembly.units * reactivityOf(assembly);
        }
        return units <= 0.0 ? 1.0 : weighted / units;
    }

    /** An assembly's reactivity now: thorium grows more reactive as it breeds uranium-233. */
    private static double reactivityOf(FuelAssembly assembly) {
        if (assembly.kind != FuelKind.THORIUM) {
            return assembly.reactivity;
        }
        double burnt = assembly.initialUnits > 0 ? 1.0 - assembly.units / assembly.initialUnits : 1.0;
        return assembly.reactivity * ThoriumBreeding.reactivityMultiplier(burnt);
    }

    /** Share of the loaded fuel burnt, weighted by each assembly's size. */
    public double getBurntFraction() {
        double initial = 0;
        double left = 0;
        for (FuelAssembly assembly : assemblies) {
            initial += assembly.initialUnits;
            left += assembly.units;
        }
        return initial <= 0 ? 0 : clamp(1 - left / initial, 0, 1);
    }

    /**
     * Share of the core's reactivity the control rods can hold back when fully inserted: the ratio of
     * control rods to fuel times their boron, capped at 100%. MOX lowers the rods' worth inside the
     * cap, so it needs more rods while a full SCRAM can still shut it down.
     */
    public double getMaxAbsorption() {
        if (uraniumRodCount == 0 || controlRodCount == 0) {
            return 0.0;
        }
        double rodRatio = (double) controlRodCount / uraniumRodCount;
        return Math.min(1.0, rodRatio * Config.boronAbsorption * getControlWorth());
    }

    /** Heat per unit power relative to uranium, weighted by the units each assembly still holds. */
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
        if (units <= 0.0) {
            return value.applyAsDouble(new FuelAssembly(FuelKind.URANIUM, 5, 1, 1, 1));
        }
        return weighted / units;
    }

    /** Units of burn a fresh assembly of this enrichment provides. */
    public static double fuelUnitsFor(float enrichment) {
        return Config.fuelUnitsPerAssembly * Math.pow(Math.max(EnrichmentTier.LOW.min(), enrichment) / EnrichmentTier.LOW.nominal(), 0.66);
    }

    public static double reactivityFor(float enrichment) {
        double ratio = Math.max(EnrichmentTier.LOW.min(), enrichment) / EnrichmentTier.LOW.nominal();
        return Math.max(0.25, 1.0 + Config.enrichmentReactivity * (Math.log(ratio) / Math.log(2.0)));
    }

    /** Burns this many fuel units now, head assembly first (tests and commands). */
    public void burnFuel(double units) {
        burn(Math.max(0, units));
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

    // ------------------------------------------------------------------ control rods

    /**
     * Sets the control rod position.
     * @param position 0.0 = fully inserted (maximum neutron absorption), 1.0 = fully withdrawn
     */
    public void setControlRodPosition(float position) {
        setControlRodInsertion(1.0f - position);
    }

    /** Every control channel at the same insertion. */
    public void setControlRodInsertion(float insertion) {
        this.controlRodInsertion = Math.max(0.0f, Math.min(1.0f, insertion));
        Arrays.fill(channelInsertion, controlRodInsertion);
    }

    /**
     * How far each control channel is filled, 0..1. A rod is worth most in the middle of the core,
     * so where the rods are matters, not only how many.
     */
    public void setChannelInsertions(double[] insertions) {
        if (insertions == null || insertions.length == 0) {
            channelInsertion = new double[]{controlRodInsertion};
            return;
        }
        channelInsertion = new double[insertions.length];
        double sum = 0;
        for (int i = 0; i < insertions.length; i++) {
            channelInsertion[i] = clamp(insertions[i], 0, 1);
            sum += channelInsertion[i];
        }
        controlRodInsertion = (float) (sum / insertions.length);
    }

    /**
     * Changes control rod position by a given amount.
     * Used when moving the rod with Create contraptions.
     */
    public void moveControlRod(float delta) {
        setControlRodPosition(getControlRodPosition() + delta);
    }

    // ------------------------------------------------------------------ outside influences

    /**
     * A tick of a core whose structure is broken: every rod falls in (the reactor is scrammed), the
     * delayed neutrons die away, the decay heat goes on.
     */
    public void scramTick() {
        setControlRodInsertion(1.0f);
        CoreThermalModel.Boundary saved = boundary;
        boundary = IDEAL_COOLING;
        core.tick(FissionConfig.core(), inputs());
        boundary = saved;
    }

    /** Takes heat out of the fuel and coolant at once, °C, and power out of the chain reaction (on the neutron scale). */
    public void applyExternalCooling(double cooling, double neutronModeration) {
        core.thermal().coolBoth(Math.max(0.0, cooling));
        if (neutronModeration > 0) {
            FissionCore.Params p = FissionConfig.core();
            core.restoreKinetics(p, weightedByUnits(a -> a.kind.delayedFraction()),
                    Math.max(0, core.power() - neutronModeration / NEUTRONS_AT_RATED), core.kinetics().precursors());
        }
    }

    /** Heats the fuel at once, °C (and the coolant too if the core is dry). */
    public void applyExternalHeat(double heat) {
        core.thermal().addToFuel(Math.max(0.0, heat), boundary instanceof CoreThermalModel.Dry);
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

    /** Loads one fresh assembly; MOX and thorium are loaded at their equivalent enrichment. */
    public boolean loadFuelAssembly(FuelKind kind, float enrichment) {
        if (!canLoadFuelAssembly()) {
            return false;
        }
        // Thorium breeds part of what it burns, so it lasts longer than the uranium it is rated as.
        double units = fuelUnitsFor(enrichment) * (kind == FuelKind.THORIUM ? ThoriumBreeding.LIFE_FACTOR : 1.0);
        assemblies.addLast(new FuelAssembly(kind, enrichment, units, units, reactivityFor(enrichment) * kind.reactivityBonus()));
        if (uraniumRodCount > 0) {
            core.damageModel().refuel(1.0 / uraniumRodCount);
        }
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
        CompoundTag state = new CompoundTag();
        previous.writeCore(state);
        readCore(state, true);
        controlRodInsertion = previous.controlRodInsertion;
        channelInsertion = previous.channelInsertion.clone();
        coolantFeedback = previous.coolantFeedback;
        boundary = previous.boundary;
        coolantKind = previous.coolantKind;
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
     * burnt: 0 fresh, 1 spent. Read-only; for showing the fuel in the channels. Builds a new list on
     * each call: ask only when it is used (the channels refresh once a second).
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

    /**
     * A hot reactor just loaded: rods in, the chain reaction stopped, the fuel no hotter than
     * critical (well under melting, so the hot shutdown can hand back to the running state).
     */
    public void forceScramForLoad() {
        setControlRodInsertion(1.0f);
        FissionCore.Params p = FissionConfig.core();
        core.restoreKinetics(p, weightedByUnits(a -> a.kind.delayedFraction()), Math.min(core.power(), 0.05), null);
        double fuel = Math.min(core.fuelTemperature(), FissionConfig.dangerTemperature());
        core.thermal().restore(fuel, Math.min(core.coolantTemperature(), fuel));
    }

    // ------------------------------------------------------------------ readings

    /** Fuel temperature, °C: what the reactor shows as its core temperature. */
    public double getCoreTemperature() {
        return core.fuelTemperature();
    }

    public double getCoolantTemperature() {
        return core.coolantTemperature();
    }

    /** Power on the old neutron scale: 1000 at rated power. */
    public double getNeutronLevel() {
        return core.power() * NEUTRONS_AT_RATED;
    }

    /** Top of the neutron scale (twice rated power). */
    public static double getMaxNeutronLevel() {
        return MAX_NEUTRON_LEVEL;
    }

    /** Power relative to the rated power. */
    public double getRelativePower() {
        return core.power();
    }

    /** Rated thermal power of the core with its fuel, MW. */
    public double getRatedPower() {
        return FissionConfig.ratedPowerPerRod * uraniumRodCount * getFuelHeatFactor();
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

    /** Fission power, MW. */
    public double getPowerOutput() {
        return core.power() * getRatedPower();
    }

    /** Heat the coolant took away last tick, MW. */
    public double getRemovedHeat() {
        return core.thermalResult().removed();
    }

    /** Of it, what boiled water into steam, MW. */
    public double getBoiledHeat() {
        return core.thermalResult().boiled();
    }

    /** Steam the core boils, mB per tick, for display. */
    public double getSteamGenerationRate() {
        return getBoiledHeat() / Math.max(1.0e-9, coolantKind == ReactivityModel.Coolant.HEAVY_WATER
                ? FissionConfig.heavyWaterSteamEnergy : FissionConfig.steamEnergy);
    }

    public double getVoidFraction() {
        return core.voidFraction();
    }

    /** Net reactivity, pcm. */
    public double getNetReactivity() {
        return core.reactivity();
    }

    /**
     * The net reactivity split into what the player can act on: fuel, rods, temperature, voids, xenon,
     * damage. An empty core has no chain reaction to read, so it shows zeros rather than the empty
     * channels' loss and the dry core.
     */
    public ReactivityModel.Parts getReactivityParts() {
        return hasFuel() ? core.breakdown().parts() : ReactivityModel.Parts.NONE;
    }

    /** Reactivity all the rods could still hold back, pcm. */
    public double getShutdownMargin() {
        return core.shutdownMargin();
    }

    /** Reactor period, seconds: how long the power takes to grow by e; infinite when steady. */
    public double getPeriod() {
        return core.period();
    }

    /** Core damage, 0-1. */
    public double getCoreDamage() {
        return core.damage();
    }

    /** How close the fuel is to melting: 0 below cladding failure, 100 where the fuel melts. */
    public double getThermalStress() {
        double start = FissionConfig.claddingFailureTemperature;
        double span = Math.max(1, FissionConfig.fuelMeltingTemperature - start);
        return 100.0 * clamp((core.fuelTemperature() - start) / span, 0, 1);
    }

    /** The fuel is melting. */
    public boolean isThermalExcursionActive() {
        return isFuelMelting();
    }

    public boolean isFuelMelting() {
        return core.fuelTemperature() >= FissionConfig.fuelMeltingTemperature;
    }

    /** The fuel is at its melting point or past it (the Melting state). */
    public boolean isMeltingDown() {
        return core.fuelTemperature() > FissionConfig.meltdownTemperature();
    }

    /** The fuel is hot enough to damage itself (the Warning state and above). */
    public boolean isCritical() {
        return core.fuelTemperature() > FissionConfig.warningTemperature();
    }

    public boolean isRunning() {
        return core.power() > RUNNING_POWER && hasFuel();
    }

    public FissionCore core() {
        return core;
    }

    /**
     * Gets the reactor state as a string for debugging.
     */
    public ReactorState getState() {
        double t = core.fuelTemperature();
        if (t > FissionConfig.meltdownTemperature()) {
            return ReactorState.MELTING;
        } else if (t > FissionConfig.dangerTemperature()) {
            return ReactorState.CRITICAL;
        } else if (t > FissionConfig.warningTemperature()) {
            return ReactorState.WARNING;
        } else if (core.power() > RUNNING_POWER) {
            return ReactorState.RUNNING;
        } else {
            return ReactorState.IDLE;
        }
    }

    // ------------------------------------------------------------------ saving

    public void serializeNBT(CompoundTag tag) {
        writeCore(tag);
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
    }

    /**
     * What a client needs of the core, for an update packet: the readings and a summary of the fuel
     * (how many assemblies, their units, average enrichment and main kind) instead of one entry per
     * assembly, so the packet stays the same size however big the core is. Clients never run the
     * physics, so the delayed-neutron and decay-heat histories are left out too. The save keeps it all
     * ({@link #serializeNBT}).
     */
    public void serializeForClient(CompoundTag tag) {
        writeCore(tag, false);
        CompoundTag fuel = new CompoundTag();
        double units = 0.0;
        double initial = 0.0;
        double enrichment = 0.0;
        double reactivity = 0.0;
        double[] byKind = new double[FuelKind.values().length];
        for (FuelAssembly assembly : assemblies) {
            units += assembly.units;
            initial += assembly.initialUnits;
            enrichment += assembly.units * assembly.enrichment;
            reactivity += assembly.reactivity;
            byKind[assembly.kind.ordinal()] += Math.max(assembly.units, 1.0e-9);
        }
        FuelKind main = FuelKind.URANIUM;
        for (FuelKind kind : FuelKind.values()) {
            if (byKind[kind.ordinal()] > byKind[main.ordinal()]) {
                main = kind;
            }
        }
        int count = assemblies.size();
        fuel.putInt("count", count);
        fuel.putDouble("units", units);
        fuel.putDouble("initialUnits", initial);
        fuel.putFloat("enrichment", units <= 0.0 ? 0.0f : (float) (enrichment / units));
        fuel.putDouble("reactivity", count == 0 ? 1.0 : reactivity / count);
        fuel.putString("type", main.id());
        tag.put("fuelSummary", fuel);
        tag.putFloat("controlRodInsertion", controlRodInsertion);
        tag.putInt("uraniumRodCount", uraniumRodCount);
        tag.putInt("controlRodCount", controlRodCount);
        tag.putDouble("moderation", moderation);
        tag.putDouble("reflection", reflection);
    }

    private void writeCore(CompoundTag tag) {
        writeCore(tag, true);
    }

    /** @param histories whether to keep the delayed-neutron precursors and the decay heat groups (saves) */
    private void writeCore(CompoundTag tag, boolean histories) {
        tag.putInt("kineticsVersion", KINETICS_VERSION);
        tag.putDouble("coreTemperature", core.fuelTemperature());
        tag.putDouble("coolantTemperature", core.coolantTemperature());
        tag.putDouble("voidFraction", core.thermal().voidFraction());
        tag.putDouble("boilingVoid", core.thermal().boilingVoid());
        tag.putDouble("neutronLevel", core.power() * NEUTRONS_AT_RATED);
        tag.putDouble("coreDamage", core.damage());
        tag.putDouble("rodReactivity", core.rodsApplied());
        tag.putDouble("iodine", core.xenon().iodine());
        tag.putDouble("xenon", core.xenon().xenon());
        tag.putDouble("decayHeatRelative", core.decay().heat());
        if (!histories) {
            return;
        }
        double[] precursors = core.kinetics().precursors();
        long[] packed = new long[precursors.length];
        for (int i = 0; i < precursors.length; i++) {
            packed[i] = Double.doubleToLongBits(precursors[i]);
        }
        tag.putLongArray("precursors", packed);
        double[] decayGroups = core.decay().groups();
        long[] packedDecay = new long[decayGroups.length];
        for (int i = 0; i < decayGroups.length; i++) {
            packedDecay[i] = Double.doubleToLongBits(decayGroups[i]);
        }
        tag.putLongArray("decayGroups", packedDecay);
    }

    public void deserializeNBT(CompoundTag tag) {
        this.controlRodInsertion = tag.contains("controlRodInsertion")
                ? tag.getFloat("controlRodInsertion")
                : 1.0f - tag.getFloat("controlRodPosition");
        this.controlRodInsertion = Math.max(0.0f, Math.min(1.0f, controlRodInsertion));
        channelInsertion = new double[]{controlRodInsertion};
        this.uraniumRodCount = Math.max(0, tag.getInt("uraniumRodCount"));
        this.controlRodCount = Math.max(0, tag.getInt("controlRodCount"));
        this.moderation = clamp(tag.getDouble("moderation"), 0.0, 1.0);
        this.reflection = clamp(tag.getDouble("reflection"), 0.0, 1.0);
        readFuel(tag);
        readCore(tag, false);
    }

    /**
     * The chain reaction, temperatures and fission products. Saves from before the kinetics model
     * kept a neutron level on a scale of its own and a single temperature: the temperature is
     * clamped so an old hot core cannot melt the moment it loads, and the power starts low with its
     * delayed neutrons in equilibrium.
     */
    private void readCore(CompoundTag tag, boolean inherited) {
        FissionCore.Params p = FissionConfig.core();
        double delayed = weightedByUnits(a -> a.kind.delayedFraction());
        boolean current = tag.getInt("kineticsVersion") >= KINETICS_VERSION;
        double fuel = Math.max(20.0, tag.getDouble("coreTemperature"));
        double n = Math.max(0, tag.getDouble("neutronLevel")) / NEUTRONS_AT_RATED;
        if (!current) {
            fuel = Math.min(fuel, legacyTemperatureCap());
            n = Math.min(n, 1.0);
        }
        double coolant = tag.contains("coolantTemperature") ? tag.getDouble("coolantTemperature")
                : Math.min(fuel, FissionConfig.boilingTemperature);
        core.thermal().restore(fuel, coolant, tag.getDouble("voidFraction"), tag.getDouble("boilingVoid"));
        double[] precursors = null;
        if (current && tag.contains("precursors", Tag.TAG_LONG_ARRAY)) {
            long[] packed = tag.getLongArray("precursors");
            precursors = new double[packed.length];
            for (int i = 0; i < packed.length; i++) {
                precursors[i] = Double.longBitsToDouble(packed[i]);
            }
        }
        core.restoreKinetics(p, delayed, n, precursors);
        core.damageModel().restore(current ? tag.getDouble("coreDamage") : 0);
        core.restoreRods(current && tag.contains("rodReactivity") ? tag.getDouble("rodReactivity") : Double.NaN);
        core.xenon().set(tag.getDouble("iodine"), tag.getDouble("xenon"));
        if (current) {
            double[] decayGroups = null;
            if (tag.contains("decayGroups", Tag.TAG_LONG_ARRAY)) {
                long[] packed = tag.getLongArray("decayGroups");
                decayGroups = new double[packed.length];
                for (int i = 0; i < packed.length; i++) {
                    decayGroups[i] = Double.longBitsToDouble(packed[i]);
                }
            }
            core.decay().restore(decayGroups, Math.max(0, tag.getDouble("decayHeatRelative")));
        } else if (!inherited) {
            // The old decay heat was in degrees per tick of the single core temperature.
            setFissionProducts(tag.getDouble("iodine"), tag.getDouble("xenon"), Math.max(0, tag.getDouble("decayHeat")));
        }
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
        if (tag.contains("fuelSummary", Tag.TAG_COMPOUND)) {
            readFuelSummary(tag.getCompound("fuelSummary"));
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

    /**
     * A client's copy of the fuel from {@link #serializeForClient}: as many equal assemblies as the
     * core holds, sharing its units, so the totals, the average enrichment and the burnup read the same.
     */
    private void readFuelSummary(CompoundTag fuel) {
        int count = Math.min(Math.max(0, fuel.getInt("count")), uraniumRodCount);
        if (count == 0) {
            return;
        }
        double units = Math.max(0.0, fuel.getDouble("units")) / count;
        double initial = Math.max(units, fuel.getDouble("initialUnits") / count);
        FuelKind kind = FuelKind.fromId(fuel.getString("type"));
        float enrichment = fuel.getFloat("enrichment");
        double reactivity = fuel.getDouble("reactivity");
        for (int i = 0; i < count; i++) {
            assemblies.addLast(new FuelAssembly(kind, enrichment, units, initial, reactivity));
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
