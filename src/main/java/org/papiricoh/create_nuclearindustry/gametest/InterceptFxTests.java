package org.papiricoh.create_nuclearindustry.missile.flight;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.defense.InterceptorDesigns;
import org.papiricoh.create_nuclearindustry.missile.defense.ShooterType;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.UUID;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class InterceptFxTests {
    @GameTest(template = org.papiricoh.create_nuclearindustry.gametest.NuclearGameTests.SMALL_ARENA, batch = "intercept_fx")
    public static void newEventsRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean valid = true;
        for (int id : new int[]{10, 11}) {
            FlightFxEventPayload sent = new FlightFxEventPayload(UUID.randomUUID(), (byte) id, (byte) 2, (byte) 96,
                    1.5, 80, -4, 0.1f, -2, 0.2f);
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            FlightFxEventPayload.STREAM_CODEC.encode(buf, sent);
            FlightFxEventPayload received = FlightFxEventPayload.STREAM_CODEC.decode(buf);
            valid &= received.fxEvent() == FlightFxEvent.byId(id) && received.id().equals(sent.id()) && received.flags() == sent.flags();
        }
        helper.assertTrue(valid, "intercept event ids 10 and 11 survive payload encoding");
        helper.succeed();
    }

    @GameTest(template = org.papiricoh.create_nuclearindustry.gametest.NuclearGameTests.SMALL_ARENA, batch = "intercept_fx")
    public static void batteryInterceptorCarriesBothFxFlags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FlightTracker tracker = new FlightTracker();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 3, 6))).add(0, 20, 0);
        Flight.Intercept task = new Flight.Intercept(UUID.randomUUID(), 1, new V3(start.x + 80, start.y + 40, start.z), 80,
                helper.absolutePos(new BlockPos(6, 3, 6)), ShooterType.BATTERY, 1);
        Flight flight = tracker.launchInterceptor(level, UUID.randomUUID(), InterceptorDesigns.battery(5, 0.1), start, new Vec3(0, 1, 0), 10,
                task, null, InterceptorDesigns.BATTERY_SHAPE, null);
        boolean flagged = false;
        if (flight != null) {
            int flags = FlightFxBroadcaster.state(flight, org.papiricoh.create_nuclearindustry.missile.MissileConfig.flightParams(), false).flags();
            flagged = (flags & (FlightFxState.INTERCEPTOR | FlightFxState.CANISTER))
                    == (FlightFxState.INTERCEPTOR | FlightFxState.CANISTER);
            tracker.remove(level, flight);
        }
        helper.assertTrue(flight != null, "battery interceptor launches");
        helper.assertTrue(flagged, "battery interceptor state includes INTERCEPTOR and CANISTER flags");
        helper.succeed();
    }
}
