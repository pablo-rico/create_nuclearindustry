package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;

import java.util.Locale;

/**
 * The tokamak's antiproton injector: a small Penning trap on a port of the vessel that, during
 * {@code HEATING}, fires one dose of antiprotons ({@code dose} ng) through a beam nozzle into the
 * plasma. Their annihilation lifts a well-held plasma straight past ignition
 * ({@link PlasmaModel#antiprotonIgnite()}). Pure arithmetic: it decides when to fire, the block entity
 * spends the dose and the shot energy and calls the plasma.
 *
 * <ul>
 *   <li>It fires only when armed, off cooldown, with a full dose in its chamber, {@code shotFe} in its
 *       buffer, and a heating plasma that will take the dose: confinement at least
 *       {@link PlasmaModel#STABLE_CONFINEMENT}, density at least
 *       {@link PlasmaModel#ANTIPROTON_IGNITION_DENSITY} and not yet at {@link PlasmaModel#IGNITION_TEMP}.
 *       Firing outside those conditions would waste the dose on the wall, so it waits.</li>
 *   <li>Each shot starts a {@code cooldownTicks} cooldown, so a plasma that somehow did not take it
 *       is not fed the whole chamber in a second.</li>
 *   <li>Its chamber is a trap ({@link #CHAMBER}): it needs its field like any other, and quenches and
 *       annihilates without it (use {@link PenningTrapModel#tick}).</li>
 * </ul>
 */
public final class InjectorModel {

    /** The injector's chamber: 8 ng (four doses) for 16 FE/t, 24 FE/t full. */
    public static final PenningTrapModel.Params CHAMBER = new PenningTrapModel.Params(8.0, 16, 8, 200, 60, 0.25);

    /** How the injector is armed. Saved by name. */
    public enum Arming {
        /** Fires by itself whenever the plasma is ready. */
        AUTOMATIC,
        /** Fires only while it has a redstone signal. */
        REDSTONE,
        /** Never fires. */
        SAFE;

