package org.papiricoh.create_nuclearindustry.reactor.visual.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorSoundModel;

/**
 * A looping sound of a fission reactor, tied to its controller: the core's hum (following power),
 * the klaxon after a SCRAM, the over-temperature alarm, steam hissing or coolant bubbling at an
 * output port, and a lifted relief valve. The core sounds play from the middle of the core, the
 * alarms from the controller, and the port sounds from their port.
 */
final class ReactorLoopSound extends MachineLoopSound<ReactorBlockEntity> {

    enum Kind {
        HUM,
        KLAXON,
        ALARM,
        HISS,
        BUBBLING,
        RELIEF
    }

    private final Kind kind;
    @Nullable
    private final BlockPos at;

    ReactorLoopSound(Kind kind, ReactorBlockEntity reactor, @Nullable BlockPos at) {
        super(switch (kind) {
            case HUM -> AllNuclearSounds.REACTOR_HUM.get();
            case KLAXON -> AllNuclearSounds.REACTOR_KLAXON.get();
            case ALARM -> AllNuclearSounds.REACTOR_OVERTEMP_ALARM.get();
            case HISS, RELIEF -> AllNuclearSounds.STEAM_HISS.get();
            case BUBBLING -> AllNuclearSounds.COOLANT_BUBBLING.get();
        }, reactor);
        this.kind = kind;
        this.at = at == null ? null : at.immutable();
        // The constructor placed the sound before the port was known.
        Vec3 position = position();
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    Kind kind() {
        return kind;
    }

    @Override
    protected boolean wanted() {
        return ReactorClientEffects.wants(machine, kind, at);
    }

    @Override
    protected float targetVolume() {
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = machine.getDisplaySnapshot();
        return switch (kind) {
            case HUM -> ReactorSoundModel.humVolume(ReactorSoundModel.powerFraction(snapshot.powerOutput()));
            case KLAXON -> 1.0f;
            case ALARM -> ReactorSoundModel.alarmVolume(snapshot.coreTemperature());
            case HISS -> ReactorSoundModel.hissVolume(ReactorClientEffects.steamFlowPerPort(machine));
            case BUBBLING -> ReactorSoundModel.bubblingVolume(ReactorClientEffects.primaryFlowPerPort(machine));
            case RELIEF -> ReactorSoundModel.RELIEF_VOLUME;
        };
    }

    @Override
    protected float targetPitch() {
        ReactorBlockEntity.ReactorDisplaySnapshot snapshot = machine.getDisplaySnapshot();
        return switch (kind) {
            case HUM -> ReactorSoundModel.humPitch(ReactorSoundModel.powerFraction(snapshot.powerOutput()));
            case KLAXON -> 1.0f;
            case ALARM -> ReactorSoundModel.alarmPitch(snapshot.coreTemperature());
            case HISS -> ReactorSoundModel.hissPitch(ReactorClientEffects.steamFlowPerPort(machine));
            case BUBBLING -> ReactorSoundModel.bubblingPitch(ReactorClientEffects.primaryFlowPerPort(machine));
            case RELIEF -> ReactorSoundModel.RELIEF_PITCH;
        };
    }

    @Override
    protected Vec3 position() {
        if (at != null) {
            return at.getCenter();
        }
        if (kind == Kind.HUM && machine != null) {
            return machine.radiationOrigin();
        }
        return machine == null ? Vec3.ZERO : machine.getBlockPos().getCenter();
    }
}
