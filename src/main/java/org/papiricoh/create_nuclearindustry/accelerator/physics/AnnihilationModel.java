package org.papiricoh.create_nuclearindustry.accelerator.physics;

import java.util.Locale;

/**
 * The annihilation generator: antiprotons meet matter in a magnetic chamber and the energy comes out
 * as rotation, Forge Energy or both. At full throttle it burns {@code ratedBurn} nanograms per tick
 * for {@code ratedCapacity} SU per RPM at {@code ratedSpeed}, or {@code ratedEnergy} FE per tick. A
 * generator with fuel keeps a small standby burn to hold its own chamber's field. Pure arithmetic.
 *
 * <p>The chamber is a tiny Penning trap ({@link #CHAMBER}, 1 ng) that the generator tops up from the
 * holders beside it and from vials:
 * <ul>
 *   <li>A <b>cold</b> chamber (nothing burnt last tick) holds antimatter only with Forge Energy in
 *       its buffer, like a trap. Antimatter in a cold chamber without energy quenches and annihilates;
 *       antimatter put in while the field is down annihilates at once ({@link #powered}).</li>
 *   <li>Once the field holds, the antimatter ignites and the chamber is <b>lit</b>: the burn itself
 *       holds the field (the standby share pays for it), so a lit chamber needs no more energy and
 *       cannot quench while it has fuel. The field the burn raised outlives the last burn by
 *       {@link #LIT_GRACE_TICKS}: a chamber that burnt itself empty still takes antimatter for that
 *       long (a brief gap in the supply), and only then goes out; the next fill needs the field again
 *       (a black start).</li>
 *   <li>In {@link Mode#ELECTRIC} or {@link Mode#SPLIT} the generator's own Forge Energy can keep that
 *       buffer full, so after one start it never needs anyone else's energy.</li>
 * </ul>
 * Use {@link #step} for the whole tick; {@link #tick} is the burn alone.
 */
public final class AnnihilationModel {

    /** Antimatter the chamber holds at most, ng. The generator keeps it topped up from outside. */
    public static final double CHAMBER_NG = 1.0;
    /** The chamber's field: a 1 ng trap that costs 8 FE/t, 12 FE/t full, with ten seconds of buffer. */
    public static final PenningTrapModel.Params CHAMBER = new PenningTrapModel.Params(CHAMBER_NG, 8, 4, 200, 60, 0.25);
    /** Antimatter the generator pulls into its chamber per tick at most, ng: a full chamber in a second. */
    public static final double PULL_PER_TICK = 0.05;
    /**
     * Ticks a lit chamber keeps its field after its last burn, empty or not: the field the burn raised
     * takes as long to collapse as a trap takes to quench ({@code CHAMBER.quenchTicks()}). A supply gap
     * shorter than this (a trap swapped, a conduit segment rebuilt) does not put the generator out,
     * so a generator in {@link Mode#ROTATION} with no cable for a black start keeps running.
     */
    public static final int LIT_GRACE_TICKS = 60;
    /** The shaft is refreshed only when the capacity moves more than this share (Create's network update is costly). */
    public static final double CAPACITY_TOLERANCE = 0.02;

    /** Where the generator's power goes. Saved by name. */
    public enum Mode {
        ROTATION, ELECTRIC, SPLIT;

        public double rotationShare() {
            return switch (this) {
                case ROTATION -> 1.0;
                case ELECTRIC -> 0.0;
                case SPLIT -> 0.5;
            };
        }

        /** Whether this mode turns a shaft at all. */
        public boolean turnsShaft() {
            return rotationShare() > 0;
        }

        /** Whether this mode makes Forge Energy at all. */
        public boolean makesEnergy() {
            return rotationShare() < 1;
        }

        /** The mode the generator runs in: with Forge Energy disabled ({@code ratedEnergy} 0) it only turns. */
        public Mode effective(Params p) {
            return p.ratedEnergy() > 0 ? this : ROTATION;
        }

