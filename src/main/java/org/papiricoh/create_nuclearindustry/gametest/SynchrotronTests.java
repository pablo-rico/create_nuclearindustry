package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorCryoFeedBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.TargetStationBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AcceleratorStructure;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.energy.EnergyRegistry;

/**
 * The synchrotron ring ({@link AcceleratorTestBuilder#ring}): it forms from the injection septum,
 * fills, ramps and extracts into its target station; superconducting dipoles need liquid helium,
 * which a cryogenic feed beside the ring hands to the controller. The target station makes
 * antiprotons for the traps beside it only above {@code accelerator.antimatter.thresholdGev}.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SynchrotronTests {
    /** Beside the ring's northernmost cell, outside the ring. */
    private static final BlockPos CRYO = AcceleratorTestBuilder.RING_CENTER.north(AcceleratorTestBuilder.RING_RADIUS + 1);
    /** A ring whose superconducting dipoles still leave its top energy within the injector's reach. */
    private static final int SC_RING_DIPOLES = 20;
    private static final int SC_DIPOLES = 2;

    // ------------------------------------------------------------------ the ring

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aRingFormsBetweenItsSepta(GameTestHelper helper) {
        AcceleratorTestBuilder.ring(helper, AcceleratorTestBuilder.RING_DIPOLES, 0);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.ringController(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.isFormed(), "the synchrotron should form: " + controller.validationText().getString());
        AcceleratorStructure s = controller.getStructure().orElse(null);
        helper.assertTrue(s != null && s.ring != null, "with a ring");
        helper.assertTrue(s.ring.radius() == AcceleratorTestBuilder.RING_RADIUS, "of radius 6, found " + s.ring.radius());
        helper.assertTrue(s.ring.center().equals(helper.absolutePos(AcceleratorTestBuilder.RING_CENTER)), "round its centre");
        helper.assertTrue(s.ring.count(BeamElementRole.DIPOLE) == AcceleratorTestBuilder.RING_DIPOLES,
                "24 dipoles, found " + s.ring.count(BeamElementRole.DIPOLE));
        helper.assertTrue(s.ring.injection().equals(helper.absolutePos(AcceleratorTestBuilder.INJECTION)), "fed by the injection septum");
        helper.assertTrue(s.ring.extraction().equals(helper.absolutePos(AcceleratorTestBuilder.EXTRACTION)), "left by the extraction septum");
        helper.assertTrue(s.terminalRole() == BeamElementRole.TARGET, "the extraction line ends in the target station: " + s.terminalRole());
        helper.assertTrue(s.terminal().equals(helper.absolutePos(AcceleratorTestBuilder.TARGET)), "at its place");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aRingWithTwoExtractionsDoesNotForm(GameTestHelper helper) {
        AcceleratorTestBuilder.ring(helper, AcceleratorTestBuilder.RING_DIPOLES, 0);
        BlockPos second = AcceleratorTestBuilder.RING_CENTER.south(AcceleratorTestBuilder.RING_RADIUS);
        helper.setBlock(second, AcceleratorRegistry.EXTRACTION_SEPTUM.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.ringController(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertFalse(controller.isFormed(), "a ring has one way out");
        helper.assertTrue(controller.getValidationKey().endsWith("error.two_extractions"), "and says so: " + controller.getValidationKey());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 500)
    public static void aRingFillsRampsAndExtractsIntoTheTarget(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.ringReady(helper, AcceleratorTestBuilder.RING_DIPOLES, 0);
        helper.assertTrue(controller != null && controller.isFormed(), "the ring should form");
        BeamTripCause start = controller.requestStart();
        helper.assertTrue(start == BeamTripCause.NONE, "the start is accepted: " + start);
        boolean[] injected = {false};
        boolean[] ramped = {false};
        helper.onEachTick(() -> {
            injected[0] |= controller.phase() == BeamPhase.INJECTION;
            ramped[0] |= controller.phase() == BeamPhase.RAMP;
        });
        helper.succeedWhen(() -> {
            BeamReadings r = controller.readings();
            helper.assertTrue(r.phase() == BeamPhase.BEAM_ON, "waiting for beam, at " + r.phase() + " (" + r.tripCause() + ")");
            helper.assertTrue(injected[0] && ramped[0], "the ring filled and ramped first");
            helper.assertTrue(r.energy() > 1000 && Math.abs(r.energy() - r.maxEnergy()) < 1, "at its top energy of about 1.9 GeV: "
                    + r.energy() + " of " + r.maxEnergy());
            TargetStationBlockEntity target = AcceleratorTestBuilder.target(helper);
            helper.assertTrue(target != null && target.incomingPower() > 0, "the target station takes the extracted beam");
            helper.assertFalse(target.aboveThreshold(), "a 2 GeV beam is below the antiproton threshold");
            helper.assertTrue(target.productionPerMinute() == 0, "so it makes no antiprotons");
        });
    }

    // ------------------------------------------------------------------ superconducting dipoles

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void superconductingDipolesWithoutHeliumRefuseTheStart(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.ringReady(helper, SC_RING_DIPOLES, SC_DIPOLES);
        helper.assertTrue(controller != null && controller.isFormed(), "the ring should form");
        AcceleratorStructure s = controller.getStructure().orElse(null);
        helper.assertTrue(s != null && s.ring != null && s.ring.count(BeamElementRole.SC_DIPOLE) == SC_DIPOLES, "with two superconducting dipoles");
        BeamTripCause start = controller.requestStart();
        helper.assertTrue(start == BeamTripCause.MAGNETS_WARM, "warm superconducting magnets cannot start: " + start);
        helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "it stays offline");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 400)
    public static void superconductingDipolesTripWhenTheHeliumRunsOut(GameTestHelper helper) {
        helper.setBlock(CRYO, AcceleratorRegistry.CRYO_FEED.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.SOUTH));
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.ringReady(helper, SC_RING_DIPOLES, SC_DIPOLES);
        helper.assertTrue(controller != null && controller.isFormed(), "the ring should form");
        // Barely a drop: enough to start the cool-down, never enough to finish it.
        controller.liquidHeliumTank().fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), 2), IFluidHandler.FluidAction.EXECUTE);
        BeamTripCause start = controller.requestStart();
        helper.assertTrue(start == BeamTripCause.NONE, "with some helium the start is accepted: " + start);
        boolean[] cooled = {false};
        helper.onEachTick(() -> cooled[0] |= controller.phase() == BeamPhase.COOL_DOWN);
        helper.succeedWhen(() -> {
            helper.assertTrue(cooled[0], "it went through the cool-down");
            helper.assertTrue(controller.tripCause() == BeamTripCause.MAGNETS_WARM,
                    "and tripped once the helium was gone, at " + controller.phase() + " (" + controller.tripCause() + ")");
            helper.assertTrue(controller.phase() == BeamPhase.OFFLINE || controller.phase() == BeamPhase.RAMP_DOWN,
                    "winding down: " + controller.phase());
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 100)
    public static void aCryoFeedBesideTheRingFillsTheController(GameTestHelper helper) {
        helper.setBlock(CRYO, AcceleratorRegistry.CRYO_FEED.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.SOUTH));
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.ringReady(helper, SC_RING_DIPOLES, SC_DIPOLES);
        helper.assertTrue(controller != null && controller.isFormed(), "the ring should form");
        AcceleratorStructure s = controller.getStructure().orElse(null);
        helper.assertTrue(s != null && s.attachments(AttachmentRole.CRYO).contains(helper.absolutePos(CRYO)),
                "the feed is one of the ring's machines");
        AcceleratorCryoFeedBlockEntity feed = AcceleratorTestBuilder.entity(helper, CRYO, AcceleratorCryoFeedBlockEntity.class);
        helper.assertTrue(feed != null, "the feed should be there");
        // The controller binds its machines when it looks them up.
        controller.attachments();
        IFluidHandler pipe = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(CRYO), Direction.NORTH);
        helper.assertTrue(pipe != null, "pipes reach the feed");
        helper.assertTrue(pipe.fill(new FluidStack(AllNuclearFluids.HYDROGEN.get(), 500), IFluidHandler.FluidAction.EXECUTE) == 0,
                "it only takes liquid helium");
        int filled = pipe.fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == 1000, "the helium goes in: " + filled);
        helper.assertTrue(controller.liquidHeliumTank().getFluidAmount() == 1000, "straight into the controller's tank");
        helper.assertTrue(controller.requestStart() == BeamTripCause.NONE, "and the cold magnets may start");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the target station

    /** A target station with its window west, a small trap at its collector and a creative generator feeding the trap. */
    private static final BlockPos STATION = new BlockPos(5, 2, 6);
    private static final BlockPos COLLECTED = STATION.east();
    private static final BlockPos FEED = COLLECTED.east();
    private static final double POWER_KW = 6800;

    private static void station(GameTestHelper helper) {
        helper.setBlock(STATION, AcceleratorRegistry.TARGET_STATION.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        helper.setBlock(COLLECTED, AntimatterRegistry.SMALL_PENNING_TRAP.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        helper.setBlock(FEED, EnergyRegistry.CREATIVE_RF_GENERATOR.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aboveTheThresholdTheTargetFillsTheTrapBesideIt(GameTestHelper helper) {
        station(helper);
        double energyMev = (AcceleratorConfig.antimatter().thresholdGev() * 4) * 1000;
        helper.onEachTick(() -> {
            TargetStationBlockEntity target = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            PenningTrapBlockEntity trap = AcceleratorTestBuilder.entity(helper, COLLECTED, PenningTrapBlockEntity.class);
            // Beam only once the trap's field is up: antiprotons into a trap without it annihilate.
            if (target != null && trap != null && trap.powered()) {
                target.receiveBeam(energyMev, POWER_KW * 1000 / energyMev, POWER_KW);
            }
        });
        helper.succeedWhen(() -> {
            TargetStationBlockEntity target = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            PenningTrapBlockEntity trap = AcceleratorTestBuilder.entity(helper, COLLECTED, PenningTrapBlockEntity.class);
            helper.assertTrue(target != null && trap != null, "the station and the trap should be there");
            helper.assertTrue(target.aboveThreshold(), "a 100 GeV beam is above the threshold");
            helper.assertTrue(target.productionPerMinute() > 1, "making antiprotons: " + target.productionPerMinute() + " ng/min");
            helper.assertTrue(trap.stored() > 0.1, "which the collector hands to the trap: " + trap.stored() + " ng");
            helper.assertTrue(trap.state() == TrapState.STABLE,
                    "and the trap holds them: " + trap.state());
            // Leave the trap empty: a loaded one must never be left behind in the arena.
            trap.setStored(0);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void belowTheThresholdTheTargetMakesNoAntiprotons(GameTestHelper helper) {
        station(helper);
        double energyMev = AcceleratorConfig.antimatter().thresholdGev() * 0.5 * 1000;
        helper.onEachTick(() -> {
            TargetStationBlockEntity target = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            if (target != null) {
                target.receiveBeam(energyMev, POWER_KW * 1000 / energyMev, POWER_KW);
            }
        });
        helper.runAfterDelay(60, () -> {
            TargetStationBlockEntity target = AcceleratorTestBuilder.entity(helper, STATION, TargetStationBlockEntity.class);
            PenningTrapBlockEntity trap = AcceleratorTestBuilder.entity(helper, COLLECTED, PenningTrapBlockEntity.class);
            helper.assertTrue(target != null && trap != null, "the station and the trap should be there");
            helper.assertTrue(target.incomingPower() > 0, "the station takes the beam");
            helper.assertFalse(target.aboveThreshold(), "a 12.5 GeV beam is below the threshold");
            helper.assertTrue(target.productionPerMinute() == 0 && target.stored() == 0, "it makes no antiprotons");
            helper.assertFalse(PenningTrapModel.loaded(trap.stored()), "and the trap stays empty: " + trap.stored());
            helper.assertTrue(target.activity() > 0, "it is only activated, like a beam dump");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ the Ponder scene

    @GameTest(template = "gametest/accelerator_synchrotron_ponder", timeoutTicks = 60)
    public static void theSynchrotronPonderSchematicForms(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.entity(helper, new BlockPos(0, 2, 8),
                AcceleratorControllerBlockEntity.class);
        helper.assertTrue(controller != null, "the scene's controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.isFormed(), "the synchrotron in the Ponder scene forms: " + controller.validationText().getString());
        AcceleratorStructure s = controller.getStructure().orElse(null);
        helper.assertTrue(s != null && s.ring != null && s.ring.radius() == 6, "round a radius-6 ring");
        helper.assertTrue(s.terminalRole() == BeamElementRole.TARGET, "ending in its target station");
        helper.assertTrue(!s.attachments(AttachmentRole.CRYO).isEmpty(), "with its cryogenic feed");
        helper.assertTrue(!s.attachments(AttachmentRole.INTERLOCK).isEmpty(), "and its beam interlock");
        helper.succeed();
    }
}
