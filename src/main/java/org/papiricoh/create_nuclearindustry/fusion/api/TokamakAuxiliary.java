package org.papiricoh.create_nuclearindustry.fusion.api;

/**
 * A block entity in the tokamak shell that serves the plasma: heating, vacuum pumping, or a
 * window that lights up. The controller finds them through the roles of
 * {@link org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakStructure}, so they need no
 * link of their own.
 */
public interface TokamakAuxiliary {

    /** Adds this part's share to the supply; called on the server once per controller tick. */
    void contribute(TokamakSupply.Builder supply);

    /**
     * Hears what the plasma is doing: called on the server every few ticks while formed, and once
     * with {@link TokamakReadings#NONE} when the tokamak breaks.
     */
    default void observe(TokamakReadings readings) {}
}
