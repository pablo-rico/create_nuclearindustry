package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * The colours and timings a detonation's explosion is drawn with, by {@link DetonationStyle}. Every explosion is drawn from the
 * same physics: a fireball is a black body cooling from white through yellow and orange to a dull red ({@link #fire}), smoke is
 * the soot and dust it lifts, condensation is white, and the flash and its afterglow are the colours of that fire. A
 * thermonuclear warhead differs from a fission one only in what physics says it should: a bigger fireball that stays hot
 * longer and a harder, longer flash. A dirty bomb's blast is plain high explosive and looks like it. There are no colours for
 * a kind of weapon.
 * <p>
 * Annihilating antimatter is the one exception, drawn by the antimatter feature's own look: a white-green flash, a shell of
 * green and cyan sparks and a green afterglow, gone in two seconds, with no smoke. No game classes.
 *
 * @param fireballScale     the fireball's size against the profile's fireball radius
 * @param fireballSeconds   seconds the fireball glows for every block of its radius, on top of {@link #FIREBALL_BASE_SECONDS}
 * @param flash             the flash's colour
 * @param flashFade         seconds the flash takes to fade to a third
 * @param glow              the lingering glow's colour over the screen
 * @param glowAlpha         and its strength
 * @param glowFade          seconds it takes to fade to a third
 * @param fog               the colour the fog and sky are drawn towards while the fireball glows
 * @param fogMax            and how far, at most
 * @param smoke             the cloud's soot and dust once cool, before the light
 * @param dust              the ground dust of the stem, the surge and the shock front, before the terrain tints it
 * @param condensation      the condensation cloud, rings and pileus
 * @param condensationAlpha how thick the condensation is
 */
public record ExplosionPalette(double fireballScale, double fireballSeconds, Rgb flash, double flashFade, Rgb glow, double glowAlpha,
                               double glowFade, Rgb fog, double fogMax, Rgb smoke, Rgb dust, Rgb condensation,
                               double condensationAlpha) {

    /** Seconds any fireball glows before its size adds to it. */
    public static final double FIREBALL_BASE_SECONDS = 1.6;

    /** A colour, each channel 0-1. */
    public record Rgb(float red, float green, float blue) {
        /** Relative luminance, Rec. 709 weights on the channels as they are. */
        public double luminance() {
            return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        }

        public Rgb lerp(Rgb to, double t) {
            return new Rgb((float) (red + (to.red - red) * t), (float) (green + (to.green - green) * t), (float) (blue + (to.blue - blue) * t));
        }

        public Rgb scale(double k) {
            return new Rgb((float) (red * k), (float) (green * k), (float) (blue * k));
        }

        /** Packed as 0xRRGGBB. */
        public int rgb() {
            return Math.round(clamp(red) * 255) << 16 | Math.round(clamp(green) * 255) << 8 | Math.round(clamp(blue) * 255);
        }

        private static float clamp(float v) {
            return Math.max(0, Math.min(1, v));
        }
    }

    /** The light of the flash: a hot, nearly white body. */
    public static final Rgb FLASH = new Rgb(1.0f, 0.98f, 0.93f);
    /** The afterglow of a fireball cooling over the screen and the sky: its orange. */
    public static final Rgb AFTERGLOW = new Rgb(1.0f, 0.58f, 0.28f);
    /** The fog and sky in that afterglow. */
    public static final Rgb FOG_GLOW = new Rgb(1.0f, 0.6f, 0.32f);
    /** Cool smoke and dust high in the cloud: a light, warm grey before the sun lights it. */
    public static final Rgb SMOKE = new Rgb(0.74f, 0.71f, 0.68f);
    /** Ground dust: a dull brown-grey, tinted further by the terrain it rose from. */
    public static final Rgb DUST = new Rgb(0.58f, 0.53f, 0.47f);
    /** Condensed water: white. */
    public static final Rgb CONDENSATION = new Rgb(0.96f, 0.96f, 0.97f);
    /** Soot of burning ground: the dark grey of fire smoke. */
    public static final Rgb SOOT = new Rgb(0.2f, 0.19f, 0.18f);

    public static final ExplosionPalette FISSION = new ExplosionPalette(1.0, 0.04, FLASH, 0.45, AFTERGLOW, 0.14, 25, FOG_GLOW, 0.5,
            SMOKE, DUST, CONDENSATION, 0.55);

    /** The same physics, bigger and hotter: its fireball is larger, stays hot longer and its flash lasts. */
    public static final ExplosionPalette THERMONUCLEAR = new ExplosionPalette(1.15, 0.05, FLASH, 0.8, AFTERGLOW, 0.18, 40, FOG_GLOW, 0.6,
            SMOKE, DUST, CONDENSATION, 0.6);

    /** Antiprotons meeting matter, drawn by the antimatter feature's look: white-green light, no smoke. */
    public static final ExplosionPalette ANTIMATTER = new ExplosionPalette(0.6, 0.0, new Rgb(0.92f, 1.0f, 0.94f), 0.25,
            new Rgb(0.45f, 1.0f, 0.65f), 0.12, 0.6, new Rgb(0.5f, 1.0f, 0.75f), 0.15, new Rgb(0.55f, 0.95f, 0.85f),
            new Rgb(0.6f, 1.0f, 0.9f), new Rgb(0.6f, 1.0f, 0.9f), 0.0);

    /** The palette a {@code style} of detonation is drawn with; a dirty bomb's blast is plain high explosive and keeps fission's. */
    public static ExplosionPalette of(DetonationStyle style) {
        return switch (style) {
            case THERMONUCLEAR -> THERMONUCLEAR;
            case ANTIMATTER -> ANTIMATTER;
            default -> FISSION;
        };
    }

    /** Seconds a fireball of {@code radius} blocks glows before it has cooled into the cloud. */
    public double fireballSeconds(double radius) {
        return FIREBALL_BASE_SECONDS + fireballSeconds * Math.max(0, radius);
    }

    /**
     * The colour of hot gas at {@code heat} (1 just formed, 0 cold), a black body cooling: blue-white, white, yellow, orange,
     * a deep red and finally the dark red of embers. Every fireball and flame is coloured by this, nuclear or not.
     */
    public static Rgb fire(double heat) {
        double h = Math.max(0, Math.min(1, heat));
        if (h > 0.85) {
            return lerp(1.0f, 0.97f, 0.88f, 1.0f, 1.0f, 1.0f, (h - 0.85) / 0.15);
        }
        if (h > 0.6) {
            return lerp(1.0f, 0.82f, 0.45f, 1.0f, 0.97f, 0.88f, (h - 0.6) / 0.25);
        }
        if (h > 0.35) {
            return lerp(1.0f, 0.52f, 0.16f, 1.0f, 0.82f, 0.45f, (h - 0.35) / 0.25);
        }
        if (h > 0.12) {
            return lerp(0.78f, 0.24f, 0.07f, 1.0f, 0.52f, 0.16f, (h - 0.12) / 0.23);
        }
        return lerp(0.25f, 0.06f, 0.03f, 0.78f, 0.24f, 0.07f, h / 0.12);
    }

    private static Rgb lerp(float r0, float g0, float b0, float r1, float g1, float b1, double t) {
        return new Rgb((float) (r0 + (r1 - r0) * t), (float) (g0 + (g1 - g0) * t), (float) (b0 + (b1 - b0) * t));
    }
}
