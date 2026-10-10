package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.FusionViewportBlock;
import org.papiricoh.create_nuclearindustry.fusion.heating.FusionViewportBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.HeatingDrive;
import org.papiricoh.create_nuclearindustry.fusion.heating.NeutralBeamInjectorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingConfig;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;
import org.papiricoh.create_nuclearindustry.fusion.heating.RfAntennaBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.ViewportGlow;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure;

/**
 * Neutral beam injectors, radio-frequency antennas and viewports in a real R=4 tokamak shell
 * (see {@link TokamakTestBuilder}): the heaters offer the controller their power for the shaft
 * speed, and the windows light up from what the controller says the plasma is doing.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PlasmaHeatingTests {
    private static final int RADIUS = 4;
    /** Lowered beam stress lets one creative motor (16384 SU/RPM) drive an injector; restored after. */
    private static final String BEAM_BATCH = "plasma_heating_beam";
    private static final double TEST_BEAM_STRESS = 1024.0;

    /** North outer equator of the shell, and the cells above and below it. */
    private static BlockPos northWall(int dy) {
        return TokamakTestBuilder.CENTER.offset(0, dy, -TokamakGeometry.controllerToAxis(RADIUS));
    }

    /** Puts a heater in the shell facing out north, with a creative motor on its shaft (none at 0 RPM). */
    private static void heater(GameTestHelper helper, BlockPos pos, Block heater, int rpm) {
        helper.setBlock(pos, heater.defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.NORTH));
        if (rpm == 0) {
            return;
        }
        BlockPos motorPos = pos.north();
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(motorPos);
        motorEntity.generatedSpeed.setValue(rpm);
    }

    /** The block entity at {@code pos} if it is of this type; never throws inside a test callback. */
    @Nullable
    private static <T> T entity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    // ------------------------------------------------------------------ neutral beam injector

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = BEAM_BATCH)
    public static void beamInjectorOffersRatedPowerAndAsksForDeuterium(GameTestHelper helper) {
        beamAtSpeed(helper, 128);
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = BEAM_BATCH)
    public static void beamInjectorPowerFollowsShaftSpeed(GameTestHelper helper) {
        beamAtSpeed(helper, 80);
    }

    /**
     * The batch sets up all its tests before any of them runs, so the tests share one override:
     * the first lowers the stress, the last to finish puts the configured value back.
     */
    private static int beamOverrides;
    private static double savedBeamStress;

    private static void lowerBeamStress() {
        if (beamOverrides++ == 0) {
            savedBeamStress = PlasmaHeatingConfig.neutralBeamStress;
            PlasmaHeatingConfig.neutralBeamStress = TEST_BEAM_STRESS;
        }
    }

    private static void restoreBeamStress() {
        if (beamOverrides > 0 && --beamOverrides == 0) {
            PlasmaHeatingConfig.neutralBeamStress = savedBeamStress;
        }
    }

    private static void beamAtSpeed(GameTestHelper helper, int rpm) {
        lowerBeamStress();
        try {
            TokamakTestBuilder.build(helper, RADIUS, true, 0);
            heater(helper, northWall(0), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get(), rpm);
        } catch (RuntimeException e) {
            restoreBeamStress();
            throw e;
        }
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        double expected = HeatingDrive.power(rpm, PlasmaHeatingConfig.neutralBeam());
        helper.runAfterDelay(40, () -> {
            try {
                helper.assertTrue(tokamak.getStructure().isPresent(), "tokamak should form with an injector: " + tokamak.getValidationMessage());
                NeutralBeamInjectorBlockEntity beam = entity(helper, northWall(0), NeutralBeamInjectorBlockEntity.class);
                helper.assertTrue(beam != null && !beam.isOverStressed(), "the injector should run without overstress");
                TokamakSupply supply = tokamak.getSupply();
                helper.assertTrue(supply.nbiCount() == 1, "one injector should run, got " + supply.nbiCount());
                helper.assertTrue(near(supply.nbiPower(), expected), "beam power at " + rpm + " RPM should be " + expected
                        + " MW, got " + supply.nbiPower());
                double deuterium = HeatingDrive.beamDeuterium(expected, PlasmaHeatingConfig.neutralBeamDeuteriumPerMw);
                helper.assertTrue(deuterium > 0 && near(supply.nbiDeuteriumDemand(), deuterium),
                        "beams should ask for " + deuterium + " mB/t of deuterium, got " + supply.nbiDeuteriumDemand());
                helper.assertTrue(supply.rfPower() == 0 && supply.rfCount() == 0, "no antenna in this shell");
            } finally {
                restoreBeamStress();
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void stoppedBeamInjectorOffersNothing(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        heater(helper, northWall(0), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get(), 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "tokamak should form: " + tokamak.getValidationMessage());
            TokamakSupply supply = tokamak.getSupply();
            helper.assertTrue(supply.nbiPower() == 0 && supply.nbiCount() == 0 && supply.nbiDeuteriumDemand() == 0,
                    "a still injector offers nothing, got " + supply);
            helper.succeed();
        });
    }

    /** At its real stress, one creative motor cannot drive an injector: overstressed, it gives nothing. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void overstressedBeamInjectorOffersNothing(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        heater(helper, northWall(0), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get(), 128);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(40, () -> {
            NeutralBeamInjectorBlockEntity beam = entity(helper, northWall(0), NeutralBeamInjectorBlockEntity.class);
            helper.assertTrue(beam != null && beam.isOverStressed(), "32768 SU/RPM should overstress one creative motor");
            helper.assertTrue(tokamak.getSupply().nbiPower() == 0, "an overstressed injector gives no power");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ radio-frequency antenna

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void antennaOffersRatedPower(GameTestHelper helper) {
        antennaAtSpeed(helper, 64);
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void antennaPowerFollowsShaftSpeed(GameTestHelper helper) {
        antennaAtSpeed(helper, 40);
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void stoppedAntennaOffersNothing(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        heater(helper, northWall(1), PlasmaHeatingRegistry.RF_ANTENNA.get(), 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "tokamak should form: " + tokamak.getValidationMessage());
            helper.assertTrue(tokamak.getSupply().rfPower() == 0 && tokamak.getSupply().rfCount() == 0,
                    "a still antenna offers nothing, got " + tokamak.getSupply());
            helper.succeed();
        });
    }

    private static void antennaAtSpeed(GameTestHelper helper, int rpm) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        heater(helper, northWall(1), PlasmaHeatingRegistry.RF_ANTENNA.get(), rpm);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        double expected = HeatingDrive.power(rpm, PlasmaHeatingConfig.radioFrequency());
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "tokamak should form with an antenna: " + tokamak.getValidationMessage());
            RfAntennaBlockEntity antenna = entity(helper, northWall(1), RfAntennaBlockEntity.class);
            helper.assertTrue(antenna != null && !antenna.isOverStressed(), "one creative motor should drive the antenna");
            TokamakSupply supply = tokamak.getSupply();
            helper.assertTrue(supply.rfCount() == 1, "one antenna should run, got " + supply.rfCount());
            helper.assertTrue(near(supply.rfPower(), expected), "antenna power at " + rpm + " RPM should be " + expected
                    + " MW, got " + supply.rfPower());
            helper.assertTrue(supply.nbiPower() == 0 && supply.nbiDeuteriumDemand() == 0, "antennas burn no deuterium");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ structure

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void tokamakFormsWithHeatersAndViewports(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        heater(helper, northWall(0), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get(), 0);
        heater(helper, northWall(1), PlasmaHeatingRegistry.RF_ANTENNA.get(), 0);
        helper.setBlock(northWall(-1), PlasmaHeatingRegistry.VIEWPORT.get());
        BlockPos roof = TokamakTestBuilder.CENTER.offset(RADIUS, TokamakGeometry.HALF_HEIGHT, 0);
        helper.setBlock(roof, PlasmaHeatingRegistry.VIEWPORT.get());
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(tokamak.getStructure().isPresent(), "heaters and windows are shell blocks: " + tokamak.getValidationMessage());
            TokamakStructure structure = tokamak.getStructure().get();
            helper.assertTrue(structure.count(TokamakShellRole.NBI) == 1, "one injector, got " + structure.count(TokamakShellRole.NBI));
            helper.assertTrue(structure.count(TokamakShellRole.RF_ANTENNA) == 1, "one antenna");
            helper.assertTrue(structure.count(TokamakShellRole.VIEWPORT) == 2, "two viewports, got " + structure.count(TokamakShellRole.VIEWPORT));
            helper.assertTrue(structure.containsPart(helper.absolutePos(northWall(-1))), "a viewport is a functional part");
            // The controller tells its parts about the plasma every few ticks.
            NeutralBeamInjectorBlockEntity beam = entity(helper, northWall(0), NeutralBeamInjectorBlockEntity.class);
            helper.assertTrue(beam != null && beam.isInTokamak(), "the injector should hear from its controller");
            FusionViewportBlockEntity window = entity(helper, northWall(-1), FusionViewportBlockEntity.class);
            helper.assertTrue(window != null, "the viewport keeps its block entity");
            if (!tokamak.phase().hasPlasma()) {
                helper.assertTrue(window.glow() == 0, "no plasma, no light, got " + window.glow());
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 200)
    public static void breakingTheControllerSilencesTheHeaters(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        heater(helper, northWall(0), PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR.get(), 0);
        helper.runAfterDelay(20, () -> {
            NeutralBeamInjectorBlockEntity beam = entity(helper, northWall(0), NeutralBeamInjectorBlockEntity.class);
            helper.assertTrue(beam != null && beam.isInTokamak(), "the injector should first join the tokamak");
            helper.destroyBlock(TokamakTestBuilder.controller(RADIUS));
        });
        helper.runAfterDelay(120, () -> {
            NeutralBeamInjectorBlockEntity beam = entity(helper, northWall(0), NeutralBeamInjectorBlockEntity.class);
            helper.assertTrue(beam != null && !beam.isInTokamak(), "without its controller the injector is on its own");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ viewport

    private static TokamakReadings plasma(TokamakPhase phase, double temperature) {
        return new TokamakReadings(true, false, RADIUS, phase, TripCause.NONE, temperature, 0, 0.9, 0.6, 0.02,
                phase == TokamakPhase.BURN ? 200 : 0, 20, 10, 0.1, 1.1, true, 1, 1, 4.5, -4, 1, 0.5, true, 1,
                1000, 1000, 0, 0, 0, 0, false);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void viewportGlowFollowsThePlasma(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, PlasmaHeatingRegistry.VIEWPORT.get());
        FusionViewportBlockEntity window = entity(helper, pos, FusionViewportBlockEntity.class);
        helper.assertTrue(window != null, "the viewport needs its block entity");
        helper.assertTrue(window.glow() == 0, "a new viewport is dark");

        TokamakReadings breakdown = plasma(TokamakPhase.BREAKDOWN, 40);
        window.observe(breakdown);
        helper.assertTrue(window.glow() == ViewportGlow.BREAKDOWN, "a dim glow at breakdown, got " + window.glow());

        TokamakReadings burning = plasma(TokamakPhase.BURN, 4500);
        window.observe(burning);
        BlockState lit = helper.getBlockState(pos);
        helper.assertTrue(lit.getValue(FusionViewportBlock.GLOW) == ViewportGlow.MAX, "full glow while burning hot");
        helper.assertTrue(lit.getLightEmission() == ViewportGlow.MAX, "the glow is the block's light");

        window.observe(TokamakReadings.NONE);
        helper.assertTrue(window.glow() == 0, "dark again when the tokamak breaks");
        window.observe(plasma(TokamakPhase.RAMP_DOWN, 3000));
        helper.assertTrue(window.glow() == 0, "dark once the plasma is out");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void viewportGoesDarkWhenTheControllerFallsSilent(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, PlasmaHeatingRegistry.VIEWPORT.get());
        FusionViewportBlockEntity window = entity(helper, pos, FusionViewportBlockEntity.class);
        helper.assertTrue(window != null, "the viewport needs its block entity");
        window.observe(plasma(TokamakPhase.HEATING, 2500));
        helper.assertTrue(window.glow() > ViewportGlow.BREAKDOWN, "lit by a heating plasma");
        helper.runAfterDelay(30, () -> {
            FusionViewportBlockEntity now = entity(helper, pos, FusionViewportBlockEntity.class);
            helper.assertTrue(now != null && now.glow() > 0, "still lit shortly after");
        });
        helper.runAfterDelay(110, () -> {
            FusionViewportBlockEntity now = entity(helper, pos, FusionViewportBlockEntity.class);
            helper.assertTrue(now != null && now.glow() == 0, "dark after a few seconds without news, got "
                    + (now == null ? "nothing" : now.glow()));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void viewportsCanBeDimmedInTheConfig(GameTestHelper helper) {
        BlockPos pos = new BlockPos(6, 2, 6);
        helper.setBlock(pos, PlasmaHeatingRegistry.VIEWPORT.get());
        FusionViewportBlockEntity window = entity(helper, pos, FusionViewportBlockEntity.class);
        helper.assertTrue(window != null, "the viewport needs its block entity");
        boolean light = PlasmaHeatingConfig.viewportLight;
        PlasmaHeatingConfig.viewportLight = false;
        try {
            window.observe(plasma(TokamakPhase.BURN, 4500));
            helper.assertTrue(window.glow() == 0, "no light with plasmaHeating.viewportLight off");
        } finally {
            PlasmaHeatingConfig.viewportLight = light;
        }
        helper.succeed();
    }
}
