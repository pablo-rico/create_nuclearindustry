package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * One missile's flight, tick by tick. Lift-off climbs straight up for {@link BallisticModel.Params#riseHeight},
 * then the guidance steers the thrust at the velocity still to be gained: the one that would fall
 * freely on the aim point from where the missile is now ({@link BallisticModel#requiredVelocity}). The
 * engines cut off the tick that velocity is reached, and the rest of the flight is free fall, worked out
 * exactly from the burnout point. A missile that burns all its stages before reaching that velocity has
 * not the reach: it coasts on and comes down short.
 *
 * <p>Spent stages drop off as they empty, so later stages push less mass. Thrust follows the stage's
 * full rating; the propellant left in the last stage at cutoff is simply not burnt.
 */
public final class MissileFlight {

    public enum Phase {
        /** Engines burning. */
        BOOST,
        /** Coasting above the air. */
        MIDCOURSE,
        /** Coming down through the air. */
        REENTRY,
        /** The last {@link BallisticModel.Params#terminalTicks} before impact. */
        TERMINAL,
        /** Down. */
        IMPACT
    }

    /** What happened in one tick. */
    public record Step(boolean stageDropped, boolean burnout, boolean impact) {
        static final Step NOTHING = new Step(false, false, false);
    }

    /** Where and when a flight will come down, and whether its guidance got it there. */
    public record Prediction(double ticks, V3 impact, double apogee, boolean onTarget) {}

    private final MissileDesign design;
    private final V3 aim;
    /** Height it lifted off from: it climbs straight up until clear of it by the rise height. */
    private final double launchY;
    private V3 position;
    private V3 velocity;
    private int stage;
    private double stagePropellant;
    private int tick;
    private boolean burnedOut;
    /** Burnout was the guidance's cutoff, not the propellant running out. */
    private boolean guidedCutoff;
    private int burnoutTick;
    private V3 burnoutPosition = V3.ZERO;
    private V3 burnoutVelocity = V3.ZERO;
    private double fallTicks;
    private boolean impacted;

    public MissileFlight(MissileDesign design, V3 start, V3 aim, V3 initialVelocity) {
        this(design, start, aim, initialVelocity, start.y());
    }

    private MissileFlight(MissileDesign design, V3 start, V3 aim, V3 initialVelocity, double launchY) {
        this.design = design;
        this.aim = aim;
        this.launchY = launchY;
        this.position = start;
        this.velocity = initialVelocity;
        this.stage = 0;
        this.stagePropellant = design.stages().isEmpty() ? 0 : design.stages().get(0).propellant();
    }

    /** A flight restored from a save. */
    public static MissileFlight restore(MissileDesign design, V3 aim, double launchY, V3 position, V3 velocity, int stage,
                                        double stagePropellant, int tick, boolean burnedOut, boolean guidedCutoff, int burnoutTick,
                                        V3 burnoutPosition, V3 burnoutVelocity, double fallTicks, boolean impacted) {
        MissileFlight flight = new MissileFlight(design, position, aim, velocity, launchY);
        flight.stage = stage;
        flight.stagePropellant = stagePropellant;
        flight.tick = tick;
        flight.burnedOut = burnedOut;
        flight.guidedCutoff = guidedCutoff;
        flight.burnoutTick = burnoutTick;
        flight.burnoutPosition = burnoutPosition;
        flight.burnoutVelocity = burnoutVelocity;
        flight.fallTicks = fallTicks;
        flight.impacted = impacted;
        return flight;
    }

    public MissileFlight copy() {
        return restore(design, aim, launchY, position, velocity, stage, stagePropellant, tick, burnedOut, guidedCutoff, burnoutTick,
                burnoutPosition, burnoutVelocity, fallTicks, impacted);
    }

    /** Advances the flight by one tick. */
    public Step step(BallisticModel.Params params) {
        if (impacted) {
            return Step.NOTHING;
        }
        tick++;
        if (burnedOut) {
            return coast(params);
        }
        double g = params.gravity();
        boolean dropped = false;
        if (stagePropellant <= 0) {
            dropped = dropStage();
            if (stage >= design.stages().size()) {
                cutOff(false, params);
                return coastAfterBurnout(dropped, params);
            }
        }
        MissileDesign.Stage current = design.stages().get(stage);
        double accel = current.thrust() / mass();
        V3 gravity = V3.UP.scale(g);
        if (rising(params)) {
            velocity = velocity.add(V3.UP.scale(accel)).sub(gravity);
            if (velocity.y() < 0) {
                // Not enough thrust to leave the pad: it sits there burning.
                velocity = new V3(velocity.x(), 0, velocity.z());
            }
        } else {
            V3 wanted = BallisticModel.requiredVelocity(position, aim, params);
            V3 toGain = wanted.sub(velocity).add(gravity);
            double needed = toGain.length();
            if (needed <= accel) {
                velocity = wanted;
                burn(current, needed / accel);
                cutOff(true, params);
                return new Step(dropped, true, false);
            }
            velocity = velocity.add(toGain.scale(accel / needed)).sub(gravity);
        }
        burn(current, 1.0);
        position = position.add(velocity);
        return new Step(dropped, false, false);
    }

    /** Still climbing straight up out of its silo. */
    public boolean rising(BallisticModel.Params params) {
        return !burnedOut && position.y() - launchY < params.riseHeight();
    }

    private Step coastAfterBurnout(boolean dropped, BallisticModel.Params params) {
        Step coast = coast(params);
        return new Step(dropped, true, coast.impact());
    }

    private Step coast(BallisticModel.Params params) {
        double since = tick - burnoutTick;
        if (since >= fallTicks) {
            position = BallisticModel.position(burnoutPosition, burnoutVelocity, fallTicks, params.gravity());
            velocity = BallisticModel.velocity(burnoutVelocity, fallTicks, params.gravity());
            impacted = true;
            return new Step(false, false, true);
        }
        position = BallisticModel.position(burnoutPosition, burnoutVelocity, since, params.gravity());
        velocity = BallisticModel.velocity(burnoutVelocity, since, params.gravity());
        return Step.NOTHING;
    }

    private void burn(MissileDesign.Stage current, double fraction) {
        stagePropellant = Math.max(0, stagePropellant - current.massFlow() * fraction);
    }

    private boolean dropStage() {
        if (stage >= design.stages().size()) {
            return false;
        }
        stage++;
        stagePropellant = stage < design.stages().size() ? design.stages().get(stage).propellant() : 0;
        return true;
    }

    private void cutOff(boolean guided, BallisticModel.Params params) {
        burnedOut = true;
        guidedCutoff = guided;
        burnoutTick = tick;
        burnoutPosition = position;
        burnoutVelocity = velocity;
        fallTicks = BallisticModel.ticksToFloor(position, velocity, aim.y(), params.gravity());
        if (fallTicks <= 0) {
            impacted = true;
        }
    }

    /** What the missile weighs now: the payload, the stages still on and the propellant left in the burning one. */
    public double mass() {
        double mass = design.payloadMass();
        for (int i = stage; i < design.stages().size(); i++) {
            MissileDesign.Stage s = design.stages().get(i);
            mass += s.dryMass() + (i == stage ? stagePropellant : s.propellant());
        }
        return mass;
    }

    public Phase phase(BallisticModel.Params params) {
        if (impacted) {
            return Phase.IMPACT;
        }
        if (!burnedOut) {
            return Phase.BOOST;
        }
        if (fallTicks - (tick - burnoutTick) <= params.terminalTicks()) {
            return Phase.TERMINAL;
        }
        return position.y() > params.atmosphereTop() ? Phase.MIDCOURSE : Phase.REENTRY;
    }

    /** Ticks until impact: exact once burnt out, flown ahead on a copy while still boosting. */
    public double ticksToImpact(BallisticModel.Params params) {
        if (impacted) {
            return 0;
        }
        if (burnedOut) {
            return Math.max(0, fallTicks - (tick - burnoutTick));
        }
        return predict(params).ticks();
    }

    /** Flies a copy of this flight to its end. */
    public Prediction predict(BallisticModel.Params params) {
        MissileFlight ahead = copy();
        int start = tick;
        int guard = 0;
        while (!ahead.burnedOut && !ahead.impacted && guard++ < 100_000) {
            ahead.step(params);
        }
        double ticks = ahead.tick - start + ahead.ticksToImpact(params);
        V3 impact = BallisticModel.position(ahead.burnoutPosition, ahead.burnoutVelocity, ahead.fallTicks, params.gravity());
        double apogee = Math.max(ahead.burnoutPosition.y(),
                BallisticModel.apogee(ahead.burnoutPosition, ahead.burnoutVelocity, params.gravity()));
        return new Prediction(ticks, impact, apogee, ahead.guidedCutoff);
    }

    /** The flight {@code design} would fly from {@code start} towards {@code aim}, worked out ahead of launch. */
    public static Prediction predict(MissileDesign design, V3 start, V3 aim, V3 initialVelocity, BallisticModel.Params params) {
        return new MissileFlight(design, start, aim, initialVelocity).predict(params);
    }

    /**
     * The furthest {@code design} can reach on level ground, blocks: the longest shot its guidance still
     * cuts off on, found by halving the gap between what it reaches and what it does not.
     */
    public static double maxRange(MissileDesign design, BallisticModel.Params params) {
        double low = 0;
        double high = 1;
        while (high < 1.0e6 && reaches(design, high, params)) {
            low = high;
            high *= 2;
        }
        for (int i = 0; i < 30 && high - low > 1; i++) {
            double mid = (low + high) / 2;
            if (reaches(design, mid, params)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private static boolean reaches(MissileDesign design, double range, BallisticModel.Params params) {
        return predict(design, V3.ZERO, new V3(range, 0, 0), V3.ZERO, params).onTarget();
    }

    public MissileDesign design() {
        return design;
    }

    public V3 aim() {
        return aim;
    }

    public double launchY() {
        return launchY;
    }

    public V3 position() {
        return position;
    }

    public V3 velocity() {
        return velocity;
    }

    public int stage() {
        return stage;
    }

    public double stagePropellant() {
        return stagePropellant;
    }

    public int tick() {
        return tick;
    }

    public boolean burnedOut() {
        return burnedOut;
    }

    public boolean guidedCutoff() {
        return guidedCutoff;
    }

    public int burnoutTick() {
        return burnoutTick;
    }

    public V3 burnoutPosition() {
        return burnoutPosition;
    }

    public V3 burnoutVelocity() {
        return burnoutVelocity;
    }

    public double fallTicks() {
        return fallTicks;
    }

    public boolean impacted() {
        return impacted;
    }
}
