package org.papiricoh.create_nuclearindustry.missile.defense;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.physics.InterceptSolution;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarTrack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Missile defence on the server: which tracked missiles to engage and with which launchers, the course each interceptor flies and
 * its chance to kill, and the launch itself. Batteries, launch controllers and command tables all go through here.
 */
public final class DefenseCommand {
    private DefenseCommand() {
    }

    /**
     * Engages, from {@code shooters}, the missiles in {@code tracks} that threaten {@code settings}' zone and are not {@code own},
     * as the doctrine asks. Returns how many orders were given.
     */
    public static int autoEngage(ServerLevel level, Collection<RadarTrack> tracks, List<? extends InterceptorShooter> shooters,
                                 DefenseSettings settings, Predicate<Flight> own, EngagementSource source) {
        return 0;
    }

    /** Engages flight {@code target}, which must be among {@code tracks}, from the best of {@code shooters}. */
    public static EngageResult manualEngage(ServerLevel level, Collection<RadarTrack> tracks, List<? extends InterceptorShooter> shooters,
                                            UUID target, Doctrine doctrine, Predicate<Flight> own) {
        return EngageResult.NO_SHOOTER;
    }

    /** The earliest course from {@code shooter} to {@code target} launched {@code delayTicks} from now, if it has one. */
    public static Optional<InterceptSolution> solve(ServerLevel level, Flight target, InterceptorShooter shooter, int delayTicks) {
        return Optional.empty();
    }

    /** The chance an interceptor from {@code shooter} flying {@code solution} destroys {@code target}. */
    public static double pk(ServerLevel level, Flight target, InterceptorShooter shooter, InterceptSolution solution) {
        return 0;
    }

    /** Launches an interceptor of {@code design} from {@code shooter} along {@code solution} at {@code target}. */
    @Nullable
    public static Flight launch(ServerLevel level, InterceptorShooter shooter, Flight target, InterceptSolution solution,
                                MissileDesign design, RocketShape shape, @Nullable CompoundTag cargo) {
        return null;
    }
}
