package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarScopeBlockEntity;

/** Radar scopes join into one screen and show their linked radar's picture, relative to the radar. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RadarScopeTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void scopesJoinAndShowTheirRadar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos radarPos = new BlockPos(6, 1, 10);
        helper.setBlock(radarPos, RadarRegistry.RADAR.get());
        RadarBlockEntity radar = helper.getBlockEntity(radarPos);
        for (int x = 3; x <= 4; x++) for (int y = 1; y <= 2; y++)
            helper.setBlock(new BlockPos(x, y, 3), RadarRegistry.SCOPE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        RadarScopeBlockEntity scope = helper.getBlockEntity(new BlockPos(4, 2, 3));
        scope.setTarget(GlobalPos.of(level.dimension(), helper.absolutePos(radarPos)));
        BlockPos at = helper.absolutePos(radarPos);
        Flight flight = FlightTracker.get(level).launch(level, MissileDesign.LEGACY_PAD, new Vec3(at.getX() + 100, at.getY() + 50, at.getZ()), Vec3.ZERO,
                new Vec3(at.getX() - 500, at.getY(), at.getZ()), at, Warhead.INERT, null);
        helper.succeedWhen(() -> {
            radar.setSpeed((float) MissileConfig.minimumRpm);
            RadarScopeBlockEntity origin = helper.getBlockEntity(new BlockPos(3, 1, 3));
            helper.assertTrue(origin.isOrigin() && origin.screenWidth() == 2 && origin.screenHeight() == 2,
                    "four scopes make one 2×2 screen from the bottom-left one, got " + origin.screenWidth() + "×" + origin.screenHeight());
            helper.assertTrue(origin.getTarget() != null, "the screen keeps the link made on one of its scopes");
            RadarScopeBlockEntity.Picture picture = origin.picture();
            helper.assertTrue(picture.status() >= 0, "it shows a radar, got status " + picture.status());
            helper.assertTrue(picture.echoes().stream().anyMatch(e -> e.id().equals(flight.id()) && Math.abs(e.dx() - 100) < 40),
                    "with the flight a hundred blocks east of it, got " + picture.echoes());
            FlightTracker.get(level).remove(level, flight);
        });
    }
}
