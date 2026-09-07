package org.papiricoh.create_nuclearindustry;

import net.neoforged.bus.api.SubscribeEvent;
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
            .comment("Fuel units consumed per neutron level per tick. An assembly supplies 100 units.",
                    "Zero disables uranium depletion.")
            .defineInRange("fission.uraniumBurnPerNeutron", 0.0000045, 0.0, 100.0);
    private static final ModConfigSpec.DoubleValue DT_BURN_RATE = BUILDER
            .comment("D-T fuel units consumed per fusion power unit per tick. A pellet supplies 100 units.",
                    "Deuterium and tritium are consumed together as pellets. Zero disables depletion.")
            .defineInRange("fusion.deuteriumTritiumBurnRate", 0.0015, 0.0, 100.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double steamCapacityPerPort = 32768.0;
    public static double plasmaCapacityPerPort = 65536.0;
    public static double boronAbsorption = 1.5;
    public static double uraniumBurnPerNeutron = 0.0000045;
    public static double deuteriumTritiumBurnRate = 0.0015;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        steamCapacityPerPort = TURBINE_CAPACITY.get();
        plasmaCapacityPerPort = FUSION_TURBINE_CAPACITY.get();
        boronAbsorption = BORON_ABSORPTION.get();
        uraniumBurnPerNeutron = URANIUM_BURN_RATE.get();
        deuteriumTritiumBurnRate = DT_BURN_RATE.get();
    }
}
