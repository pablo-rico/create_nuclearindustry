package org.papiricoh.create_nuclearindustry.civildefense.siren;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A siren block entity the client plays the looping siren sound for. Both kinds read
 * {@link #isSounding()} from the block state, so clients need no extra sync to start the sound.
 */
public interface SoundingSiren {

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    boolean isRemoved();

    boolean isSounding();

    /** Pitch of the tone {@code ticks} into the siren's cycle (game time plus partial ticks). */
    float pitch(double ticks);

    /** Client-side sound per siren, installed by the client setup so this common code never touches client classes. */
    final class ClientHooks {
        private static Consumer<SoundingSiren> ticker = siren -> {};

        private ClientHooks() {}

        public static void install(Consumer<SoundingSiren> clientTicker) {
            ticker = clientTicker;
        }

        static void tick(SoundingSiren siren) {
            ticker.accept(siren);
        }
    }
}
