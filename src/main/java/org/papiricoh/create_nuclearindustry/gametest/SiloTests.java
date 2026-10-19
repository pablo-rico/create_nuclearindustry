package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketValidator;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;

/**
 * The silo's state machine on a real structure: what makes it ready, what it says when it is not, the redstone edge that
 * launches it, losing rotation, the rocket going with the launch and a stale chunk that comes back after one.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SiloTests {
    private static final String BATCH = "silo";

    /** A built silo aimed 150 blocks east by a mock player. */
    static SiloControllerEntity aimed(GameTestHelper helper, SiloTestBuilder builder, Player player) {
        SiloControllerEntity silo = builder.build(helper);
        silo.configure(player, target(helper), false);
        return silo;
    }

    static BlockPos target(GameTestHelper helper) {
        return helper.absolutePos(SiloTestBuilder.FLOOR).offset(150, 0, 0);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aCompleteSiloIsReady(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        helper.succeedWhen(() -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.READY, "a complete, turning, aimed silo is ready, got "
                    + silo.state() + " " + silo.error());
            helper.assertTrue(silo.error().isEmpty(), "with nothing wrong");
            helper.assertTrue(silo.rocketHeight() == 5, "it sees its five-section rocket, got " + silo.rocketHeight());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aMissingWallIsReported(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos gap = SiloTestBuilder.FLOOR.offset(2, 2, 0);
        helper.setBlock(gap, Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.INCOMPLETE, "a gap in the wall leaves it incomplete, got " + silo.state());
            helper.assertTrue("wall".equals(silo.error()), "and says so, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(gap)), "where the gap is, got " + silo.errorPos());
            helper.assertTrue(silo.errorMessage().startsWith("silo.create_nuclearindustry.error.wall|"), "as a translatable message");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aLooseBlockInsideIsReported(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos clutter = SiloTestBuilder.FLOOR.offset(1, 2, 1);
        helper.setBlock(clutter, Blocks.STONE);
        helper.succeedWhen(() -> {
            helper.assertTrue("interior".equals(silo.error()), "a loose block beside the rocket obstructs it, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(clutter)), "and is pointed at, got " + silo.errorPos());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aBlockedExitIsReported(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().tanks(2);
        SiloControllerEntity silo = aimed(helper, builder, helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos cap = SiloTestBuilder.FLOOR.above(builder.height() + 4).east();
        helper.setBlock(cap, Blocks.STONE);
        helper.succeedWhen(() -> {
            helper.assertTrue("exit".equals(silo.error()), "a block over the shaft blocks the exit, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(cap)), "and is pointed at, got " + silo.errorPos());
        });
    }

    /** Arming with the redstone already on does not launch: only a fresh rising edge does. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void aSteadySignalDoesNotLaunch(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), player);
        BlockPos redstone = SiloTestBuilder.CONTROLLER.below();
        helper.setBlock(redstone, Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            silo.arm(player);
            helper.assertTrue(silo.state() == SiloControllerEntity.State.ARMED, "it arms, got " + silo.state() + " " + silo.error());
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.ARMED, "a signal already on is not an edge, got " + silo.state());
            helper.setBlock(redstone, Blocks.AIR);
        });
        helper.runAfterDelay(45, () -> helper.setBlock(redstone, Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.COUNTDOWN, "a fresh edge starts the countdown, got " + silo.state());
            silo.disarm();
            helper.setBlock(redstone, Blocks.AIR);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void losingRotationCancelsTheCountdown(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), player);
        helper.runAfterDelay(10, () -> {
            silo.arm(player);
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.REDSTONE_BLOCK);
        });
        boolean[] stopped = {false};
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.COUNTDOWN, "it counts down, got " + silo.state() + " " + silo.error());
            helper.setBlock(SiloTestBuilder.MOTOR, Blocks.AIR);
            stopped[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(stopped[0], "the motor is still there");
            helper.assertTrue(silo.state() == SiloControllerEntity.State.UNPREPARED, "without rotation it stands down, got " + silo.state());
            helper.assertTrue("speed".equals(silo.error()), "for want of rotation, got " + silo.error());
            helper.assertTrue(silo.countdown() < 0, "and its countdown is cancelled");
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.AIR);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aLaunchTakesTheRocket(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloTestBuilder builder = new SiloTestBuilder().tanks(2);
        SiloControllerEntity silo = aimed(helper, builder, player);
        BlockPos target = target(helper);
        FlightTracker tracker = FlightTracker.get(level);
        helper.runAfterDelay(10, () -> {
            silo.arm(player);
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            Flight launched = tracker.flights().stream().filter(f -> f.target().equals(target)).findFirst().orElse(null);
            helper.assertTrue(launched != null, "the silo launches, state " + silo.state() + " " + silo.error());
            tracker.remove(level, launched);
            helper.assertTrue(silo.state() == SiloControllerEntity.State.LAUNCHED, "and says it has, got " + silo.state());
            for (int i = 1; i <= builder.height(); i++) {
                helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR.above(i)).isAir(), "the rocket left the silo, section " + i + " is "
                        + helper.getBlockState(SiloTestBuilder.FLOOR.above(i)));
            }
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(SiloTestBuilder.FLOOR)).inflate(8)).isEmpty(), "and dropped nothing");
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.AIR);
        });
    }

    /** A chunk saved before a launch and restored after it must not fly the same rocket twice. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aStaleReplayStaysLaunched(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiloTestBuilder builder = new SiloTestBuilder().tanks(2);
        SiloControllerEntity silo = aimed(helper, builder, helper.makeMockPlayer(GameType.SURVIVAL));
        helper.runAfterDelay(5, () -> {
            RocketValidator.Result result = RocketValidator.validate(level, helper.absolutePos(SiloTestBuilder.FLOOR).above());
            helper.assertTrue(result.valid(), "the rocket is valid, got " + result.error());
            FlightTracker tracker = FlightTracker.get(level);
            Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(SiloTestBuilder.FLOOR).above());
            Flight receipt = tracker.launchModular(level, result.rocket().launchId(), result.rocket().design(), start, Vec3.atCenterOf(target(helper)),
                    target(helper), result.rocket().warhead(), null, java.util.List.of(), java.util.List.of(0));
            helper.assertTrue(receipt != null, "the receipt's flight is made");
            tracker.remove(level, receipt);
            silo.validate();
            helper.assertTrue(silo.state() == SiloControllerEntity.State.LAUNCHED, "the silo knows that rocket already flew, got " + silo.state());
            for (int i = 1; i <= builder.height(); i++) {
                helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR.above(i)).isAir(), "and its stale copy is gone, section " + i);
            }
            helper.succeed();
        });
    }
}
