package org.papiricoh.create_nuclearindustry.radiation.contamination.client;

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
