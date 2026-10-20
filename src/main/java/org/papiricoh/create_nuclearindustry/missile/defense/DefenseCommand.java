package org.papiricoh.create_nuclearindustry.missile.defense;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.RocketShape;
import org.papiricoh.create_nuclearindustry.missile.physics.InterceptSolution;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarTrack;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.physics.KillProbability;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.TargetArc;
import org.papiricoh.create_nuclearindustry.missile.physics.InterceptSolver;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarNetwork;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
        if (!DefenseConfig.enabled) return 0;
        FlightTracker tracker = FlightTracker.get(level);
        double radius = settings.zoneRadius() == 0 ? shooters.stream()
                .mapToDouble(shooter -> shooter.envelope(MissileConfig.flightParams()).maxRange()).max().orElse(0) : settings.zoneRadius();
        List<EngagementPlanner.Threat> threats = tracks.stream().filter(track -> {
            Flight flight = tracker.get(track.id());
            return flight != null && flight.kind() == Flight.Kind.MISSILE
                    && TargetArc.of(flight.flight(), MissileConfig.flightParams().gravity()) != null
                    && level.getGameTime() - track.firstSeenTick() >= DefenseConfig.trackMinSeconds * 20L
                    && (DefenseConfig.engageOwnMissiles || !own.test(flight))
                    && track.threatens(settings.zoneX(), settings.zoneZ(), radius);
        }).map(track -> threat(level, tracker.get(track.id())))
                .sorted(Comparator.comparingDouble(EngagementPlanner.Threat::remaining)).limit(8).toList();
        return orders(level, shooters, threats, settings.doctrine(), source);
    }

    /** Engages flight {@code target}, which must be among {@code tracks}, from the best of {@code shooters}. */
    public static EngageResult manualEngage(ServerLevel level, Collection<RadarTrack> tracks, List<? extends InterceptorShooter> shooters,
                                            UUID target, Doctrine doctrine, Predicate<Flight> own) {
        if (!DefenseConfig.enabled) return EngageResult.NO_SHOOTER;
        if (tracks.stream().noneMatch(track -> track.id().equals(target))) return EngageResult.NOT_TRACKED;
        Flight flight = FlightTracker.get(level).get(target);
        if (flight == null || flight.kind() != Flight.Kind.MISSILE || flight.flight().impacted()) return EngageResult.GONE;
        if (!DefenseConfig.engageOwnMissiles && own.test(flight)) return EngageResult.FRIENDLY;
        EngagementPlanner.Threat threat = threat(level, flight);
        if (threat.inFlight() + threat.reserved() >= doctrine.volley()) return EngageResult.ALREADY_ENGAGED;
        if (shooters.stream().noneMatch(InterceptorShooter::operational)) return EngageResult.NO_SHOOTER;
        if (shooters.stream().noneMatch(shooter -> shooter.operational() && shooter.readyRounds() > 0)) return EngageResult.NO_AMMO;
        EngagementPlanner.Threat available = new EngagementPlanner.Threat(target, threat.remaining(), 0, 0);
        Doctrine remaining = doctrine.volley() - threat.inFlight() - threat.reserved() == 1 ? Doctrine.SHOOT_LOOK_SHOOT : doctrine;
        return orders(level, shooters, List.of(available), remaining, EngagementSource.MANUAL_TABLE) > 0
                ? EngageResult.QUEUED : EngageResult.NO_SOLUTION;
    }

    /** The earliest course from {@code shooter} to {@code target} launched {@code delayTicks} from now, if it has one. */
    public static Optional<InterceptSolution> solve(ServerLevel level, Flight target, InterceptorShooter shooter, int delayTicks) {
        var params = MissileConfig.flightParams();
        TargetArc arc = TargetArc.of(target.flight(), params.gravity());
        if (arc == null || target.kind() != Flight.Kind.MISSILE) return Optional.empty();
        return InterceptSolver.solve(arc, shooter.launchPoint(), shooter.envelope(params), params, delayTicks, DefenseConfig.solverStepTicks);
    }

    /** The chance an interceptor from {@code shooter} flying {@code solution} destroys {@code target}. */
    public static double pk(ServerLevel level, Flight target, InterceptorShooter shooter, InterceptSolution solution) {
        var params = MissileConfig.flightParams();
        var env = shooter.envelope(params);
        MissileFlight.Phase phase = solution.terminal() ? MissileFlight.Phase.TERMINAL
                : solution.point().y() > params.atmosphereTop() ? MissileFlight.Phase.MIDCOURSE : MissileFlight.Phase.REENTRY;
        double closing = -solution.arrivalVelocity().normalize().dot(solution.targetVelocity().normalize());
        RadarTrack track = RadarNetwork.tracks(level).get(target.id());
        double age = track == null ? 0 : Math.max(0, level.getGameTime() - track.firstSeenTick()) / 20.0;
        return KillProbability.of(new KillProbability.Inputs(env.basePk(), phase, closing,
                env.maxRange() > 0 ? solution.groundRange() / env.maxRange() : 1,
                RadarNetwork.watchers(level, target.id()), age, shooter.seekerFactor(),
                shooter.shooterType() == ShooterType.BATTERY ? DefenseConfig.batteryTerminalPkFactor : 1,
                DefenseConfig.pkScale, DefenseConfig.maxPk));
    }

    /** Launches an interceptor of {@code design} from {@code shooter} along {@code solution} at {@code target}. */
    @Nullable
    public static Flight launch(ServerLevel level, InterceptorShooter shooter, Flight target, InterceptSolution solution,
                                MissileDesign design, RocketShape shape, @Nullable CompoundTag cargo) {
        if (!DefenseConfig.enabled) return null;
        Flight.Intercept intercept = new Flight.Intercept(target.id(), pk(level, target, shooter, solution), solution.point(),
                solution.arrivalTicks(), shooter.shooterPos(), shooter.shooterType(), level.random.nextLong());
        Flight flight = FlightTracker.get(level).launchInterceptor(level, UUID.randomUUID(), design, Flight.vec(shooter.launchPoint()),
                Flight.vec(solution.ejectVelocity()), shooter.envelope(MissileConfig.flightParams()).rise(), intercept,
                shooter.shooterOwner(), shape, cargo);
        EngagementBoard.release(level, target.id(), shooter.shooterPos());
        return flight;
    }

    private static EngagementPlanner.Threat threat(ServerLevel level, Flight flight) {
        FlightTracker tracker = FlightTracker.get(level);
        return new EngagementPlanner.Threat(flight.id(), flight.flight().ticksToImpact(MissileConfig.flightParams()),
                tracker.interceptorsToward(flight.id()), EngagementBoard.reserved(level, flight.id()));
    }

    private static int orders(ServerLevel level, List<? extends InterceptorShooter> shooters,
                              List<EngagementPlanner.Threat> threats, Doctrine doctrine, EngagementSource source) {
        List<EngagementPlanner.Shooter> ready = new ArrayList<>();
        for (int i = 0; i < shooters.size(); i++) {
            InterceptorShooter shooter = shooters.get(i);
            if (shooter.operational() && shooter.readyRounds() > 0)
                ready.add(new EngagementPlanner.Shooter(i, shooter.readyRounds()));
        }
        var plan = EngagementPlanner.plan(threats, ready, doctrine, (threat, entry) -> {
            Flight target = FlightTracker.get(level).get(threat.id());
            InterceptorShooter shooter = shooters.get(entry.index());
            V3 direction = target.flight().position().sub(shooter.launchPoint()).normalize();
            return solve(level, target, shooter, shooter.reactionTicks(direction))
                    .map(solution -> solution.arrivalTicks() + solution.delayTicks()).orElse(-1);
        });
        int count = 0;
        for (var assignment : plan) {
            InterceptorShooter shooter = shooters.get(assignment.shooter());
            FireResult result = shooter.order(level, assignment.threat(), source);
            // A queued order holds its target until it launches; one already launched counts as in flight.
            if (result == FireResult.QUEUED)
                EngagementBoard.reserve(level, assignment.threat(), shooter.shooterPos(), level.getGameTime() + DefenseConfig.orderExpireTicks);
            if (result == FireResult.QUEUED || result == FireResult.LAUNCHED) count++;
        }
        return count;
    }
}
