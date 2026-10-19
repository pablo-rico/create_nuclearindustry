package org.papiricoh.create_nuclearindustry.missile.physics;

import java.util.List;

/**
 * What a rocket stage burns, from what its tanks hold. A tank holds either a solid propellant cartridge or two liquids pumped in
 * through the silo's fuel port: rocket fuel and liquid oxygen, burnt one to one. A stage burns one kind or the other: solid
 * motors push harder for a shorter time, liquid engines get more out of each unit (a faster exhaust), so a liquid stage reaches
 * further. A tank part filled carries that share of a full one. Game units, deliberately fictional, as in {@link MissileDesign}.
 */
public final class PropellantModel {
    /** Millibuckets of each liquid a rocket tank holds. */
    public static final int TANK_CAPACITY = 1000;
    /** Propellant units a full tank of either kind carries. */
    public static final double UNITS_PER_TANK = 2.0;

    public static final double SOLID_THRUST = 0.24;
    public static final double SOLID_EXHAUST = 4.0;
    public static final double LIQUID_THRUST = 0.18;
    public static final double LIQUID_EXHAUST = 5.2;

    public enum Kind { SOLID, LIQUID }

    /**
     * What one tank holds.
     *
     * @param cartridge whether it holds a solid propellant cartridge
     * @param fuel      millibuckets of rocket fuel
     * @param oxidizer  millibuckets of liquid oxygen
     */
    public record Tank(boolean cartridge, int fuel, int oxidizer) {
        public static final Tank EMPTY = new Tank(false, 0, 0);

        public boolean liquid() {
            return fuel > 0 || oxidizer > 0;
        }

        public boolean empty() {
            return !cartridge && !liquid();
        }

        /** The share of a full tank it can burn: all of a cartridge, or as much of the liquids as there is of both. */
        public double fill() {
            if (cartridge) return 1;
            return Math.max(0, Math.min(1, Math.min(fuel, oxidizer) / (double) TANK_CAPACITY));
        }
    }

    /** Why a stage cannot fly, and which of its tanks (from the bottom, zero based) is to blame. */
    public static final class Invalid extends IllegalArgumentException {
        public final int tank;
        public final String reason;

        public Invalid(int tank, String reason) {
            super(reason + " at tank " + tank);
            this.tank = tank;
            this.reason = reason;
        }
    }

    /** A stage worked out: what it burns and how much of it. */
    public record Stage(Kind kind, double fill, MissileDesign.Stage design) {
    }

    private PropellantModel() {
    }

    /**
     * The stage whose tanks hold {@code tanks}, bottom to top. Throws {@link Invalid} with {@code "fuel"} for a tank with nothing it
     * can burn (empty, or holding only one of the two liquids) and {@code "mixed_propellant"} for a stage of both kinds.
     */
    public static Stage stage(List<Tank> tanks) {
        if (tanks.isEmpty()) throw new Invalid(0, "fuel");
        Kind kind = null;
        double fill = 0;
        for (int i = 0; i < tanks.size(); i++) {
            Tank tank = tanks.get(i);
            if (tank.cartridge() && tank.liquid()) throw new Invalid(i, "mixed_propellant");
            if (tank.fill() <= 0) throw new Invalid(i, "fuel");
            Kind here = tank.cartridge() ? Kind.SOLID : Kind.LIQUID;
            if (kind != null && kind != here) throw new Invalid(i, "mixed_propellant");
            kind = here;
            fill += tank.fill();
        }
        int count = tanks.size();
        double dry = 0.6 + 0.1 * count;
        double propellant = UNITS_PER_TANK * fill;
        MissileDesign.Stage design = kind == Kind.SOLID
                ? new MissileDesign.Stage(dry, propellant, SOLID_THRUST, SOLID_EXHAUST)
                : new MissileDesign.Stage(dry, propellant, LIQUID_THRUST, LIQUID_EXHAUST);
        return new Stage(kind, fill, design);
    }

    /** A stage of {@code tanks} full solid cartridges, as every silo rocket burned before liquids. */
    public static Stage solid(int tanks) {
        return stage(java.util.Collections.nCopies(tanks, new Tank(true, 0, 0)));
    }
}
