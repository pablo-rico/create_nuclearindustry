package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import net.minecraft.world.level.block.state.BlockState;

/** A machine beside the beamline that serves it. Not saved. */
public enum AttachmentRole {
    KLYSTRON,
    MAGNET_POWER,
    VACUUM_PUMP,
    CRYO;

    /** A block that serves the beamline from beside it. */
    public interface Block {
        AttachmentRole attachmentRole(BlockState state);
    }
}
