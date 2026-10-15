package org.papiricoh.create_nuclearindustry.antimatter.client;

import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;

/**
 * The annihilation drone of a burning generator: a low murmur on the standby burn, louder and higher
 * the more it gives ({@link AnnihilationModel#droneVolume}, {@link AnnihilationModel#dronePitch}),
 * stuttering when its supply runs short.
 */
final class GeneratorDroneSound extends MachineLoopSound<AnnihilationGeneratorBlockEntity> {

    GeneratorDroneSound(AnnihilationGeneratorBlockEntity generator) {
        super(AllNuclearSounds.ANNIHILATION_DRONE.get(), generator);
    }

    static boolean wants(AnnihilationGeneratorBlockEntity generator) {
        return !generator.isRemoved() && generator.burning();
    }

    @Override
    protected boolean wanted() {
        return wants(machine);
    }

    @Override
    protected float targetVolume() {
        float volume = 0.8f * AnnihilationModel.droneVolume(machine.burning(), machine.outputFraction());
        if (machine.shortSupply() && machine.getLevel() != null) {
            volume *= 0.7f + 0.3f * (float) Math.sin(machine.getLevel().getGameTime() * 1.3);
        }
        return Math.max(0, Math.min(1, volume));
    }

    @Override
    protected float targetPitch() {
        return Math.max(0.5f, Math.min(2.0f, AnnihilationModel.dronePitch(machine.outputFraction())));
    }

    @Override
    protected Vec3 position() {
        return machine == null ? Vec3.ZERO : machine.centre();
    }
}
