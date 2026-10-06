package org.papiricoh.create_nuclearindustry.fusion.physics;

import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.Config;

/**
 * Zero-dimensional plasma model of a tokamak.
 *
 * <ul>
 *   <li>Confinement {@code kappa} = toroidal coil coverage x magnet drive (0 without a solenoid).</li>
 *   <li>Auxiliary (ohmic) heating from the magnet drive fades out between 1500 and 3000 degrees,
 *       as plasma resistance drops; above that only fusion keeps the plasma hot.</li>
 *   <li>Losses grow with temperature and shrink with confinement.</li>
 *   <li>Burn rate scales with plasma volume and temperature. D-T needs tritium; without it the
 *       plasma runs on D-D at a fraction of the power, which slowly breeds tritium to start D-T.</li>
 *   <li>20% of the fusion power heats the plasma; 80% leaves through the coolant as plasma steam.
 *       Whatever the coolant cannot carry stays in the plasma, so a dry tokamak overheats.</li>
 *   <li>Overheating or poor confinement while hot builds instability; at 100 the plasma disrupts.</li>
 * </ul>
 */
public class TokamakPhysicsSimulator {
    public static final double AMBIENT_TEMP = 20.0;
    public static final double IGNITION_TEMP = 1500.0;
    public static final double FULL_BURN_SPAN = 1500.0;
    public static final double DANGER_TEMP = 5000.0;
    public static final double MAX_TEMP = 6000.0;
    public static final double MIN_BURN_CONFINEMENT = 0.65;
    public static final double STABLE_CONFINEMENT = 0.55;
    public static final double DISRUPTION_INSTABILITY = 100.0;

    private static final double AUX_HEATING = 8.0;
    private static final double AUX_FADE_END = 3000.0;
    private static final double LOSS_COEFFICIENT = 0.004;
    private static final double POWER_PER_MB = 1000.0;          // MW per mB/t of fuel burnt
    private static final double SELF_HEATING = 68.0;            // degrees/tick per MW per plasma cell
    private static final double PLASMA_HEATING_SHARE = 0.2;
    private static final double STEAM_PER_MW = 1.6;             // mB/t of plasma steam per MW extracted
    private static final double DD_POWER_FACTOR = 0.3;          // per mB of D, versus D-T's two mB
    private static final double DD_DIRECT_TRITIUM = 0.25;       // T made per mB of D in the D-D branch
    private static final double MAX_TBR = 1.6;

    private double plasmaTemperature = AMBIENT_TEMP;
    private double instability;
    private double confinement;
    private double fusionPower;
    private double burnRate;
    private double tritiumBreedingRatio;
    private Mode mode = Mode.COLD;

    public enum Mode { COLD, DT, DD }

    /** What the structure and tanks offer this tick. */
    public record Inputs(
            double coilCoverage,
            double magnetDrive,
            boolean solenoid,
            int plasmaVolume,
            double blanketFraction,
            boolean fuelEnabled,
            double deuterium,
            double tritium,
            double coolant,
            double steamCapacity) {}

    /** What the controller must move between its tanks after the tick. */
    public record Outputs(double deuteriumUsed, double tritiumUsed, double tritiumBred, double steamProduced) {
        static final Outputs NONE = new Outputs(0, 0, 0, 0);
    }

