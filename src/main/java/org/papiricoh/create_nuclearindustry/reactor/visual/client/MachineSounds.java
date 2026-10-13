package org.papiricoh.create_nuclearindustry.reactor.visual.client;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearSounds;
import org.papiricoh.create_nuclearindustry.chemistry.electrolyzer.ElectrolyzerBlockEntity;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorSoundModel;
import org.papiricoh.create_nuclearindustry.rtg.RtgBlockEntity;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationMelterBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Running sounds of the fuel cycle's machines (installed on
 * {@link org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks}): the gas
 * centrifuge's whine and the electrolyzer's buzz while they work, following the shaft's speed; the
 * vitrification melter's roar while it melts; and a radioisotope generator's hum while it holds a
 * pellet.
 */
public final class MachineSounds {
    /** Sounds start only this close to the machine, in blocks. */
    private static final double EFFECT_RANGE = 24.0;

    private static final Map<BlockEntity, MachineSound> PLAYING = new WeakHashMap<>();

    private MachineSounds() {}

    public static void tick(BlockEntity machine) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = machine.getLevel();
        if (level == null || level != minecraft.level || minecraft.player == null) {
            return;
        }
        if (machine.getBlockPos().getCenter().distanceToSqr(minecraft.player.position()) > EFFECT_RANGE * EFFECT_RANGE) {
            return;
        }
        SoundEvent event = soundOf(machine);
        if (event == null || !wants(machine)) {
            return;
        }
        if (!ReactorClientEffects.playing(PLAYING.get(machine))) {
            MachineSound sound = new MachineSound(event, machine);
            PLAYING.put(machine, sound);
            minecraft.getSoundManager().play(sound);
        }
    }

    @Nullable
    private static SoundEvent soundOf(BlockEntity machine) {
        if (machine instanceof CentrifugeBlockEntity) {
            return AllNuclearSounds.CENTRIFUGE_WHINE.get();
        }
        if (machine instanceof ElectrolyzerBlockEntity) {
            return AllNuclearSounds.ELECTROLYZER_BUZZ.get();
        }
        if (machine instanceof VitrificationMelterBlockEntity) {
            return AllNuclearSounds.MELTER_ROAR.get();
        }
        if (machine instanceof RtgBlockEntity) {
            return AllNuclearSounds.RTG_HUM.get();
        }
        return null;
    }

    static boolean wants(BlockEntity machine) {
        if (machine instanceof CentrifugeBlockEntity centrifuge) {
            return centrifuge.looksBusy();
        }
        if (machine instanceof ElectrolyzerBlockEntity electrolyzer) {
            return electrolyzer.looksBusy();
        }
        if (machine instanceof VitrificationMelterBlockEntity melter) {
            return melter.isMelting();
        }
        if (machine instanceof RtgBlockEntity rtg) {
            return rtg.getFuel() != null;
        }
        return false;
    }

    /** One machine's running sound. */
    private static final class MachineSound extends MachineLoopSound<BlockEntity> {
        MachineSound(SoundEvent sound, BlockEntity machine) {
            super(sound, machine);
        }

        @Override
        protected boolean wanted() {
            return wants(machine);
        }

        @Override
        protected float targetVolume() {
            if (machine instanceof CentrifugeBlockEntity centrifuge) {
                return ReactorSoundModel.centrifugeVolume(centrifuge.getSpeed());
            }
            if (machine instanceof ElectrolyzerBlockEntity electrolyzer) {
                return ReactorSoundModel.electrolyzerVolume(electrolyzer.getSpeed());
            }
            if (machine instanceof VitrificationMelterBlockEntity melter) {
                return ReactorSoundModel.melterVolume(melter.heatLevel() == HeatLevel.SEETHING);
            }
            if (machine instanceof RtgBlockEntity rtg) {
                return ReactorSoundModel.rtgVolume(rtg.getCharge());
            }
            return 0.5f;
        }

        @Override
        protected float targetPitch() {
            if (machine instanceof CentrifugeBlockEntity centrifuge) {
                return ReactorSoundModel.centrifugePitch(centrifuge.getSpeed());
            }
            if (machine instanceof ElectrolyzerBlockEntity electrolyzer) {
                return ReactorSoundModel.electrolyzerPitch(electrolyzer.getSpeed());
            }
            if (machine instanceof VitrificationMelterBlockEntity melter) {
                return ReactorSoundModel.melterPitch(melter.heatLevel() == HeatLevel.SEETHING);
            }
            if (machine instanceof RtgBlockEntity rtg) {
                return ReactorSoundModel.rtgPitch(rtg.getCharge());
            }
            return 1.0f;
        }
    }
}
