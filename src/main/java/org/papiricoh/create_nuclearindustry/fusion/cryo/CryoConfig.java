package org.papiricoh.create_nuclearindustry.fusion.cryo;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;

/**
 * Common config for the cryogenic plant, the tokamak's vacuum pumps and cryogenic ports, under {@code cryo.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class CryoConfig {
    private static ModConfigSpec.DoubleValue HELIUM_PER_LAYER;
    private static ModConfigSpec.DoubleValue AIR_PER_LAYER;
    private static ModConfigSpec.DoubleValue LOOP_LOSS;
    private static ModConfigSpec.DoubleValue NITROGEN_BONUS;
    private static ModConfigSpec.DoubleValue NITROGEN_PER_HELIUM;
    private static ModConfigSpec.DoubleValue PLANT_MIN_SPEED;
    private static ModConfigSpec.DoubleValue PLANT_RATED_SPEED;
    private static ModConfigSpec.DoubleValue COMPRESSOR_STRESS;
    private static ModConfigSpec.IntValue PORT_TRANSFER;
    private static ModConfigSpec.DoubleValue PUMP_MIN_SPEED;
    private static ModConfigSpec.DoubleValue PUMP_RATED_SPEED;
    private static ModConfigSpec.DoubleValue PUMP_STRESS;

    public static double heliumPerLayer = 2.0;
    public static double airPerLayer = 5.0;
    public static double loopLoss = 0.01;
    public static double nitrogenBonus = 0.5;
    public static double nitrogenPerHelium = 1.0;
    public static double plantMinSpeed = 32.0;
    public static double plantRatedSpeed = 128.0;
    public static double compressorStressPerLayer = 10_240.0;
    public static int portTransfer = 160;
    public static double pumpMinSpeed = 32.0;
    public static double pumpRatedSpeed = 128.0;
    public static double pumpStress = 2_048.0;

    private CryoConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        HELIUM_PER_LAYER = builder
                .comment("Liquid helium a cryogenic plant makes per casing layer of height at rated speed, in mB per tick.",
                        "A 3x3x3 plant makes 3 times this; an R=4 tokamak's coils boil about 4 mB/t at full field.")
                .defineInRange("cryo.plant.heliumPerLayer", 2.0, 0.0, 1000.0);
        AIR_PER_LAYER = builder
                .comment("Liquid air a cryogenic plant with a Liquid Air port makes per casing layer at rated speed, in mB per tick.")
                .defineInRange("cryo.plant.airPerLayer", 5.0, 0.0, 1000.0);
        LOOP_LOSS = builder
                .comment("Share of the helium passing through the plant that leaks out of the loop. The plasma's helium ash",
                        "and centrifuged liquid air make it up.")
                .defineInRange("cryo.plant.loopLoss", 0.01, 0.0, 0.5);
        NITROGEN_BONUS = builder
                .comment("Extra liquid helium a plant makes while liquid nitrogen pre-cools it, as a share (0.5 = +50%).")
                .defineInRange("cryo.plant.nitrogenBonus", 0.5, 0.0, 10.0);
        NITROGEN_PER_HELIUM = builder
                .comment("Liquid nitrogen boiled off per mB of liquid helium made while pre-cooling.")
                .defineInRange("cryo.plant.nitrogenPerHelium", 1.0, 0.0, 100.0);
        PLANT_MIN_SPEED = builder
                .comment("Slowest compressor speed that does any work, in RPM. From here output grows in proportion to the speed.")
                .defineInRange("cryo.plant.minSpeed", 32.0, 0.0, 256.0);
        PLANT_RATED_SPEED = builder
                .comment("Compressor speed that gives full output, in RPM.")
                .defineInRange("cryo.plant.ratedSpeed", 128.0, 1.0, 256.0);
        COMPRESSOR_STRESS = builder
                .comment("Stress impact of a cryogenic plant per casing layer, in SU per RPM. It is shared among the plant's",
                        "compressors; a compressor outside a formed plant takes one layer's worth.")
                .defineInRange("cryo.plant.compressorStressPerLayer", 10_240.0, 0.0, 1.0e6);
        PORT_TRANSFER = builder
                .comment("Fluid a cryogenic plant port moves per tick to or from a tank or port right next to it, in mB.")
                .defineInRange("cryo.plant.portTransfer", 160, 0, 100_000);
        PUMP_MIN_SPEED = builder
                .comment("Slowest speed at which a tokamak vacuum pump pumps at all, in RPM.")
                .defineInRange("cryo.vacuumPump.minSpeed", 32.0, 0.0, 256.0);
        PUMP_RATED_SPEED = builder
                .comment("Vacuum pump speed that counts as one full pump, in RPM. An R=4 tokamak needs one pump at this speed",
                        "to reach plasma vacuum; every 4 blocks of major radius more need another.")
                .defineInRange("cryo.vacuumPump.ratedSpeed", 128.0, 1.0, 256.0);
        PUMP_STRESS = builder
                .comment("Stress impact of a tokamak vacuum pump, in SU per RPM.")
                .defineInRange("cryo.vacuumPump.stress", 2_048.0, 0.0, 1.0e6);
    }

    public static void bake() {
        heliumPerLayer = HELIUM_PER_LAYER.get();
        airPerLayer = AIR_PER_LAYER.get();
        loopLoss = LOOP_LOSS.get();
        nitrogenBonus = NITROGEN_BONUS.get();
        nitrogenPerHelium = NITROGEN_PER_HELIUM.get();
        plantMinSpeed = PLANT_MIN_SPEED.get();
        plantRatedSpeed = PLANT_RATED_SPEED.get();
        compressorStressPerLayer = COMPRESSOR_STRESS.get();
        portTransfer = PORT_TRANSFER.get();
        pumpMinSpeed = PUMP_MIN_SPEED.get();
        pumpRatedSpeed = PUMP_RATED_SPEED.get();
        pumpStress = PUMP_STRESS.get();
    }

    /** The plant model's parameters from the current config. */
    public static CryoplantModel.Params plantParams() {
        return new CryoplantModel.Params(heliumPerLayer, airPerLayer, loopLoss, nitrogenBonus, nitrogenPerHelium);
    }
}
