package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/** Standalone models drawn by the cryogenic renderers. Must be created before models load (mod constructor). */
public final class CryoPartials {
    /** The compressor's impeller behind its grille, modelled facing south. */
    public static final PartialModel COMPRESSOR_IMPELLER = of("cryoplant_compressor_impeller");
    /** The vacuum pump's turbomolecular rotor, modelled facing south. */
    public static final PartialModel VACUUM_PUMP_ROTOR = of("fusion_vacuum_pump_rotor");

    private CryoPartials() {}

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + path));
    }

    static void init() {}
}
