package org.papiricoh.create_nuclearindustry.radiation.scrubber;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;

/** Draws contamination out of the ground around it. Driven by a shaft from below. */
public class RadiationScrubberBlock extends KineticBlock implements IBE<RadiationScrubberBlockEntity> {
    public static final MapCodec<RadiationScrubberBlock> CODEC = simpleCodec(RadiationScrubberBlock::new);

    public RadiationScrubberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RadiationScrubberBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public Class<RadiationScrubberBlockEntity> getBlockEntityClass() {
        return RadiationScrubberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RadiationScrubberBlockEntity> getBlockEntityType() {
        return AllNuclearEntities.RADIATION_SCRUBBER.get();
    }
}
