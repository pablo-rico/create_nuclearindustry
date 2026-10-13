package org.papiricoh.create_nuclearindustry.reactor.physics;

/** A burnt-out fuel assembly waiting to leave the core: its kind and the enrichment it went in at. */
public record SpentAssembly(FuelKind kind, float enrichment) {
    public static SpentAssembly uranium(float enrichment) {
        return new SpentAssembly(FuelKind.URANIUM, enrichment);
    }
}
