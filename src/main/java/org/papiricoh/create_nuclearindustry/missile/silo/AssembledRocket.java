package org.papiricoh.create_nuclearindustry.missile.silo;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;

public record AssembledRocket(UUID launchId, BlockPos base, RocketComposition composition, MissileDesign design,
                              Warhead warhead, List<String> visuals, List<UUID> identities) {
    public AssembledRocket { base = base.immutable(); visuals = List.copyOf(visuals); identities = List.copyOf(identities); }
    public int height() { return composition.height(); }
}
