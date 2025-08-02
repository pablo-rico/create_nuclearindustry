package org.papiricoh.create_nuclearindustry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ControlRodBlock extends Block {
    public ControlRodBlock(Properties p) {
        super(p.noOcclusion()); // <— important if the model isn’t a full cube
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        // prevents neighbor face culling even if properties change
        return Shapes.empty();
    }
}
