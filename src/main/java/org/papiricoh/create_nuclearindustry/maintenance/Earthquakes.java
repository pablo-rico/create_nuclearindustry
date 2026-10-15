package org.papiricoh.create_nuclearindustry.maintenance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.create_nuclearindustry.civildefense.seismic.SeismicNetwork;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.maintenance.physics.EarthquakeModel;
import org.papiricoh.create_nuclearindustry.maintenance.physics.InesScale;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;

import java.util.List;
import java.util.Locale;

/**
 * Natural earthquakes ({@link EarthquakeModel}): now and then, in a natural dimension with players,
 * the ground shakes somewhere near one of them. Players feel it, seismic detectors record it, and
 * the plants it reaches take a transient on their parts; a reactor past its seismic trip scrams.
 * Off with {@code maintenance.earthquakes.enabled}; {@code /nuclear incident trigger earthquake} sets one off.
 */
public final class Earthquakes {
    /** Ground motion players feel as a jolt (nausea and the rumble). */
    private static final double FELT = 0.5;

    private Earthquakes() {}

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !MaintenanceConfig.earthquakesOn()
                || !level.dimensionType().natural()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() || level.random.nextDouble() >= EarthquakeModel.chancePerTick(MaintenanceConfig.earthquakesPerDay)) {
            return;
        }
        ServerPlayer near = players.get(level.random.nextInt(players.size()));
        int range = MaintenanceConfig.earthquakeRange;
        BlockPos epicentre = near.blockPosition().offset(level.random.nextInt(2 * range + 1) - range, 0,
                level.random.nextInt(2 * range + 1) - range);
        double magnitude = EarthquakeModel.magnitude(level.random.nextDouble(), MaintenanceConfig.earthquakeMinMagnitude,
                MaintenanceConfig.earthquakeMaxMagnitude, 1.0);
        strike(level, epicentre, magnitude);
    }

    /** An earthquake of {@code magnitude} at {@code epicentre}. Returns the plants it scrammed. */
    public static int strike(ServerLevel level, BlockPos epicentre, double magnitude) {
        Vec3 centre = Vec3.atCenterOf(epicentre);
        String detail = String.format(Locale.ROOT, "M%.1f", magnitude);
        IncidentLog.record(level, epicentre, IncidentType.EARTHQUAKE, InesScale.NONE, detail);
        SeismicNetwork.onEarthquake(level, centre, EarthquakeModel.detectorPower(magnitude));
        for (ServerPlayer player : level.players()) {
            double motion = EarthquakeModel.groundMotion(magnitude, player.position().distanceTo(centre));
            if (motion >= FELT) {
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.AMBIENT,
                        (float) Math.min(2.0, motion / 4.0), 0.5f);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) Math.min(200, 40 + motion * 10), 0, false, false));
                player.displayClientMessage(Component.translatable(ReactorMaintenance.KEY + "earthquake", detail)
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
        double trip = MaintenanceConfig.seismicTrip;
        int scrammed = 0;
        for (ReactorBlockEntity reactor : MultiblockRegistry.all(level, ReactorBlockEntity.class)) {
            double motion = EarthquakeModel.groundMotion(magnitude, Vec3.atCenterOf(reactor.getBlockPos()).distanceTo(centre));
            boolean trips = reactor.isFormed() && EarthquakeModel.trips(motion, trip);
            if (motion > 0.05 * trip) {
                boolean wasScrammed = reactor.isScramRequested();
                reactor.shake(EarthquakeModel.severity(motion, trip), trips);
                if (trips && !wasScrammed) {
                    scrammed++;
                }
            }
        }
        for (SteamGeneratorControllerBlockEntity generator : MultiblockRegistry.all(level, SteamGeneratorControllerBlockEntity.class)) {
            double motion = EarthquakeModel.groundMotion(magnitude, Vec3.atCenterOf(generator.getBlockPos()).distanceTo(centre));
            generator.tubeWear().shock(EarthquakeModel.severity(motion, trip));
        }
        for (AbstractTurbineControllerBlockEntity turbine : MultiblockRegistry.all(level, AbstractTurbineControllerBlockEntity.class)) {
            double motion = EarthquakeModel.groundMotion(magnitude, Vec3.atCenterOf(turbine.getBlockPos()).distanceTo(centre));
            turbine.bladeWear().shock(EarthquakeModel.severity(motion, trip));
        }
        return scrammed;
    }
}
