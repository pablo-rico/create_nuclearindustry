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
    public static final PartialModel TURBINE_FRONT_BEZEL = of("turbine/front_bezel");
    public static final PartialModel TURBINE_FAN = of("turbine/fan");
    public static final PartialModel FUSION_TURBINE_FRONT_BEZEL = of("fusion_turbine/front_bezel");
    public static final PartialModel FUSION_TURBINE_FAN = of("fusion_turbine/fan");

    private NuclearPartialModels() {}

    public static PartialModel hub(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? TURBINE_ROTOR_HUB : FUSION_TURBINE_ROTOR_HUB;
    }

    public static PartialModel blade(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? TURBINE_ROTOR_BLADE : FUSION_TURBINE_ROTOR_BLADE;
    }

    /** The 3x3 intake front of a formed turbine, built facing north around the controller. */
    public static PartialModel bezel(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? TURBINE_FRONT_BEZEL : FUSION_TURBINE_FRONT_BEZEL;
    }

    /** The intake fan, built facing north and spinning about Z through the controller's centre. */
    public static PartialModel fan(TurbineTier tier) {
        return tier == TurbineTier.STEAM ? TURBINE_FAN : FUSION_TURBINE_FAN;
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + path));
    }

    static void init() {}
}
