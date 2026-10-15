package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.papiricoh.create_nuclearindustry.reactor.physics.DecayHeatModel;
import org.papiricoh.create_nuclearindustry.reactor.physics.XenonModel;

/**
 * A fission core, one game tick at a time: the control rods set the reactivity, the point kinetics
 * turn it into power, the power heats the fuel and the coolant, and the fuel and coolant
 * temperatures and the steam voids feed back into the reactivity. Xenon, decay heat and core damage
 * follow along. What the fuel is (enrichment, kind, burnup) and what cools the core come in each
 * tick as {@link Inputs}; the block entity and its fuel assemblies stay outside.
 *
 * <p>Two things keep a reactor in hand. Below prompt criticality the power climbs only as fast as
 * the delayed neutrons allow, and the negative temperature and void coefficients pull the
 * reactivity back as it heats, so a withdrawn rod gives a new, higher power instead of a runaway.
 * And withdrawing rods only frees their reactivity at {@code maxWithdrawalRate}, the speed of the
 * rod drives: a piston jerking a rod out never takes the core prompt critical at once. Inserting is
 * immediate. Pure arithmetic.
 */
public final class FissionCore {
    /** Seconds in a game tick. */
    public static final double TICK = 0.05;

    /**
     * @param kinetics          point kinetics
     * @param reactivity        reactivity coefficients and worths
     * @param thermal           heat capacities and transfer
     * @param damage            core damage thresholds and rates
     * @param sixGroups         six delayed groups, or one
     * @param delayedTimeScale  speeds the delayed neutrons' decay up (gameplay), 1 = real
     * @param maxWithdrawalRate reactivity a withdrawal frees per second at most, pcm
     * @param xenonWorth        reactivity a fully poisoned core loses, pcm
     * @param xenonPoisoning    whether xenon builds up at all
     * @param iodineHalfLife    ticks
     * @param xenonHalfLife     ticks
     * @param decayFraction     share of the fission heat that comes later as decay heat
     * @param decayTime         time scale of the decay heat groups, ticks (see {@link DecayHeatModel})
     */
    public record Params(PointKinetics.Params kinetics, ReactivityModel.Params reactivity, CoreThermalModel.Params thermal,
                         CoreDamageModel.Params damage, boolean sixGroups, double delayedTimeScale, double maxWithdrawalRate,
                         double xenonWorth, boolean xenonPoisoning, double iodineHalfLife, double xenonHalfLife,
                         double decayFraction, double decayTime) {
        public static final Params DEFAULT = new Params(PointKinetics.Params.DEFAULT, ReactivityModel.Params.DEFAULT,
                CoreThermalModel.Params.DEFAULT, CoreDamageModel.Params.DEFAULT, true, 1.0, 100.0, 2800.0, true,
                3600.0, 5400.0, 0.07, 20000.0);
    }

    /**
     * @param rods             fuel rods (channels' blocks) in the core
     * @param fuelled          the core holds fuel
     * @param heatFactor       heat per unit power of the loaded fuel (MOX runs hotter)
     * @param delayedFraction  delayed neutron fraction of the loaded fuel, pcm
     * @param fuelReactivity   relative reactivity of the loaded fuel
     * @param burnt            share of it burnt
     * @param noDefectShare    share that breeds instead of losing reactivity (thorium)
     * @param emptyFraction    share of the fuel channels with no fuel
     * @param moderation       graphite round the fuel, 0..1
     * @param reflection       beryllium lining the core, 0..1
     * @param coolant          what cools it
     * @param maxAbsorption    how much of the core's reactivity the rods could hold back, in rod banks
     *                         (more than one when the core has more rod blocks than it needs)
     * @param channelInsertion how far each control rod channel is filled, 0..1
     * @param boundary         what the coolant is doing this tick
     * @param oxidationHeat    heat of oxidising cladding per rod, MW
     * @param pwrVoidWorth     pressurized loop: reactivity share of a fully voided core
     * @param pwrTemperatureCoefficient pressurized loop: reactivity share per kelvin
     * @param boronPpm         boron dissolved in the core's water, ppm
     */
    public record Inputs(int rods, boolean fuelled, double heatFactor, double delayedFraction, double fuelReactivity, double burnt,
                         double noDefectShare, double emptyFraction, double moderation, double reflection,
                         ReactivityModel.Coolant coolant, double maxAbsorption, double[] channelInsertion,
                         CoreThermalModel.Boundary boundary, double oxidationHeat, double pwrVoidWorth,
                         double pwrTemperatureCoefficient, double boronPpm) {
        /** A core with no boron in its water. */
        public Inputs(int rods, boolean fuelled, double heatFactor, double delayedFraction, double fuelReactivity, double burnt,
                      double noDefectShare, double emptyFraction, double moderation, double reflection,
                      ReactivityModel.Coolant coolant, double maxAbsorption, double[] channelInsertion,
                      CoreThermalModel.Boundary boundary, double oxidationHeat, double pwrVoidWorth,
                      double pwrTemperatureCoefficient) {
            this(rods, fuelled, heatFactor, delayedFraction, fuelReactivity, burnt, noDefectShare, emptyFraction, moderation,
                    reflection, coolant, maxAbsorption, channelInsertion, boundary, oxidationHeat, pwrVoidWorth,
                    pwrTemperatureCoefficient, 0);
        }
    }

