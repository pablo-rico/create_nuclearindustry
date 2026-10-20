package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.radar.*;
import java.util.Optional;
import java.util.UUID;

/** Manual satellite engagements; automatic missile defence never enters this path. */
public final class AsatCommand {
    private AsatCommand() { }

    public static Optional<InterceptSolution> solve(ServerLevel level, Satellite target, InterceptorShooter shooter, int delay) {
        if (target.orbit().geostationary() || target.orbit().altitude() > OrbitConfig.asatCeiling) return Optional.empty();
        var params = MissileConfig.flightParams();
        var env = shooter.envelope(params);
        // Match the actual kill vehicle launched by a silo, including its payload mass.
        var design = InterceptorDesigns.killVehicle(env.design(), DefenseConfig.siloKillVehicleMass);
        var capped = new InterceptorEnvelope(design, env.rise(), env.ejectSpeed(), env.maxRange(), env.minY(),
                Math.min(env.maxY(), Orbit.SEA_LEVEL + OrbitConfig.asatCeiling), env.exoatmospheric(), env.basePk(), env.minPitch(), env.maxFlightTicks());
        return InterceptSolver.solve(new OrbitPath(target.orbit(), level.getGameTime(), env.maxFlightTicks()),
                shooter.launchPoint(), capped, params, delay, DefenseConfig.solverStepTicks);
    }

    public static double pk(ServerLevel level, Satellite target, String code, InterceptorShooter shooter, InterceptSolution solution) {
        long now = level.getGameTime();
        int radars = 0;
        for (RadarBlockEntity radar : RadarNetwork.radars(level)) {
            if (!radar.operational() || !radar.code().equals(code)) continue;
            double reach = radar.reach();
            reach = reach <= 0 ? Double.POSITIVE_INFINITY : reach * Math.pow(RadarModel.signature(MissileFlight.Phase.MIDCOURSE), .25);
            if (RadarModel.detects(radar.antenna(), target.orbit().position(now), reach, MissileConfig.radarEarthRadius)) radars++;
        }
        var env = shooter.envelope(MissileConfig.flightParams());
        return KillProbability.of(new KillProbability.Inputs(OrbitConfig.satelliteBasePk, MissileFlight.Phase.MIDCOURSE,
                -solution.arrivalVelocity().normalize().dot(solution.targetVelocity().normalize()),
                env.maxRange() > 0 ? solution.groundRange() / env.maxRange() : 1, Math.max(1, radars),
                Math.max(0, now - target.launchedAt()) / 20.0, shooter.seekerFactor(), 1, DefenseConfig.pkScale, DefenseConfig.maxPk));
    }

    @Nullable
    public static Flight launch(ServerLevel level, InterceptorShooter shooter, Satellite target, String code, InterceptSolution solution,
                                MissileDesign design, RocketShape shape, @Nullable CompoundTag cargo) {
        if (!DefenseConfig.enabled) return null;
        var intercept = new Flight.Intercept(target.id(), pk(level, target, code, shooter, solution), solution.point(),
                solution.arrivalTicks(), shooter.shooterPos(), shooter.shooterType(), level.random.nextLong());
        Flight flight = FlightTracker.get(level).launchInterceptor(level, UUID.randomUUID(), design, Flight.vec(shooter.launchPoint()),
                Flight.vec(solution.ejectVelocity()), shooter.envelope(MissileConfig.flightParams()).rise(), intercept,
                shooter.shooterOwner(), shape, cargo);
        EngagementBoard.release(level, target.id(), shooter.shooterPos());
        return flight;
    }
}
