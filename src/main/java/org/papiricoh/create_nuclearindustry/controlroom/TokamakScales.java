package org.papiricoh.create_nuclearindustry.controlroom;

import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.physics.MagnetModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;
import org.papiricoh.create_nuclearindustry.fusion.physics.VacuumModel;

import java.util.Locale;

/**
 * Full scales, warning levels and alarm logic of everything the control room shows of a tokamak:
 * monitor rows, gauges and pens, alarms and the annunciator preset. The plasma thresholds come from
 * the fusion code and are referenced only here, so a change on that side is a one-line change.
 * No game classes, so the logic can be tested on its own.
 */
public final class TokamakScales {
    // ------------------------------------------------------------------ from the fusion code
    /** Plasma temperature, in the model's degrees, where fusion starts. */
    public static final float IGNITION_TEMPERATURE = (float) PlasmaModel.IGNITION_TEMP;
    /** Plasma temperature above which instability builds up. */
    public static final float DANGER_TEMPERATURE = (float) PlasmaModel.DANGER_TEMP;
    /** Hottest the plasma gets: the full scale of temperature gauges. */
    public static final float MAX_TEMPERATURE = (float) PlasmaModel.MAX_TEMP;
    /** Confinement below which a hot plasma grows unstable. */
    public static final float MIN_BURN_CONFINEMENT = (float) PlasmaModel.STABLE_CONFINEMENT;
    /** Instability, 0-100, at which the plasma disrupts. */
    public static final float DISRUPTION_INSTABILITY = (float) PlasmaModel.DISRUPTION_INSTABILITY;
    /** Coils stop superconducting above this, in kelvin; above it with field on the tokamak trips. */
    public static final float SUPERCONDUCTING_LIMIT = (float) MagnetModel.SUPERCONDUCTING_TEMPERATURE;
    /** Chamber pressure, log10 of pascals, a plasma needs at most by default ({@code fusion.vacuumLimit} may change it). */
    public static final float PLASMA_VACUUM_LIMIT = (float) VacuumModel.Params.DEFAULT.plasmaLimit();

    // ------------------------------------------------------------------ full scales and warnings
    /** Instability that already shows red on a monitor: well before the disruption. */
    public static final float INSTABILITY_WARNING = 0.6f * DISRUPTION_INSTABILITY;
    /** Full scale of fusion power gauges, pens and the automatic power setpoint, in MW. */
    public static final float POWER_MAX = 1000.0f;
    /** Full scale of a fusion gain gauge; an ignited plasma pins the needle. */
    public static final float GAIN_MAX = 20.0f;
    /** What a sample stores for the gain of an ignited plasma, whose real gain is infinite. */
    public static final float GAIN_IGNITED = 1.0e6f;
    /** Below this gain the heating costs more than the plasma gives back (heating costs about 3x its MW in shaft power). */
    public static final float NET_GAIN = 3.0f;
    /** Full scale of density gauges, as a fraction of the Greenwald limit. */
    public static final float DENSITY_MAX = 1.2f;
    /** Full scale of helium ash gauges, as a share of the plasma. */
    public static final float ASH_MAX = 0.25f;
    /** Full scale of magnet temperature gauges, in kelvin: room temperature, so a cool-down is seen whole. */
    public static final float MAGNET_TEMPERATURE_MAX = 300.0f;
    /** Range of the logarithmic vacuum gauge, in log10 of pascals: from high vacuum to atmosphere. */
    public static final float VACUUM_MIN = -5.0f, VACUUM_MAX = 5.0f;
    /** Full scale of heating power gauges, in MW: a few beam injectors and antennas. */
    public static final float HEATING_MAX = 100.0f;

    private TokamakScales() {}

    // ------------------------------------------------------------------ samples

    /** The gain as a sample stores it: finite, {@link #GAIN_IGNITED} when the plasma needs no heating. */
    public static float storedGain(double gain) {
        if (Double.isNaN(gain) || gain <= 0) {
            return 0.0f;
        }
        return Double.isInfinite(gain) || gain >= GAIN_IGNITED ? GAIN_IGNITED : (float) gain;
    }

