package org.papiricoh.create_nuclearindustry.missile.physics;

import java.util.List;

/**
 * What a missile is made of, as the flight sees it: its stages, fired from the first, and what rides
 * on top of the last. Masses are in tonnes, thrust in tonne-blocks per tick squared and exhaust speeds
 * in blocks per tick, so a stage's acceleration is its thrust over the mass it still carries.
 *
 * @param stages      fired in order; each one drops off when it burns out
 * @param payloadMass guidance, reentry vehicles and fairing: everything above the last stage
 * @param cep         circular error probable of the guidance, blocks: half its shots land closer than this
 */
public record MissileDesign(List<Stage> stages, double payloadMass, double cep) {

    /**
     * One stage.
     *
     * @param dryMass         its tanks or casing and engine, dropped when it burns out
     * @param propellant      what it burns
     * @param thrust          how hard its engine pushes
     * @param exhaustVelocity how fast its exhaust leaves; propellant burnt per tick is thrust over this
     */
    public record Stage(double dryMass, double propellant, double thrust, double exhaustVelocity) {
        public double massFlow() {
            return exhaustVelocity <= 0 ? 0 : thrust / exhaustVelocity;
        }

        /** Ticks it burns at full thrust. */
        public double burnTicks() {
            double flow = massFlow();
            return flow <= 0 ? 0 : propellant / flow;
        }
    }

    /**
     * The missile an old launch pad fires: one solid stage carrying a single warhead. Its reach is
     * about 3000 blocks at the default gravity, and its guidance is the pad's own: no spread.
     */
    public static final MissileDesign LEGACY_PAD = new MissileDesign(List.of(new Stage(1.0, 8.0, 0.12, 3.0)), 1.0, 0.0);

    public MissileDesign {
        stages = List.copyOf(stages);
    }

    /** Mass at lift-off. */
    public double launchMass() {
        double mass = payloadMass;
        for (Stage stage : stages) {
            mass += stage.dryMass() + stage.propellant();
        }
        return mass;
    }

    /** The ideal speed the stages can give the payload in empty space (the rocket equation, stage by stage). */
    public double deltaV() {
        double above = payloadMass;
        double total = 0;
        for (int i = stages.size() - 1; i >= 0; i--) {
            Stage stage = stages.get(i);
            double full = above + stage.dryMass() + stage.propellant();
            double empty = above + stage.dryMass();
            if (empty > 0 && full > empty) {
                total += stage.exhaustVelocity() * Math.log(full / empty);
            }
            above = full;
        }
        return total;
    }

    /** Thrust over weight at lift-off under {@code gravity}: below 1 it never leaves the ground. */
    public double liftOffThrustToWeight(double gravity) {
        if (stages.isEmpty() || gravity <= 0) {
            return 0;
        }
        return stages.get(0).thrust() / (launchMass() * gravity);
    }

    /** Ticks every stage burns, end to end. */
    public double totalBurnTicks() {
        double ticks = 0;
        for (Stage stage : stages) {
            ticks += stage.burnTicks();
        }
        return ticks;
    }
}
