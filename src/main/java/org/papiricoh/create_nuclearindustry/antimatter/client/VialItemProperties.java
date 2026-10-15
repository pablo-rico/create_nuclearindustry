package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.minecraft.Util;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;

/**
 * The vial's item model switches: {@code fill} picks the empty, third, two-thirds or full ampoule,
 * and {@code low_battery} blinks the full-bright warning layer twice a second while a loaded vial's
 * battery is low. Wall-clock blinking, so it also blinks in menus where there is no level.
 */
public final class VialItemProperties {
    public static final ResourceLocation FILL = id("fill");
    public static final ResourceLocation LOW_BATTERY = id("low_battery");
    private static final long BLINK_MS = 250;

    private VialItemProperties() {}

    /** Must run on the main thread after items are registered (client setup's enqueueWork). */
    public static void register() {
        AntimatterVialItem vial = AntimatterRegistry.ANTIMATTER_VIAL.get();
        ItemProperties.register(vial, FILL, (stack, level, entity, seed) ->
                VialBattery.fillProperty(AntimatterConfig.vial(), AntimatterVialItem.antimatter(stack)));
        ItemProperties.register(vial, LOW_BATTERY, (stack, level, entity, seed) -> {
            VialBattery.Params p = AntimatterConfig.vial();
            boolean low = VialBattery.lowBattery(p, AntimatterVialItem.charge(stack), AntimatterVialItem.antimatter(stack));
            return low && (Util.getMillis() / BLINK_MS) % 2 == 0 ? 1 : 0;
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
