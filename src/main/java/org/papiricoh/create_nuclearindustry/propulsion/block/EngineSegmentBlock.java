package org.papiricoh.create_nuclearindustry.propulsion.block;

import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * A segment of a nuclear engine between its controller and its nozzle: a heat exchanger section of the
 * thermal engine, a magnetic coil of the antimatter engine. Laid along the engine's axis.
 */
public class EngineSegmentBlock extends RotatedPillarBlock {
    public EngineSegmentBlock(Properties properties) {
        super(properties);
    }
}
