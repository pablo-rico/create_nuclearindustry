package org.papiricoh.create_nuclearindustry.radiation.cache;

import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.Radioactivity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How strongly a placed block irradiates, per block state, looked up once: the {@code radioactivity}
 * data map of the block's item (waste barrels, ore blocks), or of the bucket of the liquid it holds,
 * less as a flowing liquid thins out. Forgotten when data maps reload. The radiation scan also uses
 * it to skip chunk sections whose palette holds nothing radioactive.
 */
public final class BlockRadioactivity {
    /**
     * @param perSecond dose rate of the block before distance and shielding
     * @param solid     true when it comes from the block's item (so geological storage applies),
     *                  false for a liquid
     */
    public record Source(double perSecond, boolean solid) {}

    private static final Source NONE = new Source(0, false);
    private static final Map<BlockState, Source> CACHE = new ConcurrentHashMap<>();

    private BlockRadioactivity() {}

    /** The state's radiation, or {@code null} if it gives off none. */
    public static Source of(BlockState state) {
        Source source = CACHE.computeIfAbsent(state, BlockRadioactivity::compute);
        return source == NONE ? null : source;
    }

    /** Whether the state gives off anything; a chunk section without such states is skipped. */
    public static boolean emits(BlockState state) {
        return of(state) != null;
    }

    /** Forgets every state, for when the data maps change. */
    public static void invalidate() {
        CACHE.clear();
    }

    private static Source compute(BlockState state) {
        if (state.isAir()) {
            return NONE;
        }
        Radioactivity radioactivity = state.getBlock().asItem().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        if (radioactivity != null) {
            return radioactivity.perSecond() > 0 ? new Source(radioactivity.perSecond(), true) : NONE;
        }
        var fluid = state.getFluidState();
        if (fluid.isEmpty()) {
            return NONE;
        }
        Radioactivity liquid = fluid.getType().getBucket().builtInRegistryHolder().getData(AllNuclearRadiation.RADIOACTIVITY);
        double strength = liquid == null ? 0 : liquid.perSecond() * fluid.getAmount() / 8.0;
        return strength > 0 ? new Source(strength, false) : NONE;
    }
}
