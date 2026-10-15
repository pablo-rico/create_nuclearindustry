package org.papiricoh.create_nuclearindustry.antimatter;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationBlastModel;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.physics.ConduitTransfer;
import org.papiricoh.create_nuclearindustry.accelerator.physics.InjectorModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;

import java.util.EnumMap;
import java.util.Map;

/**
 * The antimatter containment keys, {@code antimatter.trap.*}, {@code antimatter.vial.*},
 * {@code antimatter.conduit.*}, {@code antimatter.blast.*} and {@code antimatter.injector.*}. Defined and baked by
 * {@link org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig}, next to the other
 * {@code antimatter.*} keys, and kept apart so that class stays readable. The baked values are the
 * physics models' {@code Params}, so game code never rebuilds them.
 */
public final class AntimatterConfig {
    private static final Map<TrapTier, ModConfigSpec.DoubleValue> CAPACITY = new EnumMap<>(TrapTier.class);
    private static final Map<TrapTier, ModConfigSpec.DoubleValue> IDLE_FE = new EnumMap<>(TrapTier.class);
    private static final Map<TrapTier, ModConfigSpec.DoubleValue> LOAD_FE = new EnumMap<>(TrapTier.class);
    private static ModConfigSpec.IntValue BUFFER_TICKS;
    private static ModConfigSpec.IntValue QUENCH_TICKS;
    private static ModConfigSpec.DoubleValue LOW_POWER_SHARE;
    private static ModConfigSpec.DoubleValue VIAL_CAPACITY;
    private static ModConfigSpec.IntValue VIAL_BATTERY;
    private static ModConfigSpec.IntValue VIAL_RECHARGE;
    private static ModConfigSpec.DoubleValue VIAL_TRANSFER;
    private static ModConfigSpec.IntValue CONDUIT_FE;
    private static ModConfigSpec.DoubleValue CONDUIT_RATE;
    private static ModConfigSpec.IntValue CONDUIT_MAX;
    private static ModConfigSpec.DoubleValue BLAST_MAX_RADIUS;
    private static ModConfigSpec.DoubleValue BLAST_DOSE_PER_NG;
    private static ModConfigSpec.DoubleValue BLAST_MAX_DOSE;
    private static ModConfigSpec.BooleanValue BLAST_BREAKS_BLOCKS;
    private static ModConfigSpec.IntValue INJECTOR_COOLDOWN;
    private static ModConfigSpec.IntValue INJECTOR_SHOT_FE;

    private static final Map<TrapTier, PenningTrapModel.Params> TRAPS = new EnumMap<>(TrapTier.class);
    private static VialBattery.Params vial = VialBattery.Params.DEFAULT;
    private static ConduitTransfer.Params conduit = ConduitTransfer.Params.DEFAULT;
    private static AnnihilationBlastModel.Params blast = AnnihilationBlastModel.Params.DEFAULT;
    /** Whether an annihilation breaks blocks, or only hurts and irradiates. */
    public static boolean blastBreaksBlocks = true;
    public static int injectorCooldownTicks = InjectorModel.Params.DEFAULT.cooldownTicks();
    public static int injectorShotFe = InjectorModel.Params.DEFAULT.shotFe();
    /** Game tests only: replaces {@link #injector()} while not null. */
    private static InjectorModel.Params injectorOverride;

    static {
        for (TrapTier tier : TrapTier.values()) {
            TRAPS.put(tier, tier.defaults());
        }
    }

