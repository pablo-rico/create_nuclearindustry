package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door.ContainmentDoorBlock;

/**
 * The containment door in the south wall of the standard concrete room: shut it is wall, open the
 * room is open, and a pressurized room keeps it shut whether it is clicked or powered.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ContainmentDoorTests {
    private static final BlockPos DOOR = new BlockPos(5, 2, 8);
    /** A closed vestibule outside the door, sharing the room's south wall: 3x3x2 of air. */
    private static final BlockPos VESTIBULE_MIN = new BlockPos(3, 1, 8);
    private static final BlockPos VESTIBULE_MAX = new BlockPos(7, 5, 11);
    private static final int VESTIBULE_AIR = 3 * 3 * 2;

    private static void placeDoor(GameTestHelper helper) {
        BlockState lower = PwrRegistry.CONTAINMENT_DOOR.get().defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        helper.setBlock(DOOR, lower);
        helper.setBlock(DOOR.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static boolean open(GameTestHelper helper) {
        BlockState state = helper.getBlockState(DOOR);
        return state.getBlock() instanceof ContainmentDoorBlock && state.getValue(DoorBlock.OPEN);
    }

    private static void setOpen(GameTestHelper helper, boolean open) {
        BlockState state = helper.getBlockState(DOOR);
        if (state.getBlock() instanceof ContainmentDoorBlock door) {
            door.setOpen(null, helper.getLevel(), state, helper.absolutePos(DOOR), open);
        }
    }

    private static ContainmentMonitorBlockEntity sealedMonitor(GameTestHelper helper) {
        ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
        helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "the room with a shut door is sealed");
        return monitor;
    }

    /**
     * The door opens into a closed vestibule rather than the open arena: a survey that runs out into
     * the unloaded world around a game test keeps its last answer, so the open door shows as the
     * room and the vestibule becoming one sealed space.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void aShutDoorIsWallAndAnOpenOneIsAHole(GameTestHelper helper) {
        ContainmentTestBuilder.room(helper, VESTIBULE_MIN, VESTIBULE_MAX, PwrRegistry.CONTAINMENT_CONCRETE.get());
        ContainmentTestBuilder.concreteRoom(helper);
        placeDoor(helper);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = sealedMonitor(helper);
                    helper.assertTrue(monitor.getShape().volume() == ContainmentTestBuilder.ROOM_AIR, "with the same air inside");
                    helper.assertTrue(monitor.getShape().isWall(helper.absolutePos(DOOR)), "the door is part of the wall");
                    helper.assertTrue(monitor.getShape().weakestRating() == 6.0,
                            "the door (8 bar) is no weaker than the concrete, " + monitor.getShape().weakestRating());
                })
                .thenExecute(() -> setOpen(helper, true))
                .thenWaitUntil(() -> {
                    helper.assertTrue(open(helper), "the door opens");
                    helper.assertTrue(helper.getBlockState(DOOR.above()).getValue(DoorBlock.OPEN), "both halves");
                    helper.assertTrue(!helper.getBlockState(DOOR).isCollisionShapeFullBlock(helper.getLevel(), helper.absolutePos(DOOR)),
                            "the doorway is clear");
                    BlockState openDoor = helper.getBlockState(DOOR);
                    helper.assertTrue(!openDoor.getShape(helper.getLevel(), helper.absolutePos(DOOR)).isEmpty()
                                    && !openDoor.getCollisionShape(helper.getLevel(), helper.absolutePos(DOOR)).isEmpty(),
                            "but its edge is left there, to click it shut by");
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    int joined = ContainmentTestBuilder.ROOM_AIR + 2 + VESTIBULE_AIR;
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying()
                                    && monitor.getShape().volume() == joined,
                            "the room is open to the vestibule, " + (monitor == null || monitor.getShape() == null ? "-" : monitor.getShape().volume())
                                    + " blocks of air, expected " + joined);
                })
                .thenExecute(() -> setOpen(helper, false))
                .thenWaitUntil(() -> {
                    helper.assertTrue(!open(helper), "the door shuts");
                    helper.assertTrue(sealedMonitor(helper).getShape().volume() == ContainmentTestBuilder.ROOM_AIR,
                            "and the room is closed on its own again");
                })
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(DOOR).getValue(ContainmentDoorBlock.VISIBLE),
                        "once it has slid home the block model shows again"))
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void redstoneOpensAnUnpressurizedDoor(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        placeDoor(helper);
        BlockPos outside = DOOR.south();
        helper.startSequence()
                .thenWaitUntil(() -> sealedMonitor(helper))
                .thenExecute(() -> helper.setBlock(outside, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(open(helper), "a powered door opens"))
                .thenExecute(() -> helper.setBlock(outside, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(!open(helper), "and shuts when the power goes"))
                .thenSucceed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void aPressurizedContainmentKeepsItsDoorShut(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        placeDoor(helper);
        BlockPos outside = DOOR.south();
        helper.startSequence()
                .thenWaitUntil(() -> sealedMonitor(helper))
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = sealedMonitor(helper);
                    // As much steam again as there is air: about twice the outside pressure.
                    monitor.addSteam(ContainmentTestBuilder.airMoles(monitor), 373.0);
                })
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = sealedMonitor(helper);
                    helper.assertTrue(monitor.getPressure() > 1.5, "the room is pressurized, " + monitor.getPressure());
                })
                .thenExecute(() -> setOpen(helper, true))
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(!open(helper), "a click does not open a pressure-locked door"))
                .thenExecute(() -> helper.setBlock(outside, Blocks.REDSTONE_BLOCK))
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(!open(helper), "nor does redstone");
                    sealedMonitor(helper);
                })
                .thenSucceed();
    }
}
