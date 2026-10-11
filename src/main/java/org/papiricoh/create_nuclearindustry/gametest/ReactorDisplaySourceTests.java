package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
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
            assertReads(helper, ReactorReadout.REACTIVITY_PARTS, reactor, "F+0 R+0 T+0 V+0 Xe+0");
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

    private static void assertReads(GameTestHelper helper, ReactorReadout readout, ReactorBlockEntity reactor, String expected) {
        String actual = readout.read(reactor).getString();
        helper.assertTrue(actual.equals(expected), readout.id() + " should read " + expected + ", got " + actual);
    }
}
