package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Something in the world that irradiates its surroundings (a running reactor, an RTG).
 * Registered in {@link RadiationEmitters} while loaded.
 */
public interface RadiationEmitter {
    Level getLevel();

    boolean isRemoved();

    Vec3 radiationOrigin();

    /** Dose per second right next to the source, before distance and shielding. */
    double radiationStrength();

    /** Blocks the source's own radiation passes freely, such as a reactor's core. */
    @Nullable
    default BoundingBox radiationIgnoreBox() {
        return null;
    }
}
