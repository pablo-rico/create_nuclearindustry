package org.papiricoh.create_nuclearindustry.explosive.conventional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;

/**
 * Posted on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} when a conventional warhead, a thermobaric charge or a
 * bomblet goes off, before anything of it happens. Cancelling it stops the blast altogether. Seismic detectors and waste
 * containers listen for it as for nuclear detonations.
 */
public class ConventionalDetonationEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final Vec3 centre;
    private final ConventionalProfile profile;
    private final DetonationCause cause;

    public ConventionalDetonationEvent(ServerLevel level, Vec3 centre, ConventionalProfile profile, DetonationCause cause) {
        this.level = level;
        this.centre = centre;
        this.profile = profile;
        this.cause = cause;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public Vec3 getCentre() {
        return centre;
    }

    /** How far each of its effects reaches. */
    public ConventionalProfile getProfile() {
        return profile;
    }

    /** Who set it off, if anyone. */
    public DetonationCause getCause() {
        return cause;
    }
}
