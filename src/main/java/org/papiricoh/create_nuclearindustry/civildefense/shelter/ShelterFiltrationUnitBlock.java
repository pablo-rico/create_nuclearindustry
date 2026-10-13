package org.papiricoh.create_nuclearindustry.civildefense.shelter;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;

/**
 * Air filter and blower for a fallout shelter. Placed inside a room (on the floor or against a wall,
 * its shaft can come through the wall), it surveys the air around it; a closed room with walls strong
 * enough is certified as a shelter, which cuts blast waves inside, and while the unit turns fast
 * enough it filters fallout and contaminated ground air out of it.
 */
public class ShelterFiltrationUnitBlock extends RotatedPillarKineticBlock implements IBE<ShelterFiltrationUnitBlockEntity> {
    public static final MapCodec<ShelterFiltrationUnitBlock> CODEC = simpleCodec(ShelterFiltrationUnitBlock::new);

    public ShelterFiltrationUnitBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends ShelterFiltrationUnitBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<ShelterFiltrationUnitBlockEntity> getBlockEntityClass() {
        return ShelterFiltrationUnitBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ShelterFiltrationUnitBlockEntity> getBlockEntityType() {
        return CivilDefenseRegistry.SHELTER_FILTRATION_UNIT_BE.get();
    }
}
