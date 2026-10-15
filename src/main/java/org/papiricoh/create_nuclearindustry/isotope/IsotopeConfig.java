package org.papiricoh.create_nuclearindustry.isotope;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.isotope.physics.AmericiumIngrowth;
import org.papiricoh.create_nuclearindustry.isotope.physics.GammaIrradiation;
import org.papiricoh.create_nuclearindustry.isotope.physics.Isotope;
import org.papiricoh.create_nuclearindustry.isotope.physics.MedicalKitModel;
import org.papiricoh.create_nuclearindustry.isotope.physics.NeutronActivation;
import org.papiricoh.create_nuclearindustry.isotope.physics.RadiographyModel;
import org.papiricoh.create_nuclearindustry.isotope.physics.SmokeDetectorModel;
import org.papiricoh.create_nuclearindustry.isotope.physics.TechnetiumCow;

import java.util.EnumMap;
import java.util.Map;

/**
 * Common config for isotopes and nuclear medicine, under {@code isotopes.*}: the decay clock, the
 * half-lives and the physics models' parameters. Defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and baked into the models' {@code Params}, so
 * game code never rebuilds them.
 */
public final class IsotopeConfig {
    /** Multiplies every isotope clock: 2 makes everything decay twice as fast. */
    public static double timeScale = 1.0;
    /** Smallest share of americium a plutonium stack must have grown before it can be separated. */
    public static double americiumMinYield = 0.15;
    /** Lowest beam energy that makes a target station irradiate targets, GeV. */
    public static double targetMinEnergyGev = 1.0;
    /** Technetium-99m doses one elution gives at full activity, and the most it ever gives. */
    public static double cowKitActivity = 0.15;
    public static int cowMaxKits = 4;

    private static final Map<Isotope, Double> HALF_LIVES = new EnumMap<>(Isotope.class);
    private static NeutronActivation.Params channel = NeutronActivation.Params.DEFAULT;
    private static GammaIrradiation.Params irradiator = GammaIrradiation.Params.DEFAULT;
    private static SmokeDetectorModel.Params smoke = SmokeDetectorModel.Params.DEFAULT;
    private static AmericiumIngrowth.Params americium = AmericiumIngrowth.Params.DEFAULT;
    private static MedicalKitModel.Params kit = MedicalKitModel.Params.DEFAULT;
    private static RadiographyModel.Params radiography = RadiographyModel.Params.DEFAULT;
    private static TechnetiumCow.Params cow = TechnetiumCow.Params.DEFAULT;

    private static ModConfigSpec.DoubleValue TIME_SCALE;
    private static final Map<Isotope, ModConfigSpec.DoubleValue> HALF_LIFE = new EnumMap<>(Isotope.class);
    private static ModConfigSpec.DoubleValue CHANNEL_FLUENCE;
    private static ModConfigSpec.DoubleValue CHANNEL_ACTIVITY;
    private static ModConfigSpec.DoubleValue STERILISING_KGY;
    private static ModConfigSpec.DoubleValue KGY_PER_TICK;
    private static ModConfigSpec.IntValue SMOKE_RADIUS;
    private static ModConfigSpec.DoubleValue AMERICIUM_HALF_LIFE;
    private static ModConfigSpec.DoubleValue AMERICIUM_MAX;
    private static ModConfigSpec.DoubleValue AMERICIUM_MIN;
    private static ModConfigSpec.DoubleValue KIT_BOOST;
    private static ModConfigSpec.IntValue KIT_TICKS;
    private static ModConfigSpec.DoubleValue RADIOGRAPHY_DOSE;
    private static ModConfigSpec.DoubleValue TARGET_MIN_ENERGY;
    private static ModConfigSpec.DoubleValue COW_KIT_ACTIVITY;
    private static ModConfigSpec.IntValue COW_MAX_KITS;

    static {
        for (Isotope isotope : Isotope.values()) {
            HALF_LIVES.put(isotope, isotope.halfLifeDays());
        }
    }

