package org.papiricoh.create_nuclearindustry.reactor.pwr.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * Makes the hydrogen in a reactor building or a sealed containment visible: wisps of
 * {@link HydrogenGasParticle} in the air of the room, most of them in its upper layers, more as the
 * fraction grows (a few at 1%, a haze from the flammability limit, thick at certain ignition).
 * Client only, run from the reactor's and the monitor's client ticks, culled by distance and by
 * the player's particle setting.
 */
public final class HydrogenHaze {
    /** Farthest the player may be from the room for its gas to show, in blocks. */
    private static final double RANGE = 48.0;
    /** Wisps a tick for a room of {@link #REFERENCE_VOLUME} m³ at certain ignition. */
    private static final float FULL_RATE = 6.0f;
    private static final float REFERENCE_VOLUME = 1000.0f;
    /** Most wisps one room adds in a tick. */
    private static final int MAX_PER_TICK = 12;
    private static final int ATTEMPTS_PER_WISP = 3;
    /** Room kept between a new wisp and the walls and roof, in blocks. */
    private static final double MARGIN = 1.6;

    private HydrogenHaze() {}

    public static void reactorTick(ReactorBlockEntity reactor) {
        spawn(reactor.getLevel(), reactor.getAccidents().building().bounds(), reactor.getBuildingHydrogen());
    }

    public static void containmentTick(ContainmentMonitorBlockEntity monitor) {
        spawn(monitor.getLevel(), monitor.clientBounds(), monitor.getHydrogen());
    }

    /**
     * Wisps a tick for a room of {@code volume} m³ at hydrogen {@code fraction}: rising faster than
     * the fraction, so 1% is a few wisps and 8% (certain ignition) the full rate; bigger rooms get
     * more, but less than in proportion, since the player sees only part of them.
     */
    public static float rate(double fraction, int volume) {
        if (!(fraction > 0)) {
            return 0f;
        }
        double share = Math.min(1.25, fraction / ContainmentModel.CERTAIN_IGNITION);
        double size = Math.max(0.25, Math.min(2.0, Math.pow(volume / REFERENCE_VOLUME, 0.75)));
        return (float) (FULL_RATE * Math.pow(share, 1.3) * size);
    }

    private static void spawn(@Nullable Level level, @Nullable BoundingBox box, double fraction) {
        if (level == null || !level.isClientSide || box == null || !(fraction > 0)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        ParticleStatus status = minecraft.options.particles().get();
        if (status == ParticleStatus.MINIMAL) {
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        double dx = Math.max(0, Math.max(box.minX() - eye.x, eye.x - (box.maxX() + 1)));
        double dy = Math.max(0, Math.max(box.minY() - eye.y, eye.y - (box.maxY() + 1)));
        double dz = Math.max(0, Math.max(box.minZ() - eye.z, eye.z - (box.maxZ() + 1)));
        if (dx * dx + dy * dy + dz * dz > RANGE * RANGE) {
            return;
        }
        int volume = box.getXSpan() * box.getYSpan() * box.getZSpan();
        float expected = rate(fraction, volume) * (status == ParticleStatus.DECREASED ? 0.4f : 1f);
        RandomSource random = level.random;
        int count = Math.min(MAX_PER_TICK, (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0));
        double strength = Math.min(1.0, fraction / ContainmentModel.CERTAIN_IGNITION);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < ATTEMPTS_PER_WISP; attempt++) {
                // clear of the walls and the roof, which the big wisps would cut through
                double x = box.minX() + MARGIN + random.nextDouble() * Math.max(0.0, box.getXSpan() - 2 * MARGIN);
                double z = box.minZ() + MARGIN + random.nextDouble() * Math.max(0.0, box.getZSpan() - 2 * MARGIN);
                // most of the gas sits in the top layers: hydrogen is far lighter than air
                double depth = Math.pow(random.nextDouble(), 3.5) * 0.75 * box.getYSpan();
                double y = Math.max(box.minY() + 0.5, box.maxY() + 1 - MARGIN - depth);
                cursor.set(x, y, z);
                if (level.getBlockState(cursor).isAir()) {
                    level.addParticle(AllNuclearParticles.HYDROGEN_GAS.get(), x, y, z, strength, 0, 0);
                    break;
                }
            }
        }
    }
}
