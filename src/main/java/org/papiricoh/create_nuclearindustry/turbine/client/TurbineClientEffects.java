package org.papiricoh.create_nuclearindustry.turbine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineDisplay;

import java.util.HashMap;
import java.util.Map;

/** Client tick for turbine controllers: keeps the hum playing and adds the odd steam hiss. */
public final class TurbineClientEffects {
    private static final ResourceLocation STEAM_HISS = ResourceLocation.fromNamespaceAndPath("create", "steam");
    private static final Map<BlockPos, TurbineSoundInstance> HUMS = new HashMap<>();

    private TurbineClientEffects() {}

    static boolean isRunning(AbstractTurbineControllerBlockEntity turbine) {
        TurbineDisplay display = turbine.getDisplay();
        return display.formed() && display.currentUse() > 0;
    }

    public static void tick(AbstractTurbineControllerBlockEntity turbine) {
        Minecraft minecraft = Minecraft.getInstance();
        // Ponder scenes run in their own client level; keep them silent.
        if (turbine.getLevel() != minecraft.level) {
            return;
        }
        BlockPos pos = turbine.getBlockPos();
        TurbineSoundInstance hum = HUMS.get(pos);
        if (!isRunning(turbine)) {
            if (hum != null) {
                hum.end();
                HUMS.remove(pos);
            }
            return;
        }
        AABB box = turbine.getRenderBoundingBox();
        double x = box.getCenter().x;
        double y = box.getCenter().y;
        double z = box.getCenter().z;
        if (hum == null || hum.isStopped() || !minecraft.getSoundManager().isActive(hum)) {
            hum = new TurbineSoundInstance(turbine, x, y, z);
            HUMS.put(pos.immutable(), hum);
            minecraft.getSoundManager().play(hum);
        }
        float load = turbine.getDisplay().load();
        if (turbine.getLevel().random.nextFloat() < 0.01f + 0.03f * load) {
            SoundEvent hiss = BuiltInRegistries.SOUND_EVENT.get(STEAM_HISS);
            if (hiss != null) {
                turbine.getLevel().playLocalSound(x, y, z, hiss, SoundSource.BLOCKS, 0.3f + 0.3f * load,
                        0.8f + 0.2f * turbine.getLevel().random.nextFloat(), false);
            }
        }
    }
}
