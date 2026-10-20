package org.papiricoh.create_nuclearindustry.missile.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;
import org.papiricoh.create_nuclearindustry.missile.physics.InterceptorEnvelope;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.UUID;

/**
 * Something that launches interceptors: an interceptor battery, or a launch controller whose rocket carries a kill vehicle. The
 * engagement code ({@link DefenseCommand}) works only through this.
 */
public interface InterceptorShooter {
    BlockPos shooterPos();

    ShooterType shooterType();

    /** Who it belongs to: its interceptors are theirs, and their missiles are never engaged by it. */
    @Nullable
    UUID shooterOwner();

    /** Formed, loaded, turning and not on a ship: able to launch at all. */
    boolean operational();

    /** Interceptors it could launch now. */
    int readyRounds();

    /** Ticks before it could launch in {@code direction}: turning to it, counting down. */
    int reactionTicks(V3 direction);

    /** Where its interceptor leaves it. */
    V3 launchPoint();

    /** What its interceptor can do. */
    InterceptorEnvelope envelope(BallisticModel.Params params);

    /** Its interceptor's guidance, 0 to 1, for the chance to kill. */
    double seekerFactor();

    /** Orders it to engage flight {@code target}: it solves again and launches once it is ready. */
    FireResult order(ServerLevel level, UUID target, EngagementSource source);
}
