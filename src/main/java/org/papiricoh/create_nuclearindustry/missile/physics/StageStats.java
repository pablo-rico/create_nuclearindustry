package org.papiricoh.create_nuclearindustry.missile.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stage performance at ignition, carrying the fully loaded stages above it. */
public record StageStats(double deltaV, double twr, double burnTicks) {
    public static List<StageStats> of(MissileDesign design, double gravity) {
        List<StageStats> result = new ArrayList<>();
        double above = design.payloadMass();
        for (int i = design.stages().size() - 1; i >= 0; i--) {
            MissileDesign.Stage stage = design.stages().get(i);
            double empty = above + stage.dryMass();
            double full = empty + stage.propellant();
            double deltaV = empty > 0 && full > empty ? stage.exhaustVelocity() * Math.log(full / empty) : 0;
            result.add(new StageStats(deltaV, gravity > 0 && full > 0 ? stage.thrust() / (full * gravity) : 0, stage.burnTicks()));
            above = full;
        }
        Collections.reverse(result);
        return List.copyOf(result);
    }
}
