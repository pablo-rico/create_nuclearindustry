package org.papiricoh.create_nuclearindustry.chemistry.electrolyzer;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlock;

import java.util.function.Supplier;

public class ElectrolyzerBlock extends KineticProcessingBlock {
    public static final MapCodec<ElectrolyzerBlock> CODEC = simpleCodec(ElectrolyzerBlock::new);

    public ElectrolyzerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ElectrolyzerBlock> codec() {
        return CODEC;
    }

    @Override
    protected Supplier<? extends BlockEntityType<ElectrolyzerBlockEntity>> blockEntityType() {
        return AllNuclearEntities.ELECTROLYZER;
    }
}
