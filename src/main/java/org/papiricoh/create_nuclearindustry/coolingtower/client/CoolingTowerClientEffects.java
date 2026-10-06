package org.papiricoh.create_nuclearindustry.coolingtower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerDisplay;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;

import java.util.HashMap;
import java.util.Map;

/** Client tick for cooling towers: keeps the water rush playing while the tower works. */
public final class CoolingTowerClientEffects {
    private static final Map<BlockPos, CoolingTowerSoundInstance> SOUNDS = new HashMap<>();

    private CoolingTowerClientEffects() {}

    static boolean isRunning(CoolingTowerControllerBlockEntity tower) {
        CoolingTowerDisplay display = tower.getDisplay();
        return display.formed() && display.currentUse() > 0;
    }

    public static void tick(CoolingTowerControllerBlockEntity tower) {
        Minecraft minecraft = Minecraft.getInstance();
        // Ponder scenes run in their own client level; keep them silent.
        if (tower.getLevel() != minecraft.level) {
            return;
        }
        BlockPos pos = tower.getBlockPos();
        CoolingTowerSoundInstance sound = SOUNDS.get(pos);
        if (!isRunning(tower)) {
            if (sound != null) {
                sound.end();
                SOUNDS.remove(pos);
            }
            return;
        }
        if (sound == null || sound.isStopped() || !minecraft.getSoundManager().isActive(sound)) {
            CoolingTowerDisplay display = tower.getDisplay();
            BlockPos c = display.center();
            sound = new CoolingTowerSoundInstance(tower, c.getX() + 0.5, c.getY() + 1.5, c.getZ() + 0.5);
            SOUNDS.put(pos.immutable(), sound);
            minecraft.getSoundManager().play(sound);
        }
    }
}
