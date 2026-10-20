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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketValidator;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
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
    public static void aWallGapIsHarmless(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        helper.setBlock(SiloTestBuilder.FLOOR.offset(2, 2, 0), Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.READY, "walls are optional, got " + silo.state() + " " + silo.error());
            helper.assertTrue(silo.shaftHeight() == 1, "the gap ends the consecutive shaft at layer one");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aMissingBaseBlockIsReported(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos gap = SiloTestBuilder.FLOOR;
        helper.setBlock(gap, Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertTrue(silo.state() == SiloControllerEntity.State.INCOMPLETE, "a base gap leaves it incomplete, got " + silo.state());
            helper.assertTrue("base".equals(silo.error()), "and says so, got " + silo.error());
            helper.assertTrue(silo.errorPos().equals(helper.absolutePos(gap)), "where the gap is, got " + silo.errorPos());
            helper.assertTrue(silo.errorMessage().startsWith("silo.create_nuclearindustry.error.base|"), "as a translatable message");
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
        BlockPos redstone = SiloTestBuilder.CONTROLLER.north();
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
            helper.setBlock(SiloTestBuilder.CONTROLLER.north(), Blocks.REDSTONE_BLOCK);
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
            helper.setBlock(SiloTestBuilder.CONTROLLER.north(), Blocks.AIR);
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
            helper.setBlock(SiloTestBuilder.CONTROLLER.north(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            Flight launched = tracker.flights().stream().filter(f -> f.target().equals(target)).findFirst().orElse(null);
            helper.assertTrue(launched != null, "the silo launches, state " + silo.state() + " " + silo.error());
            tracker.remove(level, launched);
            helper.assertTrue(silo.state() == SiloControllerEntity.State.LAUNCHED, "and says it has, got " + silo.state());
            double cep = launched.flight().design().cep();
            helper.assertTrue(Math.abs(cep - GuidanceTier.BASIC.cep(150) * MissileConfig.cepScale) < 1, "an empty guidance module flies on dead reckoning, cep " + cep);
            double miss = Math.hypot(launched.aim().x - (target.getX() + .5), launched.aim().z - (target.getZ() + .5));
            helper.assertTrue(cep == 0 || miss > 0 && miss < cep * 6, "it is sent off the target by its guidance's miss, " + miss);
            for (int i = 1; i <= builder.height(); i++) {
                helper.assertTrue(helper.getBlockState(SiloTestBuilder.FLOOR.above(i)).isAir(), "the rocket left the silo, section " + i + " is "
                        + helper.getBlockState(SiloTestBuilder.FLOOR.above(i)));
            }
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(SiloTestBuilder.FLOOR)).inflate(8)).isEmpty(), "and dropped nothing");
            helper.setBlock(SiloTestBuilder.CONTROLLER.north(), Blocks.AIR);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void theGuidanceModuleSetsTheCep(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), helper.makeMockPlayer(GameType.SURVIVAL));
        BlockPos guidance = SiloTestBuilder.FLOOR.above(4);
        helper.runAfterDelay(5, () -> {
            RocketSectionEntity module = helper.getBlockEntity(guidance);
            helper.assertTrue(module.section() == RocketComposition.Section.GUIDANCE, "the guidance module is where expected");
            helper.assertTrue(module.contents().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "it takes no dirt");
            module.contents().setStackInSlot(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(RocketSectionEntity.PRECISION_MECHANISM)));
        });
        helper.runAfterDelay(10, () -> {
            silo.validate();
            helper.assertTrue(silo.guidance() == GuidanceTier.INERTIAL, "a precision mechanism gives inertial guidance, got " + silo.guidance());
            helper.assertTrue(Math.abs(silo.cep() - GuidanceTier.INERTIAL.cep(150) * MissileConfig.cepScale) < 0.01, "its CEP at 150 blocks, got " + silo.cep());
        });
        helper.runAfterDelay(15, () -> {
            RocketSectionEntity module = helper.getBlockEntity(guidance);
            module.contents().setStackInSlot(0, new ItemStack(SiloRegistry.STAR_TRACKER.get()));
            silo.validate();
            helper.assertTrue(silo.guidance() == GuidanceTier.STELLAR, "a star tracker gives stellar guidance, got " + silo.guidance());
            helper.assertTrue(Math.abs(silo.cep() - GuidanceTier.STELLAR.cep(150) * MissileConfig.cepScale) < 0.01, "its CEP at 150 blocks, got " + silo.cep());
            double saved = MissileConfig.cepScale;
            double perfect;
            try {
                MissileConfig.cepScale = 0;
                perfect = silo.cep();
            } finally {
                MissileConfig.cepScale = saved;
            }
            helper.assertTrue(perfect == 0, "with missile.guidance.cepScale at 0 every rocket is exact");
            helper.succeed();
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

    /**
     * Reach is judged on the target as designated: one near the edge of the rocket's reach is ready even though its guidance
     * misses by hundreds of blocks, and one beyond says how far it is and how far the rocket reaches.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void reachIsJudgedOnTheDesignatedTarget(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(2), player);
        helper.runAfterDelay(10, () -> {
            silo.validate();
            double reach = silo.reach();
            helper.assertTrue(reach > 200, "the rocket reaches somewhere, got " + reach);
            BlockPos floor = helper.absolutePos(SiloTestBuilder.FLOOR);
            BlockPos edge = floor.offset((int) (reach * 0.97), 0, 0);
            silo.configure(player, edge, false);
            silo.validate();
            helper.assertTrue(silo.canReach(edge), "97% of its reach can be reached");
            helper.assertTrue(silo.state() == SiloControllerEntity.State.READY,
                    "so it is ready whatever its guidance misses by (CEP " + Math.round(silo.cep()) + "), got " + silo.state() + " " + silo.error());
            BlockPos beyond = floor.offset((int) (reach * 1.3), 0, 0);
            silo.configure(player, beyond, false);
            silo.validate();
            helper.assertTrue(!silo.canReach(beyond), "130% of its reach cannot");
            helper.assertTrue("range".equals(silo.error()), "so it is out of range, got " + silo.error());
            String message = silo.errorMessage();
            helper.assertTrue(message.contains("range_detail") && message.contains(String.valueOf((int) Math.floor(reach))),
                    "and says how far the rocket reaches: " + message);
            helper.succeed();
        });
    }

    /** A single solid tank only lifts its rocket out of the silo: it says so rather than that it reaches 0 blocks. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aRocketThatReachesNothingSaysWhy(GameTestHelper helper) {
        SiloControllerEntity silo = aimed(helper, new SiloTestBuilder().tanks(1), helper.makeMockPlayer(GameType.SURVIVAL));
        helper.runAfterDelay(10, () -> {
            silo.validate();
            helper.assertTrue(silo.reach() < 1, "one solid tank reaches nothing, got " + silo.reach());
            helper.assertTrue("range_none".equals(silo.error()), "and says why, got " + silo.error() + " " + silo.errorMessage());
            helper.succeed();
        });
    }
}
