package org.papiricoh.create_nuclearindustry.turbine.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/** A low machine hum that follows the turbine's load, and stops when it does. */
public class TurbineSoundInstance extends AbstractTickableSoundInstance {
    private final AbstractTurbineControllerBlockEntity turbine;

    TurbineSoundInstance(AbstractTurbineControllerBlockEntity turbine, double x, double y, double z) {
        super(SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, RandomSource.create());
        this.turbine = turbine;
        this.looping = true;
        this.delay = 0;
        this.x = x;
        this.y = y;
        this.z = z;
        update();
    }

    @Override
    public void tick() {
        if (turbine.isRemoved() || !TurbineClientEffects.isRunning(turbine)) {
            stop();
            return;
        }
        update();
    }

    private void update() {
        float load = turbine.getDisplay().load();
        float tierShift = turbine.tier() == TurbineTier.PLASMA ? -0.1f : 0f;
        volume = 0.15f + 0.55f * load;
        pitch = Math.max(0.3f, 0.5f + 0.25f * load + tierShift);
    }

    void end() {
        stop();
    }
}
