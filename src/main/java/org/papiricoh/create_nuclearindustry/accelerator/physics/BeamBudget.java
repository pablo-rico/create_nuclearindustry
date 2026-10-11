package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * One tick of beam through the whole machine: the ion source, the injector line (the linac and its
 * transport), an optional ring and its extraction line. Works out the energy and current that reach
 * the end, and the power lost in each stage, from {@link BeamModel}. Pure arithmetic.
 *
 * <p>A linac's energy follows the radio-frequency drive, limited by the operator's energy setpoint.
 * A ring's energy is whatever its ramp reached, passed in by the controller. A ring only delivers
 * beam for its duty share of the time; the rest of the beam is not lost, only not delivered yet.
 */
public final class BeamBudget {

    /**
     * @param copper              copper cavities on the injector line
     * @param superconducting     superconducting cavities on the injector line
     * @param lineElements        elements on the injector line
     * @param lineQuadrupoles     quadrupoles on the injector line
     * @param rfDrive             share of the radio-frequency power the cavities get, 0..1
     * @param energySetpoint      operator's energy setpoint, 0..1
     * @param sourceDrive         the ion source's drive times the operator's current setpoint, 0..1
     * @param gas                 share of the hydrogen the source needs that it has, 0..1
     * @param vacuum              beam pipe pressure, log10 Pa
     * @param magnetPower         share of the magnets that are fed, 0..1
     * @param ringRadius          ring radius, 0 without a ring
     * @param ringCells           ring cells
     * @param ringQuadrupoles     quadrupoles in the ring
     * @param ringEnergy          energy the ring's ramp reached, MeV
     * @param extractionElements  elements on the extraction line
     * @param extractionQuadrupoles quadrupoles on the extraction line
     */
    public record Inputs(int copper, int superconducting, int lineElements, int lineQuadrupoles, double rfDrive,
                         double energySetpoint, double sourceDrive, double gas, double vacuum, double magnetPower,
                         int ringRadius, int ringCells, int ringQuadrupoles, double ringEnergy, int extractionElements,
                         int extractionQuadrupoles) {
        /** A linac on its own. */
        public static Inputs linac(int copper, int superconducting, int elements, int quadrupoles, double rfDrive,
                                   double energySetpoint, double sourceDrive, double gas, double vacuum, double magnetPower) {
            return new Inputs(copper, superconducting, elements, quadrupoles, rfDrive, energySetpoint, sourceDrive, gas, vacuum,
                    magnetPower, 0, 0, 0, 0, 0, 0);
        }

        public boolean ring() {
            return ringRadius > 0;
        }
    }

    /**
     * @param linacEnergy    energy at the end of the linac, MeV
     * @param energy         energy reaching the end of the machine, MeV
     * @param sourceCurrent  current the source makes, uA
     * @param current        current reaching the end, uA
     * @param power          beam power at the end, kW
     * @param linacLoss      power lost on the injector line, kW
     * @param ringLoss       power lost in the ring, kW
     * @param extractionLoss power lost on the extraction line, kW
     * @param lossFraction   share of the source current lost (not counting a ring's duty), 0..1
     */
    public record Result(double linacEnergy, double energy, double sourceCurrent, double current, double power,
                         double linacLoss, double ringLoss, double extractionLoss, double lossFraction) {
        public static final Result NONE = new Result(0, 0, 0, 0, 0, 0, 0, 0, 0);

        public double totalLoss() {
            return linacLoss + ringLoss + extractionLoss;
        }
    }

    private BeamBudget() {}

    /** Energy the linac reaches at this drive and setpoint, MeV. */
    public static double linacEnergy(BeamModel.Params p, Inputs in) {
        double drive = in.ring() ? in.rfDrive() : Math.min(in.rfDrive(), Math.max(0, in.energySetpoint()));
        return BeamModel.linacEnergy(p, in.copper(), in.superconducting(), drive);
    }

    /**
     * @param beamOn the source is making beam this tick
     */
    public static Result compute(BeamModel.Params p, Inputs in, boolean beamOn) {
        double linacEnergy = linacEnergy(p, in);
        if (!beamOn) {
            return new Result(linacEnergy, in.ring() ? in.ringEnergy() : linacEnergy, 0, 0, 0, 0, 0, 0, 0);
        }
        double source = BeamModel.sourceCurrent(p, in.sourceDrive(), in.gas());
        double vacuum = BeamModel.vacuumFactor(p, in.vacuum());
        double magnets = BeamModel.clamp(in.magnetPower(), 0, 1);

        double lineFocus = BeamModel.focus(in.lineElements(), in.lineQuadrupoles(), p.quadSpacing()) * magnets;
        BeamModel.Stage linac = BeamModel.stage(source, BeamModel.transmission(p, lineFocus, vacuum));
        // Particles are lost all along the linac, on average at half its energy.
        double linacLoss = BeamModel.beamPower(linacEnergy / 2, linac.lost());
        if (!in.ring()) {
            double power = BeamModel.beamPower(linacEnergy, linac.current());
            double lossFraction = source > 0 ? linac.lost() / source : 0;
            return new Result(linacEnergy, linacEnergy, source, linac.current(), power, linacLoss, 0, 0, lossFraction);
        }

        double ringFocus = BeamModel.focus(in.ringCells(), in.ringQuadrupoles(), p.ringCellsPerQuad()) * magnets;
        BeamModel.Stage ring = BeamModel.stage(linac.current(), BeamModel.transmission(p, ringFocus, vacuum));
        double energy = Math.max(linacEnergy, in.ringEnergy());
        double ringLoss = BeamModel.beamPower(energy, ring.lost());
        double extractFocus = BeamModel.focus(in.extractionElements(), in.extractionQuadrupoles(), p.quadSpacing()) * magnets;
        BeamModel.Stage extraction = BeamModel.stage(ring.current(), BeamModel.transmission(p, extractFocus, vacuum));
        double duty = BeamModel.clamp(p.ringDuty(), 0, 1);
        double current = extraction.current() * duty;
        double extractionLoss = BeamModel.beamPower(energy, extraction.lost() * duty);
        double lost = linac.lost() + ring.lost() + extraction.lost();
        double lossFraction = source > 0 ? lost / source : 0;
        return new Result(linacEnergy, energy, source, current, BeamModel.beamPower(energy, current), linacLoss,
                ringLoss * duty, extractionLoss, lossFraction);
    }
}
