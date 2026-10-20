package org.papiricoh.create_nuclearindustry.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.link.*;
import org.papiricoh.create_nuclearindustry.missile.orbit.*;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import java.util.UUID;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class GroundStationTests {
    private static final String BATCH = "ground_station";
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static GroundStationBlockEntity station(GameTestHelper h) {
        h.setBlock(AT, OrbitRegistry.GROUND_STATION.get());
        return h.getBlockEntity(AT);
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void needsRotationAndCompleteCode(GameTestHelper h) {
        var station = station(h);
        station.setCode("ab"); station.setSpeed((float) MissileConfig.minimumRpm);
        boolean incomplete = station.operational();
        station.setCode("ab-12x"); station.setSpeed(0);
        boolean stopped = station.operational();
        station.setSpeed((float) MissileConfig.minimumRpm);
        boolean working = station.operational();
        var lines = new GroundStationDisplaySource().read(station);
        boolean displaysCode = lines.get(1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && java.util.Arrays.asList(text.getArgs()).contains("AB12");
        h.succeedWhen(() -> {
            h.assertTrue(!incomplete && !stopped && working, "station needs full code and rotation");
            h.assertTrue(station.code().equals("AB12"), "station sanitizes code");
            h.assertTrue(lines.size() == 4, "display provides status, code, count and pass");
            h.assertTrue(displaysCode, "display names network");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void configurePayloadSanitizesAndChecksDistance(GameTestHelper h) {
        var station = station(h);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "station_test"));
        BlockPos pos = station.getBlockPos();
        player.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.containerMenu = new GroundStationMenu(1, player.getInventory(), station, pos);
        boolean accepted = GroundStationConfigurePayload.apply(player, new GroundStationConfigurePayload(pos, "a-b12z"));
        String code = station.code();
        player.moveTo(pos.getX() + 100, pos.getY(), pos.getZ());
        boolean remote = GroundStationConfigurePayload.apply(player, new GroundStationConfigurePayload(pos, "FAIL"));
        h.succeedWhen(() -> {
            h.assertTrue(accepted && code.equals("AB12"), "nearby menu edit is sanitized");
            h.assertTrue(!remote && station.code().equals("AB12"), "distant edit rejected");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void linkerStoresGroundStation(GameTestHelper h) {
        var station = station(h);
        ItemStack stack = new ItemStack(SiloRegistry.TACTICAL_LINKER.get());
        var player = TacticalLinkerTests.player(h, stack);
        TacticalLinkerTests.click(h, player, stack, AT);
        TacticalLink stored = TacticalLinkerItem.getLink(stack);
        h.succeedWhen(() -> {
            h.assertTrue(stored != null, "linker stores station");
            h.assertTrue(stored.kind() == TacticalLink.Kind.GROUND_STATION && stored.target().pos().equals(station.getBlockPos()), "station kind and position preserved");
        });
    }
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void stationCountsSatellitesAndShowsCurrentPass(GameTestHelper h) {
        var station = station(h);
        station.setCode("S2GS");
        var level = h.getLevel();
        var pos = station.getBlockPos();
        var satellite = new Satellite(UUID.randomUUID(), "CM-T", "S2GS", SatelliteKind.COMMS,
                Orbit.geo(pos.getX(), pos.getZ(), 1000, level.getGameTime(), Orbit.Bounds.UNBOUNDED), null, level.getGameTime());
        SatelliteTracker.get(level).add(level, satellite);
        long[] sample = {-1, -1};
        h.startSequence().thenIdle(21).thenExecute(() -> {
            sample[0] = station.satelliteCount(); sample[1] = station.nextPass();
            SatelliteTracker.get(level).remove(level, satellite.id(), SatelliteTracker.Removal.DEORBITED);
        });
        h.succeedWhen(() -> {
            h.assertTrue(sample[0] == 1, "station counts its satellite");
            h.assertTrue(sample[1] == 0, "station sees the parked satellite now");
        });
    }
}