    private AntimatterConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        for (TrapTier tier : TrapTier.values()) {
            PenningTrapModel.Params d = tier.defaults();
            String key = "antimatter.trap." + tier.id() + ".";
            CAPACITY.put(tier, builder.comment("Antimatter a " + tier.id() + " Penning trap holds, in nanograms.")
                    .defineInRange(key + "capacity", d.capacity(), 0.001, 1.0e6));
            IDLE_FE.put(tier, builder.comment("Forge Energy per tick its field costs, loaded or not.")
                    .defineInRange(key + "idleFe", d.idleFe(), 0.0, 1.0e6));
            LOAD_FE.put(tier, builder.comment("Extra Forge Energy per tick at full charge, in proportion to the fill.")
                    .defineInRange(key + "loadFe", d.loadFe(), 0.0, 1.0e6));
        }
        PenningTrapModel.Params d = PenningTrapModel.Params.DEFAULT;
        BUFFER_TICKS = builder.comment("Ticks of full demand a trap's own energy buffer holds when its power is cut.")
                .defineInRange("antimatter.trap.bufferTicks", d.bufferTicks(), 1, 72000);
        QUENCH_TICKS = builder.comment("Ticks from a loaded trap losing its field to the annihilation (the quench alarm).")
                .defineInRange("antimatter.trap.quenchTicks", d.quenchTicks(), 1, 1200);
        LOW_POWER_SHARE = builder.comment("Share of the energy buffer below which a trap warns of low power.")
                .defineInRange("antimatter.trap.lowPowerShare", d.lowPowerShare(), 0.0, 1.0);
        VialBattery.Params v = VialBattery.Params.DEFAULT;
        VIAL_CAPACITY = builder.comment("Antimatter an Antimatter Vial holds, in nanograms.")
                .defineInRange("antimatter.vial.capacity", v.capacity(), 0.001, 1.0e6);
        VIAL_BATTERY = builder.comment("Ticks a vial's battery holds its field outside a powered trap; then it annihilates.")
                .defineInRange("antimatter.vial.batteryTicks", v.batteryTicks(), 1, 1_000_000);
        VIAL_RECHARGE = builder.comment("Battery ticks a powered trap's vial port puts back per tick.")
                .defineInRange("antimatter.vial.rechargePerTick", v.rechargePerTick(), 0, 1_000_000);
        VIAL_TRANSFER = builder.comment("Antimatter a trap's vial port moves into or out of a vial per tick, in nanograms.")
                .defineInRange("antimatter.vial.transferPerTick", v.transferPerTick(), 0.0001, 1.0e6);
        ConduitTransfer.Params c = ConduitTransfer.Params.DEFAULT;
        CONDUIT_FE = builder.comment("Forge Energy per tick every block of Antimatter Conduit needs. A network short of it moves nothing.")
                .defineInRange("antimatter.conduit.fePerBlock", c.fePerBlock(), 0, 1_000_000);
        CONDUIT_RATE = builder.comment("Antimatter one conduit network moves per tick, in nanograms.")
                .defineInRange("antimatter.conduit.ratePerTick", c.ratePerTick(), 0.0, 1.0e6);
        CONDUIT_MAX = builder.comment("Longest conduit network that still works, in blocks.")
                .defineInRange("antimatter.conduit.maxSegments", c.maxSegments(), 1, 4096);
        AnnihilationBlastModel.Params b = AnnihilationBlastModel.Params.DEFAULT;
        BLAST_MAX_RADIUS = builder.comment("Largest annihilation explosion, in blocks: 2 + 1.2 x cube root of the nanograms, at least 3.")
                .defineInRange("antimatter.blast.maxRadius", b.maxRadius(), b.minRadius(), 32.0);
        BLAST_DOSE_PER_NG = builder.comment("Gamma dose at the centre of an annihilation per nanogram; it reaches twice the blast radius.")
                .defineInRange("antimatter.blast.dosePerNanogram", b.dosePerNg(), 0.0, 1.0e6);
        BLAST_MAX_DOSE = builder.comment("Highest gamma dose at the centre of an annihilation.")
                .defineInRange("antimatter.blast.maxCentreDose", b.maxCentreDose(), 0.0, 1.0e6);
        BLAST_BREAKS_BLOCKS = builder.comment("Whether an annihilation breaks blocks, or only hurts and irradiates what is near.")
                .define("antimatter.blast.breaksBlocks", true);
        InjectorModel.Params i = InjectorModel.Params.DEFAULT;
        INJECTOR_COOLDOWN = builder.comment("Ticks an Antiproton Injector waits after a shot before it may fire again.")
                .defineInRange("antimatter.injector.cooldownTicks", i.cooldownTicks(), 0, 72000);
        INJECTOR_SHOT_FE = builder.comment("Forge Energy one shot of an Antiproton Injector takes from its buffer (the beam nozzle's pulse).",
                        "Its dose is antimatter.ignitionDose.")
                .defineInRange("antimatter.injector.shotFe", i.shotFe(), 0, 100_000_000);
    }

    public static void bake() {
        int bufferTicks = BUFFER_TICKS.get();
        int quenchTicks = QUENCH_TICKS.get();
        double lowShare = LOW_POWER_SHARE.get();
        for (TrapTier tier : TrapTier.values()) {
            TRAPS.put(tier, new PenningTrapModel.Params(CAPACITY.get(tier).get(), IDLE_FE.get(tier).get(), LOAD_FE.get(tier).get(),
                    bufferTicks, quenchTicks, lowShare));
        }
        VialBattery.Params v = VialBattery.Params.DEFAULT;
        vial = new VialBattery.Params(VIAL_CAPACITY.get(), VIAL_BATTERY.get(), VIAL_RECHARGE.get(), v.lowShare(), VIAL_TRANSFER.get());
        conduit = new ConduitTransfer.Params(CONDUIT_FE.get(), CONDUIT_RATE.get(), CONDUIT_MAX.get());
        AnnihilationBlastModel.Params b = AnnihilationBlastModel.Params.DEFAULT;
        blast = new AnnihilationBlastModel.Params(b.baseRadius(), b.radiusPerCbrtNg(), b.minRadius(), BLAST_MAX_RADIUS.get(),
                b.pulseRadiusFactor(), BLAST_DOSE_PER_NG.get(), BLAST_MAX_DOSE.get(), b.fizzleNg());
        blastBreaksBlocks = BLAST_BREAKS_BLOCKS.get();
        injectorCooldownTicks = INJECTOR_COOLDOWN.get();
        injectorShotFe = INJECTOR_SHOT_FE.get();
    }

    /** The Antiproton Injector's numbers: {@code antimatter.ignitionDose} and the {@code antimatter.injector.*} keys. */
    public static InjectorModel.Params injector() {
        if (injectorOverride != null) {
            return injectorOverride;
        }
        InjectorModel.Params d = InjectorModel.Params.DEFAULT;
        return new InjectorModel.Params(AcceleratorConfig.ignitionDose, injectorCooldownTicks, injectorShotFe, d.minConfinement(),
                d.minDensity(), d.ignitionTemp());
    }

    /** Replaces the injector's numbers (game tests, inside try/finally); null goes back to the config. */
    public static void setInjector(InjectorModel.Params params) {
        injectorOverride = params;
    }

    /** A trap tier's numbers, from the current config. */
    public static PenningTrapModel.Params trap(TrapTier tier) {
        return TRAPS.get(tier);
    }

    public static VialBattery.Params vial() {
        return vial;
    }

    public static ConduitTransfer.Params conduit() {
        return conduit;
    }

    public static AnnihilationBlastModel.Params blast() {
        return blast;
    }

    /** Replaces a trap tier's numbers (game tests, inside try/finally). */
    public static void setTrap(TrapTier tier, PenningTrapModel.Params params) {
        TRAPS.put(tier, params);
    }

    /** Replaces the vial's numbers (game tests, inside try/finally). */
    public static void setVial(VialBattery.Params params) {
        vial = params;
    }

    /** Replaces the conduit's numbers (game tests, inside try/finally). */
    public static void setConduit(ConduitTransfer.Params params) {
        conduit = params;
    }
}
