package org.papiricoh.create_nuclearindustry.civildefense;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The threats pending over one level, keyed by whatever reported them (a missile, a bomb, a launch
 * pad). Pure bookkeeping without game classes so it can be unit tested; {@link ThreatRegistry}
 * keeps one per level.
 */
public final class ThreatBoard {

    /**
     * Something about to hit {@code (x, y, z)}.
     *
     * @param radius    how far around the target it reaches, in blocks (its fallout radius, say)
     * @param impactAt  game time of the impact; at or before "now" for a hazard already under way
     * @param expiresAt game time after which the entry is dropped unless reported again
     */
    public record Threat(int x, int y, int z, int radius, long impactAt, long expiresAt) {

        /** Whether a siren at {@code (listenX, listenZ)} listening {@code listenRadius} blocks around is in reach. */
        public boolean reaches(int listenX, int listenZ, int listenRadius) {
            long dx = (long) x - listenX;
            long dz = (long) z - listenZ;
            long reach = (long) Math.max(0, listenRadius) + Math.max(0, radius);
            return dx * dx + dz * dz <= reach * reach;
        }

        public long ticksToImpact(long now) {
            return Math.max(0, impactAt - now);
        }
    }

    private final Map<Object, Threat> threats = new LinkedHashMap<>();

    public void put(Object source, Threat threat) {
        threats.put(source, threat);
    }

    public void remove(Object source) {
        threats.remove(source);
    }

    public Threat get(Object source) {
        return threats.get(source);
    }

    /** Drops the entries whose source stopped reporting them. */
    public void prune(long now) {
        threats.values().removeIf(threat -> threat.expiresAt() < now);
    }

    /** The threat in reach of a listener that lands first, or {@code null}. */
    public Threat soonestReaching(int listenX, int listenZ, int listenRadius, long now) {
        prune(now);
        Threat soonest = null;
        for (Threat threat : threats.values()) {
            if (threat.reaches(listenX, listenZ, listenRadius) && (soonest == null || threat.impactAt() < soonest.impactAt())) {
                soonest = threat;
            }
        }
        return soonest;
    }

    public Collection<Threat> all() {
        return Collections.unmodifiableCollection(threats.values());
    }

    public int size() {
        return threats.size();
    }

    public void clear() {
        threats.clear();
    }
}
