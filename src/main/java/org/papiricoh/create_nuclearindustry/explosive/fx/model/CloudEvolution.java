package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.explosive.blast.BlastHash;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette.Rgb;

/**
 * How a nuclear fireball grows into a mushroom cloud and fades, at any moment after the detonation, as a set of soft puffs
 * handed to a {@link PuffSink} (no garbage: the renderer turns them straight into vertices). The phases follow the photographs
 * of real tests, with the heights squeezed to fit a Minecraft sky:
 * <ol>
 *     <li>the fireball, white-hot at first and cooling through yellow and orange ({@link ExplosionPalette#fire}), its surface
 *     turbulent;</li>
 *     <li>a condensation (Wilson) cloud flashing round it as the shock's rarefaction passes through damp air, gone in seconds;</li>
 *     <li>the fireball rising and rolling into a toroidal cap whose gas visibly turns over: up through the middle, out over the
 *     top and down the outside; a pileus hood of condensation over it while it punches upwards, and condensation rings round the
 *     stem where it passes through damp layers;</li>
 *     <li>the stem of dust drawn up from the ground behind it, flared where it meets the cap; along the ground a base surge
 *     rolling outwards;</li>
 *     <li>the cloud stopping at its height: a fission cloud stands 250 to 400 blocks tall, while a thermonuclear one would rise
 *     far higher and instead flattens against the {@link #TROPOPAUSE}, spreading into a wide anvil;</li>
 *     <li>late, the cloud darkens as its condensation evaporates, drifts downwind (the higher the faster, so the stem leans),
 *     spreads, thins and is gone after five to six minutes; fires lit by the flash send up columns of soot that merge into a
 *     pyrocumulus.</li>
 * </ol>
 * Every puff comes from the seed, so every player sees the same cloud, and from the time alone, so a player told of it late sees
 * it at its true age. Positions are blocks from ground zero at ground level. No game classes.
 */
public final class CloudEvolution {
    /** Where a thermonuclear cloud stops rising and spreads: blocks above the ground. */
    public static final double TROPOPAUSE = 420;
    /** Blocks a second the high wind carries the cloud's top. */
    public static final double WIND_SPEED = 0.28;
    /** Lightning bolts a young cloud may throw. */
    public static final int BOLTS = 20;
    /** Seconds a bolt flickers. */
    public static final double BOLT_SECONDS = 0.2;

    /** Puff flags: drawn upright, never spun (the pileus hood and lightning keep their way up). */
    public static final int UPRIGHT = 1;
    /** Puff flags: light of its own (fire, lightning), its colour already the light given off; not lit by the sun. */
    public static final int EMISSIVE = 2;

    /** Atlas tiles ({@code textures/fx/nuke_cloud.png}, row * 4 + column). */
    public static final int TILE_SOFT = 0, TILE_DENSE = 4, TILE_FIRE = 8, TILE_VEIL = 12, TILE_PILEUS = 13, TILE_RING = 14,
            TILE_LIGHTNING = 15;

    /** Where puffs go: one call each, already coloured (albedo before light, or light for emissive ones). */
    public interface PuffSink {
        /**
         * @param x         blocks east of ground zero
         * @param y         blocks above the ground there
         * @param z         blocks south of it
         * @param radius    the puff's half width
         * @param tile      atlas tile
         * @param spin      radians the sprite is turned by
         * @param alpha     opacity, 0-1
         * @param heat      for smoke, how hot it still is (glows red at night), 0-1
         * @param nx        the cloud's outward direction there, for light
         * @param occlusion how deep inside the cloud it sits, 0 on the surface to 1 deep in
         * @param flags     {@link #UPRIGHT}, {@link #EMISSIVE}
         */
        void puff(double x, double y, double z, double radius, int tile, float spin, float red, float green, float blue, float alpha,
                  float heat, float nx, float ny, float nz, float occlusion, int flags);
    }

    /** How many puffs each part is drawn with this time. */
    public record Counts(int cap, int stem, int surge, int extra) {
        public static Counts of(QualityPreset preset, double detail, double lod) {
            return new Counts(QualityPreset.scaled(preset.capPuffs(), detail, lod), QualityPreset.scaled(preset.stemPuffs(), detail, lod),
                    QualityPreset.scaled(preset.surgePuffs(), detail, lod), QualityPreset.scaled(preset.extraPuffs(), detail, lod));
        }

        public int total() {
            return cap + stem + surge + extra;
        }
    }

