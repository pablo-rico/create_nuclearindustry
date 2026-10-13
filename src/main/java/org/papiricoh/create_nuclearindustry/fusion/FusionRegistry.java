package org.papiricoh.create_nuclearindustry.fusion;

import net.neoforged.bus.api.IEventBus;
import org.papiricoh.create_nuclearindustry.NuclearFeature;
import org.papiricoh.create_nuclearindustry.fusion.gui.TokamakControlPayload;

/** Content for the tokamak core: plasma physics, startup sequence, controller and its fluids: blocks, items and block entities register here. */
public final class FusionRegistry implements NuclearFeature {
    public static final FusionRegistry INSTANCE = new FusionRegistry();

    private FusionRegistry() {}

    @Override
    public void register(IEventBus modEventBus) {
        TokamakControlPayload.register(modEventBus);
    }
}
