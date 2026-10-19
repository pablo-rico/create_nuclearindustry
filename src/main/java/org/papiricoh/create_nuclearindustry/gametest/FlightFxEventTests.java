package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightEvents;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The moments of a flight the client effects hang on, in the order they happen: lift-off, a stage falling away and the next
 * lighting, burnout, the way down and impact; and an abort for one taken out of the sky. A tracker of its own, ticked by hand.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FlightFxEventTests {
    private static final String BATCH = "missile_flight";

    private record Seen(FlightFxEvent event, int stage) {
    }

    /** A small first stage that runs dry early under a second big enough to finish the climb. */
    private static final MissileDesign TWO_STAGES = new MissileDesign(List.of(new MissileDesign.Stage(0.3, 1.0, 0.24, 4.0),
            new MissileDesign.Stage(0.5, 6.0, 0.24, 4.0)), 1.0, 0.0);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aFlightsMomentsComeInOrder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 3, 6))).add(0, 40, 0);
        BlockPos target = BlockPos.containing(start.add(400, 0, 0));
        UUID id = UUID.randomUUID();
        List<Seen> seen = new ArrayList<>();
        FlightEvents.Listener listener = (l, flight, event, stage, pos, vel) -> {
            if (flight.id().equals(id)) seen.add(new Seen(event, stage));
        };
        FlightEvents.addListener(listener);
        Flight flight;
        CompoundTag saved;
        try {
            flight = tracker.launchModular(level, id, TWO_STAGES, start, Vec3.atCenterOf(target), target, Warhead.INERT, null,
                    RocketShape.column(6, List.of(0, 2)), new CompoundTag(), 1.25f);
            saved = tracker.save(new CompoundTag(), level.registryAccess());
            for (int t = 0; t < 20_000 && tracker.get(id) != null; t++) tracker.tick(level);
        } finally {
            FlightEvents.removeListener(listener);
            if (tracker.get(id) != null) tracker.remove(level, tracker.get(id));
        }
        helper.assertTrue(flight != null, "the flight launches");
        List<FlightFxEvent> order = seen.stream().map(Seen::event).toList();
        helper.assertTrue(!order.isEmpty() && order.get(0) == FlightFxEvent.LAUNCH, "lift-off comes first: " + order);
        helper.assertTrue(order.get(order.size() - 1) == FlightFxEvent.IMPACT, "and impact last: " + order);
        helper.assertTrue(seen.contains(new Seen(FlightFxEvent.STAGE_SEPARATION, 0)), "the first stage falls away: " + seen);
        helper.assertTrue(seen.contains(new Seen(FlightFxEvent.STAGE_IGNITION, 1)), "and the second lights: " + seen);
        helper.assertTrue(order.indexOf(FlightFxEvent.STAGE_SEPARATION) < order.indexOf(FlightFxEvent.STAGE_IGNITION),
                "the spent stage drops before the next lights: " + order);
        helper.assertTrue(order.stream().filter(e -> e == FlightFxEvent.BURNOUT).count() == 1, "the engines stop once: " + order);
        helper.assertTrue(order.indexOf(FlightFxEvent.STAGE_IGNITION) < order.indexOf(FlightFxEvent.BURNOUT), "after the last stage lit: " + order);
        helper.assertTrue(order.contains(FlightFxEvent.TERMINAL) && order.indexOf(FlightFxEvent.TERMINAL) > order.indexOf(FlightFxEvent.BURNOUT),
                "its last seconds come on the way down: " + order);
        helper.assertTrue(!order.contains(FlightFxEvent.ABORT), "a flight that lands is not aborted: " + order);

        Flight restored = FlightTracker.load(saved, level.registryAccess()).get(id);
        helper.assertTrue(restored != null && restored.shape().equals(RocketShape.column(6, List.of(0, 2)))
                && restored.cargo() != null && restored.heading() == 1.25f, "its shape, cargo and heading are saved with it");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void aRemovedFlightIsAborted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 3, 6))).add(0, 40, 0);
        BlockPos target = BlockPos.containing(start.add(400, 0, 0));
        List<FlightFxEvent> seen = new ArrayList<>();
        Flight[] launched = new Flight[1];
        FlightEvents.Listener listener = (l, flight, event, stage, pos, vel) -> {
            if (launched[0] != null && flight.id().equals(launched[0].id())) seen.add(event);
        };
        FlightEvents.addListener(listener);
        try {
            launched[0] = tracker.launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, Vec3.atCenterOf(target), target, Warhead.INERT, null);
            tracker.tick(level);
            tracker.remove(level, launched[0]);
        } finally {
            FlightEvents.removeListener(listener);
        }
        helper.assertTrue(seen.equals(List.of(FlightFxEvent.ABORT)), "taking it out of the sky aborts it, got " + seen);
        helper.assertTrue(launched[0].shape().equals(RocketShape.LEGACY), "a pad missile has the old outline");
        helper.succeed();
    }
}
