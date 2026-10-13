package org.papiricoh.create_nuclearindustry.accelerator;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.accelerator.physics.ActivationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AntimatterModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.BeamModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.BeamSequence;
import org.papiricoh.create_nuclearindustry.fusion.heating.HeatingDrive;

/**
 * Common config for the particle accelerator and the antimatter machines, under
 * {@code accelerator.*} and {@code antimatter.*}. Values are defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class AcceleratorConfig {
    private static ModConfigSpec.IntValue MAX_LINE_LENGTH;
    private static ModConfigSpec.IntValue MAX_RING_RADIUS;
    private static ModConfigSpec.DoubleValue SOURCE_CURRENT;
    private static ModConfigSpec.DoubleValue COPPER_GAIN;
    private static ModConfigSpec.DoubleValue SC_GAIN;
    private static ModConfigSpec.DoubleValue COPPER_CAVITY_POWER;
    private static ModConfigSpec.DoubleValue SC_CAVITY_POWER;
    private static ModConfigSpec.DoubleValue NORMAL_DIPOLE_FIELD;
    private static ModConfigSpec.DoubleValue SC_DIPOLE_FIELD;
    private static ModConfigSpec.DoubleValue INJECTION_RATIO;
    private static ModConfigSpec.DoubleValue RING_DUTY;
    private static ModConfigSpec.DoubleValue KLYSTRON_POWER;
    private static ModConfigSpec.DoubleValue KLYSTRON_RATED_SPEED;
    private static ModConfigSpec.DoubleValue KLYSTRON_MIN_SPEED;
    private static ModConfigSpec.DoubleValue KLYSTRON_STRESS;
    private static ModConfigSpec.DoubleValue MAGNET_SUPPLY_STRESS;
    private static ModConfigSpec.DoubleValue MAGNET_SUPPLY_RATED_SPEED;
    private static ModConfigSpec.IntValue MAGNETS_PER_SUPPLY;
    private static ModConfigSpec.DoubleValue ION_SOURCE_STRESS;
    private static ModConfigSpec.DoubleValue ION_SOURCE_RATED_SPEED;
    private static ModConfigSpec.DoubleValue HYDROGEN_PER_TICK;
    private static ModConfigSpec.DoubleValue PUMP_STRESS;
    private static ModConfigSpec.DoubleValue PUMP_RATED_SPEED;
    private static ModConfigSpec.IntValue CELLS_PER_PUMP;
    private static ModConfigSpec.BooleanValue FE_INPUT;
    private static ModConfigSpec.DoubleValue FE_PER_SU;
    private static ModConfigSpec.DoubleValue ACTIVATION_SATURATION;
    private static ModConfigSpec.IntValue ACTIVATION_HALF_LIFE;
    private static ModConfigSpec.DoubleValue PROMPT_DOSE;
    private static ModConfigSpec.DoubleValue MAX_HOT_SPOT;
    private static ModConfigSpec.BooleanValue LOCKS_DOORS;
    private static ModConfigSpec.BooleanValue PLAYERS_OUTSIDE;
    private static ModConfigSpec.DoubleValue ANTIPROTON_THRESHOLD;
    private static ModConfigSpec.DoubleValue COLLECTOR_EFFICIENCY;
    private static ModConfigSpec.DoubleValue GENERATOR_BURN;
    private static ModConfigSpec.DoubleValue GENERATOR_CAPACITY;
    private static ModConfigSpec.DoubleValue GENERATOR_ENERGY;
    private static ModConfigSpec.BooleanValue GENERATOR_FE;
    private static ModConfigSpec.DoubleValue IGNITION_DOSE;

    public static int maxLineLength = 160;
    public static int maxRingRadius = 96;
    public static double sourceCurrent = 200;
    public static double copperGain = 2.5;
    public static double superconductingGain = 12;
    public static double copperCavityPower = 0.25;
    public static double superconductingCavityPower = 0.08;
    public static double normalDipoleField = 1.8;
    public static double superconductingDipoleField = 8.0;
    public static double injectionRatio = 100;
    public static double ringDuty = 0.5;
    public static double klystronPower = 2.0;
    public static double klystronRatedSpeed = 128;
    public static double klystronMinSpeed = 32;
    public static double klystronStress = 32;
    public static double magnetSupplyStress = 16;
    public static double magnetSupplyRatedSpeed = 64;
    public static int magnetsPerSupply = 32;
    public static double ionSourceStress = 8;
    public static double ionSourceRatedSpeed = 64;
    public static double hydrogenPerTick = 0.5;
    public static double pumpStress = 8;
    public static double pumpRatedSpeed = 64;
    public static int cellsPerPump = 24;
    public static boolean feInput = true;
    public static double fePerSu = 0.25;
    public static double activationSaturationPerKw = 0.5;
    public static int activationHalfLife = 12000;
    public static double promptDosePerKw = 2.0;
    public static double maxHotSpotStrength = 500;
    public static boolean interlockLocksDoors = true;
    public static boolean interlockRequiresPlayersOutside = false;
    public static double antiprotonThreshold = 25;
    public static double collectorEfficiency = 0.8;
    public static double generatorBurnPerMinute = 6;
    public static double generatorCapacity = 256;
    public static double generatorEnergy = 16384;
    public static boolean generatorFe = true;
    public static double ignitionDose = 2;

    private AcceleratorConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        MAX_LINE_LENGTH = builder
                .comment("Longest straight or bent beamline (controller to target, or a ring's extraction line), in blocks.")
                .defineInRange("accelerator.maxLineLength", 160, 8, 512);
        MAX_RING_RADIUS = builder
                .comment("Largest synchrotron ring radius the controller looks for, in blocks. Bigger rings reach higher energies.")
                .defineInRange("accelerator.maxRingRadius", 96, 6, 256);
        SOURCE_CURRENT = builder
                .comment("Beam current the ion source makes at full drive, in microamperes.")
                .defineInRange("accelerator.beam.sourceCurrent", 200.0, 1.0, 100000.0);
        COPPER_GAIN = builder
                .comment("Energy one copper RF Cavity adds to the beam at full radio-frequency drive, in MeV.")
                .defineInRange("accelerator.beam.copperCavityGain", 2.5, 0.0, 1000.0);
        SC_GAIN = builder
                .comment("Energy one Superconducting RF Cavity adds at full drive, in MeV.")
                .defineInRange("accelerator.beam.superconductingCavityGain", 12.0, 0.0, 1000.0);
        COPPER_CAVITY_POWER = builder
                .comment("Radio-frequency power a copper RF Cavity needs, in MW.")
                .defineInRange("accelerator.beam.copperCavityPower", 0.25, 0.0, 100.0);
        SC_CAVITY_POWER = builder
                .comment("Radio-frequency power a Superconducting RF Cavity needs, in MW.")
                .defineInRange("accelerator.beam.superconductingCavityPower", 0.08, 0.0, 100.0);
        NORMAL_DIPOLE_FIELD = builder
                .comment("Field of a Dipole Magnet, in tesla. A ring's top energy is 0.3 x field x radius x share of dipoles, in GeV.")
                .defineInRange("accelerator.beam.normalDipoleField", 1.8, 0.1, 20.0);
        SC_DIPOLE_FIELD = builder
                .comment("Field of a Superconducting Dipole Magnet, in tesla.")
                .defineInRange("accelerator.beam.superconductingDipoleField", 8.0, 0.1, 20.0);
        INJECTION_RATIO = builder
                .comment("A ring only accepts beam from a linac with at least its top energy divided by this.")
                .defineInRange("accelerator.beam.injectionRatio", 100.0, 1.0, 10000.0);
        RING_DUTY = builder
                .comment("Share of the time a ring delivers beam; the rest it spends filling and ramping.")
                .defineInRange("accelerator.beam.ringDuty", 0.5, 0.01, 1.0);
        KLYSTRON_POWER = builder
                .comment("Radio-frequency power one Klystron gives at its rated speed, in MW.")
                .defineInRange("accelerator.klystron.ratedPower", 2.0, 0.0, 1000.0);
        KLYSTRON_RATED_SPEED = builder
                .comment("Shaft speed at which a Klystron reaches its rated power, in RPM.")
                .defineInRange("accelerator.klystron.ratedSpeed", 128.0, 1.0, 256.0);
        KLYSTRON_MIN_SPEED = builder
                .comment("Shaft speed below which a Klystron gives nothing, in RPM. Must be below the rated speed.")
                .defineInRange("accelerator.klystron.minSpeed", 32.0, 0.0, 255.0);
        KLYSTRON_STRESS = builder
                .comment("Stress impact of a Klystron, in SU per RPM.")
                .defineInRange("accelerator.klystron.stress", 32.0, 0.0, 1.0e6);
        MAGNET_SUPPLY_STRESS = builder
                .comment("Stress impact of a Magnet Power Supply, in SU per RPM.")
                .defineInRange("accelerator.magnetSupply.stress", 16.0, 0.0, 1.0e6);
        MAGNET_SUPPLY_RATED_SPEED = builder
                .comment("Shaft speed at which a Magnet Power Supply powers all its magnets, in RPM.")
                .defineInRange("accelerator.magnetSupply.ratedSpeed", 64.0, 1.0, 256.0);
        MAGNETS_PER_SUPPLY = builder
                .comment("Magnets (quadrupoles, bending magnets, dipoles) one Magnet Power Supply powers.",
                        "Superconducting magnets count half.")
                .defineInRange("accelerator.magnetSupply.magnetsPerSupply", 32, 1, 4096);
        ION_SOURCE_STRESS = builder
                .comment("Stress impact of the Ion Source, in SU per RPM.")
                .defineInRange("accelerator.ionSource.stress", 8.0, 0.0, 1.0e6);
        ION_SOURCE_RATED_SPEED = builder
                .comment("Shaft speed at which the Ion Source makes its full current, in RPM.")
                .defineInRange("accelerator.ionSource.ratedSpeed", 64.0, 1.0, 256.0);
        HYDROGEN_PER_TICK = builder
                .comment("Hydrogen the Ion Source uses per tick while making beam, in mB.")
                .defineInRange("accelerator.ionSource.hydrogenPerTick", 0.5, 0.0, 1000.0);
        PUMP_STRESS = builder
                .comment("Stress impact of a Beamline Vacuum Pump, in SU per RPM.")
                .defineInRange("accelerator.vacuumPump.stress", 8.0, 0.0, 1.0e6);
        PUMP_RATED_SPEED = builder
                .comment("Shaft speed at which a Beamline Vacuum Pump pumps at full speed, in RPM.")
                .defineInRange("accelerator.vacuumPump.ratedSpeed", 64.0, 1.0, 256.0);
        CELLS_PER_PUMP = builder
                .comment("Beamline blocks one Beamline Vacuum Pump at full speed keeps at beam vacuum.")
                .defineInRange("accelerator.vacuumPump.cellsPerPump", 24, 1, 4096);
        FE_INPUT = builder
                .comment("Whether the accelerator's machines also take Forge Energy from other mods instead of rotation.")
                .define("accelerator.energy.feInput", true);
        FE_PER_SU = builder
                .comment("Forge Energy per tick that stands in for one SU of rotation (stress impact x rated speed).")
                .defineInRange("accelerator.energy.fePerSu", 0.25, 0.0, 100.0);
        ACTIVATION_SATURATION = builder
                .comment("Activity a part reaches under one kW of lost beam, in dose per second next to it. It keeps",
                        "radiating after the beam stops and leaves radioactive waste when dismantled.")
                .defineInRange("accelerator.activation.saturationPerKw", 0.5, 0.0, 1000.0);
        ACTIVATION_HALF_LIFE = builder
                .comment("Half-life of the activation of accelerator parts, in ticks.")
                .defineInRange("accelerator.activation.halfLifeTicks", 12000, 20, 10000000);
        PROMPT_DOSE = builder
                .comment("Dose per second next to a spot losing one kW of beam, only while the beam runs.")
                .defineInRange("accelerator.activation.promptDosePerKw", 2.0, 0.0, 1000.0);
        MAX_HOT_SPOT = builder
                .comment("Most dose per second one spot of the accelerator gives off.")
                .defineInRange("accelerator.activation.maxStrength", 500.0, 0.0, 100000.0);
        LOCKS_DOORS = builder
                .comment("Whether doors next to a Beam Interlock refuse to open by hand while the beam is on.",
                        "Doors opened anyway (by redstone) trip the beam.")
                .define("accelerator.interlock.locksDoors", true);
        PLAYERS_OUTSIDE = builder
                .comment("Whether the accelerator refuses to start with a player within 2 blocks of its beamline.")
                .define("accelerator.interlock.requirePlayersOutside", false);
        ANTIPROTON_THRESHOLD = builder
                .comment("Beam energy below which a target makes no antiprotons, in GeV.")
                .defineInRange("accelerator.antimatter.thresholdGev", 25.0, 0.1, 10000.0);
        COLLECTOR_EFFICIENCY = builder
                .comment("Share of the antiprotons a Target Station's collector catches.")
                .defineInRange("accelerator.antimatter.collectorEfficiency", 0.8, 0.0, 1.0);
        GENERATOR_BURN = builder
                .comment("Antiprotons an Annihilation Generator burns at full throttle, in nanograms per minute.")
                .defineInRange("antimatter.generator.burnPerMinute", 6.0, 0.001, 10000.0);
        GENERATOR_CAPACITY = builder
                .comment("Stress capacity of an Annihilation Generator at full throttle, in SU per RPM (it turns at 256 RPM).")
                .defineInRange("antimatter.generator.capacity", 256.0, 0.0, 1.0e6);
        GENERATOR_ENERGY = builder
                .comment("Forge Energy an Annihilation Generator makes at full throttle in electric mode, in FE per tick.")
                .defineInRange("antimatter.generator.energy", 16384.0, 0.0, 1.0e9);
        GENERATOR_FE = builder
                .comment("Whether the Annihilation Generator can make Forge Energy for other mods.")
                .define("antimatter.generator.feEnabled", true);
        IGNITION_DOSE = builder
                .comment("Antiprotons a Tokamak Antiproton Injector spends to ignite a plasma, in nanograms.")
                .defineInRange("antimatter.ignitionDose", 2.0, 0.0, 1000.0);
    }

    public static void bake() {
        maxLineLength = MAX_LINE_LENGTH.get();
        maxRingRadius = MAX_RING_RADIUS.get();
        sourceCurrent = SOURCE_CURRENT.get();
        copperGain = COPPER_GAIN.get();
        superconductingGain = SC_GAIN.get();
        copperCavityPower = COPPER_CAVITY_POWER.get();
        superconductingCavityPower = SC_CAVITY_POWER.get();
        normalDipoleField = NORMAL_DIPOLE_FIELD.get();
        superconductingDipoleField = SC_DIPOLE_FIELD.get();
        injectionRatio = INJECTION_RATIO.get();
        ringDuty = RING_DUTY.get();
        klystronPower = KLYSTRON_POWER.get();
        klystronRatedSpeed = KLYSTRON_RATED_SPEED.get();
        klystronMinSpeed = Math.min(KLYSTRON_MIN_SPEED.get(), klystronRatedSpeed - 1.0);
        klystronStress = KLYSTRON_STRESS.get();
        magnetSupplyStress = MAGNET_SUPPLY_STRESS.get();
        magnetSupplyRatedSpeed = MAGNET_SUPPLY_RATED_SPEED.get();
        magnetsPerSupply = MAGNETS_PER_SUPPLY.get();
        ionSourceStress = ION_SOURCE_STRESS.get();
        ionSourceRatedSpeed = ION_SOURCE_RATED_SPEED.get();
        hydrogenPerTick = HYDROGEN_PER_TICK.get();
        pumpStress = PUMP_STRESS.get();
        pumpRatedSpeed = PUMP_RATED_SPEED.get();
        cellsPerPump = CELLS_PER_PUMP.get();
        feInput = FE_INPUT.get();
        fePerSu = FE_PER_SU.get();
        activationSaturationPerKw = ACTIVATION_SATURATION.get();
        activationHalfLife = ACTIVATION_HALF_LIFE.get();
        promptDosePerKw = PROMPT_DOSE.get();
        maxHotSpotStrength = MAX_HOT_SPOT.get();
        interlockLocksDoors = LOCKS_DOORS.get();
        interlockRequiresPlayersOutside = PLAYERS_OUTSIDE.get();
        antiprotonThreshold = ANTIPROTON_THRESHOLD.get();
        collectorEfficiency = COLLECTOR_EFFICIENCY.get();
        generatorBurnPerMinute = GENERATOR_BURN.get();
        generatorCapacity = GENERATOR_CAPACITY.get();
        generatorEnergy = GENERATOR_ENERGY.get();
        generatorFe = GENERATOR_FE.get();
        ignitionDose = IGNITION_DOSE.get();
    }

    /** The beam's parameters, from the current config. */
    public static BeamModel.Params beam() {
        BeamModel.Params d = BeamModel.Params.DEFAULT;
        return new BeamModel.Params(sourceCurrent, d.sourceEnergy(), copperGain, superconductingGain, copperCavityPower,
                superconductingCavityPower, d.quadSpacing(), d.ringCellsPerQuad(), normalDipoleField,
                superconductingDipoleField, d.rigidity(), injectionRatio, d.goodVacuum(), d.badVacuum(),
                d.baseTransmission(), ringDuty, d.rampPerCavity());
    }

    /** The startup sequence's grace times and limits. */
    public static BeamSequence.Params sequence() {
        return BeamSequence.Params.DEFAULT;
    }

    /** The klystron's drive curve, from the current config. */
    public static HeatingDrive.Params klystron() {
        return new HeatingDrive.Params(klystronPower, klystronRatedSpeed, Math.max(0, klystronMinSpeed));
    }

    public static ActivationModel.Params activation() {
        ActivationModel.Params d = ActivationModel.Params.DEFAULT;
        return new ActivationModel.Params(activationSaturationPerKw, activationHalfLife, promptDosePerKw, maxHotSpotStrength,
                d.wasteThreshold(), d.wastePerItem(), d.maxLowLevelWaste(), d.hotThreshold());
    }

    public static AntimatterModel.Params antimatter() {
        return new AntimatterModel.Params(antiprotonThreshold, AntimatterModel.Params.DEFAULT.maxYield(), collectorEfficiency);
    }

    public static AnnihilationModel.Params generator() {
        AnnihilationModel.Params d = AnnihilationModel.Params.DEFAULT;
        return new AnnihilationModel.Params(generatorBurnPerMinute / 1200.0, d.ratedSpeed(), generatorCapacity,
                generatorFe ? generatorEnergy : 0, d.standbyShare());
    }
}
