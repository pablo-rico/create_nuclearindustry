package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette.Rgb;

/**
 * How the sun, the sky and the cloud's own heat light a puff of a mushroom cloud. The side facing the sun is bright and, with
 * the sun low, warm; the far side is lit only by the sky and darker the deeper inside the cloud a puff sits. At night the sky
 * gives almost nothing: what shows is the red glow of the still-hot gas deep inside a young cloud and, from below, the light of
 * the fires on the ground on the cloud's underside. The sun's direction is the one the game (or a shader pack, with its tilted
 * path) draws, so the bright side faces the sun the player sees. No game classes.
 */
public final class CloudLighting {
    private CloudLighting() {}

    /**
     * The light of one frame.
     *
     * @param sunX      unit direction to the sun
     * @param daylight  0 at night to 1 at noon: how much the sun and the sky light
     * @param sun       the sunlight's colour and strength
     * @param ambient   the skylight's
     * @param fireBelow light of the fires on the ground under the cloud, 0-1
     * @param night     how dark it is, 0 by day to 1
     */
    public record Sky(double sunX, double sunY, double sunZ, double daylight, Rgb sun, Rgb ambient, double fireBelow, double night) {}

    /** Warm sunlight low on the horizon, white high up. */
    private static final Rgb LOW_SUN = new Rgb(1.0f, 0.62f, 0.38f);
    private static final Rgb HIGH_SUN = new Rgb(1.0f, 0.97f, 0.92f);
    /** The sky's light by day, bluish, and by night, a dim moonlit blue. */
    private static final Rgb DAY_SKY = new Rgb(0.42f, 0.46f, 0.55f);
    private static final Rgb NIGHT_SKY = new Rgb(0.05f, 0.055f, 0.08f);
    /** The light of burning ground. */
    private static final Rgb FIRE_LIGHT = new Rgb(1.0f, 0.42f, 0.15f);

    /**
     * The light of a frame with the sun towards {@code sun} (unit), {@code daylight} 0 to 1 and fires on the ground lighting the
     * cloud's underside by {@code fireBelow} (0-1).
     */
    public static Sky sky(double sunX, double sunY, double sunZ, double daylight, double fireBelow) {
        double day = clamp(daylight);
        double up = CloudEvolution.smooth((sunY + 0.05) / 0.25);
        Rgb sun = LOW_SUN.lerp(HIGH_SUN, CloudEvolution.smooth(sunY / 0.35)).scale(day * up);
        Rgb ambient = NIGHT_SKY.lerp(DAY_SKY, day);
        return new Sky(sunX, sunY, sunZ, day, sun, ambient, clamp(fireBelow), 1 - day);
    }

    /**
     * The colour a puff of {@code albedo} is seen in, into {@code out} (r, g, b, 0-1): {@code (nx, ny, nz)} the cloud's outward
     * direction there, {@code occlusion} how deep inside it sits (0-1) and {@code heat} how hot its gas still is (0-1).
     */
    public static void light(Sky sky, float red, float green, float blue, float nx, float ny, float nz, float occlusion, float heat,
                             float[] out) {
        double dot = nx * sky.sunX() + ny * sky.sunY() + nz * sky.sunZ();
        double direct = Math.max(0, dot);
        double wrap = 0.5 + 0.5 * dot;
        double shadow = 1 - 0.55 * occlusion;
        double diffuse = (0.15 * wrap + 0.85 * direct) * shadow;
        double sky1 = (0.55 + 0.25 * ny) * (1 - 0.4 * occlusion);
        double below = sky.fireBelow() * Math.max(0, -ny) * sky.night() * 0.6;
        double r = red * (sky.sun().red() * diffuse + sky.ambient().red() * sky1 + FIRE_LIGHT.red() * below);
        double g = green * (sky.sun().green() * diffuse + sky.ambient().green() * sky1 + FIRE_LIGHT.green() * below);
        double b = blue * (sky.sun().blue() * diffuse + sky.ambient().blue() * sky1 + FIRE_LIGHT.blue() * below);
        if (heat > 0.01) {
            // The still-hot gas inside glows; by day the sunlit cloud drowns most of it.
            double glow = heat * (0.3 + 0.7 * sky.night()) * (0.4 + 0.6 * occlusion);
            Rgb ember = ExplosionPalette.fire(0.12 + 0.3 * heat);
            r += ember.red() * glow;
            g += ember.green() * glow;
            b += ember.blue() * glow;
        }
        out[0] = (float) clamp(r);
        out[1] = (float) clamp(g);
        out[2] = (float) clamp(b);
    }

    /**
     * How bright the side of a whole cloud facing {@code (dx, dz)} (horizontal, unit) is against its far side, 0-1, for the far
     * impostor's left and right edges: the sun's side lit, the other in shadow.
     */
    public static double side(Sky sky, double dx, double dz) {
        double flat = Math.sqrt(sky.sunX() * sky.sunX() + sky.sunZ() * sky.sunZ());
        if (flat < 1e-6) return 0.5;
        return 0.5 + 0.5 * (dx * sky.sunX() + dz * sky.sunZ()) / flat * Math.min(1, flat * 2);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