    private IsotopeConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        TIME_SCALE = builder.comment("Speeds every isotope clock up (2 = twice as fast) or slows it down (0.5).")
                .defineInRange("isotopes.timeScale", 1.0, 0.001, 1000.0);
        for (Isotope isotope : Isotope.values()) {
            HALF_LIFE.put(isotope, builder.comment("Half-life of " + isotope.id() + ", in game days (24000 ticks) at time scale 1.")
                    .defineInRange("isotopes." + isotope.id() + ".halfLifeDays", isotope.halfLifeDays(), 0.001, 1.0e7));
        }
        NeutronActivation.Params c = NeutronActivation.Params.DEFAULT;
        CHANNEL_FLUENCE = builder.comment("Fluence (neutron level x ticks) that activates 63% of a target in the reactor irradiation channel.")
                .defineInRange("isotopes.channel.fluenceScale", c.fluenceScale(), 1.0, 1.0e12);
        CHANNEL_ACTIVITY = builder.comment("Share of a target the channel activates before it hands the target out as cobalt-60.")
                .defineInRange("isotopes.channel.targetActivity", c.targetActivity(), 0.01, 0.99);
        GammaIrradiation.Params g = GammaIrradiation.Params.DEFAULT;
        STERILISING_KGY = builder.comment("Dose that sterilises food under the Gamma Irradiator, kGy.")
                .defineInRange("isotopes.irradiator.sterilisingKGy", g.sterilisingKGy(), 0.01, 1000.0);
        KGY_PER_TICK = builder.comment("Dose rate of one fresh cobalt-60 source in the Gamma Irradiator, kGy per tick.")
                .defineInRange("isotopes.irradiator.kGyPerTick", g.kGyPerTickAtFull(), 0.0001, 100.0);
        SMOKE_RADIUS = builder.comment("Blocks a Smoke Detector looks for fire, campfires and lava.")
                .defineInRange("isotopes.smokeDetector.radius", SmokeDetectorModel.Params.DEFAULT.radius(), 1, 16);
        AmericiumIngrowth.Params a = AmericiumIngrowth.Params.DEFAULT;
        AMERICIUM_HALF_LIFE = builder.comment("Days for plutonium to grow half its final share of americium-241 (Pu-241 decay).")
                .defineInRange("isotopes.americium.ingrowthHalfLifeDays", a.ingrowthHalfLifeDays(), 0.01, 1.0e6);
        AMERICIUM_MAX = builder.comment("Share of americium-241 very old plutonium ends up with.")
                .defineInRange("isotopes.americium.maxYield", a.maxYield(), 0.0, 1.0);
        AMERICIUM_MIN = builder.comment("Share of americium-241 plutonium needs before the americium separation takes it.")
                .defineInRange("isotopes.americium.minYield", 0.15, 0.0, 1.0);
        MedicalKitModel.Params k = MedicalKitModel.Params.DEFAULT;
        KIT_BOOST = builder.comment("Extra strength a fresh technetium-99m dose gives potassium iodide and Prussian blue (0.5 = 50%).")
                .defineInRange("isotopes.medicalKit.maxBoost", k.maxBoost(), 0.0, 10.0);
        KIT_TICKS = builder.comment("Ticks the medical kit's tracer lasts at full activity.")
                .defineInRange("isotopes.medicalKit.boostTicks", k.boostTicks(), 1, 72000);
        RADIOGRAPHY_DOSE = builder.comment("Dose the operator of a Radiography Camera takes per shot with a fresh source.")
                .defineInRange("isotopes.radiography.operatorDose", RadiographyModel.Params.DEFAULT.operatorDosePerShot(), 0.0, 1000.0);
        TARGET_MIN_ENERGY = builder.comment("Lowest beam energy, GeV, at which a Target Station irradiates its target (it makes antiprotons above accelerator.antimatter.thresholdGev).")
                .defineInRange("isotopes.target.minEnergyGev", 1.0, 0.0, 1000.0);
        COW_KIT_ACTIVITY = builder.comment("Technetium-99m activity one dose takes from an elution.")
                .defineInRange("isotopes.technetiumGenerator.doseActivity", 0.15, 0.001, 1.0);
        COW_MAX_KITS = builder.comment("Most technetium-99m doses one elution gives.")
                .defineInRange("isotopes.technetiumGenerator.maxDoses", 4, 1, 64);
    }

    public static void bake() {
        timeScale = TIME_SCALE.get();
        HALF_LIFE.forEach((isotope, value) -> HALF_LIVES.put(isotope, value.get()));
        channel = new NeutronActivation.Params(CHANNEL_FLUENCE.get(), CHANNEL_ACTIVITY.get());
        GammaIrradiation.Params g = GammaIrradiation.Params.DEFAULT;
        irradiator = new GammaIrradiation.Params(STERILISING_KGY.get(), KGY_PER_TICK.get(), g.maxTicks(), g.emissionAtFull());
        SmokeDetectorModel.Params s = SmokeDetectorModel.Params.DEFAULT;
        smoke = new SmokeDetectorModel.Params(SMOKE_RADIUS.get(), s.halfDropSmoke(), s.alarmDrop(), s.minActivity());
        americium = new AmericiumIngrowth.Params(AMERICIUM_HALF_LIFE.get(), AMERICIUM_MAX.get());
        americiumMinYield = AMERICIUM_MIN.get();
        MedicalKitModel.Params k = MedicalKitModel.Params.DEFAULT;
        kit = new MedicalKitModel.Params(k.minActivity(), KIT_BOOST.get(), KIT_TICKS.get());
        RadiographyModel.Params r = RadiographyModel.Params.DEFAULT;
        radiography = new RadiographyModel.Params(r.minActivity(), r.exposureTicksAtFull(), r.maxExposureTicks(), r.fullPenetration(),
                r.halfValueBlocks(), RADIOGRAPHY_DOSE.get());
        targetMinEnergyGev = TARGET_MIN_ENERGY.get();
        cowKitActivity = COW_KIT_ACTIVITY.get();
        cowMaxKits = COW_MAX_KITS.get();
        TechnetiumCow.Params w = TechnetiumCow.Params.DEFAULT;
        cow = new TechnetiumCow.Params(HALF_LIVES.get(Isotope.MOLYBDENUM_99), HALF_LIVES.get(Isotope.TECHNETIUM_99M), w.branching(),
                w.elutionEfficiency(), w.exhaustedBelow());
    }

    public static double halfLifeDays(Isotope isotope) {
        return HALF_LIVES.get(isotope);
    }

    public static NeutronActivation.Params channel() {
        return channel;
    }

    public static GammaIrradiation.Params irradiator() {
        return irradiator;
    }

    public static SmokeDetectorModel.Params smoke() {
        return smoke;
    }

    public static AmericiumIngrowth.Params americium() {
        return americium;
    }

    public static MedicalKitModel.Params kit() {
        return kit;
    }

    public static RadiographyModel.Params radiography() {
        return radiography;
    }

    public static TechnetiumCow.Params cow() {
        return cow;
    }
}
