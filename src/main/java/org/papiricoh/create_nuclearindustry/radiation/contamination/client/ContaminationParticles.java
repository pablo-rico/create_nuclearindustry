package org.papiricoh.create_nuclearindustry.radiation.contamination.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;

/**
 * Dust over contaminated ground near the player, thicker with the chunk's synced severity.
 * Reads only the 25 chunks around the player and nothing from the server beyond that byte.
 */
public final class ContaminationParticles {
    /** Chunks out from the player's own that show dust. */
    private static final int RANGE = 2;
    /** Average specks per tick per chunk at severity 1. */
    private static final float PER_SEVERITY = 0.12f;

    private ContaminationParticles() {}

    /** Ships in sight carry their contamination too: dust over their decks, drawn in their plot. */
    private static void shipDust(ClientLevel level, Player player, RandomSource random, float density) {
        if (!ShipSpace.enabled()) {
            return;
        }
        for (ShipSpace.Ship ship : ShipSpace.shipsIn(level, player.getBoundingBox().inflate(RANGE * 16 + 16))) {
            BoundingBox box = ship.localBounds();
            int severity = org.papiricoh.create_nuclearindustry.integration.sable.client.ShipSeverities.severity(ship.id());
            if (severity <= 0) {
                continue;
            }
            float expected = severity * PER_SEVERITY * density * Math.max(1, box.getXSpan() * box.getZSpan()) / 256f;
            int count = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
            for (int i = 0; i < count; i++) {
                int x = box.minX() + random.nextInt(box.getXSpan());
                int z = box.minZ() + random.nextInt(box.getZSpan());
                int y = box.maxY();
                while (y >= box.minY() && level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                    y--;
                }
                if (y >= box.minY()) {
                    // Placed where the ship is drawn, not in its plot.
                    net.minecraft.world.phys.Vec3 at = ship.toWorld(new net.minecraft.world.phys.Vec3(
                            x + random.nextDouble(), y + 1 + random.nextDouble() * 0.3, z + random.nextDouble()));
                    level.addParticle(AllNuclearParticles.RADIOACTIVE_DUST.get(), at.x, at.y, at.z, 0, 0, 0);
                }
            }
        }
    }

    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllNuclearParticles.RADIOACTIVE_DUST.get(), RadioactiveDustParticle.Provider::new);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused() || !ClientConfig.contaminationParticles) {
            return;
        }
        ParticleStatus status = minecraft.options.particles().get();
        if (status == ParticleStatus.MINIMAL) {
            return;
        }
        float density = (float) ClientConfig.particleDensity * (status == ParticleStatus.DECREASED ? 0.4f : 1f);
        if (density <= 0) {
            return;
        }
        RandomSource random = level.random;
        shipDust(level, player, random, density);
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centerX + dx, centerZ + dz, false);
                if (chunk == null) {
                    continue;
                }
                Byte severity = chunk.getExistingDataOrNull(AllNuclearRadiation.CONTAMINATION_SEVERITY.get());
                if (severity == null || severity <= 0) {
                    continue;
                }
                float expected = severity * PER_SEVERITY * density;
                int count = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
                for (int i = 0; i < count; i++) {
                    int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
                    int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
                    int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
                    level.addParticle(AllNuclearParticles.RADIOACTIVE_DUST.get(),
                            x + random.nextDouble(), y + random.nextDouble() * 0.3, z + random.nextDouble(), 0, 0, 0);
                }
            }
        }
    }
}
