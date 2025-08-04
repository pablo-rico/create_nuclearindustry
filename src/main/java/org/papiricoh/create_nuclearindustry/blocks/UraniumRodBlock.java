package org.papiricoh.create_nuclearindustry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UraniumRodBlock extends Block {

    // Partes del modelo
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape TOP    = Block.box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape CORE   = Block.box(1, 1, 1, 15, 15, 15);

    private static final VoxelShape PILLAR_NW = Block.box(0, 1, 0, 1, 15, 1);
    private static final VoxelShape PILLAR_NE = Block.box(15, 1, 0, 16, 15, 1);
    private static final VoxelShape PILLAR_SW = Block.box(0, 1, 15, 1, 15, 16);
    private static final VoxelShape PILLAR_SE = Block.box(15, 1, 15, 16, 15, 16);

    // Unión de todas las piezas
    private static final VoxelShape SHAPE = Shapes.or(
        BOTTOM, TOP, CORE,
        PILLAR_NW, PILLAR_NE, PILLAR_SW, PILLAR_SE
    ).optimize();

    public UraniumRodBlock(Properties props) {
        // como no es un cubo lleno, evita culling de vecinos
        super(props.noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // opcional: si tuviste problemas de culling de vecinos, deja la oclusión vacía
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}