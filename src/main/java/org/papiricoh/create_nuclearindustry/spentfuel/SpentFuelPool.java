package org.papiricoh.create_nuclearindustry.spentfuel;

/**
 * Heat and shielding of a spent fuel pool, without game classes so it can be unit tested.
 * Water over a rack is a very good shield, and the pool soaks up the racks' decay heat up to what
 * its volume (and any coolers) can carry away; past that, the surplus boils the water off.
 */
public final class SpentFuelPool {

    private SpentFuelPool() {}

    /** Fraction of a rack's radiation that gets through {@code depth} blocks of water above it. */
    public static double waterShielding(int depth, double perBlock) {
        return Math.pow(Math.max(0.0, Math.min(1.0, perBlock)), Math.max(0, depth));
    }

    /** Heat the pool's water sheds on its own, in kW. */
    public static double passiveCooling(int waterBlocks, double perBlock) {
        return Math.max(0, waterBlocks) * perBlock;
    }

    /**
     * Heat a pool cooler carries away at {@code rpm}: nothing below {@code minRpm}, rising with speed
     * up to {@code capacity} at {@code ratedRpm}.
     */
    public static double coolerCapacity(double rpm, double minRpm, double ratedRpm, double capacity) {
        double speed = Math.abs(rpm);
        if (speed < minRpm || ratedRpm <= 0) {
            return 0.0;
        }
        return capacity * Math.min(1.0, speed / ratedRpm);
    }

    /**
     * Surplus heat held by the water after {@code seconds}, in kJ: it grows by what the cooling cannot
     * carry away, and falls back by the spare capacity once the cooling catches up.
     */
    public static double accumulateBoil(double stored, double heat, double cooling, double seconds) {
        return Math.max(0.0, stored + (heat - cooling) * seconds);
    }

    /** Whether the stored surplus is enough to boil off a whole surface layer of {@code layerBlocks}. */
    public static boolean boilsLayer(double stored, int layerBlocks, double energyPerBlock) {
        return layerBlocks > 0 && stored >= layerBlocks * energyPerBlock;
    }
}
