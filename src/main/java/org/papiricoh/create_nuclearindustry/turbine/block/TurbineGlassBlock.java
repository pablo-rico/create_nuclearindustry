package org.papiricoh.create_nuclearindustry.turbine.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TransparentBlock;

/**
 * Reinforced glass for turbine walls. Panes join into one window through Create's connected
 * textures (registered client-side in {@code NuclearModelSwaps}).
 */
public class TurbineGlassBlock extends TransparentBlock {
    public static final MapCodec<TurbineGlassBlock> CODEC = simpleCodec(TurbineGlassBlock::new);

    public TurbineGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends TurbineGlassBlock> codec() {
        return CODEC;
    }
}
