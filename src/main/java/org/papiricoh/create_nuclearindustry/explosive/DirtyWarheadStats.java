package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;

/**
 * A radiological ("dirty") warhead: anything radioactive that is not weapons material (a core or the
 * metal for one). It does
 * not split atoms, so the blast is small, but it scatters its load far and wide. The load is the
 * dose rate the charge would give whoever carried it (the radioactivity data map), in rad/s.
 */
public final class DirtyWarheadStats {
    /** The small conventional charge that scatters the load. */
    public static final float BASE_BLAST = 3.0f;
    public static final float MAX_BLAST = 10.0f;
    public static final int MIN_FALLOUT_RADIUS = 24;
    public static final int MAX_FALLOUT_RADIUS = 128;
    /** How long the hot spot at ground zero keeps irradiating, in ticks (an in-game hour is 1000). */
    public static final int HOT_SPOT_TICKS = 72_000;
    public static final float MAX_HOT_SPOT = 200.0f;

    private DirtyWarheadStats() {}

    /** Radioactive and not weapons material: a dirty bomb takes it, a nuclear one does not. */
    public static boolean isRadiological(ItemStack stack) {
        return !stack.isEmpty() && !FissileMaterial.isWeaponsMaterial(stack) && RadiationHandler.stackRate(stack) > 0;
    }

    /** Total radioactivity of the radiological items in {@code from..to}, in rad/s. */
    public static float load(IItemHandler handler, int from, int to) {
        float load = 0;
        for (int slot = from; slot < to; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (isRadiological(stack)) {
                load += (float) RadiationHandler.stackRate(stack);
            }
        }
        return load;
    }

    public static float blastPower(float load) {
        return Math.min(MAX_BLAST, BASE_BLAST + (float) Math.sqrt(Math.max(0, load)) * 0.3f);
    }

    /** How far the load spreads: grows with the square root of the load. */
    public static int falloutRadius(float load) {
        int radius = Math.round(MIN_FALLOUT_RADIUS + (float) Math.sqrt(Math.max(0, load)) * 4.0f);
        return Math.max(MIN_FALLOUT_RADIUS, Math.min(MAX_FALLOUT_RADIUS, radius));
    }

    /** Ground contamination at ground zero, in rad/s. */
    public static float falloutIntensity(float load) {
        return (float) Math.min(Config.contaminationMax, 2.0f + Math.max(0, load) * 0.05f);
    }

    /** Strength of the fading hot spot left at ground zero. */
    public static float hotSpot(float load) {
        return Math.min(MAX_HOT_SPOT, Math.max(0, load) * 0.5f);
    }
}
