package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorMachineBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;

import java.util.function.Supplier;

/**
 * A machine beside the beamline that serves it from a shaft (or Forge Energy): a klystron, a magnet
 * power supply or a vacuum pump. It counts for the accelerator when it touches a beamline block.
 */
public class AcceleratorMachineBlock extends ShaftFacingBlock implements IBE<AcceleratorMachineBlockEntity>, AttachmentRole.Block {
    private final AttachmentRole role;
    private final Supplier<BlockEntityType<AcceleratorMachineBlockEntity>> type;

    public AcceleratorMachineBlock(AttachmentRole role, Supplier<BlockEntityType<AcceleratorMachineBlockEntity>> type,
                                   Properties properties) {
        super(properties);
        this.role = role;
        this.type = type;
    }

    @Override
    public AttachmentRole attachmentRole(BlockState state) {
        return role;
    }

    public AttachmentRole role() {
        return role;
    }

    @Override
    public Class<AcceleratorMachineBlockEntity> getBlockEntityClass() {
        return AcceleratorMachineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AcceleratorMachineBlockEntity> getBlockEntityType() {
        return type.get();
    }
}
