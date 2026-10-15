package org.papiricoh.create_nuclearindustry.antimatter;

/**
 * Anything that holds or takes antimatter, in nanograms: the Penning traps, the Target Station's
 * collector and, later, the annihilation generator and the antiproton injector. The conduit moves
 * antimatter between holders on its network; a target station pushes what it collects into the
 * holders beside it.
 *
 * <p>Inserting into a holder that is not {@link #powered()} still takes the antimatter, and the
 * holder annihilates it at once: a trap without its field cannot hold anything. Callers that want to
 * avoid that check {@link #powered()} first.
 */
public interface AntimatterHolder {

    /** Antimatter held now, ng. */
    double stored();

    /** Most it can hold, ng; 0 for a holder that only passes antimatter on. */
    double capacity();

    /**
     * Puts up to {@code ng} in and returns what it took.
     *
     * @param simulate only work out what it would take
     */
    double insert(double ng, boolean simulate);

    /** Takes up to {@code ng} out and returns what it gave. */
    double extract(double ng, boolean simulate);

    /** Its containment field is up: antimatter put in now is held, not annihilated. */
    boolean powered();

    /** Room left, ng. */
    default double space() {
        return Math.max(0, capacity() - stored());
    }

    /** Whether it ever takes antimatter in (a collector only gives). The conduit sorts holders by these two. */
    default boolean acceptsAntimatter() {
        return true;
    }

    /** Whether it ever gives antimatter out (a generator only takes). */
    default boolean givesAntimatter() {
        return true;
    }
}
