package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * Isotope production at the target station: below the antiproton threshold the beam does not make
 * antiprotons but transmutes the target in its slot instead (uranium-238 to molybdenum-99 by
 * photofission in the beam's shower, the mod's one recipe). A {@code target_irradiation} recipe names the target,
 * the least beam energy that works ({@code minEnergyGev}) and the beam energy one item takes
 * ({@code nominalKj}); each tick the target takes the whole beam power ({@link TargetStationModel#depositedKj}).
 * Above the antiproton threshold the station collects antiprotons instead and the target only waits.
 * Pure arithmetic.
 */
public final class TargetIrradiationModel {

    /**
     * After a tick of irradiation.
     *
     * @param made     items of output finished this tick (more than one only for a huge beam)
     * @param progress beam energy carried to the next item, kJ
     */
    public record Result(int made, double progress) {}

    private TargetIrradiationModel() {}

    /** Whether a beam of this energy irradiates a target that needs {@code minEnergyGev}: at or above it, and below the antiproton threshold. */
    public static boolean irradiates(AntimatterModel.Params am, double energyGev, double minEnergyGev) {
        return energyGev > 0 && energyGev >= minEnergyGev && !TargetStationModel.produces(am, energyGev);
    }

    /**
     * One tick: adds the deposited beam energy to {@code progressKj} and finishes as many items as it
     * covers, at most {@code maxItems} (the targets left in the slot). A beam that does not irradiate
     * leaves the progress as it was.
     */
    public static Result tick(AntimatterModel.Params am, double progressKj, double energyGev, double powerKw,
                              double minEnergyGev, double nominalKj, int maxItems) {
        double progress = Math.max(0.0, progressKj);
        if (maxItems <= 0 || !irradiates(am, energyGev, minEnergyGev)) {
            return new Result(0, progress);
        }
        progress += TargetStationModel.depositedKj(powerKw);
        if (!(nominalKj > 0)) {
            return new Result(maxItems, 0.0);
        }
        int made = (int) Math.min(maxItems, Math.floor(progress / nominalKj + 1.0e-9));
        progress = Math.max(0.0, progress - made * nominalKj);
        if (made >= maxItems) {
            // Nothing left to irradiate: the rest of the beam goes into the empty holder.
            progress = 0.0;
        }
        return new Result(made, progress);
    }

    /** Progress towards the next item, 0..1. */
    public static double fraction(double progressKj, double nominalKj) {
        if (!(nominalKj > 0)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, progressKj / nominalKj));
    }

    /** Ticks until the next item at this beam power, rounded up; {@link Long#MAX_VALUE} with no beam. */
    public static long ticksLeft(double progressKj, double nominalKj, double powerKw) {
        double left = nominalKj - Math.max(0.0, progressKj);
        if (left <= 0) {
            return 0L;
        }
        double perTick = TargetStationModel.depositedKj(powerKw);
        if (!(perTick > 0)) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(left / perTick - 1.0e-9);
    }

    /** Items a minute at this beam power, for JEI and the goggles. */
    public static double perMinute(double nominalKj, double powerKw) {
        if (!(nominalKj > 0)) {
            return 0.0;
        }
        return TargetStationModel.depositedKj(powerKw) * 1200.0 / nominalKj;
    }
}