        /** Next mode for the wrench or the value box; only {@link #ROTATION} when Forge Energy is disabled. */
        public Mode next(Params p) {
            if (p.ratedEnergy() <= 0) {
                return ROTATION;
            }
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        /** Lower-case name for lang keys. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Mode saved by name, {@link #ROTATION} for anything unknown. */
        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return ROTATION;
        }
    }

    /**
     * @param ratedBurn     antiprotons burnt per tick at full throttle, ng
     * @param ratedSpeed    shaft speed the generator turns at, RPM
     * @param ratedCapacity stress capacity at full throttle, SU per RPM
     * @param ratedEnergy   Forge Energy at full throttle, FE per tick (0 disables Forge Energy)
     * @param standbyShare  share of the rated burn a fuelled generator always uses
     */
    public record Params(double ratedBurn, double ratedSpeed, double ratedCapacity, double ratedEnergy,
                         double standbyShare) {
        /** 6 ng a minute for 256 RPM x 256 SU/RPM, or 16384 FE/t. */
        public static final Params DEFAULT = new Params(6.0 / 1200.0, 256, 256, 16384, 0.02);

        /** Antiprotons the standby burn uses per tick, ng. */
        public double standbyBurn() {
            return Math.max(0, ratedBurn) * clamp(standbyShare, 0, 1);
        }
    }

    /**
     * One tick of the burn.
     *
     * @param burnt    antiprotons used, ng
     * @param capacity stress capacity offered, SU per RPM
     * @param energy   Forge Energy made, FE per tick
     * @param fraction share of the rated output delivered, 0..1 (the throttle, less any shortfall)
     */
    public record Output(double burnt, double capacity, double energy, double fraction) {
        public static final Output NONE = new Output(0, 0, 0, 0);

        /** Whether anything burnt this tick. */
        public boolean burning() {
            return burnt > 0;
        }

