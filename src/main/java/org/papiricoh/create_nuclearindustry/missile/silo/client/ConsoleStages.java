package org.papiricoh.create_nuclearindustry.missile.silo.client;

import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;
import org.papiricoh.create_nuclearindustry.missile.physics.StageStats;
import org.papiricoh.create_nuclearindustry.missile.silo.LaunchDiagram;
import org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition;

import java.util.ArrayList;
import java.util.List;

/** Charge validation may omit flight stats even though all the stages are sound. Cache estimates per snapshot. */
public final class ConsoleStages {
    private LaunchDiagram snapshot;
    private List<LaunchDiagram.StageInfo> rows = List.of();

    public List<LaunchDiagram.StageInfo> rows(LaunchDiagram diagram) {
        if (!diagram.stages().isEmpty()) return diagram.stages();
        if (diagram.equals(snapshot)) return rows;
        snapshot = diagram;
        rows = List.of();
        if (!diagram.error().startsWith("charge_")) return rows;
        try {
            var kinds = RocketComposition.Section.values();
            List<RocketComposition.Section> column = new ArrayList<>();
            for (var cell : diagram.column()) {
                if (cell.has(LaunchDiagram.Cell.GHOST) || cell.section() < 0 || cell.section() >= kinds.length) return rows;
                column.add(kinds[cell.section()]);
            }
            RocketComposition composition = RocketComposition.parse(column);
            List<PropellantModel.Stage> stages = new ArrayList<>();
            for (int s = 0; s < composition.stageStarts().size(); s++) {
                List<PropellantModel.Tank> tanks = new ArrayList<>();
                for (int t = 0; t < composition.tanksPerStage().get(s); t++) {
                    var cell = diagram.column().get(composition.stageStarts().get(s) + 1 + t);
                    int liquid = Math.clamp((int) cell.fill(), 0, 100) * PropellantModel.TANK_CAPACITY / 100;
                    tanks.add(cell.has(LaunchDiagram.Cell.LIQUID) ? new PropellantModel.Tank(false, liquid, liquid)
                            : new PropellantModel.Tank(cell.fill() > 0, 0, 0));
                }
                stages.add(PropellantModel.stage(tanks));
            }
            MissileDesign design = new MissileDesign(stages.stream().map(PropellantModel.Stage::design).toList(), diagram.warhead().mass(), diagram.cep());
            var stats = StageStats.of(design, MissileConfig.gravity);
            List<LaunchDiagram.StageInfo> result = new ArrayList<>();
            for (int s = 0; s < stages.size(); s++) {
                var stage = stages.get(s);
                int tanks = composition.tanksPerStage().get(s);
                var stat = stats.get(s);
                float reach = (float) MissileFlight.maxRange(new MissileDesign(design.stages().subList(0, s + 1), design.payloadMass(), design.cep()), MissileConfig.flightParams());
                result.add(new LaunchDiagram.StageInfo((byte) tanks, (byte) (stage.kind() == PropellantModel.Kind.LIQUID ? 1 : 0),
                        (byte) Math.round(stage.fill() * 100 / tanks), (float) stat.twr(), (float) stat.deltaV(), (float) (stat.burnTicks() / 20), reach));
            }
            rows = List.copyOf(result);
        } catch (IllegalArgumentException ignored) {
            // An incomplete column or unprepared stage still needs the normal build advice.
        }
        return rows;
    }
}
