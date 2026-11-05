package org.papiricoh.create_nuclearindustry.missile.cluster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.DetonationCause;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalConfig;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxBroadcaster;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState;

import java.util.SplittableRandom;

/** A cluster warhead opening over its target: its casing blows apart and frees its bomblets ({@link ClusterPattern}). */
public final class ClusterDispenser {
    private ClusterDispenser() {}

    /**
     * Opens a warhead of {@code count} bomblets of {@code charge} TNT equivalents each at {@code at}, going {@code velocity}.
     * Each bomblet fails to go off on landing by {@code missile.cluster.dudChance}, and goes off later instead.
     */
    public static void open(ServerLevel level, Vec3 at, Vec3 velocity, int count, float charge, DetonationCause cause) {
        if (count <= 0 || charge <= 0) {
            return;
        }
        at = ShipSpace.toWorld(level, at);
        long seed = level.random.nextLong();
        double height = Math.max(1, NuclearDetonation.burstHeight(level, at));
        double[] v = ClusterPattern.velocities(count, height, ConventionalConfig.clusterFootprintRadius, velocity.x, velocity.y,
                velocity.z, seed);
        SplittableRandom random = new SplittableRandom(seed ^ 0x5DEECE66DL);
        int minTicks = ConventionalConfig.clusterDudMinSeconds * 20;
        int maxTicks = Math.max(minTicks, ConventionalConfig.clusterDudMaxSeconds * 20);
        for (int i = 0; i < count; i++) {
            boolean dud = random.nextDouble() < ConventionalConfig.clusterDudChance;
            int dudTicks = minTicks + (maxTicks > minTicks ? random.nextInt(maxTicks - minTicks + 1) : 0);
            level.addFreshEntity(BombletEntity.create(level, at, new Vec3(v[3 * i], v[3 * i + 1], v[3 * i + 2]), charge, dud,
                    Math.max(1, dudTicks), cause));
        }
        FlightFxBroadcaster.event(level, new java.util.UUID(seed, ~seed), FlightFxEvent.CLUSTER_OPEN, FlightFxState.LIVE_WARHEAD, at, velocity);
    }
}