    private final int seedHash;
    private final ExplosionPalette palette;
    private final double scale;
    private final double burstHeight;
    private final boolean airburst;
    private final double waveSpeed;
    private final double waveRadius;
    private final double thermalRadius;
    private final double windX, windZ;
    private final Rgb groundDust;

    private final double fireball;
    private final double fireSeconds;
    private final double freeTop;
    private final boolean flattened;
    private final double top;
    private final double capRadius;
    private final double thickRatio;
    private final double centreFinal;
    private final double riseTau;
    private final double spreadTau;
    private final double lifetime;
    private final double stemRadius;
    private final double surgeRadius;
    private final boolean highAirburst;

    /**
     * @param seed          picks the puffs
     * @param palette       the style's physics (fireball size and how long it glows)
     * @param scale         the warhead's destruction radius, which sizes the cloud
     * @param fireball      the fireball's radius
     * @param burstHeight   blocks above the ground it went off at
     * @param airburst      whether it went off above its fireball
     * @param waveSpeed     blocks per tick the shock front travels
     * @param waveRadius    how far it travels
     * @param thermalRadius how far the flash sets fires
     * @param windAngle     radians the wind blows towards (0 along +x, pi/2 along +z)
     * @param ground        the colour of the ground at ground zero, which tints the dust it raises
     */
    public CloudEvolution(long seed, ExplosionPalette palette, double scale, double fireball, double burstHeight, boolean airburst,
                          double waveSpeed, double waveRadius, double thermalRadius, double windAngle, Rgb ground) {
        this.seedHash = (int) seed ^ (int) (seed >>> 32);
        this.palette = palette;
        this.scale = Math.max(1, scale);
        this.burstHeight = Math.max(0, burstHeight);
        this.airburst = airburst;
        this.waveSpeed = Math.max(0.1, waveSpeed);
        this.waveRadius = Math.max(0, waveRadius);
        this.thermalRadius = Math.max(0, thermalRadius);
        this.windX = Math.cos(windAngle);
        this.windZ = Math.sin(windAngle);
        this.groundDust = palette.dust().lerp(ground == null ? palette.dust() : ground, 0.45);

        this.fireball = Math.max(3, fireball * palette.fireballScale());
        this.fireSeconds = palette.fireballSeconds(this.fireball);
        this.freeTop = Math.max(120, 160 + 0.95 * this.scale);
        this.flattened = freeTop > TROPOPAUSE;
        this.top = flattened ? TROPOPAUSE + Math.min(80, 0.15 * (freeTop - TROPOPAUSE)) : freeTop;
        this.capRadius = flattened ? Math.min(TROPOPAUSE, 0.34 * TROPOPAUSE + 0.5 * (freeTop - TROPOPAUSE)) : 0.34 * freeTop;
        this.thickRatio = flattened ? 0.28 : 0.45;
        // The cap's puffs bulge past its half-thickness by about a fifth of its radius: its visible top stands at the top.
        this.centreFinal = top - (thickRatio + 0.18) * capRadius;
        this.riseTau = flattened ? 26 : 20;
        this.spreadTau = flattened ? 45 : 30;
        this.lifetime = 300 + 60 * Math.min(1, this.scale / 750);
        this.stemRadius = 0.12 * capRadius + 4;
        this.surgeRadius = Math.min(0.9 * this.scale, 6 * this.fireball);
        this.highAirburst = airburst && this.burstHeight > 2.5 * this.fireball;
    }

    // ------------------------------------------------------------------ the shape over time

    /** Seconds the cloud lasts. */
    public double lifetime() {
        return lifetime;
    }

    /** The fireball's largest radius. */
    public double fireballRadius() {
        return fireball;
    }

    /** Seconds the fireball glows before it has cooled into the cap. */
    public double fireballSeconds() {
        return fireSeconds;
    }

    /** How high the top of the cloud ends up. */
    public double topHeight() {
        return top;
    }

    /** How high it would rise with nothing to stop it; above {@link #TROPOPAUSE} it flattens instead. */
    public double freeTop() {
        return freeTop;
    }

    public boolean flattened() {
        return flattened;
    }

    /** The cap's radius at the end of its rise (it keeps spreading slowly after). */
    public double capRadius() {
        return capRadius;
    }

    /** The fireball's radius {@code t} seconds in. */
    public double fireballRadius(double t) {
        double grow = Math.pow(1 - Math.exp(-Math.max(0, t) / 0.15), 0.4);
        return fireball * grow * (1 + 0.15 * Math.min(1, t / fireSeconds));
    }

