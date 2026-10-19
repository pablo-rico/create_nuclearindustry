package org.papiricoh.create_nuclearindustry.missile.flight;

import java.util.ArrayList;
import java.util.List;

/**
 * The outline of a rocket the client effects need: how tall it is, where each stage starts and ends and where its nozzles sit,
 * all in the rocket's own frame. The pivot is the original tail's bottom centre (the flight's position); +Y runs up the axis
 * and stays put as stages fall away. Kept free of game classes; {@link RocketShapeCodec} writes it. Bounded on construction,
 * so whatever a save or a packet holds stays small.
 */
public record RocketShape(int height, float radius, List<Stage> stages, int payloadBottom) {
    public static final int MAX_HEIGHT = 32;
    public static final int MAX_STAGES = 3;
    public static final int MAX_NOZZLES = 9;

    public enum Propellant {
        SOLID, LIQUID;

        public static Propellant byId(int id) {
            return id == 1 ? LIQUID : SOLID;
        }
    }

    /** A nozzle's exit, in blocks from the pivot. */
    public record Nozzle(float x, float y, float z) {
    }

    /** Sections {@code bottom} to {@code top} (inclusive, from the pivot) burn together and drop together. */
    public record Stage(int bottom, int top, List<Nozzle> nozzles, Propellant propellant) {
        public Stage {
            bottom = Math.max(0, Math.min(MAX_HEIGHT - 1, bottom));
            top = Math.max(bottom, Math.min(MAX_HEIGHT - 1, top));
            nozzles = List.copyOf(nozzles.subList(0, Math.min(MAX_NOZZLES, nozzles.size())));
            if (propellant == null) propellant = Propellant.SOLID;
        }
    }

    /** The old two-block missile of a launch pad: one solid stage under its warhead. */
    public static final RocketShape LEGACY = new RocketShape(2, 0.3f,
            List.of(new Stage(0, 1, List.of(new Nozzle(0, 0, 0)), Propellant.SOLID)), 2);

    public RocketShape {
        height = Math.max(1, Math.min(MAX_HEIGHT, height));
        radius = Math.max(0.1f, Math.min(1.5f, radius));
        stages = List.copyOf(stages.subList(0, Math.min(MAX_STAGES, stages.size())));
        payloadBottom = Math.max(0, Math.min(height, payloadBottom));
    }

    /**
     * A plain column of {@code sections} blocks whose stages start at {@code stageStarts}, one solid nozzle under each, guidance
     * and warhead on top; what a silo rocket looked like before shapes were saved.
     */
    public static RocketShape column(int sections, List<Integer> stageStarts) {
        if (sections <= 0 || stageStarts.isEmpty()) return LEGACY;
        int payload = Math.max(0, sections - 2);
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < stageStarts.size() && i < MAX_STAGES; i++) {
            int bottom = stageStarts.get(i);
            int top = (i + 1 < stageStarts.size() ? stageStarts.get(i + 1) : payload) - 1;
            stages.add(new Stage(bottom, top, List.of(new Nozzle(0, bottom, 0)), Propellant.SOLID));
        }
        return new RocketShape(sections, 0.5f, stages, payload);
    }

    /** The lowest section still attached while stage {@code stage} burns, or the payload's once its engines are done. */
    public int attachedBottom(int stage, boolean burnedOut) {
        if (burnedOut || stage >= stages.size()) return payloadBottom;
        return stages.get(Math.max(0, stage)).bottom();
    }

    /** Stage {@code stage}, or null past the last. */
    public Stage stage(int stage) {
        return stage >= 0 && stage < stages.size() ? stages.get(stage) : null;
    }
}
