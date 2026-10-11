package org.papiricoh.create_nuclearindustry.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A looping machine sound tied to a block entity: it fades in while the machine wants it, follows
 * the machine's volume and pitch, fades out instead of cutting, and stops for good once the block
 * entity goes away or no longer calls for it. Subclasses say when the sound is wanted, how loud and
 * how high it is, and where it sounds from.
 */
public abstract class MachineLoopSound<T extends BlockEntity> extends AbstractTickableSoundInstance {
    private static final float FADE_STEP = 0.05f;

    protected final T machine;
    private float fade;

    protected MachineLoopSound(SoundEvent sound, T machine) {
        super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.machine = machine;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        // Start silent and fade in. The engine fixes the range from the starting volume (at least the
        // sounds.json attenuation distance, 16 blocks).
        this.volume = 0.01f;
        this.pitch = 1.0f;
        place();
    }

    /** Whether the machine still calls for this sound. */
    protected abstract boolean wanted();

    /** The volume at full fade, 0..1. */
    protected abstract float targetVolume();

    /** The pitch, 0.5..2. */
    protected abstract float targetPitch();

    /** Where the sound comes from; the block's centre by default. */
    protected Vec3 position() {
        return machine.getBlockPos().getCenter();
    }

    @Override
    public void tick() {
        if (machine.isRemoved() || machine.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        boolean wanted = wanted();
        fade = Math.max(0, Math.min(1, fade + (wanted ? FADE_STEP : -FADE_STEP)));
        if (!wanted && fade <= 0) {
            stop();
            return;
        }
        volume = Math.max(0.01f, fade * targetVolume());
        pitch = targetPitch();
        place();
    }

    private void place() {
        Vec3 at = position();
        x = at.x;
        y = at.y;
        z = at.z;
    }
}
