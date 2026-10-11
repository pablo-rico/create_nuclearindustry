package org.papiricoh.create_nuclearindustry.spentfuel;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;
import org.papiricoh.create_nuclearindustry.spentfuel.block.PoolCoolerBlockEntity;
import org.papiricoh.create_nuclearindustry.spentfuel.block.SpentFuelRackBlockEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The flood fills of spent fuel pools, shared by the racks in them. Every rack used to fill its
 * whole pool (up to {@code spentFuel.poolScanLimit} cells) on its own every few seconds, so a pool
 * of N racks cost N fills; now the first rack to ask fills it and the others in the same water reuse
 * the result for {@link #LIFETIME} ticks, the interval a rack used to wait between its own fills.
 * Boiling a layer off forces a fresh fill. Entries hold no level, and the per-level maps are dropped
 * on level unload and server stop.
 */
@EventBusSubscriber(modid = Create_NuclearIndustry.MODID)
public final class SpentFuelPoolSurvey {
    /** Ticks a fill stays valid: five rack updates of twenty ticks, as before. */
    public static final int LIFETIME = 100;

    /**
     * One fill: the cells it reached, what it found in them and when it was made. {@code racks}
     * counts the racks standing in the water itself; a dry rack under the pool adds itself.
     */
    public record Survey(SealedVolume volume, boolean sealed, int racks, double coolerCapacity, int topY,
                         List<BlockPos> surface, long madeAt) {
        boolean covers(BlockPos pos) {
            return volume.contains(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static final Map<Level, List<Survey>> SURVEYS = new WeakHashMap<>();
    /** Flood fills run since the server started; tests compare it before and after. */
    private static int fills;

    private SpentFuelPoolSurvey() {}

    /** The fill of the water around {@code start}, from a fresh-enough fill that reached it, or a new one. */
    public static synchronized Survey of(Level level, BlockPos start) {
        long now = level.getGameTime();
        List<Survey> surveys = SURVEYS.computeIfAbsent(level, ignored -> new ArrayList<>());
        Iterator<Survey> iterator = surveys.iterator();
        while (iterator.hasNext()) {
            Survey survey = iterator.next();
            if (expired(survey, now)) {
                iterator.remove();
            } else if (survey.covers(start)) {
                return survey;
            }
        }
        Survey survey = fill(level, start, now);
        surveys.add(survey);
        return survey;
    }

    /** Fills the water around {@code start} now, replacing any cached fill that reached it. */
    public static synchronized Survey refresh(Level level, BlockPos start) {
        List<Survey> surveys = SURVEYS.computeIfAbsent(level, ignored -> new ArrayList<>());
        surveys.removeIf(survey -> survey.covers(start));
        Survey survey = fill(level, start, level.getGameTime());
        surveys.add(survey);
        return survey;
    }

    /** Flood fills run so far, for tests. */
    public static synchronized int fills() {
        return fills;
    }

    /** Whether a level still has cached fills, for tests of the clean-up. */
    public static synchronized boolean tracks(Level level) {
        return SURVEYS.containsKey(level);
    }

    /** Forgets every cached fill, for tests. */
    public static synchronized void clear() {
        SURVEYS.clear();
    }

    @SubscribeEvent
    public static synchronized void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            SURVEYS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clear();
    }

    /** Outlived its lifetime, or made in another timeline (the game time went back). */
    private static boolean expired(Survey survey, long now) {
        return now - survey.madeAt() >= LIFETIME || now < survey.madeAt();
    }

    private static Survey fill(Level level, BlockPos start, long now) {
        fills++;
        int[] racks = {0};
        double[] coolers = {0.0};
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        SealedVolume.World water = new SealedVolume.World() {
            @Override
            public boolean known(int x, int y, int z) {
                return !level.isOutsideBuildHeight(y) && level.isLoaded(cursor.set(x, y, z));
            }

            @Override
            public boolean passable(int x, int y, int z) {
                cursor.set(x, y, z);
                if (!isWaterSource(level, cursor)) {
                    return false;
                }
                if (level.getBlockState(cursor).hasBlockEntity()) {
                    BlockEntity entity = level.getBlockEntity(cursor);
                    if (entity instanceof SpentFuelRackBlockEntity) {
                        racks[0]++;
                    } else if (entity instanceof PoolCoolerBlockEntity cooler) {
                        coolers[0] += cooler.coolingCapacity();
                    }
                }
                return true;
            }

            @Override
            public double wallRating(int x, int y, int z) {
                return 0.0;
            }
        };
        SealedVolume volume = SealedVolume.fill(water, start.getX(), start.getY(), start.getZ(), SpentFuelConfig.poolScanLimit);
        boolean sealed = volume.state() == SealedVolume.State.SEALED;
        List<BlockPos> surface = new ArrayList<>();
        if (sealed) {
            for (int x = volume.minX(); x <= volume.maxX(); x++) {
                for (int z = volume.minZ(); z <= volume.maxZ(); z++) {
                    if (volume.contains(x, volume.maxY(), z)) {
                        surface.add(new BlockPos(x, volume.maxY(), z));
                    }
                }
            }
        }
        return new Survey(volume, sealed, racks[0], coolers[0], volume.maxY(), List.copyOf(surface), now);
    }

    public static boolean isWaterSource(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(FluidTags.WATER) && fluid.isSource();
    }
}
