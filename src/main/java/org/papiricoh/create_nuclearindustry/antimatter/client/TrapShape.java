package org.papiricoh.create_nuclearindustry.antimatter.client;

import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;

/**
 * Where things are in each trap's model (tools/modelgen/generate.py), in pixels from the master
 * block's corner before any facing rotation: the middle of the cloud and how far it may grow inside
 * the glass, the two glowing field bands and the coils the quench sparks leap from.
 *
 * @param cx         the trap's vertical axis, x
 * @param cz         the trap's vertical axis, z
 * @param cloudY     the cloud's middle
 * @param maxRadius  cloud radius when full, a pixel inside the glass
 * @param bandHalf   half-width of the square field bands
 * @param bandLow    the lower head's band, {bottom, top}
 * @param bandHigh   the upper head's band, {bottom, top}
 * @param glassLow   bottom of the glass
 * @param glassHigh  top of the glass
 */
record TrapShape(float cx, float cz, float cloudY, float maxRadius, float bandHalf, float[] bandLow, float[] bandHigh,
                 float glassLow, float glassHigh) {

    static final TrapShape SMALL = new TrapShape(8, 8, 8, 4.0f, 6.5f, new float[]{2.5f, 3.25f}, new float[]{12.75f, 13.5f}, 4, 12);
    static final TrapShape MEDIUM = new TrapShape(8, 8, 16, 3.4f, 6.0f, new float[]{4.5f, 5.25f}, new float[]{26.75f, 27.5f}, 6, 26);
    static final TrapShape LARGE = new TrapShape(16, 16, 24, 9.0f, 13.5f, new float[]{10, 11}, new float[]{37, 38}, 12, 36);

    static TrapShape of(TrapTier tier) {
        return switch (tier) {
            case SMALL -> SMALL;
            case MEDIUM -> MEDIUM;
            case LARGE -> LARGE;
        };
    }
}
