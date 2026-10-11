package org.papiricoh.create_nuclearindustry.accelerator;

import org.papiricoh.create_nuclearindustry.NuclearFeature;

/**
 * Content for the particle accelerator: the beamline and ring blocks, the target stations and the
 * antimatter machines. Blocks, items and block entities go on the shared registers.
 */
public final class AcceleratorRegistry implements NuclearFeature {
    public static final AcceleratorRegistry INSTANCE = new AcceleratorRegistry();

    private AcceleratorRegistry() {}
}
