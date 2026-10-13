package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

/**
 * A looping tokamak sound tied to its controller: the magnets' hum, whose pitch and loudness follow
 * the field, or the plasma's roar while it is lit. It sounds from the point of the ring nearest the
 * listener, fades in and out instead of cutting, and stops for good when the controller goes away
 * or the readings no longer call for it.
 */
final class TokamakLoopSound extends MachineLoopSound<FusionReactorBlockEntity> {

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

    TokamakLoopSound(Kind kind, FusionReactorBlockEntity controller) {
        super(kind == Kind.HUM ? AllNuclearSounds.MAGNET_HUM.get() : AllNuclearSounds.PLASMA_RUMBLE.get(), controller);
        this.kind = kind;
    }

    @Override
    protected boolean wanted() {
        return kind.wants(machine.readings());
    }

    @Override
    protected float targetVolume() {
        TokamakReadings readings = machine.readings();
        if (kind == Kind.HUM) {
            return (float) (0.25 + 0.6 * field(readings));
        }
        return (float) (0.35 + 0.65 * heat(readings));
    }

    @Override
    protected float targetPitch() {
        TokamakReadings readings = machine.readings();
        if (kind == Kind.HUM) {
            return (float) (0.6 + 0.6 * field(readings));
        }
        double wobble = Math.max(0, Math.min(100, readings.instability())) / 100.0 * 0.12
                * Math.sin(machine.getLevel().getGameTime() * 0.9);
        return (float) Math.max(0.5, 0.75 + 0.35 * heat(readings) + wobble);
    }

    @Override
    protected Vec3 position() {
        // Follows the listener along the ring.
        Minecraft minecraft = Minecraft.getInstance();
        int radius = machine.getMajorRadius();
        return minecraft.player == null || radius <= 0
                ? machine.getBlockPos().getCenter()
                : TokamakClientEffects.nearestRingPoint(machine.getCenter(), radius, minecraft.player.position());
    }

    private static double field(TokamakReadings readings) {
        return Math.max(0, Math.min(1, readings.field()));
    }

    private static double heat(TokamakReadings readings) {
        return Math.max(0, Math.min(1, readings.temperature() / 4000.0));
    }
}
