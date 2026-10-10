package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentShape;

/**
 * The containment building: a monitor inside a closed room measures it as sealed, any opening
 * unseals it, penetrations carry fluid through the wall, the weakest wall bursts first, and a
 * reactor standing inside reports the building's pressure.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ContainmentTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aSealedConcreteRoomForms(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(monitor != null, "the monitor has its block entity");
            helper.assertTrue(monitor.sealed(), "a closed concrete room is sealed");
            ContainmentShape shape = monitor.getShape();
            helper.assertTrue(shape != null && shape.volume() == ContainmentTestBuilder.ROOM_AIR,
                    "it measures the air inside, " + (shape == null ? 0 : shape.volume()));
            helper.assertTrue(shape.weakestRating() == 6.0, "concrete holds 6 bar, got " + shape.weakestRating());
            helper.assertTrue(shape.wallArea() == 2 * (5 * 4 + 5 * 4 + 5 * 5), "every inner face of the walls counts, " + shape.wallArea());
            helper.assertTrue(Math.abs(monitor.getPressure() - ContainmentModel.ATMOSPHERE) < 1e-3, "at the outside pressure");
            MonitorReading reading = MonitorReading.sample(new LinkedTarget.Plant(monitor));
            helper.assertTrue(reading.kind() == TargetKind.CONTAINMENT && reading.flag(MonitorReading.N_SEALED),
                    "control room monitors read it as a sealed containment");
            helper.assertTrue(reading.value(MonitorReading.N_VOLUME) == ContainmentTestBuilder.ROOM_AIR, "with its volume");
            helper.assertTrue(monitor.comparatorSignal() == 0, "no overpressure, no comparator signal");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aHoleUnsealsIt(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        BlockPos wall = new BlockPos(2, 3, 5);
        boolean[] opened = new boolean[1];
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            if (!opened[0] && monitor != null && monitor.sealed()) {
                opened[0] = true;
                helper.setBlock(wall, Blocks.AIR);
            }
        });
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(opened[0], "the room sealed first");
            helper.assertTrue(monitor != null && !monitor.sealed() && !monitor.isSurveying(), "a hole in the wall unseals it");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aDoorwayIsAnOpening(GameTestHelper helper) {
        // an open doorway (a door is not a full block) never seals
        ContainmentTestBuilder.concreteRoom(helper);
        helper.setBlock(new BlockPos(8, 2, 5), Blocks.OAK_DOOR);
        helper.runAfterDelay(30, () -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(monitor != null && !monitor.sealed(), "gas gets past a door");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aPenetrationIsWallAndCarriesFluid(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        BlockPos sleeve = new BlockPos(2, 3, 5);
        helper.setBlock(sleeve, PwrRegistry.CONTAINMENT_PENETRATION.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(monitor != null && monitor.sealed(), "the room with a penetration is still sealed");
            helper.assertTrue(monitor.getShape().weakestRating() == 6.0, "and as strong as concrete");
            BlockPos at = helper.absolutePos(sleeve);
            IFluidHandler outside = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.WEST);
            IFluidHandler inside = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.EAST);
            IFluidHandler side = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.NORTH);
            helper.assertTrue(outside != null && inside != null, "both ends take pipes");
            helper.assertTrue(side == null, "the sides along the wall do not");
            int filled = outside.fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 600, "the outer end takes fluid, took " + filled);
            helper.assertTrue(outside.drain(600, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "and does not give it back");
            FluidStack through = inside.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(through.getAmount() == 600 && through.getFluid() == Fluids.WATER, "it comes out of the inner end");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theWeakestWallBurstsFirst(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        BlockPos window = new BlockPos(8, 3, 5);
        helper.setBlock(window, Blocks.GLASS);
        boolean[] pressurized = new boolean[1];
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            if (!pressurized[0] && monitor != null && monitor.sealed()) {
                helper.assertTrue(Math.abs(monitor.getShape().weakestRating() - 1.05) < 1e-6,
                        "the window is the weak point, " + monitor.getShape().weakestRating());
                pressurized[0] = true;
                monitor.addSteam(ContainmentTestBuilder.airMoles(monitor) * 0.5, ContainmentModel.WALL_TEMPERATURE);
            }
        });
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(pressurized[0], "the room sealed and was pressurized");
            helper.assertBlockNotPresent(Blocks.GLASS, window);
            helper.assertBlockPresent(PwrRegistry.CONTAINMENT_CONCRETE.get(), new BlockPos(8, 3, 4));
            helper.assertTrue(monitor != null && !monitor.sealed(), "the burst room is open");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aReactorInsideReportsItsContainment(GameTestHelper helper) {
        ContainmentTestBuilder.room(helper, BlockPos.ZERO, new BlockPos(12, 12, 12), PwrRegistry.CONTAINMENT_CONCRETE.get());
        BlockPos origin = new BlockPos(4, 1, 4);
        FissionTestBuilder.build(helper, origin, true);
        helper.setBlock(new BlockPos(1, 1, 1), PwrRegistry.CONTAINMENT_MONITOR.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(origin));
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "the reactor forms inside");
            float pressure = reactor.plantReadings().containmentPressure();
            helper.assertTrue(Math.abs(pressure - ContainmentModel.ATMOSPHERE) < 0.01, "it reports the containment's pressure, got " + pressure);
            helper.assertTrue(ReactorReadout.CONTAINMENT_PRESSURE.read(reactor).getString().equals("1.01 bar"),
                    "Display Links read it, got " + ReactorReadout.CONTAINMENT_PRESSURE.read(reactor).getString());
            MonitorReading reading = MonitorReading.sample(new LinkedTarget.Reactor(reactor));
            helper.assertTrue(reading.value(MonitorReading.R_CONT_PRESSURE) > 1.0f, "and monitors linked to the reactor");
        });
    }
}
