package org.papiricoh.create_nuclearindustry.propulsion;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.propulsion.physics.AntimatterRocketModel;
import org.papiricoh.create_nuclearindustry.propulsion.physics.ThermalRocketModel;

/**
 * Common config for the nuclear engines of Create Aeronautics ships, under {@code aeronautics.engines.*}.
 * Defined on the shared builder by {@link org.papiricoh.create_nuclearindustry.Config} and copied into
 * public statics on load.
 */
public final class PropulsionConfig {
    /** Multiplies every engine's thrust. */
    public static double thrustScale = 1.0;
    /** Power of the explosion of a failed thermal engine's nozzle. */
    public static float thermalFailurePower = 5.0f;
    /** Contamination the antimatter jet leaves where it lands, per ng annihilated. */
    public static double activationPerNg = 0.04;
    /** Ticks from an antimatter engine losing its field to the annihilation of its chamber. */
    public static int quenchTicks = 60;
    /** Antimatter the engine's chamber holds, ng. */
    public static double chamberCapacity = 16.0;
    private static ThermalRocketModel.Params thermal = ThermalRocketModel.Params.DEFAULT;
    private static AntimatterRocketModel.Params antimatter = AntimatterRocketModel.Params.DEFAULT;

    private static ModConfigSpec.DoubleValue THRUST_SCALE;
    private static ModConfigSpec.IntValue FLOW;
    private static ModConfigSpec.DoubleValue THRUST_PER_MB;
    private static ModConfigSpec.DoubleValue NOZZLE_LIMIT;
    private static ModConfigSpec.DoubleValue LEAK;
    private static ModConfigSpec.DoubleValue FAILURE_POWER;
    private static ModConfigSpec.DoubleValue MAX_FEED;
    private static ModConfigSpec.DoubleValue THRUST_PER_NG;
    private static ModConfigSpec.IntValue COIL_FE;
    private static ModConfigSpec.DoubleValue FE_PER_NG;
    private static ModConfigSpec.DoubleValue COIL_LIMIT;
    private static ModConfigSpec.DoubleValue GAMMA;
    private static ModConfigSpec.DoubleValue ACTIVATION;
    private static ModConfigSpec.IntValue QUENCH;
    private static ModConfigSpec.DoubleValue CHAMBER;

    private PropulsionConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        ThermalRocketModel.Params t = ThermalRocketModel.Params.DEFAULT;
        AntimatterRocketModel.Params a = AntimatterRocketModel.Params.DEFAULT;
        THRUST_SCALE = builder.comment("Multiplies the thrust of every nuclear engine.")
                .defineInRange("aeronautics.engines.thrustScale", 1.0, 0.0, 1000.0);
        FLOW = builder.comment("Propellant each heat exchanger segment of a nuclear thermal engine passes at full throttle, mB per tick.")
                .defineInRange("aeronautics.engines.thermal.flowPerSegment", t.flowPerSegment(), 1, 1000);
        THRUST_PER_MB = builder.comment("Thrust of a nuclear thermal engine per mB/tick of hydrogen; water gives a third of it.")
                .defineInRange("aeronautics.engines.thermal.thrustPerMb", t.thrustPerMb(), 0.0, 1.0e7);
        NOZZLE_LIMIT = builder.comment("Nozzle temperature, °C, above which a nuclear thermal engine's nozzle wears; worn through, it blows up.")
                .defineInRange("aeronautics.engines.thermal.nozzleLimit", t.nozzleLimit(), 100.0, 1.0e5);
        LEAK = builder.comment("Contamination a nuclear thermal engine's exhaust carries per mB of propellant, new nozzle (five times worn out).")
                .defineInRange("aeronautics.engines.thermal.leakPerMb", t.leakPerMb(), 0.0, 1.0);
        FAILURE_POWER = builder.comment("Explosion power of a nuclear thermal engine whose nozzle fails.")
                .defineInRange("aeronautics.engines.thermal.failurePower", 5.0, 0.0, 50.0);
        MAX_FEED = builder.comment("Antimatter an antimatter engine annihilates at full throttle, ng per tick.")
                .defineInRange("aeronautics.engines.antimatter.maxFeed", a.maxFeed(), 1.0e-6, 100.0);
        THRUST_PER_NG = builder.comment("Thrust of an antimatter engine per ng/tick, before the coils' efficiency.")
                .defineInRange("aeronautics.engines.antimatter.thrustPerNg", a.thrustPerNg(), 0.0, 1.0e12);
        COIL_FE = builder.comment("Forge Energy each magnetic coil needs a tick to hold the field.")
                .defineInRange("aeronautics.engines.antimatter.coilFe", a.coilFe(), 0, 1_000_000);
        FE_PER_NG = builder.comment("Forge Energy each nanogram fed needs on top of the coils'.")
                .defineInRange("aeronautics.engines.antimatter.fePerNg", a.fePerNg(), 0.0, 1.0e9);
        COIL_LIMIT = builder.comment("Coil temperature, °C, above which an antimatter engine's coils wear; worn through, the field fails.")
                .defineInRange("aeronautics.engines.antimatter.coilLimit", a.coilLimit(), 100.0, 1.0e5);
        GAMMA = builder.comment("Gamma dose behind an antimatter engine's nozzle, at one block, per ng/tick its coils miss.")
                .defineInRange("aeronautics.engines.antimatter.gammaPerNg", a.gammaPerNg(), 0.0, 1.0e9);
        ACTIVATION = builder.comment("Contamination an antimatter jet leaves where it lands, per ng annihilated.")
                .defineInRange("aeronautics.engines.antimatter.activationPerNg", 0.04, 0.0, 100.0);
        QUENCH = builder.comment("Ticks from an antimatter engine losing its field to the annihilation of what its chamber holds.")
                .defineInRange("aeronautics.engines.antimatter.quenchTicks", 60, 1, 1200);
        CHAMBER = builder.comment("Antimatter an antimatter engine's chamber holds, ng.")
                .defineInRange("aeronautics.engines.antimatter.chamberCapacity", 16.0, 0.01, 1.0e6);
    }

    public static void bake() {
        ThermalRocketModel.Params t = ThermalRocketModel.Params.DEFAULT;
        AntimatterRocketModel.Params a = AntimatterRocketModel.Params.DEFAULT;
        thrustScale = THRUST_SCALE.get();
        thermalFailurePower = FAILURE_POWER.get().floatValue();
        activationPerNg = ACTIVATION.get();
        quenchTicks = QUENCH.get();
        chamberCapacity = CHAMBER.get();
        thermal = new ThermalRocketModel.Params(FLOW.get(), THRUST_PER_MB.get(), t.primaryTemperature(), t.primaryPerMb(),
                t.chamberDrop(), t.nozzleRise(), NOZZLE_LIMIT.get(), t.nozzleResponse(), t.wearPerTick(), LEAK.get());
        antimatter = new AntimatterRocketModel.Params(MAX_FEED.get(), THRUST_PER_NG.get(), COIL_FE.get(), FE_PER_NG.get(),
                a.coilRise(), COIL_LIMIT.get(), a.coilResponse(), a.wearPerTick(), GAMMA.get());
    }

    public static ThermalRocketModel.Params thermal() {
        return thermal;
    }

    public static AntimatterRocketModel.Params antimatter() {
        return antimatter;
    }
}
