package org.papiricoh.create_nuclearindustry.missile.silo;

import java.util.List;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.*;
import static org.papiricoh.create_nuclearindustry.missile.silo.LaunchDiagram.Advice.*;

/** First repair for the bottom-to-top rocket grammar, including incomplete prefixes. */
public final class RocketAdvisor {
    private RocketAdvisor() {}

    public static List<LaunchDiagram.Advice> advise(List<RocketComposition.Section> column) {
        int expected = mask(ENGINE), stages = 0, tanks = 0;
        boolean guidance = false, done = false;
        int guidanceIndex = -1;
        for (int i = 0; i < column.size(); i++) {
            var section = column.get(i);
            if ((expected & mask(section)) == 0) {
                return List.of(new LaunchDiagram.Advice((byte) i, REPLACE, (byte) expected,
                        done ? "remove_extra" : "replace", 0));
            }
            switch (section) {
                case ENGINE -> { stages++; tanks = 0; expected = mask(TANK); }
                case TANK -> {
                    tanks++;
                    expected = mask(GUIDANCE) | (tanks < 4 ? mask(TANK) : 0) | (stages < 3 ? mask(ENGINE) : 0);
                }
                case GUIDANCE -> { guidance = true; guidanceIndex = i; expected = mask(WARHEAD) | mask(KILL_VEHICLE); }
                case WARHEAD, KILL_VEHICLE -> { done = true; expected = 0; }
            }
        }
        if (!done) return List.of(new LaunchDiagram.Advice((byte) column.size(), ADD, (byte) expected,
                guidance ? "warhead" : tanks == 0 && stages > 0 ? "tank" : stages == 0 ? "engine" : "next", stages));
        if (stages < 3) return List.of(new LaunchDiagram.Advice((byte) guidanceIndex, INFO, (byte) mask(ENGINE), "add_stage", stages));
        return List.of();
    }

    private static int mask(RocketComposition.Section section) { return 1 << section.ordinal(); }
}
