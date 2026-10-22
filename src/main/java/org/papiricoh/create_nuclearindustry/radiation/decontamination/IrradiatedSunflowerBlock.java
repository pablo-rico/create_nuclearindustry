package org.papiricoh.create_nuclearindustry.radiation.decontamination;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.DoublePlantBlock;

/**
 * A sunflower that has drawn its share of fallout out of the ground and wilted. It absorbs no
 * more; broken, it gives Irradiated Biomass to be compacted into low-level waste.
 */
public class IrradiatedSunflowerBlock extends DoublePlantBlock {
    public static final MapCodec<IrradiatedSunflowerBlock> CODEC = simpleCodec(IrradiatedSunflowerBlock::new);

    public IrradiatedSunflowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }
}
