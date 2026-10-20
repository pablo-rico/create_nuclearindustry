package org.papiricoh.create_nuclearindustry.missile.silo.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The launch site's moving and lit parts, drawn by {@link SiloControllerRenderer}. Must be created before models load (mod
 * constructor).
 *
 * <p>Coordinates are block pixels (16 a block) from the lowest corner of the block model they belong with; the {@code float[]}
 * pivots are the same points in blocks, ready for {@code SuperByteBuffer.translate}.
 */
public final class SiloPartials {
    /**
     * The generator's flywheel in the launch controller's plinth, seen through its grille. Spins with the shaft about the
     * vertical through x = z = 8, so it must be round about that axis; modelled for the controller facing north.
     */
    public static final PartialModel FLYWHEEL = of("block/silo_controller/flywheel");
    /**
     * The controller's three status lamps, lit: green when ready, amber armed (blinking through a countdown), red while it cannot
     * launch. Each is only its glowing lens, in its socket on the front panel of the controller facing north (z = 0 side); the
     * body model has the same lenses dark. Drawn at full brightness and turned with the controller.
     */
    public static final PartialModel LAMP_GREEN = of("block/silo_controller/lamp_green");
    public static final PartialModel LAMP_AMBER = of("block/silo_controller/lamp_amber");
    public static final PartialModel LAMP_RED = of("block/silo_controller/lamp_red");
    /**
     * A hold-down clamp of a formed launch base, modelled on the base's north edge block ({@code part=n}) and standing on its top
     * (y from 16): it hinges on the x axis through {@link #CLAMP_HINGE} and closes on the rocket to the south, its jaw meeting the
     * 12 pixel fuselage at z = 18 (the centre block's z = 2). It opens by turning {@link #CLAMP_OPEN_ANGLE} degrees outward
     * ({@code rotateXDegrees(-angle)} about the hinge). The other three are the same turned about the base's centre.
     */
    public static final PartialModel CLAMP = of("block/launch_base/clamp");

    /** The clamp's hinge: (8, 16, 6) pixels on its edge block. */
    public static final float[] CLAMP_HINGE = {0.5f, 1, 6 / 16f};
    /** How far an open clamp is turned back from the rocket, degrees. */
    public static final float CLAMP_OPEN_ANGLE = 75;

    private SiloPartials() {
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    public static void init() {
    }
}
