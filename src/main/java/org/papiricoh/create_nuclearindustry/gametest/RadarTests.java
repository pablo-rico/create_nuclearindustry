package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarNetwork;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarTrack;

import java.util.List;

/**
 * Missile radars on real flights: reach and horizon (and their switches), seeing everything with a base range of 0, sharing
 * tracks over a link code, the defended zone that gates the redstone output, acknowledging, and what clients are told. Each test
 * sets the radar config and puts it back within one tick, working the picture out synchronously.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public final class RadarTests {
    private static final String BATCH = "radar";

    private record Saved(double base, double earth, double network) {
        static Saved take() {
            return new Saved(MissileConfig.radarBaseRange, MissileConfig.radarEarthRadius, MissileConfig.radarNetworkRange);
        }

        void restore() {
            MissileConfig.radarBaseRange = base;
            MissileConfig.radarEarthRadius = earth;
            MissileConfig.radarNetworkRange = network;
        }
    }

    private static final java.util.Set<RadarBlockEntity> created = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final java.util.Set<RadarBlockEntity> stopped = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private static RadarBlockEntity radar(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = h.getBlockEntity(pos);
        radar.setSpeed((float) MissileConfig.minimumRpm);
        created.add(radar);
        return radar;
    }

    /** A pad missile climbing from {@code start}. */
    private static Flight flight(ServerLevel level, Vec3 start) {
        return FlightTracker.get(level).launch(level, MissileDesign.LEGACY_PAD, start, Vec3.ZERO, start.add(1000, -start.y + level.getMinBuildHeight(), 0),
                BlockPos.containing(start), Warhead.INERT, null);
    }

    /** Whether {@code radar} tracks {@code flight} now. A radar with no source loses its speed each tick: it is spun again first. */
    private static boolean sees(RadarBlockEntity radar, Flight flight) {
        for (RadarBlockEntity other : RadarNetwork.radars(radar.getLevel()))
            if (created.contains(other) && !stopped.contains(other)) other.setSpeed((float) MissileConfig.minimumRpm);
        RadarNetwork.refresh((ServerLevel) radar.getLevel());
        radar.refresh();
        return radar.tracks().stream().anyMatch(t -> t.id().equals(flight.id()));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void withNoRangeARadarSeesTheWholeDimension(GameTestHelper h) {
        RadarBlockEntity radar = radar(h, new BlockPos(3, 1, 3));
        ServerLevel level = h.getLevel();
        Vec3 start = new Vec3(20_000_000, 1000, 20_000_000);
        Flight first = flight(level, start);
        Flight second = flight(level, start.add(0, 0, 100));
        h.runAfterDelay(2, () -> {
            Saved saved = Saved.take();
            boolean both, alarm, afterStop;
            try {
                MissileConfig.radarBaseRange = 0;
                MissileConfig.radarEarthRadius = 0;
                both = sees(radar, first) && sees(radar, second);
                alarm = radar.alarm();
                radar.setSpeed(0);
                stopped.add(radar);
                afterStop = sees(radar, first) || radar.alarm();
            } finally {
                saved.restore();
                FlightTracker.get(level).remove(level, first);
                FlightTracker.get(level).remove(level, second);
            }
            h.assertTrue(both, "a radar with no base range sees flights twenty million blocks away");
            h.assertTrue(alarm, "and with no zone set any track sets its redstone off");
            h.assertTrue(!afterStop, "a radar that stops turning sees nothing and goes quiet");
            h.assertTrue(!level.hasChunkAt(BlockPos.containing(start)), "and never loads a flight's chunk");
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void reachAndHorizonLimitWhatARadarSees(GameTestHelper h) {
        RadarBlockEntity radar = radar(h, new BlockPos(3, 1, 3));
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
        Flight high = flight(level, new Vec3(at.getX() + 4000, 1000, at.getZ()));
        Flight low = flight(level, new Vec3(at.getX() + 4000, at.getY() + 2, at.getZ() + 300));
        h.runAfterDelay(2, () -> {
            Saved saved = Saved.take();
            boolean highSeen, lowHidden, lowWithoutHorizon, highOutOfReach;
            try {
                MissileConfig.radarBaseRange = 3000;
                MissileConfig.radarEarthRadius = 16000;
                MissileConfig.radarNetworkRange = 4096;
                // A burning rocket shows 3000 × 8^¼ ≈ 5000 blocks off.
                highSeen = sees(radar, high);
                lowHidden = !sees(radar, low);
                MissileConfig.radarEarthRadius = 0;
                lowWithoutHorizon = sees(radar, low);
                MissileConfig.radarBaseRange = 100;
                highOutOfReach = !sees(radar, high);
            } finally {
                saved.restore();
                FlightTracker.get(level).remove(level, high);
                FlightTracker.get(level).remove(level, low);
            }
            h.assertTrue(highSeen, "a rocket climbing 4 km off, high above the horizon, is in reach");
            h.assertTrue(lowHidden, "one as far but skimming the ground is below the horizon");
            h.assertTrue(lowWithoutHorizon, "with no horizon it shows");
            h.assertTrue(highOutOfReach, "and with a short base range neither does");
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void radarsOnOneCodeShareTheirTracks(GameTestHelper h) {
        RadarBlockEntity near = radar(h, new BlockPos(2, 1, 2));
        RadarBlockEntity far = radar(h, new BlockPos(12, 1, 12));
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        Flight flight = flight(level, new Vec3(at.getX() - 44, at.getY(), at.getZ()));
        h.runAfterDelay(2, () -> {
            Saved saved = Saved.take();
            boolean nearSees, farAlone, farLinked, sameTrack;
            try {
                MissileConfig.radarBaseRange = 30;
                MissileConfig.radarEarthRadius = 0;
                MissileConfig.radarNetworkRange = 4096;
                // Reach 30 × 8^¼ ≈ 50: the flight is 44 blocks from one radar and 57 from the other.
                nearSees = sees(near, flight);
                farAlone = sees(far, flight);
                near.setCode("ab-12");
                far.setCode("AB12");
                farLinked = sees(far, flight);
                List<RadarTrack> a = near.tracks(), b = far.tracks();
                sameTrack = a.stream().filter(t -> t.id().equals(flight.id())).count() == 1
                        && b.stream().filter(t -> t.id().equals(flight.id())).count() == 1;
            } finally {
                saved.restore();
                FlightTracker.get(level).remove(level, flight);
            }
            h.assertTrue(nearSees && !farAlone, "only the nearer radar reaches the flight on its own");
            h.assertTrue("AB12".equals(near.code()), "a typed code is cleaned up, got " + near.code());
            h.assertTrue(farLinked, "on one code the other radar shows it too");
            h.assertTrue(sameTrack, "once each, as one track");
            h.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH, timeoutTicks = 100)
    public static void theDefendedZoneGatesTheRedstoneAndAckOnlySilences(GameTestHelper h) {
        RadarBlockEntity radar = radar(h, new BlockPos(3, 1, 3));
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(3, 1, 3));
        Flight flight = flight(level, new Vec3(at.getX() + 200, at.getY() + 100, at.getZ()));
        h.runAfterDelay(2, () -> {
            Saved saved = Saved.take();
            boolean elsewhere, onIt, unackedBefore, ackedQuiet, ackedStillSignals, sampled;
            CompoundTag update;
            try {
                MissileConfig.radarBaseRange = 0;
                MissileConfig.radarEarthRadius = 0;
                sees(radar, flight);
                RadarTrack track = radar.tracks().stream().filter(t -> t.id().equals(flight.id())).findFirst().orElse(null);
                h.assertTrue(track != null, "the radar tracks the flight");
                radar.setZone(at.getX() + 5000, at.getZ() + 5000, 50);
                elsewhere = radar.threats().stream().noneMatch(t -> t.id().equals(flight.id()));
                radar.setZone((int) track.impact().x(), (int) track.impact().z(), 50);
                onIt = radar.threats().stream().anyMatch(t -> t.id().equals(flight.id())) && radar.alarm();
                unackedBefore = radar.alarmUnacked();
                radar.acknowledge();
                ackedQuiet = !radar.alarmUnacked();
                ackedStillSignals = radar.alarm();
                update = radar.getUpdateTag(level.registryAccess());
                MonitorReading sample = radar.plantSample();
                sampled = sample.kind() == TargetKind.RADAR && sample.value(MonitorReading.D_THREATS) >= 1;
            } finally {
                saved.restore();
                FlightTracker.get(level).remove(level, flight);
            }
            h.assertTrue(elsewhere, "a zone far from the predicted impact sees no threat");
            h.assertTrue(onIt, "a zone on it does, and sets the redstone off");
            h.assertTrue(unackedBefore && ackedQuiet, "acknowledging silences the alarm");
            h.assertTrue(ackedStillSignals, "but the redstone stays on while the threat lasts");
            h.assertTrue(update.getInt("Tracks") == radar.trackCount() && update.getInt("Threats") == radar.threatCount() && update.getInt("Tracks") > 0,
                    "clients (goggles, screens) are told the counts, got " + update.getInt("Tracks"));
            h.assertTrue(sampled, "and the control room reads the threat");
            h.succeed();
        });
    }
}
