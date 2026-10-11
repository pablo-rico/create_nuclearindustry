package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorConfig;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorSupply;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorMachineBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.BeamDumpBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.gui.AcceleratorControlPayload;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AcceleratorStructure;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;
import org.papiricoh.create_nuclearindustry.accelerator.physics.BeamModel;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

/**
 * The particle accelerator's linac (see {@link AcceleratorTestBuilder}): it forms from its controller
 * along the beamline, starts through its sequence, delivers beam to the dump, runs on Forge Energy
 * as well as rotation, and its dump irradiates while the beam runs.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AcceleratorTests {
    private static final int CAVITIES = 8;

    // ------------------------------------------------------------------ formation

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void aLinacFormsAlongItsBeamline(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        AcceleratorTestBuilder.machines(helper);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.isFormed(), "the linac should form: " + controller.validationText().getString());
        AcceleratorStructure s = controller.getStructure().orElse(null);
        helper.assertTrue(s != null, "a formed linac has a structure");
        helper.assertTrue(s.copperCavities() == CAVITIES, "8 cavities, found " + s.copperCavities());
        helper.assertTrue(s.injector.count(BeamElementRole.QUADRUPOLE) == 4, "4 quadrupoles");
        helper.assertTrue(s.terminalRole() == BeamElementRole.DUMP, "it ends in the dump");
        helper.assertTrue(s.terminal().equals(helper.absolutePos(AcceleratorTestBuilder.DUMP)), "at the dump's place");
        for (AttachmentRole role : new AttachmentRole[]{AttachmentRole.KLYSTRON, AttachmentRole.MAGNET_POWER, AttachmentRole.VACUUM_PUMP}) {
            helper.assertTrue(s.attachments(role).size() == 1, "one " + role + " beside the line");
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void aBlockTurnedAcrossTheBeamBreaksTheLine(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        helper.setBlock(AcceleratorTestBuilder.cell(5), AcceleratorTestBuilder.along(AcceleratorRegistry.BEAM_PIPE.get(), Direction.Axis.X));
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertFalse(controller.isFormed(), "a pipe turned across the beam stops it forming");
        helper.assertTrue(controller.getValidationKey().endsWith("error.misaligned"), "and says so: " + controller.getValidationKey());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void aLineWithoutAnEndDoesNotForm(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        helper.setBlock(AcceleratorTestBuilder.DUMP, Blocks.AIR.defaultBlockState());
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertFalse(controller.isFormed(), "a beamline must end in a dump or target");
        helper.assertTrue(controller.getValidationKey().endsWith("error.gap"), "the line is open: " + controller.getValidationKey());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void theSourceMustSitInFrontOfTheController(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        helper.setBlock(AcceleratorTestBuilder.SOURCE, AcceleratorTestBuilder.along(AcceleratorRegistry.BEAM_PIPE.get(), Direction.Axis.Z));
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.getValidationKey().endsWith("error.no_source"), "no source: " + controller.getValidationKey());
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void aBendingMagnetTurnsTheLine(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, 2, true);
        // Turn east at the ninth block and end four blocks further on.
        BlockPos bend = AcceleratorTestBuilder.cell(8);
        helper.setBlock(bend, AcceleratorRegistry.BENDING_MAGNET.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        for (int i = 9; i < AcceleratorTestBuilder.LENGTH; i++) {
            helper.setBlock(AcceleratorTestBuilder.cell(i), Blocks.AIR.defaultBlockState());
        }
        helper.setBlock(AcceleratorTestBuilder.DUMP, Blocks.AIR.defaultBlockState());
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(bend.east(x), AcceleratorTestBuilder.along(AcceleratorRegistry.BEAM_PIPE.get(), Direction.Axis.X));
        }
        helper.setBlock(bend.east(4), AcceleratorRegistry.BEAM_DUMP.get().defaultBlockState());
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.isFormed(), "a bent line forms: " + controller.validationText().getString());
        helper.assertTrue(controller.injectorPath().size() == 4, "source, entry, bend and dump: " + controller.injectorPath());
        helper.assertTrue(controller.getStructure().map(s -> s.terminal().equals(helper.absolutePos(bend.east(4)))).orElse(false),
                "and ends east of the bend");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void breakingTheLineUnformsIt(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.isFormed(), "formed first");
        // Once the controller is loaded and indexed, it hears the break.
        helper.runAfterDelay(5, () -> helper.destroyBlock(AcceleratorTestBuilder.cell(6)));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 5, "waiting for the break");
            helper.assertFalse(controller.isFormed(), "a broken line unforms the accelerator");
        });
    }

    @GameTest(template = "gametest/accelerator_linac_ponder")
    public static void theLinacPonderSchematicForms(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.entity(helper, new BlockPos(0, 1, 2),
                AcceleratorControllerBlockEntity.class);
        helper.assertTrue(controller != null, "the scene's controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.isFormed(), "the linac in the Ponder scene forms: " + controller.validationText().getString());
        AcceleratorStructure s = controller.getStructure().orElse(null);
        helper.assertTrue(s != null && s.attachments(AttachmentRole.KLYSTRON).size() == 1
                && s.attachments(AttachmentRole.MAGNET_POWER).size() == 1 && s.attachments(AttachmentRole.VACUUM_PUMP).size() == 1,
                "with its three machines beside the line");
        helper.succeed();
    }

    // ------------------------------------------------------------------ startup and beam

    @GameTest(template = NuclearGameTests.LONG_ARENA, timeoutTicks = 300)
    public static void aReadyLinacSendsBeamIntoTheDump(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.startReady(helper, CAVITIES);
        helper.assertTrue(controller != null, "the controller should be there");
        helper.assertTrue(controller.phase() == BeamPhase.PUMP_DOWN, "the start is accepted: " + controller.tripCause());
        double expected = BeamModel.linacEnergy(AcceleratorConfig.beam(), CAVITIES, 0, 1);
        helper.succeedWhen(() -> {
            BeamReadings r = controller.readings();
            helper.assertTrue(r.phase() == BeamPhase.BEAM_ON, "waiting for beam, at " + r.phase());
            helper.assertTrue(Math.abs(r.energy() - expected) < 1e-6, "energy " + r.energy() + " should be " + expected);
            helper.assertTrue(r.current() > 0.9 * AcceleratorConfig.sourceCurrent, "most of the current arrives: " + r.current());
            BeamDumpBlockEntity dump = AcceleratorTestBuilder.dump(helper);
            helper.assertTrue(dump != null && dump.incomingPower() > 0, "the dump takes the beam");
        });
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void noHydrogenNoStart(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        controller.setSupplyOverride(AcceleratorTestBuilder.PLENTY);
        helper.assertTrue(controller.requestStart() == BeamTripCause.NO_ION_GAS, "an empty source refuses the start");
        helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "and it stays offline");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void noMachinesNoStart(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        AcceleratorTestBuilder.fillGas(helper, 1000);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        helper.assertTrue(controller.requestStart() == BeamTripCause.VACUUM_LOST, "without pumps it cannot reach beam vacuum");
        controller.setSupplyOverride(new AcceleratorSupply(0, 1000, 10, 0, 1, 1, 1));
        helper.assertTrue(controller.requestStart() == BeamTripCause.NO_RF_POWER, "without klystrons the cavities have no power");
        controller.setSupplyOverride(new AcceleratorSupply(100, 0, 10, 1, 0, 1, 1));
        helper.assertTrue(controller.requestStart() == BeamTripCause.NO_MAGNET_POWER, "without supplies the magnets stay off");
        controller.setSupplyOverride(new AcceleratorSupply(100, 1000, 10, 1, 1, 1, 0));
        helper.assertTrue(controller.requestStart() == BeamTripCause.NO_SOURCE_POWER, "an ion source that does not turn makes no beam");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA, timeoutTicks = 400)
    public static void aStopDumpsTheBeamAndRampsDown(GameTestHelper helper) {
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.startReady(helper, CAVITIES);
        helper.assertTrue(controller != null, "the controller should be there");
        boolean[] stopped = {false};
        helper.onEachTick(() -> {
            if (!stopped[0] && controller.phase() == BeamPhase.BEAM_ON) {
                stopped[0] = true;
                controller.requestStop();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(stopped[0], "waiting for beam");
            helper.assertTrue(controller.phase() == BeamPhase.OFFLINE, "back offline after the dump, at " + controller.phase());
            helper.assertTrue(controller.tripCause() == BeamTripCause.OPERATOR, "stopped by the operator");
        });
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void theControlScreenIsChecked(GameTestHelper helper) {
        AcceleratorTestBuilder.linac(helper, CAVITIES, true);
        AcceleratorControllerBlockEntity controller = AcceleratorTestBuilder.controller(helper);
        helper.assertTrue(controller != null, "the controller should be there");
        controller.attemptFormation(null);
        BlockPos pos = helper.absolutePos(AcceleratorTestBuilder.CONTROLLER);
        Player far = helper.makeMockPlayer(GameType.SURVIVAL);
        far.setPos(pos.getCenter().add(40, 0, 0));
        helper.assertFalse(AcceleratorControlPayload.apply(far, new AcceleratorControlPayload(pos, AcceleratorControlPayload.ENERGY, 0.5)),
                "a player out of reach cannot change it");
        Player near = helper.makeMockPlayer(GameType.SURVIVAL);
        near.setPos(pos.getCenter().add(0, 0, -2));
        helper.assertTrue(AcceleratorControlPayload.apply(near, new AcceleratorControlPayload(pos, AcceleratorControlPayload.ENERGY, 7)),
                "a player in reach can");
        helper.assertTrue(controller.readings().energySetpoint() == 1, "and the setpoint is clamped to 0..1");
        helper.assertTrue(AcceleratorControlPayload.apply(near, new AcceleratorControlPayload(pos, AcceleratorControlPayload.CURRENT, 0.25)),
                "the current can be set");
        helper.assertTrue(controller.readings().currentSetpoint() == 0.25, "to 25%");
        helper.assertFalse(AcceleratorControlPayload.apply(near, new AcceleratorControlPayload(pos, AcceleratorControlPayload.ENERGY, Double.NaN)),
                "a broken value is refused");
        helper.assertFalse(AcceleratorControlPayload.apply(near, new AcceleratorControlPayload(pos.above(3), AcceleratorControlPayload.START, 0)),
                "only an accelerator controller takes it");
        helper.succeed();
    }

    // ------------------------------------------------------------------ Forge Energy

    /** The readings a running accelerator would send its machines. */
    private static BeamReadings running() {
        BeamReadings n = BeamReadings.NONE;
        return new BeamReadings(true, false, BeamPhase.RF_ON, BeamTripCause.NONE, 0, 0, 0, 0, 0, 0, 0, -7, 1, 1, 1, 0, 0, 0, 1, 1,
                0, 0, n.magnetTemperature(), true, 0, 10);
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void forgeEnergyDrivesAKlystronWithoutAShaft(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos, AcceleratorRegistry.KLYSTRON.get().defaultBlockState());
        AcceleratorMachineBlockEntity klystron = AcceleratorTestBuilder.entity(helper, pos, AcceleratorMachineBlockEntity.class);
        helper.assertTrue(klystron != null, "the klystron should be there");
        klystron.observe(running());
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(energy != null, "it takes Forge Energy");
        helper.assertTrue(energy.canReceive() && !energy.canExtract(), "in, never out");
        int demand = (int) Math.ceil(klystron.energyDemand());
        helper.assertTrue(energy.receiveEnergy(demand * 10, false) == demand * 10, "the buffer holds ten ticks of it");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(klystron.drive() > 0.99, "full drive on energy alone: " + klystron.drive());
            AcceleratorSupply.Builder supply = AcceleratorSupply.builder();
            klystron.contribute(supply);
            helper.assertTrue(Math.abs(supply.build().rfPower() - AcceleratorConfig.klystronPower) < 1e-6, "its full RF power");
            helper.assertTrue(energy.getEnergyStored() < demand * 10, "spending the energy");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.LONG_ARENA)
    public static void withoutEnergyOrShaftAMachineGivesNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 5);
        helper.setBlock(pos, AcceleratorRegistry.VACUUM_PUMP.get().defaultBlockState());
        AcceleratorMachineBlockEntity pump = AcceleratorTestBuilder.entity(helper, pos, AcceleratorMachineBlockEntity.class);
        helper.assertTrue(pump != null, "the pump should be there");
        pump.observe(running());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(pump.drive() == 0, "no shaft, no energy, no drive");
            boolean saved = AcceleratorConfig.feInput;
            try {
                AcceleratorConfig.feInput = false;
                helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.UP) == null,
                        "with Forge Energy switched off there is no energy port");
            } finally {
                AcceleratorConfig.feInput = saved;
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ radiation

    @GameTest(template = NuclearGameTests.LONG_ARENA, timeoutTicks = 200)
    public static void theDumpIrradiatesWhileTheBeamRunsAndStaysActive(GameTestHelper helper) {
        helper.setBlock(AcceleratorTestBuilder.DUMP, AcceleratorRegistry.BEAM_DUMP.get().defaultBlockState());
        BeamDumpBlockEntity dump = AcceleratorTestBuilder.dump(helper);
        helper.assertTrue(dump != null, "the dump should be there");
        dump.setActivity(4.0);
        helper.onEachTick(() -> {
            if (helper.getTick() < 40) {
                dump.receiveBeam(25, 180, 4.5);
            }
        });
        helper.runAfterDelay(20, () -> {
            double strength = dump.radiationStrength();
            helper.assertTrue(strength > 4.0 + 8.0, "prompt dose on top of the activation while beam runs: " + strength);
            Vec3 body = helper.absolutePos(AcceleratorTestBuilder.DUMP).getCenter().add(0, 0, -3);
            helper.assertTrue(RadiationHandler.pointExposure(helper.getLevel(), dump.radiationOrigin(), strength, body, null) > 0,
                    "a body three blocks away is exposed");
        });
        helper.runAfterDelay(80, () -> {
            double strength = dump.radiationStrength();
            helper.assertTrue(strength > 0 && strength < 5.0, "after the beam only the activation radiates: " + strength);
            helper.succeed();
        });
    }
}
