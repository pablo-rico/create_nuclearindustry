package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.explosive.blast.ShockFront;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Which sounds a listener hears of a detonation, when and how loud. Everything is timed by {@link ShockFront}, the same model
 * as the server's wave and the shaking: the ground's tremor first, then the crack and boom riding the shock front where the
 * shock still carries, or past it the far roll arriving at the speed of sound. A listener told of a detonation late hears only
 * what has yet to reach them ({@link #stale}): no boom replayed for a cloud two minutes old.
 * <p>
 * Loudness is worked out here rather than by the game's sixteen-block falloff, so a nuclear boom carries for kilometres. The
 * sound files are peak-normalised with different loudness (close cracks and bangs about -20 dB RMS, booms and distant sounds
 * -15 dB, debris and the cluster casing -23 dB): {@link Sound#gain} evens them out. No game classes.
 */
public final class ExplosionAudio {
    /** Ticks after its moment a cue may still start; any later and it is skipped. */
    public static final double STALE_TICKS = 6;

    /** Every sound an explosion plays, with the gain that evens out its file's loudness. */
    public enum Sound {
        NUKE_CRACK(1.0f), NUKE_BOOM(0.85f), NUKE_RUMBLE(0.8f), NUKE_RUMBLE_FAR(0.9f), NUKE_RETURN_WIND(0.7f), SEISMIC_RUMBLE(0.9f),
        CLOUD_LIGHTNING(0.55f), EXPLOSION_SMALL(1.0f), EXPLOSION_MEDIUM(1.0f), EXPLOSION_LARGE(1.0f), EXPLOSION_DISTANT(0.85f),
        DEBRIS_RAIN(1.0f), THERMOBARIC_BLAST(1.0f), THERMOBARIC_DISTANT(0.85f), CLUSTER_OPEN(1.0f), CLUSTER_CRACKLE(0.9f);

        private final float gain;

        Sound(float gain) {
            this.gain = gain;
        }

        public float gain() {
            return gain;
        }
    }

    /**
     * One sound to play.
     *
     * @param id     which cue of its detonation this is (0-63), to remember it was played
     * @param sound  what
     * @param tick   ticks after the detonation it reaches the listener
     * @param volume 0-1, the file's gain included
     * @param pitch  playback pitch
     */
    public record Cue(int id, Sound sound, double tick, float volume, float pitch) {}

    /** The cue ids of a nuclear detonation's fixed sounds; its lightning crackles take {@link #LIGHTNING_ID} on. */
    public static final int SEISMIC_ID = 0, SHOCK_ID = 1, BOOM_ID = 2, RUMBLE_ID = 3, WIND_ID = 4, LIGHTNING_ID = 8;
    /** A conventional blast's. */
    public static final int BANG_ID = 0, DEBRIS_ID = 1, CONVENTIONAL_SEISMIC_ID = 2;

    private ExplosionAudio() {}

    /** Whether {@code cue} has passed for a detonation {@code ageTicks} old: it would only start mid-way or out of time. */
    public static boolean stale(Cue cue, double ageTicks) {
        return ageTicks - cue.tick() > STALE_TICKS;
    }

    /** Whether {@code cue} is due now, {@code ageTicks} after the detonation. */
    public static boolean due(Cue cue, double ageTicks) {
        return ageTicks >= cue.tick() && !stale(cue, ageTicks);
    }

    /**
     * The cues of a nuclear detonation for a listener {@code distance} blocks away: its shock front moving at {@code waveSpeed}
     * blocks a tick dies out at {@code waveRadius}; {@code yieldKg} is its TNT equivalent and {@code fireball} its radius.
     * {@code lightningTicks} are when bolts flash in its cloud (ticks after the detonation), heard at the speed of sound.
     */
    public static List<Cue> nuclear(double distance, double waveSpeed, double waveRadius, double yieldKg, double fireball,
                                    boolean thermonuclear, double[] lightningTicks) {
        List<Cue> cues = new ArrayList<>(8);
        double d = Math.max(0, distance);
        double reach = Math.max(32, waveRadius);
        double shock = ShockFront.arrivalTicks(d, waveSpeed, waveRadius);
        double size = Math.cbrt(Math.max(1, yieldKg));
        double seismicReach = 30 * size;
        if (d > 1.2 * fireball) {
            float v = (float) (0.9 * Math.exp(-d / seismicReach));
            if (v > 0.04) cues.add(cue(SEISMIC_ID, Sound.SEISMIC_RUMBLE, ShockFront.seismicArrivalTicks(d), v, 1.0f));
        }
        float low = thermonuclear ? 0.9f : 1.0f;
        if (d <= waveRadius) {
            float near = (float) (1 / (1 + d / (0.5 * reach)));
            if (d < 0.6 * waveRadius) cues.add(cue(SHOCK_ID, Sound.NUKE_CRACK, shock, Math.min(1f, 0.4f + 0.8f * near), 1.0f));
            cues.add(cue(BOOM_ID, Sound.NUKE_BOOM, shock, Math.min(1f, 0.55f + 0.6f * near), low));
            cues.add(cue(RUMBLE_ID, Sound.NUKE_RUMBLE, shock + 10, Math.min(1f, 0.45f + 0.5f * near), low));
            if (d < 0.8 * waveRadius) {
                cues.add(cue(WIND_ID, Sound.NUKE_RETURN_WIND, shock + 60, Math.min(1f, 0.3f + 0.6f * near), 1.0f));
            }
        } else {
            double past = (d - waveRadius) / Math.max(1, 2 * reach);
            float far = (float) (1 / (1 + past));
            if (d < 1.6 * waveRadius) cues.add(cue(BOOM_ID, Sound.NUKE_BOOM, shock, 0.6f * far, low * 0.95f));
            cues.add(cue(RUMBLE_ID, Sound.NUKE_RUMBLE_FAR, shock, Math.max(0.12f, far), low * (0.95f - 0.05f * Math.min(1, (float) past))));
        }
        if (lightningTicks != null) {
            float v = (float) (1 / (1 + d / 400));
            if (v > 0.08) {
                for (int k = 0; k < lightningTicks.length && LIGHTNING_ID + k < 64; k++) {
                    cues.add(cue(LIGHTNING_ID + k, Sound.CLOUD_LIGHTNING, lightningTicks[k] + ShockFront.soundArrivalTicks(d), v,
                            0.9f + 0.2f * (k % 3) / 2));
                }
            }
        }
        return cues;
    }

    /** The sound a conventional blast of {@code charge} TNT-block equivalents makes close by: small, medium or large. */
    public static Sound size(double charge) {
        if (charge <= 8) return Sound.EXPLOSION_SMALL;
        if (charge >= 60) return Sound.EXPLOSION_LARGE;
        return Sound.EXPLOSION_MEDIUM;
    }

    /** Blocks past which a conventional blast of {@code charge} is heard only as its distant roll. */
    public static double distantFrom(double charge) {
        return 120 + 40 * Math.cbrt(Math.max(0.1, charge));
    }

    /** Blocks out to which a conventional blast of {@code charge} is heard at all. */
    public static double audibleTo(double charge) {
        return 600 + 260 * Math.cbrt(Math.max(0.1, charge));
    }

    /**
     * The cues of a conventional blast for a listener {@code distance} blocks away. A bomblet's far bang is left out: many of
     * them going off together are heard as one crackle ({@link CrackleWindow}).
     */
    public static List<Cue> conventional(double distance, ConventionalKind kind, double charge, double waveSpeed, double waveRadius,
                                         boolean airburst) {
        List<Cue> cues = new ArrayList<>(3);
        double d = Math.max(0, distance);
        double c = Math.max(0.1, charge);
        double arrival = ShockFront.arrivalTicks(d, waveSpeed, waveRadius);
        double distant = distantFrom(c);
        boolean thermobaric = kind == ConventionalKind.THERMOBARIC;
        float pitch = (float) Math.max(0.7, Math.min(1.25, 1.15 - 0.08 * Math.cbrt(c)));
        if (d <= distant) {
            float v = (float) Math.min(1, 1 / (1 + d / (20 + 12 * Math.cbrt(c))) * 1.4);
            Sound s = thermobaric ? Sound.THERMOBARIC_BLAST : size(c);
            cues.add(cue(BANG_ID, s, arrival, v, kind == ConventionalKind.BOMBLET ? pitch * 1.05f : pitch));
        } else if (kind != ConventionalKind.BOMBLET) {
            double reach = audibleTo(c);
            float v = (float) (0.9 * Math.max(0, 1 - (d - distant) / (reach - distant)));
            if (v > 0.03) cues.add(cue(BANG_ID, thermobaric ? Sound.THERMOBARIC_DISTANT : Sound.EXPLOSION_DISTANT, arrival, v, pitch));
        }
        if (!airburst && !thermobaric && kind != ConventionalKind.BOMBLET && c >= 4 && d < 96) {
            cues.add(cue(DEBRIS_ID, Sound.DEBRIS_RAIN, arrival + 20, (float) (0.8 * (1 - d / 96)), 1.0f));
        }
        if (c >= 48 && d > 24 && d < 40 * Math.cbrt(c)) {
            cues.add(cue(CONVENTIONAL_SEISMIC_ID, Sound.SEISMIC_RUMBLE, ShockFront.seismicArrivalTicks(d),
                    (float) (0.5 * (1 - d / (40 * Math.cbrt(c)))), 1.3f));
        }
        return cues;
    }

    /** The cluster casing blown open {@code distance} blocks away, heard at the speed of sound. */
    public static Cue clusterOpen(double distance) {
        double d = Math.max(0, distance);
        float v = (float) Math.max(0, Math.min(1, 1.3 / (1 + d / 60)));
        return cue(0, Sound.CLUSTER_OPEN, ShockFront.soundArrivalTicks(d), v, 1.0f);
    }

    private static Cue cue(int id, Sound sound, double tick, float volume, float pitch) {
        return new Cue(id, sound, tick, Math.max(0, Math.min(1, volume * sound.gain())), pitch);
    }

    /**
     * Hears bomblets go off and says when they add up to the crackle of a cluster strike: {@link #THRESHOLD} or more heard within
     * {@link #WINDOW} ticks start one, and another may follow only {@link #COOLDOWN} ticks later. Close bomblets still bang on
     * their own; this is what the rest of them sound like together.
     */
    public static final class CrackleWindow {
        public static final double WINDOW = 60;
        public static final int THRESHOLD = 4;
        public static final double COOLDOWN = 80;

        private final double[] heard = new double[THRESHOLD];
        private int count;
        private int next;
        private double lastCrackle = Double.NEGATIVE_INFINITY;

        /** A bomblet heard at {@code tick}; true when a crackle should start now. */
        public boolean hear(double tick) {
            heard[next] = tick;
            next = (next + 1) % THRESHOLD;
            count = Math.min(THRESHOLD, count + 1);
            if (count < THRESHOLD || tick - lastCrackle < COOLDOWN) return false;
            // The oldest of the last few: they all fell within the window.
            double oldest = heard[next];
            if (tick - oldest > WINDOW) return false;
            lastCrackle = tick;
            return true;
        }

        public void clear() {
            count = 0;
            next = 0;
            lastCrackle = Double.NEGATIVE_INFINITY;
        }
    }
}
