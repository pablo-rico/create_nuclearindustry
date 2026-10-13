package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.IonSourceBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementBlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;

/**
 * The ion source: sits right in front of the accelerator controller and turns hydrogen into a beam of
 * protons. Its shaft must come in across the beam, so it is placed with the shaft up unless a shaft
 * beside it says otherwise.
 */
public class IonSourceBlock extends ShaftFacingBlock implements IBE<IonSourceBlockEntity>, BeamElementBlock {

    public IonSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction defaultFacing(BlockPlaceContext context) {
        return Direction.UP;
    }

    @Override
    public BeamElementRole beamRole(BlockState state) {
        return BeamElementRole.ION_SOURCE;
    }

    @Override
    public Class<IonSourceBlockEntity> getBlockEntityClass() {
        return IonSourceBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IonSourceBlockEntity> getBlockEntityType() {
        return AcceleratorRegistry.ION_SOURCE_ENTITY.get();
    }
}
