package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;

/**
 * The drone of a Penning trap's field while it is up: louder and a little higher the more it
 * holds, deeper for the bigger traps, wavering when the trap runs short of energy and sliding up and
 * down while it quenches. Sounds from the middle of the cloud.
 */
final class TrapHumSound extends MachineLoopSound<PenningTrapBlockEntity> {

    TrapHumSound(PenningTrapBlockEntity trap) {
        super(AllNuclearSounds.TRAP_HUM.get(), trap);
    }

    static boolean wants(PenningTrapBlockEntity trap) {
        return !trap.isRemoved() && trap.state() != TrapState.OFFLINE && trap.fieldStrength() > 0.05f;
    }

    @Override
    protected boolean wanted() {
        return wants(machine);
    }

    private double fill() {
        double capacity = machine.capacity();
        return capacity > 0 ? Math.cbrt(Math.max(0, Math.min(1, machine.stored() / capacity))) : 0;
    }

    @Override
    protected float targetVolume() {
        float tier = switch (machine.tier()) {
            case SMALL -> 0.35f;
            case MEDIUM -> 0.5f;
            case LARGE -> 0.7f;
        };
        float volume = tier * (0.45f + 0.55f * (float) fill()) * machine.fieldStrength();
        if (machine.state() == TrapState.LOW_POWER && machine.getLevel() != null) {
            volume *= 0.75f + 0.25f * (float) Math.sin(machine.getLevel().getGameTime() * 0.9);
        }
        return Math.min(1, volume * (machine.state() == TrapState.QUENCHING ? 1.3f : 1));
    }

    @Override
    protected float targetPitch() {
        float base = machine.tier() == TrapTier.SMALL ? 1.15f : machine.tier() == TrapTier.MEDIUM ? 1.0f : 0.8f;
        float pitch = base * (0.9f + 0.2f * (float) fill());
        if (machine.state() == TrapState.QUENCHING && machine.getLevel() != null) {
            pitch *= 1.0f + 0.12f * (float) Math.sin(machine.getLevel().getGameTime() * 0.6);
        }
        return Math.max(0.5f, Math.min(2.0f, pitch));
    }

    @Override
    protected Vec3 position() {
        return machine == null ? Vec3.ZERO : machine.centre();
    }
}
