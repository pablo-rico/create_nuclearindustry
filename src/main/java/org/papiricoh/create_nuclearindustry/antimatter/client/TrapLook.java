package org.papiricoh.create_nuclearindustry.antimatter.client;

import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;

/**
 * How a trap's antimatter cloud looks at one instant: its radius as a share of the largest, its
 * colour, its glow and how much it shakes. Grows with the cube root of the charge (the cloud's
 * volume follows the charge), beats with the field (faster and deeper the stronger it is), stutters
 * when the trap runs short of energy and, while quenching, turns red, swells and trembles as the
 * countdown runs out. Pure arithmetic on the synced state and the render clock.
 *
 * @param radius    cloud radius as a share of the trap's largest, 0..~1.15 (0: nothing to draw)
 * @param glow      overall brightness, 0..1
 * @param red       cloud colour (the tier colour, or red in a quench), 0..1 per channel
 * @param green     see {@code red}
 * @param blue      see {@code red}
 * @param shake     jitter of the cloud's middle as a share of the radius
 * @param spin      rotation of the wisps, radians
 * @param bob       vertical float of the cloud's middle, pixels
 */
public record TrapLook(float radius, float glow, float red, float green, float blue, float shake, float spin, float bob) {
    /** A quenching cloud's colour. */
    public static final int QUENCH_RED = 0xFF3A2A;

    public static TrapLook of(TrapState state, double fill, float field, float quenchProgress, int tierColour, double time) {
        if (!(fill > 1.0e-6)) {
            return new TrapLook(0, 0, 0, 0, 0, 0, 0, 0);
        }
        float size = (float) Math.cbrt(Math.min(1.0, fill));
        // A visible wisp even for a trace of antimatter.
        size = Math.max(size, 0.12f);
        float f = Math.max(0, Math.min(1, field));
        float beatRate = 0.12f + 0.22f * f;
        float beat = (float) Math.sin(time * beatRate);
        float radius = size * (1.0f + (0.03f + 0.05f * f) * beat);
        float glow = (0.45f + 0.55f * f) * (0.88f + 0.12f * beat);
        float shake = 0;
        int colour = tierColour;
        float mix = 0;
        if (state == TrapState.LOW_POWER) {
            // The field stutters: brief dips, and a faint continuous shimmer.
            float flicker = noise((long) Math.floor(time * 1.5)) < 0.3f ? 0.35f : 1.0f;
            glow *= flicker * (0.85f + 0.15f * (float) Math.sin(time * 2.3));
            radius *= 1.0f + 0.04f * (1 - flicker);
        } else if (state == TrapState.QUENCHING) {
            float q = Math.max(0, Math.min(1, quenchProgress));
            mix = 0.55f + 0.45f * q;
            float flash = (float) Math.sin(time * (0.8 + 1.6 * q));
            glow = Math.min(1, 0.7f + 0.3f * flash * flash + 0.2f * q);
            radius *= 1.0f + 0.15f * q + 0.06f * noise((long) Math.floor(time * 3) + 7);
            shake = 0.06f + 0.18f * q;
        }
        float r = channel(colour, 16), g = channel(colour, 8), b = channel(colour, 0);
        float qr = channel(QUENCH_RED, 16), qg = channel(QUENCH_RED, 8), qb = channel(QUENCH_RED, 0);
        r += (qr - r) * mix;
        g += (qg - g) * mix;
        b += (qb - b) * mix;
        float spin = (float) (time * (0.02 + 0.05 * f + (state == TrapState.QUENCHING ? 0.12 : 0)));
        float bob = (float) Math.sin(time * 0.05) * (0.15f + 0.45f * (1 - size));
        return new TrapLook(radius, Math.max(0, Math.min(1, glow)), r, g, b, shake, spin, bob);
    }

    private static float channel(int rgb, int shift) {
        return ((rgb >> shift) & 0xFF) / 255f;
    }

    /** A repeatable random number in 0..1 for a step of the clock. */
    static float noise(long step) {
        long h = step * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1L << 24);
    }
}
