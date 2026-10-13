package org.papiricoh.create_nuclearindustry.controlroom.link;

import net.minecraft.core.BlockPos;

/** Something on an {@link AdvancedLinkNetwork} code: an Advanced Redstone Link, or a control module sending on one. */
public interface CodeLinkable {
    /** What it sends, 0-15; listeners send nothing. */
    int transmittedStrength();

    /** What the network gives it now, for listeners. */
    void setReceivedStrength(int strength);

    boolean isListening();

    /** False once its block is gone: the network drops it. */
    boolean isAlive();

    /** Its code; only {@link LinkCode#complete} codes are on a network. */
    String code();

    BlockPos location();
}
