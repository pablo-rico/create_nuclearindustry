package org.papiricoh.create_nuclearindustry.fusion;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.fusion.physics.MagnetModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.StartupSequence;
import org.papiricoh.create_nuclearindustry.fusion.physics.VacuumModel;

/**
 * Common config for the tokamak, under {@code fusion.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 * The older keys predate this class, so existing config files keep their values. The physics
 * models never read this class: {@link #plasmaParams()} and its siblings copy it into their params.
 */
public final class FusionConfig {
    private static ModConfigSpec.DoubleValue DT_BURN_RATE;
    private static ModConfigSpec.IntValue MAX_MAJOR_RADIUS;
    private static ModConfigSpec.IntValue COILS_PER_SECTOR;
    private static ModConfigSpec.DoubleValue MAGNET_STRESS;
    private static ModConfigSpec.BooleanValue DISRUPTION_EXPLOSIONS;
    private static ModConfigSpec.DoubleValue MINIMUM_FIELD;
    private static ModConfigSpec.DoubleValue FIELD_RAMP;
    private static ModConfigSpec.DoubleValue VACUUM_LIMIT;
    private static ModConfigSpec.DoubleValue PUMP_DOWN_SECONDS;
    private static ModConfigSpec.DoubleValue COOLDOWN_SECONDS;
    private static ModConfigSpec.DoubleValue HELIUM_PER_SECTOR;
    private static ModConfigSpec.DoubleValue MAGNET_STANDBY;
    private static ModConfigSpec.DoubleValue GREENWALD_DENSITY;
    private static ModConfigSpec.DoubleValue DEFAULT_DENSITY;
    private static ModConfigSpec.DoubleValue ENERGY_CONFINEMENT;
    private static ModConfigSpec.DoubleValue PARTICLE_CONFINEMENT;
    private static ModConfigSpec.DoubleValue ASH_REMOVAL;
    private static ModConfigSpec.DoubleValue OHMIC_POWER;
    private static ModConfigSpec.DoubleValue SOLENOID_PULSE;
    private static ModConfigSpec.DoubleValue BREAKDOWN_DEUTERIUM;
    private static ModConfigSpec.DoubleValue BEAM_FUELLING;

    public static double deuteriumTritiumBurnRate = 0.0011;
    public static int maxMajorRadius = 16;
    public static int coilsPerSector = 4;
    public static double magnetInputStress = 1024.0;
    public static boolean disruptionExplosions = true;
    public static double minimumField = 0.5;
    public static double fieldRampPerSecond = 0.1;
    public static double vacuumLimit = -3.0;
    public static double pumpDownSeconds = 15.0;
    public static double magnetCooldownSeconds = 20.0;
    public static double heliumPerSector = 0.25;
    public static double magnetStandbyShare = 0.3;
    public static double greenwaldDensity = 1.0;
    public static double defaultDensity = 0.7;
    public static double energyConfinementSeconds = 7.5;
    public static double particleConfinementSeconds = 2.0;
    public static double ashRemovalSeconds = 5.0;
    public static double ohmicPowerPerCell = 0.08;
    public static double solenoidPulseSeconds = 30.0;
    public static double breakdownDeuterium = 10.0;
    public static double beamFuelling = 0.25;

