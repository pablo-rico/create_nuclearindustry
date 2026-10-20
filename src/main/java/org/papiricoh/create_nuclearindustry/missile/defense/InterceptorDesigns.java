package org.papiricoh.create_nuclearindustry.missile.defense;

import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;

import java.util.List;

/** The rockets interceptors fly: a battery's small solid round, and a launch controller's rocket topped by a kill vehicle. */
public final class InterceptorDesigns {
    /** A battery round's outline: two blocks long, slender, one solid motor. */
    public static final RocketShape BATTERY_SHAPE = new RocketShape(2, 0.15f,
            List.of(new RocketShape.Stage(0, 1, List.of(new RocketShape.Nozzle(0, 0, 0)), RocketShape.Propellant.SOLID)), 2);

    private InterceptorDesigns() {
    }

    /** A one-stage round that gains {@code deltaV} blocks per tick and starts at {@code acceleration} blocks per tick squared. */
    public static MissileDesign battery(double deltaV, double acceleration) {
        return new MissileDesign(List.of(new MissileDesign.Stage(0.5, 1.0, acceleration * 1.6, 6.0)), 0.1, 0);
    }

    /** {@code rocket} with its warhead swapped for a kill vehicle of {@code killVehicleMass}. */
    public static MissileDesign killVehicle(MissileDesign rocket, double killVehicleMass) {
        return new MissileDesign(rocket.stages(), killVehicleMass, rocket.cep());
    }
}
