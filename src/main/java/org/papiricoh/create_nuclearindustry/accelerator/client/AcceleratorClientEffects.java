package org.papiricoh.create_nuclearindustry.accelerator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamReadings;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The accelerator's sounds on the client: the klystrons' hum while the cavities carry radio-frequency
 * power, and the magnets' ramp whose pitch climbs with the beam energy. Both sound from the point of
 * the beamline nearest the listener.
 */
public final class AcceleratorClientEffects {
    private static final Map<AcceleratorControllerBlockEntity, State> STATES = new WeakHashMap<>();

    private static final class State {
        @Nullable Loop hum;
        @Nullable Loop ramp;
    }

    private AcceleratorClientEffects() {}

    public static void tick(AcceleratorControllerBlockEntity controller) {
        BeamReadings readings = controller.readings();
        State state = STATES.computeIfAbsent(controller, c -> new State());
        Minecraft minecraft = Minecraft.getInstance();
        if (Loop.Kind.HUM.wants(readings) && !playing(state.hum)) {
            state.hum = new Loop(Loop.Kind.HUM, controller);
            minecraft.getSoundManager().play(state.hum);
        }
        if (Loop.Kind.RAMP.wants(readings) && !playing(state.ramp)) {
            state.ramp = new Loop(Loop.Kind.RAMP, controller);
            minecraft.getSoundManager().play(state.ramp);
        }
    }

    private static boolean playing(@Nullable Loop sound) {
        return sound != null && !sound.isStopped() && Minecraft.getInstance().getSoundManager().isActive(sound);
    }

    /** The point of the beamline (lines and ring) closest to {@code from}. */
    static Vec3 nearestBeamPoint(AcceleratorControllerBlockEntity controller, Vec3 from) {
        Vec3 best = controller.getBlockPos().getCenter();
        double bestDistance = best.distanceToSqr(from);
        for (List<BlockPos> line : List.of(controller.injectorPath(), controller.extractionPath())) {
            for (int i = 0; i + 1 < line.size(); i++) {
                Vec3 a = line.get(i).getCenter();
                Vec3 b = line.get(i + 1).getCenter();
                Vec3 ab = b.subtract(a);
                double length = ab.lengthSqr();
                double t = length <= 0 ? 0 : Math.max(0, Math.min(1, from.subtract(a).dot(ab) / length));
                Vec3 p = a.add(ab.scale(t));
                double d = p.distanceToSqr(from);
                if (d < bestDistance) {
                    best = p;
                    bestDistance = d;
                }
            }
        }
        int radius = controller.ringRadius();
        if (radius > 0) {
            Vec3 c = controller.ringCenter().getCenter();
            double dx = from.x - c.x;
            double dz = from.z - c.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            Vec3 p = distance < 1.0e-3 ? c.add(radius, 0, 0) : new Vec3(c.x + dx / distance * radius, c.y, c.z + dz / distance * radius);
            if (p.distanceToSqr(from) < bestDistance) {
                best = p;
            }
        }
        return best;
    }

    /** A looping accelerator sound tied to its controller. */
    static final class Loop extends MachineLoopSound<AcceleratorControllerBlockEntity> {
        enum Kind {
            HUM,
            RAMP;

            boolean wants(BeamReadings readings) {
                return readings.formed() && !readings.suspended() && switch (this) {
                    case HUM -> readings.phase().rfOn();
                    case RAMP -> readings.field() > 0.02;
                };
            }
        }

        private final Kind kind;

        Loop(Kind kind, AcceleratorControllerBlockEntity controller) {
            super(kind == Kind.HUM ? AllNuclearSounds.ACCELERATOR_RF_HUM.get() : AllNuclearSounds.MAGNET_RAMP.get(), controller);
            this.kind = kind;
        }

        @Override
        protected boolean wanted() {
            return kind.wants(machine.readings());
        }

        @Override
        protected float targetVolume() {
            BeamReadings r = machine.readings();
            return kind == Kind.HUM ? (float) (0.3 + 0.5 * Math.min(1, r.rfDrive())) : (float) (0.2 + 0.4 * Math.min(1, r.field()));
        }

        @Override
        protected float targetPitch() {
            BeamReadings r = machine.readings();
            if (kind == Kind.HUM) {
                return (float) (0.8 + 0.3 * Math.min(1, r.rfDrive()));
            }
            double share = r.maxEnergy() <= 0 ? 0 : Math.min(1, r.energy() / r.maxEnergy());
            return (float) (0.6 + 0.9 * share);
        }

        @Override
        protected Vec3 position() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player == null ? machine.getBlockPos().getCenter() : nearestBeamPoint(machine, minecraft.player.position());
        }
    }
}
