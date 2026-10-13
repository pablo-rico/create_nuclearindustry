package org.papiricoh.create_nuclearindustry.turbine.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/** Standalone models drawn by renderers. Must be created before models load (mod constructor). */
public final class NuclearPartialModels {
    public static final PartialModel TURBINE_ROTOR_HUB = of("turbine/rotor_hub");
    public static final PartialModel TURBINE_ROTOR_BLADE = of("turbine/rotor_blade");
    public static final PartialModel FUSION_TURBINE_ROTOR_HUB = of("fusion_turbine/rotor_hub");
    public static final PartialModel FUSION_TURBINE_ROTOR_BLADE = of("fusion_turbine/rotor_blade");

    private NuclearPartialModels() {}

    public static PartialModel hub(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? TURBINE_ROTOR_HUB : FUSION_TURBINE_ROTOR_HUB;
    }

    public static PartialModel blade(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? TURBINE_ROTOR_BLADE : FUSION_TURBINE_ROTOR_BLADE;
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + path));
    }

    static void init() {}
}