    /** How hot the fireball still is, 1 just formed to 0 cold: white at 1, red near 0.2 ({@link ExplosionPalette#fire}). */
    public double fireballHeat(double t) {
        return Math.exp(-1.6 * Math.max(0, t) / fireSeconds);
    }

    /** How hot the cloud's gas still is on the whole, 1 to 0. */
    public double heat(double t) {
        return Math.exp(-Math.max(0, t) / (2.2 * fireSeconds));
    }

    /** The height of the cap's (first the fireball's) centre. */
    public double capCentre(double t) {
        double start = burstHeight + (airburst ? 0 : 0.6 * fireball);
        double x = Math.max(0, t) / riseTau;
        double rise = 1 - (1 + x) * Math.exp(-x);
        return start + (Math.max(start, centreFinal) - start) * rise;
    }

    /** The cap's radius. */
    public double capRadius(double t) {
        double r = Math.max(fireball * 1.05, capRadius * (1 - Math.exp(-Math.max(0, t) / spreadTau)));
        if (flattened) {
            r *= 1 + 0.2 * clamp((t - 60) / 240);
        }
        return r;
    }

    /** Half the cap's height. */
    public double capHalfThickness(double t) {
        double ratio = 1 + (thickRatio - 1) * (1 - Math.exp(-Math.max(0, t) / 20));
        return ratio * capRadius(t);
    }

    /** The top of the cloud. */
    public double capTop(double t) {
        return capCentre(t) + capHalfThickness(t);
    }

    /** How opaque the whole cloud still is: 1, fading over its last two minutes to 0 at its end. */
    public double life(double t) {
        double x = clamp((t - (lifetime - 120)) / 120);
        return 1 - x * x * (3 - 2 * x);
    }

    /** How far the wind has carried what is {@code y} blocks up, {@code t} seconds in: the top fastest. */
    public double drift(double y, double t) {
        return WIND_SPEED * Math.max(0, t) * (0.15 + 0.85 * clamp(y / top));
    }

    /** Where the shock front is, blocks from ground zero. */
    public double shockRadius(double t) {
        return Math.min(waveRadius, waveSpeed * 20 * Math.max(0, t));
    }

    // ------------------------------------------------------------------ colours

    /**
     * The colour of the cap's smoke {@code t} seconds in, before the light: reddish-brown while it is hot and full of nitrogen
     * oxides, whitening as water condenses in it, then greying and darkening as that evaporates and dust and soot are left.
     */
    public Rgb capColour(double t) {
        Rgb young = groundDust.lerp(new Rgb(0.45f, 0.3f, 0.22f), 0.5);
        Rgb white = palette.smoke().lerp(palette.condensation(), 0.6);
        Rgb grey = palette.smoke().scale(0.9);
        Rgb old = palette.smoke().scale(0.74);
        if (t < 8) return young;
        if (t < 40) return young.lerp(white, smooth((t - 8) / 32));
        if (t < 120) return white.lerp(grey, smooth((t - 40) / 80));
        return grey.lerp(old, smooth((t - 120) / Math.max(1, lifetime - 120)));
    }

    /** The stem's dust: the ground's colour, darkening as it ages. */
    public Rgb stemColour(double t) {
        return groundDust.scale(1 - 0.18 * clamp(t / lifetime));
    }

    // ------------------------------------------------------------------ the puffs

    /** Hands {@code sink} every puff of the cloud {@code t} seconds in; nothing once it is over. */
    public void emit(double t, Counts counts, PuffSink sink) {
        if (t < 0 || t > lifetime) {
            return;
        }
        double life = life(t);
        double grown = 1 + 0.6 * clamp((t - (lifetime - 150)) / 150);
        if (t < fireSeconds * 1.6) {
            fireballPuffs(t, Math.max(10, counts.cap() / 5), sink);
        }
        cap(t, counts.cap(), life, grown, sink);
        stem(t, counts.stem(), life, grown, sink);
        surge(t, counts.surge(), life, sink);
        int extra = counts.extra();
        condensation(t, (int) Math.round(extra * 0.45), sink);
        fireColumns(t, extra, life, sink);
        pyrocumulus(t, Math.max(4, (int) Math.round(extra * 0.15)), life, sink);
    }

