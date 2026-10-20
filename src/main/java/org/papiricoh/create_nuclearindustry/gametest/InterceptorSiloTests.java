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
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.EngagementSource;
import org.papiricoh.create_nuclearindustry.missile.defense.FireResult;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketSectionEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.List;
import java.util.UUID;

/** Kill vehicle columns are ready without a ground target and only launch from an engagement order. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class InterceptorSiloTests {
    private static final String BATCH = "interceptor_silo";

    private static SiloControllerEntity interceptor(GameTestHelper helper) {
        SiloTestBuilder builder = new SiloTestBuilder().tanks(2);
        SiloControllerEntity controller = builder.build(helper);
        BlockPos payload = SiloTestBuilder.FLOOR.above(builder.height());
        helper.setBlock(payload, SiloRegistry.KILL_VEHICLE.get());
        controller.validate();
        return controller;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void killVehicleColumnIsReadyWithoutGroundTarget(GameTestHelper helper) {
        SiloControllerEntity controller = interceptor(helper);
        RocketSectionEntity payload = helper.getBlockEntity(SiloTestBuilder.FLOOR.above(5));
        helper.succeedWhen(() -> {
            helper.assertTrue(payload != null, "the payload section exists");
            helper.assertTrue(controller.rocket() != null && controller.rocket().composition().interceptor(), "the column composes as an interceptor");
            helper.assertTrue(controller.rocket().warhead().kind() == org.papiricoh.create_nuclearindustry.missile.flight.Warhead.Kind.INERT,
                    "a kill vehicle is an inert payload");
            helper.assertTrue(controller.state() == SiloControllerEntity.State.READY, "it reaches ready with no ground target, got " + controller.state() + " " + controller.error());
            helper.assertTrue(payload.contents().getSlots() == 0, "kill vehicles have no inventory slots");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void warheadAndKillVehicleAreNotAValidPayloadPair(GameTestHelper helper) {
        SiloControllerEntity controller = interceptor(helper);
        helper.setBlock(SiloTestBuilder.FLOOR.above(6), SiloRegistry.WARHEAD.get());
        controller.validate();
        helper.succeedWhen(() -> helper.assertTrue(controller.rocket() == null && !controller.error().isEmpty(),
                "a second payload is rejected by the composition grammar"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void redstoneDoesNotLaunchAnInterceptor(GameTestHelper helper) {
        SiloControllerEntity controller = interceptor(helper);
        helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(controller.state() == SiloControllerEntity.State.READY, "redstone leaves an interceptor standing by");
            BlockPos floor = helper.absolutePos(SiloTestBuilder.FLOOR);
            helper.assertTrue(FlightTracker.get(helper.getLevel()).flights().stream()
                    .noneMatch(f -> f.origin() != null && f.origin().closerThan(floor, 8)), "redstone creates no flight");
            helper.setBlock(SiloTestBuilder.CONTROLLER.below(), Blocks.AIR);
            helper.succeed();
        });
    }

    /**
     * A body already coasting from {@code offset} above and beside the test's floor at {@code velocity}: a rocket without stages
     * burns out on its first tick, so it can be met at once.
     */
    private static Flight coasting(GameTestHelper helper, Vec3 offset, Vec3 velocity) {
        ServerLevel level = helper.getLevel();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(SiloTestBuilder.FLOOR)).add(offset);
        Flight flight = FlightTracker.get(level).launch(level, new MissileDesign(List.of(), .4, 0), start, velocity,
                new Vec3(start.x, level.getMinBuildHeight() + 1, start.z), BlockPos.containing(start.x, level.getMinBuildHeight() + 1, start.z),
                org.papiricoh.create_nuclearindustry.missile.flight.Warhead.INERT, UUID.randomUUID());
        flight.flight().step(org.papiricoh.create_nuclearindustry.missile.MissileConfig.flightParams());
        return flight;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 300)
    public static void anOrderCountsDownAndLaunchesInterceptorCargo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiloControllerEntity controller = interceptor(helper);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        controller.setOperator(owner.getUUID());
        // High above the air and coasting towards the silo: a midcourse target.
        Flight target = coasting(helper, new Vec3(600, 700, 0), new Vec3(-1, 0.5, 0));
        int oldDelay = DefenseConfig.siloLaunchDelayTicks;
        FireResult result;
        try {
            DefenseConfig.siloLaunchDelayTicks = 2;
            result = controller.order(level, target.id(), EngagementSource.MANUAL_TABLE);
        } finally { DefenseConfig.siloLaunchDelayTicks = oldDelay; }
        helper.assertTrue(result == FireResult.QUEUED, "the controller accepts an order");
        helper.succeedWhen(() -> {
            Flight interceptor = FlightTracker.get(level).interceptors().stream()
                    .filter(f -> f.intercept() != null && f.intercept().target().equals(target.id())).findFirst().orElse(null);
            helper.assertTrue(interceptor != null, "the order launches an interceptor, controller " + controller.state());
            helper.assertTrue(interceptor.cargo() != null, "the assembled rocket cargo travels with the interceptor");
            helper.assertTrue(controller.state() == SiloControllerEntity.State.LAUNCHED, "the rocket is consumed after launch");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 200)
    public static void noMeetingPointMeansNoLaunch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiloControllerEntity controller = interceptor(helper);
        // Coasting well within the air, never above it: a kill vehicle cannot meet it.
        Flight low = coasting(helper, new Vec3(300, 150, 0), new Vec3(-1, 0.3, 0));
        int oldDelay = DefenseConfig.siloLaunchDelayTicks;
        try { DefenseConfig.siloLaunchDelayTicks = 1; controller.order(level, low.id(), EngagementSource.MANUAL_TABLE); }
        finally { DefenseConfig.siloLaunchDelayTicks = oldDelay; }
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(FlightTracker.get(level).interceptors().stream().noneMatch(f -> f.intercept() != null && f.intercept().target().equals(low.id())),
                    "an endoatmospheric target has no silo solution");
            helper.assertTrue(controller.state() == SiloControllerEntity.State.READY, "the failed solution returns the silo to ready");
            helper.succeed();
        });
    }
}
