package org.papiricoh.create_nuclearindustry.fusion.physics;

/**
 * What a tokamak plant gives and takes in Create stress units, so the cost of heating and cryogenics
 * can be weighed against what the plasma turbine makes from the plasma steam. Everything is in SU per
 * RPM of stress impact or capacity, the way Create counts it.
 *
 * <p>The defaults are the mod's balance numbers: a neutral beam injector is rated 20 MW for 32768
 * SU/RPM and an RF antenna 12 MW for 13312 SU/RPM (about 1600 and 1100 SU/RPM per MW, a wall-plug
 * efficiency well below 1); a cryogenic plant layer liquefies 2 mB/t of helium for 10240 SU/RPM; a
 * vacuum pump costs 2048 SU/RPM; the plasma turbine gives 409.6 SU/RPM per mB/t of plasma steam, and
 * the coolant carries 80% of the fusion power out as 1.6 mB/t of steam per MW. So the heating alone
 * breaks even near Q = 3, and a whole R=4 plant somewhat above Q = 4.
 */
public final class PowerBalance {

    /**
     * @param nbiStressPerMw         stress impact of neutral beam heating, SU/RPM per MW
     * @param rfStressPerMw          stress impact of radio-frequency heating, SU/RPM per MW
     * @param cryoStressPerHelium    stress impact of liquefying helium, SU/RPM per mB/t
     * @param pumpStress             stress impact of one vacuum pump
     * @param magnetDriveStress      stress impact of one magnet drive
     * @param turbinePerSteam        plasma turbine capacity, SU/RPM per mB/t of plasma steam
     */
    public record Params(double nbiStressPerMw, double rfStressPerMw, double cryoStressPerHelium, double pumpStress,
                         double magnetDriveStress, double turbinePerSteam) {
        public static final Params DEFAULT = new Params(32768.0 / 20.0, 13312.0 / 12.0, 10240.0 / 2.0, 2048.0,
                1024.0, 409.6);
    }

    /**
     * A running plant.
     *
     * @param fusionPower  MW
     * @param nbiPower     neutral beam power drawn, MW
     * @param rfPower      radio-frequency power drawn, MW
     * @param liquidHelium liquid helium the coils use, mB/t
     * @param pumps        vacuum pumps
     * @param magnetDrives magnet drives
     */
    public record Plant(double fusionPower, double nbiPower, double rfPower, double liquidHelium, int pumps,
                        int magnetDrives) {}

    private PowerBalance() {}

    /** Plasma steam the coolant takes out at this fusion power, mB/t. */
    public static double steam(double fusionPower) {
        return (1 - PlasmaModel.PLASMA_HEATING_SHARE) * fusionPower * PlasmaModel.STEAM_PER_MW;
    }

    /** Stress capacity the plasma turbine makes from that steam, SU/RPM. */
    public static double turbineCapacity(Params p, double fusionPower) {
        return steam(fusionPower) * p.turbinePerSteam();
    }

    /** Stress impact of the auxiliary heating, SU/RPM. */
    public static double heatingCost(Params p, double nbiPower, double rfPower) {
        return nbiPower * p.nbiStressPerMw() + rfPower * p.rfStressPerMw();
    }

    /** Stress impact of the plant's cryogenics, pumps and magnet drives, SU/RPM. */
    public static double supportCost(Params p, Plant plant) {
        return plant.liquidHelium() * p.cryoStressPerHelium() + plant.pumps() * p.pumpStress()
                + plant.magnetDrives() * p.magnetDriveStress();
    }

    /** Turbine capacity minus everything the plant uses, SU/RPM; negative means it loses energy. */
    public static double net(Params p, Plant plant) {
        return turbineCapacity(p, plant.fusionPower()) - heatingCost(p, plant.nbiPower(), plant.rfPower())
                - supportCost(p, plant);
    }

    /** Q at which neutral beam heating alone pays for itself in turbine capacity. */
    public static double heatingBreakEvenGain(Params p) {
        return p.nbiStressPerMw() / (steam(1.0) * p.turbinePerSteam());
    }
}