    /** The fireball: turbulent fire filling a sphere, hotter inside, cooling all over; then swallowed by the cap's smoke. */
    private void fireballPuffs(double t, int count, PuffSink sink) {
        double r = fireballRadius(t);
        double cy = capCentre(t);
        double heat = fireballHeat(t);
        float fade = (float) (1 - smooth((t - 0.8 * fireSeconds) / (0.8 * fireSeconds)));
        if (fade <= 0.01) return;
        for (int i = 0; i < count; i++) {
            double u = Math.cbrt(rand(i, 1));
            double y = rand(i, 2) * 2 - 1;
            double ring = Math.sqrt(1 - y * y);
            double a = rand(i, 3) * 2 * Math.PI + t * 0.6;
            double px = r * 0.6 * u * ring * Math.cos(a), py = r * 0.6 * u * y, pz = r * 0.6 * u * ring * Math.sin(a);
            Rgb c = ExplosionPalette.fire(Math.min(1, heat + 0.18 * (1 - u)));
            double brightness = 0.8 + 0.2 * rand(i, 4);
            sink.puff(px, cy + py, pz, r * (0.42 + 0.12 * rand(i, 5)), TILE_FIRE + (i & 3), (float) (rand(i, 6) * 6.283 + t * 0.4),
                    (float) (c.red() * brightness), (float) (c.green() * brightness), (float) (c.blue() * brightness), fade, 1,
                    (float) (ring * Math.cos(a)), (float) y, (float) (ring * Math.sin(a)), (float) (1 - u), EMISSIVE);
        }
    }

    /**
     * The cap: a torus of smoke turning over (up through its middle, out over the top, down its outside), a crown filling its top
     * and a darker underside where the stem feeds it. Hot puffs carry a fire puff behind their smoke while they glow.
     */
    private void cap(double t, int count, double life, double grown, PuffSink sink) {
        double formed = smooth((t - 0.35 * fireSeconds) / (0.8 * fireSeconds));
        if (formed <= 0) return;
        double radius = capRadius(t);
        double centre = capCentre(t);
        double halfThick = capHalfThickness(t);
        double major = 0.62 * radius;
        double tube = 0.38 * radius;
        // Turning over fast at first, slower as it spreads.
        double turned = 0.25 * 50 * (1 - Math.exp(-t / 50));
        double heat = heat(t);
        Rgb colour = capColour(t);
        double stretch = flattened ? 1 + 0.4 * clamp((t - 60) / 240) : 1 + 0.15 * clamp((t - 90) / 200);
        int torus = (int) Math.round(count * 0.7);
        int crown = (int) Math.round(count * 0.2);
        int under = Math.max(0, count - torus - crown);
        for (int i = 0; i < torus; i++) {
            double theta = rand(i, 11) * 2 * Math.PI + t * 0.004;
            double phi = rand(i, 12) * 2 * Math.PI - turned;
            double s = 0.55 + 0.45 * rand(i, 13);
            double out = major + tube * s * Math.cos(phi);
            double x = Math.cos(theta) * out, z = Math.sin(theta) * out;
            double y = centre + halfThick * s * Math.sin(phi);
            double size = 0.3 * radius * (0.75 + 0.5 * rand(i, 14)) * grown;
            double occlusion = (1 - s) / 0.45;
            double hot = heat * (0.5 + 0.7 * occlusion + 0.3 * Math.max(0, -Math.sin(phi)));
            capPuff(t, i, x, y, z, size, centre, radius, halfThick, stretch, colour, (float) occlusion, hot, formed, life, sink);
        }
        for (int i = 0; i < crown; i++) {
            double a = rand(i, 21) * 2 * Math.PI;
            double r = major * Math.sqrt(rand(i, 22));
            double x = Math.cos(a) * r, z = Math.sin(a) * r;
            double y = centre + halfThick * (0.5 + 0.38 * (1 - (r / major) * (r / major)));
            double size = 0.3 * radius * (0.8 + 0.4 * rand(i, 23)) * grown;
            capPuff(t, i + 5000, x, y, z, size, centre, radius, halfThick, stretch, colour, 0.15f, heat * 0.8, formed, life, sink);
        }
        for (int i = 0; i < under; i++) {
            double a = rand(i, 31) * 2 * Math.PI;
            double r = major * (0.3 + 0.7 * Math.sqrt(rand(i, 32)));
            double x = Math.cos(a) * r, z = Math.sin(a) * r;
            double y = centre - halfThick * (0.55 + 0.15 * rand(i, 33));
            double size = 0.25 * radius * (0.8 + 0.4 * rand(i, 34)) * grown;
            capPuff(t, i + 9000, x, y, z, size, centre, radius, halfThick, stretch, colour.scale(0.88), 0.45f, heat * 1.1, formed, life,
                    sink);
        }
    }

