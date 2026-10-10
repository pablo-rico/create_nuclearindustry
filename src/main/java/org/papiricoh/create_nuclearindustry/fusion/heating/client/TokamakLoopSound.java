package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

/**
 * A looping tokamak sound tied to its controller: the magnets' hum, whose pitch and loudness follow
 * the field, or the plasma's roar while it is lit. It sounds from the point of the ring nearest the
 * listener, fades in and out instead of cutting, and stops for good when the controller goes away
 * or the readings no longer call for it.
 */
final class TokamakLoopSound extends AbstractTickableSoundInstance {
    private static final float FADE_STEP = 0.05f;

    enum Kind {
        HUM,
        RUMBLE;

        boolean wants(TokamakReadings readings) {
            return readings.formed() && !readings.suspended() && switch (this) {
                case HUM -> readings.field() > 0.02;
                case RUMBLE -> readings.phase().hasPlasma();
            };
        }
    }

    private final Kind kind;
    private final FusionReactorBlockEntity controller;
    private float fade;

    TokamakLoopSound(Kind kind, FusionReactorBlockEntity controller) {
        super(kind == Kind.HUM ? AllNuclearSounds.MAGNET_HUM.get() : AllNuclearSounds.PLASMA_RUMBLE.get(),
                SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.kind = kind;
        this.controller = controller;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        // Start silent and fade in. The engine fixes the range from the starting volume (at least the
        // sounds.json attenuation distance, 16 blocks), and the sound follows the listener along the ring.
        this.volume = 0.01f;
        this.pitch = 1.0f;
        place();
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (controller.isRemoved() || controller.getLevel() != minecraft.level) {
            stop();
            return;
        }
        TokamakReadings readings = controller.readings();
        boolean wanted = kind.wants(readings);
        fade = Math.max(0, Math.min(1, fade + (wanted ? FADE_STEP : -FADE_STEP)));
        if (!wanted && fade <= 0) {
            stop();
            return;
        }
        if (kind == Kind.HUM) {
            double field = Math.max(0, Math.min(1, readings.field()));
            volume = Math.max(0.01f, fade * (float) (0.25 + 0.6 * field));
            pitch = (float) (0.6 + 0.6 * field);
        } else {
            double heat = Math.max(0, Math.min(1, readings.temperature() / 4000.0));
            double wobble = Math.max(0, Math.min(100, readings.instability())) / 100.0 * 0.12
                    * Math.sin(controller.getLevel().getGameTime() * 0.9);
            volume = Math.max(0.01f, fade * (float) (0.35 + 0.65 * heat));
            pitch = (float) Math.max(0.5, 0.75 + 0.35 * heat + wobble);
        }
        place();
    }

    private void place() {
        Minecraft minecraft = Minecraft.getInstance();
        int radius = controller.getMajorRadius();
        Vec3 at = minecraft.player == null || radius <= 0
                ? controller.getBlockPos().getCenter()
                : TokamakClientEffects.nearestRingPoint(controller.getCenter(), radius, minecraft.player.position());
        x = at.x;
        y = at.y;
        z = at.z;
    }
}
