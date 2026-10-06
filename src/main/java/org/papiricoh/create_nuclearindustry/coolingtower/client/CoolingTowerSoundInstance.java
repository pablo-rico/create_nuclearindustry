package org.papiricoh.create_nuclearindustry.coolingtower.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;

/** The rush of falling water inside a working tower; louder under load. */
class CoolingTowerSoundInstance extends AbstractTickableSoundInstance {
    private final CoolingTowerControllerBlockEntity tower;

    CoolingTowerSoundInstance(CoolingTowerControllerBlockEntity tower, double x, double y, double z) {
        super(SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, RandomSource.create());
        this.tower = tower;
        this.looping = true;
        this.delay = 0;
        this.x = x;
        this.y = y;
        this.z = z;
        update();
    }

    @Override
    public void tick() {
        if (tower.isRemoved() || !CoolingTowerClientEffects.isRunning(tower)) {
            stop();
            return;
        }
        update();
    }

    private void update() {
        float load = tower.getDisplay().load();
        volume = 0.2f + 0.6f * load;
        pitch = 0.6f + 0.2f * load;
    }

    void end() {
        stop();
    }
}
