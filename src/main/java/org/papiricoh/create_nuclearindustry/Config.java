package org.papiricoh.create_nuclearindustry;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue TURBINE_CAPACITY = BUILDER
            .comment("Steam turbine stress capacity per RPM for every 320 mB/t of steam expanded",
                    "(one full port pair, or four rotor stages at 80 mB/t each).")
            .defineInRange("turbines.steamCapacityPerPort", 32768.0, 0.0, 1.0e9);
    private static final ModConfigSpec.DoubleValue FUSION_TURBINE_CAPACITY = BUILDER
            .comment("Plasma turbine stress capacity per RPM for every 160 mB/t of plasma steam expanded",
                    "(one full port pair, or four rotor stages at 40 mB/t each).")
            .defineInRange("turbines.plasmaCapacityPerPort", 65536.0, 0.0, 1.0e9);
    private static final ModConfigSpec.DoubleValue BORON_ABSORPTION = BUILDER
            .comment("Boron control rod absorption effectiveness, multiplied by the control/fuel rod ratio.",
                    "Absorption is capped at 100% before applying insertion. Zero disables absorption.")
            .defineInRange("fission.boronAbsorption", 1.5, 0.0, 1000.0);
    private static final ModConfigSpec.DoubleValue URANIUM_BURN_RATE = BUILDER
            .comment("Fuel units consumed per neutron level per tick. Zero disables uranium depletion.")
            .defineInRange("fission.uraniumBurnPerNeutron", 0.0000045, 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue FUEL_UNITS = BUILDER
            .comment("Fuel units in a low-enriched (5%) assembly. Richer fuel lasts longer:",
                    "units = this * (enrichment / 5)^0.66, so 20% fuel holds about 2.5 times as much.")
            .defineInRange("fission.fuelUnitsPerAssembly", 100.0, 1.0, 100000.0);
    private static final ModConfigSpec.DoubleValue ENRICHMENT_REACTIVITY = BUILDER
            .comment("Extra neutron production per doubling of fuel enrichment above 5%.",
                    "0.25 makes 20% fuel 1.5 times as reactive. Zero makes enrichment affect only fuel life.")
            .defineInRange("fission.enrichmentReactivity", 0.25, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue GRAPHITE_MODERATOR_BONUS = BUILDER
            .comment("Extra neutron production of a fully moderated core: every fuel column with two Graphite Moderator",
                    "columns beside it. 0.35 makes such a core 35% more reactive. Zero makes graphite inert.")
            .defineInRange("fission.graphiteModeratorBonus", 0.35, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue BERYLLIUM_REFLECTOR_BONUS = BUILDER
            .comment("Fraction of neutron leakage a core whose whole inner ring is Beryllium Reflector bounces back.",
                    "0.25 cuts losses by a quarter. Zero makes reflectors inert.")
            .defineInRange("fission.berylliumReflectorBonus", 0.25, 0.0, 0.9);
    private static final ModConfigSpec.BooleanValue XENON_POISONING = BUILDER
            .comment("Whether fission builds up xenon-135, which holds the reactor back. It peaks a few minutes after",
                    "power drops (the iodine pit), so a reactor just shut down needs more rods out to restart.")
            .define("fission.xenonPoisoning", true);
    private static final ModConfigSpec.DoubleValue XENON_WORTH = BUILDER
            .comment("Most neutron production xenon can swallow, as a fraction. A flat-out core settles near half of it;",
                    "the peak after a shutdown reaches about two thirds.")
            .defineInRange("fission.xenonWorth", 0.30, 0.0, 0.95);
    private static final ModConfigSpec.IntValue IODINE_HALF_LIFE = BUILDER
            .comment("Ticks for iodine-135 to halve into xenon. 3600 is three minutes.")
            .defineInRange("fission.iodineHalfLifeTicks", 3600, 20, 1_000_000);
    private static final ModConfigSpec.IntValue XENON_HALF_LIFE = BUILDER
            .comment("Ticks for xenon-135 to halve once nothing burns it. 5400 is four and a half minutes.")
            .defineInRange("fission.xenonHalfLifeTicks", 5400, 20, 1_000_000);
    private static final ModConfigSpec.DoubleValue DECAY_HEAT_FRACTION = BUILDER
            .comment("Decay heat of a core that has run for a while, as a fraction of its fission heat. It keeps heating",
                    "the core after a SCRAM. Zero makes a reactor cool as soon as its chain reaction stops.")
            .defineInRange("fission.decayHeatFraction", 0.07, 0.0, 1.0);
    private static final ModConfigSpec.IntValue DECAY_HEAT_TIME = BUILDER
            .comment("Ticks for decay heat to fall to about a third once fission stops (it also takes this long to build up).")
            .defineInRange("fission.decayHeatTimeTicks", 2400, 1, 1_000_000);
    private static final ModConfigSpec.DoubleValue DT_BURN_RATE = BUILDER
            .comment("mB of deuterium (and of tritium) burnt per plasma cell per tick at full temperature.",
                    "A tokamak of major radius R has about 56*R plasma cells. Fusion power is 1000 MW per mB/t burnt.")
            .defineInRange("fusion.deuteriumTritiumBurnRate", 0.0011, 0.0, 1.0);
    private static final ModConfigSpec.IntValue MAX_MAJOR_RADIUS = BUILDER
            .comment("Largest tokamak major radius (distance from the central solenoid to the plasma centre).")
            .defineInRange("fusion.maxMajorRadius", 16, 4, 32);
    private static final ModConfigSpec.IntValue COILS_PER_SECTOR = BUILDER
            .comment("Electromagnets a toroidal sector needs for full field coverage. A tokamak has 4*R sectors.")
            .defineInRange("fusion.coilsPerSector", 4, 1, 25);
    private static final ModConfigSpec.DoubleValue MAGNET_STRESS = BUILDER
            .comment("Stress impact (SU per RPM) of each confinement magnet drive.")
            .defineInRange("fusion.magnetInputStress", 1024.0, 0.0, 1.0e6);
    private static final ModConfigSpec.BooleanValue DISRUPTION_EXPLOSIONS = BUILDER
            .comment("Whether a plasma disruption blows holes in the tokamak. If false, the plasma just quenches.")
            .define("fusion.disruptionExplosions", true);

    private static final ModConfigSpec.BooleanValue RADIATION_ENABLED = BUILDER
            .comment("Whether carrying or standing near radioactive material gives players a radiation dose.")
            .define("radiation.enabled", true);
    private static final ModConfigSpec.DoubleValue RADIATION_MULTIPLIER = BUILDER
            .comment("Multiplier on every dose rate.")
            .defineInRange("radiation.doseMultiplier", 1.0, 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue RADIATION_DECAY = BUILDER
            .comment("Dose removed per second while not exposed.")
            .defineInRange("radiation.decayPerSecond", 1.0, 0.0, 1000.0);
    private static final ModConfigSpec.DoubleValue NAUSEA_THRESHOLD = BUILDER
            .comment("Dose at which nausea starts.")
            .defineInRange("radiation.nauseaThreshold", 100.0, 0.0, 1.0e6);
    private static final ModConfigSpec.DoubleValue WEAKNESS_THRESHOLD = BUILDER
            .comment("Dose at which weakness starts.")
            .defineInRange("radiation.weaknessThreshold", 300.0, 0.0, 1.0e6);
    private static final ModConfigSpec.DoubleValue SICKNESS_THRESHOLD = BUILDER
            .comment("Dose at which radiation sickness (damage) starts; it worsens at twice this dose.")
            .defineInRange("radiation.sicknessThreshold", 600.0, 0.0, 1.0e6);

    private static final ModConfigSpec.DoubleValue REACTOR_EMISSION = BUILDER
            .comment("Dose per second next to a running fission reactor, per neutron of its activity (0-2000). Zero stops reactors irradiating.")
            .defineInRange("radiation.reactorEmissionPerNeutron", 0.002, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue SOLID_TRANSMISSION = BUILDER
            .comment("Fraction of radiation a solid block without its own radiation_transmission entry lets through.")
            .defineInRange("radiation.solidTransmission", 0.9, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue BACKGROUND_RATE = BUILDER
            .comment("Dose rates below this count as background: they add no dose and let it decay.")
            .defineInRange("radiation.backgroundRate", 0.05, 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue MAX_SHIELDING = BUILDER
            .comment("Most radiation worn armour can stop, as a fraction.")
            .defineInRange("radiation.maxShielding", 0.98, 0.0, 1.0);
    private static final ModConfigSpec.BooleanValue CONTAMINATION_ENABLED = BUILDER
            .comment("Whether nuclear accidents leave lasting ground contamination (one value per chunk).")
            .define("contamination.enabled", true);
    private static final ModConfigSpec.IntValue CONTAMINATION_HALF_LIFE = BUILDER
            .comment("Ticks for ground contamination to halve on its own. 240000 is ten in-game days; 0 means it never decays.")
            .defineInRange("contamination.halfLifeTicks", 240000, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue CONTAMINATION_MAX = BUILDER
            .comment("Most contamination a chunk can hold, in rad/s at ground level.")
            .defineInRange("contamination.max", 50.0, 0.0, 10000.0);
    private static final ModConfigSpec.DoubleValue CONTAMINATION_DOSE_SCALE = BUILDER
            .comment("Multiplier on the dose rate of standing on contaminated ground.")
            .defineInRange("contamination.doseScale", 1.0, 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue BARREL_LEAK = BUILDER
            .comment("Contamination (rad/s) a waste barrel above the storage depth adds to its chunk on each random tick",
                    "(about every 68 seconds at the default random tick speed). Zero stops barrels leaking.")
            .defineInRange("contamination.barrelLeak", 0.02, 0.0, 100.0);
    private static final ModConfigSpec.IntValue STORAGE_MAX_Y = BUILDER
            .comment("Waste barrels at or below this height count as geologically stored: they neither irradiate nor leak.")
            .defineInRange("contamination.geologicalStorageMaxY", 0, -2048, 2048);
    private static final ModConfigSpec.IntValue WATER_COLUMNS_PER_TICK = BUILDER
            .comment("Water columns per tick checked for fallout settling on them (256 is one chunk). Zero leaves water clean.")
            .defineInRange("contamination.waterColumnsPerTick", 256, 0, 65536);
    private static final ModConfigSpec.DoubleValue SCRUBBER_RATE = BUILDER
            .comment("Contamination (rad/s) a Radiation Scrubber at 256 RPM or more removes each second, shared among the chunks it reaches.")
            .defineInRange("contamination.scrubberRate", 0.05, 0.0, 1000.0);
    private static final ModConfigSpec.IntValue SCRUBBER_RADIUS = BUILDER
            .comment("Chunks around its own a Radiation Scrubber reaches: 1 is a 3x3 area.")
            .defineInRange("contamination.scrubberRadius", 1, 0, 4);
    private static final ModConfigSpec.DoubleValue SCRUBBER_WASTE_PER = BUILDER
            .comment("Contamination a Radiation Scrubber absorbs for each Radioactive Waste it produces.")
            .defineInRange("contamination.scrubberWastePer", 2.0, 0.01, 1000.0);
    private static final ModConfigSpec.DoubleValue SCRUBBER_STRESS = BUILDER
            .comment("Stress impact of a Radiation Scrubber, in SU per RPM.")
            .defineInRange("contamination.scrubberStress", 8.0, 0.0, 1.0e6);
    private static final ModConfigSpec.IntValue CONTROL_ROOM_RANGE = BUILDER
            .comment("Farthest, in blocks, a control room monitor, panel or alarm can be from what it is linked to.",
                    "The target must also be in a loaded chunk of the same dimension.")
            .defineInRange("controlRoom.linkRange", 128, 8, 4096);
    private static final ModConfigSpec.DoubleValue COOLING_TOWER_PASSIVE = BUILDER
            .comment("Steam a cooling tower condenses without fans, in mB/t per block of base radius per layer of height.")
            .defineInRange("coolingTower.passivePerBlock", 1.0, 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue COOLING_TOWER_FAN_STRESS = BUILDER
            .comment("Stress impact of a cooling tower fan, in SU per RPM.")
            .defineInRange("coolingTower.fanStress", 8.0, 0.0, 1.0e6);
    private static final ModConfigSpec.DoubleValue COOLING_TOWER_FAN_BOOST = BUILDER
            .comment("Capacity a fan at 256 RPM or more adds, as a fraction of the passive capacity.")
            .defineInRange("coolingTower.fanBoostPerFan", 0.25, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue COOLING_TOWER_MAX_BOOST = BUILDER
            .comment("Most capacity all fans together can add, as a fraction of the passive capacity.")
            .defineInRange("coolingTower.maxFanBoost", 1.0, 0.0, 100.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double steamCapacityPerPort = 32768.0;
    public static double plasmaCapacityPerPort = 65536.0;
    public static double boronAbsorption = 1.5;
    public static double uraniumBurnPerNeutron = 0.0000045;
    public static double fuelUnitsPerAssembly = 100.0;
    public static double enrichmentReactivity = 0.25;
    public static double graphiteModeratorBonus = 0.35;
    public static double berylliumReflectorBonus = 0.25;
    public static boolean xenonPoisoning = true;
    public static double xenonWorth = 0.30;
    public static int iodineHalfLifeTicks = 3600;
    public static int xenonHalfLifeTicks = 5400;
    public static double decayHeatFraction = 0.07;
    public static int decayHeatTimeTicks = 2400;
    public static double deuteriumTritiumBurnRate = 0.0011;
    public static int fusionMaxMajorRadius = 16;
    public static int fusionCoilsPerSector = 4;
    public static double fusionMagnetStress = 1024.0;
    public static boolean fusionDisruptionExplosions = true;
    public static boolean radiationEnabled = true;
    public static double radiationDoseMultiplier = 1.0;
    public static double radiationDecayPerSecond = 1.0;
    public static double radiationNauseaThreshold = 100.0;
    public static double radiationWeaknessThreshold = 300.0;
    public static double radiationSicknessThreshold = 600.0;
    public static double reactorEmissionPerNeutron = 0.002;
    public static double radiationSolidTransmission = 0.9;
    public static double radiationBackgroundRate = 0.05;
    public static double radiationMaxShielding = 0.98;
    public static boolean contaminationEnabled = true;
    public static long contaminationHalfLifeTicks = 240000;
    public static double contaminationMax = 50.0;
    public static double contaminationDoseScale = 1.0;
    public static double barrelLeak = 0.02;
    public static int geologicalStorageMaxY = 0;
    public static int waterColumnsPerTick = 256;
    public static double scrubberRate = 0.05;
    public static int scrubberRadius = 1;
    public static double scrubberWastePer = 2.0;
    public static double scrubberStress = 8.0;
    public static int controlRoomLinkRange = 128;
    public static double coolingTowerPassivePerBlock = 1.0;
    public static double coolingTowerFanStress = 8.0;
    public static double coolingTowerFanBoost = 0.25;
    public static double coolingTowerMaxFanBoost = 1.0;

    static void onLoad(final ModConfigEvent.Loading event)
    {
        bake(event);
    }

    static void onReload(final ModConfigEvent.Reloading event)
    {
        bake(event);
    }

    private static void bake(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        steamCapacityPerPort = TURBINE_CAPACITY.get();
        plasmaCapacityPerPort = FUSION_TURBINE_CAPACITY.get();
        boronAbsorption = BORON_ABSORPTION.get();
        uraniumBurnPerNeutron = URANIUM_BURN_RATE.get();
        fuelUnitsPerAssembly = FUEL_UNITS.get();
        enrichmentReactivity = ENRICHMENT_REACTIVITY.get();
        graphiteModeratorBonus = GRAPHITE_MODERATOR_BONUS.get();
        berylliumReflectorBonus = BERYLLIUM_REFLECTOR_BONUS.get();
        xenonPoisoning = XENON_POISONING.get();
        xenonWorth = XENON_WORTH.get();
        iodineHalfLifeTicks = IODINE_HALF_LIFE.get();
        xenonHalfLifeTicks = XENON_HALF_LIFE.get();
        decayHeatFraction = DECAY_HEAT_FRACTION.get();
        decayHeatTimeTicks = DECAY_HEAT_TIME.get();
        deuteriumTritiumBurnRate = DT_BURN_RATE.get();
        fusionMaxMajorRadius = MAX_MAJOR_RADIUS.get();
        fusionCoilsPerSector = COILS_PER_SECTOR.get();
        fusionMagnetStress = MAGNET_STRESS.get();
        fusionDisruptionExplosions = DISRUPTION_EXPLOSIONS.get();
        radiationEnabled = RADIATION_ENABLED.get();
        radiationDoseMultiplier = RADIATION_MULTIPLIER.get();
        radiationDecayPerSecond = RADIATION_DECAY.get();
        radiationNauseaThreshold = NAUSEA_THRESHOLD.get();
        radiationWeaknessThreshold = WEAKNESS_THRESHOLD.get();
        radiationSicknessThreshold = SICKNESS_THRESHOLD.get();
        reactorEmissionPerNeutron = REACTOR_EMISSION.get();
        radiationSolidTransmission = SOLID_TRANSMISSION.get();
        radiationBackgroundRate = BACKGROUND_RATE.get();
        radiationMaxShielding = MAX_SHIELDING.get();
        contaminationEnabled = CONTAMINATION_ENABLED.get();
        contaminationHalfLifeTicks = CONTAMINATION_HALF_LIFE.get();
        contaminationMax = CONTAMINATION_MAX.get();
        contaminationDoseScale = CONTAMINATION_DOSE_SCALE.get();
        barrelLeak = BARREL_LEAK.get();
        geologicalStorageMaxY = STORAGE_MAX_Y.get();
        waterColumnsPerTick = WATER_COLUMNS_PER_TICK.get();
        scrubberRate = SCRUBBER_RATE.get();
        scrubberRadius = SCRUBBER_RADIUS.get();
        scrubberWastePer = SCRUBBER_WASTE_PER.get();
        scrubberStress = SCRUBBER_STRESS.get();
        controlRoomLinkRange = CONTROL_ROOM_RANGE.get();
        coolingTowerPassivePerBlock = COOLING_TOWER_PASSIVE.get();
        coolingTowerFanStress = COOLING_TOWER_FAN_STRESS.get();
        coolingTowerFanBoost = COOLING_TOWER_FAN_BOOST.get();
        coolingTowerMaxFanBoost = COOLING_TOWER_MAX_BOOST.get();
    }
}
