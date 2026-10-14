package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

/**
 * What a plasma section module shows of a tokamak: a cut through the torus with the plasma in it. The
 * tokamak's plasma has no simulated shape, so the picture is drawn from what it does simulate: the
 * phase picks the colour, the temperature how bright the plasma glows, the density how much of the
 * chamber it fills, the instability how much it shakes and the field how many field lines circle it.
 * Packed into a few bytes for the desk's sync; no game classes.
 */
public final class PlasmaSection {
    public static final int BYTES = 7;
    private static final int LEVELS = 255;
    private static final TokamakPhase[] PHASES = TokamakPhase.values();
    public static final int MAX_FIELD_LINES = 6;

    private PlasmaSection() {}

    /**
     * The readings as fractions of their full scale: {@code instability} of the disruption limit,
     * {@code field} and {@code confinement} 0-1.
     */
    public static byte[] encode(TokamakPhase phase, float temperature, float density, float instability, float field,
                                float confinement, boolean tripped) {
        return new byte[]{(byte) phase.ordinal(), level(temperature), level(density), level(instability), level(field),
                level(confinement), (byte) (tripped ? 1 : 0)};
    }

    /** How the section looks for the bytes a module synced, or null for none. */
    public static Look decode(byte[] data) {
        if (data.length < BYTES) {
            return null;
        }
        int ordinal = data[0] & 0xFF;
        TokamakPhase phase = ordinal < PHASES.length ? PHASES[ordinal] : TokamakPhase.OFFLINE;
        return look(phase, fraction(data[1]), fraction(data[2]), fraction(data[3]), fraction(data[4]), fraction(data[5]), data[6] != 0);
    }

    /**
     * @param plasma       whether there is plasma to draw
     * @param radius       its size, a fraction of the chamber's half-height
     * @param brightness   0-1
     * @param color        ARGB of its core
     * @param wobble       how far its edge shakes, a fraction of its radius
     * @param fieldLines   field lines to draw round it
     * @param coils        whether the coils carry current (drawn lit)
     * @param tripped      whether the tokamak tripped (the frame warns)
     */
    public record Look(boolean plasma, float radius, float brightness, int color, float wobble, int fieldLines, boolean coils,
                       boolean tripped) {}

    public static Look look(TokamakPhase phase, float temperature, float density, float instability, float field,
                            float confinement, boolean tripped) {
        boolean plasma = phase.hasPlasma() || phase == TokamakPhase.RAMP_DOWN && temperature > 0.01f;
        float radius = plasma ? 0.3f + 0.55f * clamp(density) * (0.7f + 0.3f * clamp(confinement)) : 0.0f;
        float brightness = plasma ? 0.25f + 0.75f * clamp(temperature) : 0.0f;
        if (phase == TokamakPhase.RAMP_DOWN) {
            brightness *= 0.5f;
        }
        float wobble = plasma ? 0.3f * (float) Math.pow(clamp(instability), 1.5) : 0.0f;
        boolean coils = phase.ordinal() >= TokamakPhase.ENERGIZE.ordinal();
        int lines = coils ? Math.round(clamp(field) * MAX_FIELD_LINES) : 0;
        return new Look(plasma, radius, brightness, color(phase, temperature), wobble, lines, coils, tripped);
    }

    /** Violet as it breaks down, orange while it heats, pink going white-hot as it burns. */
    private static int color(TokamakPhase phase, float temperature) {
        int base = switch (phase) {
            case BREAKDOWN -> 0xFFB07CFF;
            case HEATING -> 0xFFFF9A4A;
            case BURN -> 0xFFFF6FD0;
            default -> 0xFF8A6AB0;
        };
        float white = phase == TokamakPhase.BURN || phase == TokamakPhase.HEATING ? 0.6f * clamp(temperature) : 0.0f;
        return mix(base, 0xFFFFFFFF, white);
    }

    public static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static byte level(float fraction) {
        return (byte) Math.round(clamp(fraction) * LEVELS);
    }

    private static float fraction(byte stored) {
        return (stored & 0xFF) / (float) LEVELS;
    }

    private static float clamp(float value) {
        return Float.isNaN(value) ? 0.0f : Math.max(0.0f, Math.min(1.0f, value));
    }
}