    public static boolean ignited(float storedGain) {
        return storedGain >= GAIN_IGNITED;
    }

    /** The phase a sample stores by ordinal, or OFFLINE for an unknown one. */
    public static TokamakPhase phase(float stored) {
        int ordinal = Math.round(stored);
        TokamakPhase[] phases = TokamakPhase.values();
        return ordinal >= 0 && ordinal < phases.length ? phases[ordinal] : TokamakPhase.OFFLINE;
    }

    /** The trip cause a sample stores by ordinal, or NONE for an unknown one. */
    public static TripCause trip(float stored) {
        int ordinal = Math.round(stored);
        TripCause[] causes = TripCause.values();
        return ordinal >= 0 && ordinal < causes.length ? causes[ordinal] : TripCause.NONE;
    }

    // ------------------------------------------------------------------ alarm logic

    /**
     * Whether the tokamak has tripped: its plasma went out, or a start was refused, for any reason but
     * the operator's own shutdown, and it has not been started again.
     */
    public static boolean tripped(TokamakPhase phase, TripCause cause) {
        return (phase == TokamakPhase.OFFLINE || phase == TokamakPhase.RAMP_DOWN)
                && cause != TripCause.NONE && cause != TripCause.OPERATOR;
    }

    /** Whether a burning plasma's gain is at or below {@code threshold}; a plasma that is not burning has no gain to judge. */
    public static boolean gainLow(TokamakPhase phase, float storedGain, float threshold) {
        return phase == TokamakPhase.BURN && storedGain <= threshold;
    }

    /** Whether the magnets are energised (or on their way), so their temperature matters. */
    public static boolean magnetsInUse(TokamakPhase phase, float field) {
        return field > 0.01f || phase == TokamakPhase.ENERGIZE || phase.hasPlasma();
    }

    // ------------------------------------------------------------------ alarm thresholds

    /** Gain threshold of a 0-100 setting: 0.2 a step, up to {@link #GAIN_MAX}. */
    public static float gainThreshold(int setting) {
        return setting * GAIN_MAX / 100.0f;
    }

    /** Vacuum threshold of a 0-100 setting, log10 of pascals: a tenth of a decade a step from {@link #VACUUM_MIN}. */
    public static float vacuumThreshold(int setting) {
        return VACUUM_MIN + setting * (VACUUM_MAX - VACUUM_MIN) / 100.0f;
    }

    /** Magnet temperature threshold of a 0-100 setting: half a kelvin a step, up to 50 K. */
    public static float magnetThreshold(int setting) {
        return setting * 0.5f;
    }

    /** Setting of a gain threshold. */
    public static int gainSetting(double gain) {
        return clampSetting(Math.round(gain / GAIN_MAX * 100.0));
    }

    /** Setting of a vacuum threshold, in log10 of pascals. */
    public static int vacuumSetting(double logPascals) {
        return clampSetting(Math.round((logPascals - VACUUM_MIN) / (VACUUM_MAX - VACUUM_MIN) * 100.0));
    }

    /** Setting of a magnet temperature threshold, in kelvin. */
    public static int magnetSetting(double kelvin) {
        return clampSetting(Math.round(kelvin / 0.5));
    }

    private static int clampSetting(long setting) {
        return (int) Math.max(0, Math.min(100, setting));
    }

    // ------------------------------------------------------------------ text

    /** A pressure given as log10 of pascals, as short as it gets: "3e-4 Pa", "1e5 Pa". */
    public static String formatPressure(double logPascals) {
        if (!Double.isFinite(logPascals)) {
            return "-";
        }
        int exponent = (int) Math.floor(logPascals);
        long mantissa = Math.round(Math.pow(10.0, logPascals - exponent));
        if (mantissa >= 10) {
            mantissa = 1;
            exponent++;
        }
        return mantissa + "e" + exponent + " Pa";
    }

    /** "Q 4.2", or "Q ∞" when ignited. */
    public static String formatGain(float storedGain) {
        if (ignited(storedGain)) {
            return "Q ∞";
        }
        return String.format(Locale.ROOT, storedGain >= 10 ? "Q %.0f" : "Q %.1f", storedGain);
    }
}