    private final PointKinetics kinetics = new PointKinetics();
    private final CoreThermalModel thermal = new CoreThermalModel();
    private final CoreDamageModel damage = new CoreDamageModel();
    private final XenonModel xenon = new XenonModel();
    private final DecayHeatModel decay = new DecayHeatModel();
    private double rodsApplied = Double.NaN;
    private ReactivityModel.Breakdown breakdown = new ReactivityModel.Breakdown(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    private CoreThermalModel.Result thermalResult = CoreThermalModel.Result.NONE;
    private double ratedPower;
    private double rodBank;
    /** What the rods are where they stand worth once their withdrawal has caught up, pcm. */
    private double rodTarget;
    /** What the boron would hold back with the core flooded, pcm (positive). */
    private double floodedBoron;
    private DelayedNeutrons groups;
    private double groupsKey = Double.NaN;

    /** Advances one game tick. */
    public void tick(Params p, Inputs in) {
        DelayedNeutrons delayed = groups(p, in.delayedFraction());
        ratedPower = p.thermal().ratedPowerPerRod() * Math.max(0, in.rods()) * Math.max(0, in.heatFactor());
        rodBank = ReactivityModel.rodBank(p.reactivity(), in.maxAbsorption());
        double target = RodWorth.bank(in.channelInsertion(), rodBank, p.reactivity().rodWorthShape());
        rodTarget = target;
        floodedBoron = ReactivityModel.boron(p.reactivity(), in.boronPpm(), 1.0);
        if (Double.isNaN(rodsApplied) || target >= rodsApplied) {
            rodsApplied = target;
        } else {
            rodsApplied = Math.max(target, rodsApplied - Math.max(0, p.maxWithdrawalRate()) * TICK);
        }
        double xenonPcm = p.xenonPoisoning() ? xenon.penalty(p.xenonWorth()) : 0;
        breakdown = ReactivityModel.compute(p.reactivity(), new ReactivityModel.Inputs(in.fuelReactivity(), in.burnt(),
                in.noDefectShare(), in.emptyFraction(), in.moderation(), in.reflection(), in.coolant(), xenonPcm, rodsApplied,
                thermal.fuelTemperature(), thermal.coolantTemperature(), thermal.voidFraction(), coverage(in.boundary()), damage.damage(),
                in.pwrVoidWorth(),
                in.pwrTemperatureCoefficient(), in.boronPpm()));

        double n;
        if (in.fuelled() && in.rods() > 0) {
            n = kinetics.step(p.kinetics(), delayed, breakdown.total() * 1.0e-5, TICK);
        } else {
            kinetics.clear();
            n = 0;
        }
        double fraction = Math.max(0, Math.min(1, p.decayFraction()));
        decay.tick(n, fraction, Math.max(1, p.decayTime()));
        double perRod = p.thermal().ratedPowerPerRod() * Math.max(0, in.heatFactor());
        double heatPerRod = ((1 - fraction) * n + decay.heat()) * perRod + Math.max(0, in.oxidationHeat());
        thermalResult = thermal.step(p.thermal(), in.boundary(), in.rods(), heatPerRod, damage.damage(), TICK);
        damage.step(p.damage(), thermal.fuelTemperature(), TICK);
        if (p.xenonPoisoning()) {
            xenon.tick(Math.min(1, n), p.iodineHalfLife(), p.xenonHalfLife());
        } else {
            xenon.clear();
        }
    }

    private static double coverage(CoreThermalModel.Boundary boundary) {
        return switch (boundary) {
            case CoreThermalModel.Boiling b -> b.coverage();
            case CoreThermalModel.Pressurized pr -> pr.coverage();
            case CoreThermalModel.Dry d -> 0;
        };
    }

    private DelayedNeutrons groups(Params p, double pcm) {
        double key = pcm * 31 + p.delayedTimeScale() + (p.sixGroups() ? 0.5 : 0);
        if (groups == null || key != groupsKey) {
            groups = p.sixGroups() ? DelayedNeutrons.sixGroups(pcm, p.delayedTimeScale())
                    : DelayedNeutrons.oneGroup(pcm, 0.08 * p.delayedTimeScale());
            groupsKey = key;
        }
        return groups;
    }

    // ------------------------------------------------------------------ readings

    /** Power relative to rated. */
    public double power() {
        return kinetics.power();
    }

    /** Thermal power from fission and decay, MW. */
    public double thermalPower(Params p, int rods, double heatFactor) {
        return ((1 - p.decayFraction()) * kinetics.power() + decay.heat()) * p.thermal().ratedPowerPerRod() * rods * heatFactor;
    }

    /** Rated thermal power of the core as last ticked, MW. */
    public double ratedPower() {
        return ratedPower;
    }

    public double fuelTemperature() {
        return thermal.fuelTemperature();
    }

    public double coolantTemperature() {
        return thermal.coolantTemperature();
    }

    public double damage() {
        return damage.damage();
    }

    /** Steam in the core, 0..1, as the thermal model last stepped or restored it. */
    public double voidFraction() {
        return thermal.voidFraction();
    }

    public ReactivityModel.Breakdown breakdown() {
        return breakdown;
    }

    /** Net reactivity, pcm. */
    public double reactivity() {
        return breakdown.total();
    }

    /** Reactivity the rods would still hold back with the core as it is now and all of them in, pcm. */
    public double shutdownMargin() {
        return rodBank - breakdown.withoutRods();
    }

    /**
     * Reactivity the rods and boron would hold back with the core cold (20 °C, so no Doppler or
     * moderator feedback), flooded, without steam and without xenon, pcm: what keeps it shut down
     * once it has cooled and its xenon has decayed. Positive is subcritical. With
     * {@code allRodsIn} the whole rod bank counts, otherwise the rods where they stand.
     */
    public double coldShutdownMargin(boolean allRodsIn) {
        double rods = allRodsIn ? rodBank : rodTarget;
        return rods + floodedBoron - (breakdown.fuel() + breakdown.moderation() + breakdown.reflector() + breakdown.heavyWater()
                + breakdown.damage());
    }

    /** Worth of the full set of rods as last ticked, pcm. */
    public double rodBank() {
        return rodBank;
    }

    public double period() {
        return kinetics.period();
    }

    /** Reactivity the rods hold back right now, pcm (lags a withdrawal). */
    public double rodsApplied() {
        return Double.isNaN(rodsApplied) ? 0 : rodsApplied;
    }

    public CoreThermalModel.Result thermalResult() {
        return thermalResult;
    }

    public PointKinetics kinetics() {
        return kinetics;
    }

    public CoreThermalModel thermal() {
        return thermal;
    }

    public CoreDamageModel damageModel() {
        return damage;
    }

    public XenonModel xenon() {
        return xenon;
    }

    public DecayHeatModel decay() {
        return decay;
    }

    /** Restores the rods' applied reactivity (saved); NaN takes the next target at once. */
    public void restoreRods(double pcm) {
        rodsApplied = Double.isFinite(pcm) ? Math.max(0, pcm) : Double.NaN;
    }

    /** Settles the power at {@code n} with its precursors (a core run steadily, or a test). */
    public void settle(Params p, double delayedFraction, double n) {
        kinetics.atEquilibrium(groups(p, delayedFraction), n);
    }

    public void restoreKinetics(Params p, double delayedFraction, double n, double[] precursors) {
        kinetics.restore(groups(p, delayedFraction), n, precursors);
    }
}
