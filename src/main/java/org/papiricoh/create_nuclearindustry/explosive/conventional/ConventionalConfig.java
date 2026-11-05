package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;

/**
 * Settings of non-nuclear warheads ({@code missile.conventional.*}, {@code missile.thermobaric.*}, {@code missile.cluster.*}):
 * how far a full bay's blast reaches, how a thermobaric charge differs from high explosive, how a cluster warhead opens and how
 * often its bomblets fail to go off. Defined on the shared builder by {@code MissileConfig} and copied into public statics on
 * load.
 */
public final class ConventionalConfig {
    public static double maxCraterRadius = 20;
    public static double maxWaveRadius = 64;
    public static double referenceCharge = 192;
    public static double airburstWaveBonus = 1.25;
    public static double airburstHeightFactor = 1.5;
    public static double blastProofResistance = 100;
    public static double fragmentsPerCharge = 40;
    public static int debrisMax = 48;
    public static double thermobaricWave = 1.6;
    public static double thermobaricCrater = 0.25;
    public static double thermobaricFire = 0.5;
    public static double thermobaricShelter = 0.5;
    public static double thermobaricHeightFactor = 1.2;
    public static double clusterOpenHeight = 64;
    public static double clusterFootprintRadius = 40;
    public static double clusterDudChance = 0.05;
    public static int clusterDudMinSeconds = 5;
    public static int clusterDudMaxSeconds = 300;

    private static ModConfigSpec.DoubleValue maxCraterRadiusValue, maxWaveRadiusValue, referenceChargeValue, airburstWaveBonusValue,
            airburstHeightFactorValue, blastProofResistanceValue, fragmentsPerChargeValue, thermobaricWaveValue,
            thermobaricCraterValue, thermobaricFireValue, thermobaricShelterValue, thermobaricHeightFactorValue,
            clusterOpenHeightValue, clusterFootprintRadiusValue, clusterDudChanceValue;
    private static ModConfigSpec.IntValue debrisMaxValue, clusterDudMinSecondsValue, clusterDudMaxSecondsValue;

