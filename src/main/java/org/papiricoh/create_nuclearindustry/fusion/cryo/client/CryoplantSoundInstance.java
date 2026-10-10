package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantControllerBlockEntity;

/** The compressors' hum, looped while the plant runs; louder and a little higher when it runs fast. */
class CryoplantSoundInstance extends AbstractTickableSoundInstance {
    private final CryoplantControllerBlockEntity plant;

    CryoplantSoundInstance(CryoplantControllerBlockEntity plant, double x, double y, double z) {
        super(AllNuclearSounds.CRYOPLANT_HUM.get(), SoundSource.BLOCKS, RandomSource.create());
        this.plant = plant;
        this.looping = true;
        this.delay = 0;
        this.x = x;
        this.y = y;
        this.z = z;
        update();
    }

    @Override
    public void tick() {
        if (plant.isRemoved() || !plant.isRunning()) {
            stop();
            return;
        }
        update();
    }

    private void update() {
        float speed = (float) Math.max(0.0, Math.min(1.0, plant.getSpeed()));
        volume = 0.25f + 0.45f * speed;
        pitch = 0.75f + 0.35f * speed;
    }

    void end() {
        stop();
    }
}
