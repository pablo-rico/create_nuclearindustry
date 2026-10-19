package org.papiricoh.create_nuclearindustry.missile.flight.client;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.physics.FlightExtrapolation;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.UUID;

/**
 * A flight as one client sees it: the server's latest sample, flown on between samples and eased so it never jumps. Positions
 * are in world space; {@link #position(float)} interpolates between this client tick and the last.
 */
public final class ClientFlight {
    /** Error, blocks, past which the drawn position snaps to the extrapolated one instead of easing onto it. */
    static final double SNAP = 24;
    /** Share of the error eased away each tick. */
    static final double EASE = 0.35;
    /** Client ticks without news after which a flight is dropped. */
    static final int STALE = 60;

    private final UUID id;
    private RocketShape shape = RocketShape.LEGACY;
    private FlightExtrapolation.Sample last;
    @Nullable
    private FlightExtrapolation.Sample previous;
    private long lastLocalTick;
    private int flags;
    private MissileFlight.Phase phase = MissileFlight.Phase.BOOST;
    private int stage;
    private float heading;
    private double gravity;
    private V3 shown, shownBefore, velocity = V3.ZERO;

    ClientFlight(UUID id, FlightFxState state, long gameTime, long localTick, double gravity) {
        this.id = id;
        V3 pos = new V3(state.x(), state.y(), state.z());
        this.shown = pos;
        this.shownBefore = pos;
        accept(state, gameTime, localTick, gravity);
    }

    void accept(FlightFxState state, long gameTime, long localTick, double gravity) {
        FlightExtrapolation.Sample sample = new FlightExtrapolation.Sample(gameTime, new V3(state.x(), state.y(), state.z()),
                new V3(state.vx(), state.vy(), state.vz()));
        if (last == null || gameTime > last.tick()) {
            previous = last;
            last = sample;
        }
        lastLocalTick = localTick;
        if (state.shape() != null) shape = state.shape();
        flags = state.flags();
        MissileFlight.Phase[] phases = MissileFlight.Phase.values();
        phase = phases[Math.max(0, Math.min(phases.length - 1, state.phase()))];
        stage = state.stage();
        heading = state.heading();
        this.gravity = gravity;
    }

    /** Advances what is drawn by one client tick. */
    void tick(long localTick) {
        double dt = localTick - lastLocalTick;
        V3 accel = burning() && previous != null ? FlightExtrapolation.accel(previous, last) : FlightExtrapolation.gravity(gravity);
        V3 target = FlightExtrapolation.position(last, accel, dt);
        velocity = FlightExtrapolation.velocity(last, accel, dt);
        shownBefore = shown;
        shown = FlightExtrapolation.converge(shown.add(velocity), target, EASE, SNAP);
    }

    boolean stale(long localTick) {
        return localTick - lastLocalTick > STALE;
    }

    public UUID id() {
        return id;
    }

    /** Where it is drawn, {@code partial} of the way through this tick. */
    public Vec3 position(float partial) {
        return new Vec3(shownBefore.x() + (shown.x() - shownBefore.x()) * partial, shownBefore.y() + (shown.y() - shownBefore.y()) * partial,
                shownBefore.z() + (shown.z() - shownBefore.z()) * partial);
    }

    /** Its velocity, blocks per tick, as extrapolated this tick. */
    public Vec3 velocity() {
        return new Vec3(velocity.x(), velocity.y(), velocity.z());
    }

    public MissileFlight.Phase phase() {
        return phase;
    }

    /** The stage burning, or the number of stages once they are all gone. */
    public int stage() {
        return stage;
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public boolean burning() {
        return has(FlightFxState.BURNING);
    }

    public boolean rising() {
        return has(FlightFxState.RISING);
    }

    /** Whether the server shows a puppet entity of it near this player, which then draws the rocket itself. */
    public boolean puppet() {
        return has(FlightFxState.PUPPET);
    }

    /** Radians over the ground it was launched along (0 is +Z). */
    public float heading() {
        return heading;
    }

    public RocketShape shape() {
        return shape;
    }

    /** Its frame (+Y up its axis) to the world. */
    public Quaternionf attitude(float partial) {
        RocketAttitude.Quat q = RocketAttitude.orientation(velocity, heading);
        return new Quaternionf((float) q.x(), (float) q.y(), (float) q.z(), (float) q.w());
    }

    /** Where nozzle {@code nozzle} of stage {@code stage} is in the world, or its position if there is no such nozzle. */
    public Vec3 nozzleWorld(int stage, int nozzle, float partial) {
        Vec3 pos = position(partial);
        RocketShape.Stage s = shape.stage(stage);
        if (s == null || nozzle < 0 || nozzle >= s.nozzles().size()) return pos;
        RocketShape.Nozzle n = s.nozzles().get(nozzle);
        V3 world = RocketAttitude.toWorld(new V3(pos.x, pos.y, pos.z), RocketAttitude.orientation(velocity, heading), new V3(n.x(), n.y(), n.z()));
        return new Vec3(world.x(), world.y(), world.z());
    }
}
