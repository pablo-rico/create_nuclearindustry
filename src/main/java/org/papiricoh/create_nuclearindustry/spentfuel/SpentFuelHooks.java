package org.papiricoh.create_nuclearindustry.spentfuel;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Where the reactor hands over spent fuel, so the spent fuel area can stamp it. */
public final class SpentFuelHooks {

    private SpentFuelHooks() {}

    /** Called for each spent assembly as it leaves a reactor core, before it goes to the output slots. */
    public static void onDischarge(ItemStack spent, @Nullable Level level) {}
}
