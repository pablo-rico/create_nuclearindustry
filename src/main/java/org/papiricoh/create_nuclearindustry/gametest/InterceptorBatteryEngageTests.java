package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseSettings;
import org.papiricoh.create_nuclearindustry.missile.defense.Doctrine;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;

import java.util.List;
import java.util.UUID;

/**
 * A battery on its own: told of missiles by the radars on its link code, it turns to and fires at one heading for its zone, and
 * leaves alone its owner's missiles and those coming down elsewhere.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class InterceptorBatteryEngageTests {
    /** Each test in a batch of its own: radars see far, and a battery would engage the missiles of a test beside it. */
    private static final String BATCH = "battery_engage_";
    private static final BlockPos RADAR = new BlockPos(9, 1, 9);

    /** A battery loaded with two rounds and a radar beside it, both on {@code code}, both kept turning. */
    private static InterceptorBatteryEntity defended(GameTestHelper helper, String code, @Nullable UUID owner) {
        InterceptorBatteryEntity battery = InterceptorBatteryTests.battery(helper);
        InterceptorBatteryTests.spin(helper, battery);
        battery.setOwner(owner);
        battery.setCode(code);
        battery.setSettings(new DefenseSettings(true, Doctrine.SHOOT_LOOK_SHOOT, battery.getBlockPos().getX(), battery.getBlockPos().getZ(), 0));
        battery.loadOne(InterceptorBatteryTests.rounds(1));
        battery.loadOne(InterceptorBatteryTests.rounds(1));
        helper.setBlock(RADAR, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = helper.getBlockEntity(RADAR);
        helper.assertTrue(radar != null, "the radar has its block entity");
        radar.setCode(code);
        helper.onEachTick(() -> radar.setSpeed((float) MissileConfig.minimumRpm));
        return battery;
    }

    /**
     * A warhead-less missile already coasting from {@code offset} above and beside the battery at {@code velocity}: a rocket with
     * no stages burns out on its first tick.
     */
    private static Flight coasting(GameTestHelper helper, Vec3 offset, Vec3 velocity, @Nullable UUID owner) {
        ServerLevel level = helper.getLevel();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(InterceptorBatteryTests.MASTER)).add(offset);
        Vec3 ground = new Vec3(start.x, helper.absolutePos(BlockPos.ZERO).getY(), start.z);
        Flight flight = FlightTracker.get(level).launch(level, new MissileDesign(List.of(), .4, 0), start, velocity, ground,
                BlockPos.containing(ground), Warhead.INERT, owner);
        flight.flight().step(MissileConfig.flightParams());
        return flight;
    }

    @Nullable
    private static Flight interceptorAt(GameTestHelper helper, Flight target) {
        return FlightTracker.get(helper.getLevel()).interceptors().stream()
                .filter(f -> f.intercept() != null && f.intercept().target().equals(target.id())).findFirst().orElse(null);
    }

    private static void cleanUp(GameTestHelper helper, Flight... flights) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = FlightTracker.get(level);
        for (Flight flight : flights) {
            Flight interceptor = interceptorAt(helper, flight);
            if (interceptor != null) tracker.remove(level, interceptor);
            if (tracker.get(flight.id()) != null) tracker.remove(level, flight);
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "zone", timeoutTicks = 300)
    public static void onItsOwnItEngagesAMissileHeadingForItsZone(GameTestHelper helper) {
        int age = DefenseConfig.trackMinSeconds;
        DefenseConfig.trackMinSeconds = 0;
        InterceptorBatteryEntity battery = defended(helper, "BTA1", UUID.randomUUID());
        // Falls about 40 blocks past the battery in some 340 ticks.
        Flight missile = coasting(helper, new Vec3(250, 300, 0), new Vec3(-0.6, 0, 0), UUID.randomUUID());
        helper.succeedWhen(() -> {
            Flight interceptor = interceptorAt(helper, missile);
            helper.assertTrue(interceptor != null, "an interceptor goes up at it, battery " + battery.status());
            helper.assertTrue(interceptor.intercept().launcher() != null && interceptor.intercept().launcher().equals(battery.getBlockPos()),
                    "from this battery");
            int loaded = 0;
            for (int i = 0; i < InterceptorBatteryEntity.TUBES; i++) if (!battery.tubeInventory().getStackInSlot(i).isEmpty()) loaded++;
            helper.assertTrue(loaded == 1, "one round spent, " + loaded + " left");
            DefenseConfig.trackMinSeconds = age;
            cleanUp(helper, missile);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "own", timeoutTicks = 200)
    public static void itLeavesItsOwnersMissilesAlone(GameTestHelper helper) {
        int age = DefenseConfig.trackMinSeconds;
        DefenseConfig.trackMinSeconds = 0;
        UUID owner = UUID.randomUUID();
        defended(helper, "BTA2", owner);
        Flight missile = coasting(helper, new Vec3(250, 300, 0), new Vec3(-0.6, 0, 0), owner);
        helper.runAfterDelay(120, () -> {
            DefenseConfig.trackMinSeconds = age;
            helper.assertTrue(interceptorAt(helper, missile) == null, "no interceptor at its owner's missile");
            cleanUp(helper, missile);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH + "outside", timeoutTicks = 200)
    public static void itLeavesAloneMissilesComingDownOutsideItsZone(GameTestHelper helper) {
        int age = DefenseConfig.trackMinSeconds;
        DefenseConfig.trackMinSeconds = 0;
        InterceptorBatteryEntity battery = defended(helper, "BTA3", UUID.randomUUID());
        // A small zone far to the west: the missile comes down near the battery, well outside it.
        battery.setSettings(battery.settings().withZone(battery.getBlockPos().getX() - 3000, battery.getBlockPos().getZ(), 50));
        Flight missile = coasting(helper, new Vec3(250, 300, 0), new Vec3(-0.6, 0, 0), UUID.randomUUID());
        helper.runAfterDelay(120, () -> {
            DefenseConfig.trackMinSeconds = age;
            helper.assertTrue(interceptorAt(helper, missile) == null, "no interceptor at a missile landing outside the zone");
            cleanUp(helper, missile);
            helper.succeed();
        });
    }
}
