package org.papiricoh.create_nuclearindustry.reactor.pwr;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A pressurizer standing on a reactor's lid: two to four Pressurizer or Pressurizer Heater Port
 * blocks stacked on one column, capped by a Relief Valve.
 *
 * @param base        lowest block of the stack, right above the lid
 * @param segments    Pressurizer and heater port blocks; each holds {@link PrimaryLoop#PRESSURIZER_VOLUME_PER_SEGMENT}
 * @param heaterPorts the heater port blocks among them
 * @param reliefValve the valve on top
 */
public record PressurizerStack(BlockPos base, int segments, List<BlockPos> heaterPorts, BlockPos reliefValve) {
    public static final int MIN_SEGMENTS = 2;
    public static final int MAX_SEGMENTS = 4;
    /** Blocks above the lid a pressurizer can take, valve included. */
    public static final int ZONE_HEIGHT = MAX_SEGMENTS + 1;

    public PressurizerStack {
        heaterPorts = List.copyOf(heaterPorts);
    }

    /** Every block of the stack, bottom to top. */
    public List<BlockPos> blocks() {
        List<BlockPos> blocks = new ArrayList<>(segments + 1);
        for (int i = 0; i < segments; i++) {
            blocks.add(base.above(i));
        }
        blocks.add(reliefValve);
        return blocks;
    }

    public static boolean isSegment(BlockState state) {
        return state.is(PwrRegistry.PRESSURIZER.get()) || state.is(PwrRegistry.PRESSURIZER_HEATER_PORT.get());
    }

    public static boolean isPart(BlockState state) {
        return isSegment(state) || state.is(PwrRegistry.RELIEF_VALVE.get());
    }

    /**
     * Reads the stack whose lowest block is {@code base}. Returns null and adds a warning when the
     * column holds pressurizer blocks that do not make a valid stack.
     */
    @Nullable
    public static PressurizerStack read(Level level, BlockPos base, List<String> warnings) {
        int segments = 0;
        List<BlockPos> heaters = new ArrayList<>();
        BlockPos pos = base;
        while (segments <= MAX_SEGMENTS && isSegment(level.getBlockState(pos))) {
            if (level.getBlockState(pos).is(PwrRegistry.PRESSURIZER_HEATER_PORT.get())) {
                heaters.add(pos);
            }
            segments++;
            pos = pos.above();
        }
        if (segments < MIN_SEGMENTS || segments > MAX_SEGMENTS) {
            warnings.add("Pressurizer at " + base.toShortString() + " needs " + MIN_SEGMENTS + "-" + MAX_SEGMENTS
                    + " blocks under its relief valve, found " + segments);
            return null;
        }
        if (!level.getBlockState(pos).is(PwrRegistry.RELIEF_VALVE.get())) {
            warnings.add("Pressurizer at " + base.toShortString() + " needs a Relief Valve on top");
            return null;
        }
        return new PressurizerStack(base, segments, heaters, pos);
    }
}
