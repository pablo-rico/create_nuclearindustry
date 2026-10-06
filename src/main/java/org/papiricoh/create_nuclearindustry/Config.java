package org.papiricoh.create_nuclearindustry;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue TURBINE_CAPACITY = BUILDER
            .comment("Steam turbine stress capacity per RPM per port at full throughput (320 mB/t).")
            .defineInRange("turbines.steamCapacityPerPort", 32768.0, 0.0, 1.0e9);
    private static final ModConfigSpec.DoubleValue FUSION_TURBINE_CAPACITY = BUILDER
            .comment("Plasma turbine stress capacity per RPM per port at full throughput (160 mB/t).")
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

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double steamCapacityPerPort = 32768.0;
    public static double plasmaCapacityPerPort = 65536.0;
    public static double boronAbsorption = 1.5;
    public static double uraniumBurnPerNeutron = 0.0000045;
    public static double fuelUnitsPerAssembly = 100.0;
    public static double enrichmentReactivity = 0.25;
    public static double deuteriumTritiumBurnRate = 0.0011;
    public static int fusionMaxMajorRadius = 16;
    public static int fusionCoilsPerSector = 4;
    public static double fusionMagnetStress = 1024.0;
    public static boolean fusionDisruptionExplosions = true;

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
        deuteriumTritiumBurnRate = DT_BURN_RATE.get();
        fusionMaxMajorRadius = MAX_MAJOR_RADIUS.get();
        fusionCoilsPerSector = COILS_PER_SECTOR.get();
        fusionMagnetStress = MAGNET_STRESS.get();
        fusionDisruptionExplosions = DISRUPTION_EXPLOSIONS.get();
    }
}
