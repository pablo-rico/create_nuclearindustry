package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.*;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.*;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public final class RadarTests {
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void radarIncludesRemoteInertFlightsAndStopsOnPowerLoss(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = h.getBlockEntity(pos);
        radar.setSpeed((float) MissileConfig.minimumRpm);
        FlightTracker tracker = FlightTracker.get(h.getLevel());
        Vec3 start = new Vec3(20_000_000, 1000, 20_000_000);
        Flight first = tracker.launch(h.getLevel(), MissileDesign.LEGACY_PAD, start, Vec3.ZERO, start.add(1000, -900, 0), BlockPos.containing(start), Warhead.INERT, null);
        Flight second = tracker.launch(h.getLevel(), MissileDesign.LEGACY_PAD, start.add(0, 0, 100), Vec3.ZERO, start.add(1000, -900, 100), BlockPos.containing(start), Warhead.INERT, null);
        h.runAfterDelay(21, () -> {
            radar.setSpeed((float) MissileConfig.minimumRpm);
            radar.tick();
            h.assertTrue(radar.alarm(), "powered radar emits alarm for remote inert flights");
            var contacts = RadarContacts.get(h.getLevel());
            h.assertTrue(contacts.stream().anyMatch(c -> c.id().equals(first.id())) && contacts.stream().anyMatch(c -> c.id().equals(second.id())), "dimension snapshot includes both distant inert contacts");
            h.assertTrue(contacts == RadarContacts.get(h.getLevel()), "snapshot shared within sample period");
            h.assertTrue(!h.getLevel().hasChunkAt(BlockPos.containing(start)), "radar never loads remote flight chunks");
            radar.setSpeed(0); radar.tick();
            h.assertTrue(!radar.alarm() && radar.contacts().isEmpty(), "rotation loss immediately removes detection and alarm");
            tracker.remove(h.getLevel(), first); tracker.remove(h.getLevel(), second);
            h.runAfterDelay(21, () -> {
                h.assertTrue(RadarContacts.get(h.getLevel()).stream().noneMatch(c -> c.id().equals(first.id()) || c.id().equals(second.id())), "removed flights disappear from next sample");
                h.succeed();
            });
        });
    }
}
