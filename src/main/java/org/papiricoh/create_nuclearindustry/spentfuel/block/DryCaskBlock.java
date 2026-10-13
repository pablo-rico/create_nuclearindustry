package org.papiricoh.create_nuclearindustry.spentfuel.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;

/** Dry storage cask for spent fuel that is Warm or cooler. */
public class DryCaskBlock extends SpentFuelStorageBlock<DryCaskBlockEntity> {
    public static final MapCodec<DryCaskBlock> CODEC = simpleCodec(DryCaskBlock::new);

    public DryCaskBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DryCaskBlock> codec() {
        return CODEC;
    }

    @Override
    public Class<DryCaskBlockEntity> getBlockEntityClass() {
        return DryCaskBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DryCaskBlockEntity> getBlockEntityType() {
        return SpentFuelRegistry.DRY_CASK_ENTITY.get();
    }
}