    private void capPuff(double t, int index, double x, double y, double z, double size, double centre, double radius, double halfThick,
                         double stretch, Rgb colour, float occlusion, double hot, double formed, double life, PuffSink sink) {
        double along = x * windX + z * windZ;
        double extra = along * (stretch - 1);
        double d = drift(y, t);
        double px = x + windX * (d + extra), pz = z + windZ * (d + extra);
        double nx = x / radius, ny = (y - centre) / Math.max(1, halfThick), nz = z / radius;
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-6) {
            ny = 1;
            nl = 1;
        }
        double heat = clamp(hot);
        float spin = (float) (rand(index, 15) * 6.283 + t * 0.01 * (rand(index, 16) - 0.5));
        float shade = (float) (0.9 + 0.15 * rand(index, 17));
        if (heat > 0.06) {
            Rgb fire = ExplosionPalette.fire(0.15 + 0.7 * heat);
            float glow = (float) Math.min(1, heat * 1.4);
            sink.puff(px, y, pz, size * 0.85, TILE_FIRE + (index & 3), spin + 1.1f, fire.red() * glow, fire.green() * glow,
                    fire.blue() * glow, (float) (formed * life * Math.min(1, heat * 1.8)), (float) heat, (float) (nx / nl),
                    (float) (ny / nl), (float) (nz / nl), occlusion, EMISSIVE);
        }
        float alpha = (float) (0.9 * formed * life * (1 - 0.8 * smooth((heat - 0.25) / 0.6)));
        if (alpha <= 0.01) return;
        sink.puff(px, y, pz, size, (index & 1) == 0 ? TILE_SOFT + (index >> 1 & 3) : TILE_DENSE + (index >> 1 & 3), spin,
                colour.red() * shade, colour.green() * shade, colour.blue() * shade, alpha, (float) heat, (float) (nx / nl),
                (float) (ny / nl), (float) (nz / nl), occlusion, 0);
    }

    /**
     * The stem: dust drawn up from the ground into the cap, flowing upwards, flared where it joins the cap and wide at its foot. A
     * high airburst raises a thinner, fainter one later that may never reach the cap.
     */
    private void stem(double t, int count, double life, double grown, PuffSink sink) {
        double capBottom = capCentre(t) - 0.6 * capHalfThickness(t);
        double formed;
        double reach;
        double width = stemRadius * Math.min(1, 0.4 + capRadius(t) / capRadius);
        double alpha = 0.85;
        if (highAirburst) {
            formed = clamp((t - 6) / 40);
            reach = Math.min(capBottom, 0.65 * capBottom * formed + 0.05 * capBottom);
            width *= 0.55;
            alpha = 0.45;
        } else {
            formed = Math.pow(clamp((t - 1.5) / 22), 0.8);
            reach = capBottom * formed;
            if (airburst) {
                width *= 0.75;
                alpha = 0.7;
            }
        }
        if (formed <= 0 || reach < 2) return;
        Rgb colour = stemColour(t);
        for (int i = 0; i < count; i++) {
            double u = frac(rand(i, 41) + t * 6 / Math.max(20, capBottom));
            double y = u * reach;
            double q = capBottom <= 0 ? 0 : y / capBottom;
            double radius = width * (1 + 1.2 * q * q * q * q + 0.8 * Math.max(0, 0.15 - q) / 0.15);
            double a = rand(i, 42) * 2 * Math.PI + t * 0.05;
            double off = radius * 0.55 * Math.sqrt(rand(i, 43));
            double d = drift(y, t);
            double x = Math.cos(a) * off + windX * d, z = Math.sin(a) * off + windZ * d;
            double edge = smooth(u / 0.05) * (1 - smooth((u - 0.9) / 0.1));
            float shade = (float) (0.88 + 0.16 * rand(i, 44));
            sink.puff(x, y, z, radius * 0.7 * (0.8 + 0.4 * rand(i, 45)) * grown, TILE_DENSE + (i & 3), (float) (rand(i, 46) * 6.283),
                    colour.red() * shade, colour.green() * shade, colour.blue() * shade, (float) (alpha * edge * life), 0,
                    (float) Math.cos(a), 0, (float) Math.sin(a), 0.3f, 0);
        }
    }

    /**
     * The base surge: a ring of dust rolling out along the ground from a ground burst, thickening and lifting as it goes and
     * settling after a few minutes. An airburst raises one too once its shock reaches the ground, smaller and fainter.
     */
    private void surge(double t, int count, double life, PuffSink sink) {
        double scaleBy = 1;
        double start = 0.3;
        double alpha = 0.6;
        if (airburst) {
            start = burstHeight / (waveSpeed * 20) + 0.3;
            scaleBy = highAirburst ? 0.45 : 0.7;
            alpha = highAirburst ? 0.3 : 0.45;
        }
        if (t < start) return;
        double since = t - start;
        double reach = surgeRadius * scaleBy * (1 - Math.exp(-since / 12));
        double settle = 1 - smooth((t - 60) / 140);
        if (settle <= 0 || reach < 2) return;
        double height = 3 + 0.07 * reach;
        for (int i = 0; i < count; i++) {
            double a = rand(i, 51) * 2 * Math.PI;
            double r = reach * (0.55 + 0.5 * rand(i, 52));
            double x = Math.cos(a) * r, z = Math.sin(a) * r;
            double y = height * 0.4 * (0.5 + rand(i, 53)) + 0.05 * since;
            double d = drift(y, t) * 0.4;
            float shade = (float) (0.9 + 0.15 * rand(i, 54));
            double size = (height * 0.8 + 0.04 * reach) * (0.8 + 0.4 * rand(i, 55)) * (1 + 0.3 * clamp(since / 120));
            sink.puff(x + windX * d, y, z + windZ * d, size, TILE_DENSE + (i & 3), (float) (rand(i, 56) * 6.283),
                    groundDust.red() * shade, groundDust.green() * shade, groundDust.blue() * shade,
                    (float) (alpha * clamp(since / 1.5) * settle * life), 0, (float) Math.cos(a), 0.6f, (float) Math.sin(a), 0.2f, 0);
        }
    }

    /**
     * Condensation: the Wilson cloud, a veil round the fireball just behind the shock that lasts a few seconds; rings round the
     * stem where the rising cloud lifts damp layers; and the pileus, a smooth hood over the cap while it punches up through one.
     */
    private void condensation(double t, int count, PuffSink sink) {
        Rgb white = palette.condensation();
        float thick = (float) palette.condensationAlpha();
        // The Wilson cloud.
        double wilsonEnd = 3.4;
        if (t > 0.25 && t < wilsonEnd && count > 0) {
            int n = Math.max(8, (int) Math.round(count * 0.6));
            double radius = Math.min(shockRadius(t) * 0.9, Math.min(4.5 * fireball, 0.6 * scale));
            double env = smooth((t - 0.25) / 0.35) * (1 - smooth((t - 1.4) / 2.0));
            double golden = Math.PI * (3 - Math.sqrt(5));
            double cy = burstHeight;
            for (int i = 0; i < n; i++) {
                // A Fibonacci sphere, cut where it meets the ground.
                double y = 1 - 2 * (i + 0.5) / n;
                double py = cy + radius * y;
                if (py < -radius * 0.05) continue;
                double ring = Math.sqrt(1 - y * y);
                double a = i * golden + rand(i, 61);
                double px = radius * ring * Math.cos(a), pz = radius * ring * Math.sin(a);
                sink.puff(px, Math.max(0, py), pz, radius * 0.33, TILE_VEIL, (float) (rand(i, 62) * 6.283), white.red(), white.green(),
                        white.blue(), (float) (0.55 * thick * env), 0, (float) (ring * Math.cos(a)), (float) y,
                        (float) (ring * Math.sin(a)), 0, 0);
            }
        }
        double capTop = capTop(t);
        double radius = capRadius(t);
        // Rings round the stem where the cloud lifted damp layers.
        int ringPuffs = Math.max(6, (int) Math.round(count * 0.3));
        double[] layers = flattened ? new double[]{0.32, 0.5, 0.68} : new double[]{0.42, 0.62};
        for (int l = 0; l < layers.length; l++) {
            double layer = layers[l] * top;
            double form = smooth((capTop - layer) / (0.12 * top));
            double fade = 1 - smooth((t - 30 - 8 * l) / 25);
            double env = form * fade;
            if (env <= 0.01) continue;
            double r = (0.45 + 0.1 * l) * radius;
            int n = ringPuffs / layers.length;
            for (int i = 0; i < n; i++) {
                double a = (i + rand(i + 100 * l, 63) * 0.5) * 2 * Math.PI / n;
                double y = layer + 2 * (rand(i + 100 * l, 64) - 0.5) + 0.08 * Math.max(0, t - 10);
                double d = drift(y, t);
                sink.puff(Math.cos(a) * r + windX * d, y, Math.sin(a) * r + windZ * d, 0.12 * radius + 2, TILE_VEIL,
                        (float) (rand(i, 65) * 6.283), white.red(), white.green(), white.blue(), (float) (0.5 * thick * env), 0,
                        (float) Math.cos(a), 0, (float) Math.sin(a), 0, 0);
            }
        }
        // The pileus: a hood forming over the cap as it nears a damp layer, swallowed once it punches through.
        double layer = 0.5 * top;
        double env = smooth((capTop - (layer - 0.25 * top)) / (0.13 * top)) * (1 - smooth((capTop - (layer - 0.02 * top)) / (0.12 * top)));
        if (env > 0.01) {
            double y = Math.max(layer, capTop + 0.05 * top);
            double d = drift(y, t);
            for (int i = 0; i < 3; i++) {
                double side = (i - 1) * 0.42 * radius;
                double size = (i == 1 ? 0.75 : 0.5) * radius;
                sink.puff(windX * d - windZ * side, y - Math.abs(side) * 0.18, windZ * d + windX * side, size, TILE_PILEUS, 0,
                        white.red(), white.green(), white.blue(), (float) (0.8 * env), 0, 0, 1, 0, 0, UPRIGHT);
            }
        }
    }

    /** How many ground fires send up smoke columns: more the wider the flash reached. */
    public int fireCount() {
        return thermalRadius <= 0 ? 0 : Math.max(3, Math.min(12, (int) Math.round(thermalRadius / 40)));
    }

    /** Where ground fire {@code i} burns, blocks from ground zero, into {@code out} (x, z), and when it lights, seconds. */
    public double firePoint(int i, double[] out) {
        double a = rand(i, 71) * 2 * Math.PI;
        double r = Math.min(1.4 * scale, thermalRadius * (0.25 + 0.65 * Math.sqrt(rand(i, 72))));
        out[0] = Math.cos(a) * r;
        out[1] = Math.sin(a) * r;
        return 10 + 40 * rand(i, 73);
    }

    /** Columns of soot from fires the flash lit, leaning downwind. */
    private void fireColumns(double t, int extra, double life, PuffSink sink) {
        int count = (int) Math.round(extra * 0.4);
        int fires = fireCount();
        if (fires == 0 || count <= 0) return;
        int per = Math.max(3, count / fires);
        double[] at = new double[2];
        Rgb soot = ExplosionPalette.SOOT;
        Rgb high = palette.smoke().scale(0.7);
        for (int f = 0; f < fires; f++) {
            double lit = firePoint(f, at);
            double grow = clamp((t - lit) / 40);
            if (grow <= 0) continue;
            double height = (50 + 70 * rand(f, 74)) * grow;
            for (int p = 0; p < per; p++) {
                double u = frac((p + rand(f * 31 + p, 75)) / per + t * 0.02);
                double y = u * height;
                double lean = y * 0.35;
                Rgb c = soot.lerp(high, u);
                double alpha = 0.55 * Math.sqrt(1 - u) * clamp((t - lit) / 8) * life;
                sink.puff(at[0] + windX * lean, y + 2, at[1] + windZ * lean, 3 + 0.18 * y, TILE_DENSE + (p & 3),
                        (float) (rand(f * 31 + p, 76) * 6.283), c.red(), c.green(), c.blue(), (float) alpha, 0, 0, 0.3f, 0, 0.2f, 0);
            }
        }
    }

    /** The pyrocumulus: a cumulus the fires' heat raises over them, downwind, from a minute or so on. */
    private void pyrocumulus(double t, int count, double life, PuffSink sink) {
        if (thermalRadius <= 0) return;
        double form = clamp((t - 70) / 60);
        if (form <= 0) return;
        double reach = Math.min(1.4 * scale, thermalRadius) * 0.5;
        double height = Math.min(0.45 * top, 200);
        double cx = windX * (40 + 0.15 * t), cz = windZ * (40 + 0.15 * t);
        Rgb c = palette.smoke().lerp(palette.condensation(), 0.55);
        for (int i = 0; i < count; i++) {
            double a = rand(i, 81) * 2 * Math.PI;
            double r = reach * Math.sqrt(rand(i, 82));
            double y = height * (0.8 + 0.35 * rand(i, 83)) * (0.6 + 0.4 * form);
            float shade = (float) (0.88 + 0.15 * rand(i, 84));
            sink.puff(cx + Math.cos(a) * r, y, cz + Math.sin(a) * r, (8 + 0.12 * reach) * (0.7 + 0.6 * rand(i, 85)) * (0.6 + 0.4 * form),
                    TILE_SOFT + (i & 3), (float) (rand(i, 86) * 6.283), c.red() * shade, c.green() * shade, c.blue() * shade,
                    (float) (0.7 * form * life), 0, (float) Math.cos(a), 0.5f, (float) Math.sin(a), 0.2f, 0);
        }
    }

    // ------------------------------------------------------------------ fast parts, drawn every frame

    /** When bolt {@code k} strikes, seconds after the detonation: most in the first seconds, none after half a minute. */
    public double lightningTime(int k) {
        return 0.8 + 24 * Math.pow(rand(k, 91), 1.7);
    }

    /** Whether bolt {@code k} is lit {@code t} seconds in: a few flickers over {@link #BOLT_SECONDS}. */
    public boolean boltLit(int k, double t) {
        double s = t - lightningTime(k);
        if (s < 0 || s > BOLT_SECONDS) return false;
        return s < 0.05 || (s > 0.09 && s < 0.13) || s > 0.16;
    }

    /** The bolts lit {@code t} seconds in: streaks of white light inside the young cap. */
    public void lightning(double t, PuffSink sink) {
        if (t > 26 || t < 0.5) return;
        for (int k = 0; k < BOLTS; k++) {
            if (!boltLit(k, t)) continue;
            double at = lightningTime(k);
            double radius = capRadius(at);
            double a = rand(k, 92) * 2 * Math.PI;
            double r = radius * 0.5 * Math.sqrt(rand(k, 93));
            double y = capCentre(at) + capHalfThickness(at) * (rand(k, 94) - 0.6);
            float glow = (float) (0.75 + 0.25 * rand(k, 95));
            sink.puff(Math.cos(a) * r, y, Math.sin(a) * r, radius * (0.25 + 0.15 * rand(k, 96)), TILE_LIGHTNING, 0, 0.85f * glow,
                    0.88f * glow, 1.0f * glow, glow, 0, 0, 0, 0, 0, EMISSIVE | UPRIGHT);
        }
    }

    /**
     * The dust front the shock sweeps up along the ground: a ring of terrain-coloured dust at the front, thinning as the shock
     * weakens; gone once it has died out. {@code count} puffs round the whole ring.
     */
    public void dustFront(double t, int count, PuffSink sink) {
        double shock = shockRadius(t);
        // An airburst's shock sweeps the ground only once it has come down to it.
        if (waveRadius <= 0 || shock <= burstHeight || t > waveRadius / (waveSpeed * 20) + 2) return;
        double front = Math.sqrt(shock * shock - burstHeight * burstHeight);
        if (front <= fireball) return;
        double strength = Math.pow(Math.max(0, 1 - shock / waveRadius), 0.7);
        double settle = 1 - clamp((t - waveRadius / (waveSpeed * 20)) / 2);
        double alpha = 0.7 * strength * settle;
        if (alpha < 0.01) return;
        double height = 2 + 0.03 * front;
        double spacing = 2 * Math.PI * front / Math.max(1, count);
        double size = Math.max(height * 1.2, Math.min(30, 0.6 * spacing));
        for (int i = 0; i < count; i++) {
            double a = (i + rand(i, 101)) * 2 * Math.PI / count;
            double r = front * (0.97 + 0.04 * rand(i, 102));
            float shade = (float) (0.9 + 0.15 * rand(i, 103));
            sink.puff(Math.cos(a) * r, size * 0.45, Math.sin(a) * r, size, TILE_DENSE + (i & 3), (float) (rand(i, 104) * 6.283),
                    groundDust.red() * shade, groundDust.green() * shade, groundDust.blue() * shade, (float) alpha, 0,
                    (float) Math.cos(a), 0.5f, (float) Math.sin(a), 0, 0);
        }
    }

    // ------------------------------------------------------------------ helpers

    private double rand(int index, int salt) {
        return BlastHash.unit(seedHash, index, salt);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    public static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /** Smoothstep of {@code v} clamped to [0, 1]. */
    public static double smooth(double v) {
        double x = clamp(v);
        return x * x * (3 - 2 * x);
    }
}
