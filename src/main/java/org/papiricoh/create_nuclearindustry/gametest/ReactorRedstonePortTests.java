package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorRedstonePortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorTemperatureSensorBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorRedstonePortBlockEntity;

/** The Reactor Redstone Port reads the reactor out as redstone; SCRAM is a signal, never a magic rod move. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorRedstonePortTests {
    private static final BlockPos PORT = FissionTestBuilder.WALL_CASING;   // north wall
    private static final BlockPos OUTSIDE = PORT.north();

    private static void placePort(GameTestHelper helper, RedstonePortMode mode) {
        helper.setBlock(PORT, AllNuclearBlocks.REACTOR_REDSTONE_PORT.get().defaultBlockState()
                .setValue(ReactorRedstonePortBlock.FACING, Direction.NORTH)
                .setValue(ReactorRedstonePortBlock.MODE, mode));
    }

    private static ReactorRedstonePortBlockEntity port(GameTestHelper helper) {
        return helper.getBlockEntity(PORT);
    }

    private static int outsideSignal(GameTestHelper helper) {
        return helper.getLevel().getBestNeighborSignal(helper.absolutePos(OUTSIDE));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void redstonePortJoinsTheShell(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        placePort(helper, RedstonePortMode.TEMPERATURE);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor with a redstone port should form");
            helper.assertTrue(reactor.getStructure().map(s -> s.containsRedstonePort(helper.absolutePos(PORT))).orElse(false),
                    "the structure should list the redstone port");
            helper.assertTrue(port(helper).getLinkedReactor().isPresent(), "the port should link to its reactor");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void temperatureModeFollowsTheCore(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        placePort(helper, RedstonePortMode.TEMPERATURE);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            port(helper).refreshSignal();
            helper.assertTrue(outsideSignal(helper) == 0, "a cold reactor gives no signal, got " + outsideSignal(helper));
            reactor.getPhysicsSimulator().applyExternalHeat(3480);
            port(helper).refreshSignal();
            helper.assertTrue(outsideSignal(helper) == 15, "a 3500 C core gives 15 outside the panel, got " + outsideSignal(helper));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void coolantModeIsComparatorLike(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        placePort(helper, RedstonePortMode.COOLANT);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            port(helper).refreshSignal();
            helper.assertTrue(outsideSignal(helper) == 0, "empty coolant tank gives 0");
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
            port(helper).refreshSignal();
            helper.assertTrue(outsideSignal(helper) == 1, "any coolant gives at least 1, got " + outsideSignal(helper));
            reactor.fillCoolant(new FluidStack(Fluids.WATER, reactor.getTankCapacity() / 2 - 1), IFluidHandler.FluidAction.EXECUTE);
            port(helper).refreshSignal();
            helper.assertTrue(outsideSignal(helper) == 8, "a half-full tank gives 8, got " + outsideSignal(helper));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void fuelModeReportsFuel(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        placePort(helper, RedstonePortMode.FUEL);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> reactor.getFuelItemHandler(ReactorFluidPortMode.INPUT).insertItem(0,
                UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM_REACTOR_FUEL.get()), 5f), false));
        helper.succeedWhen(() -> {
            port(helper).refreshSignal();
            // One LEU assembly of 100 units in a core with room for six: ceil(100 / 600 * 15) = 3.
            helper.assertTrue(outsideSignal(helper) == 3, "one assembly in six channels gives 3, got " + outsideSignal(helper));
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wrenchCyclesAllModes(GameTestHelper helper) {
        RedstonePortMode mode = RedstonePortMode.TEMPERATURE;
        for (int i = 0; i < RedstonePortMode.values().length; i++) {
            mode = mode.next();
        }
        helper.assertTrue(mode == RedstonePortMode.TEMPERATURE, "six steps should come back to temperature");
        helper.assertTrue(RedstonePortMode.STEAM.next() == RedstonePortMode.SCRAM, "SCRAM follows steam");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void scramModeSignalsWithoutMovingRods(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        placePort(helper, RedstonePortMode.SCRAM);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor without rods should form");
            helper.assertTrue(outsideSignal(helper) == 0, "no SCRAM, no signal");
            reactor.setManualScram(true);
            helper.assertTrue(outsideSignal(helper) == 15, "manual SCRAM should light the SCRAM port, got " + outsideSignal(helper));
            helper.assertTrue(reactor.getPhysicsSimulator().getControlRodInsertion() == 0.0f,
                    "SCRAM must not move rods by itself, insertion " + reactor.getPhysicsSimulator().getControlRodInsertion());
            reactor.setManualScram(false);
            helper.assertTrue(outsideSignal(helper) == 0, "reset should turn the SCRAM signal off");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void legacyManualScramHoldsRodsUntilReset(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            var registries = helper.getLevel().registryAccess();
            CompoundTag tag = reactor.saveWithoutMetadata(registries);
            tag.putBoolean("manualScram", true);
            tag.remove("scramVersion");
            tag.remove("legacyScramHold");
            reactor.loadWithComponents(tag, registries);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(reactor.isScramRequested(), "an old manual SCRAM still asks for SCRAM");
            helper.assertTrue(reactor.getPhysicsSimulator().getControlRodInsertion() == 1.0f,
                    "an old manual SCRAM keeps the rods in until reset, insertion " + reactor.getPhysicsSimulator().getControlRodInsertion());
            reactor.setManualScram(false);
            helper.assertTrue(reactor.getPhysicsSimulator().getControlRodInsertion() == 0.0f, "after reset only physical rods count");
            helper.assertFalse(reactor.isScramRequested(), "reset clears the old hold");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void addingAPortKeepsCoreTemperature(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            reactor.getPhysicsSimulator().applyExternalHeat(800);
            placePort(helper, RedstonePortMode.TEMPERATURE);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(reactor.getStructure().map(s -> s.containsRedstonePort(helper.absolutePos(PORT))).orElse(false),
                    "the new port should be part of the structure");
            helper.assertTrue(reactor.getCoreTemperature() > 700, "rebuilding the structure must keep the heat, got "
                    + reactor.getCoreTemperature());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void temperatureSensorOutputsOnItsPanelSide(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockState sensor = AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get().defaultBlockState()
                .setValue(ReactorTemperatureSensorBlock.FACING, Direction.NORTH);
        helper.setBlock(PORT, sensor);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> reactor.getPhysicsSimulator().applyExternalHeat(3480));
        helper.succeedWhen(() -> helper.assertTrue(outsideSignal(helper) > 0,
                "the sensor should power the block in front of its panel, got " + outsideSignal(helper)));
    }
}
