package org.papiricoh.create_nuclearindustry.reactor.visual;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TransparentBlock;

/**
 * A lead glass window for the fission reactor's casing wall, to watch the fuel channels and the
 * Cherenkov glow inside. Like glass, it hides the faces it shares with another viewport.
 */
public class ReactorViewportBlock extends TransparentBlock {
    public static final MapCodec<ReactorViewportBlock> CODEC = simpleCodec(ReactorViewportBlock::new);

    public ReactorViewportBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ReactorViewportBlock> codec() {
        return CODEC;
    }
}
