package org.papiricoh.create_nuclearindustry.missile.flight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEventPayload;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxStatePayload;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Every flight the server has told this client of ({@link org.papiricoh.create_nuclearindustry.missile.flight.FlightFxBroadcaster}),
 * flown on each client tick, and the moments of each passed to whoever listens. Emptied when the player leaves or changes level.
 */
public final class ClientFlights {
    private static final Map<UUID, ClientFlight> FLIGHTS = new LinkedHashMap<>();
    private static final List<FlightFxListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static long localTick;
    @Nullable
    private static ClientLevel level;
    private static boolean initialised;

    private ClientFlights() {
    }

    /** Hooks the client tick and logout; call once from the client side's init. */
    public static void init() {
        if (initialised) return;
        initialised = true;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> clear());
    }

    public static Collection<ClientFlight> all() {
        return Collections.unmodifiableCollection(FLIGHTS.values());
    }

    @Nullable
    public static ClientFlight get(UUID id) {
        return FLIGHTS.get(id);
    }

    public static void addListener(FlightFxListener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(FlightFxListener listener) {
        LISTENERS.remove(listener);
    }

    public static void handleState(FlightFxStatePayload payload) {
        checkLevel();
        for (FlightFxState state : payload.states()) {
            ClientFlight flight = FLIGHTS.get(state.id());
            if (flight == null) {
                FLIGHTS.put(state.id(), new ClientFlight(state.id(), state, payload.gameTime(), localTick, payload.gravity()));
            } else {
                flight.accept(state, payload.gameTime(), localTick, payload.gravity());
            }
        }
    }

    public static void handleEvent(FlightFxEventPayload payload) {
        checkLevel();
        FlightFxEvent event = payload.fxEvent();
        if (event == null) return;
        ClientFlight flight = FLIGHTS.get(payload.id());
        Vec3 pos = new Vec3(payload.x(), payload.y(), payload.z());
        Vec3 vel = new Vec3(payload.vx(), payload.vy(), payload.vz());
        // A satellite's moments carry its id, never a flight's: flight is null for them and nothing here is dropped.
        for (FlightFxListener listener : LISTENERS) listener.onEvent(flight, event, payload.stage(), pos, vel, payload.flags() & 0xFF);
        if (event == FlightFxEvent.IMPACT || event == FlightFxEvent.ABORT || event == FlightFxEvent.INTERCEPT
                || event == FlightFxEvent.INTERCEPTOR_MISS || event == FlightFxEvent.ORBIT_INSERTION) FLIGHTS.remove(payload.id());
    }

    private static void tick() {
        checkLevel();
        if (level == null || FLIGHTS.isEmpty()) return;
        if (Minecraft.getInstance().isPaused()) return;
        localTick++;
        FLIGHTS.values().removeIf(flight -> flight.stale(localTick));
        FLIGHTS.values().forEach(flight -> flight.tick(localTick));
    }

    private static void checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            FLIGHTS.clear();
            level = current;
        }
    }

    private static void clear() {
        FLIGHTS.clear();
        level = null;
    }
}
