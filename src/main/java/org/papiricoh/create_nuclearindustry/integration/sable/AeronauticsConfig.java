package org.papiricoh.create_nuclearindustry.integration.sable;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for Create Aeronautics ships, under {@code aeronautics.*}. Only does anything with
 * Sable installed. Where a block on a ship really is (alerts, ranges, explosions centred in the
 * world) is always right; these switches cover what blasts and contamination do to ships, and the
 * nuclear engines. Defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class AeronauticsConfig {
    public static boolean enabled = true;
    public static boolean blastDamagesShips = true;
    public static boolean blastPushesShips = true;
    /** Multiplies the impulse a blast wave gives a ship. */
    public static double blastImpulseScale = 1.0;
    public static boolean contamination = true;
    /** Share of the gap to the contamination around it a ship takes up each check (every 5 s). */
    public static double contaminationUptake = 0.05;
    /** Dose rate aboard a ship per unit of its contamination, as a share of the same contamination on the ground. */
    public static double shipDoseFactor = 1.0;

    private static ModConfigSpec.BooleanValue ENABLED;
    private static ModConfigSpec.BooleanValue BLAST_DAMAGE;
    private static ModConfigSpec.BooleanValue BLAST_PUSH;
    private static ModConfigSpec.DoubleValue BLAST_IMPULSE;
    private static ModConfigSpec.BooleanValue CONTAMINATION;
    private static ModConfigSpec.DoubleValue UPTAKE;
    private static ModConfigSpec.DoubleValue DOSE_FACTOR;

    private AeronauticsConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        ENABLED = builder.comment("Master switch for what nuclear blasts and contamination do to Create Aeronautics ships, and for the",
                        "nuclear engines. Explosions on a ship always happen where the ship really is.")
                .define("aeronautics.enabled", true);
        BLAST_DAMAGE = builder.comment("A nuclear blast tears, wrecks and burns the blocks of ships within its reach, like the terrain.")
                .define("aeronautics.blast.damageShips", true);
        BLAST_PUSH = builder.comment("A blast wave throws ships it reaches, spinning them when it hits them off centre.")
                .define("aeronautics.blast.pushShips", true);
        BLAST_IMPULSE = builder.comment("Multiplies the impulse a blast wave gives a ship.")
                .defineInRange("aeronautics.blast.impulseScale", 1.0, 0.0, 100.0);
        CONTAMINATION = builder.comment("Ships carry their own contamination: picked up flying through contaminated air, from fallout and from",
                        "accidents aboard; it travels and is saved with the ship, decays like the ground's and doses those aboard.")
                .define("aeronautics.contamination.enabled", true);
        UPTAKE = builder.comment("Share of the gap to the contamination around it a ship takes up each check (every 5 seconds).")
                .defineInRange("aeronautics.contamination.uptake", 0.05, 0.0, 1.0);
        DOSE_FACTOR = builder.comment("Dose rate aboard per unit of a ship's contamination, compared with the same contamination on the ground.")
                .defineInRange("aeronautics.contamination.doseFactor", 1.0, 0.0, 100.0);
    }

    public static void bake() {
        enabled = ENABLED.get();
        blastDamagesShips = BLAST_DAMAGE.get();
        blastPushesShips = BLAST_PUSH.get();
        blastImpulseScale = BLAST_IMPULSE.get();
        contamination = CONTAMINATION.get();
        contaminationUptake = UPTAKE.get();
        shipDoseFactor = DOSE_FACTOR.get();
    }

    public static boolean blastDamageOn() {
        return enabled && blastDamagesShips;
    }

    public static boolean blastPushOn() {
        return enabled && blastPushesShips;
    }

    public static boolean contaminationOn() {
        return enabled && contamination;
    }
}
