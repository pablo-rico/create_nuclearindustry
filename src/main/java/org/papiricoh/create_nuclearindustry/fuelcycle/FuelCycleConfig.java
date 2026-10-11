package org.papiricoh.create_nuclearindustry.fuelcycle;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the renewable fuel cycle (acid leaching, irradiation, brine and thorium), under
 * {@code fuelcycle.*}. Values are defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class FuelCycleConfig {
    private static ModConfigSpec.DoubleValue IRRADIATION_MIN_ACTIVITY;
    private static ModConfigSpec.DoubleValue IRRADIATION_DOSE;
    private static ModConfigSpec.DoubleValue ACID_DAMAGE;

    public static double irradiationMinActivity = 2.0;
    public static double irradiationDosePerSecond = 10.0;
    public static double acidDamagePerSecond = 1.0;

    private FuelCycleConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        IRRADIATION_MIN_ACTIVITY = builder
                .comment("How radioactive a block must be (its item's radioactivity, in rad/s) for a fan blowing past it to",
                        "irradiate the items in its air current. Blocks in the irradiation catalyst tag always do.",
                        "A full block stops the air current, so only blocks air can pass (like the Irradiation Source) work.")
                .defineInRange("fuelcycle.irradiationMinActivity", 2.0, 0.0, 1000.0);
        IRRADIATION_DOSE = builder
                .comment("Dose a living creature caught in an irradiating air current takes, in rad per second,",
                        "before its armour's shielding. 0 makes the current harmless.")
                .defineInRange("fuelcycle.irradiationDosePerSecond", 10.0, 0.0, 1000.0);
        ACID_DAMAGE = builder
                .comment("Damage a living creature caught in an air current blown through sulfuric acid takes each second",
                        "(half hearts). 0 makes the acid spray harmless.")
                .defineInRange("fuelcycle.acidDamagePerSecond", 1.0, 0.0, 100.0);
    }

    public static void bake() {
        irradiationMinActivity = IRRADIATION_MIN_ACTIVITY.get();
        irradiationDosePerSecond = IRRADIATION_DOSE.get();
        acidDamagePerSecond = ACID_DAMAGE.get();
    }
}
