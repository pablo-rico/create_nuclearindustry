package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * Builds containment rooms for the accident tests: a hollow box of one wall block with a
 * Containment Monitor inside, and cleans up what accidents leave behind.
 */
public final class ContainmentTestBuilder {
    /** A 7x6x7 room in the small arena: 5x4x5 = 100 blocks of air inside. */
    public static final BlockPos ROOM_MIN = new BlockPos(2, 1, 2);
    public static final BlockPos ROOM_MAX = new BlockPos(8, 6, 8);
    public static final BlockPos MONITOR = new BlockPos(5, 2, 5);
    public static final int ROOM_AIR = 5 * 4 * 5;

    private ContainmentTestBuilder() {}

    /** Walls, floor and roof of {@code wall} from {@code min} to {@code max}; air inside. */
    public static void room(GameTestHelper helper, BlockPos min, BlockPos max, Block wall) {
        room(BlockPlacer.of(helper), min, max, wall);
    }

    public static void room(BlockPlacer helper, BlockPos min, BlockPos max, Block wall) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean shell = x == min.getX() || x == max.getX() || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    helper.set(new BlockPos(x, y, z), shell ? wall : Blocks.AIR);
                }
            }
        }
    }

    /** The standard concrete room with its monitor. */
    public static void concreteRoom(GameTestHelper helper) {
        concreteRoom(BlockPlacer.of(helper), BlockPos.ZERO);
    }

    /** The standard concrete room with its monitor, moved by {@code offset} from where the tests have it. */
    public static void concreteRoom(BlockPlacer helper, BlockPos offset) {
        room(helper, ROOM_MIN.offset(offset), ROOM_MAX.offset(offset), PwrRegistry.CONTAINMENT_CONCRETE.get());
        helper.set(MONITOR.offset(offset), PwrRegistry.CONTAINMENT_MONITOR.get());
    }

    @Nullable
    public static ContainmentMonitorBlockEntity monitor(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof ContainmentMonitorBlockEntity monitor ? monitor : null;
    }

    /** Mol of gas the room holds at the outside pressure. */
    public static double airMoles(ContainmentMonitorBlockEntity monitor) {
        return monitor.getModel().totalMoles();
    }

    /** Clears everything inside {@code min}-{@code max}, so nothing keeps reacting after the test. */
    public static void clear(GameTestHelper helper, BlockPos min, BlockPos max) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    /** Clears the whole small arena. */
    public static void clearArena(GameTestHelper helper) {
        clear(helper, BlockPos.ZERO, new BlockPos(12, 15, 12));
    }

    /** Fallout events the level holds now, so a test can drop the ones it added. */
    public static int falloutEvents(GameTestHelper helper) {
        return ContaminationData.get(helper.getLevel()).events().size();
    }

    /** Drops the fallout events and point sources added since {@code events}, and cleans the chunks around the arena. */
    public static void cleanFallout(GameTestHelper helper, int events, int chunkRadius) {
        ServerLevel level = helper.getLevel();
        ContaminationData data = ContaminationData.get(level);
        while (data.events().size() > events) {
            data.events().removeLast();
        }
        BlockPos center = helper.absolutePos(new BlockPos(6, 1, 6));
        FalloutData.get(level).sources().removeIf(source -> source.pos().closerThan(center, 24));
        ContaminationTests.clean(helper, chunkRadius);
    }
}
