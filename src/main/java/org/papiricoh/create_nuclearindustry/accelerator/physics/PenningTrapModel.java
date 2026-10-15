package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * A Penning trap: a strong axial magnetic field between two magnet heads and an electric well hold
 * a cloud of antiprotons in vacuum, away from the walls. Holding them costs Forge Energy every tick,
 * a fixed {@code idleFe} for the field plus {@code loadFe} at full charge (a denser cloud needs a
 * deeper well against its own space charge). Pure arithmetic.
 *
 * <ul>
 *   <li>Each tick the trap spends its demand from its buffer. The field strength is the share of the
 *       demand it got.</li>
 *   <li>A fully fed trap is {@link TrapState#STABLE} and loses nothing; with its buffer under
 *       {@code lowPowerShare} it warns {@link TrapState#LOW_POWER}.</li>
 *   <li>A loaded trap that is short of energy, even slightly, is {@link TrapState#QUENCHING}: the
 *       countdown ({@code quenchTicks}, three seconds by default) drops one per tick and, at zero,
 *       the cloud touches the wall and annihilates. Restoring power stops the countdown and it
 *       climbs back one per tick, so flickering power does not reset it.</li>
 *   <li>An empty trap never quenches; without power it is {@link TrapState#OFFLINE}.</li>
 * </ul>
 */
public final class PenningTrapModel {
    /** Antimatter below this counts as none, ng. */
    public static final double EMPTY = 1.0e-9;

    /**
     * @param capacity      most antimatter the trap holds, ng
     * @param idleFe        FE per tick the field costs, loaded or not
     * @param loadFe        extra FE per tick at full charge, in proportion to the fill
     * @param bufferTicks   ticks of full demand the trap's own buffer holds
     * @param quenchTicks   ticks from losing the field to annihilation
     * @param lowPowerShare buffer share below which a loaded trap warns of low power, 0..1
     */
    public record Params(double capacity, double idleFe, double loadFe, int bufferTicks, int quenchTicks,
                         double lowPowerShare) {
        /** 32 ng for 20 FE/t at full charge. */
        public static final Params SMALL = new Params(32, 8, 12, 200, 60, 0.25);
        /** 256 ng for 80 FE/t at full charge. */
        public static final Params MEDIUM = new Params(256, 32, 48, 200, 60, 0.25);
        /** 2048 ng for 250 FE/t at full charge. */
        public static final Params LARGE = new Params(2048, 100, 150, 200, 60, 0.25);
        public static final Params DEFAULT = SMALL;
    }

    /**
     * One tick of a trap.
     *
     * @param feUsed          FE taken from the buffer
     * @param field           share of its demand the trap got, 0..1
     * @param state           what the field is doing now
     * @param quenchTicksLeft countdown to annihilation, {@code quenchTicks} when healthy
     * @param annihilate      the cloud is lost this tick: the trap must explode with what it holds
     */
    public record Step(int feUsed, float field, TrapState state, int quenchTicksLeft, boolean annihilate) {}

    private PenningTrapModel() {}

    /** Share of the capacity in use, 0..1. */
    public static double fill(Params p, double stored) {
        return p.capacity() <= 0 ? 0 : clamp(stored / p.capacity(), 0, 1);
    }

    /** Room left, ng. */
    public static double space(Params p, double stored) {
        return Math.max(0, p.capacity() - Math.max(0, stored));
    }

    public static boolean loaded(double stored) {
        return stored > EMPTY;
    }

    /** FE per tick the trap needs holding this much, exact. */
    public static double demand(Params p, double stored) {
        return Math.max(0, p.idleFe()) + Math.max(0, p.loadFe()) * fill(p, stored);
    }

    /** FE per tick the trap takes from its buffer holding this much, rounded up. */
    public static int demandFe(Params p, double stored) {
        return (int) Math.ceil(demand(p, stored) - 1.0e-9);
    }

    /** FE per tick at full charge. */
    public static double ratedDemand(Params p) {
        return Math.max(0, p.idleFe()) + Math.max(0, p.loadFe());
    }

    /** Size of the trap's FE buffer: {@code bufferTicks} of its rated demand. */
    public static int bufferCapacity(Params p) {
        return FeDrive.buffer(ratedDemand(p), p.bufferTicks());
    }

    /**
     * @param stored     antimatter in the trap, ng
     * @param buffered   FE in its buffer this tick (after cables pushed theirs)
     * @param quenchLeft countdown from the last tick; {@code quenchTicks} on a healthy trap
     */
    public static Step tick(Params p, double stored, int buffered, int quenchLeft) {
        int full = Math.max(1, p.quenchTicks());
        int need = demandFe(p, stored);
        int used = Math.min(Math.max(0, buffered), need);
        float field = need <= 0 ? 1f : (float) used / need;
        int after = Math.max(0, buffered) - used;
        boolean low = after < p.lowPowerShare() * bufferCapacity(p);
        int left = Math.max(0, Math.min(full, quenchLeft));

        if (!loaded(stored)) {
            TrapState state = !holds(field) ? TrapState.OFFLINE : low ? TrapState.LOW_POWER : TrapState.STABLE;
            return new Step(used, field, state, full, false);
        }
        if (holds(field)) {
            left = Math.min(full, left + 1);
            return new Step(used, field, low || left < full ? TrapState.LOW_POWER : TrapState.STABLE, left, false);
        }
        left = Math.max(0, left - 1);
        return new Step(used, field, TrapState.QUENCHING, left, left == 0);
    }

    /** Whether a trap at this field strength holds antimatter safely. Below it, anything put in annihilates. */
    public static boolean holds(float field) {
        return field >= 1f - 1.0e-6f;
    }

    /** Ticks the buffer alone keeps the field up, with nothing coming in. */
    public static int bufferTicksLeft(Params p, double stored, int buffered) {
        return bufferTicksLeft(p, stored, buffered, 0);
    }

    /** Ticks the buffer keeps the field up with {@code inputPerTick} FE arriving; {@link Integer#MAX_VALUE} if it never runs out. */
    public static int bufferTicksLeft(Params p, double stored, int buffered, double inputPerTick) {
        double deficit = demandFe(p, stored) - Math.max(0, inputPerTick);
        if (deficit <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(Math.max(0, buffered) / deficit);
    }

    /**
     * Ticks until a loaded trap annihilates if power never comes back: what the buffer holds plus the
     * quench countdown. {@link Integer#MAX_VALUE} for an empty trap or one fed well enough.
     */
    public static int ticksToAnnihilation(Params p, double stored, int buffered, int quenchLeft, double inputPerTick) {
        if (!loaded(stored)) {
            return Integer.MAX_VALUE;
        }
        int buffer = bufferTicksLeft(p, stored, buffered, inputPerTick);
        if (buffer == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return buffer + Math.max(0, quenchLeft);
    }

    /** Comparator signal: 0 empty, 1..15 with the fill. */
    public static int comparator(Params p, double stored) {
        if (!loaded(stored)) {
            return 0;
        }
        return 1 + (int) Math.floor(fill(p, stored) * 14 + 1.0e-9);
    }

    /** Radius of the rendered cloud as a share of the largest, {@code cbrt(fill)}: its volume follows the charge. */
    public static double cloudScale(Params p, double stored) {
        return Math.cbrt(fill(p, stored));
    }

    /** Pitch of the field hum: deeper empty, higher as the well deepens. */
    public static float humPitch(Params p, double stored) {
        return (float) (0.8 + 0.4 * fill(p, stored));
    }

    /** Volume of the field hum: silent without field, louder with the charge. */
    public static float humVolume(Params p, double stored, float field) {
        return (float) (clamp(field, 0, 1) * (0.3 + 0.5 * fill(p, stored)));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
