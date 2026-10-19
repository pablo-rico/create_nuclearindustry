package org.papiricoh.create_nuclearindustry.missile.silo;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;

public record AssembledRocket(UUID launchId, BlockPos base, RocketComposition composition, MissileDesign design,
                              Warhead warhead, List<String> visuals, List<UUID> identities, GuidanceTier guidance,
                              List<PropellantModel.Kind> propellants) {
    public AssembledRocket { base = base.immutable(); visuals = List.copyOf(visuals); identities = List.copyOf(identities); propellants = List.copyOf(propellants); }
    public int height() { return composition.height(); }
    /** Its design flown with guidance of circular error probable {@code cep}. */
    public MissileDesign design(double cep) { return new MissileDesign(design.stages(), design.payloadMass(), Math.max(0, cep)); }
    /** The seed its guidance error is drawn from: the same rocket always misses the same way. */
    public long missSeed() { return launchId.getMostSignificantBits() ^ Long.rotateLeft(launchId.getLeastSignificantBits(), 17); }
}
