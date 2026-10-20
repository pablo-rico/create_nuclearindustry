package org.papiricoh.create_nuclearindustry.missile.flight;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.orbit.Orbit;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;
import org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteKind;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** What the clients are told of satellites' launches and moments; the effects themselves are client-only and seen in game. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SatelliteFxTests {
    private static final Vec3 START = new Vec3(-19_000_000, 64, 19_000_000);

    @GameTest(template = org.papiricoh.create_nuclearindustry.gametest.NuclearGameTests.SMALL_ARENA, batch = "satellite_fx")
    public static void satelliteEventsRoundTripWithTheTopFlag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean valid = true;
        for (int id : new int[]{12, 13, 14}) {
            FlightFxEventPayload sent = new FlightFxEventPayload(UUID.randomUUID(), (byte) id, (byte) 1, (byte) FlightFxState.SATELLITE,
                    1.5, 1263, -4, 0.1f, -2, 12f);
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            FlightFxEventPayload.STREAM_CODEC.encode(buf, sent);
            FlightFxEventPayload received = FlightFxEventPayload.STREAM_CODEC.decode(buf);
            valid &= received.fxEvent() == FlightFxEvent.byId(id) && received.id().equals(sent.id())
                    && (received.flags() & 0xFF) == FlightFxState.SATELLITE && received.stage() == 1;
        }
        helper.assertTrue(valid, "satellite event ids 12 to 14 and the top flag bit survive payload encoding");
        FlightFxEventPayload unknown = new FlightFxEventPayload(UUID.randomUUID(), (byte) 99, (byte) 0, (byte) 0, 0, 0, 0, 0, 0, 0);
        helper.assertTrue(unknown.fxEvent() == null, "an event id this version does not know is no event");
        helper.succeed();
    }

    @GameTest(template = org.papiricoh.create_nuclearindustry.gametest.NuclearGameTests.SMALL_ARENA, batch = "satellite_fx")
    public static void launchVehicleIsFlaggedAndInsertionCarriesTheKind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        var stage = PropellantModel.stage(Collections.nCopies(4, new PropellantModel.Tank(false, 1000, 1000))).design();
        Satellite satellite = new Satellite(UUID.randomUUID(), "CM-fx", "FXFX", SatelliteKind.COMMS,
                Orbit.leo(START.x, START.z, 0.7, 1600, 0, 12, Orbit.Bounds.UNBOUNDED), null, 0);
        Flight flight = tracker.launchSatellite(level, UUID.randomUUID(), new MissileDesign(List.of(stage, stage), 1, 0), START, 0.7f,
                satellite, null, RocketShape.LEGACY, null);
        int flags = 0, insertion = -1, separation = -1;
        if (flight != null) {
            flags = FlightFxBroadcaster.state(flight, MissileConfig.flightParams(), false).flags() & 0xFF;
            insertion = FlightFxBroadcaster.clientStage(flight, FlightFxEvent.ORBIT_INSERTION, 1);
            separation = FlightFxBroadcaster.clientStage(flight, FlightFxEvent.STAGE_SEPARATION, 0);
            tracker.remove(level, flight);
        }
        helper.assertTrue(flight != null, "launch vehicle launches");
        helper.assertTrue((flags & FlightFxState.SATELLITE) != 0 && (flags & FlightFxState.INTERCEPTOR) == 0,
                "launch vehicle state carries the SATELLITE flag only");
        helper.assertTrue(insertion == SatelliteKind.COMMS.ordinal(), "orbit insertion tells the clients the satellite's kind");
        helper.assertTrue(separation == 0, "other moments keep their stage");
        helper.succeed();
    }
}