    public Outputs tick(Inputs in) {
        confinement = in.solenoid() ? clamp(in.coilCoverage() * in.magnetDrive(), 0, 1) : 0.0;
        tritiumBreedingRatio = Math.min(MAX_TBR, 1.5 * in.blanketFraction() / 0.4);

        double auxFade = clamp((AUX_FADE_END - plasmaTemperature) / (AUX_FADE_END - IGNITION_TEMP), 0, 1);
        double heating = AUX_HEATING * in.magnetDrive() * auxFade;
        double losses = (plasmaTemperature - AMBIENT_TEMP) * LOSS_COEFFICIENT / (confinement + 0.05);

        Outputs outputs = Outputs.NONE;
        fusionPower = 0;
        burnRate = 0;
        mode = Mode.COLD;
        if (in.fuelEnabled() && confinement >= MIN_BURN_CONFINEMENT && plasmaTemperature >= IGNITION_TEMP
                && in.plasmaVolume() > 0 && in.deuterium() > 0) {
            double heat = clamp((plasmaTemperature - IGNITION_TEMP) / FULL_BURN_SPAN, 0.05, 1);
            double potential = Config.deuteriumTritiumBurnRate * in.plasmaVolume() * heat;
            double steamWanted;
            double tritiumBred;
            double tritiumUsed;
            double deuteriumUsed;
            if (in.tritium() > 0) {
                mode = Mode.DT;
                burnRate = Math.min(potential, Math.min(in.deuterium(), in.tritium()));
                deuteriumUsed = burnRate;
                tritiumUsed = burnRate;
                fusionPower = burnRate * POWER_PER_MB;
                tritiumBred = burnRate * tritiumBreedingRatio;
            } else {
                mode = Mode.DD;
                burnRate = Math.min(potential, in.deuterium());
                deuteriumUsed = burnRate;
                tritiumUsed = 0;
                fusionPower = burnRate * POWER_PER_MB * DD_POWER_FACTOR;
                // Half the D-D reactions make tritium directly; the other half's neutrons can be bred.
                tritiumBred = burnRate * DD_DIRECT_TRITIUM + burnRate * 0.25 * tritiumBreedingRatio;
            }
            steamWanted = (1 - PLASMA_HEATING_SHARE) * fusionPower * STEAM_PER_MW;
            double steam = Math.max(0, Math.min(steamWanted, Math.min(in.coolant(), in.steamCapacity())));
            double extracted = steamWanted <= 0 ? 1 : steam / steamWanted;
            double powerDensity = fusionPower / in.plasmaVolume();
            heating += SELF_HEATING * powerDensity * (PLASMA_HEATING_SHARE + (1 - PLASMA_HEATING_SHARE) * (1 - extracted));
            outputs = new Outputs(deuteriumUsed, tritiumUsed, tritiumBred, steam);
        }

        plasmaTemperature = clamp(plasmaTemperature + heating - losses, AMBIENT_TEMP, MAX_TEMP);

        if (plasmaTemperature > DANGER_TEMP) {
            instability += (plasmaTemperature - DANGER_TEMP) / 100.0;
        } else if (plasmaTemperature >= IGNITION_TEMP && confinement < STABLE_CONFINEMENT) {
            instability += (STABLE_CONFINEMENT - confinement) * 4.0;
        } else {
            instability = Math.max(0, instability - 0.5);
        }
        instability = Math.min(DISRUPTION_INSTABILITY, instability);
        return outputs;
    }

    /** The plasma is lost: everything resets to cold. */
    public void quench() {
        plasmaTemperature = AMBIENT_TEMP;
        instability = 0;
        fusionPower = 0;
        burnRate = 0;
        mode = Mode.COLD;
    }

    public boolean isDisrupting() {
        return instability >= DISRUPTION_INSTABILITY;
    }

    public boolean isBurning() {
        return mode != Mode.COLD;
    }

    public double getPlasmaTemperature() {
        return plasmaTemperature;
    }

    public double getInstability() {
        return instability;
    }

    public double getConfinement() {
        return confinement;
    }

    public double getFusionPower() {
        return fusionPower;
    }

    public double getBurnRate() {
        return burnRate;
    }

    public double getTritiumBreedingRatio() {
        return tritiumBreedingRatio;
    }

    public Mode getMode() {
        return mode;
    }

    public void serializeNBT(CompoundTag tag) {
        tag.putDouble("plasmaTemperature", plasmaTemperature);
        tag.putDouble("instability", instability);
        tag.putDouble("confinement", confinement);
        tag.putDouble("fusionPower", fusionPower);
        tag.putDouble("burnRate", burnRate);
        tag.putDouble("tbr", tritiumBreedingRatio);
        tag.putString("mode", mode.name());
    }

    public void deserializeNBT(CompoundTag tag) {
        plasmaTemperature = clamp(tag.contains("plasmaTemperature") ? tag.getDouble("plasmaTemperature") : AMBIENT_TEMP, AMBIENT_TEMP, MAX_TEMP);
        instability = clamp(tag.getDouble("instability"), 0, DISRUPTION_INSTABILITY - 1);
        confinement = clamp(tag.getDouble("confinement"), 0, 1);
        fusionPower = Math.max(0, tag.getDouble("fusionPower"));
        burnRate = Math.max(0, tag.getDouble("burnRate"));
        tritiumBreedingRatio = Math.max(0, tag.getDouble("tbr"));
        try {
            mode = Mode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = Mode.COLD;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