        public Arming next() {
            Arming[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        /** Lower-case name for lang keys. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Arming byName(String name) {
            for (Arming arming : values()) {
                if (arming.name().equalsIgnoreCase(name)) {
                    return arming;
                }
            }
            return AUTOMATIC;
        }
    }

    /**
     * Why the injector does or does not fire this tick, the first that applies in this order (so the
     * goggles show the operator's problem before the plasma's). Saved and synced by ordinal: new
     * verdicts go at the end.
     */
    public enum Verdict {
        /** Fire now. */
        READY,
        /** Disarmed: {@link Arming#SAFE}, or {@link Arming#REDSTONE} without a signal. */
        SAFE,
        /** Recovering from the last shot. */
        COOLDOWN,
        /** Less than one dose in the chamber. */
        NO_DOSE,
        /** Not enough energy in the buffer for a shot (or the chamber's field is down). */
        NO_POWER,
        /** Not part of a formed tokamak, or the tokamak is not in its heating phase with a plasma. */
        WAITING,
        /** The plasma is already at ignition temperature: a dose would be wasted. */
        IGNITED,
        /** The plasma is held too loosely to take the dose. */
        LOW_CONFINEMENT,
        /** The plasma is too thin to take the dose. */
        LOW_DENSITY;

        public boolean fires() {
            return this == READY;
        }

        /** Whether all the injector's own needs are met and only the plasma is lacking (or it fires). */
        public boolean armedAndLoaded() {
            return ordinal() >= WAITING.ordinal() || this == READY;
        }

        /** Lower-case name for lang keys. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Verdict byOrdinal(int ordinal) {
            Verdict[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : WAITING;
        }
    }

    /**
     * @param dose           antiprotons one shot spends, ng ({@code antimatter.ignitionDose})
     * @param cooldownTicks  ticks after a shot before the next
     * @param shotFe         Forge Energy one shot takes from the buffer (the nozzle's pulse)
     * @param minConfinement least plasma confinement that takes a dose, 0..1
     * @param minDensity     least plasma density, as a fraction of the Greenwald limit
     * @param ignitionTemp   plasma temperature from which a dose is pointless
     */
    public record Params(double dose, int cooldownTicks, int shotFe, double minConfinement, double minDensity,
                         double ignitionTemp) {
        /** 2 ng a shot, every 10 s at most, 20 kFE a shot, with the plasma's own ignition limits. */
        public static final Params DEFAULT = new Params(2.0, 200, 20_000, PlasmaModel.STABLE_CONFINEMENT,
                PlasmaModel.ANTIPROTON_IGNITION_DENSITY, PlasmaModel.IGNITION_TEMP);

        /** The same with another dose (the config's {@code antimatter.ignitionDose}). */
        public Params withDose(double dose) {
            return new Params(dose, cooldownTicks, shotFe, minConfinement, minDensity, ignitionTemp);
        }
    }

    /**
     * What the injector sees of the tokamak this tick.
     *
     * @param heating     formed, in its {@code HEATING} phase, with a plasma
     * @param confinement plasma confinement, 0..1 ({@code PlasmaModel.getConfinement()})
     * @param density     plasma density, fraction of the Greenwald limit ({@code PlasmaModel.getDensity()})
     * @param temperature plasma temperature ({@code PlasmaModel.getTemperature()})
     */
    public record Plasma(boolean heating, double confinement, double density, double temperature) {
        /** No tokamak, or one not heating. */
        public static final Plasma NONE = new Plasma(false, 0, 0, PlasmaModel.AMBIENT_TEMP);
    }

    private InjectorModel() {}

    /** Whether the injector is armed with this setting and redstone signal. */
    public static boolean armed(Arming arming, boolean redstone) {
        return switch (arming == null ? Arming.AUTOMATIC : arming) {
            case AUTOMATIC -> true;
            case REDSTONE -> redstone;
            case SAFE -> false;
        };
    }

    /**
     * Whether to fire this tick and why not.
     *
     * @param armed        from {@link #armed}
     * @param stored       antiprotons in the chamber, ng
     * @param fe           FE in the buffer
     * @param fieldUp      the chamber's field holds ({@link PenningTrapModel#holds})
     * @param cooldownLeft ticks of cooldown left
     */
    public static Verdict verdict(Params p, boolean armed, double stored, int fe, boolean fieldUp, int cooldownLeft,
                                  Plasma plasma) {
        if (!armed) {
            return Verdict.SAFE;
        }
        if (cooldownLeft > 0) {
            return Verdict.COOLDOWN;
        }
        if (!hasDose(p, stored)) {
            return Verdict.NO_DOSE;
        }
        if (!fieldUp || fe < Math.max(0, p.shotFe())) {
            return Verdict.NO_POWER;
        }
        Plasma pl = plasma == null ? Plasma.NONE : plasma;
        if (!pl.heating()) {
            return Verdict.WAITING;
        }
        if (pl.temperature() >= p.ignitionTemp()) {
            return Verdict.IGNITED;
        }
        if (pl.confinement() < p.minConfinement()) {
            return Verdict.LOW_CONFINEMENT;
        }
        if (pl.density() < p.minDensity()) {
            return Verdict.LOW_DENSITY;
        }
        return Verdict.READY;
    }

    /** Whether the plasma would take a dose now, whatever the injector's own state. */
    public static boolean plasmaReady(Params p, Plasma plasma) {
        return plasma != null && plasma.heating() && plasma.temperature() < p.ignitionTemp()
                && plasma.confinement() >= p.minConfinement() && plasma.density() >= p.minDensity();
    }

    /** At least one dose in the chamber (a zero dose always counts). */
    public static boolean hasDose(Params p, double stored) {
        return Math.max(0, stored) >= Math.max(0, p.dose()) - 1.0e-9;
    }

    /** Whole doses in the chamber; {@link Integer#MAX_VALUE} for a zero dose. */
    public static int doses(Params p, double stored) {
        if (p.dose() <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(Math.max(0, stored) / p.dose() + 1.0e-9);
    }

    /** Antiprotons one shot takes out of the chamber, ng: the dose, or what there is. */
    public static double spend(Params p, double stored) {
        return Math.min(Math.max(0, stored), Math.max(0, p.dose()));
    }

    /** Cooldown one tick later. */
    public static int cooldownTick(int left) {
        return Math.max(0, left - 1);
    }

    /** Cooldown right after a shot. */
    public static int cooldownAfterShot(Params p) {
        return Math.max(0, p.cooldownTicks());
    }

    /** The chamber for this dose: {@link #CHAMBER}, grown to hold at least two doses if the config asks for big ones. */
    public static PenningTrapModel.Params chamber(Params p) {
        double capacity = Math.max(CHAMBER.capacity(), 2 * Math.max(0, p.dose()));
        return new PenningTrapModel.Params(capacity, CHAMBER.idleFe(), CHAMBER.loadFe(), CHAMBER.bufferTicks(),
                CHAMBER.quenchTicks(), CHAMBER.lowPowerShare());
    }

    /** Size of the injector's FE buffer: one shot plus its chamber's own field buffer. */
    public static int bufferCapacity(Params p, PenningTrapModel.Params chamber) {
        long total = (long) Math.max(0, p.shotFe()) + PenningTrapModel.bufferCapacity(chamber);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, total));
    }

    /** Antimatter to pull into the chamber this tick, ng: the room left, at most {@code maxPerTick}. */
    public static double pull(PenningTrapModel.Params chamber, double stored, double maxPerTick) {
        return ConduitTransfer.move(maxPerTick, Double.MAX_VALUE, PenningTrapModel.space(chamber, stored));
    }

    /** Energy released in the plasma by one shot, kJ (antiprotons plus as much matter). */
    public static double shotEnergyKj(Params p) {
        return AnnihilationBlastModel.energyKj(Math.max(0, p.dose()));
    }

    /** Comparator signal: whole doses ready, 0..15. */
    public static int comparator(Params p, double stored) {
        return Math.max(0, Math.min(15, doses(p, stored)));
    }
}