    private FusionConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        DT_BURN_RATE = builder
                .comment("mB of deuterium (and of tritium) burnt per plasma cell per tick at the reference density (0.7 of the",
                        "density limit), peak temperature and no helium ash. A tokamak of major radius R has about 56*R plasma cells.",
                        "Fusion power is 1000 MW per mB/t of D-T burnt.")
                .defineInRange("fusion.deuteriumTritiumBurnRate", 0.0011, 0.0, 1.0);
        MAX_MAJOR_RADIUS = builder
                .comment("Largest tokamak major radius (distance from the central solenoid to the plasma centre).")
                .defineInRange("fusion.maxMajorRadius", 16, 4, 32);
        COILS_PER_SECTOR = builder
                .comment("Electromagnets a toroidal sector needs for full field coverage. A tokamak has 4*R sectors.")
                .defineInRange("fusion.coilsPerSector", 4, 1, 25);
        MAGNET_STRESS = builder
                .comment("Stress impact (SU per RPM) of each confinement magnet drive.")
                .defineInRange("fusion.magnetInputStress", 1024.0, 0.0, 1.0e6);
        DISRUPTION_EXPLOSIONS = builder
                .comment("Whether a plasma disruption blows holes in the tokamak. If false, the plasma just quenches.")
                .define("fusion.disruptionExplosions", true);
        MINIMUM_FIELD = builder
                .comment("Field (fraction of rated) the magnet drives must ask for before a tokamak may start or keep its plasma.")
                .defineInRange("fusion.minimumField", 0.5, 0.0, 1.0);
        FIELD_RAMP = builder
                .comment("How fast the toroidal field ramps up or down, in fractions of rated field per second.")
                .defineInRange("fusion.fieldRampPerSecond", 0.1, 0.01, 10.0);
        VACUUM_LIMIT = builder
                .comment("Highest chamber pressure a plasma can be lit or kept at, as log10 of pascals (atmosphere is 5).")
                .defineInRange("fusion.vacuumLimit", -3.0, -4.0, 4.0);
        PUMP_DOWN_SECONDS = builder
                .comment("Seconds the vacuum pumps a tokamak needs (ceil(R/4) at rated speed) take to pump it down from",
                        "atmosphere to the plasma limit.")
                .defineInRange("fusion.pumpDownSeconds", 15.0, 0.5, 600.0);
        COOLDOWN_SECONDS = builder
                .comment("Seconds liquid helium takes to cool the coils from room temperature to 4.5 K, with enough helium.")
                .defineInRange("fusion.magnetCooldownSeconds", 20.0, 0.5, 600.0);
        HELIUM_PER_SECTOR = builder
                .comment("mB/t of liquid helium each toroidal sector (4*R per tokamak) uses at full field. Twice this while cooling down.",
                        "Every mB used comes back as warm helium gas for the cryogenic plant.")
                .defineInRange("fusion.heliumPerSector", 0.25, 0.0, 100.0);
        MAGNET_STANDBY = builder
                .comment("Share of the full-field helium use that cold coils use with no field in them.")
                .defineInRange("fusion.magnetStandbyShare", 0.3, 0.0, 1.0);
        GREENWALD_DENSITY = builder
                .comment("Plasma density limit at full field, mB of plasma per plasma cell. It scales with the field; above it",
                        "the plasma grows unstable and disrupts.")
                .defineInRange("fusion.greenwaldDensity", 1.0, 0.01, 100.0);
        DEFAULT_DENSITY = builder
                .comment("Share of the density limit the default fuelling rate aims at; a new tokamak starts with that rate.")
                .defineInRange("fusion.defaultDensity", 0.7, 0.05, 1.0);
        ENERGY_CONFINEMENT = builder
                .comment("Energy confinement time of an R=4 tokamak with full coils and field, in seconds. It grows as R^0.8;",
                        "longer confinement means less heating is needed and an easier ignition.")
                .defineInRange("fusion.energyConfinementSeconds", 7.5, 0.1, 600.0);
        PARTICLE_CONFINEMENT = builder
                .comment("Seconds fuel stays in the plasma before it is pumped out and recycled to the tanks.")
                .defineInRange("fusion.particleConfinementSeconds", 2.0, 0.1, 600.0);
        ASH_REMOVAL = builder
                .comment("Seconds helium ash stays in the plasma before it is pumped out as helium gas.")
                .defineInRange("fusion.ashRemovalSeconds", 5.0, 0.1, 600.0);
        OHMIC_POWER = builder
                .comment("Ohmic heating of a cold plasma by the central solenoid, MW per plasma cell. It fades as the plasma heats.")
                .defineInRange("fusion.ohmicPowerPerCell", 0.08, 0.0, 10.0);
        SOLENOID_PULSE = builder
                .comment("Seconds the central solenoid can drive a plasma before its volt-seconds run out. It recharges as",
                        "long while the tokamak is offline.")
                .defineInRange("fusion.solenoidPulseSeconds", 30.0, 1.0, 3600.0);
        BREAKDOWN_DEUTERIUM = builder
                .comment("mB of deuterium puffed into the chamber to light the plasma.")
                .defineInRange("fusion.breakdownDeuterium", 10.0, 0.5, 1000.0);
        BEAM_FUELLING = builder
                .comment("Share of the neutral beams' deuterium that ends up fuelling the plasma.")
                .defineInRange("fusion.beamFuelling", 0.25, 0.0, 1.0);
    }

    public static void bake() {
        deuteriumTritiumBurnRate = DT_BURN_RATE.get();
        maxMajorRadius = MAX_MAJOR_RADIUS.get();
        coilsPerSector = COILS_PER_SECTOR.get();
        magnetInputStress = MAGNET_STRESS.get();
        disruptionExplosions = DISRUPTION_EXPLOSIONS.get();
        minimumField = MINIMUM_FIELD.get();
        fieldRampPerSecond = FIELD_RAMP.get();
        vacuumLimit = VACUUM_LIMIT.get();
        pumpDownSeconds = PUMP_DOWN_SECONDS.get();
        magnetCooldownSeconds = COOLDOWN_SECONDS.get();
        heliumPerSector = HELIUM_PER_SECTOR.get();
        magnetStandbyShare = MAGNET_STANDBY.get();
        greenwaldDensity = GREENWALD_DENSITY.get();
        defaultDensity = DEFAULT_DENSITY.get();
        energyConfinementSeconds = ENERGY_CONFINEMENT.get();
        particleConfinementSeconds = PARTICLE_CONFINEMENT.get();
        ashRemovalSeconds = ASH_REMOVAL.get();
        ohmicPowerPerCell = OHMIC_POWER.get();
        solenoidPulseSeconds = SOLENOID_PULSE.get();
        breakdownDeuterium = BREAKDOWN_DEUTERIUM.get();
        beamFuelling = BEAM_FUELLING.get();
    }

    // ------------------------------------------------------------------ model params

    public static PlasmaModel.Params plasmaParams() {
        PlasmaModel.Params d = PlasmaModel.Params.DEFAULT;
        return new PlasmaModel.Params(deuteriumTritiumBurnRate, greenwaldDensity, d.referenceDensity() * greenwaldDensity,
                d.heatCapacity(), energyConfinementSeconds * 20.0, particleConfinementSeconds * 20.0,
                ashRemovalSeconds * 20.0, d.radiation(), ohmicPowerPerCell, d.ohmicReferenceTemp(),
                solenoidPulseSeconds * 20.0, solenoidPulseSeconds * 20.0, d.rfMinDensity(), d.rfFullDensity(),
                beamFuelling, d.densityInstability());
    }

    public static MagnetModel.Params magnetParams() {
        MagnetModel.Params d = MagnetModel.Params.DEFAULT;
        return new MagnetModel.Params(fieldRampPerSecond / 20.0, Math.max(d.dumpPerTick(), fieldRampPerSecond / 20.0),
                heliumPerSector, magnetStandbyShare, 2 * heliumPerSector, magnetCooldownSeconds * 20.0,
                d.warmingPerTick(), d.idleWarmingRate(), d.superconductingTemperature());
    }

    public static VacuumModel.Params vacuumParams() {
        VacuumModel.Params d = VacuumModel.Params.DEFAULT;
        double span = VacuumModel.ATMOSPHERE - vacuumLimit;
        return new VacuumModel.Params(Math.min(d.basePressure(), vacuumLimit - 1.0), vacuumLimit,
                span / Math.max(1.0, pumpDownSeconds * 20.0), d.outgasPerTick(), d.pumpingSlope(), d.plasmaLoad());
    }

    public static StartupSequence.Params startupParams() {
        StartupSequence.Params d = StartupSequence.Params.DEFAULT;
        return new StartupSequence.Params(minimumField, d.vacuumGraceTicks(), d.heliumGraceTicks(), d.fieldGraceTicks(),
                d.breakdownTicks(), breakdownDeuterium, d.plasmaLossTicks(), d.minimumDensity(), d.burnExitTemperature());
    }
}
