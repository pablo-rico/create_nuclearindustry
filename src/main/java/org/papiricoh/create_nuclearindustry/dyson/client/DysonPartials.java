package org.papiricoh.create_nuclearindustry.dyson.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * The moving parts of the swarm's machines, drawn by their renderers. Must be created before models load (mod constructor).
 *
 * <p>Coordinates are block pixels (16 a block) from the lowest corner of the block model they belong with; the {@code float[]}
 * pivots are the same points in blocks, ready for {@code SuperByteBuffer.translate}. Both machines aim like the ground station:
 * turn the mount by the azimuth (degrees clockwise from north) with {@code rotateYDegrees(-azimuth)} about the vertical through
 * x = z = 8, and raise the upper part by the elevation with {@code rotateXDegrees(elevation)} about its pivot (90 looks straight
 * up), then turn it with the mount.
 */
public final class DysonPartials {
    /** The ejector's turntable and the yoke carrying the rails, over its plinth (the block model, 12 pixels high). */
    public static final PartialModel EJECTOR_TURNTABLE = of("block/em_rail_ejector/turntable");
    /**
     * The ejector's launch rails: two rails with the breech between them, modelled level and pointing north (-z), their elevation
     * axle along x through {@link #EJECTOR_PIVOT}. They run from {@link #EJECTOR_RAIL_BACK} pixels behind the pivot to
     * {@link #EJECTOR_RAIL_FRONT} in front of it, so they overhang the block.
     */
    public static final PartialModel EJECTOR_RAILS = of("block/em_rail_ejector/rails");
    /** The coil rings along the rails, placed as the rails are; pale, to be drawn lit and tinted as they charge. */
    public static final PartialModel EJECTOR_COILS = of("block/em_rail_ejector/coils");
    /** The sled at the rails' breech holding a folded sail, placed as the rails are; slid forward (-z) along them on a launch. */
    public static final PartialModel EJECTOR_SLED = of("block/em_rail_ejector/sled");

    /** The receiver's turntable and fork, over its plinth (the block model, 10 pixels high). */
    public static final PartialModel RECEIVER_MOUNT = of("block/ray_receiver/mount");
    /**
     * The receiver's gold dish, modelled looking north (-z) and level, its elevation axle along x through {@link #RECEIVER_PIVOT}.
     * Its rim is {@link #RECEIVER_DISH_RADIUS} pixels round its axis of sight.
     */
    public static final PartialModel RECEIVER_DISH = of("block/ray_receiver/dish");
    /** The collector at the dish's focus, {@link #RECEIVER_FOCUS} pixels in front of the pivot, placed as the dish is; pale, drawn lit. */
    public static final PartialModel RECEIVER_FOCUS_GLOW = of("block/ray_receiver/focus");

    /** The ejector's elevation axle: (8, 15, 8) pixels. */
    public static final float[] EJECTOR_PIVOT = {0.5f, 15 / 16f, 0.5f};
    /** How far the rails reach behind and in front of the pivot, pixels. */
    public static final float EJECTOR_RAIL_BACK = 6, EJECTOR_RAIL_FRONT = 22;
    /** The receiver's elevation axle: (8, 14, 8) pixels. */
    public static final float[] RECEIVER_PIVOT = {0.5f, 14 / 16f, 0.5f};
    /** The dish's rim radius, and how far its focus stands in front of the pivot, pixels. */
    public static final float RECEIVER_DISH_RADIUS = 11, RECEIVER_FOCUS = 9;
    /** The mounts' vertical axis: (8, 0, 8) pixels. */
    public static final float[] AXIS = {0.5f, 0, 0.5f};

    private DysonPartials() {
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    /** Loads the class, creating the partials. */
    public static void init() {
    }
}
