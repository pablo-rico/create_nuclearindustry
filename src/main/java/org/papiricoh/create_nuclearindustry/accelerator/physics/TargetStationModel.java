package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * The target station at the end of a synchrotron's extraction line. The beam hits a dense target
 * (iridium, in real antiproton factories) and the whole beam power ends up in it. Above the
 * antiproton threshold of {@link AntimatterModel}, a magnetic horn collects antiprotons out of the
 * shower: {@code kjPerNanogram} of beam energy deposited makes one nominal nanogram, times the
 * yield at that energy and the collector's share. Below the threshold it only gets activated, like a
 * beam dump. Pure arithmetic.
 */
public final class TargetStationModel {
    /** Seconds in a tick. */
    public static final double SECONDS_PER_TICK = 0.05;

    /**
     * @param kjPerNanogram beam energy one nominal nanogram of antiprotons takes, kJ
     */
    public record Params(double kjPerNanogram) {
        /** 60000 kJ a nanogram: a 6.8 MW, 100 GeV beam makes about 11 ng a minute. */
        public static final Params DEFAULT = new Params(60000);
    }

    private TargetStationModel() {}

    /** Beam energy the target takes in one tick, kJ. */
    public static double depositedKj(double powerKw) {
        return Math.max(0, powerKw) * SECONDS_PER_TICK;
    }

    /** Whether a beam of this energy makes antiprotons at all. */
    public static boolean produces(AntimatterModel.Params am, double energyGev) {
        return AntimatterModel.yield(am, energyGev) > 0;
    }

    /** Antiprotons collected in one tick from a beam of this energy and power, ng. */
    public static double perTick(Params p, AntimatterModel.Params am, double energyGev, double powerKw) {
        if (p.kjPerNanogram() <= 0) {
            return 0;
        }
        return AntimatterModel.collected(am, depositedKj(powerKw) / p.kjPerNanogram(), energyGev);
    }

    /** The same per minute, for goggles and JEI. */
    public static double perMinute(Params p, AntimatterModel.Params am, double energyGev, double powerKw) {
        return perTick(p, am, energyGev, powerKw) * 1200;
    }

    /** Beam energy from which the yield stops rising, GeV: the threshold times one plus the most yield. */
    public static double saturationGev(AntimatterModel.Params am) {
        return Math.max(0, am.thresholdGev()) * (1 + Math.max(0, am.maxYield()));
    }

    /** Antiprotons a minute per megawatt of beam at this energy, ng: the JEI category's curve. */
    public static double perMinutePerMw(Params p, AntimatterModel.Params am, double energyGev) {
        return perMinute(p, am, energyGev, 1000.0);
    }

    /**
     * Sample points of {@link #perMinutePerMw} for JEI: {@code count} energies evenly from the threshold
     * to {@code 1.5 x} the saturation energy, as {@code {GeV, ng/min per MW}} pairs.
     */
    public static double[][] yieldCurve(Params p, AntimatterModel.Params am, int count) {
        int n = Math.max(2, count);
        double from = Math.max(0, am.thresholdGev());
        double to = Math.max(from + 1, 1.5 * saturationGev(am));
        double[][] points = new double[n][2];
        for (int i = 0; i < n; i++) {
            double gev = from + (to - from) * i / (n - 1);
            points[i][0] = gev;
            points[i][1] = perMinutePerMw(p, am, gev);
        }
        return points;
    }

    /** Power lost in the target, kW, for {@link ActivationModel}: all of the beam, whatever it makes. */
    public static double lossKw(double powerKw) {
        return Math.max(0, powerKw);
    }
}
