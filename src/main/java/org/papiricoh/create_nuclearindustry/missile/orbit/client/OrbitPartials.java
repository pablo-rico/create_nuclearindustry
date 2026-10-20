package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The satellites' moving and floating parts, drawn by their renderers. Must be created before models load (mod constructor).
 *
 * <p>Coordinates are block pixels (16 a block) from the lowest corner of the block model they belong with; the {@code float[]}
 * pivots are the same points in blocks, ready for {@code SuperByteBuffer.translate}.
 */
public final class OrbitPartials {
    /**
     * The ground station's turntable and yoke, over its plinth (the block model). Turns about the vertical through x = z = 8
     * ({@link #STATION_AXIS}); modelled with its yoke across x, so at rest the dish between its arms looks north. Turn it by the
     * azimuth, degrees clockwise from north, with {@code rotateYDegrees(-azimuth)}.
     */
    public static final PartialModel STATION_MOUNT = of("block/ground_station/mount");
    /**
     * The station's dish with its feed horn and struts. Modelled at rest looking north (-z) and level, its elevation axle along x
     * through (8, 16, 8) ({@link #DISH_PIVOT}) between the yoke's arms. Raise it by the elevation with {@code rotateXDegrees(elevation)}
     * about that pivot (90 looks straight up), then turn it with the mount. It reaches 6 pixels round its axis of sight and its
     * horn 12.5 in front of the pivot, so it overhangs the block: up to y = 28.5 looking up, to z = -4.5 looking level.
     */
    public static final PartialModel STATION_DISH = of("block/ground_station/dish");
    /**
     * The fairing's west half (x 3..8, y 1.5..16) where it sits in the {@code rocket_satellite_fairing} block, without the joint
     * ring, which stays with the rocket. It parts from the other half on the plane x = 8; to peel it away, turn it with
     * {@code rotateZDegrees(+angle)} about its outer bottom edge, the z axis through (3, 1.5, 8) ({@link #FAIRING_LEFT_HINGE}).
     */
    public static final PartialModel FAIRING_HALF_LEFT = of("block/rocket_satellite_fairing/half_left");
    /** The fairing's east half (x 8..13): the mirror of {@link #FAIRING_HALF_LEFT}, peeled with {@code rotateZDegrees(-angle)} about (13, 1.5, 8). */
    public static final PartialModel FAIRING_HALF_RIGHT = of("block/rocket_satellite_fairing/half_right");
    /**
     * An early-warning satellite: a gold bus with the infrared telescope under it looking down (-y, at the Earth) and solar wings
     * along x. Centred on (8, 8, 8) ({@link #SATELLITE_CENTRE}): 13 pixels across the wings, 10 from the sunshade (y 3) to the
     * mast (y 13). Scale and turn it about its centre.
     */
    public static final PartialModel SATELLITE_EARLY_WARNING = of("block/satellite/early_warning");
    /**
     * A communications satellite: a box bus, solar wings along x, a dish on its north and south faces tipped down to the Earth
     * (-y), an omni antenna on top. Centred on (8, 8, 8) like {@link #SATELLITE_EARLY_WARNING}: 14 pixels across the wings,
     * y 5 to 13.
     */
    public static final PartialModel SATELLITE_COMMS = of("block/satellite/comms");

    /** The mount's vertical axis: (8, 0, 8) pixels. */
    public static final float[] STATION_AXIS = {0.5f, 0, 0.5f};
    /** The dish's elevation axle, along x: (8, 16, 8) pixels. */
    public static final float[] DISH_PIVOT = {0.5f, 1, 0.5f};
    /** The fairing halves' hinges, along z: their outer bottom edges, (3, 1.5, 8) and (13, 1.5, 8) pixels. */
    public static final float[] FAIRING_LEFT_HINGE = {3 / 16f, 1.5f / 16, 0.5f}, FAIRING_RIGHT_HINGE = {13 / 16f, 1.5f / 16, 0.5f};
    /** The satellites' centre: (8, 8, 8) pixels. */
    public static final float[] SATELLITE_CENTRE = {0.5f, 0.5f, 0.5f};

    private OrbitPartials() {
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    public static void init() {
    }
}
