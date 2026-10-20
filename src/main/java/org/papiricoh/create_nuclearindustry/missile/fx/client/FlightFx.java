package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlight;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketAcoustics;
import org.papiricoh.create_nuclearindustry.missile.fx.TrailModel;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

import java.util.ArrayDeque;
import java.util.UUID;

/**
 * What the effects remember of one flight: its trail, how far its engines have lit, where it was launched from, its sounds
 * and the glowing path of a warhead coming back in. Outlives the flight itself until its trail has faded, so the smoke of a
 * rocket that landed or was taken out stays in the sky.
 */
final class FlightFx {
    /** Ticks of plume level remembered, for sounds that arrive late: enough for sound to cross 2.7 km. */
    private static final int HEARD = 160;
    /** Ticks of a warhead's glowing path kept. */
    static final int STREAK = 24;
    /** Ticks a stage waits after the last one falls away before it lights: the ullage motors settle its propellant. */
    static final int ULLAGE_TICKS = 7;

    final UUID id;
    final int seed;
    @Nullable
    ClientFlight flight;
    final TrailModel trail = new TrailModel();

    // Where it was last, at the end of the last client tick (for after it is gone).
    Vec3 position = Vec3.ZERO;
    Vec3 velocity = Vec3.ZERO;
    Quaternionf attitude = new Quaternionf();
    RocketShape shape = RocketShape.LEGACY;
    MissileFlight.Phase phase = MissileFlight.Phase.BOOST;
    int stage;
    int flags;

    /** Height of the ground (or the silo's mouth) it rose from. */
    double groundY;
    /** Whether {@link #groundY} was read from the world rather than guessed (its chunk was not loaded here). */
    boolean groundKnown;
    /** Height it was launched from, for the shock diamonds and the shaking. */
    double launchY;
    /** Game tick of its lift-off, or -1 if this client never saw it. */
    long launchTick = -1;
    @Nullable
    SiloFx.Mouth silo;
    /** Ticks it has been in the air since leaving the silo's shaft, -1 while still rising. */
    long clearedTick = -1;

    /** How lit the engines look, 0 to 1, eased; and what it was each tick for a while. */
    float plume;
    private final float[] heard = new float[HEARD];
    private int heardHead;
    /** The current stage may not light before this game tick (ullage after a separation). */
    long litAfter = Long.MIN_VALUE;
    long ullageFrom = Long.MIN_VALUE;
    /** Game tick its engines stopped for good, or -1. */
    long burnoutTick = -1;
    /** Game tick it was taken away (impact or abort), or -1 while flying. */
    long endedTick = -1;
    boolean nuclearEnd;

    double distance;
    FxBudget.Lod lod = FxBudget.Lod.FAR;
    boolean trailed;

    /** Where the trail's newest smoke left the plume last tick, to spread particles along this tick's path. */
    @Nullable
    Vec3 lastTail;
    final ArrayDeque<Vec3> streak = new ArrayDeque<>();

    final RocketAcoustics.Passage passage = new RocketAcoustics.Passage();
    @Nullable
    RocketSounds.Loop roarNear, roarFar, hiss;
    boolean roarWanted, hissWanted;

    FlightFx(UUID id) {
        this.id = id;
        this.seed = id.hashCode();
    }

    boolean ended() {
        return endedTick >= 0;
    }

    boolean has(int flag) {
        return (flags & flag) != 0;
    }

    boolean burning() {
        return !ended() && has(FlightFxState.BURNING) && !has(FlightFxState.BURNED_OUT);
    }

    boolean rising() {
        return !ended() && has(FlightFxState.RISING);
    }

    /** Coming back down through the air (the game calls the climb through it after burnout re-entry too). */
    boolean descending() {
        return (phase == MissileFlight.Phase.REENTRY || phase == MissileFlight.Phase.TERMINAL) && velocity.y < 0;
    }

    /** Its axis (+Y local) in the world. */
    Vec3 axis() {
        Vector3f a = attitude.transform(new Vector3f(0, 1, 0));
        return new Vec3(a.x(), a.y(), a.z());
    }

    /** Takes in this tick's state of {@code flight}. */
    void update(ClientFlight flight) {
        this.flight = flight;
        position = flight.position(1);
        velocity = flight.velocity();
        attitude = flight.attitude(1);
        shape = flight.shape();
        phase = flight.phase();
        stage = flight.stage();
        flags = 0;
        for (int f : new int[]{FlightFxState.BURNING, FlightFxState.RISING, FlightFxState.NUCLEAR_WARHEAD, FlightFxState.PUPPET,
                FlightFxState.BURNED_OUT}) {
            if (flight.has(f)) flags |= f;
        }
    }

    /** Records this tick's plume level, for what is heard later. */
    void remember() {
        heard[heardHead] = plume;
        heardHead = (heardHead + 1) % HEARD;
    }

    /** The plume level {@code ticksAgo} ticks ago (as long ago as is kept). */
    float heard(int ticksAgo) {
        int back = Math.max(0, Math.min(HEARD - 1, ticksAgo));
        return heard[((heardHead - 1 - back) % HEARD + HEARD) % HEARD];
    }

    void pushStreak(Vec3 nose) {
        streak.addLast(nose);
        while (streak.size() > STREAK) streak.removeFirst();
    }
}
