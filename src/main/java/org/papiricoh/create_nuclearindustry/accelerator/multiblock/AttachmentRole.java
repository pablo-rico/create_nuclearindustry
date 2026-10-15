package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

import net.minecraft.world.level.block.state.BlockState;

/** A machine beside the beamline that serves it. Not saved. */
public enum AttachmentRole {
    KLYSTRON,
    MAGNET_POWER,
    VACUUM_PUMP,
    /** Feeds liquid helium to the superconducting parts and takes the boiled-off gas back. */
    CRYO,
    /** A personnel interlock panel: the beam only runs while every one of them is secured. */
    INTERLOCK;

    /** A block that serves the beamline from beside it. */
    public interface Block {
        AttachmentRole attachmentRole(BlockState state);
    }
}
