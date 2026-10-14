package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * The colours and timings a detonation's explosion is drawn with, by {@link DetonationStyle}. A fission
 * warhead keeps the look it always had: a fireball going white, yellow, orange in four seconds and a cloud
 * cooling to grey-brown. A thermonuclear one burns bigger, whiter and longer, its fireball deepening to
 * a saturated orange-red, and the cloud it raises is sooty, near black, with a darker ground surge, a
 * harder flash and a redder sky. No game classes.
 *
 * @param fireballSeconds   how long the fireball shows before the cap swallows it
 * @param fireballScale     its size against the profile's fireball radius
 * @param fireGreenEnd      the fireball's green at its reddest (its red stays full)
 * @param fireBlueStart     its blue when it is born
 * @param fireBlueEnd       its blue at its reddest
 * @param coreBlueStart     the blinding core's blue when it is born
 * @param coreBlueEnd       and once it has yellowed
 * @param heatTau           seconds the cloud's glow takes to fall to a third
 * @param hotSmoke          the cloud while it is hot
 * @param coolSmoke         the cloud once it has cooled
 * @param dust              the stem's skirt of ground dust
 * @param smokeAlpha        how opaque the cloud is against the fission one
 * @param glowThreshold     heat above which a puff glows on its own, whatever the daylight
 * @param topHeightCap      the cap never rises above this
 * @param capRadiusCap      nor spreads wider than this
 * @param flash             the flash's colour
 * @param flashFade         seconds the flash takes to fade to a third
 * @param glow              the lingering glow's colour over the screen
 * @param glowAlpha         and its strength
 * @param glowFade          seconds it takes to fade to a third
 * @param fog               the colour the fog is drawn towards
 * @param fogMax            and how far, at most
 * @param shock             the shock wave's ring of dust
 * @param condensationAlpha how thick the condensation dome is
 * @param condensationDomes how many nested domes form
 */
public record ExplosionPalette(double fireballSeconds, double fireballScale, float fireGreenEnd, float fireBlueStart, float fireBlueEnd,
                               float coreBlueStart, float coreBlueEnd, double heatTau, Rgb hotSmoke, Rgb coolSmoke, Rgb dust,
                               double smokeAlpha, double glowThreshold, double topHeightCap, double capRadiusCap,
                               Rgb flash, double flashFade, Rgb glow, double glowAlpha, double glowFade, Rgb fog, double fogMax,
                               Rgb shock, double condensationAlpha, int condensationDomes) {

    /** A colour, each channel 0-1. */
    public record Rgb(float red, float green, float blue) {
        /** Relative luminance, Rec. 709 weights on the channels as they are. */
        public double luminance() {
            return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        }

        public Rgb lerp(Rgb to, double t) {
            return new Rgb((float) (red + (to.red - red) * t), (float) (green + (to.green - green) * t), (float) (blue + (to.blue - blue) * t));
        }
    }

    public static final ExplosionPalette FISSION = new ExplosionPalette(4, 1.0, 0.55f, 0.95f, 0.15f, 0.9f, 0.4f, 6,
            new Rgb(1.0f, 0.5f, 0.18f), new Rgb(0.52f, 0.47f, 0.43f), new Rgb(0.62f, 0.56f, 0.48f), 1.0, 0.35, 360,
            Double.POSITIVE_INFINITY, new Rgb(1.0f, 0.98f, 0.92f), 0.45, new Rgb(1.0f, 0.6f, 0.3f), 0.14, 25,
            new Rgb(1.0f, 0.55f, 0.25f), 0.5, new Rgb(0.78f, 0.72f, 0.62f), 0.18, 1);

    public static final ExplosionPalette THERMONUCLEAR = new ExplosionPalette(7, 1.35, 0.40f, 1.0f, 0.05f, 1.0f, 0.75f, 9,
            new Rgb(1.0f, 0.33f, 0.07f), new Rgb(0.15f, 0.13f, 0.12f), new Rgb(0.30f, 0.27f, 0.24f), 1.1, 0.30, 560, 320,
            new Rgb(1.0f, 1.0f, 1.0f), 0.8, new Rgb(1.0f, 0.42f, 0.15f), 0.22, 40,
            new Rgb(0.85f, 0.30f, 0.12f), 0.6, new Rgb(0.45f, 0.40f, 0.36f), 0.28, 2);

    /** The palette a {@code style} of detonation is drawn with; a dirty bomb's small blast keeps fission's. */
    public static ExplosionPalette of(DetonationStyle style) {
        return style == DetonationStyle.THERMONUCLEAR ? THERMONUCLEAR : FISSION;
    }
}
