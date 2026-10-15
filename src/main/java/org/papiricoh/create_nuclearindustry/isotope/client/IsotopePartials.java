package org.papiricoh.create_nuclearindustry.isotope.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The Gamma Irradiator's moving and lit parts: the two shutter leaves, the glow of the open
 * aperture, the cone of light down to the belt and the status lamps. They must exist before models
 * load, so {@link #init()} runs from the mod constructor.
 */
public final class IsotopePartials {
    public static final PartialModel SHUTTER_WEST = of("shutter_west");
    public static final PartialModel SHUTTER_EAST = of("shutter_east");
    public static final PartialModel GLOW = of("glow");
    public static final PartialModel BEAM = of("beam");
    public static final PartialModel LAMP = of("lamp");

    private IsotopePartials() {}

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/gamma_irradiator/partial/" + path));
    }

    public static void init() {}
}
