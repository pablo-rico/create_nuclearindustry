package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The silos near this player, as the client sees them: steam venting from the shaft in the last seconds of a countdown, and
 * where a rocket lifting off came out of, so its flash and exhaust come out of the right hole. Reads only what the
 * controllers sync to the client; changes no block.
 */
final class SiloFx {
    /** Chunks round the player searched for silos. */
    private static final int SEARCH_CHUNKS = 8;
    /** Ticks before lift-off the vents open. */
    static final int VENT_TICKS = 60;

    /** A silo's opening: the centre of its 3x3 mouth at the top of its walls, and its floor. */
    record Mouth(double x, double y, double z, double floorY) {
        Vec3 centre() {
            return new Vec3(x, y, z);
        }
    }

    private static final class Silo {
        final BlockPos controller;
        BlockPos floor;
        int height;
        int shaft;
        int lastSynced = Integer.MIN_VALUE;
        double estimate = -1;
        boolean vented;

        Silo(BlockPos controller) {
            this.controller = controller;
        }

        Mouth mouth() {
            return new Mouth(floor.getX() + 0.5, floor.getY() + 1 + (shaft >= height ? height : 0), floor.getZ() + 0.5, floor.getY() + 1);
        }
    }

    private final Map<BlockPos, Silo> silos = new HashMap<>();

    void clear() {
        silos.clear();
    }

    /** Looks for silos and follows their countdowns; steams the ones about to launch. */
    void tick(ClientLevel level, Vec3 eye, long gameTime, RandomSource random) {
        if (gameTime % 20 == 0) scan(level, eye);
        Iterator<Silo> it = silos.values().iterator();
        while (it.hasNext()) {
            Silo silo = it.next();
            if (!(level.getBlockEntity(silo.controller) instanceof SiloControllerEntity controller)) {
                it.remove();
                continue;
            }
            silo.floor = controller.floor();
            silo.shaft = controller.shaftHeight();
            if (controller.rocketHeight() > 0) silo.height = controller.rocketHeight();
            if (controller.state() != SiloControllerEntity.State.COUNTDOWN) {
                silo.estimate = -1;
                silo.vented = false;
                silo.lastSynced = Integer.MIN_VALUE;
                continue;
            }
            // The controller syncs its countdown only now and then: count down here in between.
            if (controller.countdown() != silo.lastSynced) {
                silo.lastSynced = controller.countdown();
                silo.estimate = controller.countdown();
            } else {
                silo.estimate = Math.max(0, silo.estimate - 1);
            }
            if (silo.height <= 0 || silo.estimate > VENT_TICKS || silo.estimate < 0) continue;
            Mouth mouth = silo.mouth();
            double distance = eye.distanceTo(mouth.centre());
            if (!silo.vented) {
                silo.vented = true;
                RocketSounds.oneShot(RocketFxRegistry.SILO_VENT.get(), mouth.centre(), distance, 200, 1.0f, 1.0f, false);
            }
            if (distance < 256) vent(level, mouth, 1 - silo.estimate / VENT_TICKS, random);
        }
    }

    /** Steam boiling out of the shaft round the rocket, harder as lift-off nears. */
    private static void vent(ClientLevel level, Mouth mouth, double urgency, RandomSource random) {
        int count = (int) Math.round((2 + 6 * urgency) * ClientConfig.rocketDetail);
        for (int i = 0; i < count; i++) {
            // From the ring between the rocket and the walls, a jet of steam shooting up and spreading.
            double a = random.nextDouble() * Math.PI * 2;
            double r = 0.9 + random.nextDouble() * 0.6;
            double x = mouth.x() + Math.cos(a) * r, z = mouth.z() + Math.sin(a) * r;
            double up = (0.25 + 0.3 * random.nextDouble()) * (0.6 + 0.6 * urgency);
            RocketParticles.smoke(level, false, x, mouth.y() - 0.3, z, Math.cos(a) * 0.05, up, Math.sin(a) * 0.05,
                    new RocketParticles.Puff(0.96f, 0.97f, 0.98f, 0.5f, 0.6f, 2.8f + 1.5f * (float) urgency, 22 + random.nextInt(22), 0, 0.3f));
        }
    }

    /** Where a rocket lifting off at {@code pos} came out of, if from a silo this client knows. */
    @Nullable
    Mouth launchedFrom(Vec3 pos) {
        Silo best = null;
        for (Silo silo : silos.values()) {
            if (silo.floor == null || silo.height <= 0 || silo.shaft < silo.height) continue;
            double dx = silo.floor.getX() + 0.5 - pos.x, dz = silo.floor.getZ() + 0.5 - pos.z;
            double dy = silo.floor.getY() + 1 - pos.y;
            if (dx * dx + dz * dz <= 4 && Math.abs(dy) <= 3) best = silo;
        }
        return best == null ? null : best.mouth();
    }

    private void scan(ClientLevel level, Vec3 eye) {
        int cx = BlockPos.containing(eye).getX() >> 4, cz = BlockPos.containing(eye).getZ() >> 4;
        for (int x = cx - SEARCH_CHUNKS; x <= cx + SEARCH_CHUNKS; x++) {
            for (int z = cz - SEARCH_CHUNKS; z <= cz + SEARCH_CHUNKS; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof SiloControllerEntity) silos.computeIfAbsent(be.getBlockPos().immutable(), Silo::new);
                }
            }
        }
    }
}
