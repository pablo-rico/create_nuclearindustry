package org.papiricoh.create_nuclearindustry.propulsion.physics;

/**
 * An antimatter engine: antiprotons fed into a chamber annihilate with propellant, and magnetic coils
 * steer the charged pions out of a magnetic nozzle. More coils catch more of them, so the efficiency
 * grows with the coils towards one; what they miss heats them and leaves as gamma rays. The coils
 * hold their field only while they have energy, for themselves and for each nanogram fed; without it,
 * or with coils worn through by heat, the confinement fails, and whatever antimatter the chamber holds
 * annihilates. No game classes.
 */
public final class AntimatterRocketModel {
    public static final double AMBIENT = 20.0;
    public static final int MIN_COILS = 1, MAX_COILS = 6;

    /**
     * @param maxFeed      antimatter fed at full throttle, ng per tick
     * @param thrustPerNg  thrust per ng/tick at full efficiency
     * @param coilFe       energy each coil needs a tick to hold its field, FE
     * @param fePerNg      energy each nanogram fed needs on top, FE
     * @param coilRise     coil temperature above ambient at full feed with one coil, °C (shared by the coils)
     * @param coilLimit    coil temperature above which they wear, °C
     * @param coilResponse share of the gap to its running temperature the coils close each tick
     * @param wearPerTick  wear a tick at twice the limit, 0-1
     * @param gammaPerNg   gamma dose behind the nozzle at one block per ng/tick that the coils miss
     */
    public record Params(double maxFeed, double thrustPerNg, int coilFe, double fePerNg, double coilRise, double coilLimit,
                         double coilResponse, double wearPerTick, double gammaPerNg) {
        public static final Params DEFAULT = new Params(0.01, 2.5e6, 64, 2.0e5, 2400.0, 900.0, 0.02, 0.002, 4000.0);
    }

    /**
     * One tick.
     *
     * @param fed      antimatter annihilated, ng
     * @param energy   energy used, FE
     * @param thrust   thrust along the engine's axis
     * @param coil     coil temperature, °C
     * @param wear     coil wear, 0-1
     * @param confined whether the field held this tick
     * @param gamma    gamma dose rate behind the nozzle, at one block
     */
    public record Step(double fed, int energy, double thrust, double coil, double wear, boolean confined, double gamma) {}

    private AntimatterRocketModel() {}

    /** Share of the annihilation energy the coils turn into thrust: half with two coils, nearly all with six. */
    public static double efficiency(int coils) {
        int n = Math.max(0, Math.min(MAX_COILS, coils));
        return n == 0 ? 0 : 1 - Math.pow(0.5, n / 2.0);
    }

    /** Energy the field needs a tick feeding {@code feed} ng. */
    public static int energyNeed(int coils, double feed, Params p) {
        return (int) Math.ceil(Math.max(0, coils) * p.coilFe() + Math.max(0, feed) * p.fePerNg());
    }

    /**
     * Advances one tick.
     *
     * @param throttle 0-1, from the redstone signal
     * @param coils    magnetic coil segments
     * @param stored   antimatter in the chamber, ng
     * @param energy   energy at hand, FE
     * @param coil     coil temperature now, °C
     * @param wear     coil wear now, 0-1
     */
    public static Step tick(double throttle, int coils, double stored, int energy, double coil, double wear, Params p) {
        double t = Math.max(0, Math.min(1, throttle));
        int n = Math.max(0, Math.min(MAX_COILS, coils));
        double feed = n > 0 && wear < 1.0 ? Math.min(t * p.maxFeed(), Math.max(0, stored)) : 0;
        int need = energyNeed(n, feed, p);
        boolean confined = n > 0 && wear < 1.0 && energy >= need;
        if (!confined) {
            feed = 0;
            need = Math.min(energy, n * p.coilFe());
        }
        double eff = efficiency(n);
        double share = p.maxFeed() <= 0 ? 0 : feed / p.maxFeed();
        double thrust = feed * p.thrustPerNg() * eff;
        double running = AMBIENT + share * p.coilRise() * (1 - eff) / Math.max(1, n) * 2;
        double response = Math.max(0, Math.min(1, p.coilResponse()));
        double next = coil + (running - coil) * response;
        double nextWear = Math.min(1.0, wear + (next > p.coilLimit() && p.coilLimit() > 0
                ? p.wearPerTick() * (next - p.coilLimit()) / p.coilLimit() : 0));
        double gamma = feed * p.gammaPerNg() * (1 - eff);
        return new Step(feed, confined ? need : Math.max(0, need), thrust, next, nextWear, confined, gamma);
    }
}
