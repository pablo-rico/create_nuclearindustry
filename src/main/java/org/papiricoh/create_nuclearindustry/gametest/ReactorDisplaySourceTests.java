package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorTemperatureSensorBlock;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.control.AutoScram;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;

import java.util.List;

/** Every reactor reading is a Display Link source on the controller, its sensors and its redstone ports. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ReactorDisplaySourceTests {
    private static final BlockPos SENSOR = FissionTestBuilder.WALL_CASING;

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void sourcesBoundToControllerSensorAndPort(GameTestHelper helper) {
        List<BlockEntityType<?>> types = List.of(AllNuclearEntities.REACTOR.get(),
                AllNuclearEntities.REACTOR_TEMPERATURE_SENSOR.get(), AllNuclearEntities.REACTOR_REDSTONE_PORT.get());
        for (BlockEntityType<?> type : types) {
            List<DisplaySource> sources = DisplaySource.BY_BLOCK_ENTITY.get(type);
            for (ReactorReadout readout : ReactorReadout.values()) {
                helper.assertTrue(sources.contains(AllNuclearDisplaySources.REACTOR_READOUTS.get(readout).get()),
                        readout.id() + " missing on " + type);
            }
        }
        helper.assertTrue(DisplaySource.BY_BLOCK_ENTITY.get(AllNuclearEntities.REACTOR_TEMPERATURE_SENSOR.get())
                        .contains(AllNuclearDisplaySources.REACTOR_TEMPERATURE_SENSOR.get()),
                "the sensor keeps its original source");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void readoutsFormatIdleReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            assertReads(helper, ReactorReadout.TEMPERATURE, reactor, "20°C");
            assertReads(helper, ReactorReadout.FUEL, reactor, "0%");
            assertReads(helper, ReactorReadout.CONTROL_RODS, reactor, "100%");
            assertReads(helper, ReactorReadout.COOLANT, reactor, "0 mB");
            assertReads(helper, ReactorReadout.REACTIVITY_PARTS, reactor, "F+0 R+0 T+0 V+0 Xe+0 B+0");
            assertReads(helper, ReactorReadout.BORON, reactor, "0 ppm");
            assertReads(helper, ReactorReadout.BUILDING_HYDROGEN, reactor, "0.0%");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void readoutsFollowSensorLink(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(SENSOR, AllNuclearBlocks.REACTOR_TEMPERATURE_SENSOR.get().defaultBlockState()
                .setValue(ReactorTemperatureSensorBlock.FACING, Direction.NORTH));
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> helper.assertTrue(
                ReactorReadout.reactorOf(helper.getBlockEntity(SENSOR)).orElse(null) == reactor,
                "a sensor's readouts come from its reactor"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void statusShowsAutoScram(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            reactor.adjustAutoScramThreshold(600 - AutoScram.DEFAULT_THRESHOLD);
            reactor.getPhysicsSimulator().applyExternalHeat(700);
        });
        helper.runAfterDelay(8, () -> {
            assertReads(helper, ReactorReadout.STATUS, reactor, "AUTO SCRAM");
            helper.succeed();
        });
    }

    /**
     * The safe shutdown readouts of a core full of water and boric acid, every rod in: a cold shutdown
     * held by rods and boron, the core covered, the rods counted against the fuel.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void readoutsShowTheShutdown(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 14_000), IFluidHandler.FluidAction.EXECUTE);
            reactor.fillCoolant(new FluidStack(AllNuclearFluids.BORIC_ACID.get(), 2_000), IFluidHandler.FluidAction.EXECUTE);
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(5f);
            }
        });
        helper.runAfterDelay(60, () -> {
            assertReads(helper, ReactorReadout.SHUTDOWN_STATE, reactor, "Cold shutdown");
            assertReads(helper, ReactorReadout.BORON, reactor, Math.round(reactor.getBoronPpm()) + " ppm");
            helper.assertTrue(reactor.getBoronPpm() > 800, "2000 mB of boric acid in 16000: " + reactor.getBoronPpm());
            assertReads(helper, ReactorReadout.CORE_LEVEL, reactor, "100%");
            assertReads(helper, ReactorReadout.COLD_MARGIN, reactor,
                    String.format(java.util.Locale.ROOT, "%+d pcm", Math.round(reactor.getColdShutdownMargin(true))));
            helper.assertTrue(reactor.getColdShutdownMargin(true) > 1000, "rods and boron hold it down with margin");
            int fuel = reactor.getFuelRodCount();
            helper.assertTrue(fuel > 0, "the core has fuel rod blocks");
            String rods = ReactorReadout.RODS_INSERTED.read(reactor).getString();
            // This core has fewer rod blocks than fuel: the boron makes up the difference.
            helper.assertTrue(rods.equals((long) Math.floor(reactor.getEffectiveRods() + 1.0e-6) + " / " + fuel)
                    && reactor.getEffectiveRods() > 0, "the rods in against the fuel: " + rods);
            helper.assertTrue(ReactorReadout.HEAT_REMOVAL.read(reactor).getString().matches("\\d+ / \\d+ MW"),
                    "heat removed against decay heat: " + ReactorReadout.HEAT_REMOVAL.read(reactor).getString());
            String parts = ReactorReadout.REACTIVITY_PARTS.read(reactor).getString();
            helper.assertTrue(parts.contains(" B-"), "the boron's term in the breakdown: " + parts);
            helper.succeed();
        });
    }

    /** An unformed reactor reads a dash on the new readouts too. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void shutdownReadoutsDashWithoutAReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.setBlock(FissionTestBuilder.WALL_CASING, net.minecraft.world.level.block.Blocks.AIR);
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(reactor.isFormed(), "a hole in the wall unforms it");
            for (ReactorReadout readout : new ReactorReadout[] {ReactorReadout.SHUTDOWN_STATE, ReactorReadout.COLD_MARGIN,
                    ReactorReadout.BORON, ReactorReadout.CORE_LEVEL, ReactorReadout.HEAT_REMOVAL, ReactorReadout.RODS_INSERTED,
                    ReactorReadout.BUILDING_HYDROGEN}) {
                assertReads(helper, readout, reactor, "-");
            }
            helper.succeed();
        });
    }

    /**
     * A fluid port sends its reactor to clients, whose goggles find the reactor through it (the
     * multiblock index lives on the server alone) to show the tanks and whether the port bleeds.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fluidPortsSendTheirReactorToClients(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            var port = helper.getLevel().getBlockEntity(helper.absolutePos(FissionTestBuilder.STEAM_PORT));
            helper.assertTrue(port instanceof org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorFluidPortBlockEntity,
                    "an output port");
            var tag = port.getUpdateTag(helper.getLevel().registryAccess());
            helper.assertTrue(tag.contains("Controller")
                    && net.minecraft.core.BlockPos.of(tag.getLong("Controller")).equals(reactor.getBlockPos()),
                    "its client update names the controller: " + tag);
        });
    }

    private static void assertReads(GameTestHelper helper, ReactorReadout readout, ReactorBlockEntity reactor, String expected) {
        String actual = readout.read(reactor).getString();
        helper.assertTrue(actual.equals(expected), readout.id() + " should read " + expected + ", got " + actual);
    }
}
