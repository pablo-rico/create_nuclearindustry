package org.papiricoh.create_nuclearindustry.missile.silo;

import java.util.ArrayList;
import java.util.List;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;

/** Pure, immutable grammar for the bottom-to-top column. Indexes in errors are zero based. */
public record RocketComposition(List<Integer> tanksPerStage, Payload payload) {
    public enum Payload { WARHEAD, KILL_VEHICLE }
    /** Sent and stored by ordinal: add new kinds at the end. */
    public enum Section { ENGINE, TANK, GUIDANCE, WARHEAD, KILL_VEHICLE }
    public RocketComposition {
        tanksPerStage = List.copyOf(tanksPerStage);
        if (tanksPerStage.isEmpty() || tanksPerStage.size() > 3
                || tanksPerStage.stream().anyMatch(n -> n < 1 || n > 4))
            throw new IllegalArgumentException("One to three stages, each with one to four tanks");
    }
    public RocketComposition(List<Integer> tanksPerStage) { this(tanksPerStage, Payload.WARHEAD); }
    public boolean interceptor() { return payload == Payload.KILL_VEHICLE; }
    public static RocketComposition parse(List<Section> sections) {
        if (sections.size() < 4 || sections.size() > 17) throw new Invalid(0, "height");
        List<Integer> stages = new ArrayList<>();
        int i = 0;
        while (i < sections.size() && sections.get(i) == Section.ENGINE) {
            if (stages.size() == 3) throw new Invalid(i, "stages");
            int engine = i++;
            int tanks = 0;
            while (i < sections.size() && sections.get(i) == Section.TANK) { tanks++; i++; }
            if (tanks < 1 || tanks > 4) throw new Invalid(engine + 1, "tanks");
            stages.add(tanks);
        }
        if (stages.isEmpty()) throw new Invalid(0, "engine");
        if (i >= sections.size() || sections.get(i) != Section.GUIDANCE) throw new Invalid(i, "guidance");
        i++;
        if (i >= sections.size() || sections.get(i) != Section.WARHEAD && sections.get(i) != Section.KILL_VEHICLE) throw new Invalid(i, "warhead");
        Payload payload = sections.get(i) == Section.KILL_VEHICLE ? Payload.KILL_VEHICLE : Payload.WARHEAD;
        if (i + 1 != sections.size()) throw new Invalid(i + 1, "extra");
        return new RocketComposition(stages, payload);
    }
    public int height() { return tanksPerStage.stream().mapToInt(n -> n + 1).sum() + 2; }
    public List<Integer> stageStarts() {
        List<Integer> starts = new ArrayList<>();
        int offset = 0;
        for (int tanks : tanksPerStage) { starts.add(offset); offset += tanks + 1; }
        return List.copyOf(starts);
    }
    /** Game units, deliberately fictional; each cartridge contributes two units of propellant. */
    public MissileDesign design(double payloadMass) {
        return design(payloadMass, 0);
    }
    /** The design with guidance of circular error probable {@code cep} blocks. */
    public MissileDesign design(double payloadMass, double cep) {
        return new MissileDesign(tanksPerStage.stream()
                .map(n -> new MissileDesign.Stage(0.6 + 0.1 * n, 2.0 * n, 0.24, 4.0)).toList(), payloadMass, Math.max(0, cep));
    }
    public static final class Invalid extends IllegalArgumentException {
        public final int index;
        public final String reason;
        public Invalid(int index, String reason) { super(reason + " at section " + index); this.index = index; this.reason = reason; }
    }
}