        /** Forge Energy made, whole FE per tick (rounded down). */
        public int energyFe() {
            return (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.floor(energy + 1.0e-9)));
        }
    }

    /**
     * One whole tick of the generator: its chamber field and the burn.
     *
     * @param output          what the burn gave (NONE when it did not burn)
     * @param feUsed          FE taken from the chamber's buffer (0 while lit)
     * @param field           field strength, 0..1
     * @param state           what the chamber's field is doing, as a trap's
     * @param quenchTicksLeft countdown to annihilation, {@code CHAMBER.quenchTicks()} when healthy
     * @param annihilate      the chamber lost its field with antimatter inside: explode with what it holds
     * @param litTicks        ticks the burn's own field still holds: {@link #LIT_GRACE_TICKS} after a
     *                        burn, counting down while empty, 0 cold. Save it and pass it to the next step
     */
    public record Step(Output output, int feUsed, float field, TrapState state, int quenchTicksLeft,
                       boolean annihilate, int litTicks) {
        /** The chamber holds its own field next tick (it burnt within the grace window). */
        public boolean lit() {
            return litTicks > 0;
        }
    }

    private AnnihilationModel() {}

    /**
     * @param throttle  wanted output, 0..1
     * @param available antiprotons in the generator, ng
     */
    public static Output tick(Params p, double throttle, double available, Mode mode) {
        if (available <= 0) {
            return Output.NONE;
        }
        double wanted = clamp(throttle, 0, 1);
        // At zero throttle the standby burn only holds the field and gives nothing.
        double demand = p.ratedBurn() * Math.max(p.standbyShare(), wanted);
        double burnt = Math.min(demand, available);
        double fraction = demand <= 0 ? 0 : wanted * burnt / demand;
        double rotation = (mode == null ? Mode.ROTATION : mode).effective(p).rotationShare();
        return new Output(burnt, fraction * rotation * p.ratedCapacity(), fraction * (1 - rotation) * p.ratedEnergy(),
                fraction);
    }

    /**
     * The generator's whole tick. The chamber holds {@code stored} ng; {@code buffered} is the FE in its
     * field buffer (cables' and, in an electric mode, its own); {@code litTicks} is what the last step
     * said ({@link Step#litTicks()}, 0 for a new generator). The caller takes {@link Step#feUsed()} from
     * the buffer, takes {@code output.burnt()} from the chamber, and explodes it on {@link Step#annihilate()}.
     */
    public static Step step(Params p, PenningTrapModel.Params chamber, double throttle, double stored, Mode mode,
                            int buffered, int quenchLeft, int litTicks) {
        int full = Math.max(1, chamber.quenchTicks());
        int grace = Math.max(0, Math.min(LIT_GRACE_TICKS, litTicks));
        if (!PenningTrapModel.loaded(stored)) {
            if (grace > 0) {
                // Burnt itself empty: the burn's field has not collapsed yet and costs nothing to keep.
                return new Step(Output.NONE, 0, 1f, TrapState.STABLE, full, false, grace - 1);
            }
            PenningTrapModel.Step field = PenningTrapModel.tick(chamber, stored, buffered, quenchLeft);
            return new Step(Output.NONE, field.feUsed(), field.field(), field.state(), field.quenchTicksLeft(), false, 0);
        }
        if (grace > 0) {
            Output out = tick(p, throttle, stored, mode);
            if (holdsItself(p, out)) {
                int left = Math.min(full, Math.max(0, quenchLeft) + 1);
                return new Step(out, 0, 1f, left < full ? TrapState.LOW_POWER : TrapState.STABLE, left, false,
                        LIT_GRACE_TICKS);
            }
            // Burning its last crumbs: the burn is too weak to hold the field, the buffer must.
            PenningTrapModel.Step field = PenningTrapModel.tick(chamber, stored, buffered, quenchLeft);
            return new Step(out, field.feUsed(), field.field(), field.state(), field.quenchTicksLeft(),
                    field.annihilate() && stored - out.burnt() > PenningTrapModel.EMPTY, litAfter(out));
        }
        // Cold: the field must come from the buffer before anything ignites.
        PenningTrapModel.Step field = PenningTrapModel.tick(chamber, stored, buffered, quenchLeft);
        if (!PenningTrapModel.holds(field.field())) {
            return new Step(Output.NONE, field.feUsed(), field.field(), field.state(), field.quenchTicksLeft(),
                    field.annihilate(), 0);
        }
        Output out = tick(p, throttle, stored, mode);
        return new Step(out, field.feUsed(), field.field(), field.state(), field.quenchTicksLeft(), false, litAfter(out));
    }

    /**
     * The old boolean form: {@code lit} counts as one tick of grace, so a chamber that burnt itself
     * empty goes out on the next tick with no grace window (the behaviour before {@link #LIT_GRACE_TICKS}).
     *
     * @deprecated save {@link Step#litTicks()} and use {@link #step(Params, PenningTrapModel.Params, double, double, Mode, int, int, int)}
     */
    @Deprecated
    public static Step step(Params p, PenningTrapModel.Params chamber, double throttle, double stored, Mode mode,
                            int buffered, int quenchLeft, boolean lit) {
        return step(p, chamber, throttle, stored, mode, buffered, quenchLeft, lit ? 1 : 0);
    }

    /** Grace the chamber has after a burn: the full window if anything burnt, cold otherwise. */
    private static int litAfter(Output out) {
        return out.burning() ? LIT_GRACE_TICKS : 0;
    }

    /** {@code litTicks} to restore from an old save that only kept the boolean {@code Lit}. */
    public static int litTicksFromLegacy(boolean lit) {
        return lit ? LIT_GRACE_TICKS : 0;
    }

    /** Whether this burn is strong enough to hold the chamber's field by itself: the full standby burn. */
    public static boolean holdsItself(Params p, Output out) {
        double standby = p.standbyBurn();
        return out.burning() && out.burnt() >= standby * (1 - 1.0e-9);
    }

    /**
     * Whether antimatter put in now is held rather than annihilated (its {@code AntimatterHolder.powered()}):
     * a lit chamber, empty or not, while its grace lasts ({@code litTicks > 0}), or a cold one whose
     * buffer can pay the full chamber's field this tick.
     */
    public static boolean powered(PenningTrapModel.Params chamber, double stored, int buffered, int litTicks) {
        return litTicks > 0
                || Math.max(0, buffered) >= PenningTrapModel.demandFe(chamber, Math.max(stored, chamber.capacity()));
    }

    /**
     * The old boolean form, without the grace window: a lit chamber counts only while it still holds fuel.
     *
     * @deprecated use {@link #powered(PenningTrapModel.Params, double, int, int)} with the saved {@link Step#litTicks()}
     */
    @Deprecated
    public static boolean powered(PenningTrapModel.Params chamber, double stored, int buffered, boolean lit) {
        // A cold chamber must afford the field it will need once filled, not just the empty one's.
        return lit && PenningTrapModel.loaded(stored)
                || Math.max(0, buffered) >= PenningTrapModel.demandFe(chamber, Math.max(stored, chamber.capacity()));
    }

    /** Antimatter to pull into the chamber this tick, ng: the room left, at most {@code maxPerTick}. */
    public static double pull(PenningTrapModel.Params chamber, double stored, double maxPerTick) {
        return ConduitTransfer.move(maxPerTick, Double.MAX_VALUE, PenningTrapModel.space(chamber, stored));
    }

    /** Throttle from an analog redstone signal: 0 standby, 15 full. */
    public static double throttle(int signal) {
        return clamp(signal / 15.0, 0, 1);
    }

    /** Shaft speed to generate: the rated speed while it offers capacity, 0 otherwise (no speed without stress capacity). */
    public static float shaftSpeed(Params p, Output out) {
        return out.capacity() > 0 ? (float) p.ratedSpeed() : 0f;
    }

    /** Whether the shaft's capacity moved enough to tell Create's network. */
    public static boolean capacityChanged(double before, double now) {
        if ((before > 0) != (now > 0)) {
            return true;
        }
        return Math.abs(now - before) > CAPACITY_TOLERANCE * Math.max(1, before);
    }

    /**
     * Forge Energy the generator keeps for itself out of what it made this tick: its chamber's buffer
     * is filled first, the rest goes to its {@code FeSource}.
     *
     * @param made        FE made this tick
     * @param bufferSpace room in the chamber's field buffer, FE
     */
    public static int selfFeed(int made, int bufferSpace) {
        return Math.max(0, Math.min(made, bufferSpace));
    }

    /** Size of the generator's {@code FeSource}: one second of its rated energy, at least 1 FE. */
    public static int sourceCapacity(Params p) {
        return FeDrive.buffer(p.ratedEnergy(), 20);
    }

    /** Comparator signal: 0 cold, 1..15 with the output delivered. */
    public static int comparator(Output out) {
        if (!out.burning()) {
            return 0;
        }
        return 1 + (int) Math.floor(clamp(out.fraction(), 0, 1) * 14 + 1.0e-9);
    }

    /** Pitch of the annihilation drone: low at standby, rising with the output. */
    public static float dronePitch(double fraction) {
        return (float) (0.6 + 0.8 * clamp(fraction, 0, 1));
    }

    /** Volume of the drone: a murmur at standby, loud flat out, silent cold. */
    public static float droneVolume(boolean burning, double fraction) {
        return burning ? (float) (0.25 + 0.75 * clamp(fraction, 0, 1)) : 0f;
    }

    /** Minutes a generator at this throttle runs on this many nanograms. */
    public static double minutesOn(Params p, double nanograms, double throttle) {
        double perTick = p.ratedBurn() * Math.max(p.standbyShare(), clamp(throttle, 0, 1));
        return perTick <= 0 ? Double.POSITIVE_INFINITY : nanograms / perTick / 1200.0;
    }

    /** Antiprotons burnt per minute at this throttle, ng: for goggles, JEI and the Display Link. */
    public static double burnPerMinute(Params p, double throttle) {
        return p.ratedBurn() * Math.max(p.standbyShare(), clamp(throttle, 0, 1)) * 1200.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
