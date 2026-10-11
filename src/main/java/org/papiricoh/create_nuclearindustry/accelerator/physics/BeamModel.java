package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * The particle beam, simplified: energy is the sum of what each stage adds, and how much of the
 * current gets through depends on the vacuum and the focusing.
 *
 * <ul>
 *   <li><b>Linac.</b> Each radio-frequency cavity adds a fixed energy gain at full drive (copper or
 *       superconducting), scaled by the share of the radio-frequency power it gets.</li>
 *   <li><b>Ring.</b> A synchrotron's top energy is set by its magnetic rigidity: {@code E = 0.3 B rho}
 *       (GeV, tesla, metres), where the bending radius {@code rho} is the ring radius times the share
 *       of the ring that bends (dipoles). Bigger rings, more dipoles and superconducting dipoles all
 *       reach higher energies. A ring only accepts beam from a linac at least
 *       {@code 1/injectionRatio} of its top energy.</li>
 *   <li><b>Transmission.</b> Every stage passes {@code base * focus * vacuum} of its current. Focus
 *       is the square of the quadrupole coverage (one quadrupole per {@code quadSpacing} elements on
 *       a line, per {@code ringCellsPerQuad} cells on a ring), so missing quadrupoles cost a lot.</li>
 *   <li><b>Power.</b> Beam power in kW is MeV times microamperes over a thousand.</li>
 * </ul>
 * Pure arithmetic; the controller copies the config into {@link Params}.
 */
public final class BeamModel {

    /**
     * @param sourceCurrent       current the ion source makes at full drive, uA
     * @param sourceEnergy        energy of the ions leaving the source, MeV
     * @param copperGain          energy one copper cavity adds at full drive, MeV
     * @param superconductingGain energy one superconducting cavity adds at full drive, MeV
     * @param copperCavityPower   radio-frequency power a copper cavity needs, MW
     * @param superconductingCavityPower radio-frequency power a superconducting cavity needs, MW
     * @param quadSpacing         line elements one quadrupole keeps focused
     * @param ringCellsPerQuad    ring cells one quadrupole keeps focused
     * @param normalDipoleField   field of a normal-conducting dipole, T
     * @param superconductingDipoleField field of a superconducting dipole, T
     * @param rigidity            GeV per tesla-metre of bending
     * @param injectionRatio      ring top energy over the lowest linac energy it accepts
     * @param goodVacuum          pressure at or below which the vacuum costs nothing, log10 Pa
     * @param badVacuum           pressure at or above which no beam gets through, log10 Pa
     * @param baseTransmission    share of the current a perfect stage passes
     * @param ringDuty            share of the time a ring delivers beam (the rest is filling and ramping)
     * @param rampPerCavity       MeV per tick each ring cavity adds while ramping
     */
    public record Params(double sourceCurrent, double sourceEnergy, double copperGain, double superconductingGain,
                         double copperCavityPower, double superconductingCavityPower, double quadSpacing,
                         double ringCellsPerQuad, double normalDipoleField, double superconductingDipoleField,
                         double rigidity, double injectionRatio, double goodVacuum, double badVacuum,
                         double baseTransmission, double ringDuty, double rampPerCavity) {
        public static final Params DEFAULT = new Params(200, 0.05, 2.5, 12, 0.25, 0.08, 4, 8, 1.8, 8.0, 0.3, 100, -6, -3,
                0.95, 0.5, 25);
    }

    /** What one stage of the machine passes on: current out and current lost, uA. */
    public record Stage(double current, double lost) {}

    private BeamModel() {}

    /** Share of the radio-frequency power the cavities need that they get, 0..1. */
    public static double rfDrive(double supplyMw, double demandMw) {
        if (demandMw <= 0) {
            return supplyMw > 0 || demandMw == 0 ? 1 : 0;
        }
        return clamp(supplyMw / demandMw, 0, 1);
    }

    /** Radio-frequency power these cavities need, MW. */
    public static double rfDemand(Params p, int copper, int superconducting) {
        return Math.max(0, copper) * p.copperCavityPower() + Math.max(0, superconducting) * p.superconductingCavityPower();
    }

    /** Share of the beam the vacuum lets through, 0..1. */
    public static double vacuumFactor(Params p, double log10Pa) {
        if (p.badVacuum() <= p.goodVacuum()) {
            return log10Pa <= p.goodVacuum() ? 1 : 0;
        }
        return clamp((p.badVacuum() - log10Pa) / (p.badVacuum() - p.goodVacuum()), 0, 1);
    }

    /** How well this many quadrupoles focus this many elements: the square of their coverage. */
    public static double focus(int elements, int quadrupoles, double elementsPerQuad) {
        if (elements <= 0) {
            return 1;
        }
        double coverage = clamp(quadrupoles * elementsPerQuad / elements, 0, 1);
        return coverage * coverage;
    }

    /** Share of the current a stage passes. */
    public static double transmission(Params p, double focus, double vacuumFactor) {
        return clamp(p.baseTransmission() * focus * vacuumFactor, 0, 1);
    }

    /** Energy at the end of a linac with these cavities at this drive, MeV. */
    public static double linacEnergy(Params p, int copper, int superconducting, double drive) {
        double gain = Math.max(0, copper) * p.copperGain() + Math.max(0, superconducting) * p.superconductingGain();
        return p.sourceEnergy() + gain * clamp(drive, 0, 1);
    }

    /** Mean dipole field of a ring with these dipoles, T. */
    public static double dipoleField(Params p, int normal, int superconducting) {
        int all = Math.max(0, normal) + Math.max(0, superconducting);
        if (all == 0) {
            return 0;
        }
        return (Math.max(0, normal) * p.normalDipoleField() + Math.max(0, superconducting) * p.superconductingDipoleField()) / all;
    }

    /** Top energy of a ring of this radius, dipole share and field, GeV ({@code 0.3 B R f}). */
    public static double ringMaxEnergy(Params p, double fieldTesla, int radius, double dipoleFraction) {
        return p.rigidity() * Math.max(0, fieldTesla) * Math.max(0, radius) * clamp(dipoleFraction, 0, 1);
    }

    /** Lowest linac energy the ring accepts, MeV. */
    public static double injectionEnergy(Params p, double ringMaxGev) {
        return ringMaxGev * 1000.0 / Math.max(1, p.injectionRatio());
    }

    public static boolean injectionOk(Params p, double linacMev, double ringMaxGev) {
        return ringMaxGev > 0 && linacMev + 1.0e-9 >= injectionEnergy(p, ringMaxGev);
    }

    /** Beam power, kW. */
    public static double beamPower(double energyMev, double currentMicroamps) {
        return Math.max(0, energyMev) * Math.max(0, currentMicroamps) / 1000.0;
    }

    /** One stage: what goes in, what gets through. */
    public static Stage stage(double currentIn, double transmission) {
        double in = Math.max(0, currentIn);
        double out = in * clamp(transmission, 0, 1);
        return new Stage(out, in - out);
    }

    /** Current from the ion source at this drive and gas supply (both 0..1), uA. */
    public static double sourceCurrent(Params p, double drive, double gas) {
        return p.sourceCurrent() * clamp(drive, 0, 1) * clamp(gas, 0, 1);
    }

    /** Ring energy one tick later while ramping towards {@code target}, MeV. */
    public static double ramp(Params p, double energyMev, double targetMev, int cavities, double drive) {
        double step = Math.max(1, cavities) * p.rampPerCavity() * clamp(drive, 0, 1);
        if (energyMev < targetMev) {
            return Math.min(targetMev, energyMev + step);
        }
        return Math.max(targetMev, energyMev - step);
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
