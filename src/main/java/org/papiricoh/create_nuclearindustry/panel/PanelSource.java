package org.papiricoh.create_nuclearindustry.panel;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity that shows a card on the panel while the crosshair is on its block, instead of
 * Create's goggle tooltip. Called on the client every frame with what it has synced.
 */
public interface PanelSource {
    /**
     * The card for a crosshair at {@code hit} (world coordinates), or null for none; {@code detailed}
     * while the player sneaks, for the cards that keep more to show then.
     */
    @Nullable
    PanelCard panelCard(Vec3 hit, boolean detailed);
}
