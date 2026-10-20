package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarMenu;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarRegistry;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainSurvey;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;

import java.util.List;

/**
 * The terrain the radar screens and command table draw: surveyed only for a player a viewer vouches for, only near it, cached per
 * dimension, and read from the loaded ground where there is one.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TerrainTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void terrainIsSurveyedOnlyForAnOpenRadarScreen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(6, 1, 6);
        helper.setBlock(pos, RadarRegistry.RADAR.get());
        BlockPos at = helper.absolutePos(pos);
        RadarBlockEntity radar = helper.getBlockEntity(pos);
        FakePlayer player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "terrain_test"));
        player.moveTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 2.5);
        long near = TerrainTile.key(3, TerrainTile.tileOf(3, at.getX()), TerrainTile.tileOf(3, at.getZ()));
        long far = TerrainTile.key(0, TerrainTile.tileOf(0, at.getX()) + 200, TerrainTile.tileOf(0, at.getZ()));
        int refused = TerrainSurvey.request(player, at, List.of(near));
        player.containerMenu = new RadarMenu(7, player.getInventory(), radar, at);
        int accepted = TerrainSurvey.request(player, at, List.of(near, far));
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(refused == 0, "without the radar's screen open nothing is surveyed");
        helper.assertTrue(accepted == 1, "with it open, only the tile near the radar is, got " + accepted);
        helper.succeedWhen(() -> {
            helper.assertTrue(TerrainSurvey.cached(level, near), "the tile is surveyed off the server thread and cached");
            ServerLevel nether = level.getServer().getLevel(Level.NETHER);
            helper.assertTrue(nether == null || !TerrainSurvey.cached(nether, near), "for its own dimension only");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void loadedGroundIsReadAsItIs(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(6, 1, 6));
        long key = TerrainTile.key(0, TerrainTile.tileOf(0, at.getX()), TerrainTile.tileOf(0, at.getZ()));
        byte[] data = TerrainSurvey.sampleNow(helper.getLevel(), key);
        int i = Math.floorDiv(at.getX() - TerrainTile.origin(0, TerrainTile.tx(key)), 8);
        int j = Math.floorDiv(at.getZ() - TerrainTile.origin(0, TerrainTile.tz(key)), 8);
        int surface = TerrainTile.surface(data, i, j);
        helper.assertTrue((surface & TerrainTile.REAL) != 0, "the arena's chunk is loaded: its sample is real");
        helper.assertTrue(TerrainTile.height(data, i, j) <= helper.getLevel().getMaxBuildHeight(), "with a sane height");
        helper.succeed();
    }
}
