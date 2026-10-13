package org.papiricoh.create_nuclearindustry.enrichment.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;

import java.util.function.Supplier;

public class CentrifugeBlock extends KineticProcessingBlock {
    public static final MapCodec<CentrifugeBlock> CODEC = simpleCodec(CentrifugeBlock::new);

    public CentrifugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends CentrifugeBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<? extends BlockEntityType<CentrifugeBlockEntity>> blockEntityType() {
        return AllNuclearEntities.CENTRIFUGE;
    }
}