    private ConventionalConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        maxCraterRadiusValue = builder
                .comment("Crater radius, blocks, of a full warhead bay of the strongest high explosive. Smaller charges dig less, by the",
                        "cube root of their charge.")
                .defineInRange("missile.conventional.maxCraterRadius", 20.0, 0.0, 128.0);
        maxWaveRadiusValue = builder
                .comment("How far, blocks, the shock wave of that full bay travels. It hurts out to 85% of it.")
                .defineInRange("missile.conventional.maxWaveRadius", 64.0, 1.0, 512.0);
        referenceChargeValue = builder
                .comment("The charge, in TNT equivalents (a TNT block is 1), that the two above are measured for: 3 slots of 16 plastic",
                        "explosive (4 each) is 192.")
                .defineInRange("missile.conventional.referenceCharge", 192.0, 1.0, 100000.0);
        airburstWaveBonusValue = builder
                .comment("How much further the shock wave of a conventional warhead set to airburst reaches. It digs no crater.")
                .defineInRange("missile.conventional.airburstWaveBonus", 1.25, 1.0, 4.0);
        airburstHeightFactorValue = builder
                .comment("How high a conventional warhead set to airburst goes off, in radii of its fireball.")
                .defineInRange("missile.conventional.airburstHeightFactor", 1.5, 1.05, 10.0);
        blastProofResistanceValue = builder
                .comment("Blocks resisting explosions more than this stand through a conventional blast (stone is 6, obsidian 1200).")
                .defineInRange("missile.conventional.blastProofResistance", 100.0, 1.0, 1.0e6);
        fragmentsPerChargeValue = builder
                .comment("Casing fragments a high-explosive warhead throws per TNT equivalent; walls stop them.")
                .defineInRange("missile.conventional.fragmentsPerCharge", 40.0, 0.0, 1000.0);
        debrisMaxValue = builder
                .comment("Most blocks one conventional blast throws through the air out of its crater.")
                .defineInRange("missile.conventional.debrisMax", 48, 0, 1000);
        thermobaricWaveValue = builder
                .comment("How much further a thermobaric warhead's pressure wave reaches than high explosive of the same charge.")
                .defineInRange("missile.thermobaric.waveFactor", 1.6, 1.0, 4.0);
        thermobaricCraterValue = builder
                .comment("How much of high explosive's crater a thermobaric warhead digs.")
                .defineInRange("missile.thermobaric.craterFactor", 0.25, 0.0, 1.0);
        thermobaricFireValue = builder
                .comment("How far a thermobaric warhead's burning cloud sets fires, a share of its wave's reach.")
                .defineInRange("missile.thermobaric.fireFactor", 0.5, 0.0, 1.0);
        thermobaricShelterValue = builder
                .comment("How much of a shelter's protection a thermobaric wave gets through, 0 none to 1 all: the fuel flows in first.")
                .defineInRange("missile.thermobaric.shelterPenetration", 0.5, 0.0, 1.0);
        thermobaricHeightFactorValue = builder
                .comment("How high a thermobaric warhead disperses and lights its cloud, in radii of its fireball. It always bursts in",
                        "the air.")
                .defineInRange("missile.thermobaric.burstHeightFactor", 1.2, 1.05, 10.0);
        clusterOpenHeightValue = builder
                .comment("How high over its target a cluster warhead opens, blocks.")
                .defineInRange("missile.cluster.openHeight", 64.0, 8.0, 512.0);
        clusterFootprintRadiusValue = builder
                .comment("Radius, blocks, of the ground its bomblets scatter over.")
                .defineInRange("missile.cluster.footprintRadius", 40.0, 2.0, 256.0);
        clusterDudChanceValue = builder
                .comment("Share of bomblets that fail to go off when they land; each goes off later, at a random moment.")
                .defineInRange("missile.cluster.dudChance", 0.05, 0.0, 1.0);
        clusterDudMinSecondsValue = builder
                .comment("Soonest a failed bomblet goes off after landing, seconds.")
                .defineInRange("missile.cluster.dudMinSeconds", 5, 0, 36000);
        clusterDudMaxSecondsValue = builder
                .comment("Latest a failed bomblet goes off after landing, seconds.")
                .defineInRange("missile.cluster.dudMaxSeconds", 300, 0, 36000);
    }

    public static void bake() {
        maxCraterRadius = maxCraterRadiusValue.get();
        maxWaveRadius = maxWaveRadiusValue.get();
        referenceCharge = referenceChargeValue.get();
        airburstWaveBonus = airburstWaveBonusValue.get();
        airburstHeightFactor = airburstHeightFactorValue.get();
        blastProofResistance = blastProofResistanceValue.get();
        fragmentsPerCharge = fragmentsPerChargeValue.get();
        debrisMax = debrisMaxValue.get();
        thermobaricWave = thermobaricWaveValue.get();
        thermobaricCrater = thermobaricCraterValue.get();
        thermobaricFire = thermobaricFireValue.get();
        thermobaricShelter = thermobaricShelterValue.get();
        thermobaricHeightFactor = thermobaricHeightFactorValue.get();
        clusterOpenHeight = clusterOpenHeightValue.get();
        clusterFootprintRadius = clusterFootprintRadiusValue.get();
        clusterDudChance = clusterDudChanceValue.get();
        clusterDudMinSeconds = clusterDudMinSecondsValue.get();
        clusterDudMaxSeconds = Math.max(clusterDudMinSeconds, clusterDudMaxSecondsValue.get());
    }

    /** The profile's measures, from this config and the blast wave's and the crater's. */
    public static ConventionalProfile.Settings settings() {
        return new ConventionalProfile.Settings(maxCraterRadius, maxWaveRadius, referenceCharge, airburstWaveBonus, thermobaricWave,
                thermobaricCrater, thermobaricFire, thermobaricShelter, fragmentsPerCharge, CivilDefenseConfig.blastWaveDamageThreshold,
                ExplosiveConfig.severeOverpressure, ExplosiveConfig.moderateOverpressure);
    }
}
